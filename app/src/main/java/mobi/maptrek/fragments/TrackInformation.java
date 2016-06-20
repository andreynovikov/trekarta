package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
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

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.view.ColorSwatch;

public class TrackInformation extends Fragment implements PopupMenu.OnMenuItemClickListener, OnBackPressedListener {
    private Track mTrack;
    private boolean mIsCurrent;

    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnTrackActionListener mListener;
    private ImageButton mMoreButton;
    private boolean mEditorMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_track_information, container, false);
        mMoreButton = (ImageButton) rootView.findViewById(R.id.moreButton);
        mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorMode) {
                    mTrack.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                    mTrack.style.color = ((ColorSwatch) rootView.findViewById(R.id.colorSwatch)).getColor();
                    mListener.onTrackSave(mTrack);
                    setEditorMode(false);
                } else {
                    PopupMenu popup = new PopupMenu(getContext(), mMoreButton);
                    mMoreButton.setOnTouchListener(popup.getDragToOpenListener());
                    popup.inflate(R.menu.context_menu_track);
                    Menu menu = popup.getMenu();
                    menu.findItem(R.id.action_delete).setVisible(mTrack.source != null && !mTrack.source.isNativeTrack());
                    popup.setOnMenuItemClickListener(TrackInformation.this);
                    popup.show();
                }
            }
        });
        if (mIsCurrent) {
            ImageButton stopButton = (ImageButton) rootView.findViewById(R.id.stopButton);
            stopButton.setVisibility(View.VISIBLE);
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMapHolder.disableTracking();
                    mFragmentHolder.popCurrent();
                }
            });
        }
        mEditorMode = false;
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateTrackInformation();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
        }
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
            mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mMapHolder = null;
        mListener = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view:
                mListener.onTrackView(mTrack);
                mFragmentHolder.popAll();
                return true;
            case R.id.action_edit:
                setEditorMode(true);
                return true;
            case R.id.action_share:
                mListener.onTrackShare(mTrack);
                return true;
            case R.id.action_delete:
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
            updateTrackInformation();
        }
    }

    private void updateTrackInformation() {
        Activity activity = getActivity();
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

        int pointCount = mTrack.points.size();
        ((TextView) rootView.findViewById(R.id.pointCount)).setText(resources.getQuantityString(R.plurals.numberOfPoints, pointCount, pointCount));

        String distance = StringFormatter.distanceH(mTrack.getDistance());
        ((TextView) rootView.findViewById(R.id.distance)).setText(distance);

        Track.TrackPoint ftp = mTrack.points.get(0);
        Track.TrackPoint ltp = mTrack.getLastPoint();
        boolean hasTime = ftp.time > 0 && ltp.time > 0;

        String start_coords = StringFormatter.coordinates(" ", ftp.latitudeE6 / 1E6, ftp.longitudeE6 / 1E6);
        ((TextView) rootView.findViewById(R.id.startCoordinates)).setText(start_coords);
        String finish_coords = StringFormatter.coordinates(" ", ltp.latitudeE6 / 1E6, ltp.longitudeE6 / 1E6);
        ((TextView) rootView.findViewById(R.id.finishCoordinates)).setText(finish_coords);

        View startDateRow = rootView.findViewById(R.id.startDateRow);
        View finishDateRow = rootView.findViewById(R.id.finishDateRow);
        View timeRow = rootView.findViewById(R.id.timeRow);
        if (hasTime) {
            Date startDate = new Date(ftp.time);
            Date finishDate = new Date(ltp.time);
            ((TextView) rootView.findViewById(R.id.startDate)).setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(startDate), DateFormat.getTimeFormat(activity).format(startDate)));
            ((TextView) rootView.findViewById(R.id.finishDate)).setText(String.format("%s %s", DateFormat.getDateFormat(activity).format(finishDate), DateFormat.getTimeFormat(activity).format(finishDate)));
            startDateRow.setVisibility(View.VISIBLE);
            finishDateRow.setVisibility(View.VISIBLE);

            long elapsed = (ltp.time - ftp.time) / 1000;
            String timeSpan;
            if (elapsed < 24 * 3600 * 3) { // 3 days
                timeSpan = DateUtils.formatElapsedTime(elapsed);
            } else {
                timeSpan = DateUtils.formatDateRange(activity, ftp.time, ltp.time, DateUtils.FORMAT_ABBREV_MONTH);
            }
            ((TextView) rootView.findViewById(R.id.timeSpan)).setText(timeSpan);
            timeRow.setVisibility(View.VISIBLE);
        } else {
            startDateRow.setVisibility(View.GONE);
            finishDateRow.setVisibility(View.GONE);
            timeRow.setVisibility(View.GONE);
        }

        // Gather statistics
        int segmentCount = 0;
        float minElevation = Float.MAX_VALUE;
        float maxElevation = Float.MIN_VALUE;
        float maxSpeed = 0;

        MeanValue mv = new MeanValue();
        boolean hasElevation = false;
        boolean hasSpeed = false;
        ArrayList<Entry> elevationValues = new ArrayList<>();
        ArrayList<Entry> speedValues = new ArrayList<>();
        ArrayList<String> xValues = new ArrayList<>();

        Track.TrackPoint ptp = null;
        long startTime = ftp.time;
        int i = 0;
        for (Track.TrackPoint point : mTrack.points) {
            if (!point.continuous)
                segmentCount++;

            int offset = (int) (point.time - startTime) / 1000;
            xValues.add("+" + DateUtils.formatElapsedTime(offset));

            if (!Float.isNaN(point.elevation)) {
                elevationValues.add(new Entry(point.elevation, i));

                if (point.elevation < minElevation && point.elevation != 0)
                    minElevation = point.elevation;
                if (point.elevation > maxElevation)
                    maxElevation = point.elevation;

                if (point.elevation != 0)
                    hasElevation = true;
            }

            float speed = Float.NaN;
            if (Float.isNaN(point.speed)) {
                if (hasTime) {
                    if (ptp != null) {
                        speed = ((float) point.distanceTo(ptp)) / ((point.time - ptp.time) / 1000);
                    } else {
                        speed = 0f;
                    }
                }
            } else {
                speed = point.speed;
            }
            if (!Float.isNaN(speed)) {
                speedValues.add(new Entry(speed * StringFormatter.speedFactor, i));
                mv.addValue(speed);
                if (speed > maxSpeed)
                    maxSpeed = speed;
                hasSpeed = true;
            }

            ptp = point;
            i++;
        }

        ((TextView) rootView.findViewById(R.id.segmentCount)).setText(resources.getQuantityString(R.plurals.numberOfSegments, segmentCount, segmentCount));

        View statisticsHeader = rootView.findViewById(R.id.statisticsHeader);
        if (hasElevation || hasSpeed) {
            statisticsHeader.setVisibility(View.VISIBLE);
        } else {
            statisticsHeader.setVisibility(View.GONE);
        }

        View elevationUpRow = rootView.findViewById(R.id.elevationUpRow);
        View elevationDownRow = rootView.findViewById(R.id.elevationDownRow);
        if (hasElevation) {
            ((TextView) rootView.findViewById(R.id.maxElevation)).setText(StringFormatter.elevationH(maxElevation));
            ((TextView) rootView.findViewById(R.id.minElevation)).setText(StringFormatter.elevationH(minElevation));
            elevationUpRow.setVisibility(View.VISIBLE);
            elevationDownRow.setVisibility(View.VISIBLE);
        } else {
            elevationUpRow.setVisibility(View.GONE);
            elevationDownRow.setVisibility(View.GONE);
        }

        View speedRow = rootView.findViewById(R.id.speedRow);
        if (hasSpeed) {
            float averageSpeed = mv.getMeanValue();
            ((TextView) rootView.findViewById(R.id.maxSpeed)).setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.max_speed), StringFormatter.speedH(maxSpeed)));
            ((TextView) rootView.findViewById(R.id.averageSpeed)).setText(String.format(Locale.getDefault(), "%s: %s", resources.getString(R.string.average_speed), StringFormatter.speedH(averageSpeed)));
            speedRow.setVisibility(View.VISIBLE);
        } else {
            speedRow.setVisibility(View.GONE);
        }

        //noinspection PointlessBooleanExpression
        if (!BuildConfig.FULL_VERSION) {
            rootView.findViewById(R.id.charts).setVisibility(View.GONE);
            return;
        }

        View elevationHeader = rootView.findViewById(R.id.elevationHeader);
        LineChart elevationChart = (LineChart) rootView.findViewById(R.id.elevationChart);
        if (hasElevation) {
            LineDataSet elevationLine = new LineDataSet(elevationValues, "Elevation");
            elevationLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            elevationLine.setDrawFilled(true);
            elevationLine.setDrawCircles(false);
            elevationLine.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));
            elevationLine.setFillColor(elevationLine.getColor());

            ArrayList<ILineDataSet> elevationDataSets = new ArrayList<>();
            elevationDataSets.add(elevationLine);

            LineData elevationData = new LineData(xValues, elevationDataSets);

            elevationChart.setData(elevationData);

            elevationChart.getLegend().setEnabled(false);
            elevationChart.setDescription("");

            XAxis xAxis = elevationChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            elevationHeader.setVisibility(View.VISIBLE);
            elevationChart.setVisibility(View.VISIBLE);
            elevationChart.invalidate();
        } else {
            elevationHeader.setVisibility(View.GONE);
            elevationChart.setVisibility(View.GONE);
        }

        View speedHeader = rootView.findViewById(R.id.speedHeader);
        LineChart speedChart = (LineChart) rootView.findViewById(R.id.speedChart);
        if (hasSpeed) {
            LineDataSet speedLine = new LineDataSet(speedValues, "Speed");
            speedLine.setAxisDependency(YAxis.AxisDependency.LEFT);
            speedLine.setDrawCircles(false);
            speedLine.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));

            ArrayList<ILineDataSet> speedDataSets = new ArrayList<>();
            speedDataSets.add(speedLine);

            LineData speedData = new LineData(xValues, speedDataSets);

            speedChart.setData(speedData);

            speedChart.getLegend().setEnabled(false);
            speedChart.setDescription("");

            XAxis xAxis = speedChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            speedHeader.setVisibility(View.VISIBLE);
            speedChart.setVisibility(View.VISIBLE);
            speedChart.invalidate();
        } else {
            speedHeader.setVisibility(View.GONE);
            speedChart.setVisibility(View.GONE);
        }
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) getView();
        assert rootView != null;

        final ColorSwatch colorSwatch = (ColorSwatch) rootView.findViewById(R.id.colorSwatch);

        int viewsState, editsState;
        if (enabled) {
            mMoreButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_done));
            ((EditText) rootView.findViewById(R.id.nameEdit)).setText(mTrack.name);
            colorSwatch.setColor(mTrack.style.color);
            colorSwatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Implement class that hides this behaviour
                    ArrayList<Integer> colorList = new ArrayList<>(7);
                    colorList.add(0xffff0000);
                    colorList.add(0xff00ff00);
                    colorList.add(0xff0000ff);
                    colorList.add(0xffffff00);
                    colorList.add(0xff00ffff);
                    colorList.add(0xff000000);
                    colorList.add(0xffff00ff);
                    if (!colorList.contains(mTrack.style.color))
                        colorList.add(mTrack.style.color);
                    if (!colorList.contains(MarkerStyle.DEFAULT_COLOR))
                        colorList.add(MarkerStyle.DEFAULT_COLOR);
                    int[] colors = new int[colorList.size()];
                    int i = 0;
                    for (Integer integer : colorList)
                        colors[i++] = integer;
                    ColorPickerDialog dialog = new ColorPickerDialog();
                    dialog.setColors(colors, mTrack.style.color);
                    dialog.setArguments(R.string.color_picker_default_title, 4, ColorPickerDialog.SIZE_SMALL);
                    dialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int color) {
                            colorSwatch.setColor(color);
                        }
                    });
                    dialog.show(getFragmentManager(), "ColorPickerDialog");
                }
            });
            viewsState = View.GONE;
            editsState = View.VISIBLE;

            if (!mTrack.source.isNativeTrack())
                HelperUtils.showAdvice(Configuration.ADVICE_UPDATE_EXTERNAL_SOURCE, R.string.advice_update_external_source, mFragmentHolder.getCoordinatorLayout());
        } else {
            mMoreButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_more_vert));
            ((TextView) rootView.findViewById(R.id.name)).setText(mTrack.name);
            viewsState = View.VISIBLE;
            editsState = View.GONE;
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        // TODO Optimize view findings
        TransitionManager.beginDelayedTransition(rootView, new Fade());
        rootView.findViewById(R.id.name).setVisibility(viewsState);
        rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);
        colorSwatch.setVisibility(editsState);

        mEditorMode = enabled;
    }

    @Override
    public boolean onBackClick() {
        if (mEditorMode) {
            setEditorMode(false);
            return true;
        } else {
            return false;
        }
    }
}
