/*
 * Copyright 2021 Andrey Novikov
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import android.graphics.RectF;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import mobi.maptrek.MapTrek;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;

@SuppressWarnings("rawtypes")
public class LegendView extends View {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(LegendView.class);

    private static final RectF PATH_RECT = new RectF(0f, 0f, 1f, 0.6f);
    public static final Path PATH_BUILDING = new Path();
    public static final Path PATH_PLATFORM = new Path();
    public static final Path PATH_PIER = new Path();
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
    private final Matrix mViewMatrix = new Matrix();
    ArrayList<RenderStyle> mLines = new ArrayList<>();

    static {
        // Paths should fit 1x0.6 rect
        PATH_BUILDING.moveTo(0.1f, 0.6f);
        PATH_BUILDING.lineTo(0.1f,0.15f);
        PATH_BUILDING.lineTo(0.9f,0.15f);
        PATH_BUILDING.lineTo(0.9f,0.45f);
        PATH_BUILDING.lineTo(0.4f, 0.45f);
        PATH_BUILDING.lineTo(0.4f, 0.6f);
        PATH_BUILDING.close();
        PATH_PLATFORM.moveTo(0f, 0.45f);
        PATH_PLATFORM.lineTo(1f, 0.45f);
        PATH_PLATFORM.lineTo(1f, 0.15f);
        PATH_PLATFORM.lineTo(0f, 0.15f);
        PATH_PLATFORM.close();
        PATH_PIER.moveTo(0f, 0.4f);
        PATH_PIER.lineTo(0.8f, 0.4f);
        PATH_PIER.lineTo(0.8f, 0.6f);
        PATH_PIER.lineTo(1f, 0.6f);
        PATH_PIER.lineTo(1f, 0f);
        PATH_PIER.lineTo(0.8f, 0f);
        PATH_PIER.lineTo(0.8f, 0.2f);
        PATH_PIER.lineTo(0f, 0.2f);
        PATH_PIER.close();
    }

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

        mViewMatrix.setRectToRect(PATH_RECT, new RectF(mLeft, mTop, mRight, mBottom), Matrix.ScaleToFit.CENTER);
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
                if (item.path != null) {
                    Path p = new Path();
                    p.addPath(item.path, mViewMatrix);
                    canvas.clipPath(p);
                } else {
                    canvas.clipRect(mLeft, mTop, mRight, mBottom);
                }
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
                if (item.path != null) {
                    Path p = new Path();
                    p.addPath(item.path, mViewMatrix);
                    canvas.drawPath(p, paint);
                } else {
                    canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
                }
            }
            if (areaStyle.strokeWidth != 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(areaStyle.strokeWidth * mDensity * .25f);
                paint.setColor(areaStyle.strokeColor);
                if (item.path != null) {
                    Path p = new Path();
                    p.addPath(item.path, mViewMatrix);
                    canvas.drawPath(p, paint);
                } else {
                    canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
                }
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
        if (symbolStyle.bitmap == null) {
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
        float scale = (float) Math.pow(item.zoomLevel / 17.0, 2);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(lineStyle.fixed ? lineStyle.width * mDensity * scale : lineStyle.width * mDensity * 8f * scale);
        paint.setColor(lineStyle.color);
        if (item.type == GeometryType.LINE) {
            if (lineStyle.texture != null) {
                Bitmap bmp = Bitmap.createBitmap(lineStyle.texture.bitmap.getPixels(),
                        lineStyle.texture.bitmap.getWidth(), lineStyle.texture.bitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Paint bmpPaint = null;
                if (lineStyle.stippleColor != 0xFF000000 && lineStyle.stippleColor != 0xFFFFFFFF) {
                    bmpPaint = new Paint();
                    bmpPaint.setColorFilter(new PorterDuffColorFilter(lineStyle.stippleColor, PorterDuff.Mode.SRC_IN));
                }

                float w = mRight - mLeft;
                float bmpWidth = bmp.getWidth();
                float bmpHeight2 = bmp.getHeight() / 2f;
                int count = (int) (Math.max(w / bmpWidth, 1.0));
                float remainder = w - bmpWidth * count;
                if (remainder > bmpWidth) {
                    count++;
                    remainder -= bmpWidth;
                }
                if (remainder > bmpWidth / 3)
                    count++;
                float xOffset = (w - bmpWidth * count) / 2f;
                paint.setStrokeWidth(5);
                xOffset += mLeft;
                while (count > 0) {
                    //canvas.drawRect(xOffset, mCenterY - bmpHeight2, xOffset + bmpWidth, mCenterY + bmpHeight2, paint);
                    canvas.drawBitmap(bmp, xOffset, mCenterY - bmpHeight2, bmpPaint);
                    xOffset += bmpWidth;
                    count--;
                }
                bmp.recycle();
            } else if (lineStyle.outline) {
                float halfWidth = mLastLineWidth / 2f;
                if (lineStyle.half != LineStyle.Half.RIGHT)
                    canvas.drawLine(mLeft, mCenterY - halfWidth, mRight, mCenterY - halfWidth, paint);
                if (lineStyle.half != LineStyle.Half.LEFT)
                    canvas.drawLine(mLeft, mCenterY + halfWidth, mRight, mCenterY + halfWidth, paint);
            } else if (lineStyle.stipple != 0) {
                float main = lineStyle.stipple * mDensity * (1f - lineStyle.stippleRatio);
                float stipple = lineStyle.stipple * mDensity * lineStyle.stippleRatio;
                Path path = new Path();
                path.moveTo(mLeft, mCenterY);
                path.quadTo(mRight / 2f, mCenterY, mRight, mCenterY);
                paint.setPathEffect(new DashPathEffect(new float[]{main, stipple}, 0));
                // draw major color dashes
                canvas.drawPath(path, paint);
                path.rewind();
                path.moveTo(mLeft + main, mCenterY);
                path.quadTo(mRight / 2f, mCenterY, mRight, mCenterY);
                paint.setPathEffect(new DashPathEffect(new float[]{stipple, main}, 0));
                // draw stipple dash background
                paint.setAlpha(android.graphics.Color.alpha(lineStyle.stippleColor));
                canvas.drawPath(path, paint);
                // draw stipple color dashes
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
                //TODO: line half
                canvas.drawLine(mLeft, mCenterY, mRight, mCenterY, paint);
                if (lineStyle.blur != 0f) {
                    canvas.restore();
                }
                mLastLineWidth = paint.getStrokeWidth();
            }
        } else if (item.type == GeometryType.POLY) {
            if (item.path != null) {
                Path p = new Path();
                p.addPath(item.path, mViewMatrix);
                canvas.drawPath(p, paint);
            } else {
                canvas.drawRoundRect(mLeft, mTop, mRight, mBottom, 10, 10, paint);
            }
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
        float gap = textStyle.bitmap != null ? textStyle.bitmap.getWidth() / 2f : 0f;
        float x;
        switch (item.align) {
            case LegendItem.ALIGN_LEFT:
                x = mLeft + textStyle.paint.getTextWidth(text) + gap;
                break;
            case LegendItem.ALIGN_RIGHT:
                x = mRight - textStyle.paint.getTextWidth(text) - gap;
                break;
            case LegendItem.ALIGN_CENTER:
            default:
                x = mCenterX;
        }
        if (item.type == GeometryType.POINT) {
            if (textStyle.bitmap != null)
                canvas.drawBitmap(textStyle.bitmap.getPixels(), 0, textStyle.bitmap.getWidth(),
                        x - gap, mCenterY - textStyle.bitmap.getHeight() / 2f - h,
                        textStyle.bitmap.getWidth(), textStyle.bitmap.getHeight(),
                        true, null);
        }

        if (textStyle.stroke != null) {
            canvas.drawText(text, x, mCenterY + textStyle.dy * .8f - h,
                    ((AndroidPaint) textStyle.stroke).getPaint());
        }
        canvas.drawText(text, x, mCenterY + textStyle.dy * .8f - h,
                ((AndroidPaint) textStyle.paint).getPaint());
    }

    public static class LegendItem {
        public static final int ALIGN_LEFT = -1;
        public static final int ALIGN_CENTER = 0;
        public static final int ALIGN_RIGHT = 1;

        public GeometryType type;
        public int zoomLevel;
        public TagSet tags;
        public int kind;
        @StringRes
        public int text;
        @StringRes
        public int name;
        public Path path;
        int align;
        int totalSymbols;
        LegendItem overlay;

        public LegendItem(GeometryType type, @StringRes int name, int zoomLevel) {
            this.type = type;
            this.name = name;
            this.zoomLevel = zoomLevel;
            this.tags = new TagSet();
            this.kind = 0;
            this.text = 0;
            this.align = ALIGN_CENTER;
            this.totalSymbols = 1;
            this.overlay = null;
            this.path = null;
        }

        public LegendItem addTag(String key, String value) {
            tags.add(new Tag(key, value));
            return this;
        }

        public LegendItem setKind(int kind) {
            this.kind = kind;
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

        public LegendItem setShape(Path path) {
            this.path = path;
            return this;
        }

        public LegendItem setTextAlign(int align) {
            this.align = align;
            return this;
        }
    }

    public static class LegendAmenityItem extends LegendItem {
        public int type;

        public LegendAmenityItem(int type) {
            super(GeometryType.POINT, Tags.getTypeName(type), 17);
            this.type = type;
            Tags.setTypeTag(type, tags);
        }
    }
}
