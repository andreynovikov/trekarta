package mobi.maptrek;

import org.oscim.backend.AssetAdapter;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;

import java.io.InputStream;
import java.util.Set;

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
        return new XmlRenderThemeMenuCallback() {
            @Override
            public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
                String[] styleCodes = MapTrek.getApplication().getResources().getStringArray(R.array.mapStyleCodes);
                String style = styleCodes[Configuration.getMapStyle()];

                // Retrieve the layer from the style id
                XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
                if (renderThemeStyleLayer == null) {
                    System.err.println("Invalid style " + style);
                    return null;
                }

                // First get the selected layer's categories that are enabled together
                Set<String> categories = renderThemeStyleLayer.getCategories();

                // Then add the selected layer's overlays that are enabled individually
                // Here we use the style menu, but users can use their own preferences
                for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                    if (overlay.isEnabled())
                        categories.addAll(overlay.getCategories());
                }

                // This is the whole categories set to be enabled
                return categories;
            }
        };
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
