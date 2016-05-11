package mobi.maptrek.io.kml;

import android.support.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.Style;
import mobi.maptrek.io.Manager;

// TODO Localize strings
public class KmlSerializer {
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    public static void serialize(OutputStream outputStream, FileDataSource source, @Nullable Manager.ProgressListener progressListener) throws IOException {

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
        serializer.setPrefix("", KmlFile.NS);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_KML);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_DOCUMENT);

        if (source.tracks.size() > 0) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
            serializer.text("Points");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
            serializer.startTag(KmlFile.NS, KmlFile.TAG_OPEN);
            serializer.text("1");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_OPEN);
        }
        for (Waypoint waypoint : source.waypoints) {
            progress = serializeWaypoint(serializer, waypoint, progressListener, progress);
        }
        if (source.tracks.size() > 0) {
            serializer.endTag(KmlFile.NS, KmlFile.TAG_FOLDER);
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

    private static int serializeWaypoint(XmlSerializer serializer, Waypoint waypoint, Manager.ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.text(waypoint.name);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        serializer.cdsect(waypoint.description);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        if (!waypoint.style.isDefault()) {
            serializeStyle(serializer, waypoint.style);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.text(String.valueOf(waypoint.latitude));
        serializer.text(",");
        serializer.text(String.valueOf(waypoint.longitude));
        if (waypoint.altitude != Integer.MIN_VALUE) {
            serializer.text(",");
            serializer.text(String.valueOf(waypoint.altitude));
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);

        progress++;
        if (progressListener != null)
            progressListener.onProgressChanged(progress);
        return progress;
    }

    private static int serializeTrack(XmlSerializer serializer, Track track, Manager.ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.text(track.name);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        serializer.cdsect(track.description);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_BEGIN);
        serializer.text(sdf.format(new Date(track.points.get(0).time)));
        serializer.endTag(KmlFile.NS, KmlFile.TAG_BEGIN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_END);
        serializer.text(sdf.format(new Date(track.getLastPoint().time)));
        serializer.endTag(KmlFile.NS, KmlFile.TAG_END);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
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
            serializer.text(String.valueOf(point.latitudeE6 / 1E6));
            serializer.text(",");
            serializer.text(String.valueOf(point.longitudeE6 / 1E6));
            if (point.elevation != Float.NaN) {
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

    private static void startTrackPart(XmlSerializer serializer, int part, String name, Style style) throws IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_NAME);
        serializer.text(String.format(Locale.US, "Part %d - %s", part, name));
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

    private static void serializeStyle(XmlSerializer serializer, Style style) throws IOException {
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
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_STYLE);
    }
}