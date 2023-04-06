/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCKED_NOTIFICATION_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settings.display.AmbientDisplayNotificationsPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.gestures.ScreenOffUdfpsPreferenceController;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.screenlock.LockScreenPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import org.zeph.support.preference.SwitchPreference;
import org.zeph.support.preference.SystemSettingListPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for lock screen preference
 */
@SearchIndexable
public class LockscreenDashboardFragment extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback, OnPreferenceChangeListener {    

    public static final String KEY_AMBIENT_DISPLAY_ALWAYS_ON = "ambient_display_always_on";

    private static final String TAG = "LockscreenDashboardFragment";

    private static final String SHORTCUT_START_KEY = "lockscreen_shortcut_start";
    private static final String SHORTCUT_END_KEY = "lockscreen_shortcut_end";
    private static final String SHORTCUT_ENFORCE_KEY = "lockscreen_shortcut_enforce";

    private static final String[] DEFAULT_START_SHORTCUT = new String[] { "home", "flashlight" };
    private static final String[] DEFAULT_END_SHORTCUT = new String[] { "wallet", "qr", "camera" };
    private static final String PREF_AMBIENT = "ambient_notification_options";

    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON = "security_setting_lock_screen_notif";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER =
            "security_setting_lock_screen_notif_work_header";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE =
            "security_setting_lock_screen_notif_work";
    @VisibleForTesting
    static final String KEY_ADD_USER_FROM_LOCK_SCREEN =
            "security_lockscreen_add_users_when_locked";


    private AmbientDisplayConfiguration mConfig;
    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    private SystemSettingListPreference mStartShortcut;
    private SystemSettingListPreference mEndShortcut;
    private SwitchPreference mEnforceShortcut;

    private ListPreference mAmbientTimeOut;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_LOCK_SCREEN_PREFERENCES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("security_setting_lock_screen_notif_work",
                WORK_PROFILE_LOCKED_NOTIFICATION_TITLE,
                R.string.locked_work_profile_notification_title);
        replaceEnterpriseStringTitle("security_setting_lock_screen_notif_work_header",
                WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER, R.string.profile_section_header);

        mStartShortcut = findPreference(SHORTCUT_START_KEY);
        mEndShortcut = findPreference(SHORTCUT_END_KEY);
        mEnforceShortcut = findPreference(SHORTCUT_ENFORCE_KEY);
        updateShortcutSelection();
        mStartShortcut.setOnPreferenceChangeListener(this);
        mEndShortcut.setOnPreferenceChangeListener(this);
        mEnforceShortcut.setOnPreferenceChangeListener(this);

        if (!getActivity().getResources().getBoolean(com.android.internal.R.bool.config_pulseOnNotificationsAvailable)) {
            getPreferenceScreen().removePreference(findPreference(PREF_AMBIENT));
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_lockscreen_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_lockscreen;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AmbientDisplayAlwaysOnPreferenceController.class).setConfig(getConfig(context));
        use(AmbientDisplayNotificationsPreferenceController.class).setConfig(getConfig(context));
        use(DoubleTapScreenPreferenceController.class).setConfig(getConfig(context));
        use(PickupGesturePreferenceController.class).setConfig(getConfig(context));
        use(ScreenOffUdfpsPreferenceController.class).setConfig(getConfig(context));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateShortcutSelection();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mStartShortcut) {
            setShortcutSelection((String) objValue, true);
            return true;
        } else if (preference == mEndShortcut) {
            setShortcutSelection((String) objValue, false);
            return true;
        } else if (preference == mEnforceShortcut) {
            final boolean value = (Boolean) objValue;
            setShortcutSelection(mStartShortcut.getValue(), true, value);
            setShortcutSelection(mEndShortcut.getValue(), false, value);
            return true;
        }
        return false;
    }

    private String getSettingsShortcutValue() {
        String value = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.KEYGUARD_QUICK_TOGGLES);
        if (value == null || value.isEmpty()) {
            StringBuilder sb = new StringBuilder(DEFAULT_START_SHORTCUT[0]);
            for (int i = 1; i < DEFAULT_START_SHORTCUT.length; i++) {
                sb.append(",").append(DEFAULT_START_SHORTCUT[i]);
            }
            sb.append(";" + DEFAULT_END_SHORTCUT[0]);
            for (int i = 1; i < DEFAULT_END_SHORTCUT.length; i++) {
                sb.append(",").append(DEFAULT_END_SHORTCUT[i]);
            }
            value = sb.toString();
        }
        return value;
    }

    private void updateShortcutSelection() {
        final String value = getSettingsShortcutValue();
        final String[] split = value.split(";");
        final String[] start = split[0].split(",");
        final String[] end = split[1].split(",");
        mStartShortcut.setValue(start[0]);
        mStartShortcut.setSummary(mStartShortcut.getEntry());
        mEndShortcut.setValue(end[0]);
        mEndShortcut.setSummary(mEndShortcut.getEntry());
        mEnforceShortcut.setChecked(start.length == 1 && end.length == 1);
    }

    private void setShortcutSelection(String value, boolean start) {
        setShortcutSelection(value, start, mEnforceShortcut.isChecked());
    }

    private void setShortcutSelection(String value, boolean start, boolean single) {
        final String oldValue = getSettingsShortcutValue();
        final int splitIndex = start ? 0 : 1;
        String[] split = oldValue.split(";");
        if (value.equals("none") || single) {
            split[splitIndex] = value;
        } else {
            StringBuilder sb = new StringBuilder(value);
            final String[] def = start ? DEFAULT_START_SHORTCUT : DEFAULT_END_SHORTCUT;
            for (String str : def) {
                if (str.equals(value)) continue;
                sb.append(",").append(str);
            }
            split[splitIndex] = sb.toString();
        }
        Settings.System.putString(getActivity().getContentResolver(),
                Settings.System.KEYGUARD_QUICK_TOGGLES, split[0] + ";" + split[1]);

        if (start) {
            mStartShortcut.setValue(value);
            mStartShortcut.setSummary(mStartShortcut.getEntry());
        } else {
            mEndShortcut.setValue(value);
            mEndShortcut.setSummary(mEndShortcut.getEntry());
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getSettingsLifecycle();
        final LockScreenNotificationPreferenceController notificationController =
                new LockScreenNotificationPreferenceController(context,
                        KEY_LOCK_SCREEN_NOTIFICATON,
                        KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER,
                        KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE);
        lifecycle.addObserver(notificationController);
        controllers.add(notificationController);
        mOwnerInfoPreferenceController = new OwnerInfoPreferenceController(context, this);
        controllers.add(mOwnerInfoPreferenceController);

        return controllers;
    }

    @Override
    public void onOwnerInfoUpdated() {
        if (mOwnerInfoPreferenceController != null) {
            mOwnerInfoPreferenceController.updateSummary();
        }
    }

    private AmbientDisplayConfiguration getConfig(Context context) {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(context);
        }
        return mConfig;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.security_lockscreen_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new LockScreenNotificationPreferenceController(context));
                    controllers.add(new OwnerInfoPreferenceController(
                            context, null /* fragment */));
                    return controllers;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> niks = super.getNonIndexableKeys(context);
                    niks.add(KEY_ADD_USER_FROM_LOCK_SCREEN);
                    return niks;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return new LockScreenPreferenceController(context, "anykey")
                            .isAvailable();
                }
            };
}
