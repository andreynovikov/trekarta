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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.Preference;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;

public class Advanced extends BasePreferences {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);

        Preference purgeMaps = findPreference("purge_maps");
        if (purgeMaps != null)
            purgeMaps.setOnPreferenceClickListener(preference -> {
                CoordinatorLayout coordinatorLayout = ((MainActivity) requireActivity()).getCoordinatorLayout();
                Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.msgPurgeMaps, Snackbar.LENGTH_LONG)
                        .setAnchorView(getView())
                        .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                                super.onDismissed(snackbar, event);
                                if (event == DISMISS_EVENT_ACTION)
                                    return;
                                requireActivity().finish();
                                MapTrek.getApplication().removeMapDatabase();
                                MapTrek.getApplication().restart();
                            }
                        })
                        .setAction(R.string.actionUndo, view -> {
                            // do nothing, we just do not precede with destructive operation
                        });
                TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                snackbarTextView.setMaxLines(99);
                snackbar.show();
                return true;
            });

        Preference resetMap = findPreference("reset_map");
        if (resetMap != null)
            resetMap.setOnPreferenceClickListener(preference -> {
                CoordinatorLayout coordinatorLayout = ((MainActivity) requireActivity()).getCoordinatorLayout();
                Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.msgMapReset, Snackbar.LENGTH_LONG)
                        .setAnchorView(getView())
                        .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                                super.onDismissed(snackbar, event);
                                if (event == DISMISS_EVENT_ACTION)
                                    return;
                                Configuration.resetMapState();
                                MapTrek.getApplication().restart();
                            }
                        })
                        .setAction(R.string.actionUndo, view -> {
                            // do nothing, we just do not precede with destructive operation
                        });
                TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                snackbarTextView.setMaxLines(99);
                snackbar.show();
                return true;
            });

        Preference resetAdvices = findPreference("reset_advices");
        if (resetAdvices != null)
            resetAdvices.setOnPreferenceClickListener(preference -> {
                Configuration.resetAdviceState();
                CoordinatorLayout coordinatorLayout = ((MainActivity) requireActivity()).getCoordinatorLayout();
                Snackbar.make(coordinatorLayout, R.string.msgAdvicesReset, Snackbar.LENGTH_LONG)
                        .setAnchorView(getView())
                        .show();
                return true;
            });
    }
}
