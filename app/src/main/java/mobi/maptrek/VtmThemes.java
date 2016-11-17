package mobi.maptrek;

import org.oscim.backend.AssetAdapter;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;

import java.io.InputStream;

/**
 * Enumeration of all internal rendering themes.
 */
enum VtmThemes implements ThemeFile {

    DEFAULT("styles/default.xml"),
    NEWTRON("styles/newtron.xml");

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
}
