package mobi.maptrek.data;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class Track {
    public String name;
    public String description;
    public boolean show;
    public int width = -1;
    public int color = -1;
    public String style;
    public DataSource source; // back reference to it's source

    //TODO Do we need this?
    public long maxPoints = 0;
    public float distance;
    public boolean removed = false;

    private final List<TrackSegment> mSegments = new ArrayList<>(1);
    private TrackSegment mLastSegment;
    private TrackPoint mLastTrackPoint;

    public class TrackPoint extends GeoPoint {
        //TODO Refactor path readers to make fields final
        public boolean continuous;
        public double elevation;
        public double speed;
        public double bearing;
        public double accuracy;
        public long time;

        public TrackPoint(boolean cont, int latE6, int lonE6, double elev, double spd, double brn, double acc, long t) {
            super(latE6, lonE6);
            continuous = cont;
            elevation = elev;
            speed = spd;
            bearing = brn;
            accuracy = acc;
            time = t;
        }
    }

    public class TrackSegment {
        private final List<TrackPoint> mTrackPoints = new ArrayList<>(0);

        public TrackSegment() {
        }

        public List<TrackPoint> getPoints() {
            return mTrackPoints;
        }
    }

    public Track() {
        this("", false);
    }

    public Track(String name, boolean show) {
        this.name = name;
        this.show = show;
        distance = 0;
        mLastSegment = new TrackSegment();
        mSegments.add(mLastSegment);
    }

    public Track(String name, boolean show, long max) {
        this(name, show);
        maxPoints = max;
    }

    public synchronized void copyFrom(Track track) {
        mSegments.clear();
        TrackSegment trackSegment = null;
        for (TrackSegment segment: track.getSegments()) {
            trackSegment = new TrackSegment();
            mSegments.add(trackSegment);
            trackSegment.mTrackPoints.addAll(segment.getPoints());
        }
        mLastSegment = trackSegment;
        mLastTrackPoint = track.getLastPoint();
        name = track.name;
        description = track.description;
        color = track.color;
        width = track.width;
        distance = track.distance;
    }

    public List<TrackSegment> getSegments() {
        return mSegments;
    }

    public synchronized int getPointCount() {
        int count = 0;
        for (TrackSegment segment : mSegments) {
            count += segment.mTrackPoints.size();
        }
        return count;
    }

    /**
     * Returns <b>new</b> list of track points, no synchronization is necessary
     * on that list.
     *
     * @return new List
     */
    public synchronized List<TrackPoint> getAllPoints() {
        List<TrackPoint> trackPoints = new ArrayList<>(0);
        for (TrackSegment segment : mSegments) {
            synchronized (segment) {
                trackPoints.addAll(segment.mTrackPoints);
            }
        }
        return trackPoints;
    }

    public void addPoint(boolean continuous, double lat, double lon, double elev, double speed, double bearing, double accuracy, long time) {
        if (mLastTrackPoint != null) {
            distance += GeoPoint.distance(mLastTrackPoint.latitudeE6 / 1E6, mLastTrackPoint.longitudeE6 / 1E6, lat, lon);
        }
        mLastTrackPoint = new TrackPoint(continuous, (int) (lat * 1E6), (int) (lon * 1E6), elev, speed, bearing, accuracy, time);
        if (!continuous) {
            synchronized (this) {
                mLastSegment = new TrackSegment();
                mSegments.add(mLastSegment);
            }
        }
        synchronized (mLastSegment) {
            if (maxPoints > 0 && mLastSegment.mTrackPoints.size() > maxPoints) {
                // TODO add correct cleaning if preferences changed
                TrackPoint fp = mLastSegment.mTrackPoints.get(0);
                TrackPoint sp = mLastSegment.mTrackPoints.get(1);
                distance -= GeoPoint.distance(fp.latitudeE6 / 1E6, fp.longitudeE6 / 1E6, sp.latitudeE6 / 1E6, sp.longitudeE6 / 1E6);
                mLastSegment.mTrackPoints.remove(0);
            }
            mLastSegment.mTrackPoints.add(mLastTrackPoint);
        }
    }

    public synchronized void clear() {
        mSegments.clear();
        mLastSegment = new TrackSegment();
        mSegments.add(mLastSegment);
        mLastTrackPoint = null;
        distance = 0;
    }

    /**
     * Returns the track point at the specified location in this Track.
     *
     * @param location the index of the element to return
     * @return the element at the specified location
     * @throws IndexOutOfBoundsException if location < 0 || location >= track length
     */
    public synchronized TrackPoint getPoint(int location) throws IndexOutOfBoundsException {
        int i = 0;
        for (TrackSegment segment : mSegments) {
            synchronized (segment) {
                int s = segment.mTrackPoints.size();
                if (i + s > location)
                    return segment.mTrackPoints.get(location - i);
                else
                    i += s;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    public TrackPoint getLastPoint() {
        return mLastTrackPoint;
    }

	/*
    public void removePoint(int location) throws IndexOutOfBoundsException
	{
		synchronized (mTrackPoints)
		{
			boolean last = location == mTrackPoints.size() - 1;
			TrackPoint pp = mTrackPoints.get(location - 1);
			TrackPoint cp = mTrackPoints.get(location);
			distance -= Geo.distance(pp.latitude, pp.longitude, cp.latitude, cp.longitude);
			if (! last)
			{
				TrackPoint np = mTrackPoints.get(location + 1);
				distance -= Geo.distance(cp.latitude, cp.longitude, np.latitude, np.longitude);
				distance += Geo.distance(pp.latitude, pp.longitude, np.latitude, np.longitude);
			}
			mTrackPoints.remove(location);
			if (last)
				mLastTrackPoint = pp;
		}
	}

	public void cutAfter(int location)
	{
		synchronized (mTrackPoints)
		{
			List<TrackPoint> tps = new ArrayList<TrackPoint>(mTrackPoints.subList(0, location + 1));
			mTrackPoints.clear();
			mTrackPoints.addAll(tps);
			if (mTrackPoints.size() > 0)
				mLastTrackPoint = mTrackPoints.get(mTrackPoints.size() - 1);
			else
				mLastTrackPoint = null;
		}
	}

	public void cutBefore(int location)
	{
		synchronized (mTrackPoints)
		{
			List<TrackPoint> tps = new ArrayList<TrackPoint>(mTrackPoints.subList(location, mTrackPoints.size()));
			mTrackPoints.clear();
			mTrackPoints.addAll(tps);
		}
	}
*/
}
