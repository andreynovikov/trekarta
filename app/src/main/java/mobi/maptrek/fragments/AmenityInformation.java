package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.util.StringFormatter;

public class AmenityInformation extends Fragment implements OnBackPressedListener, LocationChangeListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_LANG = "lang";

    private Waypoint mWaypoint;
    private double mLatitude;
    private double mLongitude;
    private int mLang;

    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_amenity_information, container, false);

        mMapHolder.updateMapViewArea();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        double latitude = getArguments().getDouble(ARG_LATITUDE, Double.NaN);
        double longitude = getArguments().getDouble(ARG_LONGITUDE, Double.NaN);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            mLang = savedInstanceState.getInt(ARG_LANG);
        }

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageResource(R.drawable.ic_navigate);
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragmentHolder.disableActionButton();
                mMapHolder.navigateTo(mWaypoint.coordinates, mWaypoint.name);
                mFragmentHolder.popAll();
            }
        });

        mMapHolder.showMarker(mWaypoint.coordinates, mWaypoint.name);
        updateAmenityInformation(latitude, longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapHolder.addLocationChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapHolder.removeLocationChangeListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
            mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapHolder.removeMarker();
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
        outState.putInt(ARG_LANG, mLang);
    }

    public void setAmenity(long id) {
        mWaypoint = MapTrekDatabaseHelper.getAmenityData(mLang, id, MapTrek.getApplication().getDetailedMapDatabase());
        if (isVisible()) {
            mMapHolder.showMarker(mWaypoint.coordinates, mWaypoint.name);
            updateAmenityInformation(mLatitude, mLongitude);
        }
    }

    private void updateAmenityInformation(double latitude, double longitude) {
        final View rootView = getView();
        assert rootView != null;
        final Activity activity = getActivity();

        TextView nameView = (TextView) rootView.findViewById(R.id.name);
        if (nameView != null)
            nameView.setText(mWaypoint.name);

        View kindRow = rootView.findViewById(R.id.kindRow);
        if ("".equals(mWaypoint.description)) {
            if (kindRow != null)
                kindRow.setVisibility(View.GONE);
        } else {
            if (kindRow != null)
                kindRow.setVisibility(View.VISIBLE);
            TextView kindView = (TextView) rootView.findViewById(R.id.kind);
            if (kindView != null) {
                Resources resources = activity.getResources();
                int id = resources.getIdentifier(mWaypoint.description, "string", activity.getPackageName());
                kindView.setText(resources.getString(id));
            }
        }
        ImageView iconView = (ImageView) rootView.findViewById(R.id.icon);
        if (iconView != null) {
            @DrawableRes int icon = ResUtils.getKindIcon(mWaypoint.proximity);
            if (icon == 0)
                icon = R.drawable.ic_place;
            iconView.setImageResource(icon);
        }

        TextView destinationView = (TextView) rootView.findViewById(R.id.destination);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            if (destinationView != null)
                destinationView.setVisibility(View.GONE);
        } else {
            GeoPoint point = new GeoPoint(latitude, longitude);
            double dist = point.vincentyDistance(mWaypoint.coordinates);
            double bearing = point.bearingTo(mWaypoint.coordinates);
            String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
            if (destinationView != null) {
                destinationView.setVisibility(View.VISIBLE);
                destinationView.setTag(true);
                destinationView.setText(distance);
            }
        }

        final TextView coordsView = (TextView) rootView.findViewById(R.id.coordinates);
        if (coordsView != null) {
            coordsView.setText(StringFormatter.coordinates(" ", mWaypoint.coordinates.getLatitude(), mWaypoint.coordinates.getLongitude()));

            if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SWITCH_COORDINATES_FORMAT)) {
                // We need this very bulky code to wait until layout is settled and keyboard is completely hidden
                // otherwise we get wrong position for advice
                ViewTreeObserver vto = rootView.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        rootView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isVisible()) {
                                    Rect r = new Rect();
                                    coordsView.getGlobalVisibleRect(r);
                                    HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, r);
                                }
                            }
                        }, 1000);
                    }
                });
            }

            coordsView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getX() >= coordsView.getRight() - coordsView.getTotalPaddingRight()) {
                            mMapHolder.shareLocation(mWaypoint.coordinates, mWaypoint.name);
                        } else {
                            StringFormatter.coordinateFormat++;
                            if (StringFormatter.coordinateFormat == 5)
                                StringFormatter.coordinateFormat = 0;
                            coordsView.setText(StringFormatter.coordinates(" ", mWaypoint.coordinates.getLatitude(), mWaypoint.coordinates.getLongitude()));
                            Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                        }
                    }
                    return true;
                }
            });
        }

        TextView elevationView = (TextView) rootView.findViewById(R.id.elevation);
        if (elevationView != null) {
            if (mWaypoint.altitude != Integer.MIN_VALUE) {
                elevationView.setText(getString(R.string.waypoint_altitude, StringFormatter.elevationH(mWaypoint.altitude)));
                elevationView.setVisibility(View.VISIBLE);
            } else {
                elevationView.setVisibility(View.GONE);
            }
        }

        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public boolean onBackClick() {
        mFragmentHolder.disableActionButton();
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        updateAmenityInformation(location.getLatitude(), location.getLongitude());
    }

    public void setPreferredLanguage(String lang) {
        mLang = MapTrekDatabaseHelper.getLanguageId(lang);
    }
}