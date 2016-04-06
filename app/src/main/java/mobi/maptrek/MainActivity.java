package mobi.maptrek;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.oscim.android.MapScaleBar;
import org.oscim.android.MapView;
import org.oscim.android.cache.PreCachedTileCache;
import org.oscim.android.canvas.AndroidSvgBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.CombinedTileSource;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.oscim.utils.Osm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import mobi.maptrek.data.DataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.WaypointDataSource;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.fragments.OnWaypointActionListener;
import mobi.maptrek.fragments.TrackProperties;
import mobi.maptrek.fragments.WaypointInformation;
import mobi.maptrek.io.Manager;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.util.ProgressHandler;

public class MainActivity extends Activity implements ILocationListener,
        MapHolder,
        Map.InputListener,
        Map.UpdateListener,
        TrackProperties.OnTrackPropertiesChangedListener,
        OnWaypointActionListener,
        ItemizedLayer.OnItemGestureListener<MarkerItem>,
        PopupMenu.OnMenuItemClickListener,
        LoaderManager.LoaderCallbacks<List<DataSource>> {
    private static final String TAG = "MailActivity";
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    //TODO Put them in separate class
    private static final String PREF_LATITUDE = "latitude";
    private static final String PREF_LONGITUDE = "longitude";
    private static final String PREF_MAP_SCALE = "map_scale";
    private static final String PREF_MAP_BEARING = "map_bearing";
    private static final String PREF_MAP_TILT = "map_tilt";
    private static final String PREF_LOCATION_STATE = "location_state";
    public static final String PREF_TRACKING_STATE = "tracking_state";

    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;

    public enum LOCATION_STATE {
        DISABLED,
        SEARCHING,
        ENABLED,
        NORTH,
        TRACK
    }

    public enum TRACKING_STATE {
        DISABLED,
        PENDING,
        TRACKING
    }

    public enum PANEL_STATE {
        NONE,
        LOCATION,
        RECORD,
        PLACES,
        LAYERS,
        MORE
    }

    private float mFingerTipSize;

    private ProgressHandler mProgressHandler;

    private ILocationService mLocationService = null;
    private boolean mIsBound = false;
    private LOCATION_STATE mLocationState;
    private LOCATION_STATE mSavedLocationState;
    private TRACKING_STATE mTrackingState;
    private MapPosition mMapPosition = new MapPosition();
    private int mTrackingOffset = 0;
    private int mMovingOffset = 0;

    protected Map mMap;
    protected MapView mMapView;
    private TextView mSatellitesText;
    private TextView mSpeedText;
    private ImageButton mLocationButton;
    //TODO Temporary fix
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mPlacesButton;
    private ImageButton mRecordButton;
    private ImageButton mMoreButton;
    private View mCompassView;
    private View mGaugePanelView;
    private ProgressBar mProgressBar;
    private CoordinatorLayout mCoordinatorLayout;

    private long mLastLocationMilliseconds = 0;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    private TileGridLayer mGridLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private LocationOverlay mLocationOverlay;
    private MarkerItem mActiveMarker;

    private PreCachedTileCache mCache;

    //private DataFragment mDataFragment;
    private PANEL_STATE mPanelState;
    private boolean secondBack;
    private Toast mBackToast;

    //private MapIndex mMapIndex;
    //TODO Should we store it here?
    private WaypointDataSource mWaypointDataSource;
    private List<DataSource> mData;
    private Track mEditedTrack;

    private static final boolean BILLBOARDS = true;
    //private MarkerSymbol mFocusMarker;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Estimate finger tip height (0.25 inch is obtained from experiments)
        mFingerTipSize = (float) (metrics.ydpi * 0.25);

        /*
        // find the retained fragment on activity restarts
        FragmentManager fm = getFragmentManager();
        mDataFragment = (DataFragment) fm.findFragmentByTag("data");

        // create the fragment and data the first time
        if (mDataFragment == null) {
            // add the fragment
            mDataFragment = new DataFragment();
            fm.beginTransaction().add(mDataFragment, "data").commit();
            // load the data from the web
            File mapsDir = getExternalFilesDir("maps");
            if (mapsDir != null) {
                mMapIndex = new MapIndex(mapsDir.getAbsolutePath());
                mDataFragment.setMapIndex(mMapIndex);
            }
        } else {
            mMapIndex = mDataFragment.getMapIndex();
        }
        */

        mLocationState = LOCATION_STATE.DISABLED;
        mSavedLocationState = LOCATION_STATE.DISABLED;

        mPanelState = PANEL_STATE.NONE;

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        mPlacesButton = (ImageButton) findViewById(R.id.placesButton);
        mMoreButton = (ImageButton) findViewById(R.id.moreButton);

        mSatellitesText = (TextView) findViewById(R.id.satellites);
        mSpeedText = (TextView) findViewById(R.id.speed);

        mCompassView = findViewById(R.id.compass);
        mGaugePanelView = findViewById(R.id.gaugePanel);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mMapView = (MapView) findViewById(R.id.mapView);
        registerMapView(mMapView);

        Resources resources = getResources();
        Resources.Theme theme = getTheme();
        mNavigationNorthDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_north, theme);
        mNavigationTrackDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_track, theme);
        mMyLocationDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_my_location, theme);
        mLocationSearchingDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_location_searching, theme);

        UrlTileSource urlTileSource = new OSciMap4TileSource();
        SQLiteAssetHelper preCachedDatabaseHelper = new SQLiteAssetHelper(this, "world_map_z7.db", getDir("databases", 0).getAbsolutePath(), null, 1);

        File cacheDir = getExternalCacheDir();
        if (cacheDir != null) {
            mCache = new PreCachedTileCache(this, preCachedDatabaseHelper.getReadableDatabase(), cacheDir.getAbsolutePath(), "tile_cache.db");
            mCache.setCacheSize(512 * (1 << 10));
            urlTileSource.setCache(mCache);
        }
        //new Thread(new TilePreloader(urlTileSource.getDataSource())).start();

        VectorTileLayer baseLayer = new OsmTileLayer(mMap);

        File mapsDir = getExternalFilesDir("maps");
        if (mapsDir != null) {
            MultiMapFileTileSource mapFileSource = new MultiMapFileTileSource(mapsDir.getAbsolutePath());
            CombinedTileSource tileSource = new CombinedTileSource(mapFileSource, urlTileSource);
            baseLayer.setTileSource(tileSource);
        } else {
            baseLayer.setTileSource(urlTileSource);
        }

        mMap.setBaseMap(baseLayer);
        mMap.setTheme(VtmThemes.DEFAULT);

        mGridLayer = new TileGridLayer(mMap);
        mLocationOverlay = new LocationOverlay(mMap);

		/* set initial position on first run */
        MapPosition pos = new MapPosition();
        mMap.getMapPosition(pos);
        if (pos.x == 0.5 && pos.y == 0.5)
            mMap.setMapPosition(55.8194, 37.6676, Math.pow(2, 16));

        Layers layers = mMap.layers();

        //BitmapTileLayer mBitmapLayer = new BitmapTileLayer(mMap, DefaultSources.OPENSTREETMAP.build());
        //mMap.setBaseMap(mBitmapLayer);

        layers.add(new BuildingLayer(mMap, baseLayer));
        layers.add(new LabelLayer(mMap, baseLayer));
        layers.add(new MapScaleBar(mMapView));
        layers.add(mLocationOverlay);
        //layers.add(mGridLayer);

        //noinspection SpellCheckingInspection
        File waypointsFile = new File(getExternalFilesDir("databases"), "waypoints.sqlitedb");
        mWaypointDataSource = new WaypointDataSource(this, waypointsFile);
        //MarkerItem marker = new MarkerItem("Home", "", new GeoPoint(55.813557, 37.645524));

        try {
            Bitmap bitmap = new AndroidSvgBitmap(this, "assets:markers/marker.svg", 70, 70);
            //drawableToBitmap(getResources(), R.drawable.marker_poi);
            MarkerSymbol symbol;
            if (BILLBOARDS)
                symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
            else
                symbol = new MarkerSymbol(bitmap, 0.5f, 0.5f, false);

            //marker.setMarker(new MarkerSymbol(new AndroidBitmap(image), MarkerItem.HotspotPlace.BOTTOM_CENTER));
            //TODO We should not skip initialization on bitmap creation failure
            mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), symbol, this);
            mMap.layers().add(mMarkerLayer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load waypoints
        mWaypointDataSource.open();
        List<Waypoint> waypoints = mWaypointDataSource.getWaypoints();
        for (Waypoint waypoint : waypoints) {
            GeoPoint geoPoint = new GeoPoint(waypoint.latitude, waypoint.longitude);
            MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, geoPoint);
            mMarkerLayer.addItem(marker);
        }

        /*
        android.graphics.Bitmap pin = BitmapFactory.decodeResource(resources, R.mipmap.marker_pin_1);
        android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(pin.getWidth(), pin.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY));

        Canvas bc = new Canvas(image);
        bc.drawBitmap(pin, 0f, 0f, paint);
        */


        //if (BuildConfig.DEBUG)
        //    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

        mBackToast = Toast.makeText(this, R.string.msg_back_quit, Toast.LENGTH_SHORT);
        mProgressHandler = new ProgressHandler(mProgressBar);

        // Initialize UI event handlers
        mLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLocationClicked();
            }
        });
        mLocationButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                onLocationLongClicked();
                return true;
            }
        });
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordClicked();
            }
        });
        mRecordButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                onRecordLongClicked();
                return true;
            }
        });
        mPlacesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlacesClicked();
            }
        });
        mPlacesButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                onPlacesLongClicked();
                return true;
            }
        });

        // Initialize data loader
        getLoaderManager();

        // Remove splash from background
        getWindow().setBackgroundDrawable(new ColorDrawable(getColor(R.color.colorBackground)));

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "New intent: " + action);

        if ("geo".equals(intent.getScheme())) {
            Uri uri = intent.getData();
            String data = uri.getSchemeSpecificPart();
            String query = uri.getQuery();
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom
            // geo:0,0?q=lat,lng(label)
            int zoom = 0;
            if (query != null) {
                data = data.substring(0, data.indexOf(query) - 1);
                if (query.startsWith("z="))
                    try {
                        zoom = Integer.parseInt(query.substring(2));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
            }
            try {
                String[] ll = data.split(",");
                double lat = Double.parseDouble(ll[0]);
                double lon = Double.parseDouble(ll[1]);
                if (lat == 0d && lon == 0d && query != null) {
                    // Parse query string
                    data = query.substring(2, query.indexOf("("));
                    ll = data.split(",");
                    lat = Double.parseDouble(ll[0]);
                    lon = Double.parseDouble(ll[1]);
                    //TODO Show marker (in any case)
                    //noinspection unused
                    String marker = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
                }
                MapPosition position = mMap.getMapPosition();
                position.setPosition(lat, lon);
                if (zoom > 0)
                    position.setZoomLevel(zoom);
                mMap.setMapPosition(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("http".equals(intent.getScheme()) || "https".equals(intent.getScheme())) {
            Uri uri = intent.getData();
            List<String> path = uri.getPathSegments();
            if ("go".equals(path.get(0))) {
                MapPosition position = Osm.decodeShortLink(path.get(1));
                //TODO Show marker (in any case)
                //noinspection unused
                String marker = uri.getQueryParameter("m");
                mMap.setMapPosition(position);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart()");

        registerReceiver(mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_SAVE));
        // Start loading user data
        DataLoader loader = (DataLoader) getLoaderManager().initLoader(0, null, this);
        loader.setProgressHandler(mProgressHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");

        if (mSavedLocationState != LOCATION_STATE.DISABLED)
            askForPermission();
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            enableTracking();
            startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_BACKGROUND_TRACK));
        }

        updateMapViewArea();

        mMap.events.bind(this);
        mMap.input.bind(this);
        mMapView.onResume();
        updateLocationDrawable();
        adjustCompass(mMap.getMapPosition().bearing);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");

        if (mLocationState != LOCATION_STATE.SEARCHING)
            mSavedLocationState = mLocationState;

        mMapView.onPause();
        mMap.events.unbind(this);

        // save the map position
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        MapPosition mapPosition = new MapPosition();
        mMap.viewport().getMapPosition(mapPosition);
        GeoPoint geoPoint = mapPosition.getGeoPoint();
        editor.putInt(PREF_LATITUDE, geoPoint.latitudeE6);
        editor.putInt(PREF_LONGITUDE, geoPoint.longitudeE6);
        editor.putFloat(PREF_MAP_SCALE, (float) mapPosition.scale);
        editor.putFloat(PREF_MAP_BEARING, mapPosition.bearing);
        editor.putFloat(PREF_MAP_TILT, mapPosition.tilt);
        editor.putInt(PREF_LOCATION_STATE, mSavedLocationState.ordinal());
        editor.putInt(PREF_TRACKING_STATE, mTrackingState.ordinal());
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop()");

        unregisterReceiver(mBroadcastReceiver);

        if (isFinishing() && mTrackingState == TRACKING_STATE.TRACKING) {
            startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_BACKGROUND_TRACK));
        }
        if (mLocationService != null)
            disableLocations();

        Loader<List<DataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).setProgressHandler(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        mWaypointDataSource.close();

        //mDataFragment.setMapIndex(mMapIndex);
        mMap.destroy();
        if (mCache != null)
            mCache.dispose();

        mProgressHandler = null;

        /*
        if (this.isFinishing()) {
            mMapIndex.clear();
        }
        */
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "onSaveInstanceState()");
        savedInstanceState.putSerializable("savedLocationState", mSavedLocationState);
        savedInstanceState.putLong("lastLocationMilliseconds", mLastLocationMilliseconds);
        savedInstanceState.putFloat("averagedBearing", mAveragedBearing);
        savedInstanceState.putInt("movementAnimationDuration", mMovementAnimationDuration);
        if (mProgressBar.getVisibility() == View.VISIBLE)
            savedInstanceState.putInt("progressBar", mProgressBar.getMax());
        savedInstanceState.putSerializable("panelState", mPanelState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        mSavedLocationState = (LOCATION_STATE) savedInstanceState.getSerializable("savedLocationState");
        mLastLocationMilliseconds = savedInstanceState.getLong("lastLocationMilliseconds");
        mAveragedBearing = savedInstanceState.getFloat("averagedBearing");
        mMovementAnimationDuration = savedInstanceState.getInt("movementAnimationDuration");
        if (savedInstanceState.containsKey("progressBar")) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setMax(savedInstanceState.getInt("progressBar"));
        }
        setPanelState((PANEL_STATE) savedInstanceState.getSerializable("panelState"));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.theme_default:
                mMap.setTheme(VtmThemes.DEFAULT);
                item.setChecked(true);
                return true;

            case R.id.theme_tubes:
                mMap.setTheme(VtmThemes.TRONRENDER);
                item.setChecked(true);
                return true;

            case R.id.theme_osmarender:
                mMap.setTheme(VtmThemes.OSMARENDER);
                item.setChecked(true);
                return true;

            case R.id.theme_newtron:
                mMap.setTheme(VtmThemes.NEWTRON);
                item.setChecked(true);
                return true;

            case R.id.action_grid:
                if (item.isChecked()) {
                    item.setChecked(false);
                    mMap.layers().remove(mGridLayer);
                } else {
                    item.setChecked(true);
                    mMap.layers().add(mGridLayer);
                }
                mMap.updateMap(true);
                return true;
        }

        return false;
    }

    @Override
    public void onLocationChanged() {
        if (mLocationState == LOCATION_STATE.SEARCHING) {
            mLocationState = mSavedLocationState;
            mMap.getEventLayer().setFixOnCenter(true);
            updateLocationDrawable();
            mLocationOverlay.setEnabled(true);
            mMap.updateMap(true);
        }

        Location location = mLocationService.getLocation();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float bearing = location.getBearing();
        if (bearing < mAveragedBearing - 180)
            mAveragedBearing -= 360;
        mAveragedBearing = (float) movingAverage(bearing, mAveragedBearing);
        if (mAveragedBearing < 0)
            mAveragedBearing += 360;

        updateGauges();

        if (mLocationState == LOCATION_STATE.NORTH || mLocationState == LOCATION_STATE.TRACK) {
            // Adjust map movement animation to location acquisition period to make movement smoother
            long locationDelay = SystemClock.uptimeMillis() - mLastLocationMilliseconds;
            double duration = Math.min(1500, locationDelay); // 1.5 seconds maximum
            mMovementAnimationDuration = (int) movingAverage(duration, mMovementAnimationDuration);
            // Update map position
            mMap.getMapPosition(mMapPosition);

            double offset;
            if (mLocationState == LOCATION_STATE.TRACK) {
                //TODO Recalculate only on tilt change
                offset = mTrackingOffset / Math.cos(Math.toRadians(mMapPosition.tilt) * 0.9);
            } else {
                offset = mMovingOffset;
            }
            offset = offset / (mMapPosition.scale * Tile.SIZE);
            mLocationOverlay.setLocationOffset(offset, mAveragedBearing, mLocationState == LOCATION_STATE.TRACK);

            double rad = Math.toRadians(mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);

            mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
            mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);
            mMapPosition.setBearing(-mAveragedBearing);
            mMap.animator().animateTo(mMovementAnimationDuration, mMapPosition, mLocationState == LOCATION_STATE.TRACK);
        }

        mLocationOverlay.setPosition(lat, lon, bearing, location.getAccuracy());
        mLastLocationMilliseconds = SystemClock.uptimeMillis();
    }

    @Override
    public void onGpsStatusChanged() {
        if (mLocationService.getStatus() == LocationService.GPS_SEARCHING) {
            int satellites = mLocationService.getSatellites();
            mSatellitesText.setText(String.format("%d / %s", satellites >> 7, satellites & 0x7f));
            if (mLocationState != LOCATION_STATE.SEARCHING) {
                mSavedLocationState = mLocationState;
                mLocationState = LOCATION_STATE.SEARCHING;
                mMap.getEventLayer().setFixOnCenter(false);
                mLocationOverlay.setPinned(false);
                mLocationOverlay.setEnabled(false);
                updateLocationDrawable();
            }
        }
    }

    public void onLocationClicked() {
        switch (mLocationState) {
            case DISABLED:
                askForPermission();
                break;
            case SEARCHING:
                mLocationState = LOCATION_STATE.DISABLED;
                disableLocations();
                break;
            case ENABLED:
                mLocationState = LOCATION_STATE.NORTH;
                mMap.getEventLayer().setFixOnCenter(true);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setPosition(mLocationService.getLocation().getLatitude(), mLocationService.getLocation().getLongitude());
                //mMapPosition.setBearing(0);
                mMap.animator().setListener(new org.oscim.map.Animator.MapAnimationListener() {
                    @Override
                    public void onMapAnimationEnd() {
                        Log.e(TAG, "from set North");
                        mLocationOverlay.setPinned(true);
                    }
                }).animateTo(MAP_POSITION_ANIMATION_DURATION, mMapPosition);
                break;
            case NORTH:
                mLocationState = LOCATION_STATE.TRACK;
                mMap.getEventLayer().enableRotation(false);
                mMap.getEventLayer().setFixOnCenter(true);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(-mLocationService.getLocation().getBearing());
                mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
                break;
            case TRACK:
                mLocationState = LOCATION_STATE.ENABLED;
                mMap.getEventLayer().enableRotation(true);
                mMap.getEventLayer().setFixOnCenter(false);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(0);
                mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
                mLocationOverlay.setPinned(false);
                break;
        }
        updateLocationDrawable();
    }

    public void onLocationLongClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble(LocationInformation.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(LocationInformation.ARG_LONGITUDE, mMapPosition.getLongitude());
        showExtendPanel(PANEL_STATE.LOCATION, "locationInformation", LocationInformation.class.getName(), args);
    }

    public void onRecordClicked() {
        if (mLocationState == LOCATION_STATE.DISABLED) {
            mTrackingState = TRACKING_STATE.PENDING;
            askForPermission();
            return;
        }
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            disableTracking();
        } else {
            enableTracking();
        }
    }

    public void onRecordLongClicked() {
        showExtendPanel(PANEL_STATE.RECORD, "trackList", DataSourceList.class.getName(), null);
    }

    public void onPlacesClicked() {
    }

    public void onPlacesLongClicked() {
        MapPosition position = mMap.getMapPosition();
        int size = mMarkerLayer.size();
        //TODO Localize!
        String name = "Place #" + (size + 1);
        Waypoint waypoint = new Waypoint(name, position.getLatitude(), position.getLongitude());
        waypoint.date = new Date();
        mWaypointDataSource.saveWaypoint(waypoint);
        MarkerItem marker = new MarkerItem(waypoint, name, null, position.getGeoPoint());
        mMarkerLayer.addItem(marker);
        mMap.updateMap(true);
    }

    public void onMoreClicked(View view) {
        PopupMenu popup = new PopupMenu(this, mMoreButton);
        mMoreButton.setOnTouchListener(popup.getDragToOpenListener());
        popup.inflate(R.menu.menu_map);
        Menu menu = popup.getMenu();
        menu.findItem(R.id.action_grid).setChecked(mMap.layers().contains(mGridLayer));
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void onCompassClicked(View view) {
        if (mLocationState == LOCATION_STATE.TRACK) {
            mLocationState = LOCATION_STATE.NORTH;
            updateLocationDrawable();
            mMap.getEventLayer().enableRotation(true);
        }
        mMap.getMapPosition(mMapPosition);
        mMapPosition.setBearing(0);
        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        MapPosition position = mMap.getMapPosition();
        Waypoint waypoint = (Waypoint) item.getUid();
        Bundle args = new Bundle(2);
        args.putDouble(WaypointInformation.ARG_LATITUDE, position.getLatitude());
        args.putDouble(WaypointInformation.ARG_LONGITUDE, position.getLongitude());
        WaypointInformation fragment = (WaypointInformation) Fragment.instantiate(this, WaypointInformation.class.getName(), args);
        fragment.setWaypoint(waypoint);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.animator.slide_in_up, R.animator.slide_out_up, R.animator.slide_out_up, R.animator.slide_in_up);
        ft.replace(R.id.contentPanel, fragment, "waypointInformation");
        ft.addToBackStack("waypointInformation");
        ft.commit();
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (mLocationState != LOCATION_STATE.DISABLED && mLocationState != LOCATION_STATE.ENABLED)
            return false;
        mActiveMarker = item;
        // For better experience get delta from marker position and finger press
        // and consider it when moving marker
        MapPosition position = mMap.getMapPosition();
        Point point = new Point();
        mMap.viewport().toScreenPoint(item.getPoint(), point);
        deltaX = (float) (downX - point.x);
        deltaY = (float) (downY - point.y);
        // Shift map to reveal marker tip position
        mMap.viewport().toScreenPoint(position.getGeoPoint(), point);
        point.y = point.y + mFingerTipSize;
        position.setPosition(mMap.viewport().fromScreenPoint((float) point.x, (float) point.y));
        mMap.getEventLayer().enableMove(false);
        mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, position);
        return true;
    }

    private void enableLocations() {
        mIsBound = bindService(new Intent(getApplicationContext(), LocationService.class), mLocationConnection, BIND_AUTO_CREATE);
        mLocationState = LOCATION_STATE.SEARCHING;
        if (mSavedLocationState == LOCATION_STATE.DISABLED)
            mSavedLocationState = LOCATION_STATE.NORTH;
        if (mTrackingState == TRACKING_STATE.PENDING)
            enableTracking();
        updateLocationDrawable();
    }

    private void disableLocations() {
        Log.e(TAG, "disableLocations()");
        if (mLocationService != null) {
            mLocationService.unregisterLocationCallback(this);
            mLocationService.setProgressListener(null);
        }
        if (mIsBound) {
            unbindService(mLocationConnection);
            mIsBound = false;
            mLocationOverlay.setEnabled(false);
            mMap.updateMap(true);
        }
        mLocationState = LOCATION_STATE.DISABLED;
        updateLocationDrawable();
    }

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mLocationService = (ILocationService) binder;
            mLocationService.registerLocationCallback(MainActivity.this);
            mLocationService.setProgressListener(mProgressHandler);
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
        }
    };

    private void enableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK));
        mCurrentTrackLayer = new CurrentTrackLayer(mMap, org.oscim.backend.canvas.Color.fade(getColor(R.color.trackColor), 0.7), getResources().getInteger(R.integer.trackWidth), getApplicationContext());
        mMap.layers().add(mCurrentTrackLayer);
        mMap.updateMap(true);
        mTrackingState = TRACKING_STATE.TRACKING;
        updateLocationDrawable();
    }

    private void disableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_TRACK));
        boolean r = mMap.layers().remove(mCurrentTrackLayer);
        Log.e(TAG, "r: " + r);
        if (mCurrentTrackLayer != null) // Can be null if called by intent
            mCurrentTrackLayer.onDetach();
        mCurrentTrackLayer = null;
        mMap.updateMap(true);
        mTrackingState = TRACKING_STATE.DISABLED;
        updateLocationDrawable();
    }

    private float downX, downY, deltaX, deltaY;

    @Override
    public void onInputEvent(Event e, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = motionEvent.getX() - mMap.getWidth() / 2;
            downY = motionEvent.getY() - mMap.getHeight() / 2;
        }
        if (mActiveMarker == null)
            return;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Update corresponding waypoint
            Waypoint waypoint = (Waypoint) mActiveMarker.getUid();
            waypoint.latitude = mActiveMarker.getPoint().getLatitude();
            waypoint.longitude = mActiveMarker.getPoint().getLongitude();
            mWaypointDataSource.saveWaypoint(waypoint);
            mActiveMarker = null;
            // Unshift map to its original position
            MapPosition position = mMap.getMapPosition();
            Point point = new Point();
            mMap.viewport().toScreenPoint(position.getGeoPoint(), point);
            point.y = point.y - mFingerTipSize;
            position.setPosition(mMap.viewport().fromScreenPoint((float) point.x, (float) point.y));
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, position);
            mMap.getEventLayer().enableMove(true);
        } else if (action == MotionEvent.ACTION_MOVE) {
            float eventX = motionEvent.getX() - deltaX - mMap.getWidth() / 2;
            float eventY = motionEvent.getY() - deltaY - mMap.getHeight() / 2 - mFingerTipSize;
            mActiveMarker.setPoint(mMap.viewport().fromScreenPoint(eventX, eventY));
            mMarkerLayer.updateItems();
            mMap.updateMap(true);
        }
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.MOVE_EVENT) {
            if (mLocationState == LOCATION_STATE.NORTH || mLocationState == LOCATION_STATE.TRACK) {
                mLocationState = LOCATION_STATE.ENABLED;
                mLocationOverlay.setPinned(false);
                updateLocationDrawable();
            }
        }
        adjustCompass(mapPosition.bearing);
        /*
        List<MapFile> maps = mMapIndex.getMaps(mapPosition.getGeoPoint());
        if (maps.isEmpty()) {
            if (mOverlayMapLayer != null) {
                mMap.setBaseMap(mBaseLayer);
                mMap.updateMap(true);
                mOverlayMapLayer.onDetach();
                mOverlayMapLayer = null;
            }
        } else {
            if (mOverlayMapLayer == null) {
                MapFile mapFile = maps.get(0);
                mOverlayMapLayer = new VectorTileLayer(mMap, mapFile.tileSource);
                ((VectorTileLayer) mOverlayMapLayer).setRenderTheme(mBaseLayer.getTheme());
                mMap.setBaseMap(mOverlayMapLayer);
                mMap.updateMap(true);
            }
        }
        */
    }

    public void adjustCompass(float bearing) {
        if (mCompassView.getRotation() == bearing)
            return;
        mCompassView.setRotation(bearing);
        if (Math.abs(bearing) < 1f && mCompassView.getAlpha() == 1f) {
            mCompassView.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCompassView.setVisibility(View.GONE);
                }
            });
        } else if (mCompassView.getVisibility() == View.GONE) {
            mCompassView.setAlpha(0f);
            mCompassView.setVisibility(View.VISIBLE);
            mCompassView.animate().alpha(1f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(null);
        }
    }

    private void updateLocationDrawable() {
        if (mRecordButton.getTag() != mTrackingState) {
            int recordColor = getColor(mTrackingState == TRACKING_STATE.TRACKING ? R.color.colorAccent : R.color.colorPrimaryDark);
            mRecordButton.getDrawable().setTint(recordColor);
            mRecordButton.setTag(mTrackingState);
        }
        if (mLocationButton.getTag() == mLocationState)
            return;
        if (mLocationButton.getTag() == LOCATION_STATE.SEARCHING) {
            mLocationButton.clearAnimation();
            mSatellitesText.animate().translationY(-200);
        }
        switch (mLocationState) {
            case DISABLED:
                mNavigationNorthDrawable.setTint(getColor(R.color.colorPrimaryDark));
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                break;
            case SEARCHING:
                mLocationSearchingDrawable.setTint(getColor(R.color.colorAccent));
                mLocationButton.setImageDrawable(mLocationSearchingDrawable);
                Animation rotation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotation.setInterpolator(new LinearInterpolator());
                rotation.setRepeatCount(Animation.INFINITE);
                rotation.setDuration(1000);
                mLocationButton.startAnimation(rotation);
                if (mGaugePanelView.getVisibility() == View.INVISIBLE) {
                    mSatellitesText.animate().translationY(8);
                } else {
                    mGaugePanelView.animate().translationX(-mGaugePanelView.getWidth()).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (mLocationState == LOCATION_STATE.SEARCHING)
                                mSatellitesText.animate().translationY(8);
                            mGaugePanelView.animate().setListener(null);
                        }
                    });
                }
                break;
            case ENABLED:
                mMyLocationDrawable.setTint(getColor(R.color.colorPrimaryDark));
                mLocationButton.setImageDrawable(mMyLocationDrawable);
                mGaugePanelView.animate().translationX(-mGaugePanelView.getWidth());
                break;
            case NORTH:
                mNavigationNorthDrawable.setTint(getColor(R.color.colorAccent));
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                mGaugePanelView.animate().translationX(0);
                break;
            case TRACK:
                mNavigationTrackDrawable.setTint(getColor(R.color.colorAccent));
                mLocationButton.setImageDrawable(mNavigationTrackDrawable);
                mGaugePanelView.animate().translationX(0);
        }
        mLocationButton.setTag(mLocationState);
    }

    private void updateGauges() {
        Location location = mLocationService.getLocation();
        mSpeedText.setText(String.format("%.0f", location.getSpeed() * 3.6));
    }

    @Override
    public void onWaypointView(Waypoint waypoint) {

    }

    @Override
    public void onWaypointDetails(Waypoint waypoint) {

    }

    @Override
    public void onWaypointNavigate(Waypoint waypoint) {

    }

    @Override
    public void onWaypointShare(Waypoint waypoint) {
        int zoom = mMap.getMapPosition().getZoomLevel();
        String location = waypoint.name + " @ " + waypoint.latitude + " " + waypoint.longitude +
                " <" + Osm.makeShortLink(waypoint.latitude, waypoint.longitude, zoom) + ">";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, location);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_location_intent_title)));
    }

    @Override
    public void onWaypointRemove(final Waypoint waypoint) {
        // Remove marker to indicate action to user
        MarkerItem marker = mMarkerLayer.getByUid(waypoint);
        mMarkerLayer.removeItem(marker);
        mMap.updateMap(true);

        // Show undo snackbar
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msg_waypoint_removed, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoint
                        mWaypointDataSource.deleteWaypoint(waypoint);
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the marker
                        GeoPoint point = new GeoPoint(waypoint.latitude, waypoint.longitude);
                        MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, point);
                        mMarkerLayer.addItem(marker);
                        mMap.updateMap(true);
                   }
                });
        snackbar.show();
    }

    private void onTrackProperties(String path) {
        //TODO Think of better way to find appropriate track
        for (DataSource source : mData) {
            if (source.path.equals(path)) {
                mEditedTrack = source.tracks.get(0);
                break;
            }
        }
        if (mEditedTrack == null)
            return;

        Bundle args = new Bundle(2);
        args.putString(TrackProperties.ARG_NAME, mEditedTrack.name);
        args.putInt(TrackProperties.ARG_COLOR, mEditedTrack.color);
        Fragment fragment = Fragment.instantiate(this, TrackProperties.class.getName(), args);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.animator.fadein, R.animator.fadeout, R.animator.fadeout, R.animator.fadein);
        ft.replace(R.id.contentPanel, fragment, "trackProperties");
        ft.addToBackStack("trackProperties");
        ft.commit();
    }

    @Override
    public void onTrackPropertiesChanged(String name, int color) {
        mEditedTrack.name = name;
        mEditedTrack.color = color;
        for (Layer layer : mMap.layers()) {
            if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(mEditedTrack)) {
                ((TrackLayer) layer).setColor(mEditedTrack.color);
            }
        }
        if (mEditedTrack.source.isSingleTrack())
            mEditedTrack.source.rename(name);

        Manager.save(getApplicationContext(), mEditedTrack.source);
        mEditedTrack = null;
    }

    /**
     * This method is called once by each MapView during its setup process.
     *
     * @param mapView the calling MapView.
     */
    public final void registerMapView(MapView mapView) {
        mMapView = mapView;
        mMap = mapView.map();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(PREF_LATITUDE) &&
                sharedPreferences.contains(PREF_LONGITUDE) &&
                sharedPreferences.contains(PREF_MAP_SCALE)) {
            // retrieve and set the map position and zoom level
            int latitudeE6 = sharedPreferences.getInt(PREF_LATITUDE, 0);
            int longitudeE6 = sharedPreferences.getInt(PREF_LONGITUDE, 0);
            float scale = sharedPreferences.getFloat(PREF_MAP_SCALE, 1);
            float bearing = sharedPreferences.getFloat(PREF_MAP_BEARING, 0);
            float tilt = sharedPreferences.getFloat(PREF_MAP_TILT, 0);

            MapPosition mapPosition = new MapPosition();
            mapPosition.setPosition(latitudeE6 / 1E6, longitudeE6 / 1E6);
            mapPosition.setScale(scale);
            mapPosition.setBearing(bearing);
            mapPosition.setTilt(tilt);

            mMap.setMapPosition(mapPosition);
        }
        int state = sharedPreferences.getInt(PREF_LOCATION_STATE, 0);
        if (state >= LOCATION_STATE.NORTH.ordinal())
            mSavedLocationState = LOCATION_STATE.values()[state];
        state = sharedPreferences.getInt(PREF_TRACKING_STATE, 0);
        mTrackingState = TRACKING_STATE.values()[state];
    }

    private void showExtendPanel(PANEL_STATE panel, String name, String fragmentName, Bundle args) {
        FragmentManager fm = getFragmentManager();

        if (mPanelState != PANEL_STATE.NONE) {
            FragmentManager.BackStackEntry bse = fm.getBackStackEntryAt(0);
            fm.popBackStackImmediate();
            if (name.equals(bse.getName())) {
                setPanelState(PANEL_STATE.NONE);
                return;
            }
        }

        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = Fragment.instantiate(this, fragmentName, args);
        fragment.setEnterTransition(new TransitionSet().addTransition(new Slide(Gravity.BOTTOM)).addTransition(new Visibility() {
            @Override
            public Animator onAppear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelBackground), getColor(R.color.panelSolidBackground));
            }
        }));
        //TODO Find out why exit transition do not work
        /*
        fragment.setExitTransition(new TransitionSet().addTransition(new Slide(Gravity.BOTTOM)).addTransition(new Visibility() {
            @Override
            public Animator onDisappear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                Log.e("MA", "ExitTransaction");
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelSolidBackground), getColor(R.color.panelBackground));
            }
        }));
        */
        ft.replace(R.id.extendPanel, fragment, name);
        ft.addToBackStack(name);
        ft.commit();

        setPanelState(panel);
    }

    private void setPanelState(PANEL_STATE state) {
        if (mPanelState == state)
            return;

        View mLBB = findViewById(R.id.locationButtonBackground);
        View mRBB = findViewById(R.id.recordButtonBackground);
        View mPBB = findViewById(R.id.placesButtonBackground);
        View mOBB = findViewById(R.id.layersButtonBackground);
        View mMBB = findViewById(R.id.moreButtonBackground);

        //TODO Search for view state animation
        // View that gains active state
        final View thisView;
        final ArrayList<View> otherViews = new ArrayList<>();

        if (mPanelState == PANEL_STATE.NONE || state == PANEL_STATE.NONE) {
            otherViews.add(mLBB);
            otherViews.add(mRBB);
            otherViews.add(mPBB);
            otherViews.add(mOBB);
            otherViews.add(mMBB);
        } else {
            // If switching from one view to another animate only that view
            switch (mPanelState) {
                case LOCATION:
                    otherViews.add(mLBB);
                    break;
                case RECORD:
                    otherViews.add(mRBB);
                    break;
                case PLACES:
                    otherViews.add(mPBB);
                    break;
                case LAYERS:
                    otherViews.add(mOBB);
                    break;
                case MORE:
                    otherViews.add(mMBB);
                    break;
            }
        }

        PANEL_STATE thisState = state == PANEL_STATE.NONE ? mPanelState : state;
        switch (thisState) {
            case LOCATION:
                thisView = mLBB;
                break;
            case RECORD:
                thisView = mRBB;
                break;
            case PLACES:
                thisView = mPBB;
                break;
            case LAYERS:
                thisView = mOBB;
                break;
            case MORE:
                thisView = mMBB;
                break;
            default:
                return;
        }
        otherViews.remove(thisView);

        int thisFrom, thisTo, otherFrom, otherTo;
        if (state == PANEL_STATE.NONE) {
            thisFrom = R.color.panelSolidBackground;
            thisTo = R.color.panelBackground;
            otherFrom = R.color.panelExtendedBackground;
            otherTo = R.color.panelBackground;
        } else {
            if (mPanelState == PANEL_STATE.NONE)
                thisFrom = R.color.panelBackground;
            else
                thisFrom = R.color.panelExtendedBackground;
            thisTo = R.color.panelSolidBackground;
            if (mPanelState == PANEL_STATE.NONE)
                otherFrom = R.color.panelBackground;
            else
                otherFrom = R.color.panelSolidBackground;
            otherTo = R.color.panelExtendedBackground;
        }
        ValueAnimator otherColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), getColor(otherFrom), getColor(otherTo));
        ValueAnimator thisColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), getColor(thisFrom), getColor(thisTo));
        thisColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();
                thisView.setBackgroundColor(color);
            }

        });
        otherColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();
                for (View otherView : otherViews)
                    otherView.setBackgroundColor(color);
            }
        });
        AnimatorSet s = new AnimatorSet();
        s.play(thisColorAnimation).with(otherColorAnimation);
        s.start();

        mPanelState = state;
        updateMapViewArea();
    }

    final Handler mBackHandler = new Handler();

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            super.onBackPressed();
            if (count == 1 && mPanelState != PANEL_STATE.NONE)
                setPanelState(PANEL_STATE.NONE);
            return;
        }

        if (count == 0 || secondBack) {
            //mBackToast.cancel();
            finish();
        } else {
            secondBack = true;
            mBackToast.show();
            mBackHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    secondBack = false;
                }
            }, 2000);
        }
    }

    private void updateMapViewArea() {
        Log.e(TAG, "updateMapViewArea()");
        final ViewTreeObserver vto = mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                Log.e(TAG, "onGlobalLayout()");
                Rect area = new Rect();
                mMapView.getLocalVisibleRect(area);
                int mapWidth = area.width();
                int mapHeight = area.height();

                if (mGaugePanelView != null)
                    area.top = mGaugePanelView.getBottom();
                View v = findViewById(R.id.actionPanel);
                if (v != null)
                    area.bottom = v.getTop();
                if (mPanelState != PANEL_STATE.NONE) {
                    v = findViewById(R.id.extendPanel);
                    if (v != null)
                        area.bottom = v.getTop();
                }
                /*
                if (mapLicense.isShown())
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && mapLicense.getRotation() != 0f)
                        area.left = mapLicense.getHeight(); // rotated view does not correctly report it's position
                    else
                        area.bottom = mapLicense.getTop();
                }
                */
                /*
                v = root.findViewById(R.id.rightbar);
                if (v != null)
                    area.right = v.getLeft();
                if (mapButtons.isShown())
                {
                    // Landscape mode
                    if (v != null)
                        area.bottom = mapButtons.getTop();
                    else
                        area.right = mapButtons.getLeft();
                }
                */

                if (!area.isEmpty()) {
                    int pointerOffset = (int) (LocationOverlay.LocationIndicator.POINTER_SIZE * 2);
                    int centerX = mapWidth / 2;
                    int centerY = mapHeight / 2;
                    mMovingOffset = Math.min(centerX - area.left, area.right - centerX);
                    mMovingOffset = Math.min(mMovingOffset, centerY - area.top);
                    mMovingOffset = Math.min(mMovingOffset, area.bottom - centerY);
                    mMovingOffset -= pointerOffset;
                    if (mMovingOffset < 0)
                        mMovingOffset = 0;

                    mTrackingOffset = area.bottom - mapHeight / 2 - pointerOffset;
                    mLocationOverlay.setPinned(false);
                    Log.w(TAG, "TO: " + mTrackingOffset);
                }

                ViewTreeObserver ob;
                if (vto.isAlive())
                    ob = vto;
                else
                    ob = mMapView.getViewTreeObserver();

                ob.removeOnGlobalLayoutListener(this);

                mGaugePanelView.setTranslationX(-mGaugePanelView.getWidth());
                mGaugePanelView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void askForPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        } else {
            enableLocations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocations();
                    //} else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                //return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public Loader<List<DataSource>> onCreateLoader(int id, Bundle args) {
        Log.e(TAG, "onCreateLoader(" + id + ")");
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<DataSource>> loader, List<DataSource> data) {
        Log.e(TAG, "onLoadFinished()");
        if (data == null)
            return;
        mData = data;
        for (DataSource source : mData) {
            for (Track track : source.tracks) {
                if (track.color == -1)
                    track.color = getColor(R.color.trackColor);
                if (track.width == -1)
                    track.width = getResources().getInteger(R.integer.trackWidth);
                for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
                    Layer layer = i.next();
                    if (!(layer instanceof TrackLayer))
                        continue;
                    DataSource src = ((TrackLayer) layer).getTrack().source;
                    if (src == null)
                        continue;
                    if (src.path.equals(source.path)) {
                        i.remove();
                        layer.onDetach();
                    }
                }
                TrackLayer trackLayer = new TrackLayer(mMap, track, track.color, track.width);
                mMap.layers().add(trackLayer);
            }
        }
        Fragment trackList = getFragmentManager().findFragmentByTag("trackList");
        if (trackList != null)
            ((DataSourceList) trackList).initData();
        mMap.updateMap(true);
    }

    @Override
    public void onLoaderReset(Loader<List<DataSource>> loader) {

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast: " + action);
            if (action.equals(BaseLocationService.BROADCAST_TRACK_SAVE)) {
                final Bundle extras = intent.getExtras();
                boolean saved = extras.getBoolean("saved");
                if (saved) {
                    Log.e(TAG, "Track saved: " + extras.getString("path"));
                    Snackbar snackbar = Snackbar
                            .make(mCoordinatorLayout, R.string.msg_track_saved, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_customize, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    onTrackProperties(extras.getString("path"));
                                }
                            });
                    snackbar.show();
                    return;
                }
                String reason = extras.getString("reason");
                Log.e(TAG, "Track not saved: " + reason);
                if ("period".equals(reason) || "distance".equals(reason)) {
                    int msg = "period".equals(reason) ? R.string.msg_track_not_saved_period : R.string.msg_track_not_saved_distance;
                    Snackbar snackbar = Snackbar
                            .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_save, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    mLocationService.saveTrack();
                                }
                            });
                    snackbar.show();
                }
            }
        }
    };

    @Override
    public Map getMap() {
        return mMap;
    }

    public List<DataSource> getData() {
        return mData;
    }

    public void setDataSourceAvailability(DataSource source, boolean available) {
        if (available) {
            if (source.isLoaded()) {
                for (Track track : source.tracks) {
                    if (track.color == -1)
                        track.color = getColor(R.color.trackColor);
                    if (track.width == -1)
                        track.width = getResources().getInteger(R.integer.trackWidth);
                    TrackLayer trackLayer = new TrackLayer(mMap, track, track.color, track.width);
                    mMap.layers().add(trackLayer);
                }
            }
        } else {
            for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
                Layer layer = i.next();
                if (!(layer instanceof TrackLayer))
                    continue;
                DataSource src = ((TrackLayer) layer).getTrack().source;
                if (src == null)
                    continue;
                if (src.equals(source)) {
                    i.remove();
                    layer.onDetach();
                }
            }
        }
        source.setVisible(available);
        Loader<List<DataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null)
            ((DataLoader) loader).markDataSourceLoadable(source, available);
        mMap.updateMap(true);
    }

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }
}