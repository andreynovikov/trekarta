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

package mobi.maptrek.util;

import android.support.annotation.DrawableRes;

import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;

public class ResUtils {
    public static @DrawableRes int getKindIcon(int kind) {
        if (Tags.isPlace(kind))
            return R.drawable.ic_location_city;
        else if (Tags.isEmergency(kind))
            return R.drawable.ic_local_hospital;
        else if (Tags.isAccommodation(kind))
            return R.drawable.ic_hotel;
        else if (Tags.isFood(kind))
            return R.drawable.ic_local_dining;
        else if (Tags.isAttraction(kind))
            return R.drawable.ic_account_balance;
        else if (Tags.isEntertainment(kind))
            return R.drawable.ic_local_see;
        else if (Tags.isHikeBike(kind))
            return R.drawable.ic_directions_bike;
        else if (Tags.isShopping(kind))
            return R.drawable.ic_shopping_cart;
        else if (Tags.isService(kind))
            return R.drawable.ic_local_laundry_service;
        else if (Tags.isReligion(kind))
            return R.drawable.ic_change_history;
        else if (Tags.isEducation(kind))
            return R.drawable.ic_school;
        else if (Tags.isKids(kind))
            return R.drawable.ic_child_care;
        else if (Tags.isPets(kind))
            return R.drawable.ic_pets;
        else if (Tags.isVehicles(kind))
            return R.drawable.ic_directions_car;
        else if (Tags.isTransportation(kind))
            return R.drawable.ic_directions_bus;
        else if (Tags.isBuilding(kind))
            return R.drawable.ic_business;
        else if (Tags.isUrban(kind))
            return R.drawable.ic_nature_people;
        else if (Tags.isRoad(kind))
            return R.drawable.ic_drag_handle;
        else if (Tags.isBarrier(kind))
            return R.drawable.ic_do_not_disturb_on;
        else
            return 0;
    }
}
