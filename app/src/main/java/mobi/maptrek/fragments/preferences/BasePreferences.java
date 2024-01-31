/*
 * Copyright 2024 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.fragments.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import org.greenrobot.eventbus.EventBus;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;

public class BasePreferences extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
   public static final String ARG_HILLSHADES_AVAILABLE = "hillshades_available";

   @Override
   public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
      if (sharedPreferences != null)
         sharedPreferences.registerOnSharedPreferenceChangeListener(this);
   }
   @Override
   public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
      addPreferencesFromResource(R.xml.preferences);
   }

   @Override
   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      PreferenceScreen preferenceScreen = getPreferenceScreen();
      for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
         Preference preference = preferenceScreen.getPreference(i);
         if (preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
            for (int j = 0; j < preferenceGroup.getPreferenceCount(); j++) {
               Preference subPref = preferenceGroup.getPreference(j);
               updatePreference(subPref, subPref.getKey());
            }
         } else {
            updatePreference(preference, preference.getKey());
         }
      }
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      EventBus.getDefault().post(new Configuration.ChangedEvent(key));
      updatePreference(findPreference(key), key);
   }

   private void updatePreference(Preference preference, String key) {
      if (preference == null) return;
      if (preference instanceof ListPreference) {
         ListPreference listPreference = (ListPreference) preference;
         listPreference.setSummary(listPreference.getEntry());
         //return;
      }
      // SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
      // preference.setSummary(sharedPrefs.getString(key, "Default"));
   }
}
