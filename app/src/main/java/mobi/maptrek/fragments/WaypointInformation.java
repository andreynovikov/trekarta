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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.view.LimitedWebView;

public class WaypointInformation extends Fragment implements OnBackPressedListener, LocationChangeListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_DETAILS = "details";

    final int SWIPE_MIN_DISTANCE = (int) (40 * MapTrek.density); // vertical distance
    final int SWIPE_MAX_OFF_PATH = (int) (30 * MapTrek.density); // horizontal displacement during fling
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_waypoint_information, container, false);
        final ImageButton editButton = rootView.findViewById(R.id.editButton);
        final ImageButton navigateButton = rootView.findViewById(R.id.navigateButton);
        final ImageButton shareButton = rootView.findViewById(R.id.shareButton);
        final ImageButton deleteButton = rootView.findViewById(R.id.deleteButton);

        editButton.setOnClickListener(v -> setEditorMode(true));
        navigateButton.setOnClickListener(v -> onNavigate());
        shareButton.setOnClickListener(v -> {
            mFragmentHolder.disableActionButton();
            mFragmentHolder.popCurrent();
            mListener.onWaypointShare(mWaypoint);
        });
        deleteButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            v.startAnimation(shake);
        });
        deleteButton.setOnLongClickListener(v -> {
            mFragmentHolder.disableActionButton();
            mFragmentHolder.popCurrent();
            mListener.onWaypointDelete(mWaypoint);
            return true;
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
                        } else if (!mEditorMode && e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            mFragmentHolder.disableActionButton();
                            mFragmentHolder.popCurrent();
                        }
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });

        rootView.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));

        mEditorMode = false;

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        double latitude = getArguments().getDouble(ARG_LATITUDE, Double.NaN);
        double longitude = getArguments().getDouble(ARG_LONGITUDE, Double.NaN);
        boolean full = getArguments().getBoolean(ARG_DETAILS);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            full = savedInstanceState.getBoolean(ARG_DETAILS);
        }
        if (full)
            expand();

        mListener.onWaypointFocus(mWaypoint);
        updateWaypointInformation(latitude, longitude);
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
        mListener.onWaypointFocus(null);
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
        //TODO Preserve edit mode on rotation
    }

    private void expand() {
        final ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        rootView.findViewById(R.id.extendTable).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.dottedLine).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.source).setVisibility(View.GONE);
        TextView destination = rootView.findViewById(R.id.destination);
        destination.setTextAppearance(android.R.style.TextAppearance_Small);
        destination.setTextColor(getContext().getColor(R.color.colorAccent));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) destination.getLayoutParams();
        params.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.fragment_padding);
        params.bottomMargin = -params.topMargin;
        destination.setLayoutParams(params);

        rootView.findViewById(R.id.navigateButton).setVisibility(View.GONE);
        rootView.findViewById(R.id.editButton).setVisibility(View.VISIBLE);

        mFloatingButton = mFragmentHolder.enableActionButton();
        setFloatingPointDrawable();
        mFloatingButton.setOnClickListener(v -> {
            if (!isVisible())
                return;
            if (mEditorMode) {
                mWaypoint.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                mWaypoint.description = ((EditText) rootView.findViewById(R.id.descriptionEdit)).getText().toString();
                mWaypoint.style.color = ((ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch)).getColor();

                mListener.onWaypointSave(mWaypoint);
                mListener.onWaypointFocus(mWaypoint);
                setEditorMode(false);
            } else {
                mFragmentHolder.disableActionButton();
                onNavigate();
            }
        });

        mMapHolder.updateMapViewArea();

        mExpanded = true;
    }

    private void onNavigate() {
        if (mMapHolder.isNavigatingTo(mWaypoint.coordinates))
            mMapHolder.stopNavigation();
        else
            mListener.onWaypointNavigate(mWaypoint);
        mFragmentHolder.popAll();
    }

    public void setWaypoint(Waypoint waypoint) {
        mWaypoint = waypoint;
        if (isVisible()) {
            mListener.onWaypointFocus(mWaypoint);
            updateWaypointInformation(mLatitude, mLongitude);
        }
    }

    private void updateWaypointInformation(double latitude, double longitude) {
        Activity activity = getActivity();
        final View rootView = getView();
        assert rootView != null;

        TextView nameView = rootView.findViewById(R.id.name);
        if (nameView != null)
            nameView.setText(mWaypoint.name);

        TextView sourceView = rootView.findViewById(R.id.source);
        if (sourceView != null)
            sourceView.setText(mWaypoint.source.name);
        sourceView = rootView.findViewById(R.id.sourceExtended);
        if (sourceView != null)
            sourceView.setText(mWaypoint.source.name);

        TextView destinationView = rootView.findViewById(R.id.destination);
        if (destinationView != null) {
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                destinationView.setVisibility(View.GONE);
            } else {
                GeoPoint point = new GeoPoint(latitude, longitude);
                double dist = point.vincentyDistance(mWaypoint.coordinates);
                double bearing = point.bearingTo(mWaypoint.coordinates);
                String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
                destinationView.setVisibility(View.VISIBLE);
                destinationView.setText(distance);
            }
        }

        final TextView coordsView = rootView.findViewById(R.id.coordinates);
        if (coordsView != null) {
            coordsView.setText(StringFormatter.coordinates(" ", mWaypoint.coordinates.getLatitude(), mWaypoint.coordinates.getLongitude()));

            int imageResource = mWaypoint.locked ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open;
            Drawable drawable = activity.getDrawable(imageResource);
            if (drawable != null) {
                int drawableSize = (int) Math.round(coordsView.getLineHeight() * 0.7);
                int drawablePadding = (int) (MapTrek.density * 1.5f);
                drawable.setBounds(0, drawablePadding, drawableSize, drawableSize + drawablePadding);
                int tintColor = mWaypoint.locked ? R.color.red : R.color.colorPrimaryDark;
                drawable.setTint(activity.getColor(tintColor));
                coordsView.setCompoundDrawables(null, null, drawable, null);
            }

            coordsView.setOnTouchListener((v, event) -> {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    if (event.getX() >= coordsView.getRight() - coordsView.getTotalPaddingRight()) {
                        // your action for drawable click event
                        mWaypoint.locked = !mWaypoint.locked;
                        mListener.onWaypointSave(mWaypoint);
                        mListener.onWaypointFocus(mWaypoint);
                        updateWaypointInformation(mLatitude, mLongitude);
                        return true;
                    }
                }
                return false;
            });
            coordsView.setOnClickListener(v -> {
                StringFormatter.coordinateFormat++;
                if (StringFormatter.coordinateFormat == 5)
                    StringFormatter.coordinateFormat = 0;
                coordsView.setText(StringFormatter.coordinates(" ", mWaypoint.coordinates.getLatitude(), mWaypoint.coordinates.getLongitude()));
                Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
            });

            if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SWITCH_COORDINATES_FORMAT)
                    || HelperUtils.needsTargetedAdvice(Configuration.ADVICE_LOCKED_COORDINATES)) {
                ViewTreeObserver vto = rootView.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Rect r = new Rect();
                        coordsView.getGlobalVisibleRect(r);
                        if (!HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, r)) {
                            if (coordsView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                                r.left = r.right - coordsView.getTotalPaddingRight();
                            } else {
                                r.right = r.left + coordsView.getTotalPaddingLeft();
                            }
                            HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_LOCKED_COORDINATES, R.string.advice_locked_coordinates, r);
                        }
                    }
                });
            }
        }

        TextView altitudeView = rootView.findViewById(R.id.altitude);
        if (altitudeView != null) {
            if (mWaypoint.altitude != Integer.MIN_VALUE) {
                altitudeView.setText(getString(R.string.place_altitude, StringFormatter.elevationH(mWaypoint.altitude)));
                altitudeView.setVisibility(View.VISIBLE);
            } else {
                altitudeView.setVisibility(View.GONE);
            }
        }

        TextView proximityView = rootView.findViewById(R.id.proximity);
        if (proximityView != null) {
            if (mWaypoint.proximity > 0) {
                proximityView.setText(getString(R.string.place_proximity, StringFormatter.distanceH(mWaypoint.proximity)));
                proximityView.setVisibility(View.VISIBLE);
            } else {
                proximityView.setVisibility(View.GONE);
            }
        }

        TextView dateView = rootView.findViewById(R.id.date);
        if (dateView != null) {
            if (mWaypoint.date != null) {
                String date = DateFormat.getDateFormat(activity).format(mWaypoint.date);
                String time = DateFormat.getTimeFormat(activity).format(mWaypoint.date);
                dateView.setText(getString(R.string.datetime, date, time));
                rootView.findViewById(R.id.dateRow).setVisibility(View.VISIBLE);
            } else {
                rootView.findViewById(R.id.dateRow).setVisibility(View.GONE);
            }
        }

        final ViewGroup row = rootView.findViewById(R.id.descriptionRow);
        if (row != null) {
            if (mWaypoint.description == null || "".equals(mWaypoint.description)) {
                row.setVisibility(View.GONE);
            } else {
                setDescription(rootView);
                row.setVisibility(View.VISIBLE);
            }
        }

        if (mMapHolder.isNavigatingTo(mWaypoint.coordinates)) {
            ImageButton navigateButton = rootView.findViewById(R.id.navigateButton);
            navigateButton.setImageResource(R.drawable.ic_navigation_off);
        }

        mLatitude = latitude;
        mLongitude = longitude;
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        final ColorPickerSwatch colorSwatch = rootView.findViewById(R.id.colorSwatch);

        int viewsState, editsState;
        if (enabled) {
            mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_done));
            ((EditText) rootView.findViewById(R.id.nameEdit)).setText(mWaypoint.name);
            ((EditText) rootView.findViewById(R.id.descriptionEdit)).setText(mWaypoint.description);
            colorSwatch.setColor(mWaypoint.style.color);
            colorSwatch.setOnClickListener(v -> {
                // TODO Implement class that hides this behaviour
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, colorSwatch.getColor());
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(colorSwatch::setColor);
                dialog.show(getFragmentManager(), "ColorPickerDialog");
            });
            viewsState = View.GONE;
            editsState = View.VISIBLE;

            if (mWaypoint.source instanceof FileDataSource)
                HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, mFloatingButton, false);
        } else {
            setFloatingPointDrawable();
            ((TextView) rootView.findViewById(R.id.name)).setText(mWaypoint.name);
            setDescription(rootView);
            viewsState = View.VISIBLE;
            editsState = View.GONE;
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
        }
        // TODO Optimize view findings
        TransitionManager.beginDelayedTransition(rootView, new Fade());
        rootView.findViewById(R.id.name).setVisibility(viewsState);
        rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);
        if (enabled || mWaypoint.description != null && !"".equals(mWaypoint.description))
            rootView.findViewById(R.id.descriptionRow).setVisibility(View.VISIBLE);
        else
            rootView.findViewById(R.id.descriptionRow).setVisibility(View.GONE);
        rootView.findViewById(R.id.description).setVisibility(viewsState);
        rootView.findViewById(R.id.descriptionWrapper).setVisibility(editsState);
        colorSwatch.setVisibility(editsState);

        if (!Double.isNaN(mLatitude) && !Double.isNaN(mLongitude)) {
            rootView.findViewById(R.id.destination).setVisibility(viewsState);
        }
        if (mWaypoint.date != null) {
            rootView.findViewById(R.id.dateRow).setVisibility(viewsState);
        }
        rootView.findViewById(R.id.editButton).setVisibility(viewsState);
        rootView.findViewById(R.id.shareButton).setVisibility(viewsState);

        mEditorMode = enabled;
    }

    private void setFloatingPointDrawable() {
        if (mMapHolder.isNavigatingTo(mWaypoint.coordinates)) {
            mFloatingButton.setImageResource(R.drawable.ic_navigation_off);
        } else {
            mFloatingButton.setImageResource(R.drawable.ic_navigate);
        }
    }

    // WebView is very heavy to initialize. That's why it is used only on demand.
    private void setDescription(View rootView) {
        View description = rootView.findViewById(R.id.description);
        // TODO Use better approach (http://stackoverflow.com/a/22581832/488489)
        if (description instanceof LimitedWebView) {
            setWebViewText((LimitedWebView) description);
        } else if (mWaypoint.description != null && mWaypoint.description.contains("<") && mWaypoint.description.contains(">")) {
            // Replace TextView with WebView
            ViewGroup parent = (ViewGroup) description.getParent();
            int index = parent.indexOfChild(description);
            parent.removeView(description);
            LimitedWebView webView = new LimitedWebView(getContext());
            webView.setId(R.id.description);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            webView.setMaxHeight(px);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            webView.setLayoutParams(params);
            parent.addView(webView, index);
            setWebViewText(webView);
        } else {
            ((TextView) description).setText(mWaypoint.description);
            ((TextView) description).setMovementMethod(new ScrollingMovementMethod());
        }
    }

    private void setWebViewText(LimitedWebView webView) {
        String css = "<style type=\"text/css\">html,body{margin:0}</style>\n";
        String descriptionHtml = css + mWaypoint.description;
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null); // flicker workaround
        WebSettings settings = webView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setAllowFileAccess(true);
        Uri baseUrl = Uri.fromFile(MapTrek.getApplication().getExternalDir("data"));
        webView.loadDataWithBaseURL(baseUrl.toString() + "/", descriptionHtml, "text/html", "utf-8", null);
    }

    @Override
    public boolean onBackClick() {
        if (mEditorMode) {
            setEditorMode(false);
            return true;
        } else {
            mFragmentHolder.disableActionButton();
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mEditorMode)
            updateWaypointInformation(location.getLatitude(), location.getLongitude());
    }
}