package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.utils.Noise;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.pixelblaze.PixelblazePort;

@LXCategory("Combo FG")
public class Audio1 extends PixelblazePort {

	private double energy;
	private float scale;
	private int electric;
	private double t1;
	private double waveBase;

	private CompoundParameter energyParameter;
	private CompoundParameter scaleParameter;
	private DiscreteParameter electricParameter;

	public Audio1(LX lx) {
		super(lx);
		energyParameter = new CompoundParameter("Energy", .5, 0, 1);
		addParameter("energy", energyParameter);

		scaleParameter = new CompoundParameter("Scale", 1.0, 0.2, 10);
		addParameter("scale", scaleParameter);

		electricParameter = new DiscreteParameter("Electric", 6, 1, 8);
		addParameter("electric", electricParameter);
	}

	@Override
	public void setup() {

	}

	@Override
	public void onPointsUpdated() {

	}

	@Override
	public void render3D(int index, float x, float y, float z) {
		float f = triangle( z*2 + y );
		double beat = (triangle(x * .5 + y * .8 - waveBase  + .6) - .5) * 4 * energy;
		beat = clamp(beat, 0, 1);

		float lacunarity = 2;
		float gain = (float) (.7 + beat / 4);
		float ridgeOffset = (float) (.78 + beat / 20);

		float a = -1 + 4 * Noise.stb_perlin_ridge_noise3(
				(float) ((x - .5f) * .2f * scale + t1), (y - .5f) * scale, (z - .5f) * scale,
				lacunarity, gain, ridgeOffset, electric);

		paint(f + a);
		setAlpha(a);
	}

	@Override
	public void beforeRender(double deltaMs) {
		t1 = time(.1 * 256) * 256;
		energy = energyParameter.getValue();
		scale = scaleParameter.getValuef();
		electric = electricParameter.getValuei();
		waveBase = (4 + measure() * 4) % 1;
	}
}
