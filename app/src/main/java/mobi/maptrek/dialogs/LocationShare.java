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

package mobi.maptrek.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.MemoryDataSource;
import mobi.maptrek.util.Osm;
import mobi.maptrek.util.StringFormatter;

public class LocationShare extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
    public static final String ARG_NAME = "name";
    public static final String ARG_ZOOM = "zoom";

    private final Item[] items = {
            new Item("Copy to clipboard", R.drawable.ic_content_copy),
            new Item("Share as text", R.drawable.ic_share),
            new Item("Open in map app", R.drawable.ic_launch),
            new Item("Share as file", R.drawable.ic_description),
    };

    private DataHolder mDataHolder;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement DataHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDataHolder = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = requireActivity();
        ListAdapter adapter = new LocationShareAdapter(activity, android.R.layout.select_dialog_item, android.R.id.text1, items);
        return new AlertDialog.Builder(activity).setAdapter(adapter, this).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        if (args == null)
            return;
        double latitude = args.getDouble(ARG_LATITUDE);
        double longitude = args.getDouble(ARG_LONGITUDE);
        int zoom = args.getInt(ARG_ZOOM, 14);
        String name = args.getString(ARG_NAME, null);

        switch (which) {
            case 0: { // copy to clipboard
                ClipData clip = ClipData.newPlainText(getString(R.string.coordinates), StringFormatter.coordinates(" ", latitude, longitude));
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null)
                    clipboard.setPrimaryClip(clip);
                return;
            }
            case 1: { // share as text
                StringBuilder location = new StringBuilder();
                location.append(String.format(Locale.US, "%.6f %.6f", latitude, longitude));
                if (name != null)
                    location.append(" ").append(name);
                location.append(" <").append(Osm.makeShortLink(latitude, longitude, zoom)).append(">");
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, location.toString());
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_location_intent_title)));
                return;
            }
            case 2: { // geo: url
                Uri location = Uri.parse(String.format(Locale.US, "geo:%f,%f?z=%d", latitude, longitude, zoom));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
                PackageManager packageManager = requireContext().getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(mapIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (activities.size() > 0)
                    startActivity(mapIntent);
                return;
            }
            case 3: { // share as file
                MemoryDataSource dataSource = new MemoryDataSource();
                dataSource.name = name;
                if (name == null) // waypoint name can not be null for export
                    name = StringFormatter.coordinates(" ", latitude, longitude);
                dataSource.waypoints.add(new Waypoint(name, latitude, longitude));
                mDataHolder.onDataSourceShare(dataSource);
                return;
            }
            default: {
                // noop
            }
        }
    }

    static class LocationShareAdapter extends ArrayAdapter<Item> {
        private final float mDensity;

        LocationShareAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull Item[] objects) {
            super(context, resource, textViewResourceId, objects);
            mDensity = context.getResources().getDisplayMetrics().density;
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = v.findViewById(android.R.id.text1);
            Item item = getItem(position);
            if (item != null) {
                Drawable icon = AppCompatResources.getDrawable(parent.getContext(), item.icon);
                if (icon != null) {
                    icon.setTint(getContext().getColor(R.color.colorPrimaryDark));
                    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tv.setCompoundDrawablePadding((int) (16 * mDensity));
                }
            }
            return v;
        }
    }

    public static class Item {
        public final String text;
        public final int icon;

        Item(String text, Integer icon) {
            this.text = text;
            this.icon = icon;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }
}
