/*
 * Copyright 2023 Andrey Novikov
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
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
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.ITrackingListener;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.StringFormatter;

public class TrackInformation extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private Track mTrack;
    private boolean mIsCurrent;

    int mSegmentCount = 0;
    float mMinElevation = Float.MAX_VALUE;
    float mMaxElevation = Float.MIN_VALUE;
    float mMaxSpeed = 0;
    private MeanValue mSpeedMeanValue;

    private LineData mElevationData;
    private LineData mSpeedData;

    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnTrackActionListener mListener;
    private TextView mPointCountView;
    private TextView mSegmentCountView;
    private TextView mDistanceView;
    private TextView mStartCoordinatesView;
    private TextView mFinishCoordinatesView;
    private TextView mTimeSpanView;
    private TextView mStartDateView;
    private TextView mFinishDateView;
    private TextView mMaxElevationView;
    private TextView mMinElevationView;
    private TextView mMaxSpeedView;
    private TextView mAverageSpeedView;
    private LineChart mElevationChart;
    private LineChart mSpeedChart;
    private ImageButton mMoreButton;
    private boolean mEditorMode;
    private boolean mBound;
    private ILocationService mTrackingService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if (mIsCurrent) {
            Context context = MapTrek.getApplication();
            mBound = context.bindService(new Intent(context, LocationService.class), mTrackingConnection, 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_track_information, container, false);
        mPointCountView = rootView.findViewById(R.id.pointCount);
        mSegmentCountView = rootView.findViewById(R.id.segmentCount);
        mDistanceView = rootView.findViewById(R.id.distance);
        mStartCoordinatesView = rootView.findViewById(R.id.startCoordinates);
        mFinishCoordinatesView = rootView.findViewById(R.id.finishCoordinates);
        mTimeSpanView = rootView.findViewById(R.id.timeSpan);
        mStartDateView = rootView.findViewById(R.id.startDate);
        mFinishDateView = rootView.findViewById(R.id.finishDate);
        mMaxElevationView = rootView.findViewById(R.id.maxElevation);
        mMinElevationView = rootView.findViewById(R.id.minElevation);
        mMaxSpeedView = rootView.findViewById(R.id.maxSpeed);
        mAverageSpeedView = rootView.findViewById(R.id.averageSpeed);
        mElevationChart = rootView.findViewById(R.id.elevationChart);
        mSpeedChart = rootView.findViewById(R.id.speedChart);
        mMoreButton = rootView.findViewById(R.id.moreButton);
        mMoreButton.setOnClickListener(v -> {
            if (mEditorMode) {
                mTrack.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                mTrack.style.color = ((ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch)).getColor();
                mListener.onTrackSave(mTrack);
                setEditorMode(false);
            } else {
                PopupMenu popup = new PopupMenu(getContext(), mMoreButton);
                mMoreButton.setOnTouchListener(popup.getDragToOpenListener());
                popup.inflate(R.menu.context_menu_track);
                Menu menu = popup.getMenu();
                menu.findItem(R.id.action_edit).setVisible(!mIsCurrent);
                menu.findItem(R.id.action_delete).setVisible(mTrack.source != null && !mTrack.source.isNativeTrack());
                popup.setOnMenuItemClickListener(TrackInformation.this);
                popup.show();
            }
        });
        if (mIsCurrent) {
            ImageButton stopButton = rootView.findViewById(R.id.stopButton);
            stopButton.setVisibility(View.VISIBLE);
            stopButton.setOnClickListener(v -> {
                mMapHolder.disableTracking();
                mFragmentHolder.popCurrent();
            });
        }
        mEditorMode = false;
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeTrackInformation();
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
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
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
        mMapHolder = null;
        mListener = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_view) {
            mListener.onTrackView(mTrack);
            mFragmentHolder.popAll();
            return true;
        }
        if (itemId == R.id.action_edit) {
            setEditorMode(true);
            return true;
        }
        if (itemId == R.id.action_share) {
            mListener.onTrackShare(mTrack);
            return true;
        }
        if (itemId == R.id.action_delete) {
            mListener.onTrackDelete(mTrack);
            mFragmentHolder.popCurrent();
            return true;
        }
        return false;
    }

    public void setTrack(Track track, boolean current) {
        mTrack = track;
        mIsCurrent = current;
        if (isVisible()) {
            initializeTrackInformation();
        }
    }

    public boolean hasCurrentTrack() {
        return mIsCurrent;
    }

    private void initializeTrackInformation() {
        Activity activity = requireActivity();
        Resources resources = getResources();

        View rootView = getView();
        assert rootView != null;

        ((TextView) rootView.findViewById(R.id.name)).setText(mTrack.name);
        View sourceRow = rootView.findViewById(R.id.sourceRow);
        if (mTrack.source == null || mTrack.source.isNativeTrack()) {
            sourceRow.setVisibility(View.GONE);
        } else {
            ((TextView) rootView.findViewById(R.id.source)).setText(mTrack.source.name);
            sourceRow.setVisibility(View.VISIBLE);
        }

        Track.TrackPoint ftp = mTrack.points.get(0);
        Track.TrackPoint ltp = mTrack.getLastPoint();
        boolean hasTime = ftp.time > 0 && ltp.time > 0;

        String startCoords = StringFormatter.coordinates(ftp);
        mStartCoordinatesView.setText(startCoords);

        View startDateRow = rootView.findViewById(R.id.startDateRow);
        View finishDateRow = rootView.findViewById(R.id.finishDateRow);
        View timeRow = rootView.findViewById(R.id.timeRow);
        if (hasTime) {
            Date startDate = new Date(ftp.time);
            mStartDateView.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(startDate), DateFormat.getTimeFormat(activity).format(startDate)));
            startDateRow.setVisibility(View.VISIBLE);
            finishDateRow.setVisibility(View.VISIBLE);
            timeRow.setVisibility(View.VISIBLE);
        } else {
            startDateRow.setVisibility(View.GONE);
            finishDateRow.setVisibility(View.GONE);
            timeRow.setVisibility(View.GONE);
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
        for (Track.TrackPoint point : mTrack.points) {
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

                if (point.elevation != 0)
                    hasElevation = true;
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
        updateTrackInformation(activity, resources);

        View statisticsHeader = rootView.findViewById(R.id.statisticsHeader);
        if (hasElevation || hasSpeed) {
            updateTrackStatistics(resources);
            statisticsHeader.setVisibility(View.VISIBLE);
        } else {
            statisticsHeader.setVisibility(View.GONE);
        }

        View elevationUpRow = rootView.findViewById(R.id.elevationUpRow);
        View elevationDownRow = rootView.findViewById(R.id.elevationDownRow);
        elevationUpRow.setVisibility(hasElevation ? View.VISIBLE : View.GONE);
        elevationDownRow.setVisibility(hasElevation ? View.VISIBLE : View.GONE);

        View speedRow = rootView.findViewById(R.id.speedRow);
        speedRow.setVisibility(hasSpeed ? View.VISIBLE : View.GONE);

        View elevationHeader = rootView.findViewById(R.id.elevationHeader);
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

            mElevationChart.setData(mElevationData);

            mElevationChart.getLegend().setEnabled(false);
            mElevationChart.setDescription("");

            XAxis xAxis = mElevationChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            elevationHeader.setVisibility(View.VISIBLE);
            mElevationChart.setVisibility(View.VISIBLE);
            mElevationChart.invalidate();
        } else {
            elevationHeader.setVisibility(View.GONE);
            mElevationChart.setVisibility(View.GONE);
        }

        View speedHeader = rootView.findViewById(R.id.speedHeader);
        if (hasSpeed) {
            LineDataSet speedLine = new LineDataSet(speedValues, "Speed");
            speedLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            speedLine.setDrawCircles(false);
            speedLine.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));

            ArrayList<ILineDataSet> speedDataSets = new ArrayList<>();
            speedDataSets.add(speedLine);

            mSpeedData = new LineData(xValues, speedDataSets);

            mSpeedChart.setData(mSpeedData);

            mSpeedChart.getLegend().setEnabled(false);
            mSpeedChart.setDescription("");

            XAxis xAxis = mSpeedChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            speedHeader.setVisibility(View.VISIBLE);
            mSpeedChart.setVisibility(View.VISIBLE);
            mSpeedChart.invalidate();
        } else {
            speedHeader.setVisibility(View.GONE);
            mSpeedChart.setVisibility(View.GONE);
        }
    }

    private void updateTrackInformation(Activity activity, Resources resources) {
        Track.TrackPoint ftp = mTrack.points.get(0);
        Track.TrackPoint ltp = mTrack.getLastPoint();

        int pointCount = mTrack.points.size();
        mPointCountView.setText(resources.getQuantityString(R.plurals.numberOfPoints, pointCount, pointCount));
        mSegmentCountView.setText(resources.getQuantityString(R.plurals.numberOfSegments, mSegmentCount, mSegmentCount));

        String distance = StringFormatter.distanceHP(mTrack.getDistance());
        mDistanceView.setText(distance);

        String finish_coords = StringFormatter.coordinates(ltp);
        mFinishCoordinatesView.setText(finish_coords);

        Date finishDate = new Date(ltp.time);
        mFinishDateView.setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(finishDate), DateFormat.getTimeFormat(activity).format(finishDate)));

        long elapsed = (ltp.time - ftp.time) / 1000;
        String timeSpan;
        if (elapsed < 24 * 3600 * 3) { // 3 days
            timeSpan = DateUtils.formatElapsedTime(elapsed);
        } else {
            timeSpan = DateUtils.formatDateRange(activity, ftp.time, ltp.time, DateUtils.FORMAT_ABBREV_MONTH);
        }
        mTimeSpanView.setText(timeSpan);
    }

    private void updateTrackStatistics(Resources resources) {
        mSegmentCountView.setText(resources.getQuantityString(R.plurals.numberOfSegments, mSegmentCount, mSegmentCount));
        mMaxElevationView.setText(StringFormatter.elevationH(mMaxElevation));
        mMinElevationView.setText(StringFormatter.elevationH(mMinElevation));
        float averageSpeed = mSpeedMeanValue.getMeanValue();
        mMaxSpeedView.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_speed), StringFormatter.speedH(mMaxSpeed)));
        mAverageSpeedView.setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.average_speed), StringFormatter.speedH(averageSpeed)));
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) requireView();
        Activity activity = requireActivity();

        final ColorPickerSwatch colorSwatch = rootView.findViewById(R.id.colorSwatch);

        int viewsState, editsState;
        if (enabled) {
            mMoreButton.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_done));
            ((EditText) rootView.findViewById(R.id.nameEdit)).setText(mTrack.name);
            colorSwatch.setColor(mTrack.style.color);
            colorSwatch.setOnClickListener(v -> {
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, mTrack.style.color);
                dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                dialog.setOnColorSelectedListener(colorSwatch::setColor);
                dialog.show(getParentFragmentManager(), "ColorPickerDialog");
            });
            viewsState = View.GONE;
            editsState = View.VISIBLE;

            if (!mTrack.source.isNativeTrack())
                HelperUtils.showTargetedAdvice(activity, Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, mMoreButton, false);
        } else {
            mMoreButton.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_more_vert));
            ((TextView) rootView.findViewById(R.id.name)).setText(mTrack.name);
            viewsState = View.VISIBLE;
            editsState = View.GONE;
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
        }
        // TODO Optimize view findings
        TransitionManager.beginDelayedTransition(rootView, new Fade());
        rootView.findViewById(R.id.name).setVisibility(viewsState);
        rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);
        colorSwatch.setVisibility(editsState);

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

    private final ITrackingListener mTrackingListener = new ITrackingListener() {
        public void onNewPoint(boolean continuous, double lat, double lon, float elev, float speed, float trk, float accuracy, long time) {
            if (!continuous)
                mSegmentCount++;
            if (elev < mMinElevation && elev != 0)
                mMinElevation = elev;
            if (elev > mMaxElevation)
                mMaxElevation = elev;
            mSpeedMeanValue.addValue(speed);
            if (speed > mMaxSpeed)
                mMaxSpeed = speed;

            int offset = (int) (time - mTrack.points.get(0).time) / 1000;
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
                updateTrackInformation(activity, resources);
                updateTrackStatistics(resources);
                mElevationChart.notifyDataSetChanged();
                mElevationChart.invalidate();
                mSpeedChart.notifyDataSetChanged();
                mSpeedChart.invalidate();
            }
        }
    };
}
