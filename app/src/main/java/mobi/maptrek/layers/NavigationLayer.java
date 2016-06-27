package mobi.maptrek.layers;

import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;

/**
 * This class draws a great circle navigation line.
 */
public class NavigationLayer extends PathLayer {
    private GeoPoint mDestination;
    private GeoPoint mPosition;

    public NavigationLayer(Map map, int lineColor, float lineWidth) {
        super(map, lineColor, lineWidth);
    }

    public void setDestination(GeoPoint destination) {
        synchronized (mPoints) {
            mDestination = destination;
            clearPath();
            if (mPosition != null) {
                addPoint(mPosition);
                addGreatCircle(mPosition, mDestination);
                addPoint(mDestination);
            }
        }
    }

    public GeoPoint getDestination() {
        return mDestination;
    }

    public void setPosition(double lat, double lon) {
        synchronized (mPoints) {
            mPosition = new GeoPoint(lat, lon);
            clearPath();
            addPoint(mPosition);
            addGreatCircle(mPosition, mDestination);
            addPoint(mDestination);
        }
    }
}