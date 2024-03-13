/*
 * Copyright 2024 Andrey Novikov
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

package mobi.maptrek.location;

import android.app.Service;

public abstract class BaseNavigationService extends Service
{
	/**
	 * Action to initiate navigation to map object registered by MapTrek (by id),
	 * which allows to navigate to moving object. In this mode navigation
	 * is not restored if application is restarted.
	 */
	public static final String NAVIGATE_TO_OBJECT = "mobi.maptrek.location.NAVIGATE_TO_OBJECT";
	/**
	 * Action to initiate navigation to map object. Navigation is restored if
	 * application is restarted.
	 */
	public static final String NAVIGATE_TO_POINT = "mobi.maptrek.location.NAVIGATE_TO_POINT";
	/**
	 * Action to initiate navigation via route. Navigation is restored if
	 * application is restarted.
	 */
	public static final String NAVIGATE_VIA_ROUTE = "mobi.maptrek.location.NAVIGATE_ROUTE";
	/**
	 * Service command to pause navigation. Is used only in background mode when service
	 * alone.
	 */
	public static final String PAUSE_NAVIGATION = "mobi.maptrek.location.pauseNavigation";
	/**
	 * Service command to resume paused navigation.
	 * alone.
	 */
	public static final String RESUME_NAVIGATION = "mobi.maptrek.location.resumeNavigation";
	/**
	 * Service command to stop navigation.
	 */
	public static final String STOP_NAVIGATION = "mobi.maptrek.location.stopNavigation";

	/**
	 * Map object id as returned by MapTrek. Used with NAVIGATE_TO_OBJECT action. Type: long
	 */
	public static final String EXTRA_ID = "id";
	/**
	 * Map object name. Type: String
	 */
	public static final String EXTRA_NAME = "name";
	/**
	 * Map object latitude. Type: double
	 */
	public static final String EXTRA_LATITUDE = "latitude";
	/**
	 * Map object longitude. Type: double
	 */
	public static final String EXTRA_LONGITUDE = "longitude";
	/**
	 * Map object proximity. Type: int
	 */
	public static final String EXTRA_PROXIMITY = "proximity";
	/**
	 * Route parcel. Type: Parcelable
	 */
	public static final String EXTRA_ROUTE = "route";
	/**
	 * Route direction: DIRECTION_FORWARD or DIRECTION_REVERSE.
	 */
	public static final String EXTRA_ROUTE_DIRECTION = "direction";
	/**
	 * Route start point index. Zero based, optional. Type: int
	 */
	public static final String EXTRA_ROUTE_START = "start";
	/**
	 * Navigation state. Type: int
	 */
	public static final String EXTRA_STATE = "state";
	/**
	 * Target moving state. Type: boolean
	 */
	public static final String EXTRA_MOVING_TARGET = "moving";

	public static final String BROADCAST_NAVIGATION_STATUS = "mobi.maptrek.navigationStatusChanged";
	public static final String BROADCAST_NAVIGATION_STATE = "mobi.maptrek.navigationStateChanged";

	public static final int STATE_STARTED = 1;
	public static final int STATE_NEXT_ROUTE_POINT = 2;
	public static final int STATE_REACHED = 3;
	public static final int STATE_STOPPED = 4;
	public static final int STATE_PAUSED = 5;

	public static final int DIRECTION_FORWARD = 1;
	public static final int DIRECTION_REVERSE = -1;
}
