package mobi.maptrek.maps.plugin;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import org.oscim.android.cache.TileCache;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class PluginTileSourceFactory {

    private final Context mContext;
    private final PackageManager mPackageManager;

    public PluginTileSourceFactory(Context context, PackageManager packageManager) {
        mContext = context;
        mPackageManager = packageManager;
    }

    public List<PluginOnlineTileSource> getOnlineTileSources() {
        Intent initializationIntent = new Intent(PluginTileSourceContract.ONLINE_PROVIDER_INTENT);
        List<ResolveInfo> providers = getMapProviders(initializationIntent);

        List<PluginOnlineTileSource> tileSources = new LinkedList<>();
        for (ResolveInfo provider: providers) {
            List<PluginOnlineTileSource> onlineTileSources = getOnlineMapsFromPlugin(provider);
            tileSources.addAll(onlineTileSources);
        }
        return tileSources;
    }

    public List<PluginOfflineTileSource> getOfflineTileSources() {
        Intent initializationIntent = new Intent(PluginTileSourceContract.OFFLINE_PROVIDER_INTENT);
        List<ResolveInfo> providers = getMapProviders(initializationIntent);

        List<PluginOfflineTileSource> tileSources = new LinkedList<>();
        for (ResolveInfo provider: providers) {
            List<PluginOfflineTileSource> offlineTileSources = getOfflineMapsFromPlugin(provider);
            tileSources.addAll(offlineTileSources);
        }
        return tileSources;
    }

    private List<ResolveInfo> getMapProviders(Intent initializationIntent) {
        List<ResolveInfo> providers = mPackageManager.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo provider : providers) {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            mContext.sendBroadcast(intent);
        }

        return providers;
    }

    @NonNull
    private List<PluginOnlineTileSource> getOnlineMapsFromPlugin(ResolveInfo provider) {
        List<PluginOnlineTileSource> sources = new ArrayList<>();

        String authority;
        try {
            authority = PluginTileSourceContract.getProviderAuthority(mPackageManager, provider);
        } catch (PluginTileSourceContractViolatedException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return sources;
        }

        File cacheDir = new File(mContext.getExternalCacheDir(), "online");
        boolean useCache = cacheDir.mkdir() || cacheDir.isDirectory();

        ContentProviderClient providerClient = mContext.getContentResolver().acquireContentProviderClient(authority);
        if (providerClient == null)
            return sources;

        List<PluginTileSourceContract.Maps.Metadata> maps;
        try {
             maps = PluginTileSourceContract.Maps.getMaps(providerClient, authority);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
            return sources;
        }

        for (PluginTileSourceContract.Maps.Metadata map : maps) {
            PluginOnlineTileSource.Builder<?> builder = PluginOnlineTileSource.builder(mContext);
            builder.name(map.getName());
            builder.mapId(map.getIdentifier());
            builder.providerAuthority(authority);

            PluginOnlineTileSource source = builder.build();
            if (useCache) {
                TileCache cache = new TileCache(mContext, cacheDir.getAbsolutePath(), authority + "/" + map.getIdentifier());
                source.setCache(cache);
            }
            sources.add(source);
        }
        providerClient.release();
        return sources;
    }

    @NonNull
    private List<PluginOfflineTileSource> getOfflineMapsFromPlugin(ResolveInfo provider) {
        List<PluginOfflineTileSource> sources = new ArrayList<>();

        String authority;
        try {
            authority = PluginTileSourceContract.getProviderAuthority(mPackageManager, provider);
        } catch (PluginTileSourceContractViolatedException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return sources;
        }

        ContentProviderClient providerClient = mContext.getContentResolver().acquireContentProviderClient(authority);
        if (providerClient == null)
            return sources;

        List<PluginTileSourceContract.Maps.Metadata> maps;
        try {
            maps = PluginTileSourceContract.Maps.getMaps(providerClient, authority);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
            return sources;
        }

        for (PluginTileSourceContract.Maps.Metadata map : maps) {
            PluginOfflineTileSource.Builder<?> builder = PluginOfflineTileSource.builder(mContext);
            builder.name(map.getName());
            builder.mapId(map.getIdentifier());
            builder.providerAuthority(authority);
            sources.add(builder.build());
        }

        providerClient.release();
        return sources;
    }
}
