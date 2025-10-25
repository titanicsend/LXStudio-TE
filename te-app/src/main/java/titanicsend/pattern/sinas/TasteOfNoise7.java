package titanicsend.pattern.sinas;

import java.nio.ByteBuffer;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * TasteOfNoise7 is a single-pass port of Leon Denise's "taste of noise 7" Shadertoy shader.
 * It relies on the built-in backbuffer for temporal feedback, so no extra passes are required.
 */
@LXCategory("Noise")
public class TasteOfNoise7 extends GLShaderPattern {
    ByteBuffer buffer;

  public TasteOfNoise7(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    // controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    // controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    // controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));

    addCommonControls();

    addShader("taste_of_noise7.fs");
  }
}
