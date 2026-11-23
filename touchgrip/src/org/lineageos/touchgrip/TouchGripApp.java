/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.touchgrip;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Keeps the FocalTech touch IC in step with what is on screen:
 *
 * - Screen rotation: the edge (palm/grip) rejection follows the long edges
 *   in portrait and the short edges in landscape. The driver maps 90 to
 *   "USB port on the right" and 270 to "USB port on the left"; anything
 *   else is portrait.
 * - Games: while a game is in the foreground (an app that declares itself
 *   a game, or one built on a game engine such as Unity, Unreal, Cocos,
 *   Godot or libGDX), the IC is
 *   put in its game mode (full-rate scanning, as MIUI's Game Turbo does)
 *   through vendor.touch.game_mode and the touch_game_mode service, and
 *   mi_thermald is switched to its game thermal profile (thermal-tgame),
 *   so a long session does not run on the everyday profile.
 *
 * The IC drops both settings when the touch panel suspends, and the driver
 * refuses writes while suspended, so both are sent again on every screen on.
 */
public class TouchGripApp extends Application implements DisplayManager.DisplayListener {

    private static final String TAG = "TouchGrip";
    private static final String GRIP_AREA = "/proc/tp_grip_area";
    private static final String PROP_GAME_MODE = "vendor.touch.game_mode";
    private static final String THERMAL_SCENE =
            "/sys/class/thermal/thermal_message/sconfig";
    private static final String SCONFIG_DEFAULT = "0";
    private static final String SCONFIG_GAME = "18";

    // Native libraries of the common game engines
    private static final String[] GAME_ENGINE_LIBS = {
        "libunity.so", "libil2cpp.so",                         // Unity
        "libUE4.so", "libUnreal.so",                           // Unreal
        "libcocos2dcpp.so", "libcocos2djs.so", "libcocos.so",  // Cocos
        "libgodot_android.so",                                 // Godot
        "libgdx.so",                                           // libGDX
    };

    // The touch panel resumes with the display; give it a moment
    private static final long SCREEN_ON_DELAY_MS = 500;

    private DisplayManager mDisplayManager;
    private PackageManager mPackageManager;
    private Handler mHandler;
    private int mLastAngle = -1;

    private final Map<String, Boolean> mEngineCache = new HashMap<>();

    private String mTopPackage = "";
    private boolean mGameMode = false;
    // Alternates so that re-sending the same state still changes the property
    private boolean mToggle = false;

    private final BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.postDelayed(() -> {
                update(true);
                if (mGameMode) {
                    sendGameMode(true);
                }
            }, SCREEN_ON_DELAY_MS);
        }
    };

    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            mHandler.post(() -> updateGameMode());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // The touch IC is global, only the system user drives it
        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) return;

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());

        mPackageManager = getPackageManager();
        mDisplayManager = getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(this, mHandler);
        registerReceiver(mScreenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON),
                null, mHandler);

        try {
            ActivityTaskManager.getService().registerTaskStackListener(mTaskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener", e);
        }

        mHandler.post(() -> {
            update(true);
            // Start from a known state after a reboot or a crash
            sendGameMode(false);
            updateGameMode();
        });
    }

    @Override
    public void onDisplayAdded(int displayId) {
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            update(false);
        }
    }

    private void update(boolean force) {
        Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) return;

        final int angle;
        switch (display.getRotation()) {
            case Surface.ROTATION_90:
                angle = 90;
                break;
            case Surface.ROTATION_180:
                angle = 180;
                break;
            case Surface.ROTATION_270:
                angle = 270;
                break;
            default:
                angle = 0;
                break;
        }

        if (!force && angle == mLastAngle) return;

        // On failure (panel suspended) keep the old value so the next
        // screen on or rotation tries again
        if (write(angle)) {
            mLastAngle = angle;
        }
    }

    // mi_thermald picks its thermal-*.conf from this scene id, as MIUI's
    // PowerKeeper sets it: 0 is thermal-normal, 18 thermal-tgame
    private static void writeThermalScene(String scene) {
        try (FileWriter writer = new FileWriter(THERMAL_SCENE)) {
            writer.write(scene);
        } catch (IOException e) {
            Log.w(TAG, "Failed to set thermal scene " + scene + ": " + e.getMessage());
        }
    }

    private static boolean write(int angle) {
        try (FileWriter writer = new FileWriter(GRIP_AREA)) {
            writer.write(String.valueOf(angle));
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Failed to set grip angle " + angle + ": " + e.getMessage());
            return false;
        }
    }

    private void updateGameMode() {
        String pkg = getTopPackage();
        if (pkg == null || pkg.equals(mTopPackage)) return;
        mTopPackage = pkg;

        boolean game = isGame(pkg);
        if (game != mGameMode) {
            mGameMode = game;
            sendGameMode(game);
        }
    }

    private static String getTopPackage() {
        try {
            ActivityTaskManager.RootTaskInfo info =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            if (info == null) return null;
            ComponentName top = info.topActivity;
            return top != null ? top.getPackageName() : null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to get the focused task: " + e.getMessage());
            return null;
        }
    }

    private boolean isGame(String pkg) {
        final ApplicationInfo ai;
        try {
            ai = mPackageManager.getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        if (ai.category == ApplicationInfo.CATEGORY_GAME
                || (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true;
        }
        // Launcher, Settings and friends are never games
        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) return false;

        // Many games do not declare themselves as one; look for a game
        // engine instead. sourceDir changes on every update, so an updated
        // app is checked again.
        String key = pkg + ":" + ai.sourceDir;
        Boolean cached = mEngineCache.get(key);
        if (cached == null) {
            cached = hasGameEngine(ai);
            mEngineCache.put(key, cached);
            Log.i(TAG, pkg + (cached ? " uses a game engine" : " is not a game"));
        }
        return cached;
    }

    private static boolean hasGameEngine(ApplicationInfo ai) {
        // Extracted native libraries
        if (ai.nativeLibraryDir != null) {
            String[] libs = new File(ai.nativeLibraryDir).list();
            if (libs != null) {
                for (String lib : libs) {
                    if (isEngineLib(lib)) return true;
                }
            }
        }

        // Libraries kept inside the APKs (extractNativeLibs="false", splits)
        List<String> apks = new ArrayList<>();
        if (ai.sourceDir != null) apks.add(ai.sourceDir);
        if (ai.splitSourceDirs != null) apks.addAll(Arrays.asList(ai.splitSourceDirs));
        for (String apk : apks) {
            try (ZipFile zip = new ZipFile(apk)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.startsWith("lib/")) continue;
                    if (isEngineLib(name.substring(name.lastIndexOf('/') + 1))) return true;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to read " + apk + ": " + e.getMessage());
            }
        }
        return false;
    }

    private static boolean isEngineLib(String lib) {
        for (String engine : GAME_ENGINE_LIBS) {
            if (engine.equals(lib)) return true;
        }
        return false;
    }

    private void sendGameMode(boolean enable) {
        writeThermalScene(enable ? SCONFIG_GAME : SCONFIG_DEFAULT);

        mToggle = !mToggle;
        String value = (enable ? "1" : "0") + (mToggle ? "a" : "b");
        try {
            SystemProperties.set(PROP_GAME_MODE, value);
            Log.i(TAG, "Touch game mode " + (enable ? "on" : "off"));
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to set " + PROP_GAME_MODE + ": " + e.getMessage());
        }
    }
}
