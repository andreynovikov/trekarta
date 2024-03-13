/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2019 devemux86
 * Copyright 2016-2017 Longri
 * Copyright 2016 Andrey Novikov
 * Copyright 2018-2019 Gustl22
 * Copyright 2018 Izumi Kawashima
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.theme;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.XMLReaderAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.core.Tag;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureAtlas.Rect;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.bucket.TextureItem;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.rule.Rule;
import org.oscim.theme.rule.Rule.Closed;
import org.oscim.theme.rule.Rule.Selector;
import org.oscim.theme.rule.RuleBuilder;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.AreaStyle.AreaBuilder;
import org.oscim.theme.styles.CircleStyle;
import org.oscim.theme.styles.CircleStyle.CircleBuilder;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.ExtrusionStyle.ExtrusionBuilder;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.LineStyle.Half;
import org.oscim.theme.styles.LineStyle.LineBuilder;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.SymbolStyle.SymbolBuilder;
import org.oscim.theme.styles.TextStyle;
import org.oscim.theme.styles.TextStyle.TextBuilder;
import org.oscim.utils.FastMath;
import org.oscim.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

public class XmlThemeBuilder extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(XmlThemeBuilder.class);

    private static final int RENDER_THEME_VERSION_MAPSFORGE = 6;
    private static final int RENDER_THEME_VERSION_VTM = 1;
    public static final String PREFIX_GEN = "gen:";

    private enum Element {
        RENDER_THEME, RENDERING_INSTRUCTION, RULE, STYLE, ATLAS, RECT, RENDERING_STYLE, TAG_TRANSFORM
    }

    private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
    private static final String ELEMENT_NAME_STYLE_MENU = "stylemenu";
    private static final String ELEMENT_NAME_MATCH = "m";
    private static final String UNEXPECTED_ELEMENT_STACK_NOT_EMPTY = "Stack not empty, unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_RENDERING_INSTRUCTION_PARENT_ELEMENT_MISMATCH = "Rendering instruction:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_RULE_PARENT_ELEMENT_MISMATCH = "Rule:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_STYLE_PARENT_ELEMENT_MISMATCH = "Style:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_ATLAS_PARENT_ELEMENT_MISMATCH = "Atlas:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_RECT_PARENT_ELEMENT_MISMATCH = "Rect:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_TAG_RENDERING_STYLE_PARENT_ELEMENT_MISMATCH = "Rendering style:: Parent element mismatch: unexpected element: ";
    private static final String UNEXPECTED_ELEMENT_TAG_TRANSFORM_PARENT_ELEMENT_MISMATCH = "Tag transform:: Parent element mismatch: unexpected element: ";

    private static final String LINE_STYLE = "L";
    private static final String OUTLINE_STYLE = "O";
    private static final String AREA_STYLE = "A";

    private static final int DEFAULT_PRIORITY = Integer.MAX_VALUE / 2;

    /**
     * @param theme an input theme containing valid render theme XML data.
     * @return a new RenderTheme which is created by parsing the XML data from the input theme.
     * @throws ThemeException if an error occurs while parsing the render theme XML.
     */
    public static IRenderTheme read(ThemeFile theme) throws ThemeException {
        return read(theme, null);
    }

    /**
     * @param theme         an input theme containing valid render theme XML data.
     * @param themeCallback the theme callback.
     * @return a new RenderTheme which is created by parsing the XML data from the input theme.
     * @throws ThemeException if an error occurs while parsing the render theme XML.
     */
    public static IRenderTheme read(ThemeFile theme, ThemeCallback themeCallback) throws ThemeException {
        XmlThemeBuilder renderThemeHandler = new XmlThemeBuilder(theme, themeCallback);

        try {
            new XMLReaderAdapter().parse(renderThemeHandler, theme.getRenderThemeAsStream());
        } catch (Exception e) {
            log.error("Parsing error", e);
            throw new ThemeException(e.getMessage(), e);
        }

        return renderThemeHandler.mRenderTheme;
    }

    /**
     * Logs the given information about an unknown XML attribute.
     *
     * @param element        the XML element name.
     * @param name           the XML attribute name.
     * @param value          the XML attribute value.
     * @param attributeIndex the XML attribute index position.
     */
    private static void logUnknownAttribute(String element, String name,
                                            String value, int attributeIndex) {
        log.warn("unknown attribute in element {} () : {} = {}",
                element, attributeIndex, name, value);
    }

    private final ArrayList<RuleBuilder> mRulesList = new ArrayList<>();
    private final Stack<Element> mElementStack = new Stack<>();
    private final Stack<RuleBuilder> mRuleStack = new Stack<>();
    private final HashMap<String, RenderStyle> mStyles = new HashMap<>(10);
    private final HashMap<String, TextStyle.TextBuilder<?>> mTextStyles = new HashMap<>(10);
    private final HashMap<String, SymbolStyle.SymbolBuilder<?>> mSymbolStyles = new HashMap<>(10);
    private final HashMap<String, Integer> mColors = new HashMap<>(10);
    private final AreaBuilder<?> mAreaBuilder = AreaStyle.builder();
    private final CircleBuilder<?> mCircleBuilder = CircleStyle.builder();
    private final ExtrusionBuilder<?> mExtrusionBuilder = ExtrusionStyle.builder();
    private final LineBuilder<?> mLineBuilder = LineStyle.builder();
    private final SymbolBuilder<?> mSymbolBuilder = SymbolStyle.builder();
    private final TextBuilder<?> mTextBuilder = TextStyle.builder();

    private RuleBuilder mCurrentRule;
    private TextureAtlas mTextureAtlas;

    int mLevels = 0;
    int mMapBackground = 0xffffffff;
    private float mStrokeScale = 1;
    float mTextScale = 1;

    final ThemeFile mTheme;
    private final ThemeCallback mThemeCallback;
    RenderTheme mRenderTheme;

    final boolean mMapsforgeTheme;
    private final float mScale, mScale2;

    private Set<String> mCategories;
    private XmlRenderThemeStyleLayer mCurrentLayer;
    private XmlRenderThemeStyleMenu mRenderThemeStyleMenu;

    private Map<String, String> mTransformKeyMap = new HashMap<>();
    private Map<Tag, Tag> mTransformTagMap = new HashMap<>();

    public XmlThemeBuilder(ThemeFile theme) {
        this(theme, null);
    }

    public XmlThemeBuilder(ThemeFile theme, ThemeCallback themeCallback) {
        mTheme = theme;
        mThemeCallback = themeCallback;
        mMapsforgeTheme = theme.isMapsforgeTheme();
        mScale = CanvasAdapter.getScale();
        mScale2 = CanvasAdapter.getScale() * 0.5f;
    }

    @Override
    public void endDocument() {
        if (mMapsforgeTheme) {
            // Building rule for Mapsforge themes
            mRulesList.add(buildingRule());
        }

        Rule[] rules = new Rule[mRulesList.size()];
        for (int i = 0, n = rules.length; i < n; i++)
            rules[i] = mRulesList.get(i).onComplete(mMapsforgeTheme ? new int[1] : null);

        mRenderTheme = createTheme(rules);

        mRulesList.clear();
        mStyles.clear();
        mSymbolStyles.clear();
        mTextStyles.clear();
        mRuleStack.clear();
        mElementStack.clear();

        mTextureAtlas = null;
    }

    RenderTheme createTheme(Rule[] rules) {
        return new RenderTheme(mMapBackground, mTextScale, rules, mLevels, mTransformKeyMap, mTransformTagMap, mMapsforgeTheme);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        mElementStack.pop();

        if (ELEMENT_NAME_MATCH.equals(localName)) {
            mRuleStack.pop();
            if (mRuleStack.empty()) {
                if (isVisible(mCurrentRule)) {
                    mRulesList.add(mCurrentRule);
                }
            } else {
                mCurrentRule = mRuleStack.peek();
            }
        } else if (ELEMENT_NAME_STYLE_MENU.equals(localName)) {
            // when we are finished parsing the menu part of the file, we can get the
            // categories to render from the initiator. This allows the creating action
            // to select which of the menu options to choose
            if (null != mTheme.getMenuCallback()) {
                // if there is no callback, there is no menu, so the categories will be null
                mCategories = mTheme.getMenuCallback().getCategories(mRenderThemeStyleMenu);
            }
        }
    }

    @Override
    public void error(SAXParseException exception) {
        log.debug(exception.getMessage());
    }

    @Override
    public void warning(SAXParseException exception) {
        log.debug(exception.getMessage());
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws ThemeException {
        try {
            if (ELEMENT_NAME_RENDER_THEME.equals(localName)) {
                checkState(localName, Element.RENDER_THEME);
                createRenderTheme(localName, attributes);

            } else if (ELEMENT_NAME_MATCH.equals(localName)) {
                checkState(localName, Element.RULE);
                RuleBuilder rule = createRule(localName, attributes);
                if (!mRuleStack.empty() && isVisible(rule)) {
                    mCurrentRule.addSubRule(rule);
                }
                mCurrentRule = rule;
                mRuleStack.push(mCurrentRule);

            } else if ("style-color".equals(localName)) {
                checkState(localName, Element.STYLE);
                handleColorElement(localName, attributes);

            } else if ("style-text".equals(localName)) {
                checkState(localName, Element.STYLE);
                handleTextElement(localName, attributes, true, false);

            } else if ("style-symbol".equals(localName)) {
                checkState(localName, Element.STYLE);
                handleSymbolElement(localName, attributes, true);

            } else if ("style-area".equals(localName)) {
                checkState(localName, Element.STYLE);
                handleAreaElement(localName, attributes, true);

            } else if ("style-line".equals(localName)) {
                checkState(localName, Element.STYLE);
                handleLineElement(localName, attributes, true, false);

            } else if ("outline-layer".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                LineStyle line = createLine(null, localName, attributes, mLevels++, true, false);
                mStyles.put(OUTLINE_STYLE + line.style, line);

            } else if ("area".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleAreaElement(localName, attributes, false);

            } else if ("caption".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleTextElement(localName, attributes, false, true);

            } else if ("circle".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                CircleStyle circle = createCircle(localName, attributes, mLevels++);
                if (isVisible(circle))
                    mCurrentRule.addStyle(circle);

            } else if ("line".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleLineElement(localName, attributes, false, false);

            } else if ("text".equals(localName) || "pathText".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleTextElement(localName, attributes, false, false);

            } else if ("symbol".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleSymbolElement(localName, attributes, false);

            } else if ("outline".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                LineStyle outline = createOutline(attributes.getValue("use"), attributes);
                if (outline != null && isVisible(outline))
                    mCurrentRule.addStyle(outline);

            } else if ("extrusion".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                ExtrusionStyle extrusion = createExtrusion(localName, attributes, mLevels++);
                if (isVisible(extrusion))
                    mCurrentRule.addStyle(extrusion);

            } else if ("lineSymbol".equals(localName)) {
                checkState(localName, Element.RENDERING_INSTRUCTION);
                handleLineElement(localName, attributes, false, true);

            } else if ("atlas".equals(localName)) {
                checkState(localName, Element.ATLAS);
                createAtlas(localName, attributes);

            } else if ("rect".equals(localName)) {
                checkState(localName, Element.RECT);
                createTextureRegion(localName, attributes);

            } else if ("cat".equals(localName)) {
                checkState(qName, Element.RENDERING_STYLE);
                mCurrentLayer.addCategory(getStringAttribute(attributes, "id"));

            } else if ("layer".equals(localName)) {
                // render theme menu layer
                checkState(qName, Element.RENDERING_STYLE);
                boolean enabled = false;
                if (getStringAttribute(attributes, "enabled") != null) {
                    enabled = Boolean.valueOf(getStringAttribute(attributes, "enabled"));
                }
                boolean visible = Boolean.valueOf(getStringAttribute(attributes, "visible"));
                mCurrentLayer = mRenderThemeStyleMenu.createLayer(getStringAttribute(attributes, "id"), visible, enabled);
                String parent = getStringAttribute(attributes, "parent");
                if (null != parent) {
                    XmlRenderThemeStyleLayer parentEntry = mRenderThemeStyleMenu.getLayer(parent);
                    if (null != parentEntry) {
                        for (String cat : parentEntry.getCategories()) {
                            mCurrentLayer.addCategory(cat);
                        }
                        for (XmlRenderThemeStyleLayer overlay : parentEntry.getOverlays()) {
                            mCurrentLayer.addOverlay(overlay);
                        }
                    }
                }

            } else if ("name".equals(localName)) {
                // render theme menu name
                checkState(qName, Element.RENDERING_STYLE);
                mCurrentLayer.addTranslation(getStringAttribute(attributes, "lang"), getStringAttribute(attributes, "value"));

            } else if ("overlay".equals(localName)) {
                // render theme menu overlay
                checkState(qName, Element.RENDERING_STYLE);
                XmlRenderThemeStyleLayer overlay = mRenderThemeStyleMenu.getLayer(getStringAttribute(attributes, "id"));
                if (overlay != null) {
                    mCurrentLayer.addOverlay(overlay);
                }

            } else if ("stylemenu".equals(localName)) {
                checkState(qName, Element.RENDERING_STYLE);
                mRenderThemeStyleMenu = new XmlRenderThemeStyleMenu(getStringAttribute(attributes, "id"),
                        getStringAttribute(attributes, "defaultlang"), getStringAttribute(attributes, "defaultvalue"));

            } else if ("tag-transform".equals(localName)) {
                checkState(qName, Element.TAG_TRANSFORM);
                tagTransform(localName, attributes);

            } else {
                log.error("unknown element: {}", localName);
                throw new SAXException("unknown element: " + localName);
            }
        } catch (SAXException e) {
            throw new ThemeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ThemeException(e.getMessage(), e);
        }
    }

    private RuleBuilder createRule(String localName, Attributes attributes) {
        String cat = null;
        int element = Rule.Element.ANY;
        int closed = Closed.ANY;
        String keys = null;
        String values = null;
        byte zoomMin = 0;
        byte zoomMax = Byte.MAX_VALUE;
        int selector = 0;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("e".equals(name)) {
                String val = value.toUpperCase(Locale.ENGLISH);
                if ("WAY".equals(val))
                    element = Rule.Element.WAY;
                else if ("PLACE".equals(val))
                    element = Rule.Element.PLACE;
                else if ("NODE".equals(val))
                    element = Rule.Element.NODE;
            } else if ("k".equals(name)) {
                if (mMapsforgeTheme) {
                    if (!"*".equals(value))
                        keys = value;
                } else
                    keys = value;
            } else if ("v".equals(name)) {
                if (mMapsforgeTheme) {
                    if (!"*".equals(value))
                        values = value;
                } else
                    values = value;
            } else if ("cat".equals(name)) {
                cat = value;
            } else if ("closed".equals(name)) {
                String val = value.toUpperCase(Locale.ENGLISH);
                if ("YES".equals(val))
                    closed = Closed.YES;
                else if ("NO".equals(val))
                    closed = Closed.NO;
            } else if ("zoom-min".equals(name)) {
                zoomMin = Byte.parseByte(value);
            } else if ("zoom-max".equals(name)) {
                zoomMax = Byte.parseByte(value);
            } else if ("select".equals(name)) {
                if ("first".equals(value))
                    selector |= Selector.FIRST;
                if ("when-matched".equals(value))
                    selector |= Selector.WHEN_MATCHED;
            } else {
                logUnknownAttribute(localName, name, value, i);
            }
        }

        if (closed == Closed.YES)
            element = Rule.Element.POLY;
        else if (closed == Closed.NO)
            element = Rule.Element.LINE;

        validateNonNegative("zoom-min", zoomMin);
        validateNonNegative("zoom-max", zoomMax);
        if (zoomMin > zoomMax)
            throw new ThemeException("zoom-min must be less or equal zoom-max: " + zoomMin);

        RuleBuilder b = RuleBuilder.create(keys, values);
        b.cat(cat);
        b.zoom(zoomMin, zoomMax);
        b.element(element);
        b.select(selector);
        return b;
    }

    private TextureRegion getAtlasRegion(String src) {
        if (mTextureAtlas == null)
            return null;

        TextureRegion texture = mTextureAtlas.getTextureRegion(src);

        if (texture == null)
            log.debug("missing texture atlas item '" + src + "'");

        return texture;
    }

    private void handleLineElement(String localName, Attributes attributes, boolean isStyle, boolean hasSymbol)
            throws SAXException {

        String use = attributes.getValue("use");
        LineStyle style = null;

        if (use != null) {
            style = (LineStyle) mStyles.get(LINE_STYLE + use);
            if (style == null) {
                log.debug("missing line style 'use': " + use);
                return;
            }
        }

        LineStyle line = createLine(style, localName, attributes, mLevels++, false, hasSymbol);

        if (isStyle) {
            mStyles.put(LINE_STYLE + line.style, line);
        } else {
            if (isVisible(line)) {
                mCurrentRule.addStyle(line);
                /* Note 'outline' will not be inherited, it's just a
                 * shortcut to add the outline RenderInstruction. */
                String outlineValue = attributes.getValue("outline");
                if (outlineValue != null) {
                    LineStyle outline = createOutline(outlineValue, attributes);
                    if (outline != null)
                        mCurrentRule.addStyle(outline);
                }
            }
        }
    }

    /**
     * @param line      optional: line style defaults
     * @param level     the drawing level of this instruction.
     * @param isOutline is outline layer
     * @return a new Line with the given rendering attributes.
     */
    private LineStyle createLine(LineStyle line, String elementName, Attributes attributes,
                                 int level, boolean isOutline, boolean hasSymbol) {
        LineBuilder<?> b = mLineBuilder.set(line);
        b.isOutline(isOutline);
        b.level(level);
        b.themeCallback(mThemeCallback);
        String src = null;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name))
                b.style = value;

            else if ("cat".equals(name))
                b.cat(value);

            else if ("src".equals(name))
                src = value;

            else if ("use".equals(name))
                ;// ignore

            else if ("outline".equals(name))
                ;// ignore

            else if ("stroke".equals(name))
                b.color(parseColor(value));

            else if ("width".equals(name) || "stroke-width".equals(name)) {
                b.strokeWidth = parseFloat(value) * mScale2 * mStrokeScale;
                if (line == null) {
                    if (!isOutline)
                        validateNonNegative("width", b.strokeWidth);
                } else {
                    /* use stroke width relative to 'line' */
                    b.strokeWidth += line.width;
                    if (b.strokeWidth <= 0)
                        b.strokeWidth = 1;
                }
            } else if ("cap".equals(name) || "stroke-linecap".equals(name))
                b.cap = Cap.valueOf(value.toUpperCase(Locale.ENGLISH));

            else if ("fix".equals(name))
                b.fixed = parseBoolean(value);

            else if ("half".equals(name))
                b.half = Half.valueOf(value.toUpperCase(Locale.ENGLISH));

            else if ("stipple".equals(name))
                b.stipple = Math.round(parseInt(value) * mScale2 * mStrokeScale);

            else if ("stipple-stroke".equals(name))
                b.stippleColor(parseColor(value));

            else if ("stipple-width".equals(name))
                b.stippleWidth = parseFloat(value);

            else if ("stipple-ratio".equals(name))
                b.stippleRatio = parseFloat(value);

            else if ("fade".equals(name))
                b.fadeScale = Integer.parseInt(value);

            else if ("min".equals(name))
                ; //min = Float.parseFloat(value);

            else if ("blur".equals(name))
                b.blur = parseFloat(value);

            else if ("style".equals(name))
                ; // ignore

            else if ("dasharray".equals(name) || "stroke-dasharray".equals(name)) {
                b.dashArray = parseFloatArray(value);
                for (int j = 0; j < b.dashArray.length; ++j) {
                    b.dashArray[j] = b.dashArray[j] * mScale * mStrokeScale;
                }

            } else if ("symbol-width".equals(name))
                b.symbolWidth = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-height".equals(name))
                b.symbolHeight = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-percent".equals(name))
                b.symbolPercent = Integer.parseInt(value);

            else if ("symbol-color".equals(name))
                b.symbolColor = parseColor(value);

            else if ("symbol-scaling".equals(name))
                ; // no-op

            else if ("repeat".equals(name))
                b.repeat(Boolean.parseBoolean(value));

            else if ("repeat-start".equals(name))
                b.repeatStart = Float.parseFloat(value) * mScale;

            else if ("repeat-gap".equals(name))
                b.repeatGap = Float.parseFloat(value) * mScale;

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        if (b.dashArray != null) {
            // Stroke dash array
            if (b.dashArray.length % 2 != 0) {
                // Odd number of entries is duplicated
                float[] newDashArray = new float[b.dashArray.length * 2];
                System.arraycopy(b.dashArray, 0, newDashArray, 0, b.dashArray.length);
                System.arraycopy(b.dashArray, 0, newDashArray, b.dashArray.length, b.dashArray.length);
                b.dashArray = newDashArray;
            }
            int width = 0;
            int height = (int) (b.strokeWidth);
            if (height < 1)
                height = 1;
            for (float f : b.dashArray) {
                if (f < 1)
                    f = 1;
                width += f;
            }
            Bitmap bitmap = CanvasAdapter.newBitmap(width, height, 0);
            Canvas canvas = CanvasAdapter.newCanvas();
            canvas.setBitmap(bitmap);
            int x = 0;
            boolean transparent = false;
            for (float f : b.dashArray) {
                if (f < 1)
                    f = 1;
                canvas.fillRectangle(x, 0, f, height, transparent ? Color.TRANSPARENT : Color.WHITE);
                x += f;
                transparent = !transparent;
            }
            b.texture = new TextureItem(Utils.potBitmap(bitmap));
            b.texture.mipmap = true;
            b.randomOffset = false;
            b.stipple = width;
            b.stippleWidth = 1;
            b.stippleRatio = 0.5f;
            b.stippleColor = b.fillColor;
        } else {
            if (src != null) {
                b.texture = Utils.loadTexture(mTheme.getRelativePathPrefix(), src, b.symbolWidth, b.symbolHeight, b.symbolPercent, b.symbolColor);
                int width = (int) (b.texture.width + b.repeatGap + b.repeatStart);
                int height = b.texture.height;
                Bitmap bitmap = CanvasAdapter.newBitmap(width, height, 0);
                Canvas canvas = CanvasAdapter.newCanvas();
                canvas.setBitmap(bitmap);
                canvas.drawBitmap(b.texture.bitmap, b.repeatStart, 0);
                b.texture = new TextureItem(Utils.potBitmap(bitmap));
                b.texture.mipmap = true;
                b.fixed = true;
                b.randomOffset = false;
                b.stipple = width;
                if (hasSymbol) {
                    b.stippleWidth = 1;
                    b.stippleRatio = 0.5f;
                }
                b.strokeWidth = height * 0.5f;
                b.stippleColor = Color.WHITE;
                if (!hasSymbol)
                    b.dashArray = new float[] {}; // quick fix for linetex_layer_tex.glsl texture step
            }
        }

        return b.build();
    }

    private void handleAreaElement(String localName, Attributes attributes, boolean isStyle)
            throws SAXException {

        String use = attributes.getValue("use");
        AreaStyle style = null;

        if (use != null) {
            style = (AreaStyle) mStyles.get(AREA_STYLE + use);
            if (style == null) {
                log.debug("missing area style 'use': " + use);
                return;
            }
        }

        AreaStyle area = createArea(style, localName, attributes, mLevels++);

        if (isStyle) {
            mStyles.put(AREA_STYLE + area.style, area);
        } else {
            if (isVisible(area))
                mCurrentRule.addStyle(area);
        }
    }

    /**
     * @return a new Area with the given rendering attributes.
     */
    private AreaStyle createArea(AreaStyle area, String elementName, Attributes attributes,
                                 int level) {
        AreaBuilder<?> b = mAreaBuilder.set(area);
        b.level(level);
        b.themeCallback(mThemeCallback);
        String src = null;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name))
                b.style = value;

            else if ("cat".equals(name))
                b.cat(value);

            else if ("use".equals(name))
                ;// ignore

            else if ("src".equals(name))
                src = value;

            else if ("fill".equals(name))
                b.color(parseColor(value));

            else if ("stroke".equals(name))
                b.strokeColor(parseColor(value));

            else if ("stroke-width".equals(name)) {
                float strokeWidth = Float.parseFloat(value);
                validateNonNegative("stroke-width", strokeWidth);
                b.strokeWidth = strokeWidth * mScale * mStrokeScale;

            } else if ("fade".equals(name))
                b.fadeScale = Integer.parseInt(value);

            else if ("blend".equals(name))
                b.blendScale = Integer.parseInt(value);

            else if ("blend-fill".equals(name))
                b.blendColor(parseColor(value));

            else if ("mesh".equals(name))
                b.mesh(Boolean.parseBoolean(value));

            else if ("symbol-width".equals(name))
                b.symbolWidth = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-height".equals(name))
                b.symbolHeight = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-percent".equals(name))
                b.symbolPercent = Integer.parseInt(value);

            else if ("symbol-color".equals(name))
                b.symbolColor = parseColor(value);

            else if ("symbol-scaling".equals(name))
                ; // no-op

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        b.texture = Utils.loadTexture(mTheme.getRelativePathPrefix(), src, b.symbolWidth, b.symbolHeight, b.symbolPercent, b.symbolColor);

        return b.build();
    }

    private LineStyle createOutline(String style, Attributes attributes) {
        if (style != null) {
            LineStyle line = (LineStyle) mStyles.get(OUTLINE_STYLE + style);
            if (line != null && line.outline) {
                String cat = null;

                for (int i = 0; i < attributes.getLength(); i++) {
                    String name = attributes.getLocalName(i);
                    String value = attributes.getValue(i);

                    if ("cat".equals(name)) {
                        cat = value;
                        break;
                    }
                }

                return line
                        .setCat(cat);
            }
        }
        log.debug("BUG not an outline style: " + style);
        return null;
    }

    private void createAtlas(String elementName, Attributes attributes) throws IOException {
        String img = null;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("img".equals(name)) {
                img = value;
            } else {
                logUnknownAttribute(elementName, name, value, i);
            }
        }
        validateExists("img", img, elementName);

        Bitmap bitmap = CanvasAdapter.getBitmapAsset(mTheme.getRelativePathPrefix(), img);
        if (bitmap != null)
            mTextureAtlas = new TextureAtlas(bitmap);
    }

    private void createTextureRegion(String elementName, Attributes attributes) {
        if (mTextureAtlas == null)
            return;

        String regionName = null;
        Rect r = null;

        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name)) {
                regionName = value;
            } else if ("pos".equals(name)) {
                String[] pos = value.split(" ");
                if (pos.length == 4) {
                    r = new Rect(Integer.parseInt(pos[0]),
                            Integer.parseInt(pos[1]),
                            Integer.parseInt(pos[2]),
                            Integer.parseInt(pos[3]));
                }
            } else {
                logUnknownAttribute(elementName, name, value, i);
            }
        }
        validateExists("id", regionName, elementName);
        validateExists("pos", r, elementName);

        mTextureAtlas.addTextureRegion(regionName.intern(), r);
    }

    private void checkElement(String elementName, Element element) throws SAXException {
        Element parentElement;
        switch (element) {
            case RENDER_THEME:
                if (!mElementStack.empty()) {
                    throw new SAXException(UNEXPECTED_ELEMENT_STACK_NOT_EMPTY + elementName);
                }
                return;

            case RULE:
                parentElement = mElementStack.peek();
                if (parentElement != Element.RENDER_THEME
                        && parentElement != Element.RULE) {
                    throw new SAXException(UNEXPECTED_ELEMENT_RULE_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case STYLE:
                parentElement = mElementStack.peek();
                if (parentElement != Element.RENDER_THEME) {
                    throw new SAXException(UNEXPECTED_ELEMENT_STYLE_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case RENDERING_INSTRUCTION:
                if (mElementStack.peek() != Element.RULE) {
                    throw new SAXException(UNEXPECTED_ELEMENT_RENDERING_INSTRUCTION_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case ATLAS:
                parentElement = mElementStack.peek();
                if (parentElement != Element.RENDER_THEME) {
                    throw new SAXException(UNEXPECTED_ELEMENT_ATLAS_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case RECT:
                parentElement = mElementStack.peek();
                if (parentElement != Element.ATLAS) {
                    throw new SAXException(UNEXPECTED_ELEMENT_RECT_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case RENDERING_STYLE:
                parentElement = mElementStack.peek();
                if (parentElement != Element.RENDER_THEME && parentElement != Element.RENDERING_STYLE) {
                    throw new SAXException(UNEXPECTED_ELEMENT_TAG_RENDERING_STYLE_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;

            case TAG_TRANSFORM:
                parentElement = mElementStack.peek();
                if (parentElement != Element.RENDER_THEME) {
                    throw new SAXException(UNEXPECTED_ELEMENT_TAG_TRANSFORM_PARENT_ELEMENT_MISMATCH + elementName);
                }
                return;
        }

        throw new SAXException("unknown enum value: " + element);
    }

    private void checkState(String elementName, Element element) throws SAXException {
        checkElement(elementName, element);
        mElementStack.push(element);
    }

    private void createRenderTheme(String elementName, Attributes attributes) {
        Integer version = null;
        int mapBackground = Color.WHITE;
        float baseStrokeWidth = 1;
        float baseTextScale = 1;

        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("schemaLocation".equals(name))
                continue;

            if ("version".equals(name))
                version = Integer.parseInt(value);

            else if ("map-background".equals(name)) {
                mapBackground = parseColor(value);
                if (mThemeCallback != null)
                    mapBackground = mThemeCallback.getColor(mapBackground);

            } else if ("base-stroke-width".equals(name))
                baseStrokeWidth = Float.parseFloat(value);

            else if ("base-text-scale".equals(name) || "base-text-size".equals(name))
                baseTextScale = Float.parseFloat(value);

            else
                logUnknownAttribute(elementName, name, value, i);

        }

        validateExists("version", version, elementName);

        int renderThemeVersion = mMapsforgeTheme ? RENDER_THEME_VERSION_MAPSFORGE : RENDER_THEME_VERSION_VTM;
        if (version > renderThemeVersion)
            throw new ThemeException("invalid render theme version:" + version);

        validateNonNegative("base-stroke-width", baseStrokeWidth);
        validateNonNegative("base-text-scale", baseTextScale);

        mMapBackground = mapBackground;
        mStrokeScale = baseStrokeWidth;
        mTextScale = baseTextScale;
    }

    private void handleColorElement(String localName, Attributes attributes) {
        Integer color = null;
        String style = null;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name))
                style = value;

            else if ("color".equals(name))
                color = Color.parseColor(value);

            else
                logUnknownAttribute(localName, name, value, i);
        }

        validateExists("id", style, localName);
        validateExists("color", color, localName);

        log.debug("put color {}", color);
        mColors.put(style, color);
    }

    private void handleTextElement(String localName, Attributes attributes, boolean isStyle,
                                   boolean isCaption) throws SAXException {

        String style = attributes.getValue("use");
        TextBuilder<?> pt = null;

        if (style != null) {
            pt = mTextStyles.get(style);
            if (pt == null) {
                log.debug("missing text style: " + style);
                return;
            }
        }

        TextBuilder<?> b = createText(localName, attributes, isCaption, pt);
        if (isStyle) {
            log.debug("put style {}", b.style);
            mTextStyles.put(b.style, TextStyle.builder().from(b));
        } else {
            TextStyle text = b.buildInternal();
            if (isVisible(text))
                mCurrentRule.addStyle(text);
        }
    }

    /**
     * @param caption ...
     * @return a new Text with the given rendering attributes.
     */
    private TextBuilder<?> createText(String elementName, Attributes attributes,
                                      boolean caption, TextBuilder<?> style) {
        TextBuilder<?> b;
        if (style == null) {
            b = mTextBuilder.reset();
            b.caption = caption;
        } else
            b = mTextBuilder.from(style);
        b.themeCallback(mThemeCallback);
        String symbol = null;

        if (mMapsforgeTheme) {
            // Reset default priority
            b.priority = DEFAULT_PRIORITY;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name))
                b.style = value;

            else if ("cat".equals(name))
                b.cat(value);

            else if ("k".equals(name))
                b.textKey = value.intern();

            else if ("font-family".equals(name))
                b.fontFamily = FontFamily.valueOf(value.toUpperCase(Locale.ENGLISH));

            else if ("style".equals(name) || "font-style".equals(name))
                b.fontStyle = FontStyle.valueOf(value.toUpperCase(Locale.ENGLISH));

            else if ("size".equals(name) || "font-size".equals(name))
                b.fontSize = Float.parseFloat(value);

            else if ("fill".equals(name))
                b.fillColor = parseColor(value);

            else if ("stroke".equals(name))
                b.strokeColor = parseColor(value);

            else if ("stroke-width".equals(name))
                b.strokeWidth = Float.parseFloat(value) * mScale;

            else if ("caption".equals(name))
                b.caption = Boolean.parseBoolean(value);

            else if ("priority".equals(name)) {
                b.priority = Integer.parseInt(value);

                if (mMapsforgeTheme) {
                    // Mapsforge: higher priorities are drawn first (0 = default priority)
                    // VTM: lower priorities are drawn first (0 = highest priority)
                    b.priority = FastMath.clamp(DEFAULT_PRIORITY - b.priority, 0, Integer.MAX_VALUE);
                }

            } else if ("area-size".equals(name))
                b.areaSize = Float.parseFloat(value);

            else if ("dy".equals(name))
                // NB: minus..
                b.dy = -Float.parseFloat(value) * mScale;

            else if ("mandatory".equals(name))
                b.mandatory = Boolean.parseBoolean(value);

            else if ("symbol".equals(name))
                symbol = value;

            else if ("use".equals(name))
                ;/* ignore */

            else if ("symbol-width".equals(name))
                b.symbolWidth = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-height".equals(name))
                b.symbolHeight = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-percent".equals(name))
                b.symbolPercent = Integer.parseInt(value);

            else if ("symbol-color".equals(name))
                b.symbolColor = parseColor(value);

            else if ("symbol-scaling".equals(name))
                ; // no-op

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        validateExists("k", b.textKey, elementName);
        validateNonNegative("size", b.fontSize);
        validateNonNegative("stroke-width", b.strokeWidth);

        if (symbol != null && symbol.length() > 0) {
            String lowValue = symbol.toLowerCase(Locale.ENGLISH);
            if (lowValue.endsWith(".png") || lowValue.endsWith(".svg")) {
                try {
                    b.bitmap = CanvasAdapter.getBitmapAsset(mTheme.getRelativePathPrefix(), symbol, b.symbolWidth, b.symbolHeight, b.symbolPercent, b.symbolColor);
                } catch (Exception e) {
                    log.error("{}: {}", symbol, e.getMessage());
                }
            } else
                b.texture = getAtlasRegion(symbol);
        }

        return b;
    }

    /**
     * @param level the drawing level of this instruction.
     * @return a new Circle with the given rendering attributes.
     */
    private CircleStyle createCircle(String elementName, Attributes attributes, int level) {
        CircleBuilder<?> b = mCircleBuilder.reset();
        b.level(level);
        b.themeCallback(mThemeCallback);

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("r".equals(name) || "radius".equals(name))
                b.radius(Float.parseFloat(value) * mScale * mStrokeScale);

            else if ("cat".equals(name))
                b.cat(value);

            else if ("scale-radius".equals(name))
                b.scaleRadius(Boolean.parseBoolean(value));

            else if ("fill".equals(name))
                b.color(parseColor(value));

            else if ("stroke".equals(name))
                b.strokeColor(parseColor(value));

            else if ("stroke-width".equals(name))
                b.strokeWidth(Float.parseFloat(value) * mScale * mStrokeScale);

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        validateExists("radius", b.radius, elementName);
        validateNonNegative("radius", b.radius);
        validateNonNegative("stroke-width", b.strokeWidth);

        return b.build();
    }

    private void handleSymbolElement(String localName, Attributes attributes, boolean isStyle) throws SAXException {

        String style = attributes.getValue("use");
        SymbolBuilder<?> ps = null;

        if (style != null) {
            ps = mSymbolStyles.get(style);
            if (ps == null) {
                log.debug("missing symbol style: " + style);
                return;
            }
        }

        SymbolBuilder<?> b = createSymbol(localName, attributes, ps);
        if (isStyle) {
            log.debug("put style {}", b.style);
            mSymbolStyles.put(b.style, SymbolStyle.builder().from(b));
        } else {
            SymbolStyle symbol = buildSymbol(b);
            if (symbol != null && isVisible(symbol))
                mCurrentRule.addStyle(symbol);
        }
    }

    /**
     * @return a new Symbol with the given rendering attributes.
     */
    private SymbolBuilder<?> createSymbol(String elementName, Attributes attributes, SymbolBuilder<?> style) {
        SymbolBuilder<?> b;
        if (style == null)
            b = mSymbolBuilder.reset();
        else
            b = mSymbolBuilder.from(style);
        b.themeCallback(mThemeCallback);

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("id".equals(name))
                b.style = value;

            else if ("src".equals(name))
                b.src(value);

            else if ("cat".equals(name))
                b.cat(value);

            else if ("use".equals(name))
                ;// ignore

            else if ("symbol-width".equals(name))
                b.symbolWidth = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-height".equals(name))
                b.symbolHeight = (int) (Integer.parseInt(value) * mScale);

            else if ("symbol-percent".equals(name))
                b.symbolPercent = Integer.parseInt(value);

            else if ("symbol-color".equals(name))
                b.symbolColor = parseColor(value);

            else if ("billboard".equals(name))
                b.billboard(Boolean.parseBoolean(value));

            else if ("rotate".equals(name))
                b.rotate(Boolean.parseBoolean(value));

            else if ("inverse".equals(name))
                b.inverse(Boolean.parseBoolean(value));

            else if ("mandatory".equals(name))
                b.mandatory(Boolean.parseBoolean(value));

            else if ("repeat".equals(name))
                b.repeat(Boolean.parseBoolean(value));

            else if ("repeat-start".equals(name))
                b.repeatStart = (int) (Integer.parseInt(value) * mScale);

            else if ("repeat-gap".equals(name))
                b.repeatGap = (int) (Integer.parseInt(value) * mScale);

            else if ("merge-gap".equals(name))
                b.mergeGap = (int) (Integer.parseInt(value) * mScale);

            else if ("merge-group".equals(name))
                b.mergeGroup(value);

            else if ("merge-group-gap".equals(name))
                b.mergeGroupGap = (int) (Integer.parseInt(value) * mScale);

            else if ("text-overlap".equals(name))
                b.textOverlap = parseBoolean(value);

            else if ("z-index".equals(name))
                b.zIndex = Integer.parseInt(value);

            else if ("symbol-scaling".equals(name))
                ; // no-op

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        validateExists("src", b.src, elementName);

        return b;
    }

    private SymbolStyle buildSymbol(SymbolBuilder<?> b) {
        String lowSrc = b.src.toLowerCase(Locale.ENGLISH);
        if (lowSrc.startsWith(PREFIX_GEN)) {
            return b.src(lowSrc.substring(PREFIX_GEN.length())).build();
        } else if (lowSrc.endsWith(".png") || lowSrc.endsWith(".svg")) {
            try {
                Bitmap bitmap = CanvasAdapter.getBitmapAsset(mTheme.getRelativePathPrefix(), b.src, b.symbolWidth, b.symbolHeight, b.symbolPercent, b.symbolColor);
                if (bitmap != null)
                    return buildSymbol(b, b.src, bitmap);
            } catch (Exception e) {
                log.error("{}: {}", b.src, e.getMessage());
            }
            return null;
        }
        TextureRegion region = getAtlasRegion(b.src);
        if (region != null)
            return b.texture(region).build();
        else
            log.error("{}: failed to get atlas region", b.src);
        return null;
    }

    SymbolStyle buildSymbol(SymbolBuilder<?> b, String src, Bitmap bitmap) {
        return b.bitmap(bitmap).build();
    }

    private ExtrusionStyle createExtrusion(String elementName, Attributes attributes, int level) {
        ExtrusionBuilder<?> b = mExtrusionBuilder.reset();
        b.level(level);
        b.themeCallback(mThemeCallback);

        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            if ("cat".equals(name))
                b.cat(value);

            else if ("side-color".equals(name))
                b.colorSide(parseColor(value));

            else if ("top-color".equals(name))
                b.colorTop(parseColor(value));

            else if ("line-color".equals(name))
                b.colorLine(parseColor(value));

            else if ("hsv-h".equals(name))
                b.hsvHue(Double.parseDouble(value));

            else if ("hsv-s".equals(name))
                b.hsvSaturation(Double.parseDouble(value));

            else if ("hsv-v".equals(name))
                b.hsvValue(Double.parseDouble(value));

            else if ("default-height".equals(name))
                b.defaultHeight(Integer.parseInt(value));

            else
                logUnknownAttribute(elementName, name, value, i);
        }

        return b.build();
    }

    private String getStringAttribute(Attributes attributes, String name) {
        for (int i = 0; i < attributes.getLength(); ++i) {
            if (attributes.getLocalName(i).equals(name)) {
                return attributes.getValue(i);
            }
        }
        return null;
    }

    /**
     * A style is visible if categories is not set or the style has no category
     * or the categories contain the style's category.
     */
    private boolean isVisible(RenderStyle renderStyle) {
        return mCategories == null || renderStyle.cat == null || mCategories.contains(renderStyle.cat);
    }

    /**
     * A rule is visible if categories is not set or the rule has no category
     * or the categories contain the rule's category.
     */
    private boolean isVisible(RuleBuilder rule) {
        return mCategories == null || rule.cat == null || mCategories.contains(rule.cat);
    }

    private static float[] parseFloatArray(String dashString) {
        String[] dashEntries = dashString.split(",");
        float[] dashIntervals = new float[dashEntries.length];
        for (int i = 0; i < dashEntries.length; ++i) {
            dashIntervals[i] = Float.parseFloat(dashEntries[i]);
        }
        return dashIntervals;
    }

    private int parseColor(String value) {
        Integer color;
        if (value.startsWith("color:")) {
            String style = value.substring(6);
            color = mColors.get(style);
            if (color == null) {
                log.error("missing color style: " + style);
                color = 0;
            }
        } else {
            color = Color.parseColor(value);
        }
        return color;
    }

    private void tagTransform(String localName, Attributes attributes) {
        String k, v, libK, libV;
        k = v = libK = libV = null;

        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            switch (name) {
                case "match-k":
                case "k":
                    k = value;
                    break;
                case "v":
                    v = value;
                    break;
                case "k-lib":
                    libK = value;
                    break;
                case "v-lib":
                    libV = value;
                    break;
                default:
                    logUnknownAttribute(localName, name, value, i);
            }
        }

        if (k == null || k.isEmpty() || libK == null || libK.isEmpty()) {
            log.debug("empty key in element " + localName);
            return;
        }

        if (v == null && libV == null) {
            mTransformKeyMap.put(k, libK);
        } else {
            mTransformTagMap.put(new Tag(k, v), new Tag(libK, libV));
        }
    }

    private static void validateNonNegative(String name, float value) {
        if (value < 0)
            throw new ThemeException(name + " must not be negative: "
                    + value);
    }

    private static void validateExists(String name, Object obj, String elementName) {
        if (obj == null)
            throw new ThemeException("missing attribute " + name
                    + " for element: " + elementName);
    }

    /**
     * Building rule for Mapsforge themes.
     */
    private RuleBuilder buildingRule() {
        ExtrusionBuilder<?> b = mExtrusionBuilder.reset();
        b.level(mLevels++);
        b.themeCallback(mThemeCallback);
        b.colorLine(0xffd9d8d6);
        b.colorSide(0xeaecebe9);
        b.colorTop(0xeaf9f8f6);
        RuleBuilder rule = new RuleBuilder(RuleBuilder.RuleType.POSITIVE, new String[]{Tag.KEY_BUILDING, Tag.KEY_BUILDING_PART}, new String[]{});
        rule.element(Rule.Element.WAY).zoom((byte) 17, Byte.MAX_VALUE).style(b);
        return rule;
    }
}
