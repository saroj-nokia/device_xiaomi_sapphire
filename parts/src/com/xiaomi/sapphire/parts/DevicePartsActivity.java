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

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new DevicePartsFragment())
                .commit();
    }
}
