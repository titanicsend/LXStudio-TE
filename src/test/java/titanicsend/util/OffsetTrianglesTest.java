package titanicsend.util;

import heronarts.lx.transform.LXVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OffsetTrianglesTest {
    @Test
    public void OffsetVertexesCorrectDistance() {
        LXVector[] vertexes = new LXVector[]{
                new LXVector(0, 0, 0),
                new LXVector(0, 1, 0),
                new LXVector(1, 1, 0)
        };

        OffsetTriangles ot = new OffsetTriangles(vertexes, 1);

        assert ot.inner.length == 3;

        for (int i=0; i < 3; i++) {
            assertEquals(ot.inner[i].copy().sub(vertexes[i]).mag(), 1);
        }
    }
}