package org.oscim.tiling.source.mapfile;

import android.util.Log;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
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
    private String mRootDir;

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
        return new CombinedMapDatabase(tileDataSources);
    }

    @Override
    public OpenResult open() {
        boolean opened = false;
        byte[] buffer = new byte[20];

        List<File> files = getFileListing(new File(mRootDir));
        for (File file : files) {

            try {
                FileInputStream is = new FileInputStream(file);
                int s = is.read(buffer);
                is.close();
                if (s != buffer.length || !Arrays.equals(FORGEMAP_MAGIC, buffer))
                    continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            MapFileTileSource tileSource = new MapFileTileSource();
            if (tileSource.setMapFile(file.getAbsolutePath())) {
                if (tileSource.open().isSuccess()) {
                    mMapFileTileSources.add(tileSource);
                    opened = true;
                } else {
                    tileSource.close();
                }
            }
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

    class CombinedMapDatabase implements ITileDataSource {
        HashSet<ITileDataSource> mTileDataSources;

        public CombinedMapDatabase(HashSet<ITileDataSource> tileDataSources) {
            mTileDataSources = tileDataSources;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            //Log.e("MMFTS", tile.x + " " + tile.y + " " + tile.zoomLevel);
            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            for (ITileDataSource tileDataSource : mTileDataSources) {
                //proxyDataSink.newDataSource();
                tileDataSource.query(tile, proxyDataSink);
                //proxyDataSink.processDelayed();
            }
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
    }

    class ProxyTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;
        QueryResult result;
        float[] points;
        String type;
        HashSet<Integer> elements;

        public ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            elements = new HashSet<>();
        }

        public void newDataSource() {
            points = null;
            type = null;
        }

        public void processDelayed() {
            if (type == null || points == null)
                return;
            MapElement delayed = new MapElement(4, 1);
            delayed.startPolygon();
            delayed.addPoint(points[0], points[1]);
            delayed.addPoint(points[2], points[3]);
            delayed.addPoint(points[4], points[5]);
            delayed.addPoint(points[6], points[7]);
            delayed.tags.add(new Tag("natural", type));
            delayed.tags.add(new Tag("mapsforge", "yes"));
            delayed.setLayer("land".equals(type) ? 1 : 0);
            //Log.w("MMFTS", delayed.toString());
            mapDataSink.process(delayed);
            type = null;
        }

        @Override
        public void process(MapElement element) {
            // Dirty workaround for sea/nosea issue
            // https://groups.google.com/forum/#!topic/mapsforge-dev/x54kHlyKiBM
            if (element.tags.contains("natural", "sea") || element.tags.contains("natural", "nosea")) {
                /*
                if (element.tags.contains("natural", "sea")) {
                    element.tags.clear();
                    element.tags.add(new Tag("natural", "water"));
                    element.tags.add(new Tag("mapsforge", "sea"));
                }
                if (element.tags.contains("natural", "nosea")) {
                    element.tags.clear();
                    element.tags.add(new Tag("natural", "land"));
                    element.tags.add(new Tag("mapsforge", "nosea"));
                }
                */
                // We need to drop empty tiles covered only with sea/nosea
                /*
                if (element.getNumPoints() == 4) {
                    if (type == null) {
                        type = element.tags.getValue("natural");
                        points = Arrays.copyOf(element.points, 8);
                        //Log.w("MMFTS", element.toString());
                        //Log.w("MMFTS", "delay");
                        return;
                    } else {
                        float[] other = Arrays.copyOf(element.points, 8);
                        if (Arrays.equals(points, other)) {
                            //Log.w("MMFTS", element.toString());
                            //Log.w("MMFTS", "skip");
                            type = null;
                            return;
                        } else {
                            processDelayed();
                        }
                    }
                }
                */
                element.setLayer("nosea".equals(element.tags.getValue("natural")) ? 1 : 0);
                //Log.w("MMFTS", element.toString());
                //Log.w("MMFTS", "process");
            }
            if (element.isPoly()) {
                //Log.w("MMFTS", element.toString());
                int hash = element.hashCode();
                //Log.w("MMFTS", "h: " + hash);
                if (elements.contains(hash))
                    return;
                elements.add(hash);
                //Log.w("MMFTS", "process");
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
