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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.databinding.FragmentRouteInformationBinding;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.RouteViewModel;

public class RouteInformation extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnRouteActionListener mListener;
    private FragmentRouteInformationBinding viewBinding;
    private RouteViewModel routeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentRouteInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InstructionListAdapter adapter = new InstructionListAdapter();
        viewBinding.list.setAdapter(adapter);

        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            viewBinding.name.setText(route.name);
            if (route.source == null || route.source.isNativeTrack()) { // TODO: isNativeTrack
                viewBinding.sourceRow.setVisibility(View.GONE);
            } else {
                viewBinding.source.setText(route.source.name);
                viewBinding.sourceRow.setVisibility(View.VISIBLE);
            }
            String distance = StringFormatter.distanceHP(route.distance);
            viewBinding.distance.setText(distance);
            adapter.submitList(route.getInstructions());
        });

        viewBinding.moreButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), viewBinding.moreButton);
            viewBinding.moreButton.setOnTouchListener(popup.getDragToOpenListener());
            popup.inflate(R.menu.context_menu_route);
            popup.setOnMenuItemClickListener(RouteInformation.this);
            popup.show();
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnRouteActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnRouteActionListener");
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mFragmentHolder.disableListActionButton();
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFloatingButton = mFragmentHolder.enableListActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_navigate);
        mFloatingButton.setOnClickListener(v -> {
            Route route = routeViewModel.getSelectedRoute().getValue();
            if (route != null)
                mMapHolder.navigateVia(route);
            mFragmentHolder.popAll();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFragmentHolder.disableListActionButton();
        mFloatingButton = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Route route = routeViewModel.getSelectedRoute().getValue();
        if (route == null)
            return true;
        int itemId = item.getItemId();
        if (itemId == R.id.action_navigate_reversed) {
            mMapHolder.navigateViaReversed(route);
            mFragmentHolder.popAll();
            return true;
        }
        if (itemId == R.id.action_share) {
            mListener.onRouteShare(route);
            return true;
        }
        return false;
    }

    private class InstructionListAdapter extends ListAdapter<Route.Instruction, InstructionListAdapter.InstructionViewHolder> {
        protected InstructionListAdapter() {
            super(DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public InstructionListAdapter.InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_route_instruction, parent, false);
            return new InstructionListAdapter.InstructionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InstructionListAdapter.InstructionViewHolder holder, int position) {
            holder.bindView(position);
        }

        class InstructionViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            TextView distance;
            ImageView sign;

            public InstructionViewHolder(@NonNull View view) {
                super(view);
                text = view.findViewById(R.id.text);
                distance = view.findViewById(R.id.distance);
                sign = view.findViewById(R.id.sign);

            }

            public void bindView(int position) {
                Route.Instruction instruction = getItem(position);
                text.setText(instruction.getText());
                if (instruction.distance > 0) {
                    distance.setText(StringFormatter.distanceH(instruction.distance));
                    distance.setVisibility(View.VISIBLE);
                } else {
                    distance.setVisibility(View.GONE);
                }
                sign.setImageResource(getSignDrawable(instruction.getSign()));
                itemView.setOnClickListener(v -> {
                    mMapHolder.setMapLocation(instruction);
                    mFragmentHolder.popAll();
                });
            }
        }
    }

    protected final DiffUtil.ItemCallback<Route.Instruction> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Route.Instruction>() {
                @Override
                public boolean areItemsTheSame(@NonNull Route.Instruction oldInstruction, @NonNull Route.Instruction newInstruction) {
                    return oldInstruction.position() == newInstruction.position();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Route.Instruction oldInstruction, @NonNull Route.Instruction newInstruction) {
                    return oldInstruction.sign == newInstruction.sign
                            && oldInstruction.text.equals(newInstruction.text)
                            && Math.abs(oldInstruction.latitudeE6 - newInstruction.latitudeE6) <= 1
                            && Math.abs(oldInstruction.longitudeE6 - newInstruction.longitudeE6) <= 1;
                }
            };

    @DrawableRes
    public static int getSignDrawable(int sign) {
        @DrawableRes int signDrawable; // https://thenounproject.com/dergraph/collection/travel-navigation/
        switch (sign) {
            case Route.Instruction.START:
                signDrawable = R.drawable.instruction_start;
                break;
            case Route.Instruction.U_TURN_UNKNOWN:
            case Route.Instruction.U_TURN_LEFT:
                signDrawable = R.drawable.instruction_u_turn_left;
                break;
            case Route.Instruction.KEEP_LEFT:
                signDrawable = R.drawable.instruction_keep_left;
                break;
            case Route.Instruction.TURN_SHARP_LEFT:
                signDrawable = R.drawable.instruction_turn_sharp_left;
                break;
            case Route.Instruction.TURN_LEFT:
                signDrawable = R.drawable.instruction_turn_left;
                break;
            case Route.Instruction.TURN_SLIGHT_LEFT:
                signDrawable = R.drawable.instruction_turn_slight_left;
                break;
            case Route.Instruction.TURN_SLIGHT_RIGHT:
                signDrawable = R.drawable.instruction_turn_slight_right;
                break;
            case Route.Instruction.TURN_RIGHT:
                signDrawable = R.drawable.instruction_turn_right;
                break;
            case Route.Instruction.TURN_SHARP_RIGHT:
                signDrawable = R.drawable.instruction_turn_sharp_right;
                break;
            case Route.Instruction.FINISH:
                signDrawable = R.drawable.instruction_finish;
                break;
            case Route.Instruction.REACHED_VIA:
                signDrawable = R.drawable.instruction_via_reached;
                break;
            case Route.Instruction.LEAVE_ROUNDABOUT: // TODO Make separate icon
            case Route.Instruction.USE_ROUNDABOUT:
                signDrawable = R.drawable.instruction_use_roundabout;
                break;
            case Route.Instruction.KEEP_RIGHT:
                signDrawable = R.drawable.instruction_keep_right;
                break;
            case Route.Instruction.U_TURN_RIGHT:
                signDrawable = R.drawable.instruction_u_turn_right;
                break;
            case Route.Instruction.CONTINUE_ON_STREET:
            case Route.Instruction.IGNORE:
            case Route.Instruction.UNKNOWN:
            default:
                signDrawable = R.drawable.instruction_continue_on_street;
                break;
        }
        return signDrawable;
    }
}
