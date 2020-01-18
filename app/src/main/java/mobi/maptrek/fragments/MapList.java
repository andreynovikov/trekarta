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

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.oscim.core.GeoPoint;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.ui.DoubleClickListener;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.view.BitmapTileMapPreviewView;

public class MapList extends Fragment {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_ZOOM_LEVEL = "zoom";
    public static final String ARG_HIDE_OBJECTS = "hide";
    public static final String ARG_TRANSPARENCY = "transparency";

    private TextView mEmptyView;
    private View mMapListHeader;
    private Switch mHideSwitch;
    private SeekBar mTransparencySeekBar;
    private LinearLayout mMapList;
    private ArrayList<MapFile> mMaps = new ArrayList<>();
    private Collection<MapFile> mActiveMaps;
    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private LayoutInflater mInflater;

    private GeoPoint mLocation;
    private int mZoomLevel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_map_list, container, false);

        mEmptyView = rootView.findViewById(android.R.id.empty);
        mMapListHeader = rootView.findViewById(R.id.mapListHeader);
        mHideSwitch = rootView.findViewById(R.id.hideSwitch);
        mTransparencySeekBar = rootView.findViewById(R.id.transparencySeekBar);
        mMapList = rootView.findViewById(R.id.mapList);

        mHideSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mListener.onHideMapObjects(isChecked));
        mTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mListener.onTransparencyChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_SELECT_MULTIPLE_MAPS)) {
            ViewTreeObserver vto = rootView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mMaps.size() > 1) {
                        View view = mMapList.getChildAt(0).findViewById(R.id.name);
                        Rect r = new Rect();
                        view.getGlobalVisibleRect(r);
                        HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_SELECT_MULTIPLE_MAPS, R.string.advice_select_multiple_maps, r);
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        double latitude = arguments.getDouble(ARG_LATITUDE);
        double longitude = arguments.getDouble(ARG_LONGITUDE);
        mZoomLevel = arguments.getInt(ARG_ZOOM_LEVEL);
        boolean hideObjects = arguments.getBoolean(ARG_HIDE_OBJECTS);
        int transparency = arguments.getInt(ARG_TRANSPARENCY);

        mLocation = new GeoPoint(latitude, longitude);

        mMapList.removeAllViews();
        if (mMaps.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mMapListHeader.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mMapListHeader.setVisibility(View.VISIBLE);
            for (MapFile map : mMaps)
                addMap(map);
        }
        mHideSwitch.setChecked(hideObjects);
        mTransparencySeekBar.setProgress(transparency);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapList.removeAllViews();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
        mFragmentHolder = (FragmentHolder) context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMaps.clear();
    }

    public void setMaps(Collection<MapFile> maps, Collection<MapFile> active) {
        mMaps.clear();
        mMaps.addAll(maps);
        mActiveMaps = active;
        Collections.sort(mMaps, new MapComparator());
    }

    private void addMap(final MapFile mapFile) {
        final View mapView = mInflater.inflate(R.layout.list_item_map, mMapList, false);
        TextView name = mapView.findViewById(R.id.name);
        final BitmapTileMapPreviewView map = mapView.findViewById(R.id.map);
        View indicator = mapView.findViewById(R.id.indicator);
        name.setText(mapFile.name);
        map.setTileSource(mapFile.tileSource, mActiveMaps.contains(mapFile));
        boolean isFileBasedMap = mapFile.tileSource.getOption("path") != null;

        if (mapFile.boundingBox.contains(mLocation)) {
            int zoomLevel = mapFile.tileSource.getZoomLevelMax();
            int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
            if (mapFile.tileSource instanceof SQLiteTileSource) {
                minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
            }
            if (!isFileBasedMap || (mZoomLevel < zoomLevel && mZoomLevel > minZoomLevel))
                zoomLevel = mZoomLevel;
            map.setLocation(mLocation, zoomLevel);
        } else {
            map.setLocation(mapFile.boundingBox.getCenterPoint(), mapFile.tileSource.getZoomLevelMax());
        }
        if (mActiveMaps.contains(mapFile)) {
            indicator.setBackgroundColor(getResources().getColor(R.color.colorAccent, getContext().getTheme()));
        } else {
            indicator.setBackgroundColor(Color.TRANSPARENT);
        }

        mapView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mActiveMaps.size() > 1) {
                    HelperUtils.showError(getString(R.string.msgMultipleMapsMode), mFragmentHolder.getCoordinatorLayout());
                } else {
                    map.setShouldNotCloseDataSource();
                    mListener.onMapSelected(mapFile);
                    mFragmentHolder.popCurrent();
                }
            }

            @Override
            public void onDoubleClick(View v) {
                map.setShouldNotCloseDataSource();
                mListener.onExtraMapSelected(mapFile);
                mFragmentHolder.popCurrent();
            }
        });

        mapView.setOnLongClickListener(view -> {
            if (isFileBasedMap) {
                PopupMenu popup = new PopupMenu(getContext(), view);
                popup.inflate(R.menu.context_menu_data_list);
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.action_share:
                            mListener.onMapShare(mapFile);
                            return true;
                        case R.id.action_delete:
                            mListener.onMapDelete(mapFile);
                            mMapList.removeView(view);
                            mMaps.remove(mapFile);
                            if (mMaps.size() == 0) {
                                mEmptyView.setVisibility(View.VISIBLE);
                                mMapListHeader.setVisibility(View.GONE);
                            }
                            return true;
                    }
                    return false;
                });
                popup.show();
            }
            return true;
        });

        mMapList.addView(mapView);
    }

    private class MapComparator implements Comparator<MapFile>, Serializable {
        @Override
        public int compare(MapFile o1, MapFile o2) {
            /*
            boolean c1 = o1.boundingBox.contains(mLocation);
            boolean c2 = o2.boundingBox.contains(mLocation);
            int res = Boolean.compare(c1, c2);
            if (res != 0)
                return res;
            // Larger max zoom is better
            res = Integer.compare(o1.tileSource.getZoomLevelMax(), o2.tileSource.getZoomLevelMax());
            if (res != 0)
                return res;
            // Larger min zoom is "better" too
            res = Integer.compare(o1.tileSource.getZoomLevelMin(), o2.tileSource.getZoomLevelMin());
            if (res != 0)
                return res;
            //TODO Compare covering area - smaller is better
            */
            return o1.name.compareTo(o2.name);
        }
    }

}
