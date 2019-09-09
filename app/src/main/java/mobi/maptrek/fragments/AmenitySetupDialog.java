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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.view.DiscreteSlider;

public class AmenitySetupDialog extends DialogFragment {
    private AmenitySetupDialogCallback mCallback;

    public interface AmenitySetupDialogCallback {
        void onAmenityKindVisibilityChanged();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", null);

        final Activity activity = getActivity();

        @SuppressLint("InflateParams") final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_list, null);
        final ListView listView = dialogView.findViewById(android.R.id.list);
        AmenitySetupListAdapter listAdapter = new AmenitySetupListAdapter(getActivity());
        listView.setAdapter(listAdapter);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        if (title != null)
            dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {});
        dialogBuilder.setView(dialogView);

        final Dialog dialog = dialogBuilder.create();

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_AMENITY_SETUP)) {
            ViewTreeObserver vto = dialogView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    dialogView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    View view = listView.getChildAt(3).findViewById(R.id.zoom);
                    Rect r = new Rect();
                    view.getGlobalVisibleRect(r);
                    HelperUtils.showTargetedAdvice(dialog, Configuration.ADVICE_AMENITY_SETUP, R.string.advice_amenity_setup, r);
                }
            });
        }

        return dialog;
    }

    public void setCallback(AmenitySetupDialogCallback callback) {
        mCallback = callback;
    }

    public static class Builder {
        private String mTitle;
        private AmenitySetupDialogCallback mCallback;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setCallback(AmenitySetupDialogCallback callback) {
            mCallback = callback;
            return this;
        }

        public AmenitySetupDialog create() {
            AmenitySetupDialog dialogFragment = new AmenitySetupDialog();
            Bundle args = new Bundle();

            if (mTitle != null)
                args.putString("title", mTitle);
            dialogFragment.setCallback(mCallback);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    private class AmenitySetupListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context mContext;

        AmenitySetupListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
        }

        @Override
        public Pair<String, Integer> getItem(int position) {
            Resources resources = mContext.getResources();
            if (position > 7) // skip unused education kind
                position = position + 1;
            int id = resources.getIdentifier(Tags.kinds[position], "string", getActivity().getPackageName());
            String name = id != 0 ? resources.getString(id) : Tags.kinds[position];
            return new Pair<>(name, Tags.kindZooms[position]);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return Tags.kinds.length - 1;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            AmenitySetupListItemHolder itemHolder;
            final Pair<String, Integer> group = getItem(position);

            if (convertView == null) {
                itemHolder = new AmenitySetupListItemHolder();
                convertView = mInflater.inflate(R.layout.list_item_amenity_setup, parent, false);
                itemHolder.name = convertView.findViewById(R.id.name);
                itemHolder.zoom = convertView.findViewById(R.id.zoom);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (AmenitySetupListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(group.first);

            itemHolder.zoom.setPosition(18 - group.second);
            itemHolder.zoom.setOnDiscreteSliderChangeListener(pos -> {
                int p = position;
                if (p > 7) // skip unused education kind
                    p = p + 1;
                boolean changed = Tags.kindZooms[p] != 18 - pos;
                Tags.kindZooms[p] = 18 - pos;
                if (changed && mCallback != null) {
                    mCallback.onAmenityKindVisibilityChanged();
                }
            });

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class AmenitySetupListItemHolder {
        TextView name;
        DiscreteSlider zoom;
    }
}
