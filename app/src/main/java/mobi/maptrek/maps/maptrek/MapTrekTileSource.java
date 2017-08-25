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
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
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
    private final SQLiteTileSource mBaseMapTileSource;
    private final SQLiteDatabase mDetailedMapDatabase;
    private String mLocalizedName;
    private OnDataMissingListener mOnDataMissingListener;


    public MapTrekTileSource(SQLiteTileSource baseMapTileSource, SQLiteDatabase detailedMapDatabase, MultiMapFileTileSource multiMapFileTileSource) {
        super(2, 17);
        mBaseMapTileSource = baseMapTileSource;
        mDetailedMapDatabase = detailedMapDatabase;
        mMultiMapFileTileSource = multiMapFileTileSource;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        mMultiMapFileTileSource.setPreferredLanguage(preferredLanguage);
        if (preferredLanguage != null)
            mLocalizedName = "name:" + preferredLanguage;
        else
            mLocalizedName = null;
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        mOnDataMissingListener = onDataMissingListener;
    }

    @Override
    public ITileDataSource getDataSource() {
        ITileDataSource baseDataSource = mBaseMapTileSource.getDataSource();
        MapTrekDataSource detailedDataSource = new MapTrekDataSource(mDetailedMapDatabase);
        ITileDataSource mapFileDataSource = mMultiMapFileTileSource.getDataSource();

        return new NativeDataSource(baseDataSource, detailedDataSource, mapFileDataSource);
    }

    @Override
    public OpenResult open() {
        try {
            mBaseMapTileSource.open();
            mMultiMapFileTileSource.open();
            return TileSource.OpenResult.SUCCESS;
        } catch (SQLiteException e) {
            return new OpenResult(e.getMessage());
        }
    }

    @Override
    public void close() {
        mBaseMapTileSource.close();
        mMultiMapFileTileSource.close();
    }

    private class NativeDataSource implements ITileDataSource {
        private ITileDataSource mBaseDataSource;
        private MapTrekDataSource mDetailedDataSource;
        private ITileDataSource mMapFileDataSource;

        NativeDataSource(ITileDataSource baseDataSource, MapTrekDataSource detailedDataSource, ITileDataSource mapFileDataSource) {
            mBaseDataSource = baseDataSource;
            mDetailedDataSource = detailedDataSource;
            mMapFileDataSource = mapFileDataSource;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            if (tile.zoomLevel < 8) {
                mapDataSink.process(mSea);
                mBaseDataSource.query(tile, new LocalizedTileDataSink(mapDataSink));
                return;
            }
            mapDataSink.process(mLand);

            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            mDetailedDataSource.query(tile, proxyDataSink);

            if (proxyDataSink.result == QueryResult.TILE_NOT_FOUND) {
                if (tile.distance == 0d && mOnDataMissingListener != null) {
                    int tileX = tile.tileX >> (tile.zoomLevel - 7);
                    int tileY = tile.tileY >> (tile.zoomLevel - 7);
                    mOnDataMissingListener.onDataMissing(tileX, tileY, (byte) 7);
                }
            }

            if (proxyDataSink.result != QueryResult.SUCCESS)
                mMapFileDataSource.query(tile, proxyDataSink);

            if (proxyDataSink.result != QueryResult.SUCCESS) {
                mapDataSink.process(mSea);
                int dz = tile.zoomLevel - 7;
                MapTile baseTile = new MapTile(tile.node, tile.tileX >> dz, tile.tileY >> dz, 7);
                TransformTileDataSink transformDataSink = new TransformTileDataSink(baseTile, tile, mapDataSink);
                mBaseDataSource.query(baseTile, new LocalizedTileDataSink(transformDataSink));
                return;
            }
            mapDataSink.completed(proxyDataSink.result);
        }

        @Override
        public void dispose() {
            mDetailedDataSource.dispose();
            mMapFileDataSource.dispose();
        }

        @Override
        public void cancel() {
            mDetailedDataSource.cancel();
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

    @Deprecated
    private class LocalizedTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;

        LocalizedTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
        }

        @Override
        public void process(MapElement element) {
            Tag name = element.tags.get("name");
            if (name != null && mLocalizedName != null && element.tags.containsKey(mLocalizedName)) {
                String localizedName = element.tags.get(mLocalizedName).value;
                if (!"".equals(localizedName))
                    name.value = localizedName;
            }
            mapDataSink.process(element);
        }

        @Override
        public void setTileImage(Bitmap bitmap) {
            // There should be no bitmaps in vector data sources
            // but we will put it here for convenience
            mapDataSink.setTileImage(bitmap);
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
