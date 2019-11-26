/*
 * Copyright 2019 Andrey Novikov
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

import org.junit.Test;
import org.oscim.core.GeoPoint;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Coordinate parser tests.
 */
public class CoordinatesParserTest {
    @Test
    public void testNumericLexer() {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39.095973 -94.573414");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39.095973", 0, 9),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-", 11, 1),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94.573414", 12, 9)
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testDegreesLexer() {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39.095973\u00B0 -94.573414\u00B0");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39.095973", 0, 10),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-", 12, 1),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94.573414", 13, 10)
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testMinutesLexer() {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39\u00B0 05.7584' -94\u00B0 34.4048'");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39", 0, 3),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "05.7584", 4, 8),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "34.4048", 0, 0)
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testSecondsLexer() {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "05", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.SEC, "45.503", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "34", 0, 0),
                new CoordinatesParser.Token(CoordinatesParser.Type.SEC, "24.290", 0, 0)
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testUtmLexer() {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("15N 363936 4328605");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_ZONE, "15N", 0, 3),
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_EASTING, "363936", 4, 6),
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_NORTHING, "4328605", 11, 6)
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testMgrsLexer() {
        //noinspection ArraysAsListWithZeroOrOneArgument
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.MGRS, "15SUD6393628605", 0, 17)
        );
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("15SUD 63936 28605");
        assertEquals(expected, actual);
        actual = CoordinatesParser.lex("15S UD 63936 28605");
        assertEquals(expected, actual);
        actual = CoordinatesParser.lex("15SUD6393628605");
        assertEquals(expected, actual);
    }

    @Test
    public void testNumericParser() {
        GeoPoint actual = CoordinatesParser.parse("39.095973 -94.573414");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973 -94.573414", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 -94.573414", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testDegreesParser() {
        GeoPoint actual = CoordinatesParser.parse("39.095973\u00B0 -94.573414\u00B0");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMinutesParser() {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("39\u00B0 05.7584' -94\u00B0 34.4048'");
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testSecondsParser() {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"");
        assertEquals("Failed to parse 39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"", testLongitude, actual.getLongitude(), 0.000005);
        actual = CoordinatesParser.parse("N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"");
        assertEquals("Failed to parse N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"", testLongitude, actual.getLongitude(), 0.000005);
        actual = CoordinatesParser.parse("39\u00B005'45.503\"N 94\u00B034'24.290\"W");
        assertEquals("Failed to parse 39°05'45.503\"N 94°34'24.290\"W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39°05'45.503\"N 94°34'24.290\"W", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testUtmParser() {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("15N 363936 4328605");
        assertEquals("Failed to parse 15N 363936 4328605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15N 363936 4328605", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMgrsParser() {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("15SUD 63936 28605");
        assertEquals("Failed to parse 15SUD 63936 28605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15SUD 63936 28605", testLongitude, actual.getLongitude(), 0.000005);
        actual = CoordinatesParser.parse("15SUD6393628605");
        assertEquals("Failed to parse 15SUD6393628605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15SUD6393628605", testLongitude, actual.getLongitude(), 0.000005);
        actual = CoordinatesParser.parse("15S UD 63936 28605");
        assertEquals("Failed to parse 15S UD 63936 28605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15S UD 63936 28605", testLongitude, actual.getLongitude(), 0.000005);
        //TODO Correctly lex this format
        /*
        actual = CoordinatesLexer.parse("15 S UD 63936 28605");
        assertEquals("Failed to parse 15 S UD 63936 28605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15 S UD 63936 28605", testLongitude, actual.getLongitude(), 0.000005);
        */
    }
}
