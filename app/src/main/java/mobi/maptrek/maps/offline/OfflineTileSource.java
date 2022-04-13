/*
 * Copyright 2020 Andrey Novikov
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

package mobi.maptrek.maps.offline;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;

public class OfflineTileSource extends TileSource {
    public static final String TILE_TYPE = "vnd.android.cursor.item/vnd.mobi.maptrek.maps.offline.provider.tile";
    public static final String[] TILE_COLUMNS = new String[]{"TILE"};

    public static class Builder<T extends Builder<T>> extends TileSource.Builder<T> {
        private Context context;
        protected String name;
        protected String code;
        protected String uri;
        protected String license;

        protected Builder(Context context) {
            this.context = context;
        }

        @Override
        public OfflineTileSource build() {
            return new OfflineTileSource(this);
        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T code(String code) {
            this.code = code;
            return self();
        }

        public T uri(String uri) {
            this.uri = uri;
            return self();
        }

        public T license(String license) {
            this.license = license;
            return self();
        }
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder(Context context) {
        return new Builder(context);
    }

    private final Context mContext;
    private ContentProviderClient mProviderClient;

    private final String mName;
    private final String mCode;
    private final String mUri;
    private final String mLicense;

    protected OfflineTileSource(Builder<?> builder) {
        super(builder);
        mContext = builder.context;
        mName = builder.name;
        mCode = builder.code;
        mUri = builder.uri;
        mLicense = builder.license;
    }

    @Override
    public OpenResult open() {
        mProviderClient = mContext.getContentResolver().acquireContentProviderClient(Uri.parse(mUri));
        if (mProviderClient != null)
            return OpenResult.SUCCESS;
        else
            return new OpenResult("Failed to get provider for uri: " + mUri);
    }

    @Override
    public void close() {
        if (mProviderClient != null) {
            mProviderClient.release();
            mProviderClient = null;
        }
    }

    @Override
    public ITileDataSource getDataSource() {
        return new OfflineDataSource();
    }

    private class OfflineDataSource implements ITileDataSource {

        OfflineDataSource() {
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            if (mProviderClient == null) {
                // QueryResult result = tile.zoomLevel > 7 ? TILE_NOT_FOUND : SUCCESS;
                mapDataSink.completed(FAILED);
                return;
            }
            Uri contentUri = Uri.parse(mUri + "/" + tile.zoomLevel + "/" + tile.tileX + "/" + tile.tileY);
            try {
                Cursor cursor = mProviderClient.query(contentUri, TILE_COLUMNS, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    byte[] bytes = cursor.getBlob(0);
                    cursor.close();
                    Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    if (bitmap.isValid()) {
                        mapDataSink.setTileImage(bitmap);
                        mapDataSink.completed(SUCCESS);
                    } else {
                        mapDataSink.completed(FAILED);
                    }
                } else {
                    mapDataSink.completed(TILE_NOT_FOUND);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                mapDataSink.completed(FAILED);
            }
        }

        @Override
        public void dispose() {
        }

        @Override
        public void cancel() {
        }
    }

    public String getName() {
        return mName;
    }

    public String getUri() {
        return mUri;
    }
}
