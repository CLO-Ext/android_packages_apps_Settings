/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.ethernet

import android.content.Context
import android.net.EthernetManager
import android.net.IpConfiguration
import androidx.core.content.ContextCompat

class EthernetInterfaceTracker private constructor(private val context: Context) :
    EthernetManager.InterfaceStateListener {
    interface EthernetInterfaceTrackerListener {
        fun onInterfaceListChanged(ethernetInterfaces: List<EthernetInterface>)
    }

    private val ethernetManager: EthernetManager? =
        context.applicationContext.getSystemService(EthernetManager::class.java)
    private val TAG = "EthernetInterfaceTracker"

    // Maps ethernet interface identifier to EthernetInterface object
    private val ethernetInterfaces = mutableMapOf<String, EthernetInterface>()
    private val interfaceListeners = mutableListOf<EthernetInterfaceTrackerListener>()

    fun getInterface(id: String): EthernetInterface? {
        return ethernetInterfaces.get(id)
    }

    val availableInterfaces: Collection<EthernetInterface>
        get() = ethernetInterfaces.values

    fun registerInterfaceListener(listener: EthernetInterfaceTrackerListener) {
        if (interfaceListeners.isEmpty()) {
            ethernetManager?.addInterfaceStateListener(ContextCompat.getMainExecutor(context), this)
        }
        interfaceListeners.add(listener)
    }

    fun unregisterInterfaceListener(listener: EthernetInterfaceTrackerListener) {
        interfaceListeners.remove(listener)
        if (interfaceListeners.isEmpty()) {
            ethernetManager?.removeInterfaceStateListener(this)
        }
    }

    override fun onInterfaceStateChanged(id: String, state: Int, role: Int, cfg: IpConfiguration?) {
        var interfacesChanged = false
        if (!ethernetInterfaces.contains(id) && state != EthernetManager.STATE_ABSENT) {
            ethernetInterfaces.put(id, EthernetInterface(context, id))
            interfacesChanged = true
        } else if (ethernetInterfaces.contains(id) && state == EthernetManager.STATE_ABSENT) {
            ethernetInterfaces.remove(id)
            interfacesChanged = true
        }
        if (interfacesChanged) {
            for (listener in interfaceListeners) {
                listener.onInterfaceListChanged(ethernetInterfaces.values.toList())
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: EthernetInterfaceTracker? = null

        fun getInstance(context: Context): EthernetInterfaceTracker {
            return INSTANCE
                ?: synchronized(this) {
                    val instance = EthernetInterfaceTracker(context.applicationContext)
                    INSTANCE = instance
                    instance
                }
        }
    }
}
