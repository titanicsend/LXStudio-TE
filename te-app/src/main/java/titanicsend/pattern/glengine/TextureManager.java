package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_COORDS;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

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

  // Textures created for each unique LX model/view
  private final Map<LXModel, CoordTexture> coordTextures = new HashMap<>();

  // Textures that have been loaded for a filename
  private final Map<String, StaticTexture> staticTextures = new HashMap<>();

  public TextureManager(LX lx, GLEngine glEngine) {
    this.lx = lx;
    this.glEngine = glEngine;

    // Register for top-level model changes
    lx.addListener(this);
  }

  public void initialize(GL4 gl4) {
    if (this.initialized) {
      throw new IllegalStateException("TextureManager already initialized");
    }
    this.initialized = true;

    this.gl4 = gl4;
    this.canvas = this.glEngine.getCanvas();
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    if (!initialized) {
      return;
    }

    // Top level model changed. Discard all coordinate textures.
    clearCoordinateTextures();
  }

  /**
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be called
   * by the parent pattern or effect at least once before the first frame is rendered and Should be
   * called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture *handle* of the view's coordinate texture
   */
  public int getCoordinatesTexture(LXModel model) {
    CoordTexture t = coordTextures.get(model);
    if (t != null) {
      return t.getHandle();
    }

    // Create a new coordinate texture
    this.canvas.getContext().makeCurrent();
    t = new CoordTexture();
    this.coordTextures.put(model, t);

    // Double check size of engine and model points
    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();
    final int enginePoints = width * height;
    final int modelPoints = model.points.length;
    // Sanity check: make sure the system resolution has enough points to hold the model
    if (modelPoints > enginePoints) {
      LX.error(
          String.format(
              "GLEngine resolution (%d) too small for number of points in the model (%d). Re-run with higher --resolution WxH",
              enginePoints, modelPoints));
    }

    // Create a FloatBuffer to hold the normalized coordinates of the model points
    FloatBuffer coords = GLBuffers.newDirectFloatBuffer(enginePoints * 3);

    // Copy the normalized model coordinates to the buffer
    // TODO: always insert points in top-level model order
    coords.rewind();
    for (int i = 0; i < enginePoints; i++) {
      // fill unused points with NaN so we can skip computation in the shader when possible
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      coords.put(Float.NaN);
    }
    for (LXPoint p : model.points) {
      coords.put(p.index * 3, p.xn);
      coords.put(p.index * 3 + 1, p.yn);
      coords.put(p.index * 3 + 2, p.zn);
    }
    coords.rewind();

    // Create an OpenGL texture to hold the coordinate data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_COORDS, t.getHandle());

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the coordinate data into the texture
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, width, height, 0, GL4.GL_RGB, GL_FLOAT, coords);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);
    gl4.glActiveTexture(GL_TEXTURE0);

    return t.getHandle();
  }

  /**
   * Load a static texture from a file and return the texture *handle*. If the texture is already
   * loaded, just increment the ref count and return the existing texture handle.
   */
  public int useTexture(String textureName) {
    try {
      // if the texture is already loaded, just increment the ref count
      StaticTexture t = staticTextures.get(textureName);
      if (t != null) {
        t.refCount++;
      } else {
        // otherwise, load the texture its file and bind it to the next available texture unit
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
   * This function must be called when the model changes (when it is edited, or when a project is
   * loaded/unloaded) to delete all existing view coordinate textures and remove them from the
   * textures map.
   */
  private void clearCoordinateTextures() {
    // TODO: make context current?

    for (CoordTexture t : this.coordTextures.values()) {
      t.dispose();
    }

    this.coordTextures.clear();
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
      // dispose coordinate textures
      clearCoordinateTextures();

      // dispose static textures
      for (StaticTexture t : this.staticTextures.values()) {
        t.dispose();
      }
      this.staticTextures.clear();
    }
    // stop listening for model changes
    this.lx.removeListener(this);
  }

  private class CoordTexture {
    final int[] handles = new int[1];

    CoordTexture() {
      // Generate texture handle
      gl4.glGenTextures(1, handles, 0);
    }

    int getHandle() {
      return handles[0];
    }

    void dispose() {
      gl4.glDeleteTextures(1, handles, 0);
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
