package titanicsend.pattern.glengine;

import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Works with textures wrapped around the entire car. Looks best if the shader is careful about its
 * coordinate wrapping behavior, so it is recommended to use this class only with shaders that are
 * designed to work this way.
 */
public class ShaderPaint3d extends ShaderPainterClass {

  public ShaderPaint3d(boolean isStaticModel) {
    super(isStaticModel);
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points. This version wraps a texture around the entire car instead of
   * the default symmetrical mirroring.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
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
      float xn = 0.5f * ((point.z >= 0) ? 1f + point.xn : 1f - point.xn);
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = (int) (0.5f + xn * xMax);
      int yi = (int) (0.5f + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      colors[point.index] = image.getInt(index);
    }
  }

  public void mapToBuffer(List<LXPoint> points, ByteBuffer image, int[] colors) {
    // TODO - simplify twist logic when we finish move to dynamic model
    if ((isTwisted && !isStatic) || isStatic && !isTwisted) {
      mapToBufferZYX(points, image, colors);
      return;
    }

    for (LXPoint point : points) {
      float xn = 0.5f * ((point.z >= 0) ? 1f + point.xn : 1f - point.xn);
      float yn = point.yn;

      int xi = (int) (0.5f + xn * xMax);
      int yi = (int) (0.5f + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      image.putInt(index, colors[point.index]);
    }
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points. This version wraps a texture around the entire car instead of
   * the default symmetrical mirroring.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   */
  private void mapToPointsZYX(List<LXPoint> points, ByteBuffer image, int[] colors) {

    for (LXPoint point : points) {
      float zn = 0.5f * ((point.x >= 0) ? 1f + point.zn : 1f - point.zn);
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = (int) (0.5f + zn * xMax);
      int yi = (int) (0.5f + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      colors[point.index] = image.getInt(index);
    }
  }

  private void mapToBufferZYX(List<LXPoint> points, ByteBuffer image, int[] colors) {

    for (LXPoint point : points) {
      float zn = 0.5f * ((point.x >= 0) ? 1f + point.zn : 1f - point.zn);
      float yn = point.yn;

      int xi = (int) (0.5f + zn * xMax);
      int yi = (int) (0.5f + yn * yMax);

      int index = 4 * ((yi * GLEngine.getWidth()) + xi);
      image.putInt(index, colors[point.index]);
    }
  }
}
