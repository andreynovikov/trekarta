package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;

public class MarkerInformation extends Fragment implements OnBackPressedListener {
    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
    public static final String ARG_NAME = "name";

    private double mLatitude;
    private double mLongitude;
    private String mName;

    private OnWaypointActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marker_information, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLatitude = getArguments().getDouble(ARG_LATITUDE);
        mLongitude = getArguments().getDouble(ARG_LONGITUDE);
        mName = getArguments().getString(ARG_NAME);

        if (savedInstanceState != null) {
            mLatitude = savedInstanceState.getDouble(ARG_LATITUDE);
            mLongitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            mName = savedInstanceState.getString(ARG_NAME);
        }

        String name;
        if (mName != null && !"".equals(mName))
            name = mName;
        else
            name = StringFormatter.coordinates(" ", mLatitude, mLongitude);
        //noinspection ConstantConditions
        ((TextView) getView().findViewById(R.id.name)).setText(name);

        final GeoPoint point = new GeoPoint(mLatitude, mLongitude);
        mMapHolder.showMarker(point, name);

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_pin_drop));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name;
                if (mName != null && !"".equals(mName))
                    name = mName;
                else
                    name = getString(R.string.waypoint_name, Configuration.getPointsCounter());
                mListener.onWaypointCreate(point, name, true);
                mFragmentHolder.disableActionButton();
                mFragmentHolder.popCurrent();
            }
        });
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
        mListener = null;
        mMapHolder = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
        outState.putString(ARG_NAME, mName);
    }

    @Override
    public boolean onBackClick() {
        mFragmentHolder.disableActionButton();
        return false;
    }
}
