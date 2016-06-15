package mobi.maptrek;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;

public interface DataHolder {
    @NonNull
    WaypointDbDataSource getWaypointDataSource();

    @NonNull
    List<FileDataSource> getData();

    void setDataSourceAvailability(FileDataSource source, boolean available);

    void onDataSourceSelected(@NonNull DataSource source);

    void onDataSourceDelete(@NonNull DataSource source);
}
