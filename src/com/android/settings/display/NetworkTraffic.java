/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preference.SystemSettingMainSwitchPreference;

import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.zeph.support.preference.SystemSettingListPreference;
import org.zeph.support.preference.SystemSettingSeekBarPreference;

public class NetworkTraffic extends SettingsPreferenceFragment
        implements OnMainSwitchChangeListener {

    private SystemSettingListPreference mIndicatorMode;
    private SystemSettingSeekBarPreference mThreshold;
    private SystemSettingSeekBarPreference mInterval;

    private SystemSettingMainSwitchPreference mSwitchBar;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.network_traffic);

        final boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.NETWORK_TRAFFIC_STATE, 0) == 1;

        mSwitchBar = (SystemSettingMainSwitchPreference) findPreference("network_traffic_state");
        mSwitchBar.addOnSwitchChangeListener(this);

        mIndicatorMode = (SystemSettingListPreference) findPreference("network_traffic_mode");
        mIndicatorMode.setEnabled(enabled);

        mThreshold = (SystemSettingSeekBarPreference) findPreference("network_traffic_autohide_threshold");
        mThreshold.setEnabled(enabled);

        mInterval = (SystemSettingSeekBarPreference) findPreference("network_traffic_refresh_interval");
        mInterval.setEnabled(enabled);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mIndicatorMode.setEnabled(isChecked);
        mThreshold.setEnabled(isChecked);
        mInterval.setEnabled(isChecked);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }
}
