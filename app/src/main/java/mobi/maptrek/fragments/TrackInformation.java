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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.Editable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.databinding.FragmentTrackInformationBinding;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.SingleLiveEvent;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.TrackViewModel;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackInformation extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private static final Logger logger = LoggerFactory.getLogger(TrackInformation.class);

    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private OnTrackActionListener mListener;
    private TrackViewModel trackViewModel;
    private TrackInformationViewModel viewModel;
    private FragmentTrackInformationBinding viewBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentTrackInformationBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(TrackInformationViewModel.initializer)).get(TrackInformationViewModel.class);

        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.selectedTrack.observe(getViewLifecycleOwner(), selectedTrack -> {
            if (!selectedTrack.equals(viewModel.track.getValue())) {
                initializeTrackInformation(selectedTrack);
                viewModel.track.setValue(selectedTrack);
                if (!viewModel.isEmpty) {
                    viewBinding.progressBar.setVisibility(View.VISIBLE);
                    viewModel.doInBackground(() -> initializeTrackStatistics(selectedTrack));
                }
            }
        });
        trackViewModel.currentTrack.observe(getViewLifecycleOwner(), currentTrack -> { // new points were added to current track
            Track track = trackViewModel.selectedTrack.getValue();
            if (track == null || track != currentTrack)
                return;
            if (viewModel.isEmpty) {
                initializeTrackInformation(track);
                if (!viewModel.isEmpty)
                    viewModel.doInBackground(() -> {
                        viewBinding.progressBar.setVisibility(View.VISIBLE);
                        initializeTrackStatistics(track);
                        updateTrackStatistics(currentTrack);
                    });
            } else {
                viewModel.doInBackground(() -> updateTrackStatistics(currentTrack));
            }
        });

        viewModel.track.observe(getViewLifecycleOwner(), trackObserver);
        viewModel.firstPoint.observe(getViewLifecycleOwner(), firstPointObserver);
        viewModel.lastPoint.observe(getViewLifecycleOwner(), lastPointObserver);
        viewModel.statisticsState.observe(getViewLifecycleOwner(), statisticsObserver);
        viewModel.chartState.observe(getViewLifecycleOwner(), chartObserver);
        viewModel.editorMode.observe(getViewLifecycleOwner(), editorModeObserver);

        viewBinding.elevationChart.setOnChartValueSelectedListener(new ChartSelectionListener(viewBinding.speedChart));
        viewBinding.elevationChart.setOnChartGestureListener(new ChartGestureListener(viewBinding.elevationChart, viewBinding.speedChart));
        viewBinding.speedChart.setOnChartValueSelectedListener(new ChartSelectionListener(viewBinding.elevationChart));
        viewBinding.speedChart.setOnChartGestureListener(new ChartGestureListener(viewBinding.speedChart, viewBinding.elevationChart));

        viewBinding.moreButton.setOnClickListener(v -> {
            Track track = trackViewModel.selectedTrack.getValue();
            if (track == null)
                return;
            boolean isCurrent = track == trackViewModel.currentTrack.getValue();
            PopupMenu popup = new PopupMenu(getContext(), viewBinding.moreButton);
            viewBinding.moreButton.setOnTouchListener(popup.getDragToOpenListener());
            popup.inflate(R.menu.context_menu_track);
            Menu menu = popup.getMenu();
            menu.findItem(R.id.action_edit).setVisible(!isCurrent);
            menu.findItem(R.id.action_delete).setVisible(track.source != null && !track.source.isNativeTrack());
            popup.setOnMenuItemClickListener(TrackInformation.this);
            popup.show();
        });
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
    public void onDetach() {
        super.onDetach();
        mBackPressedCallback.remove();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFloatingButton = mFragmentHolder.enableListActionButton(R.drawable.ic_share, v -> {
            mListener.onTrackShare(trackViewModel.selectedTrack.getValue());
            mFragmentHolder.popCurrent();
        });
        mFloatingButton.setVisibility(View.INVISIBLE);
        viewBinding.getRoot().setOnScrollChangeListener(scrollChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        viewBinding.getRoot().setOnScrollChangeListener(null);
        mFragmentHolder.disableListActionButton();
        mFloatingButton = null;
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
            viewModel.editorMode.setValue(true);
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
        viewModel.reset();
        viewModel.isEmpty = track.points.isEmpty();
        if (!viewModel.isEmpty) {
            Track.TrackPoint ftp = track.points.get(0);
            Track.TrackPoint ltp = track.getLastPoint();
            viewModel.hasTime = ftp.time > 0 && ltp.time > 0;
            viewModel.startTime = ftp.time;
            viewModel.firstPoint.setValue(ftp);
            viewModel.lastPoint.setValue(ltp);
            viewModel.segmentCount = 1;
        }
    }

    private void initializeTrackStatistics(Track track) {
        logger.debug("initializeTrackStatistics");
        for (Track.TrackPoint point : track.points) {
            if (Thread.currentThread().isInterrupted())
                return;
            processTrackPoint(point);
        }
        if (Thread.currentThread().isInterrupted())
            return;
        viewModel.lastKnownSize = track.points.size();
        viewModel.statisticsState.postValue(viewModel.lastKnownSize);
        viewModel.chartState.postValue(viewModel.lastKnownSize); // the value does not matter
        logger.debug("done");
    }

    private void updateTrackStatistics(Track currentTrack) {
        if (viewModel.lastKnownSize >= currentTrack.points.size())
            return;

        Track.TrackPoint point = null;
        for (ListIterator<Track.TrackPoint> it = currentTrack.points.listIterator(viewModel.lastKnownSize); it.hasNext(); ) {
            point = it.next();
            processTrackPoint(point);
        }
        viewModel.lastPoint.postValue(point);
        viewModel.lastKnownSize = currentTrack.points.size();
        viewModel.statisticsState.postValue(viewModel.lastKnownSize);
        viewModel.chartState.postValue(viewModel.lastKnownSize); // the value does not matter
    }

    private void processTrackPoint(Track.TrackPoint point) {
        double distance = Double.NaN;
        if (viewModel.prevPoint != null) {
            distance = point.vincentyDistance(viewModel.prevPoint);
            viewModel.distance += distance;
            if (!viewModel.prevPoint.continuous)
                viewModel.segmentCount++;
        }

        if (!Float.isNaN(point.elevation) && point.elevation != 0) {
            if (point.elevation < viewModel.minElevation)
                viewModel.minElevation = point.elevation;
            if (point.elevation > viewModel.maxElevation)
                viewModel.maxElevation = point.elevation;

            viewModel.hasElevation = true;
            if (!Float.isNaN(viewModel.prevElevation)) {
                float diff = point.elevation - viewModel.prevElevation;
                if (diff > 0)
                    viewModel.elevationGain += diff;
                if (diff < 0)
                    viewModel.elevationLoss -= diff;
            }
            viewModel.prevElevation = point.elevation;
        }

        float speed = Float.NaN;
        if (!Float.isNaN(point.speed)) {
            speed = point.speed;
        } else {
            if (viewModel.hasTime) {
                if (viewModel.prevPoint != null) {
                    speed = ((float) distance) / ((point.time - viewModel.prevPoint.time) / 1000f);
                } else {
                    speed = 0f;
                }
            }
        }

        if (!Float.isNaN(speed) && !Float.isInfinite(speed)) {
            viewModel.speedMeanValue.addValue(speed);
            if (speed > viewModel.maxSpeed)
                viewModel.maxSpeed = speed;
            viewModel.hasSpeed = true;
        }

        // TODO: Put this in separate pass? (test on huge track)
        int offset = (int) (point.time - viewModel.startTime) / 1000;
        String xValue = "+" + DateUtils.formatElapsedTime(offset);

        // TODO: resolve data inconsistency between charts
        if (!Float.isNaN(point.elevation)) {
            int count = viewModel.elevationData.getDataSetByIndex(0).getEntryCount();
            viewModel.elevationData.addEntry(new Entry(point.elevation, count), 0);
            viewModel.elevationData.addXValue(xValue);
        }

        if (!Float.isNaN(speed) && !Float.isInfinite(speed)) {
            int count = viewModel.speedData.getDataSetByIndex(0).getEntryCount();
            viewModel.speedData.addEntry(new Entry(speed * StringFormatter.speedFactor, count), 0);
            viewModel.speedData.addXValue(xValue);
        }

        viewModel.prevPoint = point;
    }

    private final Observer<Track> trackObserver = new Observer<Track>() {
        @Override
        public void onChanged(Track track) {
            if (track == null)
                return;

            viewBinding.name.setText(track.name);
            if (track.source == null || track.source.isNativeTrack()) {
                viewBinding.sourceRow.setVisibility(View.GONE);
            } else {
                viewBinding.source.setText(track.source.name);
                viewBinding.sourceRow.setVisibility(View.VISIBLE);
            }
        }
    };

    private final Observer<Track.TrackPoint> firstPointObserver = new Observer<Track.TrackPoint>() {
        @Override
        public void onChanged(Track.TrackPoint firstPoint) {
            if (firstPoint == null) {
                viewBinding.moreButton.setVisibility(View.GONE);
                viewBinding.statisticsTable.setVisibility(View.GONE);
                viewBinding.charts.setVisibility(View.GONE);
                viewBinding.empty.setVisibility(View.VISIBLE);
            } else {
                logger.debug("firstPoint changed");
                Activity activity = requireActivity();
                viewBinding.startCoordinates.setText(StringFormatter.coordinates(firstPoint));
                if (viewModel.hasTime) {
                    Date startDate = new Date(firstPoint.time);
                    viewBinding.startDate.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(startDate), DateFormat.getTimeFormat(activity).format(startDate)));
                    viewBinding.startDateRow.setVisibility(View.VISIBLE);
                    viewBinding.finishDateRow.setVisibility(View.VISIBLE);
                    viewBinding.timeRow.setVisibility(View.VISIBLE);
                } else {
                    viewBinding.startDateRow.setVisibility(View.GONE);
                    viewBinding.finishDateRow.setVisibility(View.GONE);
                    viewBinding.timeRow.setVisibility(View.GONE);
                }
                viewBinding.moreButton.setVisibility(View.VISIBLE);
                viewBinding.statisticsTable.setVisibility(View.VISIBLE);
                viewBinding.charts.setVisibility(View.VISIBLE);
                viewBinding.empty.setVisibility(View.GONE);
                mFloatingButton.show();
            }
        }
    };

    private final Observer<Track.TrackPoint> lastPointObserver = new Observer<Track.TrackPoint>() {
        @Override
        public void onChanged(Track.TrackPoint lastPoint) {
            if (lastPoint == null)
                return;
            logger.debug("lastPoint changed");
            Activity activity = requireActivity();
            viewBinding.finishCoordinates.setText(StringFormatter.coordinates(lastPoint));
            if (viewModel.hasTime) {
                Date finishDate = new Date(lastPoint.time);
                viewBinding.finishDate.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(finishDate), DateFormat.getTimeFormat(activity).format(finishDate)));
                long elapsed = (lastPoint.time - viewModel.startTime) / 1000;
                String timeSpan;
                if (elapsed < 24 * 3600 * 3) { // 3 days
                    timeSpan = DateUtils.formatElapsedTime(elapsed);
                } else {
                    timeSpan = DateUtils.formatDateRange(activity, viewModel.startTime, lastPoint.time, DateUtils.FORMAT_ABBREV_MONTH);
                }
                viewBinding.timeSpan.setText(timeSpan);
            }
        }
    };

    private final Observer<Integer> statisticsObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer lastSize) {
            Resources resources = getResources();
            if (lastSize > 0) {
                viewBinding.progressBar.setVisibility(View.GONE);
                viewBinding.pointCount.setText(resources.getQuantityString(R.plurals.numberOfPoints, lastSize, lastSize));
                viewBinding.segmentCount.setText(resources.getQuantityString(R.plurals.numberOfSegments, viewModel.segmentCount, viewModel.segmentCount));
                String distance = StringFormatter.distanceHP(viewModel.distance);
                viewBinding.distance.setText(distance);
            } else {
                viewBinding.pointCount.setText(R.string.calculating);
                viewBinding.segmentCount.setText(null);
                viewBinding.distance.setText(R.string.calculating);
            }
            if (viewModel.hasElevation || viewModel.hasSpeed) {
                viewBinding.maxElevation.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_elevation), StringFormatter.elevationH(viewModel.maxElevation)));
                viewBinding.elevationGain.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.elevation_gain), StringFormatter.elevationH(viewModel.elevationGain)));
                viewBinding.minElevation.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.min_elevation), StringFormatter.elevationH(viewModel.minElevation)));
                viewBinding.elevationLoss.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.elevation_loss), StringFormatter.elevationH(viewModel.elevationLoss)));
                float averageSpeed = viewModel.speedMeanValue.getMeanValue();
                viewBinding.maxSpeed.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_speed), StringFormatter.speedH(viewModel.maxSpeed)));
                viewBinding.averageSpeed.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.average_speed), StringFormatter.speedH(averageSpeed)));
                viewBinding.statisticsHeader.setVisibility(View.VISIBLE);
            } else {
                viewBinding.statisticsHeader.setVisibility(View.GONE);
            }
            viewBinding.elevationUpRow.setVisibility(viewModel.hasElevation ? View.VISIBLE : View.GONE);
            viewBinding.elevationDownRow.setVisibility(viewModel.hasElevation ? View.VISIBLE : View.GONE);
            viewBinding.speedRow.setVisibility(viewModel.hasSpeed ? View.VISIBLE : View.GONE);
        }
    };

    private final Observer<Integer> chartObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer lastSize) {
            if (viewModel.hasElevation) {
                if (viewBinding.elevationChart.getData() == null) {
                    viewBinding.elevationChart.setData(viewModel.elevationData);
                    viewBinding.elevationChart.getLegend().setEnabled(false);
                    viewBinding.elevationChart.setDescription("");

                    XAxis xAxis = viewBinding.elevationChart.getXAxis();
                    xAxis.setDrawGridLines(false);
                    xAxis.setDrawAxisLine(true);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    viewBinding.elevationHeader.setVisibility(View.VISIBLE);
                    viewBinding.elevationChart.setVisibility(View.VISIBLE);
                } else {
                    viewBinding.elevationChart.notifyDataSetChanged();
                }
                viewBinding.elevationChart.invalidate();
            } else {
                viewBinding.elevationHeader.setVisibility(View.GONE);
                viewBinding.elevationChart.setVisibility(View.GONE);
            }

            if (viewModel.hasSpeed) {
                if (viewBinding.speedChart.getData() == null) {
                    viewBinding.speedChart.setData(viewModel.speedData);
                    viewBinding.speedChart.getLegend().setEnabled(false);
                    viewBinding.speedChart.setDescription("");

                    XAxis xAxis = viewBinding.speedChart.getXAxis();
                    xAxis.setDrawGridLines(false);
                    xAxis.setDrawAxisLine(true);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    viewBinding.speedHeader.setVisibility(View.VISIBLE);
                    viewBinding.speedChart.setVisibility(View.VISIBLE);
                } else {
                    viewBinding.speedChart.notifyDataSetChanged();
                }
                viewBinding.speedChart.invalidate();
            } else {
                viewBinding.speedHeader.setVisibility(View.GONE);
                viewBinding.speedChart.setVisibility(View.GONE);
            }
        }
    };

    private final Observer<Boolean> editorModeObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean enabled) {
            Track track = trackViewModel.selectedTrack.getValue();
            if (track == null)
                return;

            Activity activity = requireActivity();

            int viewsState, editsState;
            if (enabled) {
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

                mFragmentHolder.enableListActionButton(R.drawable.ic_done, v -> {
                    Editable text = viewBinding.nameEdit.getText();
                    if (text != null)
                        track.name = text.toString().trim();
                    track.style.color = viewBinding.colorSwatch.getColor();
                    mListener.onTrackSave(track);
                    viewModel.editorMode.setValue(false);
                });

                if (!track.source.isNativeTrack())
                    HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, viewBinding.moreButton, false);
            } else {
                mFragmentHolder.disableListActionButton(); // return FAB to previous state
                viewBinding.name.setText(track.name);
                viewsState = View.VISIBLE;
                editsState = View.GONE;
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(viewBinding.getRoot().getWindowToken(), 0);
            }
            if (!enabled) // when enabled, delayed transition is initiated by list FAB
                TransitionManager.beginDelayedTransition(viewBinding.getRoot(), new Fade());
            viewBinding.name.setVisibility(viewsState);
            viewBinding.moreButton.setVisibility(viewsState);
            viewBinding.nameWrapper.setVisibility(editsState);
            viewBinding.colorSwatch.setVisibility(editsState);
            mBackPressedCallback.setEnabled(enabled);
        }
    };

    View.OnScrollChangeListener scrollChangeListener = new View.OnScrollChangeListener() {
        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            if (viewModel.isEmpty || Boolean.TRUE.equals(viewModel.editorMode.getValue()))
                return;
            int dy = scrollY - oldScrollY;
            if (scrollY < 10 || dy < -15 && !mFloatingButton.isShown())
                mFloatingButton.show();
            else if (dy > 10 && mFloatingButton.isShown())
                mFloatingButton.hide();
        }
    };

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            viewModel.editorMode.setValue(false);
        }
    };

    private static class TrackInformationViewModel extends ViewModel {
        private boolean isEmpty = true;
        private boolean hasTime;
        private boolean hasElevation;
        private boolean hasSpeed;
        private int lastKnownSize;
        private Track.TrackPoint prevPoint;
        private float prevElevation;
        private long startTime;
        private int segmentCount;
        public double distance;
        private float elevationGain;
        private float elevationLoss;
        private float minElevation;
        private float maxElevation;
        private float maxSpeed;
        private MeanValue speedMeanValue;

        private final LineData elevationData;
        private final LineData speedData;

        private final MutableLiveData<Track> track = new MutableLiveData<>(null);
        private final MutableLiveData<Track.TrackPoint> firstPoint = new MutableLiveData<>(null);
        private final MutableLiveData<Track.TrackPoint> lastPoint = new MutableLiveData<>(null);
        private final MutableLiveData<Integer> statisticsState = new MutableLiveData<>(0);
        private final MutableLiveData<Integer> chartState = new MutableLiveData<>(0);
        private final SingleLiveEvent<Boolean> editorMode = new SingleLiveEvent<>();

        public TrackInformationViewModel(int color) {
            LineDataSet elevationLine = new LineDataSet(null, "Elevation");
            elevationLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            elevationLine.setDrawFilled(true);
            elevationLine.setDrawCircles(false);
            elevationLine.setColor(color);
            elevationLine.setFillColor(elevationLine.getColor());
            elevationData = new LineData();
            elevationData.addDataSet(elevationLine);

            LineDataSet speedLine = new LineDataSet(null, "Speed");
            speedLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            speedLine.setDrawCircles(false);
            speedLine.setColor(color);
            speedData = new LineData();
            speedData.addDataSet(speedLine);
        }

        public void reset() {
            isEmpty = true;
            hasTime = false;
            hasElevation = false;
            hasSpeed = false;
            lastKnownSize = 0;
            prevPoint = null;
            prevElevation = Float.NaN;
            startTime = 0;
            segmentCount = 0;
            distance = 0.0;
            elevationGain = 0f;
            elevationLoss = 0f;
            minElevation = Float.MAX_VALUE;
            maxElevation = Float.MIN_VALUE;
            maxSpeed = 0f;
            speedMeanValue = new MeanValue();

            elevationData.getDataSetByIndex(0).clear();
            elevationData.setXVals(new ArrayList<>());
            speedData.getDataSetByIndex(0).clear();
            speedData.setXVals(new ArrayList<>());

            firstPoint.setValue(null);
            lastPoint.setValue(null);
        }

        static final ViewModelInitializer<TrackInformationViewModel> initializer = new ViewModelInitializer<>(
                TrackInformationViewModel.class,
                creationExtras -> {
                    Application app = creationExtras.get(APPLICATION_KEY);
                    assert app != null;
                    @ColorInt int color = app.getResources().getColor(R.color.colorAccentLight, app.getTheme());
                    return new TrackInformationViewModel(color);
                }
        );

        private final ExecutorService service =  Executors.newSingleThreadExecutor();
        private Future<?> task;

        private void doInBackground(Runnable runnable) {
            logger.debug("doInBackground");
            if (task != null && !task.isDone())
                task.cancel(true);
            task = service.submit(runnable);
        }

        @Override
        protected void onCleared() {
            logger.debug("onCleared");
            service.shutdownNow();
        }

    }

    private static class ChartSelectionListener implements OnChartValueSelectedListener {
        private final LineChart other;

        public ChartSelectionListener(LineChart other) {
            this.other = other;
        }

        @Override
        public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
            if (!other.isEmpty())
                other.highlightValue(e.getXIndex(), 0, false);
        }

        @Override
        public void onNothingSelected() {
        }
    }

    private static class ChartGestureListener implements OnChartGestureListener {
        private final LineChart self;
        private final LineChart other;

        public ChartGestureListener(LineChart me, LineChart other) {
            this.self = me;
            this.other = other;
        }

        private void syncCharts() {
            if (other.isEmpty())
                return;
            float[] myValues = new float[9];
            Matrix otherMatrix;
            float[] otherValues = new float[9];
            self.getViewPortHandler().getMatrixTouch().getValues(myValues);
            otherMatrix = other.getViewPortHandler().getMatrixTouch();
            otherMatrix.getValues(otherValues);
            otherValues[Matrix.MSCALE_X] = myValues[Matrix.MSCALE_X];
            otherValues[Matrix.MTRANS_X] = myValues[Matrix.MTRANS_X];
            otherValues[Matrix.MSKEW_X] = myValues[Matrix.MSKEW_X];
            otherMatrix.setValues(otherValues);
            other.getViewPortHandler().refresh(otherMatrix, other, true);
        }

        @Override
        public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        }

        @Override
        public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        }

        @Override
        public void onChartLongPressed(MotionEvent me) {
        }

        @Override
        public void onChartDoubleTapped(MotionEvent me) {
            self.post(this::syncCharts);
        }

        @Override
        public void onChartSingleTapped(MotionEvent me) {
        }

        @Override
        public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            syncCharts();
        }

        @Override
        public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            syncCharts();
        }

        @Override
        public void onChartTranslate(MotionEvent me, float dX, float dY) {
            syncCharts();
        }
    }
}
