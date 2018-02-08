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

package mobi.maptrek.util;

public class MathUtils {
    private final static float FLOAT_EPSILON = 1E-5f;
    private final static double DOUBLE_EPSILON = 1E-6;

    public static boolean equals(float a, float b) {
        return a == b || Math.abs(a - b) < FLOAT_EPSILON;
    }

    public static boolean equals(double a, double b) {
        return a == b || Math.abs(a - b) < DOUBLE_EPSILON;
    }
}
