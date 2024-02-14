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

package mobi.maptrek.io.gpx;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class GpxFile {
    public static final String NS = "http://www.topografix.com/GPX/1/1";
    public static final String TAG_GPX = "gpx";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_NAME = "name";
    public static final String TAG_NUMBER = "number";
    public static final String TAG_DESC = "desc";
    public static final String TAG_ELE = "ele";
    public static final String TAG_TIME = "time";
    public static final String TAG_WPT = "wpt";
    public static final String TAG_TRK = "trk";
    public static final String TAG_TRKSEG = "trkseg";
    public static final String TAG_TRKPT = "trkpt";
    public static final String TAG_RTE = "rte";
    public static final String TAG_RTEPT = "rtept";
    public static final String ATTRIBUTE_LAT = "lat";
    public static final String ATTRIBUTE_LON = "lon";
    public static final String ATTRIBUTE_CREATOR = "creator";

    @SuppressLint("NewApi") // X is supported since API 24
    static final DateFormat[] TRKTIME_FORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT)
    };

    public static long parseTime(String timeString) throws ParseException {
        long time = -1;
        for (DateFormat format : TRKTIME_FORMATS) {
            try {
                Date date = format.parse(timeString);
                if (date == null)
                    continue;
                time = date.getTime();
            } catch (ParseException ignore) {
            }
        }
        if (time < 0)
            throw new ParseException("Unparseable date: \"" + timeString + "\"", 0);
        return time;
    }

    public static String formatTime(Date date) {
        return TRKTIME_FORMATS[0].format(date);
    }

    // http://www.topografix.com/GPX/1/1/#type_metadataType
    static public class Metadata {
        String name;
    }
}
