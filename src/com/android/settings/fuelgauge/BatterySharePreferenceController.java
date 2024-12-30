/*
 * SPDX-FileCopyrightText: 2024 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import vendor.lineage.powershare.IPowerShare;

public class BatterySharePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String POWERSHARE_SERVICE_NAME = "vendor.lineage.powershare.IPowerShare/default";

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
        if (mPowerShare == null) {
            preference.setSummary(R.string.battery_share_unavailable);
            return;
        }

        try {
            boolean isEnabled = mPowerShare.isEnabled();

            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(isEnabled);
            }

            String state = isEnabled
                    ? mContext.getString(R.string.battery_share_state_on)
                    : mContext.getString(R.string.battery_share_state_off);

            if (preference.getKey().equals("battery_share_summary")) {
                preference.setSummary(state);
            } else {
                preference.setSummary(R.string.battery_share_summary);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
            preference.setSummary(R.string.battery_share_error);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mPowerShare == null) return false;

        try {
            boolean enabled = (Boolean) newValue;
            mPowerShare.setEnabled(enabled);

            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(enabled);
            }

            String state = enabled
                    ? mContext.getString(R.string.battery_share_state_on)
                    : mContext.getString(R.string.battery_share_state_off);

            if (preference.getKey().equals("battery_share_summary")) {
                preference.setSummary(state);
            }

            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}
