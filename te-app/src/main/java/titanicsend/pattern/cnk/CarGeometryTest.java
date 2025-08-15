package titanicsend.pattern.cnk;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.nio.FloatBuffer;
import java.util.List;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

/**
 * CarGeometryTest
 *
 * Step 1: demonstrate passing per-panel data (panel centroids) to the shader and drawing small
 * circles at those centers using model coordinates in the shader. In a later step we'll compute and
 * color by panel normals.
 */
@LXCategory("Combo FG")
public class CarGeometryTest extends GLShaderPattern {
  private static final int MAX_PANEL_COUNT = 512;

  private int panelCount = 0;
  private final FloatBuffer glPanelCenters; // vec3 per panel: (xn, yn, zn)
  private final FloatBuffer glPanelNormals; // vec3 per panel: unit normal in raw space
  private final FloatBuffer glPanelV0; // vec3 per panel: vertex 0 (normalized)
  private final FloatBuffer glPanelV1; // vec3 per panel: vertex 1 (normalized)
  private final FloatBuffer glPanelV2; // vec3 per panel: vertex 2 (normalized)
  private boolean updateGeometry = true;
  // Physical axis lengths of current model view (raw units)
  private float axisLx = 1f, axisLy = 1f, axisLz = 1f;

  public CarGeometryTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Allocate buffers: MAX_PANEL_COUNT vec3 entries (float count, not bytes)
    this.glPanelCenters = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);
    this.glPanelNormals = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);
    this.glPanelV0 = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);
    this.glPanelV1 = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);
    this.glPanelV2 = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);

    // Precompute normalized panel centroids
    computePanelCenters();

    controls.setRange(TEControlTag.SIZE, 0.1, 0.0, 1.0);

    // Hack: use for Z-axis translation
    controls.setRange(TEControlTag.WOW1, 0.0, -1.0, 1.0);

    // Hack: control how many balls get displayed
    controls.setRange(TEControlTag.LEVELREACTIVITY, 1.0, 0.0, 1.0);

    addCommonControls();

    addShader(
        GLShader.config(lx)
            .withFilename("car_geometry_pattern.fs")
            .withUniformSource(
                (s) -> {
                  if (updateGeometry) {
                    // Send panel centers once (they're static w.r.t. the dynamic model structure
                    // unless the model regenerates)
                    s.setUniform("panelCount", panelCount);
                    // For uniform arrays, OpenGL expects the base element name
                    // (e.g., "panelCenters[0]") when uploading a vector array
                    s.setUniform("panelCenters[0]", glPanelCenters, 3);
                    s.setUniform("panelNormals[0]", glPanelNormals, 3);
                    s.setUniform("panelV0[0]", glPanelV0, 3);
                    s.setUniform("panelV1[0]", glPanelV1, 3);
                    s.setUniform("panelV2[0]", glPanelV2, 3);
                    // small default radius in model XY space
                    s.setUniform("panelRadius", 0.02f);
                    // Provide axis lengths so the shader can correct for non-uniform scaling
                    s.setUniform("axisLengths", axisLx, axisLy, axisLz);
                    updateGeometry = false;
                  }
                }));
  }

  private void computePanelCenters() {
    List<TEPanelModel> panels = getModelTE().getPanels();
    this.panelCount = Math.min(panels.size(), MAX_PANEL_COUNT);

    this.glPanelCenters.clear();
    this.glPanelNormals.clear();
    this.glPanelV0.clear();
    this.glPanelV1.clear();
    this.glPanelV2.clear();

    // Compute model extents from all points in current LXModel view
    float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
    for (LXPoint pt : getModel().points) {
      if (pt.x < minX) minX = (float) pt.x;
      if (pt.y < minY) minY = (float) pt.y;
      if (pt.z < minZ) minZ = (float) pt.z;
      if (pt.x > maxX) maxX = (float) pt.x;
      if (pt.y > maxY) maxY = (float) pt.y;
      if (pt.z > maxZ) maxZ = (float) pt.z;
    }
    this.axisLx = Math.max(1e-6f, maxX - minX);
    this.axisLy = Math.max(1e-6f, maxY - minY);
    this.axisLz = Math.max(1e-6f, maxZ - minZ);
    for (int i = 0; i < panelCount; i++) {
      TEPanelModel p = panels.get(i);

      // Average normalized coordinates of the panel's points to get a normalized centroid
      double sx = 0, sy = 0, sz = 0;
      LXPoint[] pts = p.points;
      int n = pts.length;
      if (n == 0) {
        // Fallback to model-space centroid mapped approximately to 0..1 if empty (unlikely)
        // but keep values reasonable.
        sx = 0.5;
        sy = 0.5;
        sz = 0.5;
      } else {
        for (LXPoint pt : pts) {
          sx += pt.xn;
          sy += pt.yn;
          sz += pt.zn;
        }
        sx /= n;
        sy /= n;
        sz /= n;
      }

      glPanelCenters.put((float) sx);
      glPanelCenters.put((float) sy);
      glPanelCenters.put((float) sz);

      // Compute an approximate panel normal from raw coordinates
      float nx = 0, ny = 0, nz = 0;
      if (n >= 3) {
        // Find farthest pair (A,B)
        int ia = 0, ib = 1;
        double maxDist2 = -1;
        for (int a = 0; a < n; a++) {
          for (int b = a + 1; b < n; b++) {
            double dx = pts[a].x - pts[b].x;
            double dy = pts[a].y - pts[b].y;
            double dz = pts[a].z - pts[b].z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > maxDist2) {
              maxDist2 = d2;
              ia = a;
              ib = b;
            }
          }
        }
        // Choose C farthest from line AB (maximize triangle area)
        int ic = -1;
        double maxArea = -1;
        double ax = pts[ia].x, ay = pts[ia].y, az = pts[ia].z;
        double bx = pts[ib].x, by = pts[ib].y, bz = pts[ib].z;
        double abx = bx - ax, aby = by - ay, abz = bz - az;
        for (int c = 0; c < n; c++) {
          if (c == ia || c == ib) continue;
          double cx = pts[c].x, cy = pts[c].y, cz = pts[c].z;
          // area proportional to norm of cross(AB, AC)
          double acx = cx - ax, acy = cy - ay, acz = cz - az;
          double cxp = aby * acz - abz * acy;
          double cyp = abz * acx - abx * acz;
          double czp = abx * acy - aby * acx;
          double area = cxp * cxp + cyp * cyp + czp * czp; // squared magnitude
          if (area > maxArea) {
            maxArea = area;
            ic = c;
          }
        }
        if (ic >= 0) {
          double cx = pts[ic].x, cy = pts[ic].y, cz = pts[ic].z;
          double acx = cx - ax, acy = cy - ay, acz = cz - az;
          // normal = normalize(cross(AB, AC))
          double nnx = aby * acz - abz * acy;
          double nny = abz * acx - abx * acz;
          double nnz = abx * acy - aby * acx;
          double len = Math.sqrt(nnx * nnx + nny * nny + nnz * nnz);
          if (len > 1e-9) {
            nx = (float) (nnx / len);
            ny = (float) (nny / len);
            nz = (float) (nnz / len);
          } else {
            nx = 0; ny = 0; nz = 1;
          }
          // Save the three vertex positions (normalized) in a consistent order
          LXPoint va = pts[ia];
          LXPoint vb = pts[ib];
          LXPoint vc = pts[ic];
          glPanelV0.put(va.xn).put(va.yn).put(va.zn);
          glPanelV1.put(vb.xn).put(vb.yn).put(vb.zn);
          glPanelV2.put(vc.xn).put(vc.yn).put(vc.zn);
        } else {
          nx = 0; ny = 0; nz = 1;
          // Fallback vertices: approximate with centroid in all slots
          glPanelV0.put((float) sx).put((float) sy).put((float) sz);
          glPanelV1.put((float) sx).put((float) sy).put((float) sz);
          glPanelV2.put((float) sx).put((float) sy).put((float) sz);
        }
      } else {
        nx = 0; ny = 0; nz = 1;
        glPanelV0.put((float) sx).put((float) sy).put((float) sz);
        glPanelV1.put((float) sx).put((float) sy).put((float) sz);
        glPanelV2.put((float) sx).put((float) sy).put((float) sz);
      }
      glPanelNormals.put(nx);
      glPanelNormals.put(ny);
      glPanelNormals.put(nz);
    }
    this.glPanelCenters.rewind();
    this.glPanelNormals.rewind();
    this.glPanelV0.rewind();
    this.glPanelV1.rewind();
    this.glPanelV2.rewind();
    this.updateGeometry = true;

    // Debug logging: dump count and a sample of centers
    TE.log("CarGeometryTest: panelCount=%d (buffer cap=%d)", panelCount, glPanelCenters.capacity() / 3);
    int sample = Math.min(panelCount, 12);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < sample; i++) {
      int base = i * 3;
      float cx = glPanelCenters.get(base);
      float cy = glPanelCenters.get(base + 1);
      float cz = glPanelCenters.get(base + 2);
      sb.append(String.format("[%d:(%.3f,%.3f,%.3f)] ", i, cx, cy, cz));
    }
    TE.log("CarGeometryTest centers sample: %s", sb.toString());
    // Log a few normals
    StringBuilder sbn = new StringBuilder();
    for (int i = 0; i < Math.min(panelCount, 6); i++) {
      int base = i * 3;
      float nx = glPanelNormals.get(base);
      float ny = glPanelNormals.get(base + 1);
      float nz = glPanelNormals.get(base + 2);
      sbn.append(String.format("[%d:(%.2f,%.2f,%.2f)] ", i, nx, ny, nz));
    }
    TE.log("CarGeometryTest normals sample: %s", sbn.toString());
    TE.log("CarGeometryTest axis lengths: Lx=%.3f Ly=%.3f Lz=%.3f", axisLx, axisLy, axisLz);
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    // Recompute panel centers when the underlying model changes
    computePanelCenters();
  }
}
