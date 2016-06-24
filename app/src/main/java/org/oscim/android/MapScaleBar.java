package org.oscim.android;

/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.BitmapRenderer;

import java.util.HashMap;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the
 * corresponding distance on the ground.
 */
public class MapScaleBar extends Layer implements UpdateListener {

    private static final int BITMAP_HEIGHT = 16;
    private static final int BITMAP_WIDTH = 64;
    private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;

    private static final double METER_FOOT_RATIO = 0.3048;
    private static final int ONE_KILOMETER = 1000;
    private static final int ONE_MILE = 5280;

    private static final Paint SCALE_BAR = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint SCALE_BAR_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint SCALE_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint SCALE_TEXT_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] SCALE_BAR_VALUES_IMPERIAL = {
            26400000, 10560000, 5280000,
            2640000, 1056000, 528000,
            264000, 105600, 52800, 26400,
            10560, 5280, 2000, 1000, 500,
            200, 100, 50, 20,
            10, 5, 2, 1};
    private static final int[] SCALE_BAR_VALUES_METRIC = {
            10000000, 5000000, 2000000, 1000000,
            500000, 200000, 100000, 50000,
            20000, 10000, 5000, 2000, 1000,
            500, 200, 100, 50, 20, 10, 5, 2, 1};

    private boolean mImperialUnits;
    private final Canvas mMapScaleCanvas;
    private boolean mRedrawNeeded;
    private double mPrevLatitude = -1;
    private double mPrevScale = -1;
    private final HashMap<TextField, String> mTextFields;
    private int mWidth;
    private int mHeight;
    private float mDensity;

    private final Bitmap mBitmap;
    // passed to BitmapRenderer - need to sync on this object.
    private final AndroidBitmap mLayerBitmap;
    private final BitmapRenderer mBitmapLayer;

    public MapScaleBar(MapView map) {
        super(map.map());

        mDensity = map.getContext().getResources().getDisplayMetrics().density;
        mWidth = (int) (BITMAP_WIDTH * mDensity);
        mHeight = (int) (BITMAP_HEIGHT * mDensity);

        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);

        mMapScaleCanvas = new Canvas(mBitmap);
        mTextFields = new HashMap<>();

        setDefaultTexts();
        configurePaints();

        mRedrawNeeded = true;
        mRenderer = mBitmapLayer = new BitmapRenderer();
        mLayerBitmap = new AndroidBitmap(mBitmap);
        mBitmapLayer.setBitmap(mLayerBitmap, mWidth, mHeight);
        mBitmapLayer.setDrawOffset(false, 0, 0);
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.UPDATE_EVENT)
            return;

        double latitude = MercatorProjection.toLatitude(mapPosition.y);

        if (!mRedrawNeeded) {
            double scaleDiff = mPrevScale / mapPosition.scale;
            if (scaleDiff < 1.1 && scaleDiff > 0.9) {
                double latitudeDiff = Math.abs(mPrevLatitude - latitude);
                if (latitudeDiff < LATITUDE_REDRAW_THRESHOLD)
                    return;
            }
        }
        mPrevScale = mapPosition.scale;
        mPrevLatitude = latitude;

        double groundResolution = MercatorProjection.groundResolution(latitude, mapPosition.scale);

        int[] scaleBarValues;
        if (mImperialUnits) {
            groundResolution = groundResolution / METER_FOOT_RATIO;
            scaleBarValues = SCALE_BAR_VALUES_IMPERIAL;
        } else {
            scaleBarValues = SCALE_BAR_VALUES_METRIC;
        }

        float scaleBarLength = 0;
        int mapScaleValue = 0;

        for (int i = 0; i < scaleBarValues.length; ++i) {
            mapScaleValue = scaleBarValues[i];
            scaleBarLength = mapScaleValue / (float) groundResolution;
            if (scaleBarLength < (mWidth - 10)) {
                break;
            }
        }
        synchronized (mLayerBitmap) {
            redrawMapScaleBitmap(scaleBarLength, mapScaleValue);
        }

        mBitmapLayer.updateBitmap();

        mRedrawNeeded = false;
    }

    /**
     * @return true if imperial units are used, false otherwise.
     */
    public boolean isImperialUnits() {
        return mImperialUnits;
    }

    /**
     * @param imperialUnits true if imperial units should be used rather than metric
     *                      units.
     */
    public void setImperialUnits(boolean imperialUnits) {
        mImperialUnits = imperialUnits;
        mRedrawNeeded = true;
    }

    /**
     * Overrides the specified text field with the given string.
     *
     * @param textField the text field to override.
     * @param value     the new value of the text field.
     */
    public void setText(TextField textField, String value) {
        mTextFields.put(textField, value);
        mRedrawNeeded = true;
    }

    private void drawScaleBar(float scaleBarLength, Paint paint) {
        mMapScaleCanvas.drawLine(7, 12 * mDensity, scaleBarLength + 3, 12 * mDensity, paint);
        mMapScaleCanvas.drawLine(5, 8 * mDensity, 5, 16 * mDensity, paint);
        mMapScaleCanvas.drawLine(scaleBarLength + 5, 8 * mDensity, scaleBarLength + 5, 16 * mDensity, paint);
    }

    private void drawScaleText(int scaleValue, String unitSymbol, Paint paint) {
        mMapScaleCanvas.drawText(scaleValue + unitSymbol, 12, 10 * mDensity, paint);
    }

    /**
     * Redraws the map scale bitmap with the given parameters.
     *
     * @param scaleBarLength the length of the map scale bar in pixels.
     * @param mapScaleValue  the map scale value in meters.
     */
    private void redrawMapScaleBitmap(float scaleBarLength, int mapScaleValue) {
        mBitmap.eraseColor(Color.TRANSPARENT);

        // draw the scale bar
        drawScaleBar(scaleBarLength, SCALE_BAR_STROKE);
        drawScaleBar(scaleBarLength, SCALE_BAR);

        int scaleValue;
        String unitSymbol;
        if (mImperialUnits) {
            if (mapScaleValue < ONE_MILE) {
                scaleValue = mapScaleValue;
                unitSymbol = mTextFields.get(TextField.FOOT);
            } else {
                scaleValue = mapScaleValue / ONE_MILE;
                unitSymbol = mTextFields.get(TextField.MILE);
            }
        } else {
            if (mapScaleValue < ONE_KILOMETER) {
                scaleValue = mapScaleValue;
                unitSymbol = mTextFields.get(TextField.METER);
            } else {
                scaleValue = mapScaleValue / ONE_KILOMETER;
                unitSymbol = mTextFields.get(TextField.KILOMETER);
            }
        }

        // draw the scale text
        drawScaleText(scaleValue, unitSymbol, SCALE_TEXT_STROKE);
        drawScaleText(scaleValue, unitSymbol, SCALE_TEXT);
    }

    private void setDefaultTexts() {
        mTextFields.put(TextField.FOOT, " ft");
        mTextFields.put(TextField.MILE, " mi");

        mTextFields.put(TextField.METER, " m");
        mTextFields.put(TextField.KILOMETER, " km");
    }

    private void configurePaints() {
        SCALE_BAR.setStrokeWidth(mDensity);
        SCALE_BAR.setStrokeCap(Paint.Cap.SQUARE);
        SCALE_BAR.setColor(Color.BLACK);
        SCALE_BAR_STROKE.setStrokeWidth(2 * mDensity);
        SCALE_BAR_STROKE.setStrokeCap(Paint.Cap.SQUARE);
        SCALE_BAR_STROKE.setColor(Color.WHITE);

        SCALE_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        SCALE_TEXT.setTextSize(9 * mDensity);
        SCALE_TEXT_STROKE.setStrokeWidth(0.8f * mDensity);
        SCALE_TEXT.setColor(Color.BLACK);
        SCALE_TEXT_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        SCALE_TEXT_STROKE.setStyle(Paint.Style.STROKE);
        SCALE_TEXT_STROKE.setColor(Color.WHITE);
        SCALE_TEXT_STROKE.setStrokeWidth(1.2f * mDensity);
        SCALE_TEXT_STROKE.setTextSize(9 * mDensity);
    }

    /**
     * Enumeration of all text fields.
     */
    public enum TextField {
        /**
         * Unit symbol for one foot.
         */
        FOOT,

        /**
         * Unit symbol for one kilometer.
         */
        KILOMETER,

        /**
         * Unit symbol for one meter.
         */
        METER,

        /**
         * Unit symbol for one mile.
         */
        MILE
    }
}
