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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class UtilsTest {
    private static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;
    private static final String TEMP_BOND_METADATA =
            "<TEMP_BOND_TYPE>le_audio_sharing</TEMP_BOND_TYPE>";
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private LocalBluetoothManager mLocalBtManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private LocalBluetoothLeBroadcast mBroadcast;
    @Mock
    private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock
    private CachedBluetoothDeviceManager mDeviceManager;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        mMetricsFeatureProvider = FakeFeatureFactory.setupForTest().getMetricsFeatureProvider();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void showConnectingError_shouldLogBluetoothConnectError() {
        when(mContext.getString(anyInt(), anyString())).thenReturn("testMessage");
        Utils.showConnectingError(mContext, "testName", mock(LocalBluetoothManager.class));

        verify(mMetricsFeatureProvider).visible(eq(mContext), anyInt(),
                eq(MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR), anyInt());
    }

    @Test
    public void shouldBlockPairingInAudioSharing_broadcastOff_returnFalse() {
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        assertThat(Utils.shouldBlockPairingInAudioSharing(mLocalBtManager)).isFalse();
    }

    @Test
    public void shouldBlockPairingInAudioSharing_singlePermanentBondSinkInSharing_returnFalse() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothDevice device = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(mDeviceManager.findDevice(device)).thenReturn(cachedDevice);
        when(cachedDevice.getGroupId()).thenReturn(1);
        when(cachedDevice.getDevice()).thenReturn(device);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(device));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(device)).thenReturn(ImmutableList.of(state));
        assertThat(Utils.shouldBlockPairingInAudioSharing(mLocalBtManager)).isFalse();
    }

    @Test
    public void shouldBlockPairingInAudioSharing_singleTempBondSinkInSharing_returnTrue() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothDevice device = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(mDeviceManager.findDevice(device)).thenReturn(cachedDevice);
        when(cachedDevice.getGroupId()).thenReturn(1);
        when(cachedDevice.getDevice()).thenReturn(device);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(device));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(device)).thenReturn(ImmutableList.of(state));
        when(device.getMetadata(METADATA_FAST_PAIR_CUSTOMIZED_FIELDS))
                .thenReturn(TEMP_BOND_METADATA.getBytes());
        assertThat(Utils.shouldBlockPairingInAudioSharing(mLocalBtManager)).isTrue();
    }

    @Test
    public void shouldBlockPairingInAudioSharing_twoSinksInSharing_returnTrue() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothDevice device1 = mock(BluetoothDevice.class);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(mDeviceManager.findDevice(device1)).thenReturn(cachedDevice1);
        when(mDeviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(cachedDevice1.getDevice()).thenReturn(device1);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(device1, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(state));
        assertThat(Utils.shouldBlockPairingInAudioSharing(mLocalBtManager)).isTrue();
    }
}
