package mobi.maptrek.layers;

import org.oscim.backend.canvas.Color;
import org.oscim.core.Box;
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

import mobi.maptrek.MapSelectionListener;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;

public class MapCoverageLayer extends AbstractVectorLayer<MapFile> implements GestureListener {
    private static final float TILE_SCALE = 1f / (1 << 7);

    private final MapIndex mMapIndex;
    private final AreaStyle mPresentAreaStyle;
    private final AreaStyle mMissingAreaStyle;
    private final AreaStyle mSelectedAreaStyle;
    private final LineStyle mLineStyle;
    private boolean[][] mSelected = new boolean[128][128];
    private MapSelectionListener mListener;

    public MapCoverageLayer(Map map, MapIndex mapIndex) {
        super(map);
        mMapIndex = mapIndex;
        mPresentAreaStyle = AreaStyle.builder().fadeScale(5).blendColor(Color.GREEN).blendScale(9).color(Color.fade(Color.GREEN, 0.4f)).build();
        mMissingAreaStyle = AreaStyle.builder().fadeScale(5).blendColor(Color.GRAY).blendScale(9).color(Color.fade(Color.GRAY, 0.4f)).build();
        mSelectedAreaStyle = AreaStyle.builder().fadeScale(5).blendColor(Color.BLUE).blendScale(9).color(Color.fade(Color.BLUE, 0.4f)).build();
        mLineStyle = LineStyle.builder().fadeScale(5).color(Color.fade(Color.DKGRAY, 0.6f)).strokeWidth(2f).fixed(true).build();
    }

    @Override
    public void onMapEvent(Event e, MapPosition pos) {
        super.onMapEvent(e, pos);
    }

    @Override
    protected void processFeatures(AbstractVectorLayer.Task t, Box b) {
        if (t.position.getZoomLevel() < 5)
            return;

        float scale = (float) (t.position.scale * Tile.SIZE / UNSCALE_COORD);

        int tileXMin = FastMath.clamp((int) (MercatorProjection.longitudeToX(b.xmin) / TILE_SCALE), 0, 127);
        int tileYMin = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymax) / TILE_SCALE), 0, 127);
        int tileXMax = FastMath.clamp((int) (MercatorProjection.longitudeToX(b.xmax) / TILE_SCALE), 0, 127);
        int tileYMax = FastMath.clamp((int) (MercatorProjection.latitudeToY(b.ymin) / TILE_SCALE), 0, 127);

        synchronized (this) {
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    AreaStyle style = mMissingAreaStyle;
                    int level = 1;
                    if (mMapIndex.getNativeMap(tileX, tileY) != null) {
                        style = mPresentAreaStyle;
                        level = 2;
                    }

                    if (mSelected[tileX][tileY]) {
                        style = mSelectedAreaStyle;
                        level = 3;
                    }

                    mGeom.clear();
                    mGeom.startPolygon();

                    float x = (float) (tileX * TILE_SCALE - t.position.x);
                    float y = (float) (tileY * TILE_SCALE - t.position.y);
                    mGeom.addPoint(x * scale, y * scale);
                    x += TILE_SCALE;
                    mGeom.addPoint(x * scale, y * scale);
                    y += TILE_SCALE;
                    mGeom.addPoint(x * scale, y * scale);
                    x -= TILE_SCALE;
                    mGeom.addPoint(x * scale, y * scale);

                    MeshBucket mesh = t.buckets.getMeshBucket(level);
                    if (mesh.area == null)
                        mesh.area = style;

                    LineBucket line = t.buckets.getLineBucket(0);
                    if (line.line == null)
                        line.line = mLineStyle;

                    mesh.addMesh(mGeom);
                    line.addLine(mGeom);
                }
            }
        }
    }

    @Override
    public boolean onGesture(Gesture gesture, MotionEvent event) {
        if (gesture instanceof Gesture.Tap) {
            Point point = new Point();
            mMap.viewport().fromScreenPoint(event.getX(), event.getY(), point);
            MapPosition mapPosition = new MapPosition();
            mMap.viewport().getMapPosition(mapPosition);
            int tileX = (int) (point.getX() / TILE_SCALE);
            int tileY = (int) (point.getY() / TILE_SCALE);
            selectMap(tileX, tileY);
            return true;
        }
        return false;
    }

    public void selectMap(int tileX, int tileY) {
        mSelected[tileX][tileY] = !mSelected[tileX][tileY];
        if (mListener != null)
            mListener.onMapSelected(tileX, tileY);
        update();
    }

    public void setOnMapSelectionListener(MapSelectionListener listener) {
        mListener = listener;
        if (mListener != null)
            mListener.registerMapSelectionState(mSelected);
    }
}
