/*
 * Copyright 2018 Andrey Novikov
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

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;

import mobi.maptrek.R;
import mobi.maptrek.data.style.MarkerStyle;

public class TrackProperties extends Fragment {
    public static final String ARG_NAME = "name";
    public static final String ARG_COLOR = "color";

    private EditText mNameEdit;
    private ColorPickerSwatch mColorSwatch;
    private String mName;
    private int mColor;

    private OnTrackPropertiesChangedListener mListener;
    private FragmentHolder mFragmentHolder;

    public TrackProperties() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_track_properties, container, false);
        mNameEdit = rootView.findViewById(R.id.nameEdit);
        mColorSwatch = rootView.findViewById(R.id.colorSwatch);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mName = getArguments().getString(ARG_NAME);
        mColor = getArguments().getInt(ARG_COLOR);
        mNameEdit.setText(mName);
        mColorSwatch.setColor(mColor);

        if (savedInstanceState != null) {
            mNameEdit.setText(savedInstanceState.getString(ARG_NAME));
            mColorSwatch.setColor(savedInstanceState.getInt(ARG_COLOR));
        }

        //FIXME WTF?
        mNameEdit.setText(mName);

        mNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                returnResult();
                mFragmentHolder.popCurrent();
            }
            return false;
        });
        mColorSwatch.setColor(mColor);

        mColorSwatch.setOnClickListener(v -> {
            ColorPickerDialog dialog = new ColorPickerDialog();
            dialog.setColors(MarkerStyle.DEFAULT_COLORS, mColor);
            dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
            dialog.setOnColorSelectedListener(color -> {
                mColorSwatch.setColor(color);
                mColor = color;
            });
            dialog.show(getFragmentManager(), "ColorPickerDialog");
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTrackPropertiesChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackPropertiesChangedListener");
        }
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mFragmentHolder = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        returnResult();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_NAME, mNameEdit.getText().toString());
        outState.putInt(ARG_COLOR, mColorSwatch.getColor());
    }

    private void returnResult() {
        String name = mNameEdit.getText().toString();
        int color = mColorSwatch.getColor();
        if (!name.equals(mName) || color != mColor) {
            mListener.onTrackPropertiesChanged(name, color);
            mName = name;
            mColor = color;
        }
    }

    public interface OnTrackPropertiesChangedListener {
        void onTrackPropertiesChanged(String name, int color);
    }
}
