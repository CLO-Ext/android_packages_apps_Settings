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

import org.neoteric.preference.SystemSettingListPreference;

public class BatteryIconSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_STYLE = "status_bar_battery_style";
    private static final String KEY_PERCENT = "status_bar_battery_percent";

    private SystemSettingListPreference mStyle;
    private SystemSettingListPreference mPercent;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.battery_icon);

        final int style = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE,
                0, UserHandle.USER_CURRENT);
        final boolean isText = style == 2;
        final boolean isHidden = style == 3;

        mStyle = (SystemSettingListPreference) findPreference(KEY_STYLE);
        mStyle.setOnPreferenceChangeListener(this);

        mPercent = (SystemSettingListPreference) findPreference(KEY_PERCENT);
        mPercent.setEnabled(!isText && !isHidden);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStyle) {
            final int style = Integer.valueOf((String) newValue);
            final boolean isText = style == 2;
            final boolean isHidden = style == 3;
            mPercent.setEnabled(!isText && !isHidden);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NEOTERIC;
    }
}
