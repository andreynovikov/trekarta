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
	public static final String NAVIGATE_ROUTE = "mobi.maptrek.location.NAVIGATE_ROUTE";
	/**
	 * Service command to pause navigation. Is used only in background mode when service
	 * alone.
	 */
	public static final String PAUSE_NAVIGATION = "mobi.maptrek.location.pauseNavigation";
	/**
	 * Service command to stop navigation.
	 */
	public static final String STOP_NAVIGATION = "mobi.maptrek.location.stopNavigation";
	/**
	 * Service command to start background mode. Service is switched to <em>Foreground</em>
	 * state to ensure it will not be killed by OS.
	 */
	public static final String ENABLE_BACKGROUND_NAVIGATION = "mobi.maptrek.location.enableBackgroundNavigation";
	/**
	 * Service command to stop background mode. Service is switched back from <em>Foreground</em>
	 * state. Navigation is continued.
	 */
	public static final String DISABLE_BACKGROUND_NAVIGATION = "mobi.maptrek.location.disableBackgroundNavigation";

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
	 * Route index as returned by Androzic. Type: int
	 */
	public static final String EXTRA_ROUTE_INDEX = "index";
	/**
	 * Route direction: DIRECTION_FORWARD or DIRECTION_REVERSE.
	 */
	public static final String EXTRA_ROUTE_DIRECTION = "direction";
	/**
	 * Route start waypoint index. Zero based, optional. Type: int
	 */
	public static final String EXTRA_ROUTE_START = "start";

	public static final String BROADCAST_NAVIGATION_STATUS = "mobi.maptrek.navigationStatusChanged";
	public static final String BROADCAST_NAVIGATION_STATE = "mobi.maptrek.navigationStateChanged";

	public static final int STATE_STARTED = 1;
	public static final int STATE_NEXT_WPT = 2;
	public static final int STATE_REACHED = 3;
	public static final int STATE_STOPPED = 4;

	public static final int DIRECTION_FORWARD = 1;
	public static final int DIRECTION_REVERSE = -1;
}
