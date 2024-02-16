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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;

public class WhatsNew extends DialogFragment {
    private static final String TAG_CHANGELOG = "changelog";
    private static final String TAG_CHANGE = "change";
    private static final String ATTRIBUTE_VERSION_CODE = "versioncode";
    private static final String ATTRIBUTE_PRIORITY = "priority";

    private final ArrayList<ChangeListItem> mChangelog = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_list, null);

        final ListView listView = dialogView.findViewById(android.R.id.list);
        WhatsNew.ChangeListAdapter listAdapter = new WhatsNew.ChangeListAdapter();
        listView.setAdapter(listAdapter);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle(R.string.whatsNewTitle);
        dialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> Configuration.setLastSeenChangelog(MapTrek.versionCode));
        dialogBuilder.setView(dialogView);

        return dialogBuilder.create();
    }

    @Override
    public void show(@NonNull FragmentManager manager, String tag) {
        MapTrek application = MapTrek.getApplication();
        int lastCode = Configuration.getLastSeenChangelog();
        getChangelog(application, lastCode);
        if (mChangelog.size() > 0) {
            Collections.sort(mChangelog, (o1, o2) -> {
                int result = Integer.compare(o1.priority, o2.priority);
                if (result == 0)
                    result = Integer.compare(o2.version, o1.version);
                return result;
            });
            super.show(manager, tag);
        }
    }

    public static boolean shouldShow() {
        int lastCode = Configuration.getLastSeenChangelog();
        if (lastCode == 0) {
            Configuration.setLastSeenChangelog(MapTrek.versionCode);
            return false;
        } else {
            return lastCode < MapTrek.versionCode;
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
                if (name.equals(TAG_CHANGE)) {
                    parser.require(XmlPullParser.START_TAG, null, TAG_CHANGE);
                    int code = Integer.parseInt(parser.getAttributeValue(null, ATTRIBUTE_VERSION_CODE));
                    if (code > version) {
                        ChangeListItem changeItem = new ChangeListItem();
                        changeItem.version = code;
                        changeItem.priority = Integer.parseInt(parser.getAttributeValue(null, ATTRIBUTE_PRIORITY));
                        changeItem.change = readText(parser);
                        mChangelog.add(changeItem);
                    } else {
                        skip(parser);
                    }
                    parser.require(XmlPullParser.END_TAG, null, TAG_CHANGE);
                } else {
                    skip(parser);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
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

    private class ChangeListAdapter extends BaseAdapter {
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
            WhatsNew.ChangeListItemHolder itemHolder;
            final ChangeListItem item = getItem(position);

            if (convertView == null) {
                itemHolder = new WhatsNew.ChangeListItemHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_change, parent, false);
                itemHolder.change = convertView.findViewById(R.id.change);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (WhatsNew.ChangeListItemHolder) convertView.getTag();
            }

            itemHolder.change.setText(item.change);

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class ChangeListItem {
        int version;
        int priority;
        String change;
    }

    private static class ChangeListItemHolder {
        TextView change;
    }
}
