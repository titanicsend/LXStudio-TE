package titanicsend.pattern.yoffa.shader_engine;

import java.util.Map;

public record FragmentShader(String shaderBody, Map<Integer, String> channelToTexture, Integer audioInputChannel) {}
