package titanicsend.pattern.glengine;

import static org.junit.jupiter.api.Assertions.*;
import static titanicsend.pattern.glengine.GLPreprocessorHelpers.parseIUniforms;

import java.util.List;
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
    List<ShaderConfiguration> parameters = parseIUniforms(TEST_IUNIFORMS);

    String actualNames =
        String.join(", ", parameters.stream().map(p -> p.name).toArray(String[]::new));

    assertEquals("Spin, Size, Speed, Quantity, Wow2, Wow1", actualNames);
  }

  @Test
  public void pragmaParsingTest() {
    GLPreprocessor gl = new GLPreprocessor();

    List<ShaderConfiguration> parameters = gl.parsePragmas(TEST_PRAGMAS);

    String actualNames =
        String.join(", ", parameters.stream().map(p -> p.name).toArray(String[]::new));

    assertEquals(
        "TechnoChurch, xPos, yPos, Speed, Quantity, LvlReact, FreqReact, Wow1, Wow2", actualNames);
  }
}
