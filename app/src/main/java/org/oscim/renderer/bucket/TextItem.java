/*
 * Copyright 2012 Hannes Janetzek
 * Copyright 2017 Luca Osten
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
package org.oscim.renderer.bucket;

import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

public class TextItem extends Inlist<TextItem> {
    //static final Logger log = LoggerFactory.getLogger(TextItem.class);
    private final static int MAX_POOL = 250;

    public final static SyncPool<TextItem> pool = new SyncPool<TextItem>(MAX_POOL) {

        @Override
        protected TextItem createItem() {
            return new TextItem();
        }

        @Override
        protected boolean clearItem(TextItem ti) {
            // drop references
            ti.label = null;
            ti.text = null;
            ti.lineSplits = null;
            //ti.n1 = null;
            //ti.n2 = null;
            return true;
        }
    };

    public static TextItem copy(TextItem orig) {

        TextItem ti = pool.get();

        ti.x = orig.x;
        ti.y = orig.y;

        ti.x1 = orig.x1;
        ti.y1 = orig.y1;
        ti.x2 = orig.x2;
        ti.y2 = orig.y2;

        ti.lines = orig.lines;
        ti.lineSplits = orig.lineSplits;

        return ti;
    }

    public TextItem set(float x, float y, float ratio, String label, TextStyle text) {
        this.x = x;
        this.y = y;
        this.ratio = ratio;
        this.label = label;
        this.text = text;
        this.x1 = 0;
        this.y1 = 0;
        this.x2 = 1;
        this.y2 = 0;
        this.width = text.paint.measureText(label);

        if (this.width > TextStyle.MAX_TEXT_WIDTH) {
            this.width = 0;
            this.lines = 0;
            this.lineSplits = new int[10]; // max 5 lines
            int index = 0;
            int length = label.length();
            while(index <= length - 1) {
                int lsi = this.lines << 1;
                this.lineSplits[lsi] = index;
                int n;
                if (this.lines == 4) {
                    n = length;
                } else {
                    n = index + text.paint.breakText(label, index, length, TextStyle.MAX_TEXT_WIDTH);
                    if (length - n < 6) // do not hang short lines
                        n = length;
                }
                this.lineSplits[lsi + 1] = n;
                if (n < length) { // find nearest space to split
                    for (int i = n - 1; i > index; i--) {
                        if (label.charAt(i) == ' ') {
                            n = i + 1;
                            this.lineSplits[lsi + 1] = i;
                            break;
                        }
                    }
                }
                float w = text.paint.measureText(label.substring(index, n));
                if (w > this.width)
                    this.width = w;
                index = n;
                this.lines++;
            }
        } else {
            this.lines = 1;
        }
        this.height = text.fontHeight * this.lines;

        return this;
    }

    // center
    public float x, y;

    // ratio
    public float ratio;

    // label text
    public String label;

    // text style
    public TextStyle text;

    // label width
    public float width;

    // label height
    public float height;

    // left and right corner of segment
    public float x1, y1, x2, y2;

    // segment length
    public short length;

    // link to next/prev label of the way
    //public TextItem n1;
    //public TextItem n2;

    public byte edges;

    public int lines;
    public int[] lineSplits;

    @Override
    public String toString() {
        return x + " " + y + " " + label;
    }
}
