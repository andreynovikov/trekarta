package mobi.maptrek.layers;

import org.oscim.backend.canvas.Color;
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
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;

import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.MapStateListener;

public class MapCoverageLayer extends AbstractVectorLayer<MapFile> implements GestureListener, MapStateListener {
    private static final float TILE_SCALE = 1f / (1 << 7);
    private static final long MAP_EXPIRE_PERIOD = 7 * 24 * 3600 * 1000; // one week
    private static final int MIN_ZOOM = 3;

    private final MapIndex mMapIndex;
    private final AreaStyle mPresentAreaStyle;
    private final AreaStyle mOutdatedAreaStyle;
    private final AreaStyle mMissingAreaStyle;
    private final AreaStyle mDownloadingAreaStyle;
    private final AreaStyle mSelectedAreaStyle;
    private final AreaStyle mDeletedAreaStyle;
    private final LineStyle mLineStyle;

    public MapCoverageLayer(Map map, MapIndex mapIndex) {
        super(map);
        mMapIndex = mapIndex;
        mPresentAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.GREEN).blendScale(10).color(Color.fade(Color.GREEN, 0.4f)).build();
        mOutdatedAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.YELLOW).blendScale(10).color(Color.fade(Color.YELLOW, 0.4f)).build();
        mMissingAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.GRAY).blendScale(10).color(Color.fade(Color.GRAY, 0.4f)).build();
        mDownloadingAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.GREEN & Color.GRAY).blendScale(10).color(Color.fade(Color.GREEN & Color.GRAY, 0.4f)).build();
        mSelectedAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.BLUE).blendScale(10).color(Color.fade(Color.BLUE, 0.4f)).build();
        mDeletedAreaStyle = AreaStyle.builder().fadeScale(MIN_ZOOM).blendColor(Color.RED).blendScale(10).color(Color.fade(Color.RED, 0.4f)).build();
        mLineStyle = LineStyle.builder().fadeScale(MIN_ZOOM + 1).color(Color.fade(Color.DKGRAY, 0.6f)).strokeWidth(2f).fixed(true).build();
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
        if (t.position.getZoomLevel() < MIN_ZOOM)
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

                    if (hasSizes) {
                        MapFile mapFile = mMapIndex.getNativeMap(tileXX, tileY);
                        if (mapFile.downloadSize == 0L)
                            continue;
                    }

                    GeometryBuffer areas = missingAreas;
                    MapFile mapFile = mMapIndex.getNativeMap(tileXX, tileY);
                    if (mapFile.downloading != 0L) {
                        areas = downloadingAreas;
                    } else if (mapFile.action == MapIndex.ACTION.REMOVE) {
                        areas = deletedAreas;
                    } else if (mapFile.action == MapIndex.ACTION.DOWNLOAD) {
                        areas = selectedAreas;
                    } else if (mapFile.downloaded) {
                        long downloadCreated = mapFile.downloadCreated * 24 * 3600000L;
                        if (hasSizes && mapFile.created + MAP_EXPIRE_PERIOD < downloadCreated) {
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
        MapFile mapFile = mMapIndex.getNativeMap(tileX, tileY);
        if (gesture instanceof Gesture.LongPress) {
            if (mapFile.downloading != 0L)
                mMapIndex.selectNativeMap(tileX, tileY, MapIndex.ACTION.CANCEL);
            else if (mapFile.downloaded)
                mMapIndex.selectNativeMap(tileX, tileY, MapIndex.ACTION.REMOVE);
            return true;
        }
        if (gesture instanceof Gesture.Tap || gesture instanceof Gesture.DoubleTap) {
            if (mapFile.downloading != 0L)
                return true;
            if (mMapIndex.hasDownloadSizes()) {
                if (mapFile.downloadSize == 0L)
                    return true;
            }
            mMapIndex.selectNativeMap(tileX, tileY, MapIndex.ACTION.DOWNLOAD);
            return true;
        }
        return false;
    }

    @Override
    public void onHasDownloadSizes() {
        update();
    }

    @Override
    public void onMapSelected(int x, int y, MapIndex.ACTION action) {
        update();
    }
}
