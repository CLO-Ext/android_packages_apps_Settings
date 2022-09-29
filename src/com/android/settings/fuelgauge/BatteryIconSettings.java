/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.zeph.support.preference.SystemSettingListPreference;
import org.zeph.support.preference.SystemSettingSwitchPreference;

public class BatteryIconSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_STYLE = "status_bar_battery_style";
    private static final String KEY_PERCENT = "status_bar_show_battery_percent";
    private static final String KEY_PERCENT_INSIDE = "status_bar_show_battery_percent_inside";

    private SystemSettingListPreference mStyle;
    private SystemSettingSwitchPreference mPercent;
    private SystemSettingSwitchPreference mPercentInside;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.battery_icon);

        final boolean isText = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE,
                0, UserHandle.USER_CURRENT) == 2;
        final boolean percentOn = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.SHOW_BATTERY_PERCENT,
                0, UserHandle.USER_CURRENT) == 1;

        mStyle = (SystemSettingListPreference) findPreference(KEY_STYLE);
        mStyle.setOnPreferenceChangeListener(this);

        mPercent = (SystemSettingSwitchPreference) findPreference(KEY_PERCENT);
        mPercent.setEnabled(!isText);
        mPercent.setOnPreferenceChangeListener(this);

        mPercentInside = (SystemSettingSwitchPreference) findPreference(KEY_PERCENT_INSIDE);
        mPercentInside.setEnabled(!isText && percentOn);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStyle) {
            final boolean isText = Integer.valueOf((String) newValue) == 2;
            final boolean percentOn = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.SHOW_BATTERY_PERCENT,
                    0, UserHandle.USER_CURRENT) == 1;
            mPercent.setEnabled(!isText);
            mPercentInside.setEnabled(!isText && percentOn);
        } else if (preference == mPercent) {
            final boolean isText = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_STYLE,
                    0, UserHandle.USER_CURRENT) == 2;
            final boolean percentOn = (Boolean) newValue;
            mPercent.setEnabled(!isText);
            mPercentInside.setEnabled(!isText && percentOn);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }
}
