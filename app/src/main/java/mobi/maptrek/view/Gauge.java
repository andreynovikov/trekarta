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
    public static final int TYPE_ELEVATION = 0x01000;
    public static final int TYPE_DISTANCE = 0x10000;
    public static final int TYPE_BEARING = 0x20000;
    public static final int TYPE_TURN = 0x40000;
    public static final int TYPE_VMG = 0x80000;
    public static final int TYPE_XTK = 0x100000;
    public static final int TYPE_ETE = 0x200000;

    private int mType;
    private TextView mValue;
    private TextView mUnit;

    public Gauge(Context context) {
        super(context);
    }

    public Gauge(Context context, int type, String unit) {
        super(context);
        mType = type;
        inflate(getContext(), R.layout.gauge, this);
        mValue = (TextView) findViewById(R.id.gaugeValue);
        mUnit = (TextView) findViewById(R.id.gaugeUnit);
        mUnit.setText(unit);
    }

    public int getType() {
        return mType;
    }

    public void setUnit(String unit) {
        mUnit.setText(unit);
    }

    public void setValue(int value) {
        mValue.setText(String.valueOf(value));
    }

    public void setValue(float value) {
        mValue.setText(String.format(StringFormatter.precisionFormat, value));
    }

    public void setValue(String value) {
        mValue.setText(value);
    }
}