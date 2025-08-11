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
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_COORDS;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_COORD_MAP;

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
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

// Manages the lifecycle of the (relatively) static OpenGL textures used by
// the shader engine.  We use are two types of textures: coordinate and static.
// Coordinate textures hold floating point normalized XYZ coordinates of the
// points in the various views of the model.  Static textures are loaded from
// image files, and are used as 2D textures in the shaders.
//
public class TextureManager implements LX.Listener {
  private static final int COORDINATE_TEXTURE_COUNT = 2;

  private final LX lx;
  private final GLEngine glEngine;
  private GLAutoDrawable canvas;
  private GL4 gl4;

  private boolean initialized = false;

  // For each LXModel we now store two CoordTextures
  // [0] = normalized model coordinates at current gl_FragCoord
  // [1] = (GL_RG32I) integer indices of the model points
  private final Map<LXModel, CoordTexture[]> coordTextures = new HashMap<>();

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
   * Create the coordinate textures for a model. This should be called by the parent pattern or
   * effect at least once before the first frame is rendered, and when the model or
   * view changes.
   *
   * This adds an entry to coordTextures that contains two textures:
   * one for normalized coordinates and one for index mapping.
   *
   * @param model The model (view) to copy coordinates from
   */
  public void createCoordinateTextures(LXModel model) {

    // Create new array with two entries, the first for normalized coordinates
    // and the second for index mapping.
    this.canvas.getContext().makeCurrent();
    CoordTexture[] slots = new CoordTexture[COORDINATE_TEXTURE_COUNT];
    CoordTexture normalizedXYZ = new CoordTexture();
    slots[0] = normalizedXYZ;
    CoordTexture indexMap = new CoordTexture();
    slots[1] = indexMap;
    this.coordTextures.put(model, slots);

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

    // Create an IntBuffer to hold the integer indices of the model points
    IntBuffer indices = GLBuffers.newDirectIntBuffer(enginePoints * 2);

    // Initialize with NaNs for coordinates and -1 for indices
    coords.rewind();
    indices.rewind();
    for (int i = 0; i < enginePoints; i++) {
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      indices.put(-1);
      indices.put(-1);
    }
    // Insert normalized coordinates and their indices into the buffers
    for (LXPoint p : model.points) {
      coords.put(p.index * 3, p.xn);
      coords.put(p.index * 3 + 1, p.yn);
      coords.put(p.index * 3 + 2, p.zn);

      // convert the index to 2D coordinates using the model's width and height
      indices.put(p.index * 2, p.index % width);
      indices.put(p.index * 2 + 1, p.index / width);
    }
    coords.rewind();
    indices.rewind();

    // Create an OpenGL texture to hold the coordinate data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_COORDS, normalizedXYZ.getHandle());

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the coordinate data into the texture
    gl4.glTexImage2D(
      GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, width, height, 0, GL4.GL_RGB, GL_FLOAT, coords);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);

    // And create an OpenGL texture to hold the index data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_COORD_MAP, indexMap.getHandle());

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the index data into the texture
    gl4.glTexImage2D(
      GL4.GL_TEXTURE_2D, 0, GL4.GL_RG32I, width, height, 0, GL4.GL_RG_INTEGER, GL4.GL_INT, indices);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);
    gl4.glActiveTexture(GL_TEXTURE0);
  }

  /**
   * Get the texture handle for a coordinate texture entry in a model.
   *
   * @param model The model (view) to retrieve the texture from
   * @param entryId The entry ID of the texture to retrieve
   * @return The OpenGL texture handle for the specified coordinate texture entry
   */
  public int getCoordinateTextureHandle(LXModel model, int entryId) {
    // Check if the model has coordinate textures
    if (!this.coordTextures.containsKey(model)) {
       // OpenGL uses 0 to indicate an invalid texture handle
       return 0;
    }

    // Retrieve array of texture entries for the model
    CoordTexture[] slots = coordTextures.get(model);
    CoordTexture tex;
    switch (entryId) {
      case TEXTURE_UNIT_COORDS: // normalized coordinates
        tex = slots[0];
        break;
      case TEXTURE_UNIT_COORD_MAP: // index mapping
        tex = slots[0];
        break;
      default:
        // invalid entry ID
        throw new IllegalArgumentException("Invalid coordinate texture entry ID: " + entryId);
    }
    return tex.getHandle();
  }

  /**
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be called
   * by the parent pattern or effect at least once before the first frame is rendered and Should be
   * called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * This returns the handle for coordinate texture slot 0 (primary).
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture handle of the view's primary (index 0) coordinate texture
   */
  public int getCoordinatesTexture(LXModel model) {
    int normalizedXYZ = getCoordinateTextureHandle(model, TEXTURE_UNIT_COORDS);

    // If the texture does not exist, create it
    if (normalizedXYZ == 0) {
      createCoordinateTextures(model);
      normalizedXYZ = getCoordinateTextureHandle(model, TEXTURE_UNIT_COORDS);
    }
    return normalizedXYZ;
  }

  /**
   * Copy a model's index mapping into a special texture for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered and should be called
   * by the pattern's frametime run() function on every frame for full Chromatik view support.
   *
   * This returns the handle for coordinate texture slot 1 (index map).
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture handle of the view's index mapping coordinate texture
   */
  public int getIndexMapTexture(LXModel model) {
    int indexMap = getCoordinateTextureHandle(model, TEXTURE_UNIT_COORD_MAP);

    // If the texture does not exist, create it
    if (indexMap == 0) {
      createCoordinateTextures(model);
      indexMap = getCoordinateTextureHandle(model, TEXTURE_UNIT_COORD_MAP);
    }
    return indexMap;
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
   * Delete all existing view coordinate textures (both slots) and clear the map.
   */
  private void clearCoordinateTextures() {
    for (CoordTexture[] arr : this.coordTextures.values()) {
      if (arr != null) {
        for (int i = 0; i < arr.length; i++) {
          if (arr[i] != null) {
            arr[i].dispose();
            arr[i] = null;
          }
        }
      }
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
    // OpenGL texture handle for this coordinate texture
    // Will be 0 (invalid) if the texture has not been created yet.
    final int[] handles = new int[1];

    CoordTexture() {
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
