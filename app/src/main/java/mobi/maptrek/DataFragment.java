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

package mobi.maptrek;

import android.app.Fragment;
import android.os.Bundle;

import mobi.maptrek.data.Waypoint;
import mobi.maptrek.maps.MapFile;

public class DataFragment extends Fragment {

    private Waypoint mEditedWaypoint;
    private MapFile mBitmapLayerMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public Waypoint getEditedWaypoint() {
        return mEditedWaypoint;
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
    }

    public MapFile getBitmapLayerMap() {
        return mBitmapLayerMap;
    }

    public void setBitmapLayerMap(MapFile bitmapLayerMap) {
        mBitmapLayerMap = bitmapLayerMap;
    }
}