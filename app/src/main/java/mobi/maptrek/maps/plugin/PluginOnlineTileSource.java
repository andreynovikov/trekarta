package mobi.maptrek.maps.plugin;

import android.content.ContentProviderClient;
import android.content.Context;
import android.os.RemoteException;

import org.oscim.core.Tile;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

public class PluginOnlineTileSource extends BitmapTileSource {

    public static class Builder<T extends Builder<T>> extends BitmapTileSource.Builder<T> {
        private final Context context;
        protected String mapId;
        protected String providerAuthority;
        protected String license;

        protected Builder(Context context) {
            this.context = context;
            // Fake url to skip UrlTileSource exception
            this.url = "http://maptrek.mobi/";
            //FIXME Switch to Volley http://developer.android.com/training/volley/index.html
            this.httpFactory(new OkHttpEngine.OkHttpFactory());
        }

        @Override
        public PluginOnlineTileSource build() {
            return new PluginOnlineTileSource(this);
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

    private final String mLicense;

    protected PluginOnlineTileSource(Builder<?> builder) {
        super(builder);
        mContext = builder.context;
        mMapId = builder.mapId;
        mProviderAuthority = builder.providerAuthority;
        mSourceId = "content://" + builder.providerAuthority + "/" + builder.mapId;
        mLicense = builder.license;
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
    public String getTileUrl(Tile tile) {
        String tileUrl = null;
        if (mProviderClient == null)
            return null;

        try {
            tileUrl = PluginTileSourceFactory.Tiles.getTileUri(
                    mProviderClient, mProviderAuthority, mMapId, tile.zoomLevel, tile.tileX, tile.tileY);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
        }

        return tileUrl;
    }

    public String getMapId() {
        return mMapId;
    }

    public String getSourceId() {
        return mSourceId;
    }

    public String getLicense() {
        return mLicense;
    }
}
