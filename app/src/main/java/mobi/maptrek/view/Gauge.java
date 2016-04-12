package mobi.maptrek.view;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;

public class Gauge extends RelativeLayout {
    public static final int TYPE_SPEED = 0x00001;
    public static final int TYPE_TRACK = 0x00002;
    public static final int TYPE_ALTITUDE = 0x00004;
    public static final int TYPE_DISTANCE = 0x10000;
    public static final int TYPE_BEARING = 0x20000;

    private int mType;
    private TextView mValue;

    public Gauge(Context context) {
        super(context);
    }

    public Gauge(Context context, int type, String unit) {
        super(context);
        mType = type;
        inflate(getContext(), R.layout.gauge, this);
        mValue = (TextView) findViewById(R.id.gaugeValue);
        TextView unitView = (TextView) findViewById(R.id.gaugeUnit);
        unitView.setText(unit);
    }

    public int getType() {
        return mType;
    }

    public void setValue(int value) {
        mValue.setText(String.valueOf(value));
    }

    public void setValue(float value) {
        mValue.setText(String.format(StringFormatter.precisionFormat, value));
    }
}