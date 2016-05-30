package mobi.maptrek.util;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Handler for interaction with ProgressBar. Manages visibility state and progress of the
 * ProgressBar.
 */
public class ProgressHandler extends Handler implements ProgressListener {
    public final static int BEGIN_PROGRESS = 1;
    public final static int UPDATE_PROGRESS = 2;
    public final static int STOP_PROGRESS = 3;

    ProgressBar mProgressBar;

    public ProgressHandler(ProgressBar progressBar) {
        super();
        mProgressBar = progressBar;
    }

    // TODO Handle simultaneous operations
    @CallSuper
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
    public final void onProgressStarted(int length) {
        Message msg = obtainMessage(BEGIN_PROGRESS, length, 0);
        sendMessage(msg);
    }

    @Override
    public final void onProgressChanged(int progress) {
        Message msg = obtainMessage(UPDATE_PROGRESS, progress, 0);
        sendMessage(msg);
    }

    @Override
    public final void onProgressFinished() {
        Message msg = obtainMessage(STOP_PROGRESS);
        sendMessage(msg);
    }

    /**
     * Called when progress step is annotated. Does nothing. If overridden care should be taken, as
     * it is called not on UI thread.
     *
     * @param annotation Annotation of a step.
     */
    @Override
    public void onProgressAnnotated(String annotation) {
    }
}
