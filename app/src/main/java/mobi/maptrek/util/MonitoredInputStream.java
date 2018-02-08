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

package mobi.maptrek.util;

import android.support.annotation.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * A class that monitors the read progress of an input stream.
 *
 * @author Hermia Yeung "Sheepy"
 * @since 2012-04-05 18:42
 */
public class MonitoredInputStream extends FilterInputStream {
    private volatile long mMark = 0;
    private volatile long mLastTriggeredLocation = 0;
    private volatile long mLocation = 0;
    private final int mThreshold;
    private final List<ChangeListener> listeners = new ArrayList<>(1);


    /**
     * Creates a MonitoredInputStream over an underlying input stream.
     *
     * @param in        Underlying input stream, should be non-null because of no public setter
     * @param threshold Min. position change (in byte) to trigger change event.
     */
    public MonitoredInputStream(InputStream in, int threshold) {
        super(in);
        this.mThreshold = threshold;
    }

    /**
     * Creates a MonitoredInputStream over an underlying input stream.
     * Default threshold is 16KB, small threshold may impact performance on larger streams.
     *
     * @param in Underlying input stream, should be non-null because of no public setter
     */
    public MonitoredInputStream(InputStream in) {
        super(in);
        this.mThreshold = 1024 * 16;
    }

    public void addChangeListener(ChangeListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    public long getProgress() {
        return mLocation;
    }

    protected void triggerChanged(final long location) {
        if (mThreshold > 0 && Math.abs(location - mLastTriggeredLocation) < mThreshold) return;
        mLastTriggeredLocation = location;
        if (listeners.size() <= 0) return;
        try {
            for (ChangeListener l : listeners) l.stateChanged(location);
        } catch (ConcurrentModificationException e) {
            triggerChanged(location);  // List changed? Let's re-try.
        }
    }


    @Override
    public int read() throws IOException {
        final int i = super.read();
        if (i != -1) triggerChanged(mLocation++);
        return i;
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        final int i = super.read(b, off, len);
        if (i > 0) triggerChanged(mLocation += i);
        return i;
    }

    @Override
    public long skip(long n) throws IOException {
        final long i = super.skip(n);
        if (i > 0) triggerChanged(mLocation += i);
        return i;
    }

    @Override
    public void mark(int readlimit) {
        super.mark(readlimit);
        mMark = mLocation;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        if (mLocation != mMark) triggerChanged(mLocation = mMark);
    }

    public interface ChangeListener {
        void stateChanged(long location);
    }
}