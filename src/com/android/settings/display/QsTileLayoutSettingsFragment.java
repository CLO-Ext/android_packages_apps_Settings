/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.nameless.qs.TileUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.widget.LayoutPreference;

import org.zeph.support.preference.CustomSeekBarPreference;

public class QsTileLayoutSettingsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_QS_COLUMN_PORTRAIT = "qs_layout_columns";
    private static final String KEY_QS_ROW_PORTRAIT = "qs_layout_rows";
    private static final String KEY_QQS_ROW_PORTRAIT = "qqs_layout_rows";
    private static final String KEY_APPLY_CHANGE_BUTTON = "apply_change_button";

    private Context mContext;

    private Button mApplyChange;

    private CustomSeekBarPreference mQsColumns;
    private CustomSeekBarPreference mQsRows;
    private CustomSeekBarPreference mQqsRows;

    private int[] currentValue = new int[2];

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.qs_tile_layout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQsColumns = (CustomSeekBarPreference) findPreference(KEY_QS_COLUMN_PORTRAIT);
        mQsColumns.setOnPreferenceChangeListener(this);

        mQsRows = (CustomSeekBarPreference) findPreference(KEY_QS_ROW_PORTRAIT);
        mQsRows.setOnPreferenceChangeListener(this);

        mQqsRows = (CustomSeekBarPreference) findPreference(KEY_QQS_ROW_PORTRAIT);
        mQqsRows.setOnPreferenceChangeListener(this);

        mContext = getContext();

        LayoutPreference preference = findPreference(KEY_APPLY_CHANGE_BUTTON);
        mApplyChange = (Button) preference.findViewById(R.id.apply_change);
        mApplyChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApplyChange.isEnabled()) {
                    final int[] newValue = {
                        mQsRows.getValue() * 10 + mQsColumns.getValue(),
                        mQqsRows.getValue() * 10 + mQsColumns.getValue()
                    };
                    Settings.System.putIntForUser(getContentResolver(),
                            Settings.System.QS_LAYOUT, newValue[0], UserHandle.USER_CURRENT);
                    Settings.System.putIntForUser(getContentResolver(),
                            Settings.System.QQS_LAYOUT, newValue[1], UserHandle.USER_CURRENT);
                    if (TileUtils.updateLayout(mContext)) {
                        currentValue[0] = newValue[0];
                        currentValue[1] = newValue[1];
                        mApplyChange.setEnabled(false);
                    } else {
                        Settings.System.putIntForUser(getContentResolver(),
                                Settings.System.QS_LAYOUT, currentValue[0], UserHandle.USER_CURRENT);
                        Settings.System.putIntForUser(getContentResolver(),
                                Settings.System.QQS_LAYOUT, currentValue[1], UserHandle.USER_CURRENT);
                        Toast.makeText(mContext, R.string.qs_apply_change_failed, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        initPreference();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsColumns) {
            int qs_columns = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + qs_columns ||
                currentValue[1] != mQqsRows.getValue() * 10 + qs_columns
            );
        } else if (preference == mQsRows) {
            int qs_rows = Integer.parseInt(newValue.toString());
            mQqsRows.setMax(qs_rows - 1);
            if (mQqsRows.getValue() > qs_rows - 1) {
                mQqsRows.setValue(qs_rows - 1);
            }
            mApplyChange.setEnabled(
                currentValue[0] != qs_rows * 10 + mQsColumns.getValue() ||
                currentValue[1] != mQqsRows.getValue() * 10 + mQsColumns.getValue()
            );
        } else if (preference == mQqsRows) {
            int qqs_rows = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + mQsColumns.getValue() ||
                currentValue[1] != qqs_rows * 10 + mQsColumns.getValue()
            );
        }
        return true;
    }

    private void initPreference() {
        final int index_qs = Settings.System.getIntForUser(getContentResolver(),
            Settings.System.QS_LAYOUT, 42, UserHandle.USER_CURRENT);
        final int index_qqs = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QQS_LAYOUT, 22, UserHandle.USER_CURRENT);
        mQsColumns.setValue(index_qs % 10);
        mQsRows.setValue(index_qs / 10);
        mQqsRows.setValue(index_qqs / 10);
        mQqsRows.setMax(mQsRows.getValue() - 1);
        currentValue[0] = index_qs;
        currentValue[1] = index_qqs;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }
}
