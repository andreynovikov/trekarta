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
        protected String mapName;
        protected String mapId;
        protected String providerAuthority;

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

        @Override
        public T name(String name) {
            this.mapName = name;
            return self();
        }

        public T mapId(String identifier) {
            this.mapId = identifier;
            return self();
        }

        public T providerAuthority(String authority) {
            this.providerAuthority = authority;
            return self();
        }
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder(Context context) {
        return new Builder(context);
    }

    private final Context mContext;
    private ContentProviderClient mProviderClient;

    private final String mMapName;
    private final String mMapId;
    private final String mProviderAuthority;
    private final String mSourceId;

    protected PluginOnlineTileSource(Builder<?> builder) {
        super(builder);
        mContext = builder.context;
        mMapName = builder.mapName;
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
    public String getTileUrl(Tile tile) {
        String tileUrl = null;
        if (mProviderClient == null)
            return null;

        try {
            tileUrl = PluginTileSourceContract.Tiles.getTileUri(
                    mProviderClient, mProviderAuthority, mMapId, tile.zoomLevel, tile.tileX, tile.tileY);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
        }

        return tileUrl;
    }

    @Override
    public String getName() {
        return mMapName;
    }

    public String getMapId() {
        return mMapId;
    }

    public String getSourceId() {
        return mSourceId;
    }
}
