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
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;

import org.oscim.android.canvas.AndroidPaint;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.CircleStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;

import java.util.ArrayList;

import mobi.maptrek.MapTrek;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;

public class LegendView extends View {
    private LegendItem mItem;
    private int mBackground;
    private IRenderTheme mTheme;
    private ShieldFactory mShieldFactory;
    private OsmcSymbolFactory mOsmcSymbolFactory;
    private float mDensity;
    private float mLeft;
    private float mRight;
    private float mTop;
    private float mBottom;
    private float mCenterX;
    private float mCenterY;
    private float mLastLineWidth;
    private int mSymbolCount;
    ArrayList<RenderStyle> mLines = new ArrayList<>();

    public LegendView(Context context) {
        this(context, null, 0);
    }

    public LegendView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegendView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setLegend(LegendItem item, int background, IRenderTheme theme, ShieldFactory shieldFactory, OsmcSymbolFactory osmcSymbolFactory) {
        mItem = item;
        mBackground = background;
        mTheme = theme;
        mShieldFactory = shieldFactory;
        mOsmcSymbolFactory = osmcSymbolFactory;
        mDensity = MapTrek.density * 0.75f;
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

        for (LegendItem item = mItem; item != null; item = item.overlay) {
            RenderStyle[] styles = mTheme.matchElement(item.type, item.tags, item.zoomLevel);
            if (styles == null)
                return;

            float gap = 3f * mDensity;
            canvas.clipRect(mLeft - gap, 0, mRight + gap, getHeight());

            mLines.clear();
            mSymbolCount = 1;
            for (RenderStyle style : styles) {
                if (style instanceof LineStyle) {
                    if (((LineStyle) style).texture != null)
                        mLines.add(0, style);
                    else
                        renderLine(item, canvas, (LineStyle) style);
                } else if (style instanceof AreaStyle) {
                    if (item.type == GeometryType.LINE)
                        continue;
                    renderArea(item, canvas, (AreaStyle) style);
                } else if (style instanceof CircleStyle) {
                    renderCircle(item, canvas, (CircleStyle) style);
                }
            }
            for (RenderStyle style : mLines) {
                renderLine(item, canvas, (LineStyle) style);
            }
            for (RenderStyle style : styles) {
                if (style instanceof SymbolStyle)
                    renderSymbol(item, canvas, (SymbolStyle) style);
                else if (style instanceof TextStyle)
                    renderText(item, canvas, (TextStyle) style);
            }
        }
    }

    void renderArea(LegendItem item, Canvas canvas, AreaStyle areaStyle) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(areaStyle.color);
        if (item.type == GeometryType.POINT) {
            canvas.drawCircle(mCenterX, mCenterY, 5 * mDensity, paint);
        } else {
            if (areaStyle.texture != null) {
                Bitmap bmp = Bitmap.createBitmap(areaStyle.texture.bitmap.getPixels(),
                        areaStyle.texture.bitmap.getWidth(), areaStyle.texture.bitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
                canvas.save();
                canvas.clipRect(mLeft, mTop, mRight, mBottom);
                float left = mCenterX - areaStyle.texture.width / 2f;
                float top = mCenterY - areaStyle.texture.height / 2f;
                canvas.drawBitmap(bmp, left, top, null);
                if (left > mLeft)
                    canvas.drawBitmap(bmp, left - areaStyle.texture.width, top, null);
                left = left + areaStyle.texture.width;
                if (left < mRight)
                    canvas.drawBitmap(bmp, left, top, null);
                canvas.restore();
            } else {
                canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
            }
            if (areaStyle.strokeWidth != 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(areaStyle.strokeWidth * mDensity * .25f);
                paint.setColor(areaStyle.strokeColor);
                canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
            }
        }
    }

    void renderCircle(LegendItem item, Canvas canvas, CircleStyle circleStyle) {
    }

    void renderSymbol(LegendItem item, Canvas canvas, SymbolStyle symbolStyle) {
        if (item.totalSymbols == 0)
            return;
        float x = item.totalSymbols > 1 ? mLeft + (mRight - mLeft) / (item.totalSymbols + 1) * mSymbolCount : mCenterX;
        mSymbolCount++;
        org.oscim.backend.canvas.Bitmap bitmap = null;
        if (symbolStyle.src != null) {
            if (symbolStyle.src.equals("/osmc-symbol")) {
                String osmcSymbol = item.tags.getValue("osmc:symbol");
                bitmap = mOsmcSymbolFactory.getBitmap(osmcSymbol, symbolStyle.symbolPercent);
            } else if (symbolStyle.src.startsWith("/shield/")) {
                bitmap = mShieldFactory.getBitmap(item.tags, symbolStyle.src, symbolStyle.symbolPercent);
            }
        } else {
            bitmap = symbolStyle.bitmap;
        }
        if (bitmap == null)
            return;
        canvas.drawBitmap(bitmap.getPixels(), 0, bitmap.getWidth(),
                x - bitmap.getWidth() / 2f, mCenterY - bitmap.getHeight() / 2f,
                bitmap.getWidth(), bitmap.getHeight(), true, null);
    }

    void renderLine(LegendItem item, Canvas canvas, LineStyle lineStyle) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(lineStyle.fixed ? lineStyle.width * mDensity : lineStyle.width * mDensity * 8f);
        paint.setColor(lineStyle.color);
        if (item.type == GeometryType.LINE) {
            if (lineStyle.texture != null) {
                // XMLThemeBuilder(623)
                float xOffset = 0;
                float symbolWidth = lineStyle.stipple - lineStyle.repeatGap;
                if (symbolWidth > 0) {
                    float r = .5f * lineStyle.texture.width / lineStyle.stipple;
                    xOffset = (lineStyle.repeatStart + symbolWidth / 2f) * r;
                }
                Bitmap bmp = Bitmap.createBitmap(lineStyle.texture.bitmap.getPixels(),
                        lineStyle.texture.bitmap.getWidth(), lineStyle.texture.bitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Bitmap resized = Bitmap.createScaledBitmap(bmp, bmp.getWidth() >> 1, bmp.getHeight() >> 1, false);
                bmp.recycle();
                Paint bmpPaint = null;
                if (lineStyle.stippleColor != 0xFF000000 && lineStyle.stippleColor != 0xFFFFFFFF) {
                    bmpPaint = new Paint();
                    bmpPaint.setColorFilter(new PorterDuffColorFilter(lineStyle.stippleColor, PorterDuff.Mode.SRC_IN));
                }
                //canvas.drawBitmap(resized, mCenterX - xOffset, mCenterY - line.texture.height / 2f, bmpPaint);
                canvas.drawBitmap(resized, mCenterX - xOffset, mCenterY - resized.getHeight() / 2f, bmpPaint);
                resized.recycle();
            } else if (lineStyle.outline) {
                float halfWidth = mLastLineWidth / 2f;
                canvas.drawLine(mLeft, mCenterY - halfWidth, mRight, mCenterY - halfWidth, paint);
                canvas.drawLine(mLeft, mCenterY + halfWidth, mRight, mCenterY + halfWidth, paint);
            } else if (lineStyle.stipple != 0) {
                float stipple = lineStyle.stipple * mDensity * .5f;
                Path path = new Path();
                path.moveTo(mLeft, mCenterY);
                path.quadTo(mRight / 2f, mCenterY, mRight, mCenterY);
                paint.setPathEffect(new DashPathEffect(new float[]{stipple, stipple}, 0));
                canvas.drawPath(path, paint);
                path.rewind();
                path.moveTo(mLeft + stipple, mCenterY);
                path.quadTo(mRight / 2f, mCenterY, mRight, mCenterY);
                paint.setAlpha(android.graphics.Color.alpha(lineStyle.stippleColor));
                canvas.drawPath(path, paint);
                paint.setStrokeWidth(paint.getStrokeWidth() * lineStyle.stippleWidth);
                paint.setColor(lineStyle.stippleColor);
                canvas.drawPath(path, paint);
            } else {
                if (lineStyle.blur != 0f) {
                    canvas.save();
                    canvas.clipRect(mLeft, mTop, mRight, mBottom);
                    paint.setStrokeCap(Paint.Cap.SQUARE);
                    paint.setMaskFilter(new BlurMaskFilter(paint.getStrokeWidth() * lineStyle.blur, BlurMaskFilter.Blur.INNER));
                }
                canvas.drawLine(mLeft, mCenterY, mRight, mCenterY, paint);
                if (lineStyle.blur != 0f) {
                    canvas.restore();
                }
                mLastLineWidth = paint.getStrokeWidth();
            }
        } else if (item.type == GeometryType.POLY) {
            canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
        }
    }

    void renderText(LegendItem item, Canvas canvas, TextStyle textStyle) {
        if (item.text == 0)
            return;
        String text = getResources().getString(item.text);
        float h = textStyle.paint.getTextHeight(text);
        if (textStyle.bitmap != null)
            h = h * Math.signum(textStyle.dy);
        else
            h = h / -2f;
        if (item.type == GeometryType.POINT) {
            if (textStyle.bitmap != null)
                canvas.drawBitmap(textStyle.bitmap.getPixels(), 0, textStyle.bitmap.getWidth(),
                        mCenterX - textStyle.bitmap.getWidth() / 2f,
                        mCenterY - textStyle.bitmap.getHeight() / 2f - h,
                        textStyle.bitmap.getWidth(), textStyle.bitmap.getHeight(),
                        true, null);
        }

        if (textStyle.stroke != null) {
            canvas.drawText(text, mCenterX, mCenterY + textStyle.dy * .8f - h,
                    ((AndroidPaint) textStyle.stroke).getPaint());
        }
        canvas.drawText(text, mCenterX, mCenterY + textStyle.dy * .8f - h,
                ((AndroidPaint) textStyle.paint).getPaint());
    }

    public static class LegendItem {
        public GeometryType type;
        public int zoomLevel;
        public TagSet tags;
        @StringRes
        public int text;
        @StringRes
        public int name;
        int totalSymbols;
        LegendItem overlay;

        public LegendItem(GeometryType type, @StringRes int name, int zoomLevel) {
            this.type = type;
            this.name = name;
            this.zoomLevel = zoomLevel;
            this.tags = new TagSet();
            this.text = 0;
            this.totalSymbols = 1;
            this.overlay = null;
        }

        public LegendItem addTag(String key, String value) {
            tags.add(new Tag(key, value));
            return this;
        }

        public LegendItem setText(@StringRes int text) {
            this.text = text;
            return this;
        }

        public LegendItem setTotalSymbols(int totalSymbols) {
            this.totalSymbols = totalSymbols;
            return this;
        }

        public LegendItem setOverlay(LegendItem overlay) {
            this.overlay = overlay;
            return this;
        }
    }
}
