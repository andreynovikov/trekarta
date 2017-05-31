package mobi.maptrek.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import at.grabner.circleprogress.CircleProgressView;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.provider.ExportProvider;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.ProgressListener;

public class DataExport extends DialogFragment implements ProgressListener {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORMAT_NATIVE, FORMAT_GPX, FORMAT_KML})
    public @interface ExportFormat {
    }

    /**
     * Native format is used only for single track sharing
     */
    public static final int FORMAT_NATIVE = 0;
    public static final int FORMAT_GPX = 1;
    public static final int FORMAT_KML = 2;

    private CircleProgressView mProgressView;

    private DataSource mDataSource;
    private Track mTrack;
    @ExportFormat
    private int mFormat;
    private boolean mCanceled;

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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCanceled = false;
        Activity activity = getActivity();

        @SuppressLint("InflateParams")
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_progress, null);
        mProgressView = (CircleProgressView) dialogView.findViewById(R.id.progress);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.title_export_track);
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //mCallback.onTextInputNegativeClick(id);
            }
        });
        dialogBuilder.setView(dialogView);
        return dialogBuilder.create();
    }

    @Override
    public void onProgressStarted(int length) {
        mProgressView.setMaxValue(length);
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
    public void onProgressAnnotated(String annotation) {

    }

    private class ExportRunnable implements Runnable {
        ExportRunnable() {
        }

        @Override
        public void run() {
            Activity activity = getActivity();
            File exportFile;
            String mime = null;
            boolean nativeFile = mFormat == FORMAT_NATIVE;
            if (mTrack != null) {
                nativeFile = nativeFile && mTrack.source != null && mTrack.source.isNativeTrack();
            } else {
                nativeFile = nativeFile && mDataSource.isNativeTrack();
            }
            if (nativeFile) {
                if (mTrack != null) {
                    exportFile = new File(((FileDataSource) mTrack.source).path);
                } else {
                    exportFile = new File(((FileDataSource) mDataSource).path);
                }
                mime = "application/octet-stream";
                onProgressStarted(100);
                onProgressChanged(100);
                onProgressFinished();
            } else {
                FileDataSource exportSource = new FileDataSource();
                if (mTrack != null) {
                    exportSource.tracks.add(mTrack);
                } else {
                    if (mDataSource instanceof WaypointDataSource)
                        exportSource.waypoints.addAll(((WaypointDataSource) mDataSource).getWaypoints());
                    if (mDataSource instanceof TrackDataSource)
                        exportSource.tracks.addAll(((TrackDataSource) mDataSource).getTracks());
                }
                File cacheDir = activity.getExternalCacheDir();
                File exportDir = new File(cacheDir, "export");
                if (!exportDir.exists())
                    exportDir.mkdir();
                String extension = null;
                switch (mFormat) {
                    case FORMAT_NATIVE:
                        extension = TrackManager.EXTENSION;
                        mime = "application/octet-stream";
                        break;
                    case FORMAT_GPX:
                        extension = GPXManager.EXTENSION;
                        mime = "text/xml";
                        break;
                    case FORMAT_KML:
                        extension = KMLManager.EXTENSION;
                        mime = "application/vnd.google-earth.kml+xml";
                        break;
                }
                //TODO Notify user on this and other errors
                if (extension == null) {
                    dismiss();
                    return;
                }
                String name;
                if (mTrack != null) {
                    name = mTrack.name;
                } else {
                    name = mDataSource.name;
                }
                exportFile = new File(exportDir, FileUtils.sanitizeFilename(name) + extension);
                if (exportFile.exists())
                    exportFile.delete();
                exportSource.name = name;
                exportSource.path = exportFile.getAbsolutePath();
                Manager manager = Manager.getDataManager(getContext(), exportSource.path);
                if (manager == null) {
                    dismiss();
                    return;
                }
                try {
                    manager.saveData(new FileOutputStream(exportFile, false), exportSource, DataExport.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    dismiss();
                    return;
                }
            }
            if (mCanceled) {
                if (!nativeFile && exportFile.exists())
                    //noinspection ResultOfMethodCallIgnored
                    exportFile.delete();
                return;
            }
            Uri contentUri = ExportProvider.getUriForFile(activity, exportFile);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType(mime);
            startActivity(Intent.createChooser(shareIntent,
                    getString(mTrack != null ? R.string.share_track_intent_title : R.string.share_data_intent_title)));
            dismiss();
        }
    }

    public static class Builder {
        private DataSource mDataSource;
        private Track mTrack;
        @ExportFormat
        private int mFormat;

        public Builder setDataSource(@NonNull DataSource dataSource) {
            mDataSource = dataSource;
            return this;
        }

        public Builder setTrack(@NonNull Track track) {
            mTrack = track;
            return this;
        }

        public Builder setFormat(@ExportFormat int format) {
            mFormat = format;
            return this;
        }

        public DataExport create() {
            DataExport dialogFragment = new DataExport();
            if (mTrack != null) {
                dialogFragment.mTrack = mTrack;
            } else {
                dialogFragment.mDataSource = mDataSource;
            }
            dialogFragment.mFormat = mFormat;
            return dialogFragment;
        }
    }
}
