/*
 * Copyright 2019 Andrey Novikov
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.oscim.android.canvas.AndroidPaint;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.CircleStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;

import mobi.maptrek.MapTrek;

public class LegendView extends View {
    private static float COORD_SCALE = .8f;

    private LegendItem mItem;
    private int mBackground;
    private RenderStyle[] mStyle;
    private float mLeft;
    private float mRight;
    private float mTop;
    private float mBottom;
    private float mCenterX;
    private float mCenterY;
    private float mLastLineWidth;
    private int mSymbolCount;

    public LegendView(Context context) {
        super(context);
    }

    public LegendView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LegendView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLegend(LegendItem item, int background, RenderStyle[] style) {
        mItem = item;
        mBackground = background;
        mStyle = style;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mLeft = getPaddingLeft();
        mRight = getPaddingRight();
        mTop = getPaddingTop();
        mBottom = getPaddingBottom();

        mCenterX = (w - mLeft - mRight) / 2f + mLeft;
        mCenterY = (h - mTop - mBottom) / 2f + mTop;

        mRight = w - mRight;
        mBottom = h - mBottom;
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mBackground);
        // Draw the label text
        //canvas.drawText(mData.get(mCurrentItem).mLabel, mTextX, mTextY, mTextPaint);

        // Draw the pointer
        //canvas.drawLine(mTextX, mPointerY, mPointerX, mPointerY, mTextPaint);
        //canvas.drawCircle(mCenterX, mCenterY, 20, mCirclePaint);

        if (mStyle == null)
            return;

        mSymbolCount = 1;
        Log.e("I", mItem.name);
        for (RenderStyle style : mStyle) {
            Log.e("S", style.getClass().getName());
            if (style instanceof AreaStyle)
                renderArea(canvas, (AreaStyle) style);
            else if (style instanceof CircleStyle)
                renderCircle(canvas, (CircleStyle) style);
            else if (style instanceof SymbolStyle)
                renderSymbol(canvas, (SymbolStyle) style);
            else if (style instanceof LineStyle)
                renderLine(canvas, (LineStyle) style);
            else if (style instanceof TextStyle)
                renderText(canvas, (TextStyle) style);
        }
    }

    void renderArea(Canvas canvas, AreaStyle area) {
        Log.e("A", "" + area.style);
        Log.e("A", "" + area.color);
        Log.e("A", "" + area.strokeWidth);
        Log.e("A", "" + area.strokeColor);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(area.color);
        canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
        if (area.strokeWidth != 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(area.strokeWidth * MapTrek.density * .25f);
            paint.setColor(area.strokeColor);
            canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
        }
    }

    void renderCircle(Canvas canvas, CircleStyle circle) {

    }

    void renderSymbol(Canvas canvas, SymbolStyle symbol) {
        if (mItem.totalSymbols == 0)
            return;
        float x = mItem.totalSymbols > 1 ? mLeft + (mRight - mLeft) / (mItem.totalSymbols + 1) * mSymbolCount : mCenterX;
        mSymbolCount++;
        canvas.drawBitmap(symbol.bitmap.getPixels(), 0, symbol.bitmap.getWidth(),
                x - symbol.bitmap.getWidth() / 2f,
                mCenterY - symbol.bitmap.getHeight() / 2f,
                symbol.bitmap.getWidth(), symbol.bitmap.getHeight(), true, null);
    }

    void renderLine(Canvas canvas, LineStyle line) {
        Log.e("L", "" + line.style);
        Log.e("L", "" + line.color);
        Log.e("L", "" + line.width);
        Log.e("L", "" + line.fixed);
        Log.e("L", "" + line.stipple);
        Log.e("L", "" + line.stippleWidth);
        Log.e("L", "" + line.stippleColor);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(line.fixed ? line.width * MapTrek.density * .6f : line.width * MapTrek.density * 8f);
        paint.setColor(line.color);
        if (line.texture != null) {
            // XMLThemeBuilder(623)
            Log.e("LT", "" + line.stipple);
            Log.e("LT", "" + line.repeatStart);
            Log.e("LT", "" + line.repeatGap);
            Log.e("LT", "" + line.texture.offset);
            Log.e("LT", "" + line.texture.width);
            float r = 1f * line.texture.width / line.stipple;
            float symbolWidth = line.stipple - line.repeatGap;
            Bitmap bmp = Bitmap.createBitmap(line.texture.bitmap.getPixels(),
                    line.texture.bitmap.getWidth(), line.texture.bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            canvas.drawBitmap(bmp, mCenterX - (line.repeatStart + symbolWidth / 2f) * r,
                    mCenterY - line.texture.height / 2f, null);
        } else if (line.outline) {
            float halfWidth = mLastLineWidth / 2f;
            canvas.drawLine(mLeft, mCenterY - halfWidth, mRight, mCenterY - halfWidth, paint);
            canvas.drawLine(mLeft, mCenterY + halfWidth, mRight, mCenterY + halfWidth, paint);
        } else if (line.stipple != 0) {
            float stipple = line.stipple * MapTrek.density * .4f;
            Path path = new Path();
            path.moveTo(mLeft, mCenterY);
            path.quadTo(mRight / 2f, mCenterY, mRight, mCenterY);
            paint.setPathEffect(new DashPathEffect(new float[]{stipple, stipple}, 0));
            canvas.drawPath(path, paint);
            Matrix matrix = new Matrix();
            matrix.setTranslate(stipple, 0f);
            path.transform(matrix);
            paint.setStrokeWidth(paint.getStrokeWidth() * line.stippleWidth);
            paint.setColor(line.stippleColor);
            canvas.drawPath(path, paint);
        } else {
            canvas.drawLine(mLeft, mCenterY, mRight, mCenterY, paint);
            mLastLineWidth = paint.getStrokeWidth();
        }
    }

    void renderText(Canvas canvas, TextStyle text) {
        Log.e("T", "" + text.style);
        Log.e("T", "" + text.dy);
        Log.e("T", "" + ((AndroidPaint) text.paint).getPaint().getTextSize());
        Log.e("T", "" + text.fontHeight);
        Log.e("T", "" + text.fontSize);
        if (mItem.text == null)
            return;
        float h = text.paint.getTextHeight(mItem.text);
        if (text.bitmap != null)
            h = h * Math.signum(text.dy);
        else
            h = h / -2f;
        if (mItem.type == GeometryType.POINT) {
            if (text.bitmap != null)
                canvas.drawBitmap(text.bitmap.getPixels(), 0, text.bitmap.getWidth(),
                        mCenterX - text.bitmap.getWidth() / 2f,
                        mCenterY - text.bitmap.getHeight() / 2f - h,
                        text.bitmap.getWidth(), text.bitmap.getHeight(), true, null);
        }

        if (text.stroke != null) {
            float w = text.stroke.getTextWidth(mItem.text);
            canvas.drawText(mItem.text, mCenterX - w / 2f, mCenterY + text.dy * COORD_SCALE - h,
                    ((AndroidPaint) text.stroke).getPaint());
        }
        float w = text.paint.getTextWidth(mItem.text);
        canvas.drawText(mItem.text, mCenterX - w / 2f, mCenterY + text.dy * COORD_SCALE - h,
                ((AndroidPaint) text.paint).getPaint());
    }

    public static class LegendItem {
        public GeometryType type;
        public int zoomLevel;
        public int totalSymbols;
        public TagSet tags;
        public String text;
        public String name;

        public LegendItem(GeometryType type, String name, int zoomLevel) {
            this.type = type;
            this.name = name;
            this.zoomLevel = zoomLevel;
            tags = new TagSet();
            totalSymbols = 1;
        }

        public LegendItem addTag(String key, String value) {
            tags.add(new Tag(key, value));
            return this;
        }

        public LegendItem setText(String text) {
            this.text = text;
            return this;
        }

        public LegendItem setTotalSymbols(int totalSymbols) {
            this.totalSymbols = totalSymbols;
            return this;
        }
    }
}
