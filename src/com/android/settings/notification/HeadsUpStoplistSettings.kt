/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification

import android.os.Bundle
import android.provider.Settings

import com.android.settings.R

import org.neoteric.preference.AppListFragment

class HeadsUpStoplistSettings : AppListFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDisplayCategory(CATEGORY_BOTH)
        val whiteListedPackages = requireContext().resources.getStringArray(
            R.array.config_headsUpConfAllowedSystemApps)
        setCustomFilter {
            !it.applicationInfo!!.isSystemApp() || whiteListedPackages.contains(it.packageName)
        }
    }

    override protected fun getTitle(): Int {
        return R.string.heads_up_stoplist_title
    }

    override protected fun getInitialCheckedList(): List<String> {
        val packageList = Settings.System.getString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_STOPLIST_VALUES
        )
        return packageList?.takeIf { it.isNotBlank() }?.split("|") ?: emptyList()
    }

    override protected fun onListUpdate(list: List<String>) {
        Settings.System.putString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_STOPLIST_VALUES,
            list.joinToString("|")
        )
    }
}
