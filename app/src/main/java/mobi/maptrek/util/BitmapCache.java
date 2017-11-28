package mobi.maptrek.util;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

public class BitmapCache<K, V extends Bitmap> {
    private final LinkedHashMap<K,V> cache;

    BitmapCache(final int maxEntries) {
        this.cache = new LinkedHashMap<K,V>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest){
                if (size() > maxEntries) {
                    // TODO Implement bitmap locking
                    eldest.getValue().recycle();
                    return true;
                }
                return false;
            }
        };
    }

    public void put(K key, V value) {
        synchronized(cache) {
            cache.put(key, value);
        }
    }
    public V get(K key) {
        synchronized(cache) {
            V bitmap = cache.get(key);
            if (bitmap != null && !AndroidGraphics.getBitmap(bitmap).isRecycled())
                return bitmap;
            return null;
        }
    }

    public void clear() {
        synchronized (cache) {
            for (Bitmap bitmap : cache.values())
                bitmap.recycle();
            cache.clear();
        }
    }
}