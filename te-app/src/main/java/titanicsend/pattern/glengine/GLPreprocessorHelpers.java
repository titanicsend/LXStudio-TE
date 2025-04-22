package titanicsend.pattern.glengine;

import heronarts.lx.parameter.BoundedParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

public class GLPreprocessorHelpers {
  /** Converts strings from control definition #pragmas to shader configuration opcode values */
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

  /** Parse a control definition #pragma and add it to the list of shader configuration parameter */
  public static ShaderConfiguration parseControl(String[] line) {
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
    return control;
  }

  /**
   * Parse a texture definition #pragma and add it to the list of shader configuration parameters
   */
  public static ShaderConfiguration parseTextures(String[] line) {
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
    return control;
  }

  public static ShaderConfiguration parseClassName(String[] line) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.SET_CLASS_NAME;

    // token 1 is the desired class name and default pattern name
    control.name = stringCleanup(line[1]);
    return control;
  }

  public static ShaderConfiguration parseLXCategory(String[] line) {
    ShaderConfiguration control = new ShaderConfiguration();
    control.opcode = ShaderConfigOpcode.SET_LX_CATEGORY;

    // since spaces are permissible in category names, we need to merge line's elements
    // from index 1 to the end back into a single string
    control.name = stringCleanup(String.join(" ", Arrays.copyOfRange(line, 1, line.length)));
    return control;
  }

  /** Parse #iUniform directives (to be compatible with VSCode ShaderToy extension). */
  public static List<ShaderConfiguration> parseIUniforms(String input) {
    List<ShaderConfiguration> parameters = new ArrayList<>();

    Pattern pattern = Pattern.compile("^\\s*#iUniform.*", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(input);

    while (matcher.find()) {
      try {
        // tokenize the line, dividing first by whitespace and parentheses
        // NOTE: trim leading/trailing whitespace, so that leading/trailing
        // newline matches don't screw up our parsing.
        String[] parts = matcher.group().trim().split("=");
        if (parts.length != 2) {
          throw new Exception("Expected 2 parts delimited by '=', but found: " + parts.length);
        }
        String[] lhsTokens = parts[0].split("\\s|\\(|\\)");
        String rhs = parts[1];

        //        System.out.println("(in) LHS: " + Arrays.toString(lhsTokens));
        if (lhsTokens.length != 3) {
          throw new Exception(
              "Expected 3 LHS tokens delimited by whitespace, but found: "
                  + lhsTokens.length
                  + " in string'"
                  + parts[0]
                  + "'");
        } else if (!lhsTokens[0].equals("#iUniform")) {
          throw new Exception("Expected first token on LHS to be '#iUniform'");
        }
        String varType = lhsTokens[1];
        String varName = lhsTokens[2];
        //        System.out.println("(out) LHS: '"+varType+" "+varName+"'");

        if (varType.equals("float")) {
          /*
           * Regular expression pattern to extract three floating-point values from strings in the format:
           * "<value> in {<lowerBound>,<upperBound>}"
           *
           * Pattern: (\\d*\\.\\d*|\\d+\\.?)\\s*in\\s*\\{\\s*(\\d*\\.\\d*|\\d+\\.?)\\s*,\\s*(\\d*\\.\\d*|\\d+\\.?)\\s*\\}
           *
           * Breakdown:
           * 1. (\\d*\\.\\d*|\\d+\\.?) - Captures a floating-point number in GLSL notation:
           *    - \\d*\\.\\d* matches numbers like ".5", "0.5", or "1.0"
           *    - \\d+\\.? matches numbers like "1" or "1."
           *    - The | (OR) operator allows either format
           *
           * 2. \\s*in\\s* - Matches the word "in" with optional whitespace before and after
           *
           * 3. \\{\\s* - Matches the opening curly brace with optional whitespace after
           *
           * 4. The same floating-point pattern is repeated for the lower bound
           *
           * 5. \\s*,\\s* - Matches the comma separator with optional whitespace
           *
           * 6. The floating-point pattern is repeated again for the upper bound
           *
           * 7. \\s*\\} - Matches the closing curly brace with optional whitespace before
           *
           * Capturing groups:
           * - Group 1: The initial value
           * - Group 2: The lower bound
           * - Group 3: The upper bound
           */
          Pattern floatRangePattern =
              Pattern.compile(
                  "(-?\\d*\\.\\d*|\\d+\\.?)\\s*"
                      + "in\\s*\\{\\s*(-?\\d*\\.\\d*|\\d+\\.?)"
                      + "\\s*,\\s*(-?\\d*\\.\\d*|\\d+\\.?)"
                      + "\\s*}" // Removed the \n and escaped the closing brace properly
                  );

          Matcher floatRangeMatcher = floatRangePattern.matcher(rhs);

          if (!floatRangeMatcher.find()) {
            throw new RuntimeException("float range didn't match: [" + rhs + "]");
          }

          float rangeDefault = parseGlslFloat(floatRangeMatcher.group(1));
          float rangeLower = parseGlslFloat(floatRangeMatcher.group(2));
          float rangeUpper = parseGlslFloat(floatRangeMatcher.group(3));

          ShaderConfiguration control = new ShaderConfiguration();

          String tagName = varName.trim().toUpperCase();
          if (tagName.startsWith("I")) {
            // "iSpeed" should look up tag "SPEED"
            tagName = tagName.substring(1);
          }
          if (tagName.equals("SCALE")) {
            tagName = "SIZE";
          }
          try {
            control.opcode = ShaderConfigOpcode.SET_RANGE;
            control.parameterId = TEControlTag.valueOf(tagName);
            control.name = control.parameterId.getLabel();
            control.value = rangeDefault;
            control.v1 = rangeLower;
            control.v2 = rangeUpper;
            parameters.add(control);
          } catch (IllegalArgumentException exception) {
            TE.error("Unsupported tag name: %s", varName);
          }
        } else if (varType.equals("vec2") || varType.equals("vec3") || varType.equals("color3")) {
          // no-op
        } else {
          throw new RuntimeException("iUniform data type not yet implemented: " + varType);
        }
      } catch (Exception e) {
        throw new RuntimeException("Error in " + matcher.group() + "\n" + e.getMessage());
      }
    }
    return parameters;
  }

  private static float parseGlslFloat(String s) {
    if (s.startsWith(".")) {
      return Float.parseFloat("0" + s);
    } else if (s.endsWith(".")) {
      return Float.parseFloat(s + "0");
    }
    return Float.parseFloat(s);
  }

  public static String removeIUniformLines(String fileContent) {
    String[] lines =
        Arrays.stream(fileContent.split("\\n"))
            .filter(line -> !line.contains("#iUniform"))
            .toArray(String[]::new);
    return String.join("\n", lines);
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
  public static String getFileName(String str) {
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
}
