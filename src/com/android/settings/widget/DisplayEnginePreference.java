/*
 * Copyright (C) 2023-2024 The risingOS Android Project
 * Copyright (C) 2024 Neoteric-OS
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
package com.android.settings.widget;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.android.settings.R;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class DisplayEnginePreference extends Preference {

    private int currentMode;

    public DisplayEnginePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_display_engine);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View view = holder.itemView;

        // Get RadioButtons
        RadioButton radioDefault = view.findViewById(R.id.radio_default);
        RadioButton radioXReality = view.findViewById(R.id.radio_x_reality);
        RadioButton radioVivid = view.findViewById(R.id.radio_vivid);
        RadioButton radioTriluminous = view.findViewById(R.id.radio_triluminous);

        // Get parent LinearLayouts (cast from ViewParent)
        LinearLayout layoutDefault = (LinearLayout) radioDefault.getParent();
        LinearLayout layoutXReality = (LinearLayout) radioXReality.getParent();
        LinearLayout layoutVivid = (LinearLayout) radioVivid.getParent();
        LinearLayout layoutTriluminous = (LinearLayout) radioTriluminous.getParent();

        // Retrieve current mode from settings
        currentMode = Settings.Secure.getInt(
                getContext().getContentResolver(),
                "display_engine_mode",
                0
        );

        // Set initial checked states
        radioDefault.setChecked(currentMode == 0);
        radioXReality.setChecked(currentMode == 1);
        radioVivid.setChecked(currentMode == 2);
        radioTriluminous.setChecked(currentMode == 3);

        // Common click listener
        View.OnClickListener listener = v -> {
            int newMode = 0;
            if (v == layoutXReality) {
                newMode = 1;
            } else if (v == layoutVivid) {
                newMode = 2;
            } else if (v == layoutTriluminous) {
                newMode = 3;
            }

            if (newMode != currentMode) {
                Settings.Secure.putInt(
                        getContext().getContentResolver(),
                        "display_engine_mode",
                        newMode
                );
                currentMode = newMode;

                // Update UI
                radioDefault.setChecked(newMode == 0);
                radioXReality.setChecked(newMode == 1);
                radioVivid.setChecked(newMode == 2);
                radioTriluminous.setChecked(newMode == 3);
            }
        };

        // Attach listeners to LinearLayouts
        layoutDefault.setOnClickListener(listener);
        layoutXReality.setOnClickListener(listener);
        layoutVivid.setOnClickListener(listener);
        layoutTriluminous.setOnClickListener(listener);
    }
}
