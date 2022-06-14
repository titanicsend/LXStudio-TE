package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXProjection;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;

import java.util.ArrayList;
import java.util.List;

@LXCategory(LXCategory.TEST)
public class Kaleidoscope extends BasicEffect {

    private int numSegments = 6;

    public Kaleidoscope(LX lx) {
        super(lx);
    }

    @Override
    protected void run(double deltaMs, double enabledAmount) {
        if (enabledAmount > 0) {
            double segmentAngle = 2 * Math.PI / numSegments;
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