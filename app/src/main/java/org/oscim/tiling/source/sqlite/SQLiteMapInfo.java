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

package org.oscim.tiling.source.sqlite;

import org.oscim.core.BoundingBox;

/**
 * Contains the immutable metadata of a map path.
 */
public class SQLiteMapInfo {
    /**
     * The bounding box of the map path.
     */
    public final BoundingBox boundingBox;

    /**
     * The name of the map.
     */
    public final String name;

    public SQLiteMapInfo(String name, BoundingBox boundingBox) {
        this.name = name;
        this.boundingBox = boundingBox;
    }
}
