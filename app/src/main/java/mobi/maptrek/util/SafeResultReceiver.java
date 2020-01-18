/*
 * Copyright 2019 Andrey Novikov
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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import java.lang.ref.WeakReference;

@SuppressLint("RestrictedApi")
public class SafeResultReceiver extends ResultReceiver {

    private WeakReference<Callback> mCallback;

    @SuppressLint("RestrictedApi")
    public SafeResultReceiver() {
        super(null);
    }

    public interface Callback {
        void onReceiveResult(int resultCode, Bundle resultData);

    }

    public void setCallback(Callback callback) {
        mCallback = new WeakReference<>(callback);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mCallback != null) {
            mCallback.get().onReceiveResult(resultCode, resultData);
        }
    }
}