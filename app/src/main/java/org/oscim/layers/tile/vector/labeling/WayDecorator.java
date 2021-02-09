/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2018 devemux86
 * Copyright 2020 Andrey Novikov
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
package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.Tile;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.geom.GeometryUtils;
import org.oscim.utils.geom.LineClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PointlessArithmeticExpression")
public final class WayDecorator {
    private static final Logger log = LoggerFactory.getLogger(WayDecorator.class);

    public static void renderText(LineClipper clipper, float[] coordinates, String label,
                                  TextStyle text, int pos, int len, LabelTileData ld) {
        //TextItem items = textItems;
        TextItem t = null;

        // calculate the way name length plus some margin of safety
        float labelWidth = -1;
        float minWidth = Tile.SIZE / 10f;

        boolean placed = false;
        float longestSegmentLength = 0;
        float longestSegmentPrevX = coordinates[pos + 0];
        float longestSegmentPrevY = coordinates[pos + 1];
        float longestSegmentCurX = len > 2 ? coordinates[pos + 2] : longestSegmentPrevX;
        float longestSegmentCurY = len > 2 ? coordinates[pos + 3] : longestSegmentPrevY;

        // find way segments long enough to draw the way name on them
        for (int i = pos; i < pos + len - 2; i += 2) {
            // get the first way point coordinates
            float prevX = coordinates[i + 0];
            float prevY = coordinates[i + 1];

            byte edge = 0;

            // get the current way point coordinates
            float curX = coordinates[i + 2];
            float curY = coordinates[i + 3];

            int last = i;

            // calculate the length of the current segment (Euclidean distance)
            float vx = prevX - curX;
            float vy = prevY - curY;
            if (vx == 0 && vy == 0)
                continue;

            float a = (float) Math.sqrt(vx * vx + vy * vy);

            // only if not cur segment crosses edge
            vx /= a;
            vy /= a;

            // add additional segments if possible
            for (int j = i + 4; j < pos + len; j += 2) {
                float nextX = coordinates[j + 0];
                float nextY = coordinates[j + 1];

                float wx = nextX - curX;
                float wy = nextY - curY;
                if (wx == 0 && wy == 0)
                    continue;

                float area = GeometryUtils.area(prevX, prevY, curX, curY, nextX, nextY);

                if (area > 1000) {
                    //log.debug("b: " + string + " " + area );
                    break;
                }

                a = (float) Math.sqrt(wx * wx + wy * wy);
                wx /= a;
                wy /= a;

                // avoid adding short segments that add much area
                if (area / 2 > a * a) {
                    //log.debug("a: " +string + " " + area + " " + a*a);
                    break;
                }

                float ux = vx + wx;
                float uy = vy + wy;
                float diff = wx * uy - wy * ux;

                // maximum angle between segments
                if (diff > 0.1 || diff < -0.1) {
                    //log.debug("c: " + string + " " + area );
                    break;
                }
                curX = nextX;
                curY = nextY;
                last = j - 2;

            }

            vx = curX - prevX;
            vy = curY - prevY;
            a = (float) Math.sqrt(vx * vx + vy * vy);

            float segmentLength = a;

            if (segmentLength > longestSegmentLength) {
                longestSegmentLength = segmentLength;
                longestSegmentPrevX = prevX;
                longestSegmentPrevY = prevY;
                longestSegmentCurX = curX;
                longestSegmentCurY = curY;
            }

            if (!text.caption && segmentLength < minWidth) {
                continue;
            }

            if (labelWidth < 0) {
                labelWidth = text.paint.measureText(label);
            }

            if (segmentLength < labelWidth * (text.caption ? 0.1 : 0.5)) {
                continue;
            }

            float x1, y1, x2, y2;
            if (prevX < curX) {
                x1 = prevX;
                y1 = prevY;
                x2 = curX;
                y2 = curY;
            } else {
                x1 = curX;
                y1 = curY;
                x2 = prevX;
                y2 = prevY;
            }

            TextItem n = TextItem.pool.get();

            // link items together
            //if (t != null) {
            //    t.n1 = n;
            //    n.n2 = t;
            //}

            t = n;
            t.x = x1 + (x2 - x1) / 2f;
            t.y = y1 + (y2 - y1) / 2f;
            t.label = label;
            t.text = text;
            t.width = labelWidth;
            t.height = text.fontHeight;
            t.x1 = x1;
            t.y1 = y1;
            t.x2 = x2;
            t.y2 = y2;
            t.length = (short) segmentLength;
            t.lines = 1;

            t.edges = edge;
            ld.labels.push(t);

            placed = true;

            i = last;
        }

        if (!placed && text.mandatory && longestSegmentLength > 0) {
            float x1, y1, x2, y2;
            if (longestSegmentPrevX < longestSegmentCurX) {
                x1 = longestSegmentPrevX;
                y1 = longestSegmentPrevY;
                x2 = longestSegmentCurX;
                y2 = longestSegmentCurY;
            } else {
                x1 = longestSegmentCurX;
                y1 = longestSegmentCurY;
                x2 = longestSegmentPrevX;
                y2 = longestSegmentPrevY;
            }

            t = TextItem.pool.get();
            t.x = x1 + (x2 - x1) / 2f;
            t.y = y1 + (y2 - y1) / 2f;
            t.label = label;
            t.text = text;
            t.width = labelWidth;
            t.height = text.fontHeight;
            t.x1 = x1;
            t.y1 = y1;
            t.x2 = x2;
            t.y2 = y2;
            t.length = (short) longestSegmentLength;
            t.lines = 1;

            t.edges = 0;
            ld.labels.push(t);
        }
    }

    public static void renderSymbol(LineClipper clipper, float[] coordinates, SymbolStyle symbol,
                                    int pos, int len, LabelTileData ld) {
        float skipPixels = symbol.repeatStart;

        float symbolWidth = symbol.symbolWidth;
        if (symbolWidth == 0f) {
            if (symbol.bitmap != null)
                symbolWidth = symbol.bitmap.getWidth();
            else
                symbolWidth = symbol.texture.rect.w;
        }
        symbolWidth += 4;

        // do not draw symbols on line edges to prevent overlapping on tile edges
        if (skipPixels < symbolWidth)
            skipPixels = symbolWidth;

        // get the first way point coordinates
        float previousX = coordinates[pos + 0];
        float previousY = coordinates[pos + 1];

        boolean placed = false;
        float lineLength = 0;
        float segmentLengthRemaining;
        float segmentSkipPercentage;
        float theta = 0;

        for (int i = pos; i < pos + len - 2; i += 2) {
            // get the current way point coordinates
            float currentX = coordinates[i + 2];
            float currentY = coordinates[i + 3];

            // calculate the length of the current segment (Euclidean distance)
            float diffX = currentX - previousX;
            float diffY = currentY - previousY;
            segmentLengthRemaining = (float) Math.sqrt(diffX * diffX + diffY * diffY);
            lineLength += segmentLengthRemaining;

            // draw non-repeated symbols in the middle of the line, so only calculate line length on the first pass
            if (symbol.repeat) {
                while (segmentLengthRemaining - skipPixels >= symbolWidth) {
                    // calculate the percentage of the current segment to skip
                    segmentSkipPercentage = skipPixels > 0f ? skipPixels / segmentLengthRemaining : 0f;

                    // move the previous point forward towards the current point
                    previousX += diffX * segmentSkipPercentage;
                    previousY += diffY * segmentSkipPercentage;

                    if (symbol.rotate) {
                        // if we do not rotate theta will be 0, which is correct
                        theta = (float) Math.toDegrees(Math.atan2(currentY - previousY, currentX - previousX));
                        if (symbol.inverse)
                            theta = (float) FastMath.clampDegree(theta + 180);
                    }

                    float x = previousX;
                    float y = previousY;

                    if (x >= 0 && x <= Tile.SIZE && y >= 0 && y <= Tile.SIZE) {
                        SymbolItem s = SymbolItem.pool.get();
                        if (symbol.bitmap != null)
                            s.set(x, y, symbol.bitmap, symbol.hash, theta, symbol.billboard, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                        else
                            s.set(x, y, symbol.texture, symbol.hash, theta, symbol.billboard, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                        ld.symbols.push(s);
                    }

                    placed = true;

                    // recalculate the distances
                    diffX = currentX - previousX;
                    diffY = currentY - previousY;

                    // recalculate the remaining length of the current segment
                    segmentLengthRemaining -= skipPixels;

                    // set the amount of pixels to skip before repeating the symbol
                    skipPixels = symbol.repeatGap + symbolWidth;
                }

                skipPixels -= segmentLengthRemaining;
            }
            // set the previous way point coordinates for the next loop
            previousX = currentX;
            previousY = currentY;
        }

        if (placed || (!symbol.mandatory && lineLength < skipPixels) || lineLength < 1)
            return;

        //if (!placed && symbol.mandatory && lineLength > 0) {
            // put symbol int the center of a line
            float half = lineLength / 2;

            // get the first way point coordinates
            previousX = coordinates[pos + 0];
            previousY = coordinates[pos + 1];

            for (int i = pos; i < pos + len - 2; i += 2) {
                // get the current way point coordinates
                float currentX = coordinates[i + 2];
                float currentY = coordinates[i + 3];

                // calculate the length of the current segment (Euclidean distance)
                float diffX = currentX - previousX;
                float diffY = currentY - previousY;
                segmentLengthRemaining = (float) Math.sqrt(diffX * diffX + diffY * diffY);

                if (half <= segmentLengthRemaining) {
                    // calculate the percentage of the current segment to skip
                    segmentSkipPercentage = half / segmentLengthRemaining;

                    // move the previous point forward towards the current point
                    previousX += diffX * segmentSkipPercentage;
                    previousY += diffY * segmentSkipPercentage;

                    if (symbol.rotate) {
                        // if we do not rotate theta will be 0, which is correct
                        theta = (float) Math.toDegrees(Math.atan2(currentY - previousY, currentX - previousX));
                        if (symbol.inverse)
                            theta = (float) FastMath.clampDegree(theta + 180);
                    }

                    float x = previousX;
                    float y = previousY;

                    if (x >= 0 && x <= Tile.SIZE && y >= 0 && y <= Tile.SIZE) {
                        SymbolItem s = SymbolItem.pool.get();
                        if (symbol.bitmap != null)
                            s.set(x, y, symbol.bitmap, symbol.hash, theta, symbol.billboard, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                        else
                            s.set(x, y, symbol.texture, symbol.hash, theta, symbol.billboard, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap, symbol.zIndex);
                        ld.symbols.push(s);
                    }

                    return;
                }

                half -= segmentLengthRemaining;

                // set the previous way point coordinates for the next loop
                previousX = currentX;
                previousY = currentY;
            }
        //}
    }

    private WayDecorator() {
        throw new IllegalStateException();
    }
}
