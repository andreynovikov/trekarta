package mobi.maptrek.data;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.style.TrackStyle;

public class Track {
    public int id;
    public String name;
    public String description;
    public boolean show;
    public TrackStyle style = new TrackStyle();
    public DataSource source; // back reference to it's source
    public boolean removed = false;

    public final List<TrackPoint> points = new ArrayList<>();
    private TrackPoint mLastTrackPoint;
    private float mDistance = Float.NaN;

    public class TrackPoint extends GeoPoint {
        //TODO Refactor path readers to make fields final
        public final boolean continuous;
        public final float elevation;
        public final float speed;
        public final float bearing;
        public final float accuracy;
        public final long time;

        //TODO Use Float.NaN to indicate unset values
        public TrackPoint(boolean cont, int latE6, int lonE6, float elev, float spd, float brn, float acc, long t) {
            super(latE6, lonE6);
            continuous = cont;
            elevation = elev;
            speed = spd;
            bearing = brn;
            accuracy = acc;
            time = t;
        }
    }

    public Track() {
        this("", false);
    }

    public Track(String name, boolean show) {
        this.name = name;
        this.show = show;
    }

    public synchronized void copyFrom(Track track) {
        points.clear();
        points.addAll(track.points);
        mLastTrackPoint = track.getLastPoint();
        name = track.name;
        description = track.description;
        track.style.copy(style);
        mDistance = track.mDistance;
    }

    public void addPoint(boolean continuous, int latE6, int lonE6, float elev, float speed, float bearing, float accuracy, long time) {
        TrackPoint previous = mLastTrackPoint;
        if (mLastTrackPoint == null)
            mDistance = 0f;
        mLastTrackPoint = new TrackPoint(continuous, latE6, lonE6, elev, speed, bearing, accuracy, time);
        if (previous != null)
            mDistance += previous.distanceTo(mLastTrackPoint);
        synchronized (points) {
            points.add(mLastTrackPoint);
        }
    }

    /**
     * Adds point without calculating distance, suitable for loading track from file. Should not be
     * mixed with addPoint.
     */
    public void addPointFast(boolean continuous, int latE6, int lonE6, float elev, float speed, float bearing, float accuracy, long time) {
        mLastTrackPoint = new TrackPoint(continuous, latE6, lonE6, elev, speed, bearing, accuracy, time);
        synchronized (points) {
            points.add(mLastTrackPoint);
        }
    }

    public float getDistance() {
        if (Float.isNaN(mDistance)) {
            mDistance = 0f;
            synchronized (points) {
                TrackPoint previous = mLastTrackPoint;
                if (points.size() > 1) {
                    for (int i = points.size() - 2; i >= 0; i--) {
                        TrackPoint current = points.get(i);
                        mDistance += previous.distanceTo(current);
                        previous = current;
                    }
                }
            }
        }
        return mDistance;
    }

    public synchronized void clear() {
        points.clear();
        mLastTrackPoint = null;
        mDistance = Float.NaN;
    }

    public TrackPoint getLastPoint() {
        return mLastTrackPoint;
    }
}
