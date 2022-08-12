package titanicsend.pattern.yoffa.effect;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.media.ImagePainter;
import titanicsend.pattern.yoffa.shader_engine.*;
import titanicsend.util.Dimensions;
import titanicsend.util.TE;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeShaderPatternEffect extends PatternEffect {

    protected OffscreenShaderRenderer offscreenShaderRenderer;
    private FragmentShader fragmentShader;
    private final List<LXParameter> parameters;

    private ShaderPainter painter;

    LinkedColorParameter iColor = null;
    TEPattern.ColorType colorType;
    FloatBuffer palette;
     AudioInfo audioInfo;
    ShaderOptions shaderOptions;

    /**
     * Creates new native shader effect with the specified shader options.
     * @param fragmentShader
     * @param target
     * @param options - ShaderOptions object
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target, ShaderOptions options) {
        super(target);
        this.colorType = target.colorType;
        this.audioInfo = new AudioInfo(pattern.getLX().engine.audio.meter);
        this.shaderOptions = options;
        painter = new ShaderPainter();

        // create an 5 x 3 array that we can pass to OpenGL so we can
        // share the entire current palette with GLSL shaders
        this.palette = Buffers.newDirectFloatBuffer(15);


        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader,options);
            this.parameters = fragmentShader.getParameters();

        } else {
            this.parameters = null;
        }
    }

    /**
     * Creates new native shader effect with default options - shader alpha will be ignored,
     * audio data and LX parameters will be provided as uniforms to the shader.
     * @param fragmentShader
     * @param target
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        // alpha disabled in this version to preserve backward compatibility
        this(fragmentShader,target,new ShaderOptions());
    }

    /**
     * Creates new native shader effect with additional texture support, using
     * the specified shader options.
     * @param shaderFilename
     * @param target
     * @param options - ShaderOptions object
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, ShaderOptions options, String... textureFilenames) {
        this(new FragmentShader(new File("resources/shaders/" + shaderFilename),
                        Arrays.stream(textureFilenames)
                                .map(x -> new File("resources/shaders/textures/" + x))
                                .collect(Collectors.toList())),
                target,options);

    }

    /**
     * Creates new native shader effect with additional texture support, using
     * the default options - shader alpha will be ignored, audio data and LX parameters
     * will be provided as uniforms to the shader.
     * @param shaderFilename
     * @param target
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, String... textureFilenames) {
        // alpha disabled in this version to preserve backward compatibility
        this(shaderFilename,target,new ShaderOptions(), textureFilenames);
    }

    @Override
    public void onPatternActive() {
        if (fragmentShader != null) {
            if (offscreenShaderRenderer == null) {
                offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader, shaderOptions);

            }
        }
        // just to make sure we actually have a pointer to the parameter and
        // that it's grabbing the right color
        iColor = (LinkedColorParameter) pattern.getParameter("iColor");
        if (iColor != null) iColor.index.setValue(colorType.index);
    }

    private void getCurrentPalette() {
        int col;
        float r,g,b;

        if (palette != null ) {
            palette.rewind();
            for (int i = 0; i < 5; i++) {

                int color = pattern.getLX().engine.palette.swatch.getColor(i).getColor();

                r = (float) (0xff & LXColor.red(color)) / 255f;
                palette.put(r);
                g = (float) (0xff & LXColor.green(color)) / 255f;
                palette.put(g);
                b = (float) (0xff & LXColor.blue(color)) / 255f;
                palette.put(b);
            }
            palette.rewind();
        }
    }


    @Override
    public void run(double deltaMs) {
        if (offscreenShaderRenderer == null) {
            return;
        }

        int color = (iColor != null) ? iColor.calcColor() : 0;
        getCurrentPalette();

        audioInfo.setFrameData(pattern.getTempo().basis(),
                pattern.sinePhaseOnBeat(), pattern.getBassLevel(),
                pattern.getTrebleLevel(), color, palette);

        int[][] snapshot = offscreenShaderRenderer.getFrame(audioInfo);

        /*
         TODO - We should really use setColor for this instead of exposing colors as this will break blending
         TODO - pass the images to be blended into a shader as textures and do blending in hardware!
        */
        painter.setImage(snapshot);
        painter.setColors(pattern.getColors());
        for (Map.Entry<LXPoint, Dimensions> entry : pointsToCanvas.entrySet()) {
            painter.paint(entry.getKey(), entry.getValue());
        }
    }

    // Saves me from having to propagate all those setUniform(name,etc.) methods up the
    // object hierarchy!  It grants you great power.  Use responsibly!
    public NativeShader getNativeShader() { return offscreenShaderRenderer.getNativeShader(); }

    @Override
    public List<LXParameter> getParameters() {
        return parameters;
    }
}
