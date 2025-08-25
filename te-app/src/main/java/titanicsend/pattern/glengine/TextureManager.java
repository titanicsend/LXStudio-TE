package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import titanicsend.pattern.glengine.model.ModelCoordsTexture;
import titanicsend.pattern.glengine.model.ModelTexture;

// Manages the lifecycle of the (relatively) static OpenGL textures used by
// the shader engine.  We use are two types of textures: coordinate and static.
// Coordinate textures hold floating point normalized XYZ coordinates of the
// points in the various views of the model.  Static textures are loaded from
// image files, and are used as 2D textures in the shaders.
//
public class TextureManager implements LX.Listener {

  private final LX lx;
  private final GLEngine glEngine;
  private GLAutoDrawable canvas;
  private GL4 gl4;

  private boolean initialized = false;

  // For each LXModel we now store two ModelTextures
  // [0] = normalized model coordinates at current gl_FragCoord
  // [1] = (GL_RG32F) indices of the model points
  private final List<ModelTexture> modelTextures = new ArrayList<>();

  private ModelCoordsTexture modelCoordsTexture = null;

  // Textures that have been loaded for a filename
  private final Map<String, StaticTexture> staticTextures = new HashMap<>();

  public TextureManager(LX lx, GLEngine glEngine) {
    this.lx = lx;
    this.glEngine = glEngine;
  }

  public void initialize(GL4 gl4) {
    if (this.initialized) {
      throw new IllegalStateException("TextureManager already initialized");
    }
    this.initialized = true;

    this.gl4 = gl4;
    this.canvas = this.glEngine.getCanvas();
  }

  public void registerModelTexture(ModelTexture modelTexture) {
    if (this.modelTextures.contains(Objects.requireNonNull(modelTexture))) {
      throw new IllegalArgumentException(
          "ModelTextureType " + modelTexture + " already registered");
    }
    this.modelTextures.add(modelTexture);
  }

  /**
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be called
   * by the parent pattern or effect at least once before the first frame is rendered and Should be
   * called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * <p>This returns the handle for coordinates model texture.
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture handle of the view's coordinates texture
   */
  public int getModelCoordsTexture(LXModel model) {
    if (this.modelCoordsTexture == null) {
      this.modelCoordsTexture = new ModelCoordsTexture(this.lx, this.glEngine);
      registerModelTexture(this.modelCoordsTexture);
    }

    return this.modelCoordsTexture.getTexture(model);
  }

  /** Delete all existing lxmodel-derived textures (both slots) and clear the map. */
  private void clearModelTextures() {
    for (ModelTexture modelTexture : this.modelTextures) {
      modelTexture.dispose();
    }
    this.modelTextures.clear();
  }

  /**
   * Load a static texture from a file and return the texture *handle*. If the texture is already
   * loaded, just increment the ref count and return the existing texture handle.
   */
  public int useTexture(String textureName) {
    try {
      StaticTexture t = staticTextures.get(textureName);
      if (t != null) {
        t.refCount++;
      } else {
        File file = new File(textureName);
        Texture texture = TextureIO.newTexture(file, false);
        t = new StaticTexture(texture);
        staticTextures.put(textureName, t);
      }
      return t.getHandle();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Release a static texture by name.
   *
   * @param textureName - filename of the texture to release
   */
  public void releaseStaticTexture(String textureName) {
    StaticTexture t = staticTextures.get(textureName);
    if (t == null) {
      throw new RuntimeException("Attempted to release texture that was never created: ");
    }
    t.refCount--;
    if (t.refCount <= 0) {
      t.dispose();
      staticTextures.remove(textureName);
    }
  }

  public void dispose() {
    if (this.initialized) {
      // dispose model textures
      clearModelTextures();

      // dispose static textures
      for (StaticTexture t : this.staticTextures.values()) {
        t.dispose();
      }
      this.staticTextures.clear();
    }
  }

  private class StaticTexture {
    final Texture texture;
    int refCount = 1;

    StaticTexture(Texture texture) {
      this.texture = texture;
    }

    int getHandle() {
      return texture.getTextureObject();
    }

    void dispose() {
      texture.destroy(gl4);
    }
  }
}
