/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;    
import com.android.settings.preference.SecureSettingMainSwitchPreference;

public class ScreenOffUdfpsPreferenceController extends GesturePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
            
    private final int ON = 1;
    private final int OFF = 0;

    private SecureSettingMainSwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    private static final String PREF_KEY_VIDEO = "gesture_screen_off_udfps_video";

    private static final String SECURE_KEY = "screen_off_udfps_enabled";

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public ScreenOffUdfpsPreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SecureSettingMainSwitchPreference) screen.findPreference(SECURE_KEY);
        mSettingObserver = new SettingObserver(mPreference);
    }

    public ScreenOffUdfpsPreferenceController setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    private static boolean screenOffUdfpsAvailable(AmbientDisplayConfiguration config) {
        return !TextUtils.isEmpty(config.udfpsLongPressSensorType());
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return isSuggestionComplete(new AmbientDisplayConfiguration(context), prefs);
    }

    @VisibleForTesting
    static boolean isSuggestionComplete(AmbientDisplayConfiguration config,
            SharedPreferences prefs) {
        return !screenOffUdfpsAvailable(config)
                || prefs.getBoolean(ScreenOffUdfpsSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for Screen-Off UDFPS
        if (!screenOffUdfpsAvailable(mAmbientConfig)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_screen_off_udfps");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().screenOffUdfpsEnabled(mUserId);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mAmbientConfig;
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
        private final Uri mUri = Settings.Secure.getUriFor(SECURE_KEY);

        private final SecureSettingMainSwitchPreference mPreference;

        SettingObserver(SecureSettingMainSwitchPreference preference) {
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
