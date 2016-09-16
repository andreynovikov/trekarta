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
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.view.ViewPropertyAnimator;
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

import org.oscim.android.MapView;
import org.oscim.android.cache.PreCachedTileCache;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.CanvasAdapter;
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
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.OnDataMissingListener;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileCacheSource;
import org.oscim.utils.Osm;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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
import mobi.maptrek.fragments.CrashReport;
import mobi.maptrek.fragments.DataList;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.FragmentHolder;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.fragments.MapList;
import mobi.maptrek.fragments.MapSelection;
import mobi.maptrek.fragments.MarkerInformation;
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
import mobi.maptrek.layers.MapCoverageLayer;
import mobi.maptrek.layers.NavigationLayer;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerSymbol;
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
import mobi.maptrek.util.SunriseSunset;
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

    private static final int MAP_BASE = 1;
    private static final int MAP_MAPS = 2;
    private static final int MAP_3D = 3;
    private static final int MAP_LABELS = 4;
    private static final int MAP_DATA = 5;
    private static final int MAP_3D_DATA = 6;
    private static final int MAP_POSITIONAL = 7;
    private static final int MAP_OVERLAYS = 8;

    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;

    private static final int NIGHT_CHECK_PERIOD = 180000; // 3 minutes
    private static final int TRACK_ROTATION_DELAY = 1000; // 1 second

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

    public enum NIGHT_MODE_STATE {
        AUTO,
        DAY,
        NIGHT
    }

    private float mFingerTipSize;
    private int mColorAccent;
    private int mColorPrimaryDark;
    private int mPanelSolidBackground;
    private int mPanelBackground;
    private int mPanelExtendedBackground;

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
    private int mMovingOffset = 0;
    private int mTrackingOffset = 0;
    private double mTrackingOffsetFactor = 1;
    private long mTrackingDelay;
    private boolean mBuildingsLayerEnabled = true;
    private boolean mHideMapObjects = true;
    private int mBitmapMapTransparency = 0;

    protected Map mMap;
    protected MapView mMapView;
    private GaugePanel mGaugePanel;
    private TextView mSatellitesText;
    private View mMapButtonHolder;
    private ImageButton mLocationButton;
    private ImageButton mRecordButton;
    private ImageButton mPlacesButton;
    private ImageButton mMapsButton;
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

    private long mLastLocationMilliseconds = 0L;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0f;

    private SunriseSunset mSunriseSunset;
    private NIGHT_MODE_STATE mNightModeState;
    private boolean mNightMode = false;
    private long mNextNightCheck = 0L;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    //TODO Temporary fix
    @SuppressWarnings("FieldCanBeLocal")
    private VectorTileLayer mBaseLayer;
    private VectorTileLayer mNativeMapsLayer;
    private BuildingLayer mBuildingsLayer;
    DefaultMapScaleBar mMapScaleBar;
    private MapScaleBarLayer mMapScaleBarLayer;
    private LabelLayer mLabelsLayer;
    private LabelLayer mNativeLabelsLayer;
    private TileGridLayer mGridLayer;
    private NavigationLayer mNavigationLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private LocationOverlay mLocationOverlay;
    private MapCoverageLayer mMapCoverageLayer;
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

        Resources resources = getResources();
        Resources.Theme theme = getTheme();
        mColorAccent = resources.getColor(R.color.colorAccent, theme);
        mColorPrimaryDark = resources.getColor(R.color.colorPrimaryDark, theme);
        mPanelBackground = resources.getColor(R.color.panelBackground, theme);
        mPanelSolidBackground = resources.getColor(R.color.panelSolidBackground, theme);
        mPanelExtendedBackground = resources.getColor(R.color.panelExtendedBackground, theme);

        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.setPriority(Thread.MIN_PRIORITY);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        StringFormatter.coordinateFormat = Configuration.getCoordinatesFormat();
        //FIXME Use preferences
        StringFormatter.speedFactor = 3.6f;
        StringFormatter.speedAbbr = "kmh";

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Estimate finger tip height (0.25 inch is obtained from experiments)
        mFingerTipSize = (float) (metrics.ydpi * 0.25);

        mSunriseSunset = new SunriseSunset();
        mNightModeState = NIGHT_MODE_STATE.values()[Configuration.getNightModeState()];

        // Apply default styles at start
        TrackStyle.DEFAULT_COLOR = resources.getColor(R.color.trackColor, theme);
        TrackStyle.DEFAULT_WIDTH = resources.getInteger(R.integer.trackWidth);

        File mapsDir = getExternalFilesDir("maps");

        // find the retained fragment on activity restarts
        mFragmentManager = getFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mDataFragment = (DataFragment) mFragmentManager.findFragmentByTag("data");

        if (mDataFragment == null) {
            // add the fragment
            mDataFragment = new DataFragment();
            mFragmentManager.beginTransaction().add(mDataFragment, "data").commit();

            // Provide application context so that maps can be cached on rotation
            mMapIndex = new MapIndex(getApplicationContext(), mapsDir);
            if (BuildConfig.FULL_VERSION)
                mMapIndex.initializeOnlineMapProviders();

            //noinspection SpellCheckingInspection
            File waypointsFile = new File(getExternalFilesDir("databases"), "waypoints.sqlitedb");
            mWaypointDbDataSource = new WaypointDbDataSource(this, waypointsFile);

            mBitmapLayerMap = mMapIndex.getMap(Configuration.getBitmapMap());

            mMapFileSource = new MultiMapFileTileSource(mMapIndex);
            String language = Configuration.getLanguage();
            if (language == null) {
                language = resources.getConfiguration().locale.getLanguage();
                if (!Arrays.asList(new String[]{"en", "de", "ru"}).contains(language))
                    language = "en";
                Configuration.setLanguage(language);
            }
            mMapFileSource.setPreferredLanguage(language);
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
        mMapButtonHolder = findViewById(R.id.mapButtonHolder);
        mCompassView = findViewById(R.id.compass);
        mNavigationArrowView = findViewById(R.id.navigationArrow);
        mNavigationArrowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapObject mapObject = mNavigationService.getWaypoint();
                GeoPoint destination = new GeoPoint(mapObject.latitude, mapObject.longitude);
                setMapLocation(destination);
            }
        });
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
        if (mapPosition.x == 0.5 && mapPosition.y == 0.5) {
            // Set initial location based on device language
            switch (resources.getConfiguration().locale.getLanguage()) {
                case "de":
                    mMap.setMapPosition(50.8, 10.45, (1 << 6) * 1.5);
                    break;
                case "ru":
                    mMap.setMapPosition(56.4, 39, 1 << 5);
                    break;
                default:
                    mMap.setMapPosition(-19, -12, 1 << 2);
            }
        }

        mNavigationNorthDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_north, theme);
        mNavigationTrackDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_track, theme);
        mMyLocationDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_my_location, theme);
        mLocationSearchingDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_location_searching, theme);

        Layers layers = mMap.layers();
        layers.addGroup(MAP_BASE);

        TileSource baseMapSource = new OSciMap4TileCacheSource();
        //TileSource baseMapSource = OSciMap4TileSource.builder().url("http://maptrek.mobi/tiles/all").zoomMax(4).build();

        File cacheDir = getExternalCacheDir();
        if (cacheDir != null) {
            SQLiteAssetHelper preCachedDatabaseHelper = new SQLiteAssetHelper(this, "world_map_z7.db", getDir("databases", 0).getAbsolutePath(), null, 1);
            mCache = new PreCachedTileCache(this, preCachedDatabaseHelper.getReadableDatabase(), cacheDir.getAbsolutePath(), "tile_cache.db");
            mCache.setCacheSize(512 * (1 << 10));
            baseMapSource.setCache(mCache);
            //baseMapSource.setCache(new TileCache(this, cacheDir.getAbsolutePath(), "tile_cache.db"));
        }

        mBaseLayer = new OsmTileLayer(mMap);
        mBaseLayer.setTileSource(baseMapSource);
        mBaseLayer.setNumLoaders(1);
        mMap.setBaseMap(mBaseLayer); // will go to base group

        // setBaseMap does not operate with layer groups so we add remaining groups later
        layers.addGroup(MAP_MAPS);
        layers.addGroup(MAP_3D);
        layers.addGroup(MAP_LABELS);
        layers.addGroup(MAP_DATA);
        layers.addGroup(MAP_3D_DATA);
        layers.addGroup(MAP_POSITIONAL);
        layers.addGroup(MAP_OVERLAYS);

        mLocationOverlay = new LocationOverlay(mMap);

        mNativeMapsLayer = new OsmTileLayer(mMap);
        mNativeMapsLayer.setTileSource(mMapFileSource);
        mNativeMapsLayer.setNumLoaders(1);
        mMapFileSource.setOnDataMissingListener(this);
        layers.add(mNativeMapsLayer, MAP_BASE);

        mGridLayer = new TileGridLayer(mMap);
        if (Configuration.getGridLayerEnabled())
            layers.add(mGridLayer, MAP_OVERLAYS);

        mBuildingsLayerEnabled = Configuration.getBuildingsLayerEnabled();
        if (mBuildingsLayerEnabled) {
            mBuildingsLayer = new BuildingLayer(mMap, mNativeMapsLayer);
            layers.add(mBuildingsLayer, MAP_3D);
        }
        mLabelsLayer = new LabelLayer(mMap, mBaseLayer);
        layers.add(mLabelsLayer, MAP_LABELS);
        mNativeLabelsLayer = new LabelLayer(mMap, mNativeMapsLayer);
        layers.add(mNativeLabelsLayer, MAP_LABELS);

        mMapScaleBar = new DefaultMapScaleBar(mMap, CanvasAdapter.dpi / 200);
        mMapScaleBarLayer = new MapScaleBarLayer(mMap, mMapScaleBar);
        layers.add(mMapScaleBarLayer, MAP_OVERLAYS);
        layers.add(mLocationOverlay, MAP_POSITIONAL);

        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this));
        MarkerSymbol symbol;
        if (BILLBOARDS)
            symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
        else
            symbol = new MarkerSymbol(bitmap, 0.5f, 0.5f, false);

        mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), symbol, this);
        layers.add(mMarkerLayer, MAP_3D_DATA);

        // Load waypoints
        mWaypointDbDataSource.open();
        List<Waypoint> waypoints = mWaypointDbDataSource.getWaypoints();
        for (Waypoint waypoint : waypoints) {
            if (mEditedWaypoint != null && mEditedWaypoint._id == waypoint._id)
                mEditedWaypoint = waypoint;
            addWaypointMarker(waypoint);
        }

        mHideMapObjects = Configuration.getHideMapObjects();
        mBitmapMapTransparency = Configuration.getBitmapMapTransparency();
        if (mBitmapLayerMap != null)
            showBitmapMap(mBitmapLayerMap);

        setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT ||
                savedInstanceState != null && savedInstanceState.getBoolean("nightMode"));

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
        mMoreButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                onMoreLongClicked();
                return true;
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
        showActionPanel(Configuration.getActionPanelState(), false);

        // Resume navigation
        MapObject mapObject = Configuration.getNavigationPoint();
        if (mapObject != null)
            startNavigation(mapObject);

        // Initialize data loader
        getLoaderManager();

        // Remove splash from background
        getWindow().setBackgroundDrawable(new ColorDrawable(resources.getColor(R.color.colorBackground, theme)));

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "New intent: " + action);
        String scheme = intent.getScheme();

        if ("geo".equals(scheme)) {
            Uri uri = intent.getData();
            Log.w(TAG, "   " + uri.toString());
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
                String marker = null;
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
                    if (bracket > -1) {
                        marker = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
                    }
                }
                MapPosition position = mMap.getMapPosition();
                position.setPosition(lat, lon);
                if (zoom > 0)
                    position.setZoomLevel(zoom);
                mMap.setMapPosition(position);
                showMarker(position, marker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            // http://wiki.openstreetmap.org/wiki/Shortlink
            Uri uri = intent.getData();
            Log.w(TAG, "   " + uri.toString());
            List<String> path = uri.getPathSegments();
            if ("go".equals(path.get(0))) {
                MapPosition position = Osm.decodeShortLink(path.get(1));
                String marker = uri.getQueryParameter("m");
                mMap.setMapPosition(position);
                showMarker(position, marker);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart()");

        // Start loading user data
        DataLoader loader = (DataLoader) getLoaderManager().initLoader(0, null, this);
        loader.setProgressHandler(mProgressHandler);

        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadReceiver.BROADCAST_DOWNLOAD_PROCESSED));
        registerReceiver(mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_SAVE));
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
        layoutExtendPanel(mPanelState);
        updateMapViewArea();

        mMap.events.bind(this);
        mMap.input.bind(this);
        mMapView.onResume();
        updateLocationDrawable();
        adjustCompass(mMap.getMapPosition().bearing);

        if (MapTrekApplication.getApplication().hasPreviousRunsExceptions()) {
            Fragment fragment = Fragment.instantiate(this, CrashReport.class.getName());
            fragment.setEnterTransition(new Slide());
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "crashReport");
            ft.addToBackStack("crashReport");
            ft.commit();
            updateMapViewArea();
        }
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

        if (isFinishing()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            if (mTrackingState == TRACKING_STATE.TRACKING)
                startService(intent.setAction(BaseLocationService.ENABLE_BACKGROUND_TRACK));
            else
                stopService(intent);
        }

        if (mLocationService != null)
            disableLocations();

        if (mNavigationService != null) {
            if (isFinishing()) {
                Intent intent = new Intent(getApplicationContext(), NavigationService.class);
                if (mNavigationService.isNavigating())
                    startService(intent.setAction(BaseNavigationService.ENABLE_BACKGROUND_NAVIGATION));
                else
                    stopService(intent);
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
        mMapScaleBar.destroy();
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

        if (mLocationService != null)
            startService(new Intent(getApplicationContext(), LocationService.class));
        if (mNavigationService != null)
            startService(new Intent(getApplicationContext(), NavigationService.class));

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
        savedInstanceState.putBoolean("savedNavigationState", mNavigationService != null);
        if (mProgressBar.getVisibility() == View.VISIBLE)
            savedInstanceState.putInt("progressBar", mProgressBar.getMax());
        savedInstanceState.putSerializable("panelState", mPanelState);
        savedInstanceState.putBoolean("nightMode", mNightMode);
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
        if (savedInstanceState.getBoolean("savedNavigationState", false))
            enableNavigation();
        if (savedInstanceState.containsKey("progressBar")) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setMax(savedInstanceState.getInt("progressBar"));
        }
        setPanelState((PANEL_STATE) savedInstanceState.getSerializable("panelState"));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_night_mode: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.action_night_mode);
                builder.setItems(R.array.night_mode_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mNightModeState = NIGHT_MODE_STATE.values()[which];
                        if (mNightModeState == NIGHT_MODE_STATE.DAY && mNightMode)
                            setNightMode(false);
                        if (mNightModeState == NIGHT_MODE_STATE.NIGHT && !mNightMode)
                            setNightMode(true);
                        Configuration.setNightModeState(mNightModeState.ordinal());
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.action_language: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.action_language);
                builder.setItems(R.array.language_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] languageCodes = getResources().getStringArray(R.array.language_code_array);
                        String code = languageCodes[which];
                        if (mMapFileSource != null) {
                            mMapFileSource.setPreferredLanguage(code);
                            mMap.clearMap();
                        }
                        Configuration.setLanguage(code);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.theme_osmarender: {
                mMap.setTheme(VtmThemes.OSMARENDER, true);
                return true;
            }
            case R.id.action_3dbuildings: {
                mBuildingsLayerEnabled = item.isChecked();
                if (mBuildingsLayerEnabled) {
                    mBuildingsLayer = new BuildingLayer(mMap, mNativeMapsLayer);
                    mMap.layers().add(mBuildingsLayer, MAP_3D);
                    // Let buildings be re-fetched from map layer
                    mMap.clearMap();
                } else {
                    mMap.layers().remove(mBuildingsLayer);
                    mBuildingsLayer = null;
                }
                Configuration.setBuildingsLayerEnabled(mBuildingsLayerEnabled);
                mMap.updateMap(true);
                return true;
            }
            case R.id.action_grid: {
                if (item.isChecked()) {
                    mMap.layers().add(mGridLayer, MAP_OVERLAYS);
                } else {
                    mMap.layers().remove(mGridLayer);
                }
                Configuration.setGridLayerEnabled(item.isChecked());
                mMap.updateMap(true);
                return true;
            }
            case R.id.actionStopNavigation: {
                stopNavigation();
                return true;
            }
            case R.id.action_manage_maps: {
                startMapSelection(true);
                return true;
            }
            case R.id.action_reset_advices: {
                Configuration.resetAdviceState();
                Snackbar snackbar = Snackbar.make(mCoordinatorLayout, R.string.msg_advices_reset, Snackbar.LENGTH_LONG);
                snackbar.show();
                return true;
            }
            case R.id.action_about: {
                Fragment fragment = Fragment.instantiate(this, About.class.getName());
                fragment.setEnterTransition(new Slide(mSlideGravity));
                fragment.setReturnTransition(new Slide(mSlideGravity));
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "about");
                ft.addToBackStack("about");
                ft.commit();
                return true;
            }
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
            long time = SystemClock.uptimeMillis();
            // Adjust map movement animation to location acquisition period to make movement smoother
            long locationDelay = time - mLastLocationMilliseconds;
            double duration = Math.min(1500, locationDelay); // 1.5 seconds maximum
            mMovementAnimationDuration = (int) movingAverage(duration, mMovementAnimationDuration);
            // Update map position
            mMap.getMapPosition(mMapPosition);

            boolean rotate = mLocationState == LocationState.TRACK && mTrackingDelay < time;
            double offset;
            if (rotate) {
                offset = mTrackingOffset / mTrackingOffsetFactor;
            } else {
                offset = mMovingOffset;
            }
            offset = offset / (mMapPosition.scale * Tile.SIZE);

            double rad = Math.toRadians(mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);

            mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
            mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);
            mMapPosition.setBearing(-mAveragedBearing);
            //FIXME VTM
            mMap.animator().animateTo(mMovementAnimationDuration, mMapPosition, rotate);
        }

        mLocationOverlay.setPosition(lat, lon, bearing, location.getAccuracy());
        if (mNavigationLayer != null)
            mNavigationLayer.setPosition(lat, lon);
        mLastLocationMilliseconds = SystemClock.uptimeMillis();
        if (mNightModeState == NIGHT_MODE_STATE.AUTO)
            checkNightMode(location);
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
                mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mMapPosition);
                break;
            case NORTH:
                mLocationState = LocationState.TRACK;
                mMap.getEventLayer().enableRotation(false);
                mMap.getEventLayer().setFixOnCenter(true);
                mTrackingDelay = SystemClock.uptimeMillis() + TRACK_ROTATION_DELAY;
                break;
            case TRACK:
                mLocationState = LocationState.ENABLED;
                mMap.getEventLayer().enableRotation(true);
                mMap.getEventLayer().setFixOnCenter(false);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(0);
                mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
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
            args.putBoolean(DataList.ARG_NO_EXTRA_SOURCES, true);
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
        onWaypointCreate(geoPoint, name);
    }

    private void onMapsClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(5);
        args.putDouble(MapList.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(MapList.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(MapList.ARG_ZOOM_LEVEL, mMapPosition.getZoomLevel());
        args.putBoolean(MapList.ARG_HIDE_OBJECTS, mHideMapObjects);
        args.putInt(MapList.ARG_TRANSPARENCY, mBitmapMapTransparency);
        MapList fragment = (MapList) Fragment.instantiate(this, MapList.class.getName(), args);
        fragment.setMaps(mMapIndex.getMaps(), mBitmapLayerMap);
        showExtendPanel(PANEL_STATE.MAPS, "mapsList", fragment);
    }

    private void onMapsLongClicked() {
        PanelMenu fragment = (PanelMenu) Fragment.instantiate(this, PanelMenu.class.getName());
        fragment.setMenu(R.menu.menu_map, new PanelMenu.OnPrepareMenuListener() {
            @Override
            public void onPrepareMenu(List<PanelMenuItem> menu) {
                PanelMenuItem osmarenderer = null;
                for (PanelMenuItem item : menu) {
                    switch (item.getItemId()) {
                        case R.id.action_night_mode:
                            String[] nightModes = getResources().getStringArray(R.array.night_mode_array);
                            ((TextView) item.getActionView()).setText(nightModes[mNightModeState.ordinal()]);
                            break;
                        case R.id.action_language:
                            ((TextView) item.getActionView()).setText(Configuration.getLanguage());
                            break;
                        case R.id.action_3dbuildings:
                            item.setChecked(mBuildingsLayerEnabled);
                            break;
                        case R.id.action_grid:
                            item.setChecked(mMap.layers().contains(mGridLayer));
                            break;
                        case R.id.theme_osmarender:
                            osmarenderer = item;
                    }
                }
                if (!BuildConfig.DEBUG && osmarenderer != null)
                    menu.remove(osmarenderer);
            }
        });
        showExtendPanel(PANEL_STATE.MAPS, "mapMenu", fragment);
    }

    private void onMoreClicked() {
        if (mLocationButton.getVisibility() == View.VISIBLE) {
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
        } else {
            showActionPanel(true, true);
        }
    }

    private void onMoreLongClicked() {
        showActionPanel(mLocationButton.getVisibility() == View.INVISIBLE, true);
    }

    private void onMapDownloadClicked() {
        mMapDownloadButton.setVisibility(View.GONE);
        startMapSelection(false);
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
        Object uid = item.getUid();
        if (uid != null)
            onWaypointDetails((Waypoint) uid, false);
        return uid != null;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (mLocationState != LocationState.DISABLED && mLocationState != LocationState.ENABLED)
            return false;
        mActiveMarker = item;
        // For better experience get delta from marker position and finger press
        // and consider it when moving marker
        Point point = new Point();
        mMap.viewport().toScreenPoint(item.getPoint(), point);
        deltaX = (float) (downX - point.x);
        deltaY = (float) (downY - point.y);
        // Shift map to reveal marker tip position
        mMap.getEventLayer().enableMove(false);
        mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(0f, mFingerTipSize), 1, true);
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
            updateLocationDrawable();
        }
        mMap.animator().animateTo(point);
    }

    private MarkerItem mMarker;

    @Override
    public void showMarker(GeoPoint point, String name) {
        // There can be only one marker at a time
        removeMarker();
        mMarker = new MarkerItem(name, null, point);
        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.round_marker, mColorAccent));
        mMarker.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER));
        mMarkerLayer.addItem(mMarker);
        mMap.updateMap(true);
    }

    @Override
    public void removeMarker() {
        if (mMarker == null)
            return;
        mMarkerLayer.removeItem(mMarker);
        mMap.updateMap(true);
        mMarker = null;
    }

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mLocationService = (ILocationService) binder;
            mLocationService.registerLocationCallback(MainActivity.this);
            mLocationService.setProgressListener(mProgressHandler);
            updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
            updateNavigationUI();
        }
    };

    private void enableNavigation() {
        Log.e(TAG, "enableNavigation");
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
            Log.e(TAG, "onServiceConnected");
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
        mMap.layers().add(mCurrentTrackLayer, MAP_DATA);
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
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(0f, -mFingerTipSize), 1, true);
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
        if (e == Map.POSITION_EVENT) {
            mTrackingOffsetFactor = Math.cos(Math.toRadians(mapPosition.tilt) * 0.9);
            if (mCompassView.getVisibility() == View.GONE && mapPosition.bearing != 0f && mLocationState != LocationState.TRACK) {
                if (Math.abs(mapPosition.bearing) < 1.5f) {
                    mapPosition.setBearing(0f);
                    mMap.setMapPosition(mapPosition);
                }
            }
            adjustCompass(mapPosition.bearing);
        }
        if (e == Map.MOVE_EVENT) {
            if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                mPreviousLocationState = mLocationState;
                mLocationState = LocationState.ENABLED;
                updateLocationDrawable();
            }
        }
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
                mMainHandler.sendMessageDelayed(m, 1000);
            }
        }
        if (mLabelsLayer.isEnabled() && mapPosition.zoomLevel > 7) {
            mMap.layers().remove(mLabelsLayer);
            mLabelsLayer.setEnabled(false);
        } else if (!mLabelsLayer.isEnabled() && mapPosition.zoomLevel <= 7) {
            mMap.layers().add(mLabelsLayer, MAP_LABELS);
            mLabelsLayer.setEnabled(true);
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
        PopupMenu popup = new PopupMenu(this, mMapButtonHolder);
        Menu menu = popup.getMenu();
        menu.add(0, R.id.actionStopNavigation, Menu.NONE, getString(R.string.action_stop_navigation));
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private void updateLocationDrawable() {
        if (mRecordButton.getTag() != mTrackingState) {
            int recordColor = mTrackingState == TRACKING_STATE.TRACKING ? mColorAccent : mColorPrimaryDark;
            mRecordButton.getDrawable().setTint(recordColor);
            mRecordButton.setTag(mTrackingState);
        }
        if (mLocationButton.getTag() == mLocationState)
            return;
        if (mLocationButton.getTag() == LocationState.SEARCHING) {
            mLocationButton.clearAnimation();
            mSatellitesText.animate().translationY(-200);
        }
        final ViewPropertyAnimator gaugePanelAnimator = mGaugePanel.animate();
        gaugePanelAnimator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mLocationState == LocationState.SEARCHING)
                    mSatellitesText.animate().translationY(8);
                gaugePanelAnimator.setListener(null);
                updateMapViewArea();
            }
        });
        switch (mLocationState) {
            case DISABLED:
                mNavigationNorthDrawable.setTint(mColorPrimaryDark);
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                break;
            case SEARCHING:
                mLocationSearchingDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mLocationSearchingDrawable);
                Animation rotation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotation.setInterpolator(new LinearInterpolator());
                rotation.setRepeatCount(Animation.INFINITE);
                rotation.setDuration(1000);
                mLocationButton.startAnimation(rotation);
                if (mGaugePanel.getVisibility() == View.INVISIBLE) {
                    mSatellitesText.animate().translationY(8);
                } else {
                    gaugePanelAnimator.translationX(-mGaugePanel.getWidth());
                }
                break;
            case ENABLED:
                mMyLocationDrawable.setTint(mColorPrimaryDark);
                mLocationButton.setImageDrawable(mMyLocationDrawable);
                gaugePanelAnimator.translationX(-mGaugePanel.getWidth());
                break;
            case NORTH:
                mNavigationNorthDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                gaugePanelAnimator.translationX(0);
                HelperUtils.showAdvice(Configuration.ADVICE_MORE_GAUGES, R.string.advice_more_gauges, mCoordinatorLayout);
                break;
            case TRACK:
                mNavigationTrackDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mNavigationTrackDrawable);
                gaugePanelAnimator.translationX(0);
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

    //TODO Logic of calling this is a total mess! Think out proper event mechanism
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
                mMap.layers().add(mNavigationLayer, MAP_POSITIONAL);
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
        updateMapViewArea();
    }

    private void showMarker(@NonNull MapPosition position, @Nullable String name) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        Bundle args = new Bundle(3);
        args.putDouble(MarkerInformation.ARG_LATITUDE, position.getLatitude());
        args.putDouble(MarkerInformation.ARG_LONGITUDE, position.getLongitude());
        args.putString(MarkerInformation.ARG_NAME, name);
        Fragment fragment = Fragment.instantiate(this, MarkerInformation.class.getName(), args);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "markerInformation");
        ft.addToBackStack("markerInformation");
        ft.commit();
        updateMapViewArea();
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
    public void onWaypointCreate(GeoPoint point, String name) {
        final Waypoint waypoint = new Waypoint(name, point.getLatitude(), point.getLongitude());
        waypoint.date = new Date();
        mWaypointDbDataSource.saveWaypoint(waypoint);
        MarkerItem marker = new MarkerItem(waypoint, name, null, point);
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

    @Override
    public void onWaypointView(Waypoint waypoint) {
        //TODO Make Waypoint inherit GeoPoint
        setMapLocation(new GeoPoint(waypoint.latitude, waypoint.longitude));
    }

    @Override
    public void onWaypointFocus(Waypoint waypoint) {
        if (waypoint != null)
            mMarkerLayer.setFocus(mMarkerLayer.getByUid(waypoint), waypoint.style.color);
        else
            mMarkerLayer.setFocus(null);
    }

    @Override
    public void onWaypointDetails(Waypoint waypoint, boolean full) {
        MapPosition position = mMap.getMapPosition();
        Bundle args = new Bundle(3);
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
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, location);
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
                        mMap.layers().add(trackLayer, MAP_DATA);
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

    private void showHideMapObjects(boolean hasBitmapMap) {
        Layers layers = mMap.layers();
        if (hasBitmapMap && mHideMapObjects && layers.contains(mNativeLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.remove(mBuildingsLayer);
            if (mLabelsLayer.isEnabled())
                layers.remove(mLabelsLayer);
            layers.remove(mNativeLabelsLayer);
        }
        if ((!hasBitmapMap || !mHideMapObjects) && !layers.contains(mNativeLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.add(mBuildingsLayer, MAP_3D);
            if (mLabelsLayer.isEnabled())
                layers.add(mLabelsLayer, MAP_LABELS);
            layers.add(mNativeLabelsLayer, MAP_LABELS);
        }
    }

    private void startMapSelection(boolean zoom) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        if (zoom) {
            MapPosition mapPosition = mMap.getMapPosition();
            mapPosition.setZoomLevel(6);
            mapPosition.setBearing(0f);
            mapPosition.setTilt(0f);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        }
        MapSelection fragment = (MapSelection) Fragment.instantiate(this, MapSelection.class.getName());
        fragment.setMapIndex(mMapIndex);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "mapSelection");
        ft.addToBackStack("mapSelection");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onMapSelected(MapFile mapFile) {
        if (mBitmapLayerMap != null) {
            mMap.layers().remove(mBitmapLayerMap.tileLayer);
            mBitmapLayerMap.tileSource.close();
            if (mapFile == mBitmapLayerMap) {
                showHideMapObjects(false);
                mMap.updateMap(true);
                mBitmapLayerMap = null;
                return;
            }
        }
        showBitmapMap(mapFile);
    }

    @Override
    public void onHideMapObjects(boolean hide) {
        mHideMapObjects = hide;
        showHideMapObjects(mBitmapLayerMap != null);
        mMap.updateMap(true);
        Configuration.setHideMapObjects(hide);
    }

    @Override
    public void onTransparencyChanged(int transparency) {
        mBitmapMapTransparency = transparency;
        if (mBitmapLayerMap != null) {
            mBitmapLayerMap.tileLayer.tileRenderer().setBitmapAlpha(1 - mBitmapMapTransparency * 0.01f);
            mMap.updateMap(true);
        }
        Configuration.setBitmapMapTransparency(transparency);
    }

    @Override
    public void onBeginMapManagement() {
        mMapCoverageLayer = new MapCoverageLayer(mMap, mMapIndex);
        mMap.layers().add(mMapCoverageLayer, MAP_OVERLAYS);
        MapPosition mapPosition = mMap.getMapPosition();
        if (mapPosition.zoomLevel > 8) {
            mapPosition.setZoomLevel(8);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        } else {
            mMap.updateMap(true);
        }
        int[] xy = (int[]) mMapDownloadButton.getTag();
        if (xy != null)
            mMapIndex.selectNativeMap(xy[0], xy[1], MapIndex.ACTION.DOWNLOAD);
    }

    @Override
    public void onFinishMapManagement() {
        mMap.layers().remove(mMapCoverageLayer);
        mMapCoverageLayer.onDetach();
        mMap.updateMap(true);
        mMapIndex.clearSelections();
        mMapCoverageLayer = null;
    }

    @Override
    public void onManageNativeMaps() {
        boolean removed = mMapIndex.manageNativeMaps();
        if (removed)
            mMap.clearMap();
    }

    private void showBitmapMap(MapFile mapFile) {
        Log.e(TAG, mapFile.name);
        showHideMapObjects(true);
        mapFile.tileSource.open();
        mapFile.tileLayer = new BitmapTileLayer(mMap, mapFile.tileSource);
        mapFile.tileLayer.tileRenderer().setBitmapAlpha(1 - mBitmapMapTransparency * 0.01f);
        mMap.layers().add(mapFile.tileLayer, MAP_MAPS);
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

    private void showActionPanel(boolean show, boolean animate) {
        Configuration.setActionPanelState(show);
        final int duration = 30;
        final View mAPB = findViewById(R.id.actionPanelBackground);

        // If this is interactive action hide all open panels
        if (animate && mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }

        if (animate)
            mMoreButton.animate().rotationBy(180).setDuration(duration * 5);
        if (show) {
            mAPB.setVisibility(View.VISIBLE);
            if (animate)
                mAPB.animate().setDuration(duration * 5).alpha(1f);
            else
                mAPB.setAlpha(1f);
            mMapsButton.setVisibility(View.VISIBLE);
            if (animate) {
                mMapsButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPlacesButton.setVisibility(View.VISIBLE);
                        mPlacesButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mRecordButton.setVisibility(View.VISIBLE);
                                mRecordButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mLocationButton.setVisibility(View.VISIBLE);
                                        mLocationButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                mMapsButton.setAlpha(1f);
                mPlacesButton.setVisibility(View.VISIBLE);
                mPlacesButton.setAlpha(1f);
                mRecordButton.setVisibility(View.VISIBLE);
                mRecordButton.setAlpha(1f);
                mLocationButton.setVisibility(View.VISIBLE);
                mLocationButton.setAlpha(1f);
            }
        } else {
            if (animate) {
                mAPB.animate().alpha(0f).setDuration(duration * 5);
                mLocationButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLocationButton.setVisibility(View.INVISIBLE);
                        mRecordButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mRecordButton.setVisibility(View.INVISIBLE);
                                mPlacesButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mPlacesButton.setVisibility(View.INVISIBLE);
                                        mMapsButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                mMapsButton.setVisibility(View.INVISIBLE);
                                                mAPB.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                mAPB.setAlpha(0f);
                mLocationButton.setAlpha(0f);
                mLocationButton.setVisibility(View.INVISIBLE);
                mRecordButton.setAlpha(0f);
                mRecordButton.setVisibility(View.INVISIBLE);
                mPlacesButton.setAlpha(0f);
                mPlacesButton.setVisibility(View.INVISIBLE);
                mMapsButton.setAlpha(0f);
                mMapsButton.setVisibility(View.INVISIBLE);
                mAPB.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showExtendPanel(PANEL_STATE panel, String name, Fragment fragment) {
        if (mPanelState != PANEL_STATE.NONE) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
            //TODO Make it properly work without "immediate" - that is why exit transitions do not work
            mFragmentManager.popBackStackImmediate(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (name.equals(bse.getName())) {
                setPanelState(PANEL_STATE.NONE);
                return;
            }
        }

        layoutExtendPanel(panel);

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

    private void layoutExtendPanel(PANEL_STATE state) {
        if (state == PANEL_STATE.NONE)
            return;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mExtendPanel.getLayoutParams();
        switch (state) {
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
            thisFrom = mPanelSolidBackground;
            thisTo = mPanelBackground;
            otherFrom = mPanelExtendedBackground;
            otherTo = mPanelBackground;
        } else {
            if (mPanelState == PANEL_STATE.NONE)
                thisFrom = mPanelBackground;
            else
                thisFrom = mPanelExtendedBackground;
            thisTo = mPanelSolidBackground;
            if (mPanelState == PANEL_STATE.NONE)
                otherFrom = mPanelBackground;
            else
                otherFrom = mPanelSolidBackground;
            otherTo = mPanelExtendedBackground;
        }
        ValueAnimator otherColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), otherFrom, otherTo);
        ValueAnimator thisColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), thisFrom, thisTo);
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
        Log.e(TAG, "popCurrent()");
        mFragmentManager.popBackStack();
    }

    @Override
    public void popAll() {
        Log.e(TAG, "popAll()");
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
    public void onDataMissing(final int x, final int y, byte zoom) {
        // Do not check "intermediate" maps - TODO: Should we consider movement when locked to location?
        if (mMap.animator().isActive())
            return;

        // Do not show button if we are already choosing maps
        if (mMapCoverageLayer != null)
            return;

        // Do not show button if this map is already downloading
        if (mMapIndex.isDownloading(x, y))
            return;

        // Do not show button if there is no map for that area
        if (mMapIndex.hasDownloadSizes() && mMapIndex.getNativeMap(x, y).downloadSize == 0L)
            return;

        // Do not show button if custom map is shown
        mMap.getMapPosition(mMapPosition);
        if (mBitmapLayerMap != null && mBitmapLayerMap.contains(mMapPosition.getX(), mMapPosition.getY()))
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMapDownloadButton.getVisibility() == View.GONE) {
                    mMapDownloadButton.setText(R.string.mapDownloadText);
                    mMapDownloadButton.setVisibility(View.VISIBLE);
                }
                mMapDownloadButton.setTag(new int[]{x, y});
                mMainHandler.removeMessages(R.id.msgRemoveMapDownloadButton);
            }
        });
    }

    @Override
    public void updateMapViewArea() {
        Log.e(TAG, "updateMapViewArea()");
        final ViewTreeObserver vto = mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                Log.e(TAG, "onGlobalLayout()");

                if (Boolean.TRUE.equals(mGaugePanel.getTag())) {
                    mGaugePanel.setTranslationX(-mGaugePanel.getWidth());
                    mGaugePanel.setVisibility(View.VISIBLE);
                    mGaugePanel.setTag(null);
                }

                Rect area = new Rect();
                mMapView.getLocalVisibleRect(area);
                int mapWidth = area.width();
                int mapHeight = area.height();

                area.left = (int) (mGaugePanel.getRight() + mGaugePanel.getTranslationX());

                View v = findViewById(R.id.actionPanel);
                if (v != null) {
                    if (mVerticalOrientation)
                        area.bottom = v.getTop();
                    else
                        area.right = v.getLeft();
                }
                if (mPanelState != PANEL_STATE.NONE) {
                    if (mVerticalOrientation)
                        area.bottom = mExtendPanel.getTop();
                    else
                        area.right = mExtendPanel.getLeft();
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

                if (!area.isEmpty()) {
                    int pointerOffset = (int) (LocationOverlay.LocationIndicator.POINTER_SIZE * 1.1f);
                    int centerX = mapWidth / 2;
                    int centerY = mapHeight / 2;
                    mMovingOffset = Math.min(centerX - area.left, area.right - centerX);
                    mMovingOffset = Math.min(mMovingOffset, centerY - area.top);
                    mMovingOffset = Math.min(mMovingOffset, area.bottom - centerY);
                    mMovingOffset -= pointerOffset;
                    if (mMovingOffset < 0)
                        mMovingOffset = 0;

                    mTrackingOffset = area.bottom - mapHeight / 2 - pointerOffset - pointerOffset / 2;

                    BitmapRenderer renderer = mMapScaleBarLayer.getRenderer();
                    renderer.setOffset(area.left + 8 * CanvasAdapter.dpi / 160, 0);
                }

                ViewTreeObserver ob;
                if (vto.isAlive())
                    ob = vto;
                else
                    ob = mMapView.getViewTreeObserver();

                ob.removeOnGlobalLayoutListener(this);
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
            if (source.isLoaded() && source.isLoadable() && !source.isVisible()) {
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
            if (action.equals(DownloadReceiver.BROADCAST_DOWNLOAD_PROCESSED)) {
                int key = intent.getIntExtra("key", 0);
                mMapIndex.setNativeMapTileSource(key);
                mMap.clearMap();
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
                } else {
                    Exception e = (Exception) extras.getSerializable("exception");
                    if (e == null)
                        e = new RuntimeException("Unknown error");
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATE)) {
                enableNavigation();
                updateNavigationUI();
            }
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATUS) && mNavigationService != null) {
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
            mMap.layers().add(trackLayer, MAP_DATA);
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
            HelperUtils.showError(getString(R.string.msg_cannot_delete_native_source), mCoordinatorLayout);
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

    private void checkNightMode(Location location) {
        if (mNextNightCheck > mLastLocationMilliseconds)
            return;

        mSunriseSunset.setLocation(location.getLatitude(), location.getLongitude());
        boolean isNightTime = !mSunriseSunset.isDaytime((location.getTime() * 1d / 3600000) % 24);

        if (isNightTime ^ mNightMode)
            setNightMode(isNightTime);

        mNextNightCheck = mLastLocationMilliseconds + NIGHT_CHECK_PERIOD;
    }

    private void setNightMode(boolean night) {
        if (night)
            mMap.setTheme(VtmThemes.NEWTRON, true);
        else
            mMap.setTheme(VtmThemes.DEFAULT, true);
        mNightMode = night;
    }

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }
}