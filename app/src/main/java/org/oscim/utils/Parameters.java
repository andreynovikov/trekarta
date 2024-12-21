/*
 * Copyright 2017-2019 devemux86
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
package org.oscim.utils;

public final class Parameters {

    /**
     * Allow custom scale factor for short vertices instead of the calculated one.
     */
    public static boolean CUSTOM_COORD_SCALE = false;

    /**
     * Allow custom tile size instead of the calculated one.
     */
    public static boolean CUSTOM_TILE_SIZE = false;

    /**
     * If true the <code>MapEventLayer2</code> will be used instead of default <code>MapEventLayer</code>.
     */
    public static boolean MAP_EVENT_LAYER2 = false;

    /**
     * Maximum buffer size for map files.
     */
    public static int MAXIMUM_BUFFER_SIZE = 8000000;

    /**
     * Optimal placement of text labels on polygons.
     */
    public static boolean POLY_LABEL = false;

    /**
     * Placement of symbols on polygons.
     */
    public static boolean POLY_SYMBOL = false;

    /**
     * POT textures in themes.
     */
    public static boolean POT_TEXTURES = false;

    /**
     * Texture atlas in themes.
     */
    public static boolean TEXTURE_ATLAS = false;

    /**
     * Transparent line layers.
     */
    public static boolean TRANSPARENT_LINES = false;

    /**
     * Threaded system initialization.
     */
    public static boolean THREADED_INIT = false;

    private Parameters() {
        throw new IllegalStateException();
    }
}
