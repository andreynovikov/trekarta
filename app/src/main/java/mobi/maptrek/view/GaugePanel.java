package mobi.maptrek.view;

import android.content.Context;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;

public class GaugePanel extends LinearLayout implements View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    public static final String DEFAULT_GAUGE_SET = Gauge.TYPE_SPEED + "," + Gauge.TYPE_DISTANCE;

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
            while (i < mGauges.size() && ! isNavigationGauge(mGauges.get(i).getType())) i++;
            addView(gauge, i);
            mGauges.add(i, gauge);
        }
        mGaugeMap.put(type, gauge);
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
        if (gauge != null)
            gauge.setValue(value);
    }

    public void setNavigationMode(boolean mode) {
        mNavigationMode = mode;
        int visibility = mode ? View.VISIBLE : View.GONE;
        TransitionManager.beginDelayedTransition(this);
        for (Gauge gauge : mGauges) {
            if (isNavigationGauge(gauge.getType()))
                gauge.setVisibility(visibility);
        }
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
