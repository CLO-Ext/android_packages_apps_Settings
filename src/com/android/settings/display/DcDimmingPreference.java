/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static android.hardware.display.DcDimmingManager.MODE_AUTO_OFF;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DcDimmingManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;

public class DcDimmingPreference extends PrimarySwitchPreference
        implements Preference.OnPreferenceChangeListener {

    private final DcDimmingManager mDcDimmingManager;
    private SettingsObserver mSettingsObserver;

    public DcDimmingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDcDimmingManager = (DcDimmingManager) context.getSystemService(Context.DC_DIM_SERVICE);

        if (mDcDimmingManager != null && mDcDimmingManager.isAvailable()) {
            mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
            mSettingsObserver.observe();
        }

        setOnPreferenceChangeListener(this);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mDcDimmingManager == null || !mDcDimmingManager.isAvailable()) {
            setVisible(false);
        } else {
            setVisible(true);
            setChecked(mDcDimmingManager.isDcDimmingOn());
            updateSummary(mDcDimmingManager.isDcDimmingOn());
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mSettingsObserver != null) {
            mSettingsObserver.unobserve();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean checked = (Boolean) newValue;
        setChecked(checked);
        return true;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mDcDimmingManager != null && isChecked() != checked) {
            super.setChecked(checked);
            mDcDimmingManager.setDcDimming(checked);
            updateSummary(checked);
        }
    }

    private void updateSummary(boolean active) {
        if (mDcDimmingManager == null) {
            return;
        }

        String summary;
        if (mDcDimmingManager.getAutoMode() != MODE_AUTO_OFF) {
            summary = getContext().getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_auto
                    : R.string.dark_ui_summary_off_auto_mode_auto);
        } else {
            summary = getContext().getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_never
                    : R.string.dark_ui_summary_off_auto_mode_never);
        }
        setSummary(summary);
    }

    private class SettingsObserver extends ContentObserver {
        private final ContentResolver mResolver;

        SettingsObserver(Handler handler) {
            super(handler);
            mResolver = getContext().getContentResolver();
        }

        void observe() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.DC_DIMMING_AUTO_MODE), false, this,
                    UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.DC_DIMMING_STATE), false, this,
                    UserHandle.USER_ALL);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mDcDimmingManager != null) {
                setChecked(mDcDimmingManager.isDcDimmingOn());
            }
        }
    }
}
