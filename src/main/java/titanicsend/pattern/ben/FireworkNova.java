package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.pixelblaze.PixelblazePort;

import static java.lang.Math.sqrt;

@LXCategory("Combo FG")

public class FireworkNova extends PixelblazePort {

	float scale = .5f;
	float energy;
	private float t1;

	CompoundParameter energyParameter;

	public FireworkNova(LX lx) {
		super(lx);
		energyParameter = new CompoundParameter("Energy", 0.5, 0, 1);
		addParameter("energy", energyParameter);
	}

	@Override
	public void setup() {

	}

	@Override
	public void onPointsUpdated() {

	}

	@Override
	public void render3D(int index, float x, float y, float z) {
		//center coordinates
		x -= 0.5;
		y -= 0.5;
		z -= 0.5;
		//get pixel distance from center
		float r = (float) (sqrt(x*x + y*y + z*z) * scale);
		//make colors
		float h = r * 3;
		//blast wave - a triangle's peak moving based on the center
		//clipped to 75% of the waveform - v goes negative: +0.25 to -0.75
		float v = (float) (triangle(r - t1) - .75);
		//trailing the outward burst are random white sparks
		//between 0-12.5% chance depending on distance to peak
		boolean spark = triangle(r - t1 + .05) - energy  > random(2);
		if (spark) {
			rgb(1,1,1); //sparks are white
		} else {
			v = v*4; //bring the triangle's peak back to 0-1 range
			v = v*v*v; //gives more definition to the wave, preserve negatives
			// hsv(h,1,v)

			paint(h);
			setAlpha(v);
		}
	}

	@Override
	public void beforeRender(double deltaMs) {
		t1 = (float) measure();
		energy = 1.0f - energyParameter.getValuef();
	}
}
