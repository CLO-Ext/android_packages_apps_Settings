/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *           (C) 2022 Nameless-AOSP Project
 *           (C) 2022 Paranoid Android
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

package com.android.settings.display

import android.app.ActivityManager
import android.os.Bundle
import android.provider.Settings

import com.android.internal.util.neoteric.cutout.CutoutFullscreenController

import com.android.settings.R

import org.neoteric.preference.AppListFragment

class DisplayCutoutForceFullscreenSettings: AppListFragment() {

    private lateinit var activityManager: ActivityManager
    private lateinit var cutoutForceFullscreenSettings: CutoutFullscreenController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDisplayCategory(CATEGORY_USER_ONLY)
        activityManager = requireContext().getSystemService(ActivityManager::class.java) as ActivityManager
        cutoutForceFullscreenSettings = CutoutFullscreenController(requireContext());
    }

    override protected fun getTitle(): Int {
        return R.string.display_cutout_force_fullscreen_title
    }

    /**
     * @return an initial list of packages that should appear as selected.
     */
    override protected fun getInitialCheckedList(): List<String> {
        val flattenedString = Settings.System.getString(
            requireContext().contentResolver, getKey()
        )
        return flattenedString?.takeIf {
            it.isNotBlank()
        }?.split(",")?.toList() ?: emptyList()
    }

    /**
     * Called when user selects an item.
     *
     * @param list a [List<String>] of selected items.
     */
    override protected fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (packageName.isBlank()) return
        if (isChecked) {
            cutoutForceFullscreenSettings.addApp(packageName);
        } else {
            cutoutForceFullscreenSettings.removeApp(packageName);
        }
        try {
            activityManager.forceStopPackage(packageName);
        } catch (ignored: Exception) {
        }
    }

    private fun getKey(): String {
        return Settings.System.FORCE_FULLSCREEN_CUTOUT_APPS
    }
}
