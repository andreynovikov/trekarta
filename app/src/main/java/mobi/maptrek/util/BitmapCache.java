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
                return size() > maxEntries;
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