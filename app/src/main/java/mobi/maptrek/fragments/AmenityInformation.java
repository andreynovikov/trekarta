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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import java.util.Locale;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Amenity;
import mobi.maptrek.databinding.FragmentAmenityInformationBinding;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.AmenityViewModel;
import mobi.maptrek.viewmodels.MapViewModel;

public class AmenityInformation extends Fragment {
    private static final String ALLOWED_URI_CHARS = " @#&=*+-_.,:!?()/~'%";

    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private AmenityBottomSheetCallback mBottomSheetCallback;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private AmenityViewModel amenityViewModel;
    private FragmentAmenityInformationBinding viewBinding;
    private int panelState;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentAmenityInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        amenityViewModel = new ViewModelProvider(requireActivity()).get(AmenityViewModel.class);
        amenityViewModel.getAmenity().observe(getViewLifecycleOwner(), amenity -> {
            if (amenity != null) {
                updateAmenityInformation(amenity);
                requireView().post(this::updatePeekHeight);
            }
        });

        MapViewModel mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        mapViewModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            if ("unknown".equals(location.getProvider())) {
                viewBinding.destination.setVisibility(View.GONE);
            } else {
                Amenity amenity = amenityViewModel.getAmenity().getValue();
                if (amenity == null)
                    return;
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                double dist = point.vincentyDistance(amenity.coordinates);
                double bearing = point.bearingTo(amenity.coordinates);
                String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
                viewBinding.destination.setVisibility(View.VISIBLE);
                viewBinding.destination.setTag(true);
                viewBinding.destination.setText(distance);
            }
        });

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_navigate);
        mFloatingButton.setOnClickListener(v -> {
            Amenity amenity = amenityViewModel.getAmenity().getValue();
            if (amenity != null)
                mMapHolder.navigateTo(amenity.coordinates, amenity.name);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
        p.setAnchorId(R.id.bottomSheetPanel);
        mFloatingButton.setLayoutParams(p);

        mBottomSheetCallback = new AmenityBottomSheetCallback();
        mBottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        mBottomSheetBehavior.addBottomSheetCallback(mBottomSheetCallback);

        panelState = BottomSheetBehavior.STATE_COLLAPSED;
        if (savedInstanceState != null)
            panelState = savedInstanceState.getInt("panelState", panelState);
        mBottomSheetBehavior.setState(panelState);
        viewBinding.dragHandle.setAlpha(panelState == BottomSheetBehavior.STATE_EXPANDED ? 0f : 1f);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBottomSheetBehavior.removeBottomSheetCallback(mBottomSheetCallback);
        viewBinding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("panelState", panelState);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void updateAmenityInformation(@NonNull Amenity amenity) {
        final Activity activity = requireActivity();
        boolean hasName = amenity.name != null;
        String type = amenity.type != -1 ? getString(amenity.type) : "";

        viewBinding.name.setText(hasName ? amenity.name : type);

        if (!hasName || amenity.type == -1) {
            viewBinding.type.setVisibility(View.GONE);
        } else {
            viewBinding.type.setVisibility(View.VISIBLE);
            viewBinding.type.setText(type);
        }

        if ("".equals(amenity.kind)) {
            viewBinding.kindRow.setVisibility(View.GONE);
        } else {
            viewBinding.kindRow.setVisibility(View.VISIBLE);
            Resources resources = getResources();
            @SuppressLint("DiscouragedApi")
            int id = resources.getIdentifier(amenity.kind, "string", activity.getPackageName());
            viewBinding.kind.setText(resources.getString(id));
        }
        @DrawableRes int icon = ResUtils.getKindIcon(amenity.kindNumber);
        if (icon == 0)
            icon = R.drawable.ic_place;
        viewBinding.kindIcon.setImageResource(icon);

        if (amenity.fee == null) {
            viewBinding.feeRow.setVisibility(View.GONE);
        } else {
            viewBinding.feeRow.setVisibility(View.VISIBLE);
            viewBinding.fee.setText(R.string.fee);
        }

        if (amenity.wheelchair == null) {
            viewBinding.wheelchairRow.setVisibility(View.GONE);
        } else {
            viewBinding.wheelchairRow.setVisibility(View.VISIBLE);
            switch (amenity.wheelchair) {
                case YES:
                    viewBinding.wheelchairIcon.setImageResource(R.drawable.ic_accessible);
                    viewBinding.wheelchair.setText(R.string.full_access);
                    break;
                case LIMITED:
                    viewBinding.wheelchairIcon.setImageResource(R.drawable.ic_accessible);
                    viewBinding.wheelchair.setText(R.string.limited_access);
                    break;
                case NO:
                    viewBinding.wheelchairIcon.setImageResource(R.drawable.ic_not_accessible);
                    viewBinding.wheelchair.setText(R.string.no_access);
                    break;
            }
        }

        if (amenity.openingHours == null) {
            viewBinding.openingHoursRow.setVisibility(View.GONE);
        } else {
            viewBinding.openingHoursRow.setVisibility(View.VISIBLE);
            viewBinding.openingHours.setText(amenity.openingHours);
        }

        if (amenity.phone == null) {
            viewBinding.phoneRow.setVisibility(View.GONE);
        } else {
            viewBinding.phoneRow.setVisibility(View.VISIBLE);
            viewBinding.phone.setText(PhoneNumberUtils.formatNumber(amenity.phone, Locale.getDefault().getCountry()));
        }

        if (amenity.website == null) {
            viewBinding.websiteRow.setVisibility(View.GONE);
        } else {
            viewBinding.websiteRow.setVisibility(View.VISIBLE);
            String website = amenity.website;
            if (!website.startsWith("http"))
                website = "http://" + website;
            String url = website;
            website = website.replaceFirst("https?://", "");
            url = "<a href=\"" + url + "\">" + website + "</a>";
            viewBinding.website.setMovementMethod(LinkMovementMethod.getInstance());
            viewBinding.website.setText(Html.fromHtml(url));
        }

        if (amenity.wikipedia == null) {
            viewBinding.wikipediaRow.setVisibility(View.GONE);
        } else {
            viewBinding.wikipediaRow.setVisibility(View.VISIBLE);
            int i = amenity.wikipedia.indexOf(':');
            String prefix, text;
            if (i > 0) {
                prefix = amenity.wikipedia.substring(0, i) + ".";
                text = amenity.wikipedia.substring(i + 1);
            } else {
                prefix = "";
                text = amenity.wikipedia;
            }
            String url = "<a href=\"https://" + prefix + "m.wikipedia.org/wiki/" +
                    Uri.encode(text, ALLOWED_URI_CHARS) + "\">" + text + "</a>";
            viewBinding.wikipedia.setMovementMethod(LinkMovementMethod.getInstance());
            viewBinding.wikipedia.setText(Html.fromHtml(url));
        }

        viewBinding.coordinates.setText(StringFormatter.coordinates(" ", amenity.coordinates.getLatitude(), amenity.coordinates.getLongitude()));
        viewBinding.coordinates.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= viewBinding.coordinates.getRight() - viewBinding.coordinates.getTotalPaddingRight()) {
                    mMapHolder.shareLocation(amenity.coordinates, amenity.name);
                } else {
                    StringFormatter.coordinateFormat++;
                    if (StringFormatter.coordinateFormat == 5)
                        StringFormatter.coordinateFormat = 0;
                    viewBinding.coordinates.setText(StringFormatter.coordinates(" ", amenity.coordinates.getLatitude(), amenity.coordinates.getLongitude()));
                    Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                }
            }
            return true;
        });

        if (amenity.altitude != Integer.MIN_VALUE) {
            viewBinding.elevation.setText(getString(R.string.place_altitude, StringFormatter.elevationH(amenity.altitude)));
            viewBinding.elevation.setVisibility(View.VISIBLE);
        } else {
            viewBinding.elevation.setVisibility(View.GONE);
        }
    }

    private void updatePeekHeight() {
        int height = viewBinding.dragHandle.getHeight() * 2 + viewBinding.name.getHeight();
        if (viewBinding.type.getVisibility() == View.VISIBLE)
            height += viewBinding.type.getHeight();
        mBottomSheetBehavior.setPeekHeight(height);
        // Somehow setPeekHeight breaks state on first show
        mBottomSheetBehavior.setState(panelState);
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    };

    private class AmenityBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                amenityViewModel.setAmenity(null);
                mBottomSheetBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                mFragmentHolder.disableActionButton();
                CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFloatingButton.getLayoutParams();
                p.setAnchorId(R.id.contentPanel);
                mFloatingButton.setLayoutParams(p);
                mFloatingButton.setAlpha(1f);
                mFragmentHolder.popCurrent();
            }
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                panelState = newState;
            }
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                panelState = newState;
                TextView view = bottomSheet.findViewById(R.id.coordinates);
                HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SWITCH_COORDINATES_FORMAT, R.string.advice_switch_coordinates_format, view, true);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            bottomSheet.findViewById(R.id.dragHandle).setAlpha(1f - slideOffset);
            mFloatingButton.setAlpha(1f + slideOffset);
        }
    }
}