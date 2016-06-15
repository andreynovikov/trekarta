package mobi.maptrek.io.kml;

import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;

public class KmlParser {
    private static final String NS = null;

    @NonNull
    public static FileDataSource parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readKml(parser);
        } finally {
            in.close();
        }
    }

    @NonNull
    private static FileDataSource readKml(XmlPullParser parser) throws XmlPullParserException, IOException {
        FileDataSource dataSource = null;
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_KML);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(KmlFile.TAG_DOCUMENT)) {
                dataSource = readDocument(parser);
            } else {
                skip(parser);
            }
        }
        if (dataSource == null)
            throw new XmlPullParserException("No valid data", parser, null);
        return dataSource;
    }

    @NonNull
    private static FileDataSource readDocument(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_DOCUMENT);
        FileDataSource dataSource = new FileDataSource();
        List<KmlFile.Folder> folders = new ArrayList<>();
        List<KmlFile.Placemark> placemarks = new ArrayList<>();
        HashMap<String, KmlFile.StyleType> styles = new HashMap<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_NAME:
                    dataSource.name = readTextElement(parser, KmlFile.TAG_NAME);
                    break;
                case KmlFile.TAG_STYLE:
                    KmlFile.Style style = readStyle(parser);
                    styles.put("#" + style.id, style);
                    break;
                case KmlFile.TAG_STYLE_MAP:
                    KmlFile.StyleMap styleMap = readStyleMap(parser);
                    styles.put("#" + styleMap.id, styleMap);
                    break;
                case KmlFile.TAG_FOLDER:
                    KmlFile.Folder folder = readFolder(parser);
                    folders.add(folder);
                    break;
                case KmlFile.TAG_PLACEMARK:
                    KmlFile.Placemark placemark = readPlacemark(parser);
                    placemarks.add(placemark);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        // If there is no document name and there is only one folder - take its name
        if (dataSource.name == null && folders.size() == 1)
            dataSource.name = folders.get(0).name;
        // Move all placemarks into DataSource applying styles as well
        for (KmlFile.Placemark placemark : placemarks) {
            applyStyles(placemark, styles);
            if (placemark.point != null)
                dataSource.waypoints.add(placemark.point);
            if (placemark.track != null)
                dataSource.tracks.add(placemark.track);
        }
        // Move everything from folders to the DataSource applying styles as well
        for (KmlFile.Folder folder : folders) {
            for (KmlFile.Placemark placemark : folder.placemarks) {
                applyStyles(placemark, styles);
                if (placemark.point != null)
                    dataSource.waypoints.add(placemark.point);
                if (placemark.track != null)
                    dataSource.tracks.add(placemark.track);
            }
        }
        return dataSource;
    }

    private static void applyStyles(KmlFile.Placemark placemark, Map<String, KmlFile.StyleType> styles) {
        KmlFile.StyleType styleType = placemark.style;
        if (placemark.styleUrl != null)
            styleType = styles.get(placemark.styleUrl);
        if (styleType instanceof KmlFile.StyleMap) {
            // Get normal style, if it is not present just get the first one
            String url = ((KmlFile.StyleMap) styleType).map.get("normal");
            if (url == null)
                url = ((KmlFile.StyleMap) styleType).map.values().iterator().next();
            styleType = styles.get(url);
        }
        if (styleType == null)
            return;
        // Apply selected style
        KmlFile.Style style = (KmlFile.Style) styleType;
        if (style.iconStyle != null && placemark.point != null) {
            placemark.point.style.color = style.iconStyle.color;
        }
        if (style.lineStyle != null && placemark.track != null) {
            placemark.track.style.color = style.lineStyle.color;
            placemark.track.style.width = style.lineStyle.width;
        }
    }

    @NonNull
    private static KmlFile.Folder readFolder(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_FOLDER);
        KmlFile.Folder folder = new KmlFile.Folder();
        folder.placemarks = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_NAME:
                    folder.name = readTextElement(parser, KmlFile.TAG_NAME);
                    break;
                case KmlFile.TAG_PLACEMARK:
                    KmlFile.Placemark placemark = readPlacemark(parser);
                    folder.placemarks.add(placemark);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_FOLDER);
        return folder;
    }

    @NonNull
    private static KmlFile.Placemark readPlacemark(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_PLACEMARK);
        KmlFile.Placemark placemark = new KmlFile.Placemark();
        String title = null, description = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_NAME:
                    title = readTextElement(parser, KmlFile.TAG_NAME);
                    break;
                case KmlFile.TAG_DESCRIPTION:
                    description = readTextElement(parser, KmlFile.TAG_DESCRIPTION);
                    // Default XMLSerializer puts line break after CDATA, so we will remove
                    // trailing spaces here
                    description = description.trim();
                    break;
                case KmlFile.TAG_STYLE:
                    placemark.style = readStyle(parser);
                    break;
                case KmlFile.TAG_STYLE_URL:
                    placemark.styleUrl = readTextElement(parser, KmlFile.TAG_STYLE_URL);
                    break;
                case KmlFile.TAG_POINT:
                    placemark.point = readPoint(parser);
                    break;
                case KmlFile.TAG_LINE_STRING:
                    placemark.track = readLineString(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_PLACEMARK);
        if (placemark.point != null) {
            placemark.point.name = title;
            placemark.point.description = description;
        }
        if (placemark.track != null) {
            placemark.track.name = title;
            placemark.track.description = description;
        }
        return placemark;
    }

    @NonNull
    private static Waypoint readPoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_POINT);
        Waypoint waypoint = new Waypoint();
        String coordinatesString = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_COORDINATES:
                    coordinatesString = readTextElement(parser, KmlFile.TAG_COORDINATES);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_POINT);
        if (coordinatesString == null)
            throw new XmlPullParserException(KmlFile.TAG_POINT + " must have coordinates", parser, null);
        try {
            String[] coordinates = coordinatesString.split(",");
            waypoint.latitude = Double.parseDouble(coordinates[1]);
            waypoint.longitude = Double.parseDouble(coordinates[0]);
            if (coordinates.length == 3) {
                double altitude = Double.parseDouble(coordinates[2]);
                if (altitude != 0d)
                    waypoint.altitude = (int) altitude;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new XmlPullParserException("Wrong coordinates format", parser, e);
        }
        return waypoint;
    }

    @NonNull
    private static Track readLineString(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_LINE_STRING);
        Track track = new Track();
        String coordinatesString = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_COORDINATES:
                    coordinatesString = readTextElement(parser, KmlFile.TAG_COORDINATES);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_LINE_STRING);
        if (coordinatesString == null)
            throw new XmlPullParserException(KmlFile.TAG_LINE_STRING + " must have coordinates", parser, null);
        String[] points = coordinatesString.split("[\\s\\n]");
        boolean continuous = false;
        for (String point : points) {
            String[] coordinates = point.split(",");
            if (coordinates.length < 2)
                continue;
            try {
                int latitudeE6 = (int) (Double.parseDouble(coordinates[1]) * 1E6);
                int longitudeE6 = (int) (Double.parseDouble(coordinates[0]) * 1E6);
                float altitude = 0f;
                if (coordinates.length == 3)
                    altitude = (float) Double.parseDouble(coordinates[2]);
                track.addPointFast(continuous, latitudeE6, longitudeE6, altitude, Float.NaN, Float.NaN, Float.NaN, 0L);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new XmlPullParserException("Wrong coordinates format: " + point, parser, e);
            }
            continuous = true;
        }
        return track;
    }

    @NonNull
    private static KmlFile.Style readStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_STYLE);
        KmlFile.Style style = new KmlFile.Style();
        style.id = parser.getAttributeValue(null, KmlFile.ATTRIBUTE_ID);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_ICON_STYLE:
                    style.iconStyle = readIconStyle(parser);
                    break;
                case KmlFile.TAG_LINE_STYLE:
                    style.lineStyle = readLineStyle(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_STYLE);
        return style;
    }

    @NonNull
    private static KmlFile.IconStyle readIconStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_ICON_STYLE);
        KmlFile.IconStyle style = new KmlFile.IconStyle();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(KmlFile.TAG_COLOR)) {
                style.color = readColor(parser);
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_ICON_STYLE);
        return style;
    }

    @NonNull
    private static KmlFile.LineStyle readLineStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_LINE_STYLE);
        KmlFile.LineStyle style = new KmlFile.LineStyle();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_COLOR:
                    style.color = readColor(parser);
                    break;
                case KmlFile.TAG_WIDTH:
                    style.width = readWidth(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_LINE_STYLE);
        return style;
    }

    @NonNull
    private static KmlFile.StyleMap readStyleMap(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_STYLE_MAP);
        KmlFile.StyleMap styleMap = new KmlFile.StyleMap();
        styleMap.id = parser.getAttributeValue(null, KmlFile.ATTRIBUTE_ID);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_PAIR:
                    Pair<String, String> pair = readPair(parser);
                    styleMap.map.put(pair.first, pair.second);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_STYLE_MAP);
        return styleMap;
    }

    @NonNull
    private static Pair<String, String> readPair(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_PAIR);
        String key = null, url = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case KmlFile.TAG_KEY:
                    key = readTextElement(parser, KmlFile.TAG_KEY);
                    break;
                case KmlFile.TAG_STYLE_URL:
                    url = readTextElement(parser, KmlFile.TAG_STYLE_URL);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_PAIR);
        if (key == null || url == null) {
            throw new XmlPullParserException(KmlFile.TAG_PAIR + " should contain key and url", parser, null);
        }
        return new Pair<>(key, url);
    }

    private static int readColor(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_COLOR);
        String color = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_COLOR);
        return KmlFile.reverseColor((int) Long.parseLong(color, 16));
    }

    private static float readWidth(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, KmlFile.TAG_WIDTH);
        float width = readFloat(parser);
        parser.require(XmlPullParser.END_TAG, NS, KmlFile.TAG_WIDTH);
        return width;
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

    @NonNull
    private static String readTextElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, name);
        String result = readText(parser);
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
