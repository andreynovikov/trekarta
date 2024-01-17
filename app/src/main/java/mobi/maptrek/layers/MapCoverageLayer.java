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

package mobi.maptrek.layers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.format.DateFormat;
import android.text.format.Formatter;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.Box;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.FastMath;

import java.io.IOException;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.maptrek.Index;

public class MapCoverageLayer extends AbstractVectorLayer<MapFile> implements GestureListener, Index.MapStateListener {
    private static final float TILE_SCALE = 1f / (1 << 7);
    private static final long MAP_EXPIRE_PERIOD = 6; // one week
    private static final int FADE_ZOOM = 3;
    private static final double MIN_SCALE = 10d;
    private static final double SYMBOL_MIN_SCALE = 30d;
    public static final double TEXT_MIN_SCALE = 40d;
    private static final double TEXT_MAX_SCALE = 260d;
    private static final float AREA_ALPHA = 0.7f;

    private static final int COLOR_ACTIVE = ColorUtil.modHsv(Color.GREEN, 1.0, 0.7, 0.8, false);
    private static final int COLOR_DOWNLOADING = ColorUtil.modHsv(COLOR_ACTIVE, 1.0, 1.0, 0.7, false);
    private static final int COLOR_OUTDATED = ColorUtil.modHsv(Color.YELLOW, 1.0, 0.6, 0.8, false);
    private static final int COLOR_MISSING = ColorUtil.modHsv(Color.GRAY, 1.0, 1.0, 1.1, false);
    private static final int COLOR_SELECTED = ColorUtil.modHsv(Color.BLUE, 1.0, 0.7, 0.8, false);
    private static final int COLOR_DELETED = ColorUtil.modHsv(Color.RED, 1.0, 0.7, 0.8, false);
    private static final int COLOR_TEXT = Color.get(0, 96, 0);
    private static final int COLOR_TEXT_OUTLINE = Color.get(224, 224, 224);

    private final Index mMapIndex;
    private boolean mAccountHillshades;

    private final AreaStyle mPresentAreaStyle;
    private final AreaStyle mOutdatedAreaStyle;
    private final AreaStyle mMissingAreaStyle;
    private final AreaStyle mDownloadingAreaStyle;
    private final AreaStyle mSelectedAreaStyle;
    private final AreaStyle mDeletedAreaStyle;
    private final LineStyle mLineStyle;
    private final TextStyle mTextStyle;
    private final TextStyle mSmallTextStyle;
    private final Bitmap mHillshadesBitmap;
    private final Bitmap mPresentHillshadesBitmap;
    private final java.text.DateFormat mDateFormat;
    private final Context mContext;

    public MapCoverageLayer(Context context, Map map, Index mapIndex, float scale) {
        super(map);
        mContext = context;
        mMapIndex = mapIndex;
        mPresentAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_ACTIVE).blendScale(10).color(Color.fade(COLOR_ACTIVE, AREA_ALPHA)).build();
        mOutdatedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_OUTDATED).blendScale(10).color(Color.fade(COLOR_OUTDATED, AREA_ALPHA)).build();
        mMissingAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_MISSING).blendScale(10).color(Color.fade(COLOR_MISSING, AREA_ALPHA)).build();
        mDownloadingAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_DOWNLOADING).blendScale(10).color(Color.fade(COLOR_DOWNLOADING, AREA_ALPHA)).build();
        mSelectedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_SELECTED).blendScale(10).color(Color.fade(COLOR_SELECTED, AREA_ALPHA)).build();
        mDeletedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(COLOR_DELETED).blendScale(10).color(Color.fade(COLOR_DELETED, AREA_ALPHA)).build();
        mLineStyle = LineStyle.builder().fadeScale(FADE_ZOOM + 1).color(Color.fade(Color.DKGRAY, 0.6f)).strokeWidth(0.5f * scale).fixed(true).build();
        mTextStyle = TextStyle.builder().fontSize(11 * scale).fontStyle(Paint.FontStyle.BOLD).color(COLOR_TEXT).strokeColor(COLOR_TEXT_OUTLINE).strokeWidth(7f).build();
        mSmallTextStyle = TextStyle.builder().fontSize(8 * scale).fontStyle(Paint.FontStyle.BOLD).color(COLOR_TEXT).strokeColor(COLOR_TEXT_OUTLINE).strokeWidth(5f).build();
        mHillshadesBitmap = getHillshadesBitmap(Color.fade(Color.DKGRAY, 0.8f));
        mPresentHillshadesBitmap = getHillshadesBitmap(Color.WHITE);
        mDateFormat = DateFormat.getDateFormat(context);
        mMapIndex.addMapStateListener(this);
        mAccountHillshades = Configuration.getHillshadesEnabled();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapIndex.removeMapStateListener(this);
        mHillshadesBitmap.recycle();
        mPresentHillshadesBitmap.recycle();
    }

    @Override
    public void onMapEvent(Event e, MapPosition pos) {
        super.onMapEvent(e, pos);
    }

    @Override
    protected void processFeatures(AbstractVectorLayer.Task t, Box b) {
        if (t.position.scale < MIN_SCALE)
            return;

        float scale = (float) (t.position.scale * Tile.SIZE / UNSCALE_COORD);
        float pxScale = (float) (t.position.scale / UNSCALE_COORD);

        int tileXMin = (int) (MercatorProjection.longitudeToX(b.xmin) / TILE_SCALE) - 2;
        int tileXMax = (int) (MercatorProjection.longitudeToX(b.xmax) / TILE_SCALE) + 2;
        int tileYMin = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymax) / TILE_SCALE) - 2, 0, 127);
        int tileYMax = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymin) / TILE_SCALE) + 2, 0, 127);

        if (b.xmin < 0)
            tileXMin--;

        boolean hasSizes = mMapIndex.hasDownloadSizes();
        boolean validSizes = hasSizes && !mMapIndex.expiredDownloadSizes();

        synchronized (this) {
            GeometryBuffer lines = new GeometryBuffer();
            GeometryBuffer missingAreas = new GeometryBuffer();
            GeometryBuffer selectedAreas = new GeometryBuffer();
            GeometryBuffer presentAreas = new GeometryBuffer();
            GeometryBuffer outdatedAreas = new GeometryBuffer();
            GeometryBuffer downloadingAreas = new GeometryBuffer();
            GeometryBuffer deletedAreas = new GeometryBuffer();

            TextBucket text = null;
            if (t.position.scale >= TEXT_MIN_SCALE && t.position.scale <= TEXT_MAX_SCALE)
                text = t.buckets.getTextBucket(7);

            SymbolBucket symbols = new SymbolBucket();

            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    int tileXX = tileX;

                    if (tileX < 0 || tileX >= 128) {
                    /* flip-around date line */
                        if (tileX < 0)
                            tileXX = 128 + tileX;
                        else
                            tileXX = tileX - 128;

                        if (tileXX < 0 || tileXX > 128)
                            continue;
                    }

                    Index.MapStatus mapStatus = mMapIndex.getNativeMap(tileXX, tileY);

                    if (hasSizes && mapStatus.downloadSize == 0L)
                        continue;

                    GeometryBuffer areas = missingAreas;
                    if (mapStatus.downloading != 0L) {
                        areas = downloadingAreas;
                    } else if (mapStatus.action == Index.ACTION.REMOVE) {
                        areas = deletedAreas;
                    } else if (mapStatus.action == Index.ACTION.DOWNLOAD) {
                        areas = selectedAreas;
                    } else if (mapStatus.created > 0) {
                        if (hasSizes && mapStatus.created + MAP_EXPIRE_PERIOD < mapStatus.downloadCreated) {
                            areas = outdatedAreas;
                        } else {
                            areas = presentAreas;
                        }
                    }

                    areas.startPolygon();
                    lines.startLine();

                    float x = (float) (tileX * TILE_SCALE - t.position.x);
                    float y = (float) (tileY * TILE_SCALE - t.position.y);
                    areas.addPoint(x * scale, y * scale);
                    lines.addPoint(x * scale, y * scale);
                    x += TILE_SCALE;
                    areas.addPoint(x * scale, y * scale);
                    lines.addPoint(x * scale, y * scale);
                    y += TILE_SCALE;
                    areas.addPoint(x * scale, y * scale);
                    lines.addPoint(x * scale, y * scale);
                    x -= TILE_SCALE;
                    areas.addPoint(x * scale, y * scale);
                    lines.addPoint(x * scale, y * scale);
                    y -= TILE_SCALE;
                    lines.addPoint(x * scale, y * scale);

                    if (t.position.scale >= SYMBOL_MIN_SCALE && t.position.scale <= TEXT_MAX_SCALE
                            && mHillshadesBitmap != null
                            && mPresentHillshadesBitmap != null) {
                        SymbolItem s = new SymbolItem();
                        if (mapStatus.hillshadeVersion > 0)
                            s.bitmap = mPresentHillshadesBitmap;
                        else if (mAccountHillshades && mapStatus.hillshadeDownloadSize > 0L)
                            s.bitmap = mHillshadesBitmap;
                        if (s.bitmap != null) {
                            s.x = (x + TILE_SCALE) * scale - s.bitmap.getWidth() * pxScale * TILE_SCALE * 2f;
                            s.y = y * scale + s.bitmap.getHeight() * pxScale * TILE_SCALE * 1.7f;
                            s.billboard = false;
                            symbols.addSymbol(s);
                        }
                    }

                    if (text != null) {
                        float tx = (x + TILE_SCALE / 2) * scale;
                        float ty = (y + TILE_SCALE / 2) * scale;
                        TextItem ti;
                        if (BuildConfig.DEBUG) {
                            ti = TextItem.pool.get();
                            ti.set(tx, ty - mTextStyle.fontHeight / 5, tileXX + "-" + tileY, mTextStyle);
                            text.addText(ti);
                        }
                        ti = TextItem.pool.get();
                        if (validSizes) {
                            long size = mapStatus.downloadSize;
                            if (mAccountHillshades)
                                size += mapStatus.hillshadeDownloadSize;
                            ti.set(tx, ty, Formatter.formatShortFileSize(mContext, size), mTextStyle);
                            text.addText(ti);
                            ty += mTextStyle.fontHeight / 5; // why 5?
                        }
                        if (validSizes || mapStatus.created > 0) {
                            int date = mapStatus.created > 0 ? mapStatus.created : mapStatus.downloadCreated;
                            ti = TextItem.pool.get();
                            ti.set(tx, ty, mDateFormat.format(date * 24 * 3600000L), mSmallTextStyle);
                            text.addText(ti);
                        }
                    }
                }
            }

            LineBucket line = t.buckets.getLineBucket(0);
            if (line.line == null)
                line.line = mLineStyle;
            line.addLine(lines);

            MeshBucket missing = t.buckets.getMeshBucket(1);
            if (missing.area == null)
                missing.area = mMissingAreaStyle;
            missing.addMesh(missingAreas);
            line.next = missing;

            MeshBucket selected = t.buckets.getMeshBucket(2);
            if (selected.area == null)
                selected.area = mSelectedAreaStyle;
            selected.addMesh(selectedAreas);
            missing.next = selected;

            MeshBucket present = t.buckets.getMeshBucket(3);
            if (present.area == null)
                present.area = mPresentAreaStyle;
            present.addMesh(presentAreas);
            selected.next = present;

            MeshBucket outdated = t.buckets.getMeshBucket(4);
            if (outdated.area == null)
                outdated.area = mOutdatedAreaStyle;
            outdated.addMesh(outdatedAreas);
            present.next = outdated;

            MeshBucket deleted = t.buckets.getMeshBucket(5);
            if (deleted.area == null)
                deleted.area = mDeletedAreaStyle;
            deleted.addMesh(deletedAreas);
            outdated.next = deleted;

            MeshBucket downloading = t.buckets.getMeshBucket(6);
            if (downloading.area == null)
                downloading.area = mDownloadingAreaStyle;
            downloading.addMesh(downloadingAreas);
            deleted.next = downloading;

            downloading.next = symbols;

            if (text != null)
                symbols.next = text;
        }
    }

    @Override
    public boolean onGesture(Gesture gesture, MotionEvent event) {
        Point point = new Point();
        mMap.viewport().fromScreenPoint(event.getX(), event.getY(), point);
        int tileX = (int) (point.getX() / TILE_SCALE);
        int tileY = (int) (point.getY() / TILE_SCALE);
        if (tileX < 0 || tileX > 127 || tileY < 0 || tileY > 127)
            return false;
        Index.MapStatus mapStatus = mMapIndex.getNativeMap(tileX, tileY);
        if (gesture instanceof Gesture.LongPress) {
            if (mapStatus.downloading != 0L)
                mMapIndex.selectNativeMap(tileX, tileY, Index.ACTION.CANCEL);
            else if (mapStatus.created > 0)
                mMapIndex.selectNativeMap(tileX, tileY, Index.ACTION.REMOVE);
            return true;
        }
        if (gesture instanceof Gesture.Tap || gesture instanceof Gesture.DoubleTap) {
            if (mapStatus.downloading != 0L)
                return true;
            if (mMapIndex.hasDownloadSizes()) {
                if (mapStatus.downloadSize == 0L)
                    return true;
            }
            mMapIndex.selectNativeMap(tileX, tileY, Index.ACTION.DOWNLOAD);
            return true;
        }
        return false;
    }

    @Override
    public void onHasDownloadSizes() {
        update();
    }

    @Override
    public void onBaseMapChanged() {
    }

    @Override
    public void onStatsChanged() {
        update();
    }

    @Override
    public void onHillshadeAccountingChanged(boolean account) {
        mAccountHillshades = account;
        update();
    }

    @Override
    public void onMapSelected(int x, int y, Index.ACTION action, Index.IndexStats stats) {
        update();
    }

    private static Bitmap getHillshadesBitmap(int color) {
        Bitmap bitmap;
        try {
            bitmap = CanvasAdapter.getBitmapAsset("", "symbols/hillshades.svg", 0, 0, 70, 0);
        } catch (IOException e) {
            log.error("Failed to read bitmap", e);
            return null;
        }
        android.graphics.Bitmap bitmapResult = android.graphics.Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmapResult);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), 0, 0, paint);
        return new AndroidBitmap(bitmapResult);
    }
}
