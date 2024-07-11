package titanicsend.pattern.glengine;

import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Abstract class for mapping a 2D texture onto the car.
 */
public abstract class ShaderPainterClass {
  protected int xMax = GLEngine.getWidth() - 1;
  protected int yMax = GLEngine.getHeight() - 1;

  // for use during static-to-dynamic model transition
  // TODO - remove isStatic when we move to dynamic model
  public boolean isStatic;
  protected boolean isTwisted = false;

  public ShaderPainterClass(boolean isStaticModel) {
    this.isStatic = isStaticModel;
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param colors array to hold colors, one for each point
   */
  public abstract void mapToPoints(List<LXPoint> points, ByteBuffer image, int[] colors);

  /**
   * Map current LX point colors to a texture buffer that can be used by a shader.
   *
   * @param points list of points to paint
   * @param image buffer for bitmap
   * @param colors array of colors, one for each point
   */
  public abstract void mapToBuffer(List<LXPoint> points, ByteBuffer image, int[] colors);

  public void setTwist(boolean twist) {
    this.isTwisted = twist;
  }
}
