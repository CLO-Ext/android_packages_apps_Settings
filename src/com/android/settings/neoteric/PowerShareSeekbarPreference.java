/*
 * Copyright (C) 2025 Neoteric OS
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

package com.android.settings.neoteric;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class PowerShareSeekbarPreference extends Preference
        implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = PowerShareSeekbarPreference.class.getSimpleName();

    private final Context mContext;
    private final int mDefThresholdValue;

    private TextView mPowerShareLimitValue;
    private SeekBar mPowerShareLimitBar;

    public PowerShareSeekbarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.preference_battery_share_limit);

        mContext = context;
        mDefThresholdValue = mContext.getResources().
                getInteger(com.android.internal.R.integer.config_defPowerShareThreshold);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mPowerShareLimitValue = (TextView) holder.findViewById(R.id.value);

        mPowerShareLimitBar = (SeekBar) holder.findViewById(R.id.seekbar_widget);
        mPowerShareLimitBar.setOnSeekBarChangeListener(this);

        int currLimit = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.POWER_SHARE_THRESHOLD, mDefThresholdValue,
                UserHandle.USER_CURRENT);

        // Getting the value for the first time.
        if (currLimit < mPowerShareLimitBar.getMin()) {
            currLimit = mPowerShareLimitBar.getProgress();
        }

        mPowerShareLimitBar.setProgress(currLimit);
        updateValue(currLimit);
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress,
            final boolean fromUser) {
        updateValue(progress);
    }

    private void updateValue(final int value) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.POWER_SHARE_THRESHOLD, value, UserHandle.USER_CURRENT);
        if (mPowerShareLimitValue != null) {
            mPowerShareLimitValue.setText(String.format("%d%%", value));
        }
    }
}
