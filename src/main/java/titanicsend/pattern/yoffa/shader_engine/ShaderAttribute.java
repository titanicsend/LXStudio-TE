package titanicsend.pattern.yoffa.shader_engine;

public enum ShaderAttribute {
    POSITION("inPosition"), INDEX("inIndex");

    private final String attributeName;

    ShaderAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
