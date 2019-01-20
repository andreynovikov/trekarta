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
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.RenderStyle;

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
    private static LegendItem country = new LegendItem(GeometryType.POINT, R.string.legend_country, 7)
            .addTag("place", "country").setText(R.string.legend_country_name);
    private static LegendItem state = new LegendItem(GeometryType.POINT, R.string.legend_state, 7)
            .addTag("place", "state").setText(R.string.legend_state_name);
    private static LegendItem country_boundary = new LegendItem(GeometryType.LINE, R.string.legend_country_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "2");
    private static LegendItem region_boundary = new LegendItem(GeometryType.LINE, R.string.legend_region_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "3");
    private static LegendItem province_boundary = new LegendItem(GeometryType.LINE, R.string.legend_province_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "4");
    private static LegendItem capital = new LegendItem(GeometryType.POINT, R.string.legend_capital, 14)
            .addTag("place", "city").addTag("admin_level", "2").setText(R.string.legend_capital_name);
    private static LegendItem city = new LegendItem(GeometryType.POINT, R.string.legend_city, 14)
            .addTag("place", "city").setText(R.string.legend_city_name);
    private static LegendItem town = new LegendItem(GeometryType.POINT, R.string.legend_town, 14)
            .addTag("place", "town").setText(R.string.legend_town_name);
    private static LegendItem village = new LegendItem(GeometryType.POINT, R.string.legend_village, 14)
            .addTag("place", "village").setText(R.string.legend_village_name);
    private static LegendItem suburb = new LegendItem(GeometryType.POINT, R.string.legend_suburb, 14)
            .addTag("place", "suburb").setText(R.string.legend_suburb_name);
    private static LegendItem allotments = new LegendItem(GeometryType.POINT, R.string.legend_allotments, 14)
            .addTag("place", "allotments").setText(R.string.legend_allotments_name);
    private static LegendItem locality = new LegendItem(GeometryType.POINT, R.string.legend_unpopulated_location, 14)
            .addTag("place", "locality").setText(R.string.legend_unpopulated_location_name);
    // Water
    private static LegendItem glacier = new LegendItem(GeometryType.POLY, R.string.legend_glacier, 17)
            .addTag("natural", "glacier");
    private static LegendItem water = new LegendItem(GeometryType.POLY, R.string.legend_pond, 17)
            .addTag("natural", "water").setText(R.string.legend_pond_name);
    private static LegendItem river = new LegendItem(GeometryType.LINE, R.string.legend_river, 17)
            .addTag("waterway", "river");
    private static LegendItem canal = new LegendItem(GeometryType.LINE, R.string.legend_canal, 17)
            .addTag("waterway", "canal");
    private static LegendItem stream = new LegendItem(GeometryType.LINE, R.string.legend_stream, 17)
            .addTag("waterway", "stream");
    private static LegendItem waterfall = new LegendItem(GeometryType.POINT, R.string.legend_waterfall, 17)
            .addTag("waterway", "waterfall").addTag("kind_attraction", "yes");
    // Land
    private static LegendItem bare_rock = new LegendItem(GeometryType.POLY, R.string.legend_bare_rock, 17)
            .addTag("natural", "bare_rock");
    //TODO Elevation
    private static LegendItem peak = new LegendItem(GeometryType.POINT, R.string.legend_peak, 17)
            .addTag("natural", "peak");
    private static LegendItem volcano = new LegendItem(GeometryType.POINT, R.string.legend_volcano, 17)
            .addTag("natural", "volcano");
    private static LegendItem saddle = new LegendItem(GeometryType.POINT, R.string.legend_saddle, 17)
            .addTag("natural", "saddle");
    private static LegendItem mountain_pass = new LegendItem(GeometryType.POINT, R.string.legend_mountain_pass, 17)
            .addTag("mountain_pass", "yes");
    private static LegendItem cliff = new LegendItem(GeometryType.LINE, R.string.legend_cliff, 17)
            .addTag("natural", "cliff");

    // Vegetation
    private static LegendItem forest = new LegendItem(GeometryType.POLY, R.string.legend_forest, 17)
            .addTag("natural", "forest");
    private static LegendItem marsh = new LegendItem(GeometryType.POLY, R.string.legend_marsh, 17)
            .addTag("natural", "marsh");
    private static LegendItem wetland = new LegendItem(GeometryType.POLY, R.string.legend_wetland, 17)
            .addTag("natural", "wetland");
    private static LegendItem tree = new LegendItem(GeometryType.POINT, R.string.legend_tree, 17)
            .addTag("natural", "tree");


    private static LegendItem swimming_pool = new LegendItem(GeometryType.POLY, R.string.legend_swimming_pool, 17)
            .addTag("leisure", "swimming_pool");
    private static LegendItem hedge = new LegendItem(GeometryType.LINE, R.string.legend_hedge, 17)
            .addTag("barrier", "hedge");
    private static LegendItem fence = new LegendItem(GeometryType.LINE, R.string.legend_fence, 17)
            .addTag("barrier", "fence");
    private static LegendItem wall = new LegendItem(GeometryType.LINE, R.string.legend_wall, 17)
            .addTag("barrier", "wall");
    private static LegendItem building = new LegendItem(GeometryType.POLY, R.string.legend_building, 17)
            .addTag("building", "yes").addTag("kind", "yes").addTag("addr:housenumber", "13").setText(R.string.legend_thirteen);
    private static LegendItem sports_centre = new LegendItem(GeometryType.POLY, R.string.legend_sports_centre, 17)
            .addTag("leisure", "sports_centre").addTag("kind", "yes").setText(R.string.legend_sports_centre_name);
    private static LegendItem fountain = new LegendItem(GeometryType.POINT, R.string.legend_fountain, 17)
            .addTag("amenity", "fountain").addTag("kind_urban", "yes");

    // Roads
    private static LegendItem motorway = new LegendItem(GeometryType.LINE, R.string.legend_motorway, 17)
            .addTag("highway", "motorway");
    private static LegendItem trunk_road = new LegendItem(GeometryType.LINE, R.string.legend_trunk_road, 17)
            .addTag("highway", "trunk");
    private static LegendItem primary_road = new LegendItem(GeometryType.LINE, R.string.legend_primary_road, 17)
            .addTag("highway", "primary");
    private static LegendItem secondary_road = new LegendItem(GeometryType.LINE, R.string.legend_secondary_road, 17)
            .addTag("highway", "secondary");
    private static LegendItem tertiary_road = new LegendItem(GeometryType.LINE, R.string.legend_tertiary_road, 17)
            .addTag("highway", "tertiary");
    private static LegendItem unclassified_road = new LegendItem(GeometryType.LINE, R.string.legend_general_road, 17)
            .addTag("highway", "unclassified");
    private static LegendItem residential_road = new LegendItem(GeometryType.LINE, R.string.legend_residental_road, 17)
            .addTag("highway", "residential");
    private static LegendItem pedestrian_road = new LegendItem(GeometryType.LINE, R.string.legend_pedestrian_road, 17)
            .addTag("highway", "pedestrian");
    private static LegendItem service_road = new LegendItem(GeometryType.LINE, R.string.legend_service_road, 17)
            .addTag("highway", "service");
    private static LegendItem oneway_road = new LegendItem(GeometryType.LINE, R.string.legend_oneway_road, 17)
            .addTag("highway", "unclassified").addTag("oneway", "1");
    private static LegendItem private_road = new LegendItem(GeometryType.LINE, R.string.legend_private_road, 17)
            .addTag("highway", "unclassified").addTag("access", "private");
    private static LegendItem no_access_road = new LegendItem(GeometryType.LINE, R.string.legend_noaccess_road, 17)
            .addTag("highway", "unclassified").addTag("access", "no");
    private static LegendItem wd4_road = new LegendItem(GeometryType.LINE, R.string.legend_4wd_road, 17)
            .addTag("highway", "unclassified").addTag("4wd_only", "yes");
    private static LegendItem unpaved_road = new LegendItem(GeometryType.LINE, R.string.legend_unpaved_road, 17)
            .addTag("highway", "unclassified").addTag("surface", "unpaved");
    private static LegendItem dirt_road = new LegendItem(GeometryType.LINE, R.string.legend_dirt_road, 17)
            .addTag("highway", "unclassified").addTag("surface", "dirt");
    private static LegendItem winter_road = new LegendItem(GeometryType.LINE, R.string.legend_winter_road, 17)
            .addTag("highway", "unclassified").addTag("winter_road", "yes");
    private static LegendItem ice_road = new LegendItem(GeometryType.LINE, R.string.legend_ice_road, 17)
            .addTag("highway", "unclassified").addTag("ice_road", "yes");
    private static LegendItem toll_road = new LegendItem(GeometryType.LINE, R.string.legend_toll_road, 17)
            .addTag("highway", "unclassified").addTag("toll", "yes");
    private static LegendItem bridge = new LegendItem(GeometryType.LINE, R.string.legend_bridge, 17)
            .addTag("highway", "unclassified").addTag("bridge", "yes");
    private static LegendItem tunnel = new LegendItem(GeometryType.LINE, R.string.legend_tunnel, 17)
            .addTag("highway", "unclassified").addTag("tunnel", "yes");
    private static LegendItem construction_road = new LegendItem(GeometryType.LINE, R.string.legend_road_under_construction, 17)
            .addTag("highway", "construction");
    private static LegendItem ford = new LegendItem(GeometryType.LINE, R.string.legend_ford, 17)
            .addTag("highway", "unclassified").addTag("ford", "yes");
    private static LegendItem border_control = new LegendItem(GeometryType.POINT, R.string.legend_border_control, 17)
            .addTag("barrier", "border_control");
    private static LegendItem toll_booth = new LegendItem(GeometryType.POINT, R.string.legend_toll_booth, 17)
            .addTag("barrier", "toll_booth");
    private static LegendItem traffic_signals = new LegendItem(GeometryType.POINT, R.string.legend_traffic_signals, 17)
            .addTag("highway", "traffic_signals").addTag("kind_vehicles", "yes");
    private static LegendItem block = new LegendItem(GeometryType.POINT, R.string.legend_block, 17)
            .addTag("barrier", "block").addTag("kind_barrier", "yes");
    private static LegendItem bollard = new LegendItem(GeometryType.POINT, R.string.legend_bollard, 17)
            .addTag("barrier", "bollard").addTag("kind_barrier", "yes");
    private static LegendItem cycle_barrier = new LegendItem(GeometryType.POINT, R.string.legend_cycle_barrier, 17)
            .addTag("barrier", "cycle_barrier").addTag("kind_barrier", "yes");
    private static LegendItem lift_gate = new LegendItem(GeometryType.POINT, R.string.legend_lift_gate, 17)
            .addTag("barrier", "lift_gate").addTag("kind_barrier", "yes");
    private static LegendItem gate = new LegendItem(GeometryType.POINT, R.string.legend_gate, 17)
            .addTag("barrier", "gate").addTag("kind_barrier", "yes");


    // Railways
    private static LegendItem railway = new LegendItem(GeometryType.LINE, R.string.legend_railway, 17)
            .addTag("railway", "rail");
    private static LegendItem railway_bridge = new LegendItem(GeometryType.LINE, R.string.legend_bridge, 17)
            .addTag("railway", "rail").addTag("bridge", "yes");
    private static LegendItem railway_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_tunnel, 17)
            .addTag("railway", "rail").addTag("tunnel", "yes");
    private static LegendItem abandoned_railway = new LegendItem(GeometryType.LINE, R.string.legend_abandoned_railway, 17)
            .addTag("railway", "abandoned");
    private static LegendItem light_railway = new LegendItem(GeometryType.LINE, R.string.legend_light_railway, 17)
            .addTag("railway", "light_rail");
    private static LegendItem tram = new LegendItem(GeometryType.LINE, R.string.legend_tram, 17)
            .addTag("railway", "tram");
    private static LegendItem subway = new LegendItem(GeometryType.LINE, R.string.legend_subway, 17)
            .addTag("railway", "subway");
    private static LegendItem monorail = new LegendItem(GeometryType.LINE, R.string.legend_monorail, 17)
            .addTag("railway", "monorail");
    private static LegendItem railway_platform = new LegendItem(GeometryType.POLY, R.string.legend_railway_platform, 17)
            .addTag("railway", "platform").setText(R.string.legend_railway_platform_name);
    private static LegendItem railway_station = new LegendItem(GeometryType.POINT, R.string.legend_railway_station, 17)
            .addTag("railway", "station").setText(R.string.legend_railway_station_name);
    private static LegendItem railway_halt = new LegendItem(GeometryType.POINT, R.string.legend_railway_halt, 17)
            .addTag("railway", "halt").setText(R.string.legend_railway_halt_name);
    private static LegendItem railway_level_crossing = new LegendItem(GeometryType.POINT, R.string.legend_level_crossing, 17)
            .addTag("railway", "level_crossing");
    private static LegendItem railway_crossing = new LegendItem(GeometryType.POINT, R.string.legend_pedestrian_crossing, 17)
            .addTag("railway", "crossing");

    private static LegendItem bus_stop = new LegendItem(GeometryType.POINT, R.string.legend_bus_stop, 17)
            .addTag("highway", "bus_stop").addTag("kind_transportation", "yes").setText(R.string.legend_bus_stop_name);
    private static LegendItem tram_stop = new LegendItem(GeometryType.POINT, R.string.legend_tram_stop, 17)
            .addTag("railway", "tram_stop").addTag("kind_transportation", "yes").setText(R.string.legend_tram_stop_name);
    private static LegendItem subway_station = new LegendItem(GeometryType.POINT, R.string.legend_subway_station, 15)
            .addTag("railway", "station").addTag("station", "subway").setText(R.string.legend_subway_station_name);
    private static LegendItem aeroway_aerodrome = new LegendItem(GeometryType.POINT, R.string.legend_aerodrome, 17)
            .addTag("aeroway", "aerodrome").setText(R.string.legend_aerodrome_name);
    private static LegendItem aeroway_heliport = new LegendItem(GeometryType.POINT, R.string.legend_heliport, 17)
            .addTag("aeroway", "heliport");

    // Pistes
    private static LegendItem piste_downhill_novice = new LegendItem(GeometryType.POLY, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "novice");
    private static LegendItem piste_downhill_easy = new LegendItem(GeometryType.POLY, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "easy");
    private static LegendItem piste_downhill_intermediate = new LegendItem(GeometryType.POLY, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "intermediate");
    private static LegendItem piste_downhill_advanced = new LegendItem(GeometryType.POLY, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "advanced");
    private static LegendItem piste_downhill_expert = new LegendItem(GeometryType.POLY, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "expert");
    private static LegendItem piste_downhill_freeride = new LegendItem(GeometryType.POLY, R.string.legend_free_ride, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "freeride");
    private static LegendItem piste_downhill_unknown = new LegendItem(GeometryType.POLY, R.string.legend_unknown_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown");
    private static LegendItem piste_downhill_mogul = new LegendItem(GeometryType.POLY, R.string.legend_mogul, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").addTag("piste:grooming", "mogul");
    private static LegendItem piste_downhill_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_piste, 15)
            .addTag("piste:type", "downhill").addTag("piste:lit", "yes").setTotalSymbols(6);

    private static LegendItem piste_nordic = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "nordic");
    private static LegendItem piste_nordic_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static LegendItem piste_nordic_oneway = new LegendItem(GeometryType.LINE, R.string.legend_oneway_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:oneway", "yes").setTotalSymbols(0);
    private static LegendItem piste_nordic_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static LegendItem piste_nordic_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:grooming", "backcountry").setTotalSymbols(0);
    private static LegendItem piste_nordic_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "novice");
    private static LegendItem piste_nordic_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "easy");
    private static LegendItem piste_nordic_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "intermediate");
    private static LegendItem piste_nordic_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "advanced");
    private static LegendItem piste_nordic_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "expert");

    private static LegendItem piste_sled = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "sled");
    private static LegendItem piste_sled_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static LegendItem piste_sled_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static LegendItem piste_sled_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:grooming", "backcountry").setTotalSymbols(0);
    private static LegendItem piste_sled_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "novice");
    private static LegendItem piste_sled_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "easy");
    private static LegendItem piste_sled_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "intermediate");
    private static LegendItem piste_sled_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "advanced");
    private static LegendItem piste_sled_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "expert");

    private static LegendItem piste_hike = new LegendItem(GeometryType.LINE, R.string.legend_groomed_trail, 15)
            .addTag("piste:type", "hike").setTotalSymbols(2);
    private static LegendItem piste_hike_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_requires_snow_shoes, 15)
            .addTag("piste:type", "hike").addTag("piste:grooming", "backcountry").setTotalSymbols(2);
    private static LegendItem piste_hike_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "hike").addTag("piste:lit", "yes").setTotalSymbols(4);
    private static LegendItem piste_hike_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "novice").setTotalSymbols(2);
    private static LegendItem piste_hike_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "easy").setTotalSymbols(2);
    private static LegendItem piste_hike_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "intermediate").setTotalSymbols(2);
    private static LegendItem piste_hike_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "advanced").setTotalSymbols(2);
    private static LegendItem piste_hike_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "expert").setTotalSymbols(2);

    private static LegendItem piste_sleigh = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "sleigh");
    private static LegendItem piste_sleigh_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static LegendItem piste_sleigh_oneway = new LegendItem(GeometryType.LINE, R.string.legend_oneway_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:oneway", "yes").setTotalSymbols(0);
    private static LegendItem piste_sleigh_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static LegendItem piste_sleigh_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:grooming", "backcountry").setTotalSymbols(0);


    private static LegendItem piste_snow_park = new LegendItem(GeometryType.POLY, R.string.legend_snow_park, 15)
            .addTag("piste:type", "snow_park");
    private static LegendItem piste_playground = new LegendItem(GeometryType.POLY, R.string.legend_kids_playground, 15)
            .addTag("piste:type", "playground");
    private static LegendItem piste_ice_skate = new LegendItem(GeometryType.POLY, R.string.legend_ice_rink, 15)
            .addTag("piste:type", "ice_skate");
    private static LegendItem piste_ski_jump = new LegendItem(GeometryType.LINE, R.string.legend_ski_jump, 15)
            .addTag("piste:type", "ski_jump");
    private static LegendItem piste_ski_jump_landing = new LegendItem(GeometryType.POLY, R.string.legend_ski_jump_landing_zone, 15)
            .addTag("piste:type", "ski_jump_landing");
    private static LegendItem piste_ski_tour = new LegendItem(GeometryType.LINE, R.string.legend_ski_tour, 15)
            .addTag("piste:type", "skitour").setTotalSymbols(3);

    // Aerial ways
    private static LegendItem cable_car = new LegendItem(GeometryType.LINE, R.string.legend_cable_car, 15)
            .addTag("aerialway", "cable_car").setTotalSymbols(3);
    private static LegendItem gondola = new LegendItem(GeometryType.LINE, R.string.legend_gondola, 15)
            .addTag("aerialway", "gondola").setTotalSymbols(3);
    private static LegendItem chair_lift = new LegendItem(GeometryType.LINE, R.string.legend_chair_lift, 15)
            .addTag("aerialway", "chair_lift").setTotalSymbols(3);
    private static LegendItem drag_lift = new LegendItem(GeometryType.LINE, R.string.legend_drag_lift, 15)
            .addTag("aerialway", "drag_lift").setTotalSymbols(3);
    private static LegendItem zip_line = new LegendItem(GeometryType.LINE, R.string.legend_zip_line, 15)
            .addTag("aerialway", "zip_line");
    private static LegendItem magic_carpet = new LegendItem(GeometryType.LINE, R.string.legend_magic_carpet, 15)
            .addTag("aerialway", "magic_carpet");
    private static LegendItem aerialway_station = new LegendItem(GeometryType.POINT, R.string.legend_station, 15)
            .addTag("aerialway", "station");

    // POI
    private static LegendItem wilderness_hut = new LegendItem(GeometryType.POINT, R.string.legend_wilderness_hut, 17)
            .addTag("tourism", "wilderness_hut").addTag("kind_accommodation", "yes");
    private static LegendItem alpine_hut = new LegendItem(GeometryType.POINT, R.string.legend_alpine_hut, 17)
            .addTag("tourism", "alpine_hut").addTag("kind_accommodation", "yes");
    private static LegendItem guest_house = new LegendItem(GeometryType.POINT, R.string.legend_guest_house, 17)
            .addTag("tourism", "guest_house").addTag("kind_accommodation", "yes");
    private static LegendItem motel = new LegendItem(GeometryType.POINT, R.string.legend_motel, 17)
            .addTag("tourism", "motel").addTag("kind_accommodation", "yes");
    private static LegendItem hostel = new LegendItem(GeometryType.POINT, R.string.legend_hostel, 17)
            .addTag("tourism", "hostel").addTag("kind_accommodation", "yes");
    private static LegendItem hotel = new LegendItem(GeometryType.POINT, R.string.legend_hotel, 17)
            .addTag("tourism", "hotel").addTag("kind_accommodation", "yes");
    private static LegendItem camp_site = new LegendItem(GeometryType.POINT, R.string.legend_camp_site, 17)
            .addTag("tourism", "camp_site").addTag("kind_accommodation", "yes");
    private static LegendItem caravan_site = new LegendItem(GeometryType.POINT, R.string.legend_caravan_site, 17)
            .addTag("tourism", "caravan_site").addTag("kind_accommodation", "yes");

    private static LegendItem ice_cream = new LegendItem(GeometryType.POINT, R.string.legend_ice_cream_shop, 17)
            .addTag("shop", "ice_cream").addTag("kind_food", "yes");
    private static LegendItem confectionery = new LegendItem(GeometryType.POINT, R.string.legend_confectionery_shop, 17)
            .addTag("shop", "confectionery").addTag("kind_food", "yes");
    private static LegendItem alcohol = new LegendItem(GeometryType.POINT, R.string.legend_alcohol_shop, 17)
            .addTag("shop", "alcohol").addTag("kind_food", "yes");
    private static LegendItem beverages = new LegendItem(GeometryType.POINT, R.string.legend_beverages_shop, 17)
            .addTag("shop", "beverages").addTag("kind_food", "yes");
    private static LegendItem bakery = new LegendItem(GeometryType.POINT, R.string.legend_bakery, 17)
            .addTag("shop", "bakery").addTag("kind_food", "yes");
    private static LegendItem greengrocer = new LegendItem(GeometryType.POINT, R.string.legend_greengrocer, 17)
            .addTag("shop", "greengrocer").addTag("kind_food", "yes");
    private static LegendItem supermarket = new LegendItem(GeometryType.POINT, R.string.legend_supermarket, 17)
            .addTag("shop", "supermarket").addTag("kind_food", "yes");
    private static LegendItem marketplace = new LegendItem(GeometryType.POINT, R.string.legend_marketplace, 17)
            .addTag("amenity", "marketplace").addTag("kind_food", "yes");
    private static LegendItem cafe = new LegendItem(GeometryType.POINT, R.string.legend_cafe, 17)
            .addTag("amenity", "cafe").addTag("kind_food", "yes");
    private static LegendItem pub = new LegendItem(GeometryType.POINT, R.string.legend_pub, 17)
            .addTag("amenity", "pub").addTag("kind_food", "yes");
    private static LegendItem bar = new LegendItem(GeometryType.POINT, R.string.legend_bar, 17)
            .addTag("amenity", "bar").addTag("kind_food", "yes");
    private static LegendItem fast_food = new LegendItem(GeometryType.POINT, R.string.legend_fast_food, 17)
            .addTag("amenity", "fast_food").addTag("kind_food", "yes");
    private static LegendItem restaurant = new LegendItem(GeometryType.POINT, R.string.legend_restaurant, 17)
            .addTag("amenity", "restaurant").addTag("kind_food", "yes");

    private static LegendItem zoo = new LegendItem(GeometryType.POINT, R.string.legend_zoo, 17)
            .addTag("tourism", "zoo").addTag("kind_entertainment", "yes");
    private static LegendItem picnic_site = new LegendItem(GeometryType.POINT, R.string.legend_picnic_site, 17)
            .addTag("tourism", "picnic_site").addTag("kind_entertainment", "yes");
    private static LegendItem theatre = new LegendItem(GeometryType.POINT, R.string.legend_theatre, 17)
            .addTag("amenity", "theatre").addTag("kind_entertainment", "yes");
    private static LegendItem cinema = new LegendItem(GeometryType.POINT, R.string.legend_cinema, 17)
            .addTag("amenity", "cinema").addTag("kind_entertainment", "yes");
    private static LegendItem water_park = new LegendItem(GeometryType.POINT, R.string.legend_water_park, 17)
            .addTag("leisure", "water_park").addTag("kind_entertainment", "yes");

    private static LegendItem police = new LegendItem(GeometryType.POINT, R.string.legend_police_office, 17)
            .addTag("amenity", "police").addTag("kind_emergency", "yes");
    private static LegendItem fire_station = new LegendItem(GeometryType.POINT, R.string.legend_fire_station, 17)
            .addTag("amenity", "fire_station").addTag("kind_emergency", "yes");
    private static LegendItem hospital = new LegendItem(GeometryType.POINT, R.string.legend_hospital, 17)
            .addTag("amenity", "hospital").addTag("kind_emergency", "yes");
    private static LegendItem ranger_station = new LegendItem(GeometryType.POINT, R.string.legend_ranger_station, 17)
            .addTag("amenity", "ranger_station").addTag("kind_emergency", "yes");
    private static LegendItem doctors = new LegendItem(GeometryType.POINT, R.string.legend_doctors_practice, 17)
            .addTag("amenity", "doctors").addTag("kind_emergency", "yes");
    private static LegendItem pharmacy = new LegendItem(GeometryType.POINT, R.string.legend_pharmacy, 17)
            .addTag("amenity", "pharmacy").addTag("kind_emergency", "yes");
    private static LegendItem telephone = new LegendItem(GeometryType.POINT, R.string.legend_telephone, 17)
            .addTag("amenity", "telephone").addTag("kind_emergency", "yes");
    private static LegendItem emergency_telephone = new LegendItem(GeometryType.POINT, R.string.legend_emergency_telephone, 17)
            .addTag("emergency", "phone").addTag("kind_emergency", "yes");

    private static LegendItem pet_shop = new LegendItem(GeometryType.POINT, R.string.legend_pet_shop, 17)
            .addTag("shop", "pet").addTag("kind_pets", "yes");
    private static LegendItem veterinary = new LegendItem(GeometryType.POINT, R.string.legend_veterinary_clinic, 17)
            .addTag("amenity", "veterinary").addTag("kind_pets", "yes");

    private static LegendItem toys = new LegendItem(GeometryType.POINT, R.string.legend_toys_shop, 17)
            .addTag("shop", "toys").addTag("kind_kids", "yes");
    private static LegendItem playground = new LegendItem(GeometryType.POLY, R.string.legend_playground, 17)
            .addTag("leisure", "playground").addTag("kind_kids", "yes");
    private static LegendItem kindergarten = new LegendItem(GeometryType.POINT, R.string.legend_kindergarten, 17)
            .addTag("amenity", "kindergarten").addTag("kind_kids", "yes");

    private static LegendItem bicycle = new LegendItem(GeometryType.POINT, R.string.legend_bicycle_shop, 17)
            .addTag("shop", "bicycle").addTag("kind_shopping", "yes");
    private static LegendItem outdoor = new LegendItem(GeometryType.POINT, R.string.legend_outdoor_shop, 17)
            .addTag("shop", "outdoor").addTag("kind_shopping", "yes");
    private static LegendItem sports = new LegendItem(GeometryType.POINT, R.string.legend_sports_shop, 17)
            .addTag("shop", "sports").addTag("kind_shopping", "yes");
    private static LegendItem gift = new LegendItem(GeometryType.POINT, R.string.legend_gift_shop, 17)
            .addTag("shop", "gift").addTag("kind_shopping", "yes");
    private static LegendItem jewelry = new LegendItem(GeometryType.POINT, R.string.legend_jewelry_shop, 17)
            .addTag("shop", "jewelry").addTag("kind_shopping", "yes");
    private static LegendItem photo = new LegendItem(GeometryType.POINT, R.string.legend_photo_shop, 17)
            .addTag("shop", "photo").addTag("kind_shopping", "yes");
    private static LegendItem books = new LegendItem(GeometryType.POINT, R.string.legend_books_shop, 17)
            .addTag("shop", "books").addTag("kind_shopping", "yes");
    private static LegendItem variety_store = new LegendItem(GeometryType.POINT, R.string.legend_variety_store, 17)
            .addTag("shop", "variety_store").addTag("kind_shopping", "yes");
    private static LegendItem doityourself = new LegendItem(GeometryType.POINT, R.string.legend_dyi_store, 17)
            .addTag("shop", "doityourself").addTag("kind_shopping", "yes");
    private static LegendItem department_store = new LegendItem(GeometryType.POINT, R.string.legend_department_store, 17)
            .addTag("shop", "department_store").addTag("kind_shopping", "yes");

    private static LegendItem hairdresser = new LegendItem(GeometryType.POINT, R.string.legend_hairdresser, 17)
            .addTag("shop", "hairdresser").addTag("kind_service", "yes");
    private static LegendItem copyshop = new LegendItem(GeometryType.POINT, R.string.legend_copy_shop, 17)
            .addTag("shop", "copyshop").addTag("kind_service", "yes");
    private static LegendItem laundry = new LegendItem(GeometryType.POINT, R.string.legend_laundry, 17)
            .addTag("shop", "laundry").addTag("kind_service", "yes");
    private static LegendItem bank = new LegendItem(GeometryType.POINT, R.string.legend_bank, 17)
            .addTag("amenity", "bank").addTag("kind_service", "yes");
    private static LegendItem post_office = new LegendItem(GeometryType.POINT, R.string.legend_post_office, 17)
            .addTag("amenity", "post_office").addTag("kind_service", "yes");
    private static LegendItem atm = new LegendItem(GeometryType.POINT, R.string.legend_atm, 17)
            .addTag("amenity", "atm").addTag("kind_service", "yes");
    private static LegendItem bureau_de_change = new LegendItem(GeometryType.POINT, R.string.legend_currency_exchange, 17)
            .addTag("amenity", "bureau_de_change").addTag("kind_service", "yes");
    private static LegendItem post_box = new LegendItem(GeometryType.POINT, R.string.legend_post_box, 17)
            .addTag("amenity", "post_box").addTag("kind_service", "yes");

    private static LegendItem lighthouse = new LegendItem(GeometryType.POINT, R.string.legend_lighthouse, 17)
            .addTag("man_made", "lighthouse").addTag("kind_attraction", "yes");
    private static LegendItem windmill = new LegendItem(GeometryType.POINT, R.string.legend_windmill, 17)
            .addTag("man_made", "windmill").addTag("kind_attraction", "yes");
    private static LegendItem museum = new LegendItem(GeometryType.POINT, R.string.legend_museum, 17)
            .addTag("tourism", "museum").addTag("kind_attraction", "yes");
    private static LegendItem attraction = new LegendItem(GeometryType.POINT, R.string.legend_attraction, 17)
            .addTag("tourism", "attraction").addTag("kind_attraction", "yes");
    private static LegendItem viewpoint = new LegendItem(GeometryType.POINT, R.string.legend_viewpoint, 17)
            .addTag("tourism", "viewpoint").addTag("kind_attraction", "yes");
    private static LegendItem artwork = new LegendItem(GeometryType.POINT, R.string.legend_artwork, 17)
            .addTag("tourism", "artwork").addTag("kind_attraction", "yes");
    private static LegendItem memorial = new LegendItem(GeometryType.POINT, R.string.legend_memorial, 17)
            .addTag("historic", "memorial").addTag("kind_attraction", "yes");
    private static LegendItem castle = new LegendItem(GeometryType.POINT, R.string.legend_castle, 17)
            .addTag("historic", "castle").addTag("kind_attraction", "yes");
    private static LegendItem monument = new LegendItem(GeometryType.POINT, R.string.legend_monument, 17)
            .addTag("historic", "monument").addTag("kind_attraction", "yes");
    private static LegendItem ruins = new LegendItem(GeometryType.POINT, R.string.legend_ruins, 17)
            .addTag("historic", "ruins").addTag("kind_attraction", "yes");

    private static LegendItem school = new LegendItem(GeometryType.POINT, R.string.legend_school, 17)
            .addTag("amenity", "school").addTag("kind_education", "yes");
    private static LegendItem university = new LegendItem(GeometryType.POINT, R.string.legend_university, 17)
            .addTag("amenity", "university").addTag("kind_education", "yes");
    private static LegendItem college = new LegendItem(GeometryType.POINT, R.string.legend_college, 17)
            .addTag("amenity", "college").addTag("kind_education", "yes");
    private static LegendItem library = new LegendItem(GeometryType.POINT, R.string.legend_library, 17)
            .addTag("amenity", "library").addTag("kind_education", "yes");

    private static LegendItem car = new LegendItem(GeometryType.POINT, R.string.legend_car_dialer, 17)
            .addTag("amenity", "car").addTag("kind_vehicles", "yes");
    private static LegendItem car_repair = new LegendItem(GeometryType.POINT, R.string.legend_car_repair, 17)
            .addTag("amenity", "car_repair").addTag("kind_vehicles", "yes");
    private static LegendItem car_rental = new LegendItem(GeometryType.POINT, R.string.legend_car_rental, 17)
            .addTag("amenity", "car_rental").addTag("kind_vehicles", "yes");
    private static LegendItem fuel = new LegendItem(GeometryType.POINT, R.string.legend_fuel_station, 17)
            .addTag("amenity", "fuel").addTag("kind_vehicles", "yes");
    private static LegendItem slipway = new LegendItem(GeometryType.POINT, R.string.legend_slipway, 17)
            .addTag("amenity", "slipway").addTag("kind_vehicles", "yes");
    private static LegendItem parking = new LegendItem(GeometryType.POLY, R.string.legend_parking, 17)
            .addTag("amenity", "parking").addTag("kind_vehicles", "yes");
    private static LegendItem parking_car_paid = new LegendItem(GeometryType.POLY, R.string.legend_paid_parking, 17)
            .addTag("amenity", "parking").addTag("fee", "yes").addTag("kind_vehicles", "yes");
    private static LegendItem parking_private = new LegendItem(GeometryType.POLY, R.string.legend_private_parking, 17)
            .addTag("amenity", "parking").addTag("access", "private").addTag("kind_vehicles", "yes");

    private static LegendItem bus_station = new LegendItem(GeometryType.POINT, R.string.legend_bus_station, 17)
            .addTag("amenity", "bus_station").addTag("kind_transportation", "yes");

    private static LegendItem place_of_worship = new LegendItem(GeometryType.POINT, R.string.legend_place_of_worship, 17)
            .addTag("amenity", "place_of_worship").addTag("kind_religion", "yes");
    private static LegendItem jewish = new LegendItem(GeometryType.POINT, R.string.legend_jewish_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "jewish").addTag("kind_religion", "yes");
    private static LegendItem muslim = new LegendItem(GeometryType.POINT, R.string.legend_muslim_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "muslim").addTag("kind_religion", "yes");
    private static LegendItem buddhist = new LegendItem(GeometryType.POINT, R.string.legend_buddhist_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "buddhist").addTag("kind_religion", "yes");
    private static LegendItem hindu = new LegendItem(GeometryType.POINT, R.string.legend_hindu_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "hindu").addTag("kind_religion", "yes");
    private static LegendItem shinto = new LegendItem(GeometryType.POINT, R.string.legend_shinto_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "shinto").addTag("kind_religion", "yes");
    private static LegendItem christian = new LegendItem(GeometryType.POINT, R.string.legend_christian_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "christian").addTag("kind_religion", "yes");
    private static LegendItem sikh = new LegendItem(GeometryType.POINT, R.string.legend_sikh_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "sikh").addTag("kind_religion", "yes");
    private static LegendItem taoist = new LegendItem(GeometryType.POINT, R.string.legend_taoist_place, 17)
            .addTag("amenity", "place_of_worship").addTag("religion", "taoist").addTag("kind_religion", "yes");

    private static LegendItem bicycle_rental = new LegendItem(GeometryType.POINT, R.string.legend_bicycle_rental, 17)
            .addTag("amenity", "bicycle_rental").addTag("kind_hikebike", "yes");
    private static LegendItem drinking_water = new LegendItem(GeometryType.POINT, R.string.legend_drinking_water, 17)
            .addTag("amenity", "drinking_water").addTag("kind_hikebike", "yes");
    private static LegendItem shelter = new LegendItem(GeometryType.POINT, R.string.legend_shelter, 17)
            .addTag("amenity", "shelter").addTag("kind_hikebike", "yes");
    private static LegendItem toilets = new LegendItem(GeometryType.POINT, R.string.legend_toilets, 17)
            .addTag("amenity", "toilets").addTag("kind_hikebike", "yes");
    private static LegendItem information_office = new LegendItem(GeometryType.POINT, R.string.legend_information_office, 17)
            .addTag("tourism", "information").addTag("information", "office").addTag("kind_hikebike", "yes");
    private static LegendItem information_guidepost = new LegendItem(GeometryType.POINT, R.string.legend_guidepost, 17)
            .addTag("tourism", "information").addTag("information", "guidepost").addTag("kind_hikebike", "yes");
    private static LegendItem information_map = new LegendItem(GeometryType.POINT, R.string.legend_map, 17)
            .addTag("tourism", "information").addTag("information", "map").addTag("kind_hikebike", "yes");
    private static LegendItem information = new LegendItem(GeometryType.POINT, R.string.legend_information, 17)
            .addTag("tourism", "information").addTag("kind_hikebike", "yes");

    private static LegendItem land = new LegendItem(GeometryType.POLY, R.string.legend_land, 14)
            .addTag("natural", "land");

    private static LegendSection administrative = new LegendSection(R.string.legend_administrative, new LegendItem[]{
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

    private static LegendSection roads = new LegendSection(R.string.legend_roads, new LegendItem[]{
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
            construction_road,
            parking,
            parking_car_paid,
            parking_private,
            border_control,
            toll_booth,
            traffic_signals,
            block,
            bollard,
            cycle_barrier,
            lift_gate,
            gate
    });

    private static LegendSection railways = new LegendSection(R.string.legend_railways, new LegendItem[]{
            railway,
            railway_bridge,
            railway_tunnel,
            abandoned_railway,
            light_railway,
            tram,
            subway,
            monorail,
            railway_level_crossing,
            railway_crossing
    });

    private static LegendSection amenities = new LegendSection(R.string.legend_amenities, new LegendItem[]{
            wilderness_hut,
            alpine_hut,
            guest_house,
            motel,
            hostel,
            hotel,
            camp_site,
            caravan_site,
            ice_cream,
            confectionery,
            alcohol,
            beverages,
            bakery,
            greengrocer,
            marketplace,
            supermarket,
            pet_shop,
            toys,
            bicycle,
            outdoor,
            sports,
            gift,
            jewelry,
            photo,
            books,
            variety_store,
            doityourself,
            department_store,
            copyshop,
            laundry,
            hairdresser,
            car,
            car_repair,
            car_rental,
            fuel,
            bicycle_rental,
            cafe,
            pub,
            bar,
            fast_food,
            restaurant,
            zoo,
            theatre,
            cinema,
            water_park,
            university,
            college,
            school,
            kindergarten,
            police,
            fire_station,
            ranger_station,
            hospital,
            doctors,
            pharmacy,
            veterinary,
            bank,
            post_office,
            atm,
            bureau_de_change,
            post_box,
            museum,
            library,
            slipway,
            memorial,
            castle,
            monument,
            ruins,
            attraction,
            viewpoint,
            artwork,
            windmill,
            lighthouse,
            place_of_worship,
            jewish,
            muslim,
            buddhist,
            hindu,
            shinto,
            christian,
            sikh,
            taoist,
            picnic_site,
            drinking_water,
            shelter,
            toilets,
            telephone,
            emergency_telephone,
            information_office,
            information_guidepost,
            information_map,
            information
    });

    private static LegendSection[] themeRoads = new LegendSection[]{
            /*
            <cat id="roads" />
            <cat id="ferries" />
            <cat id="railways" />
            urban areas
            */
            roads,
            new LegendSection(R.string.legend_water, new LegendItem[]{
                    water,
                    river,
                    canal,
                    stream,
                    waterfall
            }),
            new LegendSection(R.string.legend_urban, new LegendItem[]{
                    building,
                    swimming_pool
            }),
            administrative
            //other
    };

    private static LegendSection[] themeWinter = new LegendSection[]{
            new LegendSection(R.string.legend_downhill_skiing, new LegendItem[]{
                    piste_downhill_novice,
                    piste_downhill_easy,
                    piste_downhill_intermediate,
                    piste_downhill_advanced,
                    piste_downhill_expert,
                    piste_downhill_freeride,
                    piste_downhill_unknown,
                    piste_downhill_mogul,
                    piste_downhill_lit,
                    piste_snow_park,
                    piste_playground
            }),
            new LegendSection(R.string.legend_nordic_skiing, new LegendItem[]{
                    piste_nordic,
                    piste_nordic_oneway,
                    piste_nordic_lit,
                    piste_nordic_scooter,
                    piste_nordic_backcountry,
                    piste_nordic_novice,
                    piste_nordic_easy,
                    piste_nordic_intermediate,
                    piste_nordic_advanced,
                    piste_nordic_expert
            }),
            new LegendSection(R.string.legend_sledding, new LegendItem[]{
                    piste_sled,
                    piste_sled_lit,
                    piste_sled_scooter,
                    piste_sled_backcountry,
                    piste_sled_novice,
                    piste_sled_easy,
                    piste_sled_intermediate,
                    piste_sled_advanced,
                    piste_sled_expert
            }),
            new LegendSection(R.string.legend_winter_hiking, new LegendItem[]{
                    piste_hike,
                    piste_hike_backcountry,
                    piste_hike_lit,
                    piste_hike_novice,
                    piste_hike_easy,
                    piste_hike_intermediate,
                    piste_hike_advanced,
                    piste_hike_expert
            }),
            new LegendSection(R.string.legend_sleighing, new LegendItem[]{
                    piste_sleigh,
                    piste_sleigh_lit,
                    piste_sleigh_oneway,
                    piste_sleigh_scooter,
                    piste_sleigh_backcountry
            }),
            new LegendSection(R.string.legend_other_activities, new LegendItem[]{
                    piste_ice_skate,
                    piste_ski_jump,
                    piste_ski_jump_landing,
                    piste_ski_tour,
                    sports_centre
            }),
            new LegendSection(R.string.legend_aerial_ways, new LegendItem[]{
                    cable_car,
                    gondola,
                    chair_lift,
                    drag_lift,
                    zip_line,
                    magic_carpet,
                    aerialway_station
            }),
            administrative,
            new LegendSection(R.string.legend_terrain_features, new LegendItem[]{
                    glacier,
                    water,
                    river,
                    stream,
                    waterfall,
                    forest,
                    tree,
                    marsh,
                    wetland,
                    bare_rock,
                    cliff,
                    peak,
                    saddle,
                    mountain_pass
            }),
            new LegendSection(R.string.legend_manmade_features, new LegendItem[]{
                    building,
                    wall,
                    fence,
                    hedge,
                    railway_platform,
                    playground,
                    fountain
            }),
            new LegendSection(R.string.legend_transportation, new LegendItem[]{
                    bus_station,
                    bus_stop,
                    tram_stop,
                    subway_station,
                    railway_station,
                    railway_halt,
                    aeroway_aerodrome,
                    aeroway_heliport
            }),
            roads.remove(dirt_road).remove(unpaved_road),
            railways,
            amenities
    };

    private Legend.LegendListAdapter mAdapter;
    private MapHolder mMapHolder;
    private IRenderTheme mTheme;
    private int mBackground;

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

        LegendSection[] theme = themeWinter;

        for (LegendSection section : theme) {
            mData.add(new LegendItem(GeometryType.NONE, section.title, 0));
            Collections.addAll(mData, section.items);
        }

        for (RenderStyle style : mTheme.matchElement(land.type, land.tags, land.zoomLevel)) {
            if (style instanceof AreaStyle) {
                mBackground = ((AreaStyle) style).color;
            }
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
                itemHolder.item.setLegend(legendItem, mBackground,
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
        @StringRes
        int title;
        LegendItem[] items;

        LegendSection(@StringRes int title, LegendItem[] items) {
            this.title = title;
            this.items = items;
        }

        public LegendSection remove(LegendItem item) {
            LegendItem[] items = new LegendItem[this.items.length - 1];
            int index = -1;
            for (int i = this.items.length - 1; i >= 0; i--)
                if (this.items[i].equals(item)) {
                    index = i;
                    break;
                }
            if (index < 0)
                return this;
            System.arraycopy(this.items, 0, items, 0, index);
            if (this.items.length != index)
                System.arraycopy(this.items, index + 1, items, index, this.items.length - index - 1);
            return new LegendSection(this.title, items);
        }
    }
}
