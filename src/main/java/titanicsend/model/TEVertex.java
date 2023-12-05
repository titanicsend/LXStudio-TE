package titanicsend.model;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.util.TEMath;

public class TEVertex extends LXVector {
    public static HashMap<Integer, TEVertex> vertexesById;

    public int id;
    public Set<TEEdgeModel> edges;

    // Set to non-null and the virtual display will shade vertex's sphere
    public TEVirtualColor virtualColor;

    public TEVertex(LXVector vector, int id) {
        super(vector);
        this.id = id;
        this.edges = new HashSet<TEEdgeModel>();
        this.virtualColor = new TEVirtualColor(255, 255, 255, 255);
    }

    public static double distance(LXVector v, float x, float y, float z) {
        return TEMath.distance(v.x, v.y, v.z, x, y, z);
    }

    public String repr() {
        return "(" + this.x + "," + this.y + "," + this.z + ")";
    }

    public static double distance(LXVector v, LXPoint p) {
        return distance(v, p.x, p.y, p.z);
    }

    public double distanceTo(LXVector v) {
        return distance(this, v.x, v.y, v.z);
    }

    public void addEdge(TEEdgeModel edge) {
        edges.add(edge);
    }

    public void nudgeToward(LXVector other, float distance) {
        lerp(other, distance);
    }
}
