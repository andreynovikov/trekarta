/*
 * Copyright 2024 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.snackbar.BaseTransientBottomBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.work.Data;
import androidx.work.WorkManager;

import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.AbstractMapEventLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import mobi.maptrek.data.Amenity;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Place;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.RouteDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.PlaceDataSource;
import mobi.maptrek.data.source.PlaceDbDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.RouteStyle;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.databinding.ActivityMainBinding;
import mobi.maptrek.fragments.AmenityInformation;
import mobi.maptrek.dialogs.AmenitySetup;
import mobi.maptrek.dialogs.BaseMapDownload;
import mobi.maptrek.dialogs.CrashReport;
import mobi.maptrek.dialogs.DataExport;
import mobi.maptrek.fragments.DataList;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.FragmentHolder;
import mobi.maptrek.fragments.Legend;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.dialogs.LocationShare;
import mobi.maptrek.fragments.MapList;
import mobi.maptrek.fragments.MapSelection;
import mobi.maptrek.fragments.MarkerInformation;
import mobi.maptrek.fragments.OnFeatureActionListener;
import mobi.maptrek.fragments.OnLocationListener;
import mobi.maptrek.fragments.OnMapActionListener;
import mobi.maptrek.fragments.OnRouteActionListener;
import mobi.maptrek.fragments.OnTrackActionListener;
import mobi.maptrek.fragments.OnPlaceActionListener;
import mobi.maptrek.fragments.PanelMenuFragment;
import mobi.maptrek.fragments.PanelMenuItem;
import mobi.maptrek.fragments.RouteEdit;
import mobi.maptrek.fragments.RouteInformation;
import mobi.maptrek.fragments.Ruler;
import mobi.maptrek.fragments.TextSearch;
import mobi.maptrek.fragments.TrackInformation;
import mobi.maptrek.dialogs.TrackProperties;
import mobi.maptrek.fragments.PlaceInformation;
import mobi.maptrek.dialogs.PlaceProperties;
import mobi.maptrek.dialogs.WhatsNew;
import mobi.maptrek.fragments.preferences.BasePreferences;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.layers.CrosshairLayer;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.MapCoverageLayer;
import mobi.maptrek.layers.MapEventLayer;
import mobi.maptrek.layers.MapObjectLayer;
import mobi.maptrek.layers.MapTrekTileLayer;
import mobi.maptrek.layers.NavigationLayer;
import mobi.maptrek.layers.RouteLayer;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerLayer;
import mobi.maptrek.layers.marker.MarkerSymbol;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.BaseLocationService.TRACKING_STATE;
import mobi.maptrek.location.BaseNavigationService;
import mobi.maptrek.location.GraphHopperService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.INavigationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.location.NavigationService;
import mobi.maptrek.maps.MapWorker;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.maps.plugin.PluginOnlineTileSource;
import mobi.maptrek.plugin.PluginRepository;
import mobi.maptrek.util.ContextUtils;
import mobi.maptrek.util.SafeResultReceiver;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.Themes;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.LabelTileLoaderHook;
import mobi.maptrek.maps.maptrek.MapTrekTileSource;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.provider.ExportProvider;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.MathUtils;
import mobi.maptrek.util.Osm;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;
import mobi.maptrek.view.Gauge;
import mobi.maptrek.viewmodels.AmenityViewModel;
import mobi.maptrek.viewmodels.DataSourceViewModel;
import mobi.maptrek.viewmodels.MapIndexViewModel;
import mobi.maptrek.viewmodels.MapViewModel;
import mobi.maptrek.viewmodels.PlaceViewModel;
import mobi.maptrek.viewmodels.RouteViewModel;
import mobi.maptrek.viewmodels.TrackViewModel;

public class MainActivity extends AppCompatActivity implements ILocationListener,
        DataHolder,
        MapHolder,
        Map.InputListener,
        Map.UpdateListener,
        GestureListener,
        FragmentHolder,
        OnLocationListener,
        OnPlaceActionListener,
        OnTrackActionListener,
        OnRouteActionListener,
        OnMapActionListener,
        OnFeatureActionListener,
        ItemizedLayer.OnItemGestureListener<MarkerItem>,
        MapTrekTileLayer.OnAmenityGestureListener,
        PopupMenu.OnMenuItemClickListener,
        LoaderManager.LoaderCallbacks<List<FileDataSource>>,
        AmenitySetup.AmenitySetupDialogCallback, SafeResultReceiver.Callback {
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_NOTIFICATION = 2;

    private static final int MAP_EVENTS = 1;
    private static final int MAP_BASE = 2;
    private static final int MAP_MAPS = 3;
    private static final int MAP_MAP_OVERLAYS = 4;
    private static final int MAP_3D = 5;
    private static final int MAP_LABELS = 6;
    private static final int MAP_DATA = 7;
    private static final int MAP_DYNAMIC_DATA = 8;
    private static final int MAP_3D_DATA = 9;
    private static final int MAP_POSITIONAL = 10;
    private static final int MAP_OVERLAYS = 11;

    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;
    public static final int MAP_ZOOM_ANIMATION_DURATION = 100;

    private static final int NIGHT_CHECK_PERIOD = 180000; // 3 minutes
    private static final int TRACK_ROTATION_DELAY = 1000; // 1 second

    private enum PANEL_STATE {
        NONE,
        LOCATION,
        TRACKS,
        PLACES,
        MAPS,
        MORE
    }

    private float mFingerTipSize;
    private int mColorAccent;
    private int mColorActionIcon;
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
    private final MapPosition mMapPosition = new MapPosition();
    private GeoPoint mSelectedPoint;
    private boolean mPositionLocked = false;
    private int mMovingOffset = 0;
    private int mTrackingOffset = 0;
    private double mTrackingOffsetFactor = 1;
    private long mTrackingDelay;
    private float mAutoTilt;
    private boolean mAutoTiltSet;
    private boolean mAutoTiltShouldSet;
    private boolean mBuildingsLayerEnabled = true;
    private boolean mHideMapObjects = true;
    private int mBitmapMapTransparency = 0;

    private Map mMap;
    private ActivityMainBinding mViews;
    private boolean mVerticalOrientation;
    private int mSlideGravity;

    private long mStartTime;
    private long mLastLocationMilliseconds = 0L;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0f;

    private SunriseSunset mSunriseSunset;
    private boolean mNightMode = false;
    private long mNextNightCheck = 0L;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    private MapEventLayer mMapEventLayer;
    private VectorTileLayer mBaseLayer;
    private BitmapTileLayer mHillshadeLayer;
    private S3DBLayer mBuildingsLayer;
    private MapScaleBarLayer mMapScaleBarLayer;
    private LabelTileLoaderHook mLabelTileLoaderHook;
    private LabelLayer mLabelsLayer;
    private TileGridLayer mGridLayer;
    private NavigationLayer mNavigationLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private CrosshairLayer mCrosshairLayer;
    private LocationOverlay mLocationOverlay;
    private MapCoverageLayer mMapCoverageLayer;
    private MarkerItem mActiveMarker;
    private MarkerItem marker;


    private FragmentManager mFragmentManager;
    private PANEL_STATE mPanelState;
    private boolean secondBack;
    private Toast mBackToast;
    private final Stack<Pair<Integer, View.OnClickListener>> listFabStack = new Stack<>();

    private AmenityViewModel amenityViewModel;
    private DataSourceViewModel dataSourceViewModel;
    private MapIndexViewModel mapIndexViewModel;
    private MapViewModel mapViewModel;
    private PlaceViewModel placeViewModel;
    private RouteViewModel routeViewModel;
    private TrackViewModel trackViewModel;

    private SQLiteDatabase mDetailedMapDatabase;
    private MapIndex mMapIndex;
    private MapTrekTileSource mNativeTileSource;
    private List<MapFile> mBitmapLayerMaps;
    private Set<Place> mDeletedPlaces;
    private Set<Track> mDeletedTracks;
    private int mTotalDataItems = 0;
    private boolean mFirstMove = true;
    private boolean mBaseMapWarningShown = false;
    private boolean mObjectInteractionEnabled = true;
    private boolean mAskedNotificationPermission = false;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler mMainHandler;
    private WeakReference<SafeResultReceiver> mResultReceiver;

    private PlaceBroadcastReceiver mPlaceBroadcastReceiver;
    private PluginRepository mPluginRepository;

    @SuppressLint({"ShowToast", "UseCompatLoadingForDrawables"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        // Required for transparent status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        mViews = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mViews.getRoot());

        // Required for proper positioning of bottom action panel
        ViewCompat.setOnApplyWindowInsetsListener(mViews.getRoot(), (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = systemBarsInsets.left;
            mlp.bottomMargin = systemBarsInsets.bottom;
            mlp.rightMargin = systemBarsInsets.right;
            v.setLayoutParams(mlp);

            int left = 0;
            int top = systemBarsInsets.top;

            if (Build.VERSION.SDK_INT >= 28) {
                DisplayCutoutCompat cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    logger.info("Top DisplayCutout: {}", cutout.getSafeInsetTop());
                    logger.info("Left DisplayCutout: {}", cutout.getSafeInsetLeft());
                    // TODO: implement for gauge panel
                    // left = cutout.getSafeInsetLeft();
                    top = Math.max(top, cutout.getSafeInsetTop());
                }
            }

            FrameLayout.MarginLayoutParams flp = (FrameLayout.MarginLayoutParams) mViews.constraintLayout.getLayoutParams();
            flp.topMargin = top;
            flp.leftMargin = left;
            mViews.constraintLayout.setLayoutParams(flp);

            // Return CONSUMED if you don't want the window insets to keep being
            // passed down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        MapTrek application = MapTrek.getApplication();

        // Initialize cached resources
        Resources resources = getResources();
        Resources.Theme theme = getTheme();
        mColorAccent = resources.getColor(R.color.colorAccent, theme);
        mColorActionIcon = resources.getColor(R.color.actionIconColor, theme);
        mPanelBackground = resources.getColor(R.color.panelBackground, theme);
        mPanelSolidBackground = resources.getColor(R.color.panelSolidBackground, theme);
        mPanelExtendedBackground = resources.getColor(R.color.panelExtendedBackground, theme);

        // Restore most important state ASAP
        mVerticalOrientation = resources.getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        mSlideGravity = mVerticalOrientation ? Gravity.BOTTOM : Gravity.END;
        mPanelState = PANEL_STATE.NONE;
        if (savedInstanceState != null) {
            setPanelState((PANEL_STATE) savedInstanceState.getSerializable("panelState"));
            if (mPanelState != PANEL_STATE.NONE)
                mViews.extendPanel.post(this::adjustPanelViews);
        }

        // Prepare handlers
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.setPriority(Thread.MIN_PRIORITY);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        // Estimate finger tip height (0.1 inch is obtained from experiments)
        mFingerTipSize = (float) (MapTrek.ydpi * 0.1);

        mSunriseSunset = new SunriseSunset();

        // Apply default styles at start
        MarkerStyle.DEFAULT_COLOR = Configuration.loadInt(Configuration.PREF_PLACE_COLOR, getColor(R.color.placeColor01));
        TrackStyle.DEFAULT_COLOR = Configuration.loadInt(Configuration.PREF_TRACK_COLOR, getColor(R.color.trackColor));
        TrackStyle.DEFAULT_WIDTH = Configuration.loadInt(Configuration.PREF_TRACK_WIDTH, resources.getInteger(R.integer.default_track_width));
        RouteStyle.DEFAULT_COLOR = Configuration.loadInt(Configuration.PREF_ROUTE_COLOR, getColor(R.color.routeColor));
        RouteStyle.DEFAULT_WIDTH = Configuration.loadInt(Configuration.PREF_ROUTE_WIDTH, resources.getInteger(R.integer.default_route_width));

        // find the retained fragment on activity restarts
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.registerFragmentLifecycleCallbacks(mFragmentLifecycleCallback, true);

        mMapIndex = application.getExtraMapIndex();

        mPluginRepository = application.getPluginRepository();

        if (savedInstanceState == null) {
            String language = Configuration.getLanguage();
            if (language == null) {
                language = resources.getConfiguration().locale.getLanguage();
                if (!Arrays.asList(new String[]{"en", "de", "ru"}).contains(language))
                    language = "none";
                Configuration.setLanguage(language);
            }
        }

        mBitmapLayerMaps = application.getBitmapLayerMaps();
        if (mBitmapLayerMaps == null)
            mBitmapLayerMaps = mMapIndex.getMaps(Configuration.getBitmapMaps());

        mLocationState = LocationState.DISABLED;
        mSavedLocationState = LocationState.DISABLED;

        mViews.license.setClickable(true);
        mViews.license.setMovementMethod(LinkMovementMethod.getInstance());

        mViews.navigationArrow.setOnClickListener(v -> setMapLocation(mNavigationService.getCurrentPoint()));
        mViews.navigationArrow.setOnLongClickListener(v -> showNavigationMenu());
        mViews.navigationSign.setOnClickListener(v -> setMapLocation(mNavigationService.getCurrentPoint()));

        mViews.extendPanel.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                adjustPanelViews();
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });

        int lastIntroduction = Configuration.getLastSeenIntroduction();

        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        mMap = mViews.mapView.map();
        if (lastIntroduction == 0) {
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
        } else {
            mMap.setMapPosition(Configuration.getPosition());
        }
        mAutoTilt = Configuration.getAutoTilt();

        mNavigationNorthDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_north, theme);
        mNavigationTrackDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_track, theme);
        mMyLocationDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_my_location, theme);
        mLocationSearchingDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_location_searching, theme);

        Tags.accessibility = Configuration.getAccessibilityBadgesEnabled();

        Layers layers = mMap.layers();
        layers.addGroup(MAP_EVENTS);
        layers.addGroup(MAP_BASE);

        try {
            mDetailedMapDatabase = application.getDetailedMapDatabase();
            mNativeTileSource = new MapTrekTileSource(mDetailedMapDatabase);
            mNativeTileSource.setContoursEnabled(Configuration.getContoursEnabled());
            mBaseLayer = new MapTrekTileLayer(mMap, mNativeTileSource, this);
            mMap.setBaseMap(mBaseLayer); // will go to base group
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        // setBaseMap does not operate with layer groups so we add remaining groups later
        layers.addGroup(MAP_MAPS);
        layers.addGroup(MAP_MAP_OVERLAYS);
        layers.addGroup(MAP_3D);
        layers.addGroup(MAP_LABELS);
        layers.addGroup(MAP_DATA);
        layers.addGroup(MAP_DYNAMIC_DATA);
        layers.addGroup(MAP_3D_DATA);
        layers.addGroup(MAP_POSITIONAL);
        layers.addGroup(MAP_OVERLAYS);

        if (Configuration.getHillshadesEnabled())
            showHillShade();

        mGridLayer = new TileGridLayer(mMap, MapTrek.density * .75f);
        if (Configuration.getGridLayerEnabled())
            layers.add(mGridLayer, MAP_OVERLAYS);

        mBuildingsLayerEnabled = Configuration.getBuildingsLayerEnabled();
        if (mBuildingsLayerEnabled && mBaseLayer != null) {
            mBuildingsLayer = new S3DBLayer(mMap, mBaseLayer, true);
            layers.add(mBuildingsLayer, MAP_3D);
        }

        mLabelTileLoaderHook = new LabelTileLoaderHook(mapViewModel.shieldFactory, mapViewModel.osmcSymbolFactory);
        String language = Configuration.getLanguage();
        if (!"none".equals(language))
            mLabelTileLoaderHook.setPreferredLanguage(language);
        if (mBaseLayer != null) {
            mLabelsLayer = new LabelLayer(mMap, mBaseLayer, mLabelTileLoaderHook);
            layers.add(mLabelsLayer, MAP_LABELS);
        }

        int paintColor = resources.getColor(R.color.textColorPrimary, theme);
        int strokeColor = resources.getColor(R.color.colorBackground, theme);
        DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap, MapTrek.density * .75f, paintColor, strokeColor);
        mMapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
        mCrosshairLayer = new CrosshairLayer(mMap, MapTrek.density, paintColor, () -> Configuration.setPosition(mMap.getMapPosition()));

        int type = Configuration.getPointerType();
        int size = Configuration.getPointerSize();
        int color = Configuration.getPointerColor();
        mLocationOverlay = new LocationOverlay(mMap, type, size, color, MapTrek.density);

        layers.add(mMapScaleBarLayer, MAP_OVERLAYS);
        layers.add(mCrosshairLayer, MAP_OVERLAYS);
        layers.add(mLocationOverlay, MAP_POSITIONAL);

        layers.add(new MapObjectLayer(mMap, MapTrek.density), MAP_3D_DATA);

        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this));
        MarkerSymbol symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
        mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<>(), symbol, MapTrek.density, strokeColor, this);
        mMarkerLayer.setTitlesEnabled(Configuration.getPlaceTitlesEnabled());
        layers.add(mMarkerLayer, MAP_3D_DATA);

        mapIndexViewModel = new ViewModelProvider(this).get(MapIndexViewModel.class);
        dataSourceViewModel = new ViewModelProvider(this).get(DataSourceViewModel.class);
        if (savedInstanceState != null) {
            dataSourceViewModel.showNativeTracks(savedInstanceState.getBoolean("nativeTracks"));
        }
        dataSourceViewModel.dataSourceCommand.observe(this, dataSourceCommandObserver);

        mapViewModel.rotationLocked.observe(this, locked -> {
            mMap.getEventLayer().enableRotation(!locked);
            mMap.getEventLayer().enableTilt(!locked);
            if (locked) {
                mViews.compass.setVisibility(View.VISIBLE);
                mViews.compassLock.setVisibility(View.VISIBLE);
                if (mLocationState == LocationState.TRACK) {
                    mLocationState = LocationState.NORTH;
                    updateLocationDrawable();
                }
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(0f);
                mMapPosition.setTilt(0f);
                mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mMapPosition);
            } else {
                mViews.compass.setVisibility(View.GONE);
                mViews.compassLock.setVisibility(View.GONE);
            }
        });
        // Observe marker state
        mapViewModel.getMarkerState().observe(this, markerState -> {
            // There can be only one marker at a time
            if (marker != null) {
                mMarkerLayer.removeItem(marker);
                marker.getMarker().getBitmap().recycle();
                marker = null;
            }
            if (markerState.isShown()) {
                marker = new MarkerItem(null, null, markerState.getCoordinates());
                int drawable = markerState.isAmenity() ? R.drawable.circle_marker : R.drawable.round_marker;
                Bitmap markerBitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, drawable, mColorAccent));
                marker.setMarker(new MarkerSymbol(markerBitmap, MarkerItem.HotspotPlace.CENTER));
                mMarkerLayer.addItem(marker);
            }
            mMap.updateMap(true);
        });

        placeViewModel = new ViewModelProvider(this).get(PlaceViewModel.class);
        trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
        routeViewModel = new ViewModelProvider(this).get(RouteViewModel.class);
        routeViewModel.isRouting.observe(this, routing -> {
            for (Layer layer : mMap.layers())
                if (layer instanceof RouteLayer && layer != mNavigationLayer)
                    layer.setEnabled(!routing);
        });
        amenityViewModel = new ViewModelProvider(this).get(AmenityViewModel.class);
        // Observe amenity state
        amenityViewModel.getAmenity().observe(this, amenity -> {
            if (amenity != null) {
                mapViewModel.showMarker(amenity.coordinates, amenity.name, true);
            } else {
                mapViewModel.removeMarker();
            }
        });

        // Load places
        dataSourceViewModel.placeDbDataSource.open();
        for (Place place : dataSourceViewModel.placeDbDataSource.getPlaces()) {
            addPlaceMarker(place);
            mTotalDataItems++;
        }

        mPlaceBroadcastReceiver = new PlaceBroadcastReceiver();
        ContextCompat.registerReceiver(this, mPlaceBroadcastReceiver, new IntentFilter(PlaceDbDataSource.BROADCAST_PLACES_MODIFIED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mPlaceBroadcastReceiver, new IntentFilter(PlaceDbDataSource.BROADCAST_PLACES_RESTORED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mPlaceBroadcastReceiver, new IntentFilter(PlaceDbDataSource.BROADCAST_PLACES_REWRITTEN), ContextCompat.RECEIVER_NOT_EXPORTED);

        mHideMapObjects = Configuration.getHideMapObjects();
        mBitmapMapTransparency = Configuration.getBitmapMapTransparency();
        for (MapFile bitmapLayerMap : mBitmapLayerMaps)
            showBitmapMap(bitmapLayerMap, false);

        setMapTheme();

        mBackToast = Toast.makeText(this, R.string.msgBackQuit, Toast.LENGTH_SHORT);
        mProgressHandler = new ProgressHandler(mViews.progressBar);

        // Initialize UI event handlers
        mViews.locationButton.setOnClickListener(v -> onLocationClicked());
        mViews.locationButton.setOnLongClickListener(v -> {
            onLocationLongClicked();
            return true;
        });
        mViews.tracksButton.setOnClickListener(v -> onTracksClicked());
        mViews.tracksButton.setOnLongClickListener(v -> {
            onTracksLongClicked();
            return true;
        });
        mViews.placesButton.setOnClickListener(v -> onPlacesClicked());
        mViews.placesButton.setOnLongClickListener(v -> {
            onPlacesLongClicked();
            return true;
        });
        mViews.mapsButton.setOnClickListener(v -> onMapsClicked());
        mViews.mapsButton.setOnLongClickListener(v -> {
            onMapsLongClicked();
            return true;
        });
        mViews.moreButton.setOnClickListener(v -> onMoreClicked());
        mViews.moreButton.setOnLongClickListener(v -> {
            onMoreLongClicked();
            return true;
        });
        mViews.mapDownloadButton.setOnClickListener(v -> onMapDownloadClicked());
        mViews.compass.setOnLongClickListener(this::onCompassLongClicked);
        mViews.navigationSign.setOnClickListener(v -> setMapLocation(mNavigationService.getCurrentPoint()));

        // Resume state
        int state = Configuration.getLocationState();
        if (state >= LocationState.NORTH.ordinal())
            mSavedLocationState = LocationState.values()[state];
        state = Configuration.getPreviousLocationState();
        mPreviousLocationState = LocationState.values()[state];
        mViews.gaugePanel.initializeGauges(Configuration.getGauges());
        showActionPanel(Configuration.getActionPanelState(), false);

        boolean visible = Configuration.getZoomButtonsVisible();
        mViews.coordinatorLayout.findViewById(R.id.mapZoomHolder).setVisibility(visible ? View.VISIBLE : View.GONE);

        // Get back to full screen mode after edge swipe
        /*
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        boolean hide = Configuration.getHideSystemUI();
                        if (hide && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            final Message m = Message.obtain(mMainHandler, new Runnable() {
                                @Override
                                public void run() {
                                    hideSystemUI();
                                }
                            });
                            m.what = R.id.msgHideSystemUI;
                            mMainHandler.sendMessageDelayed(m, 5000);
                        }
                    }
                });
        */

        mStartTime = SystemClock.uptimeMillis();

        onNewIntent(getIntent());

        if (lastIntroduction < IntroductionActivity.CURRENT_INTRODUCTION)
            startActivity(new Intent(this, IntroductionActivity.class));
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        logger.debug("New intent: {}", action);
        String scheme = intent.getScheme();
        if ("mobi.maptrek.action.CENTER_ON_COORDINATES".equals(action)) {
            MapPosition position = mMap.getMapPosition();
            double lat = intent.getDoubleExtra("lat", position.getLatitude());
            double lon = intent.getDoubleExtra("lon", position.getLongitude());
            position.setPosition(lat, lon);
            setMapLocation(position.getGeoPoint());
        } else if ("mobi.maptrek.action.NAVIGATE_TO_OBJECT".equals(action)) {
            startNavigation(intent.getLongExtra(NavigationService.EXTRA_ID, 0L));
        } else if ("geo".equals(scheme)) {
            Uri uri = intent.getData();
            if (uri == null)
                return;
            logger.debug("   {}", uri);
            String data = uri.getSchemeSpecificPart();
            String query = null;
            // String query = uri.getQuery(); returns null for some reason
            int queryIdx = data.indexOf('?');
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom
            // geo:0,0?q=lat,lng(label)
            // geo:0,0?q=lat, lng - buggy Instagram (with space)
            int zoom = 0;
            if (queryIdx >= 0) {
                query = data.substring(queryIdx + 1);
                data = data.substring(0, queryIdx);
                if (query.startsWith("z="))
                    try {
                        zoom = Integer.parseInt(query.substring(2));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
            }
            try {
                logger.info(data);
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
                showMarkerInformation(position.getGeoPoint(), marker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            // http://wiki.openstreetmap.org/wiki/Shortlink
            Uri uri = intent.getData();
            if (uri == null)
                return;
            logger.debug("   {}", uri);
            List<String> path = uri.getPathSegments();
            if ("go".equals(path.get(0))) {
                MapPosition position = Osm.decodeShortLink(path.get(1));
                String marker = uri.getQueryParameter("m");
                mMap.setMapPosition(position);
                showMarkerInformation(position.getGeoPoint(), marker);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start loading user data
        DataLoader loader = (DataLoader) LoaderManager.getInstance(this).initLoader(0, null, this);
        loader.setProgressHandler(mProgressHandler);

        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(MapWorker.BROADCAST_MAP_STARTED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(MapWorker.BROADCAST_MAP_ADDED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(MapWorker.BROADCAST_MAP_REMOVED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(MapWorker.BROADCAST_MAP_FAILED), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_STATE), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_SAVE), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATUS), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED);

        MapTrek application = MapTrek.getApplication();
        SafeResultReceiver resultReceiver = application.getResultReceiver();
        if (resultReceiver == null) {
            resultReceiver = new SafeResultReceiver();
            application.setResultReceiver(resultReceiver);
        }
        resultReceiver.setCallback(this);
        mResultReceiver = new WeakReference<>(resultReceiver);

        if (Configuration.getConfirmExitEnabled())
            getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
        mBackPressedCallback.setEnabled(mFragmentManager.getBackStackEntryCount() == 0);
        MapTrek.isMainActivityRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSavedLocationState != LocationState.DISABLED)
            askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);

        TRACKING_STATE savedState = TRACKING_STATE.values()[Configuration.getTrackingState()];
        logger.info("Saved tracking state: {}", savedState);
        logger.info("Current tracking state: {}", trackViewModel.trackingState.getValue());
        if (savedState != trackViewModel.trackingState.getValue()) {
            if (savedState == TRACKING_STATE.TRACKING)
                enableTracking();
            else if (savedState == TRACKING_STATE.PAUSED)
                trackViewModel.trackingState.setValue(savedState);
        }

        trackViewModel.trackingState.observe(this, trackingState -> {
            logger.info("trackingState changed: {}", trackingState);
            int recordColor = trackingState == TRACKING_STATE.TRACKING ? mColorAccent : mColorActionIcon;
            mViews.tracksButton.getDrawable().setTint(recordColor);

            if ((trackingState == TRACKING_STATE.TRACKING || trackingState == TRACKING_STATE.PAUSED) && mCurrentTrackLayer == null) {
                mCurrentTrackLayer = new CurrentTrackLayer(mMap, getApplicationContext(), this);
                mMap.layers().add(mCurrentTrackLayer, MAP_DYNAMIC_DATA);
                mMap.updateMap(true);
                if (trackingState == TRACKING_STATE.TRACKING) {
                    if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_RECORD_TRACK))
                        HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_RECORD_TRACK, R.string.advice_record_track, mViews.tracksButton, false, new TapTargetView.Listener() {
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                askForPermission(PERMISSIONS_REQUEST_NOTIFICATION);
                            }
                        });
                    else
                        askForPermission(PERMISSIONS_REQUEST_NOTIFICATION);
                }
            }
            if (trackingState == TRACKING_STATE.DISABLED && mCurrentTrackLayer != null) {
                mMap.layers().remove(mCurrentTrackLayer);
                mCurrentTrackLayer.onDetach();
                mCurrentTrackLayer = null;
                trackViewModel.currentTrack.setValue(null);
                mMap.updateMap(true);
            }
        });
        trackViewModel.trackingCommand.observe(this, trackingCommand -> {
            TRACKING_STATE trackingState = trackViewModel.trackingState.getValue();
            logger.info("trackingCommand received: {}, state: {}", trackingCommand, trackingState);
            if (trackingState == trackingCommand)
                return;
            if (trackingCommand == TRACKING_STATE.PENDING && trackingState != TRACKING_STATE.TRACKING) {
                if (mLocationState == LocationState.DISABLED) {
                    trackViewModel.trackingState.setValue(TRACKING_STATE.PENDING);
                    askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
                } else {
                    enableTracking();
                }

            }
            if (trackingCommand == TRACKING_STATE.PAUSED)
                pauseTracking();
            if (trackingCommand == TRACKING_STATE.DISABLED)
                disableTracking();
        });

        // Resume navigation
        if (Configuration.getNavigationPoint() != null)
            resumeNavigation();

        mMapEventLayer = new MapEventLayer(mMap, this);
        mMap.layers().add(mMapEventLayer, MAP_EVENTS);
        mMap.events.bind(this);
        mMap.input.bind(this);
        mViews.mapView.onResume();
        updateLocationDrawable();
        adjustCompass(mMap.getMapPosition().bearing);

        if ("".contentEquals(mViews.license.getText()))
            showLicense(null);

        String userNotification = MapTrek.getApplication().getUserNotification();
        if (userNotification != null)
            HelperUtils.showError(userNotification, mViews.coordinatorLayout);

        if (MapTrek.getApplication().hasPreviousRunsExceptions()) {
            CrashReport dialogFragment = new CrashReport();
            dialogFragment.show(mFragmentManager, "crashReport");
        } else if (!mBaseMapWarningShown && mapIndexViewModel.nativeIndex.getBaseMapVersion() == 0) {
            BaseMapDownload dialogFragment = new BaseMapDownload();
            dialogFragment.show(mFragmentManager, "baseMapDownload");
            mBaseMapWarningShown = true;
        } else if (WhatsNew.shouldShow()) {
            WhatsNew dialogFragment = new WhatsNew();
            dialogFragment.show(mFragmentManager, "whatsNew");
        }

        if (Configuration.getHideSystemUI())
            hideSystemUI();

        int type = Configuration.getHighlightedType();
        if (type >= 0 && mViews.highlightedType.getVisibility() != View.VISIBLE)
            setHighlightedType(type);

        mViews.routePointBarrier.addOnLayoutChangeListener(layoutChangeListener);
        mViews.gaugesRightBarrier.addOnLayoutChangeListener(layoutChangeListener);
        mViews.gaugesBottomBarrier.addOnLayoutChangeListener(layoutChangeListener);
        mViews.contentBarrier.addOnLayoutChangeListener(layoutChangeListener);

        mMap.updateMap(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mLocationState != LocationState.SEARCHING)
            mSavedLocationState = mLocationState;

        mViews.routePointBarrier.removeOnLayoutChangeListener(layoutChangeListener);
        mViews.gaugesRightBarrier.removeOnLayoutChangeListener(layoutChangeListener);
        mViews.gaugesBottomBarrier.removeOnLayoutChangeListener(layoutChangeListener);
        mViews.contentBarrier.removeOnLayoutChangeListener(layoutChangeListener);

        mViews.mapView.onPause();
        mMap.events.unbind(this);
        mMap.layers().remove(mMapEventLayer);
        mMapEventLayer = null;
        mViews.gaugePanel.onVisibilityChanged(false);

        // save the map position and state
        Configuration.setPosition(mMap.getMapPosition());
        Configuration.setBitmapMaps(mBitmapLayerMaps);
        Configuration.setLocationState(mSavedLocationState.ordinal());
        Configuration.setPreviousLocationState(mPreviousLocationState.ordinal());
        Configuration.setGauges(mViews.gaugePanel.getGaugeSettings());

        if (!isChangingConfigurations()) {
            if (trackViewModel.trackingState.getValue() != TRACKING_STATE.TRACKING)
                stopService(new Intent(getApplicationContext(), LocationService.class));

            if (mNavigationService != null && !mNavigationService.isNavigating())
                stopService(new Intent(getApplicationContext(), NavigationService.class));
            disableNavigation();
            disableLocations();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        MapTrek.isMainActivityRunning = false;
        mBackPressedCallback.remove();
        listFabStack.clear();

        mResultReceiver.get().setCallback(null);

        unregisterReceiver(mBroadcastReceiver);

        Loader<List<FileDataSource>> loader = LoaderManager.getInstance(this).getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).setProgressHandler(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        long runningTime = (SystemClock.uptimeMillis() - mStartTime) / 60000;
        Configuration.updateRunningTime(runningTime);

        if (mDeletedPlaces != null) {
            deletePlaces(mDeletedPlaces);
            mDeletedPlaces = null;
        }
        if (mDeletedTracks != null) {
            deleteTracks(mDeletedTracks);
            mDeletedTracks = null;
        }

        if (mMap != null)
            mMap.destroy();
        //mMapScaleBar.destroy();

        for (FileDataSource source : dataSourceViewModel.fileDataSources)
            source.setVisible(false);

        if (mPlaceBroadcastReceiver != null) {
            unregisterReceiver(mPlaceBroadcastReceiver);
            mPlaceBroadcastReceiver = null;
        }

        mDetailedMapDatabase = null;
        mProgressHandler = null;

        if (mBackgroundThread != null) {
            logger.debug("  stopping threads...");
            mBackgroundThread.interrupt();
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundThread.quit();
            mBackgroundThread = null;
        }

        mMainHandler = null;

        if (isFinishing()) {
            sendBroadcast(new Intent("mobi.maptrek.plugins.action.FINALIZE"));
            if (mBitmapLayerMaps != null) {
                for (MapFile bitmapLayerMap : mBitmapLayerMaps)
                    bitmapLayerMap.tileSource.close();
                mBitmapLayerMaps.clear();
            }
        }

        //mFragmentManager = null;

        Configuration.commit();
        logger.debug("  done!");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        MapTrek application = MapTrek.getApplication();
        application.setBitmapLayerMaps(mBitmapLayerMaps);

        if (mLocationService != null)
            startService(new Intent(getApplicationContext(), LocationService.class));
        if (mNavigationService != null)
            startService(new Intent(getApplicationContext(), NavigationService.class));

        savedInstanceState.putSerializable("savedLocationState", mSavedLocationState);
        savedInstanceState.putSerializable("previousLocationState", mPreviousLocationState);
        savedInstanceState.putLong("lastLocationMilliseconds", mLastLocationMilliseconds);
        savedInstanceState.putFloat("averagedBearing", mAveragedBearing);
        savedInstanceState.putInt("movementAnimationDuration", mMovementAnimationDuration);
        savedInstanceState.putBoolean("savedNavigationState", mNavigationService != null);
        if (mViews.progressBar.getVisibility() == View.VISIBLE)
            savedInstanceState.putInt("progressBar", mViews.progressBar.getMax());
        savedInstanceState.putBoolean("nativeTracks", Boolean.TRUE.equals(dataSourceViewModel.getNativeTracksState().getValue()));
        savedInstanceState.putSerializable("panelState", mPanelState);
        savedInstanceState.putBoolean("autoTiltShouldSet", mAutoTiltShouldSet);
        savedInstanceState.putBoolean("baseMapWarningShown", mBaseMapWarningShown);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        logger.debug("onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        mSavedLocationState = (LocationState) savedInstanceState.getSerializable("savedLocationState");
        mPreviousLocationState = (LocationState) savedInstanceState.getSerializable("previousLocationState");
        mLastLocationMilliseconds = savedInstanceState.getLong("lastLocationMilliseconds");
        mAveragedBearing = savedInstanceState.getFloat("averagedBearing");
        mMovementAnimationDuration = savedInstanceState.getInt("movementAnimationDuration");
        if (savedInstanceState.getBoolean("savedNavigationState", false))
            enableNavigation();
        if (savedInstanceState.containsKey("progressBar")) {
            mViews.progressBar.setVisibility(View.VISIBLE);
            mViews.progressBar.setMax(savedInstanceState.getInt("progressBar"));
        }
        mAutoTiltShouldSet = savedInstanceState.getBoolean("autoTiltShouldSet");
        mBaseMapWarningShown = savedInstanceState.getBoolean("baseMapWarningShown");
    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                Intent intent = new Intent(Intent.ACTION_SEND, uri, MainActivity.this, DataImportActivity.class);
                startActivity(intent);
            }
    );

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int action = item.getItemId();
        if (action == R.id.actionNightMode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionNightMode);
            builder.setItems(R.array.night_mode_array, (dialog, which) -> {
                Configuration.setNightModeState(which - 1);
                AppCompatDelegate.setDefaultNightMode(which - 1);
                getDelegate().setLocalNightMode(which - 1);
                getDelegate().applyDayNight();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.actionStyle) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionStyle);
            builder.setItems(R.array.mapStyles, (dialog, which) -> {
                Configuration.setMapStyle(which);
                // With rule categories it became a long lasting operation
                // so it has to be run in background
                mBackgroundHandler.post(this::setMapTheme);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.actionMapScale) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionMapScale);
            builder.setItems(R.array.size_array, (dialog, which) -> {
                Configuration.setMapUserScale(which);
                // With rule categories it became a long lasting operation
                // so it has to be run in background
                mBackgroundHandler.post(this::setMapTheme);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.actionFontSize) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionFontSize);
            builder.setItems(R.array.size_array, (dialog, which) -> {
                Configuration.setMapFontSize(which);
                // With rule categories it became a long lasting operation
                // so it has to be run in background
                mBackgroundHandler.post(this::setMapTheme);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.actionLanguage) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionLanguage);
            builder.setItems(R.array.language_array, (dialog, which) -> {
                String[] languageCodes = getResources().getStringArray(R.array.language_code_array);
                String language = languageCodes[which];
                if ("none".equals(language)) {
                    mLabelTileLoaderHook.setPreferredLanguage(null);
                } else {
                    mLabelTileLoaderHook.setPreferredLanguage(language);
                }
                mMap.clearMap();
                Configuration.setLanguage(language);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.actionAmenityZooms) {
            AmenitySetup.Builder builder = new AmenitySetup.Builder();
            AmenitySetup dialog = builder.setCallback(this).create();
            dialog.show(mFragmentManager, "amenitySetup");
            return true;
        } else if (action == R.id.actionOtherFeatures) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            PanelMenuFragment fragment = (PanelMenuFragment) factory.instantiate(getClassLoader(), PanelMenuFragment.class.getName());
            fragment.setMenu(R.menu.menu_map_features, menu -> {
                menu.findItem(R.id.action3dBuildings).setChecked(mBuildingsLayerEnabled);
                menu.findItem(R.id.actionHillshades).setChecked(Configuration.getHillshadesEnabled());
                menu.findItem(R.id.actionContours).setChecked(Configuration.getContoursEnabled());
                menu.findItem(R.id.actionGrid).setChecked(mMap.layers().contains(mGridLayer));
            });
            showExtendPanel(PANEL_STATE.MAPS, "mapFeaturesMenu", fragment);
            return true;
        } else if (action == R.id.actionActivity) {
            int activity = Configuration.getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionActivity);
            builder.setSingleChoiceItems(R.array.activities, activity, (dialog, which) -> {
                dialog.dismiss();
                Configuration.setActivity(which);
                // With rule categories it became a long lasting operation
                // so it has to be run in background
                mBackgroundHandler.post(this::setMapTheme);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (action == R.id.action3dBuildings) {
            mBuildingsLayerEnabled = item.isChecked();
            if (mBuildingsLayerEnabled) {
                mBuildingsLayer = new S3DBLayer(mMap, mBaseLayer);
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
        } else if (action == R.id.actionHillshades) {// layer is managed in event subscription as it can be configured in other places
            Configuration.setHillshadesEnabled(item.isChecked());
            return true;
        } else if (action == R.id.actionContours) {
            mNativeTileSource.setContoursEnabled(item.isChecked());
            mMap.clearMap();
            Configuration.setContoursEnabled(item.isChecked());
            return true;
        } else if (action == R.id.actionGrid) {
            if (item.isChecked()) {
                mMap.layers().add(mGridLayer, MAP_OVERLAYS);
            } else {
                mMap.layers().remove(mGridLayer);
            }
            Configuration.setGridLayerEnabled(item.isChecked());
            mMap.updateMap(true);
            return true;
        } else if (action == R.id.actionAutoTilt) {
            mMap.getMapPosition(mMapPosition);
            if (item.isChecked()) {
                Configuration.setAutoTilt(65f);
                mAutoTilt = 65f;
            } else {
                Configuration.setAutoTilt(-1f);
                mAutoTilt = -1f;
                if (mAutoTiltSet) {
                    mMapPosition.setTilt(0f);
                    mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
                    mAutoTiltSet = false;
                }
            }
            return true;
        } else if (action == R.id.actionOverviewRoute) {
            if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                mLocationState = LocationState.ENABLED;
                updateLocationDrawable();
            }
            BoundingBox box = mNavigationService.getRouteBoundingBox();
            mMap.getMapPosition(mMapPosition);
            box.extend(mMapPosition.getLatitude(), mMapPosition.getLongitude());
            box.extendBy(0.05);
            mMap.animator().animateTo(box);
            return true;
        } else if (action == R.id.actionNextRoutePoint) {
            mNavigationService.nextRoutePoint();
            return true;
        } else if (action == R.id.actionPrevRoutePoint) {
            mNavigationService.prevRoutePoint();
            return true;
        } else if (action == R.id.actionStopNavigation) {
            stopNavigation();
            return true;
        } else if (action == R.id.actionManageMaps) {
            startMapSelection(true);
            return true;
        } else if (action == R.id.actionImport) {
            mGetContent.launch("*/*");
            return true;
        } else if (action == R.id.actionHideSystemUI) {
            if (Configuration.getHideSystemUI())
                showSystemUI();
            else
                hideSystemUI();
            return true;
        } else if (action == R.id.actionCreateRoute) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), RouteEdit.class.getName());
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "routeEdit");
            ft.addToBackStack("routeEdit");
            ft.commit();
            return true;
        } else if (action == R.id.actionRuler) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), Ruler.class.getName());
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "ruler");
            ft.addToBackStack("ruler");
            ft.commit();
            return true;
        } else if (action == R.id.actionAddGauge) {
            mViews.gaugePanel.onLongClick(mViews.gaugePanel);
            return true;
        } else if (action == R.id.actionLegend) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), Legend.class.getName());
            showExtendPanel(PANEL_STATE.MAPS, "legend", fragment);
            return true;
        } else if (action == R.id.actionSettings) {
            adjustGuideline(true);
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), BasePreferences.class.getName());
            fragment.setEnterTransition(new Slide(mSlideGravity));
            fragment.setReturnTransition(new Slide(mSlideGravity));
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "settings");
            ft.addToBackStack("settings");
            ft.commit();
            return true;
        } else if (action == R.id.actionSearch) {
            Bundle args = new Bundle(2);
            if (mLocationState != LocationState.DISABLED && mLocationService != null) {
                Location location = mLocationService.getLocation();
                args.putDouble(TextSearch.ARG_LATITUDE, location.getLatitude());
                args.putDouble(TextSearch.ARG_LONGITUDE, location.getLongitude());
            } else {
                MapPosition position = mMap.getMapPosition();
                args.putDouble(TextSearch.ARG_LATITUDE, position.getLatitude());
                args.putDouble(TextSearch.ARG_LONGITUDE, position.getLongitude());
            }
            if (mFragmentManager.getBackStackEntryCount() > 0) {
                popAll();
            }
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), TextSearch.class.getName());
            fragment.setArguments(args);
            showExtendPanel(PANEL_STATE.MORE, "search", fragment);
            return true;
        } else if (action == R.id.actionShareCoordinates) {
            mapViewModel.removeMarker();
            shareLocation(mSelectedPoint, null);
            return true;
        } else if (action == R.id.actionAddPlaceHere) {
            mapViewModel.removeMarker();
            String name = getString(R.string.place_name, Configuration.getPointsCounter());
            onPlaceCreate(mSelectedPoint, name, false, true);
            return true;
        } else if (action == R.id.actionNavigateHere) {
            mapViewModel.removeMarker();
            MapObject mapObject = new MapObject(mSelectedPoint.getLatitude(), mSelectedPoint.getLongitude());
            mapObject.name = getString(R.string.selectedLocation);
            startNavigation(mapObject);
            return true;
        } else if (action == R.id.actionFindRouteHere) {
            mapViewModel.removeMarker();
            Intent routeIntent = new Intent(Intent.ACTION_PICK, null, this, GraphHopperService.class);
            double[] points = new double[]{0.0, 0.0, 0.0, 0.0};
            if (mLocationState != LocationState.DISABLED && mLocationService != null) {
                Location location = mLocationService.getLocation();
                points[0] = location.getLatitude();
                points[1] = location.getLongitude();
            }
            points[2] = mSelectedPoint.getLatitude();
            points[3] = mSelectedPoint.getLongitude();
            routeIntent.putExtra(GraphHopperService.EXTRA_POINTS, points);
            routeIntent.putExtra(Intent.EXTRA_RESULT_RECEIVER, mResultReceiver.get());
            startService(routeIntent);
            return true;
        } else if (action == R.id.actionRememberScale) {
            HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_REMEMBER_SCALE, R.string.advice_remember_scale, mViews.popupAnchor, true);
            mapViewModel.removeMarker();
            mMap.getMapPosition(mMapPosition);
            Configuration.setRememberedScale((float) mMapPosition.getScale());
            return true;
        } else if (action == R.id.actionRememberTilt) {
            mapViewModel.removeMarker();
            mMap.getMapPosition(mMapPosition);
            mAutoTilt = mMapPosition.getTilt();
            Configuration.setAutoTilt(mAutoTilt);
            mAutoTiltSet = true;
            mAutoTiltShouldSet = true;
            return true;
        } else {
            Intent intent = item.getIntent();
            if (intent != null) {
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onLocationChanged() {
        if (mLocationState == LocationState.SEARCHING) {
            mLocationState = mSavedLocationState;
            //TODO Change from center to location pivot (see zooming)
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

        if (!mPositionLocked && mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
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
                mMapPosition.setBearing(-mAveragedBearing);
                offset = mTrackingOffset / mTrackingOffsetFactor;
                if (mAutoTilt > 0f && !mAutoTiltSet && mAutoTiltShouldSet)
                    mMapPosition.setTilt(mAutoTilt);
            } else {
                offset = mMovingOffset;
            }
            offset = offset / (mMapPosition.scale * Tile.SIZE);

            double rad = Math.toRadians(mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);

            mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
            mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);

            if (mMap.animator().isActive()) {
                MapPosition endPosition = mMap.animator().getEndPosition();
                mMapPosition.setTilt(endPosition.tilt);
                if (!rotate)
                    mMapPosition.setBearing(endPosition.bearing);
            }
            mMap.animator().animateTo(mMovementAnimationDuration, mMapPosition);
        }

        mLocationOverlay.setPosition(lat, lon, bearing);
        if (mNavigationLayer != null)
            mNavigationLayer.setPosition(lat, lon);
        mLastLocationMilliseconds = SystemClock.uptimeMillis();

        mapViewModel.currentLocation.setValue(location);

        // we use TIME for custom sunrise/sunset theme switching
        //noinspection deprecation
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_AUTO_TIME)
            checkNightMode(location);
    }

    @Override
    public void onGpsStatusChanged() {
        logger.debug("onGpsStatusChanged()");
        if (mLocationService.getStatus() == LocationService.GPS_SEARCHING) {
            int satellites = mLocationService.getSatellites();
            mViews.satellites.setText(String.format(Locale.getDefault(), "%d / %s", satellites >> 7, satellites & 0x7f));
            if (mLocationState != LocationState.SEARCHING) {
                mSavedLocationState = mLocationState;
                mLocationState = LocationState.SEARCHING;
                mMap.getEventLayer().setFixOnCenter(false);
                mLocationOverlay.setEnabled(false);
                mapViewModel.clearCurrentLocation();
                updateLocationDrawable();
            }
        }
    }

    private void onLocationClicked() {
        switch (mLocationState) {
            case DISABLED:
                askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
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
                mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mMapPosition);
                break;
            case NORTH:
                if (Boolean.FALSE.equals(mapViewModel.rotationLocked.getValue())) {
                    mLocationState = LocationState.TRACK;
                    mMap.getEventLayer().enableRotation(false);
                    mMap.getEventLayer().setFixOnCenter(true);
                    mTrackingDelay = SystemClock.uptimeMillis() + TRACK_ROTATION_DELAY;
                    mAutoTiltShouldSet = mMapPosition.getTilt() == 0f;
                    break;
                } // else continue to set ENABLED
            case TRACK:
                mLocationState = LocationState.ENABLED;
                mMap.getEventLayer().enableRotation(Boolean.FALSE.equals(mapViewModel.rotationLocked.getValue()));
                mMap.getEventLayer().setFixOnCenter(false);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(0);
                long duration = MAP_BEARING_ANIMATION_DURATION;
                if (mAutoTiltSet) {
                    mMapPosition.setTilt(0f);
                    mAutoTiltSet = false;
                    duration = MAP_POSITION_ANIMATION_DURATION;
                }
                mAutoTiltShouldSet = false;
                mMap.animator().animateTo(duration, mMapPosition);
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
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        Fragment fragment = factory.instantiate(getClassLoader(), LocationInformation.class.getName());
        fragment.setArguments(args);
        showExtendPanel(PANEL_STATE.LOCATION, "locationInformation", fragment);
    }

    private void onTracksClicked() {
        dataSourceViewModel.showNativeTracks(true);
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        Fragment fragment = factory.instantiate(getClassLoader(), DataSourceList.class.getName());
        showExtendPanel(PANEL_STATE.TRACKS, "nativeTrackList", fragment);
    }

    private void onTracksLongClicked() {
        if (trackViewModel.trackingState.getValue() == TRACKING_STATE.TRACKING) {
            Track currentTrack = mCurrentTrackLayer.getTrack();
            if (currentTrack.points.size() == 0)
                trackViewModel.trackingCommand.setValue(TRACKING_STATE.DISABLED);
            else
                onTrackDetails(currentTrack);
        } else {
            trackViewModel.trackingCommand.setValue(TRACKING_STATE.PENDING);
        }
    }

    private void onPlacesClicked() {
        boolean hasExtraSources = dataSourceViewModel.hasExtraDataSources();
        if (hasExtraSources) {
            dataSourceViewModel.showNativeTracks(false);
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            Fragment fragment = factory.instantiate(getClassLoader(), DataSourceList.class.getName());
            showExtendPanel(PANEL_STATE.PLACES, "dataSourceList", fragment);
        } else {
            dataSourceViewModel.selectDataSource(dataSourceViewModel.placeDbDataSource, DataSourceViewModel.MODE_PANEL);
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
        String name = getString(R.string.place_name, Configuration.getPointsCounter());
        onPlaceCreate(geoPoint, name, false, true);
    }

    private void onMapsClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(5);
        args.putDouble(MapList.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(MapList.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(MapList.ARG_ZOOM_LEVEL, mMapPosition.getZoomLevel());
        args.putBoolean(MapList.ARG_HIDE_OBJECTS, mHideMapObjects);
        args.putInt(MapList.ARG_TRANSPARENCY, mBitmapMapTransparency);
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        MapList fragment = (MapList) factory.instantiate(getClassLoader(), MapList.class.getName());
        fragment.setArguments(args);
        fragment.setMaps(mMapIndex.getMaps(), mBitmapLayerMaps);
        showExtendPanel(PANEL_STATE.MAPS, "mapsList", fragment);
    }

    private void onMapsLongClicked() {
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        PanelMenuFragment fragment = (PanelMenuFragment) factory.instantiate(getClassLoader(), PanelMenuFragment.class.getName());
        fragment.setMenu(R.menu.menu_map, menu -> {
            Resources resources = getResources();
            String[] nightModes = resources.getStringArray(R.array.night_mode_array_short);
            MenuItem item = menu.findItem(R.id.actionNightMode);
            TextView view = (TextView) item.getActionView();
            if (view != null)
                view.setText(nightModes[AppCompatDelegate.getDefaultNightMode() + 1]);
            String[] mapStyles = resources.getStringArray(R.array.mapStyles);
            item = menu.findItem(R.id.actionStyle);
            view = (TextView) item.getActionView();
            if (view != null)
                view.setText(mapStyles[Configuration.getMapStyle()]);
            String[] sizes = resources.getStringArray(R.array.size_array);
            item = menu.findItem(R.id.actionMapScale);
            view = (TextView) item.getActionView();
            if (view != null)
                view.setText(sizes[Configuration.getMapUserScale()]);
            item = menu.findItem(R.id.actionFontSize);
            view = (TextView) item.getActionView();
            if (view != null)
                view.setText(sizes[Configuration.getMapFontSize()]);
            item = menu.findItem(R.id.actionLanguage);
            view = (TextView) item.getActionView();
            if (view != null)
                view.setText(Configuration.getLanguage());
            menu.findItem(R.id.actionAutoTilt).setChecked(mAutoTilt != -1f);
        });
        showExtendPanel(PANEL_STATE.MAPS, "mapMenu", fragment);
        Configuration.setAdviceState(Configuration.ADVICE_MAP_SETTINGS);
    }

    private void onMoreClicked() {
        if (mViews.locationButton.getVisibility() == View.VISIBLE) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            PanelMenuFragment fragment = (PanelMenuFragment) factory.instantiate(getClassLoader(), PanelMenuFragment.class.getName());
            fragment.setMenu(R.menu.menu_main, menu -> {
                Resources resources = getResources();
                MenuItem item = menu.findItem(R.id.actionActivity);
                String[] activities = resources.getStringArray(R.array.activities);
                int activity = Configuration.getActivity();
                if (activity > 0) {
                    TextView textView = (TextView) item.getActionView();
                    if (textView != null)
                        textView.setText(activities[activity]);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    menu.findItem(R.id.actionHideSystemUI).setChecked(Configuration.getHideSystemUI());
                } else {
                    menu.removeItem(R.id.actionHideSystemUI);
                }
                if (mViews.gaugePanel.hasVisibleGauges() || (mLocationState != LocationState.NORTH && mLocationState != LocationState.TRACK))
                    menu.removeItem(R.id.actionAddGauge);
                java.util.Map<String, Pair<Drawable, Intent>> tools = mPluginRepository.getPluginTools();
                String[] toolNames = tools.keySet().toArray(new String[0]);
                Arrays.sort(toolNames, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
                for (String toolName : toolNames) {
                    Pair<Drawable, Intent> tool = tools.get(toolName);
                    item = menu.add(PanelMenuItem.HEADER_ID_UNDEFINED, 0, toolName);
                    //item.setIcon(tool.first);
                    if (tool != null)
                        item.setIntent(tool.second);
                }
            });
            showExtendPanel(PANEL_STATE.MORE, "panelMenu", fragment);
        } else {
            showActionPanel(true, true);
        }
    }

    private void onMoreLongClicked() {
        boolean show = mViews.locationButton.getVisibility() == View.INVISIBLE;
        showActionPanel(show, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !show && !Configuration.getHideSystemUI())
            hideSystemUI();
    }

    private void onMapDownloadClicked() {
        mViews.mapDownloadButton.setVisibility(View.GONE);
        mViews.mapDownloadButton.setTag(null);
        startMapSelection(false);
    }

    public void onZoomInClicked(View view) {
        zoomMap(2.0, 0, 0);
    }

    public void onZoomOutClicked(View view) {
        zoomMap(0.5, 0, 0);
    }

    public void onHighlightedTypeClicked(View view) {
        Tags.resetHighlightedType();
        Configuration.setHighlightedType(-1);
        mMap.clearMap();
        mViews.highlightedType.setVisibility(View.GONE);
    }

    public void onCompassClicked(View view) {
        if (mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.NORTH;
            updateLocationDrawable();
            mMap.getEventLayer().enableRotation(Boolean.FALSE.equals(mapViewModel.rotationLocked.getValue()));
        }
        mMap.getMapPosition(mMapPosition);
        mMapPosition.setBearing(0);
        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
    }

    public boolean onCompassLongClicked(View view) {
        mapViewModel.rotationLocked.setValue(Boolean.FALSE.equals(mapViewModel.rotationLocked.getValue()));
        return true;
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        if (!mObjectInteractionEnabled)
            return true;
        Object uid = item.getUid();
        if (uid != null)
            onPlaceDetails((Place) uid, false);
        return uid != null;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (!mObjectInteractionEnabled)
            return true;
        if (mLocationState != LocationState.DISABLED && mLocationState != LocationState.ENABLED)
            return false;
        Object uid = item.getUid();
        if (uid != null) {
            Place place = (Place) uid;
            if (place.locked) {
                Toast.makeText(this, R.string.msgPlaceLocked, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        mActiveMarker = item;
        // For better experience get delta from marker position and finger press
        // and consider it when moving marker
        Point point = new Point();
        mMap.viewport().toScreenPoint(item.getPoint(), point);
        deltaX = (float) (downX - point.x);
        deltaY = (float) (downY - point.y);
        // Shift map to reveal marker tip position
        mMap.getEventLayer().enableMove(false);
        mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(mMap.getWidth() / 2f, mMap.getHeight() / 2f + 3 * mFingerTipSize), 1, true);
        return true;
    }

    @Override
    public boolean onAmenitySingleTapUp(long amenityId) {
        onFeatureDetails(amenityId, false);
        return true;
    }

    private void enableLocations() {
        if (!LocationService.isGpsProviderEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.msgEnableGps)
                    .setPositiveButton(getString(R.string.actionSettings), (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {})
                    .show();
            return;
        }

        mIsLocationBound = bindService(new Intent(getApplicationContext(), LocationService.class), mLocationConnection, BIND_AUTO_CREATE);
        mLocationState = LocationState.SEARCHING;
        if (mSavedLocationState == LocationState.DISABLED) {
            mSavedLocationState = mPreviousLocationState;
            mPreviousLocationState = LocationState.NORTH;
        }
        if (trackViewModel.trackingState.getValue() == TRACKING_STATE.PENDING)
            enableTracking();
        if (mNavigationService != null && mNavigationService.isNavigating())
            askForPermission(PERMISSIONS_REQUEST_NOTIFICATION);
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
            mapViewModel.clearCurrentLocation();
            mMap.updateMap(true);
        }
        mLocationState = LocationState.DISABLED;
        updateLocationDrawable();
    }

    @Override
    public void setMapLocation(@NonNull GeoPoint point) {
        if (mSavedLocationState == LocationState.NORTH || mSavedLocationState == LocationState.TRACK) {
            mSavedLocationState = LocationState.ENABLED;
        }
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        MapPosition mapPosition = mMap.getMapPosition();
        if (mapPosition.scale > (2 << 7)) {
            mMap.animator().animateTo(point);
        } else {
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, point, 2 << 14, false);
        }
    }

    private final ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            logger.debug("onServiceConnected: LocationService");
            mLocationService = (ILocationService) binder;
            mLocationService.registerLocationCallback(MainActivity.this);
            mLocationService.setProgressListener(mProgressHandler);
        }

        public void onServiceDisconnected(ComponentName className) {
            logger.debug("onServiceDisconnected: LocationService");
            mLocationService = null;
        }
    };

    private void enableNavigation() {
        logger.warn("enableNavigation");
        mIsNavigationBound = bindService(new Intent(getApplicationContext(), NavigationService.class), mNavigationConnection, BIND_AUTO_CREATE);
    }

    private void disableNavigation() {
        logger.warn("disableNavigation");
        if (mIsNavigationBound) {
            unbindService(mNavigationConnection);
            mIsNavigationBound = false;
        }
        updateNavigationUI(true);
        updatePanels();
    }

    private final ServiceConnection mNavigationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            logger.warn("onServiceConnected: NavigationService");
            mNavigationService = (INavigationService) binder;
            updateNavigationUI(false);
            updatePanels();
            updateNavigationGauges(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            logger.warn("onServiceDisconnected: NavigationService");
            mNavigationService = null;
            updateNavigationUI(false);
            updatePanels();
        }
    };

    private void startNavigation(MapObject mapObject) {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_TO_POINT);
        i.putExtra(NavigationService.EXTRA_NAME, mapObject.name);
        i.putExtra(NavigationService.EXTRA_LATITUDE, mapObject.coordinates.getLatitude());
        i.putExtra(NavigationService.EXTRA_LONGITUDE, mapObject.coordinates.getLongitude());
        i.putExtra(NavigationService.EXTRA_PROXIMITY, mapObject.proximity);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    private void startNavigation(long id) {
        if (MapTrek.getMapObject(id) == null)
            return;
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_TO_OBJECT);
        i.putExtra(NavigationService.EXTRA_ID, id);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    private void startNavigation(Route route, int dir) {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_VIA_ROUTE);
        i.putExtra(NavigationService.EXTRA_ROUTE, route);
        i.putExtra(NavigationService.EXTRA_ROUTE_DIRECTION, dir);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    private void resumeNavigation() {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.RESUME_NAVIGATION);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission(PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    @Override
    public void stopNavigation() {
        startService(new Intent(this, NavigationService.class).setAction(NavigationService.STOP_NAVIGATION));
    }

    @Override
    public void setHighlightedType(int type) {
        Tags.setHighlightedType(type);
        Configuration.setHighlightedType(type);
        mMap.clearMap();
        Drawable icon = Tags.getTypeDrawable(this, mMap.getTheme(), type);
        if (icon != null)
            mViews.highlightedType.setImageDrawable(icon);
        mViews.highlightedType.setVisibility(View.VISIBLE);
    }

    private void enableTracking() {
        logger.info("enableTracking");
        Intent intent = new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK);
        if (Build.VERSION.SDK_INT >= 26)
            startForegroundService(intent);
        else
            startService(intent);
    }

    public void pauseTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.PAUSE_TRACK));
    }

    @Override
    public void disableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_TRACK));
    }

    @Override
    public void navigateTo(@NonNull GeoPoint coordinates, @Nullable String name) {
        startNavigation(new MapObject(name, coordinates));
    }

    @Override
    public boolean isNavigatingTo(@NonNull GeoPoint coordinates) {
        if (mNavigationService == null)
            return false;
        if (!mNavigationService.isNavigating())
            return false;
        return coordinates.equals(mNavigationService.getCurrentPoint());
    }

    public void navigateVia(@NonNull Route route) {
        startNavigation(route, NavigationService.DIRECTION_FORWARD);
    }

    public void navigateViaReversed(@NonNull Route route) {
        startNavigation(route, NavigationService.DIRECTION_REVERSE);
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
            downX = motionEvent.getX() - mMap.getWidth() / 2f;
            downY = motionEvent.getY() - mMap.getHeight() / 2f;
        }
        if (mActiveMarker == null)
            return;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Update corresponding place
            Place place = (Place) mActiveMarker.getUid();
            place.setCoordinates(mActiveMarker.getPoint());
            onPlaceSave(place);
            mActiveMarker = null;
            // Unshift map to its original position
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(mMap.getWidth() / 2f, mMap.getHeight() / 2f - mFingerTipSize), 1, true);
            mMap.getEventLayer().enableMove(true);
        } else if (action == MotionEvent.ACTION_MOVE) {
            float eventX = motionEvent.getX() - deltaX;
            float eventY = motionEvent.getY() - deltaY - 3 * mFingerTipSize;
            mActiveMarker.setPoint(mMap.viewport().fromScreenPoint(eventX, eventY));
            mMarkerLayer.updateItems();
            mMap.updateMap(true);
        }
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.ROTATE_EVENT) {
            mMainHandler.removeMessages(R.id.msgResetBearing);
            mViews.compass.setTag(e);
        }
        if (e == Map.FINISH_EVENT) {
            final Message m = Message.obtain(mMainHandler, () -> {
                if (mViews.compass.getTag() == Map.ROTATE_EVENT && mLocationState != LocationState.TRACK) {
                    if (Math.abs(mapPosition.bearing) < 5f) {
                        mMap.getMapPosition(true, mapPosition);
                        mapPosition.setBearing(0f);
                        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mapPosition);
                    } else {
                        HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_LOCK_ROTATION, R.string.advice_lock_rotation, mViews.compass, true);
                    }
                }
                mViews.compass.setTag(null);
            });
            m.what = R.id.msgResetBearing;
            mMainHandler.sendMessageDelayed(m, 500);
        }
        if (e == Map.POSITION_EVENT) {
            mTrackingOffsetFactor = Math.cos(Math.toRadians(mapPosition.tilt) * 0.85);
            adjustCompass(mapPosition.bearing);
            if (mAutoTiltSet) {
                if (mAutoTilt != mapPosition.tilt) {
                    mAutoTiltSet = false;
                    mAutoTiltShouldSet = false;
                }
            } else {
                if (mAutoTiltShouldSet)
                    mAutoTiltSet = mapPosition.tilt == mAutoTilt;
            }
        }
        if (e == Map.MOVE_EVENT) {
            if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                mPreviousLocationState = mLocationState;
                mLocationState = LocationState.ENABLED;
                updateLocationDrawable();
            }
            if (mFirstMove) {
                mFirstMove = false;
                mViews.popupAnchor.setX(mMap.getWidth() - 32 * MapTrek.density);
                mViews.popupAnchor.setY(mViews.constraintLayout.getTop() + 64 * MapTrek.density);
                HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_LOCK_MAP_POSITION, R.string.advice_lock_map_position, mViews.popupAnchor, R.drawable.ic_volume_down);
            }
        }
        if (e != Map.FINISH_EVENT)
            checkMissingData(mapPosition);
    }

    @Override
    public boolean onGesture(Gesture gesture, MotionEvent event) {
        mMap.getMapPosition(mMapPosition);
        // override default behavior to adjust pivot point
        if (gesture == Gesture.DOUBLE_TAP) {
            zoomMap(2.0, event.getX() - (mMap.getScreenWidth() >> 1), event.getY() - (mMap.getScreenHeight() >> 1));
            return true;
        } else if (gesture == Gesture.TWO_FINGER_TAP) {
            zoomMap(0.5, 0f, 0f);
            return true;
        } else if (gesture == Gesture.TRIPLE_TAP) {
            float scale = Configuration.getRememberedScale();
            double scaleBy = scale / mMapPosition.getScale();
            float x = scaleBy > 1 ? event.getX() - (mMap.getScreenWidth() >> 1) : 0f;
            float y = scaleBy > 1 ? event.getY() - (mMap.getScreenHeight() >> 1) : 0f;
            zoomMap(scaleBy, x, y);
            return true;
        } else if (gesture == Gesture.LONG_PRESS) {
            if (!mMap.getEventLayer().moveEnabled() || !mObjectInteractionEnabled)
                return true;
            mViews.popupAnchor.setX(event.getX() + mFingerTipSize);
            mViews.popupAnchor.setY(event.getY() - mFingerTipSize);
            mSelectedPoint = mMap.viewport().fromScreenPoint(event.getX(), event.getY());
            mapViewModel.showMarker(mSelectedPoint, null, false);
            PopupMenu popup = new PopupMenu(this, mViews.popupAnchor);
            popup.inflate(R.menu.context_menu_map);
            Menu popupMenu = popup.getMenu();
            if (mLocationState == LocationState.DISABLED || mLocationState == LocationState.SEARCHING || mLocationService == null || !isOnline())
                popupMenu.removeItem(R.id.actionFindRouteHere);
            if ((int) Configuration.getRememberedScale() == (int) mMapPosition.getScale())
                popupMenu.removeItem(R.id.actionRememberScale);
            if (mLocationState != LocationState.TRACK || mAutoTilt == -1f || MathUtils.equals(mAutoTilt, mMapPosition.getTilt()))
                popupMenu.removeItem(R.id.actionRememberTilt);
            popup.setOnMenuItemClickListener(this);
            popup.setOnDismissListener(menu -> mapViewModel.removeMarker());
            popup.show();
            return true;
        }
        return false;
    }

    private void adjustCompass(float bearing) {
        if (mViews.compass.getRotation() == bearing)
            return;
        mViews.compass.setRotation(bearing);
        if (Boolean.TRUE.equals(mapViewModel.rotationLocked.getValue()))
            return;
        if (bearing == 0f) {
            if (mViews.compass.getVisibility() != View.GONE)
                mViews.compass.setVisibility(View.GONE);
        } else if (mViews.compass.getVisibility() == View.GONE) {
            mViews.compass.setVisibility(View.VISIBLE);
        }
        // +/-5 degrees
        mViews.compass.setAlpha(FastMath.clamp(Math.abs(bearing) / 5f, 0.2f, 1f));
    }

    private void adjustNavigationArrow(float turn) {
        if (mViews.navigationArrow.getRotation() == turn)
            return;
        mViews.navigationArrow.setRotation(turn);
    }

    private boolean showNavigationMenu() {
        PopupMenu popup = new PopupMenu(this, mViews.mapButtonHolder);
        popup.inflate(R.menu.context_menu_navigation);
        Menu popupMenu = popup.getMenu();
        if (!mNavigationService.isNavigatingViaRoute() || !mNavigationService.hasNextRoutePoint())
            popupMenu.removeItem(R.id.actionNextRoutePoint);
        if (!mNavigationService.isNavigatingViaRoute() || !mNavigationService.hasPrevRoutePoint())
            popupMenu.removeItem(R.id.actionPrevRoutePoint);
        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    private void updateLocationDrawable() {
        logger.info("updateLocationDrawable()");
        if (mViews.locationButton.getTag() == mLocationState)
            return;
        if (mViews.locationButton.getTag() == LocationState.SEARCHING) {
            mViews.locationButton.clearAnimation();
            mViews.satellites.animate().translationY(-200);
        }

        @ColorInt int color = mColorActionIcon;
        switch (mLocationState) {
            case DISABLED:
                mViews.locationButton.setImageDrawable(mNavigationNorthDrawable);
                mCrosshairLayer.setEnabled(true);
                updatePanels();
                break;
            case SEARCHING:
                color = mColorAccent;
                mViews.locationButton.setImageDrawable(mLocationSearchingDrawable);
                Animation rotation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotation.setInterpolator(new LinearInterpolator());
                rotation.setRepeatCount(Animation.INFINITE);
                rotation.setDuration(1000);
                mViews.locationButton.startAnimation(rotation);
                if (mViews.gaugePanel.getVisibility() == View.GONE) {
                    mViews.satellites.animate().translationY(8);
                } else {
                    updatePanels();
                }
                break;
            case ENABLED:
                mViews.locationButton.setImageDrawable(mMyLocationDrawable);
                mCrosshairLayer.setEnabled(true);
                updatePanels();
                break;
            case NORTH:
                color = mColorAccent;
                mViews.locationButton.setImageDrawable(mNavigationNorthDrawable);
                mCrosshairLayer.setEnabled(false);
                updatePanels();
                break;
            case TRACK:
                color = mColorAccent;
                mViews.locationButton.setImageDrawable(mNavigationTrackDrawable);
                mCrosshairLayer.setEnabled(false);
                updatePanels();
        }
        mViews.locationButton.setImageTintList(ColorStateList.valueOf(color));
        mViews.locationButton.setTag(mLocationState);
        for (WeakReference<LocationStateChangeListener> weakRef : mLocationStateChangeListeners) {
            LocationStateChangeListener locationStateChangeListener = weakRef.get();
            if (locationStateChangeListener != null) {
                locationStateChangeListener.onLocationStateChanged(mLocationState);
            }
        }
    }

    private void updatePanels() {
        logger.info("updatePanels()");
        TransitionSet transitionSet = new TransitionSet();
        Transition transition = new Slide(Gravity.START);
        transition.addTarget(mViews.gaugePanel);
        transitionSet.addTransition(transition);
        if (Boolean.TRUE.equals(routeViewModel.isRouting.getValue())) {
            transition = new Slide(Gravity.TOP);
            transition.addTarget(mViews.navigationPanel);
            transitionSet.addTransition(transition);
            transition = new AutoTransition();
            transition.addTarget(mViews.mapButtonHolder);
            transition.setDuration(300);
            transitionSet.addTransition(transition);
        }
        transitionSet.setDuration(600);

        transitionSet.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                if (mLocationState == LocationState.NORTH)
                    HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_MORE_GAUGES, R.string.advice_more_gauges, mViews.gaugePanel, true);
                // TODO: implement as transition too?
                if (mLocationState == LocationState.SEARCHING)
                    mViews.satellites.animate().translationY(8);
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
        TransitionManager.beginDelayedTransition(mViews.getRoot(), transitionSet);

        switch (mLocationState) {
            case DISABLED:
            case SEARCHING:
            case ENABLED:
                if (mViews.gaugePanel.getVisibility() != View.GONE) {
                    mViews.navigationPanel.setVisibility(View.GONE);
                    mViews.gaugePanel.setVisibility(View.GONE);
                    mViews.gaugePanel.onVisibilityChanged(false);
                }
                getWindow().setStatusBarColor(getColor(android.R.color.transparent));
                break;
            case NORTH:
            case TRACK:
                if (Boolean.TRUE.equals(routeViewModel.isRouting.getValue())) {
                    mViews.navigationPanel.setVisibility(View.VISIBLE);
                    getWindow().setStatusBarColor(getColor(R.color.colorNavigationBackground));
                } else if (mViews.navigationPanel.getVisibility() == View.VISIBLE) {
                    mViews.navigationPanel.setVisibility(View.GONE);
                    getWindow().setStatusBarColor(getColor(android.R.color.transparent));
                }
                mViews.gaugePanel.setVisibility(View.VISIBLE);
                mViews.gaugePanel.onVisibilityChanged(true);
                break;
        }
    }

    private void updateGauges() {
        Location location = mLocationService.getLocation();
        mViews.gaugePanel.setValue(Gauge.TYPE_SPEED, location.getSpeed());
        mViews.gaugePanel.setValue(Gauge.TYPE_TRACK, location.getBearing());
        mViews.gaugePanel.setValue(Gauge.TYPE_ALTITUDE, (float) location.getAltitude());
    }

    private void updateNavigationGauges(boolean updateRoutePanels) {
        if (mNavigationService == null)
            return; // not ready

        if (mNavigationService.isNavigatingViaRoute()) {
            if (updateRoutePanels) {
                mViews.routePoint.setText(mNavigationService.getInstructionText());
                @DrawableRes int sign = RouteInformation.getSignDrawable(mNavigationService.getSign());
                Drawable signDrawable = AppCompatResources.getDrawable(MainActivity.this, sign);
                mViews.navigationSign.setImageDrawable(signDrawable);
                mViews.navigationPanel.setActivated(false);
                mViews.navigationSign.setTag(false);
            }
            mViews.routePointDistance.setText(StringFormatter.distanceH(mNavigationService.getPointDistance()));
            int ete = mNavigationService.getPointEte();
            if (ete == Integer.MAX_VALUE) {
                mViews.routePointEte.setVisibility(View.GONE);
            } else {
                mViews.routePointEte.setVisibility(View.VISIBLE);
                mViews.routePointEte.setText(StringFormatter.timeH(ete));
                if (ete <= 1) {
                    if (!Boolean.TRUE.equals(mViews.navigationSign.getTag())) {
                        mViews.navigationPanel.setActivated(true);
                        mViews.navigationSign.setTag(true);
                    }
                } else if (Boolean.TRUE.equals(mViews.navigationSign.getTag())) {
                    mViews.navigationPanel.setActivated(false);
                    mViews.navigationSign.setTag(false);
                }
            }
        }
        mViews.gaugePanel.setValue(Gauge.TYPE_DISTANCE, mNavigationService.getDistance());
        mViews.gaugePanel.setValue(Gauge.TYPE_BEARING, mNavigationService.getBearing());
        mViews.gaugePanel.setValue(Gauge.TYPE_TURN, mNavigationService.getTurn());
        mViews.gaugePanel.setValue(Gauge.TYPE_VMG, mNavigationService.getVmg());
        mViews.gaugePanel.setValue(Gauge.TYPE_XTK, mNavigationService.getXtk());
        mViews.gaugePanel.setValue(Gauge.TYPE_ETE, mNavigationService.getEte());
    }

    private void updateNavigationUI(boolean force) {
        logger.debug("updateNavigationUI({})", force);
        boolean isNavigating = mNavigationService != null && mNavigationService.isNavigating();

        if (!force && mViews.gaugePanel.getNavigationMode() == isNavigating)
            return; // nothing changed

        mViews.gaugePanel.setNavigationMode(isNavigating);
        if (isNavigating) {
            if (mViews.navigationArrow.getVisibility() == View.GONE) {
                mViews.navigationArrow.setAlpha(0f);
                mViews.navigationArrow.setVisibility(View.VISIBLE);
                mViews.navigationArrow.animate().alpha(1f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(null);
            }
            if (mNavigationLayer == null) {
                int color = Configuration.loadInt(Configuration.PREF_ACTIVE_ROUTE_COLOR, getColor(R.color.activeRouteColor));
                int width = Configuration.loadInt(Configuration.PREF_ACTIVE_ROUTE_WIDTH, getResources().getInteger(R.integer.default_route_width));
                Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.route_point, color));
                MarkerSymbol pointSymbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
                bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.route_end, color));
                MarkerSymbol endSymbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
                mNavigationLayer = new NavigationLayer(mMap, color, width, pointSymbol, null, endSymbol);
                mMap.layers().add(mNavigationLayer, MAP_DYNAMIC_DATA);
            }
            Location location = mapViewModel.currentLocation.getValue();
            if (location != null && !"unknown".equals(location.getProvider()))
                mNavigationLayer.setPosition(location.getLatitude(), location.getLongitude());
            mNavigationLayer.setRemainingPoints(mNavigationService.getRemainingPoints(), mNavigationService.getPrevRoutePoint());
        } else {
            if (mViews.navigationArrow.getAlpha() == 1f) {
                mViews.navigationArrow.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViews.navigationArrow.setVisibility(View.GONE);
                    }
                });
            }
            if (mNavigationLayer != null) {
                mMap.layers().remove(mNavigationLayer);
                mNavigationLayer = null;
            }
        }
        routeViewModel.isRouting.setValue(isNavigating && mNavigationService.isNavigatingViaRoute());
    }

    @Override
    public void showMarkerInformation(@NonNull GeoPoint point, @Nullable String name) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        mapViewModel.showMarker(point, name, false);
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        Fragment fragment = factory.instantiate(getClassLoader(), MarkerInformation.class.getName());
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "markerInformation");
        ft.addToBackStack("markerInformation");
        ft.commit();
    }

    private void onPlaceProperties(Place place) {
        placeViewModel.selectedPlace.setValue(place);
        new PlaceProperties().show(mFragmentManager, "placeProperties");
    }

    @Override
    public void onPlaceCreate(GeoPoint point, String name, boolean locked, boolean customize) {
        final Place place = new Place(name, point.getLatitude(), point.getLongitude());
        place.date = new Date();
        place.locked = locked;
        dataSourceViewModel.placeDbDataSource.savePlace(place);
        MarkerItem marker = new MarkerItem(place, name, null, point, place.style.color);
        mMarkerLayer.addItem(marker);
        mMap.updateMap(true);
        if (!customize)
            return;
        Snackbar.make(mViews.coordinatorLayout, R.string.msgPlaceSaved, Snackbar.LENGTH_LONG)
                .setAction(R.string.actionCustomize, view -> onPlaceProperties(place))
                .setAnchorView(mViews.actionPanel)
                .show();
    }

    @Override
    public void onPlaceView(Place place) {
        setMapLocation(place.coordinates);
    }

    @Override
    public void onPlaceFocus(Place place) {
        if (place != null)
            mMarkerLayer.setFocus(mMarkerLayer.getByUid(place), place.style.color);
        else
            mMarkerLayer.setFocus(null);
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    @Override
    public void onPlaceDetails(Place place, boolean fromList) {
        mViews.popupAnchor.setX(0);
        mViews.popupAnchor.setY(0);

        placeViewModel.expanded = fromList;
        placeViewModel.selectedPlace.setValue(place);

        Fragment fragment = mFragmentManager.findFragmentByTag("amenityInformation");
        if (fragment != null) {
            mFragmentManager.popBackStack("amenityInformation", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fragment = mFragmentManager.findFragmentByTag("placeInformation");
        if (fragment == null) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            fragment = factory.instantiate(getClassLoader(), PlaceInformation.class.getName());
            Slide slide = new Slide(Gravity.BOTTOM);
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.bottomSheetPanel, fragment, "placeInformation");
            ft.addToBackStack("placeInformation");
            ft.commit();
        }
        dimExtendPanel();
    }

    @Override
    public void onPlaceNavigate(Place place) {
        startNavigation(place);
    }

    @Override
    public void onPlaceShare(Place place) {
        shareLocation(place.coordinates, place.name);
    }

    @Override
    public void onPlaceSave(final Place place) {
        if (place.source instanceof PlaceDbDataSource) {
            dataSourceViewModel.placeDbDataSource.savePlace(place);
        } else {
            Manager.save((FileDataSource) place.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(() -> place.source.notifyListeners());
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mViews.coordinatorLayout, e);
                }
            }, mProgressHandler);
        }
        // Markers are immutable so simply recreate it
        removePlaceMarker(place);
        addPlaceMarker(place);
        mMap.updateMap(true);
    }

    @Override
    public void onPlaceDelete(final Place place) {
        // Remove marker to indicate action to user
        removePlaceMarker(place);
        mMap.updateMap(true);
        mDeletedPlaces = new HashSet<>();
        mDeletedPlaces.add(place);

        // Show undo snackbar
        Snackbar.make(mViews.coordinatorLayout, R.string.msgPlaceDeleted, Snackbar.LENGTH_LONG)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){
                    public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        if (mDeletedPlaces == null) // removed on application exit
                            return;
                        // If dismissed, actually remove place
                        deletePlaces(mDeletedPlaces);
                        mDeletedPlaces = null;

                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the marker
                    addPlaceMarker(place);
                    mMap.updateMap(true);
                    mDeletedPlaces = null;
                })
                .setAnchorView(mViews.actionPanel)
                .show();
    }

    @Override
    public void onPlacesDelete(final Set<Place> places) {
        // Remove markers to indicate action to user
        for (Place place : places) {
            removePlaceMarker(place);
        }
        mMap.updateMap(true);
        mDeletedPlaces = places;

        // Show undo snackbar
        int count = places.size();
        String msg = getResources().getQuantityString(R.plurals.placesDeleted, count, count);
        Snackbar.make(mViews.coordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){
                    @Override
                    public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        if (mDeletedPlaces == null) // removed on application exit
                            return;
                        // If dismissed, actually remove places
                        deletePlaces(mDeletedPlaces);
                        mDeletedPlaces = null;
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the marker
                    for (Place place : places) {
                        addPlaceMarker(place);
                    }
                    mMap.updateMap(true);
                    mDeletedPlaces = null;
                })
                .setAnchorView(mViews.actionPanel)
                .show();
    }

    private void onTrackProperties(String path) {
        logger.debug("onTrackProperties({})", path);
        Track track = null;
        //TODO Think of better way to find appropriate track
        for (FileDataSource source : dataSourceViewModel.fileDataSources) {
            if (source.path.equals(path)) {
                track = source.tracks.get(0);
                break;
            }
        }
        if (track == null)
            return;

        trackViewModel.selectedTrack.setValue(track);
        new TrackProperties().show(mFragmentManager, "trackProperties");
    }

    @Override
    public void onTrackView(Track track) {
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        BoundingBox box = track.getBoundingBox();
        if (box == null)
            return;
        box.extendBy(0.05);
        mMap.animator().animateTo(box);
    }

    @Override
    public void onTrackDetails(Track track) {
        trackViewModel.selectedTrack.setValue(track);
        adjustGuideline(true);
        Fragment fragment = mFragmentManager.findFragmentByTag("trackInformation");
        if (fragment == null) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            fragment = factory.instantiate(getClassLoader(), TrackInformation.class.getName());
            Slide slide = new Slide(mSlideGravity);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "trackInformation");
            ft.addToBackStack("trackInformation");
            ft.commit();
        }
        dimExtendPanel();
    }

    @Override
    public void onTrackShare(final Track track) {
        final AtomicInteger selected = new AtomicInteger(0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(R.array.track_format_array, selected.get(), (dialog, which) -> selected.set(which));
        builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
            DataExport.Builder builder12 = new DataExport.Builder();
            @FileDataSource.Format int format = selected.get();
            DataExport dataExport = builder12.setTrack(track).setFormat(format).create();
            dataExport.show(mFragmentManager, "trackExport");
        });
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            String msgNative = getString(R.string.msgNativeFormatExplanation);
            String msgOther = getString(R.string.msgOtherFormatsExplanation);
            builder1.setMessage(msgNative + " " + msgOther);
            builder1.setPositiveButton(R.string.ok, null);
            AlertDialog dialog1 = builder1.create();
            dialog1.show();
        });
    }

    @Override
    public void onTrackSave(final Track track) {
        FileDataSource fileSource = (FileDataSource) track.source;
        Manager manager = Manager.getDataManager(fileSource.path);
        if (manager instanceof TrackManager) {
            // Use optimized save for native track
            try {
                ((TrackManager) manager).saveProperties(fileSource);
                // Rename file if name changed
                File thisFile = new File(fileSource.path);
                File thatFile = new File(thisFile.getParent(), FileUtils.sanitizeFilename(track.name) + TrackManager.EXTENSION);
                if (!thisFile.equals(thatFile)) {
                    Loader<List<FileDataSource>> loader = LoaderManager.getInstance(this).getLoader(0);
                    if (loader != null) {
                        // Let loader do the task if it is available
                        ((DataLoader) loader).renameSource(fileSource, thatFile);
                        // otherwise do it manually (this normally should not happen)
                    } else if (thisFile.renameTo(thatFile)) {
                        fileSource.path = thatFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                HelperUtils.showSaveError(this, mViews.coordinatorLayout, e);
                e.printStackTrace();
            }
        } else {
            // Save hole data source
            Manager.save((FileDataSource) track.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(() -> track.source.notifyListeners());
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mViews.coordinatorLayout, e);
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
        mDeletedTracks = new HashSet<>();
        mDeletedTracks.add(track);

        // Show undo snackbar
        Snackbar.make(mViews.coordinatorLayout, R.string.msgTrackDeleted, Snackbar.LENGTH_LONG)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){
                    @Override
                    public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        if (mDeletedTracks == null) // removed on application exit
                            return;
                        // If dismissed, actually remove track
                        // Native tracks can not be deleted through this procedure
                        deleteTracks(mDeletedTracks);
                        mDeletedTracks = null;
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the track on map
                    TrackLayer trackLayer = new TrackLayer(mMap, track);
                    mMap.layers().add(trackLayer, MAP_DATA);
                    mMap.updateMap(true);
                    mDeletedTracks = null;
                })
                .setAnchorView(mViews.actionPanel)
                .show();
    }

    @Override
    public void onTracksDelete(final Set<Track> tracks) {
        // Remove track layers to indicate action to user
        for (Track track : tracks) {
            for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
                Layer layer = i.next();
                if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(track)) {
                    i.remove();
                    layer.onDetach();
                    break;
                }
            }
        }
        mMap.updateMap(true);
        mDeletedTracks = tracks;

        // Show undo snackbar
        int count = tracks.size();
        String msg = getResources().getQuantityString(R.plurals.tracksDeleted, count, count);
        Snackbar.make(mViews.coordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){
                    @Override
                    public void onDismissed(Snackbar snackbar, @DismissEvent int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        if (mDeletedTracks == null) // removed on application exit
                            return;
                        // If dismissed, actually remove tracks
                        // Native tracks can not be deleted through this procedure
                        deleteTracks(mDeletedTracks);
                        mDeletedTracks = null;
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore tracks on map
                    for (Track track : tracks) {
                        TrackLayer trackLayer = new TrackLayer(mMap, track);
                        mMap.layers().add(trackLayer, MAP_DATA);
                    }
                    mMap.updateMap(true);
                    mDeletedTracks = null;
                })
                .setAnchorView(mViews.actionPanel)
                .show();
    }

    @Override
    public void onRouteView(Route route) {
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        BoundingBox box = route.getBoundingBox();
        box.extendBy(0.05);
        mMap.animator().animateTo(box);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onRouteDetails(Route route) {
        routeViewModel.selectedRoute.setValue(route);
        adjustGuideline(true);
        Fragment fragment = mFragmentManager.findFragmentByTag("routeInformation");
        if (fragment == null) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            fragment = factory.instantiate(getClassLoader(), RouteInformation.class.getName());
            Slide slide = new Slide(mSlideGravity);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "routeInformation");
            ft.addToBackStack("routeInformation");
            ft.commit();
        }
        dimExtendPanel();
    }

    @Override
    public void onRouteShare(Route route) {
        final AtomicInteger selected = new AtomicInteger(0);
        final DialogInterface.OnClickListener exportAction = (dialog, which) -> {
            DataExport.Builder builder = new DataExport.Builder();
            @FileDataSource.Format int format = selected.get() + 1;
            DataExport dataExport = builder.setRoute(route).setFormat(format).create();
            dataExport.show(mFragmentManager, "dataExport");
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(R.array.data_format_array,
                selected.get(), (dialog, which) -> selected.set(which));
        builder.setPositiveButton(R.string.actionContinue, exportAction);
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            builder1.setMessage(getString(R.string.msgOtherFormatsExplanation));
            builder1.setPositiveButton(R.string.ok, null);
            AlertDialog dialog1 = builder1.create();
            dialog1.show();
        });
    }

    @Override
    public void onRouteSave(Route route) {

    }

    @Override
    public void onRouteDelete(Route route) {

    }

    @Override
    public void onRoutesDelete(Set<Route> route) {

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onFeatureDetails(long id, boolean fromList) {
        // For some weird reason popupAnchor view limits height of bottom sheet
        mViews.popupAnchor.setX(0);
        mViews.popupAnchor.setY(0);

        if (!mObjectInteractionEnabled)
            return;

        int language = MapTrekDatabaseHelper.getLanguageId(Configuration.getLanguage());
        Amenity amenity = MapTrekDatabaseHelper.getAmenityData(language, id, mDetailedMapDatabase);
        amenityViewModel.setAmenity(amenity);

        Fragment fragment = mFragmentManager.findFragmentByTag("placeInformation");
        if (fragment != null) {
            mFragmentManager.popBackStack("placeInformation", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fragment = mFragmentManager.findFragmentByTag("amenityInformation");
        if (fragment == null) {
            FragmentFactory factory = mFragmentManager.getFragmentFactory();
            fragment = factory.instantiate(getClassLoader(), AmenityInformation.class.getName());
            Slide slide = new Slide(Gravity.BOTTOM);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.bottomSheetPanel, fragment, "amenityInformation");
            ft.addToBackStack("amenityInformation");
            ft.commit();
        }
        dimExtendPanel();
    }

    @Override
    public void shareLocation(@NonNull GeoPoint coordinates, @Nullable String name) {
        LocationShare dialogFragment = new LocationShare();
        Bundle args = new Bundle();
        args.putDouble(LocationShare.ARG_LATITUDE, coordinates.getLatitude());
        args.putDouble(LocationShare.ARG_LONGITUDE, coordinates.getLongitude());
        args.putInt(LocationShare.ARG_ZOOM, mMap.getMapPosition().getZoomLevel());
        if (name != null)
            args.putString(LocationShare.ARG_NAME, name);
        dialogFragment.setArguments(args);
        dialogFragment.show(mFragmentManager, "locationShare");
    }

    private void showHideMapObjects(boolean hasBitmapMap) {
        Layers layers = mMap.layers();
        if (hasBitmapMap && mHideMapObjects && layers.contains(mLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.remove(mBuildingsLayer);
            layers.remove(mLabelsLayer);
        }
        if ((!hasBitmapMap || !mHideMapObjects) && !layers.contains(mLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.add(mBuildingsLayer, MAP_3D);
            layers.add(mLabelsLayer, MAP_LABELS);
        }
    }

    private void startMapSelection(boolean zoom) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        if (zoom) {
            MapPosition mapPosition = mMap.getMapPosition();
            mapPosition.setScale(MapCoverageLayer.TEXT_MIN_SCALE + 5f);
            mapPosition.setBearing(0f);
            mapPosition.setTilt(0f);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        }
        FragmentFactory factory = mFragmentManager.getFragmentFactory();
        MapSelection fragment = (MapSelection) factory.instantiate(getClassLoader(), MapSelection.class.getName());
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "mapSelection");
        ft.addToBackStack("mapSelection");
        ft.commit();
    }

    @Override
    public void onMapSelected(MapFile mapFile) {
        if (!mBitmapLayerMaps.isEmpty()) {
            boolean current = false;
            for (MapFile bitmapLayerMap : mBitmapLayerMaps) {
                mMap.layers().remove(bitmapLayerMap.tileLayer);
                bitmapLayerMap.tileSource.close();
                current = current || mapFile == bitmapLayerMap;
            }
            mBitmapLayerMaps.clear();
            if (current) {
                showHideMapObjects(false);
                mMap.updateMap(true);
                showLicense(null);
                return;
            }
        }
        showBitmapMap(mapFile, true);
        mBitmapLayerMaps.add(mapFile);
    }

    @Override
    public void onExtraMapSelected(MapFile mapFile) {
        if (mBitmapLayerMaps.contains(mapFile)) { // map is shown
            mMap.layers().remove(mapFile.tileLayer);
            mapFile.tileSource.close();
            mBitmapLayerMaps.remove(mapFile);
            if (mBitmapLayerMaps.isEmpty()) {
                showHideMapObjects(false);
                showLicense(null);
            }
            mMap.updateMap(true);
        } else {
            showBitmapMap(mapFile, mBitmapLayerMaps.isEmpty());
            mBitmapLayerMaps.add(mapFile);
            //TODO Bitmap layer should respond to update map (see TileLayer)
            mMap.clearMap();
        }
    }

    @Override
    public void onMapShare(MapFile mapFile) {
        String filename = mapFile.tileSource.getOption("path");
        File exportFile = new File(filename);
        Uri contentUri = ExportProvider.getUriForFile(this, exportFile);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType("application/octet-stream");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_map_intent_title)));
    }

    @Override
    public void onMapDelete(MapFile mapFile) {
        if (mBitmapLayerMaps.contains(mapFile)) {
            mMap.layers().remove(mapFile.tileLayer);
            mapFile.tileSource.close();
            mBitmapLayerMaps.remove(mapFile);
            showHideMapObjects(false);
            mMap.updateMap(true);
        }
        mMapIndex.removeMap(mapFile);
        String filename = mapFile.tileSource.getOption("path");
        File file = new File(filename);
        if (!file.delete())
            HelperUtils.showError(getString(R.string.msgMapDeleteFailed), mViews.coordinatorLayout);
    }

    @Override
    public void onHideMapObjects(boolean hide) {
        mHideMapObjects = hide;
        showHideMapObjects(mBitmapLayerMaps.size() > 0);
        mMap.updateMap(true);
        Configuration.setHideMapObjects(hide);
    }

    @Override
    public void onTransparencyChanged(int transparency) {
        mBitmapMapTransparency = transparency;
        for (MapFile bitmapLayerMap : mBitmapLayerMaps)
            if (bitmapLayerMap.tileLayer instanceof BitmapTileLayer)
                ((BitmapTileLayer) bitmapLayerMap.tileLayer).setBitmapAlpha(1 - mBitmapMapTransparency * 0.01f, true);
        Configuration.setBitmapMapTransparency(transparency);
    }

    @Override
    public void onBeginMapManagement() {
        mMapCoverageLayer = new MapCoverageLayer(getApplicationContext(), mMap, mapIndexViewModel.nativeIndex, MapTrek.density);
        mMap.layers().add(mMapCoverageLayer, MAP_OVERLAYS);
        MapPosition mapPosition = mMap.getMapPosition();
        if (mapPosition.zoomLevel > 8) {
            mapPosition.setZoomLevel(8);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        } else {
            mMap.updateMap(true);
        }
        int[] xy = (int[]) mViews.mapDownloadButton.getTag(R.id.mapKey);
        if (xy != null)
            mapIndexViewModel.nativeIndex.selectNativeMap(xy[0], xy[1], Index.ACTION.DOWNLOAD);
    }

    @Override
    public void onFinishMapManagement() {
        mMap.layers().remove(mMapCoverageLayer);
        mMapCoverageLayer.onDetach();
        mMapCoverageLayer = null;
        mMap.updateMap(true);
    }

    private void deletePlaces(@NonNull Set<Place> places) {
        HashSet<FileDataSource> sources = new HashSet<>();
        for (Place place : places) {
            if (place.source instanceof PlaceDbDataSource) {
                dataSourceViewModel.placeDbDataSource.deletePlace(place);
            } else {
                ((FileDataSource) place.source).places.remove(place);
                sources.add((FileDataSource) place.source);
            }
            mTotalDataItems--;
        }
        for (FileDataSource source : sources) {
            Manager.save(source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(final FileDataSource source) {
                    if (mMainHandler != null) // can be null on application exit
                        mMainHandler.post(source::notifyListeners);
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mViews.coordinatorLayout, e);
                }
            }, mProgressHandler);
        }
    }

    private void deleteTracks(Set<Track> tracks) {
        HashSet<FileDataSource> sources = new HashSet<>();
        for (Track track : tracks) {
            ((FileDataSource) track.source).tracks.remove(track);
            sources.add((FileDataSource) track.source);
            mTotalDataItems--;
        }
        for (FileDataSource source : sources) {
            Manager.save(source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    if (mMainHandler != null) // can be null on application exit
                        mMainHandler.post(source::notifyListeners);
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mViews.coordinatorLayout, e);
                }
            }, mProgressHandler);
        }
    }

    private void showHillShade() {
        SQLiteTileSource hillShadeTileSource = MapTrek.getApplication().getHillShadeTileSource();
        if (hillShadeTileSource != null) {
            int transparency = Configuration.getHillshadesTransparency();
            mHillshadeLayer = new BitmapTileLayer(mMap, hillShadeTileSource, 1 - transparency * 0.01f);
            mMap.layers().add(mHillshadeLayer, MAP_MAP_OVERLAYS);
            mMap.updateMap(true);
        }
    }

    private void hideHillShade() {
        mMap.layers().remove(mHillshadeLayer);
        mHillshadeLayer.onDetach();
        mMap.updateMap(true);
        mHillshadeLayer = null;
    }

    private void showBitmapMap(MapFile mapFile, boolean reposition) {
        logger.debug("showBitmapMap({})", mapFile.name);
        showHideMapObjects(true);
        mapFile.tileSource.open();
        if ("vtm".equals(mapFile.tileSource.getOption("format"))) {
            OsmTileLayer layer = new OsmTileLayer(mMap);
            layer.setTileSource(mapFile.tileSource);
            layer.setRenderTheme(ThemeLoader.load(Themes.MAPTREK));
            mapFile.tileLayer = layer;
        } else {
            mapFile.tileLayer = new BitmapTileLayer(mMap, mapFile.tileSource, 1 - mBitmapMapTransparency * 0.01f);
        }
        mMap.layers().add(mapFile.tileLayer, MAP_MAPS);
        if (mapFile.tileSource instanceof PluginOnlineTileSource) {
            PluginOnlineTileSource tileSource = (PluginOnlineTileSource) mapFile.tileSource;
            String license = tileSource.getLicense();
            if (license != null)
                showLicense(license);
        }
        if (!reposition)
            return;

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
        int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
        if (mapFile.tileSource instanceof SQLiteTileSource) {
            minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
        }
        double minScale = (1 << minZoomLevel) * 0.7 + (1 << (minZoomLevel + 1)) * 0.3 + 5;
        if (position.getScale() < minScale) {
            position.setScale(minScale);
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
            mViews.moreButton.animate().rotationBy(180).setDuration(duration * 5).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mViews.moreButton.setRotation(0f);
                }
            });
        if (show) {
            mAPB.setVisibility(View.VISIBLE);
            if (animate)
                mAPB.animate().setDuration(duration * 5).alpha(1f);
            else
                mAPB.setAlpha(1f);
            mViews.mapsButton.setVisibility(View.VISIBLE);
            if (animate) {
                mViews.mapsButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViews.placesButton.setVisibility(View.VISIBLE);
                        mViews.placesButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mViews.tracksButton.setVisibility(View.VISIBLE);
                                mViews.tracksButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mViews.locationButton.setVisibility(View.VISIBLE);
                                        mViews.locationButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                mViews.extendPanel.requestLayout();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                mViews.mapsButton.setAlpha(1f);
                mViews.placesButton.setVisibility(View.VISIBLE);
                mViews.placesButton.setAlpha(1f);
                mViews.tracksButton.setVisibility(View.VISIBLE);
                mViews.tracksButton.setAlpha(1f);
                mViews.locationButton.setVisibility(View.VISIBLE);
                mViews.locationButton.setAlpha(1f);
                mViews.extendPanel.requestLayout();
            }
        } else {
            if (animate) {
                mAPB.animate().alpha(0f).setDuration(duration * 5);
                mViews.locationButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViews.locationButton.setVisibility(View.INVISIBLE);
                        mViews.tracksButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mViews.tracksButton.setVisibility(View.INVISIBLE);
                                mViews.placesButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mViews.placesButton.setVisibility(View.INVISIBLE);
                                        mViews.mapsButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                mViews.mapsButton.setVisibility(View.INVISIBLE);
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
                mViews.locationButton.setAlpha(0f);
                mViews.locationButton.setVisibility(View.INVISIBLE);
                mViews.tracksButton.setAlpha(0f);
                mViews.tracksButton.setVisibility(View.INVISIBLE);
                mViews.placesButton.setAlpha(0f);
                mViews.placesButton.setVisibility(View.INVISIBLE);
                mViews.mapsButton.setAlpha(0f);
                mViews.mapsButton.setVisibility(View.INVISIBLE);
                mAPB.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showExtendPanel(PANEL_STATE panel, String name, Fragment fragment) {
        if (mPanelState != PANEL_STATE.NONE && mFragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
            //TODO Make it properly work without "immediate" - that is why exit transitions do not work
            mFragmentManager.popBackStackImmediate(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (name.equals(bse.getName()))
                return;
        }
        //brightenExtendPanel();

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
        ft.setReorderingAllowed(true);
        ft.commit();

        setPanelState(panel);

        if ("dataList".equals(name) || "dataSourceList".equals(name))
            HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_ADDING_PLACE, R.string.advice_adding_place, mViews.placesButton, false);
    }

    private void setPanelState(PANEL_STATE state) {
        if (mPanelState == state)
            return;

        View mLBB = findViewById(R.id.locationButtonBackground);
        View mRBB = findViewById(R.id.tracksButtonBackground);
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
                case TRACKS:
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
            case TRACKS:
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
        thisColorAnimation.addUpdateListener(animator -> {
            int color = (Integer) animator.getAnimatedValue();
            thisView.setBackgroundColor(color);
        });
        otherColorAnimation.addUpdateListener(animator -> {
            int color = (Integer) animator.getAnimatedValue();
            for (View otherView : otherViews)
                otherView.setBackgroundColor(color);
        });
        AnimatorSet s = new AnimatorSet();
        s.play(thisColorAnimation).with(otherColorAnimation);
        if (state == PANEL_STATE.NONE) {
            s.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    if (mFragmentManager.getBackStackEntryCount() > 0)
                        return;
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_ENABLE_LOCATIONS, R.string.advice_enable_locations, mViews.locationButton, false);
                    } else if (mTotalDataItems > 5) {
                        mViews.popupAnchor.setX(mMap.getWidth() - 32 * MapTrek.density);
                        mViews.popupAnchor.setY(mViews.constraintLayout.getTop() + 64 * MapTrek.density);
                        HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_HIDE_MAP_OBJECTS, R.string.advice_hide_map_objects, mViews.popupAnchor, R.drawable.ic_volume_up);
                    }
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {
                }
            });
        }
        s.start();

        mPanelState = state;
    }

    private void dimExtendPanel() {
        int count = mViews.extendPanel.getChildCount();
        if (count == 0)
            return;
        View child = mViews.extendPanel.getChildAt(count - 1);
        child.setForeground(AppCompatResources.getDrawable(this, R.drawable.dim));
        child.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(child.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    private void brightenExtendPanel() {
        int count = mViews.extendPanel.getChildCount();
        if (count == 0)
            return;
        View child = mViews.extendPanel.getChildAt(count - 1);
        child.setForeground(null);
    }

    private void adjustPanelViews() {
        switch (mPanelState) {
            case LOCATION:
                if (mVerticalOrientation)
                    adjustGuideline(false);
                adjustPanelConstraint(Gravity.START, null);
                break;
            case TRACKS:
                if (mVerticalOrientation) {
                    adjustGuideline(true);
                    adjustPanelConstraint(Gravity.FILL, null);
                } else {
                    adjustPanelConstraint(Gravity.END, mViews.tracksButton);
                }
                break;
            case PLACES:
                if (mVerticalOrientation) {
                    adjustGuideline(true);
                    adjustPanelConstraint(Gravity.FILL, null);
                } else {
                    adjustPanelConstraint(Gravity.END, mViews.placesButton);
                }
                break;
            case MAPS:
                if (mVerticalOrientation) {
                    adjustGuideline(false);
                    adjustPanelConstraint(Gravity.END, null);
                } else {
                    adjustPanelConstraint(Gravity.END, mViews.mapsButton);
                }
                break;
            case MORE:
                if (mVerticalOrientation)
                    adjustGuideline(false);
                adjustPanelConstraint(Gravity.END, null);
        }
    }

    private void adjustGuideline(boolean limited) {
        if (mVerticalOrientation) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mViews.guideline.getLayoutParams();
            params.guidePercent = limited ? 0.25f : 0f;
            mViews.guideline.setLayoutParams(params);
        }
    }

    private void adjustPanelConstraint(int gravity, View ref) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mViews.extendPanel.getLayoutParams();
        if (mVerticalOrientation) {
            switch (gravity) {
                case Gravity.START:
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                    break;
                case Gravity.FILL:
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    break;
                case Gravity.END:
                    params.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    break;
            }
        } else {
            params.matchConstraintMinHeight = 0;
            switch (gravity) {
                case Gravity.START:
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
                    if (ref != null)
                        params.matchConstraintMinHeight = (int) (ref.getHeight() + ref.getY());
                    break;
                case Gravity.FILL:
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    break;
                case Gravity.END:
                    params.topToTop = ConstraintLayout.LayoutParams.UNSET;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    if (ref != null)
                        params.matchConstraintMinHeight = (int) (mViews.constraintLayout.getHeight() - ref.getY());
                    break;
            }
        }
        mViews.extendPanel.setLayoutParams(params);
        mViews.extendPanel.requestLayout();
    }

    @Override
    public FloatingActionButton enableActionButton() {
        if (mViews.listActionButton.getVisibility() == View.VISIBLE)
            mViews.listActionButton.setVisibility(View.INVISIBLE);
        TransitionManager.beginDelayedTransition(mViews.coordinatorLayout, new Fade());
        mViews.actionButton.setVisibility(View.VISIBLE);
        return mViews.actionButton;
    }

    @Override
    public void disableActionButton() {
        mViews.actionButton.setVisibility(View.GONE);
        if (mViews.listActionButton.getVisibility() == View.INVISIBLE)
            mViews.listActionButton.setVisibility(View.VISIBLE);
    }

    @Override
    public FloatingActionButton enableListActionButton(@DrawableRes int drawable, View.OnClickListener listener) {
        listFabStack.push(new Pair<>(drawable, listener));
        TransitionManager.beginDelayedTransition(mViews.coordinatorLayout, new Fade());
        mViews.listActionButton.setImageResource(drawable);
        mViews.listActionButton.setOnClickListener(listener);
        mViews.listActionButton.setVisibility(View.VISIBLE);
        return mViews.listActionButton;
    }

    @Override
    public void disableListActionButton() {
        if (! listFabStack.empty()) // it may be not enabled by logic
            listFabStack.pop();
        if (listFabStack.empty()) {
            mViews.listActionButton.setVisibility(View.GONE);
        } else {
            Pair<Integer, View.OnClickListener> peeked = listFabStack.peek();
            mViews.listActionButton.setImageResource(peeked.first);
            mViews.listActionButton.setOnClickListener(peeked.second);
        }
    }

    @Override
    public void popCurrent() {
        logger.debug("popCurrent()");
        mFragmentManager.popBackStack();
    }

    @Override
    public void popAll() {
        logger.debug("popAll()");
        FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
        mFragmentManager.popBackStack(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return mViews.coordinatorLayout;
    }

    private final OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        final Handler mBackHandler = new Handler(Looper.getMainLooper());

        @Override
        public void handleOnBackPressed() {
            if (secondBack) {
                mBackToast.cancel();
                this.setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            } else {
                secondBack = true;
                mBackToast.show();
                mBackHandler.postDelayed(() -> secondBack = false, 2000);
            }
        }
    };

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentLifecycleCallback = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            logger.warn("onFragmentPreAttached({})", f.getClass().getName());
        }

        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            logger.warn("onFragmentAttached({})", f.getClass().getName());
            mBackPressedCallback.setEnabled(false);
        }

        @Override
        public void onFragmentPreCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            logger.warn("onFragmentPreCreated({})", f.getClass().getName());
        }

        @Override
        public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            logger.warn("onFragmentCreated({})", f.getClass().getName());
        }

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
            logger.warn("onFragmentViewCreated({})", f.getClass().getName());
        }

        @Override
        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentStarted({})", f.getClass().getName());
        }

        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentResumed({})", f.getClass().getName());
            Class<?> cls = f.getClass();
            if (cls == Ruler.class || cls == RouteEdit.class) {
                mCrosshairLayer.lock(Color.RED);
                mObjectInteractionEnabled = false;
            }
        }

        @Override
        public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentPaused({})", f.getClass().getName());
            Class<?> cls = f.getClass();
            if (cls == Ruler.class || cls == RouteEdit.class) {
                mCrosshairLayer.unlock();
                mObjectInteractionEnabled = true;
            }
        }

        @Override
        public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentStopped({})", f.getClass().getName());
            if (mFragmentManager.getBackStackEntryCount() == 0 && mPanelState != PANEL_STATE.NONE)
                setPanelState(PANEL_STATE.NONE);
        }

        @Override
        public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
            logger.warn("onFragmentSaveInstanceState({})", f.getClass().getName());
        }

        @Override
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentViewDestroyed({})", f.getClass().getName());

            int count = mFragmentManager.getBackStackEntryCount();
            if (count == 0)
                return;
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
            Fragment fr = mFragmentManager.findFragmentByTag(bse.getName());
            if (fr == null)
                return;
            View fv = fr.getView();
            if (fv == null)
                return;
            if (fv.getForeground() == null)
                return;
            ObjectAnimator anim = ObjectAnimator.ofInt(fv.getForeground(), "alpha", 255, 0);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    fv.setForeground(null);
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                    fv.setForeground(null);
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {
                }
            });
            anim.setDuration(500);
            anim.start();
        }

        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentDestroyed({})", f.getClass().getName());
        }

        @Override
        public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
            logger.warn("onFragmentDetached({})", f.getClass().getName());
            mBackPressedCallback.setEnabled(mFragmentManager.getBackStackEntryCount() == 0);

            Class<? extends Fragment> cls = f.getClass();
            if (cls == BaseMapDownload.class) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    HelperUtils.showTargetedAdvice(
                            MainActivity.this,
                            Configuration.ADVICE_ENABLE_LOCATIONS,
                            R.string.advice_enable_locations,
                            mViews.locationButton,
                            false
                    );
            } else if (cls == BasePreferences.class)
                HelperUtils.showTargetedAdvice(
                        MainActivity.this,
                        Configuration.ADVICE_MAP_SETTINGS,
                        R.string.advice_map_settings,
                        mViews.mapsButton,
                        false
                );
        }
    };

    private void hideDownloadButton() {
        if (mViews.mapDownloadButton.getVisibility() == View.VISIBLE) {
            mViews.mapDownloadButton.setVisibility(View.GONE);
            mViews.mapDownloadButton.setTag(null);
        }
    }

    public void checkMissingData(MapPosition mapPosition) {
        if (MapTrekTileSource.MissingTileData.counter.get() == 0) {
            hideDownloadButton();
            return;
        }

        // Do not show button if we are already choosing maps
        if (mMapCoverageLayer != null) {
            hideDownloadButton();
            return;
        }

        // Do not show button if custom map is shown
        for (MapFile bitmapLayerMap : mBitmapLayerMaps)
            if (bitmapLayerMap.contains(mapPosition.getX(), mapPosition.getY())) {
                hideDownloadButton();
                return;
            }

        float tileScale = 1f / (1 << mapPosition.zoomLevel);
        int tileX = (int) (mapPosition.x / tileScale);
        int tileY = (int) (mapPosition.y / tileScale);
        String tag = tileX + "/" + tileY + "/" + mapPosition.zoomLevel;
        if (!tag.equals(mViews.mapDownloadButton.getTag())) {
            MapTile tile = null;
            try {
                tile = mBaseLayer.getManager().getTile(tileX, tileY, mapPosition.zoomLevel);
            } catch (IllegalArgumentException e) {
                // ignore - position is invalid on overscroll
            }
            if (tile != null) {
                mViews.mapDownloadButton.setTag(tag);
                int visibility = View.GONE;
                int[] map = null;
                MapTrekTileSource.MissingTileData tileData = (MapTrekTileSource.MissingTileData) tile.getData(MapTrekTileSource.TILE_DATA);
                if (tileData != null) {
                    int mapX = tile.tileX >> (tile.zoomLevel - 7);
                    int mapY = tile.tileY >> (tile.zoomLevel - 7);
                    if (!mapIndexViewModel.nativeIndex.isDownloading(mapX, mapY) && // Do not show button if this map is already downloading
                            !(mapIndexViewModel.nativeIndex.hasDownloadSizes()
                                    && mapIndexViewModel.nativeIndex.getNativeMap(mapX, mapY).downloadSize == 0L)) { // Do not show button if there is no map for that area
                        visibility = View.VISIBLE;
                        map = new int[]{mapX, mapY};
                    }
                }
                mViews.mapDownloadButton.setVisibility(visibility);
                mViews.mapDownloadButton.setTag(R.id.mapKey, map);
            }
        }
    }

    private final View.OnLayoutChangeListener layoutChangeListener = (v, l, t, r, b, ol, ot, or, ob) -> updateMapArea();

    private void updateMapArea() {
        logger.info("updateMapArea");

        Rect area = new Rect();
        mViews.mapView.getLocalVisibleRect(area);
        int mapWidth = area.width();
        int mapHeight = area.height();

        area.top = mViews.constraintLayout.getTop() + mViews.routePointBarrier.getBottom();

        int left = mViews.gaugesRightBarrier.getRight();
        if (left > 0) { // gauge panel is visible
            if (mapHeight / 3 < mViews.gaugesBottomBarrier.getBottom())
                area.left = left;
        }

        if (mVerticalOrientation)
            area.bottom = mViews.contentBarrier.getTop();
        else
            area.right = mViews.contentBarrier.getLeft();

        // if (mVerticalOrientation) {
        //     if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
        //             || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
        //         area.bottom = Math.min(area.bottom, mViews.bottomSheetPanel.getTop());
        // }

        if (!area.isEmpty()) {
            int pointerOffset = (int) (50 * MapTrek.density);

            int centerX = mapWidth / 2;
            int centerY = mapHeight / 2;
            mMovingOffset = Math.min(centerX - area.left, area.right - centerX);
            mMovingOffset = Math.min(mMovingOffset, centerY - area.top);
            mMovingOffset = Math.min(mMovingOffset, area.bottom - centerY);
            mMovingOffset -= pointerOffset;
            if (mMovingOffset < 0)
                mMovingOffset = 0;

            mTrackingOffset = area.bottom - mapHeight / 2 - 2 * pointerOffset;

            // float shiftedCenterY = (area.bottom - (area.height() >> 1)) * 2f / mapHeight - 1;
            // mMap.viewport().setMapViewCenter(shiftedCenterY);
        }

        BitmapRenderer renderer = mMapScaleBarLayer.getRenderer();
        renderer.setOffset(left + 8 * MapTrek.density, area.top);
    }

    private void askForPermission(int permissionRequest) {
        String permission = null;
        @StringRes int title = 0;
        @StringRes int rationale = 0;

        switch (permissionRequest) {
            case PERMISSIONS_REQUEST_FINE_LOCATION:
                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                title = R.string.titleLocationPermissionRationale;
                rationale = R.string.msgAccessFineLocationRationale;
                break;
            case PERMISSIONS_REQUEST_NOTIFICATION:
                if (Build.VERSION.SDK_INT >= 33 && !Configuration.notificationsDenied() && !mAskedNotificationPermission) {
                    permission = Manifest.permission.POST_NOTIFICATIONS;
                    title = R.string.titleNotificationPermissionRationale;
                    rationale = R.string.msgShowNotificationRationale;
                    mAskedNotificationPermission = true;
                }
                break;
        }
        if (permission == null)
            return;

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(permission) || permissionRequest == PERMISSIONS_REQUEST_NOTIFICATION) {
                String finalPermission = permission;
                String name;
                try {
                    PackageManager pm = getPackageManager();
                    PermissionInfo permissionInfo = pm.getPermissionInfo(permission, PackageManager.GET_META_DATA);
                    name = permissionInfo.loadLabel(pm).toString().toLowerCase();
                } catch (PackageManager.NameNotFoundException e) {
                    logger.error("Failed to obtain name for permission", e);
                    name = "access precise location";
                }
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(getString(rationale, name))
                        .setPositiveButton(R.string.actionGrant, (dialog, which) -> requestPermissions(new String[]{finalPermission}, permissionRequest))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                        .create()
                        .show();
            } else {
                requestPermissions(new String[]{permission}, permissionRequest);
            }
        } else if (permissionRequest == PERMISSIONS_REQUEST_FINE_LOCATION) {
            enableLocations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocations();
                } else {
                    HelperUtils.showError(getString(R.string.msgLocationPermissionError), R.string.actionGrant, view -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(intent);
                    }, mViews.coordinatorLayout);
                }
                break;
            }
            case PERMISSIONS_REQUEST_NOTIFICATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Save decision only if user implicitly denied notifications
                    Configuration.setNotificationsDenied();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 26) {
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<List<FileDataSource>> onCreateLoader(int id, Bundle args) {
        logger.debug("onCreateLoader({})", id);
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<FileDataSource>> loader, List<FileDataSource> data) {
        logger.debug("onLoadFinished()");
        if (data == null)
            return;
        dataSourceViewModel.fileDataSources = data;
        // TODO: May be compare lists first?
        for (FileDataSource source : dataSourceViewModel.fileDataSources) {
            if (source.isLoaded() && source.isLoadable() && !source.isVisible()) {
                addSourceToMap(source);
                source.setVisible(true);
            }
        }
        dataSourceViewModel.updateCurrentDataSources();
        mMap.updateMap(true);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<FileDataSource>> loader) {

    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("Broadcast: {}", action);
            if (MapWorker.BROADCAST_MAP_ADDED.equals(action) || MapWorker.BROADCAST_MAP_REMOVED.equals(action)) {
                if (mProgressHandler != null && MapWorker.BROADCAST_MAP_ADDED.equals(action))
                    mProgressHandler.onProgressFinished();
                mMap.clearMap();
            }
            if (MapWorker.BROADCAST_MAP_STARTED.equals(action)) {
                // TODO: handle rotation (ViewModel?)
                final Bundle extras = intent.getExtras();
                if (extras == null)
                    return;
                UUID id = (UUID) extras.getSerializable(MapWorker.EXTRA_UUID);
                if (id == null)
                    return;
                if (mProgressHandler != null)
                    mProgressHandler.onProgressStarted(100);
                WorkManager.getInstance(getApplicationContext())
                        .getWorkInfoByIdLiveData(id)
                        .observe(MainActivity.this, workInfo -> {
                            if (workInfo != null) {
                                Data progress = workInfo.getProgress();
                                int value = progress.getInt(MapWorker.PROGRESS, 0);
                                if (mProgressHandler != null)
                                    mProgressHandler.onProgressChanged(value);
                            }
                        });
            }
            if (MapWorker.BROADCAST_MAP_FAILED.equals(action)) {
                if (mProgressHandler != null)
                    mProgressHandler.onProgressFinished();
                final Bundle extras = intent.getExtras();
                String title = extras != null ? extras.getString(MapWorker.EXTRA_TITLE) : getString(R.string.map);
                HelperUtils.showError(getString(R.string.msgMapDownloadFailed, title), mViews.coordinatorLayout);
            }
            if (BaseLocationService.BROADCAST_TRACK_STATE.equals(action)) {
                int stateOrdinal = intent.getIntExtra("state", TRACKING_STATE.DISABLED.ordinal());
                TRACKING_STATE state = TRACKING_STATE.values()[stateOrdinal];
                logger.info("{}: {}", action, state);
                trackViewModel.trackingState.setValue(state);
            }
            if (BaseLocationService.BROADCAST_TRACK_SAVE.equals(action)) {
                final Bundle extras = intent.getExtras();
                boolean saved = extras != null && extras.getBoolean("saved");
                if (saved) {
                    logger.debug("Track saved: {}", extras.getString("path"));
                    Snackbar.make(mViews.coordinatorLayout, R.string.msgTrackSaved, Snackbar.LENGTH_LONG)
                            .setAction(R.string.actionCustomize, view -> onTrackProperties(extras.getString("path")))
                            .setAnchorView(mViews.actionPanel)
                            .show();
                    return;
                }
                String reason = extras != null ? extras.getString("reason") : null;
                logger.warn("Track not saved: {}", reason);
                if ("period".equals(reason) || "distance".equals(reason)) {
                    int msg = "period".equals(reason) ? R.string.msgTrackNotSavedPeriod : R.string.msgTrackNotSavedDistance;
                    Snackbar.make(mViews.coordinatorLayout, msg, Snackbar.LENGTH_LONG)
                            .setAction(R.string.actionSave, view -> mLocationService.saveTrack())
                            .setAnchorView(mViews.actionPanel)
                            .show();
                } else {
                    Exception e = extras != null ? (Exception) extras.getSerializable("exception") : null;
                    if (e == null)
                        e = new RuntimeException("Unknown error");
                    HelperUtils.showSaveError(MainActivity.this, mViews.coordinatorLayout, e);
                }
            }
            if (BaseNavigationService.BROADCAST_NAVIGATION_STATE.equals(action)) {
                int state = intent.getIntExtra(BaseNavigationService.EXTRA_STATE, -1);
                if (state == BaseNavigationService.STATE_STARTED) {
                    enableNavigation();
                    updateNavigationUI(true);
                    updateNavigationGauges(true);
                    if (mLocationState != LocationState.DISABLED)
                        askForPermission(PERMISSIONS_REQUEST_NOTIFICATION);
                }
                if (state == BaseNavigationService.STATE_NEXT_ROUTE_POINT) {
                    updateNavigationGauges(true);
                    if (mNavigationLayer != null && mNavigationService != null)
                        mNavigationLayer.setRemainingPoints(mNavigationService.getRemainingPoints(), mNavigationService.getPrevRoutePoint());
                }
                if (state == BaseNavigationService.STATE_STOPPED) {
                    disableNavigation();
                }
                updatePanels();
            }
            if (BaseNavigationService.BROADCAST_NAVIGATION_STATUS.equals(action)) {
                updateNavigationGauges(false);
                if (mNavigationService != null) {
                    adjustNavigationArrow(mNavigationService.getTurn());
                    if (intent.getBooleanExtra(BaseNavigationService.EXTRA_MOVING_TARGET, false) && mNavigationLayer != null)
                        mNavigationLayer.setRemainingPoints(mNavigationService.getRemainingPoints(), null);
                }
            }
        }
    };

    class PlaceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("Broadcast: {}", action);
            if (PlaceDbDataSource.BROADCAST_PLACES_MODIFIED.equals(action)) {
                ContextUtils.sendExplicitBroadcast(MainActivity.this, PlaceDbDataSource.BROADCAST_PLACES_MODIFIED);
            }
            if (PlaceDbDataSource.BROADCAST_PLACES_RESTORED.equals(action)) {
                for (Place place : dataSourceViewModel.placeDbDataSource.getPlaces())
                    removePlaceMarker(place);
                dataSourceViewModel.placeDbDataSource.close();
            }
            if (PlaceDbDataSource.BROADCAST_PLACES_REWRITTEN.equals(action)) {
                dataSourceViewModel.placeDbDataSource.open();
                dataSourceViewModel.placeDbDataSource.notifyListeners();
                for (Place place : dataSourceViewModel.placeDbDataSource.getPlaces()) {
                    addPlaceMarker(place);
                }
            }
        }
    }

    private void addSourceToMap(FileDataSource source) {
        for (Place place : source.places) {
            addPlaceMarker(place);
            mTotalDataItems++;
        }
        for (Track track : source.tracks) {
            TrackLayer trackLayer = new TrackLayer(mMap, track);
            mMap.layers().add(trackLayer, MAP_DATA);
            mTotalDataItems++;
        }
        for (Route route : source.routes) {
            Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.route_start, route.style.color));
            MarkerSymbol startSymbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
            bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.route_point, route.style.color));
            MarkerSymbol pointSymbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
            bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.route_end, route.style.color));
            MarkerSymbol endSymbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
            RouteLayer routeLayer = new RouteLayer(mMap, route, pointSymbol, startSymbol, endSymbol);
            routeLayer.setEnabled(Boolean.FALSE.equals(routeViewModel.isRouting.getValue()));
            mMap.layers().add(routeLayer, MAP_DATA);
            mTotalDataItems++;
        }
    }

    private void removeSourceFromMap(FileDataSource source) {
        for (Place place : source.places) {
            removePlaceMarker(place);
            mTotalDataItems--;
        }
        for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if (layer instanceof TrackLayer) {
                if (source.tracks.contains(((TrackLayer) layer).getTrack())) {
                    i.remove();
                    layer.onDetach();
                    mTotalDataItems--;
                }
            } else if (layer instanceof RouteLayer) {
                if (source.routes.contains(((RouteLayer) layer).getRoute())) {
                    i.remove();
                    layer.onDetach();
                    mTotalDataItems--;
                }
            }
        }
    }

    private void addPlaceMarker(Place place) {
        MarkerItem marker = new MarkerItem(place, place.name, place.description, place.coordinates, place.style.color);
        if (place.style.color != 0 && place.style.color != MarkerStyle.DEFAULT_COLOR) {
            Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, place.style.color));
            marker.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
        }
        mMarkerLayer.addItem(marker);
    }

    private void removePlaceMarker(Place place) {
        MarkerItem marker = mMarkerLayer.getByUid(place);
        mMarkerLayer.removeItem(marker);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mObjectInteractionEnabled && mMap.getEventLayer().moveEnabled()) {
                AbstractMapEventLayer eventLayer = mMap.getEventLayer();
                eventLayer.enableMove(false);
                eventLayer.enableRotation(false);
                eventLayer.enableTilt(false);
                eventLayer.enableZoom(false);
                mCrosshairLayer.lock(mColorAccent);
                mPositionLocked = true;
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            for (Layer layer : mMap.layers()) {
                if (layer instanceof TrackLayer || layer instanceof MapObjectLayer
                        || layer instanceof MarkerLayer || layer instanceof RouteLayer)
                    layer.setEnabled(false);
            }
            mMap.updateMap(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mObjectInteractionEnabled) {
                AbstractMapEventLayer eventLayer = mMap.getEventLayer();
                eventLayer.enableMove(true);
                eventLayer.enableRotation(Boolean.FALSE.equals(mapViewModel.rotationLocked.getValue()));
                eventLayer.enableTilt(true);
                eventLayer.enableZoom(true);
                mCrosshairLayer.unlock();
                mPositionLocked = false;
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            for (Layer layer : mMap.layers()) {
                if (layer instanceof TrackLayer || layer instanceof MapObjectLayer
                        || layer instanceof MarkerLayer)
                    layer.setEnabled(true);
                if (layer instanceof RouteLayer)
                    layer.setEnabled(layer == mNavigationLayer || Boolean.FALSE.equals(routeViewModel.isRouting.getValue()));
            }
            mMap.updateMap(true);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public Map getMap() {
        return mMap;
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
        Loader<List<FileDataSource>> loader = LoaderManager.getInstance(this).getLoader(0);
        if (loader != null)
            ((DataLoader) loader).markDataSourceLoadable(source, available);
        mMap.updateMap(true);
    }

    private final Observer<DataSourceViewModel.SelectedDataSourceState> dataSourceCommandObserver = new Observer<DataSourceViewModel.SelectedDataSourceState>() {
        @Override
        public void onChanged(DataSourceViewModel.SelectedDataSourceState dataSourceState) {
            if (dataSourceState.dataSource.isNativeTrack()) {
                Track track = ((TrackDataSource) dataSourceState.dataSource).getTracks().get(0);
                onTrackDetails(track);
            } else if (dataSourceState.dataSource.isNativeRoute()) {
                Route route = ((RouteDataSource) dataSourceState.dataSource).getRoutes().get(0);
                onRouteDetails(route);
            } else if (dataSourceState.dataSource.isIndividual()) {
                Cursor cursor = dataSourceState.dataSource.getCursor();
                cursor.moveToPosition(0);
                int itemType = dataSourceState.dataSource.getDataType(0);
                if (itemType == DataSource.TYPE_PLACE) {
                    Place place = ((PlaceDataSource) dataSourceState.dataSource).cursorToPlace(cursor);
                    onPlaceDetails(place, true);
                } else if (itemType == DataSource.TYPE_TRACK) {
                    Track track = ((TrackDataSource) dataSourceState.dataSource).cursorToTrack(cursor);
                    onTrackDetails(track);
                } else if (itemType == DataSource.TYPE_ROUTE) {
                    Route route = ((RouteDataSource) dataSourceState.dataSource).cursorToRoute(cursor);
                    onRouteDetails(route);
                }
            } else {
                Fragment fragment = mFragmentManager.findFragmentByTag("dataList");
                if (fragment == null) {
                    FragmentFactory factory = mFragmentManager.getFragmentFactory();
                    fragment = factory.instantiate(getClassLoader(), DataList.class.getName());
                    if (dataSourceState.mode == DataSourceViewModel.MODE_SELECTOR) {
                        FragmentTransaction ft = mFragmentManager.beginTransaction();
                        fragment.setEnterTransition(new Fade());
                        ft.add(R.id.extendPanel, fragment, "dataList");
                        ft.addToBackStack("dataList");
                        ft.commit();
                    }
                }
                if (dataSourceState.mode == DataSourceViewModel.MODE_PANEL) {
                    showExtendPanel(PANEL_STATE.PLACES, "dataList", fragment);
                }
            }
        }
    };

    @Override
    public void onDataSourceShare(@NonNull final DataSource dataSource) {
        final boolean askName = dataSource.name == null || dataSource instanceof PlaceDbDataSource;
        final AtomicInteger selected = new AtomicInteger(0);
        final EditText inputView = new EditText(this);
        final DialogInterface.OnClickListener exportAction = (dialog, which) -> {
            if (askName)
                dataSource.name = inputView.getText().toString();
            DataExport.Builder builder = new DataExport.Builder();
            @FileDataSource.Format int format = dataSource.isNativeTrack() ? selected.get() : selected.get() + 1;
            DataExport dataExport = builder.setDataSource(dataSource).setFormat(format).create();
            dataExport.show(mFragmentManager, "dataExport");
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(dataSource.isNativeTrack() ? R.array.track_format_array : R.array.data_format_array,
                selected.get(), (dialog, which) -> selected.set(which));
        if (askName) {
            builder.setPositiveButton(R.string.actionContinue, (dialogInterface, i) -> {
                AlertDialog.Builder nameBuilder = new AlertDialog.Builder(MainActivity.this);
                nameBuilder.setTitle(R.string.title_input_name);
                nameBuilder.setPositiveButton(R.string.actionContinue, null);
                final AlertDialog dialog = nameBuilder.create();
                if (dataSource.name != null)
                    inputView.setText(dataSource.name);
                int margin = getResources().getDimensionPixelOffset(R.dimen.dialogContentMargin);
                dialog.setView(inputView, margin, margin >> 1, margin, 0);
                Window window = dialog.getWindow();
                if (window != null)
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (!inputView.getText().toString().trim().isEmpty()) {
                        exportAction.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                        // Hide keyboard
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(inputView.getRootView().getWindowToken(), 0);
                        dialog.dismiss();
                    }
                });
            });
        } else {
            builder.setPositiveButton(R.string.actionContinue, exportAction);
        }
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            StringBuilder stringBuilder = new StringBuilder();
            if (dataSource.isNativeTrack()) {
                stringBuilder.append(getString(R.string.msgNativeFormatExplanation));
                stringBuilder.append(" ");
            }
            stringBuilder.append(getString(R.string.msgOtherFormatsExplanation));
            builder1.setMessage(stringBuilder.toString());
            builder1.setPositiveButton(R.string.ok, null);
            AlertDialog dialog1 = builder1.create();
            dialog1.show();
        });
    }

    @Override
    public void onDataSourceDelete(@NonNull final DataSource source) {
        if (!(source instanceof FileDataSource)) {
            HelperUtils.showError(getString(R.string.msgCannotDeleteNativeSource), mViews.coordinatorLayout);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.title_delete_permanently);
        builder.setMessage(R.string.msgDeleteSourcePermanently);
        builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
            File sourceFile = new File(((FileDataSource) source).path);
            if (sourceFile.exists()) {
                if (sourceFile.delete()) {
                    removeSourceFromMap((FileDataSource) source);
                } else {
                    HelperUtils.showError(getString(R.string.msgDeleteFailed), mViews.coordinatorLayout);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onAmenityKindVisibilityChanged() {
        Configuration.saveKindZoomState();
        Tags.recalculateTypeZooms();
        mMap.clearMap();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == 0) {
            String message = resultData.getString("message", "Failed to find route");
            HelperUtils.showError(message, mViews.coordinatorLayout);
            return;
        }
        double distance = resultData.getDouble("distance");
        long time = resultData.getLong("time");
        String poly = resultData.getString("points");
        HelperUtils.showError(distance + " " + time, mViews.coordinatorLayout);
        if (poly != null) {
            Track track = new Track();
            ArrayList<GeoPoint> points = GraphHopperService.decodePolyline(poly, 0, false);
            for (GeoPoint point : points)
                track.addPointFast(true, point.latitudeE6, point.longitudeE6, 0f, 0f, 0f, 0f, 0L);
            mMap.layers().add(new TrackLayer(mMap, track));
        }
    }

    /** @noinspection unused*/
    @Subscribe
    public void onConfigurationChanged(Configuration.ChangedEvent event) {
        switch (event.key) {
            case Configuration.PREF_SPEED_UNIT: {
                int unit = Configuration.getSpeedUnit();
                Resources resources = getResources();
                StringFormatter.speedFactor = Float.parseFloat(resources.getStringArray(R.array.speed_factors)[unit]);
                StringFormatter.speedAbbr = resources.getStringArray(R.array.speed_abbreviations)[unit];
                mViews.gaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_DISTANCE_UNIT: {
                int unit = Configuration.getDistanceUnit();
                Resources resources = getResources();
                StringFormatter.distanceFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors)[unit]);
                StringFormatter.distanceAbbr = resources.getStringArray(R.array.distance_abbreviations)[unit];
                StringFormatter.distanceShortFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors_short)[unit]);
                StringFormatter.distanceShortAbbr = resources.getStringArray(R.array.distance_abbreviations_short)[unit];
                mViews.gaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_ELEVATION_UNIT: {
                int unit = Configuration.getElevationUnit();
                Resources resources = getResources();
                StringFormatter.elevationFactor = Float.parseFloat(resources.getStringArray(R.array.elevation_factors)[unit]);
                StringFormatter.elevationAbbr = resources.getStringArray(R.array.elevation_abbreviations)[unit];
                mViews.gaugePanel.refreshGauges();
                mMap.clearMap();
                break;
            }
            case Configuration.PREF_ANGLE_UNIT: {
                int unit = Configuration.getAngleUnit();
                Resources resources = getResources();
                StringFormatter.angleFactor = Double.parseDouble(resources.getStringArray(R.array.angle_factors)[unit]);
                StringFormatter.angleAbbr = resources.getStringArray(R.array.angle_abbreviations)[unit];
                mViews.gaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_UNIT_PRECISION: {
                boolean precision = Configuration.getUnitPrecision();
                StringFormatter.precisionFormat = precision ? "%.1f" : "%.0f";
                mViews.gaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_ZOOM_BUTTONS_VISIBLE: {
                boolean visible = Configuration.getZoomButtonsVisible();
                mViews.coordinatorLayout.findViewById(R.id.mapZoomHolder).setVisibility(visible ? View.VISIBLE : View.GONE);
                break;
            }
            case Configuration.PREF_PLACE_TITLES: {
                mMarkerLayer.setTitlesEnabled(Configuration.getPlaceTitlesEnabled());
                mMarkerLayer.updateItems();
                mMap.updateMap();
                break;
            }
            case Configuration.PREF_ACCESSIBILITY_BADGES: {
                Tags.accessibility = Configuration.getAccessibilityBadgesEnabled();
                mMap.clearMap();
                break;
            }
            case Configuration.PREF_CONFIRM_EXIT: {
                if (Configuration.getConfirmExitEnabled())
                    getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
                else
                    mBackPressedCallback.remove();
                break;
            }
            case Configuration.PREF_MAP_HILLSHADES: {
                boolean enabled = Configuration.getHillshadesEnabled();
                if (enabled)
                    showHillShade();
                else
                    hideHillShade();
                mMap.clearMap();
                break;
            }
            case Configuration.PREF_HILLSHADES_TRANSPARENCY: {
                int transparency = Configuration.getHillshadesTransparency();
                if (mHillshadeLayer != null)
                    mHillshadeLayer.setBitmapAlpha(1 - transparency * 0.01f, true);
                break;
            }
            case Configuration.PREF_POINTER_TYPE: {
                int type = Configuration.getPointerType();
                mLocationOverlay.setType(type);
                break;
            }
            case Configuration.PREF_POINTER_SIZE: {
                int size = Configuration.getPointerSize();
                mLocationOverlay.setSize(size);
                break;
            }
            case Configuration.PREF_POINTER_COLOR: {
                int color = Configuration.getPointerColor();
                mLocationOverlay.setColor(color);
                break;
            }
            case Configuration.PREF_PLACE_COLOR: {
                // Items with default color do not have markers
                for (MarkerItem item : mMarkerLayer.getItems()) {
                    if (item.getMarker() == null) {
                        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, item.color));
                        item.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
                    }
                }

                MarkerStyle.DEFAULT_COLOR = Configuration.loadInt(Configuration.PREF_PLACE_COLOR, getColor(R.color.placeColor01));
                Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this));
                MarkerSymbol symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
                mMarkerLayer.setDefaultMarker(symbol);
                break;
            }
            case Configuration.PREF_TRACK_WIDTH: {
                int width = Configuration.loadInt(Configuration.PREF_TRACK_WIDTH, getResources().getInteger(R.integer.default_track_width));
                for (Layer layer : mMap.layers()) {
                    if (layer instanceof TrackLayer) {
                        ((TrackLayer) layer).setWidth(width);
                    }
                }
                break;
            }
            case Configuration.PREF_ROUTE_WIDTH: {
                int width = Configuration.loadInt(Configuration.PREF_ROUTE_WIDTH, getResources().getInteger(R.integer.default_route_width));
                for (Layer layer : mMap.layers()) {
                    if (layer instanceof RouteLayer && layer != mNavigationLayer) {
                        ((RouteLayer) layer).setWidth(width);
                    }
                }
                break;
            }
            case Configuration.PREF_ACTIVE_ROUTE_COLOR:
            case Configuration.PREF_ACTIVE_ROUTE_WIDTH: {
                if (mNavigationLayer != null) {
                    int color = Configuration.loadInt(Configuration.PREF_ACTIVE_ROUTE_COLOR, getColor(R.color.activeRouteColor));
                    int width = Configuration.loadInt(Configuration.PREF_ACTIVE_ROUTE_WIDTH, getResources().getInteger(R.integer.default_route_width));
                    mNavigationLayer.setLineStyle(color, width);
                }
                break;
            }
        }
    }

    private void checkNightMode(Location location) {
        if (mNextNightCheck > mLastLocationMilliseconds)
            return;

        mSunriseSunset.setLocation(location.getLatitude(), location.getLongitude());
        final boolean isNightTime = !mSunriseSunset.isDaytime((location.getTime() * 1d / 3600000) % 24);

        if (isNightTime ^ mNightMode) {
            int nightMode = isNightTime ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nightMode);
            getDelegate().setLocalNightMode(nightMode);
        }

        mNextNightCheck = mLastLocationMilliseconds + NIGHT_CHECK_PERIOD;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level > TRIM_MEMORY_MODERATE) {
            mapViewModel.shieldFactory.dispose();
            mapViewModel.osmcSymbolFactory.dispose();
            mMap.clearMap();
        }
    }

    private void zoomMap(double scaleBy, float x, float y) {
        if (mLocationOverlay.isEnabled() && mLocationOverlay.isVisible()) {
            Point out = new Point();
            mMap.viewport().toScreenPoint(mLocationOverlay.getX(), mLocationOverlay.getY(), true, out);
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION >> 2, scaleBy, (float) out.x, (float) out.y);
        } else {
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION, scaleBy, x, y);
        }
    }

    private void setMapTheme() {
        Configuration.loadKindZoomState();
        Tags.recalculateTypeZooms();
        ThemeFile themeFile;
        switch (Configuration.getActivity()) {
            case 3:
                themeFile = Themes.WINTER;
                Configuration.accountSkiing();
                mNightMode = false;
                break;
            case 2:
                if (Tags.kindZooms[13] == 18) {
                    Tags.kindZooms[13] = 14;
                    Tags.recalculateTypeZooms();
                }
                themeFile = Themes.MAPTREK;
                Configuration.accountCycling();
                mNightMode = false;
                break;
            case 1:
                if (Tags.kindZooms[13] == 18) {
                    Tags.kindZooms[13] = 14;
                    Tags.recalculateTypeZooms();
                }
                themeFile = Themes.MAPTREK;
                Configuration.accountHiking();
                mNightMode = false;
                break;
            case 0:
            default:
                int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                mNightMode = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                // TODO Show advises on cursor hide
                if (mNightMode) {
                    mMainHandler.postDelayed(() -> HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_NIGHT_MODE, R.string.advice_night_mode, mViews.mapsButton, false), 2000);
                } else if (Configuration.getRunningTime() > 10) {
                    mMainHandler.postDelayed(() -> HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_MAP_LEGEND, R.string.advice_map_legend, mViews.mapsButton, false), 2000);
                }
                themeFile = mNightMode ? Themes.NIGHT : Themes.MAPTREK;
                break;
        }
        float fontSize = Themes.MAP_FONT_SIZES[Configuration.getMapFontSize()];
        float mapScale = Themes.MAP_SCALE_SIZES[Configuration.getMapUserScale()];
        CanvasAdapter.textScale = fontSize / mapScale;
        CanvasAdapter.userScale = mapScale;
        IRenderTheme theme = ThemeLoader.load(themeFile);
        mMap.setTheme(theme, true);
        int color = getResources().getColor(R.color.colorBackground, getTheme());
        mMarkerLayer.setOutlineColor(color); // this is redundant as activity is recreated on night mode change
        mMarkerLayer.updateItems();
        mapViewModel.shieldFactory.setFontSize(fontSize);
        mapViewModel.shieldFactory.dispose();
        mapViewModel.osmcSymbolFactory.dispose();
        mapViewModel.theme.postValue(theme);
    }

    private void hideSystemUI() {
        Configuration.setHideSystemUI(true);
        Configuration.accountFullScreen();

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        mMap.updateMap();
    }

    private void showSystemUI() {
        mMainHandler.removeMessages(R.id.msgHideSystemUI);
        Configuration.setHideSystemUI(false);

        WindowInsetsControllerCompat windowInsetsController =  WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Show the system bars.
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars());

        mMap.updateMap();
    }

    private void showLicense(String license) {
        Spanned html = Html.fromHtml(license != null ? license : getString(R.string.osmLicense));
        mViews.license.setText(html);
        mViews.license.setVisibility(View.VISIBLE);
        final Message m = Message.obtain(mMainHandler,
                () -> mViews.license.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViews.license.setVisibility(View.GONE);
                        mViews.license.animate().setListener(null);
                    }
                }));
        m.what = R.id.msgRemoveLicense;
        mMainHandler.sendMessageDelayed(m, 120000);
    }

    public boolean isOnline() {
        // FIXME temporary
        return false;
        /*
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
        */
    }

    @Override
    public String getStatsString() {
        return Configuration.getRunningTime() + "," +
                Configuration.getTrackingTime() + "," +
                dataSourceViewModel.placeDbDataSource.getPlacesCount() + "," +
                dataSourceViewModel.fileDataSources.size() + "," +
                mapIndexViewModel.nativeIndex.getMapsCount() + "," +
                mMapIndex.getMaps().size() + "," +
                Configuration.getFullScreenTimes() + "," +
                Configuration.getHikingTimes() + "," +
                Configuration.getSkiingTimes() + "," +
                Configuration.getCyclingTimes();
    }

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }

    /** @noinspection unused*/
    @Subscribe
    public void onNewPluginEntry(Pair<String, Pair<Drawable, Intent>> entry) {
        mPluginRepository.addPluginEntry(entry);
    }
}
