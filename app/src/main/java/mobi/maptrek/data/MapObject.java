package mobi.maptrek.data;

//TODO Refactor
public class MapObject {
    public long _id = 0;
    public String name;
    public String description;
    public double latitude;
    public double longitude;
    /**
     * Object altitude, if set to Integer.MIN_VALUE then it is undefined
     */
    public int altitude = Integer.MIN_VALUE;
    public int proximity = 0;
    public int color;
    public String icon;
    public String style;

    public MapObject() {
    }

    public MapObject(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

    @Override
    public boolean equals(Object o) {
        return this._id != 0 && o instanceof MapObject && this._id == ((MapObject) o)._id || super.equals(o);
    }
}
