package mobi.maptrek.maps;

import android.content.res.AssetManager;

import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.util.ByteArrayInOutStream;

/**
 * Enumeration of all internal rendering themes.
 */
public enum Themes implements ThemeFile {
    MAPTREK("styles/maptrek.xml"),
    WINTER("styles/winter.xml"),
    NEWTRON("styles/newtron.xml");

    private static final Logger logger = LoggerFactory.getLogger(Themes.class);

    public static final float[] MAP_FONT_SIZES = {.3f, .5f, .7f, .9f, 1.1f};

    private final String mPath;

    Themes(String path) {
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
                    logger.error("Invalid style {}", style);
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

                switch (Configuration.getActivity()) {
                    case 1:
                        categories.add("hiking");
                        break;
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
        /* This code contains A DANGEROUS HACK as Android does not support xi:include!
         It assumes a lot of prerequisites how includes are formatted. It requires xi:include
         to be on separate line. It requires included file to contain exactly two starting lines
          with XML header and opening document tag and one ending line with closing document tag.
         */
        try {
            final AssetManager assets = MapTrek.getApplication().getAssets();
            final String dir = mPath.substring(0, mPath.indexOf(File.separatorChar) + 1);
            final InputStream is = assets.open(mPath);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final ByteArrayInOutStream ois = new ByteArrayInOutStream(1024);
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ois));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                // <xi:include href="included.xml"
                if (line.startsWith("<xi:include")) {
                    int b = line.indexOf("href=");
                    int e = line.indexOf("\"", b + 6);
                    if (b > 0) {
                        String src = dir + line.substring(b + 6, e);
                        logger.error("include: {}", src);
                        final InputStream iis = assets.open(src);
                        final BufferedReader ibr = new BufferedReader(new InputStreamReader(iis));
                        String il = ibr.readLine(); // skip <?xml>
                        if (il != null)
                            ibr.readLine(); // skip <rendertheme> IT MUST BE ON ONE LINE
                        if (il != null) // avoid IOE
                            il = ibr.readLine();
                        while (il != null) {
                            String nl = ibr.readLine(); // skip </rendertheme> IT MUST BE LAST LINE
                            if (nl != null) {
                                writer.write(il);
                                writer.newLine();
                            }
                            il = nl;
                        }
                        ibr.close();
                    }
                } else {
                    writer.write(line);
                    writer.newLine();
                }
                line = reader.readLine();
            }
            reader.close();
            writer.close();
            return ois.getInputStream();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback) {
    }
}
