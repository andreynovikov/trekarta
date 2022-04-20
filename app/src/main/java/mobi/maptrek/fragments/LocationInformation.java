/*
 * Copyright 2022 Andrey Novikov
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
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;

import java.util.Locale;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.LocationState;
import mobi.maptrek.LocationStateChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.ui.TextInputDialogFragment;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;

public class LocationInformation extends Fragment implements Map.UpdateListener, TextInputDialogFragment.TextInputDialogCallback, LocationStateChangeListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_ZOOM = "zoom";

    private SunriseSunset mSunriseSunset;
    private ViewGroup mRootView;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    TextView mCoordinateDegree;
    TextView mCoordinateDegMin;
    TextView mCoordinateDegMinSec;
    TextView mCoordinateUtmUps;
    TextView mCoordinateMgrs;
    TextView mSunriseTitle;
    TextView mSunsetTitle;
    TextView mSunrise;
    TextView mSunset;
    TextView mOffset;
    TextView mDeclination;

    private double mLatitude;
    private double mLongitude;
    private int mZoom;

    private ImageButton mSwitchOffButton;

    private TextInputDialogFragment mTextInputDialog;
    private int mColorTextPrimary;
    private int mColorDarkBlue;
    private int mColorRed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mSunriseSunset = new SunriseSunset();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_location_information, container, false);

        mSwitchOffButton = mRootView.findViewById(R.id.switchOffButton);
        mSwitchOffButton.setOnClickListener(v -> mMapHolder.disableLocations());
        ImageButton shareButton = mRootView.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> mMapHolder.shareLocation(new GeoPoint(mLatitude, mLongitude), null));
        ImageButton inputButton = mRootView.findViewById(R.id.inputButton);
        inputButton.setOnClickListener(v -> {
            TextInputDialogFragment.Builder builder = new TextInputDialogFragment.Builder();
            mTextInputDialog = builder.setCallbacks(LocationInformation.this)
                    .setHint(container.getContext().getString(R.string.coordinates))
                    .setShowPasteButton(true)
                    .create();
            mTextInputDialog.show(getFragmentManager(), "coordinatesInput");
        });

        mCoordinateDegree = mRootView.findViewById(R.id.coordinate_degree);
        mCoordinateDegMin = mRootView.findViewById(R.id.coordinate_degmin);
        mCoordinateDegMinSec = mRootView.findViewById(R.id.coordinate_degminsec);
        mCoordinateUtmUps = mRootView.findViewById(R.id.coordinate_utmups);
        mCoordinateMgrs = mRootView.findViewById(R.id.coordinate_mgrs);
        mSunriseTitle = mRootView.findViewById(R.id.sunriseTitle);
        mSunsetTitle = mRootView.findViewById(R.id.sunsetTitle);
        mSunrise = mRootView.findViewById(R.id.sunrise);
        mSunset = mRootView.findViewById(R.id.sunset);
        mOffset = mRootView.findViewById(R.id.offset);
        mDeclination = mRootView.findViewById(R.id.declination);

        if (BuildConfig.FULL_VERSION) {
            mRootView.findViewById(R.id.extendTable).setVisibility(View.VISIBLE);
        }

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SUNRISE_SUNSET)) {
            ViewTreeObserver vto = mRootView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (BuildConfig.FULL_VERSION) {
                        View view = mSunrise.getVisibility() == View.VISIBLE ? mSunrise : mSunset;
                        HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SUNRISE_SUNSET, R.string.advice_sunrise_sunset, view, true);
                    }
                }
            });
        }

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        double latitude = 0d;
        double longitude = 0d;
        int zoom = 14;

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            zoom = savedInstanceState.getInt(ARG_ZOOM);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                latitude = getArguments().getDouble(ARG_LATITUDE);
                longitude = getArguments().getDouble(ARG_LONGITUDE);
                zoom = getArguments().getInt(ARG_ZOOM);
            }
        }

        updateLocation(latitude, longitude, zoom);
    }

    @Override
    public void onResume() {
        super.onResume();

        mMapHolder.getMap().events.bind(this);
        mMapHolder.addLocationStateChangeListener(this);

        TextInputDialogFragment coordinatesInput = (TextInputDialogFragment) getFragmentManager().findFragmentByTag("coordinatesInput");
        if (coordinatesInput != null) {
            coordinatesInput.setCallback(this);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mMapHolder.getMap().events.unbind(this);
        mMapHolder.removeLocationStateChangeListener(this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mColorTextPrimary = context.getColor(R.color.textColorPrimary);
        mColorDarkBlue = context.getColor(R.color.darkBlue);
        mColorRed = context.getColor(R.color.red);

        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
        outState.putInt(ARG_ZOOM, mZoom);
    }

    private void updateLocation(double latitude, double longitude, int zoom) {
        mLatitude = latitude;
        mLongitude = longitude;
        mZoom = zoom;

        mCoordinateDegree.setText(StringFormatter.coordinates(0, " ", latitude, longitude));
        mCoordinateDegMin.setText(StringFormatter.coordinates(1, " ", latitude, longitude));
        mCoordinateDegMinSec.setText(StringFormatter.coordinates(2, " ", latitude, longitude));
        mCoordinateUtmUps.setText(StringFormatter.coordinates(3, " ", latitude, longitude));
        mCoordinateMgrs.setText(StringFormatter.coordinates(4, " ", latitude, longitude));

        if (BuildConfig.FULL_VERSION) {
            mSunriseSunset.setLocation(latitude, longitude);
            double sunrise = mSunriseSunset.compute(true);
            double sunset = mSunriseSunset.compute(false);

            if (sunrise == Double.MAX_VALUE || sunset == Double.MAX_VALUE) {
                mSunrise.setText(R.string.never_rises);
                mSunsetTitle.setVisibility(View.GONE);
                mSunset.setVisibility(View.GONE);
            } else if (sunrise == Double.MIN_VALUE || sunset == Double.MIN_VALUE) {
                mSunset.setText(R.string.never_sets);
                mSunriseTitle.setVisibility(View.GONE);
                mSunrise.setVisibility(View.GONE);
            } else {
                mSunrise.setText(mSunriseSunset.formatTime(sunrise));
                mSunset.setText(mSunriseSunset.formatTime(sunset));
                mSunriseTitle.setVisibility(View.VISIBLE);
                mSunrise.setVisibility(View.VISIBLE);
                mSunsetTitle.setVisibility(View.VISIBLE);
                mSunset.setVisibility(View.VISIBLE);
            }

            mOffset.setText(StringFormatter.timeO((int) (mSunriseSunset.getUtcOffset() * 60)));

            GeomagneticField mag = new GeomagneticField((float) latitude, (float) longitude, 0.0f, System.currentTimeMillis());
            mDeclination.setText(String.format(Locale.getDefault(), "%+.1f\u00B0", mag.getDeclination()));
        }
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        //Log.w("LI", "C: " + (e == Map.CLEAR_EVENT) + " P: " + (e == Map.POSITION_EVENT) + " M: " + (e == Map.MOVE_EVENT) + " R: " + (e == Map.REDRAW_EVENT) + " U: " + (e == Map.UPDATE_EVENT));
        if (e == Map.POSITION_EVENT) {
            updateLocation(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.getZoomLevel());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0) {
            mTextInputDialog.setDescription("");
            return;
        }
        try {
            JosmCoordinatesParser.Result result = JosmCoordinatesParser.parseWithResult(s.toString());
            s.setSpan(
                    new ForegroundColorSpan(mColorDarkBlue),
                    0,
                    result.offset,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(
                    new ForegroundColorSpan(mColorTextPrimary),
                    result.offset,
                    s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextInputDialog.setDescription(StringFormatter.coordinates(" ", result.coordinates.getLatitude(), result.coordinates.getLongitude()));
        } catch (IllegalArgumentException e) {
            s.setSpan(
                    new ForegroundColorSpan(mColorRed),
                    0,
                    s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextInputDialog.setDescription("");
        }
    }

    @Override
    public void onTextInputPositiveClick(String id, String inputText) {
        mTextInputDialog = null;
        try {
            GeoPoint geoPoint = JosmCoordinatesParser.parse(inputText);
            mMapHolder.setMapLocation(geoPoint);
        } catch (IllegalArgumentException e) {
            HelperUtils.showError(getString(R.string.msgParseCoordinatesFailed), mFragmentHolder.getCoordinatorLayout());
        }
    }

    @Override
    public void onTextInputNegativeClick(String id) {
        mTextInputDialog = null;
    }

    @Override
    public void onLocationStateChanged(LocationState locationState) {
        int visibility = (locationState == LocationState.DISABLED) ? View.GONE : View.VISIBLE;
        TransitionManager.beginDelayedTransition(mRootView, new Fade());
        mSwitchOffButton.setVisibility(visibility);
    }
}
