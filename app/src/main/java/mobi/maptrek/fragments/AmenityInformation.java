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
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import java.util.Locale;

import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Amenity;
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

    private BottomSheetBehavior mBottomSheetBehavior;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_amenity_information, container, false);

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

    private void updateAmenityInformation(double latitude, double longitude) {
        final View rootView = getView();
        assert rootView != null;
        final Activity activity = getActivity();
        boolean hasName = mAmenity.name != null;
        String type = mAmenity.type != -1 ? getString(mAmenity.type) : "";

        TextView nameView = rootView.findViewById(R.id.name);
        if (nameView != null) {
            nameView.setText(hasName ? mAmenity.name : type);
        }

        TextView typeView = rootView.findViewById(R.id.type);
        if (typeView != null) {
            if (!hasName || mAmenity.type == -1) {
                typeView.setVisibility(View.GONE);
            } else {
                typeView.setVisibility(View.VISIBLE);
                typeView.setText(type);
            }
        }

        View kindRow = rootView.findViewById(R.id.kindRow);
        if ("".equals(mAmenity.kind)) {
            if (kindRow != null)
                kindRow.setVisibility(View.GONE);
        } else {
            if (kindRow != null)
                kindRow.setVisibility(View.VISIBLE);
            TextView kindView = rootView.findViewById(R.id.kind);
            if (kindView != null) {
                Resources resources = activity.getResources();
                int id = resources.getIdentifier(mAmenity.kind, "string", activity.getPackageName());
                kindView.setText(resources.getString(id));
            }
        }
        ImageView iconView = rootView.findViewById(R.id.icon);
        if (iconView != null) {
            @DrawableRes int icon = ResUtils.getKindIcon(mAmenity.kindNumber);
            if (icon == 0)
                icon = R.drawable.ic_place;
            iconView.setImageResource(icon);
        }

        View openingHoursRow = rootView.findViewById(R.id.openingHoursRow);
        if (mAmenity.openingHours == null) {
            if (openingHoursRow != null)
                openingHoursRow.setVisibility(View.GONE);
        } else {
            if (openingHoursRow != null)
                openingHoursRow.setVisibility(View.VISIBLE);
            TextView openingHoursView = rootView.findViewById(R.id.openingHours);
            if (openingHoursView != null) {
                openingHoursView.setText(mAmenity.openingHours);
            }
        }

        View phoneRow = rootView.findViewById(R.id.phoneRow);
        if (mAmenity.phone == null) {
            if (phoneRow != null)
                phoneRow.setVisibility(View.GONE);
        } else {
            if (phoneRow != null)
                phoneRow.setVisibility(View.VISIBLE);
            TextView phoneView = rootView.findViewById(R.id.phone);
            if (phoneView != null) {
                phoneView.setText(PhoneNumberUtils.formatNumber(mAmenity.phone, Locale.getDefault().getCountry()));
            }
        }

        View websiteRow = rootView.findViewById(R.id.websiteRow);
        if (mAmenity.website == null) {
            if (websiteRow != null)
                websiteRow.setVisibility(View.GONE);
        } else {
            if (websiteRow != null)
                websiteRow.setVisibility(View.VISIBLE);
            TextView websiteView = rootView.findViewById(R.id.website);
            if (websiteView != null) {
                String website = mAmenity.website;
                if (!website.startsWith("http"))
                    website = "http://" + website;
                String url = website;
                website = website.replaceFirst("https?://", "");
                url = "<a href=\"" + url + "\">" + website + "</a>";
                websiteView.setMovementMethod(LinkMovementMethod.getInstance());
                websiteView.setText(Html.fromHtml(url));
            }
        }

        View wikipediaRow = rootView.findViewById(R.id.wikipediaRow);
        if (mAmenity.wikipedia == null) {
            if (wikipediaRow != null)
                wikipediaRow.setVisibility(View.GONE);
        } else {
            if (wikipediaRow != null)
                wikipediaRow.setVisibility(View.VISIBLE);
            TextView wikipediaView = rootView.findViewById(R.id.wikipedia);
            if (wikipediaView != null) {
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
                wikipediaView.setMovementMethod(LinkMovementMethod.getInstance());
                wikipediaView.setText(Html.fromHtml(url));
            }
        }

        TextView destinationView = rootView.findViewById(R.id.destination);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            if (destinationView != null)
                destinationView.setVisibility(View.GONE);
        } else {
            GeoPoint point = new GeoPoint(latitude, longitude);
            double dist = point.vincentyDistance(mAmenity.coordinates);
            double bearing = point.bearingTo(mAmenity.coordinates);
            String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
            if (destinationView != null) {
                destinationView.setVisibility(View.VISIBLE);
                destinationView.setTag(true);
                destinationView.setText(distance);
            }
        }

        final TextView coordsView = rootView.findViewById(R.id.coordinates);
        if (coordsView != null) {
            coordsView.setText(StringFormatter.coordinates(" ", mAmenity.coordinates.getLatitude(), mAmenity.coordinates.getLongitude()));

            if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SWITCH_COORDINATES_FORMAT)) {
                // We need this very bulky code to wait until layout is settled and keyboard is completely hidden
                // otherwise we get wrong position for advice
                ViewTreeObserver vto = rootView.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        rootView.postDelayed(() -> {
                            if (isVisible()) {
                                Rect r = new Rect();
                                coordsView.getGlobalVisibleRect(r);
                                HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, r);
                            }
                        }, 1000);
                    }
                });
            }

            coordsView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getX() >= coordsView.getRight() - coordsView.getTotalPaddingRight()) {
                        mMapHolder.shareLocation(mAmenity.coordinates, mAmenity.name);
                    } else {
                        StringFormatter.coordinateFormat++;
                        if (StringFormatter.coordinateFormat == 5)
                            StringFormatter.coordinateFormat = 0;
                        coordsView.setText(StringFormatter.coordinates(" ", mAmenity.coordinates.getLatitude(), mAmenity.coordinates.getLongitude()));
                        Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                    }
                }
                return true;
            });
        }

        TextView elevationView = rootView.findViewById(R.id.elevation);
        if (elevationView != null) {
            if (mAmenity.altitude != Integer.MIN_VALUE) {
                elevationView.setText(getString(R.string.place_altitude, StringFormatter.elevationH(mAmenity.altitude)));
                elevationView.setVisibility(View.VISIBLE);
            } else {
                elevationView.setVisibility(View.GONE);
            }
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