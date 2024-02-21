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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import mobi.maptrek.R;
import mobi.maptrek.data.Place;
import mobi.maptrek.data.style.MarkerStyle;

public class PlaceProperties extends DialogFragment {
    private EditText mNameEdit;
    private ColorPickerSwatch mColorSwatch;
    private String mName;
    private int mColor;

    private OnPlacePropertiesChangedListener mListener;

    public PlaceProperties() {
        super();
    }

    public PlaceProperties(Place place) {
        super();
        mName = place.name;
        mColor = place.style.color;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_track_properties, null);

        mNameEdit = dialogView.findViewById(R.id.nameEdit);
        mColorSwatch = dialogView.findViewById(R.id.colorSwatch);

        String name = mName;
        int color = mColor;

        if (savedInstanceState != null) {
            name = savedInstanceState.getString("name");
            color = savedInstanceState.getInt("color");
        }

        mNameEdit.setText(name);
        mNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                returnResult();
                dismiss();
            }
            return false;
        });

        mColorSwatch.setColor(color);
        mColorSwatch.setOnClickListener(v -> {
            ColorPickerDialog dialog = new ColorPickerDialog();
            dialog.setColors(MarkerStyle.DEFAULT_COLORS, mColor);
            dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
            dialog.setOnColorSelectedListener(color1 -> mColorSwatch.setColor(color1));
            dialog.show(getParentFragmentManager(), "ColorPickerDialog");
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setPositiveButton(R.string.actionSave, (dialog, which) -> returnResult());
        dialogBuilder.setView(dialogView);

        Dialog dialog = dialogBuilder.create();

        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);

        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnPlacePropertiesChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnPlacePropertiesChangedListener");
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
        outState.putString("name", mNameEdit.getText().toString());
        outState.putInt("color", mColorSwatch.getColor());
    }

    private void returnResult() {
        String name = mNameEdit.getText().toString();
        int color = mColorSwatch.getColor();
        if (!name.equals(mName) || color != mColor) {
            mListener.onPlacePropertiesChanged(name, color);
            mName = name;
            mColor = color;
        }
    }

    public interface OnPlacePropertiesChangedListener {
        void onPlacePropertiesChanged(String name, int color);
    }
}
