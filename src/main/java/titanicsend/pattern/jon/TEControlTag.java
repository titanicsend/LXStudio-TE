package titanicsend.pattern.jon;

public enum TEControlTag {
    SPEED,
    XPOS,
    YPOS,
    SIZE,
    QUANTITY,
    SPIN,
    BRIGHTNESS,
    EXPLODE,
    WOW1,
    WOW2,
    WOWTRIGGER,
    ANGLE;

    public String getPath() {
        String path = switch (this) {
            case SPEED -> "te_speed";
            case XPOS -> "te_xpos";
            case YPOS -> "te_ypos";
            case SIZE -> "te_size";
            case QUANTITY -> "te_quantity";
            case SPIN -> "te_spin";
            case BRIGHTNESS -> "te_brightness";
            case EXPLODE -> "te_explode";
            case WOW1 -> "te_wow1";
            case WOW2 -> "te_wow2";
            case WOWTRIGGER -> "te_wowtrigger";
            case ANGLE -> "te_angle";
            default -> "";
        };

        return path;
    }

    public String getLabel() {
        return switch(this) {
            case SIZE -> "Size";
            case SPIN -> "Spin";
            case SPEED -> "Speed";
            case WOW1 -> "Wow1";
            case WOW2 -> "Wow2";
            case XPOS -> "xPos";
            case YPOS -> "yPos";
            case ANGLE -> "Angle";
            case QUANTITY -> "Quantity";
            case BRIGHTNESS -> "Brightness";
            case WOWTRIGGER -> "WowTrigger";
            case EXPLODE -> "Explode";
        };
    }
}
