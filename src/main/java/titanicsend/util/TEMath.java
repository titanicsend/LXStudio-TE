package titanicsend.util;

import heronarts.lx.utils.LXUtils;

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

    // Triangle wave starting at 0, returning 1 at .5, and 0 again at 1 and all
    // other integers. See also LXUtils.tri(), which has range of -1..1 and a
    // different phase.
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

    /**
     *  Distance between two arbitrary three-dimensional points
     */
    public static double distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Exponential moving average
     * https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average
     */
    public static class EMA {
        // The moving average
        public double average;

        // The weight applied to the most recent value
        public double alpha;

        // If samples are not regular, we can scale them by the time elapsed
        // since the last sample. While any denominator used consistently can work,
        // TE targets 70 FPS.
        protected final double NOMINAL_PERIOD_MS = 1000.D / 70;

        /** Create an Exponential moving average, given alpha.
         *
         * @param initialAverage Starting value
         * @param alpha Weight applied to the most recent sample
         */
        public EMA(double initialAverage, double alpha) {
            this.average = initialAverage;
            this.alpha = alpha;
        }

        /** Create an exponential moving average, given numSamples. The number
         * of samples averaged in an EMA is estimated by 1/(1-alpha).
         *
         * @param initialAverage Starting value
         * @param numSamples an approximate number of samples to smooth over
         */
        public EMA(double initialAverage, int numSamples) {
            this.average = initialAverage;
            this.alpha = ((double) numSamples - 1.) / numSamples;
        }

        /** Accumulate a new sample into the moving average
         *
         * @param value, the new sample
         * @return the updated average
         */
        public double update(double value) {
            average = alpha * value + (1. - alpha) * average;
            return average;
        }
        /** Accumulate a new float sample into the moving average
         *
         * @param value, the new sample
         * @return the updated average as a flot
         */
        public float updatef(float value) {
            return (float) update(value);
        }

        /** Accumulate a new sample into the moving average
         *  Weighted by how much time has passed since the last sample was added
         * @param value the new sample
         * @param deltaMs number of milliseconds since the last sample was added
         * @return the updated average
         */
        public double update(double value, double deltaMs) {
            double frameAlpha = alpha * deltaMs / NOMINAL_PERIOD_MS;
            average = frameAlpha * value + (1. - frameAlpha) * average;
            return average;
        }

        /**
         * @return the current moving average
         */
        public double getValue() {
            return average;
        }
        /**
         * @return the current moving average as a float
         */
        public float getValuef() {
            return (float) average;
        }

    }
}
