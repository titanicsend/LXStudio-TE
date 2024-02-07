package titanicsend.pattern.glengine;

import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Interface for painting a 2D texture onto the car.  The default implementation
 * mirrors the texture symmetrically on both sides.
 */
public interface ShaderPaintFn {
  int xMax = GLEngine.getWidth() - 1;
  int yMax = GLEngine.getHeight() - 1;
  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param colors array to hold colors, one for each point
   */
  default void mapToPoints(List<LXPoint> points, ByteBuffer image, int[] colors) {
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
   * Map current LX point colors to a texture buffer
   * @param points list of points to paint
   * @param image backbuffer for map
   * @param colors array of colors, one for each point
   */
  default void mapToBuffer(List<LXPoint> points, ByteBuffer image, int[] colors) {
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
