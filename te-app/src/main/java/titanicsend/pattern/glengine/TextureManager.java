package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Manages the lifecycle of the (relatively) static OpenGL textures used by
// the shader engine.  We use are two types of textures: coordinate and static.
// Coordinate textures hold floating point normalized XYZ coordinates of the
// points in the various views of the model.  Static textures are loaded from
// image files, and are used as 2D textures in the shaders.
//
public class TextureManager {

  // texture types we manage
  private enum CachedTextureType {
    COORDINATE,
    STATIC
  }

  // per texture management data for the texture cache
  private class CachedTextureInfo {
    protected Texture texture;
    protected int textureUnit;
    protected int[] textureHandle = new int[1];
    protected CachedTextureType type;
    protected int refCount;
  }

  private final GL4 gl4;

  // the next available OpenGL texture unit number
  private static final int FIRST_UNRESERVED_TEXTURE_UNIT = 3;
  private int nextTextureUnit = FIRST_UNRESERVED_TEXTURE_UNIT;

  // map of texture hash codes to texture data
  // The hash code uniquely identifies the texture, the texture unit number is
  // used to bind the texture to the GPU, and the other data is used for
  // cache management.
  private final Map<Integer, CachedTextureInfo> textures = new HashMap<>();

  // a list of that we can use to keep track of released texture unit numbers
  private final ArrayList<Integer> releasedTextureUnits = new ArrayList<>();

  public TextureManager(GL4 gl4) {
    this.gl4 = gl4;
  }

  /**
   * Almost certainly reliable hashing function! This generates a hash code for a texture file name
   * string, which is then used to look up the texture unit number in our static textures map. It
   * is, as mentioned, almost certainly reliable for our use case, but not absolutely guaranteed to
   * generate a unique code for every possible filename.
   */
  private int stringToHash(String s) {
    int hash = 0;
    for (int i = 0; i < s.length(); i++) {
      hash = (hash << 5) - hash + s.charAt(i);
    }
    return hash;
  }

  /**
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be called
   * by the parent pattern or effect at least once before the first frame is rendered and Should be
   * called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture unit number that the view's coordinate texture is bound to.
   */
  public int useViewCoordinates(LXModel model) {
    CachedTextureInfo t;

    // if the view's coordinate texture is already loaded, just use it
    // NOTE: We don't use the cache's texture reference counting mechanism
    // for the coordinate texture. Once a view coord texture is loaded, it's never
    // released until the model changes.
    int textureHash = model.hashCode();
    if (textures.containsKey(textureHash)) {
      t = textures.get(textureHash);
    } else {
      // otherwise, create a new coordinate texture and bind it to the next available texture unit
      t = createCoordinateTexture(model);
      textures.put(textureHash, t);
    }
    // return the texture's information block
    return t.textureUnit;
  }

  /**
   * Create a coordinate texture from the normalized coordinates of the given model and bind it to
   * the next available texture unit. If the view's coordinate texture already exists,return the
   * bound texture unit number.
   */
  public CachedTextureInfo createCoordinateTexture(LXModel model) {
    // create new cached texture management object
    CachedTextureInfo t = new CachedTextureInfo();
    t.type = CachedTextureType.COORDINATE;
    t.textureUnit = getNextTextureUnit();
    t.refCount = 1;

    // get data we need to create the texture from the system and
    // the model
    int xSize = GLEngine.current.getWidth();
    int ySize = GLEngine.current.getHeight();
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
        // fill unused points with NaN so we can stop
        // computation in the shader early when possible.
        coords.put(Float.NaN);
        coords.put(Float.NaN);
        coords.put(Float.NaN);
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

    // Create an OpenGL texture to hold the coordinate data
    gl4.glActiveTexture(GL_TEXTURE0 + t.textureUnit);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, t.textureHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, t.textureHandle[0]);

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the coordinate data into the texture, where it will stay 'till
    // the model changes or Chromatik is stopped.
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, xSize, ySize, 0, GL4.GL_RGB, GL_FLOAT, coords);

    // done!  return the cache management object
    return t;
  }

  /**
   * Returns the next available texture unit number, either by reusing a released texture unit
   * number or by allocating a new one.
   */
  public int getNextTextureUnit() {
    if (releasedTextureUnits.size() > 0) {
      return releasedTextureUnits.remove(0);
    } else {
      return nextTextureUnit++;
    }
  }

  /** Return a texture unit number to the pool of available texture units. */
  public int releaseTextureUnit(int textureUnit) {
    if (textureUnit < FIRST_UNRESERVED_TEXTURE_UNIT) {
      throw new RuntimeException(
          "GLEngine: Attempt to release a reserved texture unit: " + textureUnit);
    }
    if (releasedTextureUnits.contains(textureUnit)) {
      throw new RuntimeException(
          "GLEngine: Attempt to release a texture unit that is already released: " + textureUnit);
    }
    releasedTextureUnits.add(textureUnit);
    return textureUnit;
  }

  /**
   * (internal only) Given a cached texture object with a ref count of zero, return its texture unit
   * number to the pool of available units.
   */
  private void recycleTextureUnit(CachedTextureInfo t) {
    if (!releasedTextureUnits.contains(t.textureUnit)) {
      releasedTextureUnits.add(t.textureUnit);
    }
  }

  /**
   * Load a static texture from a file and bind it to the next available texture unit Returns the
   * texture unit number. If the texture is already loaded, just increment the ref count and return
   * the existing texture unit number.
   */
  public int useTexture(GL4 gl4, String textureName) {
    CachedTextureInfo t;

    try {
      // if the texture is already loaded, just increment the ref count
      int textureHash = stringToHash(textureName);
      if (textures.containsKey(textureHash)) {
        t = textures.get(textureHash);
        t.refCount++;
      } else {
        // otherwise, load the texture its file and bind it to the next available texture unit
        File file = new File(textureName);
        t = new CachedTextureInfo();
        t.type = CachedTextureType.STATIC;
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
   * This function must be called when the model changes (when it is edited, or when a project is
   * loaded/unloaded) to delete all existing view coordinate textures and remove them from the
   * textures map.
   */
  public void clearCoordinateTextures() {
    ArrayList<Integer> keysToRemove = new ArrayList<>();

    for (Map.Entry<Integer, CachedTextureInfo> entry : textures.entrySet()) {
      CachedTextureInfo t = entry.getValue();

      // create a list of keys for all coordinate textures
      if (t.type == CachedTextureType.COORDINATE) {
        t.refCount = 0; // should already be zero, but just in case
        keysToRemove.add(entry.getKey());
      }
    }

    // remove all coordinate textures from the map
    for (Integer key : keysToRemove) {
      releaseTexture(key);
    }
  }

  /**
   * Release a cached texture. If the texture's ref count reaches 0, delete the texture, return it's
   * GL texture unit to the pool and remove it from the textures map.
   *
   * @param textureName - filename of the texture to release
   */
  public void releaseTexture(String textureName) {
    releaseTexture(stringToHash(textureName));
  }

  /**
   * Release a cached texture. If the texture's ref count reaches 0, delete the texture, return it's
   * GL texture unit to the pool and remove it from the textures map.
   *
   * @param textureHash - integer hash code of the texture to release
   */
  public void releaseTexture(int textureHash) {
    CachedTextureInfo t = textures.get(textureHash);
    if (t == null) {
      throw new RuntimeException("Attempt to release texture that was never used:");
    }
    t.refCount--;
    if (t.refCount <= 0) {
      switch (t.type) {
        case STATIC:
          t.texture.destroy(gl4);
          break;
        case COORDINATE:
          gl4.glDeleteTextures(1, t.textureHandle, 0);
          break;
      }
      recycleTextureUnit(t);
      textures.remove(textureHash);
    }
  }

}
