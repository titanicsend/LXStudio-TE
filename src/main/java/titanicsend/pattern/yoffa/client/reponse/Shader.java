package titanicsend.pattern.yoffa.client.reponse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Shader {

    @SerializedName("renderpass")
    private List<ShaderInfo> shaderInfos;

    public List<ShaderInfo> getShaderInfos() {
        return shaderInfos;
    }

}
