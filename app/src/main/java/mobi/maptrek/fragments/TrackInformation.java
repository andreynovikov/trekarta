package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.StringFormatter;

public class TrackInformation extends Fragment {
    private FragmentHolder mFragmentHolder;
    private OnTrackActionListener mListener;

    private Track mTrack;
    //private Drawable fabDrawable;
    //private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_information, container, false);
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
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
        fab = mFragmentHolder.enableActionButton();
        fabDrawable = fab.getDrawable();
        fab.setImageResource(R.drawable.ic_visibility_white_24dp);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTrackView(mTrack);
            }
        });
        */
    }

    @Override
    public void onPause() {
        super.onPause();

        //fab.setImageDrawable(fabDrawable);
        //mFragmentHolder.disableActionButton();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.track_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        //menu.findItem(R.id.action_view).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        switch (item.getItemId()) {
            case R.id.action_edit:
                mListener.onTrackEdit(mTrack);
                return true;
            case R.id.action_edit_path:
                mListener.onTrackEditPath(mTrack);
                return true;
            case R.id.action_track_to_route:
                mListener.onTrackToRoute(mTrack);
                return true;
            case R.id.action_save:
                mListener.onTrackSave(mTrack);
                return true;
            case R.id.action_remove:
                Androzic application = Androzic.getApplication();
                application.removeTrack(mTrack);
                // "Close" fragment
                getFragmentManager().popBackStack();
                return true;
        }
        */
        return false;
    }

    public void setTrack(Track track) {
        this.mTrack = track;
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
        if (mTrack.source.isNativeTrack()) {
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
}
