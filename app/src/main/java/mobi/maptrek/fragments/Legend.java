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
import androidx.annotation.StringRes;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.view.LegendView;
import mobi.maptrek.view.LegendView.LegendItem;

// http://www.compassdude.com/map-symbols.php
// https://support.viewranger.com/index.php?pg=kb.page&id=143
// TODO add newly added POIs

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
    private static LegendItem allotment = new LegendItem(GeometryType.POINT, R.string.legend_allotments, 14)
            .addTag("place", "allotments").setText(R.string.legend_allotments_name);
    private static LegendItem locality = new LegendItem(GeometryType.POINT, R.string.legend_unpopulated_location, 14)
            .addTag("place", "locality").setText(R.string.legend_unpopulated_location_name);
    private static LegendItem island = new LegendItem(GeometryType.POINT, R.string.legend_island, 14)
            .addTag("place", "island").setText(R.string.legend_island_name);

    // Land use
    private static LegendItem residential = new LegendItem(GeometryType.POLY, R.string.legend_residental, 17)
            .addTag("landuse", "residential");
    private static LegendItem industrial = new LegendItem(GeometryType.POLY, R.string.legend_industrial, 17)
            .addTag("landuse", "industrial");
    private static LegendItem recreation = new LegendItem(GeometryType.POLY, R.string.legend_recreation, 17)
            .addTag("landuse", "recreation_ground");
    private static LegendItem educational = new LegendItem(GeometryType.POLY, R.string.legend_educational, 17)
            .addTag("amenity", "school");
    private static LegendItem hospital_area = new LegendItem(GeometryType.POLY, R.string.legend_hospital, 17)
            .addTag("amenity", "hospital");
    private static LegendItem construction = new LegendItem(GeometryType.POLY, R.string.legend_construction, 17)
            .addTag("landuse", "construction");
    private static LegendItem aerodrome = new LegendItem(GeometryType.POLY, R.string.legend_aerodrome, 10) // do not show icon
            .addTag("aeroway", "aerodrome");
    private static LegendItem allotments = new LegendItem(GeometryType.POLY, R.string.legend_allotments, 17)
            .addTag("landuse", "allotments");
    private static LegendItem quarry = new LegendItem(GeometryType.POLY, R.string.legend_quarry, 17)
            .addTag("landuse", "quarry");
    private static LegendItem farmland = new LegendItem(GeometryType.POLY, R.string.legend_farmland, 17)
            .addTag("landuse", "farmland");
    private static LegendItem orchard = new LegendItem(GeometryType.POLY, R.string.legend_orchard, 17)
            .addTag("landuse", "orchard");
    private static LegendItem plant_nursery = new LegendItem(GeometryType.POLY, R.string.legend_plant_nursery, 17)
            .addTag("landuse", "plant_nursery");
    private static LegendItem farmyard = new LegendItem(GeometryType.POLY, R.string.legend_farmyard, 17)
            .addTag("landuse", "farmyard");
    private static LegendItem nature_reserve = new LegendItem(GeometryType.POLY, R.string.legend_nature_reserve, 13)
            .addTag("boundary", "nature_reserve").setText(R.string.legend_nature_reserve_name);
    private static LegendItem military = new LegendItem(GeometryType.POLY, R.string.legend_military, 13)
            .addTag("landuse", "military").setText(R.string.legend_military_name);
    private static LegendItem zoo_area = new LegendItem(GeometryType.POLY, R.string.legend_zoo, 17)
            .addTag("tourism", "zoo").addTag("kind_entertainment", "yes");
    private static LegendItem theme_park_area = new LegendItem(GeometryType.POLY, R.string.legend_theme_park, 17)
            .addTag("tourism", "theme_park").addTag("kind_entertainment", "yes");
    private static LegendItem marina = new LegendItem(GeometryType.POLY, R.string.legend_marina, 17)
            .addTag("leisure", "marina").setText(R.string.legend_marina_name);

    // Water
    private static LegendItem glacier = new LegendItem(GeometryType.POLY, R.string.legend_glacier, 17)
            .addTag("natural", "glacier");
    private static LegendItem water = new LegendItem(GeometryType.POLY, R.string.legend_pond, 17)
            .addTag("natural", "water").setText(R.string.legend_pond_name);
    private static LegendItem river = new LegendItem(GeometryType.LINE, R.string.legend_river, 17)
            .addTag("waterway", "river");
    private static LegendItem intermittent_river = new LegendItem(GeometryType.LINE, R.string.legend_intermittent_river, 17)
            .addTag("waterway", "river").addTag("intermittent", "yes");
    private static LegendItem underground_river = new LegendItem(GeometryType.LINE, R.string.legend_underground_river, 17)
            .addTag("waterway", "river").addTag("tunnel", "yes");
    private static LegendItem stream = new LegendItem(GeometryType.LINE, R.string.legend_stream, 17)
            .addTag("waterway", "stream");
    private static LegendItem ditch = new LegendItem(GeometryType.LINE, R.string.legend_ditch, 17)
            .addTag("waterway", "ditch");
    private static LegendItem waterfall = new LegendItem(GeometryType.POINT, R.string.legend_waterfall, 17)
            .addTag("waterway", "waterfall").addTag("kind_attraction", "yes");
    private static LegendItem dam = new LegendItem(GeometryType.LINE, R.string.legend_dam, 17)
            .addTag("waterway", "dam");
    private static LegendItem lock_gate = new LegendItem(GeometryType.POLY, R.string.legend_lock_gate, 17)
            .addTag("natural", "water").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17)
            .addTag("waterway", "lock_gate").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17)
                            .addTag("waterway", "lock_gate")));
    private static LegendItem weir = new LegendItem(GeometryType.POLY, R.string.legend_weir, 17)
            .addTag("natural", "water").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17)
            .addTag("waterway", "weir").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17)
                            .addTag("waterway", "weir")));
    private static LegendItem ford_point = new LegendItem(GeometryType.LINE, R.string.legend_ford, 17)
            .addTag("waterway", "stream").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17).addTag("ford", "yes"));

    // Land
    private static LegendItem bare_rock = new LegendItem(GeometryType.POLY, R.string.legend_bare_rock, 17)
            .addTag("natural", "bare_rock");
    private static LegendItem scree = new LegendItem(GeometryType.POLY, R.string.legend_scree, 17)
            .addTag("natural", "scree");
    private static LegendItem shingle = new LegendItem(GeometryType.POLY, R.string.legend_shingle, 17)
            .addTag("natural", "shingle");
    private static LegendItem mud = new LegendItem(GeometryType.POLY, R.string.legend_mud, 17)
            .addTag("natural", "mud");
    private static LegendItem sand = new LegendItem(GeometryType.POLY, R.string.legend_sand, 17)
            .addTag("natural", "sand");
    private static LegendItem beach = new LegendItem(GeometryType.POLY, R.string.legend_beach, 17)
            .addTag("natural", "beach").setText(R.string.legend_beach_text);
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
    private static LegendItem cave_entrance = new LegendItem(GeometryType.POINT, R.string.legend_cave_entrance, 17)
            .addTag("natural", "cave_entrance");
    private static LegendItem spring = new LegendItem(GeometryType.POINT, R.string.legend_spring, 17)
            .addTag("natural", "spring");
    private static LegendItem contour = new LegendItem(GeometryType.LINE, R.string.legend_contour, 14)
            .addTag("contour", "elevation_major").setText(R.string.legend_elevation);

    // Vegetation
    private static LegendItem forest = new LegendItem(GeometryType.POLY, R.string.legend_forest, 17)
            .addTag("natural", "forest");
    private static LegendItem marsh = new LegendItem(GeometryType.POLY, R.string.legend_marsh, 17)
            .addTag("natural", "marsh");
    private static LegendItem saltmarsh = new LegendItem(GeometryType.POLY, R.string.legend_wetland_saltmarsh, 17)
            .addTag("natural", "wetland").addTag("wetland", "saltmarsh");
    private static LegendItem reedbed = new LegendItem(GeometryType.POLY, R.string.legend_wetland_reedbed, 17)
            .addTag("natural", "wetland").addTag("wetland", "reedbed");
    private static LegendItem wet_meadow = new LegendItem(GeometryType.POLY, R.string.legend_wetland_wet_meadow, 17)
            .addTag("natural", "wetland").addTag("wetland", "wet_meadow");
    private static LegendItem swamp = new LegendItem(GeometryType.POLY, R.string.legend_wetland_swamp, 17)
            .addTag("natural", "wetland").addTag("wetland", "swamp");
    private static LegendItem mangrove = new LegendItem(GeometryType.POLY, R.string.legend_wetland_mangrove, 17)
            .addTag("natural", "wetland").addTag("wetland", "mangrove");
    private static LegendItem bog = new LegendItem(GeometryType.POLY, R.string.legend_wetland_bog, 17)
            .addTag("natural", "wetland").addTag("wetland", "bog");
    private static LegendItem fen = new LegendItem(GeometryType.POLY, R.string.legend_wetland_fen, 17)
            .addTag("natural", "wetland").addTag("wetland", "fen");
    private static LegendItem tidalflat = new LegendItem(GeometryType.POLY, R.string.legend_wetland_tidalflat, 17)
            .addTag("natural", "wetland").addTag("wetland", "tidalflat");
    private static LegendItem wetland = new LegendItem(GeometryType.POLY, R.string.legend_wetland, 17)
            .addTag("natural", "wetland");
    private static LegendItem tree_row = new LegendItem(GeometryType.LINE, R.string.legend_tree_row, 17)
            .addTag("natural", "tree_row");
    private static LegendItem tree = new LegendItem(GeometryType.POINT, R.string.legend_tree, 17)
            .addTag("natural", "tree");
    private static LegendItem grass = new LegendItem(GeometryType.POLY, R.string.legend_grass, 17)
            .addTag("landuse", "grass"); // we use grass instead of grassland here for urban legend
    private static LegendItem scrub = new LegendItem(GeometryType.POLY, R.string.legend_scrub, 17)
            .addTag("natural", "scrub");
    private static LegendItem heath = new LegendItem(GeometryType.POLY, R.string.legend_heath, 17)
            .addTag("natural", "heath");
    private static LegendItem meadow = new LegendItem(GeometryType.POLY, R.string.legend_meadow, 17)
            .addTag("leisure", "meadow"); // ?

    // Urban
    private static LegendItem hedge = new LegendItem(GeometryType.LINE, R.string.legend_hedge, 17)
            .addTag("barrier", "hedge");
    private static LegendItem fence = new LegendItem(GeometryType.LINE, R.string.legend_fence, 17)
            .addTag("barrier", "fence");
    private static LegendItem wall = new LegendItem(GeometryType.LINE, R.string.legend_wall, 17)
            .addTag("barrier", "wall");
    private static LegendItem city_wall = new LegendItem(GeometryType.LINE, R.string.legend_city_wall, 17)
            .addTag("barrier", "city_wall");
    private static LegendItem retaining_wall = new LegendItem(GeometryType.LINE, R.string.legend_retaining_wall, 17)
            .addTag("barrier", "retaining_wall");
    private static LegendItem embankment = new LegendItem(GeometryType.LINE, R.string.legend_embankment, 17)
            .addTag("man_made", "embankment");
    private static LegendItem building = new LegendItem(GeometryType.POLY, R.string.legend_building, 17)
            .addTag("building", "yes").addTag("kind", "yes").addTag("addr:housenumber", "13").setText(R.string.legend_thirteen).setShape(LegendView.PATH_BUILDING);
    private static LegendItem stadium = new LegendItem(GeometryType.POLY, R.string.legend_stadium, 17)
            .addTag("leisure", "stadium");
    private static LegendItem sports_centre = new LegendItem(GeometryType.POLY, R.string.legend_sports_centre, 17)
            .addTag("leisure", "sports_centre").addTag("kind", "yes").setText(R.string.legend_sports_centre_name);
    private static LegendItem swimming_pool = new LegendItem(GeometryType.POLY, R.string.legend_swimming_pool, 17)
            .addTag("leisure", "swimming_pool").setShape(LegendView.PATH_PLATFORM);
    private static LegendItem garden = new LegendItem(GeometryType.POLY, R.string.legend_garden, 17)
            .addTag("leisure", "garden");
    private static LegendItem camp_site_area = new LegendItem(GeometryType.POLY, R.string.legend_camp_site, 17)
            .addTag("leisure", "camp_site");
    private static LegendItem playground_area = new LegendItem(GeometryType.POLY, R.string.legend_playground, 17)
            .addTag("leisure", "playground").addTag("kind_kids", "yes");
    private static LegendItem pitch = new LegendItem(GeometryType.POLY, R.string.legend_pitch, 17)
            .addTag("leisure", "pitch");
    private static LegendItem dog_park = new LegendItem(GeometryType.POLY, R.string.legend_dog_park, 17)
            .addTag("leisure", "dog_park");
    private static LegendItem cemetery = new LegendItem(GeometryType.POLY, R.string.legend_cemetery, 17)
            .addTag("landuse", "cemetery");
    private static LegendItem fountain = new LegendItem(GeometryType.POINT, R.string.legend_fountain, 17)
            .addTag("amenity", "fountain").addTag("kind_urban", "yes");
    private static LegendItem runway = new LegendItem(GeometryType.LINE, R.string.legend_runway, 17)
            .addTag("aeroway", "runway").setText(R.string.legend_runway_name);
    private static LegendItem apron = new LegendItem(GeometryType.POLY, R.string.legend_apron, 17)
            .addTag("aeroway", "apron");
    private static LegendItem pier = new LegendItem(GeometryType.POLY, R.string.legend_pier, 17)
            .addTag("man_made", "pier").setShape(LegendView.PATH_PIER);
    private static LegendItem bridge = new LegendItem(GeometryType.POLY, R.string.legend_bridge, 17)
            .addTag("man_made", "bridge").setShape(LegendView.PATH_PLATFORM);
    private static LegendItem tower = new LegendItem(GeometryType.POINT, R.string.legend_tower, 17)
            .addTag("man_made", "tower");
    private static LegendItem power_line = new LegendItem(GeometryType.LINE, R.string.legend_power_line, 17)
            .addTag("power", "line");
    private static LegendItem power_tower = new LegendItem(GeometryType.POINT, R.string.legend_power_line, 17)
            .addTag("power", "tower");
    private static LegendItem power_generator_wind = new LegendItem(GeometryType.POINT, R.string.legend_power_generator_wind, 17)
            .addTag("power", "generator").addTag("generator:source", "wind");

    // Roads
    private static LegendItem motorway = new LegendItem(GeometryType.LINE, R.string.legend_motorway, 16)
            .addTag("highway", "motorway").addTag("ref", "A8");
    private static LegendItem trunk_road = new LegendItem(GeometryType.LINE, R.string.legend_trunk_road, 16)
            .addTag("highway", "trunk").addTag("ref", "E95");
    private static LegendItem primary_road = new LegendItem(GeometryType.LINE, R.string.legend_primary_road, 16)
            .addTag("highway", "primary").addTag("ref", "M1");
    private static LegendItem secondary_road = new LegendItem(GeometryType.LINE, R.string.legend_secondary_road, 16)
            .addTag("highway", "secondary").addTag("ref", "L519");
    private static LegendItem tertiary_road = new LegendItem(GeometryType.LINE, R.string.legend_tertiary_road, 16)
            .addTag("highway", "tertiary").addTag("ref", "K9651");
    private static LegendItem unclassified_road = new LegendItem(GeometryType.LINE, R.string.legend_general_road, 16)
            .addTag("highway", "unclassified");
    private static LegendItem residential_road = new LegendItem(GeometryType.LINE, R.string.legend_residental_road, 16)
            .addTag("highway", "residential");
    private static LegendItem service_road = new LegendItem(GeometryType.LINE, R.string.legend_service_road, 16)
            .addTag("highway", "service");
    private static LegendItem oneway_road = new LegendItem(GeometryType.LINE, R.string.legend_oneway_road, 16)
            .addTag("highway", "unclassified").addTag("oneway", "1");
    private static LegendItem private_road = new LegendItem(GeometryType.LINE, R.string.legend_private_road, 16)
            .addTag("highway", "unclassified").addTag("access", "private");
    private static LegendItem no_access_road = new LegendItem(GeometryType.LINE, R.string.legend_noaccess_road, 16)
            .addTag("highway", "unclassified").addTag("access", "no");
    private static LegendItem wd4_road = new LegendItem(GeometryType.LINE, R.string.legend_4wd_road, 16)
            .addTag("highway", "unclassified").addTag("4wd_only", "yes");
    private static LegendItem unpaved_road = new LegendItem(GeometryType.LINE, R.string.legend_unpaved_road, 16)
            .addTag("highway", "unclassified").addTag("surface", "unpaved");
    private static LegendItem dirt_road = new LegendItem(GeometryType.LINE, R.string.legend_dirt_road, 16)
            .addTag("highway", "unclassified").addTag("surface", "dirt");
    private static LegendItem winter_road = new LegendItem(GeometryType.LINE, R.string.legend_winter_road, 16)
            .addTag("highway", "unclassified").addTag("winter_road", "yes");
    private static LegendItem ice_road = new LegendItem(GeometryType.LINE, R.string.legend_ice_road, 16)
            .addTag("highway", "unclassified").addTag("ice_road", "yes");
    private static LegendItem toll_road = new LegendItem(GeometryType.LINE, R.string.legend_toll_road, 16)
            .addTag("highway", "unclassified").addTag("toll", "yes");
    private static LegendItem road_bridge = new LegendItem(GeometryType.LINE, R.string.legend_bridge, 16)
            .addTag("highway", "unclassified").addTag("bridge", "yes");
    private static LegendItem road_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_tunnel, 16)
            .addTag("highway", "unclassified").addTag("tunnel", "yes");
    private static LegendItem construction_road = new LegendItem(GeometryType.LINE, R.string.legend_road_under_construction, 16)
            .addTag("highway", "construction");
    private static LegendItem ford = new LegendItem(GeometryType.LINE, R.string.legend_ford, 16)
            .addTag("highway", "unclassified").addTag("ford", "yes");
    private static LegendItem border_control = new LegendItem(GeometryType.POINT, R.string.legend_border_control, 17)
            .addTag("barrier", "border_control");
    private static LegendItem toll_booth = new LegendItem(GeometryType.POINT, R.string.legend_toll_booth, 17)
            .addTag("barrier", "toll_booth");
    private static LegendItem block = new LegendItem(GeometryType.POINT, R.string.legend_block, 17)
            .addTag("barrier", "block").addTag("kind_barrier", "yes");
    private static LegendItem bollard = new LegendItem(GeometryType.POINT, R.string.legend_bollard, 17)
            .addTag("barrier", "bollard").addTag("kind_barrier", "yes");
    private static LegendItem cycle_barrier = new LegendItem(GeometryType.POINT, R.string.legend_cycle_barrier, 17)
            .addTag("barrier", "cycle_barrier").addTag("kind_barrier", "yes");
    private static LegendItem kissing_gate = new LegendItem(GeometryType.POINT, R.string.legend_kissing_gate, 17)
            .addTag("barrier", "kissing_gate").addTag("kind_barrier", "yes");
    private static LegendItem lift_gate = new LegendItem(GeometryType.POINT, R.string.legend_lift_gate, 17)
            .addTag("barrier", "lift_gate").addTag("kind_barrier", "yes");
    private static LegendItem stile = new LegendItem(GeometryType.POINT, R.string.legend_stile, 17)
            .addTag("barrier", "stile").addTag("kind_barrier", "yes");
    private static LegendItem gate = new LegendItem(GeometryType.POINT, R.string.legend_gate, 17)
            .addTag("barrier", "gate").addTag("kind_barrier", "yes");
    private static LegendItem highway_services = new LegendItem(GeometryType.POLY, R.string.legend_highway_services, 17)
            .addTag("highway", "services");
    private static LegendItem rest_area = new LegendItem(GeometryType.POLY, R.string.legend_rest_area, 17)
            .addTag("amenity", "rest_area");

    // Tracks
    private static LegendItem track = new LegendItem(GeometryType.LINE, R.string.legend_track, 17)
            .addTag("highway", "track");
    private static LegendItem track_bridge = new LegendItem(GeometryType.LINE, R.string.legend_track_bridge, 17)
            .addTag("highway", "track").addTag("bridge", "yes");
    private static LegendItem good_track = new LegendItem(GeometryType.LINE, R.string.legend_good_track, 17)
            .addTag("highway", "track").addTag("smoothness", "good");
    private static LegendItem very_bad_track = new LegendItem(GeometryType.LINE, R.string.legend_very_bad_track, 17)
            .addTag("highway", "track").addTag("smoothness", "very_bad");
    private static LegendItem horrible_track = new LegendItem(GeometryType.LINE, R.string.legend_horrible_track, 17)
            .addTag("highway", "track").addTag("smoothness", "horrible");
    private static LegendItem very_horrible_track = new LegendItem(GeometryType.LINE, R.string.legend_very_horrible_track, 17)
            .addTag("highway", "track").addTag("smoothness", "very_horrible");
    private static LegendItem impassable_track = new LegendItem(GeometryType.LINE, R.string.legend_impassable_track, 17)
            .addTag("highway", "track").addTag("smoothness", "impassable");
    private static LegendItem winter_track = new LegendItem(GeometryType.LINE, R.string.legend_winter_track, 17)
            .addTag("highway", "track").addTag("winter_road", "yes");
    private static LegendItem ice_track = new LegendItem(GeometryType.LINE, R.string.legend_ice_track, 17)
            .addTag("highway", "track").addTag("ice_road", "yes");
    private static LegendItem ford_track = new LegendItem(GeometryType.LINE, R.string.legend_track_ford, 17)
            .addTag("highway", "track").addTag("ford", "yes");
    private static LegendItem bridleway = new LegendItem(GeometryType.LINE, R.string.legend_bridleway, 17)
            .addTag("highway", "bridleway");

    // Pedestrian ways
    private static LegendItem pedestrian_area = new LegendItem(GeometryType.POLY, R.string.legend_pedestrian_area, 17)
            .addTag("highway", "pedestrian"); //.addTag("area", "yes");
    private static LegendItem pedestrian_road = new LegendItem(GeometryType.LINE, R.string.legend_pedestrian_road, 17)
            .addTag("highway", "pedestrian");
    private static LegendItem footway = new LegendItem(GeometryType.LINE, R.string.legend_footway, 17)
            .addTag("highway", "footway");
    private static LegendItem path = new LegendItem(GeometryType.LINE, R.string.legend_path, 17)
            .addTag("highway", "path");
    private static LegendItem path_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_path_visibility_good, 17)
            .addTag("highway", "path").addTag("trail_visibility", "good");
    private static LegendItem path_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_path_visibility_bad, 17)
            .addTag("highway", "path").addTag("trail_visibility", "bad");
    private static LegendItem no_access_path = new LegendItem(GeometryType.LINE, R.string.legend_noaccess_path, 17)
            .addTag("highway", "path").addTag("access", "no");
    private static LegendItem footway_bridge = new LegendItem(GeometryType.LINE, R.string.legend_footway_bridge, 17)
            .addTag("highway", "footway").addTag("bridge", "yes");
    private static LegendItem steps = new LegendItem(GeometryType.LINE, R.string.legend_steps, 17)
            .addTag("highway", "steps");
    private static LegendItem via_ferrata = new LegendItem(GeometryType.LINE, R.string.legend_via_ferrata, 17)
            .addTag("highway", "via_ferrata");
    private static LegendItem cycleway = new LegendItem(GeometryType.LINE, R.string.legend_cycleway, 17)
            .addTag("highway", "cycleway");

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
            .addTag("railway", "platform").setShape(LegendView.PATH_PLATFORM);
    private static LegendItem railway_station = new LegendItem(GeometryType.POINT, R.string.legend_railway_station, 17)
            .addTag("railway", "station").setText(R.string.legend_railway_station_name);
    private static LegendItem railway_halt = new LegendItem(GeometryType.POINT, R.string.legend_railway_halt, 17)
            .addTag("railway", "halt").setText(R.string.legend_railway_halt_name);
    private static LegendItem railway_level_crossing = new LegendItem(GeometryType.POINT, R.string.legend_level_crossing, 17)
            .addTag("railway", "level_crossing");
    private static LegendItem railway_crossing = new LegendItem(GeometryType.POINT, R.string.legend_pedestrian_crossing, 17)
            .addTag("railway", "crossing");

    // Transportation
    private static LegendItem bus_station = new LegendItem(GeometryType.POINT, R.string.legend_bus_station, 17)
            .addTag("amenity", "bus_station").addTag("kind_transportation", "yes");
    private static LegendItem bus_stop = new LegendItem(GeometryType.POINT, R.string.legend_bus_stop, 17)
            .addTag("highway", "bus_stop").addTag("kind_transportation", "yes").setText(R.string.legend_bus_stop_name);
    private static LegendItem tram_stop = new LegendItem(GeometryType.POINT, R.string.legend_tram_stop, 17)
            .addTag("railway", "tram_stop").addTag("kind_transportation", "yes").setText(R.string.legend_tram_stop_name);
    private static LegendItem subway_entrance = new LegendItem(GeometryType.POINT, R.string.legend_subway_entrance, 17)
            .addTag("railway", "subway_entrance").addTag("kind_transportation", "yes");
    private static LegendItem subway_station = new LegendItem(GeometryType.POINT, R.string.legend_subway_station, 15)
            .addTag("railway", "station").addTag("station", "subway").setText(R.string.legend_subway_station_name);
    private static LegendItem aeroway_aerodrome = new LegendItem(GeometryType.POINT, R.string.legend_aerodrome, 17)
            .addTag("aeroway", "aerodrome").setText(R.string.legend_aerodrome_name);
    private static LegendItem aeroway_heliport = new LegendItem(GeometryType.POINT, R.string.legend_heliport, 17)
            .addTag("aeroway", "heliport");
    private static LegendItem ferry = new LegendItem(GeometryType.LINE, R.string.legend_ferry, 17)
            .addTag("route", "ferry");
    private static LegendItem ferry_terminal = new LegendItem(GeometryType.POINT, R.string.legend_ferry_terminal, 17)
            .addTag("amenity", "ferry_terminal");

    // Pistes
    private static LegendItem piste_downhill_novice = new LegendItem(GeometryType.POLY, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "novice").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "novice"));
    private static LegendItem piste_downhill_easy = new LegendItem(GeometryType.POLY, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "easy").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "easy"));
    private static LegendItem piste_downhill_intermediate = new LegendItem(GeometryType.POLY, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "intermediate").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "intermediate"));
    private static LegendItem piste_downhill_advanced = new LegendItem(GeometryType.POLY, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "advanced").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "advanced"));
    private static LegendItem piste_downhill_expert = new LegendItem(GeometryType.POLY, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "expert").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "expert"));
    private static LegendItem piste_downhill_freeride = new LegendItem(GeometryType.POLY, R.string.legend_free_ride, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "freeride").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "freeride"));
    private static LegendItem piste_downhill_unknown = new LegendItem(GeometryType.POLY, R.string.legend_unknown_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown"));
    private static LegendItem piste_downhill_mogul = new LegendItem(GeometryType.POLY, R.string.legend_mogul, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").addTag("piste:grooming", "mogul").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown"));
    private static LegendItem piste_downhill_lit = new LegendItem(GeometryType.POLY, R.string.legend_lit_piste, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 15)
                            .addTag("piste:type", "downhill").addTag("piste:lit", "yes").setTotalSymbols(4)));
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
            .addTag("piste:type", "snow_park").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "snow_park"));
    private static LegendItem piste_playground = new LegendItem(GeometryType.POLY, R.string.legend_kids_playground, 15)
            .addTag("piste:type", "playground").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "playground"));
    private static LegendItem piste_ice_skate = new LegendItem(GeometryType.POLY, R.string.legend_ice_rink, 15)
            .addTag("piste:type", "ice_skate");
    private static LegendItem piste_ski_jump = new LegendItem(GeometryType.LINE, R.string.legend_ski_jump, 15)
            .addTag("piste:type", "ski_jump");
    private static LegendItem piste_ski_jump_landing = new LegendItem(GeometryType.POLY, R.string.legend_ski_jump_landing_zone, 15)
            .addTag("piste:type", "ski_jump_landing");
    private static LegendItem piste_ski_tour = new LegendItem(GeometryType.LINE, R.string.legend_ski_tour, 15)
            .addTag("piste:type", "skitour").setTotalSymbols(3);

    // Aerial cableways
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

    // Hiking

    private static LegendItem hiking_route_iwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_iwn, 14)
            .addTag("route", "hiking").addTag("network", "iwn");
    private static LegendItem hiking_route_nwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_nwn, 14)
            .addTag("route", "hiking").addTag("network", "nwn");
    private static LegendItem hiking_route_rwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_rwn, 14)
            .addTag("route", "hiking").addTag("network", "rwn");
    private static LegendItem hiking_route_lwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_lwn, 14)
            .addTag("route", "hiking").addTag("network", "lwn");
    private static LegendItem hiking_route_symbol = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_symbol, 14)
            .addTag("route", "hiking").addTag("network", "iwn").addTag("osmc:symbol", "blue:blue:shell_modern");

    private static LegendItem hiking_path_with_route = new LegendItem(GeometryType.LINE, R.string.legend_hiking_path_with_route, 14)
            .addTag("highway", "path").addTag("route:network", "iwn");
    private static LegendItem hiking_road_with_route = new LegendItem(GeometryType.LINE, R.string.legend_hiking_road_with_route, 17)
            .addTag("highway", "unclassified").addTag("route:network", "iwn");

    private static LegendItem hiking_path_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path");
    private static LegendItem hiking_path_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("trail_visibility", "no");

    private static LegendItem hiking_path_sac_scale_t1_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1");
    private static LegendItem hiking_path_sac_scale_t1_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t1_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t1_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t1_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t1_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "no");
    private static LegendItem hiking_path_sac_scale_t2_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2");
    private static LegendItem hiking_path_sac_scale_t2_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t2_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t2_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t2_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t2_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "no");
    private static LegendItem hiking_path_sac_scale_t3_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3");
    private static LegendItem hiking_path_sac_scale_t3_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t3_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t3_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t3_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t3_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "no");
    private static LegendItem hiking_path_sac_scale_t4_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4");
    private static LegendItem hiking_path_sac_scale_t4_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t4_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t4_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t4_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t4_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "no");
    private static LegendItem hiking_path_sac_scale_t5_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5");
    private static LegendItem hiking_path_sac_scale_t5_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t5_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t5_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t5_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t5_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "no");
    private static LegendItem hiking_path_sac_scale_t6_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6");
    private static LegendItem hiking_path_sac_scale_t6_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "excellent");
    private static LegendItem hiking_path_sac_scale_t6_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "good");
    private static LegendItem hiking_path_sac_scale_t6_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "intermediate");
    private static LegendItem hiking_path_sac_scale_t6_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "bad");
    private static LegendItem hiking_path_sac_scale_t6_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 14)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "no");

    // POI

    // Accommodation
    private static LegendItem wilderness_hut = new LegendItem(GeometryType.POINT, R.string.legend_wilderness_hut, 17)
            .addTag("tourism", "wilderness_hut").addTag("kind_accommodation", "yes");
    private static LegendItem alpine_hut = new LegendItem(GeometryType.POINT, R.string.legend_alpine_hut, 17)
            .addTag("tourism", "alpine_hut").addTag("kind_accommodation", "yes");
    private static LegendItem guest_house = new LegendItem(GeometryType.POINT, R.string.legend_guest_house, 17)
            .addTag("tourism", "guest_house").addTag("kind_accommodation", "yes");
    private static LegendItem chalet = new LegendItem(GeometryType.POINT, R.string.legend_chalet, 17)
            .addTag("tourism", "chalet").addTag("kind_accommodation", "yes");
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
    private static LegendItem theme_park = new LegendItem(GeometryType.POINT, R.string.legend_theme_park, 17)
            .addTag("tourism", "theme_park").addTag("kind_entertainment", "yes");
    private static LegendItem picnic_site = new LegendItem(GeometryType.POINT, R.string.legend_picnic_site, 17)
            .addTag("tourism", "picnic_site").addTag("kind_entertainment", "yes");
    private static LegendItem theatre = new LegendItem(GeometryType.POINT, R.string.legend_theatre, 17)
            .addTag("amenity", "theatre").addTag("kind_entertainment", "yes");
    private static LegendItem cinema = new LegendItem(GeometryType.POINT, R.string.legend_cinema, 17)
            .addTag("amenity", "cinema").addTag("kind_entertainment", "yes");
    private static LegendItem library = new LegendItem(GeometryType.POINT, R.string.legend_library, 17)
            .addTag("amenity", "library").addTag("kind_entertainment", "yes");
    private static LegendItem water_park = new LegendItem(GeometryType.POINT, R.string.legend_water_park, 17)
            .addTag("leisure", "water_park").addTag("kind_entertainment", "yes");
    private static LegendItem beach_resort = new LegendItem(GeometryType.POINT, R.string.legend_beach_resort, 17)
            .addTag("leisure", "beach_resort").addTag("kind_entertainment", "yes");
    private static LegendItem boat_rental = new LegendItem(GeometryType.POINT, R.string.legend_boat_rental, 17)
            .addTag("amenity", "boat_rental").addTag("kind_entertainment", "yes");
    private static LegendItem horse_riding = new LegendItem(GeometryType.POINT, R.string.legend_horse_riding, 17)
            .addTag("leisure", "horse_riding").addTag("kind_entertainment", "yes");

    private static LegendItem embassy = new LegendItem(GeometryType.POINT, R.string.legend_embassy, 17)
            .addTag("diplomatic", "embassy").addTag("kind_emergency", "yes");
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
    private static LegendItem dentist = new LegendItem(GeometryType.POINT, R.string.legend_dentist, 17)
            .addTag("amenity", "dentist").addTag("kind_emergency", "yes");
    private static LegendItem pharmacy = new LegendItem(GeometryType.POINT, R.string.legend_pharmacy, 17)
            .addTag("amenity", "pharmacy").addTag("kind_emergency", "yes");
    private static LegendItem telephone = new LegendItem(GeometryType.POINT, R.string.legend_telephone, 17)
            .addTag("amenity", "telephone").addTag("kind_emergency", "yes");
    private static LegendItem emergency_telephone = new LegendItem(GeometryType.POINT, R.string.legend_emergency_telephone, 17)
            .addTag("emergency", "phone").addTag("kind_emergency", "yes");

    private static LegendItem sauna = new LegendItem(GeometryType.POINT, R.string.legend_sauna, 17)
            .addTag("leisure", "sauna").addTag("kind_healthbeauty", "yes");
    private static LegendItem massage = new LegendItem(GeometryType.POINT, R.string.legend_massage, 17)
            .addTag("shop", "massage").addTag("kind_healthbeauty", "yes");
    private static LegendItem hairdresser = new LegendItem(GeometryType.POINT, R.string.legend_hairdresser, 17)
            .addTag("shop", "hairdresser").addTag("kind_healthbeauty", "yes");

    private static LegendItem pet_shop = new LegendItem(GeometryType.POINT, R.string.legend_pet_shop, 17)
            .addTag("shop", "pet").addTag("kind_pets", "yes");
    private static LegendItem veterinary = new LegendItem(GeometryType.POINT, R.string.legend_veterinary_clinic, 17)
            .addTag("amenity", "veterinary").addTag("kind_pets", "yes");

    private static LegendItem toys = new LegendItem(GeometryType.POINT, R.string.legend_toys_shop, 17)
            .addTag("shop", "toys").addTag("kind_kids", "yes");
    private static LegendItem amusement_arcade = new LegendItem(GeometryType.POINT, R.string.legend_amusement_arcade, 17)
            .addTag("leisure", "amusement_arcade").addTag("kind_kids", "yes");
    private static LegendItem playground = new LegendItem(GeometryType.POINT, R.string.legend_playground, 17)
            .addTag("leisure", "playground").addTag("kind_kids", "yes");

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
    private static LegendItem doityourself = new LegendItem(GeometryType.POINT, R.string.legend_diy_store, 17)
            .addTag("shop", "doityourself").addTag("kind_shopping", "yes");
    private static LegendItem department_store = new LegendItem(GeometryType.POINT, R.string.legend_department_store, 17)
            .addTag("shop", "department_store").addTag("kind_shopping", "yes");

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
    private static LegendItem shower = new LegendItem(GeometryType.POINT, R.string.legend_shower, 17)
            .addTag("amenity", "shower").addTag("kind_service", "yes");

    private static LegendItem lighthouse = new LegendItem(GeometryType.POINT, R.string.legend_lighthouse, 17)
            .addTag("man_made", "lighthouse").addTag("kind_attraction", "yes");
    private static LegendItem windmill = new LegendItem(GeometryType.POINT, R.string.legend_windmill, 17)
            .addTag("man_made", "windmill").addTag("kind_attraction", "yes");
    private static LegendItem museum = new LegendItem(GeometryType.POINT, R.string.legend_museum, 17)
            .addTag("tourism", "museum").addTag("kind_attraction", "yes");
    private static LegendItem gallery = new LegendItem(GeometryType.POINT, R.string.legend_gallery, 17)
            .addTag("tourism", "gallery").addTag("kind_attraction", "yes");
    private static LegendItem castle = new LegendItem(GeometryType.POINT, R.string.legend_castle, 17)
            .addTag("historic", "castle").addTag("kind_attraction", "yes");
    private static LegendItem attraction = new LegendItem(GeometryType.POINT, R.string.legend_attraction, 17)
            .addTag("tourism", "attraction").addTag("kind_attraction", "yes");
    private static LegendItem viewpoint = new LegendItem(GeometryType.POINT, R.string.legend_viewpoint, 17)
            .addTag("tourism", "viewpoint").addTag("kind_attraction", "yes");
    private static LegendItem artwork = new LegendItem(GeometryType.POINT, R.string.legend_artwork, 17)
            .addTag("tourism", "artwork").addTag("kind_attraction", "yes");
    private static LegendItem bust = new LegendItem(GeometryType.POINT, R.string.legend_bust, 17)
            .addTag("memorial", "bust").addTag("kind_attraction", "yes");
    private static LegendItem statue = new LegendItem(GeometryType.POINT, R.string.legend_statue, 17)
            .addTag("memorial", "statue").addTag("kind_attraction", "yes");
    private static LegendItem memorial = new LegendItem(GeometryType.POINT, R.string.legend_memorial, 17)
            .addTag("historic", "memorial").addTag("kind_attraction", "yes");
    private static LegendItem stone = new LegendItem(GeometryType.POINT, R.string.legend_stone, 17)
            .addTag("memorial", "stone").addTag("kind_attraction", "yes");
    private static LegendItem plaque = new LegendItem(GeometryType.POINT, R.string.legend_plaque, 17)
            .addTag("memorial", "plaque").addTag("kind_attraction", "yes");
    private static LegendItem monument = new LegendItem(GeometryType.POINT, R.string.legend_monument, 17)
            .addTag("historic", "monument").addTag("kind_attraction", "yes");
    private static LegendItem archaeological_site = new LegendItem(GeometryType.POINT, R.string.legend_archaeological_site, 17)
            .addTag("historic", "archaeological_site").addTag("kind_attraction", "yes");
    private static LegendItem ruins = new LegendItem(GeometryType.POINT, R.string.legend_ruins, 17)
            .addTag("historic", "ruins").addTag("kind_attraction", "yes");
    private static LegendItem wayside_shrine = new LegendItem(GeometryType.POINT, R.string.legend_wayside_shrine, 17)
            .addTag("historic", "wayside_shrine").addTag("kind_attraction", "yes");

    private static LegendItem car = new LegendItem(GeometryType.POINT, R.string.legend_car_dialer, 17)
            .addTag("shop", "car").addTag("kind_vehicles", "yes");
    private static LegendItem car_repair = new LegendItem(GeometryType.POINT, R.string.legend_car_repair, 17)
            .addTag("shop", "car_repair").addTag("kind_vehicles", "yes");
    private static LegendItem car_rental = new LegendItem(GeometryType.POINT, R.string.legend_car_rental, 17)
            .addTag("amenity", "car_rental").addTag("kind_vehicles", "yes");
    private static LegendItem fuel = new LegendItem(GeometryType.POINT, R.string.legend_fuel_station, 17)
            .addTag("amenity", "fuel").addTag("kind_vehicles", "yes");
    private static LegendItem slipway = new LegendItem(GeometryType.POINT, R.string.legend_slipway, 17)
            .addTag("amenity", "slipway").addTag("kind_vehicles", "yes");
    private static LegendItem parking_point = new LegendItem(GeometryType.POINT, R.string.legend_parking, 17)
            .addTag("amenity", "parking").addTag("kind_vehicles", "yes");
    private static LegendItem parking = new LegendItem(GeometryType.POLY, R.string.legend_parking, 17)
            .addTag("amenity", "parking").addTag("kind_vehicles", "yes");
    private static LegendItem parking_unpaved = new LegendItem(GeometryType.POLY, R.string.legend_unpaved_parking, 17)
            .addTag("amenity", "parking").addTag("surface", "unpaved").addTag("kind_vehicles", "yes");
    private static LegendItem parking_dirt = new LegendItem(GeometryType.POLY, R.string.legend_dirt_parking, 17)
            .addTag("amenity", "parking").addTag("surface", "dirt").addTag("kind_vehicles", "yes");
    private static LegendItem parking_car_paid = new LegendItem(GeometryType.POLY, R.string.legend_paid_parking, 17)
            .addTag("amenity", "parking").addTag("fee", "yes").addTag("kind_vehicles", "yes");
    private static LegendItem parking_private = new LegendItem(GeometryType.POLY, R.string.legend_private_parking, 17)
            .addTag("amenity", "parking").addTag("access", "private").addTag("kind_vehicles", "yes");

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
    private static LegendItem firepit = new LegendItem(GeometryType.POINT, R.string.legend_firepit, 17)
            .addTag("leisure", "firepit").addTag("kind_hikebike", "yes");
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

    private static HashSet<LegendItem> notRoadItems = new HashSet<>(Arrays.asList(
            educational, recreation, construction, hospital_area, military, stream, ditch, grass,
            forest, tree_row, tree, beach, wall, retaining_wall, fence, hedge, power_generator_wind,
            runway, apron, railway_platform, bridge, pier, pitch, sports_centre, stadium, garden,
            camp_site_area, zoo_area, theme_park_area, dog_park, cemetery, cycleway,
            railway_crossing, bus_station, subway_entrance, subway_station, railway_station,
            railway_halt, aeroway_aerodrome, aeroway_heliport, embankment
    ));

    private static HashSet<LegendItem> notUrbanItems = new HashSet<>(Arrays.asList(
            farmland, orchard, plant_nursery, farmyard, quarry, nature_reserve, underground_river,
            dam, lock_gate, weir, ford_point, meadow, scrub, heath, wetland, reedbed, wet_meadow,
            swamp, mangrove, bog, fen, marsh, saltmarsh, tidalflat, bare_rock, scree, shingle, mud,
            sand, glacier, cliff, peak, volcano, saddle, cave_entrance, contour, power_line, tower,
            highway_services
    ));

    private static HashSet<LegendItem> notNightItems = new HashSet<>(Arrays.asList(
            recreation, construction, farmland, orchard, plant_nursery, farmyard, quarry,
            underground_river, grass, meadow, scrub, heath, reedbed, wet_meadow, swamp, mangrove,
            bog, fen, marsh, saltmarsh, tidalflat, bare_rock, scree, shingle, sand, beach, glacier,
            contour, pitch, sports_centre, stadium, building, garden, marina, theme_park_area,
            camp_site_area, zoo_area, runway, apron, dog_park, cemetery, cycleway, railway_tunnel,
            tram, railway_crossing, ferry, highway_services
    ));

    private static HashSet<LegendItem> notWinterItems = new HashSet<>(Arrays.asList(
            ferry, unpaved_road, dirt_road, parking_unpaved, parking_dirt, cycleway
    ));

    private static LegendSection administrative = new LegendSection(R.string.legend_administrative, new LegendItem[]{
            country,
            state,
            country_boundary,
            region_boundary,
            province_boundary,
            capital,
            city,
            town,
            suburb,
            village,
            allotment,
            island,
            locality
    });

    private static LegendSection land_use = new LegendSection(R.string.legend_land_use, new LegendItem[]{
            residential,
            educational,
            recreation,
            industrial,
            construction,
            hospital_area,
            aerodrome,
            allotments,
            quarry,
            farmland,
            farmyard,
            orchard,
            plant_nursery,
            nature_reserve,
            military,
            marina
    });

    private static LegendSection water_features = new LegendSection(R.string.legend_water, new LegendItem[]{
            water,
            river,
            intermittent_river,
            underground_river,
            stream,
            ditch,
            dam,
            lock_gate,
            weir,
            ford_point
    });

    private static LegendSection terrain_features = new LegendSection(R.string.legend_terrain_features, new LegendItem[]{
            grass,
            meadow,
            forest,
            tree_row,
            tree,
            scrub,
            heath,
            wetland,
            reedbed,
            wet_meadow,
            swamp,
            mangrove,
            bog,
            fen,
            marsh,
            saltmarsh,
            tidalflat,
            bare_rock,
            scree,
            shingle,
            mud,
            sand,
            glacier,
            cliff,
            peak,
            volcano,
            saddle,
            mountain_pass,
            cave_entrance,
            spring,
            contour
    });

    private static LegendSection manmade_features = new LegendSection(R.string.legend_manmade_features, new LegendItem[]{
            city_wall,
            wall,
            embankment,
            retaining_wall,
            fence,
            hedge,
            power_line.setOverlay(power_tower),
            power_generator_wind,
            tower,
            building,
            runway,
            apron,
            railway_platform,
            bridge,
            pier
    });

    private static LegendSection urban_features = new LegendSection(R.string.legend_urban, new LegendItem[]{
            pitch, //?
            sports_centre,
            stadium,
            swimming_pool,
            garden,
            beach,
            camp_site_area,
            playground_area,
            theme_park_area,
            zoo_area,
            dog_park,
            cemetery
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
            road_bridge,
            road_tunnel,
            ford,
            construction_road,
            parking,
            parking_car_paid,
            parking_private,
            parking_unpaved,
            highway_services,
            rest_area,
            parking_dirt,
            border_control,
            toll_booth,
            lift_gate,
            kissing_gate,
            gate,
            stile,
            block,
            bollard,
            cycleway,
            cycle_barrier
    });

    private static LegendSection tracks = new LegendSection(R.string.legend_tracks, new LegendItem[]{
            track,
            track_bridge,
            winter_track,
            ice_track,
            ford_track,
            good_track,
            very_bad_track,
            horrible_track,
            very_horrible_track,
            impassable_track,
            bridleway
    });

    private static LegendSection pedestrian = new LegendSection(R.string.legend_pedestrian, new LegendItem[]{
            pedestrian_area,
            pedestrian_road,
            footway,
            path,
            path_visibility_good,
            path_visibility_bad,
            no_access_path,
            footway_bridge,
            steps,
            via_ferrata // TODO Should go to hiking
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

    private static LegendSection aerial_ways = new LegendSection(R.string.legend_aerial_ways, new LegendItem[]{
            cable_car,
            gondola,
            chair_lift,
            drag_lift,
            zip_line,
            magic_carpet,
            aerialway_station
    });

    private static LegendSection transportation = new LegendSection(R.string.legend_transportation, new LegendItem[]{
            bus_station,
            bus_stop,
            tram_stop,
            subway_entrance,
            subway_station,
            railway_station,
            railway_halt,
            aeroway_aerodrome,
            aeroway_heliport,
            ferry_terminal,
            ferry
    });

    private static LegendSection amenities_accommodation = new LegendSection(R.string.kind_accommodation, new LegendItem[]{
            wilderness_hut,
            alpine_hut,
            guest_house,
            chalet,
            motel,
            hostel,
            hotel,
            camp_site,
            caravan_site
    });

    private static LegendSection amenities_food = new LegendSection(R.string.kind_food, new LegendItem[]{
            ice_cream,
            confectionery,
            alcohol,
            beverages,
            bakery,
            greengrocer,
            supermarket,
            marketplace,
            cafe,
            pub,
            bar,
            fast_food,
            restaurant
    });


    private static LegendSection amenities_entertainment = new LegendSection(R.string.kind_entertainment, new LegendItem[]{
            zoo,
            theme_park,
            theatre,
            cinema,
            library,
            picnic_site,
            boat_rental,
            horse_riding,
            water_park,
            beach_resort
    });

    private static LegendSection amenities_emergency = new LegendSection(R.string.kind_emergency, new LegendItem[]{
            hospital,
            doctors,
            dentist,
            pharmacy,
            embassy,
            police,
            fire_station,
            ranger_station,
            telephone,
            emergency_telephone
    });

    private static LegendSection amenities_healthbeauty = new LegendSection(R.string.kind_healthbeauty, new LegendItem[]{
            hairdresser,
            sauna,
            massage
    });

    private static LegendSection amenities_pets = new LegendSection(R.string.kind_pets, new LegendItem[]{
            pet_shop,
            veterinary
    });

    private static LegendSection amenities_kids = new LegendSection(R.string.kind_kids, new LegendItem[]{
            toys,
            amusement_arcade,
            playground
    });

    private static LegendSection amenities_shopping = new LegendSection(R.string.kind_shopping, new LegendItem[]{
            sports,
            gift,
            jewelry,
            photo,
            books,
            variety_store,
            supermarket,
            doityourself,
            department_store,
            marketplace
    });

    private static LegendSection amenities_attraction = new LegendSection(R.string.kind_attraction, new LegendItem[]{
            waterfall,
            lighthouse,
            windmill,
            museum,
            gallery,
            castle,
            monument,
            statue,
            bust,
            memorial,
            archaeological_site,
            ruins,
            artwork,
            stone,
            plaque,
            wayside_shrine,
            viewpoint,
            attraction
    });

    private static LegendSection amenities_urban = new LegendSection(R.string.kind_urban, new LegendItem[]{
            fountain
    });

    private static LegendSection amenities_vehicles = new LegendSection(R.string.kind_vehicles, new LegendItem[]{
            car,
            car_repair,
            car_rental,
            fuel,
            slipway,
            parking_point
    });

    private static LegendSection amenities_religion = new LegendSection(R.string.kind_religion, new LegendItem[]{
            place_of_worship,
            buddhist,
            christian,
            hindu,
            jewish,
            muslim,
            shinto,
            sikh,
            taoist
    });

    private static LegendSection amenities_hikebike = new LegendSection(R.string.kind_hikebike, new LegendItem[]{
            bicycle,
            outdoor,
            bicycle_rental,
            drinking_water,
            shelter,
            toilets,
            firepit,
            information_office,
            information_guidepost,
            information_map,
            information
    });

    private static LegendSection amenities_service = new LegendSection(R.string.kind_service, new LegendItem[]{
            copyshop,
            laundry,
            shower,
            post_office,
            post_box,
            bank,
            atm,
            bureau_de_change
    });

    private static LegendSection hiking_routes = new LegendSection(R.string.legend_hiking_routes, new LegendItem[]{
            hiking_route_iwn,
            hiking_route_nwn,
            hiking_route_rwn,
            hiking_route_lwn,
            hiking_route_symbol,
            hiking_path_with_route,
            hiking_road_with_route
    });

    private static LegendSection hiking_path_visibility = new LegendSection(R.string.legend_hiking_visibility, new LegendItem[]{
            hiking_path_visibility_unknown,
            hiking_path_visibility_excellent,
            hiking_path_visibility_good,
            hiking_path_visibility_intermediate,
            hiking_path_visibility_bad,
            hiking_path_visibility_no
    });

    private static LegendSection hiking_sac_scale_t1 = new LegendSection(R.string.legend_hiking_sac_scale_t1, new LegendItem[]{
            hiking_path_sac_scale_t1_visibility_unknown,
            hiking_path_sac_scale_t1_visibility_excellent,
            hiking_path_sac_scale_t1_visibility_good,
            hiking_path_sac_scale_t1_visibility_intermediate,
            hiking_path_sac_scale_t1_visibility_bad,
            hiking_path_sac_scale_t1_visibility_no
    });

    private static LegendSection hiking_sac_scale_t2 = new LegendSection(R.string.legend_hiking_sac_scale_t2, new LegendItem[]{
            hiking_path_sac_scale_t2_visibility_unknown,
            hiking_path_sac_scale_t2_visibility_excellent,
            hiking_path_sac_scale_t2_visibility_good,
            hiking_path_sac_scale_t2_visibility_intermediate,
            hiking_path_sac_scale_t2_visibility_bad,
            hiking_path_sac_scale_t2_visibility_no
    });

    private static LegendSection hiking_sac_scale_t3 = new LegendSection(R.string.legend_hiking_sac_scale_t3, new LegendItem[]{
            hiking_path_sac_scale_t3_visibility_unknown,
            hiking_path_sac_scale_t3_visibility_excellent,
            hiking_path_sac_scale_t3_visibility_good,
            hiking_path_sac_scale_t3_visibility_intermediate,
            hiking_path_sac_scale_t3_visibility_bad,
            hiking_path_sac_scale_t3_visibility_no
    });

    private static LegendSection hiking_sac_scale_t4 = new LegendSection(R.string.legend_hiking_sac_scale_t4, new LegendItem[]{
            hiking_path_sac_scale_t4_visibility_unknown,
            hiking_path_sac_scale_t4_visibility_excellent,
            hiking_path_sac_scale_t4_visibility_good,
            hiking_path_sac_scale_t4_visibility_intermediate,
            hiking_path_sac_scale_t4_visibility_bad,
            hiking_path_sac_scale_t4_visibility_no
    });

    private static LegendSection hiking_sac_scale_t5 = new LegendSection(R.string.legend_hiking_sac_scale_t5, new LegendItem[]{
            hiking_path_sac_scale_t5_visibility_unknown,
            hiking_path_sac_scale_t5_visibility_excellent,
            hiking_path_sac_scale_t5_visibility_good,
            hiking_path_sac_scale_t5_visibility_intermediate,
            hiking_path_sac_scale_t5_visibility_bad,
            hiking_path_sac_scale_t5_visibility_no
    });

    private static LegendSection hiking_sac_scale_t6 = new LegendSection(R.string.legend_hiking_sac_scale_t6, new LegendItem[]{
            hiking_path_sac_scale_t6_visibility_unknown,
            hiking_path_sac_scale_t6_visibility_excellent,
            hiking_path_sac_scale_t6_visibility_good,
            hiking_path_sac_scale_t6_visibility_intermediate,
            hiking_path_sac_scale_t6_visibility_bad,
            hiking_path_sac_scale_t6_visibility_no
    });

    private static LegendSection[] themeTopo = new LegendSection[]{
            administrative,
            land_use,
            water_features,
            terrain_features,
            manmade_features,
            urban_features,
            roads,
            tracks,
            pedestrian,
            railways,
            aerial_ways,
            transportation,
            amenities_emergency,
            amenities_accommodation,
            amenities_food,
            amenities_attraction,
            amenities_entertainment,
            amenities_healthbeauty,
            amenities_shopping,
            amenities_service,
            amenities_religion,
            amenities_kids,
            amenities_pets,
            amenities_vehicles,
            amenities_hikebike,
            amenities_urban
    };

    private static LegendSection[] themeNight = new LegendSection[]{
            administrative,
            land_use,
            water_features,
            terrain_features,
            manmade_features,
            urban_features,
            roads,
            railways,
            transportation,
            amenities_emergency,
            amenities_accommodation,
            amenities_food,
            amenities_attraction,
            amenities_entertainment,
            amenities_healthbeauty,
            amenities_shopping,
            amenities_service,
            amenities_religion,
            amenities_kids,
            amenities_pets,
            amenities_vehicles,
            amenities_hikebike,
            amenities_urban
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
                    sports_centre,
                    theme_park_area
            }),
            aerial_ways,
            administrative,
            new LegendSection(R.string.legend_terrain_features, new LegendItem[]{
                    glacier,
                    water,
                    river,
                    stream,
                    forest,
                    tree_row,
                    tree,
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
            }),
            transportation,
            roads,
            pedestrian,
            railways,
            amenities_emergency,
            amenities_accommodation,
            amenities_food,
            amenities_attraction,
            amenities_entertainment,
            amenities_healthbeauty,
            amenities_shopping,
            amenities_service,
            amenities_religion,
            amenities_kids,
            amenities_pets,
            amenities_vehicles,
            amenities_hikebike,
            amenities_urban
    };

    private Legend.LegendListAdapter mAdapter;
    private MapHolder mMapHolder;
    private IRenderTheme mTheme;
    private ShieldFactory mShieldFactory;
    private OsmcSymbolFactory mOsmcSymbolFactory;
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
            mShieldFactory = mMapHolder.getShieldFactory();
            mOsmcSymbolFactory = mMapHolder.getOsmcSymbolFactory();

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

        LegendSection[] theme;

        final int activity = Configuration.getActivity();
        final int mapStyle = Configuration.getMapStyle();
        final boolean nightMode = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        switch (activity) {
            case 2: // Winter
                theme = themeWinter;
                break;
            case 1: // Hiking
                theme = new LegendSection[themeTopo.length + 8];
                theme[0] = hiking_routes;
                theme[1] = hiking_path_visibility;
                theme[2] = hiking_sac_scale_t1;
                theme[3] = hiking_sac_scale_t2;
                theme[4] = hiking_sac_scale_t3;
                theme[5] = hiking_sac_scale_t4;
                theme[6] = hiking_sac_scale_t5;
                theme[7] = hiking_sac_scale_t6;
                System.arraycopy(themeTopo, 0, theme, 8, themeTopo.length);
                break;
            case 0:
            default:
                theme = nightMode ? themeNight : themeTopo;
                break;
        }

        for (LegendSection section : theme) {
            switch (mapStyle) {
                case 0: // Roads
                    if (section.title == R.string.legend_pedestrian)
                        continue;
                    if (section.title == R.string.legend_aerial_ways)
                        continue;
                case 1: // Urban
                    if (section.title == R.string.legend_tracks)
                        continue;
            }
            mData.add(new LegendItem(GeometryType.NONE, section.title, 0));
            for (LegendItem item : section.items) {
                switch (activity) {
                    case 2: // Winter
                        if (notWinterItems.contains(item))
                            continue;
                        break;
                    case 1:
                        break;
                    case 0:
                    default:
                        if (nightMode && notNightItems.contains(item))
                            continue;
                }
                switch (mapStyle) {
                    case 0: // Roads
                        if (notRoadItems.contains(item))
                            continue;
                    case 1: // Urban
                        if (notUrbanItems.contains(item))
                            continue;
                }
                mData.add(item);
            }
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
                itemHolder.item.setLegend(legendItem, mBackground, mTheme, mShieldFactory, mOsmcSymbolFactory);
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
    }
}
