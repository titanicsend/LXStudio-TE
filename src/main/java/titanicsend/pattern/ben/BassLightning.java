package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.app.TEVirtualColor;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEVertex;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@LXCategory("Geometry Masks")
public class BassLightning extends TEAudioPattern {

	static <T> T randomItem(Collection<T> collection) {
		return collection.stream().skip((int) (collection.size() * Math.random())).findFirst().get();
	}

	class Bolt {
		TEEdgeModel edge;
		float life;
		int edgePointsIndex;
		TEVertex startVertex;
		double waitMs = 0;
		HashSet<String> visitedEdges = new HashSet<>();

		/**
		 * Make a random bolt
		 * @param edge
		 */
		public Bolt(TEEdgeModel edge) {
			this(edge, 1, (int) (Math.random() * edge.points.length), Math.random() > .5 ? edge.v0 : edge.v1, 0);
		}

		/**
		 * Make a bolt with specific location and parameters
		 * @param edge
		 * @param life
		 * @param edgePointsIndex
		 * @param startVertex
		 * @param waitMs
		 */
		public Bolt(TEEdgeModel edge, float life, int edgePointsIndex, TEVertex startVertex, double waitMs) {
			this.edge = edge;
			this.life = life;
			this.edgePointsIndex = edgePointsIndex;
			this.startVertex = startVertex;
			this.waitMs = waitMs;
			visitedEdges.add(edge.getId());
		}

		private int pixelsPerJump() {
			//TODO parameterize
			double r = Math.random();

			return (int) ((fixedDistanceParam.getValue() * life) + (r*r * randomDistanceParam.getValue()));
//			return 100;
		}

		/**
		 * Fork a bolt into another bolt at the destination vertex. Both this bolt and the new bolt
		 * start along new different edges.
		 * @return
		 */
		Bolt fork() {
			TEVertex origin = edge.v0 == startVertex ? edge.v1 : edge.v0;

			origin.virtualColor = new TEVirtualColor(0, 100, 255, 255);

			ArrayList<TEEdgeModel> candidateEdges;

			if (allowLoopsParam.getValueb()) {
				candidateEdges = new ArrayList<>(origin.edges);
				candidateEdges.remove(edge);
			} else {
				candidateEdges = new ArrayList<>(
						origin.edges.stream().filter(e -> !visitedEdges.contains(e.getId())).collect(Collectors.toList())
				);
			}

			Collections.shuffle(candidateEdges);

			//with possibly two edges picked, move this bolt to the first, and make a new bolt for the second
			if (candidateEdges.size() >= 1) {
				edge = candidateEdges.get(0);
				visitedEdges.add(edge.getId());
				startVertex = origin;
				edgePointsIndex = edge.v0 == origin ? 0 : edge.points.length - 1;
			} else {
				life = 0; //no new edges to traverse, die here
			}

			if (candidateEdges.size() >= 2) {
				TEEdgeModel e2 = candidateEdges.get(1);
				Bolt b2 = new Bolt(e2,
						(float) (life * .7), //randomly shorten life of forks
						e2.v0 == origin ? 0 : e2.points.length - 1,
						origin,
						waitMs);
				visitedEdges.add(e2.getId());
				//overwrite child bolt's visited edges so it shares (TODO refactor for cleaner interface)
				b2.visitedEdges = visitedEdges;
				if (b2.life > 0)
					newBolts.add(b2);
				return b2;
			}

			return null;
		}

		void run(double deltaMs) {
			waitMs -= deltaMs;
			life -= deltaMs/lifeParam.getValue();
			if (waitMs <= 0) {
				//time to jump!
				waitMs = boltWaitMs();
				int pixelsToGo = pixelsPerJump();
				jump(pixelsToGo);
			}
		}

		private void jump(int pixelsToGo) {
			while (pixelsToGo > 0 && life > 0) {
				int dir = edge.v0 == startVertex ? 1 : -1;
				edgePointsIndex += dir;
				if (edgePointsIndex >= 0 && edgePointsIndex < edge.points.length) {
					values[edge.points[edgePointsIndex].index] = 1;
					pixelsToGo--;
				} else {
					//fork at vertex
					Bolt b2 = fork();
					if (b2 != null) {
						//split jump distance
						int forkPixels = (int) (pixelsToGo * .7);
//						pixelsToGo -= forkPixels;
						b2.jump(forkPixels);
					}
				}
			}
		}
	}

	public final DiscreteParameter energy =
			new DiscreteParameter("Energy", 3, 1, 11)
					.setDescription("Amount of Bolts");
	public final CompoundParameter fadeParam =
			new CompoundParameter("Fade", .98, .95, .999);
	public final CompoundParameter delayParam =
			new CompoundParameter("Delay", 100, 1, 250);
	public final CompoundParameter lifeParam =
			new CompoundParameter("Life", 250, 1, 3000);
	public final CompoundParameter fixedDistanceParam =
			new CompoundParameter("Size", 50, 0, 200)
					.setDescription("Bolt distance based on remaining life");
	public final CompoundParameter randomDistanceParam =
			new CompoundParameter("Vary", 50, 0, 200)
					.setDescription("Random distance");

	public final BooleanParameter onBeatParam =
			new BooleanParameter("On Beat", false)
					.setDescription("Trigger on each beat");
	public final BooleanParameter onBassParam =
			new BooleanParameter("On Bass", false)
					.setDescription("Trigger on audio bass");
	public final BooleanParameter trigger =
			new BooleanParameter("Trigger", false)
					.setMode(BooleanParameter.Mode.MOMENTARY)
					.setDescription("Trigger a bolt of lightning manually");
	public final BooleanParameter allowLoopsParam =
			new BooleanParameter("Loops", true)
					.setDescription("Allow bolts to loop to an edge they've already visited");

	float[] values;
	List<Bolt> bolts = new LinkedList<>();
	List<Bolt> newBolts = new LinkedList<>();

	public BassLightning(LX lx) {
		super(lx);
		addParameter("energy", energy);
		addParameter("fade", fadeParam);
		addParameter("delay", delayParam);
		addParameter("life", lifeParam);
		addParameter("fDistance", fixedDistanceParam);
		addParameter("rDistance", randomDistanceParam);
		addParameter("onBeat", onBeatParam);
		addParameter("onBass", onBassParam);
		addParameter("trigger", trigger);
		addParameter("loops", allowLoopsParam);

		values = new float[model.size];
	}

	double boltWaitMs() {
		//TODO parameterize
		double r = Math.random();
		return r * r * delayParam.getValue();
//		return 0;
	}

	@Override
	public void onParameterChanged(LXParameter p) {
		if (p == this.trigger && p.getValue() > 0.5 ) {
			makeBolt();
		}
	}

	private void makeBolt() {
		TEVertex vertex = model.vertexesById.get(Math.random() > .5 ? 30 : 122);
		synchronized (bolts) {
			for (int i = 0; i < energy.getValuei(); i++) {
				Collection<TEEdgeModel> edges = this.model.edgesById.values();
				TEEdgeModel edge = randomItem(vertex.edges);
				bolts.add(new Bolt(edge, 1, edge.v0 == vertex ? 0 : edge.points.length - 1, vertex, 0));
			}
		}
	}

	public void runTEAudioPattern(double deltaMs) {
		float fade = (float) Math.pow(fadeParam.getValue(), deltaMs);
		for (int i = 0; i < values.length; i++) {
			values[i] *= fade;
		}

		this.model.vertexesById.values().forEach(v -> {
			v.virtualColor.alpha *= .99;
			v.virtualColor.alpha = Math.max(64, v.virtualColor.alpha);
		} );

		synchronized (bolts) {

			if (onBeatParam.getValueb() && getTempo().beat()) {
				makeBolt();
			}

			if (onBassParam.getValueb() && bassHit) {
				makeBolt();
			}

			Iterator<Bolt> i = bolts.iterator();
			while (i.hasNext()) {
				Bolt bolt = i.next();
				bolt.run(deltaMs);
				//prune any dead bolts
				if (bolt.life <= 0)
					i.remove();
			}
			bolts.addAll(newBolts);
			newBolts.clear();
		}

		for (int i = 0; i < values.length; i++) {
			int v = (int) (values[i] * 255);
			colors[i] = LXColor.rgb(v, v, v);
		}


//		for (TEEdgeModel edge : model.edgesById.values()) {
//			// Max width of the lit section of this edge, from 0 to 200 percent of its length
//			// of the overall edge length, depending on the energy parameter.
//			float maxWidth = energy.getNormalizedf() * 2;
//
//			// Scale the fractional percentage of this max size we will light based on `bassLevel`
//			float bassWidth = (float) bassLevel * maxWidth;
//
//			// low and high straddle the center (.5f is 50% of the edge length)
//			float lowFrac = .5f - bassWidth / 2;
//			float highFrac = lowFrac + bassWidth;
//
//			for (TEEdgeModel.Point point : edge.points) {
//				// Only color the pixels between the low and high fraction.
//				// Red is used for brevity. For real show patterns use LinkedColorParameters.
//				if (point.frac >= lowFrac && point.frac < highFrac)
//					colors[point.index] = LXColor.RED;
//			}
//		}
	}
}
