package mobi.maptrek.fragments;

import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MenuRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.List;

import mobi.maptrek.R;
import mobi.maptrek.util.XmlUtils;
import mobi.maptrek.view.PanelMenu;

public class PanelMenuFragment extends ListFragment implements PanelMenu {
    private MenuListAdapter mAdapter;
    private ArrayList<PanelMenuItem> mMenuItems;
    private HashMap<Integer, PanelMenuItem> mItemsMap;
    private OnPrepareMenuListener mOnPrepareMenuListener;
    private FragmentHolder mFragmentHolder;
    private int mMenuId;
    private boolean mPopulating;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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

    @Nullable
    public PanelMenuItem findItem(@IdRes int id) {
        return mItemsMap.get(id);
    }

    @Override
    public MenuItem add(@IdRes int id, int order, CharSequence title) {
        PanelMenuItem item = new PanelMenuItem(getContext());
        if (id == PanelMenuItem.HEADER_ID_UNDEFINED)
            id = View.generateViewId();
        item.setItemId(id);
        item.setTitle(title);
        mMenuItems.add(order, item);
        mItemsMap.put(id, item);
        if (isVisible() && !mPopulating)
            mAdapter.notifyDataSetChanged();
        return item;
    }

    @Override
    public void removeItem(@IdRes int id) {
        mMenuItems.remove(mItemsMap.remove(id));
        if (isVisible() && !mPopulating)
            mAdapter.notifyDataSetChanged();
    }

    @SuppressLint("UseSparseArrays")
    private void populateMenu() {
        mPopulating = true;
        mMenuItems = new ArrayList<>();
        mItemsMap = new HashMap<>();
        loadHeadersFromResource(mMenuId, mMenuItems);
        for (PanelMenuItem item : mMenuItems)
            mItemsMap.put(item.getItemId(), item);
        if (mOnPrepareMenuListener != null)
            mOnPrepareMenuListener.onPrepareMenu(this);
        mPopulating = false;
    }

    /**
     * Parse the given XML file as a header description, adding each
     * parsed Header into the target list.
     *
     * @param resId  The XML resource to load and parse.
     * @param target The list in which the parsed headers should be placed.
     */
    @SuppressWarnings("ResourceType")
    private void loadHeadersFromResource(@MenuRes int resId, List<PanelMenuItem> target) {
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
                            android.R.attr.checkable,
                            android.R.attr.actionLayout
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

                    int actionRes = sa.getResourceId(4, 0);
                    if (actionRes != 0)
                        item.setActionView(actionRes);

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

        MenuListAdapter(Context context) {
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

            View actionView = item.getActionView();
            if (actionView != null) {
                if (actionView.getTag() == null) {
                    itemHolder = new MenuItemHolder();
                    convertView = mInflater.inflate(R.layout.menu_item, parent, false);
                    itemHolder.title = (TextView) convertView.findViewById(R.id.title);
                    itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    itemHolder.check = (Switch) convertView.findViewById(R.id.check);
                    itemHolder.action = (ViewGroup) convertView.findViewById(R.id.actionViewContainer);
                    itemHolder.action.addView(actionView, itemHolder.action.getLayoutParams());
                    itemHolder.action.setVisibility(View.VISIBLE);
                    actionView.setTag(convertView);
                    actionView.setTag(R.id.itemHolder, itemHolder);
                } else {
                    convertView = (View) actionView.getTag();
                    itemHolder = (MenuItemHolder) actionView.getTag(R.id.itemHolder);
                }
            } else if (convertView == null || convertView.getTag() == null) {
                itemHolder = new MenuItemHolder();
                convertView = mInflater.inflate(R.layout.menu_item, parent, false);
                itemHolder.title = (TextView) convertView.findViewById(R.id.title);
                itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                itemHolder.check = (Switch) convertView.findViewById(R.id.check);
                itemHolder.action = (ViewGroup) convertView.findViewById(R.id.actionViewContainer);
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
                // itemHolder is reused - unset change listener first
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
            if (actionView == null) {
                itemHolder.action.setVisibility(View.GONE);
            }
            // Make whole item clickable in any case
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
        ViewGroup action;
    }
}
