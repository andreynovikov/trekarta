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

package mobi.maptrek.maps;

import android.content.res.AssetManager;

import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
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
    NIGHT("styles/night.xml");

    private static final Logger logger = LoggerFactory.getLogger(Themes.class);

    public static final float[] MAP_FONT_SIZES = {.3f, .5f, .7f, .9f, 1.1f};
    public static final float[] MAP_SCALE_SIZES = {.7f, .85f, 1f, 1.3f, 1.7f};

    private final String mPath;

    Themes(String path) {
        mPath = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return renderThemeStyleMenu -> {
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
                case 2:
                    categories.add("cycling");
                    break;
            }

            // This is the whole categories set to be enabled
            return categories;
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
            readThemeAsset(assets, dir, reader, writer, false);
            reader.close();
            writer.close();
            return ois.getInputStream();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private void readThemeAsset(AssetManager assets, String dir, BufferedReader reader, BufferedWriter writer, boolean included) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            String nextLine = reader.readLine();
            line = line.trim();
            // <xi:include href="included.xml"
            if (line.startsWith("<xi:include")) {
                int b = line.indexOf("href=");
                int e = line.indexOf("\"", b + 6);
                if (b > 0) {
                    String src = dir + line.substring(b + 6, e);
                    logger.debug("include: {}", src);
                    final InputStream iis = assets.open(src);
                    final BufferedReader ibr = new BufferedReader(new InputStreamReader(iis));
                    String il = ibr.readLine(); // skip <?xml>  in included file
                    if (il != null)
                        il = ibr.readLine(); // skip <rendertheme> in included file - IT MUST BE ON ONE LINE
                    if (il != null) // avoid IOE
                        readThemeAsset(assets, dir, ibr, writer, true);
                    ibr.close();
                }
            } else {
                if (nextLine != null || !included) // skip </rendertheme> in included file - IT MUST BE LAST LINE
                writer.write(line);
                writer.newLine();
            }
            line = nextLine;
        }
    }

    @Override
    public boolean isMapsforgeTheme() {
        return false;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback) {
    }
}
