/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.touchgrip;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Tells the FocalTech touch IC how the screen is rotated, so its edge
 * (palm/grip) rejection follows the long edges in portrait and the short
 * edges in landscape. The driver maps 90 to "USB port on the right" and
 * 270 to "USB port on the left"; anything else is portrait.
 *
 * The IC drops the setting when the touch panel suspends, and the driver
 * refuses writes while suspended, so it is sent again on every screen on.
 */
public class TouchGripApp extends Application implements DisplayManager.DisplayListener {

    private static final String TAG = "TouchGrip";
    private static final String GRIP_AREA = "/proc/tp_grip_area";

    // The touch panel resumes with the display; give it a moment
    private static final long SCREEN_ON_DELAY_MS = 500;

    private DisplayManager mDisplayManager;
    private Handler mHandler;
    private int mLastAngle = -1;

    private final BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.postDelayed(() -> update(true), SCREEN_ON_DELAY_MS);
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

        mDisplayManager = getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(this, mHandler);
        registerReceiver(mScreenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON),
                null, mHandler);

        mHandler.post(() -> update(true));
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
}
