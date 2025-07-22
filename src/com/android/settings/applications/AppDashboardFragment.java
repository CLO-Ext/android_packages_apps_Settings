/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.provider.SearchIndexableResource;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.appcompat.UserAspectRatioAppsPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Settings page for apps. */
@SearchIndexable
public class AppDashboardFragment extends DashboardFragment {

    private static final String TAG = "AppDashboardFragment";
    private static final String ADVANCED_CATEGORY_KEY = "advanced_category";
    private static final String ASPECT_RATIO_PREF_KEY = "aspect_ratio_apps";
    private static final String KEYBOX_DATA_KEY = "keybox_data_setting";
    private static final String PIF_DATA_KEY = "pif_data_setting";
    private ActivityResultLauncher<Intent> mKeyboxFilePickerLauncher;
    private ActivityResultLauncher<Intent> mPifFilePickerLauncher;
    private KeyboxDataPreference mKeyboxDataPreference;
    private PifDataPreference mPifDataPreference;
    private AppsPreferenceController mAppsPreferenceController;

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AppsPreferenceController(context));

        final UserAspectRatioAppsPreferenceController aspectRatioAppsPreferenceController =
                new UserAspectRatioAppsPreferenceController(context, ASPECT_RATIO_PREF_KEY);
        final AdvancedAppsPreferenceCategoryController advancedCategoryController =
                new AdvancedAppsPreferenceCategoryController(context, ADVANCED_CATEGORY_KEY);
        advancedCategoryController.setChildren(List.of(aspectRatioAppsPreferenceController));
        controllers.add(advancedCategoryController);

        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_APPLICATIONS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_apps_and_notifications;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.apps;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAppsPreferenceController = use(AppsPreferenceController.class);
        mAppsPreferenceController.setFragment(this /* fragment */);
        getSettingsLifecycle().addObserver(mAppsPreferenceController);

        final HibernatedAppsPreferenceController hibernatedAppsPreferenceController =
                use(HibernatedAppsPreferenceController.class);
        getSettingsLifecycle().addObserver(hibernatedAppsPreferenceController);

        mKeyboxFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Preference pref = findPreference(KEYBOX_DATA_KEY);
                    if (pref instanceof KeyboxDataPreference) {
                        ((KeyboxDataPreference) pref).handleFileSelected(uri);
                    }
                }
            }
        );

        mPifFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Preference pref = findPreference(PIF_DATA_KEY);
                    if (pref instanceof PifDataPreference) {
                        ((PifDataPreference) pref).handleFileSelected(uri);
                    }
                }
            }
        );
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mKeyboxDataPreference = findPreference(KEYBOX_DATA_KEY);
        mPifDataPreference = findPreference(PIF_DATA_KEY);

        if (mKeyboxDataPreference != null) {
            mKeyboxDataPreference.setFilePickerLauncher(mKeyboxFilePickerLauncher);
        }

        if (mPifDataPreference != null) {
            mPifDataPreference.setFilePickerLauncher(mPifFilePickerLauncher);
        }
    }

    @VisibleForTesting
    PreferenceCategoryController getAdvancedAppsPreferenceCategoryController() {
        return use(AdvancedAppsPreferenceCategoryController.class);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.apps;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
