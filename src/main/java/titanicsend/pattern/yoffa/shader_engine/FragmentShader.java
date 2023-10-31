package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLPreprocessor;
import titanicsend.pattern.glengine.ShaderConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FragmentShader {
    private String shaderBody;
    private String shaderName;
    private final Map<Integer, String> channelToTexture;
    private boolean hasTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters = new ArrayList<>();
    public ArrayList<ShaderConfiguration> shaderConfig = new ArrayList<ShaderConfiguration>();

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        this.audioInputChannel = 0;
        this.channelToTexture = new HashMap<>();

        // try the new way
        GLPreprocessor glp = new GLPreprocessor();
        shaderName = shaderFile.getName();
        try {
            shaderBody = glp.preprocessShader(shaderName,shaderConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if the shader doesn't have any directives for the new preprocessor,
        // set it up the old way.  (This way, the old preprocessor can still benefit
        // from the new preprocessor's #include support)
        // NOTE That you can't mix old and new style texture declarations.  The
        // new preprocessor will take precedence.
        if (shaderConfig.size() == 0) {
            for (int i = 0; i < textureFiles.size(); i++) {
                // automatically assign textures to iChannels, starting
                // at 1 since audio will be texture 0
                channelToTexture.put(i + 1, textureFiles.get(i).getPath());
            }
            shaderBody = ShaderUtils.preprocessShader(shaderBody, this.parameters);
            this.hasTextures = (!channelToTexture.isEmpty());
        }
    }

    public FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {
        this.shaderBody = ShaderUtils.preprocessShader(shaderBody,this.parameters);
        this.channelToTexture = channelToTexture;
        this.audioInputChannel = audioInputChannel;
        this.hasTextures = true;
    }

    public String getShaderBody() {
        return shaderBody;
    }

    public String getShaderName() { return shaderName; }

    public Map<Integer, String> getChannelToTexture() {
        return channelToTexture;
    }

    public boolean hasTextures() {
        return hasTextures;
    }

    public Integer getAudioInputChannel() {
        return audioInputChannel;
    }

    public List<LXParameter> getParameters() {
        return parameters;
    }
}
