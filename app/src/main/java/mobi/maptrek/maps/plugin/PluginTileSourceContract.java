package mobi.maptrek.maps.plugin;

/**
 * Plugins must declare a BroadcastReceiver accepting ONLINE_PROVIDER_INTENT
 * or OFFLINE_PROVIDER_INTENT. They must provide the authority of the ContentProvider serving maps
 * in a string resource named "authority".
 *
 * This ContentProvider must accept two types of queries:
 * - "content://authority/maps" must be answered with a list of map descriptions.
 *   Each map is described by columns:
 *   - IDENTIFIER: unique identifier (string)
 *   - NAME: human readable name (string)
 *   - MIN_ZOOM: minimum supported zoom level (integer)
 *   - MAX_ZOOM: maximum supported zoom level (integer)
 *   - LICENSE: license (string, optional)
 *
 * - "content://authority/tiles/mapIdentifier/zoomLevel/tileX/tileY" must be answered with the
 *   the target tile uri or blob
 */
final class PluginTileSourceContract {

    static final String ONLINE_PROVIDER_INTENT  = "mobi.maptrek.maps.online.provider.action.INITIALIZE";

    static final String OFFLINE_PROVIDER_INTENT = "mobi.maptrek.maps.offline.provider.action.INITIALIZE";

    static final String COLUMN_IDENTIFIER = "IDENTIFIER";
    static final String COLUMN_NAME = "NAME";
    static final String COLUMN_LICENSE = "LICENSE";
    static final String COLUMN_MIN_ZOOM = "MIN_ZOOM";
    static final String COLUMN_MAX_ZOOM = "MAX_ZOOM";

    static final String[] MAP_COLUMNS = new String[]{COLUMN_IDENTIFIER, COLUMN_NAME, COLUMN_MIN_ZOOM, COLUMN_MAX_ZOOM, COLUMN_LICENSE};

    static final String[] TILE_COLUMNS = new String[]{"TILE"};
}
