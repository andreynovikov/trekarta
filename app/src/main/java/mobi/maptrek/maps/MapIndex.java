/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.oscim.android.cache.TileCache;
import org.oscim.core.BoundingBox;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.maps.offline.OfflineTileSource;
import mobi.maptrek.maps.offline.OfflineTileSourceFactory;
import mobi.maptrek.maps.online.OnlineTileSource;
import mobi.maptrek.maps.online.OnlineTileSourceFactory;
import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;

public class MapIndex implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MapIndex.class);

    private static final long serialVersionUID = 1L;
    private static final BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(-85.0511d, -180d, 85.0511d, 180d);

    private final Context mContext;
    private HashSet<MapFile> mMaps;

    @SuppressLint("UseSparseArrays")
    public MapIndex(@NonNull Context context, @Nullable File root) {
        mContext = context;
        mMaps = new HashSet<>();
        if (root != null) {
            logger.debug("MapIndex({})", root.getAbsolutePath());
            List<File> files = FileList.getFileListing(root, new MapFilenameFilter());
            for (File file : files)
                loadMap(file);
            File nativeDir = new File(root, "native");
            if (nativeDir.exists())
                deleteRecursive(nativeDir);
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        //noinspection ResultOfMethodCallIgnored
        fileOrDirectory.delete();
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

            List<OnlineTileSource> tileSources = OnlineTileSourceFactory.fromPlugin(mContext, packageManager, provider);
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

    public void initializeOfflineMapProviders() {
        PackageManager packageManager = mContext.getPackageManager();

        Intent initializationIntent = new Intent("mobi.maptrek.maps.offline.provider.action.INITIALIZE");
        // enumerate offline map providers
        List<ResolveInfo> providers = packageManager.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo provider : providers) {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            mContext.sendBroadcast(intent);

            List<OfflineTileSource> tileSources = OfflineTileSourceFactory.fromPlugin(mContext, packageManager, provider);
            for (OfflineTileSource tileSource : tileSources) {
                MapFile mapFile = new MapFile(tileSource.getName());
                mapFile.tileSource = tileSource;
                mapFile.boundingBox = WORLD_BOUNDING_BOX;
                mMaps.add(mapFile);
            }
        }
    }

    @NonNull
    public ArrayList<MapFile> getMaps(@Nullable String[] filenames) {
        ArrayList<MapFile> maps = new ArrayList<>();
        if (filenames == null)
            return maps;
        for (String filename : filenames) {
            for (MapFile map : mMaps) {
                if (filename.equals(map.tileSource.getOption("path")))
                    maps.add(map);
            }
        }
        return maps;
    }

    @NonNull
    public Collection<MapFile> getMaps() {
        return mMaps;
    }


    public void removeMap(MapFile mapFile) {
        logger.debug("  removed {}", mapFile.boundingBox);
        mMaps.remove(mapFile);
    }

    public void clear() {
        for (MapFile map : mMaps) {
            map.tileSource.close();
            if (map.tileSource.tileCache instanceof TileCache)
                ((TileCache) map.tileSource.tileCache).dispose();
        }
        mMaps.clear();
    }
}
