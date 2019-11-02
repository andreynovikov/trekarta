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

import androidx.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.ProgressListener;

public class GpxSerializer {
    public static void serialize(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws IOException {

        int progress = 0;
        if (progressListener != null) {
            int size = source.waypoints.size();
            for (Track track : source.tracks)
                size += track.points.size();
            progressListener.onProgressStarted(size);
        }

        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", null);
        serializer.setPrefix("", GpxFile.NS);
        serializer.startTag(GpxFile.NS, GpxFile.TAG_GPX);
        serializer.attribute("", GpxFile.ATTRIBUTE_CREATOR, "Trekarta https://trekarta.info");
        serializer.startTag(GpxFile.NS, GpxFile.TAG_METADATA);
        serializer.startTag(GpxFile.NS, GpxFile.TAG_NAME);
        serializer.text(source.name);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_NAME);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_METADATA);

        for (Waypoint waypoint : source.waypoints) {
            progress = serializeWaypoint(serializer, waypoint, progressListener, progress);
        }
        for (Track track : source.tracks) {
            progress = serializeTrack(serializer, track, progressListener, progress);
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_GPX);
        serializer.endDocument();
        serializer.flush();
        writer.close();
        if (progressListener != null)
            progressListener.onProgressFinished();
    }

    private static int serializeWaypoint(XmlSerializer serializer, Waypoint waypoint, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(GpxFile.NS, GpxFile.TAG_WPT);
        serializer.attribute("", GpxFile.ATTRIBUTE_LAT, String.valueOf(waypoint.coordinates.getLatitude()));
        serializer.attribute("", GpxFile.ATTRIBUTE_LON, String.valueOf(waypoint.coordinates.getLongitude()));
        serializer.startTag(GpxFile.NS, GpxFile.TAG_NAME);
        serializer.text(waypoint.name);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_NAME);
        if (waypoint.description != null && !waypoint.description.trim().isEmpty()) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_DESC);
            serializer.cdsect(waypoint.description.trim());
            serializer.endTag(GpxFile.NS, GpxFile.TAG_DESC);
        }
        if (waypoint.altitude != Integer.MIN_VALUE) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_ELE);
            serializer.text(String.valueOf(waypoint.altitude));
            serializer.endTag(GpxFile.NS, GpxFile.TAG_ELE);
        }
        if (waypoint.date != null) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_TIME);
            serializer.text(String.valueOf(GpxFile.formatTime(waypoint.date)));
            serializer.endTag(GpxFile.NS, GpxFile.TAG_TIME);
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_WPT);

        progress++;
        if (progressListener != null)
            progressListener.onProgressChanged(progress);
        return progress;
    }

    private static int serializeTrack(XmlSerializer serializer, Track track, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(GpxFile.NS, GpxFile.TAG_TRK);
        serializer.startTag(GpxFile.NS, GpxFile.TAG_NAME);
        serializer.text(track.name);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_NAME);
        if (track.description != null) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_DESC);
            serializer.cdsect(track.description);
            serializer.endTag(GpxFile.NS, GpxFile.TAG_DESC);
        }
        serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKSEG);

        boolean first = true;
        for (Track.TrackPoint tp : track.points) {
            if (!tp.continuous && !first) {
                serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
                serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
            }
            serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKPT);
            serializer.attribute("", GpxFile.ATTRIBUTE_LAT, String.valueOf(tp.latitudeE6 / 1E6));
            serializer.attribute("", GpxFile.ATTRIBUTE_LON, String.valueOf(tp.longitudeE6 / 1E6));
            if (tp.elevation != Float.NaN) {
                serializer.startTag(GpxFile.NS, GpxFile.TAG_ELE);
                serializer.text(String.valueOf(tp.elevation));
                serializer.endTag(GpxFile.NS, GpxFile.TAG_ELE);
            }
            if (tp.time > 0L) {
                serializer.startTag(GpxFile.NS, GpxFile.TAG_TIME);
                serializer.text(GpxFile.formatTime(new Date(tp.time)));
                serializer.endTag(GpxFile.NS, GpxFile.TAG_TIME);
            }
            serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKPT);
            first = false;
            progress++;
            if (progressListener != null)
                progressListener.onProgressChanged(progress);
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_TRK);

        return progress;
    }

}
