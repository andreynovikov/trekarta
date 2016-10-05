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
