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
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;

public class Gauge extends ConstraintLayout {
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
    private TextView mNameView;

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
        mUnitView.setText(getDefaultGaugeUnit());
        mNameView = findViewById(R.id.gaugeName);
        mNameView.setText(getGaugeAbbr());
    }

    public int getType() {
        return mType;
    }

    public void setValue(float value) {
        if (value == mValue)
            return;

        mValue = value;
        String unit = getDefaultGaugeUnit();
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            mValueView.setText("-");
            mUnitView.setText(unit);
            return;
        }

        String indication;
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

        if (!mValueView.getText().equals(indication))
            mValueView.setText(indication);
        if (!mUnitView.getText().equals(unit))
            mUnitView.setText(unit);
    }

    public void refresh() {
        setValue(mValue);
    }

    public void enableAbbr(boolean enable) {
        mNameView.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    public String getDefaultGaugeUnit() {
        switch (mType) {
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

    private String getGaugeAbbr() {
        Context context = getContext();
        switch (mType) {
            case Gauge.TYPE_SPEED:
                return context.getString(R.string.gauge_speed_abbr);
            case Gauge.TYPE_TRACK:
                return context.getString(R.string.gauge_track_abbr);
            case Gauge.TYPE_ALTITUDE:
                return context.getString(R.string.gauge_altitude_abbr);
            case Gauge.TYPE_DISTANCE:
                return context.getString(R.string.gauge_distance_abbr);
            case Gauge.TYPE_ELEVATION:
                return context.getString(R.string.gauge_elevation_abbr);
            case Gauge.TYPE_BEARING:
                return context.getString(R.string.gauge_bearing_abbr);
            case Gauge.TYPE_TURN:
                return context.getString(R.string.gauge_turn_abbr);
            case Gauge.TYPE_VMG:
                return context.getString(R.string.gauge_vmg_abbr);
            case Gauge.TYPE_XTK:
                return context.getString(R.string.gauge_xtk_abbr);
            case Gauge.TYPE_ETE:
                return context.getString(R.string.gauge_ete_abbr);
            default:
                return "";
        }
    }
}