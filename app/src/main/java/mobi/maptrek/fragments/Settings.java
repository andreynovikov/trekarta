/*
 * Copyright 2022 Andrey Novikov
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

package mobi.maptrek.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;

public class Settings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ARG_HILLSHADES_AVAILABLE = "hillshades_available";

    private FragmentHolder mFragmentHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Bundle args = getArguments();
        if (!args.getBoolean(ARG_HILLSHADES_AVAILABLE, false)) {
            PreferenceCategory category = (PreferenceCategory) findPreference("category_general");
            category.removePreference(findPreference("hillshades_transparency"));
        }

        /*
        Preference sdcardPref = findPreference("move_data");
        sdcardPref.setOnPreferenceClickListener(preference -> {
            mFragmentHolder.popCurrent();
            return false;
        });
         */

        /*
        Preference resetPref = findPreference("reset_advices");
        resetPref.setOnPreferenceClickListener(preference -> {
            mFragmentHolder.popCurrent();
            return false;
        });
         */

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
    }

    @Override
    public void onResume() {
        super.onResume();
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

    @SuppressWarnings("unused")
    private void updatePreference(Preference preference, String key) {
        if (preference == null) return;
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
            //return;
        }
        //SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        //preference.setSummary(sharedPrefs.getString(key, "Default"));
    }
}