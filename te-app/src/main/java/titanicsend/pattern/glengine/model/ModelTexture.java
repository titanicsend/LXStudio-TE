package titanicsend.pattern.glengine.model;

import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.util.HashMap;
import java.util.Map;
import titanicsend.pattern.glengine.GLEngine;

public abstract class ModelTexture {

  protected final LX lx;
  protected final GLEngine glEngine;
  protected final GL4 gl4;

  private final Map<LXModel, Texture> textures = new HashMap<>();

  public ModelTexture(LX lx, GLEngine glEngine) {
    this.lx = lx;
    this.glEngine = glEngine;
    this.gl4 = glEngine.getGL4();

    this.lx.addListener(this.lxListener);
  }

  private final LX.Listener lxListener =
      new LX.Listener() {
        @Override
        public void modelGenerationChanged(LX lx, LXModel model) {
          // The model object didn't change but the points moved. All texture contents are stale.
          setStale();
        }
      };

  private Texture createTexture(LXModel model) {
    Texture texture = new Texture();

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
    refreshTextureContents(texture.getHandle(), enginePoints, model);

    return texture;
  }

  public int getTexture(LXModel model) {
    if (model == null) {
      throw new IllegalArgumentException("Model must not be null");
    }

    Texture texture = this.textures.get(model);
    if (texture == null) {
      texture = createTexture(model);
      this.textures.put(model, texture);
    }
    return texture.getHandle();
  }

  protected abstract void refreshTextureContents(int handle, int enginePoints, LXModel model);

  private void setStale() {
    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();
    final int enginePoints = width * height;

    for (Map.Entry<LXModel, Texture> entry : this.textures.entrySet()) {
      LXModel model = entry.getKey();
      Texture texture = entry.getValue();
      // Recalculate now
      refreshTextureContents(texture.getHandle(), enginePoints, model);
    }
  }

  public void dispose() {
    this.lx.removeListener(this.lxListener);
    for (Texture texture : this.textures.values()) {
      texture.dispose();
    }
    textures.clear();
  }

  /**
   * Internal class which is not totally necessary at the moment, but might be later if we want to
   * mark it as stale and recalculate on the fly, for example.
   */
  private class Texture {
    // OpenGL texture handle for this lxmodel texture
    // Will be 0 (invalid) if the texture has not been created yet.
    final int[] handles = new int[1];

    Texture() {
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
