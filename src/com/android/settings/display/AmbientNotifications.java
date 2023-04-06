/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preference.SystemSettingMainSwitchPreference;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class AmbientNotifications extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {    


    private static final String PREF_AMBIENT_TIME_OUT = "ambient_notif_timeout";
    private static final String PREF_ALERT_SLIDER = "ambient_alert_slider";

    private ListPreference mAmbientTimeOut;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.ambient_notifications);
        
        Resources systemUiResources;
        try {
            systemUiResources = getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        if (!getActivity().getResources().getBoolean(com.android.internal.R.bool.config_hasAlertSlider)) {
            getPreferenceScreen().removePreference(findPreference(PREF_ALERT_SLIDER));
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mAmbientTimeOut = (ListPreference) findPreference(PREF_AMBIENT_TIME_OUT);
        mAmbientTimeOut.setOnPreferenceChangeListener(this);
        int ambientTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_NOTIF_TIMEOUT, defaultTimeOut);
        mAmbientTimeOut.setValue(String.valueOf(ambientTimeOut));
        updateAmbientTimeOutSummary(ambientTimeOut);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mAmbientTimeOut) {
            int ambientTimeOut = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.AMBIENT_NOTIF_TIMEOUT,
                    ambientTimeOut);
            updateAmbientTimeOutSummary(ambientTimeOut);
        }
        return false;
    }

    private void updateAmbientTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary, value / 1000);
        mAmbientTimeOut.setSummary(summary);
    }
}
