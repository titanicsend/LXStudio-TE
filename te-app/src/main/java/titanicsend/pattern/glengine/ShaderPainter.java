package titanicsend.pattern.glengine;

import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;

/** Tools for mapping textures onto the model. Most key methods are static for best performance. */
public class ShaderPainter {

  public static void mapToPointsDirect(LXPoint[] points, ByteBuffer image, int[] colors) {
    for (int i = 0; i < points.length; i++) {
      colors[points[i].index] = image.getInt(i * 4);
    }
  }

  public static void mapToBufferDirect(LXPoint[] points, ByteBuffer image, int[] colors) {
    for (int i = 0; i < points.length; i++) {
      image.putInt(points[i].index * 4, colors[i]);
    }
  }

  public static void mapFromLinearBuffer(
      LXPoint[] points, int xSize, int ySize, ByteBuffer dest, int[] colors) {
    float xm = (float) xSize - 1;
    float ym = (float) ySize - 1;

    for (int i = 0; i < points.length; i++) {
      // this is a full 3D mapping.  It splits the model into two 'sides' at the z origin
      // and mirrors the "far" half to the right side of the texture so we can address points
      // with identical x and y, but different z coordinates.  Note that correct 3D unmapping
      // requires this process to be duplicated in the shader.
      float xn = 0.5f * ((points[i].z < 0.5f) ? points[i].xn : 1 + (1 - points[i].xn));
      float yn = points[i].yn;

      int xi = (int) (xn * xm);
      int yi = (int) (yn * ym);

      int index = 4 * ((yi * xSize) + xi);
      dest.putInt(index, colors[i]);
    }
  }

  public static void setTwist(boolean twist) {
    // New model doesn't support the old "twist" axis swap
    // TODO - build a new, improved axis swapper for twist mode.
  }
}
