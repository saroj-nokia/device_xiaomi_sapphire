/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.sapphire.parts;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class DevicePartsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private ListPreference mThermalSconfig;
    private ListPreference mThermalBalance;
    private ListPreference mZramSize;

    private String[] mSconfigDescriptions;
    private String[] mBalanceDescriptions;
    private String[] mZramDescriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setStorageDeviceProtected();
        addPreferencesFromResource(R.xml.parts_preferences);

        Resources res = getResources();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

        mSconfigDescriptions = res.getStringArray(R.array.thermal_sconfig_descriptions);
        mBalanceDescriptions = res.getStringArray(R.array.thermal_balance_descriptions);
        mZramDescriptions = res.getStringArray(R.array.zram_size_descriptions);

        mThermalSconfig = (ListPreference) findPreference(ThermalUtils.PREF_THERMAL_SCONFIG);
        mThermalSconfig.setValue(prefs.getString(
                ThermalUtils.PREF_THERMAL_SCONFIG, ThermalUtils.DEFAULT_THERMAL_SCONFIG));
        updateSummary(mThermalSconfig, mSconfigDescriptions);
        mThermalSconfig.setOnPreferenceChangeListener(this);

        mThermalBalance = (ListPreference) findPreference(ThermalUtils.PREF_THERMAL_BALANCE);
        mThermalBalance.setValue(prefs.getString(
                ThermalUtils.PREF_THERMAL_BALANCE, ThermalUtils.DEFAULT_THERMAL_BALANCE));
        updateSummary(mThermalBalance, mBalanceDescriptions);
        mThermalBalance.setOnPreferenceChangeListener(this);

        mZramSize = (ListPreference) findPreference(ThermalUtils.PREF_ZRAM_SIZE);
        mZramSize.setValue(prefs.getString(
                ThermalUtils.PREF_ZRAM_SIZE, ThermalUtils.DEFAULT_ZRAM_SIZE));
        updateSummary(mZramSize, mZramDescriptions);
        mZramSize.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;

        if (preference == mThermalSconfig) {
            ThermalUtils.applyThermalSconfig(value);
            mThermalSconfig.setValue(value);
            updateSummary(mThermalSconfig, mSconfigDescriptions);
            return true;
        }

        if (preference == mThermalBalance) {
            ThermalUtils.applyThermalBalance(value);
            mThermalBalance.setValue(value);
            updateSummary(mThermalBalance, mBalanceDescriptions);
            return true;
        }

        if (preference == mZramSize) {
            ThermalUtils.applyZramSize(value);
            mZramSize.setValue(value);
            updateSummary(mZramSize, mZramDescriptions);
            return true;
        }

        return false;
    }

    private void updateSummary(ListPreference pref, String[] descriptions) {
        int index = pref.findIndexOfValue(pref.getValue());
        if (index >= 0 && index < descriptions.length) {
            pref.setSummary(pref.getEntry() + "\n" + descriptions[index]);
        } else {
            pref.setSummary(pref.getEntry());
        }
    }
}

