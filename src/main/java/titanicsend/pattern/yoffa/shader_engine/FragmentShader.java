package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLPreprocessor;
import titanicsend.pattern.glengine.ShaderConfigOperation;
import titanicsend.pattern.glengine.ShaderConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FragmentShader {
    private String shaderBody;
    private String shaderName;
    private final Map<Integer, String> channelToTexture;
    private final boolean hasTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters = new ArrayList<>();
    private final List<ShaderConfiguration> shaderConfig = new ArrayList<>();

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        this.audioInputChannel = 0;
        this.channelToTexture = new HashMap<>();

        // try the new way
        GLPreprocessor glp = new GLPreprocessor();
        shaderName = shaderFile.getName();
        try {
            shaderBody = glp.preprocessShader(shaderFile,shaderConfig);
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
            shaderBody = ShaderUtils.preprocessShader(shaderBody, this.parameters);
        }
        // otherwise, see if there are any texture declarations in the shader
        // code and if so, use those.
        else {
            for (ShaderConfiguration config : shaderConfig) {
                if (config.operation == ShaderConfigOperation.SET_TEXTURE) {
                    channelToTexture.put(config.textureChannel, config.textureFileName);
                }
            }
        }
        this.hasTextures = (!channelToTexture.isEmpty());
    }

    public FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {
        this.shaderBody = ShaderUtils.preprocessShader(shaderBody,this.parameters);
        this.channelToTexture = channelToTexture;
        this.audioInputChannel = audioInputChannel;
        this.hasTextures = true;
    }

    public String getShaderName() { return shaderName; }

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
