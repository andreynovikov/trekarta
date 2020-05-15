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

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.util.StringFormatter;

public class RouteInformation extends ListFragment {
    private Route mRoute;

    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnRouteActionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        InstructionListAdapter mAdapter = new InstructionListAdapter(getActivity());
        setListAdapter(mAdapter);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnRouteActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnRouteActionListener");
        }
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        mMapHolder.setMapLocation(mRoute.get(position));
        mFragmentHolder.popAll();
    }

    public void setRoute(Route route) {
        mRoute = route;
    }

    private class InstructionListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        InstructionListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Route.Instruction getItem(int position) {
            return mRoute.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mRoute.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            InstructionListItemHolder itemHolder;

            if (convertView == null) {
                itemHolder = new InstructionListItemHolder();
                convertView = mInflater.inflate(R.layout.list_item_route_instruction, parent, false);
                itemHolder.text = convertView.findViewById(R.id.text);
                itemHolder.distance = convertView.findViewById(R.id.distance);
                itemHolder.sign = convertView.findViewById(R.id.sign);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (InstructionListItemHolder) convertView.getTag();
            }

            itemHolder.text.setText(mRoute.getInstructionText(position));
            double distance = mRoute.distanceBetween(position - 1, position);
            itemHolder.distance.setText(distance > 0 ? StringFormatter.distanceH(distance) : "");
            @DrawableRes int sign; // https://thenounproject.com/dergraph/collection/travel-navigation/
            switch (mRoute.getSign(position)) {
                case Route.Instruction.START:
                    sign = R.drawable.instruction_start;
                    break;
                case Route.Instruction.U_TURN_UNKNOWN:
                case Route.Instruction.U_TURN_LEFT:
                    sign = R.drawable.instruction_u_turn_left;
                    break;
                case Route.Instruction.KEEP_LEFT:
                    sign = R.drawable.instruction_keep_left;
                    break;
                case Route.Instruction.TURN_SHARP_LEFT:
                    sign = R.drawable.instruction_turn_sharp_left;
                    break;
                case Route.Instruction.TURN_LEFT:
                    sign = R.drawable.instruction_turn_left;
                    break;
                case Route.Instruction.TURN_SLIGHT_LEFT:
                    sign = R.drawable.instruction_turn_slight_left;
                    break;
                case Route.Instruction.TURN_SLIGHT_RIGHT:
                    sign = R.drawable.instruction_turn_slight_right;
                    break;
                case Route.Instruction.TURN_RIGHT:
                    sign = R.drawable.instruction_turn_right;
                    break;
                case Route.Instruction.TURN_SHARP_RIGHT:
                    sign = R.drawable.instruction_turn_sharp_right;
                    break;
                case Route.Instruction.FINISH:
                    sign = R.drawable.instruction_finish;
                    break;
                case Route.Instruction.REACHED_VIA:
                    sign = R.drawable.instruction_via_reached;
                    break;
                case Route.Instruction.LEAVE_ROUNDABOUT: // TODO Make separate icon
                case Route.Instruction.USE_ROUNDABOUT:
                    sign = R.drawable.instruction_use_roundabout;
                    break;
                case Route.Instruction.KEEP_RIGHT:
                    sign = R.drawable.instruction_keep_right;
                    break;
                case Route.Instruction.U_TURN_RIGHT:
                    sign = R.drawable.instruction_u_turn_right;
                    break;
                case Route.Instruction.CONTINUE_ON_STREET:
                case Route.Instruction.IGNORE:
                case Route.Instruction.UNKNOWN:
                default:
                    sign = R.drawable.instruction_continue_on_street;
                    break;
            }
            itemHolder.sign.setImageResource(sign);

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class InstructionListItemHolder {
        TextView text;
        TextView distance;
        ImageView sign;
    }

}
