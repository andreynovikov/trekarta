/*
 * Copyright 2021 Andrey Novikov
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

import androidx.annotation.NonNull;
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
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.view.LegendView;
import mobi.maptrek.view.LegendView.LegendItem;
import mobi.maptrek.view.LegendView.LegendAmenityItem;

// http://www.compassdude.com/map-symbols.php
// https://support.viewranger.com/index.php?pg=kb.page&id=143

public class Legend extends ListFragment {
    // Administrative
    private static final LegendItem country = new LegendItem(GeometryType.POINT, R.string.legend_country, 7)
            .addTag("place", "country").setText(R.string.legend_country_name);
    private static final LegendItem state = new LegendItem(GeometryType.POINT, R.string.legend_state, 7)
            .addTag("place", "state").setText(R.string.legend_state_name);
    private static final LegendItem country_boundary = new LegendItem(GeometryType.LINE, R.string.legend_country_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "2");
    private static final LegendItem region_boundary = new LegendItem(GeometryType.LINE, R.string.legend_region_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "3");
    private static final LegendItem province_boundary = new LegendItem(GeometryType.LINE, R.string.legend_province_boundary, 14)
            .addTag("boundary", "administrative").addTag("admin_level", "4");
    private static final LegendItem capital = new LegendItem(GeometryType.POINT, R.string.legend_capital, 14)
            .addTag("place", "city").addTag("admin_level", "2").setText(R.string.legend_capital_name);
    private static final LegendItem city = new LegendItem(GeometryType.POINT, R.string.legend_city, 14)
            .addTag("place", "city").setText(R.string.legend_city_name);
    private static final LegendItem town = new LegendItem(GeometryType.POINT, R.string.legend_town, 14)
            .addTag("place", "town").setText(R.string.legend_town_name);
    private static final LegendItem village = new LegendItem(GeometryType.POINT, R.string.legend_village, 14)
            .addTag("place", "village").setText(R.string.legend_village_name);
    private static final LegendItem suburb = new LegendItem(GeometryType.POINT, R.string.legend_suburb, 14)
            .addTag("place", "suburb").setText(R.string.legend_suburb_name);
    private static final LegendItem allotment = new LegendItem(GeometryType.POINT, R.string.legend_allotments, 14)
            .addTag("place", "allotments").setText(R.string.legend_allotments_name);
    private static final LegendItem locality = new LegendItem(GeometryType.POINT, R.string.legend_unpopulated_location, 14)
            .addTag("place", "locality").setText(R.string.legend_unpopulated_location_name);
    private static final LegendItem island = new LegendItem(GeometryType.POINT, R.string.legend_island, 14)
            .addTag("place", "island").setText(R.string.legend_island_name);

    // Land use
    private static final LegendItem residential = new LegendItem(GeometryType.POLY, R.string.legend_residental, 17)
            .addTag("landuse", "residential");
    private static final LegendItem industrial = new LegendItem(GeometryType.POLY, R.string.legend_industrial, 17)
            .addTag("landuse", "industrial");
    private static final LegendItem recreation = new LegendItem(GeometryType.POLY, R.string.legend_recreation, 17)
            .addTag("landuse", "recreation_ground");
    private static final LegendItem educational = new LegendItem(GeometryType.POLY, R.string.legend_educational, 17)
            .addTag("amenity", "school");
    private static final LegendItem hospital_area = new LegendItem(GeometryType.POLY, R.string.legend_hospital, 17)
            .addTag("amenity", "hospital");
    private static final LegendItem construction = new LegendItem(GeometryType.POLY, R.string.legend_construction, 17)
            .addTag("landuse", "construction");
    private static final LegendItem aerodrome = new LegendItem(GeometryType.POLY, R.string.legend_aerodrome, 10) // do not show icon
            .addTag("aeroway", "aerodrome");
    private static final LegendItem allotments = new LegendItem(GeometryType.POLY, R.string.legend_allotments, 17)
            .addTag("landuse", "allotments");
    private static final LegendItem quarry = new LegendItem(GeometryType.POLY, R.string.legend_quarry, 17)
            .addTag("landuse", "quarry");
    private static final LegendItem farmland = new LegendItem(GeometryType.POLY, R.string.legend_farmland, 17)
            .addTag("landuse", "farmland");
    private static final LegendItem orchard = new LegendItem(GeometryType.POLY, R.string.legend_orchard, 17)
            .addTag("landuse", "orchard");
    private static final LegendItem plant_nursery = new LegendItem(GeometryType.POLY, R.string.legend_plant_nursery, 17)
            .addTag("landuse", "plant_nursery");
    private static final LegendItem farmyard = new LegendItem(GeometryType.POLY, R.string.legend_farmyard, 17)
            .addTag("landuse", "farmyard");
    private static final LegendItem nature_reserve = new LegendItem(GeometryType.POLY, R.string.legend_nature_reserve, 13)
            .addTag("boundary", "nature_reserve").setText(R.string.legend_nature_reserve_name);
    private static final LegendItem aboriginal_lands = new LegendItem(GeometryType.POLY, R.string.legend_aboriginal_lands, 13)
            .addTag("boundary", "aboriginal_lands").setText(R.string.legend_aboriginal_lands_name);
    private static final LegendItem military = new LegendItem(GeometryType.POLY, R.string.legend_military, 13)
            .addTag("landuse", "military").setText(R.string.legend_military_name);
    private static final LegendItem zoo_area = new LegendItem(GeometryType.POLY, R.string.legend_zoo, 17)
            .addTag("tourism", "zoo");
    private static final LegendItem theme_park_area = new LegendItem(GeometryType.POLY, R.string.legend_theme_park, 17)
            .addTag("tourism", "theme_park");
    private static final LegendItem marina = new LegendItem(GeometryType.POLY, R.string.legend_marina, 17)
            .addTag("leisure", "marina").setText(R.string.legend_marina_name);

    // Water
    private static final LegendItem glacier = new LegendItem(GeometryType.POLY, R.string.legend_glacier, 17)
            .addTag("natural", "glacier");
    private static final LegendItem water = new LegendItem(GeometryType.POLY, R.string.legend_pond, 17)
            .addTag("natural", "water").setText(R.string.legend_pond_name);
    private static final LegendItem intermittent_water = new LegendItem(GeometryType.POLY, R.string.legend_intermittent_pond, 17)
            .addTag("natural", "water").addTag("intermittent", "yes");
    private static final LegendItem river = new LegendItem(GeometryType.LINE, R.string.legend_river, 17)
            .addTag("waterway", "river");
    private static final LegendItem intermittent_river = new LegendItem(GeometryType.LINE, R.string.legend_intermittent_river, 17)
            .addTag("waterway", "river").addTag("intermittent", "yes");
    private static final LegendItem underground_river = new LegendItem(GeometryType.LINE, R.string.legend_underground_river, 17)
            .addTag("waterway", "river").addTag("tunnel", "yes");
    private static final LegendItem stream = new LegendItem(GeometryType.LINE, R.string.legend_stream, 17)
            .addTag("waterway", "stream");
    private static final LegendItem ditch = new LegendItem(GeometryType.LINE, R.string.legend_ditch, 17)
            .addTag("waterway", "ditch");
    private static final LegendItem dam = new LegendItem(GeometryType.LINE, R.string.legend_dam, 17)
            .addTag("waterway", "dam");
    private static final LegendItem lock_gate = new LegendItem(GeometryType.POLY, R.string.legend_lock_gate, 17)
            .addTag("natural", "water").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17)
            .addTag("waterway", "lock_gate").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17)
                            .addTag("waterway", "lock_gate")));
    private static final LegendItem weir = new LegendItem(GeometryType.POLY, R.string.legend_weir, 17)
            .addTag("natural", "water").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17)
            .addTag("waterway", "weir").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17)
                            .addTag("waterway", "weir")));
    private static final LegendItem ford_point = new LegendItem(GeometryType.LINE, R.string.legend_ford, 17)
            .addTag("waterway", "stream").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17).addTag("ford", "yes"));

    // Land
    private static final LegendItem bare_rock = new LegendItem(GeometryType.POLY, R.string.legend_bare_rock, 17)
            .addTag("natural", "bare_rock");
    private static final LegendItem scree = new LegendItem(GeometryType.POLY, R.string.legend_scree, 17)
            .addTag("natural", "scree");
    private static final LegendItem shingle = new LegendItem(GeometryType.POLY, R.string.legend_shingle, 17)
            .addTag("natural", "shingle");
    private static final LegendItem mud = new LegendItem(GeometryType.POLY, R.string.legend_mud, 17)
            .addTag("natural", "mud");
    private static final LegendItem sand = new LegendItem(GeometryType.POLY, R.string.legend_sand, 17)
            .addTag("natural", "sand");
    private static final LegendItem beach = new LegendItem(GeometryType.POLY, R.string.legend_beach, 17)
            .addTag("natural", "beach").setText(R.string.legend_beach_text);
    //TODO Elevation
    private static final LegendItem peak = new LegendItem(GeometryType.POINT, R.string.legend_peak, 17)
            .addTag("natural", "peak");
    private static final LegendItem volcano = new LegendItem(GeometryType.POINT, R.string.legend_volcano, 17)
            .addTag("natural", "volcano");
    private static final LegendItem saddle = new LegendItem(GeometryType.POINT, R.string.legend_saddle, 17)
            .addTag("natural", "saddle");
    private static final LegendItem mountain_pass = new LegendItem(GeometryType.POINT, R.string.legend_mountain_pass, 17)
            .addTag("mountain_pass", "yes");
    private static final LegendItem ridge = new LegendItem(GeometryType.LINE, R.string.legend_ridge, 17)
            .addTag("natural", "ridge");
    private static final LegendItem arete = new LegendItem(GeometryType.LINE, R.string.legend_arete, 17)
            .addTag("natural", "arete");
    private static final LegendItem cliff = new LegendItem(GeometryType.LINE, R.string.legend_cliff, 17)
            .addTag("natural", "cliff");
    private static final LegendItem rock = new LegendItem(GeometryType.POINT, R.string.legend_rock, 17)
            .addTag("natural", "rock");
    private static final LegendItem cave_entrance = new LegendItem(GeometryType.POINT, R.string.legend_cave_entrance, 17)
            .addTag("natural", "cave_entrance");
    private static final LegendItem spring = new LegendItem(GeometryType.POINT, R.string.legend_spring, 17)
            .addTag("natural", "spring");
    private static final LegendItem contour = new LegendItem(GeometryType.LINE, R.string.legend_contour, 17)
            .addTag("contour", "elevation_major").setText(R.string.legend_elevation);

    // Vegetation
    private static final LegendItem forest = new LegendItem(GeometryType.POLY, R.string.legend_forest, 17)
            .addTag("natural", "forest");
    private static final LegendItem marsh = new LegendItem(GeometryType.POLY, R.string.legend_marsh, 17)
            .addTag("natural", "marsh");
    private static final LegendItem saltmarsh = new LegendItem(GeometryType.POLY, R.string.legend_wetland_saltmarsh, 17)
            .addTag("natural", "wetland").addTag("wetland", "saltmarsh");
    private static final LegendItem reedbed = new LegendItem(GeometryType.POLY, R.string.legend_wetland_reedbed, 17)
            .addTag("natural", "wetland").addTag("wetland", "reedbed");
    private static final LegendItem wet_meadow = new LegendItem(GeometryType.POLY, R.string.legend_wetland_wet_meadow, 17)
            .addTag("natural", "wetland").addTag("wetland", "wet_meadow");
    private static final LegendItem swamp = new LegendItem(GeometryType.POLY, R.string.legend_wetland_swamp, 17)
            .addTag("natural", "wetland").addTag("wetland", "swamp");
    private static final LegendItem mangrove = new LegendItem(GeometryType.POLY, R.string.legend_wetland_mangrove, 17)
            .addTag("natural", "wetland").addTag("wetland", "mangrove");
    private static final LegendItem bog = new LegendItem(GeometryType.POLY, R.string.legend_wetland_bog, 17)
            .addTag("natural", "wetland").addTag("wetland", "bog");
    private static final LegendItem fen = new LegendItem(GeometryType.POLY, R.string.legend_wetland_fen, 17)
            .addTag("natural", "wetland").addTag("wetland", "fen");
    private static final LegendItem tidalflat = new LegendItem(GeometryType.POLY, R.string.legend_wetland_tidalflat, 17)
            .addTag("natural", "wetland").addTag("wetland", "tidalflat");
    private static final LegendItem wetland = new LegendItem(GeometryType.POLY, R.string.legend_wetland, 17)
            .addTag("natural", "wetland");
    private static final LegendItem tree_row = new LegendItem(GeometryType.LINE, R.string.legend_tree_row, 17)
            .addTag("natural", "tree_row");
    private static final LegendItem tree = new LegendItem(GeometryType.POINT, R.string.legend_tree, 17)
            .addTag("natural", "tree");
    private static final LegendItem grass = new LegendItem(GeometryType.POLY, R.string.legend_grass, 17)
            .addTag("landuse", "grass"); // we use grass instead of grassland here for urban legend
    private static final LegendItem scrub = new LegendItem(GeometryType.POLY, R.string.legend_scrub, 17)
            .addTag("natural", "scrub");
    private static final LegendItem heath = new LegendItem(GeometryType.POLY, R.string.legend_heath, 17)
            .addTag("natural", "heath");
    private static final LegendItem meadow = new LegendItem(GeometryType.POLY, R.string.legend_meadow, 17)
            .addTag("leisure", "meadow"); // ?

    // Urban
    private static final LegendItem hedge = new LegendItem(GeometryType.LINE, R.string.legend_hedge, 17)
            .addTag("barrier", "hedge");
    private static final LegendItem fence = new LegendItem(GeometryType.LINE, R.string.legend_fence, 17)
            .addTag("barrier", "fence");
    private static final LegendItem wall = new LegendItem(GeometryType.LINE, R.string.legend_wall, 17)
            .addTag("barrier", "wall");
    private static final LegendItem city_wall = new LegendItem(GeometryType.LINE, R.string.legend_city_wall, 17)
            .addTag("barrier", "city_wall");
    private static final LegendItem retaining_wall = new LegendItem(GeometryType.LINE, R.string.legend_retaining_wall, 17)
            .addTag("barrier", "retaining_wall");
    private static final LegendItem embankment = new LegendItem(GeometryType.LINE, R.string.legend_embankment, 17)
            .addTag("man_made", "embankment");
    private static final LegendItem building = new LegendItem(GeometryType.POLY, R.string.legend_building, 17)
            .addTag("building", "yes").addTag("kind", "yes").addTag("addr:housenumber", "13").setText(R.string.legend_thirteen).setShape(LegendView.PATH_BUILDING);
    private static final LegendItem addresses = new LegendItem(GeometryType.LINE, R.string.legend_house_numbers, 17)
            .addTag("addr:interpolation", "yes").setOverlay(
                    new LegendItem(GeometryType.POINT, R.string.legend_house_numbers, 17)
                            .addTag("addr:housenumber", "13").setText(R.string.legend_thirteen).setTextAlign(LegendItem.ALIGN_LEFT).setOverlay(
                            new LegendItem(GeometryType.POINT, R.string.legend_house_numbers, 17)
                                    .addTag("addr:housenumber", "19").setText(R.string.legend_nineteen).setTextAlign(LegendItem.ALIGN_RIGHT)));
    private static final LegendItem stadium = new LegendItem(GeometryType.POLY, R.string.legend_stadium, 17)
            .addTag("leisure", "stadium");
    private static final LegendItem sports_centre = new LegendItem(GeometryType.POLY, R.string.legend_sports_centre, 17)
            .addTag("leisure", "sports_centre").addTag("kind", "yes").setText(R.string.legend_sports_centre_name);
    private static final LegendItem swimming_pool = new LegendItem(GeometryType.POLY, R.string.legend_swimming_pool, 17)
            .addTag("leisure", "swimming_pool").setShape(LegendView.PATH_PLATFORM);
    private static final LegendItem garden = new LegendItem(GeometryType.POLY, R.string.legend_garden, 17)
            .addTag("leisure", "garden");
    private static final LegendItem camp_site_area = new LegendItem(GeometryType.POLY, R.string.legend_camp_site, 17)
            .addTag("leisure", "camp_site");
    private static final LegendItem playground_area = new LegendItem(GeometryType.POLY, R.string.legend_playground, 17)
            .addTag("leisure", "playground");
    private static final LegendItem pitch = new LegendItem(GeometryType.POLY, R.string.legend_pitch, 17)
            .addTag("leisure", "pitch");
    private static final LegendItem dog_park = new LegendItem(GeometryType.POLY, R.string.legend_dog_park, 17)
            .addTag("leisure", "dog_park");
    private static final LegendItem cemetery = new LegendItem(GeometryType.POLY, R.string.legend_cemetery, 17)
            .addTag("landuse", "cemetery");
    private static final LegendItem runway = new LegendItem(GeometryType.LINE, R.string.legend_runway, 17)
            .addTag("aeroway", "runway").setText(R.string.legend_runway_name);
    private static final LegendItem apron = new LegendItem(GeometryType.POLY, R.string.legend_apron, 17)
            .addTag("aeroway", "apron");
    private static final LegendItem pier = new LegendItem(GeometryType.POLY, R.string.legend_pier, 17)
            .addTag("man_made", "pier").setShape(LegendView.PATH_PIER);
    private static final LegendItem bridge = new LegendItem(GeometryType.POLY, R.string.legend_bridge, 17)
            .addTag("man_made", "bridge").setShape(LegendView.PATH_PLATFORM);
    private static final LegendItem water_well = new LegendItem(GeometryType.POINT, R.string.legend_water_well, 17)
            .addTag("man_made", "water_well");
    private static final LegendItem water_pump = new LegendItem(GeometryType.POINT, R.string.legend_water_pump, 17)
            .addTag("man_made", "water_well").addTag("pump", "yes");
    private static final LegendItem tower = new LegendItem(GeometryType.POINT, R.string.legend_tower, 17)
            .addTag("man_made", "tower");
    private static final LegendItem power_line = new LegendItem(GeometryType.LINE, R.string.legend_power_line, 17)
            .addTag("power", "line");
    private static final LegendItem power_tower = new LegendItem(GeometryType.POINT, R.string.legend_power_line, 17)
            .addTag("power", "tower");
    private static final LegendItem power_generator_wind = new LegendItem(GeometryType.POINT, R.string.legend_power_generator_wind, 17)
            .addTag("power", "generator").addTag("generator:source", "wind");
    private static final LegendItem water_pipeline = new LegendItem(GeometryType.LINE, R.string.legend_water_pipeline, 17)
            .addTag("man_made", "pipeline").addTag("substance", "water");
    private static final LegendItem steam_pipeline = new LegendItem(GeometryType.LINE, R.string.legend_steam_pipeline, 17)
            .addTag("man_made", "pipeline").addTag("substance", "hot_water");
    private static final LegendItem gas_pipeline = new LegendItem(GeometryType.LINE, R.string.legend_gas_pipeline, 17)
            .addTag("man_made", "pipeline").addTag("substance", "gas");
    private static final LegendItem oil_pipeline = new LegendItem(GeometryType.LINE, R.string.legend_oil_pipeline, 17)
            .addTag("man_made", "pipeline").addTag("substance", "oil");
    private static final LegendItem general_pipeline = new LegendItem(GeometryType.LINE, R.string.legend_general_pipeline, 17)
            .addTag("man_made", "pipeline");

    // Roads
    private static final LegendItem motorway = new LegendItem(GeometryType.LINE, R.string.legend_motorway, 16)
            .addTag("highway", "motorway").addTag("ref", "A8");
    private static final LegendItem trunk_road = new LegendItem(GeometryType.LINE, R.string.legend_trunk_road, 16)
            .addTag("highway", "trunk").addTag("ref", "E95");
    private static final LegendItem primary_road = new LegendItem(GeometryType.LINE, R.string.legend_primary_road, 16)
            .addTag("highway", "primary").addTag("ref", "M1");
    private static final LegendItem secondary_road = new LegendItem(GeometryType.LINE, R.string.legend_secondary_road, 16)
            .addTag("highway", "secondary").addTag("ref", "L519");
    private static final LegendItem tertiary_road = new LegendItem(GeometryType.LINE, R.string.legend_tertiary_road, 16)
            .addTag("highway", "tertiary").addTag("ref", "K9651");
    private static final LegendItem unclassified_road = new LegendItem(GeometryType.LINE, R.string.legend_general_road, 16)
            .addTag("highway", "unclassified");
    private static final LegendItem residential_road = new LegendItem(GeometryType.LINE, R.string.legend_residental_road, 16)
            .addTag("highway", "residential");
    private static final LegendItem service_road = new LegendItem(GeometryType.LINE, R.string.legend_service_road, 16)
            .addTag("highway", "service");
    private static final LegendItem oneway_road = new LegendItem(GeometryType.LINE, R.string.legend_oneway_road, 16)
            .addTag("highway", "unclassified").addTag("oneway", "1");
    private static final LegendItem private_road = new LegendItem(GeometryType.LINE, R.string.legend_private_road, 16)
            .addTag("highway", "unclassified").addTag("access", "private");
    private static final LegendItem no_access_road = new LegendItem(GeometryType.LINE, R.string.legend_noaccess_road, 16)
            .addTag("highway", "unclassified").addTag("access", "no");
    private static final LegendItem wd4_road = new LegendItem(GeometryType.LINE, R.string.legend_4wd_road, 16)
            .addTag("highway", "unclassified").addTag("4wd_only", "yes");
    private static final LegendItem unpaved_road = new LegendItem(GeometryType.LINE, R.string.legend_unpaved_road, 16)
            .addTag("highway", "unclassified").addTag("surface", "unpaved");
    private static final LegendItem dirt_road = new LegendItem(GeometryType.LINE, R.string.legend_dirt_road, 16)
            .addTag("highway", "unclassified").addTag("surface", "dirt");
    private static final LegendItem winter_road = new LegendItem(GeometryType.LINE, R.string.legend_winter_road, 16)
            .addTag("highway", "unclassified").addTag("winter_road", "yes");
    private static final LegendItem ice_road = new LegendItem(GeometryType.LINE, R.string.legend_ice_road, 16)
            .addTag("highway", "unclassified").addTag("ice_road", "yes");
    private static final LegendItem toll_road = new LegendItem(GeometryType.LINE, R.string.legend_toll_road, 16)
            .addTag("highway", "unclassified").addTag("toll", "yes");
    private static final LegendItem road_bridge = new LegendItem(GeometryType.LINE, R.string.legend_bridge, 16)
            .addTag("highway", "unclassified").addTag("bridge", "yes");
    private static final LegendItem road_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_tunnel, 16)
            .addTag("highway", "unclassified").addTag("tunnel", "yes");
    private static final LegendItem construction_road = new LegendItem(GeometryType.LINE, R.string.legend_road_under_construction, 16)
            .addTag("highway", "construction");
    private static final LegendItem ford = new LegendItem(GeometryType.LINE, R.string.legend_ford, 16)
            .addTag("highway", "unclassified").addTag("ford", "yes");
    private static final LegendItem border_control = new LegendItem(GeometryType.POINT, R.string.legend_border_control, 17)
            .addTag("barrier", "border_control");
    private static final LegendItem toll_booth = new LegendItem(GeometryType.POINT, R.string.legend_toll_booth, 17)
            .addTag("barrier", "toll_booth");
    private static final LegendItem block = new LegendAmenityItem(64);
    private static final LegendItem bollard = new LegendAmenityItem(67);
    private static final LegendItem cycle_barrier = new LegendAmenityItem(70);
    private static final LegendItem kissing_gate = new LegendAmenityItem(74);
    private static final LegendItem lift_gate = new LegendAmenityItem(73);
    private static final LegendItem stile = new LegendAmenityItem(68);
    private static final LegendItem gate = new LegendAmenityItem(76);
    private static final LegendItem highway_services = new LegendItem(GeometryType.POLY, R.string.legend_highway_services, 17)
            .addTag("highway", "services");
    private static final LegendItem rest_area = new LegendItem(GeometryType.POLY, R.string.legend_rest_area, 17)
            .addTag("amenity", "rest_area");

    // Tracks
    private static final LegendItem track = new LegendItem(GeometryType.LINE, R.string.legend_track, 17)
            .addTag("highway", "track");
    private static final LegendItem track_bridge = new LegendItem(GeometryType.LINE, R.string.legend_track_bridge, 17)
            .addTag("highway", "track").addTag("bridge", "yes");
    private static final LegendItem track_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_track_tunnel, 17)
            .addTag("highway", "track").addTag("tunnel", "yes");
    private static final LegendItem good_track = new LegendItem(GeometryType.LINE, R.string.legend_good_track, 17)
            .addTag("highway", "track").addTag("smoothness", "good");
    private static final LegendItem very_bad_track = new LegendItem(GeometryType.LINE, R.string.legend_very_bad_track, 17)
            .addTag("highway", "track").addTag("smoothness", "very_bad");
    private static final LegendItem horrible_track = new LegendItem(GeometryType.LINE, R.string.legend_horrible_track, 17)
            .addTag("highway", "track").addTag("smoothness", "horrible");
    private static final LegendItem very_horrible_track = new LegendItem(GeometryType.LINE, R.string.legend_very_horrible_track, 17)
            .addTag("highway", "track").addTag("smoothness", "very_horrible");
    private static final LegendItem impassable_track = new LegendItem(GeometryType.LINE, R.string.legend_impassable_track, 17)
            .addTag("highway", "track").addTag("smoothness", "impassable");
    private static final LegendItem winter_track = new LegendItem(GeometryType.LINE, R.string.legend_winter_track, 17)
            .addTag("highway", "track").addTag("winter_road", "yes");
    private static final LegendItem ice_track = new LegendItem(GeometryType.LINE, R.string.legend_ice_track, 17)
            .addTag("highway", "track").addTag("ice_road", "yes");
    private static final LegendItem ford_track = new LegendItem(GeometryType.LINE, R.string.legend_track_ford, 17)
            .addTag("highway", "track").addTag("ford", "yes");
    private static final LegendItem bridleway = new LegendItem(GeometryType.LINE, R.string.legend_bridleway, 17)
            .addTag("highway", "bridleway");

    // Pedestrian ways
    private static final LegendItem pedestrian_area = new LegendItem(GeometryType.POLY, R.string.legend_pedestrian_area, 17)
            .addTag("highway", "pedestrian"); //.addTag("area", "yes");
    private static final LegendItem pedestrian_road = new LegendItem(GeometryType.LINE, R.string.legend_pedestrian_road, 17)
            .addTag("highway", "pedestrian");
    private static final LegendItem path = new LegendItem(GeometryType.LINE, R.string.legend_path, 17)
            .addTag("highway", "path");
    private static final LegendItem path_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_path_visibility_good, 17)
            .addTag("highway", "path").addTag("trail_visibility", "good");
    private static final LegendItem path_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_path_visibility_bad, 17)
            .addTag("highway", "path").addTag("trail_visibility", "bad");
    private static final LegendItem no_access_path = new LegendItem(GeometryType.LINE, R.string.legend_noaccess_path, 17)
            .addTag("highway", "path").addTag("access", "no");
    private static final LegendItem path_bridge = new LegendItem(GeometryType.LINE, R.string.legend_path_bridge, 17)
            .addTag("highway", "path").addTag("bridge", "yes");
    private static final LegendItem path_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_path_tunnel, 17)
            .addTag("highway", "path").addTag("tunnel", "yes");
    private static final LegendItem steps = new LegendItem(GeometryType.LINE, R.string.legend_steps, 17)
            .addTag("highway", "steps");
    private static final LegendItem via_ferrata = new LegendItem(GeometryType.LINE, R.string.legend_via_ferrata, 17)
            .addTag("highway", "via_ferrata");

    // Railways
    private static final LegendItem railway = new LegendItem(GeometryType.LINE, R.string.legend_railway, 17)
            .addTag("railway", "rail");
    private static final LegendItem railway_service = new LegendItem(GeometryType.LINE, R.string.legend_railway_service, 17)
            .addTag("railway", "rail").addTag("service", "yes");
    private static final LegendItem railway_bridge = new LegendItem(GeometryType.LINE, R.string.legend_bridge, 17)
            .addTag("railway", "rail").addTag("bridge", "yes");
    private static final LegendItem railway_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_tunnel, 17)
            .addTag("railway", "rail").addTag("tunnel", "yes");
    private static final LegendItem abandoned_railway = new LegendItem(GeometryType.LINE, R.string.legend_abandoned_railway, 17)
            .addTag("railway", "abandoned");
    private static final LegendItem light_railway = new LegendItem(GeometryType.LINE, R.string.legend_light_railway, 17)
            .addTag("railway", "light_rail");
    private static final LegendItem tram = new LegendItem(GeometryType.LINE, R.string.legend_tram, 17)
            .addTag("railway", "tram");
    private static final LegendItem subway = new LegendItem(GeometryType.LINE, R.string.legend_subway, 17)
            .addTag("railway", "subway");
    private static final LegendItem monorail = new LegendItem(GeometryType.LINE, R.string.legend_monorail, 17)
            .addTag("railway", "monorail");
    private static final LegendItem railway_platform = new LegendItem(GeometryType.POLY, R.string.legend_railway_platform, 17)
            .addTag("railway", "platform").setShape(LegendView.PATH_PLATFORM);
    private static final LegendItem railway_station = new LegendItem(GeometryType.POINT, R.string.legend_railway_station, 17)
            .addTag("railway", "station").setText(R.string.legend_railway_station_name);
    private static final LegendItem railway_halt = new LegendItem(GeometryType.POINT, R.string.legend_railway_halt, 17)
            .addTag("railway", "halt").setText(R.string.legend_railway_halt_name);
    private static final LegendItem railway_level_crossing = new LegendItem(GeometryType.POINT, R.string.legend_level_crossing, 17)
            .addTag("railway", "level_crossing");
    private static final LegendItem railway_crossing = new LegendItem(GeometryType.POINT, R.string.legend_pedestrian_crossing, 17)
            .addTag("railway", "crossing");

    // Transportation
    private static final LegendItem bus_station = new LegendAmenityItem(247);
    private static final LegendItem bus_stop = new LegendAmenityItem(248);
    private static final LegendItem tram_stop = new LegendAmenityItem(249);
    private static final LegendItem subway_entrance = new LegendItem(GeometryType.POINT, R.string.legend_subway_entrance, 17)
            .addTag("railway", "subway_entrance").addTag("feature", "yes").setKind(12);
    private static final LegendItem subway_station = new LegendItem(GeometryType.POINT, R.string.legend_subway_station, 15)
            .addTag("railway", "station").addTag("station", "subway").setText(R.string.legend_subway_station_name);
    private static final LegendItem aeroway_aerodrome = new LegendItem(GeometryType.POINT, R.string.legend_aerodrome, 17)
            .addTag("aeroway", "aerodrome").setText(R.string.legend_aerodrome_name);
    private static final LegendItem aeroway_heliport = new LegendItem(GeometryType.POINT, R.string.legend_heliport, 17)
            .addTag("aeroway", "heliport");
    private static final LegendItem ferry = new LegendItem(GeometryType.LINE, R.string.legend_ferry, 17)
            .addTag("route", "ferry");
    private static final LegendItem ferry_terminal = new LegendItem(GeometryType.POINT, R.string.legend_ferry_terminal, 17)
            .addTag("amenity", "ferry_terminal");

    // Pistes
    private static final LegendItem piste_downhill_novice = new LegendItem(GeometryType.POLY, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "novice").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "novice"));
    private static final LegendItem piste_downhill_easy = new LegendItem(GeometryType.POLY, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "easy").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "easy"));
    private static final LegendItem piste_downhill_intermediate = new LegendItem(GeometryType.POLY, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "intermediate").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "intermediate"));
    private static final LegendItem piste_downhill_advanced = new LegendItem(GeometryType.POLY, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "advanced").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "advanced"));
    private static final LegendItem piste_downhill_expert = new LegendItem(GeometryType.POLY, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "expert").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "expert"));
    private static final LegendItem piste_downhill_freeride = new LegendItem(GeometryType.POLY, R.string.legend_free_ride, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "freeride").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "freeride"));
    private static final LegendItem piste_downhill_unknown = new LegendItem(GeometryType.POLY, R.string.legend_unknown_difficulty, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown"));
    private static final LegendItem piste_downhill_mogul = new LegendItem(GeometryType.POLY, R.string.legend_mogul, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").addTag("piste:grooming", "mogul").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown"));
    private static final LegendItem piste_downhill_lit = new LegendItem(GeometryType.POLY, R.string.legend_lit_piste, 15)
            .addTag("piste:type", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "downhill").addTag("piste:difficulty", "unknown").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 15)
                            .addTag("piste:type", "downhill").addTag("piste:lit", "yes").setTotalSymbols(2)));
    private static final LegendItem piste_nordic = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "nordic");
    private static final LegendItem piste_nordic_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static final LegendItem piste_nordic_oneway = new LegendItem(GeometryType.LINE, R.string.legend_oneway_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:oneway", "yes").setTotalSymbols(2);
    private static final LegendItem piste_nordic_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static final LegendItem piste_nordic_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "nordic").addTag("piste:grooming", "backcountry").setTotalSymbols(0);
    private static final LegendItem piste_nordic_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "novice");
    private static final LegendItem piste_nordic_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "easy");
    private static final LegendItem piste_nordic_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "intermediate");
    private static final LegendItem piste_nordic_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "advanced");
    private static final LegendItem piste_nordic_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "nordic").addTag("piste:difficulty", "expert");

    private static final LegendItem piste_sled = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "sled");
    private static final LegendItem piste_sled_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static final LegendItem piste_sled_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static final LegendItem piste_sled_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "sled").addTag("piste:grooming", "backcountry").setTotalSymbols(0);
    private static final LegendItem piste_sled_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "novice");
    private static final LegendItem piste_sled_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "easy");
    private static final LegendItem piste_sled_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "intermediate");
    private static final LegendItem piste_sled_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "advanced");
    private static final LegendItem piste_sled_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "sled").addTag("piste:difficulty", "expert");

    private static final LegendItem piste_hike = new LegendItem(GeometryType.LINE, R.string.legend_groomed_trail, 15)
            .addTag("piste:type", "hike");
    private static final LegendItem piste_hike_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_requires_snow_shoes, 15)
            .addTag("piste:type", "hike").addTag("piste:grooming", "backcountry");
    private static final LegendItem piste_hike_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "hike").addTag("piste:lit", "yes").setTotalSymbols(3);
    private static final LegendItem piste_hike_novice = new LegendItem(GeometryType.LINE, R.string.legend_novice_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "novice");
    private static final LegendItem piste_hike_easy = new LegendItem(GeometryType.LINE, R.string.legend_easy_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "easy");
    private static final LegendItem piste_hike_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_intermediate_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "intermediate");
    private static final LegendItem piste_hike_advanced = new LegendItem(GeometryType.LINE, R.string.legend_advanced_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "advanced");
    private static final LegendItem piste_hike_expert = new LegendItem(GeometryType.LINE, R.string.legend_expert_difficulty, 15)
            .addTag("piste:type", "hike").addTag("piste:difficulty", "expert");

    private static final LegendItem piste_sleigh = new LegendItem(GeometryType.LINE, R.string.legend_trail, 15)
            .addTag("piste:type", "sleigh");
    private static final LegendItem piste_sleigh_lit = new LegendItem(GeometryType.LINE, R.string.legend_lit_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:lit", "yes").setTotalSymbols(2);
    private static final LegendItem piste_sleigh_oneway = new LegendItem(GeometryType.LINE, R.string.legend_oneway_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:oneway", "yes").setTotalSymbols(2);
    private static final LegendItem piste_sleigh_scooter = new LegendItem(GeometryType.LINE, R.string.legend_loosely_groomed_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:grooming", "scooter").setTotalSymbols(0);
    private static final LegendItem piste_sleigh_backcountry = new LegendItem(GeometryType.LINE, R.string.legend_ungroomed_trail, 15)
            .addTag("piste:type", "sleigh").addTag("piste:grooming", "backcountry").setTotalSymbols(0);


    private static final LegendItem piste_snow_park = new LegendItem(GeometryType.POLY, R.string.legend_snow_park, 15)
            .addTag("piste:type", "snow_park").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "snow_park"));
    private static final LegendItem piste_playground = new LegendItem(GeometryType.POLY, R.string.legend_kids_playground, 15)
            .addTag("piste:type", "playground").setOverlay(
                    new LegendItem(GeometryType.POLY, 0, 15)
                            .addTag("piste:border", "playground"));
    private static final LegendItem piste_ice_skate = new LegendItem(GeometryType.POLY, R.string.legend_ice_rink, 15)
            .addTag("piste:type", "ice_skate");
    private static final LegendItem piste_ski_jump = new LegendItem(GeometryType.LINE, R.string.legend_ski_jump, 15)
            .addTag("piste:type", "ski_jump");
    private static final LegendItem piste_ski_jump_landing = new LegendItem(GeometryType.POLY, R.string.legend_ski_jump_landing_zone, 15)
            .addTag("piste:type", "ski_jump_landing");
    private static final LegendItem piste_ski_tour = new LegendItem(GeometryType.LINE, R.string.legend_ski_tour, 15)
            .addTag("piste:type", "skitour");

    // Aerial cableways
    private static final LegendItem cable_car = new LegendItem(GeometryType.LINE, R.string.legend_cable_car, 17)
            .addTag("aerialway", "cable_car");
    private static final LegendItem gondola = new LegendItem(GeometryType.LINE, R.string.legend_gondola, 17)
            .addTag("aerialway", "gondola");
    private static final LegendItem chair_lift = new LegendItem(GeometryType.LINE, R.string.legend_chair_lift, 17)
            .addTag("aerialway", "chair_lift");
    private static final LegendItem drag_lift = new LegendItem(GeometryType.LINE, R.string.legend_drag_lift, 17)
            .addTag("aerialway", "drag_lift");
    private static final LegendItem zip_line = new LegendItem(GeometryType.LINE, R.string.legend_zip_line, 17)
            .addTag("aerialway", "zip_line");
    private static final LegendItem magic_carpet = new LegendItem(GeometryType.LINE, R.string.legend_magic_carpet, 17)
            .addTag("aerialway", "magic_carpet");
    private static final LegendItem aerialway_station = new LegendItem(GeometryType.POINT, R.string.legend_station, 17)
            .addTag("aerialway", "station");

    // Hiking

    private static final LegendItem hiking_route_iwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_iwn, 8)
            .addTag("route", "hiking").addTag("network", "iwn");
    private static final LegendItem hiking_route_nwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_nwn, 8)
            .addTag("route", "hiking").addTag("network", "nwn");
    private static final LegendItem hiking_route_rwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_rwn, 8)
            .addTag("route", "hiking").addTag("network", "rwn");
    private static final LegendItem hiking_route_lwn = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_lwn, 8)
            .addTag("route", "hiking").addTag("network", "lwn");
    private static final LegendItem hiking_route_symbol = new LegendItem(GeometryType.LINE, R.string.legend_hiking_route_symbol, 8)
            .addTag("route", "hiking").addTag("network", "iwn").addTag("osmc:symbol", "blue:blue:shell_modern");

    private static final LegendItem hiking_path_with_route = new LegendItem(GeometryType.LINE, R.string.legend_hiking_path_with_route, 17)
            .addTag("highway", "path").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17).addTag("route", "hiking").addTag("network", "iwn"));
    private static final LegendItem hiking_road_with_route = new LegendItem(GeometryType.LINE, R.string.legend_hiking_road_with_route, 17)
            .addTag("highway", "unclassified").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17).addTag("route", "hiking").addTag("network", "iwn"));

    private static final LegendItem hiking_path_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path");
    private static final LegendItem hiking_path_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("trail_visibility", "no");

    private static final LegendItem hiking_path_sac_scale_t1_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1");
    private static final LegendItem hiking_path_sac_scale_t1_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t1_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t1_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t1_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t1_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t1").addTag("trail_visibility", "no");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t2_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t2").addTag("trail_visibility", "no");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t3_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t3").addTag("trail_visibility", "no");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t4_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t4").addTag("trail_visibility", "no");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t5_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t5").addTag("trail_visibility", "no");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_unknown = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_unknown, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_excellent = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_excellent, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "excellent");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "good");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "intermediate");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "bad");
    private static final LegendItem hiking_path_sac_scale_t6_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("sac_scale", "t6").addTag("trail_visibility", "no");

    // Cycling
    private static final LegendItem cycling_route_icn = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_icn, 8)
            .addTag("route", "bicycle").addTag("network", "icn");
    private static final LegendItem cycling_route_ncn = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_ncn, 8)
            .addTag("route", "bicycle").addTag("network", "ncn");
    private static final LegendItem cycling_route_rcn = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_rcn, 8)
            .addTag("route", "bicycle").addTag("network", "rcn");
    private static final LegendItem cycling_route_lcn = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_lcn, 8)
            .addTag("route", "bicycle").addTag("network", "lcn");
    private static final LegendItem cycling_route_mtb = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_mtb, 9)
            .addTag("route", "mtb");

    private static final LegendItem cycling_route_number = new LegendItem(GeometryType.LINE, R.string.legend_cycling_route_number, 8)
            .addTag("route", "bicycle").addTag("network", "ncn").addTag("ref","EV1").addTag("route:colour", "35071");
    private static final LegendItem cycling_network_node = new LegendItem(GeometryType.LINE, R.string.legend_cycling_network_node, 8)
            .addTag("route", "bicycle").addTag("network", "rcn").setOverlay(
                    new LegendItem(GeometryType.POINT, 0, 17).addTag("network", "rcn").addTag("ref","69"));

    private static final LegendItem cycling_path_with_route = new LegendItem(GeometryType.LINE, R.string.legend_cycling_path_with_route, 17)
            .addTag("highway", "path").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17).addTag("route", "bicycle").addTag("network", "ncn"));
    private static final LegendItem cycling_road_with_route = new LegendItem(GeometryType.LINE, R.string.legend_cycling_road_with_route, 17)
            .addTag("highway", "secondary").setOverlay(
                    new LegendItem(GeometryType.LINE, 0, 17).addTag("route", "bicycle").addTag("network", "ncn"));

    private static final LegendItem cycleway = new LegendItem(GeometryType.LINE, R.string.legend_cycleway, 17)
            .addTag("highway", "cycleway");
    private static final LegendItem cycleway_oneway = new LegendItem(GeometryType.LINE, R.string.legend_cycleway_oneway, 17)
            .addTag("highway", "cycleway").addTag("oneway", "1");
    private static final LegendItem cycleway_bridge = new LegendItem(GeometryType.LINE, R.string.legend_cycleway_bridge, 17)
            .addTag("highway", "cycleway").addTag("bridge", "yes");
    private static final LegendItem cycleway_tunnel = new LegendItem(GeometryType.LINE, R.string.legend_cycleway_tunnel, 17)
            .addTag("highway", "cycleway").addTag("tunnel", "yes");
    private static final LegendItem cycling_road_with_tracks = new LegendItem(GeometryType.LINE, R.string.legend_cycling_road_with_tracks, 17)
            .addTag("highway", "unclassified").addTag("cycleway", "track");
    private static final LegendItem cycling_road_with_track = new LegendItem(GeometryType.LINE, R.string.legend_cycling_road_with_track, 17)
            .addTag("highway", "unclassified").addTag("cycleway:right", "track");
    private static final LegendItem cycling_road_doubleway = new LegendItem(GeometryType.LINE, R.string.legend_cycling_road_doubleway, 17)
            .addTag("highway", "residential").addTag("oneway", "1").addTag("oneway:bicycle", "no");
    private static final LegendItem cycling_path_designated = new LegendItem(GeometryType.LINE, R.string.legend_cycling_path_designated, 17)
            .addTag("highway", "path").addTag("bicycle", "designated");
    private static final LegendItem cycling_path_yes = new LegendItem(GeometryType.LINE, R.string.legend_cycling_path_yes, 17)
            .addTag("highway", "path").addTag("bicycle", "yes");
    private static final LegendItem cycling_steps = new LegendItem(GeometryType.LINE, R.string.legend_cycling_steps, 17)
            .addTag("highway", "steps").addTag("ramp:bicycle", "yes");

    private static final LegendItem cycling_path_visibility_good = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_good, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("trail_visibility", "good");
    private static final LegendItem cycling_path_visibility_intermediate = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_intermediate, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("trail_visibility", "intermediate");
    private static final LegendItem cycling_path_visibility_bad = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_bad, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("trail_visibility", "bad");
    private static final LegendItem cycling_path_visibility_no = new LegendItem(GeometryType.LINE, R.string.legend_hiking_visibility_no, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("trail_visibility", "no");

    private static final LegendItem cycling_mtb_scale0 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale0, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "0");
    private static final LegendItem cycling_mtb_scale1 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale1, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "1");
    private static final LegendItem cycling_mtb_scale2 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale2, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "2");
    private static final LegendItem cycling_mtb_scale3 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale3, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "3");
    private static final LegendItem cycling_mtb_scale4 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale4, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "4");
    private static final LegendItem cycling_mtb_scale5 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale5, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "5");
    private static final LegendItem cycling_mtb_scale6 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale6, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale", "6");

    private static final LegendItem cycling_mtb_scale_imba0 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_imba0, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:imba", "0");
    private static final LegendItem cycling_mtb_scale_imba1 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_imba1, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:imba", "1");
    private static final LegendItem cycling_mtb_scale_imba2 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_imba2, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:imba", "2");
    private static final LegendItem cycling_mtb_scale_imba3 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_imba3, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:imba", "3");
    private static final LegendItem cycling_mtb_scale_imba4 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_imba4, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:imba", "4");

    private static final LegendItem cycling_mtb_scale_uphill0 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill0, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "0");
    private static final LegendItem cycling_mtb_scale_uphill1 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill1, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "1");
    private static final LegendItem cycling_mtb_scale_uphill2 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill2, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "2");
    private static final LegendItem cycling_mtb_scale_uphill3 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill3, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "3");
    private static final LegendItem cycling_mtb_scale_uphill4 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill4, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "4");
    private static final LegendItem cycling_mtb_scale_uphill5 = new LegendItem(GeometryType.LINE, R.string.legend_cycling_mtb_scale_uphill5, 17)
            .addTag("highway", "path").addTag("bicycle", "designated").addTag("mtb:scale:uphill", "5");

    // POI

    private static final LegendItem wilderness_hut = new LegendAmenityItem(1);
    private static final LegendItem alpine_hut = new LegendAmenityItem(4);
    private static final LegendItem guest_house = new LegendAmenityItem(7);
    private static final LegendItem chalet = new LegendAmenityItem(8);
    private static final LegendItem motel = new LegendAmenityItem(10);
    private static final LegendItem hostel = new LegendAmenityItem(13);
    private static final LegendItem hotel = new LegendAmenityItem(16);
    private static final LegendItem camp_site = new LegendAmenityItem(19);
    private static final LegendItem caravan_site = new LegendAmenityItem(22);

    private static final LegendItem ice_cream = new LegendAmenityItem(25);
    private static final LegendItem confectionery = new LegendAmenityItem(28);
    private static final LegendItem alcohol = new LegendAmenityItem(31);
    private static final LegendItem beverages = new LegendAmenityItem(34);
    private static final LegendItem bakery = new LegendAmenityItem(37);
    private static final LegendItem greengrocer = new LegendAmenityItem(40);
    private static final LegendItem supermarket = new LegendAmenityItem(43);
    private static final LegendItem marketplace = new LegendAmenityItem(61);
    private static final LegendItem cafe = new LegendAmenityItem(46);
    private static final LegendItem pub = new LegendAmenityItem(49);
    private static final LegendItem bar = new LegendAmenityItem(52);
    private static final LegendItem fast_food = new LegendAmenityItem(55);
    private static final LegendItem restaurant = new LegendAmenityItem(58);

    private static final LegendItem zoo = new LegendAmenityItem(82);
    private static final LegendItem theme_park = new LegendAmenityItem(83);
    private static final LegendItem picnic_site = new LegendAmenityItem(85);
    private static final LegendItem theatre = new LegendAmenityItem(88);
    private static final LegendItem cinema = new LegendAmenityItem(91);
    private static final LegendItem library = new LegendAmenityItem(94);
    private static final LegendItem water_park = new LegendAmenityItem(100);
    private static final LegendItem beach_resort = new LegendAmenityItem(103);
    private static final LegendItem boat_rental = new LegendAmenityItem(97);
    private static final LegendItem horse_riding = new LegendAmenityItem(101);

    private static final LegendItem embassy = new LegendAmenityItem(108);
    private static final LegendItem police = new LegendAmenityItem(109);
    private static final LegendItem fire_station = new LegendAmenityItem(112);
    private static final LegendItem hospital = new LegendAmenityItem(115);
    private static final LegendItem ranger_station = new LegendAmenityItem(118);
    private static final LegendItem doctors = new LegendAmenityItem(121);
    private static final LegendItem dentist = new LegendAmenityItem(122);
    private static final LegendItem pharmacy = new LegendAmenityItem(124);
    private static final LegendItem telephone = new LegendAmenityItem(127);
    private static final LegendItem emergency_telephone = new LegendAmenityItem(130);

    private static final LegendItem sauna = new LegendAmenityItem(106);
    private static final LegendItem massage = new LegendAmenityItem(107);
    private static final LegendItem hairdresser = new LegendAmenityItem(262);

    private static final LegendItem pet_shop = new LegendAmenityItem(133);
    private static final LegendItem veterinary = new LegendAmenityItem(136);

    private static final LegendItem toys = new LegendAmenityItem(139);
    private static final LegendItem amusement_arcade = new LegendAmenityItem(142);
    private static final LegendItem playground = new LegendAmenityItem(145);

    private static final LegendItem bicycle = new LegendAmenityItem(148);
    private static final LegendItem outdoor = new LegendAmenityItem(151);
    private static final LegendItem sports = new LegendAmenityItem(154);
    private static final LegendItem gift = new LegendAmenityItem(157);
    private static final LegendItem jewelry = new LegendAmenityItem(160);
    private static final LegendItem photo = new LegendAmenityItem(163);
    private static final LegendItem books = new LegendAmenityItem(166);
    private static final LegendItem variety_store = new LegendAmenityItem(169);
    private static final LegendItem doityourself = new LegendAmenityItem(172);
    private static final LegendItem department_store = new LegendAmenityItem(175);

    private static final LegendItem copyshop = new LegendAmenityItem(265);
    private static final LegendItem laundry = new LegendAmenityItem(268);
    private static final LegendItem bank = new LegendAmenityItem(271);
    private static final LegendItem post_office = new LegendAmenityItem(274);
    private static final LegendItem atm = new LegendAmenityItem(277);
    private static final LegendItem bureau_de_change = new LegendAmenityItem(280);
    private static final LegendItem post_box = new LegendAmenityItem(283);
    private static final LegendItem shower = new LegendAmenityItem(286);

    private static final LegendItem lighthouse = new LegendAmenityItem(181);
    private static final LegendItem watermill = new LegendAmenityItem(183);
    private static final LegendItem windmill = new LegendAmenityItem(184);
    private static final LegendItem museum = new LegendAmenityItem(202);
    private static final LegendItem gallery = new LegendAmenityItem(203);
    private static final LegendItem castle = new LegendAmenityItem(190);
    private static final LegendItem fort = new LegendAmenityItem(191);
    private static final LegendItem city_gate = new LegendAmenityItem(192);
    private static final LegendItem attraction = new LegendAmenityItem(223);
    private static final LegendItem viewpoint = new LegendAmenityItem(220);
    private static final LegendItem artwork = new LegendAmenityItem(217);
    private static final LegendItem bust = new LegendAmenityItem(185);
    private static final LegendItem statue = new LegendAmenityItem(188);
    private static final LegendItem memorial = new LegendAmenityItem(189);
    private static final LegendItem stone = new LegendAmenityItem(186);
    private static final LegendItem plaque = new LegendAmenityItem(187);
    private static final LegendItem monument = new LegendAmenityItem(193);
    private static final LegendItem archaeological_site = new LegendAmenityItem(196);
    private static final LegendItem ruins = new LegendAmenityItem(199);
    private static final LegendItem wayside_shrine = new LegendAmenityItem(197);
    private static final LegendItem waterfall = new LegendAmenityItem(178);

    private static final LegendItem car = new LegendAmenityItem(229);
    private static final LegendItem car_repair = new LegendAmenityItem(232);
    private static final LegendItem car_parts = new LegendAmenityItem(233);
    private static final LegendItem car_rental = new LegendAmenityItem(235);
    private static final LegendItem motorcycle = new LegendAmenityItem(230);
    private static final LegendItem fuel = new LegendAmenityItem(238);
    private static final LegendItem charging_station = new LegendAmenityItem(239);
    private static final LegendItem slipway = new LegendAmenityItem(241);
    private static final LegendItem parking = new LegendItem(GeometryType.POLY, R.string.legend_parking, 17)
            .addTag("amenity", "parking").setKind(11);
    private static final LegendItem parking_unpaved = new LegendItem(GeometryType.POLY, R.string.legend_unpaved_parking, 17)
            .addTag("amenity", "parking").addTag("surface", "unpaved").setKind(11);
    private static final LegendItem parking_dirt = new LegendItem(GeometryType.POLY, R.string.legend_dirt_parking, 17)
            .addTag("amenity", "parking").addTag("surface", "dirt").setKind(11);
    private static final LegendItem parking_car_paid = new LegendItem(GeometryType.POLY, R.string.legend_paid_parking, 17)
            .addTag("amenity", "parking").addTag("fee", "yes").setKind(11);
    private static final LegendItem parking_private = new LegendItem(GeometryType.POLY, R.string.legend_private_parking, 17)
            .addTag("amenity", "parking").addTag("access", "private").setKind(11);

    private static final LegendItem place_of_worship = new LegendAmenityItem(420);
    private static final LegendItem jewish = new LegendAmenityItem(401);
    private static final LegendItem muslim = new LegendAmenityItem(402);
    private static final LegendItem buddhist = new LegendAmenityItem(403);
    private static final LegendItem hindu = new LegendAmenityItem(404);
    private static final LegendItem shinto = new LegendAmenityItem(405);
    private static final LegendItem christian = new LegendAmenityItem(406);
    private static final LegendItem sikh = new LegendAmenityItem(407);
    private static final LegendItem taoist = new LegendAmenityItem(408);

    private static final LegendItem bicycle_rental = new LegendAmenityItem(250);
    private static final LegendItem bicycle_repair_station = new LegendAmenityItem(251);
    private static final LegendItem bicycle_parking = new LegendAmenityItem(252);
    private static final LegendItem drinking_water = new LegendAmenityItem(253);
    private static final LegendItem shelter = new LegendAmenityItem(256);
    private static final LegendItem toilets = new LegendAmenityItem(259);
    private static final LegendItem firepit = new LegendAmenityItem(86);
    private static final LegendItem hunting_stand = new LegendAmenityItem(87);
    private static final LegendItem information_office = new LegendAmenityItem(205);
    private static final LegendItem information_guidepost = new LegendAmenityItem(208);
    private static final LegendItem information_map = new LegendAmenityItem(211);
    private static final LegendItem information = new LegendAmenityItem(214);

    private static final LegendItem fountain = new LegendAmenityItem(226);

    private static final LegendItem land = new LegendItem(GeometryType.POLY, R.string.legend_land, 14)
            .addTag("natural", "land");

    private static final HashSet<LegendItem> notRoadItems = new HashSet<>(Arrays.asList(
            educational, recreation, construction, hospital_area, aboriginal_lands, military,
            stream, ditch, grass, forest, tree_row, tree, beach, wall, retaining_wall, fence, hedge,
            power_generator_wind, water_pipeline, steam_pipeline, gas_pipeline, oil_pipeline,
            general_pipeline, runway, apron, railway_platform, bridge, pier, pitch, marina, spring,
            sports_centre, stadium, garden, camp_site_area, zoo_area, theme_park_area, dog_park,
            cemetery, railway_crossing, bus_station, subway_entrance, subway_station,
            railway_station, railway_halt, aeroway_aerodrome, aeroway_heliport, embankment,
            water_well, water_pump, city_wall, playground_area
    ));

    private static final HashSet<LegendItem> notUrbanItems = new HashSet<>(Arrays.asList(
            farmland, orchard, plant_nursery, farmyard, quarry, nature_reserve, underground_river,
            dam, lock_gate, weir, ford_point, meadow, scrub, heath, wetland, reedbed, wet_meadow,
            swamp, mangrove, bog, fen, marsh, saltmarsh, tidalflat, bare_rock, scree, shingle, mud,
            sand, glacier, ridge, arete, cliff, peak, volcano, saddle, rock, cave_entrance, contour,
            power_line, tower, water_pipeline, steam_pipeline, gas_pipeline, oil_pipeline,
            general_pipeline, spring
    ));

    private static final HashSet<LegendItem> notNightItems = new HashSet<>(Arrays.asList(
            aboriginal_lands, recreation, construction, farmland, orchard, plant_nursery, farmyard,
            quarry, underground_river, grass, meadow, scrub, heath, reedbed, wet_meadow, swamp,
            mangrove, bog, fen, marsh, saltmarsh, tidalflat, bare_rock, scree, shingle, sand, beach,
            glacier, contour, pitch, sports_centre, stadium, building, addresses, garden, marina,
            theme_park_area, camp_site_area, zoo_area, runway, apron, dog_park, cemetery,
            railway_tunnel, tram, railway_crossing, ferry, highway_services, water_well, water_pump,
            water_pipeline, steam_pipeline, gas_pipeline, oil_pipeline, general_pipeline,
            playground_area
    ));

    private static final HashSet<LegendItem> notWinterItems = new HashSet<>(Arrays.asList(
            ferry, unpaved_road, dirt_road, parking_unpaved, parking_dirt, highway_services
    ));

    private static final LegendSection administrative = new LegendSection(R.string.legend_administrative, new LegendItem[]{
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

    private static final LegendSection land_use = new LegendSection(R.string.legend_land_use, new LegendItem[]{
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
            aboriginal_lands,
            military,
            marina
    });

    private static final LegendSection water_features = new LegendSection(R.string.legend_water, new LegendItem[]{
            water,
            intermittent_water,
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

    private static final LegendSection terrain_features = new LegendSection(R.string.legend_terrain_features, new LegendItem[]{
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
            ridge,
            arete,
            cliff,
            peak,
            volcano,
            saddle,
            mountain_pass,
            rock,
            cave_entrance,
            spring,
            contour
    });

    private static final LegendSection manmade_features = new LegendSection(R.string.legend_manmade_features, new LegendItem[]{
            city_wall,
            wall,
            embankment,
            retaining_wall,
            fence,
            hedge,
            power_line.setOverlay(power_tower),
            power_generator_wind,
            tower,
            water_pipeline,
            steam_pipeline,
            gas_pipeline,
            oil_pipeline,
            general_pipeline,
            building,
            runway,
            apron,
            railway_platform,
            bridge,
            pier,
            water_well,
            water_pump,
            addresses
    });

    private static final LegendSection urban_features = new LegendSection(R.string.legend_urban, new LegendItem[]{
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

    private static final LegendSection roads = new LegendSection(R.string.legend_roads, new LegendItem[]{
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
            parking_dirt,
            highway_services,
            rest_area,
            border_control,
            toll_booth,
            lift_gate,
            kissing_gate,
            gate,
            stile,
            block,
            bollard,
            cycle_barrier
    });

    private static final LegendSection tracks = new LegendSection(R.string.legend_tracks, new LegendItem[]{
            track,
            track_bridge,
            track_tunnel,
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

    private static final LegendSection pedestrian = new LegendSection(R.string.legend_pedestrian, new LegendItem[]{
            pedestrian_area,
            pedestrian_road,
            path,
            path_visibility_good,
            path_visibility_bad,
            no_access_path,
            path_bridge,
            path_tunnel,
            steps,
            via_ferrata // TODO Should go to hiking
    });

    private static final LegendSection railways = new LegendSection(R.string.legend_railways, new LegendItem[]{
            railway,
            railway_bridge,
            railway_tunnel,
            railway_service,
            abandoned_railway,
            light_railway,
            tram,
            subway,
            monorail,
            railway_level_crossing,
            railway_crossing
    });

    private static final LegendSection aerial_ways = new LegendSection(R.string.legend_aerial_ways, new LegendItem[]{
            cable_car,
            gondola,
            chair_lift,
            drag_lift,
            zip_line,
            magic_carpet,
            aerialway_station
    });

    private static final LegendSection transportation = new LegendSection(R.string.legend_transportation, new LegendItem[]{
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

    private static final LegendSection amenities_accommodation = new LegendSection(R.string.kind_accommodation, new LegendItem[]{
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

    private static final LegendSection amenities_food = new LegendSection(R.string.kind_food, new LegendItem[]{
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


    private static final LegendSection amenities_entertainment = new LegendSection(R.string.kind_entertainment, new LegendItem[]{
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

    private static final LegendSection amenities_emergency = new LegendSection(R.string.kind_emergency, new LegendItem[]{
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

    private static final LegendSection amenities_healthbeauty = new LegendSection(R.string.kind_healthbeauty, new LegendItem[]{
            hairdresser,
            sauna,
            massage
    });

    private static final LegendSection amenities_pets = new LegendSection(R.string.kind_pets, new LegendItem[]{
            pet_shop,
            veterinary
    });

    private static final LegendSection amenities_kids = new LegendSection(R.string.kind_kids, new LegendItem[]{
            toys,
            amusement_arcade,
            playground
    });

    private static final LegendSection amenities_shopping = new LegendSection(R.string.kind_shopping, new LegendItem[]{
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

    private static final LegendSection amenities_attraction = new LegendSection(R.string.kind_attraction, new LegendItem[]{
            waterfall,
            lighthouse,
            watermill,
            windmill,
            museum,
            gallery,
            castle,
            fort,
            city_gate,
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

    private static final LegendSection amenities_urban = new LegendSection(R.string.kind_urban, new LegendItem[]{
            fountain
    });

    private static final LegendSection amenities_vehicles = new LegendSection(R.string.kind_vehicles, new LegendItem[]{
            car,
            motorcycle,
            car_parts,
            car_repair,
            car_rental,
            fuel,
            charging_station,
            slipway
    });

    private static final LegendSection amenities_religion = new LegendSection(R.string.kind_religion, new LegendItem[]{
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

    private static final LegendSection amenities_hikebike = new LegendSection(R.string.kind_hikebike, new LegendItem[]{
            bicycle,
            outdoor,
            bicycle_rental,
            bicycle_parking,
            bicycle_repair_station,
            drinking_water,
            shelter,
            toilets,
            firepit,
            information_office,
            information_guidepost,
            information_map,
            information,
            hunting_stand
    });

    private static final LegendSection amenities_service = new LegendSection(R.string.kind_service, new LegendItem[]{
            copyshop,
            laundry,
            shower,
            post_office,
            post_box,
            bank,
            atm,
            bureau_de_change
    });

    private static final LegendSection hiking_routes = new LegendSection(R.string.legend_hiking_routes, new LegendItem[]{
            hiking_route_iwn,
            hiking_route_nwn,
            hiking_route_rwn,
            hiking_route_lwn,
            hiking_route_symbol,
            hiking_path_with_route,
            hiking_road_with_route
    });

    private static final LegendSection hiking_path_visibility = new LegendSection(R.string.legend_hiking_visibility, new LegendItem[]{
            hiking_path_visibility_unknown,
            hiking_path_visibility_excellent,
            hiking_path_visibility_good,
            hiking_path_visibility_intermediate,
            hiking_path_visibility_bad,
            hiking_path_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t1 = new LegendSection(R.string.legend_hiking_sac_scale_t1, new LegendItem[]{
            hiking_path_sac_scale_t1_visibility_unknown,
            hiking_path_sac_scale_t1_visibility_excellent,
            hiking_path_sac_scale_t1_visibility_good,
            hiking_path_sac_scale_t1_visibility_intermediate,
            hiking_path_sac_scale_t1_visibility_bad,
            hiking_path_sac_scale_t1_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t2 = new LegendSection(R.string.legend_hiking_sac_scale_t2, new LegendItem[]{
            hiking_path_sac_scale_t2_visibility_unknown,
            hiking_path_sac_scale_t2_visibility_excellent,
            hiking_path_sac_scale_t2_visibility_good,
            hiking_path_sac_scale_t2_visibility_intermediate,
            hiking_path_sac_scale_t2_visibility_bad,
            hiking_path_sac_scale_t2_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t3 = new LegendSection(R.string.legend_hiking_sac_scale_t3, new LegendItem[]{
            hiking_path_sac_scale_t3_visibility_unknown,
            hiking_path_sac_scale_t3_visibility_excellent,
            hiking_path_sac_scale_t3_visibility_good,
            hiking_path_sac_scale_t3_visibility_intermediate,
            hiking_path_sac_scale_t3_visibility_bad,
            hiking_path_sac_scale_t3_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t4 = new LegendSection(R.string.legend_hiking_sac_scale_t4, new LegendItem[]{
            hiking_path_sac_scale_t4_visibility_unknown,
            hiking_path_sac_scale_t4_visibility_excellent,
            hiking_path_sac_scale_t4_visibility_good,
            hiking_path_sac_scale_t4_visibility_intermediate,
            hiking_path_sac_scale_t4_visibility_bad,
            hiking_path_sac_scale_t4_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t5 = new LegendSection(R.string.legend_hiking_sac_scale_t5, new LegendItem[]{
            hiking_path_sac_scale_t5_visibility_unknown,
            hiking_path_sac_scale_t5_visibility_excellent,
            hiking_path_sac_scale_t5_visibility_good,
            hiking_path_sac_scale_t5_visibility_intermediate,
            hiking_path_sac_scale_t5_visibility_bad,
            hiking_path_sac_scale_t5_visibility_no
    });

    private static final LegendSection hiking_sac_scale_t6 = new LegendSection(R.string.legend_hiking_sac_scale_t6, new LegendItem[]{
            hiking_path_sac_scale_t6_visibility_unknown,
            hiking_path_sac_scale_t6_visibility_excellent,
            hiking_path_sac_scale_t6_visibility_good,
            hiking_path_sac_scale_t6_visibility_intermediate,
            hiking_path_sac_scale_t6_visibility_bad,
            hiking_path_sac_scale_t6_visibility_no
    });

    private static final LegendSection cycling_routes = new LegendSection(R.string.legend_cycling_routes, new LegendItem[]{
            cycling_route_icn,
            cycling_route_ncn,
            cycling_route_rcn,
            cycling_route_lcn,
            cycling_route_mtb,
            cycling_route_number,
            cycling_network_node,
            cycling_path_with_route,
            cycling_road_with_route
    });

    private static final LegendSection cycling_paths = new LegendSection(R.string.legend_cycling_paths, new LegendItem[]{
            cycleway,
            cycleway_oneway,
            cycleway_bridge,
            cycleway_tunnel,
            cycling_road_with_tracks,
            cycling_road_with_track,
            cycling_road_doubleway,
            cycling_path_designated,
            cycling_path_yes,
            cycling_steps
    });

    private static final LegendSection cycling_path_visibility = new LegendSection(R.string.legend_cycling_visibility, new LegendItem[]{
            cycling_path_visibility_good,
            cycling_path_visibility_intermediate,
            cycling_path_visibility_bad,
            cycling_path_visibility_no
    });

    private static final LegendSection cycling_mtb = new LegendSection(R.string.legend_cycling_mtb, new LegendItem[]{
            cycling_mtb_scale0,
            cycling_mtb_scale1,
            cycling_mtb_scale2,
            cycling_mtb_scale3,
            cycling_mtb_scale4,
            cycling_mtb_scale5,
            cycling_mtb_scale6,
            cycling_mtb_scale_imba0,
            cycling_mtb_scale_imba1,
            cycling_mtb_scale_imba2,
            cycling_mtb_scale_imba3,
            cycling_mtb_scale_imba4
    });

    private static final LegendSection cycling_uphill = new LegendSection(R.string.legend_cycling_uphill, new LegendItem[]{
            cycling_mtb_scale_uphill0,
            cycling_mtb_scale_uphill1,
            cycling_mtb_scale_uphill2,
            cycling_mtb_scale_uphill3,
            cycling_mtb_scale_uphill4,
            cycling_mtb_scale_uphill5
    });

    private static final LegendSection[] themeTopo = new LegendSection[]{
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

    private static final LegendSection[] themeNight = new LegendSection[]{
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

    private static final LegendSection[] themeWinter = new LegendSection[]{
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
                    ridge,
                    arete,
                    cliff,
                    peak,
                    saddle,
                    mountain_pass
            }),
            new LegendSection(R.string.legend_manmade_features, new LegendItem[]{
                    building,
                    wall,
                    embankment,
                    fence,
                    hedge,
                    railway_platform,
                    addresses
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

    private final List<LegendItem> mData = new ArrayList<>();

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
    public void onAttach(@NonNull Context context) {
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
            case 3: // Winter
                theme = themeWinter;
                break;
            case 2: // Cycling
                theme = new LegendSection[themeTopo.length + 5];
                theme[0] = cycling_routes;
                theme[1] = cycling_paths;
                theme[2] = cycling_path_visibility;
                theme[3] = cycling_mtb;
                theme[4] = cycling_uphill;
                System.arraycopy(themeTopo, 0, theme, 5, themeTopo.length);
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
                //theme = new LegendSection[1];
                //theme[0] = new LegendSection(R.string.legend_terrain_features, new LegendItem[]{
                //        ridge,
                //        cable_car,
                //        cliff,
                //        embankment
                //});
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
            boolean hasItems = false;
            for (LegendItem item : section.items) {
                switch (activity) {
                    case 3: // Winter
                        if (notWinterItems.contains(item))
                            continue;
                        break;
                    case 2: // Cycling
                    case 1: // Hiking
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
                if (item.kind > 0 && Tags.kindZooms[item.kind] > 17)
                    continue;
                if (item instanceof LegendView.LegendAmenityItem && !Tags.isVisible(((LegendView.LegendAmenityItem)item).type))
                    continue;
                mData.add(item);
                hasItems = true;
            }
            if (!hasItems) // remove section header if all items were skipped
                mData.remove(mData.size() - 1);
        }

        //noinspection rawtypes
        for (RenderStyle style : mTheme.matchElement(land.type, land.tags, land.zoomLevel)) {
            if (style instanceof AreaStyle) {
                mBackground = ((AreaStyle) style).color;
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private class LegendListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;

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
                } else {
                    convertView = mInflater.inflate(R.layout.list_item_legend, parent, false);
                    itemHolder.item = convertView.findViewById(R.id.item);
                }
                itemHolder.name = convertView.findViewById(R.id.name);
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
