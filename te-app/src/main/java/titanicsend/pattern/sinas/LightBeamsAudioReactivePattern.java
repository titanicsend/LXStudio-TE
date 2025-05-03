package titanicsend.pattern.sinas;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.GaussianFilter;

@LXCategory("Native Shaders Panels")
public class LightBeamsAudioReactivePattern extends ConstructedShaderPattern {
  // Internally keeps track of what rotation should be applied to the shader texture.
  private float degrees = 0;

  // Kept internally to keep track of the time difference applied to the speed of the shader
  // based on the trebleLevel.
  private float timeDiff = 0;

  // Gaussian filter used to smoothen out the bassLevel
  private GaussianFilter degreesFilter = new GaussianFilter(10);

  // Gaussian filter used to smoothen out the trebleLevel
  private GaussianFilter timeDiffFilter = new GaussianFilter(20);

  public LightBeamsAudioReactivePattern(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
  }

  @Override
  protected void createShader() {
    controls.setRange(TEControlTag.SPEED, 0.02, -2, 2);
    controls.setRange(TEControlTag.SPIN, 0.05, -1.0, 1.0);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 1.2, 0, 2);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.01, 0, 1);

    addShader("light_beams.fs", this::setUniforms);
  }

  private void setUniforms(GLShader s) {
    // Update the pattern local control values based on the UI values.
    float levelReactivityControl =
        (float) getControls().getControl(TEControlTag.LEVELREACTIVITY).getValue();
    float freqReactivityControl =
        (float) getControls().getControl(TEControlTag.FREQREACTIVITY).getValue();

    // Get the rotation angle from the spin. This is the default value for the
    // iRotationAngle and we're going to apply a diff on this value based on the
    // bassLevel.
    double radians = getRotationAngleFromSpin();
    int spinControlSign =
        Float.compare((float) getControls().getLXControl(TEControlTag.SPIN).getValue(), 0.0f);
    radians += getRotationDiff(levelReactivityControl) * spinControlSign;
    s.setUniform("iRotationAngle", (float) radians);

    // Similar to the rotation, but for speed instead.
    double currentTime = getTime();
    currentTime += getTimeDiff(freqReactivityControl);
    s.setUniform("iTime", (float) currentTime);
  }

  /***
   * This function updates the internal degree value based on the audio signals.
   * @return
   */
  private float getRotationDiff(float levelReactivityControl) {
    // Use the GaussianFilter to smppthen the bassLevel signal
    double filteredBassLevel =
        degreesFilter.applyGaussianFilter((float) bassLevel * levelReactivityControl);
    // Amplify the filtered level by 5 so it is more visible.
    float degreeStep = (float) (filteredBassLevel * 5);

    // Update the internal degree value.
    degrees -= degreeStep;
    degrees = normalizeDegree(degrees);

    return degreesToRadians(degrees);
  }

  /**
   * Utility function to convert degrees to radians.
   *
   * @param degrees
   * @return degree converted to radian.
   */
  private float degreesToRadians(float degrees) {
    return (float) (degrees * Math.PI / 180.0);
  }

  /**
   * Function used to normalize the degree values. This normalization keeps the value between [0,
   * 360] and is not affecting the visuals. Users can call this to keep the degree values sane and
   * avoid potential overflow problems.
   *
   * @param degree The angel in degrees
   * @return the degree brought back to 0-360
   */
  private float normalizeDegree(double degree) {
    return (degrees % 360 + 360) % 360;
  }

  private float getTimeDiff(float freqReactivityControl) {
    float filtered =
        (float) timeDiffFilter.applyGaussianFilter(trebleLevel * freqReactivityControl);
    timeDiff += filtered;

    return timeDiff;
  }
}
