package mobi.maptrek.maps.plugin;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;

import android.content.ContentProviderClient;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.RemoteException;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

public class PluginOfflineTileSource extends TileSource {

    public static class Builder<T extends Builder<T>> extends TileSource.Builder<T> {
        private final Context context;
        protected String mapId;
        protected String providerAuthority;
        protected String license;

        protected Builder(Context context) {
            this.context = context;
        }

        @Override
        public PluginOfflineTileSource build() {
            return new PluginOfflineTileSource(this);
        }

        public T mapId(String identifier) {
            this.mapId = identifier;
            return self();
        }

        public T providerAuthority(String authority) {
            this.providerAuthority = authority;
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

    private final String mMapId;
    private final String mProviderAuthority;
    private final String mSourceId;

    protected PluginOfflineTileSource(Builder<?> builder) {
        super(builder);
        mContext = builder.context;
        mMapId = builder.mapId;
        mProviderAuthority = builder.providerAuthority;
        mSourceId = "content://" + builder.providerAuthority + "/" + builder.mapId;
    }

    @Override
    public OpenResult open() {
        mProviderClient = mContext.getContentResolver().acquireContentProviderClient(mProviderAuthority);
        if (mProviderClient != null)
            return OpenResult.SUCCESS;
        else
            return new OpenResult("Failed to get provider for authority: " + mProviderAuthority);
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

            byte[] blob;
            try {
                blob = PluginTileSourceFactory.Tiles.getTileBlob(
                        mProviderClient, mProviderAuthority, mMapId, tile.zoomLevel, tile.tileX, tile.tileY);
                if (blob != null) {
                    Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeByteArray(blob, 0, blob.length));
                    if (bitmap.isValid()) {
                        mapDataSink.setTileImage(bitmap);
                        mapDataSink.completed(SUCCESS);
                    } else {
                        mapDataSink.completed(FAILED);
                    }
                } else {
                    mapDataSink.completed(TILE_NOT_FOUND);
                }
            } catch (RemoteException | PluginTileSourceContractViolatedException e) {
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

    public String getMapId() {
        return mMapId;
    }

    public String getSourceId() {
        return mSourceId;
    }
}
