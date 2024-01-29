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

package mobi.maptrek.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.databinding.FragmentTrackInformationBinding;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.ITrackingListener;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.TrackViewModel;

public class TrackInformation extends Fragment implements PopupMenu.OnMenuItemClickListener {
    int mSegmentCount = 0;
    float mPrevElevation = Float.NaN;
    float mElevationGain = 0;
    float mElevationLoss = 0;
    float mMinElevation = Float.MAX_VALUE;
    float mMaxElevation = Float.MIN_VALUE;
    float mMaxSpeed = 0;
    private MeanValue mSpeedMeanValue;

    private LineData mElevationData;
    private LineData mSpeedData;

    private FragmentHolder mFragmentHolder;
    private OnTrackActionListener mListener;
    private boolean mEditorMode;
    private boolean mBound;
    private ILocationService mTrackingService;
    private TrackViewModel trackViewModel;
    private FragmentTrackInformationBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentTrackInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.selectedTrack.observe(getViewLifecycleOwner(), selectedTrack -> {
            boolean isCurrent = selectedTrack == trackViewModel.currentTrack.getValue();
            initializeTrackInformation(selectedTrack);

            if (isCurrent && !mBound) {
                Context context = MapTrek.getApplication();
                mBound = context.bindService(new Intent(context, LocationService.class), mTrackingConnection, 0);
            } else if (mBound && !isCurrent) {
                if (mTrackingService != null) {
                    mTrackingService.unregisterTrackingCallback(mTrackingListener);
                }
                Context context = MapTrek.getApplication();
                context.unbindService(mTrackingConnection);
                mBound = false;
            }
        });

        viewBinding.moreButton.setOnClickListener(v -> {
            Track track = trackViewModel.selectedTrack.getValue();
            if (track == null)
                return;
            boolean isCurrent = track == trackViewModel.currentTrack.getValue();
            if (mEditorMode) {
                Editable text = viewBinding.nameEdit.getText();
                if (text != null)
                    track.name = text.toString();
                track.style.color = viewBinding.colorSwatch.getColor();
                mListener.onTrackSave(track);
                setEditorMode(false);
            } else {
                PopupMenu popup = new PopupMenu(getContext(), viewBinding.moreButton);
                viewBinding.moreButton.setOnTouchListener(popup.getDragToOpenListener());
                popup.inflate(R.menu.context_menu_track);
                Menu menu = popup.getMenu();
                menu.findItem(R.id.action_edit).setVisible(!isCurrent);
                menu.findItem(R.id.action_delete).setVisible(track.source != null && !track.source.isNativeTrack());
                popup.setOnMenuItemClickListener(TrackInformation.this);
                popup.show();
            }
        });

        mEditorMode = false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnTrackActionListener");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
        }
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            if (mTrackingService != null) {
                mTrackingService.unregisterTrackingCallback(mTrackingListener);
            }
            Context context = MapTrek.getApplication();
            context.unbindService(mTrackingConnection);
            mBound = false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackPressedCallback.remove();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Track track = trackViewModel.selectedTrack.getValue();
        if (track == null)
            return true;
        int itemId = item.getItemId();
        if (itemId == R.id.action_view) {
            mListener.onTrackView(track);
            mFragmentHolder.popAll();
            return true;
        }
        if (itemId == R.id.action_edit) {
            setEditorMode(true);
            return true;
        }
        if (itemId == R.id.action_share) {
            mListener.onTrackShare(track);
            return true;
        }
        if (itemId == R.id.action_delete) {
            mListener.onTrackDelete(track);
            mFragmentHolder.popCurrent();
            return true;
        }
        return false;
    }

    private void initializeTrackInformation(Track track) {
        Activity activity = requireActivity();
        Resources resources = getResources();

        viewBinding.name.setText(track.name);
        if (track.source == null || track.source.isNativeTrack()) {
            viewBinding.sourceRow.setVisibility(View.GONE);
        } else {
            viewBinding.source.setText(track.source.name);
            viewBinding.sourceRow.setVisibility(View.VISIBLE);
        }

        Track.TrackPoint ftp = track.points.get(0);
        Track.TrackPoint ltp = track.getLastPoint();
        boolean hasTime = ftp.time > 0 && ltp.time > 0;

        String startCoords = StringFormatter.coordinates(ftp);
        viewBinding.startCoordinates.setText(startCoords);

        if (hasTime) {
            Date startDate = new Date(ftp.time);
            viewBinding.startDate.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(startDate), DateFormat.getTimeFormat(activity).format(startDate)));
            viewBinding.startDateRow.setVisibility(View.VISIBLE);
            viewBinding.finishDateRow.setVisibility(View.VISIBLE);
            viewBinding.timeRow.setVisibility(View.VISIBLE);
        } else {
            viewBinding.startDateRow.setVisibility(View.GONE);
            viewBinding.finishDateRow.setVisibility(View.GONE);
            viewBinding.timeRow.setVisibility(View.GONE);
        }

        // Gather statistics
        mSpeedMeanValue = new MeanValue();
        boolean hasElevation = false;
        boolean hasSpeed = false;
        ArrayList<Entry> elevationValues = new ArrayList<>();
        ArrayList<Entry> speedValues = new ArrayList<>();
        ArrayList<String> xValues = new ArrayList<>();

        Track.TrackPoint ptp = null;
        long startTime = ftp.time;
        int i = 0;
        mSegmentCount = 1;
        for (Track.TrackPoint point : track.points) {
            if (!point.continuous)
                mSegmentCount++;

            int offset = (int) (point.time - startTime) / 1000;
            xValues.add("+" + DateUtils.formatElapsedTime(offset));

            if (!Float.isNaN(point.elevation)) {
                elevationValues.add(new Entry(point.elevation, i));

                if (point.elevation < mMinElevation && point.elevation != 0)
                    mMinElevation = point.elevation;
                if (point.elevation > mMaxElevation)
                    mMaxElevation = point.elevation;

                if (point.elevation != 0) {
                    hasElevation = true;
                    if (!Float.isNaN(mPrevElevation)) {
                        float diff = point.elevation - mPrevElevation;
                        if (diff > 0)
                            mElevationGain += diff;
                        if (diff < 0)
                            mElevationLoss -= diff;
                    }
                    mPrevElevation = point.elevation;
                }
            }

            float speed = Float.NaN;
            if (Float.isNaN(point.speed)) {
                if (hasTime) {
                    if (ptp != null) {
                        speed = ((float) point.vincentyDistance(ptp)) / ((point.time - ptp.time) / 1000f);
                    } else {
                        speed = 0f;
                    }
                }
            } else {
                speed = point.speed;
            }
            if (!Float.isNaN(speed) && !Float.isInfinite(speed)) {
                speedValues.add(new Entry(speed * StringFormatter.speedFactor, i));
                mSpeedMeanValue.addValue(speed);
                if (speed > mMaxSpeed)
                    mMaxSpeed = speed;
                hasSpeed = true;
            }

            ptp = point;
            i++;
        }
        updateTrackInformation(track, activity, resources);

        if (hasElevation || hasSpeed) {
            updateTrackStatistics(resources);
            viewBinding.statisticsHeader.setVisibility(View.VISIBLE);
        } else {
            viewBinding.statisticsHeader.setVisibility(View.GONE);
        }

        viewBinding.elevationUpRow.setVisibility(hasElevation ? View.VISIBLE : View.GONE);
        viewBinding.elevationDownRow.setVisibility(hasElevation ? View.VISIBLE : View.GONE);

        viewBinding.speedRow.setVisibility(hasSpeed ? View.VISIBLE : View.GONE);

        if (hasElevation) {
            LineDataSet elevationLine = new LineDataSet(elevationValues, "Elevation");
            elevationLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            elevationLine.setDrawFilled(true);
            elevationLine.setDrawCircles(false);
            elevationLine.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));
            elevationLine.setFillColor(elevationLine.getColor());

            ArrayList<ILineDataSet> elevationDataSets = new ArrayList<>();
            elevationDataSets.add(elevationLine);

            mElevationData = new LineData(xValues, elevationDataSets);

            viewBinding.elevationChart.setData(mElevationData);

            viewBinding.elevationChart.getLegend().setEnabled(false);
            viewBinding.elevationChart.setDescription("");

            XAxis xAxis = viewBinding.elevationChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            viewBinding.elevationHeader.setVisibility(View.VISIBLE);
            viewBinding.elevationChart.setVisibility(View.VISIBLE);
            viewBinding.elevationChart.invalidate();
        } else {
            viewBinding.elevationHeader.setVisibility(View.GONE);
            viewBinding.elevationChart.setVisibility(View.GONE);
        }

        if (hasSpeed) {
            LineDataSet speedLine = new LineDataSet(speedValues, "Speed");
            speedLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            speedLine.setDrawCircles(false);
            speedLine.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));

            ArrayList<ILineDataSet> speedDataSets = new ArrayList<>();
            speedDataSets.add(speedLine);

            mSpeedData = new LineData(xValues, speedDataSets);

            viewBinding.speedChart.setData(mSpeedData);

            viewBinding.speedChart.getLegend().setEnabled(false);
            viewBinding.speedChart.setDescription("");

            XAxis xAxis = viewBinding.speedChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            viewBinding.speedHeader.setVisibility(View.VISIBLE);
            viewBinding.speedChart.setVisibility(View.VISIBLE);
            viewBinding.speedChart.invalidate();
        } else {
            viewBinding.speedHeader.setVisibility(View.GONE);
            viewBinding.speedChart.setVisibility(View.GONE);
        }
    }

    private void updateTrackInformation(Track track, Activity activity, Resources resources) {
        Track.TrackPoint ftp = track.points.get(0);
        Track.TrackPoint ltp = track.getLastPoint();

        int pointCount = track.points.size();
        viewBinding.pointCount.setText(resources.getQuantityString(R.plurals.numberOfPoints, pointCount, pointCount));
        viewBinding.segmentCount.setText(resources.getQuantityString(R.plurals.numberOfSegments, mSegmentCount, mSegmentCount));

        String distance = StringFormatter.distanceHP(track.getDistance());
        viewBinding.distance.setText(distance);

        String finish_coords = StringFormatter.coordinates(ltp);
        viewBinding.finishCoordinates.setText(finish_coords);

        Date finishDate = new Date(ltp.time);
        viewBinding.finishDate.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(finishDate), DateFormat.getTimeFormat(activity).format(finishDate)));

        long elapsed = (ltp.time - ftp.time) / 1000;
        String timeSpan;
        if (elapsed < 24 * 3600 * 3) { // 3 days
            timeSpan = DateUtils.formatElapsedTime(elapsed);
        } else {
            timeSpan = DateUtils.formatDateRange(activity, ftp.time, ltp.time, DateUtils.FORMAT_ABBREV_MONTH);
        }
        viewBinding.timeSpan.setText(timeSpan);
    }

    private void updateTrackStatistics(Resources resources) {
        viewBinding.segmentCount.setText(resources.getQuantityString(R.plurals.numberOfSegments, mSegmentCount, mSegmentCount));
        viewBinding.maxElevation.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_elevation), StringFormatter.elevationH(mMaxElevation)));
        viewBinding.elevationGain.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.elevation_gain), StringFormatter.elevationH(mElevationGain)));
        viewBinding.minElevation.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.min_elevation), StringFormatter.elevationH(mMinElevation)));
        viewBinding.elevationLoss.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.elevation_loss), StringFormatter.elevationH(mElevationLoss)));
        float averageSpeed = mSpeedMeanValue.getMeanValue();
        viewBinding.maxSpeed.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_speed), StringFormatter.speedH(mMaxSpeed)));
        viewBinding.averageSpeed.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.average_speed), StringFormatter.speedH(averageSpeed)));
    }

    private void setEditorMode(boolean enabled) {
        Track track = trackViewModel.selectedTrack.getValue();
        if (track == null)
            return;

        Activity activity = requireActivity();

        int viewsState, editsState;
        if (enabled) {
            viewBinding.moreButton.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_done));
            viewBinding.nameEdit.setText(track.name);
            viewBinding.colorSwatch.setColor(track.style.color);
            viewBinding.colorSwatch.setOnClickListener(v -> {
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, track.style.color);
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(viewBinding.colorSwatch::setColor);
                dialog.show(getParentFragmentManager(), "ColorPickerDialog");
            });
            viewsState = View.GONE;
            editsState = View.VISIBLE;

            if (!track.source.isNativeTrack())
                HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, viewBinding.moreButton, false);
        } else {
            viewBinding.moreButton.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_more_vert));
            viewBinding.name.setText(track.name);
            viewsState = View.VISIBLE;
            editsState = View.GONE;
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(viewBinding.getRoot().getWindowToken(), 0);
        }
        TransitionManager.beginDelayedTransition(viewBinding.getRoot(), new Fade());
        viewBinding.name.setVisibility(viewsState);
        viewBinding.nameWrapper.setVisibility(editsState);
        viewBinding.colorSwatch.setVisibility(editsState);

        mEditorMode = enabled;
        mBackPressedCallback.setEnabled(enabled);
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEditorMode(false);
        }
    };

    private final ServiceConnection mTrackingConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mTrackingService = (ILocationService) service;
            mTrackingService.registerTrackingCallback(mTrackingListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            mTrackingService = null;
        }
    };

    private final ITrackingListener mTrackingListener = new ITrackingListener() { // TODO: refactor for optimization
        public void onNewPoint(boolean continuous, double lat, double lon, float elev, float speed, float trk, float accuracy, long time) {
            Track track = trackViewModel.selectedTrack.getValue();
            if (track == null)
                return;

            if (!continuous)
                mSegmentCount++;
            if (elev < mMinElevation && elev != 0)
                mMinElevation = elev;
            if (elev > mMaxElevation)
                mMaxElevation = elev;
            mSpeedMeanValue.addValue(speed);
            if (speed > mMaxSpeed)
                mMaxSpeed = speed;

            int offset = (int) (time - track.points.get(0).time) / 1000;
            String xValue = "+" + DateUtils.formatElapsedTime(offset);
            if (mElevationData != null) {
                int count = mElevationData.getDataSets().get(0).getEntryCount();
                mElevationData.addEntry(new Entry(elev, count), 0);
                mElevationData.addXValue(xValue);
            }
            if (mSpeedData != null) {
                int count = mSpeedData.getDataSets().get(0).getEntryCount();
                mSpeedData.addEntry(new Entry(speed * StringFormatter.speedFactor, count), 0);
                //mSpeedData.addXValue(xValue); they appear to share the same array
            }

            if (isVisible()) {
                Activity activity = getActivity();
                Resources resources = getResources();
                updateTrackInformation(track, activity, resources);
                updateTrackStatistics(resources);
                viewBinding.elevationChart.notifyDataSetChanged();
                viewBinding.elevationChart.invalidate();
                viewBinding.speedChart.notifyDataSetChanged();
                viewBinding.speedChart.invalidate();
            }
        }
    };
}
