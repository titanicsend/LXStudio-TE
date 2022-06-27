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

    private static final Pattern PLACEHOLDER_FINDER = Pattern.compile("\\{%(.*?)(\\[(.*?)\\])??\\}");

    private final String shaderBody;
    private final Map<Integer, String> channelToTexture;
    private boolean remoteTextures;
    private final Integer audioInputChannel;
    private final List<LXParameter> parameters;

    public FragmentShader(File shaderFile, List<File> textureFiles) {
        String shaderBody = ShaderUtils.loadResource(shaderFile.getPath());
        Map<Integer, String> channelToTexture = new HashMap<>();
        this.audioInputChannel = 0;
        for (int i = 0; i < textureFiles.size(); i++) {
            //start at 1 since audio will be texture 0
            channelToTexture.put(i+1, textureFiles.get(i).getPath());
        }
        this.parameters = new ArrayList<>();
        this.shaderBody = parseCustomParameters(shaderBody);;
        this.channelToTexture = channelToTexture;
        this.remoteTextures = false;
    }

    public FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {
        this.parameters = new ArrayList<>();
        this.shaderBody = parseCustomParameters(shaderBody);;
        this.channelToTexture = channelToTexture;
        this.audioInputChannel = audioInputChannel;
        this.remoteTextures = true;
    }

    private String parseCustomParameters(String shaderBody) {
        Matcher matcher = PLACEHOLDER_FINDER.matcher(shaderBody);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            try {
                String placeholderName = matcher.group(1);
                if (matcher.groupCount() >= 3) {
                    String metadata = matcher.group(3);
                    if ("bool".equals(metadata)) {
                        parameters.add(new BooleanParameter(placeholderName));
                    } else {
                        Double[] rangeValues = Arrays.stream(metadata.split(","))
                                .map(Double::parseDouble)
                                .toArray(Double[]::new);
                        parameters.add(new CompoundParameter(placeholderName, rangeValues[0], rangeValues[1], rangeValues[2]));
                    }
                }
                matcher.appendReplacement(stringBuilder, placeholderName + Uniforms.CUSTOM_SUFFIX);
            } catch (Exception e) {
                throw new RuntimeException("Problem parsing placeholder: " + matcher.group(0), e);
            }
        }
        matcher.appendTail(stringBuilder);
        return buildCustomUniforms(parameters) + stringBuilder.toString();
    }

    private String buildCustomUniforms(List<LXParameter> customParameters) {
        StringBuilder stringBuilder = new StringBuilder();
        for (LXParameter parameter : customParameters) {
            String type = parameter instanceof BooleanParameter ? "bool" : "float";
            stringBuilder.append("uniform ").append(type).append(" ")
                    .append(parameter.getLabel()).append(Uniforms.CUSTOM_SUFFIX)
                    .append(";\n");
        }
        return stringBuilder.toString();
    }

    public String getShaderBody() {
        return shaderBody;
    }

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
