/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display;

import static android.view.Display.INVALID_DISPLAY;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplayAllowed;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplaySizeSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isResolutionSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isRotationSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isTopologyPaneEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isUseDisplaySettingEnabled;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.accessibility.AccessibilitySeekBarPreference;
import com.android.settings.accessibility.TextReadingPreferenceFragment;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.Injector;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.TwoTargetPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Settings screen for External Displays configuration and connection management.
 */
public class ExternalDisplayPreferenceFragment extends SettingsPreferenceFragmentBase {
    @VisibleForTesting enum PrefBasics {
        DISPLAY_TOPOLOGY(10, "display_topology_preference", null),
        MIRROR(20, "mirror_preference", null),

        // If shown, use toggle should be before other per-display settings.
        EXTERNAL_DISPLAY_USE(30, "external_display_use_preference",
                R.string.external_display_use_title),

        ILLUSTRATION(35, "external_display_illustration", null),

        // If shown, external display size is before other per-display settings.
        EXTERNAL_DISPLAY_SIZE(40, "external_display_size", R.string.screen_zoom_title),
        EXTERNAL_DISPLAY_ROTATION(50, "external_display_rotation",
                R.string.external_display_rotation),
        EXTERNAL_DISPLAY_RESOLUTION(60, "external_display_resolution",
                R.string.external_display_resolution_settings_title),

        // Built-in display link is after per-display settings.
        BUILTIN_DISPLAY_LIST(70, "builtin_display_list_preference",
                R.string.builtin_display_settings_category),

        // If shown, footer should appear below everything.
        FOOTER(90, "footer_preference", null);


        PrefBasics(int order, String key, @Nullable Integer titleResource) {
            this.order = order;
            this.key = key;
            this.titleResource = titleResource;
        }

        // Fields must be public to make the linter happy.
        public final int order;
        public final String key;
        @Nullable public final Integer titleResource;

        void apply(Preference preference) {
            if (order != -1) {
                preference.setOrder(order);
            }
            if (titleResource != null) {
                preference.setTitle(titleResource);
            }
            preference.setKey(key);
            preference.setPersistent(false);
        }
    }

    static final int EXTERNAL_DISPLAY_SETTINGS_RESOURCE = R.xml.external_display_settings;
    static final int EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE =
            R.string.external_display_change_resolution_footer_title;
    static final int EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE =
            R.drawable.external_display_mirror_landscape;
    static final int EXTERNAL_DISPLAY_TITLE_RESOURCE =
            R.string.external_display_settings_title;
    static final int EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE =
            R.string.external_display_not_found_footer_title;
    static final int EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE =
            R.drawable.external_display_mirror_portrait;
    static final int EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE = R.string.screen_zoom_short_summary;

    @VisibleForTesting
    static final String PREVIOUSLY_SHOWN_LIST_KEY = "mPreviouslyShownListOfDisplays";
    private boolean mStarted;
    @Nullable
    private IllustrationPreference mImagePreference;
    @Nullable
    private Preference mDisplayTopologyPreference;
    @Nullable
    private Preference mMirrorPreference;
    @Nullable
    private PreferenceCategory mBuiltinDisplayPreference;
    @Nullable
    private Preference mBuiltinDisplaySizeAndTextPreference;
    @Nullable
    private Injector mInjector;
    @Nullable
    private String[] mRotationEntries;
    @Nullable
    private String[] mRotationEntriesValues;
    @NonNull
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener = new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };
    private boolean mPreviouslyShownListOfDisplays;

    public ExternalDisplayPreferenceFragment() {}

    @VisibleForTesting
    ExternalDisplayPreferenceFragment(@NonNull Injector injector) {
        mInjector = injector;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return EXTERNAL_DISPLAY_HELP_URL;
    }

    @Override
    public void onSaveInstanceStateCallback(@NonNull Bundle outState) {
        outState.putSerializable(PREVIOUSLY_SHOWN_LIST_KEY,
                mPreviouslyShownListOfDisplays);
    }

    @Override
    public void onCreateCallback(@Nullable Bundle icicle) {
        if (mInjector == null) {
            mInjector = new Injector(getPrefContext());
        }
        addPreferencesFromResource(EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    @Override
    public void onActivityCreatedCallback(@Nullable Bundle savedInstanceState) {
        restoreState(savedInstanceState);
        View view = getView();
        TextView emptyView = null;
        if (view != null) {
            emptyView = view.findViewById(android.R.id.empty);
        }
        if (emptyView != null) {
            emptyView.setText(EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE);
            setEmptyView(emptyView);
        }
    }

    @Override
    public void onStartCallback() {
        mStarted = true;
        if (mInjector == null) {
            return;
        }
        mInjector.registerDisplayListener(mListener);
        scheduleUpdate();
    }

    @Override
    public void onStopCallback() {
        mStarted = false;
        if (mInjector == null) {
            return;
        }
        mInjector.unregisterDisplayListener(mListener);
        unscheduleUpdate();
    }

    /**
     * @return id of the preference.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
    }

    @VisibleForTesting
    protected void launchResolutionSelector(@NonNull final Context context, final int displayId) {
        final Bundle args = new Bundle();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(context)
                .setDestination(ResolutionPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @VisibleForTesting
    protected void launchExternalDisplaySettings(final int displayId) {
        final Bundle args = new Bundle();
        var context = getPrefContext();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(context)
                .setDestination(this.getClass().getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @VisibleForTesting
    protected void launchBuiltinDisplaySettings() {
        final Bundle args = new Bundle();
        var context = getPrefContext();
        new SubSettingLauncher(context)
                .setDestination(TextReadingPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    // The real FooterPreference requires a resource which is not available in unit tests.
    @VisibleForTesting
    Preference newFooterPreference(Context context) {
        return new FooterPreference(context);
    }

    /**
     * Returns the preference for the footer.
     */
    private void addFooterPreference(Context context, PrefRefresh refresh, int title) {
        var pref = refresh.findUnusedPreference(PrefBasics.FOOTER.key);
        if (pref == null) {
            pref = newFooterPreference(context);
            PrefBasics.FOOTER.apply(pref);
        }
        pref.setTitle(title);
        refresh.addPreference(pref);
    }

    @NonNull
    private ListPreference reuseRotationPreference(@NonNull Context context, PrefRefresh refresh) {
        ListPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_ROTATION.key);
        if (pref == null) {
            pref = new ListPreference(context);
            PrefBasics.EXTERNAL_DISPLAY_ROTATION.apply(pref);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private Preference reuseResolutionPreference(@NonNull Context context, PrefRefresh refresh) {
        var pref = refresh.findUnusedPreference(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.key);
        if (pref == null) {
            pref = new Preference(context);
            PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.apply(pref);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private MainSwitchPreference reuseUseDisplayPreference(
            @NonNull Context context, @NonNull PrefRefresh refresh) {
        MainSwitchPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_USE.key);
        if (pref == null) {
            pref = new MainSwitchPreference(context);
            PrefBasics.EXTERNAL_DISPLAY_USE.apply(pref);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    @VisibleForTesting
    IllustrationPreference getIllustrationPreference(@NonNull Context context) {
        if (mImagePreference == null) {
            mImagePreference = new IllustrationPreference(context);
            PrefBasics.ILLUSTRATION.apply(mImagePreference);
        }
        return mImagePreference;
    }

    /**
     * @return return display id argument of this settings page.
     */
    @VisibleForTesting
    protected int getDisplayIdArg() {
        var args = getArguments();
        return args != null ? args.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) : INVALID_DISPLAY;
    }

    @NonNull
    private PreferenceCategory getBuiltinDisplayListPreference(@NonNull Context context) {
        if (mBuiltinDisplayPreference == null) {
            mBuiltinDisplayPreference = new PreferenceCategory(context);
            PrefBasics.BUILTIN_DISPLAY_LIST.apply(mBuiltinDisplayPreference);
        }
        return mBuiltinDisplayPreference;
    }

    @NonNull
    private Preference getBuiltinDisplaySizeAndTextPreference(@NonNull Context context) {
        if (mBuiltinDisplaySizeAndTextPreference == null) {
            mBuiltinDisplaySizeAndTextPreference = new BuiltinDisplaySizeAndTextPreference(context);
        }
        return mBuiltinDisplaySizeAndTextPreference;
    }

    @NonNull Preference getDisplayTopologyPreference(@NonNull Context context) {
        if (mDisplayTopologyPreference == null) {
            mDisplayTopologyPreference = new DisplayTopologyPreference(context);
            PrefBasics.DISPLAY_TOPOLOGY.apply(mDisplayTopologyPreference);
        }
        return mDisplayTopologyPreference;
    }

    @NonNull Preference getMirrorPreference(@NonNull Context context) {
        if (mMirrorPreference == null) {
            mMirrorPreference = new MirrorPreference(context);
            PrefBasics.MIRROR.apply(mMirrorPreference);
        }
        return mMirrorPreference;
    }

    @NonNull
    private AccessibilitySeekBarPreference reuseSizePreference(Context context,
            PrefRefresh refresh) {
        AccessibilitySeekBarPreference pref =
                refresh.findUnusedPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.key);
        if (pref == null) {
            pref = new AccessibilitySeekBarPreference(context, /* attrs= */ null);
            pref.setIconStart(R.drawable.ic_remove_24dp);
            pref.setIconStartContentDescription(R.string.screen_zoom_make_smaller_desc);
            pref.setIconEnd(R.drawable.ic_add_24dp);
            pref.setIconEndContentDescription(R.string.screen_zoom_make_larger_desc);
            PrefBasics.EXTERNAL_DISPLAY_SIZE.apply(pref);
        }
        refresh.addPreference(pref);
        return pref;
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mPreviouslyShownListOfDisplays = Boolean.TRUE.equals(savedInstanceState.getSerializable(
                PREVIOUSLY_SHOWN_LIST_KEY, Boolean.class));
    }

    private void update() {
        final var screen = getPreferenceScreen();
        if (screen == null || mInjector == null || mInjector.getContext() == null) {
            return;
        }
        try (var cleanableScreen = new PrefRefresh(screen)) {
            updateScreenForDisplayId(getDisplayIdArg(), cleanableScreen, mInjector.getContext());
        }
    }

    private void updateScreenForDisplayId(final int displayId,
            @NonNull final PrefRefresh screen, @NonNull Context context) {
        final var displaysToShow = externalDisplaysToShow(displayId);

        if (displaysToShow.isEmpty() && displayId == INVALID_DISPLAY) {
            showTextWhenNoDisplaysToShow(screen, context);
        } else if (displaysToShow.size() == 1
                && ((displayId == INVALID_DISPLAY && !mPreviouslyShownListOfDisplays)
                        || displaysToShow.get(0).getDisplayId() == displayId)) {
            showDisplaySettings(displaysToShow.get(0), screen, context);
            if (displayId == INVALID_DISPLAY && isTopologyPaneEnabled(mInjector)) {
                // Only show the topology pane if the user did not arrive via the displays list.
                maybeAddV2Components(context, screen);
            }
        } else if (displayId == INVALID_DISPLAY) {
            // If ever shown a list of displays - keep showing it for consistency after
            // disconnecting one of the displays, and only one display is left.
            mPreviouslyShownListOfDisplays = true;
            showDisplaysList(displaysToShow, screen, context);
        }
        updateSettingsTitle(displaysToShow, displayId);
    }

    private void updateSettingsTitle(@NonNull final List<Display> displaysToShow, int displayId) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        if (displaysToShow.size() == 1 && displaysToShow.get(0).getDisplayId() == displayId) {
            var displayName = displaysToShow.get(0).getName();
            if (!displayName.isEmpty()) {
                activity.setTitle(displayName.substring(0, Math.min(displayName.length(), 40)));
                return;
            }
        }
        activity.setTitle(EXTERNAL_DISPLAY_TITLE_RESOURCE);
    }

    private void showTextWhenNoDisplaysToShow(@NonNull final PrefRefresh screen,
            @NonNull Context context) {
        if (isUseDisplaySettingEnabled(mInjector)) {
            addUseDisplayPreferenceNoDisplaysFound(context, screen);
        }
        addFooterPreference(context, screen, EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE);
    }

    private static PreferenceCategory getCategoryForDisplay(@NonNull Display display,
            @NonNull PrefRefresh screen, @NonNull Context context) {
        // The rest of the settings are in a category with the display name as the title.
        String categoryKey = "expanded_display_items_" + display.getDisplayId();
        var category = (PreferenceCategory) screen.findUnusedPreference(categoryKey);

        if (category != null) {
            screen.addPreference(category);
        } else {
            category = new PreferenceCategory(context);
            screen.addPreference(category);
            category.setPersistent(false);
            category.setKey(categoryKey);
            category.setTitle(display.getName());
            category.setOrder(PrefBasics.BUILTIN_DISPLAY_LIST.order + 1);
        }

        return category;
    }

    private void showDisplaySettings(@NonNull Display display, @NonNull PrefRefresh screen,
            @NonNull Context context) {
        final var isEnabled = mInjector != null && mInjector.isDisplayEnabled(display);
        if (isUseDisplaySettingEnabled(mInjector)) {
            addUseDisplayPreferenceForDisplay(context, screen, display, isEnabled);
        }
        if (!isEnabled) {
            // Skip all other settings
            return;
        }
        final var displayRotation = getDisplayRotation(display.getDisplayId());
        if (!isTopologyPaneEnabled(mInjector)) {
            screen.addPreference(updateIllustrationImage(context, displayRotation));
        }

        if (isTopologyPaneEnabled(mInjector)) {
            var displayCategory = getCategoryForDisplay(display, screen, context);
            try (var categoryRefresh = new PrefRefresh(displayCategory)) {
                addDisplaySettings(context, categoryRefresh, display, displayRotation);
            }
        } else {
            addDisplaySettings(context, screen, display, displayRotation);
        }

    }

    private void addDisplaySettings(Context context, PrefRefresh refresh, Display display,
            int displayRotation) {
        addResolutionPreference(context, refresh, display);
        addRotationPreference(context, refresh, display, displayRotation);
        if (isResolutionSettingEnabled(mInjector)) {
            // Do not show the footer about changing resolution affecting apps. This is not in the
            // UX design for v2, and there is no good place to put it, since (a) if it is on the
            // bottom of the screen, the external resolution setting must be below the built-in
            // display options for the per-display fragment, which is too hidden for the per-display
            // fragment, or (b) the footer is above the Built-in display settings, rather than the
            // bottom of the screen, which contradicts the visual style and purpose of the
            // FooterPreference class, or (c) we must hide the built-in display settings, which is
            // inconsistent with the topology pane, which shows that display.
            // TODO(b/352648432): probably remove footer once the pane and rest of v2 UI is in
            // place.
            if (!isTopologyPaneEnabled(mInjector)) {
                addFooterPreference(
                        context, refresh, EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE);
            }
        }
        if (isDisplaySizeSettingEnabled(mInjector)) {
            addSizePreference(context, refresh);
        }
    }

    private void maybeAddV2Components(Context context, PrefRefresh screen) {
        if (isTopologyPaneEnabled(mInjector)) {
            screen.addPreference(getDisplayTopologyPreference(context));
            screen.addPreference(getMirrorPreference(context));

            // If topology is shown, we also show a preference for the built-in display for
            // consistency with the topology.
            var builtinCategory = getBuiltinDisplayListPreference(context);
            screen.addPreference(builtinCategory);
            builtinCategory.addPreference(getBuiltinDisplaySizeAndTextPreference(context));
        }
    }

    private void showDisplaysList(@NonNull List<Display> displaysToShow,
                                  @NonNull PrefRefresh screen, @NonNull Context context) {
        maybeAddV2Components(context, screen);
        int order = PrefBasics.BUILTIN_DISPLAY_LIST.order;
        for (var display : displaysToShow) {
            var pref = getDisplayPreference(context, display, screen, ++order);
            pref.setSummary(display.getMode().getPhysicalWidth() + " x "
                               + display.getMode().getPhysicalHeight());
        }
    }

    @VisibleForTesting
    static String displayListDisplayCategoryKey(int displayId) {
        return "display_list_display_category_" + displayId;
    }

    @VisibleForTesting
    static String resolutionRotationPreferenceKey(int displayId) {
        return "display_id_" + displayId;
    }

    private Preference getDisplayPreference(@NonNull Context context,
            @NonNull Display display, @NonNull PrefRefresh groupCleanable, int categoryOrder) {
        var itemKey = resolutionRotationPreferenceKey(display.getDisplayId());
        var categoryKey = displayListDisplayCategoryKey(display.getDisplayId());
        var category = (PreferenceCategory) groupCleanable.findUnusedPreference(categoryKey);

        if (category != null) {
            groupCleanable.addPreference(category);
            return category.findPreference(itemKey);
        } else {
            category = new PreferenceCategory(context);
            category.setPersistent(false);
            category.setKey(categoryKey);
            category.setOrder(categoryOrder);
            // Must add the category to the hierarchy before adding its descendants. Otherwise
            // the category will not have a preference manager, which causes an exception when a
            // child is added to it.
            groupCleanable.addPreference(category);

            var prefItem = new DisplayPreference(context, display);
            prefItem.setTitle(
                    context.getString(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.titleResource) + " | "
                    + context.getString(PrefBasics.EXTERNAL_DISPLAY_ROTATION.titleResource));
            prefItem.setKey(itemKey);

            category.addPreference(prefItem);
            category.setTitle(display.getName());

            return prefItem;
        }
    }

    private List<Display> externalDisplaysToShow(int displayIdToShow) {
        if (mInjector == null) {
            return List.of();
        }
        if (displayIdToShow != INVALID_DISPLAY) {
            var display = mInjector.getDisplay(displayIdToShow);
            if (display != null && isDisplayAllowed(display, mInjector)) {
                return List.of(display);
            }
        }
        var displaysToShow = new ArrayList<Display>();
        for (var display : mInjector.getAllDisplays()) {
            if (display != null && isDisplayAllowed(display, mInjector)) {
                displaysToShow.add(display);
            }
        }
        return displaysToShow;
    }

    private void addUseDisplayPreferenceNoDisplaysFound(Context context, PrefRefresh refresh) {
        final var pref = reuseUseDisplayPreference(context, refresh);
        pref.setChecked(false);
        pref.setEnabled(false);
        pref.setOnPreferenceChangeListener(null);
    }

    private void addUseDisplayPreferenceForDisplay(final Context context,
            PrefRefresh refresh, final Display display, boolean isEnabled) {
        final var pref = reuseUseDisplayPreference(context, refresh);
        pref.setChecked(isEnabled);
        pref.setEnabled(true);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            final boolean result;
            if (mInjector == null) {
                return false;
            }
            if ((Boolean) newValue) {
                result = mInjector.enableConnectedDisplay(display.getDisplayId());
            } else {
                result = mInjector.disableConnectedDisplay(display.getDisplayId());
            }
            if (result) {
                pref.setChecked((Boolean) newValue);
            }
            return result;
        });
    }

    private Preference updateIllustrationImage(@NonNull final Context context,
            final int displayRotation) {
        var pref = getIllustrationPreference(context);
        if (displayRotation % 2 == 0) {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE);
        } else {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE);
        }
        return pref;
    }

    private void addRotationPreference(final Context context,
            PrefRefresh refresh, final Display display, final int displayRotation) {
        var pref = reuseRotationPreference(context, refresh);
        if (mRotationEntries == null || mRotationEntriesValues == null) {
            mRotationEntries = new String[] {
                    context.getString(R.string.external_display_standard_rotation),
                    context.getString(R.string.external_display_rotation_90),
                    context.getString(R.string.external_display_rotation_180),
                    context.getString(R.string.external_display_rotation_270)};
            mRotationEntriesValues = new String[] {"0", "1", "2", "3"};
        }
        pref.setEntries(mRotationEntries);
        pref.setEntryValues(mRotationEntriesValues);
        pref.setValueIndex(displayRotation);
        pref.setSummary(mRotationEntries[displayRotation]);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            var rotation = Integer.parseInt((String) newValue);
            var displayId = display.getDisplayId();
            if (mInjector == null || !mInjector.freezeDisplayRotation(displayId, rotation)) {
                return false;
            }
            pref.setValueIndex(rotation);
            return true;
        });
        pref.setEnabled(isRotationSettingEnabled(mInjector));
    }

    private void addResolutionPreference(final Context context, PrefRefresh refresh,
            final Display display) {
        var pref = reuseResolutionPreference(context, refresh);
        pref.setSummary(display.getMode().getPhysicalWidth() + " x "
                + display.getMode().getPhysicalHeight());
        pref.setOnPreferenceClickListener((Preference p) -> {
            writePreferenceClickMetric(p);
            launchResolutionSelector(context, display.getDisplayId());
            return true;
        });
        pref.setEnabled(isResolutionSettingEnabled(mInjector));
    }

    private void addSizePreference(final Context context, PrefRefresh refresh) {
        var pref = reuseSizePreference(context, refresh);
        pref.setSummary(EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE);
        pref.setOnPreferenceClickListener(
                (Preference p) -> {
                    writePreferenceClickMetric(p);
                    return true;
                });
    }

    private int getDisplayRotation(int displayId) {
        if (mInjector == null) {
            return 0;
        }
        return Math.min(3, Math.max(0, mInjector.getDisplayUserRotation(displayId)));
    }

    private void scheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        unscheduleUpdate();
        mInjector.getHandler().post(mUpdateRunnable);
    }

    private void unscheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        mInjector.getHandler().removeCallbacks(mUpdateRunnable);
    }

    private class BuiltinDisplaySizeAndTextPreference extends Preference
            implements Preference.OnPreferenceClickListener {
        BuiltinDisplaySizeAndTextPreference(@NonNull final Context context) {
            super(context);

            setPersistent(false);
            setKey("builtin_display_size_and_text");
            setTitle(R.string.accessibility_text_reading_options_title);
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            launchBuiltinDisplaySettings();
            return true;
        }
    }

    @VisibleForTesting
    class DisplayPreference extends TwoTargetPreference
            implements Preference.OnPreferenceClickListener {
        private final int mDisplayId;

        DisplayPreference(@NonNull final Context context, @NonNull final Display display) {
            super(context);
            mDisplayId = display.getDisplayId();

            setPersistent(false);
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            launchExternalDisplaySettings(mDisplayId);
            writePreferenceClickMetric(preference);
            return true;
        }
    }

    private static class PrefRefresh implements AutoCloseable {
        private final PreferenceGroup mScreen;
        private final HashMap<String, Preference> mUnusedPreferences = new HashMap<>();

        PrefRefresh(@NonNull final PreferenceGroup screen) {
            mScreen = screen;
            int preferencesCount = mScreen.getPreferenceCount();
            for (int i = 0; i < preferencesCount; i++) {
                var pref = mScreen.getPreference(i);
                if (pref.hasKey()) {
                    mUnusedPreferences.put(pref.getKey(), pref);
                }
            }
        }

        @Nullable
        <P extends Preference> P findUnusedPreference(@NonNull String key) {
            return (P) mUnusedPreferences.get(key);
        }

        boolean addPreference(@NonNull final Preference pref) {
            if (pref.hasKey()) {
                final var previousPref = mUnusedPreferences.get(pref.getKey());
                if (pref == previousPref) {
                    // Exact preference already added, no need to add it again.
                    // And no need to remove this preference either.
                    mUnusedPreferences.remove(pref.getKey());
                    return true;
                }
                // Exact preference is not yet added
            }
            return mScreen.addPreference(pref);
        }

        @Override
        public void close() {
            for (var v : mUnusedPreferences.values()) {
                mScreen.removePreference(v);
            }
        }
    }
}
