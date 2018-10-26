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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import mobi.maptrek.R;

/**
 * Originally created by etiennelawlor on 7/4/16.
 */

public class DiscreteSlider extends FrameLayout {

    private DiscreteSliderBackdrop discreteSliderBackdrop;
    private DiscreteSeekBar discreteSeekBar;

    private int tickMarkCount;
    private float tickMarkRadius;
    private int position;
    private OnDiscreteSliderChangeListener onDiscreteSliderChangeListener;

    public interface OnDiscreteSliderChangeListener {
        void onPositionChanged(int position);
    }

    public DiscreteSlider(Context context) {
        super(context);
        init(context, null);
    }

    public DiscreteSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DiscreteSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributeArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.DiscreteSlider);

        float horizontalBarThickness;
        int backdropFillColor;
        int backdropStrokeColor;
        float backdropStrokeWidth;
        Drawable thumb;
        Drawable progressDrawable;
        try {
            tickMarkCount = attributeArray.getInteger(R.styleable.DiscreteSlider_tickMarkCount, 5);
            tickMarkRadius = attributeArray.getDimension(R.styleable.DiscreteSlider_tickMarkRadius, 8);
            position = attributeArray.getInteger(R.styleable.DiscreteSlider_position, 0);
            horizontalBarThickness = attributeArray.getDimension(R.styleable.DiscreteSlider_horizontalBarThickness, 4);
            backdropFillColor = attributeArray.getColor(R.styleable.DiscreteSlider_backdropFillColor, Color.GRAY);
            backdropStrokeColor = attributeArray.getColor(R.styleable.DiscreteSlider_backdropStrokeColor, Color.GRAY);
            backdropStrokeWidth = attributeArray.getDimension(R.styleable.DiscreteSlider_backdropStrokeWidth, 1);
            thumb = attributeArray.getDrawable(R.styleable.DiscreteSlider_thumb);
            progressDrawable = attributeArray.getDrawable(R.styleable.DiscreteSlider_progressDrawable);
        } finally {
            attributeArray.recycle();
        }

        View view = inflate(context, R.layout.discrete_slider, this);
        discreteSliderBackdrop = view.findViewById(R.id.discrete_slider_backdrop);
        discreteSeekBar = view.findViewById(R.id.discrete_seek_bar);

        setTickMarkCount(tickMarkCount);
        setTickMarkRadius(tickMarkRadius);
        setHorizontalBarThickness(horizontalBarThickness);
        setBackdropFillColor(backdropFillColor);
        setBackdropStrokeColor(backdropStrokeColor);
        setBackdropStrokeWidth(backdropStrokeWidth);
        setPosition(position);
        setThumb(thumb, backdropStrokeWidth);
        setProgressDrawable(progressDrawable);

        discreteSeekBar.setOnDiscreteSeekBarChangeListener(position -> {
            if (onDiscreteSliderChangeListener != null) {
                onDiscreteSliderChangeListener.onPositionChanged(position);
                setPosition(position);
            }
        });
    }

    public void setTickMarkCount(int tickMarkCount) {
        this.tickMarkCount = tickMarkCount;
        discreteSliderBackdrop.setTickMarkCount(tickMarkCount);
        discreteSliderBackdrop.invalidate();
        discreteSeekBar.setTickMarkCount(tickMarkCount);
    }

    public void setTickMarkRadius(float tickMarkRadius) {
        this.tickMarkRadius = tickMarkRadius;
        discreteSliderBackdrop.setTickMarkRadius(tickMarkRadius);
        discreteSliderBackdrop.invalidate();
    }

    public void setPosition(int position) {
        if (position < 0) {
            this.position = 0;
        } else if (position > tickMarkCount - 1) {
            this.position = tickMarkCount - 1;
        } else {
            this.position = position;
        }
        discreteSeekBar.setPosition(this.position);
        int color = getContext().getColor(position == 0 ? R.color.textColorSecondary : R.color.colorAccent);
        discreteSeekBar.getThumb().setTint(color);
    }

    public void setHorizontalBarThickness(float horizontalBarThickness) {
        discreteSliderBackdrop.setHorizontalBarThickness(horizontalBarThickness);
        discreteSliderBackdrop.invalidate();
    }

    public void setBackdropFillColor(int backdropFillColor) {
        discreteSliderBackdrop.setBackdropFillColor(backdropFillColor);
        discreteSliderBackdrop.invalidate();
    }

    public void setBackdropStrokeColor(int backdropStrokeColor) {
        discreteSliderBackdrop.setBackdropStrokeColor(backdropStrokeColor);
        discreteSliderBackdrop.invalidate();
    }

    public void setBackdropStrokeWidth(float backdropStrokeWidth) {
        discreteSliderBackdrop.setBackdropStrokeWidth(backdropStrokeWidth);
        discreteSliderBackdrop.invalidate();
    }

    public void setThumb(Drawable thumb, float backdropStrokeWidth) {
        if (thumb != null)
            discreteSeekBar.setThumb(thumb);

        Rect bounds = discreteSeekBar.getThumb().getBounds();
        float padding = backdropStrokeWidth + bounds.width() / 2;
        discreteSeekBar.setPadding((int) padding,0,(int) padding,0);
        discreteSliderBackdrop.setPadding(padding);
    }

    public void setProgressDrawable(Drawable progressDrawable) {
        if (progressDrawable != null)
            discreteSeekBar.setProgressDrawable(progressDrawable);
    }

    public void setOnDiscreteSliderChangeListener(OnDiscreteSliderChangeListener onDiscreteSliderChangeListener) {
        this.onDiscreteSliderChangeListener = onDiscreteSliderChangeListener;
    }

    public int getPosition() {
        return position;
    }
}
