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
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.view.DiscreteSlider;

public class AmenitySetupDialog extends DialogFragment {
    private AmenitySetupDialogCallback mCallback;

    public interface AmenitySetupDialogCallback {
        void onAmenityKindVisibilityChanged();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args != null ? args.getString("title", null) : null;

        @SuppressLint("InflateParams") final View dialogView = getLayoutInflater().inflate(R.layout.dialog_list, null);
        final ListView listView = dialogView.findViewById(android.R.id.list);
        AmenitySetupListAdapter listAdapter = new AmenitySetupListAdapter();
        listView.setAdapter(listAdapter);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
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
                    View view = listView.getChildAt(10).findViewById(R.id.zoom);
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
        @Override
        public Pair<String, Integer> getItem(int position) {
            Context context = requireContext();
            Resources resources = context.getResources();
            int id = resources.getIdentifier(Tags.kinds[position], "string", context.getPackageName());
            String name = id != 0 ? resources.getString(id) : Tags.kinds[position];
            return new Pair<>(name, Tags.kindZooms[position]);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return Tags.kinds.length;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            AmenitySetupListItemHolder itemHolder;
            final Pair<String, Integer> group = getItem(position);

            if (convertView == null) {
                itemHolder = new AmenitySetupListItemHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_amenity_setup, parent, false);
                itemHolder.icon = convertView.findViewById(R.id.icon);
                itemHolder.name = convertView.findViewById(R.id.name);
                itemHolder.zoom = convertView.findViewById(R.id.zoom);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (AmenitySetupListItemHolder) convertView.getTag();
            }

            @DrawableRes int icon = ResUtils.getKindIcon(1 << (position + 3));
            if (icon == 0)
                icon = R.drawable.ic_place;
            itemHolder.icon.setImageResource(icon);

            itemHolder.name.setText(group.first);

            itemHolder.zoom.setPosition(18 - group.second);
            itemHolder.zoom.setOnDiscreteSliderChangeListener(pos -> {
                boolean changed = Tags.kindZooms[position] != 18 - pos;
                Tags.kindZooms[position] = 18 - pos;
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
        ImageView icon;
        TextView name;
        DiscreteSlider zoom;
    }
}
