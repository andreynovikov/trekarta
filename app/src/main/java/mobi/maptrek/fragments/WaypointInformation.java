package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.DateFormat;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

public class WaypointInformation extends Fragment implements Map.UpdateListener, OnBackPressedListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_DETAILS = "details";

    //TODO Honor dpi
    final int SWIPE_MIN_DISTANCE = 120;
    final int SWIPE_MAX_OFF_PATH = 50;
    final int SWIPE_THRESHOLD_VELOCITY = 200;

    private Waypoint mWaypoint;
    private double mLatitude;
    private double mLongitude;

    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnWaypointActionListener mListener;
    private boolean mExpanded;
    private boolean mEditorMode;

    public WaypointInformation() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_waypoint_information, container, false);
        final ImageButton saveButton = (ImageButton) rootView.findViewById(R.id.saveButton);
        final ImageButton editButton = (ImageButton) rootView.findViewById(R.id.editButton);
        final ImageButton navigateButton = (ImageButton) rootView.findViewById(R.id.navigateButton);
        final ImageButton shareButton = (ImageButton) rootView.findViewById(R.id.shareButton);
        final ImageButton deleteButton = (ImageButton) rootView.findViewById(R.id.deleteButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWaypoint.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                mListener.onWaypointSave(mWaypoint);
                setEditorMode(false);
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditorMode(true);
            }
        });
        navigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                mListener.onWaypointNavigate(mWaypoint);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragmentHolder.disableActionButton();
                getFragmentManager().popBackStack();
                mListener.onWaypointShare(mWaypoint);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
                v.startAnimation(shake);
            }
        });
        deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mFragmentHolder.disableActionButton();
                getFragmentManager().popBackStack();
                mListener.onWaypointRemove(mWaypoint);
                return true;
            }
        });

        mExpanded = false;

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
                        if (!mExpanded && e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            expand();
                            updateWaypointInformation(mLatitude, mLongitude);
                        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            mFragmentHolder.disableActionButton();
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

        mEditorMode = false;

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        double latitude = getArguments().getDouble(ARG_LATITUDE);
        double longitude = getArguments().getDouble(ARG_LONGITUDE);
        boolean full = getArguments().getBoolean(ARG_DETAILS);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            full = savedInstanceState.getBoolean(ARG_DETAILS);
        }
        if (full)
            expand();

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
        mFragmentHolder = (FragmentHolder) context;
        mFragmentHolder.addBackClickListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
        outState.putBoolean(ARG_DETAILS, mExpanded);
    }

    private void expand() {
        final ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        //ChangeBounds changeBounds = new ChangeBounds();
        //changeBounds.setReparent(true);
        //changeBounds.excludeTarget(rootView, true);
        //changeBounds.excludeTarget(R.id.action_buttons, true);
        //changeBounds.addTarget(R.id.source);

        //TransitionSet set = new TransitionSet();
        //set.addTransition(new Fade());
        //set.addTransition(changeBounds);
        //set.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
        //set.excludeTarget(R.id.extend_table, true);
        //set.setDuration(3000);

        //TextView destination = ((TextView) rootView.findViewById(R.id.destination));
        //String dst = destination.getText().toString();
        //TransitionManager.beginDelayedTransition(rootView, set);

        rootView.findViewById(R.id.extendTable).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.dottedLine).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.source).setVisibility(View.GONE);
        rootView.findViewById(R.id.destination).setVisibility(View.GONE);

        rootView.findViewById(R.id.navigateButton).setVisibility(View.GONE);
        rootView.findViewById(R.id.editButton).setVisibility(View.VISIBLE);

        //destination = new TextView(getContext());
        //destination.setText(dst);
        //destination.setId(R.id.destination);
        //ViewGroup row = (ViewGroup) rootView.findViewById(R.id.destination_row);
        //row.addView(destination);
        //changeBounds.addTarget(destination);

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_navigate));
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorMode) {
                    mWaypoint.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                    mListener.onWaypointSave(mWaypoint);
                    setEditorMode(false);
                } else {
                    mFragmentHolder.disableActionButton();
                    getFragmentManager().popBackStack();
                    mListener.onWaypointNavigate(mWaypoint);
                }
            }
        });

        mMapHolder.updateMapViewArea();

        mExpanded = true;
    }

    public void setWaypoint(Waypoint waypoint) {
        mWaypoint = waypoint;
        if (isVisible()) {
            updateWaypointInformation(mLatitude, mLongitude);
        }
    }

    private void updateWaypointInformation(double latitude, double longitude) {
        Activity activity = getActivity();
        View view = getView();
        assert view != null;

        double dist = GeoPoint.distance(latitude, longitude, mWaypoint.latitude, mWaypoint.longitude);
        double bearing = GeoPoint.bearing(latitude, longitude, mWaypoint.latitude, mWaypoint.longitude);
        String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);

        TextView nameView = (TextView) view.findViewById(R.id.name);
        if (nameView != null)
            nameView.setText(mWaypoint.name);

        TextView sourceView = (TextView) view.findViewById(R.id.source);
        if (sourceView != null)
            sourceView.setText(R.string.waypoint_store_name);
        sourceView = (TextView) view.findViewById(R.id.sourceExtended);
        if (sourceView != null)
            sourceView.setText(R.string.waypoint_store_name);

        TextView destinationView = (TextView) view.findViewById(R.id.destination);
        if (destinationView != null)
            destinationView.setText(distance);
        destinationView = (TextView) view.findViewById(R.id.destinationExtended);
        if (destinationView != null)
            destinationView.setText(distance);

        final TextView coordsView = (TextView) view.findViewById(R.id.coordinates);
        if (coordsView != null) {
            coordsView.requestFocus();
            coordsView.setTag(StringFormatter.coordinateFormat);
            coordsView.setText(StringFormatter.coordinates(" ", mWaypoint.latitude, mWaypoint.longitude));
            coordsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int format = (Integer) coordsView.getTag() + 1;
                    if (format == 5)
                        format = 0;
                    coordsView.setText(StringFormatter.coordinates(format, " ", mWaypoint.latitude, mWaypoint.longitude));
                    coordsView.setTag(format);
                }
            });
        }

        TextView altitudeView = (TextView) view.findViewById(R.id.altitude);
        if (altitudeView != null) {
            if (mWaypoint.altitude != Integer.MIN_VALUE) {
                altitudeView.setText(getString(R.string.waypoint_altitude, StringFormatter.elevationH(mWaypoint.altitude)));
                altitudeView.setVisibility(View.VISIBLE);
            } else {
                altitudeView.setVisibility(View.GONE);
            }
        }

        TextView proximityView = (TextView) view.findViewById(R.id.proximity);
        if (proximityView != null) {
            if (mWaypoint.proximity > 0) {
                proximityView.setText(getString(R.string.waypoint_proximity, StringFormatter.distanceH(mWaypoint.proximity)));
                proximityView.setVisibility(View.VISIBLE);
            } else {
                proximityView.setVisibility(View.GONE);
            }
        }

        TextView dateView = (TextView) view.findViewById(R.id.date);
        if (dateView != null) {
            if (mWaypoint.date != null) {
                String date = DateFormat.getDateFormat(activity).format(mWaypoint.date);
                String time = DateFormat.getTimeFormat(activity).format(mWaypoint.date);
                dateView.setText(getString(R.string.datetime, date, time));
                view.findViewById(R.id.dateRow).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.dateRow).setVisibility(View.GONE);
            }
        }

        View row = view.findViewById(R.id.descriptionRow);
        if (row != null) {
            if (mWaypoint.description == null || "".equals(mWaypoint.description)) {
                view.findViewById(R.id.descriptionRow).setVisibility(View.GONE);
            } else {
                /*
                WebView description = (WebView) view.findViewById(R.id.description);
                String descriptionHtml;
                try {
                    TypedValue tv = new TypedValue();
                    Resources.Theme theme = activity.getTheme();
                    Resources resources = getResources();
                    theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true);
                    int secondaryColor = resources.getColor(tv.resourceId, theme);
                    String css = String.format("<style type=\"text/css\">html,body{margin:0;background:transparent} *{color:#%06X}</style>\n", (secondaryColor & 0x00FFFFFF));
                    descriptionHtml = css + mWaypoint.description;
                    description.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            view.setBackgroundColor(Color.TRANSPARENT);
                            view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                        }
                    });
                    description.setBackgroundColor(Color.TRANSPARENT);
                    description.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                } catch (Resources.NotFoundException e) {
                    description.setBackgroundColor(Color.LTGRAY);
                    descriptionHtml = mWaypoint.description;
                }

                WebSettings settings = description.getSettings();
                settings.setDefaultTextEncodingName("utf-8");
                settings.setAllowFileAccess(true);
                Uri baseUrl = Uri.fromFile(activity.getExternalFilesDir("data"));
                description.loadDataWithBaseURL(baseUrl.toString() + "/", descriptionHtml, "text/html", "utf-8", null);
                */
                view.findViewById(R.id.descriptionRow).setVisibility(View.VISIBLE);
            }
        }

        mLatitude = latitude;
        mLongitude = longitude;
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        int viewsState, editsState;
        if (enabled) {
            mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_done));
            ((EditText) rootView.findViewById(R.id.nameEdit)).setText(mWaypoint.name);
            viewsState = View.GONE;
            editsState = View.VISIBLE;
        } else {
            mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_navigate));
            ((TextView) rootView.findViewById(R.id.name)).setText(mWaypoint.name);
            viewsState = View.VISIBLE;
            editsState = View.GONE;
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        TransitionManager.beginDelayedTransition(rootView, new Fade());
        rootView.findViewById(R.id.name).setVisibility(viewsState);
        rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);

        rootView.findViewById(R.id.destinationRow).setVisibility(viewsState);

        rootView.findViewById(R.id.editButton).setVisibility(viewsState);
        rootView.findViewById(R.id.shareButton).setVisibility(viewsState);
        //rootView.findViewById(R.id.saveButton).setVisibility(editsState);

        mEditorMode = enabled;
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT && !mEditorMode) {
            updateWaypointInformation(mapPosition.getLatitude(), mapPosition.getLongitude());
        }
    }

    @Override
    public boolean onBackClick() {
        ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;
        if (mEditorMode) {
            setEditorMode(false);
            return true;
        } else {
            mFragmentHolder.disableActionButton();
            return false;
        }
    }
}