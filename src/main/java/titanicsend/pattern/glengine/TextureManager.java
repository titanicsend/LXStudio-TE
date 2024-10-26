package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import static com.jogamp.opengl.GL.*;

public class TextureManager {

  private GL4 gl4;

  // texture unit management
  private class TextureUnit {
    private Texture texture;
    private int textureUnit;
    private int refCount;
  }

  private int nextTextureUnit = 3;

  // textures is a map of integer hash codes for textures to texture unit numbers.
  // The hash code is used to identify the texture, and the texture unit number is
  // used to bind the texture to the GPU.
  private final HashMap<Integer, TextureUnit> textures = new HashMap<>();

  // a list of that we can use to keep track of released texture unit numbers
  private final ArrayList<Integer> releasedTextureUnits = new ArrayList<>();

  // Almost certainly reliable hashing function!
  // This generates a hash code for a texture file name string, which is
  // then used to look up the texture unit number in our static textures map.
  // It is, as mentioned, almost certainly reliable for our use case, but
  // not absolutely guaranteed to generate a unique code for every
  // possible filename.
  private int stringToHash(String s) {
    int hash = 0;
    for (int i = 0; i < s.length(); i++) {
      hash = (hash << 5) - hash + s.charAt(i);
    }
    return hash;
  }

  public static Texture createCoordinateTexture(GL4 gl4, FloatBuffer coords, int width, int height) {
    // Create a TextureData object for a set of normalized floating point coordinates
    TextureData textureData = new TextureData(
      gl4.getGLProfile(),
      GL4.GL_RGB32F,
      width,
      height,
      0,
      GL4.GL_RGB,
      GL4.GL_FLOAT,
      false,
      false,
      false,
      coords,
      null
    );

    // Create a Texture object from the TextureData
    Texture texture = TextureIO.newTexture(textureData);

    // Bind the texture to the GL context
    texture.bind(gl4);

    // TODO - set texture parameters and do all the other things that useTexture does
    // TODO - for file-based textures
    // TODO - or keep this as a separate function and call it from a useTexture variant
    // TODO - that takes an LX model (a view) instead of a filename.
    // TODO - once that's done, convert all texture management in the shader to this system.
    // TODO - it'll be faster, simpler and more reliable
    // TODO - DEFINITELY remember to unload/free/delete the texture when done with it

    return texture;
  }

  // Returns the next available texture unit number, either by reusing a released
  // texture unit number or by allocating a new one.
  public int getNextTextureUnit() {
    if (releasedTextureUnits.size() > 0) {
      return releasedTextureUnits.remove(0);
    } else {
      return nextTextureUnit++;
    }
  }

  public int releaseTextureUnit(int textureUnit) {
    if (textureUnit < 3) {
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
  private void recycleTextureUnit(TextureUnit t) {
    if (!releasedTextureUnits.contains(t.textureUnit)) {
      releasedTextureUnits.add(t.textureUnit);
    }
  }

  public int useTexture(GL4 gl4, String textureName) {
    TextureUnit t = new TextureUnit();

    try {
      // if the texture is already loaded, just increment the ref count
      int textureHash = stringToHash(textureName);
      if (textures.containsKey(textureHash)) {
        t = textures.get(textureHash);
        t.refCount++;
      } else {
        // otherwise, load the texture its file and bind it to the next available texture unit
        File file = new File(textureName);
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

  public void releaseTexture(String textureName) {
    releaseTexture(stringToHash(textureName));
  }

  public void releaseTexture(int textureHash) {
    TextureUnit t = textures.get(textureHash);
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
