package mobi.maptrek.fragments;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

public interface FragmentHolder {
    FloatingActionButton enableActionButton();
    void disableActionButton();
    void addBackClickListener(OnBackPressedListener listener);
    void removeBackClickListener(OnBackPressedListener listener);
    void popCurrent();
    void popAll();
    CoordinatorLayout getCoordinatorLayout();
}
