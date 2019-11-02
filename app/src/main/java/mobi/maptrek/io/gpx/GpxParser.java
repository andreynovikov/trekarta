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

import androidx.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;

public class GpxParser {
    private static final String NS = null;

    @NonNull
    public static FileDataSource parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readGpx(parser);
        } finally {
            in.close();
        }
    }

    @NonNull
    private static FileDataSource readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
        FileDataSource dataSource = new FileDataSource();
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_GPX);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_METADATA:
                    GpxFile.Metadata metadata = readMetadata(parser);
                    dataSource.name = metadata.name;
                    break;
                case GpxFile.TAG_WPT:
                    Waypoint waypoint = readWaypoint(parser);
                    dataSource.waypoints.add(waypoint);
                    break;
                case GpxFile.TAG_TRK:
                    Track track = readTrack(parser);
                    dataSource.tracks.add(track);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return dataSource;
    }

    @NonNull
    private static GpxFile.Metadata readMetadata(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_METADATA);
        GpxFile.Metadata metadata = new GpxFile.Metadata();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_NAME:
                    metadata.name = readTextElement(parser, GpxFile.TAG_NAME);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_METADATA);
        return metadata;
    }

    @NonNull
    private static Waypoint readWaypoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_WPT);
        Waypoint waypoint = new Waypoint(Float.valueOf(parser.getAttributeValue(null, GpxFile.ATTRIBUTE_LAT)), Float.valueOf(parser.getAttributeValue(null, GpxFile.ATTRIBUTE_LON)));
        waypoint.locked = true;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_NAME:
                    waypoint.name = readTextElement(parser, GpxFile.TAG_NAME);
                    break;
                case GpxFile.TAG_DESC:
                    waypoint.description = readTextElement(parser, GpxFile.TAG_DESC);
                    // Default XMLSerializer puts line break after CDATA, so we will remove
                    // trailing spaces here
                    waypoint.description = waypoint.description.trim();
                    break;
                case GpxFile.TAG_ELE:
                    waypoint.altitude = (int) readFloatElement(parser, GpxFile.TAG_ELE);
                    break;
                case GpxFile.TAG_TIME:
                    waypoint.date = new Date(readTime(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_WPT);
        return waypoint;
    }

    @NonNull
    private static Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_TRK);
        Track track = new Track();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_NAME:
                    track.name = readTextElement(parser, GpxFile.TAG_NAME);
                    break;
                case GpxFile.TAG_DESC:
                    track.description = readTextElement(parser, GpxFile.TAG_DESC);
                    break;
                case GpxFile.TAG_TRKSEG:
                    readTrackSegment(parser, track);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_TRK);
        return track;
    }

    private static void readTrackSegment(XmlPullParser parser, Track track) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_TRKSEG);
        boolean continuous = false;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_TRKPT:
                    readTrackPoint(parser, track, continuous);
                    continuous = true;
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_TRKSEG);
    }

    private static void readTrackPoint(XmlPullParser parser, Track track, boolean continuous) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_TRKPT);
        float lat = Float.valueOf(parser.getAttributeValue(null, GpxFile.ATTRIBUTE_LAT));
        float lon = Float.valueOf(parser.getAttributeValue(null, GpxFile.ATTRIBUTE_LON));
        float altitude = Float.NaN;
        long time = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case GpxFile.TAG_ELE:
                    altitude = readFloatElement(parser, GpxFile.TAG_ELE);
                    break;
                case GpxFile.TAG_TIME:
                    time = readTime(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_TRKPT);
        track.addPointFast(continuous, (int) (lat * 1E6), (int) (lon * 1E6), altitude, Float.NaN, Float.NaN, Float.NaN, time);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static long readTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, GpxFile.TAG_TIME);
        String timeString = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, GpxFile.TAG_TIME);
        try {
            return GpxFile.parseTime(timeString);
        } catch (ParseException e) {
            throw new XmlPullParserException("Unexpected time format: " + timeString, parser, e);
        }
    }

    @NonNull
    private static String readTextElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, name);
        String result = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, name);
        return result;
    }

    private static float readFloatElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, name);
        float result = readFloat(parser);
        parser.require(XmlPullParser.END_TAG, NS, name);
        return result;
    }

    @NonNull
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static float readFloat(XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        float result;
        try {
            result = Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            throw new XmlPullParserException("Expected float", parser, e);
        }
        return result;
    }
}
