package titanicsend.pattern.yoffa.client.reponse;

import com.google.gson.annotations.SerializedName;

public class Input {

    private String src;

    @SerializedName("ctype")
    private Type type;

    @SerializedName("channel")
    private int channelNumber;

    public String getSrc() {
        return src;
    }

    public Type getType() {
        return type;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public enum Type {
        @SerializedName("texture") TEXTURE,
        @SerializedName("music") MUSIC,
        @SerializedName("musicstream") MUSIC_STREAM,
        @SerializedName("mic") MIC
    }

}
