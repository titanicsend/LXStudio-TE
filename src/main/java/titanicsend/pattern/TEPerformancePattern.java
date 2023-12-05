package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import java.nio.FloatBuffer;
import java.util.List;
import titanicsend.app.TEGlobalPatternControls;
import titanicsend.pattern.glengine.ShaderConfiguration;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.Rotor;
import titanicsend.util.TEColor;

public abstract class TEPerformancePattern extends TEAudioPattern {

    private final TEShaderView defaultView;

    /**
     * Subclasses can override to specify a preferred default view.
     * Alternatively, just pass a default to TEPerformancePattern's constructor.
     *
     * Warning for overrides:
     * Called from this constructor prior to child class constructors.
     */
    public TEShaderView getDefaultView() {
        return defaultView;
    }

    // TODO(JKB): Move these 4 to TECommonControls?
    private final VariableSpeedTimer iTime = new VariableSpeedTimer();
    private final VariableSpeedTimer spinTimer = new VariableSpeedTimer();
    private final Rotor speedRotor = new Rotor();
    final Rotor spinRotor = new Rotor();

    protected final TECommonControls controls;

    protected final FloatBuffer palette = Buffers.newDirectFloatBuffer(15);

    protected final TEGlobalPatternControls globalControls;

    protected TEPerformancePattern(LX lx) {
        this(lx, null);
    }

    protected TEPerformancePattern(LX lx, TEShaderView defaultView) {
        super(lx);
        controls = new TECommonControls(this);

        this.defaultView = defaultView;

        this.globalControls = (TEGlobalPatternControls) lx.engine.getChild("globalPatternControls");

        lx.engine.addTask(() -> {
            if (this.controls.color == null) {
                // Instantiation failed. Turn off now to avoid fatal call to null variables.
                return;
            }

            // Because LXPattern child classes can not override defaultRemoteControls and have to use
            // customRemoteControls, that means a list of our remote controls are saved to file and
            // then on file open the pattern is loaded and THEN LXDeviceComponent.load() restores a
            // list of the old remote controls thinking they're user-custom.  In our case to prevent
            // needing to recreate .lxps every time a pattern parameter is changed, we'll just refresh
            // the remote controls AGAIN after LXDeviceComponent restored the old ones AFTER the
            // pattern loaded from file.  This means runtime user-customized remote controls will not
            // survive a file save/load which is much less inconvenient than this default behavior.
            this.controls.setRemoteControls();
        });
    }

    public TECommonControls getControls() {
        return controls;
    }

    public void addCommonControls() {
        this.controls.addCommonControls(this);
        this.controls.setRemoteControls();

        this.controls.getLXControl(TEControlTag.WOWTRIGGER).addListener(wowTriggerListener);
    }

    // package-protected passthrough so TECommonControls can add parameters
    LXComponent addParam(String path, LXParameter parameter) {
        addParameter(path, parameter);
        return this;
    }

    LXComponent removeParam(String path) {
        removeParameter(path);
        return this;
    }

    public FloatBuffer getCurrentPalette() {
        float r, g, b;

        if (palette != null) {
            palette.rewind();
            for (int i = 0; i < 5; i++) {

                int color = getLX().engine.palette.swatch.getColor(i).getColor();

                r = (float) (0xff & LXColor.red(color)) / 255f;
                palette.put(r);
                g = (float) (0xff & LXColor.green(color)) / 255f;
                palette.put(g);
                b = (float) (0xff & LXColor.blue(color)) / 255f;
                palette.put(b);
            }
            palette.rewind();
        }
        return palette;
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Speed" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes. The "Angle" controls sets an additional
     * angular offset.
     */
    public double getRotationAngleFromSpeed() {
        // Loosely beat linked speed.  What this thinks it's doing is moving at one complete rotation
        // per beat, based on the elapsed time and the engine's bpm rate.
        // But since we're using variable time, we can speed it up and slow it down smoothly by adjusting
        // the speed of time, and still have keep its speed in sync with the beat.
        return -(speedRotor.getAngle() - getStaticRotationAngle());
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Spin" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes. The "Angle" controls sets an additional
     * angular offset.
     */
    public double getRotationAngleFromSpin() {
        // See comments in getRotationAngleFromSpeed() above.
        return spinRotor.getAngle() - getStaticRotationAngle();
    }

    public double getStaticRotationAngle() {
        double t = controls.getValue(TEControlTag.ANGLE);
        return (t >= 0) ? t : t + LX.TWO_PI;
    }

    /**
     * Sets the maximum rotation speed used by both getRotationAngleFromSpin() and
     * getRotationAngleFromSpeed().
     * <p>
     * The default maximum radians per second is PI, which gives one complete rotation
     * every two beats at the current engine BPM.  Do not change this value unless you
     * have a specific reason for doing so.  Too high a rotation speed can cause visuals
     * to become erratic.
     *
     * @param radiansPerSecond
     */
    public void setMaxRotationSpeed(double radiansPerSecond) {}

    /*
     * Color safety mechanism: only calculate solid colors once per frame.
     * Child classes are still encouraged to only call the color methods
     * once but this will reduce impact of uncaught cases.
     */
    private boolean isStaleColor = true;
    private boolean isStaleColor2 = true;
    private boolean isStaleColorBase = true;
    private int _getColor, _calcColor, _calcColor2;

    protected void expireColors() {
        isStaleColorBase = isStaleColor = isStaleColor2 = true;
    }

    /**
     * @return Color derived from the current setting of the color and brightness controls
     *
     * NOTE:  The design philosophy here is that palette colors (and the color control)
     * have precedence.
     *
     * Brightness modifies the current color, and is set to 1.0 (100%) by default. So
     * if you don't move the brightness control you get *exactly* the currently
     * selected color.
     *
     * At present, the brightness control lets you dim the current color,
     * but if you want to brighten it, you have to do that with the channel fader or
     * the color control.
     */
    public int calcColor() {
        if (isStaleColor) {
            _calcColor = TEColor.setBrightness(controls.color.calcColor(), (float) getBrightness());
            isStaleColor = false;
        }
        return _calcColor;
    }

    /**
     * For patterns that consume two solid colors, use this method
     * to retrieve the 2nd color.
     * Returns a color offset in position from the first color.
     *
     * @return
     */
    public int calcColor2() {
        if (isStaleColor2) {
            _calcColor2 = TEColor.setBrightness(controls.color.calcColor2(), (float) getBrightness());
            isStaleColor2 = false;
        }
        return _calcColor2;
    }

    /**
     * Gets the current color as set in the color control, without adjusting
     * for brightness.  This is used by the OpenGL renderer, which has
     * a unified mechanism for handling brightness.
     */
    public int getColor() {
        if (isStaleColorBase) {
            _getColor = controls.color.calcColor();
            isStaleColorBase = false;
        }
        return _getColor;
    }

    public int getGradientColor(float lerp) {
        return TEColor.setBrightness(controls.color.getGradientColor(lerp), (float) getBrightness());
    }

    /**
     * @return Current variable time in seconds.  Note that time
     * can run both forward and backward, so the returned value can be negative.
     */
    public double getTime() {
        return iTime.getTime();
    }

    /**
     * @return Current variable time in milliseconds.  Note that time
     * can run both forward and backward, so the returned value can be negative.
     */
    public double getTimeMs() {
        return iTime.getTimeMs();
    }

    /**
     * Controls whether time is allowed to run both forward and backward,
     * according to the sign of the current scale value.  Bidirectional
     * time is allowed (true) by default.
     *
     * @param val true for bidirectional time, false for forward-moving time only,
     *            regardless of time scale setting.
     */
    public void allowBidirectionalTime(boolean val) {
        iTime.allowBidirectionalTime(val);
    }

    /**
     * @return current variable time in milliseconds since last call to this timer's
     * Tick() function (normally called automatically at the start of each frame.)
     * Note that time can run both forward and backward, so the returned value can be negative.
     */
    public double getDeltaMs() {
        return iTime.getDeltaMs();
    }

    public double getSpeed() {
        BoundedParameter speedControl = (BoundedParameter) controls.getControl(TEControlTag.SPEED).control;
        double spd = speedControl.getValue();
        if (globalControls.useGlobalSpeed.isOn()) {
            double g = globalControls.globalSpeed.getValue();
            spd = g * ((spd >= 0) ? speedControl.range.max : speedControl.range.min);
        }
        return spd;
    }

    public double getXPos() {
        return controls.getValue(TEControlTag.XPOS);
    }

    public double getYPos() {
        return controls.getValue(TEControlTag.YPOS);
    }

    public double getSize() {
        return controls.getValue(TEControlTag.SIZE);
    }

    public double getQuantity() {
        return controls.getValue(TEControlTag.QUANTITY);
    }

    /**
     * For most uses, getRotationAngle() is recommended, but if you
     * need direct access to the spin control value, here it is.
     */
    public double getSpin() {
        return controls.getValue(TEControlTag.SPIN);
    }

    /**
     * <b>NOTE:</b> This control has functional overlap with color and channel fader settings, and
     * could potentially cause confusing brightness behavior.
     *
     * <b>It may be deprecated or removed in the future and should not be used in patterns.</b>
     *
     * @return The current value of the brightness control, by default in the range 0.0 to 1.0
     */
    public double getBrightness() {
        return controls.getValue(TEControlTag.BRIGHTNESS);
    }

    public double getWow1() {
        return controls.getValue(TEControlTag.WOW1);
    }

    public double getWow2() {
        return controls.getValue(TEControlTag.WOW2);
    }

    public boolean getWowTrigger() {
        return controls.getValue(TEControlTag.WOWTRIGGER) > 0.0;
    }

    public double getExplode() {
        return controls.getValue(TEControlTag.EXPLODE);
    }

    /**
     * Restarts the specified timer's elapsed time when called.
     * The timer's rate is not changed.
     * <p>
     * This is useful for syncing a timer precisely to beats,
     * measures and other external events.
     *
     * @param tag - the tag of the control to be retriggered.  Only
     *            works timer-linked controls - SPEED and SPIN at
     *            present.
     */
    public void retrigger(TEControlTag tag) {
        switch (tag) {
            case SPEED:
                iTime.reset();
                break;
            case SPIN:
                spinTimer.reset();
                break;
            default:
                // TE.log("retrigger: Invalid parameter.");
                break;
        }
    }

    private final LXParameterListener wowTriggerListener = (p) -> {
        onWowTrigger(getWowTrigger());
    };

    /**
     * Subclasses can override
     */
    protected void onWowTrigger(boolean on) {}

    /**
     * To be called from the constructor of automatically configured shader patterns prior
     * to calling addCommonControls(). Calls common control functions based on a list of
     * opcodes and parameters extracted from shader code by our GLSL preparser.
     *
     * @param shaderConfig list of configuration operations from the shader
     */
    protected void configureCommonControls(List<ShaderConfiguration> shaderConfig) {
        for (ShaderConfiguration config : shaderConfig) {
            switch (config.opcode) {
                case SET_VALUE:
                    controls.setValue(config.parameterId, config.value);
                    break;
                case SET_RANGE:
                    controls.setRange(config.parameterId, config.value, config.v1, config.v2);
                    break;
                case SET_LABEL:
                    controls.setLabel(config.parameterId, config.name);
                    break;
                case SET_EXPONENT:
                    controls.setExponent(config.parameterId, config.value);
                    break;
                case SET_NORMALIZATION_CURVE:
                    controls.setNormalizationCurve(config.parameterId, config.normalizationCurve);
                    break;
                case DISABLE:
                    controls.markUnused(controls.getLXControl(config.parameterId));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    @Override
    protected void run(double deltaMs) {
        // get the current tempo in beats per second
        double bps = lx.engine.tempo.bpm() / 60;

        // Spin control
        double value = getSpin() * bps;
        spinTimer.setScale(value);
        spinTimer.tick();
        spinRotor.updateAngle(spinTimer.getTime(), value);

        // Speed control
        // To calculate timescale for speed, we multiply the control value
        // by the engine bpm (converted to beats/sec).  This makes the core
        // speed clock rate 1 virtual second per beat - quarter note pace in
        // 4/4 time signature, which is then modified by the Speed UI control.
        //
        // This makes tempo syncing in patterns a LOT easier, with no extra parameters,
        // controls or efforts.  You can set speed for specific time divisions as follows:
        // speed = 0.25 is whole notes
        // speed = 0.5 is half notes
        // speed = 1 is quarter notes
        // speed = 2 is eighth notes
        // speed = 3 is eighth note triplets
        // speed = 4 (the default maximum) is 16th notes
        //
        // If you need to go faster than 16ths in a pattern, expand the range with setRange()
        // in your constructor.  Of course, other speeds work too, and can create
        // interesting syncopated visuals.
        value = getSpeed() * bps;
        iTime.setScale(value);
        iTime.tick();
        speedRotor.updateAngle(iTime.getTime(), value);

        // Gradients always need to be up to date for TEColorParameter
        updateGradients();
        expireColors();

        super.run(deltaMs);
    }

    @Override
    public void dispose() {
        this.controls.getLXControl(TEControlTag.WOWTRIGGER).removeListener(wowTriggerListener);
        this.controls.dispose();
        super.dispose();
    }
}
