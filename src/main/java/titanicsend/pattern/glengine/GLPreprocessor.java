package titanicsend.pattern.glengine;

import heronarts.lx.parameter.BoundedParameter;
import titanicsend.pattern.jon.TEControlTag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLPreprocessor {

    // used in #include processing to prevent infinite recursion
    // and to track line numbering for relevant error messages.
    public static final int MAX_INCLUDE_DEPTH = 10;
    public boolean foundInclude = false;
    public int lineCount = 0;

    // paths to various shader resources
    public static final String SHADER_PATH = "resources/shaders/";
    public static final String FRAMEWORK_PATH = SHADER_PATH + "framework/";

    // regex strings for internal use by the preprocessor
    private static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";

    public static String loadResource(String fileName) throws IOException {
        Scanner s = new Scanner(new File(fileName), StandardCharsets.UTF_8);
        s.useDelimiter("\\A");
        String result = s.next();
        s.close();
        return result;
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
                String filename = getFileName(line.substring("#include ".length(), line.length()));

                BufferedReader fileReader = new BufferedReader(new FileReader(filename));
                String fileLine;

                // restart line counter for include file
                output.append("#line 1 \n");
                while ((fileLine = fileReader.readLine()) != null) {
                    output.append(fileLine).append("\n");
                }
                fileReader.close();

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
    public static ShaderConfigOperation opcodeFromString(String str) {
        return switch (str) {
            case "Value" -> ShaderConfigOperation.SET_VALUE;
            case "Range" -> ShaderConfigOperation.SET_RANGE;
            case "Label" -> ShaderConfigOperation.SET_LABEL;
            case "Exponent" -> ShaderConfigOperation.SET_EXPONENT;
            case "NormalizationCurve" -> ShaderConfigOperation.SET_NORMALIZATION_CURVE;
            case "Disable" -> ShaderConfigOperation.DISABLE;
            case "NORMAL" -> ShaderConfigOperation.SET_TRANSLATE_MODE_NORMAL;
            case "DRIFT" -> ShaderConfigOperation.SET_TRANSLATE_MODE_DRIFT;
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
            control.operation = opcodeFromString(tokens[2]);
            return;
        } else {
            control.parameterId = TEControlTag.valueOf(tokens[1]);
            control.name = control.parameterId.getLabel();
            // the third token is the operation to be performed on the specified control

            control.operation = opcodeFromString(tokens[2]);

            // now we need to parse the rest of the tokens, which will depend on the operation
            switch (control.operation) {
                case SET_VALUE -> control.value = Double.parseDouble(line[1]);
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
                case SET_EXPONENT -> control.exponent = Double.parseDouble(line[1]);
                case SET_NORMALIZATION_CURVE -> {
                    control.normalizationCurve = BoundedParameter.NormalizationCurve.valueOf(line[1]);
                }
            }
        }
        parameters.add(control);
    }

    // Parse a texture definition #pragma and add it to the list of shader
    // configuration parameters
    public void parseTextures(String[] line, List<ShaderConfiguration> parameters) {
        ShaderConfiguration control = new ShaderConfiguration();
        control.operation = ShaderConfigOperation.SET_TEXTURE;

        // the last character of token 0 is the integer channel identifier
        // valid channels are 1-9.  Channel 0 is reserved for audio input.
        control.textureChannel = Integer.parseInt(line[0].substring(line[0].length() - 1));
        if (control.textureChannel == 0) {
            throw new IllegalArgumentException("iChannel0 is reserved for audio input.");
        }

        // token 1 is the texture file name.
        control.textureFileName = getFileName(line[1]);

        parameters.add(control);

    }

    public void parseClassName(String[] line, List<ShaderConfiguration> parameters) {
        ShaderConfiguration control = new ShaderConfiguration();
        control.operation = ShaderConfigOperation.SET_LABEL;

        // token 1 is the desired class name (and pattern
        // name in the UI.)
        control.textureFileName = stringCleanup(line[1]);
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
            str = SHADER_PATH + str;
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
            // tokenize the line, dividing first by whitespace and parentheses
            // then by commas
            String[] tokens = matcher.group().split("\\s|\\(|\\)");
            // discard empty tokens
            tokens = Arrays.stream(tokens).filter(s -> !s.isEmpty()).toArray(String[]::new);
            // discard the #pragma token
            tokens = Arrays.copyOfRange(tokens, 1, tokens.length);

            // Common controls configuration
            if (tokens[0].startsWith("TEControl.")) {
                parseControl(tokens, parameters);
            }
            // Texture channel definition
            else if (tokens[0].startsWith("iChannel")) {
                parseTextures(tokens, parameters);
            } else if (tokens[0].startsWith("Name")) {
                parseClassName(tokens, parameters);
            } else if (tokens[0].startsWith("LXCategory")) {
                // TODO - set LX Pattern sort category
                // TODO - Placeholder: not yet implemented.
            }
        }
    }

    public static String getVertexShaderTemplate() throws IOException {
        return loadResource(FRAMEWORK_PATH + "default.vs");
    }

    public static String getFragmentShaderTemplate() throws IOException {
        return loadResource(FRAMEWORK_PATH + "template.fs");
    }

    /**
     * Preprocess the shader, expanding #includes and handling our TE-specific control
     * and texture configuration #pragmas.
     * Note that this preprocessor doesn't support the old method of adding
     * extra controls.  TODO - should we add this as an option?
     */
    public String preprocessShader(String shaderPath, List<ShaderConfiguration> parameters) throws IOException {
        String shaderBody = loadResource(shaderPath);
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

            parsePragmas(shaderBody, parameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // combine the fragment shader code with the template
        shaderBody = getFragmentShaderTemplate().replace(SHADER_BODY_PLACEHOLDER, shaderBody);
        return shaderBody;
    }
}
