/*
 * Copyright 2019 Andrey Novikov
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

package mobi.maptrek.maps.maptrek;

import android.database.sqlite.SQLiteDatabase;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.TileSource;
import org.oscim.utils.geom.TileClipper;

import java.util.HashSet;

public class MapTrekTileSource extends TileSource {
    private static final MapElement mLand = new MapElement();

    private static final int CLIP_BUFFER = 32;

    static {
        mLand.tags.add(new Tag("natural", "land"));
        mLand.startPolygon();
        mLand.addPoint(-16, -16);
        mLand.addPoint(Tile.SIZE + 16, -16);
        mLand.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mLand.addPoint(-16, Tile.SIZE + 16);
    }

    private final SQLiteDatabase mNativeMapDatabase;
    private final HashSet<MapTrekDataSource> mMapTrekDataSources;
    private OnDataMissingListener mOnDataMissingListener;
    private boolean mContoursEnabled = true;

    public MapTrekTileSource(SQLiteDatabase nativeMapDatabase) {
        super(2, 17);
        mNativeMapDatabase = nativeMapDatabase;
        mMapTrekDataSources = new HashSet<>();
    }

    public void setContoursEnabled(boolean enabled) {
        mContoursEnabled = enabled;
        for (MapTrekDataSource source : mMapTrekDataSources)
            source.setContoursEnabled(enabled);
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        mOnDataMissingListener = onDataMissingListener;
    }

    @Override
    public ITileDataSource getDataSource() {
        MapTrekDataSource mapTrekDataSource = new MapTrekDataSource(mNativeMapDatabase);
        mapTrekDataSource.setContoursEnabled(mContoursEnabled);
        mMapTrekDataSources.add(mapTrekDataSource);
        return new NativeDataSource(mapTrekDataSource);
    }

    @Override
    public OpenResult open() {
        try {
            return TileSource.OpenResult.SUCCESS;
        } catch (Exception e) {
            return new OpenResult(e.getMessage());
        }
    }

    @Override
    public void close() {
    }

    private class NativeDataSource implements ITileDataSource {
        private MapTrekDataSource mNativeDataSource;

        NativeDataSource(MapTrekDataSource nativeDataSource) {
            mNativeDataSource = nativeDataSource;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            mapDataSink.process(mLand);

            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            mNativeDataSource.query(tile, proxyDataSink);

            if (proxyDataSink.result == QueryResult.TILE_NOT_FOUND) {
                if (tile.distance == 0d && mOnDataMissingListener != null) {
                    int tileX = tile.tileX >> (tile.zoomLevel - 7);
                    int tileY = tile.tileY >> (tile.zoomLevel - 7);
                    mOnDataMissingListener.onDataMissing(tileX, tileY, (byte) 7);
                }
            }

            if (proxyDataSink.result != QueryResult.SUCCESS) {
                MapTile baseTile = tile;
                ITileDataSink dataSink = mapDataSink;
                if (tile.zoomLevel > 7) {
                    int dz = tile.zoomLevel - 7;
                    baseTile = new MapTile(tile.node, tile.tileX >> dz, tile.tileY >> dz, 7);
                    dataSink = new TransformTileDataSink(baseTile, tile, mapDataSink);
                }
                mNativeDataSource.query(baseTile, dataSink);
                return;
            }
            mapDataSink.completed(proxyDataSink.result);
        }

        @Override
        public void dispose() {
            mNativeDataSource.dispose();
        }

        @Override
        public void cancel() {
            mNativeDataSource.cancel();
        }
    }

    private class TransformTileDataSink implements ITileDataSink {
        private final float scale;
        private final float dx;
        private final float dy;
        private TileClipper mTileClipper;
        ITileDataSink mapDataSink;

        TransformTileDataSink(MapTile baseTile, MapTile tile, ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            int dz = tile.zoomLevel - baseTile.zoomLevel;
            scale = 1 << dz;
            dx = (tile.tileX - (baseTile.tileX << dz)) * Tile.SIZE;
            dy = (tile.tileY - (baseTile.tileY << dz)) * Tile.SIZE;
            float buffer = CLIP_BUFFER * CanvasAdapter.getScale();
            mTileClipper = new TileClipper((dx - buffer) / scale, (dy - buffer) / scale,
                    (dx + Tile.SIZE + buffer) / scale, (dy + Tile.SIZE + buffer) / scale);        }

        @Override
        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;
            if (!mTileClipper.clip(element))
                return;
            element.scale(scale, scale);
            element.translate(-dx, -dy);
            if (element.hasLabelPosition && element.labelPosition != null) {
                element.labelPosition.x = element.labelPosition.x * scale - dx;
                element.labelPosition.y = element.labelPosition.y * scale - dy;
                if (element.labelPosition.x < 0 || element.labelPosition.x > Tile.SIZE
                        || element.labelPosition.y < 0 || element.labelPosition.y > Tile.SIZE)
                    element.labelPosition = null;
            }
            mapDataSink.process(element);
        }

        @Override
        public void setTileImage(Bitmap bitmap) {
        }

        @Override
        public void completed(QueryResult result) {
            mapDataSink.completed(result);
        }
    }

    private class ProxyTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;
        QueryResult result;
        boolean hasElements;

        ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            hasElements = false;
        }

        @Override
        public void process(MapElement element) {
            mapDataSink.process(element);
            hasElements = true;
        }

        @Override
        public void setTileImage(Bitmap bitmap) {
            // There should be no bitmaps in vector data sources
            // but we will put it here for convenience
            mapDataSink.setTileImage(bitmap);
        }

        @Override
        public void completed(QueryResult result) {
            this.result = result;
        }
    }

    public interface OnDataMissingListener {
        void onDataMissing(int x, int y, byte zoom);
    }
}
