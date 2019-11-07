/*
 * Copyright 2019 Andrey Novikov
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

package mobi.maptrek.ui;

import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import mobi.maptrek.R;

import static androidx.core.view.ViewCompat.setElevation;

public class SnackbarHelper {

    public static void configureSnackbar(Snackbar snackbar) {
        addMargins(snackbar);
        setBackground(snackbar);
        setElevation(snackbar.getView(), 6f);
    }

    private static void addMargins(Snackbar snackbar) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbar.getView().getLayoutParams();
        params.setMargins(12, 12, 12, 12);
        snackbar.getView().setLayoutParams(params);
    }

    private static void setBackground(Snackbar snackbar) {
        snackbar.getView().setBackgroundResource(R.drawable.background_snackbar);
    }
}