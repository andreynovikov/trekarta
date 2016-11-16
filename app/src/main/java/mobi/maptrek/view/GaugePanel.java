package mobi.maptrek.view;

import android.content.Context;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;

/**
 * Wrapping is based on https://github.com/blazsolar/FlowLayout
 */
//TODO Redesign to balance gauge quantity in columns
public class GaugePanel extends ViewGroup implements View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    public static final String DEFAULT_GAUGE_SET = Gauge.TYPE_SPEED + "," + Gauge.TYPE_DISTANCE;

    private final List<List<View>> mLines = new ArrayList<>();
    private final List<Integer> mLineWidths = new ArrayList<>();

    private ArrayList<Gauge> mGauges = new ArrayList<>();
    private HashMap<Integer, Gauge> mGaugeMap = new HashMap<>();
    private MapHolder mMapHolder;
    private boolean mNavigationMode = false;

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

            if (lp.width == LayoutParams.MATCH_PARENT) {
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

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(
                (modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height);
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
        List<View> lineViews = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (lineHeight + childHeight > height) {
                mLineWidths.add(lineWidth);
                mLines.add(lineViews);

                linesSum += lineWidth;

                lineHeight = 0;
                lineWidth = 0;
                lineViews = new ArrayList<>();
            }

            lineHeight += childHeight;
            lineWidth = Math.max(lineWidth, childWidth);
            lineViews.add(child);
        }

        mLineWidths.add(lineWidth);
        mLines.add(lineViews);

        linesSum += lineWidth;

        int horizontalGravityMargin = width - linesSum;
        int numLines = mLines.size();
        int top;
        int left = getPaddingLeft();

        for (int i = 0; i < numLines; i++) {
            lineWidth = mLineWidths.get(i);
            lineViews = mLines.get(i);
            top = getPaddingTop();
            int children = lineViews.size();

            for (int j = 0; j < children; j++) {
                View child = lineViews.get(j);

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
        String[] gauges = settings.split(",");
        for (String gaugeStr : gauges) {
            int type = Integer.valueOf(gaugeStr);
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

    private String getGaugeUnit(int type) {
        switch (type) {
            case Gauge.TYPE_SPEED:
            case Gauge.TYPE_VMG:
                return "kmh";
            case Gauge.TYPE_TRACK:
            case Gauge.TYPE_BEARING:
            case Gauge.TYPE_TURN:
                return "deg";
            case Gauge.TYPE_ALTITUDE:
            case Gauge.TYPE_DISTANCE:
            case Gauge.TYPE_XTK:
                return "m";
            case Gauge.TYPE_ETE:
                return "min";
            default:
                return "";
        }
    }

    private void addGauge(int type) {
        Gauge gauge = new Gauge(getContext(), type, getGaugeUnit(type));
        if (isNavigationGauge(type)) {
            addView(gauge);
            mGauges.add(gauge);
        } else {
            int i = 0;
            while (i < mGauges.size() && !isNavigationGauge(mGauges.get(i).getType())) i++;
            addView(gauge, i);
            mGauges.add(i, gauge);
        }
        mGaugeMap.put(type, gauge);
        gauge.setGravity(Gravity.END | Gravity.TOP);
        gauge.setOnLongClickListener(this);
    }

    private void removeGauge(int type) {
        Gauge gauge = mGaugeMap.get(type);
        removeView(gauge);
        mGauges.remove(gauge);
        mGaugeMap.remove(type);
    }

    @Override
    public boolean onLongClick(View v) {
        if (!(v instanceof Gauge))
            return false;
        Gauge gauge = (Gauge) v;
        Context context = getContext();
        PopupMenu popup = new PopupMenu(context, v);
        Menu menu = popup.getMenu();
        menu.add(0, gauge.getType(), Menu.NONE, context.getString(R.string.remove_gauge, getGaugeName(gauge.getType())));
        ArrayList<Integer> availableGauges = getAvailableGauges(gauge.getType());
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
        if (mGaugeMap.containsKey(type))
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
        switch (type) {
            case Gauge.TYPE_SPEED: {
                String indication = StringFormatter.speedC(value);
                gauge.setValue(indication);
                gauge.setUnit(StringFormatter.speedAbbr);
                break;
            }
            case Gauge.TYPE_DISTANCE: {
                String[] indication = StringFormatter.distanceC(value);
                gauge.setValue(indication[0]);
                gauge.setUnit(indication[1]);
                break;
            }
            default:
                gauge.setValue(value);
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
        return true;
    }

    private boolean isNavigationGauge(int type) {
        return type > 0x9999;
    }

    private ArrayList<Integer> getAvailableGauges(int type) {
        ArrayList<Integer> gauges = new ArrayList<>();
        gauges.add(Gauge.TYPE_SPEED);
        gauges.add(Gauge.TYPE_TRACK);
        gauges.add(Gauge.TYPE_ALTITUDE);
        if (mNavigationMode) {
            gauges.add(Gauge.TYPE_DISTANCE);
            gauges.add(Gauge.TYPE_BEARING);
            gauges.add(Gauge.TYPE_TURN);
        }
        for (int gauge : mGaugeMap.keySet())
            gauges.remove(Integer.valueOf(gauge));
        gauges.remove(Integer.valueOf(type));
        return gauges;
    }

    public String getGaugeSettings() {
        String[] gauges = new String[mGauges.size()];
        for (int i = 0; i < gauges.length; i++)
            gauges[i] = String.valueOf(mGauges.get(i).getType());
        return TextUtils.join(",", gauges);
    }
}
