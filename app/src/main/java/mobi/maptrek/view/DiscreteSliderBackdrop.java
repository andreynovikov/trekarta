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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import mobi.maptrek.MapTrek;

/**
 * Originally created by etiennelawlor on 7/4/16.
 */

public class DiscreteSliderBackdrop extends FrameLayout {
    private Paint fillPaint = new Paint();
    private Paint strokePaint = new Paint();
    private int tickMarkCount = 0;
    private float tickMarkRadius = 0.0F;
    private float horizontalBarThickness = 0.0F;
    private int backdropFillColor = 0;
    private int backdropStrokeColor = 0;
    private float backdropStrokeWidth = 0.0F;
    private int xRadius = (int) (MapTrek.density * 6);
    private int yRadius = (int) (MapTrek.density * 6);
    private float padding;

    public DiscreteSliderBackdrop(Context context) {
        super(context);
    }

    public DiscreteSliderBackdrop(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DiscreteSliderBackdrop(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float interval = (width - padding * 2) / (tickMarkCount-1);

        setUpFillPaint();
        setUpStrokePaint();

        canvas.drawRoundRect(padding,
                        (height/2) - (horizontalBarThickness/2),
                        width - padding,
                        (height/2) + (horizontalBarThickness/2),
                        xRadius,
                        yRadius,
                        fillPaint);

        canvas.drawRoundRect(padding,
                        (height/2) - (horizontalBarThickness/2),
                        width - padding,
                        (height/2) + (horizontalBarThickness/2),
                        xRadius,
                        yRadius,
                        strokePaint);

        for(int i=0; i<tickMarkCount; i++){
            canvas.drawCircle(padding + (i * interval), height/2, tickMarkRadius, fillPaint);
            canvas.drawCircle(padding + (i * interval), height/2, tickMarkRadius, strokePaint);
        }

        canvas.drawRoundRect(padding,
                        (height/2) - ((horizontalBarThickness/2)-MapTrek.density),
                        width - padding,
                        (height/2) + ((horizontalBarThickness/2)-MapTrek.density),
                        xRadius,
                        yRadius,
                        fillPaint);
    }

    private void setUpFillPaint(){
        fillPaint.setColor(backdropFillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
    }

    private void setUpStrokePaint(){
        strokePaint.setColor(backdropStrokeColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(backdropStrokeWidth);
    }

    public void setTickMarkCount(int tickMarkCount) {
        this.tickMarkCount = Math.max(tickMarkCount, 2);
    }

    public void setTickMarkRadius(float tickMarkRadius) {
        this.tickMarkRadius = Math.max(tickMarkRadius, 2.0F);
    }

    public void setHorizontalBarThickness(float horizontalBarThickness) {
        this.horizontalBarThickness = Math.max(horizontalBarThickness, 2.0F);
    }

    public void setBackdropFillColor(int backdropFillColor) {
        this.backdropFillColor = backdropFillColor;
    }

    public void setBackdropStrokeColor(int backdropStrokeColor) {
        this.backdropStrokeColor = backdropStrokeColor;
    }

    public void setBackdropStrokeWidth(float backdropStrokeWidth) {
        this.backdropStrokeWidth = Math.max(backdropStrokeWidth, 1.0F);
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }
}
