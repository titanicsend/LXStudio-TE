package titanicsend.pattern.glengine;

import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.util.List;

/** Paints a 2D texture onto the car, mirroring the texture symmetrically on both sides. */
public class ShaderPaint2d extends ShaderPainterClass {
  int xMax = GLEngine.getWidth() - 1;
  int yMax = GLEngine.getHeight() - 1;

  public ShaderPaint2d(boolean isStaticModel) {
    super(isStaticModel);
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param colors array to hold colors, one for each point
   */
  public void mapToPoints(List<LXPoint> points, ByteBuffer image, int[] colors) {
    // Added "twist" feature - allows controlled swap of x and z axes.  This must
    // work on both static and dynamic models for now, which makes the logic a little
    // odd to look at. So...
    // TODO - simplify twist logic when we finish move to dynamic model
    if ((isTwisted && !isStatic) || isStatic && !isTwisted) {
      mapToPointsZYX(points, image, colors);
      return;
    }

    for (LXPoint point : points) {
      float xn = 1f - point.xn;
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = (int) (0.5 + xn * xMax);
      int yi = (int) (0.5 + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      colors[point.index] = image.getInt(index);
    }
  }

  /**
   * Map current LX point colors to a texture buffer that can be used by a shader.
   *
   * @param points list of points to paint
   * @param image buffer for bitmap
   * @param colors array of colors, one for each point
   */
  public void mapToBuffer(List<LXPoint> points, ByteBuffer image, int[] colors) {
    // TODO - simplify twist logic when we finish move to dynamic model
    if ((isTwisted && !isStatic) || isStatic && !isTwisted) {
      mapToBufferZYX(points, image, colors);
      return;
    }

    for (LXPoint point : points) {
      float xn = 1f - point.xn;
      float yn = point.yn;

      int xi = (int) (0.5 + xn * xMax);
      int yi = (int) (0.5 + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      image.putInt(index, colors[point.index]);
    }
  }

  /**
   * Static (old) model variant - remove when we move to dynamic model
   *
   * <p>Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param colors array to hold colors, one for each point
   */
  private void mapToPointsZYX(List<LXPoint> points, ByteBuffer image, int[] colors) {
    for (LXPoint point : points) {
      float zn = 1f - point.zn;
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = (int) (0.5 + zn * xMax);
      int yi = (int) (0.5 + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      colors[point.index] = image.getInt(index);
    }
  }

  /**
   * Static (old) model variant - remove when we move to dynamic model
   *
   * <p>Map current LX point colors to a texture buffer that can be used by a shader.
   *
   * @param points list of points to paint
   * @param image buffer for bitmap
   * @param colors array of colors, one for each point
   */
  private void mapToBufferZYX(List<LXPoint> points, ByteBuffer image, int[] colors) {
    // TODO - do we need to zero the buffer when we do this?
    // TODO - not if we stick to strictly to the points we're painting,
    // TODO - but maybe if we're doing something that changes coordinates.
    for (LXPoint point : points) {
      float zn = 1f - point.zn;
      float yn = point.yn;

      int xi = (int) (0.5 + zn * xMax);
      int yi = (int) (0.5 + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      image.putInt(index, colors[point.index]);
    }
  }
}
