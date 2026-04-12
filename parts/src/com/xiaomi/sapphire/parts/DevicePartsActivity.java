/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.sapphire.parts;

import android.app.Activity;
import android.os.Bundle;

public class DevicePartsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_parts);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DevicePartsFragment())
                .commit();
    }
}
