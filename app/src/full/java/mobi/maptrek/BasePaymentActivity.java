package mobi.maptrek;

import android.app.Activity;

import mobi.maptrek.fragments.OnMapActionListener;

public abstract class BasePaymentActivity extends Activity implements OnMapActionListener {
    public static final int MAPS_LIMIT = Integer.MAX_VALUE;

    @Override
    public void onPurchaseMaps() {
    }

    protected abstract void onMapLimitChanged(int limit);
}
