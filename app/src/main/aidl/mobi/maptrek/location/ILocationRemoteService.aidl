package mobi.maptrek.location;

import mobi.maptrek.location.ILocationCallback;

interface ILocationRemoteService
{
    void registerCallback(ILocationCallback cb);
    void unregisterCallback(ILocationCallback cb);
    boolean isLocating();
	android.location.Location getLocation();
    int getStatus();
}
