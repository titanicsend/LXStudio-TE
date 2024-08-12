package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.util.*;
import titanicsend.pattern.glengine.GLPreprocessor;
import titanicsend.pattern.glengine.ShaderConfiguration;

/**
 * Everything that needs to be stored for later compilation and use when we read a shader file.
 * Doesn't allocate or use any native OpenGL resources. - the expanded, preprocessed source code -
 * the list of texture files and channels to be used by the shader - control configuration and other
 * parameter data
 */
public class FragmentShader {
  private final String shaderName;
  private final Map<Integer, String> channelToTexture;
  private final List<LXParameter> parameters = new ArrayList<>();
  private final List<ShaderConfiguration> shaderConfig = new ArrayList<>();

  public FragmentShader(File shaderFile, List<File> textureFiles) {
    String shaderBody;
    this.channelToTexture = new HashMap<>();

    // try the new way
    GLPreprocessor glp = new GLPreprocessor();
    shaderName = shaderFile.getName();

    try {
      shaderBody = glp.preprocessShader(shaderFile, shaderConfig);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // if the shader doesn't have any directives for the new preprocessor,
    // we just need to add any textures specified in the constructor.
    if (shaderConfig.isEmpty()) {
      for (int i = 0; i < textureFiles.size(); i++) {
        // automatically assign textures to iChannels, starting
        // at 5 since audio will be texture 0, plus 4 location textures
        channelToTexture.put(i + 5, textureFiles.get(i).getPath());
      }
    }
    // otherwise, see if there are any texture declarations or extra parameters
    // in the shader code and if so, use those.
    else {
      for (ShaderConfiguration config : shaderConfig) {
        switch (config.opcode) {
          case SET_TEXTURE:
            channelToTexture.put(config.textureChannel, new File(config.name).getPath());
            break;
          case ADD_LX_PARAMETER:
            parameters.add(config.lxParameter);
            break;
          default:
            break;
        }
      }
    }
  }

  public String getShaderName() {
    return shaderName;
  }

  public Map<Integer, String> getChannelToTexture() {
    return channelToTexture;
  }

  public List<ShaderConfiguration> getShaderConfig() {
    return shaderConfig;
  }

  public List<LXParameter> getParameters() {
    return parameters;
  }
}
