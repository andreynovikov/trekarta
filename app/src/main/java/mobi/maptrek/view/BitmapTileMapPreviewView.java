package mobi.maptrek.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapElement;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.JobQueue;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileDistanceSort;
import org.oscim.layers.tile.TileSet;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.TileSource;
import org.oscim.utils.PausableThread;
import org.oscim.utils.ScanBox;
import org.oscim.utils.quadtree.TileIndex;

import java.util.ArrayList;
import java.util.HashMap;

import static org.oscim.layers.tile.MapTile.State.DEADBEEF;
import static org.oscim.layers.tile.MapTile.State.LOADING;
import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;

public class BitmapTileMapPreviewView extends TextureView implements SurfaceTextureListener {
    private static final String TAG = "BTMPV";

    private static final int CACHE_LIMIT = 40;
    private static final int BITMAP_TILE_SIZE = 256;

    private final float[] mMapPlane = new float[8];

    private TileSet mNewTiles;
    private HashMap<MapTile, Bitmap> mTileMap;
    protected BitmapTileLoader mTileLoader;


    /**
     * new tile jobs for MapWorkers
     */
    private final ArrayList<MapTile> mJobs;

    /**
     * job queue filled in TileManager and polled by TileLoaders
     */
    private final JobQueue mJobQueue;

    /**
     * cache for all tiles
     */
    private MapTile[] mTiles;

    /**
     * actual number of tiles in mTiles
     */
    private int mTilesCount;

    /**
     * current end position in mTiles
     */
    private int mTilesEnd;

    //mDrawThread that will drive the animation
    private DrawThread mDrawThread;
    private TileSource mTileSource;
    private boolean mActive;
    private MapPosition mPosition;
    private int mHalfWidth;
    private int mHalfHeight;

    public BitmapTileMapPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSurfaceTextureListener(this);
        setOpaque(false);
        mJobQueue = new JobQueue();
        mJobs = new ArrayList<>();
        mTiles = new MapTile[CACHE_LIMIT];
    }

    /*
     * Listener tells us when the texture has been created and is ready to be drawn on.
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (!mActive)
            mTileSource.open();
        mIndex.drop();

        mTileMap = new HashMap<>();

        mTilesEnd = 0;
        mTilesCount = 0;

        mHalfWidth = width / 2;
        mHalfHeight = height / 2;

   		/* set up TileSet large enough to hold current tiles */
        int numTiles = (width / BITMAP_TILE_SIZE + 1) * (height / BITMAP_TILE_SIZE + 1) * 4;

        mNewTiles = new TileSet(numTiles);

        mNewTiles.cnt = 0;

        // top-right
        mMapPlane[0] = mHalfWidth + BITMAP_TILE_SIZE;
        mMapPlane[1] = mHalfHeight + BITMAP_TILE_SIZE;
        // top-left
        mMapPlane[2] = -mMapPlane[0];
        mMapPlane[3] = mMapPlane[1];
        // bottom-left
        mMapPlane[4] = -mMapPlane[0];
        mMapPlane[5] = -mMapPlane[1];
        // bottom-right
        mMapPlane[6] = mMapPlane[0];
        mMapPlane[7] = -mMapPlane[1];

        mScanBox.scan(mPosition.x, mPosition.y, mPosition.scale, mPosition.zoomLevel, mMapPlane);

		/* Add tile jobs to queue */
        if (mJobs.isEmpty())
            return;

        MapTile[] jobs = new MapTile[mJobs.size()];
        jobs = mJobs.toArray(jobs);

        /* sets tiles to state == LOADING */
        mJobQueue.setJobs(jobs);
        mJobs.clear();

        mDrawThread = new DrawThread();
        mDrawThread.setRunning(true);
        mDrawThread.start();

        mTileLoader = new BitmapTileLoader();
        mTileLoader.start();
        mTileLoader.go();
    }

    /*
     * Listener tells us when the texture is being destroyed. Need to stop our mDrawThread
     * and make sure we do not touch the texture at all after this method
     * is called.
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed()");

        // Stop tile loader
        mTileLoader.pause();
        mTileLoader.finish();
        mTileLoader.dispose();

        // Clear the queue
        mJobQueue.clear();

        // Stop drawing
        mDrawThread.setRunning(false);
        synchronized (mDrawThread.mWaitLock) {
            mDrawThread.mWaitLock.notify();
        }

        boolean retry = true;
        while (retry) {
            try {
                mDrawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Clear all loaded tiles
        mNewTiles.releaseTiles();
        mNewTiles = null;

        // Free resources
        for (Bitmap bitmap : mTileMap.values()) {
            bitmap.recycle();
        }
        mTileMap.clear();
        mTileMap = null;

        if (!mActive)
            mTileSource.close();

        return true;
    }

    /*
     * Listener calls when the texture changes buffer size.
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, width + " " + height);
        //TODO Handle view resize
    }

    /*
     * Listener calls when the texture is updated by updateTexImage()
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Do nothing.

    }

    public void setTileSource(TileSource tileSource, boolean active) {
        mTileSource = tileSource;
        mActive = active;
    }

    public void setLocation(GeoPoint location) {
        assert (mTileSource != null);
        mPosition = new MapPosition(location.getLatitude(), location.getLongitude(), (1 << mTileSource.getZoomLevelMax()) + 5);
    }

    MapTile addTile(int x, int y, int zoomLevel) {
        MapTile tile = mIndex.getTile(x, y, zoomLevel);

        if (tile == null) {
            MapTile.TileNode n = mIndex.add(x, y, zoomLevel);
            tile = n.item = new MapTile(n, x, y, zoomLevel);
            tile.setState(LOADING);
            mJobs.add(tile);
            addToCache(tile);
        } else if (!tile.isActive()) {
            tile.setState(LOADING);
            mJobs.add(tile);
        }

        return tile;
    }

    public void jobCompleted(MapTile tile, boolean success) {
        if (success && tile.state(LOADING)) {
            tile.setState(NEW_DATA);
        }
        /* got orphaned tile */
        if (tile.state(DEADBEEF)) {
            //tile.clear();
            return;
        }

        //tile.clear();

		/* locked means the tile is visible or referenced by
         * a tile that might be visible. */
        //if (tile.isLocked()) {
        synchronized (mDrawThread.mWaitLock) {
            mDrawThread.mWaitLock.notify();
        }
        //}
    }

    private void addToCache(MapTile tile) {

        if (mTilesEnd == mTiles.length) {
            if (mTilesEnd > mTilesCount) {
                TileDistanceSort.sort(mTiles, 0, mTilesEnd);
                /* sorting also repacks the 'sparse' filled array
                 * so end of mTiles is at mTilesCount now */
                mTilesEnd = mTilesCount;
            }

            if (mTilesEnd == mTiles.length) {
                Log.d("TileManager", "realloc tiles " + mTilesEnd);
                MapTile[] tmp = new MapTile[mTiles.length + 20];
                System.arraycopy(mTiles, 0, tmp, 0, mTilesCount);
                mTiles = tmp;
            }
        }

        mTiles[mTilesEnd++] = tile;
        mTilesCount++;
    }

    public void setShouldNotCloseDataSource() {
        mActive = true;
    }

    class DrawThread extends Thread {

        private boolean mRun = false;

        private final Object mRunLock = new Object();
        private final Object mWaitLock = new Object();

        public DrawThread() {
        }

        private void doDraw(Canvas canvas) {
            double tileScale = BITMAP_TILE_SIZE * mPosition.scale;
            //clear the canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (MapTile tile : mNewTiles.tiles) {
                if (tile == null || tile.getState() != NEW_DATA)
                    continue;
                float x = (float) ((tile.x - mPosition.x) * tileScale) + mHalfWidth;
                float y = (float) ((tile.y - mPosition.y) * tileScale) + mHalfHeight;
                canvas.drawBitmap(mTileMap.get(tile), x, y, null);
            }
        }

        public void run() {
            while (mRun) {
                //draw to our canvas
                Canvas c = null;
                try {
                    c = lockCanvas(null);
                    if (c != null) {
                        doDraw(c);
                    }
                } finally {
                    if (c != null) {
                        unlockCanvasAndPost(c);
                    }
                }
                try {
                    synchronized (mWaitLock) {
                        mWaitLock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRunning(boolean b) {
            synchronized (mRunLock) {
                mRun = b;
            }
        }
    }

    class BitmapTileLoader extends PausableThread implements ITileDataSink {
        private final String THREAD_NAME;

        /**
         * currently processed tile
         */
        protected MapTile mTile;

        public BitmapTileLoader() {
            super();
            THREAD_NAME = "BitmapTileLoader";
        }

        protected boolean loadTile(MapTile tile) {
            try {
                mTileSource.getDataSource().query(tile, this);
            } catch (Exception e) {
                Log.e("BitmapTileLoader", tile + " " + e.getMessage());
                return false;
            }
            return true;
        }

        public void go() {
            synchronized (this) {
                notify();
            }
        }

        @Override
        protected void doWork() {
            mTile = mJobQueue.poll();

            if (mTile == null)
                return;

            try {
                loadTile(mTile);
            } catch (Exception e) {
                e.printStackTrace();
                completed(FAILED);
            }
        }

        @Override
        protected String getThreadName() {
            return THREAD_NAME;
        }

        @Override
        protected int getThreadPriority() {
            return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2;
        }

        @Override
        protected boolean hasWork() {
            return !mJobQueue.isEmpty();
        }

        public void dispose() {
            mTileSource.getDataSource().cancel();
        }

        public void cancel() {
            mTileSource.getDataSource().cancel();
        }

        /**
         * Callback to be called by TileDataSource when finished
         * loading or on failure. MUST BE CALLED IN ANY CASE!
         */
        @Override
        public void completed(QueryResult result) {
            boolean ok = (result == SUCCESS);

            if (ok && (isCanceled() || isInterrupted()))
                ok = false;

            jobCompleted(mTile, ok);
            mTile = null;
        }

        /**
         * Called by TileDataSource
         */
        @Override
        public void process(MapElement element) {
        }

        /**
         * Called by TileDataSource
         */
        @Override
        public void setTileImage(org.oscim.backend.canvas.Bitmap bitmap) {
            if (isCanceled() || !mTile.state(LOADING)) {
                bitmap.recycle();
                return;
            }
            Bitmap bmp = Bitmap.createBitmap(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mTileMap.put(mTile, bmp);
        }
    }

    private final TileIndex<MapTile.TileNode, MapTile> mIndex =
            new TileIndex<MapTile.TileNode, MapTile>() {
                @Override
                public void removeItem(MapTile t) {
                    if (t.node == null)
                        throw new IllegalStateException("Already removed: " + t);

                    super.remove(t.node);
                    t.node.item = null;
                }

                @Override
                public MapTile.TileNode create() {
                    return new MapTile.TileNode();
                }
            };

    private final ScanBox mScanBox = new ScanBox() {

        @Override
        protected void setVisible(int y, int x1, int x2) {
            MapTile[] tiles = mNewTiles.tiles;
            int cnt = mNewTiles.cnt;
            int maxTiles = tiles.length;

            int xmax = 1 << mZoom;

            for (int x = x1; x < x2; x++) {
                MapTile tile = null;

                if (cnt == maxTiles) {
                    Log.w(TAG, "too many tiles " + maxTiles);
                    break;
                }
                int xx = x;

                if (x < 0 || x >= xmax) {
                    /* flip-around date line */
                    if (x < 0)
                        xx = xmax + x;
                    else
                        xx = x - xmax;

                    if (xx < 0 || xx >= xmax)
                        continue;
                }

				/* check if tile is already added */
                for (int i = 0; i < cnt; i++)
                    if (tiles[i].tileX == xx && tiles[i].tileY == y) {
                        tile = tiles[i];
                        break;
                    }

                if (tile == null) {
                    tile = addTile(xx, y, mZoom);
                    tiles[cnt++] = tile;
                }
            }
            mNewTiles.cnt = cnt;
        }
    };

}