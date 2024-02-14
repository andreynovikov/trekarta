package mobi.maptrek.io.gpx;

import java.text.ParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GpxDateTimeParserTest {
    @Test
    public void testZones() throws ParseException {
        long time = GpxFile.parseTime("2017-09-02T05:35:11Z");
        assertEquals("Failed to parse 2017-09-02T05:35:11Z", 1504330511000L, time);
        time = GpxFile.parseTime("2007-04-02T07:29:23.312Z");
        assertEquals("Failed to parse 2007-04-02T07:29:23.312Z", 1175498963312L, time);
        time = GpxFile.parseTime("2023-09-20T10:09:08+12:00");
        assertEquals("Failed to parse 2023-09-20T10:09:08+12:00", 1695161348000L, time);
        time = GpxFile.parseTime("2020-01-20T08:09:08+03:00");
        assertEquals("Failed to parse 2020-01-20T08:09:08+03:00", 1579496948000L, time);
        time = GpxFile.parseTime("2020-01-20T08:09:08-0300");
        assertEquals("Failed to parse 2020-01-20T08:09:08-0300", 1579518548000L, time);
    }
}
