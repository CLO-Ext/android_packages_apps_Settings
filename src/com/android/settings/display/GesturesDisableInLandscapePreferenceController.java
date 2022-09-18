/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.android.settings.core.BasePreferenceController;

import org.zeph.support.preference.SystemSettingSwitchPreference;

public class GesturesDisableInLandscapePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private SettingObserver mSettingObserver;
    private SystemSettingSwitchPreference mPreference;

    public GesturesDisableInLandscapePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
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

    private void updateEnabledState() {
        final boolean brightnessControlEnabled = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0,
                UserHandle.USER_CURRENT) == 1;
        mPreference.setEnabled(brightnessControlEnabled);
    }

    private class SettingObserver extends ContentObserver {

        private final Uri mBrightnessControlUri = Settings.System.getUriFor(
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL);

        SettingObserver() {
            super(new Handler());
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mBrightnessControlUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateEnabledState();
        }
    }
}
