package mobi.maptrek.maps.maptrek;

/*
 * Copyright 2012 Hannes Janetzek
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
 */

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;

import java.util.ArrayList;
import java.util.Arrays;

import mobi.maptrek.R;

public class Tags {
    static final Tag TAG_KIND = new Tag("kind", "yes");
    static final Tag TAG_FEATURE = new Tag("feature", "yes");
    static final Tag TAG_DEPTH = new Tag("depth", "yes");
    static final Tag TAG_MEASURED = new Tag("measured", "yes");
    static final Tag TAG_FEE = new Tag("fee", "yes");
    static final Tag TAG_WHEELCHAIR_YES = new Tag("wheelchair", "yes");
    static final Tag TAG_WHEELCHAIR_LIMITED = new Tag("wheelchair", "limited");
    static final Tag TAG_WHEELCHAIR_NO = new Tag("wheelchair", "no");

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
            "kind_healthbeauty",
            "kind_kids",
            "kind_pets",
            "kind_vehicles",
            "kind_transportation",
            "kind_hikebike",
            "kind_urban",
            "kind_barrier"
    };

    public static boolean accessibility = true;

    //TODO Return multiple kinds if applicable
    public static @NonNull String getKindName(int kind) {
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
        else if (Tags.isHealthBeauty(kind))
            return kinds[8];
        else if (Tags.isEntertainment(kind))
            return kinds[4];
        else if (Tags.isHikeBike(kind))
            return kinds[13];
        else if (Tags.isShopping(kind))
            return kinds[5];
        else if (Tags.isService(kind))
            return kinds[6];
        else if (Tags.isReligion(kind))
            return kinds[7];
        else if (Tags.isKids(kind))
            return kinds[9];
        else if (Tags.isPets(kind))
            return kinds[10];
        else if (Tags.isVehicles(kind))
            return kinds[11];
        else if (Tags.isTransportation(kind))
            return kinds[12];
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

    public static boolean isHealthBeauty(int kind) {
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

    public static boolean isRoute(int kind) {
        return (kind & 0x80000000) < 0;
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
            17, // health'n'beauty
            18, // kids
            18, // pets
            16, // vehicles
            17, // transportation
            17, // hike'n'bike
            17, // urban
            16  // barrier
    };

    public final static int[] selectedTypes = {7, //guest house
            13, //hostel
            19, //camp_site
            43, //supermarket
            58, //restaurant
            121, //doctors
            124, //pharmacy
            220, //viewpoint
            253, //drinking water
            259, //toilet
            286 //shower
    };

    public final static Tag[] typeTags = {
            null,
            new Tag("tourism", "wilderness_hut"), // 1
            null, null,
            new Tag("tourism", "alpine_hut"), // 4
            null, null,
            new Tag("tourism", "guest_house"), // 7
            new Tag("tourism", "chalet"), // 8
            null,
            new Tag("tourism", "motel"), // 10
            null, null,
            new Tag("tourism", "hostel"), // 13
            null, null,
            new Tag("tourism", "hotel"), // 16
            null, null,
            new Tag("tourism", "camp_site"), // 19
            null, null,
            new Tag("tourism", "caravan_site"), // 22
            null, null,
            new Tag("shop", "ice_cream"), // 25
            null, null,
            new Tag("shop", "confectionery"), // 28
            null, null,
            new Tag("shop", "alcohol"), // 31
            null, null,
            new Tag("shop", "beverages"), // 34
            null, null,
            new Tag("shop", "bakery"), // 37
            null, null,
            new Tag("shop", "greengrocer"), // 40
            null, null,
            new Tag("shop", "supermarket"), // 43
            null, null,
            new Tag("amenity", "cafe"), // 46
            null, null,
            new Tag("amenity", "pub"), // 49
            null, null,
            new Tag("amenity", "bar"), // 52
            null, null,
            new Tag("amenity", "fast_food"), // 55
            null, null,
            new Tag("amenity", "restaurant"), // 58
            null, null,
            new Tag("amenity", "marketplace"), // 61
            null, null,
            new Tag("barrier", "block"), // 64
            null, null,
            new Tag("barrier", "bollard"), // 67
            new Tag("barrier", "stile"), // 68
            null,
            new Tag("barrier", "cycle_barrier"), // 70
            null, null,
            new Tag("barrier", "lift_gate"), // 73
            new Tag("barrier", "kissing_gate"), // 74
            null,
            new Tag("barrier", "gate"), // 76
            null, null, null, null, null,
            new Tag("tourism", "zoo"), // 82
            new Tag("tourism", "theme_park"), // 83
            null,
            new Tag("tourism", "picnic_site"), // 85
            new Tag("leisure", "firepit"), // 86
            new Tag("amenity", "hunting_stand"), // 87
            new Tag("amenity", "theatre"), // 88
            null, null,
            new Tag("amenity", "cinema"), // 91
            null, null,
            new Tag("amenity", "library"), // 94
            null, null,
            new Tag("amenity", "boat_rental"), // 97
            null, null,
            new Tag("leisure", "water_park"), // 100
            new Tag("leisure", "horse_riding"), // 101
            null,
            new Tag("leisure", "beach_resort"), // 103
            null, null,
            new Tag("leisure", "sauna"), // 106
            new Tag("shop", "massage"), // 107
            new Tag("diplomatic", "embassy"), // 108
            new Tag("amenity", "police"), // 109
            null, null,
            new Tag("amenity", "fire_station"), // 112
            null, null,
            new Tag("amenity", "hospital"), // 115
            null, null,
            new Tag("amenity", "ranger_station"), // 118
            null, null,
            new Tag("amenity", "doctors"), // 121
            new Tag("amenity", "dentist"), // 122
            null,
            new Tag("amenity", "pharmacy"), // 124
            null, null,
            new Tag("amenity", "telephone"), // 127
            null, null,
            new Tag("emergency", "phone"), // 130
            null, null,
            new Tag("shop", "pet"), // 133
            null, null,
            new Tag("amenity", "veterinary"), // 136
            null, null,
            new Tag("shop", "toys"), // 139
            null, null,
            new Tag("leisure", "amusement_arcade"), // 142
            null, null,
            new Tag("leisure", "playground"), // 145
            null, null,
            new Tag("shop", "bicycle"), // 148
            null, null,
            new Tag("shop", "outdoor"), // 151
            null, null,
            new Tag("shop", "sports"), // 154
            null, null,
            new Tag("shop", "gift"), // 157
            null, null,
            new Tag("shop", "jewelry"), // 160
            null, null,
            new Tag("shop", "photo"), // 163
            null, null,
            new Tag("shop", "books"), // 166
            null, null,
            new Tag("shop", "variety_store"), // 169
            null, null,
            new Tag("shop", "doityourself"), // 172
            null, null,
            new Tag("shop", "department_store"), // 175
            null, null,
            new Tag("waterway", "waterfall"), // 178
            null, null,
            new Tag("man_made", "lighthouse"), // 181
            null,
            new Tag("man_made", "watermill"), // 183
            new Tag("man_made", "windmill"), // 184
            new ExtendedTag("historic", "memorial").addTag("memorial", "bust"), // 185
            new ExtendedTag("historic", "memorial").addTag("memorial", "stone"), // 186
            new ExtendedTag("historic", "memorial").addTag("memorial", "plaque"), // 187
            new ExtendedTag("historic", "memorial").addTag("memorial", "statue"), // 188
            new Tag("historic", "memorial"), // 189
            new Tag("historic", "castle"), // 190
            new Tag("historic", "fort"), // 191
            new Tag("historic", "city_gate"), // 192
            new Tag("historic", "monument"), // 193
            null, null,
            new Tag("historic", "archaeological_site"), // 196
            new Tag("historic", "wayside_shrine"), // 197
            null,
            new Tag("historic", "ruins"), // 199
            null, null,
            new Tag("tourism", "museum"), // 202
            new Tag("tourism", "gallery"), // 203
            null,
            new ExtendedTag("tourism", "information").addTag("information", "office"), // 205
            null, null,
            new ExtendedTag("tourism", "information").addTag("information", "guidepost"), // 208
            null, null,
            new ExtendedTag("tourism", "information").addTag("information", "map"), // 211
            null, null,
            new Tag("tourism", "information"), // 214
            null, null,
            new Tag("tourism", "artwork"), // 217
            null, null,
            new Tag("tourism", "viewpoint"), // 220
            null, null,
            new Tag("tourism", "attraction"), // 223
            null, null,
            new Tag("amenity", "fountain"), // 226
            null, null,
            new Tag("shop", "car"), // 229
            new Tag("shop", "motorcycle"), // 230
            null,
            new Tag("shop", "car_repair"), // 232
            new Tag("shop", "car_parts"), // 233
            null,
            new Tag("amenity", "car_rental"), // 235
            null, null,
            new Tag("amenity", "fuel"), // 238
            new Tag("amenity", "charging_station"), // 239
            null,
            new Tag("leisure", "slipway"), // 241
            null, null,
            new Tag("amenity", "parking"), // 244
            null, null,
            new Tag("amenity", "bus_station"), // 247
            new Tag("highway", "bus_stop"), // 248
            new Tag("railway", "tram_stop"), // 249
            new Tag("amenity", "bicycle_rental"), // 250
            new Tag("amenity", "bicycle_repair_station"), // 251
            new Tag("amenity", "bicycle_parking"), // 252
            new Tag("amenity", "drinking_water"), // 253
            null, null,
            new Tag("amenity", "shelter"), // 256
            null, null,
            new Tag("amenity", "toilets"), // 259
            null, null,
            new Tag("shop", "hairdresser"), // 262
            null, null,
            new Tag("shop", "copyshop"), // 265
            null, null,
            new Tag("shop", "laundry"), // 268
            null, null,
            new Tag("amenity", "bank"), // 271
            null, null,
            new Tag("amenity", "post_office"), // 274
            null, null,
            new Tag("amenity", "atm"), // 277
            null, null,
            new Tag("amenity", "bureau_de_change"), // 280
            null, null,
            new Tag("amenity", "post_box"), // 283
            null, null,
            new Tag("amenity", "shower"), // 286
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, // -300
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "jewish"), // 401
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "muslim"), // 402
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "buddhist"), // 403
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "hindu"), // 404
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "shinto"), // 405
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "christian"), // 406
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "sikh"), // 407
            new ExtendedTag("amenity", "place_of_worship").addTag("religion", "taoist"), // 408
            null, null, null, null, null, null, null, null, null, null, null,
            new Tag("amenity", "place_of_worship") // 420
    };

    final static Tag[] typeAliasTags = new Tag[] {
            new Tag("shop", "farm"), // 40
            new Tag("shop", "convenience"), // 43
            new Tag("shop", "hardware"), // 172
            new Tag("shop", "mall"), // 175
            new Tag("shop", "dry_cleaning"), // 268
            new ExtendedTag("tourism", "artwork").addTag("artwork_type", "statue"), // 188
            new ExtendedTag("tourism", "artwork").addTag("artwork_type", "bust"), // 185
            new ExtendedTag("tourism", "artwork").addTag("artwork_type", "stone") // 186
    };

    public final static int[] typeNames = {
            -1,
            R.string.legend_wilderness_hut, // 1
            -1, -1,
            R.string.legend_alpine_hut, // 4
            -1, -1,
            R.string.legend_guest_house, // 7
            R.string.legend_chalet, // 8
            -1,
            R.string.legend_motel, // 10
            -1, -1,
            R.string.legend_hostel, // 13
            -1, -1,
            R.string.legend_hotel, // 16
            -1, -1,
            R.string.legend_camp_site, // 19
            -1, -1,
            R.string.legend_caravan_site, // 22
            -1, -1,
            R.string.legend_ice_cream_shop, // 25
            -1, -1,
            R.string.legend_confectionery_shop, // 28
            -1, -1,
            R.string.legend_alcohol_shop, // 31
            -1, -1,
            R.string.legend_beverages_shop, // 34
            -1, -1,
            R.string.legend_bakery, // 37
            -1, -1,
            R.string.legend_greengrocer, // 40
            -1, -1,
            R.string.legend_supermarket, // 43
            -1, -1,
            R.string.legend_cafe, // 46
            -1, -1,
            R.string.legend_pub, // 49
            -1, -1,
            R.string.legend_bar, // 52
            -1, -1,
            R.string.legend_fast_food, // 55
            -1, -1,
            R.string.legend_restaurant, // 58
            -1, -1,
            R.string.legend_marketplace, // 61
            -1, -1,
            R.string.legend_block, // 64
            -1, -1,
            R.string.legend_bollard, // 67
            R.string.legend_stile, // 68
            -1,
            R.string.legend_cycle_barrier, // 70
            -1, -1,
            R.string.legend_lift_gate, // 73
            R.string.legend_kissing_gate, // 74
            -1,
            R.string.legend_gate, // 76
            -1, -1, -1, -1, -1,
            R.string.legend_zoo, // 82
            R.string.legend_theme_park, // 83
            -1,
            R.string.legend_picnic_site, // 85
            R.string.legend_firepit, // 86
            R.string.legend_hunting_stand, // 87
            R.string.legend_theatre, // 88
            -1, -1,
            R.string.legend_cinema, // 91
            -1, -1,
            R.string.legend_library, // 94
            -1, -1,
            R.string.legend_boat_rental, // 97
            -1, -1,
            R.string.legend_water_park, // 100
            R.string.legend_horse_riding, // 101
            -1,
            R.string.legend_beach_resort, // 103
            -1, -1,
            R.string.legend_sauna, // 106
            R.string.legend_massage, // 107
            R.string.legend_embassy, // 108
            R.string.legend_police_office, // 109
            -1, -1,
            R.string.legend_fire_station, // 112
            -1, -1,
            R.string.legend_hospital, // 115
            -1, -1,
            R.string.legend_ranger_station, // 118
            -1, -1,
            R.string.legend_doctors_practice, // 121
            R.string.legend_dentist, // 122
            -1,
            R.string.legend_pharmacy, // 124
            -1, -1,
            R.string.legend_telephone, // 127
            -1, -1,
            R.string.legend_emergency_telephone, // 130
            -1, -1,
            R.string.legend_pet_shop, // 133
            -1, -1,
            R.string.legend_veterinary_clinic, // 136
            -1, -1,
            R.string.legend_toys_shop, // 139
            -1, -1,
            R.string.legend_amusement_arcade, // 142
            -1, -1,
            R.string.legend_playground, // 145
            -1, -1,
            R.string.legend_bicycle_shop, // 148
            -1, -1,
            R.string.legend_outdoor_shop, // 151
            -1, -1,
            R.string.legend_sports_shop, // 154
            -1, -1,
            R.string.legend_gift_shop, // 157
            -1, -1,
            R.string.legend_jewelry_shop, // 160
            -1, -1,
            R.string.legend_photo_shop, // 163
            -1, -1,
            R.string.legend_books_shop, // 166
            -1, -1,
            R.string.legend_variety_store, // 169
            -1, -1,
            R.string.legend_diy_store, // 172
            -1, -1,
            R.string.legend_department_store, // 175
            -1, -1,
            R.string.legend_waterfall, // 178
            -1, -1,
            R.string.legend_lighthouse, // 181
            -1,
            R.string.legend_watermill, // 183
            R.string.legend_windmill, // 184
            R.string.legend_bust, // 185
            R.string.legend_stone, // 186
            R.string.legend_plaque, // 187
            R.string.legend_statue, // 188
            R.string.legend_memorial, // 189
            R.string.legend_castle, // 190
            R.string.legend_fort, // 191
            R.string.legend_city_gate, // 192
            R.string.legend_monument, // 193
            -1, -1,
            R.string.legend_archaeological_site, // 196
            R.string.legend_wayside_shrine, // 197
            -1,
            R.string.legend_ruins, // 199
            -1, -1,
            R.string.legend_museum, // 202
            R.string.legend_gallery, // 203
            -1,
            R.string.legend_information_office, // 205
            -1, -1,
            R.string.legend_guidepost, // 208
            -1, -1,
            R.string.legend_map, // 211
            -1, -1,
            R.string.legend_information, // 214
            -1, -1,
            R.string.legend_artwork, // 217
            -1, -1,
            R.string.legend_viewpoint, // 220
            -1, -1,
            R.string.legend_attraction, // 223
            -1, -1,
            R.string.legend_fountain, // 226
            -1, -1,
            R.string.legend_car_dialer, // 229
            R.string.legend_motorcycle_shop, // 230
            -1,
            R.string.legend_car_repair, // 232
            R.string.legend_car_parts, // 233
            -1,
            R.string.legend_car_rental, // 235
            -1, -1,
            R.string.legend_fuel_station, // 238
            R.string.legend_charging_station, // 239
            -1,
            R.string.legend_slipway, // 241
            -1, -1,
            R.string.legend_parking, // 244
            -1, -1,
            R.string.legend_bus_station, // 247
            R.string.legend_bus_stop, // 248
            R.string.legend_tram_stop, // 249
            R.string.legend_bicycle_rental, // 250
            R.string.legend_bicycle_repair_station, // 251
            R.string.legend_bicycle_parking, // 252
            R.string.legend_drinking_water, // 253
            -1, -1,
            R.string.legend_shelter, // 256
            -1, -1,
            R.string.legend_toilets, // 259
            -1, -1,
            R.string.legend_hairdresser, // 262
            -1, -1,
            R.string.legend_copy_shop, // 265
            -1, -1,
            R.string.legend_laundry, // 268
            -1, -1,
            R.string.legend_bank, // 271
            -1, -1,
            R.string.legend_post_office, // 274
            -1, -1,
            R.string.legend_atm, // 277
            -1, -1,
            R.string.legend_currency_exchange, // 280
            -1, -1,
            R.string.legend_post_box, // 283
            -1, -1,
            R.string.legend_shower, // 286
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // -300
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            R.string.legend_jewish_place, // 401
            R.string.legend_muslim_place, // 402
            R.string.legend_buddhist_place, // 403
            R.string.legend_hindu_place, // 404
            R.string.legend_shinto_place, // 405
            R.string.legend_christian_place, // 406
            R.string.legend_sikh_place, // 407
            R.string.legend_taoist_place, // 408
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            R.string.legend_place_of_worship // 420
    };

    public final static boolean[] typeSelectable = {
            false,
            true, // 1
            false, false,
            true, // 4
            false, false,
            true, // 7
            true, // 8
            false,
            true, // 10
            false, false,
            true, // 13
            false, false,
            true, // 16
            false, false,
            true, // 19
            false, false,
            true, // 22
            false, false,
            true, // 25
            false, false,
            true, // 28
            false, false,
            true, // 31
            false, false,
            true, // 34
            false, false,
            true, // 37
            false, false,
            true, // 40
            false, false,
            true, // 43
            false, false,
            true, // 46
            false, false,
            true, // 49
            false, false,
            true, // 52
            false, false,
            true, // 55
            false, false,
            true, // 58
            false, false,
            true, // 61
            false, false,
            false, // 64 - block
            false, false,
            false, // 67 - bollard
            false, // 68 - stile
            false,
            false, // 70 - cycle barrier
            false, false,
            false, // 73 - lift gate
            false, // 74 - kissing gate
            false,
            false, // 76 - gate
            false, false, false, false, false,
            true, // 82
            true, // 83
            false,
            true, // 85
            false, // 86 - fire pit
            false, // 87 - hunting stand
            true, // 88
            false, false,
            true, // 91
            false, false,
            true, // 94
            false, false,
            true, // 97
            false, false,
            true, // 100
            true, // 101
            false,
            true, // 103
            false, false,
            true, // 106
            true, // 107
            true, // 108
            true, // 109
            false, false,
            true, // 112
            false, false,
            true, // 115
            false, false,
            true, // 118
            false, false,
            true, // 121
            true, // 122
            false,
            true, // 124
            false, false,
            true, // 127
            false, false,
            true, // 130
            false, false,
            true, // 133
            false, false,
            true, // 136
            false, false,
            true, // 139
            false, false,
            true, // 142
            false, false,
            false, // 145 - playground
            false, false,
            true, // 148
            false, false,
            true, // 151
            false, false,
            true, // 154
            false, false,
            true, // 157
            false, false,
            true, // 160
            false, false,
            true, // 163
            false, false,
            true, // 166
            false, false,
            true, // 169
            false, false,
            true, // 172
            false, false,
            true, // 175
            false, false,
            true, // 178
            false, false,
            true, // 181
            false,
            true, // 183
            true, // 184
            true, // 185
            true, // 186
            true, // 187
            true, // 188
            true, // 189
            true, // 190
            true, // 191
            true, // 192
            true, // 193
            false, false,
            true, // 196
            true, // 197
            false,
            true, // 199
            false, false,
            true, // 202
            true, // 203
            false,
            true, // 205
            false, false,
            false, // 208 - guidepost
            false, false,
            false, // 211 - map
            false, false,
            false, // 214 - information
            false, false,
            true, // 217
            false, false,
            true, // 220
            false, false,
            true, // 223
            false, false,
            false, // 226 - fountain
            false, false,
            true, // 229
            true, // 230
            false,
            true, // 232
            true, // 233
            false,
            true, // 235
            false, false,
            true, // 238
            true, // 239
            false,
            false, // 241 - slipway
            false, false,
            false, // 244 - parking
            false, false,
            true, // 247
            true, // 248
            true, // 249
            true, // 250
            true, // 251
            false, // 252 - bicycle parking
            true, // 253
            false, false,
            true, // 256
            false, false,
            true, // 259
            false, false,
            true, // 262
            false, false,
            true, // 265
            false, false,
            true, // 268
            false, false,
            true, // 271
            false, false,
            true, // 274
            false, false,
            true, // 277
            false, false,
            true, // 280
            false, false,
            false, // 283 - post box
            false, false,
            true, // 286
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, // -300
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            true, // 401
            true, // 402
            true, // 403
            true, // 404
            true, // 405
            true, // 406
            true, // 407
            true, // 408
            false, false, false, false, false, false, false, false, false, false, false,
            true // 420
    };

    public static final int[][] kindTypes = {
            new int[] {108, 109, 112, 115, 118, 121, 122, 124, 127, 130}, // emergency
            new int[] {1, 4, 7, 8, 10, 13, 16, 19, 22}, // accommodation
            new int[] {25, 28, 31, 34, 37, 40, 43, 46, 49, 52, 55, 58, 61}, // food
            new int[] {178, 181, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 196, 197, 199, 202, 203, 205, 208, 211, 214, 217, 220, 223}, // attraction
            new int[] {82, 83, 85, 88, 91, 94, 97, 100, 101, 103}, // entertainment
            new int[] {148, 151, 154, 157, 160, 163, 166, 169, 43, 172, 175, 61}, // shopping
            new int[] {265, 268, 271, 274, 277, 280, 283, 286}, // service
            new int[] {401, 402, 403, 404, 405, 406, 407, 408, 420}, // religion
            new int[] {106, 107, 262}, // health'n'beauty
            new int[] {139, 142, 145}, // kids
            new int[] {133, 136}, // pets
            new int[] {229, 230, 232, 233, 235, 238, 239, 241, 244}, // vehicles
            new int[] {247, 248, 249}, // transportation
            new int[] {148, 151, 118, 250, 251, 252, 253, 256, 259, 1, 4, 86, 87, 205, 208, 211, 214}, // hike'n'bike
            new int[] {226}, // urban
            new int[] {64, 67, 68, 70, 73, 74, 76}  // barrier
    };

    private final static int[] typeZooms = new int[typeTags.length];

    final static String[] typeSelectors = new String[4];

    static int highlightedType = -1;
    static ArrayList<Integer> highlightedTypes = new ArrayList<Integer>();

    public static void setTypeTag(int type, TagSet tags) {
        if (type < 0 || type > typeTags.length || typeTags[type] == null)
            return;
        tags.add(typeTags[type]);
        if (typeTags[type] instanceof ExtendedTag) {
            Tag tag = typeTags[type];
            while ((tag = ((ExtendedTag) tag).next) != null) {
                tags.add(tag);
            }
        }
        tags.add(Tags.TAG_FEATURE);
    }

    public static void setFlags(int flags, ExtendedMapElement element) {
        if ((flags & 0x00000001) == 0x00000001)
            element.tags.add(TAG_FEE);
        if (accessibility) {
            if ((flags & 0x00000006) == 0x00000006)
                element.tags.add(TAG_WHEELCHAIR_YES);
            else if ((flags & 0x00000004) == 0x00000004)
                element.tags.add(TAG_WHEELCHAIR_LIMITED);
            else if ((flags & 0x00000002) == 0x00000002)
                element.tags.add(TAG_WHEELCHAIR_NO);
        }
    }

    public static void setExtra(int type, int enum1, TagSet tags) {
        if (type < 0 || type > typeTags.length || typeTags[type] == null)
            return;
        switch (type) {
            case 239: // charging_station
                tags.add(new Tag("capacity", String.valueOf(enum1)));
            case 252: // bicycle_parking
                tags.add(new Tag("capacity", String.valueOf(enum1)));
        }
    }

    @StringRes
    public static int getTypeName(int type) {
        if (type >= 0 && type < typeNames.length)
            return typeNames[type];
        return -1;
    }

    public static Drawable getTypeDrawable(Context context, IRenderTheme theme, int type) {
        TagSet tags = new TagSet();
        setTypeTag(type, tags);
        if (tags.size() == 0)
            return null;

        //noinspection rawtypes
        RenderStyle[] styles = theme.matchElement(GeometryBuffer.GeometryType.POINT, tags, 17);
        if (styles != null) {
            //noinspection rawtypes
            for (RenderStyle style : styles) {
                if (style instanceof SymbolStyle) {
                    try {
                        org.oscim.backend.canvas.Bitmap bitmap;
                        bitmap = ((SymbolStyle) style).bitmap;
                        return new BitmapDrawable(context.getResources(), AndroidGraphics.getBitmap(bitmap));
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static void setHighlightedType(int type) {
        if (type >= 0 && type < typeNames.length) {
            highlightedTypes.add(type);
        }
    }

    public static void removeHighlightedType(int type) {
        if (type >= 0 && type < typeNames.length) {
            highlightedTypes.remove(Integer.valueOf(type));
        }
    }

    public static void resetHighlightedType() {
        highlightedTypes = new ArrayList<Integer>();
    }


    public static boolean isVisible(int type) {
        return typeZooms[type] < 18;
    }

    public static boolean isVisible(int type, int zoom) {
        return typeZooms[type] <= zoom;
    }

    public static void recalculateTypeZooms() {
        Arrays.fill(typeZooms, 18);
        StringBuilder[] lists = {
                new StringBuilder(),
                new StringBuilder(),
                new StringBuilder(),
                new StringBuilder()
        };
        String[] delimiters = {"", "", "", ""};
        for (int k = 0; k < kinds.length; k++) {
            for (int t = 0; t < kindTypes[k].length; t++) {
                typeZooms[kindTypes[k][t]] = kindZooms[k];
                switch (typeZooms[kindTypes[k][t]]) {
                    case 14:
                        lists[0].append(delimiters[0]).append(kindTypes[k][t]);
                        delimiters[0] = ",";
                    case 15:
                        lists[1].append(delimiters[1]).append(kindTypes[k][t]);
                        delimiters[1] = ",";
                    case 16:
                        lists[2].append(delimiters[2]).append(kindTypes[k][t]);
                        delimiters[2] = ",";
                    case 17:
                        lists[3].append(delimiters[3]).append(kindTypes[k][t]);
                        delimiters[3] = ",";
                }
            }
        }
        typeSelectors[0] = lists[0].toString();
        typeSelectors[1] = lists[1].toString();
        typeSelectors[2] = lists[2].toString();
        typeSelectors[3] = lists[3].toString();
    }

    final static String[] roofShapes = {
            "flat",
            "skillion",
            "gabled",
            "half-hipped",
            "hipped",
            "pyramidal",
            "gambrel",
            "mansard",
            "dome",
            "onion",
            "round",
            "saltbox"
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
            "smoothness",
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
            "foot",
            "information",
            "piste:border",
            "piste:grooming",
            "piste:lit",
            "piste:oneway",
            "memorial",
            "diplomatic",
            "addr:interpolation",
            "substance",
            "pump",
            "cycleway",
            "cycleway:right",
            "cycleway:left",
            "bicycle",
            "ramp:bicycle",
            "oneway:bicycle",
            "mtb:scale",
            "mtb:scale:uphill",
            "mtb:scale:imba",
            "artwork_type"
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
            "intermediate",
            "ditch",
            "very_bad",
            "hut",
            "locality",
            "school",
            "gate",
            "pitch",
            "village",
            "hamlet",
            "river",
            "detached",
            "very_horrible",
            "bicycle_parking",
            "cycleway",
            "hedge",
            "place_of_worship",
            "roof",
            "living_street",
            "shed",
            "tree_row",
            "trunk",
            "commercial",
            "steps",
            "restaurant",
            "motorway",
            "impassable",
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
            "motorcycle",
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
            "water_well",
            "volcano",
            "watermill",
            "agricultural",
            "gas",
            "7",
            "pipeline",
            "tank",
            "kiosk",
            "bicycle_repair_station",
            "region",
            "3",
            "carport",
            "dwelling_house",
            "wilderness_hut",
            "designated",
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
            "charging_station",
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
            "car_parts",
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
            "city_gate",
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
            "fort",
            "ferry_terminal",
            "shower",
            "zip_line",
            "boat_rental",
            "theme_park",
            "amusement_arcade",
            "bay",
            "strait",
            "reedbed",
            "saltmarsh",
            "wet_meadow",
            "swamp",
            "mangrove",
            "bog",
            "string_bog",
            "tidalflat",
            "fen",
            "via_ferrata",
            "rock",
            "statue",
            "bust",
            "stone",
            "plaque",
            "massage",
            "dentist",
            "hunting_stand",
            "horse_riding",
            "gallery",
            "kissing_gate",
            "stile",
            "firepit",
            "marina",
            "weir",
            "hot_water",
            "rest_area",
            "embankment",
            "turntable",
            "aboriginal_lands",
            "ridge",
            "arete"
    };
    public final static int MAX_VALUE = values.length - 1;
}
