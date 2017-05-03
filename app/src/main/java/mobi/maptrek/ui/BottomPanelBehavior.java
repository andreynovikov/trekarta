package mobi.maptrek.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;

import mobi.maptrek.R;

// It is referenced in layout file
@SuppressWarnings("unused")
public class BottomPanelBehavior extends CoordinatorLayout.Behavior<RelativeLayout> {
    private final boolean mIsLandscape;

    public BottomPanelBehavior(Context context, AttributeSet attrs) {
        mIsLandscape = context.getResources().getBoolean(R.bool.isLandscape);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, final RelativeLayout child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float width = 0, shift = 0;
            final List<View> dependencies = parent.getDependencies(child);
            for (int i = 0, z = dependencies.size(); i < z; i++) {
                final View view = dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(child, view)) {
                    shift = Math.min(shift, view.getTranslationY() - view.getHeight());
                    width = parent.getWidth() - view.getWidth();
                }
            }
            if (mIsLandscape) {
                if (width < 10f) {
                    child.setPaddingRelative(0, 0, 0, (int) -shift);
                    return true;
                }
            } else {
                child.setTranslationY(shift);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            child.setPaddingRelative(0, 0, 0, 0);
            child.setTranslationY(0);
        }
    }
}
