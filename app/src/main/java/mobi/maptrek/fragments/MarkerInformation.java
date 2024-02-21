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

import android.content.Context;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.databinding.FragmentMarkerInformationBinding;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.MapViewModel;

public class MarkerInformation extends Fragment {
    private OnPlaceActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private MapViewModel mapViewModel;
    private FragmentMarkerInformationBinding viewBinding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentMarkerInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        mapViewModel.getMarkerState().observe(requireActivity(), markerState -> {
            if (markerState.isShown()) {
                String name = markerState.getName();
                if (name == null || "".equals(name))
                    name = StringFormatter.coordinates(markerState.getCoordinates());
                viewBinding.name.setText(name);
            }
        });

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(AppCompatResources.getDrawable(view.getContext(), R.drawable.ic_pin_drop));
        floatingButton.setOnClickListener(v -> {
            MapViewModel.MarkerState markerState = mapViewModel.getMarkerState().getValue();
            if (markerState == null)
                return;
            String name = markerState.getName();
            if (name == null || "".equals(name))
                name = getString(R.string.place_name, Configuration.getPointsCounter());
            mListener.onPlaceCreate(markerState.getCoordinates(), name, true, true);
            mFragmentHolder.disableActionButton();
            mFragmentHolder.popCurrent();
        });
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
        mListener = null;
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            mapViewModel.removeMarker();
            mFragmentHolder.disableActionButton();
            remove();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    };
}
