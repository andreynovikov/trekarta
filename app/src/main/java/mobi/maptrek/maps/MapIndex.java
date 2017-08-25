package mobi.maptrek.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.android.cache.TileCache;
import org.oscim.core.BoundingBox;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.online.OnlineTileSource;
import mobi.maptrek.maps.online.TileSourceFactory;
import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;
import mobi.maptrek.util.NativeMapFilenameFilter;

public class MapIndex implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MapIndex.class);

    private static final long serialVersionUID = 1L;
    private static final BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(-85.0511d, -180d, 85.0511d, 180d);

    private final Context mContext;
    private final File mRootDir;
    private HashSet<MapFile> mMaps;
    private HashMap<Integer, MapFile> mNativeMaps;

    @SuppressLint("UseSparseArrays")
    public MapIndex(@NonNull Context context, @Nullable File root) {
        mContext = context;
        mRootDir = root;
        mMaps = new HashSet<>();
        mNativeMaps = new HashMap<>();
        if (mRootDir != null) {
            logger.debug("MapIndex({})", mRootDir.getAbsolutePath());
            List<File> files = FileList.getFileListing(mRootDir, new MapFilenameFilter());
            for (File file : files) {
                loadMap(file);
            }
            files = FileList.getFileListing(mRootDir, new NativeMapFilenameFilter());
            for (File file : files) {
                loadNativeMap(file);
            }
        }
    }

    private void loadMap(@NonNull File file) {
        String fileName = file.getName();
        logger.debug("load({})", fileName);
        byte[] buffer = new byte[13];
        try {
            FileInputStream is = new FileInputStream(file);
            if (is.read(buffer) != buffer.length) {
                throw new IOException("Unknown map file format");
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MapFile mapFile = new MapFile();
        if (Arrays.equals(SQLiteTileSource.MAGIC, buffer)) {
            SQLiteTileSource tileSource = new SQLiteTileSource();
            if (tileSource.setMapFile(file.getAbsolutePath())) {
                TileSource.OpenResult result = tileSource.open();
                if (result.isSuccess()) {
                    SQLiteMapInfo info = tileSource.getMapInfo();
                    mapFile.name = info.name;
                    mapFile.boundingBox = info.boundingBox;
                    mapFile.tileSource = tileSource;
                    tileSource.close();
                }
            }
        }

        if (mapFile.tileSource == null)
            return;

        logger.debug("  added {}", mapFile.boundingBox);
        mMaps.add(mapFile);
    }

    private void loadNativeMap(@NonNull File file) {
        String fileName = file.getName();
        logger.debug("load({})", fileName);
        if (!file.canRead())
            return;
        String[] parts = fileName.split("[\\-\\.]");
        try {
            if (parts.length < 3 || parts.length > 4)
                throw new NumberFormatException("unexpected name");
            int x = Integer.valueOf(parts[0]);
            int y = Integer.valueOf(parts[1]);
            if (x > 127 || y > 127)
                throw new NumberFormatException("out of range");
            if (fileName.endsWith(".map")) {
                MapFile mapFile = new MapFile("7-" + x + "-" + y);
                String filePath = file.getAbsolutePath();
                //TODO Check if tile source exists and close it
                MapFileTileSource tileSource = new MapFileTileSource();
                if (tileSource.setMapFile(filePath)) {
                    TileSource.OpenResult openResult = tileSource.open();
                    if (openResult.isSuccess()) {
                        mapFile.tileSource = tileSource;
                        mNativeMaps.put(Index.getNativeKey(x, y), mapFile);
                        logger.debug("  indexed");
                    } else {
                        logger.warn("Failed to open file: {}", openResult.getErrorMessage());
                        logger.debug("  skipped");
                    }
                    tileSource.close();
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("  skipped: {}", e.getMessage());
        }
    }

    /**
     * Returns native map for a specified square.
     */
    @Nullable
    public MapFile getNativeMap(int key) {
        return mNativeMaps.get(key);
    }

    public void initializeOnlineMapProviders() {
        PackageManager packageManager = mContext.getPackageManager();

        Intent initializationIntent = new Intent("mobi.maptrek.maps.online.provider.action.INITIALIZE");
        // enumerate online map providers
        List<ResolveInfo> providers = packageManager.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo provider : providers) {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            mContext.sendBroadcast(intent);

            List<OnlineTileSource> tileSources = TileSourceFactory.fromPlugin(mContext, packageManager, provider);
            for (OnlineTileSource tileSource : tileSources) {
                MapFile mapFile = new MapFile(tileSource.getName());
                mapFile.tileSource = tileSource;
                mapFile.boundingBox = WORLD_BOUNDING_BOX;
                //TODO Implement tile cache expiration
                //tileProvider.tileExpiration = onlineMapTileExpiration;
                mMaps.add(mapFile);
            }
        }
    }

    @Nullable
    public MapFile getMap(@Nullable String filename) {
        if (filename == null)
            return null;
        for (MapFile map : mMaps) {
            if (filename.equals(map.tileSource.getOption("path")))
                return map;
        }
        return null;
    }

    @NonNull
    public Collection<MapFile> getMaps() {
        return mMaps;
    }

    public void removeMap(MapFile map) {
        mMaps.remove(map);
        map.tileSource.close();
        File file = new File(map.tileSource.getOption("file"));
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public void clear() {
        for (MapFile map : mMaps) {
            map.tileSource.close();
            if (map.tileSource.tileCache != null && map.tileSource.tileCache instanceof TileCache)
                ((TileCache) map.tileSource.tileCache).dispose();
        }
        mMaps.clear();
    }
}
