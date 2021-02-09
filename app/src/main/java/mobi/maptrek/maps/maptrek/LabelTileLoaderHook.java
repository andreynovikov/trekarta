/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2016 Andrey Novikov
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
 */

package mobi.maptrek.maps.maptrek;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.tile.vector.labeling.LabelTileData;
import org.oscim.layers.tile.vector.labeling.WayDecorator;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.geom.PolyLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.util.StringFormatter;

import static org.oscim.core.GeometryBuffer.GeometryType.LINE;
import static org.oscim.core.GeometryBuffer.GeometryType.POINT;
import static org.oscim.core.GeometryBuffer.GeometryType.POLY;

public class LabelTileLoaderHook implements VectorTileLayer.TileLoaderThemeHook {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(LabelTileLoaderHook.class);

    private static final String LABEL_DATA = LabelLayer.class.getName();

    private final ShieldFactory mShieldFactory;
    private final OsmcSymbolFactory mOsmcSymbolFactory;
    private final SymbolStyle.SymbolBuilder<?> mSymbolBuilder = SymbolStyle.builder();

    private int mLang = 0;
    private float mSquareTile;

    public LabelTileLoaderHook(ShieldFactory shieldFactory, OsmcSymbolFactory osmcSymbolFactory) {
        mShieldFactory = shieldFactory;
        mOsmcSymbolFactory = osmcSymbolFactory;
        mSquareTile = Tile.SIZE * Tile.SIZE; // we can't use static as it's recalculated based on dpi
    }

    //public final static LabelTileData EMPTY = new LabelTileData();

    private LabelTileData get(MapTile tile) {
        // FIXME could be 'this'..
        LabelTileData ld = (LabelTileData) tile.getData(LABEL_DATA);
        if (ld == null) {
            ld = new LabelTileData();
            tile.addData(LABEL_DATA, ld);
        }
        return ld;
    }

    @Override
    public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
        if (style instanceof TextStyle) {
            LabelTileData ld = get(tile);

            TextStyle text = (TextStyle) style.current();
            if (element.type == LINE) {
                String value = getTextValue(element, text.textKey);
                if (value == null)
                    return false;

                int offset = 0;
                for (int i = 0, n = element.index.length; i < n; i++) {
                    int length = element.index[i];
                    if (length < 4)
                        break;

                    WayDecorator.renderText(null, element.points, value, text,
                            offset, length, ld);
                    offset += length;
                }
            } else if (element.type == POLY) {
                PointF label = element.labelPosition;

                if (element instanceof ExtendedMapElement) {
                    // skip any calculations if element has label position but it is not defined
                    if (((ExtendedMapElement) element).hasLabelPosition && label == null)
                        return false;
                }

                // skip unnecessary calculations if label is outside of visible area
                if (label != null && (label.x < 0 || label.x > Tile.SIZE || label.y < 0 || label.y > Tile.SIZE))
                    return false;

                float pixelArea;
                if (element instanceof ExtendedMapElement && ((ExtendedMapElement) element).featureArea > 0) {
                    double resolution = tile.getGroundScale();
                    pixelArea = (float) (((ExtendedMapElement) element).featureArea / Math.pow(resolution, 2));
                } else {
                    pixelArea = element.area();
                }
                float ratio = pixelArea / mSquareTile;
                if (ratio < text.areaSize)
                    return false;

                String value = getTextValue(element, text.textKey);
                if (value == null)
                    return false;

                if (label == null)
                    label = PolyLabel.get(element);

                ld.labels.push(TextItem.pool.get().set(label.x, label.y, ratio, value, text));
            } else if (element.type == POINT) {
                String value = getTextValue(element, text.textKey);
                if (value == null)
                    return false;

                float ratio = 0f;
                if (element instanceof ExtendedMapElement && ((ExtendedMapElement) element).featureArea > 0) {
                    /* Reference: how to get element latitude
                     * double latitude = MercatorProjection.toLatitude(tile.y + y / tile.mapSize);
                     */
                    double resolution = tile.getGroundScale();
                    float pixelArea = (float) (((ExtendedMapElement) element).featureArea / Math.pow(resolution, 2));
                    ratio = pixelArea / mSquareTile;
                }

                if (ratio < text.areaSize)
                    return false;

                for (int i = 0, n = element.getNumPoints(); i < n; i++) {
                    PointF p = element.getPoint(i);
                    ld.labels.push(TextItem.pool.get().set(p.x, p.y, ratio, value, text));
                }
            }
        } else if (style instanceof SymbolStyle) {
            SymbolStyle symbol = (SymbolStyle) style.current();

            if (symbol.src != null) {
                if (symbol.src.equals("/osmc-symbol")) {
                    String osmcSymbol = element.tags.getValue("osmc:symbol");
                    Bitmap bitmap = mOsmcSymbolFactory.getBitmap(osmcSymbol, symbol.symbolPercent);
                    if (bitmap != null)
                        symbol = mSymbolBuilder.set(symbol).bitmap(bitmap).build();
                } else if (symbol.src.startsWith("/shield/")) {
                    Bitmap bitmap = mShieldFactory.getBitmap(element.tags, symbol.src, symbol.symbolPercent);
                    if (bitmap != null)
                        symbol = mSymbolBuilder.set(symbol).bitmap(bitmap).build();
                }
            }

            if (symbol.bitmap == null && symbol.texture == null)
                return false;

            LabelTileData ld = get(tile);

            if (element.type == POINT) {
                for (int i = 0, n = element.getNumPoints(); i < n; i++) {
                    PointF p = element.getPoint(i);

                    SymbolItem it = SymbolItem.pool.get();
                    if (symbol.bitmap != null)
                        it.set(p.x, p.y, symbol.bitmap, 0, 0f, true, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                    else
                        it.set(p.x, p.y, symbol.texture, 0, 0f, true,  symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                    ld.symbols.push(it);
                }
            } else if (element.type == LINE) {
                int offset = 0;
                for (int i = 0, n = element.index.length; i < n; i++) {
                    int length = element.index[i];
                    if (length < 4)
                        break;

                    WayDecorator.renderSymbol(null, element.points, symbol, offset, length, ld);
                    offset += length;
                }
            } else if (element.type == POLY) {
                PointF centroid = element.labelPosition;
                if (centroid == null)
                    return false;

                if (centroid.x < 0 || centroid.x > Tile.SIZE || centroid.y < 0 || centroid.y > Tile.SIZE)
                    return false;

                SymbolItem it = SymbolItem.pool.get();
                if (symbol.bitmap != null)
                    it.set(centroid.x, centroid.y, symbol.bitmap, 0, 0f, true,  symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                else
                    it.set(centroid.x, centroid.y, symbol.texture, 0, 0f, true,  symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                ld.symbols.push(it);
            }
        }
        return false;
    }

    @Override
    public void complete(MapTile tile, boolean success) {
    }

    private String getTextValue(MapElement element, String key) {
        if ("name".equals(key) && element instanceof ExtendedMapElement) {
            ExtendedMapElement extendedElement = (ExtendedMapElement) element;
            if (extendedElement.id == 0L)
                return null;
            String name = extendedElement.database.getName(mLang, extendedElement.id);
            if (name != null)
                return name;
        }
        if ("ele".equals(key) && element instanceof ExtendedMapElement) {
            ExtendedMapElement extendedElement = (ExtendedMapElement) element;
            if (extendedElement.elevation != 0) {
                //TODO Replace with kind flag
                if (element.tags.containsKey("contour"))
                    return StringFormatter.elevationC(extendedElement.elevation);
                else
                    return StringFormatter.elevationH(extendedElement.elevation);
            }

        }
        if ("depth".equals(key) && element instanceof ExtendedMapElement) {
            ExtendedMapElement extendedElement = (ExtendedMapElement) element;
            if (extendedElement.depth != 0) {
                float depth = extendedElement.depth * 0.01f;
                String format = extendedElement.depth % 100 != 0 ? "%.1f" : StringFormatter.elevationFormat;
                return StringFormatter.elevationH(depth, format);
            }
        }
        String value = element.tags.getValue(key);
        if (value != null && value.length() > 0)
            return value;
        return null;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        mLang = MapTrekDatabaseHelper.getLanguageId(preferredLanguage);
    }
}
