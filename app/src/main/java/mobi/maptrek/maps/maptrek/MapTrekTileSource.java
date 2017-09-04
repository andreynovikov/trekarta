package mobi.maptrek.maps.maptrek;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

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

import mobi.maptrek.maps.mapsforge.MultiMapFileTileSource;

public class MapTrekTileSource extends TileSource {
    private static final MapElement mSea = new MapElement();
    private static final MapElement mLand = new MapElement();

    private static final int CLIP_BUFFER = 32;

    static {
        mSea.tags.add(new Tag("natural", "sea"));
        mSea.startPolygon();
        mSea.addPoint(-16, -16);
        mSea.addPoint(Tile.SIZE + 16, -16);
        mSea.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mSea.addPoint(-16, Tile.SIZE + 16);
        mLand.tags.add(new Tag("natural", "land"));
        mLand.startPolygon();
        mLand.addPoint(-16, -16);
        mLand.addPoint(Tile.SIZE + 16, -16);
        mLand.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mLand.addPoint(-16, Tile.SIZE + 16);
    }

    private final MultiMapFileTileSource mMultiMapFileTileSource;
    private final SQLiteDatabase mDetailedMapDatabase;
    private OnDataMissingListener mOnDataMissingListener;


    public MapTrekTileSource(SQLiteDatabase detailedMapDatabase, MultiMapFileTileSource multiMapFileTileSource) {
        super(2, 17);
        mDetailedMapDatabase = detailedMapDatabase;
        mMultiMapFileTileSource = multiMapFileTileSource;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        mMultiMapFileTileSource.setPreferredLanguage(preferredLanguage);
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        mOnDataMissingListener = onDataMissingListener;
    }

    @Override
    public ITileDataSource getDataSource() {
        MapTrekDataSource detailedDataSource = new MapTrekDataSource(mDetailedMapDatabase);
        ITileDataSource mapFileDataSource = mMultiMapFileTileSource.getDataSource();

        return new NativeDataSource(detailedDataSource, mapFileDataSource);
    }

    @Override
    public OpenResult open() {
        try {
            mMultiMapFileTileSource.open();
            return TileSource.OpenResult.SUCCESS;
        } catch (SQLiteException e) {
            return new OpenResult(e.getMessage());
        }
    }

    @Override
    public void close() {
        mMultiMapFileTileSource.close();
    }

    private class NativeDataSource implements ITileDataSource {
        private MapTrekDataSource mNativeDataSource;
        private ITileDataSource mMapFileDataSource;

        NativeDataSource(MapTrekDataSource nativeDataSource, ITileDataSource mapFileDataSource) {
            mNativeDataSource = nativeDataSource;
            mMapFileDataSource = mapFileDataSource;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            //mapDataSink.process(mLand);

            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            mNativeDataSource.query(tile, proxyDataSink);

            if (proxyDataSink.result == QueryResult.TILE_NOT_FOUND) {
                if (tile.distance == 0d && mOnDataMissingListener != null) {
                    int tileX = tile.tileX >> (tile.zoomLevel - 7);
                    int tileY = tile.tileY >> (tile.zoomLevel - 7);
                    mOnDataMissingListener.onDataMissing(tileX, tileY, (byte) 7);
                }
            }

            if (proxyDataSink.result == QueryResult.SUCCESS) {
                mapDataSink.process(mLand);
            } else {
                mMapFileDataSource.query(tile, proxyDataSink);
                //mapDataSink.process(mLand);
                //if (proxyDataSink.result == QueryResult.SUCCESS)
                //    mapDataSink.process(mSea);
            }
            if (proxyDataSink.result != QueryResult.SUCCESS) {
                mapDataSink.process(mLand);
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
            mMapFileDataSource.dispose();
        }

        @Override
        public void cancel() {
            mNativeDataSource.cancel();
            mMapFileDataSource.cancel();
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
            scale = (float) Math.pow(2, dz);
            dx = (tile.tileX - (baseTile.tileX << dz)) * Tile.SIZE;
            dy = (tile.tileY - (baseTile.tileY << dz)) * Tile.SIZE;
            mTileClipper = new TileClipper((dx - CLIP_BUFFER) / scale, (dy - CLIP_BUFFER) / scale,
                    (dx + Tile.SIZE + CLIP_BUFFER) / scale, (dy + Tile.SIZE + CLIP_BUFFER) / scale);
        }

        @Override
        public void process(MapElement element) {
            if (!mTileClipper.clip(element))
                return;
            element.scale(scale, scale);
            element.translate(-dx, -dy);
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
