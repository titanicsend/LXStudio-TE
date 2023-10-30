package titanicsend.util;

import java.util.ArrayList;
import java.util.Arrays;

import heronarts.lx.LX;
import heronarts.lx.utils.LXUtils;

import static java.lang.Math.PI;

public class TEMath {

    public static final double TAU = 2 * PI;

    /** Take a normalized position (n)
     * where 0 is 0 and n=1 is x = PI * 2
     * Return a 0..1 sin wave
     */
    public static double wave(double n) {
        return (Math.sin(n * PI * 2) + 1) / 2;
    }
    public static float wavef(float n) {
        return (float) ((Math.sin(n * PI * 2) + 1) / 2);
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

    public static double step(double a, double b) {
        return a > b ? 1 : 0;
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

    public static float clamp(float value, float min, float max) {
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

    public static double[] vectorNormalize(double[] v) {
        double[] result = new double[v.length];
        double sum = 0;
        for (double d : v) {
            sum += d;
            for (int i = 0; i < v.length; i++) {
                result[i] = v[i] / sum;
            }
        }
        return result;
    }

    public static double fract(double x) {
        return x - Math.floor(x);
    }

    public static double[] fract(double[] x) {
        return Arrays.stream(x).map(TEMath::fract).toArray();
    }

    public static double[] abs(double[] x) {
        return Arrays.stream(x).map(Math::abs).toArray();
    }

    public static double[] sqrt(double[] x) {
        return Arrays.stream(x).map(Math::sqrt).toArray();
    }

    public static double[] floor(double[] x) {
        return Arrays.stream(x).map(Math::floor).toArray();
    }

    public static double vectorDistance(double[] a, double[] b) {
        return vectorLength(subtractArrays(a, b));
    }

    public static double mix(double x, double y, double a) {
        return x * (1 - a) + y * a;
    }

    public static double[][] multiplyMatricies(double[][] m1, double[][] m2) {
        int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if(m1ColLength != m2RowLength) return null; // matrix multiplication is not possible
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        double[][] mResult = new double[mRRowLength][mRColLength];
        for(int i = 0; i < mRRowLength; i++) {         // rows from m1
            for(int j = 0; j < mRColLength; j++) {     // columns from m2
                for(int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
    }

    public static double[] multiplyVectorByMatrix(double[] vector, double[][] matrix) {
        return multiplyMatricies(new double[][]{vector}, matrix)[0];
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

    public static long calcSum(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return sum;
    }

    public static double calcMean(long[] values) {
        long acc = calcSum(values);
        return acc / values.length;
    }

    public static double calcStddev(long[] values) {
        // first, get the mean
        double mean = calcMean(values);

        // then calc stddev
        double std = 0.0;
        for (long num : values) {
            std += Math.pow(num - mean, 2);
        }
        return Math.sqrt(std / values.length);
    }

    public static double calcInlierMean(long[] values, double outlierZscore) {
        assert outlierZscore > 0 :
            "Outlier argument must be > 0, it is the absolute value of the limit of inliers";

        double avg = calcMean(values);
        double stddev = calcStddev(values);

        // transform the values into zscores, and average
        long acc = 0;
        int n = 0;
        for (long v : values) {
            double z = (v - avg) / stddev;
            if (Math.abs(z) < Math.abs(outlierZscore)) {
                acc += v;
                n++;
            }
        }

        return acc / n;
    }

    /*
        Linearly weighted recency mean.
     */
    public static double calcRecencyWeightedMean(ArrayList<Long> values) {
        int n = values.size();
        double denom = n * (n + 1) / 2.;
        double acc = 0;
        for (int i = n; i > 0; i--) {
            long v = values.get(n - i);
            acc += (i * v);
        }
        return acc / denom;
    }

    public enum EasingFunction {
        LINEAR_RAMP_DOWN,
        LINEAR_RAMP_UP;
    }
    
    static final private double EASE_WARN_FREQUENCY = 10000;
    static private double lastEaseWarn = 0;

    public static double ease(EasingFunction fn, double inp, double minInput, double maxInput, double minOutput, double maxOutput) {
        double inputRank = (inp - minInput) / (maxInput - minInput);
        double outputRange = maxOutput - minOutput;

        double result;
        if (fn == EasingFunction.LINEAR_RAMP_UP) {
            result = minOutput + inputRank * outputRange;

        } else if (fn == EasingFunction.LINEAR_RAMP_DOWN) {
            result = minOutput + outputRange * (1.0 - inputRank);

        } else {
            // default to LINEAR_RAMP_DOWN
            TE.err("Unsupported easing func: %s", fn);
            result = minOutput + inputRank * outputRange;
        }
        
        if (result < minOutput || result > maxOutput) {
        	if (System.currentTimeMillis() - EASE_WARN_FREQUENCY > lastEaseWarn) {
        		LX.log("WARNING: TEMath.ease() method received illegal parameter values");
        		lastEaseWarn = System.currentTimeMillis();
        	}
        	result = LXUtils.constrain(result, minOutput, maxOutput);
        }
        return result;
    }
}
