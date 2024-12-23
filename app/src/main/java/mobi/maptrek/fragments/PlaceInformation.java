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

import android.text.Editable;
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
import mobi.maptrek.viewmodels.MapViewModel;
import mobi.maptrek.viewmodels.PlaceViewModel;

public class PlaceInformation extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(PlaceInformation.class);

    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private PlaceBottomSheetCallback mBottomSheetCallback;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnPlaceActionListener mListener;
    private boolean popAll;
    private PlaceViewModel placeViewModel;
    private PlaceInformationViewModel viewModel;
    private FragmentPlaceInformationBinding viewBinding;
    private int panelState;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentPlaceInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);
        placeViewModel.selectedPlace.observe(getViewLifecycleOwner(), place -> {
            if (place != null) {
                updatePlaceInformation(place);
                mListener.onPlaceFocus(place);
                requireView().post(this::updatePeekHeight);
            }
        });

        MapViewModel mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        mapViewModel.currentLocation.observe(getViewLifecycleOwner(), location -> {
            if ("unknown".equals(location.getProvider())) {
                viewBinding.destination.setVisibility(View.GONE);
                viewModel.showDestination = false;
            } else {
                Place place = placeViewModel.selectedPlace.getValue();
                if (place == null)
                    return;
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                double dist = point.vincentyDistance(place.coordinates);
                double bearing = point.bearingTo(place.coordinates);
                String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
                viewBinding.destination.setVisibility(View.VISIBLE);
                viewBinding.destination.setTag(true);
                viewBinding.destination.setText(distance);
                viewModel.showDestination = true;
            }
        });

        viewModel = new ViewModelProvider(this).get(PlaceInformationViewModel.class);
        viewModel.editorMode.observe(getViewLifecycleOwner(), editorModeObserver);

        viewBinding.editButton.setOnClickListener(v -> viewModel.editorMode.setValue(true));
        viewBinding.shareButton.setOnClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            Place place = placeViewModel.selectedPlace.getValue();
            if (place != null)
                mListener.onPlaceShare(place);
        });
        viewBinding.deleteButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            v.startAnimation(shake);
        });
        viewBinding.deleteButton.setOnLongClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            Place place = placeViewModel.selectedPlace.getValue();
            if (place != null)
                mListener.onPlaceDelete(place);
            return true;
        });

        popAll = false;

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setOnClickListener(v -> {
            Place place = placeViewModel.selectedPlace.getValue();
            if (place == null)
                return;
            if (Boolean.TRUE.equals(viewModel.editorMode.getValue())) {
                Editable text = viewBinding.nameEdit.getText();
                if (text != null)
                    place.name = text.toString().trim();
                text = viewBinding.descriptionEdit.getText();
                if (text != null)
                    place.description = text.toString();
                place.style.color = viewBinding.colorSwatch.getColor();

                mListener.onPlaceSave(place);
                mListener.onPlaceFocus(place);
                viewModel.editorMode.setValue(false);
            } else {
                if (mMapHolder.isNavigatingTo(place.coordinates))
                    mMapHolder.stopNavigation();
                else
                    mListener.onPlaceNavigate(place);
                popAll = true;
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
        p.setAnchorId(R.id.bottomSheetPanel);
        mFloatingButton.setLayoutParams(p);

        mBottomSheetCallback = new PlaceBottomSheetCallback();
        mBottomSheetBehavior = BottomSheetBehavior.from((View) viewBinding.getRoot().getParent());
        mBottomSheetBehavior.setSkipCollapsed(placeViewModel.expanded);
        mBottomSheetBehavior.addBottomSheetCallback(mBottomSheetCallback);

        panelState = placeViewModel.expanded ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED;
        if (savedInstanceState != null)
            panelState = savedInstanceState.getInt("panelState", panelState);
        mBottomSheetBehavior.setState(panelState);
        viewBinding.dragHandle.setAlpha(panelState == BottomSheetBehavior.STATE_EXPANDED ? 0f : 1f);
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
        outState.putInt("panelState", panelState);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void updatePlaceInformation(@NonNull Place place) {
        Activity activity = requireActivity();

        viewBinding.name.setText(place.name);
        viewBinding.source.setText(place.source.name);

        viewBinding.coordinates.setText(StringFormatter.coordinates(" ", place.coordinates.getLatitude(), place.coordinates.getLongitude()));
        setLockDrawable(place.locked);

        viewBinding.coordinates.setOnTouchListener((v, event) -> {
            if (event.getX() >= viewBinding.coordinates.getRight() - viewBinding.coordinates.getTotalPaddingRight()) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    place.locked = !place.locked;
                    mListener.onPlaceSave(place);
                    mListener.onPlaceFocus(place);
                    setLockDrawable(place.locked);
                }
                return true;
            }
            return false;
        });
        viewBinding.coordinates.setOnClickListener(v -> {
            StringFormatter.coordinateFormat++;
            if (StringFormatter.coordinateFormat == 5)
                StringFormatter.coordinateFormat = 0;
            viewBinding.coordinates.setText(StringFormatter.coordinates(" ", place.coordinates.getLatitude(), place.coordinates.getLongitude()));
            Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
        });

        if (place.altitude != Integer.MIN_VALUE) {
            viewBinding.altitude.setText(getString(R.string.place_altitude, StringFormatter.elevationH(place.altitude)));
            viewBinding.altitude.setVisibility(View.VISIBLE);
        } else {
            viewBinding.altitude.setVisibility(View.GONE);
        }

        if (place.proximity > 0) {
            viewBinding.proximity.setText(getString(R.string.place_proximity, StringFormatter.distanceH(place.proximity)));
            viewBinding.proximity.setVisibility(View.VISIBLE);
        } else {
            viewBinding.proximity.setVisibility(View.GONE);
        }

        if (place.date != null) {
            String date = DateFormat.getDateFormat(activity).format(place.date);
            String time = DateFormat.getTimeFormat(activity).format(place.date);
            viewBinding.date.setText(getString(R.string.datetime, date, time));
            viewBinding.dateRow.setVisibility(View.VISIBLE);
        } else {
            viewBinding.dateRow.setVisibility(View.GONE);
        }

        setDescription(place);
    }

    private final Observer<Boolean> editorModeObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean enabled) {
            Place place = placeViewModel.selectedPlace.getValue();
            if (place == null)
                return;

            Activity activity = requireActivity();

            int viewsState, editsState;
            if (enabled) {
                mFloatingButton.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_done));
                viewBinding.nameEdit.setText(place.name);
                viewBinding.descriptionEdit.setText(place.description);
                viewBinding.colorSwatch.setColor(place.style.color);
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

                if (place.source instanceof FileDataSource)
                    HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, mFloatingButton, false);
            } else {
                setFloatingPointDrawable(place);
                viewBinding.name.setText(place.name);
                setDescription(place);
                viewsState = View.VISIBLE;
                editsState = View.GONE;
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(viewBinding.getRoot().getWindowToken(), 0);
                viewBinding.getRoot().post(() -> updatePeekHeight());
            }
            if (!enabled) // when enabled, delayed transition is initiated by list FAB
                TransitionManager.beginDelayedTransition(viewBinding.getRoot(), new Fade());
            viewBinding.name.setVisibility(viewsState);
            viewBinding.nameWrapper.setVisibility(editsState);
            if (enabled || place.description != null && !"".equals(place.description))
                viewBinding.descriptionRow.setVisibility(View.VISIBLE);
            else
                viewBinding.descriptionRow.setVisibility(View.GONE);
            viewBinding.getRoot().findViewById(R.id.description).setVisibility(viewsState); // it can be substituted, we can not reference it directly
            viewBinding.descriptionWrapper.setVisibility(editsState);
            viewBinding.colorSwatch.setVisibility(editsState);

            if (viewModel.showDestination) {
                viewBinding.destination.setVisibility(viewsState);
            }
            if (place.date != null) {
                viewBinding.dateRow.setVisibility(viewsState);
            }
            viewBinding.source.setVisibility(viewsState);
            viewBinding.coordinatesRow.setVisibility(viewsState);
            viewBinding.descriptionIcon.setVisibility(viewsState);
            viewBinding.editButton.setVisibility(viewsState);
            viewBinding.shareButton.setVisibility(viewsState);
        }
    };

    private void setFloatingPointDrawable(Place place) {
        if (mMapHolder.isNavigatingTo(place.coordinates)) {
            mFloatingButton.setImageResource(R.drawable.ic_navigation_off);
        } else {
            mFloatingButton.setImageResource(R.drawable.ic_navigate);
        }
    }

    private void setLockDrawable(boolean locked) {
        int imageResource = locked ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open;
        Drawable drawable = AppCompatResources.getDrawable(viewBinding.coordinates.getContext(), imageResource);
        if (drawable != null) {
            int drawableSize = (int) Math.round(viewBinding.coordinates.getLineHeight() * 0.7);
            int drawablePadding = (int) (MapTrek.density * 1.5f);
            drawable.setBounds(0, drawablePadding, drawableSize, drawableSize + drawablePadding);
            int tintColor = locked ? R.color.red : R.color.colorPrimaryDark;
            drawable.setTint(viewBinding.coordinates.getContext().getColor(tintColor));
            viewBinding.coordinates.setCompoundDrawables(null, null, drawable, null);
        }
    }

    // WebView is very heavy to initialize. That's why it is used only on demand.
    private void setDescription(Place place) {
        if (place.description == null || "".equals(place.description)) {
            viewBinding.descriptionRow.setVisibility(View.GONE);
            return;
        }
        viewBinding.descriptionRow.setVisibility(View.VISIBLE);

        String text = place.description;
        boolean hasHTML = false;
        if (DetectHtml.isHtml(place.description)) {
            hasHTML = true;
        } else {
            String marked = extractLinks(place.description);
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

    private void updatePeekHeight() {
        mBottomSheetBehavior.setPeekHeight(viewBinding.dragHandle.getHeight() * 2 + viewBinding.name.getHeight() + viewBinding.source.getHeight());
        // Somehow setPeekHeight breaks state on first show
        mBottomSheetBehavior.setState(panelState);
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
                if (popAll)
                    mFragmentHolder.popAll();
                else
                    mFragmentHolder.popCurrent();
            }
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                panelState = newState;
            }
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                panelState = newState;
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
            if (!placeViewModel.expanded)
                bottomSheet.findViewById(R.id.dragHandle).setAlpha(1f - slideOffset);
            mFloatingButton.setAlpha(1f + slideOffset);
        }
    }

    public static class PlaceInformationViewModel extends ViewModel {
        public boolean showDestination = false;
        private final MutableLiveData<Boolean> editorMode = new MutableLiveData<>(false);
    }
}