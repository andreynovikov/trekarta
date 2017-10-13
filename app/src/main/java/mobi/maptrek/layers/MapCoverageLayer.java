package mobi.maptrek.layers;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.Formatter;

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
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.FastMath;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.maptrek.Index;

public class MapCoverageLayer extends AbstractVectorLayer<MapFile> implements GestureListener, Index.MapStateListener {
    private static final float TILE_SCALE = 1f / (1 << 7);
    private static final long MAP_EXPIRE_PERIOD = 6; // one week
    private static final int FADE_ZOOM = 3;
    private static final double MIN_SCALE = 10d;
    public static final double TEXT_MIN_SCALE = 40d;
    private static final double TEXT_MAX_SCALE = 260d;

    private final Index mMapIndex;
    private final AreaStyle mPresentAreaStyle;
    private final AreaStyle mOutdatedAreaStyle;
    private final AreaStyle mMissingAreaStyle;
    private final AreaStyle mDownloadingAreaStyle;
    private final AreaStyle mSelectedAreaStyle;
    private final AreaStyle mDeletedAreaStyle;
    private final LineStyle mLineStyle;
    private final TextStyle mTextStyle;
    private final java.text.DateFormat mDateFormat;
    private final TextStyle mSmallTextStyle;
    private Context mContext;

    public MapCoverageLayer(Context context, Map map, Index mapIndex, float scale) {
        super(map);
        mContext = context;
        mMapIndex = mapIndex;
        mPresentAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.GREEN).blendScale(10).color(Color.fade(Color.GREEN, 0.4f)).build();
        mOutdatedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.YELLOW).blendScale(10).color(Color.fade(Color.YELLOW, 0.4f)).build();
        mMissingAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.GRAY).blendScale(10).color(Color.fade(Color.GRAY, 0.4f)).build();
        mDownloadingAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.GREEN & Color.GRAY).blendScale(10).color(Color.fade(Color.GREEN & Color.GRAY, 0.4f)).build();
        mSelectedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.BLUE).blendScale(10).color(Color.fade(Color.BLUE, 0.4f)).build();
        mDeletedAreaStyle = AreaStyle.builder().fadeScale(FADE_ZOOM).blendColor(Color.RED).blendScale(10).color(Color.fade(Color.RED, 0.4f)).build();
        mLineStyle = LineStyle.builder().fadeScale(FADE_ZOOM + 1).color(Color.fade(Color.DKGRAY, 0.6f)).strokeWidth(0.5f * scale).fixed(true).build();
        mTextStyle = TextStyle.builder().fontSize(10 * scale).fontStyle(Paint.FontStyle.BOLD).color(Color.get(0, 64, 0)).strokeColor(Color.WHITE).strokeWidth(8f).build();
        mSmallTextStyle = TextStyle.builder().fontSize(8 * scale).fontStyle(Paint.FontStyle.BOLD).color(Color.get(0, 64, 0)).strokeColor(Color.WHITE).strokeWidth(8f).build();
        mDateFormat = DateFormat.getDateFormat(context);
        mMapIndex.addMapStateListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapIndex.removeMapStateListener(this);
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

        int tileXMin = (int) (MercatorProjection.longitudeToX(b.xmin) / TILE_SCALE);
        int tileXMax = (int) (MercatorProjection.longitudeToX(b.xmax) / TILE_SCALE);
        int tileYMin = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymax) / TILE_SCALE), 0, 127);
        int tileYMax = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymin) / TILE_SCALE), 0, 127);

        if (b.xmin < 0)
            tileXMin--;

        boolean hasSizes = mMapIndex.hasDownloadSizes();

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
                text = t.buckets.getTextBucket(-1);

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
                        if (hasSizes) {
                            ti.set(tx, ty, Formatter.formatShortFileSize(mContext, mapStatus.downloadSize), mTextStyle);
                            text.addText(ti);
                            ty += mTextStyle.fontHeight / 5; // why 5?
                        }
                        if (hasSizes || mapStatus.created > 0) {
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
            if (text != null)
                text.next = line;

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

            if (text != null)
                text.setLevel(7);
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
    public void onStatsChanged() {
        update();
    }

    @Override
    public void onMapSelected(int x, int y, Index.ACTION action, Index.IndexStats stats) {
        update();
    }
}
