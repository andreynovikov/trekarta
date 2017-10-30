package mobi.maptrek.location;

public interface ITrackingListener
{
    void onNewPoint(boolean continous, double lat, double lon, float elev, float speed, float track, float accuracy, long time);
}
