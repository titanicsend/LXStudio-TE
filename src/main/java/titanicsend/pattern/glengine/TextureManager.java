package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import static com.jogamp.opengl.GL.*;

// Manages the lifecycle of the (relatively) static OpenGL textures used by
// the shader engine.  We use are two types of textures: coordinate and static.
// Coordinate textures hold floating point normalized XYZ coordinates of the
// points in the various views of the model.  Static textures are loaded from
// image files, and are used as 2D textures in the shaders.
//
public class TextureManager {

  private enum TextureType {
    COORDINATE,
    STATIC
  }

  private GL4 gl4;

  // per texture data for the texture cache
  private class CachedTexture {
    private Texture texture;
    private int textureUnit;
    private TextureType type;
    private int refCount;
  }

  // the next available OpenGL texture unit number
  private static final int FIRST_UNRESERVED_TEXTURE_UNIT = 3;
  private int nextTextureUnit = FIRST_UNRESERVED_TEXTURE_UNIT;

  // map of texture hash codes to texture data
  // The hash code uniquely identifies the texture, the texture unit number is
  // used to bind the texture to the GPU, and the other data is used for
  // cache management.
  private final HashMap<Integer, CachedTexture> textures = new HashMap<>();

  // a list of that we can use to keep track of released texture unit numbers
  private final ArrayList<Integer> releasedTextureUnits = new ArrayList<>();

  /**
   * Almost certainly reliable hashing function!
   * This generates a hash code for a texture file name string, which is
   * then used to look up the texture unit number in our static textures map.
   * It is, as mentioned, almost certainly reliable for our use case, but
   * not absolutely guaranteed to generate a unique code for every
   * possible filename.
   */
  private int stringToHash(String s) {
    int hash = 0;
    for (int i = 0; i < s.length(); i++) {
      hash = (hash << 5) - hash + s.charAt(i);
    }
    return hash;
  }

  /**
   * Copy LXPoints' normalized coordinates into textures for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered. And should be called
   * by the pattern's frametime run() function if the model has changed since the last frame.
   */
  public int useCoordinateTexture(LXModel model, FloatBuffer modelCoords) {
    CachedTexture t;

    // if the view's coordinate texture is already loaded, just use it
    // NOTE: We don't use the cache's texture reference counting mechanism
    // for the coordinate texture. Once a view coord texture is loaded, it's never
    // released until the model changes.
    int textureHash = model.hashCode();
    if (textures.containsKey(textureHash)) {
      t = textures.get(textureHash);
    } else {
      // otherwise, create a new coordinate texture and bind it to the next available texture unit
      t = new CachedTexture();
      t.type = TextureType.COORDINATE;
      t.texture = createCoordinateTexture(gl4, model);
      t.textureUnit = getNextTextureUnit();
      t.refCount = 1;
      textures.put(textureHash, t);

      gl4.glActiveTexture(GL_TEXTURE0 + t.textureUnit);
      gl4.glEnable(GL_TEXTURE_2D);
      t.texture.enable(gl4);
      t.texture.bind(gl4);
    }
    // return the texture unit number
    return t.textureUnit;
  }

  /**
   * Create a coordinate texture from the normalized coordinates of the given model
   * and bind it to the next available texture unit.  If the view's coordinate texture
   * already exists,return the bound texture unit number.
   */
  public Texture createCoordinateTexture(GL4 gl4, LXModel model) {
    // get data we need to create the texture from the system and
    // the model
    int xSize = GLEngine.getWidth();
    int ySize = GLEngine.getHeight();
    int maxPoints = xSize * ySize;

    final int numPoints = model.points.length;

    // Create a FloatBuffer to hold the normalized coordinates of the model points
    FloatBuffer coords = GLBuffers.newDirectFloatBuffer(maxPoints * 3);

    // Copy the normalized model coordinates to the buffer
    coords.rewind();
    for (int i = 0; i < maxPoints; i++) {
      if (i < numPoints) {
        coords.put(model.points[i].xn);
        coords.put(model.points[i].yn);
        coords.put(model.points[i].zn);
      } else {
        coords.put(0);
        coords.put(0);
        coords.put(0);
      }
    }
    coords.rewind();

    // Sanity check: make sure the system resolution has enough points
    // to hold the model.
    // This should really never fail, but we're checkin' anyway.
    if (numPoints > maxPoints) {
      LX.error(
        "GLEngine resolution ("
          + maxPoints
          + ") too small for number of points in the model ("
          + numPoints
          + ")");
    }

    // Now that we have our model data, use it to create an OpenGL texture
    TextureData textureData = new TextureData(
      gl4.getGLProfile(),
      GL4.GL_RGB32F,
      xSize,
      ySize,
      0,
      GL4.GL_RGB,
      GL4.GL_FLOAT,
      false,
      false,
      false,
      coords,
      null
    );

    // Create a jogamp Texture object from the TextureData
    Texture texture = TextureIO.newTexture(textureData);

    // Bind the texture to the GL context
    texture.bind(gl4);

    // Set the texture parameters
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    return texture;
  }

  /**
   Returns the next available texture unit number, either by reusing a released
   texture unit number or by allocating a new one.
  */
  public int getNextTextureUnit() {
    if (releasedTextureUnits.size() > 0) {
      return releasedTextureUnits.remove(0);
    } else {
      return nextTextureUnit++;
    }
  }

  public int releaseTextureUnit(int textureUnit) {
    if (textureUnit < FIRST_UNRESERVED_TEXTURE_UNIT) {
      throw new RuntimeException("GLEngine: Attempt to release a reserved texture unit: " + textureUnit);
    }
    if (releasedTextureUnits.contains(textureUnit)) {
      throw new RuntimeException("GLEngine: Attempt to release a texture unit that is already released: " + textureUnit);
    }
    releasedTextureUnits.add(textureUnit);
    return textureUnit;
  }

  // Add a released (ref count 0) texture unit number to the list of available
  // texture units
  private void recycleTextureUnit(CachedTexture t) {
    if (!releasedTextureUnits.contains(t.textureUnit)) {
      releasedTextureUnits.add(t.textureUnit);
    }
  }

  /**
   * Load a static texture from a file and bind it to the next available texture unit
   * Returns the texture unit number. If the texture is already loaded, just increment
   * the ref count and return the existing texture unit number.
   */
  public int useTexture(GL4 gl4, String textureName) {
    CachedTexture t;

    try {
      // if the texture is already loaded, just increment the ref count
      int textureHash = stringToHash(textureName);
      if (textures.containsKey(textureHash)) {
        t = textures.get(textureHash);
        t.refCount++;
      } else {
        // otherwise, load the texture its file and bind it to the next available texture unit
        File file = new File(textureName);
        t = new CachedTexture();
        t.type = TextureType.STATIC;
        t.texture = TextureIO.newTexture(file, false);
        t.textureUnit = getNextTextureUnit();
        t.refCount = 1;
        textures.put(textureHash, t);

        gl4.glActiveTexture(GL_TEXTURE0 + t.textureUnit);
        gl4.glEnable(GL_TEXTURE_2D);
        t.texture.enable(gl4);
        t.texture.bind(gl4);
      }
      // return the texture unit number
      return t.textureUnit;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This function must be called when the model changes (when it is
   * edited, or when a project is loaded/unloaded) to delete
   * all existing view coordinate textures and remove them from the
   * textures map.
   */
  public void clearCoordinateTextures() {
    CachedTexture t;
    for (int k : textures.keySet()) {
      t = textures.get(k);
      if (t.type == TextureType.COORDINATE) {
        t.refCount = 0; // just making sure
        releaseTexture(k);
      }
    }
  }

  public void releaseTexture(String textureName) {
    releaseTexture(stringToHash(textureName));
  }

  public void releaseTexture(int textureHash) {
    CachedTexture t = textures.get(textureHash);
    if (t == null) {
      throw new RuntimeException("Attempt to release texture that was never used:");
    }
    t.refCount--;
    if (t.refCount <= 0) {
      t.texture.destroy(gl4);
      recycleTextureUnit(t);
      textures.remove(textureHash);
    }
  }
}
