/*
 * Copyright (C) 2023 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preference.SystemSettingMainSwitchPreference;

public class PocketJudge extends SettingsPreferenceFragment {

    private static final String ALERT_SLIDER_CAT = "pocket_alert_slider";

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.pocket_judge);

        if (!getActivity().getResources().getBoolean(com.android.internal.R.bool.config_pocketModeSupported) || !getActivity().getResources().getBoolean(com.android.internal.R.bool.config_hasAlertSlider)) {
            getPreferenceScreen().removePreference(findPreference(ALERT_SLIDER_CAT));
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }
}
