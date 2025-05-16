package titanicsend.pattern.glengine;

import static titanicsend.pattern.glengine.GLPreprocessorHelpers.*;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.pattern.yoffa.shader_engine.UniformNames;

public class GLPreprocessor {

  // used in #include processing to prevent infinite recursion
  // and to track line numbering for relevant error messages.
  private static final int MAX_INCLUDE_DEPTH = 10;
  private boolean foundInclude = false;
  private int lineCount = 0;
  private boolean isDriftModeShader = false;

  /**
   * Preprocess the shader, expanding #includes and handling our TE-specific control and texture
   * configuration #pragmas. Note that this preprocessor doesn't support the old method of adding
   * extra controls. TODO - should we add this as an option?
   */
  public String preprocessShader(File shaderFile, List<ShaderConfiguration> parameters)
      throws Exception {
    String shaderBody = ShaderUtils.loadResource(shaderFile);
    return preprocessShader(shaderBody, parameters);
  }

  public String preprocessShader(String shaderBody, List<ShaderConfiguration> parameters)
      throws Exception {
    lineCount = 0;
    try {
      int depth = 0;
      while (true) {
        if (depth >= MAX_INCLUDE_DEPTH) {
          throw new RuntimeException("Exceeded maximum #include depth of " + MAX_INCLUDE_DEPTH);
        }
        shaderBody = expandIncludes(shaderBody);
        depth++;
        if (!this.foundInclude) {
          break;
        }
      }
      shaderBody = legacyPreprocessor(shaderBody, parameters);

      // VSCode Shadertoy extension support - parse '#iUniform' syntax as an alternate
      // to '#pragma TEControl.NAME RANGE' syntax.
      List<ShaderConfiguration> configsFromIUniforms = parseIUniforms(shaderBody);
      parameters.addAll(configsFromIUniforms);
      // After extracting those, remove all lines beginning with #iUniform.
      shaderBody = removeIUniformLines(shaderBody);

      // Extract configs specified using #pragma
      List<ShaderConfiguration> configsFromPragmas = parsePragmas(shaderBody);
      for (ShaderConfiguration control : configsFromPragmas) {
        // If there was a pragma indicating DRIFT, we need to maintain some state so
        // we can pass it to the template shader.
        if (control.opcode == ShaderConfigOpcode.SET_TRANSLATE_MODE_DRIFT) {
          isDriftModeShader = true;
          break;
        }
      }
      parameters.addAll(configsFromPragmas);
    } catch (Exception e) {
      throw new Exception("Shader Preprocessor Error. " + e.getMessage());
    }

    // in drift mode shaders, x/y translate controls set movement direction and speed rather than
    // absolute offset.  Define a constant to tell the shader framework what we want.
    if (isDriftModeShader) {
      shaderBody = "#define TE_NOTRANSLATE\n" + shaderBody;
    }

    // combine the fragment shader code with the framework template
    shaderBody =
        getFragmentShaderTemplate().replace(ShaderUtils.SHADER_BODY_PLACEHOLDER, shaderBody);
    return shaderBody;
  }

  // Expand #include statements in the shader code.  Handles nested includes
  // up to MAX_INCLUDE_DEPTH (defaults to 10 levels.)
  public String expandIncludes(String input) throws IOException {
    StringBuilder output = new StringBuilder();
    BufferedReader reader = new BufferedReader(new StringReader(input));
    String line;
    this.foundInclude = false;
    while ((line = reader.readLine()) != null) {
      lineCount++;
      if (line.startsWith("#include")) {
        foundInclude = true;
        try {
          String filename = getFileName(line.substring("#include ".length()));

          BufferedReader fileReader = new BufferedReader(new FileReader(filename));
          String fileLine;

          // restart line counter for include file
          output.append("#line 1 \n");
          while ((fileLine = fileReader.readLine()) != null) {
            output.append(fileLine).append("\n");
          }
          fileReader.close();
        } catch (Exception e) {
          throw new IOException("Line " + lineCount + " : " + line + "\n" + e.getMessage());
        }

        // reset line counter to main file count
        output.append("#line ").append(lineCount + 1).append("\n");
      } else {
        output.append(line).append("\n");
      }
    }
    reader.close();
    return output.toString();
  }

  // Parse #pragma statements in the shader code, subdividing them into shader
  // configuration parameters and texture definitions.  Note that GLSL requires
  // us to ignore any #pragma statements that we don't recognize.
  public List<ShaderConfiguration> parsePragmas(String input) {
    List<ShaderConfiguration> parameters = new ArrayList<>();
    Pattern pattern = Pattern.compile("^\\s*#pragma.*", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(input);

    while (matcher.find()) {
      try {
        // tokenize the line, dividing first by whitespace and parentheses
        // then by commas
        String[] tokens = matcher.group().split("\\s|\\(|\\)");
        // discard empty tokens
        tokens = Arrays.stream(tokens).filter(s -> !s.isEmpty()).toArray(String[]::new);
        // discard the #pragma token
        tokens = Arrays.copyOfRange(tokens, 1, tokens.length);

        String pragma = tokens[0].toLowerCase();
        if (pragma.startsWith("tecontrol.")) {
          // Common controls configuration
          ShaderConfiguration control = parseControl(tokens);
          parameters.add(control);
        } else if (pragma.startsWith("ichannel")) {
          // Texture channel definition
          ShaderConfiguration control = parseTextures(tokens);
          parameters.add(control);
        } else if (pragma.equals("name")) {
          // name of class/pattern in UI
          ShaderConfiguration control = parseClassName(tokens);
          parameters.add(control);
        } else if (pragma.equals("lxcategory")) {
          // set LXCategory for pattern
          ShaderConfiguration control = parseLXCategory(tokens);
          parameters.add(control);
        } else if (pragma.equals("auto")) {
          // auto keyword forces use of automatic class generation system
          ShaderConfiguration p = new ShaderConfiguration();
          p.opcode = ShaderConfigOpcode.AUTO;
          parameters.add(p);
        }
      } catch (Exception e) {
        throw new RuntimeException("Error in " + matcher.group() + "\n" + e.getMessage());
      }
    }
    return parameters;
  }

  public static String getFragmentShaderTemplate() {
    return ShaderUtils.loadResource(ShaderUtils.FRAMEWORK_PATH + "template.fs");
  }

  public void addLXParameter(List<ShaderConfiguration> parameters, LXParameter p) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.ADD_LX_PARAMETER;
    control.lxParameter = p;
    parameters.add(control);
  }

  /**
   * Convert legacy (pre-TECommonControls) embedded control specifiers to uniforms, and create
   * corresponding LX parameters for them.
   */
  public String legacyPreprocessor(String shaderBody, List<ShaderConfiguration> parameters) {
    Matcher matcher = ShaderUtils.PLACEHOLDER_FINDER.matcher(shaderBody);

    StringBuilder shaderCode = new StringBuilder(shaderBody.length());
    StringBuilder finalShader = new StringBuilder(shaderBody.length() + 512);
    while (matcher.find()) {
      try {
        String placeholderName = matcher.group(1);
        if (matcher.groupCount() >= 3) {
          String metadata = matcher.group(3);
          if ("bool".equals(metadata)) {
            finalShader
                .append("uniform bool ")
                .append(placeholderName)
                .append(UniformNames.LX_PARAMETER_SUFFIX)
                .append(";\n");
            addLXParameter(parameters, new BooleanParameter(placeholderName));
          } else {
            finalShader
                .append("uniform float ")
                .append(placeholderName)
                .append(UniformNames.LX_PARAMETER_SUFFIX)
                .append(";\n");
            Double[] rangeValues =
                Arrays.stream(metadata.split(",")).map(Double::parseDouble).toArray(Double[]::new);
            addLXParameter(
                parameters,
                new CompoundParameter(
                    placeholderName, rangeValues[0], rangeValues[1], rangeValues[2]));
          }
        }
        matcher.appendReplacement(shaderCode, placeholderName + UniformNames.LX_PARAMETER_SUFFIX);
      } catch (Exception e) {
        throw new RuntimeException("Problem parsing placeholder: " + matcher.group(0), e);
      }
    }
    matcher.appendTail(shaderCode);
    finalShader.append(shaderCode);

    return finalShader.toString();
  }
}
