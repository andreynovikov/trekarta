package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.util.StringFormatter;

public class WaypointInformation extends Fragment implements Map.UpdateListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";

    //TODO Honor dpi
    final int SWIPE_MIN_DISTANCE = 120;
    final int SWIPE_MAX_OFF_PATH = 50;
    final int SWIPE_THRESHOLD_VELOCITY = 200;

    private TextView mNameView;
    private TextView mSourceView;
    private TextView mDestinationView;
    private ImageButton mNavigateButton;
    private ImageButton mShareButton;
    private ImageButton mDeleteButton;

    private Waypoint mWaypoint;
    private double mLatitude;
    private double mLongitude;

    private MapHolder mMapHolder;
    private OnWaypointActionListener mListener;

    public WaypointInformation() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_waypoint_information, container, false);
        mNameView = (TextView) rootView.findViewById(R.id.nameView);
        mSourceView = (TextView) rootView.findViewById(R.id.sourceView);
        mDestinationView = (TextView) rootView.findViewById(R.id.destinationView);
        mNavigateButton = (ImageButton) rootView.findViewById(R.id.navigateButton);
        mShareButton = (ImageButton) rootView.findViewById(R.id.shareButton);
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.deleteButton);

        mNavigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                mListener.onWaypointNavigate(mWaypoint);
            }
        });
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                mListener.onWaypointShare(mWaypoint);
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                mListener.onWaypointRemove(mWaypoint);
            }
        });

        final GestureDetector gesture = new GestureDetector(getActivity(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
                            return false;
                        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("WI", "Bottom to Top");
                        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            getFragmentManager().popBackStack();
                        }
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gesture.onTouchEvent(event);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        double latitude = getArguments().getDouble(ARG_LATITUDE);
        double longitude = getArguments().getDouble(ARG_LONGITUDE);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
        }

        updateWaypointInformation(latitude, longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapHolder.getMap().events.bind(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapHolder.getMap().events.unbind(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnWaypointActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointActionListener");
        }
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mMapHolder = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
    }

    public void setWaypoint(Waypoint waypoint) {
        mWaypoint = waypoint;
        if (isVisible()) {
            updateWaypointInformation(mLatitude, mLongitude);
        }
    }

    private void updateWaypointInformation(double latitude, double longitude) {
        mNameView.setText(mWaypoint.name);
        mSourceView.setText("My places");

        double dist = GeoPoint.distance(latitude, longitude, mWaypoint.latitude, mWaypoint.longitude);
        double bearing = GeoPoint.bearing(latitude, longitude, mWaypoint.latitude, mWaypoint.longitude);
        String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
        mDestinationView.setText(distance);

        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            updateWaypointInformation(mapPosition.getLatitude(), mapPosition.getLongitude());
        }
    }
}