package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_MODEL_COORDS;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_MODEL_INDEX;

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
  private static final int MODEL_TEXTURE_COUNT = 2;

  private final LX lx;
  private final GLEngine glEngine;
  private GLAutoDrawable canvas;
  private GL4 gl4;

  private boolean initialized = false;

  // For each LXModel we now store two ModelTextures
  // [0] = normalized model coordinates at current gl_FragCoord
  // [1] = (GL_RG32F) indices of the model points
  private final Map<LXModel, ModelTexture[]> modelTextures = new HashMap<>();

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
    clearModelTextures();
  }

  /**
   * Writes the index of a model point to a 5x5 pixel neighborhood in the index texture buffer. This
   * greatly improves the quality of sampling from the model buffer
   *
   * @param p The model point
   * @param indices The float buffer for the index texture
   * @param width The width of the texture
   * @param height The height of the texture
   */
  private void setIndexNeighborhood(LXPoint p, FloatBuffer indices, int width, int height) {
    // Calculate the center pixel coordinates from the point's normalized position
    int px = Math.round(p.xn * (width - 1));
    int py = Math.round(p.yn * (height - 1));

    // Convert index to 2D coordinates
    float val1 = (float) (p.index % width);
    float val2 = (float) Math.floor(p.index / width);

    // Iterate over neighborhood centered at (px, py)
    for (int ny = py - 2; ny <= py + 2; ny++) {
      for (int nx = px - 2; nx <= px + 2; nx++) {
        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
          int destIndex = (ny * width + nx) * 2;
          indices.put(destIndex, val1);
          indices.put(destIndex + 1, val2);
        }
      }
    }
  }

  /**
   * Create the coordinate textures for a model. This should be called by the parent pattern or
   * effect at least once before the first frame is rendered, and when the model or view changes.
   *
   * <p>This adds an entry to coordTextures that contains two textures: one for normalized
   * coordinates and one for index mapping.
   *
   * @param model The model (view) to copy coordinates from
   */
  public void createModelTextures(LXModel model) {

    // Create new array with two entries, the first for normalized coordinates
    // and the second for index mapping.
    this.canvas.getContext().makeCurrent();
    ModelTexture[] slots = new ModelTexture[MODEL_TEXTURE_COUNT];
    ModelTexture normalizedXYZ = new ModelTexture();
    slots[0] = normalizedXYZ;
    ModelTexture indexMap = new ModelTexture();
    slots[1] = indexMap;
    this.modelTextures.put(model, slots);

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

    // Create a FloatBuffer to hold the normalized coordinates of the model points
    FloatBuffer coords = GLBuffers.newDirectFloatBuffer(enginePoints * 3);

    // Create a buffer to hold the indices of the model points
    FloatBuffer indices = GLBuffers.newDirectFloatBuffer(enginePoints * 2);

    // Initialize with NaNs for coordinates and indices
    coords.rewind();
    indices.rewind();
    for (int i = 0; i < enginePoints; i++) {
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      indices.put(Float.NaN);
      indices.put(Float.NaN);
    }
    // Insert normalized coordinates and their indices into the buffers
    for (LXPoint p : model.points) {
      int destIndex = p.index * 3;
      coords.put(destIndex, p.xn);
      coords.put(destIndex + 1, p.yn);
      coords.put(destIndex + 2, p.zn);

      // save normalized coordinates to a rectangular texture index
      // using the model's width and height. We actually write to
      // a rectangular neighborhood around the target pixel to compensate
      // for rounding errors in sampling, and the plain old non-contiguous
      // nature of the model points.
      setIndexNeighborhood(p, indices, width, height);
    }

    coords.rewind();
    indices.rewind();

    // Create an OpenGL texture to hold the coordinate data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_MODEL_COORDS, normalizedXYZ.getHandle());

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the coordinate data into the texture
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, width, height, 0, GL4.GL_RGB, GL_FLOAT, coords);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);
    gl4.glActiveTexture(GL_TEXTURE0);

    // And create an OpenGL texture to hold the index data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_MODEL_INDEX, indexMap.getHandle());

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the index data into the texture
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RG32F, width, height, 0, GL4.GL_RG, GL4.GL_FLOAT, indices);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);
    gl4.glActiveTexture(GL_TEXTURE0);
  }

  private boolean hasModelTextures(LXModel model) {
    return this.modelTextures.containsKey(model);
  }

  /**
   * Get the texture handle for an LXModel-derived texture
   *
   * @param model The model (view) that the textures were derived from
   * @param textureUnit The reserved texture unit of the texture to retrieve
   * @return The OpenGL texture handle for the specified model texture
   */
  private int getModelTexture(LXModel model, int textureUnit) {
    // Retrieve array of texture handles for the model
    ModelTexture[] slots = modelTextures.get(model);
    ModelTexture tex;
    switch (textureUnit) {
      case TEXTURE_UNIT_MODEL_COORDS: // normalized coordinates
        tex = slots[0];
        break;
      case TEXTURE_UNIT_MODEL_INDEX: // textureUnit mapping
        tex = slots[1];
        break;
      default:
        // invalid textureUnit - this is a programming error and should not happen
        // so we throw an exception to make a big fuss.
        throw new IllegalArgumentException("Invalid model texture unit: " + textureUnit);
    }
    return tex.getHandle();
  }

  /**
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be called
   * by the parent pattern or effect at least once before the first frame is rendered and Should be
   * called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * <p>This returns the handle for model texture slot 0 (coordinates).
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture handle of the view's coordinates texture
   */
  public int getModelCoordsTexture(LXModel model) {
    // If the texture does not exist, create it
    if (!hasModelTextures(model)) {
      createModelTextures(model);
    }
    return getModelTexture(model, TEXTURE_UNIT_MODEL_COORDS);
  }

  /**
   * Copy a model's index mapping into a special texture for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered and should be called
   * by the pattern's frametime run() function on every frame for full Chromatik view support.
   *
   * <p>This returns the handle for model texture slot 1 (index map).
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture handle of the view's indices texture
   */
  public int getModelIndexTexture(LXModel model) {
    if (!hasModelTextures(model)) {
      createModelTextures(model);
    }
    return getModelTexture(model, TEXTURE_UNIT_MODEL_INDEX);
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

  /** Delete all existing lxmodel-derived textures (both slots) and clear the map. */
  private void clearModelTextures() {
    for (ModelTexture[] arr : this.modelTextures.values()) {
      if (arr != null) {
        for (int i = 0; i < arr.length; i++) {
          if (arr[i] != null) {
            arr[i].dispose();
            arr[i] = null;
          }
        }
      }
    }
    this.modelTextures.clear();
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
    // stop listening for model changes
    this.lx.removeListener(this);
  }

  private class ModelTexture {
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
