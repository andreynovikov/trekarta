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

package mobi.maptrek.maps.online;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.oscim.core.Tile;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

public class OnlineTileSource extends BitmapTileSource {
    public static final String TILE_TYPE = "vnd.android.cursor.item/vnd.mobi.maptrek.maps.online.provider.tile";
    public static final String[] TILE_COLUMNS = new String[]{"TILE"};

    public static class Builder<T extends Builder<T>> extends BitmapTileSource.Builder<T> {
        private Context context;
        protected String name;
        protected String code;
        protected String uri;
        protected String license;
        protected int threads;

        protected Builder(Context context) {
            this.context = context;
            // Fake url to skip UrlTileSource exception
            this.url = "http://maptrek.mobi/";
            //FIXME Switch to Volley http://developer.android.com/training/volley/index.html
            this.httpFactory(new OkHttpEngine.OkHttpFactory());
        }

        @Override
        public OnlineTileSource build() {
            return new OnlineTileSource(this);
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

        public T threads(int threads) {
            this.threads = threads;
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
    private final int mThreads;

    protected OnlineTileSource(Builder<?> builder) {
        super(builder);
        mContext = builder.context;
        mName = builder.name;
        mCode = builder.code;
        mUri = builder.uri;
        mLicense = builder.license;
        mThreads = builder.threads;
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
    public String getTileUrl(Tile tile) {
        String tileUrl = null;
        if (mProviderClient == null)
            return null;
        Uri contentUri = Uri.parse(mUri + "/" + tile.zoomLevel + "/" + tile.tileX + "/" + tile.tileY);
        try {
            Cursor cursor = mProviderClient.query(contentUri, TILE_COLUMNS, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                tileUrl = cursor.getString(0);
                cursor.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return tileUrl;
    }

    public String getName() {
        return mName;
    }
}
