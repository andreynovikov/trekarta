/*
 * Copyright 2024 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mobi.maptrek.MapTrek;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;

public class DataSourceViewModel extends ViewModel {
    public final WaypointDbDataSource waypointDbDataSource = MapTrek.getApplication().getWaypointDbDataSource();
    public List<FileDataSource> fileDataSources = new ArrayList<>();
    private final MutableLiveData<List<DataSource>> currentDataSources = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<DataSource>> getDataSourcesState() {
        return currentDataSources;
    }

    private final MutableLiveData<Boolean> nativeTracksState = new MutableLiveData<>(false);

    public LiveData<Boolean> getNativeTracksState() {
        return nativeTracksState;
    }

    public void showNativeTracks(boolean show) {
        nativeTracksState.setValue(show);
        updateCurrentDataSources();
    }

    public void updateCurrentDataSources() {
        boolean nativeTracks = Boolean.TRUE.equals(nativeTracksState.getValue());
        List<DataSource> dataSources = new ArrayList<>();
        if (!nativeTracks)
            dataSources.add(waypointDbDataSource);

        // TODO Preserve position after source is loaded and name changes
        Collections.sort(fileDataSources, (lhs, rhs) -> {
            if (nativeTracks) {
                // Newer tracks first
                File lf = new File(lhs.path);
                File rf = new File(rhs.path);
                return Long.compare(rf.lastModified(), lf.lastModified());
            }
            return lhs.name.compareTo(rhs.name);
        });
        for (FileDataSource source : fileDataSources) {
            if (nativeTracks ^ !source.isNativeTrack()) {
                dataSources.add(source);
            }
        }
        currentDataSources.setValue(dataSources);
    }

    public boolean hasExtraDataSources() {
        boolean hasExtraSources = false;
        for (FileDataSource source : fileDataSources) {
            if (!source.isNativeTrack()) {
                hasExtraSources = true;
                break;
            }
        }
        return hasExtraSources;
    }
}
