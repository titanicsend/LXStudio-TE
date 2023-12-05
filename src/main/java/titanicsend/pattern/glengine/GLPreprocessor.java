package titanicsend.pattern.glengine;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.pattern.yoffa.shader_engine.Uniforms;

public class GLPreprocessor {

  // used in #include processing to prevent infinite recursion
  // and to track line numbering for relevant error messages.
  private static final int MAX_INCLUDE_DEPTH = 10;
  private boolean foundInclude = false;
  private int lineCount = 0;
  private boolean isDriftModeShader = false;

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
          String filename = getFileName(line.substring("#include ".length(), line.length()));

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

  // Converts strings from control definition #pragmas to shader configuration
  // opcode values
  public static ShaderConfigOpcode opcodeFromString(String str) {
    return switch (str) {
      case "auto" -> ShaderConfigOpcode.AUTO;
      case "Value" -> ShaderConfigOpcode.SET_VALUE;
      case "Range" -> ShaderConfigOpcode.SET_RANGE;
      case "Label" -> ShaderConfigOpcode.SET_LABEL;
      case "Exponent" -> ShaderConfigOpcode.SET_EXPONENT;
      case "NormalizationCurve" -> ShaderConfigOpcode.SET_NORMALIZATION_CURVE;
      case "Disable" -> ShaderConfigOpcode.DISABLE;
      case "NORMAL" -> ShaderConfigOpcode.SET_TRANSLATE_MODE_NORMAL;
      case "DRIFT" -> ShaderConfigOpcode.SET_TRANSLATE_MODE_DRIFT;
      default -> throw new IllegalArgumentException("Unknown configuration operation: " + str);
    };
  }

  // Parse a control definition #pragma and add it to the list of shader
  // configuration parameters
  public void parseControl(String[] line, List<ShaderConfiguration> parameters) {
    ShaderConfiguration control = new ShaderConfiguration();
    // tokenize the first element, dividing by periods
    String[] tokens = line[0].split("\\.");

    // token[0] will always be "TEControl" to specify common control configuration
    //
    // token[1] will be the control name, which should correspond to a TEControlTag entry
    // or 'TranslateMode', which specifies how the pattern should handle x/y translation.
    if (tokens[1].equals("TranslateMode")) {
      // NOTE: This affects pattern subclassing, so must be handled separately
      // from (and prior to) "normal" control configurations during initialization.
      control.opcode = opcodeFromString(line[1]);
      isDriftModeShader = (control.opcode == ShaderConfigOpcode.SET_TRANSLATE_MODE_DRIFT);
    } else {
      control.parameterId = TEControlTag.valueOf(tokens[1].toUpperCase());
      control.name = control.parameterId.getLabel();
      // the third token is the operation to be performed on the specified control

      control.opcode = opcodeFromString(tokens[2]);

      // now we need to parse the rest of the tokens, which will depend on the operation
      switch (control.opcode) {
        case SET_VALUE, SET_EXPONENT -> control.value = Double.parseDouble(line[1]);
        case SET_RANGE -> {
          // tokenize line[1] by commas to get setRange() parameters
          String[] range = line[1].split(",");
          if (range.length != 3) {
            throw new IllegalArgumentException("Invalid range specification: " + line[1]);
          }
          control.value = Double.parseDouble(range[0]);
          control.v1 = Double.parseDouble(range[1]);
          control.v2 = Double.parseDouble(range[2]);
        }
        case SET_LABEL -> control.name = stringCleanup(line[1]);
        case SET_NORMALIZATION_CURVE -> control.normalizationCurve =
            BoundedParameter.NormalizationCurve.valueOf(line[1].toUpperCase());
      }
    }
    parameters.add(control);
  }

  // Parse a texture definition #pragma and add it to the list of shader
  // configuration parameters
  public void parseTextures(String[] line, List<ShaderConfiguration> parameters) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.SET_TEXTURE;

    // the last character of token 0 is the integer channel identifier
    // valid channels are 1-9.  Channel 0 is reserved for audio input.
    control.textureChannel = Integer.parseInt(line[0].substring(line[0].length() - 1));
    if (control.textureChannel == 0) {
      throw new IllegalArgumentException(
          "iChannel0 is reserved for system audio. Use channels 1-9 for textures.");
    }

    // token 1 is the texture file name.
    control.name = getFileName(line[1]);
    parameters.add(control);
  }

  public void parseClassName(String[] line, List<ShaderConfiguration> parameters) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.SET_CLASS_NAME;

    // token 1 is the desired class name and default pattern name
    control.name = stringCleanup(line[1]);
    parameters.add(control);
  }

  public void parseLXCategory(String[] line, List<ShaderConfiguration> parameters) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.SET_LX_CATEGORY;

    // since spaces are permissible in category names, we need to merge line's elements
    // from index 1 to the end back into a single string
    control.name = stringCleanup(String.join(" ", Arrays.copyOfRange(line, 1, line.length)));
    parameters.add(control);
  }

  private static String stringCleanup(String str) {
    // clean up delimiters
    if (str.startsWith("\"") && str.endsWith("\"")) {
      str = str.substring(1, str.length() - 1);
    }
    return str.trim();
  }

  // Convert an input token to a valid filename, removing any delimiters and
  // checking to see that the file actually exists.
  private static String getFileName(String str) {
    str = stringCleanup(str);

    // if name is enclosed in angle brackets, prefix with default resource path
    // to save repetitive typing
    if (str.startsWith("<") && str.endsWith(">")) {
      str = str.substring(1, str.length() - 1);
      str = ShaderUtils.SHADER_PATH + str;
      // cleanup again in case there were spaces or more quotes
      str = stringCleanup(str);
    }

    // check to see if the file actually exists
    File f = new File(str);
    if (!f.exists()) {
      throw new IllegalArgumentException("File " + str + " not found.");
    }
    return str;
  }

  // Parse #pragma statements in the shader code, subdividing them into shader
  // configuration parameters and texture definitions.  Note that GLSL requires
  // us to ignore any #pragma statements that we don't recognize.
  public void parsePragmas(String input, List<ShaderConfiguration> parameters) {
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

        // Common controls configuration
        String pragma = tokens[0].toLowerCase();
        if (pragma.startsWith("tecontrol.")) {
          parseControl(tokens, parameters);
        }
        // Texture channel definition
        else if (pragma.startsWith("ichannel")) {
          parseTextures(tokens, parameters);
        }
        // name of class/pattern in UI
        else if (pragma.equals("name")) {
          parseClassName(tokens, parameters);
        }
        // set LXCategory for pattern
        else if (pragma.equals("lxcategory")) {
          parseLXCategory(tokens, parameters);
        }
        // auto keyword forces use of automatic class generation system
        else if (pragma.equals("auto")) {
          ShaderConfiguration p = new ShaderConfiguration();
          p.opcode = ShaderConfigOpcode.AUTO;
          parameters.add(p);
        }
      } catch (Exception e) {
        throw new RuntimeException("Error in " + matcher.group() + "\n" + e.getMessage());
      }
    }
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
                .append(Uniforms.CUSTOM_SUFFIX)
                .append(";\n");
            addLXParameter(parameters, new BooleanParameter(placeholderName));
          } else {
            finalShader
                .append("uniform float ")
                .append(placeholderName)
                .append(Uniforms.CUSTOM_SUFFIX)
                .append(";\n");
            Double[] rangeValues =
                Arrays.stream(metadata.split(",")).map(Double::parseDouble).toArray(Double[]::new);
            addLXParameter(
                parameters,
                new CompoundParameter(
                    placeholderName, rangeValues[0], rangeValues[1], rangeValues[2]));
          }
        }
        matcher.appendReplacement(shaderCode, placeholderName + Uniforms.CUSTOM_SUFFIX);
      } catch (Exception e) {
        throw new RuntimeException("Problem parsing placeholder: " + matcher.group(0), e);
      }
    }
    matcher.appendTail(shaderCode);
    finalShader.append(shaderCode);

    return finalShader.toString();
  }

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

      parsePragmas(shaderBody, parameters);
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
}
