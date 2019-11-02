/*
 * Copyright 2018 Andrey Novikov
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

package mobi.maptrek.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.MapTrek;
import mobi.maptrek.data.source.WaypointDbDataSource;

public class ExportProvider extends ContentProvider {
    private static final Logger logger = LoggerFactory.getLogger(ExportProvider.class);

    private static final String COLUMN_LAST_MODIFIED = "_last_modified";
    private static final String AUTHORITY = BuildConfig.EXPORT_PROVIDER_AUTHORITY;
    private static final String[] COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, COLUMN_LAST_MODIFIED};

    private PathStrategy mStrategy;
    private static PathStrategy mCachedStrategy;
    private Handler mHandler;

    /**
     * The default FileProvider implementation does not need to be initialized. If you want to
     * override this method, you must provide your own subclass of FileProvider.
     */
    @Override
    public boolean onCreate() {
        mHandler = new Handler(Looper.getMainLooper());
        return true;
    }

    /**
     * After the FileProvider is instantiated, this method is called to provide the system with
     * information about the provider.
     *
     * @param context A {@link Context} for the current component.
     * @param info    A {@link ProviderInfo} for the new provider.
     */
    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        // Sanity check our security
        /*
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        }
        */
        if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grant uri permissions");
        }

        mStrategy = getPathStrategy(context);
    }

    /**
     * Return a content URI for a given {@link File}. Specific temporary
     * permissions for the content URI can be set with
     * {@link Context#grantUriPermission(String, Uri, int)}, or added
     * to an {@link Intent} by calling {@link Intent#setData(Uri) setData()} and then
     * {@link Intent#setFlags(int) setFlags()}; in both cases, the applicable flags are
     * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}. A FileProvider can only return a
     * <code>content</code> {@link Uri} for file paths defined in their <code>&lt;paths&gt;</code>
     * meta-data element. See the Class Overview for more information.
     *
     * @param context A {@link Context} for the current component.
     * @param file    A {@link File} pointing to the filename for which you want a
     *                <code>content</code> {@link Uri}.
     * @return A content URI for the file.
     * @throws IllegalArgumentException When the given {@link File} is outside
     *                                  the paths supported by the provider.
     */
    public static Uri getUriForFile(Context context, File file) {
        return getPathStrategy(context).getUriForFile(file);
    }

    /**
     * Use a content URI returned by
     * {@link #getUriForFile(Context, File) getUriForFile()} to get information about a file
     * managed by the FileProvider.
     * FileProvider reports the column names defined in {@link android.provider.OpenableColumns}:
     * <ul>
     * <li>{@link android.provider.OpenableColumns#DISPLAY_NAME}</li>
     * <li>{@link android.provider.OpenableColumns#SIZE}</li>
     * </ul>
     * For more information, see
     * {@link ContentProvider#query(Uri, String[], String, String[], String)
     * ContentProvider.query()}.
     *
     * @param uri           A content URI returned by {@link #getUriForFile}.
     * @param projection    The list of columns to put into the {@link Cursor}. If null all columns are
     *                      included.
     * @param selection     Selection criteria to apply. If null then all data that matches the content
     *                      URI is returned.
     * @param selectionArgs An array of {@link java.lang.String}, containing arguments to bind to
     *                      the <i>selection</i> parameter. The <i>query</i> method scans <i>selection</i> from left to
     *                      right and iterates through <i>selectionArgs</i>, replacing the current "?" character in
     *                      <i>selection</i> with the value at the current position in <i>selectionArgs</i>. The
     *                      values are bound to <i>selection</i> as {@link java.lang.String} values.
     * @param sortOrder     A {@link java.lang.String} containing the column name(s) on which to sort
     *                      the resulting {@link Cursor}.
     * @return A {@link Cursor} containing the results of the query.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);

        if (projection == null) {
            projection = COLUMNS;
        }

        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                cols[i] = OpenableColumns.DISPLAY_NAME;
                values[i++] = file.getName();
            } else if (OpenableColumns.SIZE.equals(col)) {
                cols[i] = OpenableColumns.SIZE;
                values[i++] = file.length();
            } else if (COLUMN_LAST_MODIFIED.equals(col)) {
                cols[i] = COLUMN_LAST_MODIFIED;
                values[i++] = file.lastModified();
            }
        }

        cols = copyOf(cols, i);
        values = copyOf(values, i);

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    /**
     * Returns the MIME type of a content URI returned by
     * {@link #getUriForFile(Context, File) getUriForFile()}.
     *
     * @param uri A content URI returned by
     *            {@link #getUriForFile(Context, File) getUriForFile()}.
     * @return If the associated file has an extension, the MIME type associated with that
     * extension; otherwise <code>application/octet-stream</code>.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);

        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return "application/octet-stream";
    }

    /**
     * By default, this method throws an {@link java.lang.UnsupportedOperationException}. You must
     * subclass FileProvider if you want to provide different functionality.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    /**
     * By default, this method throws an {@link java.lang.UnsupportedOperationException}. You must
     * subclass FileProvider if you want to provide different functionality.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    /**
     * Deletes the file associated with the specified content URI, as
     * returned by {@link #getUriForFile(Context, File) getUriForFile()}. Notice that this
     * method does <b>not</b> throw an {@link java.io.IOException}; you must check its return value.
     *
     * @param uri           A content URI for a file, as returned by
     *                      {@link #getUriForFile(Context, File) getUriForFile()}.
     * @param selection     Ignored. Set to {@code null}.
     * @param selectionArgs Ignored. Set to {@code null}.
     * @return 1 if the delete succeeds; otherwise, 0.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);
        return file.delete() ? 1 : 0;
    }

    /**
     * By default, FileProvider automatically returns the
     * {@link ParcelFileDescriptor} for a file associated with a <code>content://</code>
     * {@link Uri}. To get the {@link ParcelFileDescriptor}, call
     * {@link android.content.ContentResolver#openFileDescriptor(Uri, String)
     * ContentResolver.openFileDescriptor}.
     * <p/>
     * To override this method, you must provide your own subclass of FileProvider.
     *
     * @param uri  A content URI associated with a file, as returned by
     *             {@link #getUriForFile(Context, File) getUriForFile()}.
     * @param mode Access mode for the file. May be "r" for read-only access, "rw" for read and
     *             write access, or "rwt" for read and write access that truncates any existing file.
     * @return A new {@link ParcelFileDescriptor} with which you can access the file.
     */
    @Override
    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode) throws FileNotFoundException {
        // ContentProvider has already checked granted permissions
        File file = mStrategy.getFileForUri(uri);
        int fileMode = modeToMode(mode);
        if ("rwt".equals(mode) && uri.getLastPathSegment().endsWith(".sqlitedb"))
            file = new File(file.getAbsolutePath() + ".restore");
        logger.error("openFile: {} {} {}", uri, file, mode);
        try {
            return ParcelFileDescriptor.open(file, fileMode, mHandler, e -> {
                if (!(e instanceof ParcelFileDescriptor.FileDescriptorDetachedException)) {
                    if ("rwt".equals(mode)) {
                        logger.error("saved");
                        Intent intent = new Intent(WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED);
                        //noinspection ConstantConditions
                        getContext().sendOrderedBroadcast(intent, null);
                    }
                    if ("export".equals(uri.getPathSegments().get(0))) {
                        File file1 = mStrategy.getFileForUri(uri);
                        //noinspection ResultOfMethodCallIgnored
                        file1.delete();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return ParcelFileDescriptor.open(file, fileMode);
        }
    }

    /**
     * Return {@link PathStrategy} for given authority, either by parsing or
     * returning from cache.
     */
    private static PathStrategy getPathStrategy(Context context) {
        synchronized (ExportProvider.class) {
            if (mCachedStrategy == null) {
                SimplePathStrategy simplePathStrategy = new SimplePathStrategy();
                simplePathStrategy.addRoot("data", buildPath(MapTrek.getExternalDirForContext(context, "data")));
                simplePathStrategy.addRoot("databases", buildPath(MapTrek.getExternalDirForContext(context,"databases")));
                simplePathStrategy.addRoot("maps", buildPath(MapTrek.getExternalDirForContext(context,"maps")));
                simplePathStrategy.addRoot("export", buildPath(context.getExternalCacheDir(), "export"));
                mCachedStrategy = simplePathStrategy;
            }
        }
        return mCachedStrategy;
    }

    /**
     * Strategy for mapping between {@link File} and {@link Uri}.
     * <p/>
     * Strategies must be symmetric so that mapping a {@link File} to a
     * {@link Uri} and then back to a {@link File} points at the original
     * target.
     * <p/>
     * Strategies must remain consistent across app launches, and not rely on
     * dynamic state. This ensures that any generated {@link Uri} can still be
     * resolved if your process is killed and later restarted.
     *
     * @see SimplePathStrategy
     */
    interface PathStrategy {
        /**
         * Return a {@link Uri} that represents the given {@link File}.
         */
        Uri getUriForFile(File file);

        /**
         * Return a {@link File} that represents the given {@link Uri}.
         */
        File getFileForUri(Uri uri);
    }

    /**
     * Strategy that provides access to files living under a narrow whitelist of
     * filesystem roots. It will throw {@link SecurityException} if callers try
     * accessing files outside the configured roots.
     * <p/>
     * For example, if configured with
     * {@code addRoot("myfiles", context.getFilesDir())}, then
     * {@code context.getFileStreamPath("foo.txt")} would map to
     * {@code content://myauthority/myfiles/foo.txt}.
     */
    private static class SimplePathStrategy implements PathStrategy {
        private final HashMap<String, File> mRoots = new HashMap<>();

        SimplePathStrategy() {
        }

        /**
         * Add a mapping from a name to a filesystem root. The provider only offers
         * access to files that live under configured roots.
         */
        void addRoot(String name, File root) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name must not be empty");
            }

            try {
                // Resolve to canonical path to keep path checking fast
                root = root.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + root, e);
            }

            mRoots.put(name, root);
        }

        @Override
        public Uri getUriForFile(File file) {
            logger.info("getUriForFile({})", file.getAbsolutePath());
            String path;
            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }

            // Find the most-specific root path
            Map.Entry<String, File> mostSpecific = null;
            for (Map.Entry<String, File> root : mRoots.entrySet()) {
                final String rootPath = root.getValue().getPath();
                if (path.startsWith(rootPath) && (mostSpecific == null
                        || rootPath.length() > mostSpecific.getValue().getPath().length())) {
                    mostSpecific = root;
                }
            }

            if (mostSpecific == null) {
                throw new IllegalArgumentException("Failed to find configured root that contains " + path);
            }

            // Start at first char of path under root
            final String rootPath = mostSpecific.getValue().getPath();
            if (rootPath.endsWith("/")) {
                path = path.substring(rootPath.length());
            } else {
                path = path.substring(rootPath.length() + 1);
            }

            // Encode the tag and path separately
            path = Uri.encode(mostSpecific.getKey()) + '/' + Uri.encode(path, "/");
            return new Uri.Builder().scheme("content").authority(AUTHORITY).encodedPath(path).build();
        }

        @Override
        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();
            logger.info("getFileForUri({})", path);

            final int splitIndex = path.indexOf('/', 1);
            final String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));

            final File root = mRoots.get(tag);
            if (root == null) {
                throw new IllegalArgumentException("Unable to find configured root for " + uri);
            }

            File file = new File(root, path);
            try {
                file = file.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }

            if (!file.getPath().startsWith(root.getPath())) {
                throw new SecurityException("Resolved path jumped beyond configured root");
            }

            return file;
        }
    }

    /**
     * Copied from ContentResolver.java
     */
    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (segment != null) {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}
