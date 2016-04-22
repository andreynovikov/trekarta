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

public class WaypointProperties extends Fragment implements OnBackPressedListener {
    public static final String ARG_NAME = "name";
    public static final String ARG_COLOR = "color";

    private EditText mNameEdit;
    private ColorSwatch mColorSwatch;
    private String mName;
    private int mColor;

    private OnWaypointPropertiesChangedListener mListener;
    private FragmentHolder mFragmentHolder;

    public WaypointProperties() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_track_properties, container, false);
        mNameEdit = (EditText) rootView.findViewById(R.id.nameEdit);
        mColorSwatch = (ColorSwatch) rootView.findViewById(R.id.colorSwatch);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mName = getArguments().getString(ARG_NAME);
        mColor = getArguments().getInt(ARG_COLOR);

        String name = mName;
        int color = mColor;

        if (savedInstanceState != null) {
            name = savedInstanceState.getString(ARG_NAME);
            color = savedInstanceState.getInt(ARG_COLOR);
        }

        mNameEdit.setText(name);
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

        mColorSwatch.setColor(color);
        mColorSwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] colors = new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00, 0xff00ffff, 0xff000000, 0xffff00ff};
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(colors, mColor);
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        mColorSwatch.setColor(color);
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
            mListener = (OnWaypointPropertiesChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointPropertiesChangedListener");
        }
        mFragmentHolder = (FragmentHolder) context;
        mFragmentHolder.addBackClickListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_NAME, mNameEdit.getText().toString());
        outState.putInt(ARG_COLOR, mColorSwatch.getColor());
    }

    @Override
    public boolean onBackClick() {
        returnResult();
        return false;
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

    public interface OnWaypointPropertiesChangedListener {
        void onWaypointPropertiesChanged(String name, int color);
    }
}
