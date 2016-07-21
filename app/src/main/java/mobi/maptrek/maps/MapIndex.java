package mobi.maptrek.maps;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;

public class MapIndex implements Serializable {
    private static final String TAG = "MapIndex";

    private static final long serialVersionUID = 1L;

    private File mRootDir;
    private HashSet<MapFile> mMaps;
    private MapFile[][] mNativeMaps = new MapFile[128][128];
    private int mHashCode;

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
            mHashCode = getMapsHash(files);
        }
    }

    public static int getMapsHash(String path) {
        File root = new File(path);
        List<File> files = FileList.getFileListing(root, new MapFilenameFilter());
        return getMapsHash(files);
    }

    private static int getMapsHash(List<File> files) {
        int result = 13;
        for (File file : files) {
            result = 31 * result + file.getAbsolutePath().hashCode();
        }
        return result;
    }

    /*
    public static MapIndex loadIndex(File file) throws Throwable {
        com.esotericsoftware.minlog.Log.DEBUG();
        Kryo kryo = new Kryo();
        kryo.register(MapIndex.class);
        kryo.register(MapFile.class);
        kryo.register(Integer.class);
        kryo.register(String.class);
        kryo.register(ArrayList.class);
        kryo.register(HashSet.class);
        kryo.register(HashMap.class);
        Input input = new Input(new FileInputStream(file));
        MapIndex index = kryo.readObject(input, MapIndex.class);
        input.close();
        return index;
    }

    public static void saveIndex(MapIndex index, File file) throws Throwable {
        Kryo kryo = new Kryo();
        kryo.register(MapIndex.class);
        kryo.register(MapFile.class);
        kryo.register(Integer.class);
        kryo.register(String.class);
        kryo.register(ArrayList.class);
        kryo.register(HashSet.class);
        kryo.register(HashMap.class);
        Output output = new Output(new FileOutputStream(file));
        kryo.writeObject(output, index);
        output.close();
    }
    */

    @Override
    public int hashCode() {
        return mHashCode;
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
                //FIXME Remove unused fields
                mNativeMaps[x][y] = new MapFile("7-" + x + "-" + "y", null, file.getAbsolutePath(), null, null);
                mNativeMaps[x][y].downloaded = true;
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

    public void removeMap(MapFile map) {
        mMaps.remove(map);
        map.tileSource.close();
    }

    @Nullable
    public MapFile getNativeMap(int x, int y) {
        if (mNativeMaps[x][y] != null && mNativeMaps[x][y].downloaded)
            return mNativeMaps[x][y];
        return null;
    }

    @Nullable
    public MapFile getMap(@Nullable String filename) {
        if (filename == null)
            return null;
        for (MapFile map : mMaps) {
            if (map.fileName.equals(filename))
                return map;
        }
        return null;
    }

    @NonNull
    public Collection<MapFile> getMaps() {
        return mMaps;
    }

    public void clear() {
        for (MapFile map : mMaps)
            map.tileSource.close();
        mMaps.clear();
    }

    public void markDownloading(int x, int y, long enqueue) {
        if (mNativeMaps[x][y] == null) {
            //FIXME Remove unused fields
            mNativeMaps[x][y] = new MapFile("7-" + x + "-" + "y", null, mRootDir.getAbsolutePath() + File.separator + getNativeMapFilePath(x, y), null, null);
        }
        mNativeMaps[x][y].downloading = enqueue;
    }

    public boolean isDownloading(int x, int y) {
        return mNativeMaps[x][y] != null && mNativeMaps[x][y].downloading != 0L;
    }

    public void processDownloadedMap(String filePath) {
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
            if (!mapFile.exists() || mapFile.delete())
                srcFile.renameTo(mapFile);
            mNativeMaps[x][y].downloaded = true;
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getMessage());
        }
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
    public static String getNativeMapFilePath(int x, int y) {
        return String.format("/native/%d/%d-%d.map", x, x, y);
    }
}
