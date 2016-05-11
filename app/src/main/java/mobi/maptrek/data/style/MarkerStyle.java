package mobi.maptrek.data.style;

public class MarkerStyle extends Style<MarkerStyle> {
    public static int DEFAULT_COLOR = android.graphics.Color.DKGRAY;

    public int color = DEFAULT_COLOR;
    //TODO Add processing for this style field
    public String icon;

    @Override
    public boolean isDefault() {
        return color == DEFAULT_COLOR && icon == null;
    }

    @Override
    public void copy(MarkerStyle style) {
        style.color = color;
        style.icon = icon;
    }
}
