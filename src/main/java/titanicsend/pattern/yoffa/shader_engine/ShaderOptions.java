package titanicsend.pattern.yoffa.shader_engine;

/**
 *  Options that control shader data and uniforms.  To be passed to the NativeShader constructor.
 *  By default, the options are set to:
 *  <br>
 *    Alpha(false) - alpha data from shader will not be used <br>
 *    WaveData(true) - waveform data from engine will be sent to shader <br>
 *    LXParameterUniforms(true) - the pattern's LX parameters will be sent to the shader as custom uniforms <br>
 */
public class ShaderOptions {
    // if true, return the alpha channel from shader output
    // if false, ignore shader alpha and return 255 (fully opaque)
    private boolean useAlpha;

    // if true, copy current fft and waveform data to corresponding uniforms
    // on each frame.
    // if false, skip the copy to save a little time on shaders that don't use the data
    private boolean useWaveData;

    // if true, automatically export the pattern's LX parameters as uniforms
    // if false, don't. (this allows the pattern writer to build custom uniforms
    // from the parameters without having redundant data sent to the shader.  (Some
    // calculations are better done per frame than per pixel, even on hardware.)
    private boolean useLXParameters;

    public ShaderOptions() {
        useAlpha(true);
        useWaveData(true);
        useLXParameterUniforms(true);
    }

    /**
     * Control how the shader's alpha (transparency) data is handled:<br>
     * @param val If true, return the alpha channel from shader output <br>
     * If false, ignore shader alpha and return 255 (fully opaque)
     */
    public void useAlpha(boolean val) { this.useAlpha = val; }

    /**
     * Control the use of live audio data from the LX engine:<br>
     * @param val If true, copy current fft and waveform data to corresponding uniforms
     * on each frame.<br>
     * If false, skip the copy to save a little time on shaders that don't use the data
     */
    public void useWaveData(boolean val) { this.useWaveData = val; }

    /**
     * Control the automatic conversion of the pattern's LX parameters to shader uniforms
     * @param val
     * If true, automatically export the pattern's LX parameters as uniforms<br>
     * If false, don't. (This allows the pattern writer to build custom uniforms
     * from the parameters without having redundant data sent to the shader.  Some
     * calculations are better done per frame than per pixel, even on hardware.)
     */
    public void useLXParameterUniforms(boolean val) { this.useLXParameters = val; }

    /**
     * @return flag indicating whether alpha (tranparency) values returned from the
     * shader should be used.
     */
    public boolean getAlpha() { return this.useAlpha; }

    /**
     * @return flag indicating whether live audio data from the LX engine should
     * be sent to the shader as a uniform texture.
     */
    public boolean getWaveData() { return this.useWaveData; }

    /**
     * @return flag indicating whether the active pattern's LX Parameters should
     * be provided to the shader as uniforms
     */
    public boolean getLXParameterUniforms() { return this.useLXParameters; }

}
