package mobi.maptrek.util;


import org.junit.Test;
import org.oscim.core.GeoPoint;


import static org.junit.Assert.assertEquals;

/**
 * Josm coordinate parser tests.
 */
public class JosmCoordinatesParserTest {
    @Test
    public void testNumericParser() throws Exception {
        GeoPoint actual = JosmCoordinatesParser.parse("39.095973 -94.573414");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973 -94.573414", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 -94.573414", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testDescriptionParser() throws Exception {
        JosmCoordinatesParser.Result result = JosmCoordinatesParser.parseWithResult("39.095973 -94.573414 Place description");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973 -94.573414 Place description", testLatitude, result.coordinates.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 -94.573414 Place description", testLongitude, result.coordinates.getLongitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 -94.573414 Place description", 20, result.offset);
        result = JosmCoordinatesParser.parseWithResult("39.095973 N, 94.573414 W Place 34 description");
        assertEquals("Failed to parse 39.095973 N, 94.573414 W Place 34 description", testLatitude, result.coordinates.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 N, 94.573414 W Place 34 description", testLongitude, result.coordinates.getLongitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 N, 94.573414 W Place 34 description", 24, result.offset);
        result = JosmCoordinatesParser.parseWithResult("15N 363936 4328605 Place 34");
        assertEquals("Failed to parse 15N 363936 4328605 Place 34", testLatitude, result.coordinates.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15N 363936 4328605 Place 34", testLongitude, result.coordinates.getLongitude(), 0.000005);
        assertEquals("Failed to parse 15N 363936 4328605 Place 34", 18, result.offset);
        result = JosmCoordinatesParser.parseWithResult("39.095973 N, 94.573414 W");
        assertEquals("Failed to parse 39.095973 N, 94.573414 W", testLatitude, result.coordinates.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 N, 94.573414 W", testLongitude, result.coordinates.getLongitude(), 0.000005);
        assertEquals("Failed to parse 39.095973 N, 94.573414 W", 24, result.offset);
    }

    @Test
    public void testDegreesParser() throws Exception {
        GeoPoint actual = JosmCoordinatesParser.parse("39.095973\u00B0 -94.573414\u00B0");
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973\u00B0 -94.573414\u00B0", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("N 39.095973 W 94.573414");
        assertEquals("Failed to parse N 39.095973 W 94.573414", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse N 39.095973 W 94.573414", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("39.095973N, 94.573414W");
        assertEquals("Failed to parse 39.095973N, 94.573414W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39.095973N, 94.573414W", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMinutesParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = JosmCoordinatesParser.parse("39\u00B0 05.7584' -94\u00B0 34.4048'");
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05.7584' -94\u00B0 34.4048'", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("39\u00B0 05.7584 N, 94\u00B0 34.4048 W");
        assertEquals("Failed to parse 39\u00B0 05.7584 N, 94\u00B0 34.4048 W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05.7584 N, 94\u00B0 34.4048 W", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("W 94\u00B034.4048' N 39\u00B005.7584'");
        assertEquals("Failed to parse W 94\u00B034.4048' N 39\u00B005.7584'", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse W 94\u00B034.4048' N 39\u00B005.7584'", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("N 39 05,7584 W 94 34,4048");
        assertEquals("Failed to parse N 39 05,7584 W 94 34,4048", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse N 39 05,7584 W 94 34,4048", testLongitude, actual.getLongitude(), 0.000005);
        testLatitude = 39.083333;
        testLongitude = -94.566666;
        actual = JosmCoordinatesParser.parse("39 05, -94 34");
        assertEquals("Failed to parse 39 05, -94 34", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39 05, -94 34", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("N 39\u00B0 05, W 94\u00B0 34");
        assertEquals("Failed to parse N 39\u00B0 05, W 94\u00B0 34", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse N 39\u00B0 05, W 94\u00B0 34", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testSecondsParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = JosmCoordinatesParser.parse("39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"");
        assertEquals("Failed to parse 39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B0 05' 45.503\" -94\u00B0 34' 24.290\"", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"");
        assertEquals("Failed to parse N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse N39\u00B0 05' 45.503\" W94\u00B0 34' 24.290\"", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("39\u00B005'45.503\"N 94\u00B034'24.290\"W");
        assertEquals("Failed to parse 39°05'45.503\"N 94°34'24.290\"W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39°05'45.503\"N 94°34'24.290\"W", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("39 deg 05' 45.50\" N, 94 deg 34' 24.29\" W");
        assertEquals("Failed to parse 39 deg 05' 45.50\" N, 94 deg 34' 24.29\" W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39 deg 05' 45.50\" N, 94 deg 34' 24.29\" W", testLongitude, actual.getLongitude(), 0.000005);
        testLatitude = 39.095833;
        testLongitude = -94.573333;
        actual = JosmCoordinatesParser.parse("39\u00B005'45\" N 94\u00B034'24\" W");
        assertEquals("Failed to parse 39\u00B005'45\" N 94\u00B034'24\" W", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39\u00B005'45\" N 94\u00B034'24\" W", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("39 5 45, -94 34 24");
        assertEquals("Failed to parse 39 5 45, -94 34 24", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 39 5 45, -94 34 24", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testUtmParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = JosmCoordinatesParser.parse("15N 363936 4328605");
        assertEquals("Failed to parse 15N 363936 4328605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15N 363936 4328605", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("15N3639364328605");
        assertEquals("Failed to parse 15N3639364328605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15N3639364328605", testLongitude, actual.getLongitude(), 0.000005);
    }

    @Test
    public void testMgrsParser() throws Exception {
        double testLatitude = 39.095973;
        double testLongitude = -94.573414;
        GeoPoint actual = JosmCoordinatesParser.parse("15SUD 63936 28605");
        assertEquals("Failed to parse 15SUD 63936 28605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15SUD 63936 28605", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("15SUD6393628605");
        assertEquals("Failed to parse 15SUD6393628605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15SUD6393628605", testLongitude, actual.getLongitude(), 0.000005);
        actual = JosmCoordinatesParser.parse("15S UD 63936 28605");
        assertEquals("Failed to parse 15S UD 63936 28605", testLatitude, actual.getLatitude(), 0.000005);
        assertEquals("Failed to parse 15S UD 63936 28605", testLongitude, actual.getLongitude(), 0.000005);
    }
}
