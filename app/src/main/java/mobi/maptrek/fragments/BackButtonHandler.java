package mobi.maptrek.fragments;

public interface BackButtonHandler {
    void addBackClickListener(OnBackPressedListener listener);
    void removeBackClickListener(OnBackPressedListener listener);
}
