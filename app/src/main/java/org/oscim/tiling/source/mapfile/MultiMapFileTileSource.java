package org.oscim.tiling.source.mapfile;

import android.util.Log;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OnDataMissingListener;
import org.oscim.tiling.TileSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;

import static org.oscim.tiling.QueryResult.DELAYED;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;

public class MultiMapFileTileSource extends TileSource {
    @SuppressWarnings("SpellCheckingInspection")
    public static final byte[] FORGEMAP_MAGIC = "mapsforge binary OSM".getBytes();

    private final static Tag mWaterTag = new Tag("natural", "sea");

    private HashSet<CombinedMapDatabase> mCombinedMapDatabases;
    private HashMap<Integer, MapFileTileSource> mMapFileTileSources;
    private final MapIndex mMapIndex;
    private String mPreferredLanguage;

    private OnDataMissingListener onDataMissingListener;

    public MultiMapFileTileSource(MapIndex mapIndex) {
        mMapFileTileSources = new HashMap<>();
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
        for (MapFileTileSource tileSource : mMapFileTileSources.values()) {
            tileSource.close();
        }
        mMapFileTileSources.clear();
    }

    public boolean openFile(int x, int y, MapFile mapFile) {
        synchronized (FORGEMAP_MAGIC) {
            Log.w("MMFTS", "openFile(" + x + "," + y + ")");
            byte[] buffer = new byte[20];
            try {
                FileInputStream is = new FileInputStream(mapFile.fileName);
                int s = is.read(buffer);
                is.close();
                if (s != buffer.length || !Arrays.equals(FORGEMAP_MAGIC, buffer))
                    return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            MapFileTileSource tileSource = new MapFileTileSource();
            if (tileSource.setMapFile(mapFile.fileName)) {
                TileSource.OpenResult openResult = tileSource.open();
                if (openResult.isSuccess()) {
                    tileSource.setPreferredLanguage(mPreferredLanguage);
                    mMapFileTileSources.put(getKey(x, y), tileSource);
                    for (CombinedMapDatabase combinedMapDatabase : mCombinedMapDatabases)
                        combinedMapDatabase.add(x, y, tileSource.getDataSource());
                    return true;
                } else {
                    Log.w("MapFile", "Failed to open file: " + openResult.getErrorMessage());
                    tileSource.close();
                }
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

    class CombinedMapDatabase implements ITileDataSource {
        private HashMap<Integer, ITileDataSource> mTileDataSources;

        public CombinedMapDatabase() {
            mTileDataSources = new HashMap<>();
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            int tileX = tile.tileX;
            int tileY = tile.tileY;
            byte zoom = tile.zoomLevel;
            if (zoom > 7) {
                tileX = tileX >> (zoom - 7);
                tileY = tileY >> (zoom - 7);
            }
            int key = getKey(tileX, tileY);
            if (!mTileDataSources.containsKey(key)) {
                MapFile mapFile = mMapIndex.getNativeMap(tileX, tileY);
                if (mapFile == null) {
                    mapDataSink.completed(TILE_NOT_FOUND);
                    if (tile.distance == 0d && onDataMissingListener != null)
                        onDataMissingListener.onDataMissing(tileX, tileY, (byte) 7);
                    return;
                }
                //TODO Run asynchronously?
                openFile(tileX, tileY, mapFile);
                mapDataSink.completed(DELAYED);
                return;
            }
            MapElement sea = new MapElement();
            sea.tags.add(mWaterTag);
            sea.startPolygon();
            sea.addPoint(-16, -16);
            sea.addPoint(Tile.SIZE + 16, -16);
            sea.addPoint(Tile.SIZE + 16, Tile.SIZE + 16);
            sea.addPoint(-16, Tile.SIZE + 16);
            //sea.setLayer(-10);
            mapDataSink.process(sea);

            ITileDataSource tileDataSource = mTileDataSources.get(key);
            //ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            tileDataSource.query(tile, mapDataSink);
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

        public void add(int x, int y, ITileDataSource dataSource) {
            mTileDataSources.put(getKey(x, y), dataSource);
        }
    }

    private static int getKey(int x, int y) {
        return (x << 7) + y;
    }

    /*
    class ProxyTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;
        QueryResult result;
        HashSet<Integer> elements;
        boolean hasNonSeaElements;

        public ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            elements = new HashSet<>();
            hasNonSeaElements = false;
        }

        @Override
        public void process(MapElement element) {
            // Dirty workaround for sea/nosea issue
            // https://groups.google.com/forum/#!topic/mapsforge-dev/x54kHlyKiBM
            if (element.tags.contains("natural", "sea") || element.tags.contains("natural", "nosea")) {
                element.setLayer("nosea".equals(element.tags.getValue("natural")) ? 1 : 0);
            } else {
                hasNonSeaElements = true;
            }
            if (element.isPoly()) {
                int hash = element.hashCode();
                if (elements.contains(hash))
                    return;
                elements.add(hash);
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
            // Do not override successful results
            if (this.result == null || result == QueryResult.SUCCESS)
                this.result = result;
        }
    }
    */
}
