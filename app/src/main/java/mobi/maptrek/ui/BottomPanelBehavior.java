package mobi.maptrek.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;

// It is referenced in layout file
@SuppressWarnings("unused")
public class BottomPanelBehavior extends CoordinatorLayout.Behavior<RelativeLayout> {
    public BottomPanelBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, final RelativeLayout child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float translationY = 0;
            final List<View> dependencies = parent.getDependencies(child);
            for (int i = 0, z = dependencies.size(); i < z; i++) {
                final View view = dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(child, view)) {
                    translationY = Math.min(translationY, view.getTranslationY() - view.getHeight());
                }
            }
            child.setTranslationY(translationY);
        }
        return false;
    }
}
