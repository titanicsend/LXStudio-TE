package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPerformancePattern;

import java.util.ArrayList;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public abstract class PixelblazePort extends TEPerformancePattern {

	public BooleanParameter enableEdges;
	public BooleanParameter enablePanels;

	public LXPoint[] modelPoints = new LXPoint[0];

	//"globals"
	public int pixelCount;
	//public long now;

	//render fields
	public LXPoint point;
	public int color;

	public PixelblazePort(LX lx) {
		super(lx);

		configureControls();
		addCommonControls();

		enableEdges = new BooleanParameter("Edges", true);
		enablePanels = new BooleanParameter("Panels", true);

		enableEdges.addListener(modelPointsListener);
		enablePanels.addListener(modelPointsListener);

		addParameter("enableEdges", enableEdges);
		addParameter("enablePanels", enablePanels);

		modelPoints = getModelPoints();
		updateLocalVars();
		setup();
	}

	private void updateLocalVars() {
		pixelCount = modelPoints.length;
		//now = System.currentTimeMillis();
	}

	protected LXParameterListener modelPointsListener = lxParameter -> {
		modelPoints = getModelPoints();
		clearPixels();
		updateLocalVars();
		onPointsUpdated();
	};


	private LXPoint[] getModelPoints() {
		ArrayList<LXPoint> newPoints = new ArrayList<>(model.points.length);
		if (enableEdges.getValueb()) {
			newPoints.addAll(model.edgePoints);
		}
		if (enablePanels.getValueb()) {
			newPoints.addAll(model.panelPoints);
		}
		return newPoints.toArray(new LXPoint[0]);
	}


	public void runTEAudioPattern(double deltaMs) {
		updateGradients();
		updateLocalVars();
		beforeRender(deltaMs);

		float xOffs = (float) getXPos();
		float yOffs = (float) -getYPos();
		for (int i = 0; i < modelPoints.length; i++) {
			color = 0;
			point = modelPoints[i];
			render3D(i, point.xn + xOffs, point.yn + yOffs, point.zn);
			colors[point.index] = color;
		}
	}

	/**
	 * Called before adding common controls. Derived classes can override this to
	 * change control ranges, response curves and other parameters if necessary.
	 * <p></p>
	 * If a pattern needs to add additional custom controls, it can do so by
	 * implementing them in setup().
	 */
    public abstract void configureControls();

	public abstract void setup();

	public abstract void onPointsUpdated();

	public abstract void render3D(int index, float x, float y, float z);

	public abstract void beforeRender(double deltaMs);


	//compat api

	public static double PI2 = Math.PI*2;


	public double clamp(double v, double min, double max) {
		return Math.min(max, Math.max(min, v));
  }
  
	public double random(double v) {
		return Math.random() * v;
	}

	public double time(double interval) {
		return ((getTime() / 65.536) % interval) / interval;
	}

	public double wave(double v) {
		return (sin(v*PI*2) + 1)/2;
	}

	public double triangle(double v) {
		v = v * 2 % 2;
		if (v < 0)
			v += 2;
		return v < 1 ? v : 2 - v;
	}

	public float triangle(float v) {
		v = v * 2 % 2;
		if (v < 0)
			v += 2;
		return v < 1 ? v : 2 - v;
	}

	public int hsv(float h, float s, float v) {
		return color = Glue.hsv(h, s, v);
	}
	public int rgb(float r, float g, float b) {
		return color = Glue.rgb(r, g, b);
	}
	public int rgba(float r, float g, float b, float a) {
		return color = Glue.rgba(r, g, b, a);
	}

	public int paint(float v) {
		return color = getPrimaryGradientColor(v);
	}

	public void setAlpha(float v) {
		color = Glue.setAlpha(color, v);
	}

	@Override
	public void dispose() {
		enableEdges.removeListener(modelPointsListener);
		enablePanels.removeListener(modelPointsListener);
		super.dispose();
	}

}
