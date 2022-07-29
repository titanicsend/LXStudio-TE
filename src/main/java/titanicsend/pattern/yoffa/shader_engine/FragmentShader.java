package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.client.reponse.Shader;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentShader {
    private final String shaderBody;
    private String shaderName;
    private long shaderTimestamp;
    private final Map<Integer, String> channelToTexture;
    private boolean remoteTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters;

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        shaderName = ShaderUtils.getCacheFilename(shaderFile.getName());
        shaderTimestamp = shaderFile.lastModified();
        String shaderBody = ShaderUtils.loadResource(shaderFile.getPath());
        Map<Integer, String> channelToTexture = new HashMap<>();
        this.audioInputChannel = 0;
        for (int i = 0; i < textureFiles.size(); i++) {
            //start at 1 since audio will be texture 0
            channelToTexture.put(i+1, textureFiles.get(i).getPath());
        }
        this.parameters = new ArrayList<>();
        this.shaderBody = ShaderUtils.preprocessShader(shaderBody,this.parameters);;
        this.channelToTexture = channelToTexture;
        this.remoteTextures = false;
    }

    public FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {
        this.parameters = new ArrayList<>();
        this.shaderBody = ShaderUtils.preprocessShader(shaderBody,this.parameters);;
        this.channelToTexture = channelToTexture;
        this.audioInputChannel = audioInputChannel;
        this.remoteTextures = true;
    }

    public String getShaderBody() {
        return shaderBody;
    }

    public String getShaderName() { return shaderName; }

    public long getShaderTimestamp() { return shaderTimestamp; }

    public Map<Integer, String> getChannelToTexture() {
        return channelToTexture;
    }

    public boolean hasRemoteTextures() {
        return remoteTextures;
    }

    public Integer getAudioInputChannel() {
        return audioInputChannel;
    }

    public List<LXParameter> getParameters() {
        return parameters;
    }

}
