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

package com.android.settings.security

import android.app.ActivityManager
import android.content.pm.UserInfo
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings

import com.android.internal.util.neoteric.HideAppListUtils

import com.android.settings.R

import org.neoteric.preference.AppListFragment

class HideAppListSettings: AppListFragment() {

    private lateinit var activityManager: ActivityManager
    private lateinit var userManager: UserManager
    private lateinit var userInfos: List<UserInfo>

    private var hideAppListUtils: HideAppListUtils = HideAppListUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityManager = requireContext().getSystemService(ActivityManager::class.java) as ActivityManager
        val blackListedPackages = requireContext().resources.getStringArray(
            R.array.hide_applist_hidden_apps)
        setCustomFilter {
            !blackListedPackages.contains(it.packageName)
        }
        userManager = UserManager.get(requireContext())
        userInfos = userManager.getUsers()
        for (info in userInfos) {
            hideAppListUtils.setApps(requireContext(), info.id)
        }
    }

    override protected fun getTitle(): Int {
        return R.string.hide_applist_title
    }

    /**
     * @return an initial list of packages that should appear as selected.
     */
    override fun getInitialCheckedList(): List<String> {
        val flattenedString = Settings.Secure.getString(
            requireContext().contentResolver, getKey()
        )
        return flattenedString?.takeIf {
            it.isNotBlank()
        }?.split(",")?.toList() ?: emptyList()
    }

    override protected fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (packageName.isBlank()) return
        for (info in userInfos) {
            if (isChecked) {
                hideAppListUtils.addApp(requireContext(), packageName, info.id)
            } else {
                hideAppListUtils.removeApp(requireContext(), packageName, info.id)
            }
        }
        try {
            activityManager.forceStopPackage(packageName)
        } catch (ignored: Exception) {
        }
    }

    private fun getKey(): String {
        return Settings.Secure.HIDE_APPLIST
    }
}
