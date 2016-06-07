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
    public void testNumericLexer() throws Exception {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39.095973 -94.573414");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39.095973"),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-"),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94.573414")
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testDegreesLexer() throws Exception {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39.095973\u00B0 -94.573414\u00B0");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39.095973"),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-"),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94.573414")
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testMinutesLexer() throws Exception {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39\u00B0 05.7584' -94\u00B0 34.4048'");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39"),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "05.7584"),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-"),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94"),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "34.4048")
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testSecondsLexer() throws Exception {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "39"),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "05"),
                new CoordinatesParser.Token(CoordinatesParser.Type.SEC, "45.503"),
                new CoordinatesParser.Token(CoordinatesParser.Type.H_PREFIX, "-"),
                new CoordinatesParser.Token(CoordinatesParser.Type.DEG, "94"),
                new CoordinatesParser.Token(CoordinatesParser.Type.MIN, "34"),
                new CoordinatesParser.Token(CoordinatesParser.Type.SEC, "24.290")
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testUtmLexer() throws Exception {
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("15N 363936 4328605");
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_ZONE, "15N"),
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_EASTING, "363936"),
                new CoordinatesParser.Token(CoordinatesParser.Type.UTM_NORTHING, "4328605")
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testMgrsLexer() throws Exception {
        //noinspection ArraysAsListWithZeroOrOneArgument
        List<CoordinatesParser.Token> expected = Arrays.asList(
                new CoordinatesParser.Token(CoordinatesParser.Type.MGRS, "15SUD6393628605")
        );
        List<CoordinatesParser.Token> actual = CoordinatesParser.lex("15SUD 63936 28605");
        assertEquals(expected, actual);
        actual = CoordinatesParser.lex("15S UD 63936 28605");
        assertEquals(expected, actual);
        actual = CoordinatesParser.lex("15SUD6393628605");
        assertEquals(expected, actual);
    }

    @Test
    public void testNumericParser() throws Exception {
        GeoPoint actual = CoordinatesParser.parse("39.095973 -94.573414");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973 -94.573414", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 -94.573414", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testDegreesParser() throws Exception {
        GeoPoint actual = CoordinatesParser.parse("39.095973\u00B0 -94.573414\u00B0");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMinutesParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("39\u00B0 05.7584' -94\u00B0 34.4048'");
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testSecondsParser() throws Exception {
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
    public void testUtmParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = CoordinatesParser.parse("15N 363936 4328605");
        assertEquals("Failed to parse 15N 363936 4328605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15N 363936 4328605", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMgrsParser() throws Exception {
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
