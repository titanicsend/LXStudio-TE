package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.util.*;

public class FragmentShader {
    private final String shaderBody;
    private String shaderName;
    private final Map<Integer, String> channelToTexture;
    private boolean hasTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters;

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        shaderName = shaderFile.getName();
        String shaderBody = ShaderUtils.loadResource(shaderFile.getPath());
        Map<Integer, String> channelToTexture = new HashMap<>();
        this.audioInputChannel = 0;
        for (int i = 0; i < textureFiles.size(); i++) {
            //start at 1 since audio will be texture 0
            channelToTexture.put(i+1, textureFiles.get(i).getPath());
        }
        this.parameters = new ArrayList<>();
        this.shaderBody = ShaderUtils.preprocessShader(shaderBody,this.parameters);
        this.channelToTexture = channelToTexture;
        this.hasTextures = false;
    }

    public FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {
        this.parameters = new ArrayList<>();
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
