package mobi.maptrek.maps.plugin;

import android.content.ContentProviderClient;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import mobi.maptrek.MainActivity;

/**
 * Plugins must declare a receiver accepting ONLINE_PROVIDER_INTENT or OFFLINE_PROVIDER_INTENT.
 * They must provide the authority of the ContentProvider serving maps in a string resource
 * named "authority".
 *
 * This ContentProvider must accept two types of queries:
 * - "content://authority/maps" must be answered with a list of map descriptions.
 *   Each map is described by two columns, its name and its identifier.
 * - "content://authority/tiles/mapIdentifier/zoomLevel/tileX/tileY" must be answered with the
 *   the target tile uri or blob.
 */
final class PluginTileSourceContract {

    final static String ONLINE_PROVIDER_INTENT  = "mobi.maptrek.maps.online.provider.action.INITIALIZE";

    final static String OFFLINE_PROVIDER_INTENT = "mobi.maptrek.maps.offline.provider.action.INITIALIZE";

    final static String[] MAP_COLUMNS = new String[]{"NAME", "IDENTIFIER"};

    final static String[] TILE_COLUMNS = new String[]{"TILE"};

    @NonNull
    static String getProviderAuthority(@NonNull PackageManager packageManager, @NonNull ResolveInfo provider)
            throws PluginTileSourceContractViolatedException, PackageManager.NameNotFoundException {

        String authority;
        Resources resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo);
        int id = resources.getIdentifier("authority", "string", provider.activityInfo.packageName);

        if (id == 0) {
            throw new PluginTileSourceContractViolatedException("Cannot find ContentProvider's authority.");
        } else try {
            authority = resources.getString(id);
        } catch (Resources.NotFoundException e) {
            throw new IllegalStateException("Identifier no longer valid.", e);
        }
        return authority;
    }

    static class Maps {

        static final class Metadata {
            private final String mName;
            private final String mIdentifier;

            Metadata(@NonNull String name, @NonNull String identifier) {
                mName = name;
                mIdentifier = identifier;
            }

             @NonNull String getName() {
                return mName;
            }

             @NonNull String getIdentifier() {
                return mIdentifier;
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
                String name = getMapName(cursor);
                String identifier = getMapIdentifier(cursor);
                Metadata map = new Metadata(name, identifier);
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

        @NonNull
        private static String getMapName(Cursor cursor) throws PluginTileSourceContractViolatedException {
            if (cursor.getColumnCount() < 1) {
                throw new PluginTileSourceContractViolatedException("Not enough columns in cursor.");
            }

            if (cursor.getType(0) != Cursor.FIELD_TYPE_STRING) {
                throw new PluginTileSourceContractViolatedException("Map name must be a string.");
            }

            return cursor.getString(0);
        }

        @NonNull
        private static String getMapIdentifier(Cursor cursor) throws PluginTileSourceContractViolatedException {
            if (cursor.getColumnCount() < 2) {
                throw new PluginTileSourceContractViolatedException("Not enough columns in cursor.");
            }

            if (cursor.getType(1) != Cursor.FIELD_TYPE_STRING) {
                throw new PluginTileSourceContractViolatedException("Map identifier must be a string.");
            }

            return cursor.getString(1);
        }
    }

    static class Tiles {

        @Nullable
        static String getTileUri(ContentProviderClient client, String authority, String map, int zoomLevel, int tileX, int tileY)
                throws RemoteException, PluginTileSourceContractViolatedException {

            Uri queryUri = getTileQueryUri(authority, map, zoomLevel, tileX, tileY);
            Cursor cursor = client.query(queryUri, TILE_COLUMNS, null, null, null);
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
            Cursor cursor = client.query(queryUri, TILE_COLUMNS, null, null, null);
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
