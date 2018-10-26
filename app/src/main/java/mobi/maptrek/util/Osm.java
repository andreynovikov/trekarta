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

package mobi.maptrek.util;

import org.oscim.core.MapPosition;

public class Osm {
    /**
     * Create a short code for the short link.
     *
     * @param lat latitude
     * @param lon longitude
     * @param z zoom
     * @return OSM short link url string
     */
    public static String makeShortLink(double lat, double lon, int z) {
        StringBuilder str = new StringBuilder("https://osm.org/go/");
        long x = (long) ((lon + 180d) * (1L << 32) / 360d);
        long y = (long) ((lat +  90d) * (1L << 32) / 180d);
        long c = interlace(x, y);
        // add eight to the zoom level, which approximates an accuracy of one pixel in a tile
        for (int i = 0; i < Math.ceil((z + 8) / 3d); i++) {
            int digit = (int) ((c >> (58 - 6 * i)) & 0x3f);
            str.append(intToBase64[digit]);
        }
        // append characters onto the end of the string to represent
        // partial zoom levels (characters themselves have a granularity of 3 zoom levels)
        for (int i = 0; i < (z + 8) % 3; i++) {
            str.append('-');
        }
        str.append("?m");
        return str.toString();
    }

    /**
     * Decodes encoded location string of OSM short link, e.g. <code>zVA6Gedo2-</code>
     *
     * @param s encoded location string
     * @return decoded {@link org.oscim.core.MapPosition}
     */
    public static MapPosition decodeShortLink(String s) {
        int i;
        long x = 0;
        long y = 0;
        int z = -8;

        for (i = 0; i < s.length(); i++) {
            int digit = -1;
            char c = s.charAt(i);
            for (int j = 0; j < intToBase64.length; j++)
                if (c == intToBase64[j]) {
                    digit = j;
                    break;
                }
            if (digit < 0)
                break;
            // distribute 6 bits into x and y
            x <<= 3;
            y <<= 3;
            for (int j = 2; j >= 0; j--) {
                x |= ((digit & (1 << (j+j+1))) == 0 ? 0 : (1 << j));
                y |= ((digit & (1 << (j+j))) == 0 ? 0 : (1 << j));
            }
            z += 3;
        }
        double lon = x * Math.pow(2, 2 - 3 * i) * 90d - 180;
        double lat = y * Math.pow(2, 2 - 3 * i) * 45d - 90;
        // adjust z
        if(i < s.length() && s.charAt(i) == '-') {
            z -= 2;
            if(i + 1 < s.length() && s.charAt(i + 1) == '-')
                z++;
        }
        return new MapPosition(lat, lon, 1 << z);
    }

    /**
     * Interlace the bits in x and y, making a Morton code.
     */
    private static long interlace(long x, long y) {
        long c = 0;
        for (byte b = 31; b >= 0; b--) {
            c = (c << 1) | ((x >> b) & 1);
            c = (c << 1) | ((y >> b) & 1);
        }
        return c;
    }

    private static final char intToBase64[] = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
    };
}
