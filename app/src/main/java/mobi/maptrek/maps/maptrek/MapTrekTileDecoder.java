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

package mobi.maptrek.maps.maptrek;

/*
 * Originated from OpenScienceMap project (http://www.opensciencemap.org).
 * Copyright 2013 Hannes Janetzek
 */

import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.source.PbfDecoder;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;


class MapTrekTileDecoder extends PbfDecoder {
    private static final Logger log = LoggerFactory.getLogger(MapTrekTileDecoder.class);

    private static final int TAG_TILE_VERSION = 1;
    //private static final int TAG_TILE_TIMESTAMP = 2;
    //private static final int TAG_TILE_ISWATER = 3;

    private static final int TAG_TILE_NUM_TAGS = 11;
    private static final int TAG_TILE_NUM_KEYS = 12;
    private static final int TAG_TILE_NUM_VALUES = 13;

    private static final int TAG_TILE_TAG_KEYS = 14;
    private static final int TAG_TILE_TAG_VALUES = 15;
    private static final int TAG_TILE_TAGS = 16;

    private static final int TAG_TILE_LINE = 21;
    private static final int TAG_TILE_POLY = 22;
    private static final int TAG_TILE_POINT = 23;
    /**
     * since version 5
     */
    private static final int TAG_TILE_MESH = 24;

    private static final int TAG_ELEM_NUM_INDICES = 1;
    private static final int TAG_ELEM_NUM_TAGS = 2;
    /**
     * since version 5
     */
    private static final int TAG_ELEM_NUM_COORDINATES = 3;
    private static final int TAG_ELEM_ID = 4;
    private static final int TAG_ELEM_TAGS = 11;
    private static final int TAG_ELEM_INDEX = 12;
    private static final int TAG_ELEM_COORDINATES = 13;
    private static final int TAG_ELEM_LAYER = 21;

    private static final int TAG_ELEM_LABEL = 31;
    private static final int TAG_ELEM_KIND = 32;
    private static final int TAG_ELEM_TYPE = 22;
    private static final int TAG_ELEM_ELEVATION = 33;
    private static final int TAG_ELEM_HEIGHT = 34;
    private static final int TAG_ELEM_MIN_HEIGHT = 35;
    private static final int TAG_ELEM_BUILDING_COLOR = 36;
    private static final int TAG_ELEM_ROOF_COLOR = 37;
    private static final int TAG_ELEM_HOUSE_NUMBER = 38;
    /**
     * since version 6
     */
    private static final int TAG_ELEM_ROOF_HEIGHT = 39;
    private static final int TAG_ELEM_ROOF_SHAPE = 40;
    private static final int TAG_ELEM_ROOF_DIRECTION = 41;
    private static final int TAG_ELEM_ROOF_ACROSS = 42;
    /**
     * since version 7
     */
    private static final int TAG_ELEM_RESERVED5 = 5;
    private static final int TAG_ELEM_RESERVED6 = 6;
    private static final int TAG_ELEM_RESERVED7 = 7;
    private static final int TAG_ELEM_RESERVED8 = 8;
    private static final int TAG_ELEM_RESERVED9 = 9;
    private static final int TAG_ELEM_AREA = 23;
    private static final int TAG_ELEM_DEPTH = 24;

    private int[] mSArray = new int[100];

    private Tile mTile;

    private final ExtendedMapElement mElem;
    private final GeometryBuffer mLabel;

    private final TagSet mTileTags;
    private ITileDataSink mMapDataSink;

    // scale coordinates to tile size
    private final static float REF_TILE_SIZE = 4096.0f;
    private final float mScaleFactor = REF_TILE_SIZE / Tile.SIZE;

    MapTrekTileDecoder() {
        mElem = new ExtendedMapElement();
        mLabel = new GeometryBuffer(100, 1);
        mTileTags = new TagSet(100);
    }

    @Override
    public boolean decode(Tile tile, ITileDataSink sink, InputStream is)
            throws IOException {

        //readUnsignedInt(is, buffer);
        setInputStream(is);

        mTile = tile;
        mMapDataSink = sink;

        mTileTags.clearAndNullTags();

        int val;
        int numTags = 0;
        int numKeys = -1;
        int numValues = -1;

        int curKey = 0;
        int curValue = 0;

        String[] keys = null;
        String[] values = null;

        while (hasData() && (val = decodeVarint32()) > 0) {
            // read tag and wire type
            int tag = (val >> 3);
            //log.debug("tag: " + tag);

            switch (tag) {
                case TAG_TILE_LINE:
                case TAG_TILE_POLY:
                case TAG_TILE_POINT:
                case TAG_TILE_MESH:
                    decodeTileElement(tile, tag);
                    break;

                case TAG_TILE_TAG_KEYS:
                    if (keys == null || curKey >= numKeys) {
                        log.error("{} wrong number of keys {}", mTile, numKeys);
                        return false;
                    }
                    keys[curKey++] = decodeString().intern();
                    break;

                case TAG_TILE_TAG_VALUES:
                    if (values == null || curValue >= numValues) {
                        log.error("{} wrong number of values {}", mTile, numValues);
                        return false;
                    }
                    values[curValue++] = decodeString();
                    break;

                case TAG_TILE_NUM_TAGS:
                    numTags = decodeVarint32();
                    log.debug("num tags {}", numTags);
                    break;

                case TAG_TILE_NUM_KEYS:
                    numKeys = decodeVarint32();
                    log.debug("num keys {}", numKeys);
                    keys = new String[numKeys];
                    break;

                case TAG_TILE_NUM_VALUES:
                    numValues = decodeVarint32();
                    log.debug("num values {}", numValues);
                    values = new String[numValues];
                    break;

                case TAG_TILE_TAGS:
                    int len = numTags * 2;
                    if (mSArray.length < len)
                        mSArray = new int[len];

                    decodeVarintArray(len, mSArray);
                    if (!decodeTileTags(numTags, mSArray, keys, values)) {
                        log.error("{} invalid tags", mTile);
                        return false;
                    }
                    break;

                case TAG_TILE_VERSION:
                    decodeVarint32();
                    break;

                default:
                    log.error("{} invalid type for tile: {}", mTile, tag);
                    return false;
            }
        }

        return true;
    }

    private boolean decodeTileTags(int numTags, int[] tagIdx, String[] keys, String[] vals) {
        Tag tag;
        for (int i = 0, n = (numTags << 1); i < n; i += 2) {
            int k = tagIdx[i];
            int v = tagIdx[i + 1];
            String key, val;

            if (k < Tags.ATTRIB_OFFSET) {
                if (k > Tags.MAX_KEY) {
                    log.warn("unknown tag key: {}", k);
                    key = String.valueOf(k);
                } else {
                    key = Tags.keys[k];
                }
            } else {
                k -= Tags.ATTRIB_OFFSET;
                if (k >= keys.length)
                    return false;
                key = keys[k];
            }

            if (v < Tags.ATTRIB_OFFSET) {
                if (v > Tags.MAX_VALUE) {
                    log.warn("unknown tag value: {}", v);
                    val = "";
                } else {
                    val = Tags.values[v];
                }
            } else {
                v -= Tags.ATTRIB_OFFSET;
                if (v >= vals.length)
                    return false;
                val = vals[v];
            }

            // FIXME filter out all variable tags
            // might depend on theme though
            if (Tag.KEY_NAME.equals(key)
                    || Tag.KEY_HOUSE_NUMBER.equals(key)
                    || Tag.KEY_REF.equals(key)
                    || Tag.KEY_ELE.equals(key)
                    || Tag.KEY_HEIGHT.equals(key)
                    || Tag.KEY_MIN_HEIGHT.equals(key)
                    || Tag.KEY_DEPTH.equals(key))
                tag = new Tag(key, val, false);
            else
                tag = new Tag(key, val, false, true);

            mTileTags.add(tag);
        }

        return true;
    }

    private int decodeWayIndices(int indexCnt, boolean shift) throws IOException {
        mElem.ensureIndexSize(indexCnt, false);
        decodeVarintArray(indexCnt, mElem.index);

        int[] index = mElem.index;
        int coordCnt = 0;

        if (shift) {
            for (int i = 0; i < indexCnt; i++) {
                coordCnt += index[i];
                index[i] *= 2;
            }
        }
        // set end marker
        if (indexCnt < index.length)
            index[indexCnt] = -1;

        return coordCnt;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean decodeTileElement(Tile tile, int geomType) throws IOException {
        mElem.clearData();
        mElem.tags.clear();

        int bytes = decodeVarint32();

        int end = position() + bytes;
        int numIndices = 1;
        int numTags = 1;

        //boolean skip = false;
        boolean fail = false;

        int coordCnt = 0;
        if (geomType == TAG_TILE_POINT) {
            coordCnt = 1;
            mElem.index[0] = 2;
        }

        int kind = -1;
        int type = -1;
        long area = -1;
        int depth = -1;
        int color = 0; // transparent black is considered as 'not defined'
        String houseNumber = null;

        while (position() < end) {
            // read tag and wire type
            int val = decodeVarint32();
            if (val == 0)
                break;

            int tag = (val >> 3);
            //log.debug("e tag: " + tag);

            switch (tag) {
                case TAG_ELEM_ID:
                    mElem.id = decodeVarint64();
                    break;

                case TAG_ELEM_RESERVED5:
                    decodeVarint64();
                    break;

                case TAG_ELEM_RESERVED6:
                    decodeVarint32();
                    break;

                case TAG_ELEM_RESERVED7:
                    //noinspection ResultOfMethodCallIgnored
                    deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_RESERVED8:
                    decodeBool();
                    break;

                case TAG_ELEM_RESERVED9:
                    decodeString();
                    break;

                case TAG_ELEM_TAGS:
                    if (!decodeElementTags(numTags))
                        return false;
                    break;

                case TAG_ELEM_NUM_INDICES:
                    numIndices = decodeVarint32();
                    break;

                case TAG_ELEM_NUM_TAGS:
                    numTags = decodeVarint32();
                    break;

                case TAG_ELEM_NUM_COORDINATES:
                    coordCnt = decodeVarint32();
                    break;

                case TAG_ELEM_INDEX:
                    if (geomType == TAG_TILE_MESH) {
                        decodeWayIndices(numIndices, false);
                    } else {
                        coordCnt = decodeWayIndices(numIndices, true);
                        // otherwise using TAG_ELEM_NUM_COORDINATES
                    }
                    break;

                case TAG_ELEM_COORDINATES:
                    if (coordCnt == 0) {
                        log.debug("{} no coordinates", mTile);
                    }

                    if (geomType == TAG_TILE_MESH) {
                        mElem.ensurePointSize((coordCnt * 3 / 2), false);
                        int cnt = decodeInterleavedPoints3D(mElem.points, 1);

                        if (cnt != (3 * coordCnt)) {
                            log.error("{} wrong number of coordintes {}/{}", mTile, coordCnt, cnt);
                            fail = true;
                        }
                        mElem.pointNextPos = cnt;
                    } else {
                        mElem.ensurePointSize(coordCnt, false);
                        int cnt = decodeInterleavedPoints(mElem, mScaleFactor);

                        if (cnt != coordCnt) {
                            log.error("{} wrong number of coordinates {}/{}", mTile, coordCnt, cnt);
                            fail = true;
                        }
                    }
                    break;

                case TAG_ELEM_LABEL:
                    int cnt = decodeInterleavedPoints(mLabel, mScaleFactor);
                    if (cnt != 1) {
                        log.warn("{} wrong number of coordinates for label: {}", mTile, cnt);
                    } else {
                        //log.error("label pos: {},{}", mLabel.getPointX(0), mLabel.getPointY(0));
                        mElem.setLabelPosition(mLabel.getPointX(0), mLabel.getPointY(0));
                    }
                    break;

                case TAG_ELEM_KIND:
                    kind = decodeVarint32();
                    break;

                case TAG_ELEM_TYPE:
                    type = decodeVarint32();
                    break;

                case TAG_ELEM_LAYER:
                    mElem.layer = decodeVarint32();
                    break;

                case TAG_ELEM_AREA:
                    area = decodeVarint64();
                    break;

                case TAG_ELEM_ELEVATION:
                    mElem.elevation = deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_DEPTH:
                    depth = deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_HEIGHT:
                    mElem.buildingHeight = deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_MIN_HEIGHT:
                    mElem.buildingMinHeight = deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_BUILDING_COLOR:
                    color = decodeVarint32();
                    break;

                case TAG_ELEM_ROOF_COLOR:
                    mElem.roofColor = decodeVarint32();
                    break;

                case TAG_ELEM_ROOF_HEIGHT:
                    mElem.roofHeight = deZigZag(decodeVarint32());
                    break;

                case TAG_ELEM_ROOF_SHAPE:
                    int v = deZigZag(decodeVarint32()) - 1;
                    if (v < Tags.roofShapes.length)
                        mElem.roofShape = Tags.roofShapes[v];
                    break;

                case TAG_ELEM_ROOF_DIRECTION:
                    mElem.roofDirection = deZigZag(decodeVarint32()) * 0.1f;
                    break;

                case TAG_ELEM_ROOF_ACROSS:
                    mElem.roofOrientationAcross = decodeBool();
                    break;

                case TAG_ELEM_HOUSE_NUMBER:
                    houseNumber = decodeString();
                    break;

                default:
                    log.debug("{} invalid type for way: {}", mTile, tag);
            }
        }

        if (fail || (numTags == 0 && houseNumber == null) || numIndices == 0) {
            log.error("{} failed: bytes:{} tags:{} ({},{})",
                    mTile, bytes,
                    mElem.tags,
                    numIndices,
                    coordCnt);
            return false;
        }

        switch (geomType) {
            case TAG_TILE_LINE:
                mElem.type = GeometryBuffer.GeometryType.LINE;
                break;
            case TAG_TILE_POLY:
                mElem.type = GeometryBuffer.GeometryType.POLY;
                break;
            case TAG_TILE_POINT:
                mElem.type = GeometryBuffer.GeometryType.POINT;
                break;
            case TAG_TILE_MESH:
                mElem.type = GeometryBuffer.GeometryType.TRIS;
                break;
        }

        if (kind != 0) {
            mElem.kind = kind;
            boolean place_road_building = (kind & 0x00000007) > 0;
            // remove technical kinds
            kind = (kind & 0x7fffffff) >> 3;
            boolean someKind = kind > 0;
            boolean hasKind = false;
            if (kind > 0) {
                int tileZoom = FastMath.clamp(tile.zoomLevel, 0, 17);
                for (int i = 0; i < 16; i++) {
                    if ((kind & 0x00000001) > 0) {
                        int zoom = Tags.kindZooms[i];
                        if (zoom <= tileZoom) {
                            hasKind = true;
                        } else {
                            for (int t = 0; t < Tags.kindTypes[i].length; t++) {
                                Tag tag = Tags.typeTags[Tags.kindTypes[i][t]];
                                if (tag.value.equals("theme_park") || tag.value.equals("zoo"))
                                    continue;
                                if (mElem.tags.remove(tag) && tag instanceof ExtendedTag) {
                                    while ((tag = ((ExtendedTag) tag).next) != null)
                                        mElem.tags.remove(tag);
                                }
                            }
                        }
                    }
                    kind = kind >> 1;
                }
            }
            for (Tag tag : Tags.typeAliasTags)
                mElem.tags.remove(tag);

            if ((mElem.tags.size() == 0 && houseNumber == null) || !(hasKind || place_road_building || geomType != TAG_TILE_POINT))
                return true;

            if (someKind) // required for building names
                mElem.tags.add(Tags.TAG_KIND);
            if (hasKind) // required for building numbers
                mElem.tags.add(Tags.TAG_FEATURE);
        }

        if (type > 0) {
            for (Tag tag : Tags.typeAliasTags) {
                mElem.tags.remove(tag);
                if (tag instanceof ExtendedTag) {
                    while ((tag = ((ExtendedTag) tag).next) != null)
                        mElem.tags.remove(tag);
                }
            }
            if (Tags.typeSelectable[type] || !Tags.isVisible(type)) {
                Tag tag = Tags.typeTags[type];
                if (tag.value.equals("theme_park") || tag.value.equals("zoo")) {
                    int tileZoom = FastMath.clamp(tile.zoomLevel, 0, 17);
                    if (Tags.isVisible(type, tileZoom))
                        mElem.tags.add(Tags.TAG_FEATURE);
                } else if (mElem.tags.remove(tag) && tag instanceof ExtendedTag) {
                    while ((tag = ((ExtendedTag) tag).next) != null)
                        mElem.tags.remove(tag);
                }
            }
        }

        if (houseNumber != null)
            mElem.tags.add(new Tag(Tag.KEY_HOUSE_NUMBER, houseNumber, false));

        if (area > 0) { // we set it here because extra tags should be added after filtering
            mElem.featureArea = area;
            mElem.tags.add(Tags.TAG_MEASURED); // required for style filtering
        }

        if (depth > 0) { // we set it here because extra tags should be added after filtering
            mElem.depth = depth;
            mElem.tags.add(Tags.TAG_DEPTH); // required for style filtering
        }

        if (color != 0) {
            if (Tags.isRoute(mElem.kind))
                mElem.tags.add(new Tag(Tag.KEY_ROUTE_COLOR, String.valueOf(color), false));
            else
                mElem.buildingColor = color;
        }

        mMapDataSink.process(mElem);

        return true;
    }

    private boolean decodeElementTags(int numTags) throws IOException {
        if (mSArray.length < numTags)
            mSArray = new int[numTags];
        int[] tagIds = mSArray;

        decodeVarintArray(numTags, tagIds);

        int max = mTileTags.size() - 1;

        for (int i = 0; i < numTags; i++) {
            int idx = tagIds[i];

            if (idx < 0 || idx > max) {
                log.error("{} invalid tag: {} {}", mTile, idx, i);
                return false;
            }
            Tag tag = mTileTags.get(idx);
            if ("contour".equals(tag.key))
                mElem.isContour = true;
            if ("building".equals(tag.key))
                mElem.isBuilding = true;
            if ("building:part".equals(tag.key))
                mElem.isBuildingPart = true;
            mElem.tags.add(tag);
        }

        return true;
    }
}
