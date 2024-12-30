/*
 * SPDX-FileCopyrightText: 2024 Neoteric OS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.fuelgauge;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

public class BatteryShareSettingsFragment extends DashboardFragment {

    private static final String TAG = "BatteryShareSettingsFragment";

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

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.battery_share);
}
