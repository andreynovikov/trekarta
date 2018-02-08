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
import android.util.AttributeSet;
import android.webkit.WebView;

public class LimitedWebView extends WebView {

    private int maxHeightPixels = -1;

    public LimitedWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LimitedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimitedWebView(Context context) {
        super(context);
    }

    public void setMaxHeight(int pixels) {
        maxHeightPixels = pixels;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (maxHeightPixels > -1 && getMeasuredHeight() > maxHeightPixels) {
            setMeasuredDimension(getMeasuredWidth(), maxHeightPixels);
        }
    }
}
