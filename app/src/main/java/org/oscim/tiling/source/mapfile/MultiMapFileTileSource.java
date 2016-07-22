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
import org.oscim.utils.LRUCache;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;

public class MultiMapFileTileSource extends TileSource {
    @SuppressWarnings("SpellCheckingInspection")
    public static final byte[] FORGEMAP_MAGIC = "mapsforge binary OSM".getBytes();

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
    //private HashMap<Integer, MapFileTileSource> mMapFileTileSources;
    private final DatabaseIndex mMapFileTileSources;

    private final MapIndex mMapIndex;
    private String mPreferredLanguage;

    private OnDataMissingListener onDataMissingListener;

    public MultiMapFileTileSource(MapIndex mapIndex) {
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

    public boolean openFile(int x, int y, MapFile mapFile) {
        synchronized (FORGEMAP_MAGIC) {
            Log.e("MMFTS", "openFile(" + x + "," + y + ")");
            int key = getKey(x, y);
            if (mMapFileTileSources.containsKey(key)) {
                Log.w("MMFTS", "   already opened");
                return true;
            }
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
                if (!openFile(tileX, tileY, mapFile)) {
                    mapDataSink.completed(FAILED);
                    return;
                }
            }

            //Add underlying sea polygon
            mapDataSink.process(mSea);

            ITileDataSource tileDataSource = mTileDataSources.get(key);
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

        public void remove(Integer key) {
            mTileDataSources.remove(key);
        }
    }

    class DatabaseIndex extends LRUCache<Integer, MapFileTileSource> {
        public DatabaseIndex() {
            super(20);
        }

        @Override
        public MapFileTileSource remove(Object key) {
            Log.e("MMFTS", "Close: " + key.toString());
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

    private static int getKey(int x, int y) {
        return (x << 7) + y;
    }
}
