package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;

import java.util.Locale;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;

public class LocationInformation extends Fragment implements Map.UpdateListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";

    private SunriseSunset mSunriseSunset;
    private ViewGroup mRootView;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e("LocationInformation", "onCreateView()");
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_location_information, container, false);
        if (BuildConfig.FULL_VERSION) {
            mRootView.findViewById(R.id.extendTable).setVisibility(View.VISIBLE);
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e("LocationInformation", "onActivityCreated()");

        double latitude = getArguments().getDouble(ARG_LATITUDE);
        double longitude = getArguments().getDouble(ARG_LONGITUDE);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
        }

        mSunriseSunset = new SunriseSunset();
        updateLocation(latitude, longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("LocationInformation", "onResume()");
        mMapHolder.getMap().events.bind(this);
        if (BuildConfig.FULL_VERSION) {
            HelperUtils.showAdvice(Configuration.ADVICE_SUNRISE_SUNSET, R.string.advice_sunrise_sunset, mFragmentHolder.getCoordinatorLayout());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("LocationInformation", "onPause()");
        mMapHolder.getMap().events.unbind(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e("LocationInformation", "onAttach()");
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e("LocationInformation", "onDetach()");
        mFragmentHolder = null;
        mMapHolder = null;
    }

    private void updateLocation(double latitude, double longitude) {
        ((TextView) mRootView.findViewById(R.id.coordinate_degree)).setText(StringFormatter.coordinates(0, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_degmin)).setText(StringFormatter.coordinates(1, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_degminsec)).setText(StringFormatter.coordinates(2, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_utmups)).setText(StringFormatter.coordinates(3, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_mgrs)).setText(StringFormatter.coordinates(4, " ", latitude, longitude));

        if (BuildConfig.FULL_VERSION) {
            mSunriseSunset.setLocation(latitude, longitude);
            double sunrise = mSunriseSunset.compute(true);
            double sunset = mSunriseSunset.compute(false);

            if (Double.isNaN(sunrise) || Double.isNaN(sunset)) {
                ((TextView) mRootView.findViewById(R.id.sunrise)).setText(R.string.never);
                ((TextView) mRootView.findViewById(R.id.sunset)).setText(R.string.never);
            } else {
                ((TextView) mRootView.findViewById(R.id.sunrise)).setText(mSunriseSunset.formatTime(sunrise));
                ((TextView) mRootView.findViewById(R.id.sunset)).setText(mSunriseSunset.formatTime(sunset));
            }

            ((TextView) mRootView.findViewById(R.id.offset)).setText(StringFormatter.timeR((int) (mSunriseSunset.getUtcOffset() * 60)));

            GeomagneticField mag = new GeomagneticField((float) latitude, (float) longitude, 0.0f, System.currentTimeMillis());
            ((TextView) mRootView.findViewById(R.id.declination)).setText(String.format(Locale.getDefault(), "%+.1f\u00B0", mag.getDeclination()));
        }
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        Log.w("LI", "C: " + (e == Map.CLEAR_EVENT) + " P: " + (e == Map.POSITION_EVENT) + " M: " + (e == Map.MOVE_EVENT) + " R: " + (e == Map.REDRAW_EVENT) + " U: " + (e == Map.UPDATE_EVENT));
        if (e == Map.POSITION_EVENT) {
            updateLocation(mapPosition.getLatitude(), mapPosition.getLongitude());
        }
    }
}
