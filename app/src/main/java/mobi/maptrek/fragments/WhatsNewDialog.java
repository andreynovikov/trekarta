/*
 * Copyright 2020 Andrey Novikov
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
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;

public class WhatsNewDialog extends DialogFragment {
    private static final String TAG_CHANGELOG = "changelog";
    private static final String TAG_RELEASE = "release";
    private static final String TAG_CHANGE = "change";
    private static final String ATTRIBUTE_VERSION_CODE = "versioncode";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String ATTRIBUTE_DATE = "date";
    private static final String ATTRIBUTE_VARIANT = "variant";

    private final ArrayList<ChangeListItem> mChangelog = new ArrayList<>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        @SuppressLint("InflateParams") final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_list, null);

        final ListView listView = dialogView.findViewById(android.R.id.list);
        WhatsNewDialog.ChangeListAdapter listAdapter = new WhatsNewDialog.ChangeListAdapter(activity);
        listView.setAdapter(listAdapter);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.whatsNewTitle);
        dialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> Configuration.setLastSeenChangelog(MapTrek.versionCode));
        dialogBuilder.setView(dialogView);

        return dialogBuilder.create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        MapTrek application = MapTrek.getApplication();
        int lastCode = Configuration.getLastSeenChangelog();
        if (lastCode == 0) {
            Configuration.setLastSeenChangelog(MapTrek.versionCode);
        } else if (lastCode < MapTrek.versionCode) {
            getChangelog(application, lastCode);
            if (mChangelog.size() > 0)
                super.show(manager, tag);
        }
    }

    private void getChangelog(MapTrek application, int version) {
        try (XmlResourceParser parser = application.getResources().getXml(R.xml.changelog)) {
            while (parser.getEventType() != XmlPullParser.START_TAG)
                parser.next();

            parser.require(XmlPullParser.START_TAG, null, TAG_CHANGELOG);
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals(TAG_RELEASE)) {
                    int code = Integer.parseInt(parser.getAttributeValue(null, ATTRIBUTE_VERSION_CODE));
                    if (code > version) {
                        ChangeListItem changeItem = new ChangeListItem();
                        changeItem.version = parser.getAttributeValue(null, ATTRIBUTE_VERSION);
                        changeItem.date = parser.getAttributeValue(null, ATTRIBUTE_DATE);
                        mChangelog.add(changeItem);
                        readRelease(parser);
                    } else {
                        skip(parser);
                    }
                } else {
                    skip(parser);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    private void readRelease(XmlResourceParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, TAG_RELEASE);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_CHANGE)) {
                parser.require(XmlPullParser.START_TAG, null, TAG_CHANGE);
                String variant = parser.getAttributeValue(null, ATTRIBUTE_VARIANT);
                ChangeListItem changeItem = new ChangeListItem();
                changeItem.change = readText(parser);
                if (BuildConfig.FULL_VERSION || !"full".equals(variant))
                    mChangelog.add(changeItem);
                parser.require(XmlPullParser.END_TAG, null, TAG_CHANGE);
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_RELEASE);
    }

    @NonNull
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private String parseDate(final String dateString) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        try {
            final Date parsedDate = dateFormat.parse(dateString);
            return DateFormat.getDateFormat(getContext()).format(parsedDate);
        } catch (ParseException ignored) {
            return dateString;
        }
    }

    private class ChangeListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;

        ChangeListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public ChangeListItem getItem(int position) {
            return mChangelog.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mChangelog.size();
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            WhatsNewDialog.ChangeListItemHolder itemHolder;
            final ChangeListItem item = getItem(position);

            if (convertView == null) {
                itemHolder = new WhatsNewDialog.ChangeListItemHolder();
                if (item.version != null) {
                    convertView = mInflater.inflate(R.layout.list_item_change_title, parent, false);
                    itemHolder.divider = convertView.findViewById(R.id.group_divider);
                    itemHolder.version = convertView.findViewById(R.id.version);
                    itemHolder.date = convertView.findViewById(R.id.date);
                } else {
                    convertView = mInflater.inflate(R.layout.list_item_change, parent, false);
                    itemHolder.change = convertView.findViewById(R.id.change);
                }
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (WhatsNewDialog.ChangeListItemHolder) convertView.getTag();
            }

            if (item.version != null) {
                itemHolder.divider.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
                itemHolder.version.setText(item.version);
                if (item.date != null)
                    itemHolder.date.setText(parseDate(item.date));
            } else {
                itemHolder.change.setText(item.change);
            }

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return mChangelog.get(position).version != null ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    private static class ChangeListItem {
        String version;
        String date;
        String change;
    }

    private static class ChangeListItemHolder {
        View divider;
        TextView version;
        TextView date;
        TextView change;
    }
}
