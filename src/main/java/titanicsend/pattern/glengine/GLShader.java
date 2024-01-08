package titanicsend.pattern.glengine;

import Jama.Matrix;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.texture.Texture;
import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.PatternControlData;
import titanicsend.util.TE;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

// TODO: for tomorrow, we want to move all the GLProgram stuff into this class
// (out of NativeShader) so this class can manage program switching and buffer
// allocation/deallocation.
// We also need to make certain that on project unload, all the programs get destroyed
// and all associated buffers freed. This may require moving program management to the
// GLEngine class, which may be the only class that knows when a project is being unloaded.
// TODO - research this to see if there's an unload notification of any kind in lx.
// TODO - how to handle buffers for multiple shaders?  Should the shader program own the FBO,
// or should the shader effect own it?  Which presents the most logical API?

public class GLShader {
  private GLEngine glEngine = null;
  private FragmentShader fragmentShader;
  private NativeShader nativeShader;
  private List<LXParameter> parameters;
  private GLAutoDrawable canvas = null;
  private TEPerformancePattern pattern;
  private PatternControlData controlData;

  /** Creates new native shader effect */
  public GLShader(LX lx, FragmentShader fragmentShader, TEPerformancePattern pattern) {
    this.pattern = pattern;
    this.controlData = new PatternControlData(pattern);

    if (glEngine == null) {
      this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
      TE.log("Shader: Retrieved GLEngine object from LX");
    }

    if (fragmentShader != null) {
      this.fragmentShader = fragmentShader;
      this.canvas = glEngine.getCanvas();
      this.parameters = fragmentShader.getParameters();

      nativeShader = new NativeShader(fragmentShader, glEngine.getWidth(), glEngine.getHeight());
    } else {
      this.parameters = null;
    }
  }

  /**
   * Creates new native shader effect with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param pattern Pattern associated w/this shader
   */
  public GLShader(
      LX lx, String shaderFilename, TEPerformancePattern pattern, String... textureFilenames) {
    this(
        lx,
        new FragmentShader(
            new File("resources/shaders/" + shaderFilename),
            Arrays.stream(textureFilenames)
                .map(x -> new File("resources/shaders/textures/" + x))
                .collect(Collectors.toList())),
        pattern);

  }

  public void onActive() {
      System.out.println("GLShader.onActive");
      nativeShader.init(canvas);
  }

  public void onInactive() {
      System.out.println("GLShader.onInactive");
  }

  public ByteBuffer getFrame(PatternControlData ctlInfo) {
    nativeShader.updateControlInfo(ctlInfo);
    nativeShader.display(canvas);

    return nativeShader.getSnapshot();
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

  public void run(double deltaMs) {
    if (canvas == null) {
      return;
    }
    ByteBuffer image = getFrame(controlData);
    paint(this.pattern.getModel().getPoints(), image, glEngine.getWidth(), glEngine.getHeight());
  }

  public List<ShaderConfiguration> getShaderConfig() {
    return fragmentShader.getShaderConfig();
  }

  public List<LXParameter> getParameters() {
    return parameters;
  }

  // setters for shader uniforms of all supported uniform types
  /** setter -- single int */
  public void setUniform(String name, int x) {
    nativeShader.setUniform(name, x);
  }

  /** 2 element int array or ivec2 */
  public void setUniform(String name, int x, int y) {
    nativeShader.setUniform(name, x, y);
  }

  /** 3 element int array or ivec3 */
  public void setUniform(String name, int x, int y, int z) {
    nativeShader.setUniform(name, x, y, z);
  }

  /** 4 element int array or ivec4 */
  public void setUniform(String name, int x, int y, int z, int w) {
    nativeShader.setUniform(name, x, y, z, w);
  }

  /** single float */
  public void setUniform(String name, float x) {
    nativeShader.setUniform(name, x);
  }

  /** 2 element float array or vec2 */
  public void setUniform(String name, float x, float y) {
    nativeShader.setUniform(name, x, y);
  }

  /** 3 element float array or vec3 */
  public void setUniform(String name, float x, float y, float z) {
    nativeShader.setUniform(name, x, y, z);
  }

  /** 4 element float array or vec4 */
  public void setUniform(String name, float x, float y, float z, float w) {
    nativeShader.setUniform(name, x, y, z, w);
  }

  /** single boolean */
  public void setUniform(String name, boolean x) {
    nativeShader.setUniform(name, x);
  }

  /** 2 element boolean array */
  public void setUniform(String name, boolean x, boolean y) {
    nativeShader.setUniform(name, x, y);
  }

  /**
   * SAMPLER2D uniform from jogl Texture object - this prototype supports both dynamic and
   * static textures. If you know your texture will be changed on every frame, use setUniform(String
   * name, Texture tex) instead. TODO - STATIC TEXTURES NOT YET IMPLEMENTED
   */
  public void setUniform(String name, Texture tex, boolean isStatic) {
    nativeShader.setUniform(name, tex, isStatic);
  }

  /**
   * SAMPLER2D uniform from jogl Texture object
   */
  public void setUniform(String name, Texture tex) {
    nativeShader.setUniform(name, tex);
  }

  /**
   * Integer array uniform, of size 2, 3 or 4
   * @param columns number of coordinates per element, max 4
   */
  public void setUniform(String name, IntBuffer vec, int columns) {
    nativeShader.setUniform(name, vec, columns);
  }

  public void setUniform(String name, FloatBuffer vec, int columns) {
    nativeShader.setUniform(name, vec, columns);
  }
  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4).
   * Input matrix must be in row major order.
   */
  public void setUniform(String name, float[][] matrix) {
    nativeShader.setUniform(name, matrix);
  }

  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4)
   */
  public void setUniform(String name, Matrix matrix) {
    nativeShader.setUniform(name, matrix);
  }
}
