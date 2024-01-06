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
import java.util.Objects;

import mobi.maptrek.maps.plugin.PluginOfflineTileSource;
import mobi.maptrek.maps.plugin.PluginOnlineTileSource;
import mobi.maptrek.maps.plugin.PluginTileSourceFactory;
import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;

public class MapIndex implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MapIndex.class);

    private static final long serialVersionUID = 1L;
    private static final BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(-85.0511d, -180d, 85.0511d, 180d);

    private final Context mContext;
    private final HashSet<MapFile> mMaps;
    private final PluginTileSourceFactory mPluginTileSourceFactory;

    @SuppressLint("UseSparseArrays")
    public MapIndex(@NonNull Context context, @Nullable File root) {
        mContext = context;
        mPluginTileSourceFactory = new PluginTileSourceFactory(context, context.getPackageManager());
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
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
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
                    mapFile.id = tileSource.getOption("path");
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

    public void initializePluginMapProviders() {
        for (PluginOnlineTileSource source : mPluginTileSourceFactory.getOnlineTileSources()) {
            addTileSource(source, source.getSourceId());
        }

        for (PluginOfflineTileSource source : mPluginTileSourceFactory.getOfflineTileSources()) {
            addTileSource(source, source.getSourceId());
        }
    }

    private void addTileSource(TileSource tileSource, String id) {
        MapFile mapFile = new MapFile(tileSource.getName(), id);
        mapFile.tileSource = tileSource;
        mapFile.boundingBox = WORLD_BOUNDING_BOX;
        //TODO Implement tile cache expiration
        //tileProvider.tileExpiration = onlineMapTileExpiration;

        mMaps.add(mapFile);
    }

    @NonNull
    public ArrayList<MapFile> getMaps(@Nullable String[] ids) {
        ArrayList<MapFile> maps = new ArrayList<>();
        if (ids == null)
            return maps;
        for (String id : ids) {
            for (MapFile map : mMaps) {
                if (id.equals(map.id))
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
