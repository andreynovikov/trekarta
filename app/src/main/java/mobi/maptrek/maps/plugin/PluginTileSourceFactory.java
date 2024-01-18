package mobi.maptrek.maps.plugin;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.oscim.android.cache.TileCache;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class PluginTileSourceFactory {
    public static List<PluginOnlineTileSource> getOnlineTileSources(Context context, PackageManager pm) {
        Intent initializationIntent = new Intent(PluginTileSourceContract.ONLINE_PROVIDER_INTENT);
        List<ResolveInfo> providers = getMapProviders(context, pm, initializationIntent);

        List<PluginOnlineTileSource> tileSources = new LinkedList<>();
        for (ResolveInfo provider: providers) {
            List<PluginOnlineTileSource> onlineTileSources = getOnlineMapsFromPlugin(context, pm, provider);
            tileSources.addAll(onlineTileSources);
        }
        return tileSources;
    }

    public static List<PluginOfflineTileSource> getOfflineTileSources(Context context, PackageManager pm) {
        Intent initializationIntent = new Intent(PluginTileSourceContract.OFFLINE_PROVIDER_INTENT);
        List<ResolveInfo> providers = getMapProviders(context, pm, initializationIntent);

        List<PluginOfflineTileSource> tileSources = new LinkedList<>();
        for (ResolveInfo provider: providers) {
            List<PluginOfflineTileSource> offlineTileSources = getOfflineMapsFromPlugin(context, pm, provider);
            tileSources.addAll(offlineTileSources);
        }
        return tileSources;
    }

    private static List<ResolveInfo> getMapProviders(Context context, PackageManager pm, Intent initializationIntent) {
        List<ResolveInfo> providers = pm.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo provider : providers) {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            context.sendBroadcast(intent);
        }

        return providers;
    }

    @NonNull
    private static List<PluginOnlineTileSource> getOnlineMapsFromPlugin(Context context, PackageManager pm, ResolveInfo provider) {
        List<PluginOnlineTileSource> sources = new ArrayList<>();

        String authority;
        try {
            authority = getProviderAuthority(pm, provider);
        } catch (PluginTileSourceContractViolatedException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return sources;
        }

        File cacheDir = new File(context.getExternalCacheDir(), "online");
        boolean useCache = cacheDir.mkdir() || cacheDir.isDirectory();

        ContentProviderClient providerClient = context.getContentResolver().acquireContentProviderClient(authority);
        if (providerClient == null)
            return sources;

        List<Maps.Metadata> maps;
        try {
             maps = Maps.getMaps(providerClient, authority);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
            return sources;
        }

        for (Maps.Metadata map : maps) {
            PluginOnlineTileSource.Builder<?> builder = PluginOnlineTileSource.builder(context);
            builder.name(map.name);
            builder.mapId(map.identifier);
            builder.providerAuthority(authority);
            builder.license(map.license);
            builder.providerAuthority(authority);
            if (map.minZoom != Integer.MIN_VALUE)
                builder.zoomMin(map.minZoom);
            if (map.maxZoom != Integer.MAX_VALUE)
                builder.zoomMax(map.maxZoom);

            PluginOnlineTileSource source = builder.build();
            if (useCache) {
                TileCache cache = new TileCache(context, cacheDir.getAbsolutePath(), authority + "/" + map.identifier);
                source.setCache(cache);
            }
            sources.add(source);
        }
        providerClient.release();
        return sources;
    }

    @NonNull
    private static List<PluginOfflineTileSource> getOfflineMapsFromPlugin(Context context, PackageManager pm, ResolveInfo provider) {
        List<PluginOfflineTileSource> sources = new ArrayList<>();

        String authority;
        try {
            authority = getProviderAuthority(pm, provider);
        } catch (PluginTileSourceContractViolatedException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return sources;
        }

        ContentProviderClient providerClient = context.getContentResolver().acquireContentProviderClient(authority);
        if (providerClient == null)
            return sources;

        List<Maps.Metadata> maps;
        try {
            maps = Maps.getMaps(providerClient, authority);
        } catch (RemoteException | PluginTileSourceContractViolatedException e) {
            e.printStackTrace();
            return sources;
        }

        for (Maps.Metadata map : maps) {
            PluginOfflineTileSource.Builder<?> builder = PluginOfflineTileSource.builder(context);
            builder.name(map.name);
            builder.mapId(map.identifier);
            builder.license(map.license);
            builder.providerAuthority(authority);
            if (map.minZoom != Integer.MIN_VALUE)
                builder.zoomMin(map.minZoom);
            if (map.maxZoom != Integer.MAX_VALUE)
                builder.zoomMax(map.maxZoom);
            sources.add(builder.build());
        }

        providerClient.release();
        return sources;
    }

    @NonNull
    static String getProviderAuthority(@NonNull PackageManager packageManager, @NonNull ResolveInfo provider)
            throws PluginTileSourceContractViolatedException, PackageManager.NameNotFoundException {

        String authority;
        Resources resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo);
        @SuppressLint("DiscouragedApi")  // name should be used because we query another package
        int id = resources.getIdentifier("authority", "string", provider.activityInfo.packageName);

        if (id == 0) {
            throw new PluginTileSourceContractViolatedException("Cannot find ContentProvider's authority for " + provider.activityInfo.packageName);
        } else try {
            authority = resources.getString(id);
        } catch (Resources.NotFoundException e) {
            throw new IllegalStateException("Identifier no longer valid", e);
        }
        return authority;
    }

    static class Maps {

        static class Metadata {
            private final String name;
            private final String identifier;
            private final int minZoom;
            private final int maxZoom;

            private String license;

            Metadata(@NonNull String name, @NonNull String identifier, int minZoom, int maxZoom) {
                this.name = name;
                this.identifier = identifier;
                this.minZoom = minZoom;
                this.maxZoom = maxZoom;
            }
        }

        @NonNull
        static List<Metadata> getMaps(@NonNull ContentProviderClient client, @NonNull String authority)
                throws  RemoteException, PluginTileSourceContractViolatedException {

            Uri queryUri = getMapsQueryUri(authority);
            List<Metadata> maps = new LinkedList<>();
            Cursor cursor = client.query(queryUri, PluginTileSourceContract.MAP_COLUMNS, null, null, null);
            if (cursor == null)
                return maps;

            cursor.moveToFirst();
            do {
                // map name
                int nameIdx = cursor.getColumnIndex(PluginTileSourceContract.COLUMN_NAME);
                if (nameIdx < 0)
                    throw new PluginTileSourceContractViolatedException("Map name must be specified");
                if (cursor.getType(nameIdx) != Cursor.FIELD_TYPE_STRING)
                    throw new PluginTileSourceContractViolatedException("Map name must be a string");
                // map identifier
                int identifierIdx = cursor.getColumnIndex(PluginTileSourceContract.COLUMN_IDENTIFIER);
                if (identifierIdx < 0)
                    throw new PluginTileSourceContractViolatedException("Map identifier must be specified");
                if (cursor.getType(identifierIdx) != Cursor.FIELD_TYPE_STRING)
                    throw new PluginTileSourceContractViolatedException("Map identifier must be a string");
                // map min zoom
                int minZoomIdx = cursor.getColumnIndex(PluginTileSourceContract.COLUMN_MIN_ZOOM);
                if (minZoomIdx < 0)
                    throw new PluginTileSourceContractViolatedException("Map min zoom must be specified");
                if (cursor.getType(minZoomIdx) != Cursor.FIELD_TYPE_INTEGER)
                    throw new PluginTileSourceContractViolatedException("Map min zoom must be an integer");
                // map max zoom
                int maxZoomIdx = cursor.getColumnIndex(PluginTileSourceContract.COLUMN_MAX_ZOOM);
                if (maxZoomIdx < 0)
                    throw new PluginTileSourceContractViolatedException("Map max zoom must be specified");
                if (cursor.getType(minZoomIdx) != Cursor.FIELD_TYPE_INTEGER)
                    throw new PluginTileSourceContractViolatedException("Map max zoom must be an integer");

                Metadata map = new Metadata(
                        cursor.getString(nameIdx),
                        cursor.getString(identifierIdx),
                        cursor.getInt(minZoomIdx),
                        cursor.getInt(maxZoomIdx)
                );

                // map license
                int licenseIdx = cursor.getColumnIndex(PluginTileSourceContract.COLUMN_LICENSE);
                if (licenseIdx >= 0 && cursor.getType(licenseIdx) != Cursor.FIELD_TYPE_NULL) {
                    if (cursor.getType(licenseIdx) != Cursor.FIELD_TYPE_STRING)
                        throw new PluginTileSourceContractViolatedException("Map license must be a string");
                    map.license = cursor.getString(licenseIdx);
                }

                maps.add(map);
            } while (cursor.moveToNext());

            cursor.close();
            return maps;
        }

        @NonNull
        private static Uri getMapsQueryUri(@NonNull String authority) {
            String uri = "content://" + authority + "/maps";
            return Uri.parse(uri);
        }
    }

    static class Tiles {

        @Nullable
        static String getTileUri(ContentProviderClient client, String authority, String map, int zoomLevel, int tileX, int tileY)
                throws RemoteException, PluginTileSourceContractViolatedException {

            Uri queryUri = getTileQueryUri(authority, map, zoomLevel, tileX, tileY);
            Cursor cursor = client.query(queryUri, PluginTileSourceContract.TILE_COLUMNS, null, null, null);
            if (cursor == null) {
                return null;
            }

            cursor.moveToFirst();

            if (cursor.getColumnCount() < 1) {
                throw new PluginTileSourceContractViolatedException("Not enough column in cursor.");
            }

            if (cursor.getType(0) != Cursor.FIELD_TYPE_STRING) {
                throw new PluginTileSourceContractViolatedException("Unexpected value type.");
            }

            String tileUri = cursor.getString(0);
            cursor.close();
            return tileUri;
        }

        @Nullable
        static byte[] getTileBlob(ContentProviderClient client, String authority, String map, int zoomLevel, int tileX, int tileY)
                throws RemoteException, PluginTileSourceContractViolatedException {

            Uri queryUri = getTileQueryUri(authority, map, zoomLevel, tileX, tileY);
            Cursor cursor = client.query(queryUri, PluginTileSourceContract.TILE_COLUMNS, null, null, null);
            if (cursor == null) {
                return null;
            }

            cursor.moveToFirst();

            if (cursor.getColumnCount() < 1) {
                throw new PluginTileSourceContractViolatedException("Not enough column in cursor.");
            }

            if (cursor.getType(0) != Cursor.FIELD_TYPE_BLOB) {
                throw new PluginTileSourceContractViolatedException("Unexpected value type.");
            }

            byte[] tileBlob = cursor.getBlob(0);
            cursor.close();
            return tileBlob;
        }

        private static Uri getTileQueryUri(String authority, String map, int zoomLevel, int tileX, int tileY) {
            String uri = "content://" + authority + "/tiles/" + map + "/" + zoomLevel + "/" + tileX + "/" + tileY;
            return Uri.parse(uri);
        }
    }
}
