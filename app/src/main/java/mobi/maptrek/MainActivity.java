package mobi.maptrek;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.oscim.android.MapScaleBar;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.data.Track;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.util.ProgressHandler;

import static org.oscim.android.canvas.AndroidGraphics.drawableToBitmap;

public class MainActivity extends Activity implements ILocationListener, Map.UpdateListener, ItemizedLayer.OnItemGestureListener<MarkerItem>, PopupMenu.OnMenuItemClickListener, LoaderManager.LoaderCallbacks<List<Track>> {
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

    private ProgressHandler mProgressHandler;

    private ILocationService mLocationService = null;
    private boolean mIsBound = false;
    private LOCATION_STATE mLocationState;
    private LOCATION_STATE mSavedLocationState;
    private TRACKING_STATE mTrackingState;
    private MapPosition mMapPosition = new MapPosition();

    protected Map mMap;
    protected MapView mMapView;
    private TextView mSatellitesText;
    private TextView mSpeedText;
    private ImageButton mLocationButton;
    private ImageButton mRecordButton;
    private ImageButton mMoreButton;
    private View mCompassView;
    private View mGaugePanelView;
    private ProgressBar mProgressBar;

    private long mLastLocationMilliseconds = 0;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    private TileGridLayer mGridLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private LocationOverlay mLocationOverlay;

    private TileCache mCache;

    private static final boolean BILLBOARDS = true;
    //private MarkerSymbol mFocusMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        mLocationState = LOCATION_STATE.DISABLED;
        mSavedLocationState = LOCATION_STATE.DISABLED;

        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
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

        TileSource tileSource = new OSciMap4TileSource();

        File cacheDir = getExternalCacheDir();
        if (cacheDir != null) {
            mCache = new TileCache(this, cacheDir.getAbsolutePath(), "tile_cache.db");
            mCache.setCacheSize(512 * (1 << 10));
            tileSource.setCache(mCache);
        }
        VectorTileLayer baseLayer = mMap.setBaseMap(tileSource);
        mGridLayer = new TileGridLayer(mMap);
        mLocationOverlay = new LocationOverlay(mMap);

		/* set initial position on first run */
        MapPosition pos = new MapPosition();
        mMap.getMapPosition(pos);
        if (pos.x == 0.5 && pos.y == 0.5)
            mMap.setMapPosition(55.8194, 37.6676, Math.pow(2, 16));

        Layers layers = mMap.layers();

        //BitmapTileLayer mBitmapLayer = new BitmapTileLayer(mMap, DefaultSources.OPENSTREETMAP.build());
        //mMap.layers().add(mBitmapLayer);

        layers.add(new BuildingLayer(mMap, baseLayer));
        layers.add(new LabelLayer(mMap, baseLayer));
        layers.add(new MapScaleBar(mMapView));
        layers.add(mLocationOverlay);

        Bitmap bitmap = drawableToBitmap(getResources(), R.drawable.marker_poi);

        MarkerSymbol symbol;
        if (BILLBOARDS)
            symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
        else
            symbol = new MarkerSymbol(bitmap, 0.5f, 0.5f, false);

        ItemizedLayer<MarkerItem> markerLayer =
                new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), symbol, this);

        mMap.layers().add(markerLayer);

        android.graphics.Bitmap pin = BitmapFactory.decodeResource(resources, R.mipmap.marker_pin_1);
        android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(pin.getWidth(), pin.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY));

        Canvas bc = new Canvas(image);
        bc.drawBitmap(pin, 0f, 0f, paint);

        MarkerItem marker = new MarkerItem("Home", "", new GeoPoint(55.813557, 37.645524));
        marker.setMarker(new MarkerSymbol(new AndroidBitmap(image), MarkerItem.HotspotPlace.BOTTOM_CENTER));

        List<MarkerItem> pts = new ArrayList<>();
        pts.add(marker);

        markerLayer.addItems(pts);

        mMap.setTheme(VtmThemes.DEFAULT);

        if (BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

        mProgressHandler = new ProgressHandler(mProgressBar);

        getLoaderManager();
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

        Loader<List<Track>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader)loader).setProgressHandler(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        mMap.destroy();
        if (mCache != null)
            mCache.dispose();

        mProgressHandler = null;
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
        boolean shouldBePinned = false;
        if (mLocationState == LOCATION_STATE.SEARCHING) {
            mLocationState = mSavedLocationState;
            mMap.getEventLayer().setFixOnCenter(true);
            updateLocationDrawable();
            shouldBePinned = true;
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
            mMapPosition.setPosition(lat, lon);
            mMapPosition.setBearing(-mAveragedBearing);
            if (shouldBePinned) {
                mMovementAnimationDuration = (int) (mMovementAnimationDuration * 0.9);
                mMap.animator().setListener(new org.oscim.map.Animator.MapAnimationListener() {
                    @Override
                    public void onMapAnimationEnd() {
                        Log.e(TAG, "from onLocationChanged()");
                        mLocationOverlay.setPinned(true);
                    }
                });
            }
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

    public void onLocationClicked(View view) {
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

    public void onRecordClicked(View view) {
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
        Log.e(TAG, item.getTitle());
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
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
            mLocationService.setProgressHandler(null);
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
            mLocationService.setProgressHandler(mProgressHandler);
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
        }
    };

    private void enableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK));
        mCurrentTrackLayer = new CurrentTrackLayer(mMap, Color.fade(getColor(R.color.trackColor), 0.7), getResources().getInteger(R.integer.trackWidth), getApplicationContext());
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

    private void updateMapViewArea() {
        final ViewTreeObserver vto = mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                Rect area = new Rect();
                mMapView.getLocalVisibleRect(area);
                if (mGaugePanelView != null)
                    area.top = mGaugePanelView.getBottom();
                View v = findViewById(R.id.actionPanel);
                if (v != null)
                    area.bottom = v.getTop();
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
                /*
                if (!area.isEmpty())
                    map.updateViewArea(area);
                */
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
                // Show an expanation to the user *asynchronously* -- don't block
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
    public Loader<List<Track>> onCreateLoader(int id, Bundle args) {
        Log.e(TAG, "onCreateLoader(" + id + ")");
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<Track>> loader, List<Track> data) {
        Log.e(TAG, "onLoadFinished()");
        if (data == null)
            return;
        for (Track track : data) {
            if (track.color == -1)
                track.color = getColor(R.color.trackColor);
            if (track.width == -1)
                track.width = getResources().getInteger(R.integer.trackWidth);
            TrackLayer trackLayer = new TrackLayer(mMap, track, track.color, track.width);
            mMap.layers().add(trackLayer);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Track>> loader) {

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast: " + action);
            if (action.equals(BaseLocationService.BROADCAST_TRACK_SAVE))
            {
                Bundle extras = intent.getExtras();
                boolean saved = extras.getBoolean("saved");
                if (saved) {
                    Log.e(TAG, "Track saved: " + extras.getString("file"));
                    return;
                }
                String reason = extras.getString("reason");
                Log.e(TAG, "Track not saved: " + reason);
            }
        }
    };

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }
}