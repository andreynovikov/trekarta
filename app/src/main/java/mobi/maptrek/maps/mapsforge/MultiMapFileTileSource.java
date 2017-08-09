package mobi.maptrek.maps.mapsforge;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.utils.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;

import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.maptrek.Index;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;

public class MultiMapFileTileSource extends TileSource {
    private static final Logger logger = LoggerFactory.getLogger(MultiMapFileTileSource.class);

    private static final byte MAP_FILE_MIN_ZOOM = 8;

    @SuppressWarnings("SpellCheckingInspection")
    private static final byte[] FORGEMAP_MAGIC = "mapsforge binary OSM".getBytes();

    private static final MapElement mSea = new MapElement();

    static {
        mSea.tags.add(new Tag("natural", "sea"));
        mSea.startPolygon();
        mSea.addPoint(-16, -16);
        mSea.addPoint(Tile.SIZE + 16, -16);
        mSea.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
        mSea.addPoint(-16, Tile.SIZE + 16);
    }

    private HashSet<CombinedMapDatabase> mCombinedMapDatabases;
    private final DatabaseIndex mMapFileTileSources;

    private final MapIndex mMapIndex;
    private String mPreferredLanguage;

    private OnDataMissingListener onDataMissingListener;

    public MultiMapFileTileSource(MapIndex mapIndex) {
        super(2, 17);
        mMapFileTileSources = new DatabaseIndex();
        mCombinedMapDatabases = new HashSet<>();
        mMapIndex = mapIndex;
    }

    @Override
    public ITileDataSource getDataSource() {
        CombinedMapDatabase combinedMapDatabase = new CombinedMapDatabase();
        mCombinedMapDatabases.add(combinedMapDatabase);
        return combinedMapDatabase;
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {
        mMapFileTileSources.clear();
    }

    private boolean openFile(int x, int y, MapFile mapFile) {
        synchronized (FORGEMAP_MAGIC) {
            logger.debug("openFile({},{})", x, y);
            int key = Index.getNativeKey(x, y);
            if (mMapFileTileSources.containsKey(key)) {
                logger.debug("   already opened");
                return true;
            }
            MapFileTileSource tileSource = (MapFileTileSource) mapFile.tileSource;
            TileSource.OpenResult openResult = tileSource.open();
            if (openResult.isSuccess()) {
                tileSource.setPreferredLanguage(mPreferredLanguage);
                mMapFileTileSources.put(key, tileSource);
                for (CombinedMapDatabase combinedMapDatabase : mCombinedMapDatabases)
                    combinedMapDatabase.add(key, tileSource.getDataSource());
                return true;
            } else {
                logger.debug("Failed to open file: {}", openResult.getErrorMessage());
                tileSource.close();
            }
            return false;
        }
    }

    public void setPreferredLanguage(String preferredLanguage) {
        mPreferredLanguage = preferredLanguage;
        for (MapFileTileSource tileSource : mMapFileTileSources.values()) {
            tileSource.setPreferredLanguage(mPreferredLanguage);
        }
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        this.onDataMissingListener = onDataMissingListener;
    }

    private class CombinedMapDatabase implements ITileDataSource {
        private HashMap<Integer, ITileDataSource> mTileDataSources;

        CombinedMapDatabase() {
            mTileDataSources = new HashMap<>();
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            if (tile.zoomLevel < MAP_FILE_MIN_ZOOM) {
                mapDataSink.completed(SUCCESS);
                return;
            }

            int tileX = tile.tileX >> (tile.zoomLevel - 7);
            int tileY = tile.tileY >> (tile.zoomLevel - 7);
            int key = Index.getNativeKey(tileX, tileY);
            if (!mTileDataSources.containsKey(key)) {
                MapFile mapFile = mMapIndex.getNativeMap(key);
                if (mapFile == null) {
                    mapDataSink.completed(TILE_NOT_FOUND);
                    /*
                    if (mapFile.downloading == 0L && tile.distance == 0d && onDataMissingListener != null)
                        onDataMissingListener.onDataMissing(tileX, tileY, (byte) 7);
                    */
                    return;
                }
                if (!openFile(tileX, tileY, mapFile)) {
                    mapDataSink.completed(FAILED);
                    return;
                }
            }

            //Add underlying sea polygon
            mapDataSink.process(mSea);

            ITileDataSource tileDataSource = mTileDataSources.get(key);

            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            tileDataSource.query(tile, proxyDataSink);
            mapDataSink.completed(proxyDataSink.result);
        }

        @Override
        public void dispose() {
            for (ITileDataSource tileDataSource : mTileDataSources.values())
                tileDataSource.dispose();
        }

        @Override
        public void cancel() {
            for (ITileDataSource tileDataSource : mTileDataSources.values())
                tileDataSource.cancel();
        }

        public void add(int key, ITileDataSource dataSource) {
            mTileDataSources.put(key, dataSource);
        }

        public void remove(Integer key) {
            mTileDataSources.remove(key);
        }
    }

    private class DatabaseIndex extends LRUCache<Integer, MapFileTileSource> {
        DatabaseIndex() {
            super(20);
        }

        @Override
        public MapFileTileSource remove(Object key) {
            logger.debug("Close: {}", key);
            MapFileTileSource removed = super.remove(key);
            if (removed != null) {
                for (CombinedMapDatabase combinedMapDatabase : mCombinedMapDatabases)
                    combinedMapDatabase.remove((Integer) key);
                removed.close();
            }
            return removed;
        }

        @Override
        public void clear() {
            for (MapFileTileSource mapFileTileSource : values()) {
                mapFileTileSource.close();
            }
            super.clear();
        }
    }

    private class ProxyTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;
        QueryResult result;
        HashSet<Integer> elements;
        boolean hasNonSeaElements;

        ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            elements = new HashSet<>();
            hasNonSeaElements = false;
        }

        @Override
        public void process(MapElement element) {
            //Log.e("MMFTS", element.tags.toString());
            //TODO refactor with filterTags
            //if (element.tags.containsKey("ele"))
            //    element.tags.get("ele").value = element.tags.get("ele").value + " m";
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
            this.result = result;
        }
    }
}
