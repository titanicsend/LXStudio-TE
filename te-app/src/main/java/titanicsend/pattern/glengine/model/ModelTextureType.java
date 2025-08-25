package titanicsend.pattern.glengine.model;

import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.util.HashMap;
import java.util.Map;
import titanicsend.pattern.glengine.GLEngine;

public abstract class ModelTextureType {

  protected final LX lx;
  protected final GLEngine glEngine;
  protected final GL4 gl4;

  private final Map<LXModel, ModelTexture> modelTextures = new HashMap<>();

  public ModelTextureType(LX lx, GLEngine glEngine, GL4 gl4) {
    this.lx = lx;
    this.glEngine = glEngine;
    this.gl4 = gl4;

    this.lx.addListener(this.lxListener);
  }

  private final LX.Listener lxListener =
      new LX.Listener() {
        @Override
        public void modelGenerationChanged(LX lx, LXModel model) {
          // The model (object) didn't change but the points moved. Mark all textures as stale.
          setStale();
        }
      };

  public ModelTexture getModelTexture(LXModel model) {
    if (model == null) {
      throw new IllegalArgumentException("Model must not be null");
    }

    ModelTexture modelTexture = modelTextures.get(model);
    if (modelTexture == null) {
      modelTexture = createModelTexture(model);
      modelTextures.put(model, modelTexture);
    }
    return modelTexture;
  }

  private ModelTexture createModelTexture(LXModel model) {
    ModelTexture modelTexture = new ModelTexture();
    modelTextures.put(model, modelTexture);

    // Double check size of engine and model points
    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();
    final int enginePoints = width * height;
    final int modelPoints = model.points.length;
    if (modelPoints > enginePoints) {
      LX.error(
          String.format(
              "GLEngine resolution (%d) too small for number of points in the model (%d). Re-run with higher --resolution WxH",
              enginePoints, modelPoints));
    }
    refreshTextureContents(modelTexture.getHandle(), enginePoints, model);

    return modelTexture;
  }

  protected abstract void refreshTextureContents(int handle, int enginePoints, LXModel model);

  private void setStale() {
    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();
    final int enginePoints = width * height;

    for (Map.Entry<LXModel, ModelTexture> entry : this.modelTextures.entrySet()) {
      LXModel model = entry.getKey();
      ModelTexture modelTexture = entry.getValue();
      // Recalculate now
      refreshTextureContents(modelTexture.getHandle(), enginePoints, model);
    }
  }

  public void dispose() {
    this.lx.removeListener(this.lxListener);
    for (ModelTexture modelTexture : this.modelTextures.values()) {
      modelTexture.dispose();
    }
    modelTextures.clear();
  }

  protected class ModelTexture {
    // OpenGL texture handle for this lxmodel texture
    // Will be 0 (invalid) if the texture has not been created yet.
    final int[] handles = new int[1];

    ModelTexture() {
      gl4.glGenTextures(1, handles, 0);
    }

    int getHandle() {
      return handles[0];
    }

    void dispose() {
      gl4.glDeleteTextures(1, handles, 0);
    }
  }
}
