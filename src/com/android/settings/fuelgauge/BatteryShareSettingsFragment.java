/*
 * SPDX-FileCopyrightText: 2024 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

public class BatteryShareSettingsFragment extends DashboardFragment {

    private static final String TAG = "BatteryShareSettingsFragment";

    private BatterySharePreferenceController mBatteryShareController;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Initialize the controller
        mBatteryShareController = new BatterySharePreferenceController(context, "battery_share");
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_share;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Register BroadcastReceiver
        if (mBatteryShareController != null) {
            mBatteryShareController.registerBatteryReceiver();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister BroadcastReceiver
        if (mBatteryShareController != null) {
            mBatteryShareController.unregisterBatteryReceiver();
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.battery_share);
}
