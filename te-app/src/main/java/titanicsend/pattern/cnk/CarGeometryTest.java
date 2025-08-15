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
  private boolean updateGeometry = true;
  // Physical axis lengths of current model view (raw units)
  private float axisLx = 1f, axisLy = 1f, axisLz = 1f;

  public CarGeometryTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Allocate buffer: MAX_PANEL_COUNT vec3 entries (float count, not bytes)
    this.glPanelCenters = Buffers.newDirectFloatBuffer(MAX_PANEL_COUNT * 3);

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
    }
    this.glPanelCenters.rewind();
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
    TE.log("CarGeometryTest axis lengths: Lx=%.3f Ly=%.3f Lz=%.3f", axisLx, axisLy, axisLz);
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    // Recompute panel centers when the underlying model changes
    computePanelCenters();
  }
}
