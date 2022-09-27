/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preference.SystemSettingMainSwitchPreference;

import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.zeph.support.colorpicker.ColorPickerPreference;
import org.zeph.support.preference.SystemSettingListPreference;
import org.zeph.support.preference.SystemSettingSwitchPreference;

public class EdgeLightSettings extends SettingsPreferenceFragment implements
        OnMainSwitchChangeListener, OnPreferenceChangeListener {

    private static String KEY_ENABLED = "edge_light_enabled";
    private static String KEY_ALWAYS_TRIGGER = "edge_light_always_trigger_on_pulse";
    private static String KEY_REPEAT = "edge_light_repeat_animation";
    private static String KEY_COLOR_MODE = "edge_light_color_mode";
    private static String KEY_COLOR = "edge_light_custom_color";

    private ContentResolver mResolver;

    private SystemSettingMainSwitchPreference mEnabled;
    private SystemSettingSwitchPreference mAlwaysTrigger;
    private SystemSettingSwitchPreference mRepeat;
    private SystemSettingListPreference mColorMode;
    private ColorPickerPreference mColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edge_light_settings);

        mResolver = getContentResolver();

        final boolean enabled = Settings.System.getIntForUser(mResolver,
                Settings.System.EDGE_LIGHT_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
        final boolean customColor = Settings.System.getIntForUser(mResolver,
                Settings.System.EDGE_LIGHT_COLOR_MODE,
                0, UserHandle.USER_CURRENT) == 3;
        final String color = Settings.System.getStringForUser(mResolver,
                Settings.System.EDGE_LIGHT_CUSTOM_COLOR,
                UserHandle.USER_CURRENT);

        mEnabled = (SystemSettingMainSwitchPreference) findPreference(KEY_ENABLED);
        mEnabled.addOnSwitchChangeListener(this);

        mAlwaysTrigger = (SystemSettingSwitchPreference) findPreference(KEY_ALWAYS_TRIGGER);
        mAlwaysTrigger.setEnabled(enabled);

        mRepeat = (SystemSettingSwitchPreference) findPreference(KEY_REPEAT);
        mRepeat.setEnabled(enabled);

        mColorMode = (SystemSettingListPreference) findPreference(KEY_COLOR_MODE);
        mColorMode.setEnabled(enabled);
        mColorMode.setOnPreferenceChangeListener(this);

        final String colorStr = TextUtils.isEmpty(color) ? "#ffffff" : color;
        mColor = (ColorPickerPreference) findPreference(KEY_COLOR);
        mColor.setDefaultValue(ColorPickerPreference.convertToColorInt("#ffffff"));
        mColor.setEnabled(enabled && customColor);
        mColor.setSummary(colorStr);
        mColor.setNewPreviewColor(ColorPickerPreference.convertToColorInt(colorStr));
        mColor.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mColorMode) {
            final int value = Integer.valueOf((String) newValue);
            final boolean enabled = Settings.System.getIntForUser(mResolver,
                    Settings.System.EDGE_LIGHT_ENABLED,
                    0, UserHandle.USER_CURRENT) == 1;
            mColor.setEnabled(enabled && value == 3);
        } else if (preference == mColor) {
            final String color = ColorPickerPreference.convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
            mColor.setSummary(color);
            Settings.System.putStringForUser(mResolver,
                    Settings.System.EDGE_LIGHT_CUSTOM_COLOR,
                    color, UserHandle.USER_CURRENT);
        }
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final boolean customColor = Settings.System.getIntForUser(mResolver,
                Settings.System.EDGE_LIGHT_COLOR_MODE,
                0, UserHandle.USER_CURRENT) == 3;
        mAlwaysTrigger.setEnabled(isChecked);
        mRepeat.setEnabled(isChecked);
        mColorMode.setEnabled(isChecked);
        mColor.setEnabled(isChecked && customColor);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }
}
