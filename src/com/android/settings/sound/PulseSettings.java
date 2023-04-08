/*
 * Copyright (C) 2016-2022 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.sound;

import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.zeph.support.colorpicker.ColorPickerPreference;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnMainSwitchChangeListener {

    private static final String TAG = PulseSettings.class.getSimpleName();

    private static final String PULSE_ENABLED_KEY = "pulse_enabled";
    private static final String LOCKSCREEN_PULSE_ENABLED_KEY = "lockscreen_pulse_enabled";
    private static final String AMBIENT_PULSE_ENABLED_KEY = "ambient_pulse_enabled";
    private static final String PULSE_SMOOTHING_KEY = "pulse_smoothing_enabled";
    private static final String PULSE_COLOR_MODE_KEY = "pulse_color_mode";
    private static final String PULSE_COLOR_MODE_CHOOSER_KEY = "pulse_color_user";
    private static final String PULSE_COLOR_MODE_LAVA_SPEED_KEY = "pulse_lavalamp_speed";
    private static final String PULSE_RENDER_CATEGORY_SOLID = "pulse_2";
    private static final String PULSE_RENDER_CATEGORY_FADING = "pulse_fading_bars_category";
    private static final String PULSE_RENDER_MODE_KEY = "pulse_render_style";
    private static final String PULSE_CUSTOM_GRAVITY_KEY = "pulse_custom_gravity";
    private static final String PULSE_VISUALIZER_MIRRORED_KEY = "visualizer_center_mirrored";
    private static final String PULSE_VERTICAL_MIRROR_KEY = "pulse_vertical_mirror";
    private static final int RENDER_STYLE_FADING_BARS = 0;
    private static final int RENDER_STYLE_SOLID_LINES = 1;
    private static final int COLOR_TYPE_ACCENT = 0;
    private static final int COLOR_TYPE_USER = 1;
    private static final int COLOR_TYPE_LAVALAMP = 2;
    private static final int COLOR_TYPE_AUTO = 3;

    private static final String PULSE_SETTINGS_FOOTER = "pulse_settings_footer";

    private MainSwitchPreference mSwitchBar;
    private SwitchPreference mLockscreenPulse;
    private SwitchPreference mAmbientPulse;
    private SwitchPreference mPulseSmoothing;
    private Preference mRenderMode;
    private ListPreference mColorModePref;
    private ColorPickerPreference mColorPickerPref;
    private Preference mLavaSpeedPref;
    private Preference mFooterPref;
    private ListPreference mGravity;
    private SwitchPreference mVisualizerMirrored;
    private SwitchPreference mVerticalMirror;

    private PreferenceCategory mFadingBarsCat;
    private PreferenceCategory mSolidBarsCat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pulse_settings);

        ContentResolver resolver = getContext().getContentResolver();

        mLockscreenPulse = (SwitchPreference) findPreference(LOCKSCREEN_PULSE_ENABLED_KEY);
        boolean lockscreenPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mLockscreenPulse.setChecked(lockscreenPulse);
        mLockscreenPulse.setOnPreferenceChangeListener(this);

        mAmbientPulse = (SwitchPreference) findPreference(AMBIENT_PULSE_ENABLED_KEY);
        boolean ambientPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mAmbientPulse.setChecked(ambientPulse);
        mAmbientPulse.setOnPreferenceChangeListener(this); 

        mColorModePref = (ListPreference) findPreference(PULSE_COLOR_MODE_KEY);
        mColorPickerPref = (ColorPickerPreference) findPreference(PULSE_COLOR_MODE_CHOOSER_KEY);
        mLavaSpeedPref = findPreference(PULSE_COLOR_MODE_LAVA_SPEED_KEY);
        mColorModePref.setOnPreferenceChangeListener(this);

        mRenderMode = findPreference(PULSE_RENDER_MODE_KEY);
        mRenderMode.setOnPreferenceChangeListener(this);

        mFadingBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_FADING);
        mSolidBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_SOLID);

        mGravity = (ListPreference) findPreference(PULSE_CUSTOM_GRAVITY_KEY);
        mVisualizerMirrored = (SwitchPreference) findPreference(PULSE_VISUALIZER_MIRRORED_KEY);
        mVerticalMirror = (SwitchPreference) findPreference(PULSE_VERTICAL_MIRROR_KEY);

        mPulseSmoothing = (SwitchPreference) findPreference(PULSE_SMOOTHING_KEY);

        mFooterPref = findPreference(PULSE_SETTINGS_FOOTER);
        mFooterPref.setTitle(R.string.pulse_help_policy_notice_summary);

        mSwitchBar = (MainSwitchPreference) findPreference(PULSE_ENABLED_KEY);
        int enabled = Settings.Secure.getInt(resolver, PULSE_ENABLED_KEY, 0);
        mSwitchBar.setChecked(enabled != 0);
        mSwitchBar.addOnSwitchChangeListener(this);
        updateAllPrefs();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        ContentResolver resolver = getActivity().getContentResolver();
        Settings.Secure.putInt(resolver, PULSE_ENABLED_KEY, isChecked ? 1 : 0);
        mSwitchBar.setChecked(isChecked);
        updateAllPrefs();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();
        if (preference == mLockscreenPulse) {
            boolean val = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mAmbientPulse) {
            boolean val = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver,
                Settings.Secure.AMBIENT_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mColorModePref) {
            updateColorPrefs(Integer.valueOf(String.valueOf(newValue)));
            return true;
        } else if (preference == mRenderMode) {
            updateRenderCategories(Integer.valueOf(String.valueOf(newValue)));
            return true;
        }
        return false;
    }

    private void updateAllPrefs() {
        ContentResolver resolver = getContext().getContentResolver();
        boolean enabledPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        mGravity.setEnabled(enabledPulse);
        mVisualizerMirrored.setEnabled(enabledPulse);
        mVerticalMirror.setEnabled(enabledPulse);

        mPulseSmoothing.setEnabled(enabledPulse);

        mColorModePref.setEnabled(enabledPulse);
        if (enabledPulse) {
            int colorMode = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_MODE, COLOR_TYPE_LAVALAMP, UserHandle.USER_CURRENT);
            updateColorPrefs(colorMode);
        } else {
            mColorPickerPref.setEnabled(false);
            mLavaSpeedPref.setEnabled(false);
        }

        mRenderMode.setEnabled(enabledPulse);
        if (enabledPulse) {
            int renderMode = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_RENDER_STYLE, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT);
            updateRenderCategories(renderMode);
        } else {
            mFadingBarsCat.setEnabled(false);
            mSolidBarsCat.setEnabled(false);
        }

        mFooterPref.setEnabled(enabledPulse);
    }

    private void updateColorPrefs(int val) {
        switch (val) {
            case COLOR_TYPE_ACCENT:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(false);
                break;
            case COLOR_TYPE_USER:
                mColorPickerPref.setEnabled(true);
                mLavaSpeedPref.setEnabled(false);
                break;
            case COLOR_TYPE_LAVALAMP:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(true);
                break;
            case COLOR_TYPE_AUTO:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(false);
                break;
        }
    }

    private void updateRenderCategories(int mode) {
        ContentResolver resolver = getContext().getContentResolver();
        boolean enabledPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        
        mFadingBarsCat.setEnabled(mode == RENDER_STYLE_FADING_BARS && enabledPulse);
        mSolidBarsCat.setEnabled(mode == RENDER_STYLE_SOLID_LINES && enabledPulse);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_PULSE_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_RENDER_STYLE, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_SMOOTHING_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_MODE, COLOR_TYPE_LAVALAMP, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_USER, 0x92FFFFFF, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_LAVALAMP_SPEED, 10000, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_CUSTOM_DIMEN, 14, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_CUSTOM_DIV, 16, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_FILLED_BLOCK_SIZE, 4, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, 1, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, 4, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_SOLID_UNITS_OPACITY, 200, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_SOLID_UNITS_COUNT, 32, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_SOLID_FUDGE_FACTOR, 4, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_SOLID_UNITS_ROUNDED, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }
}
