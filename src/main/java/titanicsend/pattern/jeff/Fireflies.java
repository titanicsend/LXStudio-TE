package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameterListener;
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

	public CompoundParameter speed = new CompoundParameter("Speed", 0.5, 0, 1);
	public CompoundParameter decayP =  new CompoundParameter("Decay", 0.5, 0, 1);
	public CompoundParameter fadeP = new CompoundParameter("Fade", 0.5, 0, 1);
	public CompoundParameter numSparksP = new CompoundParameter("NumSparks", 1, 0, 1);
	public CompoundParameter colorP = new CompoundParameter("Color", 0.5, 0, 1);
	
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
		
		addParameter("speed", this.speed);
		addParameter("decay", this.decayP);
		addParameter("fade", this.fadeP);
		addParameter("numSparks", this.numSparksP);
		addParameter("color", this.colorP);
		
		this.speed.addListener(this.speedListener);
		this.decayP.addListener(this.decayListener);
		this.fadeP.addListener(this.fadeListener);
		this.numSparksP.addListener(this.numSparksListener);
		this.colorP.addListener(this.colorListener);
	}
	
	private final LXParameterListener speedListener = (p) -> {
		double v = p.getValue();
		this.speedMultiplier = 1 + (v * v * 40 - .5);
		};

	private final LXParameterListener decayListener = (p) -> {
		double v = p.getValue();
		this.decay = .8 + (1 - v) * .199;
		};

	private final LXParameterListener fadeListener = (p) -> {
		double v = p.getValue();
		this.fade = 0.5 + (1 - v) * .49;
		};

	private final LXParameterListener numSparksListener = (p) -> {
		double v = p.getValue();
		this.numSparks = (int) (1 + floor(this.pixelCount * v / 10));
		};

	private final LXParameterListener colorListener = (p) -> {
		double v = p.getValue();
        this.colorRange = v;
		};

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
	
	@Override
	public void dispose() {
		this.speed.removeListener(this.speedListener);
		this.decayP.removeListener(this.decayListener);
		this.fadeP.removeListener(this.fadeListener);
		this.numSparksP.removeListener(this.numSparksListener);
		this.colorP.removeListener(this.colorListener);
		super.dispose();
	}
}