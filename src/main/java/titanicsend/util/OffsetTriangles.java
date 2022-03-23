package titanicsend.util;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;

// Construct from a triangle and a distance and you'll get two more triangles,
// parallel to the original and offset along its normal by that distance.
// this.inner is the one closer to (0,0,0).
public class OffsetTriangles {
    public LXVector[] inner;
    public LXVector[] outer;

    public OffsetTriangles(LXVector v0, LXVector v1, LXVector v2, float distance) {
        // Get the vector from v0 to each of v1 and v2, cross these,
        // normalize (divide the result by its magnitude to make it
        // the unit length) and multiply it by the desired distance.
        LXVector d10 = v1.copy().sub(v0);
        LXVector d20 = v2.copy().sub(v0);
        LXVector normal = d10.cross(d20).normalize().mult(distance);

        LXVector[] t0 = new LXVector[3];
        LXVector[] t1 = new LXVector[3];

        t0[0] = v0.copy().add(normal);
        t1[0] = v0.copy().sub(normal);

        t0[1] = v1.copy().add(normal);
        t1[1] = v1.copy().sub(normal);

        t0[2] = v2.copy().add(normal);
        t1[2] = v2.copy().sub(normal);

        if (t0[0].mag() < t1[0].mag()) {
            this.inner = t0;
            this.outer = t1;
        } else {
            this.inner = t1;
            this.outer = t0;
        }
    }

    public OffsetTriangles(LXVector[] vertexes, float distance) {
        this(vertexes[0], vertexes[1], vertexes[2], distance);
    }

    public void flip() {
        LXVector[] tmp = this.inner;
        this.inner = this.outer;
        this.outer = tmp;
    }
}