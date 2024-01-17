/*
 * Copyright 2024 Andrey Novikov
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

package mobi.maptrek.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.maps.maptrek.Index;

public class MapIndexViewModel extends ViewModel implements Index.MapStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MapIndexViewModel.class);

    private static final long INDEX_CACHE_TIMEOUT = 24 * 3600 * 1000L; // One day
    private static final long INDEX_CACHE_EXPIRATION = 60 * 24 * 3600 * 1000L; // Two months
    private static final long HILLSHADE_CACHE_TIMEOUT = 60 * 24 * 3600 * 1000L; // Two months

    public final Index nativeIndex = MapTrek.getApplication().getMapIndex();
    public final File cacheFile;
    private final File hillshadeCacheFile;

    private final MutableLiveData<Index.IndexStats> indexState = new MutableLiveData<>(nativeIndex.getMapStats());

    private final ExecutorService executorService =  Executors.newSingleThreadExecutor();

    public MapIndexViewModel() {
        super();
        boolean hillshadesEnabled = Configuration.getHillshadesEnabled();
        nativeIndex.accountHillshades(hillshadesEnabled);
        nativeIndex.addMapStateListener(this);
        File cacheDir = MapTrek.getApplication().getExternalCacheDir();
        cacheFile = new File(cacheDir, "mapIndex");
        hillshadeCacheFile = new File(cacheDir, "hillshadeIndex");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        nativeIndex.removeMapStateListener(this);
        executorService.shutdown();
    }

    public LiveData<Index.IndexStats> getNativeIndexState() {
        return indexState;
    }

    public static class BaseMapState {
        public int version;
        public long size;
        public boolean outdated;

        public BaseMapState(int version, long size, boolean outdated) {
            this.version = version;
            this.size = size;
            this.outdated = outdated;
        }
    }

    private final MutableLiveData<BaseMapState> baseMapState = new MutableLiveData<>(
            new BaseMapState(
                    nativeIndex.getBaseMapVersion(),
                    nativeIndex.getBaseMapSize(),
                    nativeIndex.isBaseMapOutdated()
            )
    );
    public LiveData<BaseMapState> getBaseMapState() {
        return baseMapState;
    }

    public static class ActionState {
        public int x;
        public int y;
        public Index.ACTION action;

        public ActionState(int x, int y, Index.ACTION action) {
            this.x = x;
            this.y = y;
            this.action = action;
        }
    }

    private final MutableLiveData<ActionState> actionState = new MutableLiveData<>(null);
    public LiveData<ActionState> getActionState() {
        return actionState;
    }

    @Override
    public void onHasDownloadSizes() {
        indexState.postValue(nativeIndex.getMapStats());
    }

    @Override
    public void onBaseMapChanged() {
        baseMapState.postValue(
                new BaseMapState(
                        nativeIndex.getBaseMapVersion(),
                        nativeIndex.getBaseMapSize(),
                        nativeIndex.isBaseMapOutdated()
                )
        );
    }

    @Override
    public void onStatsChanged() {
        indexState.postValue(nativeIndex.getMapStats());
    }

    @Override
    public void onHillshadeAccountingChanged(boolean account) {
        indexState.postValue(nativeIndex.getMapStats());
    }

    @Override
    public void onMapSelected(int x, int y, Index.ACTION action, Index.IndexStats stats) {
        actionState.postValue(new ActionState(x, y, action));
        indexState.postValue(stats);
    }

    private int progress = -1;
    private final MutableLiveData<Integer> indexDownloadProgressState = new MutableLiveData<>(progress);
    public LiveData<Integer> getIndexDownloadProgressState() {
        return indexDownloadProgressState;
    }

    public void loadMapIndexes(String stats) {
        if (nativeIndex.hasDownloadSizes())
            return;
        if (progress >= 0) // already loading
            return;

        progress = 0;
        indexDownloadProgressState.setValue(0);

        executorService.execute(() -> {
            boolean result = doLoadMapIndexes(stats);
            if (result) {
                boolean expired = cacheFile.lastModified() + INDEX_CACHE_EXPIRATION < System.currentTimeMillis();
                progress = -1;
                indexDownloadProgressState.postValue(progress);
                nativeIndex.setHasDownloadSizes(expired);
            } else {
                progress = -2; // error
                indexDownloadProgressState.postValue(progress);
            }
        });
    }

    private boolean doLoadMapIndexes(String stats) {
        long now = System.currentTimeMillis();
        boolean validCache = cacheFile.lastModified() + INDEX_CACHE_TIMEOUT > now;
        boolean validHillshadeCache = hillshadeCacheFile.lastModified() + HILLSHADE_CACHE_TIMEOUT > now;
        int divider = validHillshadeCache ? 1 : 2;
        // load map index
        try {
            boolean loaded = false;
            InputStream in;
            if (!validCache) {
                URL url = new URL(Index.getIndexUri().toString() + "?" + stats);
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    File tmpFile = new File(cacheFile.getAbsoluteFile() + "_tmp");
                    OutputStream out = new FileOutputStream(tmpFile);
                    loadMapIndex(in, out, divider);
                    loaded = tmpFile.renameTo(cacheFile);
                } catch (IOException e) {
                    logger.error("Failed to download map index", e);
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
            if (!loaded) {
                in = new FileInputStream(cacheFile);
                loadMapIndex(in, null, divider);
            }
        } catch (Exception e) {
            logger.error("Failed to load map index", e);
            // remove cache on any error
            //noinspection ResultOfMethodCallIgnored
            cacheFile.delete();
            return false;
        }
        // load hillshade index
        try {
            boolean loaded = false;
            InputStream in;
            if (!validHillshadeCache) {
                URL url = new URL(Index.getHillshadeIndexUri().toString());
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    File tmpFile = new File(hillshadeCacheFile.getAbsoluteFile() + "_tmp");
                    OutputStream out = new FileOutputStream(hillshadeCacheFile);
                    loadHillshadesIndex(in, out, divider);
                    loaded = tmpFile.renameTo(hillshadeCacheFile);
                } catch (IOException e) {
                    logger.error("Failed to download hillshades index", e);
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
            if (!loaded) {
                in = new FileInputStream(hillshadeCacheFile);
                loadHillshadesIndex(in, null, divider);
            }
        } catch (Exception e) {
            logger.error("Failed to load hillshades index", e);
            // remove cache on any error
            //noinspection ResultOfMethodCallIgnored
            hillshadeCacheFile.delete();
            return false;
        }
        return true;
    }

    private void loadMapIndex(InputStream in, OutputStream out, int divider) throws IOException {
        DataInputStream data = new DataInputStream(new BufferedInputStream(in));
        DataOutputStream dataOut = null;
        if (out != null)
            dataOut = new DataOutputStream(new BufferedOutputStream(out));

        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                short date = data.readShort();
                int size = data.readInt();
                if (dataOut != null) {
                    dataOut.writeShort(date);
                    dataOut.writeInt(size);
                }
                nativeIndex.setNativeMapStatus(x, y, date, size);
                int p = (int) ((x * 128 + y) / 163.84 / divider);
                if (p > progress) {
                    progress = p;
                    indexDownloadProgressState.postValue(progress);
                }
            }
        short date = data.readShort();
        int size = data.readInt();
        nativeIndex.setBaseMapStatus(date, size);
        if (dataOut != null) {
            dataOut.writeShort(date);
            dataOut.writeInt(size);
            dataOut.close();
        }
    }

    private void loadHillshadesIndex(InputStream in, OutputStream out, int divider) throws IOException {
        DataInputStream data = new DataInputStream(new BufferedInputStream(in));
        DataOutputStream dataOut = null;
        if (out != null)
            dataOut = new DataOutputStream(new BufferedOutputStream(out));

        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                byte version = data.readByte();
                int size = data.readInt();
                if (dataOut != null) {
                    dataOut.writeByte(version);
                    dataOut.writeInt(size);
                }
                nativeIndex.setHillshadeStatus(x, y, version, size);
                int p = (int) ((x * 128 + y) / 163.84 / divider);
                if (p > progress) {
                    progress = p;
                    indexDownloadProgressState.postValue(progress);
                }
            }
        if (dataOut != null) {
            dataOut.close();
        }
    }
}
