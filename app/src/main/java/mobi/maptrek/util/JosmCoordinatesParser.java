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

/*
JOSM License
============

JOSM, and all its integral parts, are released under the GNU General
Public License v2 or later.

The GPL v3 is accessible here:
http://www.gnu.org/licenses/gpl.html

The GPL v2 is accessible here:
http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package mobi.maptrek.util;

import android.support.annotation.NonNull;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;

/**
 * Support for parsing a {@link GeoPoint} object from a string.
 */
public final class JosmCoordinatesParser {

    /**
     * Character denoting South, as string
     */
    public static final String SOUTH = "Ю";
    /**
     * Character denoting North, as string
     */
    public static final String NORTH = "С";
    /**
     * Character denoting West, as string
     */
    public static final String WEST = "З";
    /**
     * Character denoting East, as string
     */
    public static final String EAST = "В";

    private static final char N_TR = NORTH.charAt(0);
    private static final char S_TR = SOUTH.charAt(0);
    private static final char E_TR = EAST.charAt(0);
    private static final char W_TR = WEST.charAt(0);

    private static final String DEG = "\u00B0";
    private static final String MIN = "\u2032";
    private static final String SEC = "\u2033";

    private static final Pattern P = Pattern.compile("" +
            "([+|-]?\\d+[.,]\\d+)|"                       // (1)
            + "([+|-]?\\d+)|"                             // (2)
            + "(" + DEG + "|o|deg)|"                      // (3)
            + "('|" + MIN + "|min)|"                      // (4)
            + "(\"|" + SEC + "|sec)|"                     // (5)
            + "([,;])|"                                   // (6)
            + "([NSEW" + N_TR + S_TR + E_TR + W_TR + "])|"// (7)
            + "\\s+|.+", Pattern.CASE_INSENSITIVE);

    // https://en.wikipedia.org/wiki/Military_Grid_Reference_System
    // http://pro.arcgis.com/en/pro-app/tool-reference/data-management/supported-notation-formats.htm#ESRI_SECTION1_D21879366B6A4D0FB1F731148AD27271
    private static final Pattern P_UTM = Pattern.compile("" +
            "([1-9]\\d?)" +                                             // (1)
            "([CDEFGHJKLMNPQRSTUVWX])\\s?" +                            // (2)
            "([ABCDEFGHJKLMNPQRSTUVWXYZ][ABCDEFGHJKLMNPQRSTUV])?\\s?" + // (3)
            "((\\d+)(:?\\s(\\d+))?)");                                  // (4,5,6)

    private static final Pattern P_XML = Pattern.compile(
            "lat=[\"']([+|-]?\\d+[.,]\\d+)[\"']\\s+lon=[\"']([+|-]?\\d+[.,]\\d+)[\"']");

    private static class LatLonHolder {
        private double lat = Double.NaN;
        private double lon = Double.NaN;
    }

    public static class Result {
        public int offset;
        public GeoPoint coordinates;
    }


    private JosmCoordinatesParser() {
        // private constructor
    }

    /**
     * Parses the given string as lat/lon.
     *
     * @param input String to parse
     * @return parsed lat/lon together with matched string offset
     */
    @NonNull
    public static Result parseWithResult(@NonNull String input) throws IllegalArgumentException {
        Result result = new Result();
        final LatLonHolder latLon = new LatLonHolder();
        final Matcher mXml = P_XML.matcher(input);
        if (mXml.matches()) {
            setLatLonObj(latLon,
                    Double.valueOf(mXml.group(1).replace(',', '.')), 0.0, 0.0, "N",
                    Double.valueOf(mXml.group(2).replace(',', '.')), 0.0, 0.0, "E");
            result.coordinates = new GeoPoint(latLon.lat, latLon.lon);
            return result;
        }
        final Matcher mUtm = P_UTM.matcher(input);
        if (mUtm.lookingAt()) {
            result.offset = mUtm.end();
            if (mUtm.group(3) != null) {
                MGRSCoord mgrs = MGRSCoord.fromString(mUtm.group());
                result.coordinates = new GeoPoint(mgrs.getLatitude().degrees, mgrs.getLongitude().degrees);
                return result;
            } else {
                int zone = Integer.valueOf(mUtm.group(1));
                String hemisphere = mUtm.group(2);
                double easting, northing;
                if ("N".equals(hemisphere))
                    hemisphere = AVKey.NORTH;
                if ("S".equals(hemisphere))
                    hemisphere = AVKey.SOUTH;
                if (mUtm.group(6) != null) {
                    easting = Double.valueOf(mUtm.group(5));
                    northing = Double.valueOf(mUtm.group(6));
                } else {
                    String en = mUtm.group(4);
                    int l = en.length() >> 1;
                    easting = Double.valueOf(en.substring(0, l));
                    northing = Double.valueOf(en.substring(l, en.length()));
                }
                UTMCoord utm = UTMCoord.fromUTM(zone, hemisphere, easting, northing);
                result.coordinates = new GeoPoint(utm.getLatitude().degrees, utm.getLongitude().degrees);
                return result;
            }
        }

        final Matcher m = P.matcher(input);

        final StringBuilder sb = new StringBuilder();
        final List<Object> list = new ArrayList<>();

        while (m.find()) {
            if (m.group(1) != null) {
                sb.append('R');     // floating point number
                list.add(Double.valueOf(m.group(1).replace(',', '.')));
                result.offset = m.end();
            } else if (m.group(2) != null) {
                sb.append('Z');     // integer number
                list.add(Double.valueOf(m.group(2)));
                result.offset = m.end();
            } else if (m.group(3) != null) {
                sb.append('o');     // degree sign
                result.offset = m.end();
            } else if (m.group(4) != null) {
                sb.append('\'');    // seconds sign
                result.offset = m.end();
            } else if (m.group(5) != null) {
                sb.append('"');     // minutes sign
                result.offset = m.end();
            } else if (m.group(6) != null) {
                sb.append(',');     // separator
                result.offset = m.end();
            } else if (m.group(7) != null) {
                sb.append('x');     // cardinal direction
                String c = m.group(7).toUpperCase(Locale.ENGLISH);
                if ("N".equalsIgnoreCase(c) || "S".equalsIgnoreCase(c) || "E".equalsIgnoreCase(c) || "W".equalsIgnoreCase(c)) {
                    list.add(c);
                } else {
                    list.add(c.replace(N_TR, 'N').replace(S_TR, 'S')
                            .replace(E_TR, 'E').replace(W_TR, 'W'));
                }
                result.offset = m.end();
            }
        }

        final String pattern = sb.toString();

        final Object[] params = list.toArray();

        if (pattern.matches("Ro?,?Ro?")) {
            setLatLonObj(latLon,
                    params[0], 0.0, 0.0, "N",
                    params[1], 0.0, 0.0, "E");
        } else if (pattern.matches("xRo?,?xRo?")) {
            setLatLonObj(latLon,
                    params[1], 0.0, 0.0, params[0],
                    params[3], 0.0, 0.0, params[2]);
        } else if (pattern.matches("Ro?x,?Ro?x")) {
            setLatLonObj(latLon,
                    params[0], 0.0, 0.0, params[1],
                    params[2], 0.0, 0.0, params[3]);
        } else if (pattern.matches("Zo[RZ]'?,?Zo[RZ]'?|Z[RZ],?Z[RZ]")) {
            setLatLonObj(latLon,
                    params[0], params[1], 0.0, "N",
                    params[2], params[3], 0.0, "E");
        } else if (pattern.matches("xZo[RZ]'?,?xZo[RZ]'?|xZo?[RZ],?xZo?[RZ]")) {
            setLatLonObj(latLon,
                    params[1], params[2], 0.0, params[0],
                    params[4], params[5], 0.0, params[3]);
        } else if (pattern.matches("Zo[RZ]'?x,?Zo[RZ]'?x|Zo?[RZ]x,?Zo?[RZ]x")) {
            setLatLonObj(latLon,
                    params[0], params[1], 0.0, params[2],
                    params[3], params[4], 0.0, params[5]);
        } else if (pattern.matches("ZoZ'[RZ]\"?,?ZoZ'[RZ]\"?|ZZ[RZ],?ZZ[RZ]")) {
            setLatLonObj(latLon,
                    params[0], params[1], params[2], "N",
                    params[3], params[4], params[5], "E");
        } else if (pattern.matches("ZoZ'[RZ]\"?x,?ZoZ'[RZ]\"?x|ZZ[RZ]x,?ZZ[RZ]x")) {
            setLatLonObj(latLon,
                    params[0], params[1], params[2], params[3],
                    params[4], params[5], params[6], params[7]);
        } else if (pattern.matches("xZoZ'[RZ]\"?,?xZoZ'[RZ]\"?|xZZ[RZ],?xZZ[RZ]")) {
            setLatLonObj(latLon,
                    params[1], params[2], params[3], params[0],
                    params[5], params[6], params[7], params[4]);
        } else if (pattern.matches("ZZ[RZ],?ZZ[RZ]")) {
            setLatLonObj(latLon,
                    params[0], params[1], params[2], "N",
                    params[3], params[4], params[5], "E");
        } else {
            throw new IllegalArgumentException("invalid format: " + pattern);
        }

        result.coordinates = new GeoPoint(latLon.lat, latLon.lon);
        return result;
    }

    /**
     * Parses the given string as lat/lon.
     *
     * @param input String to parse
     * @return parsed lat/lon
     */
    @NonNull
    public static GeoPoint parse(@NonNull String input) throws IllegalArgumentException {
        Result result = parseWithResult(input);
        return result.coordinates;
    }

    private static void setLatLonObj(final LatLonHolder latLon,
                                     final Object coord1deg, final Object coord1min, final Object coord1sec, final Object card1,
                                     final Object coord2deg, final Object coord2min, final Object coord2sec, final Object card2) {

        setLatLon(latLon,
                (Double) coord1deg, (Double) coord1min, (Double) coord1sec, (String) card1,
                (Double) coord2deg, (Double) coord2min, (Double) coord2sec, (String) card2);
    }

    private static void setLatLon(final LatLonHolder latLon,
                                  final double coord1deg, final double coord1min, final double coord1sec, final String card1,
                                  final double coord2deg, final double coord2min, final double coord2sec, final String card2) {

        setLatLon(latLon, coord1deg, coord1min, coord1sec, card1);
        setLatLon(latLon, coord2deg, coord2min, coord2sec, card2);
        if (Double.isNaN(latLon.lat) || Double.isNaN(latLon.lon)) {
            throw new IllegalArgumentException("invalid lat/lon parameters");
        }
    }

    private static void setLatLon(final LatLonHolder latLon, final double coordDeg, final double coordMin, final double coordSec,
                                  final String card) {
        if (coordDeg < -180 || coordDeg > 180 || coordMin < 0 || coordMin >= 60 || coordSec < 0 || coordSec > 60) {
            throw new IllegalArgumentException("out of range");
        }

        double coord = (coordDeg < 0 ? -1 : 1) * (Math.abs(coordDeg) + coordMin / 60 + coordSec / 3600);
        coord = "N".equals(card) || "E".equals(card) ? coord : -coord;
        if ("N".equals(card) || "S".equals(card)) {
            latLon.lat = coord;
        } else {
            latLon.lon = coord;
        }
    }
}
