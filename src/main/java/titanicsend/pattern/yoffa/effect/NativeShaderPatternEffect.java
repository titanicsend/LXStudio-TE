package titanicsend.pattern.yoffa.effect;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import titanicsend.pattern.glengine.ShaderConfiguration;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.*;

@Deprecated // use GLShaderPattern instead
public class NativeShaderPatternEffect extends PatternEffect {
  private final SplittableRandom random;
  protected OffscreenShaderRenderer renderer;
  private FragmentShader fragmentShader;
  private final List<LXParameter> parameters;
  private final PatternControlData controlData;

  /** Creates new native shader effect */
  public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
    super(target);
    random = new SplittableRandom();

    this.controlData = new PatternControlData(pattern);

    if (fragmentShader != null) {
      this.fragmentShader = fragmentShader;
      this.renderer = new OffscreenShaderRenderer(fragmentShader);
      this.parameters = fragmentShader.getParameters();

    } else {
      this.parameters = null;
    }
  }

  /**
   * Creates new native shader effect with additional texture support
   *
   * @param shaderFilename shader to use
   * @param target render target
   */
  public NativeShaderPatternEffect(
      String shaderFilename, PatternTarget target, String... textureFilenames) {
    this(
        new FragmentShader(
            new File("resources/shaders/" + shaderFilename),
            Arrays.stream(textureFilenames)
                .map(x -> new File("resources/shaders/textures/" + x))
                .collect(Collectors.toList())),
        target);
  }

  @Override
  public void onPatternActive() {
    if (fragmentShader != null) {
      if (renderer == null) {
        renderer = new OffscreenShaderRenderer(fragmentShader);
      }
    }
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param xSize x resolution of image
   * @param ySize y resolution of image
   */
  public void paint(List<LXPoint> points, ByteBuffer image, int xSize, int ySize) {
    double k = 0;
    int xMax = xSize - 1;
    int yMax = ySize - 1;
    int[] colors = pattern.getColors();

    for (LXPoint point : points) {
      float zn = (1f - point.zn);
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = Math.round(zn * xMax);
      int yi = Math.round(yn * yMax);

      int index = 4 * ((yi * xSize) + xi);

      colors[point.index] = image.getInt(index);
    }
  }

  @Override
  public void run(double deltaMs) {
    if (renderer == null) {
      return;
    }
    ByteBuffer image = renderer.getFrame(controlData);
    paint(
        getPoints(),
        image,
        OffscreenShaderRenderer.getXResolution(),
        OffscreenShaderRenderer.getYResolution());
  }

  public List<ShaderConfiguration> getShaderConfig() {
    return fragmentShader.getShaderConfig();
  }

  // Saves me from having to propagate all those setUniform(name,etc.) methods up the
  // object hierarchy!  It grants great power.  Use responsibly!
  public NativeShader getNativeShader() {
    return renderer.getNativeShader();
  }

  @Override
  public List<LXParameter> getParameters() {
    return parameters;
  }
}
