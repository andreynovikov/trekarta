package mobi.maptrek.util;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;

/**
 * Lexical analyzer for geographical coordinates:<br/>
 * 39.095973 -94.573414<br/>
 * N39.095973 W94.573414<br/>
 * 39° 05.7584' -94° 34.4048'<br/>
 * N39° 05.7584' W94° 34.4048'<br/>
 * 39° 05' 45.503" -94° 34' 24.290"<br/>
 * N39° 05' 45.503" W94° 34' 24.290"<br/>
 * 39°05'45.503"N 94°34'24.290"W<br/>
 * 15N 363936 4328605<br/>
 * 15SUD 63936 28605<br/>
 * 15S UD 63936 28605<br/>
 * 15SUD6393628605
 */
public class CoordinatesParser {
    public enum Type {
        H_PREFIX, H_SUFFIX, DEG, MIN, SEC, UTM_ZONE, UTM_EASTING, UTM_NORTHING, MGRS
    }

    public static class Token {
        public final Type t;
        public final String c;

        public Token(Type t, String c) {
            this.t = t;
            this.c = c;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Token && this.t == ((Token) o).t && this.c.equals(((Token) o).c);
        }

        @Override
        public String toString() {
            if (t == Type.H_PREFIX) {
                return "HPREF<" + c + ">";
            }
            if (t == Type.H_SUFFIX) {
                return "HSUFF<" + c + ">";
            }
            if (t == Type.DEG) {
                return "DEG<" + c + ">";
            }
            if (t == Type.MIN) {
                return "MIN<" + c + ">";
            }
            if (t == Type.SEC) {
                return "SEC<" + c + ">";
            }
            if (t == Type.UTM_ZONE) {
                return "UTM_ZONE<" + c + ">";
            }
            if (t == Type.UTM_EASTING) {
                return "UTM_EASTING<" + c + ">";
            }
            if (t == Type.UTM_NORTHING) {
                return "UTM_NORTHING<" + c + ">";
            }
            if (t == Type.MGRS) {
                return "MGRS<" + c + ">";
            }
            return t.toString();
        }
    }

    public static List<Token> lex(String input) {
        List<Token> result = new ArrayList<>();
        StringBuilder atom = null;
        int i = 0;
        int len = input.length();
        boolean before = true;
        while (i < len) {
            char c = input.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                if (atom == null)
                    atom = new StringBuilder(1).append(c);
                else
                    atom.append(c);
                before = false;
            } else if (atom != null && c >= 'C' && c <= 'X') { // add test for 'I' and 'O'
                // UTM or MGRS grid zone
                atom.append(c);
                lexUtmOrGprs(result, atom, input, len, i);
                atom = null;
                break;
            } else if (c == 'N' || c == 'E' || c == '+') {
                // North or east hemisphere
                result.add(new Token(before ? Type.H_PREFIX : Type.H_SUFFIX, String.valueOf(c)));
            } else if (c == 'S' || c == 'W' || c == '-') {
                // South or west hemisphere
                result.add(new Token(before ? Type.H_PREFIX : Type.H_SUFFIX, String.valueOf(c)));
            } else if (Character.isWhitespace(c)) {
                if (atom != null) {
                    result.add(new Token(Type.DEG, atom.toString()));
                }
                atom = null;
                before = true;
            } else if (c == '\u00B0') {
                if (atom != null) {
                    result.add(new Token(Type.DEG, atom.toString()));
                }
                atom = null;
            } else if (c == '\'') {
                if (atom != null) {
                    result.add(new Token(Type.MIN, atom.toString()));
                }
                atom = null;
            } else if (c == '"') {
                if (atom != null) {
                    result.add(new Token(Type.SEC, atom.toString()));
                }
                atom = null;
            }
            i++;
        }
        if (atom != null) {
            result.add(new Token(Type.DEG, atom.toString()));
        }
        return result;
    }

    private static void lexUtmOrGprs(List<Token> result, StringBuilder atom, String input, int len, int start) {
        int i = start + 1;
        Type type = null;
        int si = 0, ws = 0;
        StringBuilder buffer = new StringBuilder();
        Token zone = new Token(Type.UTM_ZONE, atom.toString());
        Token easting = null;
        while (i < len) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                if (ws == 2 && type != Type.MGRS)
                    throw new RuntimeException("Not a UTM coordinate");
                if (ws == 3)
                    throw new RuntimeException("Not a MGRS coordinate");
                if (type == Type.UTM_EASTING) {
                    easting = new Token(Type.UTM_EASTING, buffer.toString());
                    buffer.setLength(0);
                    type = Type.UTM_NORTHING;
                }
                if (type == null)
                    type = Type.UTM_EASTING;
                ws++;
            } else if ('A' <= c && c <= 'Z') { // add test for 'I' and 'O'
                if (si == 0)
                    type = Type.MGRS;
                if (si == 2)
                    throw new RuntimeException("Not a MGRS coordinate");
                atom.append(c);
                si++;
            } else if (Character.isDigit(c)) {
                atom.append(c);
                buffer.append(c);
            }
            i++;
        }
        if (type == Type.MGRS) {
            result.add(new Token(type, atom.toString()));
        } else {
            result.add(zone);
            result.add(easting);
            result.add(new Token(Type.UTM_NORTHING, buffer.toString()));
        }
    }

    public static GeoPoint parse(String input) {
        List<Token> tokens = lex(input);
        double lat = Double.NaN, lon = Double.NaN, latSign = 1, lonSign = 1;
        int zone = 0;
        String hemisphere = null;
        double easting = Double.NaN;
        double northing = Double.NaN;
        for (Token token : tokens) {
            //System.err.println(token.toString());
            if (token.t == Type.UTM_ZONE) {
                zone = Integer.valueOf(token.c.substring(0, token.c.length() - 1));
                hemisphere = token.c.substring(token.c.length() - 1, token.c.length());
                if ("N".equals(hemisphere))
                    hemisphere = AVKey.NORTH;
                if ("S".equals(hemisphere))
                    hemisphere = AVKey.SOUTH;
            }
            if (token.t == Type.UTM_EASTING) {
                easting = Double.valueOf(token.c);
            }
            if (token.t == Type.UTM_NORTHING) {
                northing = Double.valueOf(token.c);
            }
            if (token.t == Type.MGRS) {
                MGRSCoord coord = MGRSCoord.fromString(token.c);
                return new GeoPoint(coord.getLatitude().degrees, coord.getLongitude().degrees);
            }
            if (token.t == Type.H_PREFIX) {
                if (Double.isNaN(lat))
                    latSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1 : 1;
                else if (Double.isNaN(lon))
                    lonSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1 : 1;
                else
                    throw new RuntimeException("Wrong coordinates format");
            }
            if (token.t == Type.H_SUFFIX) {
                if (!Double.isNaN(lon))
                    lonSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1 : 1;
                else if (!Double.isNaN(lat))
                    latSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1 : 1;
                else
                    throw new RuntimeException("Wrong coordinates format");
            }
            if (token.t == Type.DEG) {
                if (Double.isNaN(lat))
                    lat = Double.valueOf(token.c);
                else
                    lon = Double.valueOf(token.c);
            }
            if (token.t == Type.MIN) {
                if (!Double.isNaN(lon))
                    lon += Math.signum(lon) * Double.valueOf(token.c) / 60;
                else if (!Double.isNaN(lat))
                    lat += Math.signum(lat) * Double.valueOf(token.c) / 60;
                else
                    throw new RuntimeException("Wrong coordinates format");
            }
            if (token.t == Type.SEC) {
                if (!Double.isNaN(lon))
                    lon += Math.signum(lon) * Double.valueOf(token.c) / 3600;
                else if (!Double.isNaN(lat))
                    lat += Math.signum(lat) * Double.valueOf(token.c) / 3600;
                else
                    throw new RuntimeException("Wrong coordinates format");
            }
        }
        if (zone != 0 && hemisphere != null && !Double.isNaN(easting) && !Double.isNaN(northing)) {
            UTMCoord coord = UTMCoord.fromUTM(zone, hemisphere, easting, northing);
            return new GeoPoint(coord.getLatitude().degrees, coord.getLongitude().degrees);
        }
        if (Double.isNaN(lat) || Double.isNaN(lon))
            throw new RuntimeException("Wrong coordinates format");
        return new GeoPoint(lat * latSign, lon * lonSign);
    }
}
