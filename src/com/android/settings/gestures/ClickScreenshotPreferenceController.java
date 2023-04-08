/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.System.CLICK_PARTIAL_SCREENSHOT;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;    
import com.android.settings.preference.SystemSettingMainSwitchPreference;

public class ClickScreenshotPreferenceController extends GesturePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final int ON = 1;
    private final int OFF = 0;

    private SystemSettingMainSwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    private static final String PREF_KEY_VIDEO = "gesture_screen_off_udfps_video";

    public ClickScreenshotPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SystemSettingMainSwitchPreference) screen.findPreference(CLICK_PARTIAL_SCREENSHOT);
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "click_partial_screenshot");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), CLICK_PARTIAL_SCREENSHOT,
                isChecked ? ON : OFF);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(), CLICK_PARTIAL_SCREENSHOT, 0) != 0;
    }
    
    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri mUri = Settings.System.getUriFor(CLICK_PARTIAL_SCREENSHOT);

        private final SystemSettingMainSwitchPreference mPreference;

        SettingObserver(SystemSettingMainSwitchPreference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || mUri.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
