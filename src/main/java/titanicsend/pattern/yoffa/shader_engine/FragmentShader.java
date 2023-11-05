package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLPreprocessor;
import titanicsend.pattern.glengine.ShaderConfigOpcode;
import titanicsend.pattern.glengine.ShaderConfiguration;

import java.io.File;
import java.util.*;

public class FragmentShader {
    private final String shaderName;
    private final Map<Integer, String> channelToTexture;
    private final boolean hasTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters = new ArrayList<>();
    private final List<ShaderConfiguration> shaderConfig = new ArrayList<>();

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        String shaderBody;
        this.audioInputChannel = 0;
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
        // set it up the old way.  (This way, the old preprocessor can still benefit
        // from the new preprocessor's #include support)
        // NOTE that you can't mix old and new style texture declarations.  The
        // new preprocessor will take precedence.
        if (shaderConfig.isEmpty()) {
            for (int i = 0; i < textureFiles.size(); i++) {
                // automatically assign textures to iChannels, starting
                // at 1 since audio will be texture 0
                channelToTexture.put(i + 1, textureFiles.get(i).getPath());
            }
            ShaderUtils.legacyPreprocessor(shaderBody, this.parameters);
        }
        // otherwise, see if there are any texture declarations in the shader
        // code and if so, use those.
        else {
            for (ShaderConfiguration config : shaderConfig) {
                if (config.opcode == ShaderConfigOpcode.SET_TEXTURE) {
                    channelToTexture.put(config.textureChannel, config.name);
                }
            }
        }
        this.hasTextures = (!channelToTexture.isEmpty());
    }

    public String getShaderName() {
        return shaderName;
    }

    public Map<Integer, String> getChannelToTexture() {
        return channelToTexture;
    }

    public boolean hasTextures() {
        return hasTextures;
    }

    public List<ShaderConfiguration> getShaderConfig() {
        return shaderConfig;
    }

    public Integer getAudioInputChannel() {
        return audioInputChannel;
    }

    public List<LXParameter> getParameters() {
        return parameters;
    }
}
