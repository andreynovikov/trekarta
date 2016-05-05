package org.oscim.tiling.source.mapfile;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OnDataMissingListener;
import org.oscim.tiling.TileSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MultiMapFileTileSource extends TileSource {
    @SuppressWarnings("SpellCheckingInspection")
    public static final byte[] FORGEMAP_MAGIC = "mapsforge binary OSM".getBytes();

    //TODO Should we combine tile sources in one to optimize cache?
    private HashSet<MapFileTileSource> mMapFileTileSources;
    private CombinedMapDatabase mCombinedMapDatabase;
    private String mRootDir;

    private OnDataMissingListener onDataMissingListener;

    public MultiMapFileTileSource(String rootDir) {
        mMapFileTileSources = new HashSet<>();
        mRootDir = rootDir;
    }

    static private List<File> getFileListing(final File rootDir) {
        List<File> result = new ArrayList<>();

        File[] files = rootDir.listFiles();

        if (files == null)
            return result;

        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".map") && file.canRead()) {
                result.add(file);
            }
            if (file.isDirectory()) {
                List<File> deeperList = getFileListing(file);
                result.addAll(deeperList);
            }
        }

        return result;
    }

    @Override
    public ITileDataSource getDataSource() {
        HashSet<ITileDataSource> tileDataSources = new HashSet<>();
        for (MapFileTileSource tileSource : mMapFileTileSources) {
            tileDataSources.add(tileSource.getDataSource());
        }
        mCombinedMapDatabase = new CombinedMapDatabase(tileDataSources);
        return mCombinedMapDatabase;
    }

    @Override
    public OpenResult open() {
        boolean opened = false;

        List<File> files = getFileListing(new File(mRootDir));
        for (File file : files) {
            opened |= openFile(file);
        }
        return opened ? OpenResult.SUCCESS : new OpenResult("No suitable map files");
    }

    @Override
    public void close() {
        for (MapFileTileSource tileSource : mMapFileTileSources) {
            tileSource.close();
        }
        mMapFileTileSources.clear();
    }

    public boolean openFile(File file) {
        byte[] buffer = new byte[20];
        try {
            FileInputStream is = new FileInputStream(file);
            int s = is.read(buffer);
            is.close();
            if (s != buffer.length || !Arrays.equals(FORGEMAP_MAGIC, buffer))
                return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        MapFileTileSource tileSource = new MapFileTileSource();
        if (tileSource.setMapFile(file.getAbsolutePath())) {
            if (tileSource.open().isSuccess()) {
                mMapFileTileSources.add(tileSource);
                if (mCombinedMapDatabase != null)
                    mCombinedMapDatabase.add(tileSource.getDataSource());
                return true;
            } else {
                tileSource.close();
            }
        }
        return false;
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        this.onDataMissingListener = onDataMissingListener;
    }

    class CombinedMapDatabase implements ITileDataSource {
        HashSet<ITileDataSource> mTileDataSources;

        public CombinedMapDatabase(HashSet<ITileDataSource> tileDataSources) {
            mTileDataSources = tileDataSources;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            for (ITileDataSource tileDataSource : mTileDataSources) {
                tileDataSource.query(tile, proxyDataSink);
            }
            if (!proxyDataSink.hasNonSeaElements && onDataMissingListener != null)
                onDataMissingListener.onDataMissing(tile);

            mapDataSink.completed(proxyDataSink.result);
        }

        @Override
        public void dispose() {
            for (ITileDataSource tileDataSource : mTileDataSources)
                tileDataSource.dispose();
        }

        @Override
        public void cancel() {
            for (ITileDataSource tileDataSource : mTileDataSources)
                tileDataSource.cancel();
        }

        public void add(ITileDataSource dataSource) {
            mTileDataSources.add(dataSource);
        }
    }

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
            if (this.result == null || result == ITileDataSink.QueryResult.SUCCESS)
                this.result = result;
        }
    }
}
