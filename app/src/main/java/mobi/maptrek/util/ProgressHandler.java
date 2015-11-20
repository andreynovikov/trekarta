package mobi.maptrek.util;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

import mobi.maptrek.io.Manager;

/**
 * Handler for interaction with ProgressBar. Manages visibility state and progress of the
 * ProgressBar.
 */
public class ProgressHandler extends Handler implements Manager.ProgressListener {
    public final static int BEGIN_PROGRESS = 1;
    public final static int UPDATE_PROGRESS = 2;
    public final static int STOP_PROGRESS = 3;

    ProgressBar mProgressBar;

    public ProgressHandler(ProgressBar progressBar) {
        super();
        mProgressBar = progressBar;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BEGIN_PROGRESS:
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setMax(msg.arg1);
                break;
            case UPDATE_PROGRESS:
                mProgressBar.setProgress(msg.arg1);
                break;
            case STOP_PROGRESS:
                mProgressBar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onProgressStarted(int length) {
        Message msg = obtainMessage(BEGIN_PROGRESS, length, 0);
        sendMessage(msg);
    }

    @Override
    public void onProgressChanged(int progress) {
        Message msg = obtainMessage(UPDATE_PROGRESS, progress, 0);
        sendMessage(msg);
    }

    @Override
    public void onProgressFinished() {
        Message msg = obtainMessage(STOP_PROGRESS);
        sendMessage(msg);
    }
}
