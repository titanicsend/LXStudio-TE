package titanicsend.pattern.jon;

public enum TEControlTag {
    SPEED,
    XPOS,
    YPOS,
    SIZE,
    QUANTITY,
    SPIN,
    BRIGHTNESS,
    WOW1,
    WOW2,
    WOWTRIGGER;

    public String getPath() {
        String path = switch (this) {
            case SPEED -> "te_speed";
            case XPOS -> "te_xpos";
            case YPOS -> "te_ypos";
            case SIZE -> "te_size";
            case QUANTITY -> "te_quantity";
            case SPIN -> "te_spin";
            case BRIGHTNESS -> "te_brightness";
            case WOW1 -> "te_wow1";
            case WOW2 -> "te_wow2";
            case WOWTRIGGER -> "te_wowtrigger";
            default -> "";
        };

        return path;
    }
}
