package titanicsend.pattern.glengine.model;

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

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLEngine;

public class ModelCoordsTexture extends ModelTexture {

  public ModelCoordsTexture(LX lx, GLEngine glEngine) {
    super(lx, glEngine);
  }

  @Override
  protected void refreshTextureContents(int handle, int enginePoints, LXModel model) {

    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();

    // Create a FloatBuffer to hold the normalized coordinates of the model points
    FloatBuffer coords = GLBuffers.newDirectFloatBuffer(enginePoints * 3);

    // Initialize with NaNs
    coords.rewind();
    for (int i = 0; i < enginePoints; i++) {
      coords.put(Float.NaN);
      coords.put(Float.NaN);
      coords.put(Float.NaN);
    }
    // Insert normalized coordinates and their indices into the buffers
    for (LXPoint p : model.points) {
      int destIndex = p.index * 3;
      coords.put(destIndex, p.xn);
      coords.put(destIndex + 1, p.yn);
      coords.put(destIndex + 2, p.zn);
    }

    coords.rewind();

    // Create an OpenGL texture to hold the coordinate data
    // TODO: Is it ok to use the same texture unit for all ModelTexture creation? Just can't use 0.
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_MODEL_COORDS, handle);

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // load the coordinate data into the texture
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, width, height, 0, GL4.GL_RGB, GL_FLOAT, coords);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);
    gl4.glActiveTexture(GL_TEXTURE0);
  }
}
