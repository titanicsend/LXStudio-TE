package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.transform.LXProjection;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;

import java.util.ArrayList;
import java.util.List;

@LXCategory(LXCategory.TEST)
public class Kaleidoscope extends BasicEffect {
    private final DiscreteParameter segments = new DiscreteParameter("Segments", 6, 2, 12);
    private final CompoundParameter startAngle = new CompoundParameter("Start Angle", 0, 0, 2 * Math.PI);

    public Kaleidoscope(LX lx) {

        super(lx);
        addParameter("segments", this.segments);
        addParameter("startAngle", this.startAngle);
    }

    @Override
    protected void run(double deltaMs, double enabledAmount) {
        if (enabledAmount > 0) {
            double segmentAngle = 2 * Math.PI / segments.getValue();
            for (TEPanelModel panel : this.model.panelsById.values()) {
                if (!panel.panelType.equals(TEPanelModel.LIT)) {
                    continue;
                }

                LXProjection projection = new LXProjection(panel);
                projection.translate(-panel.centroid.x, -panel.centroid.y, -panel.centroid.z);

                LXVector zAxis = new LXVector(0, 0, 1);

                ArrayList<LXVector> vectors = new ArrayList<LXVector>();
                projection.iterator().forEachRemaining(vectors::add);
                LXVector normal = vectors.get(0).copy().cross(vectors.get(1));
                LXVector rotationAxis = zAxis.cross(normal);
                projection.rotate(LXVector.angleBetween(normal, zAxis), rotationAxis.x, rotationAxis.y, rotationAxis.z);

                for (LXVector v : projection) {
                    double angle = Math.atan2(v.y, v.x);
                    angle -= segmentAngle * Math.floor(angle / segmentAngle);
                    angle = Math.min(angle, segmentAngle - angle);
                    angle += startAngle.getValue();
                    double radius = Math.sqrt(v.dot(v));
                    LXVector sampleVector = new LXVector((float) Math.sin(angle), (float) Math.cos(angle), 0).mult((float) radius);
                    LXVector closest = null;
                    double minDistance = Double.MAX_VALUE;
                    for (LXVector w : projection) {
                        if (w.dist(sampleVector) < minDistance) {
                            closest = w;
                            minDistance = w.dist(sampleVector);
                        }
                    }
                    if (closest == null) {
                        continue;
                    }
                    colors[v.point.index] = colors[closest.point.index];
                }
            }
        }
    }
}