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

package mobi.maptrek.view;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;

/**
 * Wrapping is based on https://github.com/blazsolar/FlowLayout
 */
//TODO Redesign to balance gauge quantity in columns
public class GaugePanel extends ViewGroup implements View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, SensorEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GaugePanel.class);

    public static final String DEFAULT_GAUGE_SET = Gauge.TYPE_SPEED + "," + Gauge.TYPE_DISTANCE;

    private final List<List<View>> mLines = new ArrayList<>();
    private final List<Integer> mLineWidths = new ArrayList<>();

    private final ArrayList<Gauge> mGauges = new ArrayList<>();
    private final SparseArray<Gauge> mGaugeMap = new SparseArray<>();
    private MapHolder mMapHolder;
    private boolean mNavigationMode = false;
    private List<View> mLineViewsBuffer = new ArrayList<>();
    private SensorManager mSensorManager;
    private Sensor mPressureSensor;
    private boolean mVisible;
    private boolean mHasSensors;

    public GaugePanel(Context context) {
        super(context);
    }

    public GaugePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GaugePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;
        int maxWidth = 0;

        int lineWidth = 0;
        int lineHeight = 0;

        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            boolean lastChild = i == childCount - 1;

            if (child.getVisibility() == View.GONE) {
                if (lastChild) {
                    width += lineWidth;
                    height = Math.max(height, lineHeight);
                }
                continue;
            }

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            LayoutParams lp = child.getLayoutParams();

            int childWidthMode = MeasureSpec.AT_MOST;
            int childWidthSize = sizeWidth;

            int childHeightMode = MeasureSpec.AT_MOST;
            int childHeightSize = sizeHeight;

            if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthMode = MeasureSpec.EXACTLY;
            } else if (lp.width >= 0) {
                childWidthMode = MeasureSpec.EXACTLY;
                childWidthSize = lp.width;
            } else if (modeWidth == MeasureSpec.UNSPECIFIED) {
                childWidthMode = MeasureSpec.UNSPECIFIED;
                childWidthSize = 0;
            }

            if (lp.height == LayoutParams.MATCH_PARENT) {
                childWidthMode = MeasureSpec.EXACTLY;
            } else if (lp.height >= 0) {
                childHeightMode = MeasureSpec.EXACTLY;
                childHeightSize = lp.height;
            } else if (modeHeight == MeasureSpec.UNSPECIFIED) {
                childHeightMode = MeasureSpec.UNSPECIFIED;
                childHeightSize = 0;
            }

            child.measure(
                    MeasureSpec.makeMeasureSpec(childWidthSize, childWidthMode),
                    MeasureSpec.makeMeasureSpec(childHeightSize, childHeightMode)
            );

            int childWidth = child.getMeasuredWidth();
            if (maxWidth < childWidth)
                maxWidth = childWidth;

            int childHeight = child.getMeasuredHeight();

            if (lineHeight + childHeight > sizeHeight) {
                height = Math.max(height, lineHeight);
                lineHeight = childHeight;
                width += lineWidth;
                lineWidth = child.getMeasuredWidth();
            } else {
                lineHeight += childHeight;
                lineWidth = Math.max(lineWidth, child.getMeasuredWidth());
            }

            if (lastChild) {
                height = Math.max(height, lineHeight);
                width += lineWidth;
            }
        }

        // set all children width to most wide one
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childHeight = child.getMeasuredHeight();
                child.measure(
                        MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                );
            }
        }

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(
                (modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mLines.clear();
        mLineWidths.clear();

        int width = getWidth();
        int height = getHeight();

        int childCount = getChildCount();

        int linesSum = getPaddingTop();

        int lineWidth = 0;
        int lineHeight = 0;
        mLineViewsBuffer.clear();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (lineHeight + childHeight > height) {
                mLineWidths.add(lineWidth);
                mLines.add(mLineViewsBuffer);

                linesSum += lineWidth;

                lineHeight = 0;
                lineWidth = 0;
                mLineViewsBuffer.clear();
            }

            lineHeight += childHeight;
            lineWidth = Math.max(lineWidth, childWidth);
            mLineViewsBuffer.add(child);
        }

        mLineWidths.add(lineWidth);
        mLines.add(mLineViewsBuffer);

        linesSum += lineWidth;

        int horizontalGravityMargin = width - linesSum;
        int numLines = mLines.size();
        int top;
        int left = getPaddingLeft();

        for (int i = 0; i < numLines; i++) {
            lineWidth = mLineWidths.get(i);
            mLineViewsBuffer = mLines.get(i);
            top = getPaddingTop();
            int children = mLineViewsBuffer.size();

            for (int j = 0; j < children; j++) {
                View child = mLineViewsBuffer.get(j);

                if (child.getVisibility() == View.GONE)
                    continue;

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                int gravityMargin = lineWidth - childWidth;

                child.layout(left + gravityMargin + horizontalGravityMargin,
                        top,
                        left + childWidth + gravityMargin + horizontalGravityMargin,
                        top + childHeight);

                top += childHeight;
            }

            left += lineWidth;
        }
    }

    public void initializeGauges(String settings) {
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        String[] gauges = settings.split(",");
        for (String gaugeStr : gauges) {
            int type = Integer.parseInt(gaugeStr);
            addGauge(type);
        }
        setNavigationMode(false);
    }

    public void setMapHolder(MapHolder mapHolder) {
        mMapHolder = mapHolder;
    }

    private String getGaugeName(int type) {
        Context context = getContext();
        switch (type) {
            case Gauge.TYPE_SPEED:
                return context.getString(R.string.gauge_speed);
            case Gauge.TYPE_TRACK:
                return context.getString(R.string.gauge_track);
            case Gauge.TYPE_ALTITUDE:
                return context.getString(R.string.gauge_altitude);
            case Gauge.TYPE_DISTANCE:
                return context.getString(R.string.gauge_distance);
            case Gauge.TYPE_ELEVATION:
                return context.getString(R.string.gauge_elevation);
            case Gauge.TYPE_BEARING:
                return context.getString(R.string.gauge_bearing);
            case Gauge.TYPE_TURN:
                return context.getString(R.string.gauge_turn);
            case Gauge.TYPE_VMG:
                return context.getString(R.string.gauge_vmg);
            case Gauge.TYPE_XTK:
                return context.getString(R.string.gauge_xtk);
            case Gauge.TYPE_ETE:
                return context.getString(R.string.gauge_ete);
            default:
                return "";
        }
    }

    private void addGauge(int type) {
        Gauge gauge = new Gauge(getContext(), type);
        gauge.setValue(Float.NaN);
        if (isNavigationGauge(type)) {
            addView(gauge);
            if (!mNavigationMode)
                gauge.setVisibility(GONE);
            mGauges.add(gauge);
        } else {
            int i = 0;
            while (i < mGauges.size() && !isNavigationGauge(mGauges.get(i).getType())) i++;
            addView(gauge, i);
            mGauges.add(i, gauge);
        }
        mGaugeMap.put(type, gauge);
        updateAbbrVisibility();

        mHasSensors = mGaugeMap.get(Gauge.TYPE_ELEVATION) != null;
        if (type == Gauge.TYPE_ELEVATION && mPressureSensor != null && mVisible)
            mSensorManager.registerListener(this, mPressureSensor, SensorManager.SENSOR_DELAY_NORMAL, 1000);

        gauge.setOnLongClickListener(this);
    }

    private void removeGauge(int type) {
        Gauge gauge = mGaugeMap.get(type);
        removeView(gauge);
        mGauges.remove(gauge);
        mGaugeMap.remove(type);
        updateAbbrVisibility();

        mHasSensors = mGaugeMap.get(Gauge.TYPE_ELEVATION) != null;
        if (type == Gauge.TYPE_ELEVATION && !mHasSensors && mVisible)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        Context context = getContext();
        PopupMenu popup = new PopupMenu(context, v);
        Menu menu = popup.getMenu();
        int type = 0;
        if (v instanceof Gauge) {
            Gauge gauge = (Gauge) v;
            menu.add(0, gauge.getType(), Menu.NONE, context.getString(R.string.remove_gauge, getGaugeName(gauge.getType())));
        }
        ArrayList<Integer> availableGauges = getAvailableGauges(type);
        for (int availableGauge : availableGauges) {
            menu.add(0, availableGauge, Menu.NONE, context.getString(R.string.add_gauge, getGaugeName(availableGauge)));
        }
        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        TransitionManager.beginDelayedTransition(this);
        int type = item.getItemId();
        if (mGaugeMap.indexOfKey(type) >= 0)
            removeGauge(type);
        else
            addGauge(type);
        if (mMapHolder != null)
            mMapHolder.updateMapViewArea();
        return true;
    }

    public void setValue(int type, float value) {
        Gauge gauge = mGaugeMap.get(type);
        if (gauge == null)
            return;
        gauge.setValue(value);
    }


    public void refreshGauges() {
        for (Gauge gauge : mGauges)
            gauge.refresh();
    }

    public void onVisibilityChanged(boolean visible) {
        if (visible == mVisible)
            return;
        mVisible = visible;
        if (!mHasSensors)
            return;
        if (mVisible) {
            if (mPressureSensor != null)
                mSensorManager.registerListener(this, mPressureSensor, SensorManager.SENSOR_DELAY_NORMAL, 1000);
        } else {
            mSensorManager.unregisterListener(this);
        }
    }

    public boolean setNavigationMode(boolean mode) {
        if (mNavigationMode == mode)
            return false;
        mNavigationMode = mode;
        int visibility = mode ? View.VISIBLE : View.GONE;
        TransitionManager.beginDelayedTransition(this);
        for (Gauge gauge : mGauges) {
            if (isNavigationGauge(gauge.getType()))
                gauge.setVisibility(visibility);
        }
        updateAbbrVisibility();
        return true;
    }

    private void updateAbbrVisibility() {
        boolean hasSameUnit = false;
        HashSet<String> units = new HashSet<>();
        for (Gauge g : mGauges) {
            String unit = g.getDefaultGaugeUnit();
            if (g.getVisibility() == View.VISIBLE && units.contains(unit)) {
                hasSameUnit = true;
                break;
            }
            units.add(unit);
        }

        for (Gauge g : mGauges)
            g.enableAbbr(hasSameUnit);
    }

    private boolean isNavigationGauge(int type) {
        return type > 0x9999;
    }

    @NonNull
    private ArrayList<Integer> getAvailableGauges(int type) {
        ArrayList<Integer> gauges = new ArrayList<>();
        gauges.add(Gauge.TYPE_SPEED);
        gauges.add(Gauge.TYPE_TRACK);
        gauges.add(Gauge.TYPE_ALTITUDE);
        if (mPressureSensor != null)
            gauges.add(Gauge.TYPE_ELEVATION);
        if (mNavigationMode) {
            gauges.add(Gauge.TYPE_DISTANCE);
            gauges.add(Gauge.TYPE_BEARING);
            gauges.add(Gauge.TYPE_TURN);
            gauges.add(Gauge.TYPE_XTK);
            gauges.add(Gauge.TYPE_VMG);
            // gauges.add(Gauge.TYPE_ETE);
        }
        for (int i = 0; i < mGaugeMap.size(); i++) {
            int gauge = mGaugeMap.keyAt(i);
            gauges.remove(Integer.valueOf(gauge));
        }
        gauges.remove(Integer.valueOf(type));
        return gauges;
    }

    public String getGaugeSettings() {
        String[] gauges = new String[mGauges.size()];
        for (int i = 0; i < gauges.length; i++)
            gauges[i] = String.valueOf(mGauges.get(i).getType());
        return TextUtils.join(",", gauges);
    }

    public boolean hasVisibleGauges() {
        for (Gauge gauge : mGauges)
            if (gauge.getVisibility() == View.VISIBLE)
                return true;
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT || event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                setValue(Gauge.TYPE_ELEVATION, Float.NaN);
            } else {
                // https://en.wikipedia.org/wiki/Pressure_altitude (converted to meters)
                float elevation = (float) ((1 - Math.pow(event.values[0] / SensorManager.PRESSURE_STANDARD_ATMOSPHERE, 0.190284)) * 145366.45 / 3.281);
                setValue(Gauge.TYPE_ELEVATION, elevation);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_PRESSURE) {
            if (accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT || accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                setValue(Gauge.TYPE_ELEVATION, Float.NaN);
        }
    }
}
