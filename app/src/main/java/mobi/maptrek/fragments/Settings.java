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
            PreferenceCategory category = (PreferenceCategory) findPreference("category_advanced");
            Preference hillshadePref = findPreference("hillshades_transparency");
            category.removePreference(hillshadePref);
        }

        Preference resetPref = findPreference("reset_advices");
        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                mFragmentHolder.popCurrent();
                return false;
            }
        });

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
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
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