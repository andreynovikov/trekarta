/*
 * Copyright 2019 Andrey Novikov
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

    private Waypoint mWaypoint;
    private double mLatitude;
    private double mLongitude;

    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private WaypointBottomSheetCallback mBottomSheetCallback;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnWaypointActionListener mListener;
    private boolean mExpanded;
    private boolean mEditorMode;
    private boolean mPopAll;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_waypoint_information, container, false);
        final ImageButton editButton = rootView.findViewById(R.id.editButton);
        final ImageButton shareButton = rootView.findViewById(R.id.shareButton);
        final ImageButton deleteButton = rootView.findViewById(R.id.deleteButton);

        editButton.setOnClickListener(v -> setEditorMode(true));
        shareButton.setOnClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            mListener.onWaypointShare(mWaypoint);
        });
        deleteButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            v.startAnimation(shake);
        });
        deleteButton.setOnLongClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            mListener.onWaypointDelete(mWaypoint);
            return true;
        });

        mEditorMode = false;
        mPopAll = false;

        rootView.post(() -> {
            updatePeekHeight(rootView, false);
            mBottomSheetBehavior.setSkipCollapsed(mExpanded);
            int panelState = mExpanded ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED;
            if (savedInstanceState != null) {
                panelState = savedInstanceState.getInt("panelState", panelState);
                View dragHandle = rootView.findViewById(R.id.dragHandle);
                dragHandle.setAlpha(panelState == BottomSheetBehavior.STATE_EXPANDED ? 0f : 1f);
            }
            mBottomSheetBehavior.setState(panelState);
            // Workaround for panel partially drawn on first slide
            // TODO Try to put transparent view above map
            if (Configuration.getHideSystemUI())
                rootView.requestLayout();
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        double latitude = Double.NaN;
        double longitude = Double.NaN;

        boolean editorMode = false;
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            mExpanded = savedInstanceState.getBoolean(ARG_DETAILS);
            editorMode = savedInstanceState.getBoolean("editorMode");
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                latitude = arguments.getDouble(ARG_LATITUDE, Double.NaN);
                longitude = arguments.getDouble(ARG_LONGITUDE, Double.NaN);
                mExpanded = arguments.getBoolean(ARG_DETAILS);
            }
        }

        final ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

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
                if (mMapHolder.isNavigatingTo(mWaypoint.coordinates))
                    mMapHolder.stopNavigation();
                else
                    mListener.onWaypointNavigate(mWaypoint);
                mPopAll = true;
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
        p.setAnchorId(R.id.bottomSheetPanel);
        mFloatingButton.setLayoutParams(p);

        updateWaypointInformation(latitude, longitude);
        if (editorMode)
            setEditorMode(true);

        rootView.findViewById(R.id.dragHandle).setAlpha(mExpanded ? 0f : 1f);
        mBottomSheetCallback = new WaypointBottomSheetCallback();
        ViewParent parent = rootView.getParent();
        mBottomSheetBehavior = BottomSheetBehavior.from((View) parent);
        mBottomSheetBehavior.addBottomSheetCallback(mBottomSheetCallback);

        mListener.onWaypointFocus(mWaypoint);
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnWaypointActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnWaypointActionListener");
        }
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
            mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
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
    public void onDestroyView() {
        super.onDestroyView();
        mBottomSheetBehavior.removeBottomSheetCallback(mBottomSheetCallback);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
        outState.putBoolean(ARG_DETAILS, mExpanded);
        outState.putInt("panelState", mBottomSheetBehavior.getState());
        outState.putBoolean("editorMode", mEditorMode);
    }

    public void setWaypoint(Waypoint waypoint) {
        mWaypoint = waypoint;
        if (isVisible()) {
            if (mEditorMode)
                setEditorMode(false);
            mListener.onWaypointFocus(mWaypoint);
            updateWaypointInformation(mLatitude, mLongitude);
            final ViewGroup rootView = (ViewGroup) getView();
            if (rootView != null)
                rootView.post(() -> updatePeekHeight(rootView, true));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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

            setLockDrawable(coordsView);

            coordsView.setOnTouchListener((v, event) -> {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    if (event.getX() >= coordsView.getRight() - coordsView.getTotalPaddingRight()) {
                        // your action for drawable click event
                        mWaypoint.locked = !mWaypoint.locked;
                        mListener.onWaypointSave(mWaypoint);
                        mListener.onWaypointFocus(mWaypoint);
                        setLockDrawable(coordsView);
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

        mLatitude = latitude;
        mLongitude = longitude;
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        final ColorPickerSwatch colorSwatch = rootView.findViewById(R.id.colorSwatch);

        int viewsState, editsState;
        if (enabled) {
            mFloatingButton.setImageDrawable(AppCompatResources.getDrawable(rootView.getContext(), R.drawable.ic_done));
            ((EditText) rootView.findViewById(R.id.nameEdit)).setText(mWaypoint.name);
            ((EditText) rootView.findViewById(R.id.descriptionEdit)).setText(mWaypoint.description);
            colorSwatch.setColor(mWaypoint.style.color);
            colorSwatch.setOnClickListener(v -> {
                // TODO Implement class that hides this behaviour
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, colorSwatch.getColor());
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(colorSwatch::setColor);
                dialog.show(getParentFragmentManager(), "ColorPickerDialog");
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
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            rootView.post(() -> updatePeekHeight(rootView, false));
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

    private void setLockDrawable(TextView coordsView) {
        int imageResource = mWaypoint.locked ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open;
        Drawable drawable = AppCompatResources.getDrawable(coordsView.getContext(), imageResource);
        if (drawable != null) {
            int drawableSize = (int) Math.round(coordsView.getLineHeight() * 0.7);
            int drawablePadding = (int) (MapTrek.density * 1.5f);
            drawable.setBounds(0, drawablePadding, drawableSize, drawableSize + drawablePadding);
            int tintColor = mWaypoint.locked ? R.color.red : R.color.colorPrimaryDark;
            drawable.setTint(coordsView.getContext().getColor(tintColor));
            coordsView.setCompoundDrawables(null, null, drawable, null);
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
        Uri baseUrl = Uri.fromFile(getContext().getExternalFilesDir("data"));
        webView.loadDataWithBaseURL(baseUrl.toString() + "/", descriptionHtml, "text/html", "utf-8", null);
    }

    private void updatePeekHeight(ViewGroup rootView, boolean setState) {
        View dragHandle = rootView.findViewById(R.id.dragHandle);
        View nameView = rootView.findViewById(R.id.name);
        View sourceView = rootView.findViewById(R.id.source);
        mBottomSheetBehavior.setPeekHeight(dragHandle.getHeight() * 2 + nameView.getHeight() + sourceView.getHeight());
        if (setState)
            mBottomSheetBehavior.setState(mBottomSheetBehavior.getState());
    }

    @Override
    public boolean onBackClick() {
        if (mEditorMode)
            setEditorMode(false);
        else
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mEditorMode)
            updateWaypointInformation(location.getLatitude(), location.getLongitude());
    }

    private class WaypointBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                mBottomSheetBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                mFragmentHolder.disableActionButton();
                CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
                p.setAnchorId(R.id.contentPanel);
                mFloatingButton.setLayoutParams(p);
                mFloatingButton.setAlpha(1f);
                if (mPopAll)
                    mFragmentHolder.popAll();
                else
                    mFragmentHolder.popCurrent();
            }
            if (newState != BottomSheetBehavior.STATE_DRAGGING && newState != BottomSheetBehavior.STATE_SETTLING)
                mMapHolder.updateMapViewArea();
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                TextView coordsView = bottomSheet.findViewById(R.id.coordinates);
                if (!HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, coordsView, true)
                        && HelperUtils.needsTargetedAdvice(Configuration.ADVICE_LOCKED_COORDINATES)) {
                    Rect r = new Rect();
                    coordsView.getGlobalVisibleRect(r);
                    if (coordsView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                        r.left = r.right - coordsView.getTotalPaddingRight();
                    } else {
                        r.right = r.left + coordsView.getTotalPaddingLeft();
                    }
                    HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_LOCKED_COORDINATES, R.string.advice_locked_coordinates, r);
                }
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            if (!mExpanded)
                bottomSheet.findViewById(R.id.dragHandle).setAlpha(1f - slideOffset);
            mFloatingButton.setAlpha(1f + slideOffset);
        }
    }
}