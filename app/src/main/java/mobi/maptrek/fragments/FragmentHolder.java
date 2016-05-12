package mobi.maptrek.fragments;

import android.support.design.widget.FloatingActionButton;

public interface FragmentHolder {
    FloatingActionButton enableActionButton();
    void disableActionButton();
    void addBackClickListener(OnBackPressedListener listener);
    void removeBackClickListener(OnBackPressedListener listener);
    void popCurrent();
    void popAll();
}
