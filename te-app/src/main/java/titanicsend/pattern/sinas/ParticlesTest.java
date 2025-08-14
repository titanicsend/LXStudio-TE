package titanicsend.pattern.sinas;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class ParticlesTest extends GLShaderPattern {
  private TEShader emitShader;
  private TEShader renderShader;

  public ParticlesTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // quantity controls number of particles
    controls
        .setRange(TEControlTag.QUANTITY, 2, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    // size controls particle size when rendering (bigger circles)
    controls.setRange(TEControlTag.SIZE, 0.15, 0.05, 0.5);

    // speed affects gravity strength
    controls.setRange(TEControlTag.SPEED, 1.0, 0.1, 5.0);

    // wow1 controls particle lifetime
    controls.setRange(TEControlTag.WOW1, 5.0, 1.0, 20.0);

    // wow2 controls damping factor
    controls.setRange(TEControlTag.WOW2, 0.98, 0.8, 1.0);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    // Keep wowtrigger for particle reset

    // register common controls with LX
    addCommonControls();

    // Modern multipass: manual chaining with custom uniform source
    this.emitShader =
        addShader(
            GLShader.config(lx)
                .withFilename("particles_emit.fs")
                .withUniformSource(this::setCustomUniforms));
    this.renderShader =
        addShader(
            GLShader.config(lx)
                .withFilename("particles_render.fs")
                .withUniformSource(this::setCustomUniforms));
  }

  // Track the current texture being passed between shaders
  private int currentBackbuffer = -1;

  // Particle state: number of particles, state texture is provided by emitter shader's output
  private static final int MAX_PARTICLES = 2; // Fixed for now

  // Custom uniform source to override iBackbuffer and set particle buffer
  private void setCustomUniforms(GLShader shader) {
    // Always bind backbuffer for standard chaining
    if (this.currentBackbuffer != -1) {
      shader.bindTextureUnit(GLShader.TEXTURE_UNIT_BACKBUFFER, this.currentBackbuffer);
      shader.setUniform("iBackbuffer", GLShader.TEXTURE_UNIT_BACKBUFFER);
    }

    // For the render shader, bind the emitter output as state texture
    if (shader == this.renderShader && this.currentBackbuffer != -1) {
      shader.bindTextureUnit(GLShader.FIRST_UNRESERVED_TEXTURE_UNIT, this.currentBackbuffer);
      shader.setUniform("stateTex", GLShader.FIRST_UNRESERVED_TEXTURE_UNIT);
      shader.setUniform("stateRes", MAX_PARTICLES, 2);
      shader.setUniform("iParticleSize", (float) 0.3f);
    }
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    // Safety check: bail if the pattern contains no shaders
    if (this.shaders.isEmpty()) {
      return;
    }

    // No separate particle buffer; emitter output is used as state texture

    // Update the model coords texture only when changed (and the first run)
    if (getModel() != null && getModel().points != null) {
      for (TEShader shader : this.shaders) {
        shader.setModelCoordinates(getModel());
      }
    }

    // Set the CPU buffer for any non-last shader to be null. These will be chained.
    for (int i = 0; i < (this.shaders.size() - 1); i++) {
      this.shaders.get(i).setCpuBuffer(null);
    }
    // Set the CPU buffer for the last shader, if using CPU mixer
    this.shaders.getLast().setCpuBuffer(this.lx.engine.renderMode.cpu ? this.colors : null);

    // Manual shader chaining like GLShaderEffect does:
    // Start with no backbuffer for the first shader (emit)
    this.currentBackbuffer = -1;

    for (int i = 0; i < this.shaders.size(); i++) {
      TEShader shader = this.shaders.get(i);

      // For second+ shaders, set the backbuffer to the previous shader's output
      if (i > 0) {
        this.currentBackbuffer = this.shaders.get(i - 1).getRenderTexture();
      }

      shader.run();
    }
  }
}
