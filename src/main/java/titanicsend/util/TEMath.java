package titanicsend.util;

public class TEMath {
    /** Take a normalized position (n)
     * where 0 is 0 and n=1 is x = PI * 2
     * Return a 0..1 sin wave
     */
    public static double wave(double n) {
        return (Math.sin(n * Math.PI * 2) + 1) / 2;
    }
    public static float wavef(float n) {
        return (float) ((Math.sin(n * Math.PI * 2) + 1) / 2);
    }

    public static double triangle(double n) {
        n = floorModd(n * 2, 2);
        if (n < 0) n += 2;
        return n < 1 ? n : 2 - n;
    }
    public static float trianglef(float n) {
        n = floorModf(n * 2, 2);
        if (n < 0) n += 2;
        return n < 1 ? n : 2 - n;
    }

    /**
     *  x mod y behaving the same way as Math.floorMod but with doubles
     */
    public static double floorModd(double x, double y) {
        return (x - Math.floor(x / y) * y);
    }

    /**
     *  x mod y behaving the same way as Math.floorMod but with floats
     */
    public static float floorModf(float x, float y) {
        return (float) (x - Math.floor(x / y) * y);
    }

    /**
     *  A variable-curve exponential, normalised ease-out function
     *
     *  See https://www.desmos.com/calculator/uasacds4ra
     *
     * @return a double between 0 and 1
     */
    public static double easeOutPow(double basis, double normalizedPower) {
        // Rescaling normalizedPower gives better fingerspitzengefühl
        double scaledPow = 1 + 40 * Math.pow(normalizedPower, 5);
        return Math.pow(basis, 1. / scaledPow);
    }
}
