/*
 * Copyright 2023 Andrey Novikov
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import mobi.maptrek.R;
import mobi.maptrek.data.style.MarkerStyle;

public class WaypointProperties extends Fragment {
    public static final String ARG_NAME = "name";
    public static final String ARG_COLOR = "color";

    private EditText mNameEdit;
    private ColorPickerSwatch mColorSwatch;
    private String mName;
    private int mColor;

    private OnWaypointPropertiesChangedListener mListener;
    private FragmentHolder mFragmentHolder;

    public WaypointProperties() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_track_properties, container, false);
        mNameEdit = rootView.findViewById(R.id.nameEdit);
        mColorSwatch = rootView.findViewById(R.id.colorSwatch);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mName = arguments.getString(ARG_NAME);
            mColor = arguments.getInt(ARG_COLOR);
        }

        String name = mName;
        int color = mColor;

        if (savedInstanceState != null) {
            name = savedInstanceState.getString(ARG_NAME);
            color = savedInstanceState.getInt(ARG_COLOR);
        }

        mNameEdit.setText(name);
        mNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                returnResult();
                mFragmentHolder.popCurrent();
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
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnWaypointPropertiesChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnWaypointPropertiesChangedListener");
        }
        mFragmentHolder = (FragmentHolder) context;
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackPressedCallback.remove();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_NAME, mNameEdit.getText().toString());
        outState.putInt(ARG_COLOR, mColorSwatch.getColor());
    }

    private void returnResult() {
        String name = mNameEdit.getText().toString();
        int color = mColorSwatch.getColor();
        if (!name.equals(mName) || color != mColor) {
            mListener.onWaypointPropertiesChanged(name, color);
            mName = name;
            mColor = color;
        }
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            returnResult();
            this.remove();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    };

    public interface OnWaypointPropertiesChangedListener {
        void onWaypointPropertiesChanged(String name, int color);
    }
}
