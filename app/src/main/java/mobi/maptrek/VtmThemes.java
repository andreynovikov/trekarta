package mobi.maptrek;

import org.oscim.backend.AssetAdapter;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;

import java.io.InputStream;

/**
 * Enumeration of all internal rendering themes.
 */
enum VtmThemes implements ThemeFile {

    MAPTREK("styles/maptrek.xml"),
    NEWTRON("styles/newtron.xml");

    public static final float[] MAP_FONT_SIZES = {.3f, .5f, .7f, .9f, 1.1f};

    private final String mPath;

    VtmThemes(String path) {
        mPath = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return null;
    }

    @Override
    public String getRelativePathPrefix() {
        return "";
    }

    @Override
    public InputStream getRenderThemeAsStream() {
        return AssetAdapter.readFileAsStream(mPath);
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback) {
    }
}
