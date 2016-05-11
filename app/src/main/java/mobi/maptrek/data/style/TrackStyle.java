package mobi.maptrek.data.style;

public class TrackStyle extends Style<TrackStyle> {
    public static int DEFAULT_COLOR = android.graphics.Color.MAGENTA;
    public static float DEFAULT_WIDTH = 5;

    public int color = DEFAULT_COLOR;
    public float width = DEFAULT_WIDTH;


    @Override
    public boolean isDefault() {
        return color == DEFAULT_COLOR && width == DEFAULT_WIDTH;
    }

    @Override
    public void copy(TrackStyle style) {
        style.color = color;
        style.width = width;
    }
}
