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

package mobi.maptrek.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

import at.grabner.circleprogress.CircleProgressView;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.RouteDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.PlaceDataSource;
import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.provider.ExportProvider;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.ProgressListener;

public class DataExport extends DialogFragment implements ProgressListener {
    private static final Logger logger = LoggerFactory.getLogger(DataExport.class);

    private CircleProgressView mProgressView;

    private DataSource mDataSource;
    private Track mTrack;
    private Route mRoute;
    @FileDataSource.Format
    private int mFormat;
    private boolean mCanceled;
    private File exportFile;

    @Override
    public void dismiss() {
        super.dismiss();
        mCanceled = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        new Thread(new ExportRunnable()).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCanceled = false;
        Activity activity = requireActivity();

        boolean nativeFile = mFormat == FileDataSource.FORMAT_NATIVE;
        boolean sameFormat = false;
        if (mTrack != null) {
            nativeFile = nativeFile && mTrack.source != null && mTrack.source.isNativeTrack();
        } else if (mRoute != null) {
            nativeFile = false;
        } else {
            nativeFile = nativeFile && mDataSource.isNativeTrack();
            sameFormat = mDataSource.getFormat() == mFormat;
        }
        if (nativeFile || sameFormat) { // no need to export data, send file directly
            if (mTrack != null) {
                exportFile = new File(((FileDataSource) mTrack.source).path);
            } else {
                exportFile = new File(((FileDataSource) mDataSource).path);
            }
            setShowsDialog(false);
            sendContent();
            return new Dialog(activity); // empty dialog is required to prevent NPE
        }

        @SuppressLint("InflateParams")
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_progress, null);
        mProgressView = dialogView.findViewById(R.id.progress);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.title_export_track);
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            //mCallback.onTextInputNegativeClick(id);
        });
        dialogBuilder.setView(dialogView);
        return dialogBuilder.create();
    }

    private void sendContent() {
        String mime;
        switch (mFormat) {
            case FileDataSource.FORMAT_GPX:
                mime = "text/xml";
                break;
            case FileDataSource.FORMAT_KML:
                mime = "application/vnd.google-earth.kml+xml";
                break;
            case FileDataSource.FORMAT_NATIVE:
            default:
                mime = "application/octet-stream";
        }
        @StringRes int titleId =
                mTrack != null ? R.string.share_track_intent_title :
                mRoute != null ? R.string.share_route_intent_title : R.string.share_data_intent_title;
        Uri contentUri = ExportProvider.getUriForFile(requireContext(), exportFile);
        logger.info("Sharing {} as {}", exportFile.getAbsolutePath(), contentUri);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType(mime);
        startActivity(Intent.createChooser(shareIntent, getString(titleId)));
        dismiss();
    }

    @Override
    public void onProgressStarted(int length) {
        mProgressView.setMaxValue(length);
        mProgressView.setValue(0f);
        mProgressView.setSeekModeEnabled(false);
    }

    @Override
    public void onProgressChanged(int progress) {
        mProgressView.setValue(progress);
    }

    @Override
    public void onProgressFinished() {

    }

    @Override
    public void onProgressFinished(Bundle data) {

    }

    @Override
    public void onProgressAnnotated(String annotation) {

    }

    private class ExportRunnable implements Runnable {
        ExportRunnable() {
        }

        @Override
        public void run() {
            Activity activity = requireActivity();
            FileDataSource exportSource = new FileDataSource();
            if (mTrack != null) {
                exportSource.tracks.add(mTrack);
            } else if (mRoute != null) {
                exportSource.routes.add(mRoute);
            } else {
                if (mDataSource instanceof PlaceDataSource)
                    exportSource.places.addAll(((PlaceDataSource) mDataSource).getPlaces());
                if (mDataSource instanceof TrackDataSource)
                    exportSource.tracks.addAll(((TrackDataSource) mDataSource).getTracks());
                if (mDataSource instanceof RouteDataSource)
                    exportSource.routes.addAll(((RouteDataSource) mDataSource).getRoutes());
            }
            File cacheDir = activity.getExternalCacheDir();
            File exportDir = new File(cacheDir, "export");
            if (!exportDir.exists() && !exportDir.mkdir()) {
                logger.error("Failed to create export dir: {}", exportDir);
                dismiss();
                return;
            }
            String extension = null;
            switch (mFormat) {
                case FileDataSource.FORMAT_NATIVE:
                    extension = TrackManager.EXTENSION;
                    break;
                case FileDataSource.FORMAT_GPX:
                    extension = GPXManager.EXTENSION;
                    break;
                case FileDataSource.FORMAT_KML:
                    extension = KMLManager.EXTENSION;
                    break;
            }
            //TODO Notify user on this and other errors
            if (extension == null) {
                logger.error("Failed to determine extension for format: {}", mFormat);
                dismiss();
                return;
            }
            String name;
            if (mTrack != null) {
                name = mTrack.name;
            } else if (mRoute != null) {
                name = mRoute.name;
            } else {
                name = mDataSource.name;
            }
            exportFile = new File(exportDir, FileUtils.sanitizeFilename(name) + extension);
            if (exportFile.exists() && !exportFile.delete())
                logger.error("Failed to remove old file");
            exportSource.name = name;
            exportSource.path = exportFile.getAbsolutePath();
            Manager manager = Manager.getDataManager(exportSource.path);
            if (manager == null) {
                logger.error("Failed to get data manager for path: {}", exportSource.path);
                dismiss();
                return;
            }
            try {
                manager.saveData(new FileOutputStream(exportFile, false), exportSource, DataExport.this);
            } catch (Exception e) {
                logger.error("Data save error", e);
                dismiss();
                return;
            }
            if (mCanceled) {
                logger.info("User canceled export");
                if (exportFile.exists())
                    //noinspection ResultOfMethodCallIgnored
                    exportFile.delete();
            }
            sendContent();
        }
    }

    public static class Builder {
        private DataSource mDataSource;
        private Track mTrack;
        private Route mRoute;
        @FileDataSource.Format
        private int mFormat;

        public Builder setDataSource(@NonNull DataSource dataSource) {
            mDataSource = dataSource;
            return this;
        }

        public Builder setTrack(@NonNull Track track) {
            mTrack = track;
            return this;
        }

        public Builder setRoute(@NonNull Route route) {
            mRoute = route;
            return this;
        }

        public Builder setFormat(@FileDataSource.Format int format) {
            mFormat = format;
            return this;
        }

        public DataExport create() {
            DataExport dialogFragment = new DataExport();
            if (mTrack != null) {
                dialogFragment.mTrack = mTrack;
            } else if (mRoute != null) {
                dialogFragment.mRoute = mRoute;
            } else {
                dialogFragment.mDataSource = mDataSource;
            }
            dialogFragment.mFormat = mFormat;
            return dialogFragment;
        }
    }
}
