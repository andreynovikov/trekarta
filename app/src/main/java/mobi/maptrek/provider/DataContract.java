package mobi.maptrek.provider;

import android.net.Uri;

public final class DataContract
{
    public static final String AUTHORITY = "mobi.maptrek.data";
    public static final String ACTION_PICK_MARKER = "mobi.maptrek.provider.PICK_MARKER";

    protected static final String MAPOBJECTS_PATH = "mapobjects";
    public static final Uri MAPOBJECTS_URI = Uri.parse("content://" + AUTHORITY + "/" + MAPOBJECTS_PATH);
    protected static final String MARKERS_PATH = "markers";
    public static final Uri MARKERS_URI = Uri.parse("content://" + AUTHORITY + "/" + MARKERS_PATH);

    public static final String[] MAPOBJECT_COLUMNS = new String[] {"latitude", "longitude", "bitmap", "name", "description", "marker", "color"};
    /**
     * Latitude (double, required)
     */
    public static final int MAPOBJECT_LATITUDE_COLUMN = 0;
    /**
     * Longitude (double, required)
     */
    public static final int MAPOBJECT_LONGITUDE_COLUMN = 1;
    /**
     * Bitmap (ByteArray, required if name is not provided)
     */
    public static final int MAPOBJECT_BITMAP_COLUMN = 2;
    /**
     * Name (String, required if bitmap is not provided)
     */
    public static final int MAPOBJECT_NAME_COLUMN = 3;
    /**
     * Description (String, optional)
     */
    public static final int MAPOBJECT_DESCRIPTION_COLUMN = 4;
    /**
     * Image marker, from markers pack (String, optional)
     */
    public static final int MAPOBJECT_MARKER_COLUMN = 5;
    /**
     * Color (int, optional)
     */
    public static final int MAPOBJECT_COLOR_COLUMN = 6;

    public static final String MAPOBJECT_ID_SELECTION = "IDLIST";

    public static final String[] MARKER_COLUMNS = new String[] {"BITMAP"};
    public static final int MARKER_COLUMN = 0;
}
