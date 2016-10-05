package mobi.maptrek;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class MapTrekApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekApplication.class);
    public static final String EXCEPTION_PATH = "exception.log";

    private static MapTrekApplication mSelf;
    private File mExceptionLog;

    public static float density = 1f;
    public static float ydpi = 160f;

    @Override
    public void onCreate() {
        super.onCreate();
        mSelf = this;
        mExceptionLog = new File(getExternalFilesDir(null), EXCEPTION_PATH);
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
        Configuration.initialize(PreferenceManager.getDefaultSharedPreferences(this));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        density = metrics.density;
        ydpi = metrics.ydpi;
    }

    public static MapTrekApplication getApplication() {
        return mSelf;
    }

    public boolean hasPreviousRunsExceptions() {
        long size = Configuration.getExceptionSize();
        File logFile = new File(getExternalFilesDir(null), EXCEPTION_PATH);
        if (logFile.exists() && logFile.length() > 0L) {
            if (size != logFile.length()) {
                Configuration.setExceptionSize(logFile.length());
                return true;
            }
        } else {
            if (size > 0L) {
                Configuration.setExceptionSize(0L);
            }
        }
        return false;
    }

    public File getExceptionLog() {
        return mExceptionLog;
    }

    private class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
        private Thread.UncaughtExceptionHandler defaultHandler;

        DefaultExceptionHandler() {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(final Thread thread, final Throwable ex) {
            try {
                StringBuilder msg = new StringBuilder();
                msg.append(DateFormat.format("dd.MM.yyyy hh:mm:ss", System.currentTimeMillis()));
                try {
                    PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (info != null) {
                        msg.append("\nVersion : ")
                                .append(info.versionCode).append(" ").append(info.versionName);
                    }
                } catch (Throwable ignore) {
                }
                msg.append("\n")
                        .append("Thread : ")
                        .append(thread.toString())
                        .append("\nException :\n\n");

                if (mExceptionLog.getParentFile().canWrite()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(mExceptionLog, false));
                    writer.write(msg.toString());
                    ex.printStackTrace(new PrintWriter(writer));
                    writer.write("\n\n");
                    writer.close();
                }
                defaultHandler.uncaughtException(thread, ex);
            } catch (Exception e) {
                // swallow all exceptions
                logger.error("Exception while handle other exception", e);
            }
        }
    }

}
