package titanicsend.pattern.glengine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GLPreprocessorTest {

  static final String TEST_PRAGMAS =
      "#pragma name \"TechnoChurch\"\n"
          + "#pragma TEControl.XPOS.Range(0.07,-1.0,1.0)\n"
          + "#pragma TEControl.YPOS.Range(-0.03,-1.0,1.0)\n"
          + "#pragma TEControl.SPEED.Range(1.0,1.0,10.0)\n"
          + "#pragma TEControl.QUANTITY.Range(8.0,1.0,16.0)\n"
          + "#pragma TEControl.LEVELREACTIVITY.Range(0.0,0.0,1.0)\n"
          + "#pragma TEControl.FREQREACTIVITY.Range(0.0,0.0,1.0)\n"
          + "#pragma TEControl.WOW1.Range(0.0,0.0,1.0)\n"
          + "#pragma TEControl.WOW2.Range(0.0,0.0,1.0)";

  static final String TEST_IUNIFORMS =
      "#iUniform vec3 iColorRGB=vec3(.964,.144,.519)\n"
          + "#iUniform vec3 iColor2RGB=vec3(.226,.046,.636)\n"
          + "#iUniform vec2 iTranslate=vec2(0.,0.)\n"
          + "#iUniform float bassLevel=0.in{0.,1.}\n"
          + "#iUniform float trebleLevel=0.in{0.,1.}\n"
          + "#iUniform float volumeRatio=0.in{0.,1.}\n"
          + "\n"
          + "#iUniform float iSpin=0. in{-1.,1.}\n"
          + "#iUniform float iScale=1.in{.01,4.}\n"
          + "#iUniform float iRotationAngle=0.in{0.,3.14}\n"
          + "\n"
          + "#iUniform float iSpeed=1. in{1.,10.}\n"
          + "#iUniform float iQuantity=8. in{1., 16.}\n"
          + "#iUniform float levelReact=0. in {0., 1.}\n"
          + "#iUniform float frequencyReact=0. in {0., 1.}\n"
          + "#iUniform float iWow2=0.5 in {0., 1.}\n"
          + "#iUniform float iWow1=-.1 in{0.,1.}";

  @Test
  public void iUniformParsingTest() {
    GLPreprocessor p = new GLPreprocessor();

    List<ShaderConfiguration> parameters = new ArrayList<ShaderConfiguration>();

    p.parseIUniforms(TEST_IUNIFORMS, parameters);

    for (ShaderConfiguration c : parameters) {
      System.out.println(c);
    }

    //    assertEquals(parameters.get(0), null);
    //    Assertions.assertIterableEquals(parameters, new ArrayList<>());
  }

  @Test
  public void pragmaParsingTest() {
    GLPreprocessor p = new GLPreprocessor();

    List<ShaderConfiguration> parameters = new ArrayList<ShaderConfiguration>();

    p.parsePragmas(TEST_PRAGMAS, parameters);

    for (ShaderConfiguration c : parameters) {
      System.out.println(c);
    }

    assertEquals(parameters.get(0), null);
    Assertions.assertIterableEquals(parameters, new ArrayList<>());
  }
}
