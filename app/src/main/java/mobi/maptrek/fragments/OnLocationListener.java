package mobi.maptrek.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.core.GeoPoint;

public interface OnLocationListener {
    void showMarkerInformation(@NonNull GeoPoint point, @Nullable String name);
}
