package mobi.maptrek.maps;

import android.database.sqlite.SQLiteOpenHelper;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

public class WorldMapTileSource extends SQLiteTileSource {
    private static final MapElement mSea = new MapElement();

    static {
        mSea.tags.add(new Tag("natural", "sea"));
        mSea.startPolygon();
        mSea.addPoint(-16, -16);
        mSea.addPoint(Tile.SIZE + 16, -16);
        mSea.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mSea.addPoint(-16, Tile.SIZE + 16);
    }

    public WorldMapTileSource() {
    }

    public WorldMapTileSource(SQLiteOpenHelper openHelper) {
        super(openHelper);
    }

    @Override
    public ITileDataSource getDataSource() {
        return new WorldMapDataSource(super.getDataSource());
    }

    private class WorldMapDataSource implements ITileDataSource {
        private ITileDataSource mDataSource;

        WorldMapDataSource(ITileDataSource dataSource) {
            mDataSource = dataSource;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            // Add underlying sea polygon
            mapDataSink.process(mSea);
            mDataSource.query(tile, mapDataSink);
        }

        @Override
        public void dispose() {
            mDataSource.dispose();
        }

        @Override
        public void cancel() {
            mDataSource.cancel();
        }
    }
}
