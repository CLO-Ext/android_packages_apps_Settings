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

package com.android.settings.notification.modes;

import android.app.AlertDialog;
import android.app.Application;
import android.app.AutomaticZenRule;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenMode;

import java.util.ArrayList;
import java.util.List;

public class ZenModeFragment extends ZenModeFragmentBase {

    // for mode deletion menu
    private static final int DELETE_MODE = 1;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_rule_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(new ZenModeHeaderController(context, "header", this, mBackend));
        prefControllers.add(new ZenModeButtonPreferenceController(context, "activate", mBackend));
        prefControllers.add(new ZenModeActionsPreferenceController(context, "actions", mBackend));
        prefControllers.add(new ZenModePeopleLinkPreferenceController(
                context, "zen_mode_people", mBackend, mHelperBackend));
        prefControllers.add(new ZenModeAppsLinkPreferenceController(
                context, "zen_mode_apps", this,
                ApplicationsState.getInstance((Application) context.getApplicationContext()),
                mBackend, mHelperBackend));
        prefControllers.add(new ZenModeOtherLinkPreferenceController(
                context, "zen_other_settings", mBackend, mHelperBackend));
        prefControllers.add(new ZenModeDisplayLinkPreferenceController(
                context, "mode_display_settings", mBackend, mHelperBackend));
        prefControllers.add(new ZenModeSetTriggerLinkPreferenceController(context,
                "zen_automatic_trigger_category", this, mBackend));
        prefControllers.add(new InterruptionFilterPreferenceController(
                context, "allow_filtering", mBackend));
        return prefControllers;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set title for the entire screen
        ZenMode mode = getMode();
        AutomaticZenRule azr = getAZR();
        if (mode == null || azr == null) {
            return;
        }
        getActivity().setTitle(azr.getName());
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, DELETE_MODE, Menu.NONE, R.string.zen_mode_menu_delete_mode);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    protected boolean onOptionsItemSelected(MenuItem item, ZenMode zenMode) {
        switch (item.getItemId()) {
            case DELETE_MODE:
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.zen_mode_delete_mode_confirmation,
                                zenMode.getRule().getName()))
                        .setPositiveButton(R.string.zen_mode_schedule_delete,
                                (dialog, which) -> {
                                    // start finishing before calling removeMode() so that we don't
                                    // try to update this activity with a nonexistent mode when the
                                    // zen mode config is updated
                                    finish();
                                    mBackend.removeMode(zenMode);
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void updateZenModeState() {
        // Because this fragment may be asked to finish by the delete menu but not be done doing
        // so yet, ignore any attempts to update info in that case.
        if (getActivity() != null && getActivity().isFinishing()) {
            return;
        }
        super.updateZenModeState();
    }
}
