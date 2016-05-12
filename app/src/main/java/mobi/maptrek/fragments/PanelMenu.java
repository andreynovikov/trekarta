package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.R;
import mobi.maptrek.util.XmlUtils;

public class PanelMenu extends ListFragment {
    private MenuListAdapter mAdapter;
    private ArrayList<PanelMenuItem> mMenuItems;
    private OnPrepareMenuListener mOnPrepareMenuListener;
    private FragmentHolder mFragmentHolder;
    private int mMenuId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.menu_list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        populateMenu();
        mAdapter = new MenuListAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        PopupMenu.OnMenuItemClickListener listener = (PopupMenu.OnMenuItemClickListener) getActivity();
        mFragmentHolder.popCurrent();
        listener.onMenuItemClick(mMenuItems.get(position));
    }

    public void setMenu(@MenuRes int menuId, OnPrepareMenuListener onPrepareMenuListener) {
        mMenuId = menuId;
        mOnPrepareMenuListener = onPrepareMenuListener;
        if (isVisible()) {
            populateMenu();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void populateMenu() {
        mMenuItems = new ArrayList<>();
        loadHeadersFromResource(mMenuId, mMenuItems);
        if (mOnPrepareMenuListener != null)
            mOnPrepareMenuListener.onPrepareMenu(mMenuItems);
    }

    /**
     * Parse the given XML file as a header description, adding each
     * parsed Header into the target list.
     *
     * @param resId  The XML resource to load and parse.
     * @param target The list in which the parsed headers should be placed.
     */
    @SuppressWarnings("ResourceType")
    private void loadHeadersFromResource(int resId, List<PanelMenuItem> target) {
        XmlResourceParser parser = null;
        try {
            Resources resources = getResources();
            parser = resources.getXml(resId);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            //noinspection StatementWithEmptyBody
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"menu".equals(nodeName)) {
                throw new RuntimeException("XML document must start with <menu> tag; found" + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("item".equals(nodeName)) {
                    PanelMenuItem item = new PanelMenuItem(getContext());
                    int[] set = {
                            android.R.attr.id,
                            android.R.attr.title,
                            android.R.attr.icon,
                            android.R.attr.checkable
                    };
                    TypedArray sa = getContext().obtainStyledAttributes(attrs, set);
                    item.setItemId(sa.getResourceId(0, PanelMenuItem.HEADER_ID_UNDEFINED));
                    TypedValue tv = sa.peekValue(1);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            item.setTitle(tv.resourceId);
                        } else {
                            item.setTitle(tv.string);
                        }
                    }
                    int iconRes = sa.getResourceId(2, 0);
                    if (iconRes != 0)
                        item.setIcon(iconRes);

                    item.setCheckable(sa.getBoolean(3, false));

                    sa.recycle();

                    target.add(item);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }

        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null)
                parser.close();
        }

    }

    public class MenuListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MenuListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public PanelMenuItem getItem(int position) {
            return mMenuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMenuItems.get(position).getItemId();
        }

        @Override
        public int getCount() {
            return mMenuItems.size();
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final MenuItemHolder itemHolder;
            final PanelMenuItem item = getItem(position);

            if (convertView == null) {
                itemHolder = new MenuItemHolder();
                convertView = mInflater.inflate(R.layout.menu_item, parent, false);
                itemHolder.title = (TextView) convertView.findViewById(R.id.title);
                itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                itemHolder.check = (Switch) convertView.findViewById(R.id.check);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (MenuItemHolder) convertView.getTag();
            }

            itemHolder.title.setText(item.getTitle());
            itemHolder.icon.setImageDrawable(item.getIcon());
            itemHolder.check.setVisibility(item.isCheckable() ? View.VISIBLE : View.GONE);
            final View view = convertView;
            final ListView listView = getListView();
            if (item.isCheckable()) {
                //TODO Strange situation: clicked listener is set
                itemHolder.check.setOnCheckedChangeListener(null);
                itemHolder.check.setChecked(item.isChecked());
                itemHolder.check.setVisibility(View.VISIBLE);
                // Make switch emulate item selection
                itemHolder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.setChecked(itemHolder.check.isChecked());
                        onListItemClick(listView, view, position, getItemId(position));
                    }
                });
            } else {
                itemHolder.check.setVisibility(View.GONE);
                itemHolder.check.setOnCheckedChangeListener(null);
            }
            // Make hole item clickable in any case
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.setChecked(!itemHolder.check.isChecked());
                    onListItemClick(listView, view, position, getItemId(position));
                }
            });
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class MenuItemHolder {
        TextView title;
        Switch check;
        ImageView icon;
    }

    public interface OnPrepareMenuListener {
        void onPrepareMenu(List<PanelMenuItem> menu);
    }
}
