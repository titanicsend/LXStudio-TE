package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.pixelblaze.PixelblazePort;

import static java.lang.Math.abs;
import static java.lang.Math.sin;

@LXCategory("Combo FG")
public class Xorcery extends PixelblazePort {
	private double t1;
	private double t2;
	private double t3;
	private double t4;

	private double scale = 5;

	public Xorcery(LX lx) {
		super(lx);
	}

	@Override
	public void setup() {

	}

	@Override
	public void onPointsUpdated() {

	}

	@Override
	public void render3D(int index, float x, float y, float z) {
		double m, h, v;

		y += wave(measure()) * .1 + time(.1); //bounce / fall
		// y += time(.1) //blittery waterfalls!
		x += wave(measure()) * .2; //breathing

		m = .3 + triangle(t1) * .2; //.3 to .5
		h = sin(t2) + wave(
				xorf(scale * (x - .5), xorf(scale * (z - .5),scale * (y - .5))) //xor coordinates
						/ 50 * (triangle(t3) * 10 + 4 * sin(t4)) //vary the density/detail
						% m //variable modulus wrapping and range
		);
		v = (abs(h) + abs(m) + t1) % 1;
		v = triangle(v * v);
		v = v * v;

		//original hsv calculates teal through purple colors (.45 - .85):
		// h = triangle(h) * .2 + triangle(x + y + z) * .2 + .45
		// hsv(h, 1, v)


		//for paint(), don't downscale the range
		h = triangle(h) + triangle(x + y + z);
		paint((float) h); //color gradient based on edge/panel
		setAlpha((float) v); //cut out areas that would otherwise be dark
	}

	@Override
	public void beforeRender(double deltaMs) {
		t1 = time(.1);
		t2 = time(.1) * PI2;
		t3 = time(.5);
		t4 = time(.34) * PI2;
	}

	double xorf(double v1, double v2) {
		v1 *= 65536;
		v2 *= 65536;
		return ((long)v1 ^ (long)v2) / 65536.0;
	}

}
