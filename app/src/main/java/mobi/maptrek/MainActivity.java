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
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.Formatter;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.oscim.android.MapScaleBar;
import org.oscim.android.MapView;
import org.oscim.android.cache.PreCachedTileCache;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
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
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.CombinedTileSource;
import org.oscim.tiling.OnDataMissingListener;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.oscim.utils.Osm;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.fragments.About;
import mobi.maptrek.fragments.DataList;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.FragmentHolder;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.fragments.MapList;
import mobi.maptrek.fragments.OnBackPressedListener;
import mobi.maptrek.fragments.OnMapActionListener;
import mobi.maptrek.fragments.OnTrackActionListener;
import mobi.maptrek.fragments.OnWaypointActionListener;
import mobi.maptrek.fragments.PanelMenu;
import mobi.maptrek.fragments.PanelMenuItem;
import mobi.maptrek.fragments.TrackExport;
import mobi.maptrek.fragments.TrackInformation;
import mobi.maptrek.fragments.TrackProperties;
import mobi.maptrek.fragments.WaypointInformation;
import mobi.maptrek.fragments.WaypointProperties;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.NavigationLayer;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.BaseNavigationService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.INavigationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.location.NavigationService;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.view.Gauge;
import mobi.maptrek.view.GaugePanel;

public class MainActivity extends Activity implements ILocationListener,
        DataHolder,
        MapHolder,
        Map.InputListener,
        Map.UpdateListener,
        FragmentHolder,
        WaypointProperties.OnWaypointPropertiesChangedListener,
        TrackProperties.OnTrackPropertiesChangedListener,
        OnWaypointActionListener,
        OnTrackActionListener,
        OnMapActionListener,
        ItemizedLayer.OnItemGestureListener<MarkerItem>,
        PopupMenu.OnMenuItemClickListener,
        LoaderManager.LoaderCallbacks<List<FileDataSource>>,
        FragmentManager.OnBackStackChangedListener,
        OnDataMissingListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;

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
        MAPS,
        MORE
    }

    private float mFingerTipSize;

    private ProgressHandler mProgressHandler;

    private ILocationService mLocationService = null;
    private boolean mIsLocationBound = false;
    private INavigationService mNavigationService = null;
    private boolean mIsNavigationBound = false;
    private LocationState mLocationState;
    private LocationState mSavedLocationState;
    private LocationState mPreviousLocationState;
    private TRACKING_STATE mTrackingState;
    private MapPosition mMapPosition = new MapPosition();
    private int mTrackingOffset = 0;
    private int mMovingOffset = 0;
    private boolean mBuildingsLayerEnabled = true;

    protected Map mMap;
    protected MapView mMapView;
    private GaugePanel mGaugePanel;
    private TextView mSatellitesText;
    private ImageButton mLocationButton;
    private ImageButton mRecordButton;
    //TODO Temporary fix
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mPlacesButton;
    //TODO Temporary fix
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mMapsButton;
    //TODO Temporary fix
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mMoreButton;
    private Button mMapDownloadButton;
    private View mCompassView;
    private View mNavigationArrowView;
    private View mExtendPanel;
    private ProgressBar mProgressBar;
    private FloatingActionButton mActionButton;
    private CoordinatorLayout mCoordinatorLayout;
    private boolean mVerticalOrientation;
    private int mSlideGravity;

    private long mLastLocationMilliseconds = 0;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    private VectorTileLayer mBaseLayer;
    private BuildingLayer mBuildingsLayer;
    private LabelLayer mLabelsLayer;
    private TileGridLayer mGridLayer;
    private NavigationLayer mNavigationLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private LocationOverlay mLocationOverlay;
    private MarkerItem mActiveMarker;

    private PreCachedTileCache mCache;

    private FragmentManager mFragmentManager;
    private DataFragment mDataFragment;
    private PANEL_STATE mPanelState;
    private boolean secondBack;
    private Toast mBackToast;

    private MapIndex mMapIndex;
    private MultiMapFileTileSource mMapFileSource;
    private MapFile mBitmapLayerMap;
    //TODO Should we store it here?
    private WaypointDbDataSource mWaypointDbDataSource;
    private List<FileDataSource> mData = new ArrayList<>();
    private Waypoint mEditedWaypoint;
    private Track mEditedTrack;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler mMainHandler;

    private static final boolean BILLBOARDS = true;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.setPriority(Thread.MIN_PRIORITY);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        Configuration.initialize(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        //FIXME Use preferences
        StringFormatter.speedFactor = 3.6f;
        StringFormatter.speedAbbr = "kmh";

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Estimate finger tip height (0.25 inch is obtained from experiments)
        mFingerTipSize = (float) (metrics.ydpi * 0.25);

        // Apply default styles at start
        TrackStyle.DEFAULT_COLOR = getColor(R.color.trackColor);
        TrackStyle.DEFAULT_WIDTH = getResources().getInteger(R.integer.trackWidth);

        File mapsDir = getExternalFilesDir("maps");

        // find the retained fragment on activity restarts
        mFragmentManager = getFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mDataFragment = (DataFragment) mFragmentManager.findFragmentByTag("data");

        if (mDataFragment == null) {
            // add the fragment
            mDataFragment = new DataFragment();
            mFragmentManager.beginTransaction().add(mDataFragment, "data").commit();

            mMapIndex = new MapIndex(mapsDir);

            //noinspection SpellCheckingInspection
            File waypointsFile = new File(getExternalFilesDir("databases"), "waypoints.sqlitedb");
            mWaypointDbDataSource = new WaypointDbDataSource(this, waypointsFile);

            mBitmapLayerMap = mMapIndex.getMap(Configuration.getBitmapMap());

            if (mapsDir != null)
                mMapFileSource = new MultiMapFileTileSource(mapsDir.getAbsolutePath());
        } else {
            mMapIndex = mDataFragment.getMapIndex();
            mEditedWaypoint = mDataFragment.getEditedWaypoint();
            mWaypointDbDataSource = mDataFragment.getWaypointDbDataSource();
            mBitmapLayerMap = mDataFragment.getBitmapLayerMap();
            mMapFileSource = mDataFragment.getMapFileSource();
        }

        mLocationState = LocationState.DISABLED;
        mSavedLocationState = LocationState.DISABLED;
        mPreviousLocationState = LocationState.NORTH;

        mPanelState = PANEL_STATE.NONE;

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mActionButton = (FloatingActionButton) findViewById(R.id.actionButton);
        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        mPlacesButton = (ImageButton) findViewById(R.id.placesButton);
        mMapsButton = (ImageButton) findViewById(R.id.mapsButton);
        mMoreButton = (ImageButton) findViewById(R.id.moreButton);
        mMapDownloadButton = (Button) findViewById(R.id.mapDownloadButton);

        mGaugePanel = (GaugePanel) findViewById(R.id.gaugePanel);
        mGaugePanel.setTag(Boolean.TRUE);
        mGaugePanel.setMapHolder(this);

        mSatellitesText = (TextView) findViewById(R.id.satellites);
        mCompassView = findViewById(R.id.compass);
        mNavigationArrowView = findViewById(R.id.navigationArrow);
        mNavigationArrowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showNavigationMenu();
                return true;
            }
        });
        mExtendPanel = findViewById(R.id.extendPanel);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mMapView = (MapView) findViewById(R.id.mapView);
        mMap = mMapView.map();
        MapPosition mapPosition = Configuration.getPosition();
        mMap.setMapPosition(mapPosition);

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

        mBaseLayer = new OsmTileLayer(mMap);

        if (mMapFileSource != null) {
            CombinedTileSource tileSource = new CombinedTileSource(mMapFileSource, urlTileSource);
            tileSource.setOnDataMissingListener(this);
            mBaseLayer.setTileSource(tileSource);
        } else {
            mBaseLayer.setTileSource(urlTileSource);
        }

        mMap.setBaseMap(mBaseLayer);
        mMap.setTheme(VtmThemes.DEFAULT);

        mGridLayer = new TileGridLayer(mMap);
        mLocationOverlay = new LocationOverlay(mMap);

		/* set initial position on first run */
        MapPosition pos = new MapPosition();
        mMap.getMapPosition(pos);
        if (pos.x == 0.5 && pos.y == 0.5)
            // TODO Try to guess user location
            mMap.setMapPosition(55.8194, 37.6676, 1 << 16);

        Layers layers = mMap.layers();

        mBuildingsLayerEnabled = Configuration.getBuildingsLayerEnabled();
        if (mBuildingsLayerEnabled) {
            mBuildingsLayer = new BuildingLayer(mMap, mBaseLayer);
            layers.add(mBuildingsLayer);
        }
        mLabelsLayer = new LabelLayer(mMap, mBaseLayer);
        layers.add(mLabelsLayer);
        //layers.add(new MapCoverageLayer(mMap));
        layers.add(new MapScaleBar(mMapView));
        layers.add(mLocationOverlay);

        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this));
        MarkerSymbol symbol;
        if (BILLBOARDS)
            symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
        else
            symbol = new MarkerSymbol(bitmap, 0.5f, 0.5f, false);

        mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), symbol, this);
        mMap.layers().add(mMarkerLayer);

        // Load waypoints
        mWaypointDbDataSource.open();
        List<Waypoint> waypoints = mWaypointDbDataSource.getWaypoints();
        for (Waypoint waypoint : waypoints) {
            if (mEditedWaypoint != null && mEditedWaypoint._id == waypoint._id)
                mEditedWaypoint = waypoint;
            addWaypointMarker(waypoint);
        }

        if (mBitmapLayerMap != null)
            showBitmapMap(mBitmapLayerMap);

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
        mMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapsClicked();
            }
        });
        mMapsButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                onMapsLongClicked();
                return true;
            }
        });
        mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMoreClicked();
            }
        });
        mMapDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapDownloadClicked();
            }
        });

        // Resume state
        int state = Configuration.getLocationState();
        if (state >= LocationState.NORTH.ordinal())
            mSavedLocationState = LocationState.values()[state];
        state = Configuration.getPreviousLocationState();
        mPreviousLocationState = LocationState.values()[state];
        state = Configuration.getTrackingState();
        mTrackingState = TRACKING_STATE.values()[state];

        mGaugePanel.initializeGauges(Configuration.getGauges());

        // Resume navigation
        MapObject mapObject = Configuration.getNavigationPoint();
        if (mapObject != null)
            startNavigation(mapObject);

        // Initialize data loader
        getLoaderManager();

        // Remove splash from background
        getWindow().setBackgroundDrawable(new ColorDrawable(getColor(R.color.colorBackground)));

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "New intent: " + action);
        String scheme = intent.getScheme();

        if ("geo".equals(scheme)) {
            Uri uri = intent.getData();
            String data = uri.getSchemeSpecificPart();
            String query = uri.getQuery();
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom
            // geo:0,0?q=lat,lng(label)
            // geo:0,0?q=lat, lng - buggy Instagram (with space)
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
                    int bracket = query.indexOf("(");
                    if (bracket > -1)
                        data = query.substring(2, query.indexOf("("));
                    else
                        data = query.substring(2);
                    ll = data.split(",\\s*");
                    lat = Double.parseDouble(ll[0]);
                    lon = Double.parseDouble(ll[1]);
                    //TODO Show marker (in any case)
                    if (bracket > -1) {
                        //noinspection unused
                        String marker = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
                    }
                }
                MapPosition position = mMap.getMapPosition();
                position.setPosition(lat, lon);
                if (zoom > 0)
                    position.setZoomLevel(zoom);
                mMap.setMapPosition(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
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

        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATUS));
        registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");

        if (mSavedLocationState != LocationState.DISABLED)
            askForPermission();
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            enableTracking();
            startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_BACKGROUND_TRACK));
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(NavigationService.PREF_NAVIGATION_BACKGROUND, false)) {
            startService(new Intent(getApplicationContext(), NavigationService.class).setAction(BaseNavigationService.DISABLE_BACKGROUND_NAVIGATION));
            enableNavigation();
        }

        mVerticalOrientation = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        mSlideGravity = mVerticalOrientation ? Gravity.BOTTOM : Gravity.END;
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

        if (mLocationState != LocationState.SEARCHING)
            mSavedLocationState = mLocationState;

        mMapView.onPause();
        mMap.events.unbind(this);

        // save the map position and state
        Configuration.setPosition(mMap.getMapPosition());
        Configuration.setBitmapMap(mBitmapLayerMap);
        Configuration.setLocationState(mSavedLocationState.ordinal());
        Configuration.setPreviousLocationState(mPreviousLocationState.ordinal());
        Configuration.setTrackingState(mTrackingState.ordinal());
        Configuration.setGauges(mGaugePanel.getGaugeSettings());
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

        if (mNavigationService != null) {
            if (isFinishing() && mNavigationService.isNavigating()) {
                startService(new Intent(getApplicationContext(), NavigationService.class).setAction(BaseNavigationService.ENABLE_BACKGROUND_NAVIGATION));
            }
            disableNavigation();
        }

        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).setProgressHandler(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        mMap.destroy();
        if (mCache != null)
            mCache.dispose();

        for (FileDataSource source : mData)
            source.setVisible(false);

        mProgressHandler = null;

        Log.w(TAG, "  stopping threads...");
        mBackgroundThread.interrupt();
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mBackgroundThread.quit();
        mBackgroundThread = null;

        mMainHandler = null;

        if (isFinishing()) {
            mMapIndex.clear();
            mWaypointDbDataSource.close();
        }

        mFragmentManager = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "onSaveInstanceState()");

        mDataFragment.setMapIndex(mMapIndex);
        mDataFragment.setEditedWaypoint(mEditedWaypoint);
        mDataFragment.setWaypointDbDataSource(mWaypointDbDataSource);
        mDataFragment.setBitmapLayerMap(mBitmapLayerMap);
        mDataFragment.setMapFileSource(mMapFileSource);

        savedInstanceState.putSerializable("savedLocationState", mSavedLocationState);
        savedInstanceState.putSerializable("previousLocationState", mPreviousLocationState);
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
        mSavedLocationState = (LocationState) savedInstanceState.getSerializable("savedLocationState");
        mPreviousLocationState = (LocationState) savedInstanceState.getSerializable("previousLocationState");
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
                return true;

            case R.id.theme_tubes:
                mMap.setTheme(VtmThemes.TRONRENDER);
                return true;

            case R.id.theme_osmarender:
                mMap.setTheme(VtmThemes.OSMARENDER);
                return true;

            case R.id.theme_newtron:
                mMap.setTheme(VtmThemes.NEWTRON);
                return true;

            case R.id.action_3dbuildings:
                mBuildingsLayerEnabled = item.isChecked();
                if (mBuildingsLayerEnabled) {
                    mBuildingsLayer = new BuildingLayer(mMap, mBaseLayer);
                    mMap.layers().add(mBuildingsLayer);
                    // Buildings should be fetched from base layer
                    mMap.clearMap();
                } else {
                    mMap.layers().remove(mBuildingsLayer);
                    mBuildingsLayer = null;
                }
                Configuration.setBuildingsLayerEnabled(mBuildingsLayerEnabled);
                mMap.updateMap(true);
                return true;
            case R.id.action_grid:
                if (item.isChecked()) {
                    mMap.layers().add(mGridLayer);
                } else {
                    mMap.layers().remove(mGridLayer);
                }
                mMap.updateMap(true);
                return true;

            case R.id.actionStopNavigation:
                stopNavigation();
                return true;

            case R.id.action_reset_advices:
                Configuration.resetAdviceState();
                Snackbar snackbar = Snackbar.make(mCoordinatorLayout, R.string.msg_advices_reset, Snackbar.LENGTH_LONG);
                snackbar.show();
                return true;

            case R.id.action_about:
                Fragment fragment = Fragment.instantiate(this, About.class.getName());
                fragment.setEnterTransition(new Slide(mSlideGravity));
                fragment.setReturnTransition(new Slide(mSlideGravity));
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "about");
                ft.addToBackStack("about");
                ft.commit();
                return true;
        }

        return false;
    }

    @Override
    public void onLocationChanged() {
        if (mLocationState == LocationState.SEARCHING) {
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
        if (bearing < mAveragedBearing - 180f)
            mAveragedBearing -= 360f;
        else if (mAveragedBearing < bearing - 180f)
            mAveragedBearing += 360f;
        mAveragedBearing = (float) movingAverage(bearing, mAveragedBearing);
        if (mAveragedBearing < 0f)
            mAveragedBearing += 360f;
        if (mAveragedBearing >= 360f)
            mAveragedBearing -= 360f;

        updateGauges();

        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            // Adjust map movement animation to location acquisition period to make movement smoother
            long locationDelay = SystemClock.uptimeMillis() - mLastLocationMilliseconds;
            double duration = Math.min(1500, locationDelay); // 1.5 seconds maximum
            mMovementAnimationDuration = (int) movingAverage(duration, mMovementAnimationDuration);
            // Update map position
            mMap.getMapPosition(mMapPosition);

            double offset;
            if (mLocationState == LocationState.TRACK) {
                //TODO Recalculate only on tilt change
                offset = mTrackingOffset / Math.cos(Math.toRadians(mMapPosition.tilt) * 0.9);
            } else {
                offset = mMovingOffset;
            }
            offset = offset / (mMapPosition.scale * Tile.SIZE);
            mLocationOverlay.setLocationOffset(offset, mAveragedBearing, mLocationState == LocationState.TRACK);

            double rad = Math.toRadians(mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);

            mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
            mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);
            mMapPosition.setBearing(-mAveragedBearing);
            mMap.animator().animateTo(mMovementAnimationDuration, mMapPosition, mLocationState == LocationState.TRACK);
        }

        mLocationOverlay.setPosition(lat, lon, bearing, location.getAccuracy());
        if (mNavigationLayer != null)
            mNavigationLayer.setPosition(lat, lon);
        mLastLocationMilliseconds = SystemClock.uptimeMillis();
    }

    @Override
    public void onGpsStatusChanged() {
        if (mLocationService.getStatus() == LocationService.GPS_SEARCHING) {
            int satellites = mLocationService.getSatellites();
            mSatellitesText.setText(String.format(Locale.getDefault(), "%d / %s", satellites >> 7, satellites & 0x7f));
            if (mLocationState != LocationState.SEARCHING) {
                mSavedLocationState = mLocationState;
                mLocationState = LocationState.SEARCHING;
                mMap.getEventLayer().setFixOnCenter(false);
                mLocationOverlay.setPinned(false);
                mLocationOverlay.setEnabled(false);
                updateLocationDrawable();
            }
        }
        updateNavigationUI();
    }

    private void onLocationClicked() {
        switch (mLocationState) {
            case DISABLED:
                askForPermission();
                break;
            case SEARCHING:
                mLocationState = LocationState.DISABLED;
                disableLocations();
                break;
            case ENABLED:
                mLocationState = mPreviousLocationState;
                mPreviousLocationState = LocationState.NORTH;
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
                mLocationState = LocationState.TRACK;
                mMap.getEventLayer().enableRotation(false);
                mMap.getEventLayer().setFixOnCenter(true);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(-mLocationService.getLocation().getBearing());
                mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
                break;
            case TRACK:
                mLocationState = LocationState.ENABLED;
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

    private void onLocationLongClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble(LocationInformation.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(LocationInformation.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(LocationInformation.ARG_ZOOM, mMapPosition.getZoomLevel());
        Fragment fragment = Fragment.instantiate(this, LocationInformation.class.getName(), args);
        showExtendPanel(PANEL_STATE.LOCATION, "locationInformation", fragment);
    }

    private void onRecordClicked() {
        if (mLocationState == LocationState.DISABLED) {
            mTrackingState = TRACKING_STATE.PENDING;
            askForPermission();
            return;
        }
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            Track currentTrack = mCurrentTrackLayer.getTrack();
            if (currentTrack.points.size() == 0)
                disableTracking();
            else
                onTrackDetails(currentTrack, true);
        } else {
            enableTracking();
        }
    }

    private void onRecordLongClicked() {
        Bundle args = new Bundle(1);
        args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, true);
        Fragment fragment = Fragment.instantiate(this, DataSourceList.class.getName(), args);
        showExtendPanel(PANEL_STATE.RECORD, "nativeTrackList", fragment);
    }

    private void onPlacesClicked() {
        boolean hasExtraSources = false;
        for (FileDataSource source : mData) {
            if (!source.isNativeTrack()) {
                hasExtraSources = true;
                break;
            }
        }
        if (hasExtraSources) {
            Bundle args = new Bundle(1);
            args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, false);
            Fragment fragment = Fragment.instantiate(this, DataSourceList.class.getName(), args);
            showExtendPanel(PANEL_STATE.PLACES, "dataSourceList", fragment);
        } else {
            mMap.getMapPosition(mMapPosition);
            Bundle args = new Bundle(2);
            args.putDouble(DataList.ARG_LATITUDE, mMapPosition.getLatitude());
            args.putDouble(DataList.ARG_LONGITUDE, mMapPosition.getLongitude());
            args.putBoolean(DataList.ARG_EMPTY_MESSAGE, true);
            DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
            fragment.setDataSource(mWaypointDbDataSource);
            showExtendPanel(PANEL_STATE.PLACES, "dataList", fragment);
        }
    }

    private void onPlacesLongClicked() {
        GeoPoint geoPoint;
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            Point point = mLocationOverlay.getPosition();
            geoPoint = new GeoPoint(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
        } else {
            geoPoint = mMap.getMapPosition().getGeoPoint();
        }
        String name = getString(R.string.waypoint_name, Configuration.getPointsCounter());
        final Waypoint waypoint = new Waypoint(name, geoPoint.getLatitude(), geoPoint.getLongitude());
        waypoint.date = new Date();
        mWaypointDbDataSource.saveWaypoint(waypoint);
        MarkerItem marker = new MarkerItem(waypoint, name, null, geoPoint);
        mMarkerLayer.addItem(marker);
        mMap.updateMap(true);
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msg_waypoint_saved, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_customize, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onWaypointProperties(waypoint);
                    }
                });
        snackbar.show();
    }

    private void onMapsClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble(MapList.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(MapList.ARG_LONGITUDE, mMapPosition.getLongitude());
        MapList fragment = (MapList) Fragment.instantiate(this, MapList.class.getName(), args);
        fragment.setMaps(mMapIndex.getMaps(), mBitmapLayerMap);
        showExtendPanel(PANEL_STATE.MAPS, "mapsList", fragment);
    }

    private void onMapsLongClicked() {
        PanelMenu fragment = (PanelMenu) Fragment.instantiate(this, PanelMenu.class.getName());
        fragment.setMenu(R.menu.menu_map, new PanelMenu.OnPrepareMenuListener() {
            @Override
            public void onPrepareMenu(List<PanelMenuItem> menu) {
                for (PanelMenuItem item : menu) {
                    switch (item.getItemId()) {
                        case R.id.action_3dbuildings:
                            item.setChecked(mBuildingsLayerEnabled);
                            break;
                        case R.id.action_grid:
                            item.setChecked(mMap.layers().contains(mGridLayer));
                            break;
                    }
                }
            }
        });
        showExtendPanel(PANEL_STATE.MAPS, "mapMenu", fragment);
    }

    private void onMoreClicked() {
        PanelMenu fragment = (PanelMenu) Fragment.instantiate(this, PanelMenu.class.getName());
        fragment.setMenu(R.menu.menu_main, new PanelMenu.OnPrepareMenuListener() {
            @Override
            public void onPrepareMenu(List<PanelMenuItem> menu) {
                for (PanelMenuItem item : menu) {
                    switch (item.getItemId()) {
                        case R.id.action_grid:
                            item.setChecked(mMap.layers().contains(mGridLayer));
                    }
                }
            }
        });
        showExtendPanel(PANEL_STATE.MORE, "panelMenu", fragment);
    }

    private void onMapDownloadClicked() {
        MapFile mapFile = (MapFile) mMapDownloadButton.getTag();
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(mapFile.downloadPath);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(getString(R.string.mapTitle, mapFile.name));
        request.setDescription(getString(R.string.app_name));
        request.setDestinationInExternalFilesDir(this, "maps", mapFile.fileName);
        request.setVisibleInDownloadsUi(false);
        mapFile.downloading = downloadManager.enqueue(request);
        mMapDownloadButton.setVisibility(View.GONE);
        mMapDownloadButton.setTag(null);
    }

    public void onCompassClicked(View view) {
        if (mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.NORTH;
            updateLocationDrawable();
            mMap.getEventLayer().enableRotation(true);
        }
        mMap.getMapPosition(mMapPosition);
        mMapPosition.setBearing(0);
        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        Waypoint waypoint = (Waypoint) item.getUid();
        onWaypointDetails(waypoint, false);
        return true;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (mLocationState != LocationState.DISABLED && mLocationState != LocationState.ENABLED)
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
        mIsLocationBound = bindService(new Intent(getApplicationContext(), LocationService.class), mLocationConnection, BIND_AUTO_CREATE);
        mLocationState = LocationState.SEARCHING;
        if (mSavedLocationState == LocationState.DISABLED) {
            mSavedLocationState = mPreviousLocationState;
            mPreviousLocationState = LocationState.NORTH;
        }
        if (mTrackingState == TRACKING_STATE.PENDING)
            enableTracking();
        updateLocationDrawable();
    }

    @Override
    public void disableLocations() {
        if (mLocationService != null) {
            mLocationService.unregisterLocationCallback(this);
            mLocationService.setProgressListener(null);
        }
        if (mIsLocationBound) {
            unbindService(mLocationConnection);
            mIsLocationBound = false;
            mLocationOverlay.setEnabled(false);
            mMap.updateMap(true);
        }
        mLocationState = LocationState.DISABLED;
        updateLocationDrawable();
    }

    @Override
    public void setMapLocation(GeoPoint point) {
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            mLocationOverlay.setPinned(false);
            updateLocationDrawable();
        }
        mMap.animator().animateTo(point);
    }

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mLocationService = (ILocationService) binder;
            mLocationService.registerLocationCallback(MainActivity.this);
            mLocationService.setProgressListener(mProgressHandler);
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
            updateNavigationUI();
        }
    };

    private void enableNavigation() {
        mIsNavigationBound = bindService(new Intent(getApplicationContext(), NavigationService.class), mNavigationConnection, BIND_AUTO_CREATE);
    }

    private void disableNavigation() {
        if (mIsNavigationBound) {
            unbindService(mNavigationConnection);
            mIsNavigationBound = false;
        }
        updateNavigationUI();
    }

    private ServiceConnection mNavigationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mNavigationService = (INavigationService) binder;
            updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            mNavigationService = null;
            updateNavigationUI();
        }
    };

    private void startNavigation(MapObject mapObject) {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_MAP_OBJECT);
        i.putExtra(NavigationService.EXTRA_NAME, mapObject.name);
        i.putExtra(NavigationService.EXTRA_LATITUDE, mapObject.latitude);
        i.putExtra(NavigationService.EXTRA_LONGITUDE, mapObject.longitude);
        i.putExtra(NavigationService.EXTRA_PROXIMITY, mapObject.proximity);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission();
    }

    public void stopNavigation() {
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.STOP_NAVIGATION);
        startService(i);
    }

    private void enableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK));
        mCurrentTrackLayer = new CurrentTrackLayer(mMap, getApplicationContext());
        mMap.layers().add(mCurrentTrackLayer);
        mMap.updateMap(true);
        mTrackingState = TRACKING_STATE.TRACKING;
        updateLocationDrawable();
    }

    @Override
    public void disableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_TRACK));
        mMap.layers().remove(mCurrentTrackLayer);
        if (mCurrentTrackLayer != null) // Can be null if called by intent
            mCurrentTrackLayer.onDetach();
        mCurrentTrackLayer = null;
        mMap.updateMap(true);
        mTrackingState = TRACKING_STATE.DISABLED;
        updateLocationDrawable();
    }

    private final Set<WeakReference<LocationStateChangeListener>> mLocationStateChangeListeners = new HashSet<>();

    @Override
    public void addLocationStateChangeListener(LocationStateChangeListener listener) {
        mLocationStateChangeListeners.add(new WeakReference<>(listener));
        listener.onLocationStateChanged(mLocationState);
    }

    @Override
    public void removeLocationStateChangeListener(LocationStateChangeListener listener) {
        for (Iterator<WeakReference<LocationStateChangeListener>> iterator = mLocationStateChangeListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<LocationStateChangeListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
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
            onWaypointSave(waypoint);
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
            if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                mPreviousLocationState = mLocationState;
                mLocationState = LocationState.ENABLED;
                mLocationOverlay.setPinned(false);
                updateLocationDrawable();
            }
        }
        adjustCompass(mapPosition.bearing);
        if (mMapDownloadButton.getVisibility() != View.GONE) {
            if (mapPosition.zoomLevel < 8) {
                mMapDownloadButton.setVisibility(View.GONE);
                mMapDownloadButton.setTag(null);
            } else if (e == Map.MOVE_EVENT) {
                final Message m = Message.obtain(mMainHandler, new Runnable() {
                    @Override
                    public void run() {
                        mMapDownloadButton.setVisibility(View.GONE);
                        mMapDownloadButton.setTag(null);
                    }
                });
                m.what = R.id.msgRemoveMapDownloadButton;
                mMainHandler.sendMessageDelayed(m, 3000);
            }
        }
    }

    private void adjustCompass(float bearing) {
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

    private void adjustNavigationArrow(float turn) {
        if (mNavigationArrowView.getRotation() == turn)
            return;
        mNavigationArrowView.setRotation(turn);
    }

    private void showNavigationMenu() {
        PopupMenu popup = new PopupMenu(this, mNavigationArrowView);
        Menu menu = popup.getMenu();
        menu.add(0, R.id.actionStopNavigation, Menu.NONE, getString(R.string.action_stop_navigation));
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private void updateLocationDrawable() {
        if (mRecordButton.getTag() != mTrackingState) {
            int recordColor = getColor(mTrackingState == TRACKING_STATE.TRACKING ? R.color.colorAccent : R.color.colorPrimaryDark);
            mRecordButton.getDrawable().setTint(recordColor);
            mRecordButton.setTag(mTrackingState);
        }
        if (mLocationButton.getTag() == mLocationState)
            return;
        if (mLocationButton.getTag() == LocationState.SEARCHING) {
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
                if (mGaugePanel.getVisibility() == View.INVISIBLE) {
                    mSatellitesText.animate().translationY(8);
                } else {
                    mGaugePanel.animate().translationX(-mGaugePanel.getWidth()).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (mLocationState == LocationState.SEARCHING)
                                mSatellitesText.animate().translationY(8);
                            mGaugePanel.animate().setListener(null);
                        }
                    });
                }
                break;
            case ENABLED:
                mMyLocationDrawable.setTint(getColor(R.color.colorPrimaryDark));
                mLocationButton.setImageDrawable(mMyLocationDrawable);
                mGaugePanel.animate().translationX(-mGaugePanel.getWidth());
                break;
            case NORTH:
                mNavigationNorthDrawable.setTint(getColor(R.color.colorAccent));
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                mGaugePanel.animate().translationX(0);
                break;
            case TRACK:
                mNavigationTrackDrawable.setTint(getColor(R.color.colorAccent));
                mLocationButton.setImageDrawable(mNavigationTrackDrawable);
                mGaugePanel.animate().translationX(0);
        }
        mLocationButton.setTag(mLocationState);
        for (WeakReference<LocationStateChangeListener> weakRef : mLocationStateChangeListeners) {
            LocationStateChangeListener locationStateChangeListener = weakRef.get();
            if (locationStateChangeListener != null) {
                locationStateChangeListener.onLocationStateChanged(mLocationState);
            }
        }

    }

    private void updateGauges() {
        Location location = mLocationService.getLocation();
        mGaugePanel.setValue(Gauge.TYPE_SPEED, location.getSpeed());
        mGaugePanel.setValue(Gauge.TYPE_TRACK, (int) location.getBearing());
        mGaugePanel.setValue(Gauge.TYPE_ALTITUDE, (int) location.getAltitude());
    }

    private void updateNavigationUI() {
        boolean enabled = mLocationService != null && mLocationService.getStatus() == BaseLocationService.GPS_OK &&
                mNavigationService != null && mNavigationService.isNavigating();
        mGaugePanel.setNavigationMode(enabled);
        if (enabled) {
            if (mNavigationArrowView.getVisibility() == View.GONE) {
                mNavigationArrowView.setAlpha(0f);
                mNavigationArrowView.setVisibility(View.VISIBLE);
                mNavigationArrowView.animate().alpha(1f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(null);
            }
            MapObject mapObject = mNavigationService.getWaypoint();
            GeoPoint destination = new GeoPoint(mapObject.latitude, mapObject.longitude);
            if (mNavigationLayer == null) {
                mNavigationLayer = new NavigationLayer(mMap, 0x66ffff00, 8);
                mNavigationLayer.setDestination(destination);
                Point point = mLocationOverlay.getPosition();
                mNavigationLayer.setPosition(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
                mMap.layers().add(mNavigationLayer);
            } else {
                GeoPoint current = mNavigationLayer.getDestination();
                if (mapObject.latitude != current.getLatitude() || mapObject.longitude != current.getLongitude()) {
                    mNavigationLayer.setDestination(destination);
                }
            }
        } else {
            if (mNavigationArrowView.getAlpha() == 1f) {
                mNavigationArrowView.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mNavigationArrowView.setVisibility(View.GONE);
                    }
                });
            }
            if (mNavigationLayer != null) {
                mMap.layers().remove(mNavigationLayer);
                mNavigationLayer = null;
            }
        }
    }

    private void onWaypointProperties(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
        Bundle args = new Bundle(2);
        args.putString(WaypointProperties.ARG_NAME, mEditedWaypoint.name);
        args.putInt(WaypointProperties.ARG_COLOR, mEditedWaypoint.style.color);
        Fragment fragment = Fragment.instantiate(this, WaypointProperties.class.getName(), args);
        fragment.setEnterTransition(new Fade());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "waypointProperties");
        ft.addToBackStack("waypointProperties");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onWaypointView(Waypoint waypoint) {
        //TODO Make Waypoint inherit GeoPoint
        setMapLocation(new GeoPoint(waypoint.latitude, waypoint.longitude));
    }

    @Override
    public void onWaypointDetails(Waypoint waypoint, boolean full) {
        MapPosition position = mMap.getMapPosition();
        Bundle args = new Bundle(2);
        args.putBoolean(WaypointInformation.ARG_DETAILS, full);
        args.putDouble(WaypointInformation.ARG_LATITUDE, position.getLatitude());
        args.putDouble(WaypointInformation.ARG_LONGITUDE, position.getLongitude());

        Fragment fragment = mFragmentManager.findFragmentByTag("waypointInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, WaypointInformation.class.getName(), args);
            Slide slide = new Slide(Gravity.BOTTOM);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "waypointInformation");
            ft.addToBackStack("waypointInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((WaypointInformation) fragment).setWaypoint(waypoint);
        mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(mExtendPanel.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    @Override
    public void onWaypointNavigate(Waypoint waypoint) {
        startNavigation(waypoint);
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
    public void onWaypointSave(final Waypoint waypoint) {
        if (waypoint.source instanceof WaypointDbDataSource) {
            mWaypointDbDataSource.saveWaypoint(waypoint);
        } else {
            Manager.save(getApplicationContext(), (FileDataSource) waypoint.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            waypoint.source.notifyListeners();
                        }
                    });
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }, mProgressHandler);
        }
        // Markers are immutable so simply recreate it
        removeWaypointMarker(waypoint);
        addWaypointMarker(waypoint);
        mMap.updateMap(true);
    }

    @Override
    public void onWaypointDelete(final Waypoint waypoint) {
        // Remove marker to indicate action to user
        removeWaypointMarker(waypoint);
        mMap.updateMap(true);

        // Show undo snackbar
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msg_waypoint_deleted, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoint
                        if (waypoint.source instanceof WaypointDbDataSource) {
                            mWaypointDbDataSource.deleteWaypoint(waypoint);
                        } else {
                            ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                            Manager.save(getApplicationContext(), (FileDataSource) waypoint.source, new Manager.OnSaveListener() {
                                @Override
                                public void onSaved(FileDataSource source) {
                                    mMainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            waypoint.source.notifyListeners();
                                        }
                                    });
                                }

                                @Override
                                public void onError(FileDataSource source, Exception e) {
                                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                                }
                            }, mProgressHandler);
                        }
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the marker
                        addWaypointMarker(waypoint);
                        mMap.updateMap(true);
                    }
                });
        snackbar.show();
    }

    @Override
    public void onWaypointsDelete(final Set<Waypoint> waypoints) {
        // Remove markers to indicate action to user
        for (Waypoint waypoint : waypoints) {
            removeWaypointMarker(waypoint);
        }
        mMap.updateMap(true);

        // Show undo snackbar
        int count = waypoints.size();
        String msg = getResources().getQuantityString(R.plurals.waypointsDeleted, count, count);
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoints
                        HashSet<FileDataSource> sources = new HashSet<>();
                        for (Waypoint waypoint : waypoints) {
                            if (waypoint.source instanceof WaypointDbDataSource) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            } else {
                                ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                                sources.add((FileDataSource) waypoint.source);
                            }
                        }
                        for (FileDataSource source : sources) {
                            Manager.save(getApplicationContext(), source, new Manager.OnSaveListener() {
                                @Override
                                public void onSaved(final FileDataSource source) {
                                    mMainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            source.notifyListeners();
                                        }
                                    });
                                }

                                @Override
                                public void onError(FileDataSource source, Exception e) {
                                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                                }
                            }, mProgressHandler);
                        }
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the marker
                        for (Waypoint waypoint : waypoints) {
                            addWaypointMarker(waypoint);
                        }
                        mMap.updateMap(true);
                    }
                });
        snackbar.show();
    }

    @Override
    public void onWaypointPropertiesChanged(String name, int color) {
        boolean colorChanged = mEditedWaypoint.style.color != color;
        mEditedWaypoint.name = name;
        mEditedWaypoint.style.color = color;
        MarkerItem item = mMarkerLayer.getByUid(mEditedWaypoint);
        item.title = name;
        if (colorChanged) {
            AndroidBitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, color));
            item.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
        }
        mMarkerLayer.updateItems();
        mMap.updateMap(true);
        // This event is relevant only to internal data source
        mWaypointDbDataSource.saveWaypoint(mEditedWaypoint);
        mEditedWaypoint = null;
    }

    private void onTrackProperties(String path) {
        Log.e(TAG, "onTrackProperties(" + path + ")");
        //TODO Think of better way to find appropriate track
        for (FileDataSource source : mData) {
            if (source.path.equals(path)) {
                mEditedTrack = source.tracks.get(0);
                break;
            }
        }
        if (mEditedTrack == null)
            return;

        Bundle args = new Bundle(2);
        args.putString(TrackProperties.ARG_NAME, mEditedTrack.name);
        args.putInt(TrackProperties.ARG_COLOR, mEditedTrack.style.color);
        Fragment fragment = Fragment.instantiate(this, TrackProperties.class.getName(), args);
        fragment.setEnterTransition(new Fade());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "trackProperties");
        ft.addToBackStack("trackProperties");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onTrackPropertiesChanged(String name, int color) {
        mEditedTrack.name = name;
        mEditedTrack.style.color = color;
        onTrackSave(mEditedTrack);
        mEditedTrack = null;
    }

    @Override
    public void onTrackView(Track track) {
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            mLocationOverlay.setPinned(false);
            updateLocationDrawable();
        }
        BoundingBox box = track.getBoundingBox();
        box.expand(0.05);
        mMap.animator().animateTo(box);
    }

    @Override
    public void onTrackDetails(Track track) {
        onTrackDetails(track, false);
    }

    private void onTrackDetails(Track track, boolean current) {
        Fragment fragment = mFragmentManager.findFragmentByTag("trackInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, TrackInformation.class.getName());
            Slide slide = new Slide(mSlideGravity);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "trackInformation");
            ft.addToBackStack("trackInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((TrackInformation) fragment).setTrack(track, current);
        mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(mExtendPanel.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    @Override
    public void onTrackShare(final Track track) {
        final AtomicInteger selected = new AtomicInteger(0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(R.array.track_format_array, selected.get(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selected.set(which);
            }
        });
        builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TrackExport.Builder builder = new TrackExport.Builder();
                @TrackExport.ExportFormat int format = selected.get();
                TrackExport trackExport = builder.setTrack(track).setFormat(format).create();
                trackExport.show(mFragmentManager, "trackExport");
            }
        });
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.msg_track_format_explanation);
                builder.setPositiveButton(R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public void onTrackSave(final Track track) {
        FileDataSource fileSource = (FileDataSource) track.source;
        Manager manager = Manager.getDataManager(getApplicationContext(), fileSource.path);
        if (manager instanceof TrackManager) {
            // Use optimized save for native track
            try {
                ((TrackManager) manager).saveProperties(fileSource);
                // Rename file if name changed
                File thisFile = new File(fileSource.path);
                File thatFile = new File(thisFile.getParent(), FileUtils.sanitizeFilename(track.name) + TrackManager.EXTENSION);
                if (!thisFile.equals(thatFile)) {
                    Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
                    if (loader != null) {
                        // Let loader do the task if it is available
                        ((DataLoader) loader).renameSource(fileSource, thatFile);
                        // otherwise do it manually (this normally should not happen)
                    } else if (thisFile.renameTo(thatFile)) {
                        fileSource.path = thatFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                HelperUtils.showSaveError(this, mCoordinatorLayout, e);
                e.printStackTrace();
            }
        } else {
            // Save hole data source
            Manager.save(getApplicationContext(), (FileDataSource) track.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            track.source.notifyListeners();
                        }
                    });
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }, mProgressHandler);
        }
        // Update track layer
        for (Layer layer : mMap.layers()) {
            if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(track)) {
                ((TrackLayer) layer).setColor(track.style.color);
            }
        }
        mMap.updateMap(true);
    }

    @Override
    public void onTrackDelete(final Track track) {
        // Remove track layer to indicate action to user
        for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(track)) {
                i.remove();
                layer.onDetach();
                break;
            }
        }
        mMap.updateMap(true);

        // Show undo snackbar
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msg_track_deleted, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove track
                        // Native tracks can not be deleted through this procedure
                        ((FileDataSource) track.source).tracks.remove(track);
                        Manager.save(getApplicationContext(), (FileDataSource) track.source, new Manager.OnSaveListener() {
                            @Override
                            public void onSaved(FileDataSource source) {
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        track.source.notifyListeners();
                                    }
                                });
                            }

                            @Override
                            public void onError(FileDataSource source, Exception e) {
                                HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                            }
                        }, mProgressHandler);
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the track on map
                        TrackLayer trackLayer = new TrackLayer(mMap, track);
                        mMap.layers().add(trackLayer);
                        mMap.updateMap(true);
                    }
                });
        snackbar.show();
    }

    @Override
    public void onTracksDelete(final Set<Track> tracks) {
        // Remove markers to indicate action to user
        /*
        for (Waypoint waypoint : waypoints) {
            MarkerItem marker = mMarkerLayer.getByUid(waypoint);
            mMarkerLayer.removeItem(marker);
        }
        mMap.updateMap(true);
        */

        // Show undo snackbar
        /*
        int count = waypoints.size();
        String msg = getResources().getQuantityString(R.plurals.waypointsDeleted, count, count);
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoint
                        if (mWaypointDbDataSource.isOpen()) {
                            for (Waypoint waypoint : waypoints) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            }
                        } else {
                            // We need this when screen is rotated but snackbar is still shown
                            mWaypointDbDataSource.open();
                            for (Waypoint waypoint : waypoints) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            }
                            mWaypointDbDataSource.close();
                        }
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the marker
                        for (Waypoint waypoint : waypoints) {
                            GeoPoint point = new GeoPoint(waypoint.latitude, waypoint.longitude);
                            MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, point);
                            mMarkerLayer.addItem(marker);
                        }
                        mMap.updateMap(true);
                    }
                });
        snackbar.show();
        */
    }

    @Override
    public void onMapSelected(MapFile mapFile) {
        Layers layers = mMap.layers();
        if (mBitmapLayerMap != null) {
            layers.remove(mBitmapLayerMap.tileLayer);
            mBitmapLayerMap.tileSource.close();
            if (mapFile == mBitmapLayerMap) {
                if (mBuildingsLayerEnabled)
                    layers.add(mBuildingsLayer);
                layers.add(mLabelsLayer);
                mMap.updateMap(true);
                mBitmapLayerMap = null;
                return;
            }
        }
        showBitmapMap(mapFile);
    }

    private void showBitmapMap(MapFile mapFile) {
        Log.e(TAG, mapFile.name);
        Layers layers = mMap.layers();
        if (mBuildingsLayerEnabled)
            layers.remove(mBuildingsLayer);
        layers.remove(mLabelsLayer);
        mapFile.tileSource.open();
        mapFile.tileLayer = new BitmapTileLayer(mMap, mapFile.tileSource);
        //FIXME Absolute positioning is a hack
        layers.add(2, mapFile.tileLayer);
        mBitmapLayerMap = mapFile;
        MapPosition position = mMap.getMapPosition();
        boolean positionChanged = false;
        if (!mapFile.boundingBox.contains(position.getGeoPoint())) {
            position.setPosition(mapFile.boundingBox.getCenterPoint());
            positionChanged = true;
        }
        if (position.getZoomLevel() > mapFile.tileSource.getZoomLevelMax()) {
            position.setScale((1 << mapFile.tileSource.getZoomLevelMax()) - 5);
            positionChanged = true;
        }
        if (position.getZoomLevel() < mapFile.tileSource.getZoomLevelMin()) {
            position.setScale((1 << mapFile.tileSource.getZoomLevelMin()) + 5);
            positionChanged = true;
        }
        if (positionChanged)
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, position);
        else
            //TODO Bitmap layer should respond to update map (see TileLayer)
            mMap.clearMap();
    }

    private void showExtendPanel(PANEL_STATE panel, String name, Fragment fragment) {
        if (mPanelState != PANEL_STATE.NONE) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
            //TODO Make it properly work without "immediate" - that is why exit transactions do not work
            mFragmentManager.popBackStackImmediate(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (name.equals(bse.getName())) {
                setPanelState(PANEL_STATE.NONE);
                return;
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mExtendPanel.getLayoutParams();
        switch (panel) {
            case LOCATION:
            case RECORD:
            case PLACES:
                if (mVerticalOrientation) {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                } else {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                }
                break;
            case MAPS:
            case MORE:
                if (mVerticalOrientation) {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                } else {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }
        }
        mExtendPanel.setLayoutParams(params);

        FragmentTransaction ft = mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new TransitionSet().addTransition(new Slide(mSlideGravity)).addTransition(new Visibility() {
            @Override
            public Animator onAppear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelBackground), getColor(R.color.panelSolidBackground));
            }
        }));
        fragment.setReturnTransition(new TransitionSet().addTransition(new Slide(mSlideGravity)).addTransition(new Visibility() {
            @Override
            public Animator onDisappear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelSolidBackground), getColor(R.color.panelBackground));
            }
        }));
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
        View mOBB = findViewById(R.id.mapsButtonBackground);
        View mMBB = findViewById(R.id.moreButtonBackground);

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
                case MAPS:
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
            case MAPS:
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

    private final Set<WeakReference<OnBackPressedListener>> mBackListeners = new HashSet<>();

    @Override
    public FloatingActionButton enableActionButton() {
        TransitionManager.beginDelayedTransition(mCoordinatorLayout, new Fade());
        mActionButton.setVisibility(View.VISIBLE);
        return mActionButton;
    }

    @Override
    public void disableActionButton() {
        mActionButton.setVisibility(View.GONE);
    }

    @Override
    public void addBackClickListener(OnBackPressedListener listener) {
        mBackListeners.add(new WeakReference<>(listener));
    }

    @Override
    public void removeBackClickListener(OnBackPressedListener listener) {
        for (Iterator<WeakReference<OnBackPressedListener>> iterator = mBackListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<OnBackPressedListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    @Override
    public void popCurrent() {
        mFragmentManager.popBackStack();
    }

    @Override
    public void popAll() {
        FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
        mFragmentManager.popBackStack(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return mCoordinatorLayout;
    }

    private boolean backKeyIntercepted() {
        boolean intercepted = false;
        for (WeakReference<OnBackPressedListener> weakRef : mBackListeners) {
            OnBackPressedListener onBackClickListener = weakRef.get();
            if (onBackClickListener != null) {
                boolean isFragIntercept = onBackClickListener.onBackClick();
                if (!intercepted)
                    intercepted = isFragIntercept;
            }
        }
        return intercepted;
    }

    final Handler mBackHandler = new Handler();

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed()");
        if (backKeyIntercepted())
            return;

        int count = mFragmentManager.getBackStackEntryCount();
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

    @Override
    public void onBackStackChanged() {
        Log.e(TAG, "onBackStackChanged()");
        int count = mFragmentManager.getBackStackEntryCount();
        if (count == 0) {
            if (mPanelState != PANEL_STATE.NONE)
                setPanelState(PANEL_STATE.NONE);
            return;
        }
        FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
        Fragment f = mFragmentManager.findFragmentByTag(bse.getName());
        if (f == null)
            return;
        View v = f.getView();
        if (v == null)
            return;
        final ViewGroup p = (ViewGroup) v.getParent();
        if (p.getForeground() != null) {
            p.setForeground(getDrawable(R.drawable.dim));
            p.getForeground().setAlpha(0);
            ObjectAnimator anim = ObjectAnimator.ofInt(p.getForeground(), "alpha", 255, 0);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    p.setForeground(null);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    p.setForeground(null);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            anim.setDuration(500);
            anim.start();
        }
    }

    // Called by tile manager, so it's on separate thread - do not block and do not update UI
    @Override
    public void onDataMissing(final MapTile tile) {
        // Do not check "intermediate" maps - TODO: Should we consider movement when locked to location?
        if (mMap.animator().isAnimating())
            return;
        // Consider only center tile
        if (tile.distance > 0d)
            return;

        mBackgroundHandler.removeMessages(R.id.msgFindMissingMap);

        // It appears to be fast enough but I will live it in separate background thread
        final Message m = Message.obtain(mBackgroundHandler, new Runnable() {
            @Override
            public void run() {
                double halfTileSize = 1d / (1 << (tile.zoomLevel + 1));
                final MapFile mapFile = mMapIndex.getNativeMap(tile.x + halfTileSize, tile.y + halfTileSize);
                if (mapFile == null)
                    return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mMapDownloadButton.getVisibility() == View.GONE) {
                            String size = Formatter.formatShortFileSize(MainActivity.this, mapFile.downloadSize);
                            mMapDownloadButton.setTag(mapFile);
                            mMapDownloadButton.setText(getString(R.string.mapDownloadText, mapFile.name, size));
                            mMapDownloadButton.setVisibility(View.VISIBLE);
                        } else if (!mapFile.downloadPath.equals(((MapFile) mMapDownloadButton.getTag()).downloadPath)) {
                            String size = Formatter.formatShortFileSize(MainActivity.this, mapFile.downloadSize);
                            mMapDownloadButton.setTag(mapFile);
                            mMapDownloadButton.setText(getString(R.string.mapDownloadText, mapFile.name, size));
                        }
                        mMainHandler.removeMessages(R.id.msgRemoveMapDownloadButton);
                    }
                });
            }
        });
        m.what = R.id.msgFindMissingMap;
        mBackgroundHandler.sendMessage(m);
    }

    @Override
    public void updateMapViewArea() {
        Log.e(TAG, "updateMapViewArea()");
        final ViewTreeObserver vto = mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                Log.e(TAG, "onGlobalLayout()");
                Rect area = new Rect();
                mMapView.getLocalVisibleRect(area);
                int mapWidth = area.width();
                int mapHeight = area.height();

                /*
                if (mGaugePanel != null)
                    area.left = mGaugePanel.getRight();
                */
                View v = findViewById(R.id.actionPanel);
                if (v != null)
                    area.bottom = v.getTop();
                if (mPanelState != PANEL_STATE.NONE) {
                    area.bottom = mExtendPanel.getTop();
                }
                v = findViewById(R.id.contentPanel);
                if (v != null)
                    area.bottom = Math.min(area.bottom, v.getTop());
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
                }

                ViewTreeObserver ob;
                if (vto.isAlive())
                    ob = vto;
                else
                    ob = mMapView.getViewTreeObserver();

                ob.removeOnGlobalLayoutListener(this);

                if (Boolean.TRUE.equals(mGaugePanel.getTag())) {
                    mGaugePanel.setTranslationX(-mGaugePanel.getWidth());
                    mGaugePanel.setVisibility(View.VISIBLE);
                    mGaugePanel.setTag(null);
                }
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
    public Loader<List<FileDataSource>> onCreateLoader(int id, Bundle args) {
        Log.e(TAG, "onCreateLoader(" + id + ")");
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<FileDataSource>> loader, List<FileDataSource> data) {
        Log.e(TAG, "onLoadFinished()");
        if (data == null)
            return;
        mData = data;
        for (FileDataSource source : mData) {
            if (source.isLoaded() && !source.isVisible()) {
                addSourceToMap(source);
                source.setVisible(true);
            }
        }
        Fragment dataSourceList = mFragmentManager.findFragmentByTag("dataSourceList");
        if (dataSourceList != null)
            ((DataSourceList) dataSourceList).updateData();
        Fragment nativeTrackList = mFragmentManager.findFragmentByTag("nativeTrackList");
        if (nativeTrackList != null)
            ((DataSourceList) nativeTrackList).updateData();
        mMap.updateMap(true);
    }

    @Override
    public void onLoaderReset(Loader<List<FileDataSource>> loader) {

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast: " + action);
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long ref = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(ref);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    String fileName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    Log.e(TAG, "Downloaded: " + fileName);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        mMapIndex.markDownloaded(fileName);
                        if (mMapFileSource != null) {
                            mMapFileSource.openFile(new File(fileName));
                            mMap.clearMap();
                        }
                    }
                }
            }
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
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATE)) {
                enableNavigation();
                updateNavigationUI();
            }
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATUS)) {
                mGaugePanel.setValue(Gauge.TYPE_DISTANCE, mNavigationService.getDistance());
                mGaugePanel.setValue(Gauge.TYPE_BEARING, mNavigationService.getBearing());
                mGaugePanel.setValue(Gauge.TYPE_TURN, mNavigationService.getTurn());
                mGaugePanel.setValue(Gauge.TYPE_VMG, mNavigationService.getVmg());
                mGaugePanel.setValue(Gauge.TYPE_XTK, mNavigationService.getXtk());
                mGaugePanel.setValue(Gauge.TYPE_ETE, mNavigationService.getEte());
                adjustNavigationArrow(mNavigationService.getTurn());
            }
        }
    };

    private void addSourceToMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            addWaypointMarker(waypoint);
        }
        for (Track track : source.tracks) {
            TrackLayer trackLayer = new TrackLayer(mMap, track);
            mMap.layers().add(trackLayer);
        }
    }

    private void removeSourceFromMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            removeWaypointMarker(waypoint);
        }
        for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if (!(layer instanceof TrackLayer))
                continue;
            if (source.tracks.contains(((TrackLayer) layer).getTrack())) {
                i.remove();
                layer.onDetach();
            }
        }
    }

    private void addWaypointMarker(Waypoint waypoint) {
        GeoPoint geoPoint = new GeoPoint(waypoint.latitude, waypoint.longitude);
        MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, geoPoint);
        if (waypoint.style.color != 0 && waypoint.style.color != MarkerStyle.DEFAULT_COLOR) {
            Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, waypoint.style.color));
            marker.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
        }
        mMarkerLayer.addItem(marker);
    }

    private void removeWaypointMarker(Waypoint waypoint) {
        MarkerItem marker = mMarkerLayer.getByUid(waypoint);
        mMarkerLayer.removeItem(marker);
    }

    @Override
    public Map getMap() {
        return mMap;
    }

    @NonNull
    @Override
    public WaypointDbDataSource getWaypointDataSource() {
        return mWaypointDbDataSource;
    }

    @NonNull
    @Override
    public List<FileDataSource> getData() {
        return mData;
    }

    @Override
    public void setDataSourceAvailability(FileDataSource source, boolean available) {
        if (available) {
            if (source.isLoaded()) {
                addSourceToMap(source);
            }
        } else {
            removeSourceFromMap(source);
        }
        source.setVisible(available); // Set visibility for UI response, it does not affect other parts as source is replaced by loader
        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null)
            ((DataLoader) loader).markDataSourceLoadable(source, available);
        mMap.updateMap(true);
    }

    @Override
    public void onDataSourceSelected(@NonNull DataSource source) {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble(DataList.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(DataList.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(DataList.ARG_HEIGHT, mExtendPanel.getHeight());
        DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
        fragment.setDataSource(source);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new Fade());
        ft.add(R.id.extendPanel, fragment, "dataList");
        ft.addToBackStack("dataList");
        ft.commit();
    }

    @Override
    public void onDataSourceDelete(@NonNull final DataSource source) {
        if (!(source instanceof FileDataSource)) {
            final Snackbar snackbar = Snackbar.make(mCoordinatorLayout, R.string.msg_cannot_delete_native_source, Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.title_delete_permanently);
        builder.setMessage(R.string.msg_delete_source_permanently);
        builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File sourceFile = new File(((FileDataSource) source).path);
                if (sourceFile.exists()) {
                    if (sourceFile.delete()) {
                        removeSourceFromMap((FileDataSource) source);
                    } else {
                        //TODO Use helper in other places
                        HelperUtils.showError(getString(R.string.msg_delete_failed), mCoordinatorLayout);
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }
}