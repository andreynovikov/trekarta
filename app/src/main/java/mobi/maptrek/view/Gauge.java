/*
 * Copyright 2018 Andrey Novikov
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

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
    private TextView mValueView;
    private TextView mUnitView;

    private float mValue;

    public Gauge(Context context) {
        super(context);
    }

    public Gauge(Context context, int type) {
        super(context);
        mType = type;
        inflate(getContext(), R.layout.gauge, this);
        mValueView = findViewById(R.id.gaugeValue);
        mUnitView = findViewById(R.id.gaugeUnit);
        mUnitView.setText(getDefaultGaugeUnit(type));
    }

    public int getType() {
        return mType;
    }

    public void setValue(float value) {
        mValue = value;
        String indication;
        String unit = null;
        switch (mType) {
            case Gauge.TYPE_SPEED:
            case Gauge.TYPE_VMG: {
                indication = StringFormatter.speedC(value);
                break;
            }
            case Gauge.TYPE_DISTANCE:
            case Gauge.TYPE_XTK: {
                String[] indications = StringFormatter.distanceC(value);
                indication = indications[0];
                unit = indications[1];
                break;
            }
            case Gauge.TYPE_ELEVATION:
            case Gauge.TYPE_ALTITUDE: {
                indication = StringFormatter.elevationC(value);
                break;
            }
            case Gauge.TYPE_TRACK:
            case Gauge.TYPE_BEARING:
            case Gauge.TYPE_TURN: {
                indication = StringFormatter.angleC(value);
                break;
            }
            default:
                indication = String.format(Locale.getDefault(), StringFormatter.precisionFormat, value);
        }

        mValueView.setText(indication);
        if (unit != null)
            mUnitView.setText(unit);
    }

    public void refresh() {
        mUnitView.setText(getDefaultGaugeUnit(mType));
        setValue(mValue);
    }

    private String getDefaultGaugeUnit(int type) {
        switch (type) {
            case Gauge.TYPE_SPEED:
            case Gauge.TYPE_VMG:
                return StringFormatter.speedAbbr;
            case Gauge.TYPE_TRACK:
            case Gauge.TYPE_BEARING:
            case Gauge.TYPE_TURN:
                return StringFormatter.angleAbbr;
            case Gauge.TYPE_DISTANCE:
            case Gauge.TYPE_XTK:
                return StringFormatter.distanceAbbr;
            case Gauge.TYPE_ETE:
                return StringFormatter.minuteAbbr;
            case Gauge.TYPE_ALTITUDE:
            case Gauge.TYPE_ELEVATION:
                return StringFormatter.elevationAbbr;
            default:
                return "";
        }
    }
}