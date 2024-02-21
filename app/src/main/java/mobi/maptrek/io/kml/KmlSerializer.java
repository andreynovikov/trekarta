/*
 * Copyright 2023 Andrey Novikov
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

package mobi.maptrek.io.kml;

import androidx.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.data.Place;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.Style;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.util.ProgressListener;

// TODO Localize strings
public class KmlSerializer {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    public static void serialize(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws IOException {

        int progress = 0;
        if (progressListener != null) {
            int size = source.places.size();
            for (Route route : source.routes)
                size += route.size();
            for (Track track : source.tracks)
                size += track.points.size();
            progressListener.onProgressStarted(size);
        }

        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", null);
        serializer.setPrefix("", KmlFile.NS);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_KML);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_DOCUMENT);

        if (source.tracks.size() > 0 || source.routes.size() > 0) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
            serializer.text("Points");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_OPEN);
            serializer.text("1");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_OPEN);
        }
        for (Place place : source.places) {
            progress = serializePlace(serializer, place, progressListener, progress);
        }
        if (source.tracks.size() > 0 || source.routes.size() > 0) {
            serializer.endTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        }
        for (Route route : source.routes) {
            progress = serializeTrack(serializer, route.toTrack(), progressListener, progress);
        }
        for (Track track : source.tracks) {
            progress = serializeTrack(serializer, track, progressListener, progress);
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_DOCUMENT);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_KML);
        serializer.endDocument();
        serializer.flush();
        writer.close();
        if (progressListener != null)
            progressListener.onProgressFinished();
    }

    private static int serializePlace(XmlSerializer serializer, Place place, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.text(place.name);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
        if (place.description != null && !place.description.trim().isEmpty()) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
            serializer.cdsect(place.description.trim());
            serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        }
        if (!place.style.isDefault()) {
            serializeStyle(serializer, place.style);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.text(String.valueOf(place.coordinates.getLongitude()));
        serializer.text(",");
        serializer.text(String.valueOf(place.coordinates.getLatitude()));
        if (place.altitude != Integer.MIN_VALUE) {
            serializer.text(",");
            serializer.text(String.valueOf(place.altitude));
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);

        progress++;
        if (progressListener != null)
            progressListener.onProgressChanged(progress);
        return progress;
    }

    private static int serializeTrack(XmlSerializer serializer, Track track, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.text(track.name);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
        if (track.description != null) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
            serializer.cdsect(track.description);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        }
        if (track.points.get(0).time > 0L) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_BEGIN);
            serializer.text(sdf.format(new Date(track.points.get(0).time)));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_BEGIN);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_END);
            serializer.text(sdf.format(new Date(track.getLastPoint().time)));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_END);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_OPEN);
        serializer.text("0");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_OPEN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_STYLE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LIST_STYLE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LIST_ITEM_TYPE);
        serializer.text("checkHideChildren");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LIST_ITEM_TYPE);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LIST_STYLE);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_STYLE);

        int part = 1;
        boolean first = true;
        startTrackPart(serializer, part, track.name, track.style);
        for (Track.TrackPoint point : track.points) {
            if (!first) {
                if (!point.continuous) {
                    stopTrackPart(serializer);
                    part++;
                    startTrackPart(serializer, part, track.name, track.style);
                } else {
                    serializer.text(" ");
                }
            }
            serializer.text(String.valueOf(point.longitudeE6 / 1E6));
            serializer.text(",");
            serializer.text(String.valueOf(point.latitudeE6 / 1E6));
            if (!Float.isNaN(point.elevation)) {
                serializer.text(",");
                serializer.text(String.valueOf(point.elevation));
            }
            first = false;
            progress++;
            if (progressListener != null)
                progressListener.onProgressChanged(progress);
        }
        stopTrackPart(serializer);

        serializer.endTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        return progress;
    }

    private static void startTrackPart(XmlSerializer serializer, int part, String name, Style<TrackStyle> style) throws IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        if (part > 1)
            serializer.text(String.format(Locale.US, "%s #%d", name, part));
        else
            serializer.text(name);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
        if (!style.isDefault()) {
            serializeStyle(serializer, style);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LINE_STRING);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_TESSELLATE);
        serializer.text("1");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_TESSELLATE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
    }

    private static void stopTrackPart(XmlSerializer serializer) throws IOException {
        serializer.endTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LINE_STRING);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
    }

    private static void serializeStyle(XmlSerializer serializer, Style<?> style) throws IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_STYLE);
        if (style.id != null && ! "".equals(style.id)) {
            serializer.attribute("", KmlFile.ATTRIBUTE_ID, style.id);
        }
        if (style instanceof MarkerStyle) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_ICON_STYLE);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_COLOR);
            serializer.text(String.format("%08X", KmlFile.reverseColor(((MarkerStyle)style).color)));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_COLOR);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_ICON_STYLE);
        } else if (style instanceof TrackStyle) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_LINE_STYLE);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_COLOR);
            serializer.text(String.format("%08X", KmlFile.reverseColor(((TrackStyle)style).color)));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_COLOR);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_WIDTH);
            serializer.text(String.valueOf(((TrackStyle)style).width));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_WIDTH);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_LINE_STYLE);
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_STYLE);
    }
}