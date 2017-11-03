package mobi.maptrek.fragments;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

public interface FragmentHolder {
    FloatingActionButton enableActionButton();

    void disableActionButton();

    FloatingActionButton enableListActionButton();

    void disableListActionButton();

    void addBackClickListener(OnBackPressedListener listener);

    void removeBackClickListener(OnBackPressedListener listener);

    void popCurrent();

    void popAll();

    CoordinatorLayout getCoordinatorLayout();

    String getStatsString();
}
