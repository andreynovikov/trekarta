package mobi.maptrek.fragments;

import mobi.maptrek.data.Waypoint;

public interface OnWaypointActionListener
{
	/**
	 * Position map so that waypoint is visible
	 */
	void onWaypointView(Waypoint waypoint);
	void onWaypointDetails(Waypoint waypoint);
	void onWaypointNavigate(Waypoint waypoint);
	void onWaypointShare(Waypoint waypoint);
	void onWaypointSave(Waypoint waypoint);
	void onWaypointRemove(Waypoint waypoint);
}
