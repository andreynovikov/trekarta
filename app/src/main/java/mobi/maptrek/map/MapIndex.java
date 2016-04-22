package mobi.maptrek.map;

import android.util.Log;

import org.oscim.core.GeoPoint;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;

public class MapIndex implements Serializable {
    private static final long serialVersionUID = 1L;

    private HashSet<MapFile> mMaps;
    private int mHashCode;
    private transient Comparator<MapFile> mComparator = new MapComparator();

    @SuppressWarnings("unused")
    MapIndex() {
    }

    public MapIndex(File root) {
        mMaps = new HashSet<>();
        List<File> files = FileList.getFileListing(root, new MapFilenameFilter());
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
        Log.e("MAPS", "load(" + file.getAbsolutePath() + ")");
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

        Log.e("MAPS", "  added " + mapFile.boundingBox.toString());
        mMaps.add(mapFile);
    }

    public void removeMap(MapFile map) {
        mMaps.remove(map);
        map.tileSource.close();
    }

    public List<MapFile> getMaps(GeoPoint geoPoint) {
        List<MapFile> mapList = new ArrayList<>();

        for (MapFile map : mMaps) {
            if (!mapList.contains(map) && map.boundingBox.contains(geoPoint))
                mapList.add(map);
        }

        Collections.sort(mapList, mComparator);

        return mapList;
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

    private class MapComparator implements Comparator<MapFile>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(MapFile o1, MapFile o2) {
            // Larger max zoom is better
            int res = Integer.compare(o1.tileSource.getZoomLevelMax(), o2.tileSource.getZoomLevelMax());
            if (res != 0)
                return res;
            // Larger min zoom is "better" too
            res = Integer.compare(o1.tileSource.getZoomLevelMin(), o2.tileSource.getZoomLevelMin());
            if (res != 0)
                return res;
            //TODO Compare covering area - smaller is better
            return 0;
        }
    }
}
