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
import android.widget.FrameLayout;
import android.widget.TextView;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.theme.IRenderTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.view.LegendView;
import mobi.maptrek.view.LegendView.LegendItem;

// http://www.compassdude.com/map-symbols.php
// https://support.viewranger.com/index.php?pg=kb.page&id=143

public class Legend extends ListFragment {
    // Administrative
    private static LegendItem country = new LegendItem(GeometryType.POINT, "Country", 7)
            .addTag("place", "country").setText("Finland");
    private static LegendItem state = new LegendItem(GeometryType.POINT, "State", 7)
            .addTag("place", "state").setText("Texas");
    private static LegendItem country_boundary = new LegendItem(GeometryType.LINE, "Country boundary", 14)
            .addTag("boundary", "administrative").addTag("admin_level", "2");
    private static LegendItem region_boundary = new LegendItem(GeometryType.LINE, "Region boundary", 14)
            .addTag("boundary", "administrative").addTag("admin_level", "3");
    private static LegendItem province_boundary = new LegendItem(GeometryType.LINE, "Province boundary", 14)
            .addTag("boundary", "administrative").addTag("admin_level", "4");
    private static LegendItem capital = new LegendItem(GeometryType.POINT, "Country capital", 14)
            .addTag("place", "city").addTag("admin_level", "2").setText("Moscow");
    private static LegendItem city = new LegendItem(GeometryType.POINT, "City", 14)
            .addTag("place", "city").setText("Munich");
    private static LegendItem town = new LegendItem(GeometryType.POINT, "Town", 14)
            .addTag("place", "town").setText("Telavi");
    private static LegendItem village = new LegendItem(GeometryType.POINT, "Vilage", 14)
            .addTag("place", "village").setText("Krasnopolie");
    private static LegendItem suburb = new LegendItem(GeometryType.POINT, "Suburb", 14)
            .addTag("place", "suburb").setText("Parnas");
    private static LegendItem allotments = new LegendItem(GeometryType.POINT, "Allotments", 14)
            .addTag("place", "allotments").setText("Polka");
    private static LegendItem locality = new LegendItem(GeometryType.POINT, "Unpopulated location", 14)
            .addTag("place", "locality").setText("Välivaara");
    // Water
    private static LegendItem water = new LegendItem(GeometryType.POLY, "Pond", 17)
            .addTag("natural", "water").setText("Peno");
    private static LegendItem river = new LegendItem(GeometryType.LINE, "River", 17)
            .addTag("waterway", "river");
    private static LegendItem canal = new LegendItem(GeometryType.LINE, "Canal", 17)
            .addTag("waterway", "canal");
    private static LegendItem swimming_pool = new LegendItem(GeometryType.POLY, "Swimming pool", 17)
            .addTag("leisure", "swimming_pool");


    private static LegendItem building = new LegendItem(GeometryType.POLY, "Building", 17)
            .addTag("building", "yes").addTag("kind", "yes").addTag("addr:housenumber", "13").setText("13");
    private static LegendItem toilets = new LegendItem(GeometryType.POINT, "Toilets", 17)
            .addTag("amenity", "toilets").addTag("kind_hikebike", "yes");
    // Roads
    private static LegendItem motorway = new LegendItem(GeometryType.LINE, "Motorway", 17)
            .addTag("highway", "motorway");
    private static LegendItem trunk_road = new LegendItem(GeometryType.LINE, "Trunk road", 17)
            .addTag("highway", "trunk");
    private static LegendItem primary_road = new LegendItem(GeometryType.LINE, "Primary road", 17)
            .addTag("highway", "primary");
    private static LegendItem secondary_road = new LegendItem(GeometryType.LINE, "Secondary road", 17)
            .addTag("highway", "secondary");
    private static LegendItem tertiary_road = new LegendItem(GeometryType.LINE, "Tertiary road", 17)
            .addTag("highway", "tertiary");
    private static LegendItem unclassified_road = new LegendItem(GeometryType.LINE, "General road", 17)
            .addTag("highway", "unclassified");
    private static LegendItem residential_road = new LegendItem(GeometryType.LINE, "Residential road", 17)
            .addTag("highway", "residential");
    private static LegendItem pedestrian_road = new LegendItem(GeometryType.LINE, "Pedestrian road", 17)
            .addTag("highway", "pedestrian");
    private static LegendItem service_road = new LegendItem(GeometryType.LINE, "Service road", 17)
            .addTag("highway", "service");
    private static LegendItem oneway_road = new LegendItem(GeometryType.LINE, "One way road", 17)
            .addTag("highway", "unclassified").addTag("oneway", "1");
    private static LegendItem private_road = new LegendItem(GeometryType.LINE, "Private road", 17)
            .addTag("highway", "unclassified").addTag("access", "private");
    private static LegendItem no_access_road = new LegendItem(GeometryType.LINE, "Road with no access", 17)
            .addTag("highway", "unclassified").addTag("access", "no");
    private static LegendItem wd4_road = new LegendItem(GeometryType.LINE, "4wd only road", 17)
            .addTag("highway", "unclassified").addTag("4wd_only", "yes");
    private static LegendItem unpaved_road = new LegendItem(GeometryType.LINE, "Unpaved road", 17)
            .addTag("highway", "unclassified").addTag("surface", "unpaved");
    private static LegendItem dirt_road = new LegendItem(GeometryType.LINE, "Dirt road", 17)
            .addTag("highway", "unclassified").addTag("surface", "dirt");
    private static LegendItem winter_road = new LegendItem(GeometryType.LINE, "Winter road", 17)
            .addTag("highway", "unclassified").addTag("winter_road", "yes");
    private static LegendItem ice_road = new LegendItem(GeometryType.LINE, "Ice road", 17)
            .addTag("highway", "unclassified").addTag("ice_road", "yes");
    private static LegendItem toll_road = new LegendItem(GeometryType.LINE, "Toll road", 17)
            .addTag("highway", "unclassified").addTag("toll", "yes");
    private static LegendItem bridge = new LegendItem(GeometryType.LINE, "Bridge", 17)
            .addTag("highway", "unclassified").addTag("bridge", "yes");
    private static LegendItem tunnel = new LegendItem(GeometryType.LINE, "Tunnel", 17)
            .addTag("highway", "secondary").addTag("tunnel", "yes");
    private static LegendItem construction_road = new LegendItem(GeometryType.LINE, "Road under construction", 17)
            .addTag("highway", "construction").addTag("tunnel", "yes");
    private static LegendItem ford = new LegendItem(GeometryType.LINE, "Ford", 17)
            .addTag("highway", "unclassified").addTag("ford", "yes");


    private static LegendSection administrative = new LegendSection("Administrative", new LegendItem[]{
            country,
            state,
            country_boundary,
            region_boundary,
            province_boundary,
            capital,
            city,
            town,
            locality,
            village,
            suburb,
            allotments
    });

    private static LegendSection roads = new LegendSection("Roads", new LegendItem[]{
            motorway,
            trunk_road,
            primary_road,
            secondary_road,
            tertiary_road,
            unclassified_road,
            residential_road,
            service_road,
            oneway_road,
            toll_road,
            private_road,
            no_access_road,
            wd4_road,
            unpaved_road,
            dirt_road,
            winter_road,
            ice_road,
            bridge,
            tunnel,
            ford,
            construction_road
    });

    private static LegendSection other = new LegendSection("Other", new LegendItem[]{
            toilets,
            unclassified_road,
            water,
    });

    private static LegendSection[] themeRoads = new LegendSection[]{
            /*
            <cat id="roads" />
            <cat id="ferries" />
            <cat id="railways" />
            urban areas
            */
            roads,
            new LegendSection("Water", new LegendItem[]{
                    water,
                    river,
                    canal
            }),
            new LegendSection("Urban", new LegendItem[]{
                    building,
                    swimming_pool
            }),
            administrative
            //other
    };

    private Legend.LegendListAdapter mAdapter;
    private MapHolder mMapHolder;
    private IRenderTheme mTheme;

    private List<LegendItem> mData = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.list_with_empty_view, container, false);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) rootView.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(R.dimen.legendWidth);
        rootView.setLayoutParams(layoutParams);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new Legend.LegendListAdapter(getActivity());
        setListAdapter(mAdapter);
        updateData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mMapHolder = (MapHolder) context;
            mTheme = mMapHolder.getMap().getTheme();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapHolder = null;
    }

    public void updateData() {
        mData.clear();

        LegendSection[] theme = themeRoads;
        for (LegendSection section : theme) {
            mData.add(new LegendItem(GeometryType.NONE, section.title, 0));
            Collections.addAll(mData, section.items);
        }

        mAdapter.notifyDataSetChanged();
    }

    private class LegendListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        LegendListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public LegendItem getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Legend.LegendListItemHolder itemHolder;
            final LegendItem legendItem = getItem(position);

            if (convertView == null) {
                itemHolder = new Legend.LegendListItemHolder();
                if (legendItem.type == GeometryType.NONE) {
                    convertView = mInflater.inflate(R.layout.list_item_section_title, parent, false);
                    itemHolder.name = convertView.findViewById(R.id.name);
                } else {
                    convertView = mInflater.inflate(R.layout.list_item_legend, parent, false);
                    itemHolder.item = convertView.findViewById(R.id.item);
                    itemHolder.name = convertView.findViewById(R.id.name);
                }
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (Legend.LegendListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(legendItem.name);
            if (legendItem.type != GeometryType.NONE) {
                itemHolder.item.setLegend(legendItem, mTheme.getMapBackground(),
                        mTheme.matchElement(legendItem.type, legendItem.tags, legendItem.zoomLevel));
            }
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return mData.get(position).type == GeometryType.NONE ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    private static class LegendListItemHolder {
        LegendView item;
        TextView name;
    }

    private static class LegendSection {
        String title;
        LegendItem[] items;

        LegendSection(String title, LegendItem[] items) {
            this.title = title;
            this.items = items;
        }
    }
}
