package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;

import mobi.maptrek.R;
import mobi.maptrek.view.ColorSwatch;

public class TrackProperties extends Fragment {
    public static final String ARG_NAME = "name";
    public static final String ARG_COLOR = "color";

    private ViewGroup mRootView;
    private EditText mNameEdit;
    private ColorSwatch mColorSwatch;
    private String mName;
    private int mColor;

    private OnTrackPropertiesChangedListener mListener;

    public TrackProperties() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_track_properties, container, false);
        mNameEdit = (EditText) mRootView.findViewById(R.id.nameEdit);
        mColorSwatch = (ColorSwatch) mRootView.findViewById(R.id.colorSwatch);
        return mRootView;
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

        mNameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    returnResult();
                    getFragmentManager().popBackStack();
                }
                return false;
            }
        });
        mColorSwatch.setColor(mColor);

        mColorSwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Add current color if it is not in the list
                int[] colors = new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00, 0xff00ffff, 0xff000000, 0xffff00ff, getContext().getColor(R.color.trackColor)};
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(colors, mColor);
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        mColorSwatch.setColor(color);
                        mColor = color;
                    }
                });
                dialog.show(getFragmentManager(), "ColorPickerDialog");
            }
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        returnResult();
        mRootView = null;
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
