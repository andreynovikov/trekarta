package mobi.maptrek.view;

import android.support.annotation.IdRes;
import android.view.MenuItem;

public interface PanelMenu {
    MenuItem findItem(@IdRes int id);

    MenuItem add(@IdRes int id, int order, CharSequence title);

    void removeItem(@IdRes int id);

    interface OnPrepareMenuListener {
        void onPrepareMenu(PanelMenu menu);
    }
}
