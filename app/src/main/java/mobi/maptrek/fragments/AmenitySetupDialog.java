package mobi.maptrek.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;

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

        @SuppressLint("InflateParams")
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_list, null);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        AmenitySetupListAdapter listAdapter = new AmenitySetupListAdapter(getActivity());
        listView.setAdapter(listAdapter);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        if (title != null)
            dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialogBuilder.setView(dialogView);
        return dialogBuilder.create();
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
            int id = resources.getIdentifier(Tags.kinds[position], "string", getActivity().getPackageName());
            String name = id != 0 ? resources.getString(id) : Tags.kinds[position];
            return new Pair<>(name, Tags.kindZooms[position] - 14);
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
                convertView = mInflater.inflate(R.layout.list_item_amenity_setup, parent, false);
                itemHolder.name = (TextView) convertView.findViewById(R.id.name);
                itemHolder.zoom = (Spinner) convertView.findViewById(R.id.zoom);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (AmenitySetupListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(group.first);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                    R.array.zooms_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            itemHolder.zoom.setAdapter(adapter);
            itemHolder.zoom.setSelection(group.second);
            itemHolder.zoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    boolean changed = Tags.kindZooms[position] != pos + 14;
                    Tags.kindZooms[position] = pos + 14;
                    if (changed && mCallback != null) {
                        mCallback.onAmenityKindVisibilityChanged();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
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
        Spinner zoom;
    }
}
