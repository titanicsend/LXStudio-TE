package titanicsend.pattern.yoffa.client.reponse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ShaderResponse {

    @SerializedName("Error")
    private String error;

    @SerializedName("Shader")
    private Shader shader;

    public String getError() {
        return error;
    }

    public ShaderInfo getShaderInfo() {
        return shader.getShaderInfos().get(0);
    }
}
