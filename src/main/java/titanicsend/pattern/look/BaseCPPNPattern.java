package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEMath;

import java.util.Random;


public abstract class BaseCPPNPattern extends TEAudioPattern {

    // Controls
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .5, 0, 1)
                    .setDescription("Stars sparkle and move");

    protected final CompoundParameter speedControl = (CompoundParameter)
            new CompoundParameter("Speed", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Overall movement speed");


    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Panel Color");

    public final BooleanParameter useSwingBeat =
            new BooleanParameter("SwingBeat")
                    .setDescription("Toggle whether swingBeat is used")
                    .setValue(true);


    // neural layers
    protected int inputLayerSize = 4;
    protected int hiddenLayerSize = 5;
    protected int outputLayerSize = 3;

    protected double[][] inputToHidden;
    protected double[][] hiddenToOutput;

    // neural net initialization
    protected Random random;
    protected double variance = 5;
    protected double mean = 0.5;

    // timers
    protected double timebase;  // main time accumulator
    protected double t1;        // scratch timer
    protected double swingBeat;

    // scratch colors
    protected float baseHue;
    protected float baseSat;
    protected float baseBri;

    public BaseCPPNPattern(LX lx) {
        super(lx);
        addParameter("speedControl", this.speedControl);
        addParameter("energy", this.energy);
        addParameter("useSwingBeat", this.useSwingBeat);

        timebase = 0;

        random = new Random();
        inputToHidden = this.newInitializedLayer(inputLayerSize, hiddenLayerSize);
        hiddenToOutput = this.newInitializedLayer(hiddenLayerSize, outputLayerSize);
    }

    protected double[][] newInitializedLayer(int rows, int cols) {
        double[][] layer = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                layer[i][j] = this.nextGaussian();
            }
        }
        return layer;
    }

    protected double nextGaussian() {
        return this.random.nextGaussian() * Math.sqrt(this.variance) + this.mean;
    }

    protected double[] computeLayer(double[] inputs, double[][] weights, String activationName) {
        if (inputs == null || weights == null) {
            return null;
        }

        double[] hidden = TEMath.multiplyVectorByMatrix(inputs, weights);
        if (hidden == null) {
            return null;
        }

        // using a matrix of dimension 1xN, so that it can be used for downstream multiplications
        double[] activated = new double[weights[0].length];
        // TODO(look): consider array.stream??
        for (int j = 0; j < activated.length; j++) {
            switch (activationName) {
                case "sin":
                    activated[j] = 0.5 * Math.sin(hidden[j]) + 0.5; // + timebase ...?
                    break;
                case "tan":
                    activated[j] = Math.tan(hidden[j]);
                    break;
                case "tanh":
                    activated[j] = Math.tanh(hidden[j]);
                    break;
                case "noop":
                    activated[j] = hidden[j];
                    break;
                case "sigmoid":
                    activated[j] = 1 / (1 + Math.exp(hidden[j]));
                    break;
                default:
                    throw new RuntimeException("Unrecognized activation function: "+activationName);
            }
        }
        return activated;
    }

    public void runTEAudioPattern(double deltaMs) {
        // pick up the current set color
        int baseColor = this.color.calcColor();
        baseHue = LXColor.h(baseColor);
        baseSat = LXColor.s(baseColor) / 100f;
        baseBri = LXColor.b(baseColor) / 100f;

        // 0..1 ramp (sawtooth) of current position in a quarter note beat.
        double beat = lx.engine.tempo.basis();
        swingBeat = this.useSwingBeat.getNormalized() * TEMath.easeOutPow(beat, energy.getNormalized());

        // whole system is constantly moving over time.
        timebase = ((double) System.currentTimeMillis()) / 1000.0;

        //t1 = timebase * swingBeat;
        //t1 = timebase * energy.getNormalized();
        t1 = timebase * this.speedControl.getValuef() * this.speedScaleFactor();

        // per pixel calculations
        for (LXPoint point : model.points) {
            colors[point.index] = pointToColor(point);
        }
    }

    // scratch variables
    protected double[] inputs;
    protected double[] activated;
    protected double[] outputs;



    protected abstract double speedScaleFactor();

    protected abstract int pointToColor(LXPoint point);
}
