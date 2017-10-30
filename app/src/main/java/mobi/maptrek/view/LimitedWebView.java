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
