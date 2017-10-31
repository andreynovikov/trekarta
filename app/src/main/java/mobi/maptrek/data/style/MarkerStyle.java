package mobi.maptrek.data.style;

public class MarkerStyle extends Style<MarkerStyle> {
    public static final int[] DEFAULT_COLORS = {
            android.graphics.Color.DKGRAY,
            0xff2062af,
            0xff58AEB7,
            0xffF4B528,
            0xffDD3E48,
            0xffBF89AE,
            0xff5C88BE,
            0xff59BC10,
            0xffE87034,
            0xfff84c44,
            0xff8c47fb,
            0xff51C1EE,
            0xff8cc453,
            0xffC2987D,
            0xffCE7777,
            0xff9086BA
    };

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
