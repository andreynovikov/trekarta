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
        this.tickMarkCount = tickMarkCount < 2 ? 2 : tickMarkCount;
    }

    public void setTickMarkRadius(float tickMarkRadius) {
        this.tickMarkRadius = tickMarkRadius < 2.0F ? 2.0F : tickMarkRadius;
    }

    public void setHorizontalBarThickness(float horizontalBarThickness) {
        this.horizontalBarThickness = horizontalBarThickness < 2.0F ? 2.0F : horizontalBarThickness;
    }

    public void setBackdropFillColor(int backdropFillColor) {
        this.backdropFillColor = backdropFillColor;
    }

    public void setBackdropStrokeColor(int backdropStrokeColor) {
        this.backdropStrokeColor = backdropStrokeColor;
    }

    public void setBackdropStrokeWidth(float backdropStrokeWidth) {
        this.backdropStrokeWidth = backdropStrokeWidth < 1.0F ? 1.0F : backdropStrokeWidth;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }
}
