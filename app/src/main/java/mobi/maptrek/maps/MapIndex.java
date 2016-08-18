package mobi.maptrek.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.oscim.android.cache.TileCache;
import org.oscim.core.BoundingBox;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mobi.maptrek.maps.online.OnlineTileSource;
import mobi.maptrek.maps.online.TileSourceFactory;
import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;

public class MapIndex implements Serializable {
    private static final String TAG = "MapIndex";

    private static final long serialVersionUID = 1L;
    private static final BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(-85.0511d, -180d, 85.0511d, 180d);

    public enum ACTION {NONE, DOWNLOAD, REMOVE}

    private File mRootDir;
    private HashSet<MapFile> mMaps;
    private MapFile[][] mNativeMaps = new MapFile[128][128];
    private boolean mHasDownloadSizes;

    private final Set<WeakReference<MapStateListener>> mMapStateListeners = new HashSet<>();

    @SuppressWarnings("unused")
    MapIndex() {
    }

    public MapIndex(@Nullable File root) {
        mRootDir = root;
        mMaps = new HashSet<>();
        if (root != null) {
            List<File> files = FileList.getFileListing(mRootDir, new MapFilenameFilter());
            for (File file : files) {
                load(file);
            }
        }
    }

    private void load(@NonNull File file) {
        String fileName = file.getName();
        Log.e(TAG, "load(" + fileName + ")");
        if (fileName.endsWith(".map") && file.canRead()) {
            String[] parts = fileName.split("[\\-\\.]");
            try {
                if (parts.length != 3)
                    throw new NumberFormatException("unexpected name");
                int x = Integer.valueOf(parts[0]);
                int y = Integer.valueOf(parts[1]);
                if (x > 127 || y > 127)
                    throw new NumberFormatException("out of range");
                MapFile mapFile = getNativeMap(x, y);
                mapFile.fileName = file.getAbsolutePath();
                setNativeMapTileSource(mapFile);
                Log.w(TAG, "  indexed");
            } catch (NumberFormatException e) {
                Log.w(TAG, "  skipped: " + e.getMessage());
            }
            return;
        }
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
                    mapFile.fileName = file.getAbsolutePath();
                    tileSource.close();
                }
            }
        }

        /*
        byte[] buffer13 = Arrays.copyOf(buffer, MapFile.SQLITE_MAGIC.length);
        if (Arrays.equals(MapFile.SQLITE_MAGIC, buffer13)) {
            if (file.getName().endsWith(".mbtiles"))
                return new MBTilesMap(file.getCanonicalPath());
            else
                return new SQLiteMap(file.getCanonicalPath());
        }
        */

        if (mapFile.tileSource == null)
            return;

        Log.w(TAG, "  added " + mapFile.boundingBox.toString());
        mMaps.add(mapFile);
    }

    public void initializeOnlineMapProviders(Context context)
    {
        PackageManager packageManager = context.getPackageManager();

        Intent initializationIntent = new Intent("mobi.maptrek.maps.online.provider.action.INITIALIZE");
        // enumerate online map providers
        List<ResolveInfo> providers = packageManager.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo provider : providers)
        {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            context.sendBroadcast(intent);

            List<OnlineTileSource> tileSources = TileSourceFactory.fromPlugin(context, packageManager, provider);
            for (OnlineTileSource tileSource : tileSources)
            {
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
            if (filename.equals(map.fileName))
                return map;
        }
        return null;
    }

    @NonNull
    public Collection<MapFile> getMaps() {
        return mMaps;
    }

    @SuppressWarnings("unused")
    public void removeMap(MapFile map) {
        mMaps.remove(map);
        map.tileSource.close();
    }

    /**
     * Returns native map for a specified square.
     */
    @SuppressLint("DefaultLocale")
    @NonNull
    public MapFile getNativeMap(int x, int y) {
        if (mNativeMaps[x][y] == null) {
            mNativeMaps[x][y] = new MapFile("7-" + x + "-" + y);
            mNativeMaps[x][y].fileName = mRootDir.getAbsolutePath() + File.separator + getLocalPath(x, y);
        }
        return mNativeMaps[x][y];
    }

    public void removeNativeMap(int x, int y) {
        if (mNativeMaps[x][y] == null)
            return;
        File file = new File(mNativeMaps[x][y].fileName);
        if (file.exists() && file.delete()) {
            mNativeMaps[x][y].downloaded = false;
        }
    }

    public void clear() {
        for (MapFile map : mMaps) {
            map.tileSource.close();
            if (map.tileSource.tileCache != null && map.tileSource.tileCache instanceof TileCache)
                ((TileCache) map.tileSource.tileCache).dispose();
        }
        mMaps.clear();
    }

    public void setNativeMapStatus(int x, int y, short date, long size) {
        if (mNativeMaps[x][y] == null)
            getNativeMap(x, y);
        mNativeMaps[x][y].downloadCreated = date;
        mNativeMaps[x][y].downloadSize = size;
    }

    public void selectNativeMap(int x, int y, ACTION action) {
        MapFile mapFile = getNativeMap(x, y);
        if (mapFile.action == action)
            mapFile.action = ACTION.NONE;
        else
            mapFile.action = action;
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onMapSelected(x, y, mapFile.action);
            }
        }
    }

    public void clearSelections() {
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                if (mNativeMaps[x][y] != null)
                    mNativeMaps[x][y].action = ACTION.NONE;
    }

    public void setNativeMapTileSource(int key) {
        int x = key >>> 7;
        int y = key & 0x7f;
        setNativeMapTileSource(mNativeMaps[x][y]);
    }

    private void setNativeMapTileSource(MapFile mapFile) {
        //TODO Check if tile source exists and close it
        MapFileTileSource tileSource = new MapFileTileSource();
        if (tileSource.setMapFile(mapFile.fileName)) {
            TileSource.OpenResult openResult = tileSource.open();
            if (openResult.isSuccess()) {
                mapFile.created = tileSource.getMapInfo().mapDate;
                mapFile.downloaded = true;
                mapFile.tileSource = tileSource;
            } else {
                Log.w(TAG, "Failed to open file: " + openResult.getErrorMessage());
                mapFile.downloaded = false;
            }
            tileSource.close();
        }
    }

    public boolean isDownloading(int x, int y) {
        return mNativeMaps[x][y] != null && mNativeMaps[x][y].downloading != 0L;
    }

    public static int processDownloadedMap(String filePath) {
        File srcFile = new File(filePath);
        File mapFile = new File(filePath.replace(".part", ""));
        String fileName = mapFile.getName();
        String[] parts = fileName.split("[\\-\\.]");
        try {
            if (parts.length != 3)
                throw new NumberFormatException("unexpected name");
            int x = Integer.valueOf(parts[0]);
            int y = Integer.valueOf(parts[1]);
            if (x > 127 || y > 127)
                throw new NumberFormatException("out of range");
            if ((!mapFile.exists() || mapFile.delete()) && srcFile.renameTo(mapFile))
                return getNativeKey(x, y);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getMessage());
        }
        return 0;
    }

    public boolean hasDownloadSizes() {
        return mHasDownloadSizes;
    }

    public void setHasDownloadSizes(boolean hasSizes) {
        mHasDownloadSizes = hasSizes;
        if (hasSizes) {
            for (int x = 0; x < 128; x++)
                for (int y = 0; y < 128; y++) {
                    MapFile mapFile = getNativeMap(x, y);
                    if (mapFile.action == MapIndex.ACTION.DOWNLOAD) {
                        if (mapFile.downloadSize == 0L)
                            selectNativeMap(x, y, MapIndex.ACTION.NONE);
                    }
                }
            for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
                MapStateListener mapStateListener = weakRef.get();
                if (mapStateListener != null) {
                    mapStateListener.onHasDownloadSizes();
                }
            }
        }
    }

    public void addMapStateListener(MapStateListener listener) {
        mMapStateListeners.add(new WeakReference<>(listener));
    }

    public void removeMapStateListener(MapStateListener listener) {
        for (Iterator<WeakReference<MapStateListener>> iterator = mMapStateListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<MapStateListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    public static int getNativeKey(int x, int y) {
        return (x << 7) + y;
    }

    @SuppressLint("DefaultLocale")
    public static Uri getIndexUri() {
        return new Uri.Builder()
                .scheme("http")
                .authority("maptrek.mobi")
                .appendPath("maps")
                .appendPath("index")
                .build();
    }

    @SuppressLint("DefaultLocale")
    public static Uri getDownloadUri(int x, int y) {
        return new Uri.Builder()
                .scheme("http")
                .authority("maptrek.mobi")
                .appendPath("maps")
                .appendPath(String.valueOf(x))
                .appendPath(String.format("%d-%d.map", x, y))
                .build();
    }

    @SuppressLint("DefaultLocale")
    public static String getLocalPath(int x, int y) {
        return String.format("native/%d/%d-%d.map", x, x, y);
    }
}
