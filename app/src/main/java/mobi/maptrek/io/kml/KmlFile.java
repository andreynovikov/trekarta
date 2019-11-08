/*
 * Copyright 2018 Andrey Novikov
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

package mobi.maptrek.io.kml;

import java.util.HashMap;
import java.util.List;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;

public class KmlFile {
    public static final String NS = "http://www.opengis.net/kml/2.2";
    public static final String TAG_KML = "kml";
    public static final String TAG_DOCUMENT = "Document";
    public static final String TAG_FOLDER = "Folder";
    public static final String TAG_PLACEMARK = "Placemark";
    public static final String TAG_POINT = "Point";
    public static final String TAG_LINE_STRING = "LineString";
    public static final String TAG_STYLE_MAP = "StyleMap";
    public static final String TAG_STYLE = "Style";
    public static final String TAG_ICON_STYLE = "IconStyle";
    public static final String TAG_LINE_STYLE = "LineStyle";
    public static final String TAG_LIST_STYLE = "ListStyle";
    public static final String TAG_LIST_ITEM_TYPE = "listItemType";
    public static final String TAG_PAIR = "Pair";
    public static final String TAG_NAME = "name";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_COLOR = "color";
    public static final String TAG_WIDTH = "width";
    public static final String TAG_KEY = "key";
    public static final String TAG_STYLE_URL = "styleUrl";
    public static final String TAG_COORDINATES = "coordinates";
    public static final String TAG_OPEN = "open";
    public static final String TAG_TESSELLATE = "tessellate";
    public static final String TAG_TIME_SPAN = "TimeSpan";
    public static final String TAG_BEGIN = "begin";
    public static final String TAG_END = "end";
    public static final String ATTRIBUTE_ID = "id";

    /**
     * Converts ARGB to ABGR and vice versa
     */
    static int reverseColor(int color) {
        return ((color & 0x00FF0000) >>> 16) | ((color & 0x000000FF) << 16) | (color & 0xFF00FF00);
    }

    static public class Folder {
        String name;
        List<Folder> folders;
        List<Placemark> placemarks;
    }

    static public class Placemark {
        Style style;
        String styleUrl;
        Waypoint point;
        Track track;
    }

    static public class StyleType {
        String id;
    }

    static public class Style extends StyleType {
        LineStyle lineStyle;
        IconStyle iconStyle;
    }

    static public class ColorStyle {
        int color;
    }

    static public class LineStyle extends ColorStyle {
        float width;
    }

    static public class IconStyle extends ColorStyle {
        //TODO Add processing for this style field
        //String icon;
    }

    static public class StyleMap extends StyleType {
        HashMap<String, String> map = new HashMap<>();
    }
}
