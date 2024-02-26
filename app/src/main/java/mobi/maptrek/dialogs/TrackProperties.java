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

package mobi.maptrek.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.databinding.DialogTrackPropertiesBinding;
import mobi.maptrek.fragments.OnTrackActionListener;
import mobi.maptrek.viewmodels.TrackViewModel;

public class TrackProperties extends DialogFragment {
    private OnTrackActionListener mListener;
    private TrackViewModel trackViewModel;
    private DialogTrackPropertiesBinding viewBinding;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        viewBinding = DialogTrackPropertiesBinding.inflate(getLayoutInflater());

        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.selectedTrack.observe(this, track -> {
            if (track == null)
                return;
            String name;
            int color;
            if (savedInstanceState != null) {
                name = savedInstanceState.getString("name");
                color = savedInstanceState.getInt("color");
            } else {
                name = track.name;
                color = track.style.color;
            }
            viewBinding.nameEdit.setText(name);
            viewBinding.colorSwatch.setColor(color);
        });

        viewBinding.nameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                returnResult();
                dismiss();
            }
            return false;
        });

        viewBinding.colorSwatch.setOnClickListener(v -> {
            ColorPickerDialog dialog = new ColorPickerDialog();
            dialog.setColors(MarkerStyle.DEFAULT_COLORS, viewBinding.colorSwatch.getColor());
            dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
            dialog.setOnColorSelectedListener(color -> viewBinding.colorSwatch.setColor(color));
            dialog.show(getParentFragmentManager(), "ColorPickerDialog");
        });

        Dialog alertDialog = new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.actionSave, (dialog, which) -> returnResult())
                .setView(viewBinding.getRoot())
                .create();

        Window window = alertDialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);

        return alertDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnPlaceActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", viewBinding.nameEdit.getText().toString());
        outState.putInt("color", viewBinding.colorSwatch.getColor());
    }

    private void returnResult() {
        Track track = trackViewModel.selectedTrack.getValue();
        if (track == null)
            return;
        String name = viewBinding.nameEdit.getText().toString().trim();
        int color = viewBinding.colorSwatch.getColor();
        if (!name.equals(track.name) || color != track.style.color) {
            if (!name.isEmpty())
                track.name = name;
            track.style.color = color;
            mListener.onTrackSave(track);
        }
    }
}
