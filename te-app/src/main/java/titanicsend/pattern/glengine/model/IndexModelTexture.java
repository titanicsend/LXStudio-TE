package titanicsend.pattern.glengine.model;

import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_MODEL_INDEX;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLEngine;

public class IndexModelTexture extends ModelTextureType {

  public IndexModelTexture(GLEngine glEngine, GL4 gl4) {
    super(glEngine, gl4);
  }

  @Override
  protected void refreshTextureContents(int handle, int enginePoints, LXModel model) {

    int width = this.glEngine.getWidth();
    int height = this.glEngine.getHeight();

    // Create a FloatBuffer to hold the indices of the model points
    FloatBuffer indices = GLBuffers.newDirectFloatBuffer(enginePoints * 2);

    // Initialize with NaNs
    indices.rewind();
    for (int i = 0; i < enginePoints; i++) {
      indices.put(Float.NaN);
      indices.put(Float.NaN);
    }
    // Insert normalized coordinates and their indices into the buffers
    for (LXPoint p : model.points) {
      // save normalized coordinates to a rectangular texture index
      // using the model's width and height. We actually write to
      // a rectangular neighborhood around the target pixel to compensate
      // for rounding errors in sampling, and the plain old non-contiguous
      // nature of the model points.
      setIndexNeighborhood(p, indices, width, height);
    }

    indices.rewind();

    // And create an OpenGL texture to hold the index data
    this.glEngine.bindTextureUnit(TEXTURE_UNIT_MODEL_INDEX, handle);

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
}
