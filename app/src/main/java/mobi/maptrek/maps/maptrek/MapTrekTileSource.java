package mobi.maptrek.maps.maptrek;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

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
import mobi.maptrek.maps.mapsforge.OnDataMissingListener;

public class MapTrekTileSource extends TileSource {
    private static final MapElement mSea = new MapElement();

    static {
        mSea.tags.add(new Tag("natural", "sea"));
        mSea.startPolygon();
        mSea.addPoint(-16, -16);
        mSea.addPoint(Tile.SIZE + 16, -16);
        mSea.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mSea.addPoint(-16, Tile.SIZE + 16);
    }

    private final MultiMapFileTileSource mMultiMapFileTileSource;
    private SQLiteDatabase mDetailedDatabase;
    private final SQLiteTileSource mBaseMapTileSource;
    private final SQLiteOpenHelper mDetailedMapOpenHelper;
    private String mLocalizedName;

    public MapTrekTileSource(SQLiteTileSource baseMapTileSource, SQLiteOpenHelper detailedMapOpenHelper, MultiMapFileTileSource multiMapFileTileSource) {
        super(2, 17);
        mBaseMapTileSource = baseMapTileSource;
        mDetailedMapOpenHelper = detailedMapOpenHelper;
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
        mMultiMapFileTileSource.setOnDataMissingListener(onDataMissingListener);
    }

    @Override
    public ITileDataSource getDataSource() {
        ITileDataSource baseDataSource = mBaseMapTileSource.getDataSource();
        ITileDataSource detailedDataSource = new MapTrekDatabase(mDetailedDatabase);
        ITileDataSource mapFileDataSource = mMultiMapFileTileSource.getDataSource();

        return new NativeDataSource(baseDataSource, detailedDataSource, mapFileDataSource);
    }

    @Override
    public OpenResult open() {
        try {
            mBaseMapTileSource.open();
            mDetailedDatabase = mDetailedMapOpenHelper.getReadableDatabase();
            mDetailedDatabase.enableWriteAheadLogging();
            mMultiMapFileTileSource.open();
            return TileSource.OpenResult.SUCCESS;
        } catch (SQLiteException e) {
            return new OpenResult(e.getMessage());
        }
    }

    @Override
    public void close() {
        mBaseMapTileSource.close();
        mDetailedDatabase.close();
        mMultiMapFileTileSource.close();
    }

    private class NativeDataSource implements ITileDataSource {
        private ITileDataSource mBaseDataSource;
        private ITileDataSource mDetailedDataSource;
        private ITileDataSource mMapFileDataSource;

        NativeDataSource(ITileDataSource baseDataSource, ITileDataSource detailedDataSource, ITileDataSource mapFileDataSource) {
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
            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            mDetailedDataSource.query(tile, proxyDataSink);

            if (!proxyDataSink.hasElements || proxyDataSink.result != QueryResult.SUCCESS)
                mMapFileDataSource.query(tile, proxyDataSink);

            if (!proxyDataSink.hasElements || proxyDataSink.result != QueryResult.SUCCESS) {
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
            mTileClipper = new TileClipper(dx / scale, dy / scale, (dx + Tile.SIZE) / scale, (dy + Tile.SIZE) / scale);
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
}
