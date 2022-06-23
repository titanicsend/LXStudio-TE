package titanicsend.pattern.yoffa.client;

import com.google.gson.*;
import titanicsend.pattern.yoffa.client.reponse.Input;
import titanicsend.pattern.yoffa.client.reponse.ShaderInfo;
import titanicsend.pattern.yoffa.client.reponse.ShaderResponse;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderToyClient {

    private static final String APP_KEY = "NtHKMw";
    private static final String BASE_URL = "https://www.shadertoy.com";
    private static final String BASE_API_URL = BASE_URL + "/api/v1/shaders/";
    private static final String APP_KEY_PARAM = "key=" + APP_KEY;

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static FragmentShader getShader(String shaderId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_API_URL + shaderId + "?" + APP_KEY_PARAM))
                .build();

        String responseBody;
        try {
            responseBody = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ShaderResponse shaderResponse = gson.fromJson(responseBody, ShaderResponse.class);
        if (shaderResponse.getError() != null) {
            throw new RuntimeException("Shader API error: " + shaderResponse.getError());
        }

        return parseShaderResponse(shaderResponse);
    }

    private static FragmentShader parseShaderResponse(ShaderResponse shaderResponse) {
        ShaderInfo shaderInfo = shaderResponse.getShaderInfo();
        String shaderBody = shaderInfo.getCode();

        Integer audioInput = null;
        Map<Integer, String> textureInputs = new HashMap<>();
        List<Input> inputs = shaderInfo.getInputs();
        for (Input input : inputs) {
            if (Input.Type.TEXTURE.equals(input.getType())) {
                String textureUrl = BASE_URL + input.getSrc();
                textureInputs.put(input.getChannelNumber(), textureUrl);
            } else if (input.getType() != null) {
                audioInput = input.getChannelNumber();
            } else {
                throw new RuntimeException("Unsupported input type!");
            }
        }

        return new FragmentShader(shaderBody, textureInputs, audioInput);
    }

}
