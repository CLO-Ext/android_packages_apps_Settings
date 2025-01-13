/*
 * SPDX-FileCopyrightText: 2025 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.hardware.PowerShareManager;
import android.os.PowerManager;
import android.os.ServiceManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.android.settingslib.widget.MainSwitchPreference;

public class BatterySharePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String BATTERY_SHARE_KEY = "battery_share";
    private static final String BATTERY_SHARE_SUMMARY = "battery_share_summary";

    private final PowerManager mPowerManager;
    private final PowerShareManager mPowerShareManager;

    public BatterySharePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPowerManager = context.getSystemService(PowerManager.class);
        mPowerShareManager = (PowerShareManager) context.getSystemService(Context.POWER_SHARE_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return mPowerShareManager != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = mPowerShareManager.isEnabled();
        boolean isPowerSaveMode = mPowerManager.isPowerSaveMode();
        if (isPowerSaveMode) {
            isEnabled = false;
        }
        if (preference.getKey().equals(BATTERY_SHARE_KEY)) {
            ((MainSwitchPreference) preference).setChecked(isEnabled);
        } else if (preference.getKey().equals(BATTERY_SHARE_SUMMARY)) {
            if (isPowerSaveMode) {
                preference.setSummary(mContext.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_on));
            } else {
                String state = isEnabled
                        ? mContext.getString(R.string.battery_share_state_on)
                        : mContext.getString(R.string.battery_share_state_off);
                preference.setSummary(state);
            }
        }
        preference.setEnabled(!isPowerSaveMode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        if (preference.getKey().equals(BATTERY_SHARE_KEY)) {
            mPowerShareManager.setEnabled(enabled);
            ((MainSwitchPreference) preference).setChecked(enabled);
            return true;
        } else if (preference.getKey().equals(BATTERY_SHARE_SUMMARY)) {
            String state = enabled
                    ? mContext.getString(R.string.battery_share_state_on)
                    : mContext.getString(R.string.battery_share_state_off);
            preference.setSummary(state);
            return true;
        }
        return false;
    }
}
