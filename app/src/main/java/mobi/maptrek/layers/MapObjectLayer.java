package mobi.maptrek.layers;

import android.graphics.Bitmap;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.geom.GeometryUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import mobi.maptrek.MapTrek;
import mobi.maptrek.data.MapObject;

public class MapObjectLayer extends Layer {
    public MapObjectLayer(Map map, float scale) {
        super(map);
        mRenderer = new MapObjectRenderer(scale);
        EventBus.getDefault().register(mRenderer);
    }

    @Override
    public void onDetach() {
        EventBus.getDefault().unregister(mRenderer);
    }

    private class MapObjectRenderer extends BucketRenderer {
        private final SymbolBucket mSymbolBucket;
        private final TextBucket mTextBucket;
        private final float[] mBox = new float[8];
        private final float mScale;
        private int mExtents = 100;
        private boolean mUpdate;

        private ArrayList<MapObjectRenderer.InternalItem> mItems = new ArrayList<>();
        private ArrayList<Bitmap> mUsedBitmaps = new ArrayList<>();
        private ArrayList<Bitmap> mOldBitmaps = new ArrayList<>();

        class InternalItem {
            private final Point mMapPoint = new Point();

            MapObject item;
            boolean visible;
            boolean changes;
            float x, y;
            double px, py;
            float dy;

            InternalItem(MapObject item) {
                this.item = item;
                MercatorProjection.project(item.coordinates, mMapPoint);
                px = mMapPoint.x;
                py = mMapPoint.y;
            }

            @Override
            public String toString() {
                return px + ":" + py + " " + x + ":" + y + " / " + dy + " " + visible;
            }
        }

        MapObjectRenderer(float scale) {
            mSymbolBucket = new SymbolBucket();
            mTextBucket = new TextBucket();
            mSymbolBucket.next = mTextBucket;
            mScale = scale;
        }

        public void update() {
            mUpdate = true;
        }

        @Override
        public synchronized void update(GLViewport v) {
            if (!v.changed() && !mUpdate)
                return;

            mUpdate = false;

            double mx = v.pos.x;
            double my = v.pos.y;
            double scale = Tile.SIZE * v.pos.scale;

            //int changesInvisible = 0;
            //int changedVisible = 0;
            int numVisible = 0;

            map().viewport().getMapExtents(mBox, mExtents);

            long flip = (long) (Tile.SIZE * v.pos.scale) >> 1;

            Iterator<MapObject> mapObjects = MapTrek.getMapObjects();
            if (!mapObjects.hasNext()) {
                if (buckets.get() != null) {
                    buckets.clear();
                    compile();
                }
                return;
            }

            mItems.clear();
            while (mapObjects.hasNext())
                mItems.add(new InternalItem(mapObjects.next()));

            double angle = Math.toRadians(v.pos.bearing);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

		    /* check visibility */
            for (InternalItem it : mItems) {
                it.changes = false;
                it.x = (float) ((it.px - mx) * scale);
                it.y = (float) ((it.py - my) * scale);

                if (it.x > flip)
                    it.x -= (flip << 1);
                else if (it.x < -flip)
                    it.x += (flip << 1);

                if (!GeometryUtils.pointInPoly(it.x, it.y, mBox, 8, 0)) {
                    if (it.visible) {
                        it.changes = true;
                        //changesInvisible++;
                    }
                    continue;
                }

                it.dy = sin * it.x + cos * it.y;

                if (!it.visible) {
                    it.visible = true;
                    //changedVisible++;
                }
                numVisible++;
            }

            //log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

            /* only update when zoomlevel changed, new items are visible
             * or more than 10 of the current items became invisible */
            //if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
            //	return;
            buckets.clear();

            if (numVisible == 0) {
                compile();
                return;
            }

            /* keep position for current state */
            mMapPosition.copy(v.pos);
            mMapPosition.bearing = -mMapPosition.bearing;

            mOldBitmaps.addAll(mUsedBitmaps);
            mUsedBitmaps.clear();

            Collections.sort(mItems, zComparator);
            int color = 0;
            TextStyle textStyle = null;
            for (InternalItem it : mItems) {
                if (!it.visible)
                    continue;

                if (it.changes) {
                    it.visible = false;
                    continue;
                }

                Bitmap bitmap = it.item.getBitmapCopy();
                if (bitmap == null)
                    continue;

                SymbolItem s = SymbolItem.pool.get();
                s.set(it.x, it.y, new AndroidBitmap(bitmap), false);
                s.offset = new PointF(0.5f, 0.5f);
                mSymbolBucket.pushSymbol(s);
                mUsedBitmaps.add(bitmap);
                if (textStyle == null || color != it.item.textColor) {
                    color = it.item.textColor;
                    textStyle = TextStyle.builder()
                            .fontSize(10 * mScale)
                            .color(color)
                            .outline(Color.WHITE, 2f)
                            .isCaption(true)
                            .build();
                }
                TextItem t = TextItem.pool.get();
                t.set(it.x, it.y - bitmap.getHeight() / 2, it.item.name, textStyle);
                mTextBucket.addText(t);
            }
            buckets.set(mSymbolBucket);
            buckets.prepare();

            compile();

            for (Bitmap bitmap : mOldBitmaps)
                bitmap.recycle();
            mOldBitmaps.clear();
        }

        final Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
            @Override
            public int compare(InternalItem a, InternalItem b) {
                if (a.visible && b.visible) {
                    if (a.dy > b.dy) {
                        return -1;
                    }
                    if (a.dy < b.dy) {
                        return 1;
                    }
                } else if (a.visible) {
                    return -1;
                } else if (b.visible) {
                    return 1;
                }

                return 0;
            }
        };

        @Subscribe
        public void onMapObjectAdded(MapObject.AddedEvent event) {
            mUpdate = true;
        }

        @Subscribe
        public void onMapObjectRemoved(MapObject.RemovedEvent event) {
            mUpdate = true;
        }

        @Subscribe
        public void onMapObjectUpdated(MapObject.UpdatedEvent event) {
            mUpdate = true;
        }
    }
}
