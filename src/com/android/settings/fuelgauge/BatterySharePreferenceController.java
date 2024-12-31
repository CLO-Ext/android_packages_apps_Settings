/*
 * SPDX-FileCopyrightText: 2024 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import android.content.Context;
import android.os.BatteryManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.neoteric.preference.SystemSettingSeekBarPreference;

import vendor.lineage.powershare.IPowerShare;

public class BatterySharePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String POWERSHARE_SERVICE_NAME = "vendor.lineage.powershare.IPowerShare/default";
    private static final String BATTERY_SHARE_THRESHOLD_KEY = "battery_share_threshold";
    private static final String BATTERY_SHARE_KEY = "battery_share";

    private IPowerShare mPowerShare;

    public BatterySharePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPowerShare = getPowerShare();
    }

    private synchronized IPowerShare getPowerShare() {
        return IPowerShare.Stub.asInterface(ServiceManager.getService(POWERSHARE_SERVICE_NAME));
    }

    @Override
    public int getAvailabilityStatus() {
        return mPowerShare != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference.getKey().equals("battery_share")) {
            if (mPowerShare == null) {
                preference.setSummary(R.string.battery_share_unavailable);
                return;
            }

            try {
                boolean isEnabled = mPowerShare.isEnabled();

                // Check if BatteryShare should turn off
                if (isEnabled && shouldTurnOffBatteryShare()) {
                    mPowerShare.setEnabled(false);

                    // Debug: Log the disabling action
                    Log.d("BatteryShare", "BatteryShare disabled due to low battery.");
                }

                if (preference instanceof SwitchPreferenceCompat) {
                    ((SwitchPreferenceCompat) preference).setChecked(isEnabled);
                }

                String state = isEnabled
                        ? mContext.getString(R.string.battery_share_state_on)
                        : mContext.getString(R.string.battery_share_state_off);
                preference.setSummary(state);
            } catch (RemoteException e) {
                e.printStackTrace();
                preference.setSummary(R.string.battery_share_error);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof SystemSettingSeekBarPreference &&
            preference.getKey().equals("battery_share_threshold")) {
            int newThreshold = (Integer) newValue;
            saveThresholdToPreferences(newThreshold);

            // Ensure minimum value is respected
            if (newThreshold < 10) {
                Log.d("BatteryShare", "Threshold below minimum, resetting to 10.");
                newThreshold = 10;
            }

            saveThresholdToPreferences(newThreshold);
            Log.d("BatteryShare", "Threshold updated to: " + newThreshold);
            return true;
        }

        if (preference.getKey().equals("battery_share")) {
            if (mPowerShare == null) return false;

            try {
                boolean enabled = (Boolean) newValue;
                mPowerShare.setEnabled(enabled);
                ((SwitchPreferenceCompat) preference).setChecked(enabled);

                // Debug: Log the new state
                Log.d("BatteryShare", "BatteryShare state updated to: " + enabled);
                return true;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    private void saveThresholdToPreferences(int threshold) {
        mContext.getSharedPreferences("battery_share_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("battery_share_threshold", threshold)
            .apply();
    }

    private int getThresholdFromPreferences() {
        return mContext.getSharedPreferences("battery_share_prefs", Context.MODE_PRIVATE)
                    .getInt("battery_share_threshold", 20); // Default to 20%
    }

    private boolean shouldTurnOffBatteryShare() {
        int currentBatteryLevel = getCurrentBatteryLevel();
        int threshold = getThresholdFromPreferences();

        // Debug: Log the comparison
        Log.d("BatteryShare", "Checking if BatteryShare should turn off: current=" + currentBatteryLevel + ", threshold=" + threshold);

        return currentBatteryLevel <= threshold;
    }

    private int getCurrentBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        // Debug: Log the current battery level
        Log.d("BatteryShare", "Current battery level: " + batteryLevel);

        return batteryLevel;
    }

    private final BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);

            // Debug: Log the received battery level
            Log.d("BatteryShare", "Battery level updated: " + batteryPct);

            if (shouldTurnOffBatteryShare()) {
                try {
                    if (mPowerShare != null && mPowerShare.isEnabled()) {
                        mPowerShare.setEnabled(false);

                        // Debug: Log the action
                        Log.d("BatteryShare", "BatteryShare disabled via BroadcastReceiver.");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void registerBatteryReceiver() {
        Log.d("BatteryShareDebug", "Registering BroadcastReceiver.");
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(batteryLevelReceiver, filter);
    }

    public void unregisterBatteryReceiver() {
        Log.d("BatteryShareDebug", "Unregistering BroadcastReceiver.");
        mContext.unregisterReceiver(batteryLevelReceiver);
    }
}
