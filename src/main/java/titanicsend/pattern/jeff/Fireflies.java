package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.pixelblaze.PixelblazePort;

import static java.lang.Math.abs;
import static java.lang.Math.floor;

/**
 * FireFlies
 * <p>
 * This is a fork of the sparks pattern where each spark is:
 * - Slowed down
 * - Given a longer lifetime
 * - Allowed to loop from one end to the other
 * <p>
 * This is a highly upvoted pattern generously contributed to the pixelblaze community
 * pattern library by an unknown person. Please reach out if you'd like an
 * attribution link here!
 */
@LXCategory("Test")
public class Fireflies extends PixelblazePort {

	private int numSparks;
	private double decay = .99;          // Decay their energy/speed. Use .999 for slower
	private double maxSpeed = .4;        // The maximum initial speed of any spark / firefly
	private double newThreshhold = .01;  // Recycle any spark under this energy
	private double fade = .9;
	private double speedMultiplier = 1;
	private double colorRange = 0;

	private double[] sparks;
	private double[] sparkX;
	private double[] pixels;

	public Fireflies(LX lx) {
		super(lx);

		addParameter("speed", new CompoundParameter("Speed", 0.5, 0, 1).addListener(p -> {
			double v = p.getValue();
			speedMultiplier = 1 + (v * v * 40 - .5);
		}));

		addParameter("decay", new CompoundParameter("Decay", 0.5, 0, 1).addListener(p -> {
			double v = p.getValue();
			decay = .8 + (1 - v) * .199;
		}));

		addParameter("fade", new CompoundParameter("Fade", 0.5, 0, 1).addListener(p -> {
			double v = p.getValue();
			fade = 0.5 + (1 - v) * .49;
		}));

		addParameter("numSparks", new CompoundParameter("NumSparks", 1, 0, 1).addListener(p -> {
			double v = p.getValue();
			numSparks = (int) (1 + floor(pixelCount * v / 10));
		}));

		addParameter("color", new CompoundParameter("Color", 0.5, 0, 1).addListener(p -> {
			double v = p.getValue();
            colorRange = v;
		}));
	}

	@Override
	public void setup() {
        init();
	}

	@Override
	public void onPointsUpdated() {
        init();
	}

    private void init() {
        int maxSparks = 1 + pixelCount / 10;  // Scale number of sparks based on # LEDs

        sparks = new double[maxSparks];
        sparkX = new double[maxSparks];
        pixels = new double[pixelCount];
    }

	@Override
	public void render3D(int index, float x, float y, float z) {
		double v = pixels[index];

		paint((float) (colorRange - v * colorRange)); // Paint palette color
		setAlpha((float) v); //cut out areas that would otherwise be dark
	}

    //TODO deleteme once merged
	public double random(double v) {
		return Math.random() * v;
	}

	@Override
	public void beforeRender(double delta) {
        //guard against both edge and panels being off
        if (pixels.length == 0)
            return;

		delta *= .1;

		for (int i = 0; i < pixelCount; i++) pixels[i] *= fade; // Air cooling

		for (int i = 0; i < numSparks; i++) {
			// Recycle dead sparks
			if (abs(sparks[i]) <= newThreshhold) {
				sparks[i] = (maxSpeed / 2) - random(maxSpeed);
				sparkX[i] = random(pixelCount);
			}

			sparks[i] *= decay;  // Spark energy decay
			int lastX = (int) floor(sparkX[i]);
			sparkX[i] += sparks[i] * delta * speedMultiplier;  // Advance each position ∝ its energy

			// Allow sparks to loop around each end
			if (sparkX[i] >= pixelCount) sparkX[i] = 0;
			if (sparkX[i] < 0) sparkX[i] = pixelCount - 1;

			// Heat up the pixel at this spark's X position
			pixels[(int) floor(sparkX[i])] = 1;
			//cover any gaps
			var up = sparks[i] >= 0;
			int j = lastX;
			while (j != floor(sparkX[i])) {
				pixels[j] = 1;
				if (up)
					j++;
				else
					j--;
				//follow any wrapping
				if (j >= pixelCount) j = 0;
				if (j < 0) j = pixelCount - 1;
			}
		}
	}
}