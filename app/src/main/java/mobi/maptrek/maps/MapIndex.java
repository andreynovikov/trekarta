package mobi.maptrek.maps;

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
    private int mHashCode;

    @SuppressWarnings("unused")
    MapIndex() {
    }

    public MapIndex(File root) {
        mRootDir = root;
        mMaps = new HashSet<>();
        List<File> files = FileList.getFileListing(mRootDir, new MapFilenameFilter());
        for (File file : files) {
            load(file);
        }
        mHashCode = getMapsHash(files);
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

    private void load(File file) {
        String fileName = file.getName();
        Log.e(TAG, "load(" + fileName + ")");
        if (NativeMaps.files.containsKey(fileName)) {
            Log.w(TAG, "  marked");
            NativeMaps.files.get(fileName).downloaded = true;
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

    public MapFile getNativeMap(double x, double y) {
        for (MapFile mapFile : NativeMaps.set) {
            if (mapFile.downloaded || mapFile.downloading != 0)
                continue;
            if (mapFile.contains(x, y))
                return mapFile;
        }
        return null;
    }

    public Collection<MapFile> getMaps() {
        return mMaps;
    }

    public void clear() {
        for (MapFile map : mMaps)
            map.tileSource.close();
        mMaps.clear();
        mMaps = null;
    }

    public void markDownloaded(String filePath) {
        String fileName = filePath.replace(mRootDir.getAbsolutePath() + File.separator, "");
        if (NativeMaps.files.containsKey(fileName))
            NativeMaps.files.get(fileName).downloaded = true;
    }
}
