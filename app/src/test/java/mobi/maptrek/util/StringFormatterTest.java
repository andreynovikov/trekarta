package mobi.maptrek.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * String formatter tests.
 */
public class StringFormatterTest {
    @Test
    public void testDistanceFormatter() throws Exception {
        String actual = StringFormatter.distanceH(321d);
        assertEquals("Failed to format distance 321", "321 m", actual);
        actual = StringFormatter.distanceH(2321d);
        assertEquals("Failed to format distance 2321", "2 km", actual);
        actual = StringFormatter.distanceH(2821d);
        assertEquals("Failed to format distance 2821", "3 km", actual);
        actual = StringFormatter.distanceH(1321d, 1000);
        assertEquals("Failed to format distance 1321 with threshold 1000", "1 km", actual);
        actual = StringFormatter.distanceH(1821d, 1000);
        assertEquals("Failed to format distance 1821 with threshold 1000", "2 km", actual);
        actual = StringFormatter.distanceHP(2321d);
        assertEquals("Failed to format precise distance 2321", "2 km 321 m", actual);
        actual = StringFormatter.distanceHP(2000d);
        assertEquals("Failed to format precise distance 2000", "2 km", actual);
        actual = StringFormatter.distanceHP(201d);
        assertEquals("Failed to format precise distance 201", "201 m", actual);

        StringFormatter.distanceFactor = 0.6214;
        StringFormatter.distanceAbbr = "mi";
        StringFormatter.distanceShortFactor = 3.281;
        StringFormatter.distanceShortAbbr = "ft";

        actual = StringFormatter.distanceH(321d);
        assertEquals("Failed to format distance 321 in feet", "1053 ft", actual);
        actual = StringFormatter.distanceH(1321d);
        assertEquals("Failed to format distance 2321 in feet", "1 mi", actual);
        actual = StringFormatter.distanceHP(2721d);
        assertEquals("Failed to format precise distance 2721 in feet", "1 mi 3648 ft", actual);
    }
}
