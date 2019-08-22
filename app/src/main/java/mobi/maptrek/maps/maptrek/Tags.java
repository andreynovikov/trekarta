package mobi.maptrek.maps.maptrek;

/*
 * Copyright 2012 Hannes Janetzek
 * Copyright 2017 Andrey Novikov
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
 */

import android.support.annotation.NonNull;

public class Tags {
    public static final String[] kinds = {
            //"place",
            //"road",
            //"building",
            "kind_emergency",
            "kind_accommodation",
            "kind_food",
            "kind_attraction",
            "kind_entertainment",
            "kind_shopping",
            "kind_service",
            "kind_religion",
            "kind_education",
            "kind_kids",
            "kind_pets",
            "kind_vehicles",
            "kind_transportation",
            "kind_hikebike",
            "kind_urban",
            "kind_barrier"
    };

    //TODO Return multiple kinds if applicable
    static
    @NonNull
    String getKindName(int kind) {
        if (Tags.isPlace(kind))
            return "kind_place";
        else if (Tags.isEmergency(kind))
            return kinds[0];
        else if (Tags.isAccommodation(kind))
            return kinds[1];
        else if (Tags.isFood(kind))
            return kinds[2];
        else if (Tags.isAttraction(kind))
            return kinds[3];
        else if (Tags.isEntertainment(kind))
            return kinds[4];
        else if (Tags.isShopping(kind))
            return kinds[5];
        else if (Tags.isService(kind))
            return kinds[6];
        else if (Tags.isReligion(kind))
            return kinds[7];
        else if (Tags.isEducation(kind))
            return kinds[8];
        else if (Tags.isKids(kind))
            return kinds[9];
        else if (Tags.isPets(kind))
            return kinds[10];
        else if (Tags.isVehicles(kind))
            return kinds[11];
        else if (Tags.isTransportation(kind))
            return kinds[12];
        else if (Tags.isHikeBike(kind))
            return kinds[13];
        else if (Tags.isBuilding(kind))
            return "kind_building";
        else if (Tags.isUrban(kind))
            return kinds[14];
        else if (Tags.isRoad(kind))
            return "kind_road";
        else if (Tags.isBarrier(kind))
            return kinds[15];
        else
            return "";
    }

    public static boolean isPlace(int kind) {
        return (kind & 0x00000001) > 0;
    }

    public static boolean isRoad(int kind) {
        return (kind & 0x00000002) > 0;
    }

    public static boolean isBuilding(int kind) {
        return (kind & 0x00000004) > 0;
    }

    public static boolean isEmergency(int kind) {
        return (kind & 0x00000008) > 0;
    }

    public static boolean isAccommodation(int kind) {
        return (kind & 0x00000010) > 0;
    }

    public static boolean isFood(int kind) {
        return (kind & 0x00000020) > 0;
    }

    public static boolean isAttraction(int kind) {
        return (kind & 0x00000040) > 0;
    }

    public static boolean isEntertainment(int kind) {
        return (kind & 0x00000080) > 0;
    }

    public static boolean isShopping(int kind) {
        return (kind & 0x00000100) > 0;
    }

    public static boolean isService(int kind) {
        return (kind & 0x00000200) > 0;
    }

    public static boolean isReligion(int kind) {
        return (kind & 0x00000400) > 0;
    }

    public static boolean isEducation(int kind) {
        return (kind & 0x00000800) > 0;
    }

    public static boolean isKids(int kind) {
        return (kind & 0x00001000) > 0;
    }

    public static boolean isPets(int kind) {
        return (kind & 0x00002000) > 0;
    }

    public static boolean isVehicles(int kind) {
        return (kind & 0x00004000) > 0;
    }

    public static boolean isTransportation(int kind) {
        return (kind & 0x00008000) > 0;
    }

    public static boolean isHikeBike(int kind) {
        return (kind & 0x00010000) > 0;
    }

    public static boolean isUrban(int kind) {
        return (kind & 0x00020000) > 0;
    }

    public static boolean isBarrier(int kind) {
        return (kind & 0x00040000) > 0;
    }

    public static final int[] kindZooms = {
            15, // emergency
            16, // accommodation
            16, // food
            14, // attraction
            16, // entertainment
            17, // shopping
            17, // service
            16, // religion
            18, // education
            18, // kids
            18, // pets
            16, // vehicles
            17, // transportation
            17, // hike'n'bike
            17, // urban
            16  // barrier
    };

    final static int ATTRIB_OFFSET = 1024;

    final static String[] keys = {
            "building",
            "highway",
            "natural",
            "landuse",
            "waterway",
            "power",
            "amenity",
            "oneway",
            "ref",
            "barrier",
            "tracktype",
            "access",
            "place",
            "railway",
            "bridge",
            "leisure",
            "service",
            "tourism",
            "boundary",
            "tunnel",
            "religion",
            "shop",
            "man_made",
            "area",
            "building:part",
            "population",
            "fee",
            "generator:source",
            "historic",
            "aeroway",
            "admin_level",
            "piste:type",
            "piste:difficulty",
            "ford",
            "emergency",
            "aerialway",
            "mountain_pass",
            "capital",
            "route",
            "icao",
            "iata",
            "station",
            "contour",
            "construction",
            "cutting",
            "embankment",
            "intermittent",
            "lock",
            "sport",
            "surface",
            "toll",
            "tower:type",
            "wetland",
            "maritime",
            "winter_road",
            "ice_road",
            "4wd_only",
            "sac_scale",
            "trail_visibility",
            "osmc:symbol",
            "network",
            "route:network",
            "information",
            "piste:border",
            "piste:grooming",
            "piste:lit",
            "piste:oneway"
    };
    final static int MAX_KEY = keys.length - 1;

    // most popular values for the selected keys
    public final static String[] values = {
            "elevation_major",
            "elevation_medium",
            "elevation_minor",
            "reserved",
            "reserved",
            "reserved",
            "reserved",
            "reserved",
            "reserved",
            "reserved",
            "yes",
            "residential",
            "house",
            "service",
            "track",
            "stream",
            "unclassified",
            "tower",
            "tree",
            "1",
            "water",
            "wood",
            "footway",
            "path",
            "tertiary",
            "private",
            "farmland",
            "secondary",
            "garage",
            "parking_aisle",
            "fence",
            "parking",
            "grass",
            "apartments",
            "meadow",
            "primary",
            "wall",
            "bus_stop",
            "rail",
            "scrub",
            "industrial",
            "wetland",
            "administrative",
            "grade3",
            "ditch",
            "grade2",
            "hut",
            "locality",
            "school",
            "gate",
            "pitch",
            "village",
            "hamlet",
            "river",
            "detached",
            "grade4",
            "traffic_signals",
            "cycleway",
            "hedge",
            "place_of_worship",
            "roof",
            "living_street",
            "shed",
            "grade1",
            "trunk",
            "commercial",
            "steps",
            "restaurant",
            "motorway",
            "grade5",
            "christian",
            "park",
            "drain",
            "farmyard",
            "no",
            "level_crossing",
            "motorway_link",
            "orchard",
            "pedestrian",
            "peak",
            "cemetery",
            "retail",
            "line",
            "construction",
            "information",
            "garages",
            "terrace",
            "reservoir",
            "garden",
            "playground",
            "grassland",
            "fuel",
            "generator",
            "cliff",
            "hotel",
            "supermarket",
            "riverbank",
            "vineyard",
            "road",
            "trunk_link",
            "cafe",
            "canal",
            "greenhouse",
            "primary_link",
            "fast_food",
            "pier",
            "bollard",
            "isolated_dwelling",
            "bank",
            "post_box",
            "farm_auxiliary",
            "pharmacy",
            "kindergarten",
            "church",
            "secondary_link",
            "barn",
            "abandoned",
            "allotments",
            "wind",
            "hospital",
            "lift_gate",
            "heath",
            "toilets",
            "shelter",
            "basin",
            "cutline",
            "island",
            "6",
            "sports_centre",
            "drinking_water",
            "memorial",
            "post_office",
            "warehouse",
            "pub",
            "dam",
            "hairdresser",
            "taxiway",
            "retaining_wall",
            "tertiary_link",
            "attraction",
            "quarry",
            "bakery",
            "neighbourhood",
            "atm",
            "bar",
            "sand",
            "muslim",
            "car_repair",
            "viewpoint",
            "suburb",
            "cabin",
            "university",
            "beach",
            "ruins",
            "farm",
            "picnic_site",
            "police",
            "telephone",
            "civic",
            "town",
            "platform",
            "manufacture",
            "station",
            "solar",
            "crossing",
            "guest_house",
            "-1",
            "fire_station",
            "disused",
            "spring",
            "village_green",
            "camp_site",
            "tram",
            "scree",
            "car",
            "fountain",
            "doctors",
            "nordic",
            "bridleway",
            "office",
            "brownfield",
            "artwork",
            "library",
            "museum",
            "cycle_barrier",
            "downhill",
            "collapsed",
            "static_caravan",
            "public",
            "protected_area",
            "4",
            "recreation_ground",
            "common",
            "college",
            "block",
            "monument",
            "greenhouse_horticulture",
            "nature_reserve",
            "tram_stop",
            "hangar",
            "runway",
            "doityourself",
            "bus_station",
            "phone",
            "subway",
            "mall",
            "stadium",
            "aerodrome",
            "easy",
            "glacier",
            "chapel",
            "castle",
            "toll_booth",
            "helipad",
            "motel",
            "buddhist",
            "narrow_gauge",
            "military",
            "semidetached_house",
            "golf_course",
            "halt",
            "hostel",
            "intermediate",
            "bungalow",
            "entrance",
            "bridge",
            "theatre",
            "slipway",
            "cave_entrance",
            "bicycle_rental",
            "apron",
            "city_wall",
            "veterinary",
            "5",
            "light_rail",
            "subway_entrance",
            "2",
            "landfill",
            "cinema",
            "caravan_site",
            "dormitory",
            "train_station",
            "storage_tank",
            "transportation",
            "city",
            "mosque",
            "damaged",
            "stable",
            "pet",
            "national_park",
            "plant_nursery",
            "semi",
            "shingle",
            "ferry",
            "hindu",
            "trullo",
            "0",
            "8",
            "transformer_tower",
            "alpine_hut",
            "houseboat",
            "bunker",
            "drag_lift",
            "advanced",
            "shinto",
            "preserved",
            "marsh",
            "mud",
            "water_park",
            "oil",
            "lighthouse",
            "shop",
            "chair_lift",
            "hydro",
            "bureau_de_change",
            "chain",
            "slurry_tank",
            "windmill",
            "jewish",
            "terminal",
            "novice",
            "pajaru",
            "cowshed",
            "dog_park",
            "zoo",
            "silo",
            "residence",
            "miniature",
            "duplex",
            "factory",
            "temple",
            "border_control",
            "dock",
            "6",
            "volcano",
            "4",
            "agricultural",
            "gas",
            "7",
            "2",
            "tank",
            "kiosk",
            "5",
            "region",
            "3",
            "carport",
            "dwelling_house",
            "wilderness_hut",
            "3",
            "pavilion",
            "chalet",
            "allotment_house",
            "store",
            "coal",
            "support",
            "10",
            "monorail",
            "ruin",
            "summer_cottage",
            "boathouse",
            "grandstand",
            "mobile_home",
            "9",
            "outbuilding",
            "anexo",
            "sled",
            "glasshouse",
            "storage",
            "state",
            "taoist",
            "elevator",
            "wayside_shrine",
            "cathedral",
            "beach_hut",
            "residences",
            "bangunan",
            "building",
            "semi-detached",
            "biomass",
            "expert",
            "education",
            "column",
            "magic_carpet",
            "home",
            "tent",
            "other",
            "flats",
            "veranda",
            "apartment",
            "government",
            "government_office",
            "gondola",
            "cable_car",
            "shrine",
            "funicular",
            "sport",
            "proposed",
            "biogas",
            "hall",
            "clinic",
            "utility",
            "monastery",
            "community_group_office",
            "prison",
            "gazebo",
            "houses",
            "destroyed",
            "container",
            "offices",
            "base",
            "canopy",
            "manor",
            "part",
            "conservatory",
            "sikh",
            "freeride",
            "townhouse",
            "verdieping",
            "window",
            "nuclear",
            "summer_house",
            "general",
            "substation",
            "unknown",
            "semi_detached",
            "bell_tower",
            "diesel",
            "healthcare",
            "depot",
            "basilica",
            "power_substation",
            "clubhouse",
            "flat",
            "foundation",
            "biofuel",
            "electricity",
            "synagogue",
            "room",
            "unit",
            "marketplace",
            "interval",
            "power",
            "some",
            "balcony",
            "gasometer",
            "villa",
            "village_office",
            "photovoltaic",
            "1",
            "family_house",
            "public_building",
            "prefab_container",
            "convent",
            "chimney",
            "storage_tank",
            "multifaith",
            "hay_barn",
            "electricity_network",
            "digester",
            "voodoo",
            "business",
            "building_concrete",
            "wayside_chapel",
            "generic_building",
            "arbour",
            "barne",
            "airport",
            "brewery",
            "condominium",
            "floor",
            "technical",
            "toilet",
            "data_center",
            "stables",
            "undefined",
            "railway_station",
            "train",
            "works",
            "prefabricated",
            "trailer_park",
            "porch",
            "farmhouse",
            "country",
            "historic",
            "minor",
            "pillar",
            "spiritualist",
            "condominiums",
            "default",
            "household",
            "stall",
            "wine_cellar",
            "varies",
            "waste",
            "cultural",
            "geothermal",
            "bandstand",
            "club_house",
            "palace",
            "sports_hall",
            "cottage",
            "riding_school",
            "railway",
            "kitchen",
            "gym",
            "0",
            "air_shaft",
            "stands",
            "livestock",
            "embassy",
            "none",
            "ship",
            "tech_cab",
            "shops",
            "free",
            "terraced_house",
            "kiln",
            "barrack",
            "fossil",
            "mixed_use",
            "amenity",
            "presbytery",
            "townhall",
            "services",
            "ramp",
            "demolished",
            "sea",
            "parish",
            "maisonette",
            "row_house",
            "parish_hall",
            "bahai",
            "baishin",
            "allotment",
            "musalla",
            "tribune",
            "derelict",
            "boat",
            "gymnasium",
            "cooling_tower",
            "guardhouse",
            "mink_shed",
            "shack",
            "tier",
            "stairs",
            "heliport",
            "pumping_station",
            "mortuary",
            "water_tank",
            "barracks",
            "tomb",
            "tumulus",
            "weir",
            "way",
            "yurta",
            "attached",
            "viaduct",
            "cellar",
            "different",
            "recreation",
            "parish_church",
            "aviary",
            "dovecote",
            "health_post",
            "stand",
            "religious",
            "mixed",
            "patio",
            "water_tower",
            "sauna",
            "convenience",
            "department_store",
            "greengrocer",
            "hardware",
            "alcohol",
            "outdoor",
            "gift",
            "toys",
            "variety_store",
            "jewelry",
            "books",
            "confectionery",
            "bicycle",
            "beverages",
            "copyshop",
            "photo",
            "laundry",
            "dry_cleaning",
            "ice_cream",
            "bare_rock",
            "lock_gate",
            "unpaved",
            "dirt",
            "hiking",
            "bicycle",
            "mtb",
            "iwn",
            "nwn",
            "rwn",
            "lwn",
            "icn",
            "ncn",
            "rcn",
            "lcn",
            "t1",
            "t2",
            "t3",
            "t4",
            "t5",
            "t6",
            "excellent",
            "good",
            "bad",
            "horrible",
            "saddle",
            "waterfall",
            "guidepost",
            "map",
            "skitour",
            "hike",
            "ice_skate",
            "sleigh",
            "snow_park",
            "sports",
            "swimming_pool",
            "scooter",
            "mogul",
            "backcountry",
            "ski_jump",
            "ski_jump_landing",
            "ranger_station",
            "car_rental",
            "archaeological_site",
            "beach_resort",
            "sauna",
            "ferry_terminal",
            "shower"
    };
    public final static int MAX_VALUE = values.length - 1;
}
