/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.sapphire.parts;

import android.util.Log;

import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class ThermalUtils {

    private static final String TAG = "DeviceParts";

    public static final String THERMAL_BALANCE_MODE_PATH =
            "/sys/class/thermal/thermal_message/balance_mode";
    public static final String THERMAL_SCONFIG_PATH =
            "/sys/class/thermal/thermal_message/sconfig";

    public static final String PREF_THERMAL_BALANCE = "thermal_balance_mode";
    public static final String PREF_THERMAL_SCONFIG = "thermal_sconfig";
    public static final String PREF_ZRAM_SIZE = "zram_size";

    public static final String DEFAULT_THERMAL_BALANCE = "2";
    public static final String DEFAULT_THERMAL_SCONFIG = "0";
    public static final String DEFAULT_ZRAM_SIZE = "0";

    public static boolean writeNode(String path, String value) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(value.getBytes());
            fos.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + value + " to " + path, e);
            return false;
        }
    }

    public static String readNode(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine().trim();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + path, e);
            return null;
        }
    }

    public static void applyThermalBalance(String value) {
        writeNode(THERMAL_BALANCE_MODE_PATH, value);
    }

    public static void applyThermalSconfig(String value) {
        writeNode(THERMAL_SCONFIG_PATH, value);
    }

    public static void applyZramSize(String value) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("set", String.class, String.class);
            // Mudamos de persist.vendor.zram_size para persist.vendor.zram_size
            method.invoke(null, "persist.vendor.zram_size", value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set system property", e);
        }
    }

    public static void restoreSettings(android.content.Context context) {
        android.content.SharedPreferences prefs =
                android.preference.PreferenceManager.getDefaultSharedPreferences(
                        context.createDeviceProtectedStorageContext());

        String balance = prefs.getString(PREF_THERMAL_BALANCE, DEFAULT_THERMAL_BALANCE);
        String sconfig = prefs.getString(PREF_THERMAL_SCONFIG, DEFAULT_THERMAL_SCONFIG);
        String zramSize = prefs.getString(PREF_ZRAM_SIZE, DEFAULT_ZRAM_SIZE);

        applyThermalBalance(balance);
        applyThermalSconfig(sconfig);
        applyZramSize(zramSize);

        Log.i(TAG, "Restored settings: balance=" + balance + " sconfig=" + sconfig + " zram=" + zramSize);
    }
}
