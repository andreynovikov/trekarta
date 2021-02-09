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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import java.util.Locale;

import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Amenity;
import mobi.maptrek.databinding.FragmentAmenityInformationBinding;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.util.StringFormatter;

public class AmenityInformation extends Fragment implements OnBackPressedListener, LocationChangeListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_LANG = "lang";

    private static final String ALLOWED_URI_CHARS = " @#&=*+-_.,:!?()/~'%";

    private Amenity mAmenity;
    private double mLatitude;
    private double mLongitude;
    private int mLang;

    private FragmentAmenityInformationBinding mViews;
    private BottomSheetBehavior mBottomSheetBehavior;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViews = FragmentAmenityInformationBinding.inflate(inflater, container, false);
        final ViewGroup rootView = mViews.getRoot();
        rootView.post(() -> {
            updatePeekHeight(rootView, false);
            int panelState = BottomSheetBehavior.STATE_COLLAPSED;
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

        final ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageResource(R.drawable.ic_navigate);
        floatingButton.setOnClickListener(v -> {
            mMapHolder.navigateTo(mAmenity.coordinates, mAmenity.name);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) floatingButton.getLayoutParams();
        p.setAnchorId(R.id.bottomSheetPanel);
        floatingButton.setLayoutParams(p);

        mMapHolder.showMarker(mAmenity.coordinates, mAmenity.name, true);
        updateAmenityInformation(latitude, longitude);

        final View dragHandle = rootView.findViewById(R.id.dragHandle);
        dragHandle.setAlpha(1f);
        ViewParent parent = rootView.getParent();
        mBottomSheetBehavior = BottomSheetBehavior.from((View) parent);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    mBottomSheetBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                    mFragmentHolder.disableActionButton();
                    CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) floatingButton.getLayoutParams();
                    p.setAnchorId(R.id.contentPanel);
                    floatingButton.setLayoutParams(p);
                    floatingButton.setAlpha(1f);
                    mFragmentHolder.popCurrent();
                }
                if (newState != BottomSheetBehavior.STATE_DRAGGING && newState != BottomSheetBehavior.STATE_SETTLING)
                    mMapHolder.updateMapViewArea();
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    TextView coordsView = rootView.findViewById(R.id.coordinates);
                    HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, coordsView, true);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dragHandle.setAlpha(1f - slideOffset);
            }
        });
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
    public void onDestroyView() {
        super.onDestroyView();
        mViews = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(ARG_LATITUDE, mLatitude);
        outState.putDouble(ARG_LONGITUDE, mLongitude);
        outState.putInt(ARG_LANG, mLang);
        outState.putInt("panelState", mBottomSheetBehavior.getState());
    }

    public void setAmenity(long id) {
        mAmenity = MapTrekDatabaseHelper.getAmenityData(mLang, id, MapTrek.getApplication().getDetailedMapDatabase());
        if (isVisible()) {
            mMapHolder.showMarker(mAmenity.coordinates, mAmenity.name, true);
            updateAmenityInformation(mLatitude, mLongitude);
            final ViewGroup rootView = (ViewGroup) getView();
            if (rootView != null)
                rootView.post(() -> updatePeekHeight(rootView, true));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void updateAmenityInformation(double latitude, double longitude) {
        final Activity activity = getActivity();
        boolean hasName = mAmenity.name != null;
        String type = mAmenity.type != -1 ? getString(mAmenity.type) : "";

        mViews.name.setText(hasName ? mAmenity.name : type);

        if (!hasName || mAmenity.type == -1) {
            mViews.type.setVisibility(View.GONE);
        } else {
            mViews.type.setVisibility(View.VISIBLE);
            mViews.type.setText(type);
        }

        if ("".equals(mAmenity.kind)) {
            mViews.kindRow.setVisibility(View.GONE);
        } else {
            mViews.kindRow.setVisibility(View.VISIBLE);
            Resources resources = activity.getResources();
            int id = resources.getIdentifier(mAmenity.kind, "string", activity.getPackageName());
            mViews.kind.setText(resources.getString(id));
        }
        @DrawableRes int icon = ResUtils.getKindIcon(mAmenity.kindNumber);
        if (icon == 0)
            icon = R.drawable.ic_place;
        mViews.kindIcon.setImageResource(icon);

        if (mAmenity.fee == null) {
            mViews.feeRow.setVisibility(View.GONE);
        } else {
            mViews.feeRow.setVisibility(View.VISIBLE);
            mViews.fee.setText(R.string.fee);
        }

        if (mAmenity.wheelchair == null) {
            mViews.wheelchairRow.setVisibility(View.GONE);
        } else {
            mViews.wheelchairRow.setVisibility(View.VISIBLE);
            switch (mAmenity.wheelchair) {
                case YES:
                    mViews.wheelchairIcon.setImageResource(R.drawable.ic_accessible);
                    mViews.wheelchair.setText(R.string.full_access);
                    break;
                case LIMITED:
                    mViews.wheelchairIcon.setImageResource(R.drawable.ic_accessible);
                    mViews.wheelchair.setText(R.string.limited_access);
                    break;
                case NO:
                    mViews.wheelchairIcon.setImageResource(R.drawable.ic_not_accessible);
                    mViews.wheelchair.setText(R.string.no_access);
                    break;
            }
        }

        if (mAmenity.openingHours == null) {
            mViews.openingHoursRow.setVisibility(View.GONE);
        } else {
            mViews.openingHoursRow.setVisibility(View.VISIBLE);
            mViews.openingHours.setText(mAmenity.openingHours);
        }

        if (mAmenity.phone == null) {
            mViews.phoneRow.setVisibility(View.GONE);
        } else {
            mViews.phoneRow.setVisibility(View.VISIBLE);
            mViews.phone.setText(PhoneNumberUtils.formatNumber(mAmenity.phone, Locale.getDefault().getCountry()));
        }

        if (mAmenity.website == null) {
            mViews.websiteRow.setVisibility(View.GONE);
        } else {
            mViews.websiteRow.setVisibility(View.VISIBLE);
            String website = mAmenity.website;
            if (!website.startsWith("http"))
                website = "http://" + website;
            String url = website;
            website = website.replaceFirst("https?://", "");
            url = "<a href=\"" + url + "\">" + website + "</a>";
            mViews.website.setMovementMethod(LinkMovementMethod.getInstance());
            mViews.website.setText(Html.fromHtml(url));
        }

        if (mAmenity.wikipedia == null) {
            mViews.wikipediaRow.setVisibility(View.GONE);
        } else {
            mViews.wikipediaRow.setVisibility(View.VISIBLE);
            int i = mAmenity.wikipedia.indexOf(':');
            String prefix, text;
            if (i > 0) {
                prefix = mAmenity.wikipedia.substring(0, i) + ".";
                text = mAmenity.wikipedia.substring(i + 1);
            } else {
                prefix = "";
                text = mAmenity.wikipedia;
            }
            String url = "<a href=\"https://" + prefix + "m.wikipedia.org/wiki/" +
                    Uri.encode(text, ALLOWED_URI_CHARS) + "\">" + text + "</a>";
            mViews.wikipedia.setMovementMethod(LinkMovementMethod.getInstance());
            mViews.wikipedia.setText(Html.fromHtml(url));
        }

        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            mViews.destination.setVisibility(View.GONE);
        } else {
            GeoPoint point = new GeoPoint(latitude, longitude);
            double dist = point.vincentyDistance(mAmenity.coordinates);
            double bearing = point.bearingTo(mAmenity.coordinates);
            String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
            mViews.destination.setVisibility(View.VISIBLE);
            mViews.destination.setTag(true);
            mViews.destination.setText(distance);
        }

        mViews.coordinates.setText(StringFormatter.coordinates(" ", mAmenity.coordinates.getLatitude(), mAmenity.coordinates.getLongitude()));

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SWITCH_COORDINATES_FORMAT)) {
            // We need this very bulky code to wait until layout is settled and keyboard is completely hidden
            // otherwise we get wrong position for advice
            final View rootView = mViews.getRoot();
            ViewTreeObserver vto = rootView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    rootView.postDelayed(() -> {
                        if (isVisible()) {
                            Rect r = new Rect();
                            mViews.coordinates.getGlobalVisibleRect(r);
                            HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, r);
                        }
                    }, 1000);
                }
            });
        }

        mViews.coordinates.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= mViews.coordinates.getRight() - mViews.coordinates.getTotalPaddingRight()) {
                    mMapHolder.shareLocation(mAmenity.coordinates, mAmenity.name);
                } else {
                    StringFormatter.coordinateFormat++;
                    if (StringFormatter.coordinateFormat == 5)
                        StringFormatter.coordinateFormat = 0;
                    mViews.coordinates.setText(StringFormatter.coordinates(" ", mAmenity.coordinates.getLatitude(), mAmenity.coordinates.getLongitude()));
                    Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                }
            }
            return true;
        });

        if (mAmenity.altitude != Integer.MIN_VALUE) {
            mViews.elevation.setText(getString(R.string.place_altitude, StringFormatter.elevationH(mAmenity.altitude)));
            mViews.elevation.setVisibility(View.VISIBLE);
        } else {
            mViews.elevation.setVisibility(View.GONE);
        }

        mLatitude = latitude;
        mLongitude = longitude;
    }

    private void updatePeekHeight(ViewGroup rootView, boolean setState) {
        View dragHandle = rootView.findViewById(R.id.dragHandle);
        View nameView = rootView.findViewById(R.id.name);
        View typeView = rootView.findViewById(R.id.type);
        mBottomSheetBehavior.setPeekHeight(dragHandle.getHeight() * 2 + nameView.getHeight() + typeView.getHeight());
        if (setState)
            mBottomSheetBehavior.setState(mBottomSheetBehavior.getState());
    }

    @Override
    public boolean onBackClick() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        updateAmenityInformation(location.getLatitude(), location.getLongitude());
    }

    public void setPreferredLanguage(String lang) {
        mLang = MapTrekDatabaseHelper.getLanguageId(lang);
    }
}