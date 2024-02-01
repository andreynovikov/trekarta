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

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import mobi.maptrek.R;
import mobi.maptrek.viewmodels.MapIndexViewModel;

public class General extends BasePreferences {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_general);

        MapIndexViewModel mapIndexViewModel = new ViewModelProvider(requireActivity()).get(MapIndexViewModel.class);

        if (!mapIndexViewModel.nativeIndex.hasHillshades()) {
            Preference preference = findPreference("hillshades_transparency");
            if (preference != null)
                getPreferenceScreen().removePreference(preference);
        }
    }
}
