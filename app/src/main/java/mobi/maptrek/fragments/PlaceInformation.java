/*
 * Copyright 2024 Andrey Novikov
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.text.TextUtilsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.github.DetectHtml;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;
import org.nibor.autolink.Span;
import org.oscim.core.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Place;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.databinding.FragmentPlaceInformationBinding;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.view.LimitedWebView;

public class PlaceInformation extends Fragment implements LocationChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(PlaceInformation.class);

    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_DETAILS = "details";

    private Place mPlace;
    private double mLatitude;
    private double mLongitude;

    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private PlaceBottomSheetCallback mBottomSheetCallback;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnPlaceActionListener mListener;
    private boolean mExpanded;
    private boolean mPopAll;
    private PlaceInformationViewModel viewModel;
    private FragmentPlaceInformationBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentPlaceInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PlaceInformationViewModel.class);
        viewModel.editorMode.observe(getViewLifecycleOwner(), editorModeObserver);

        viewBinding.editButton.setOnClickListener(v -> viewModel.editorMode.setValue(true));
        viewBinding.shareButton.setOnClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            mListener.onPlaceShare(mPlace);
        });
        viewBinding.deleteButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            v.startAnimation(shake);
        });
        viewBinding.deleteButton.setOnLongClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            mListener.onPlaceDelete(mPlace);
            return true;
        });

        mPopAll = false;

        view.post(() -> {
            updatePeekHeight(false);
            mBottomSheetBehavior.setSkipCollapsed(mExpanded);
            int panelState = mExpanded ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED;
            if (savedInstanceState != null) {
                panelState = savedInstanceState.getInt("panelState", panelState);
                View dragHandle = view.findViewById(R.id.dragHandle);
                dragHandle.setAlpha(panelState == BottomSheetBehavior.STATE_EXPANDED ? 0f : 1f);
            }
            mBottomSheetBehavior.setState(panelState);
            // Workaround for panel partially drawn on first slide
            // TODO Try to put transparent view above map
            if (Configuration.getHideSystemUI())
                view.requestLayout();
        });

        double latitude = Double.NaN;
        double longitude = Double.NaN;

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
            mExpanded = savedInstanceState.getBoolean(ARG_DETAILS);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                latitude = arguments.getDouble(ARG_LATITUDE, Double.NaN);
                longitude = arguments.getDouble(ARG_LONGITUDE, Double.NaN);
                mExpanded = arguments.getBoolean(ARG_DETAILS);
            }
        }

        mFloatingButton = mFragmentHolder.enableActionButton();
        setFloatingPointDrawable();
        mFloatingButton.setOnClickListener(v -> {
            if (!isVisible())
                return;
            if (Boolean.TRUE.equals(viewModel.editorMode.getValue())) {
                mPlace.name = viewBinding.nameEdit.getText().toString();
                mPlace.description = viewBinding.descriptionEdit.getText().toString();
                mPlace.style.color = viewBinding.colorSwatch.getColor();

                mListener.onPlaceSave(mPlace);
                mListener.onPlaceFocus(mPlace);
                viewModel.editorMode.setValue(false);
            } else {
                if (mMapHolder.isNavigatingTo(mPlace.coordinates))
                    mMapHolder.stopNavigation();
                else
                    mListener.onPlaceNavigate(mPlace);
                mPopAll = true;
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
        p.setAnchorId(R.id.bottomSheetPanel);
        mFloatingButton.setLayoutParams(p);

        updatePlaceInformation(latitude, longitude);

        viewBinding.dragHandle.setAlpha(mExpanded ? 0f : 1f);
        mBottomSheetCallback = new PlaceBottomSheetCallback();
        mBottomSheetBehavior = BottomSheetBehavior.from((View) viewBinding.getRoot().getParent());
        mBottomSheetBehavior.addBottomSheetCallback(mBottomSheetCallback);

        mListener.onPlaceFocus(mPlace);
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
            mListener = (OnPlaceActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnPlaceActionListener");
        }
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
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackPressedCallback.remove();
        mListener.onPlaceFocus(null);
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
    }

    public void setPlace(Place place) {
        mPlace = place;
        if (isVisible()) {
            if (Boolean.TRUE.equals(viewModel.editorMode.getValue()))
                viewModel.editorMode.setValue(false);
            mListener.onPlaceFocus(mPlace);
            updatePlaceInformation(mLatitude, mLongitude);
            viewBinding.getRoot().post(() -> updatePeekHeight(true));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void updatePlaceInformation(double latitude, double longitude) {
        Activity activity = requireActivity();

        viewBinding.name.setText(mPlace.name);
        viewBinding.source.setText(mPlace.source.name);

        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            viewBinding.destination.setVisibility(View.GONE);
        } else {
            GeoPoint point = new GeoPoint(latitude, longitude);
            double dist = point.vincentyDistance(mPlace.coordinates);
            double bearing = point.bearingTo(mPlace.coordinates);
            String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
            viewBinding.destination.setVisibility(View.VISIBLE);
            viewBinding.destination.setText(distance);
        }

        viewBinding.coordinates.setText(StringFormatter.coordinates(" ", mPlace.coordinates.getLatitude(), mPlace.coordinates.getLongitude()));
        setLockDrawable();

        viewBinding.coordinates.setOnTouchListener((v, event) -> {
            if (event.getX() >= viewBinding.coordinates.getRight() - viewBinding.coordinates.getTotalPaddingRight()) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    mPlace.locked = !mPlace.locked;
                    mListener.onPlaceSave(mPlace);
                    mListener.onPlaceFocus(mPlace);
                    setLockDrawable();
                }
                return true;
            }
            return false;
        });
        viewBinding.coordinates.setOnClickListener(v -> {
            StringFormatter.coordinateFormat++;
            if (StringFormatter.coordinateFormat == 5)
                StringFormatter.coordinateFormat = 0;
            viewBinding.coordinates.setText(StringFormatter.coordinates(" ", mPlace.coordinates.getLatitude(), mPlace.coordinates.getLongitude()));
            Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
        });

        if (mPlace.altitude != Integer.MIN_VALUE) {
            viewBinding.altitude.setText(getString(R.string.place_altitude, StringFormatter.elevationH(mPlace.altitude)));
            viewBinding.altitude.setVisibility(View.VISIBLE);
        } else {
            viewBinding.altitude.setVisibility(View.GONE);
        }

        if (mPlace.proximity > 0) {
            viewBinding.proximity.setText(getString(R.string.place_proximity, StringFormatter.distanceH(mPlace.proximity)));
            viewBinding.proximity.setVisibility(View.VISIBLE);
        } else {
            viewBinding.proximity.setVisibility(View.GONE);
        }

        if (mPlace.date != null) {
            String date = DateFormat.getDateFormat(activity).format(mPlace.date);
            String time = DateFormat.getTimeFormat(activity).format(mPlace.date);
            viewBinding.date.setText(getString(R.string.datetime, date, time));
            viewBinding.dateRow.setVisibility(View.VISIBLE);
        } else {
            viewBinding.dateRow.setVisibility(View.GONE);
        }

        setDescription();

        mLatitude = latitude;
        mLongitude = longitude;
    }

    private final Observer<Boolean> editorModeObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean enabled) {
            Activity activity = requireActivity();

            int viewsState, editsState;
            if (enabled) {
                mFloatingButton.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_done));
                viewBinding.nameEdit.setText(mPlace.name);
                viewBinding.descriptionEdit.setText(mPlace.description);
                viewBinding.colorSwatch.setColor(mPlace.style.color);
                viewBinding.colorSwatch.setOnClickListener(v -> {
                    // TODO Implement class that hides this behaviour
                    ColorPickerDialog dialog = new ColorPickerDialog();
                    dialog.setColors(MarkerStyle.DEFAULT_COLORS, viewBinding.colorSwatch.getColor());
                    dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                    dialog.setOnColorSelectedListener(viewBinding.colorSwatch::setColor);
                    dialog.show(getParentFragmentManager(), "ColorPickerDialog");
                });
                viewsState = View.GONE;
                editsState = View.VISIBLE;

                if (mPlace.source instanceof FileDataSource)
                    HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, mFloatingButton, false);
            } else {
                setFloatingPointDrawable();
                viewBinding.name.setText(mPlace.name);
                setDescription();
                viewsState = View.VISIBLE;
                editsState = View.GONE;
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(viewBinding.getRoot().getWindowToken(), 0);
                viewBinding.getRoot().post(() -> updatePeekHeight(false));
            }
            if (!enabled) // when enabled, delayed transition is initiated by list FAB
                TransitionManager.beginDelayedTransition(viewBinding.getRoot(), new Fade());
            viewBinding.name.setVisibility(viewsState);
            viewBinding.nameWrapper.setVisibility(editsState);
            if (enabled || mPlace.description != null && !"".equals(mPlace.description))
                viewBinding.descriptionRow.setVisibility(View.VISIBLE);
            else
                viewBinding.descriptionRow.setVisibility(View.GONE);
            viewBinding.getRoot().findViewById(R.id.description).setVisibility(viewsState); // it can be substituted, we can not reference it directly
            viewBinding.descriptionWrapper.setVisibility(editsState);
            viewBinding.colorSwatch.setVisibility(editsState);

            if (!Double.isNaN(mLatitude) && !Double.isNaN(mLongitude)) {
                viewBinding.destination.setVisibility(viewsState);
            }
            if (mPlace.date != null) {
                viewBinding.dateRow.setVisibility(viewsState);
            }
            viewBinding.source.setVisibility(viewsState);
            viewBinding.coordinatesRow.setVisibility(viewsState);
            viewBinding.descriptionIcon.setVisibility(viewsState);
            viewBinding.editButton.setVisibility(viewsState);
            viewBinding.shareButton.setVisibility(viewsState);
        }
    };

    private void setFloatingPointDrawable() {
        if (mMapHolder.isNavigatingTo(mPlace.coordinates)) {
            mFloatingButton.setImageResource(R.drawable.ic_navigation_off);
        } else {
            mFloatingButton.setImageResource(R.drawable.ic_navigate);
        }
    }

    private void setLockDrawable() {
        int imageResource = mPlace.locked ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open;
        Drawable drawable = AppCompatResources.getDrawable(viewBinding.coordinates.getContext(), imageResource);
        if (drawable != null) {
            int drawableSize = (int) Math.round(viewBinding.coordinates.getLineHeight() * 0.7);
            int drawablePadding = (int) (MapTrek.density * 1.5f);
            drawable.setBounds(0, drawablePadding, drawableSize, drawableSize + drawablePadding);
            int tintColor = mPlace.locked ? R.color.red : R.color.colorPrimaryDark;
            drawable.setTint(viewBinding.coordinates.getContext().getColor(tintColor));
            viewBinding.coordinates.setCompoundDrawables(null, null, drawable, null);
        }
    }

    // WebView is very heavy to initialize. That's why it is used only on demand.
    private void setDescription() {
        if (mPlace.description == null || "".equals(mPlace.description)) {
            viewBinding.descriptionRow.setVisibility(View.GONE);
            return;
        }
        viewBinding.descriptionRow.setVisibility(View.VISIBLE);

        String text = mPlace.description;
        boolean hasHTML = false;
        if (DetectHtml.isHtml(mPlace.description)) {
            hasHTML = true;
        } else {
            String marked = extractLinks(mPlace.description);
            if (marked != null) { // links found
                text = marked;
                hasHTML = true;
            }
        }
        View descriptionView = viewBinding.getRoot().findViewById(R.id.description); // it can be substituted, we can not reference it directly
        if (descriptionView instanceof LimitedWebView) {
            setWebViewText((LimitedWebView) descriptionView, text);
        } else if (hasHTML) {
            // Replace TextView with WebView
            convertToWebView(descriptionView, text);
        } else {
            ((TextView) descriptionView).setText(text);
            ((TextView) descriptionView).setMovementMethod(new ScrollingMovementMethod());
        }
    }

    private String extractLinks(String input) {
        LinkExtractor linkExtractor = LinkExtractor.builder().linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW)).build();
        Iterable<Span> spans = linkExtractor.extractSpans(input);

        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for (Span span : spans) {
            String text = input.substring(span.getBeginIndex(), span.getEndIndex());
            if (span instanceof LinkSpan) {
                sb.append("<a href=\"");
                sb.append(TextUtilsCompat.htmlEncode(text));
                sb.append("\">");
                sb.append(TextUtilsCompat.htmlEncode(text));
                sb.append("</a>");
                found = true;
            } else {
                sb.append(TextUtilsCompat.htmlEncode(text));
            }
        }
        return found ? sb.toString() : null;
    }

    private void convertToWebView(View description, String text) {
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
        setWebViewText(webView, text);
    }

    private void setWebViewText(LimitedWebView webView, String text) {
        logger.debug("[[[[{}]]]]", text);
        String css = "<style type=\"text/css\">html,body{margin:0}</style>\n";
        String descriptionHtml = css + text;
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null); // flicker workaround
        WebSettings settings = webView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setAllowFileAccess(true);
        Uri baseUrl = Uri.fromFile(requireContext().getExternalFilesDir("data"));
        webView.loadDataWithBaseURL(baseUrl.toString() + "/", descriptionHtml, "text/html", "utf-8", null);
    }

    private void updatePeekHeight(boolean setState) {
        mBottomSheetBehavior.setPeekHeight(viewBinding.dragHandle.getHeight() * 2 + viewBinding.name.getHeight() + viewBinding.source.getHeight());
        if (setState)
            mBottomSheetBehavior.setState(mBottomSheetBehavior.getState());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (Boolean.FALSE.equals(viewModel.editorMode.getValue()))
            updatePlaceInformation(location.getLatitude(), location.getLongitude());
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (Boolean.TRUE.equals(viewModel.editorMode.getValue()))
                viewModel.editorMode.setValue(false);
            else
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    };

    private class PlaceBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
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
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                Activity activity = requireActivity();
                if (!HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, viewBinding.coordinates, true)
                        && HelperUtils.needsTargetedAdvice(Configuration.ADVICE_LOCKED_COORDINATES)) {
                    Rect r = new Rect();
                    viewBinding.coordinates.getGlobalVisibleRect(r);
                    if (viewBinding.coordinates.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                        r.left = r.right - viewBinding.coordinates.getTotalPaddingRight();
                    } else {
                        r.right = r.left + viewBinding.coordinates.getTotalPaddingLeft();
                    }
                    HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_LOCKED_COORDINATES, R.string.advice_locked_coordinates, r);
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

    public static class PlaceInformationViewModel extends ViewModel {
        private final MutableLiveData<Boolean> editorMode = new MutableLiveData<>(false);
    }
}