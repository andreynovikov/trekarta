package mobi.maptrek.util;

public class MeanValue {
    private float K = 0;
    private float n = 0;
    private float Ex = 0;
    private float Ex2 = 0;

    public void addValue(float x) {
        if (n == 0) {
            K = x;
        }
        n = n + 1;
        Ex = Ex + (x - K);
        Ex2 = Ex2 + (x - K) * (x - K);
    }

    public void removeValue(float x) {
        n = n - 1;
        Ex = Ex - (x - K);
        Ex2 = Ex2 - (x - K) * (x - K);
    }

    public float getMeanValue() {
        return K + Ex / n;
    }

    public float getVariance() {
        return (Ex2 - (Ex * Ex) / n) / (n - 1);
    }
}
