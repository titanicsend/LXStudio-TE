package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.util.TEMath;


@LXCategory("Combo FG")
public class NeuralBeatXY extends BaseCPPNPattern {

    public NeuralBeatXY(LX lx) {
        super(lx);
    }

    // scratch variables
    protected double[] inputs;
    protected double[] activated;
    protected double[] outputs;

    protected double speedScaleFactor() {
        return 0.0001;
    }

    protected int pointToColor(LXPoint point) {
        // translate and rescale normalized coords from -1 to 1
        float x = 2 * (float)(point.zn - 0.5);  // z axis on vehicle
        float y = 2 * (float)(point.yn - 0.5);
        float r = (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));

        inputs = new double[] {x + ((1 + swingBeat) * TEMath.wave(t1)), y + ((1 + swingBeat) * TEMath.wave(t1)), t1 * (1 - r), t1};
        activated = computeLayer(inputs, inputToHidden, "sin");
        outputs = computeLayer(activated, hiddenToOutput, "sin");

        float alpha = 100f * (float) TEMath.clamp(100f * outputs[0],0,100);
//            double hue = baseHue * (float) TEMath.clamp(100f * output[0][0],0,100);
        float saturation = baseSat * (float) TEMath.clamp(100f * outputs[1],0,100);
        float brightness = baseBri * (float) TEMath.clamp(100f * outputs[2],0,100);

        int outputColor = LXColor.hsba(baseHue, saturation, brightness, alpha);
        return outputColor;
    }
}
