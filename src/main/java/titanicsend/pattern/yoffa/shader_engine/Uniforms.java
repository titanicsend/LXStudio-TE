package titanicsend.pattern.yoffa.shader_engine;

public class Uniforms {

    public static final String TIME_SECONDS = "iTime";
    public static final String RESOLUTION = "iResolution";
    public static final String MOUSE = "iMouse";
    public static final String CHANNEL = "iChannel";
    public static final String CUSTOM_SUFFIX = "_parameter";

    public enum Audio {
        BEAT ("beat"),
        SIN_PHASE_BEAT ("sinPhaseBeat"),
        BASS_LEVEL ("bassLevel"),
        TREBLE_LEVEL ("trebleLevel");

        private final String uniformName;

        Audio(String uniformName) {
            this.uniformName = uniformName;
        }

        public String getUniformName() {
            return uniformName;
        }

        public static Audio fromString(String text) {
            for (Audio audio : Audio.values()) {
                if (audio.uniformName.equalsIgnoreCase(text)) {
                    return audio;
                }
            }
            return null;
        }
    }

}
