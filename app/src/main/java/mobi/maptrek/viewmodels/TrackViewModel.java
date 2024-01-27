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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mobi.maptrek.data.Track;
import mobi.maptrek.location.BaseLocationService.TRACKING_STATE;

public class TrackViewModel extends ViewModel {
    public final MutableLiveData<Track> currentTrack = new MutableLiveData<>();
    public final MutableLiveData<TRACKING_STATE> trackingState = new MutableLiveData<>(TRACKING_STATE.DISABLED);
    public final MutableLiveData<TRACKING_STATE> trackingCommand = new MutableLiveData<>(TRACKING_STATE.DISABLED);
}
