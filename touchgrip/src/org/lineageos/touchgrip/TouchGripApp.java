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

import java.io.FileWriter;
import java.io.IOException;

/**
 * Keeps the FocalTech touch IC in step with what is on screen:
 *
 * - Screen rotation: the edge (palm/grip) rejection follows the long edges
 *   in portrait and the short edges in landscape. The driver maps 90 to
 *   "USB port on the right" and 270 to "USB port on the left"; anything
 *   else is portrait.
 * - Games: while an app marked as a game is in the foreground, the IC is
 *   put in its game mode (full-rate scanning, as MIUI's Game Turbo does)
 *   through vendor.touch.game_mode and the touch_game_mode service.
 *
 * The IC drops both settings when the touch panel suspends, and the driver
 * refuses writes while suspended, so both are sent again on every screen on.
 */
public class TouchGripApp extends Application implements DisplayManager.DisplayListener {

    private static final String TAG = "TouchGrip";
    private static final String GRIP_AREA = "/proc/tp_grip_area";
    private static final String PROP_GAME_MODE = "vendor.touch.game_mode";

    // The touch panel resumes with the display; give it a moment
    private static final long SCREEN_ON_DELAY_MS = 500;

    private DisplayManager mDisplayManager;
    private PackageManager mPackageManager;
    private Handler mHandler;
    private int mLastAngle = -1;

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
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(pkg, 0);
            return ai.category == ApplicationInfo.CATEGORY_GAME
                    || (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void sendGameMode(boolean enable) {
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
