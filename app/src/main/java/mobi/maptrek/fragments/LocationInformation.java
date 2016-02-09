package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import mobi.maptrek.MainActivity;
import mobi.maptrek.R;
import mobi.maptrek.util.Astro;
import mobi.maptrek.util.StringFormatter;

public class LocationInformation extends Fragment implements Map.UpdateListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";

    private ViewGroup mRootView;

    MainActivity mActivity;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e("LocationInformation", "onCreateView()");
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_location_information, container, false);
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

        updateLocation(latitude, longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("LocationInformation", "onResume()");
        mActivity.getMap().events.bind(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("LocationInformation", "onPause()");
        mActivity.getMap().events.unbind(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e("LocationInformation", "onAttach()");
        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must be MainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e("LocationInformation", "onDetach()");
        mActivity = null;
    }

    private void updateLocation(double latitude, double longitude) {
        ((TextView) mRootView.findViewById(R.id.coordinate_degree)).setText(StringFormatter.coordinates(0, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_degmin)).setText(StringFormatter.coordinates(1, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_degminsec)).setText(StringFormatter.coordinates(2, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_utmups)).setText(StringFormatter.coordinates(3, " ", latitude, longitude));
        ((TextView) mRootView.findViewById(R.id.coordinate_mgrs)).setText(StringFormatter.coordinates(4, " ", latitude, longitude));

        Location loc = new Location("fake");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);

        Calendar now = GregorianCalendar.getInstance(TimeZone.getDefault());
        double sunrise = Astro.computeSunriseTime(Astro.Zenith.OFFICIAL, loc, now);
        double sunset = Astro.computeSunsetTime(Astro.Zenith.OFFICIAL, loc, now);

        if (Double.isNaN(sunrise))
        {
            ((TextView) mRootView.findViewById(R.id.sunrise)).setText(R.string.never);
        }
        else
        {
            ((TextView) mRootView.findViewById(R.id.sunrise)).setText(Astro.getLocalTimeAsString(sunrise));
        }
        if (Double.isNaN(sunset))
        {
            ((TextView) mRootView.findViewById(R.id.sunset)).setText(R.string.never);
        }
        else
        {
            ((TextView) mRootView.findViewById(R.id.sunset)).setText(Astro.getLocalTimeAsString(sunset));
        }
        GeomagneticField mag = new GeomagneticField((float) latitude, (float) longitude, 0.0f, System.currentTimeMillis());
        ((TextView) mRootView.findViewById(R.id.declination)).setText(String.format("%+.1f\u00B0", mag.getDeclination()));
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            updateLocation(mapPosition.getLatitude(), mapPosition.getLongitude());
        }
    }
}
