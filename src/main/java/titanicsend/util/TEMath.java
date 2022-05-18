package titanicsend.util;

import heronarts.lx.utils.LXUtils;

import java.util.Arrays;

import static java.lang.Math.floor;

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
        return (x - floor(x / y) * y);
    }

    /**
     *  x mod y behaving the same way as Math.floorMod but with floats
     */
    public static float floorModf(float x, float y) {
        return (float) (x - floor(x / y) * y);
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
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     *  Matrix dot product
     */
    public static double dotProduct(double[] a, double[] b) {
        if (a.length != b.length)
            throw new RuntimeException("Arrays must be same size");
        double sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += a[i] * b[i];
        return sum;
    }

    /**
     *  Built-in function from GLSL fragment shader language ported to Java
     *  Performs smooth Hermite interpolation between 0 and 1
     *
     *  See: https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/smoothstep.xhtml
     */
    public static double smoothstep(double edge0, double edge1, double x)
    {
        if (x < edge0)
            return 0;

        if (x >= edge1)
            return 1;

        // Scale/bias into [0..1] range
        x = (x - edge0) / (edge1 - edge0);

        return x * x * (3 - 2 * x);
    }

    public static double[] addArrays(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new ArithmeticException();
        } else {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] + b[i];
            }
            return result;
        }
    }

    public static double[] subtractArrays(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new ArithmeticException();
        } else {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] - b[i];
            }
            return result;
        }
    }

    public static double[] multiplyArray(double multiplier, double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] * multiplier;
        }
        return result;
    }

    public static double[] multiplyArrays(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new ArithmeticException();
        } else {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] * b[i];
            }
            return result;
        }
    }

    public static double[] divideArrays(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new ArithmeticException();
        } else {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] / b[i];
            }
            return result;
        }
    }

    public static double[] addToArray(double addend, double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] + addend;
        }
        return result;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double[] clamp(double[] array, double min, double max) {
        return Arrays.stream(array).map(x -> clamp(x, min, max)).toArray();
    }

    public static double[] mod(double[] array, double value) {
        return Arrays.stream(array).map(x -> x % value).toArray();
    }

    public static double vectorLength(double[] vector) {
        double value = 0;
        for (double v : vector) {
            value += v * v;
        }
        return Math.sqrt(value);
    }

    public static double fract(double x) {
        return x - floor(x);
    }

    public static double vectorDistance(double[] a, double[] b) {
        return vectorLength(subtractArrays(a, b));
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
