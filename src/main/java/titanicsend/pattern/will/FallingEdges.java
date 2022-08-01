package titanicsend.pattern.will;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TE;

/**
 * This is a really tricky pattern.
 *
 * To do it we need to have an array of points representing our single edge to fall, and update it overtime with trajectory.
 *
 * Then we have to pick a viewerpoint, otherwise there would be inifinite number of posisble positions to draw the edge on.
 * Let's pick looking directly at the car from the front, so essentially squash the X dimension.
 *
 * https://math.stackexchange.com/questions/2305792/3d-projection-on-a-2d-plane-weak-maths-ressources
 *
 * for (each pixel on car)
 *      draw a ray from camera to pixel on car
 *      if ray intersects with bounding rectangle of falling edge,
 *          then find closest point on THAT edge's points, and color the
 *          pixel on car according to distance the ray is from point on edge
 *
 * Note: will want the camera preeeeettty far back into audience, i think!
 */
@LXCategory("DREVO Shaders")
public class FallingEdges extends TEPattern {
    public static final float MAX_DIST = 17670660f;

    private float[][] coords;
    private int[] indices;
    private int t = 0;

    public FallingEdges(LX lx) {
        super(lx);

        // get edge points
        TEEdgeModel edge = this.model.edgesById.get("30-118");
        coords = new float[edge.points.length][3];
        indices = new int[edge.points.length];
        int i = 0;
        for (LXPoint p : edge.points) {
            coords[i][0] = (float) p.x;
            coords[i][1] = (float) p.y; // this is up & down
            coords[i][2] = (float) p.z;
            indices[i] = p.index;
            i++;
        }
    }

    private void updateWithGravity() {
        for (int i = 0; i < indices.length; i++) {
            coords[i][1] = coords[i][1] - 1000000f / 8f; //0.5f * 9.8f * t * t;
        }
    }

    public void run(double deltaMs) {
        for (LXPoint p : this.model.points) {
            double minDist = 3424242342342342342.;

//            int f = 0;
//            for (TEEdgeModel edge : this.model.edgesById.values()) {
//                if (edge.getId().equals("30-118")) {
//                    for (LXPoint e : edge.points) {
//                        if (p.index == e.index) {
//                            minDist = 0.0;
//                            break;
//                        }
//                        float dist = distance(p, e);
//                        if (dist < minDist)
//                            minDist = dist;
//                        f++;
//                    }
//                    break;
//                }
//            }
//            float percOfMax = (float) (minDist / MAX_DIST);
//            colors[p.index] = LXColor.hsb(180f, 100f, (1.f - Math.pow(percOfMax, 0.1)) * 100f);

            for (int i = 0; i < indices.length; i++) {
                float dist = distance(p, coords[i][0], coords[i][1], coords[i][2]);
                if (dist < minDist)
                    minDist = dist;
            }

            float percOfMax = (float) (minDist / MAX_DIST);
            float brightness = clamp(0f, 100f, (float)(1.f - Math.pow(percOfMax, 0.2)) * 100f);
            colors[p.index] = LXColor.hsb(180f, 100f, brightness);
        }

        updateWithGravity();
    }

    private float clamp(float min, float max, float v) {
        if (v > max) return max;
        if (v < min) return min;
        return v;
    }

    private float distance(LXPoint a, LXPoint b) {
        return (float)Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2) + Math.pow(b.z - a.z, 2));
    }

    private float distance(LXPoint a, double x, double y, double z) {
        return (float)Math.sqrt(Math.pow(x - a.x, 2) + Math.pow(y - a.y, 2) + Math.pow(z - a.z, 2));
    }


//    private static double distPointToLineSegment(LXPoint other, LXPoint v0, LXPoint v1) {
//
//        return 0.0;
//    }
}
