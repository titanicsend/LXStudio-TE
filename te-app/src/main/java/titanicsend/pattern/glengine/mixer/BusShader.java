package titanicsend.pattern.glengine.mixer;

import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;

import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

public class BusShader extends GLShader {

  // Input texture units
  private int textureUnitSrc;

  // Framebuffer object (FBO) for rendering
  private FBO fbo;

  // Pixel Pack Buffers (PBOs) for ping-pong output
  private PingPongPBO ppPBOs;

  // Variables that will be passed to uniforms
  // Source texture handle
  private int src = -1;
  // Amount of src to blend onto a black background
  private float level = 1f;
  // Target CPU buffer
  private int[] main;

  private static class BusUniforms {
    private Uniform.Int1 iSrc;
    private Uniform.Float1 level;
  }

  private final BusUniforms uniforms = new BusUniforms();
  private boolean initializedUniforms = false;

  public BusShader(LX lx) {
    super(config(lx).withFilename("bus.fs"));

    addUniformSource(this::setUniforms);
  }

  @Override
  protected boolean useTEPreProcess() {
    return false;
  }

  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    this.textureUnitSrc = getNextTextureUnit();

    // FBO (framebuffer and texture) for rendering
    this.fbo = new FBO();

    // Pixel Pack Buffers (PBOs) for ping-pong output
    this.ppPBOs = new PingPongPBO();
  }

  public void setLevel(float level) {
    this.level = level;
  }

  public void setMain(int[] main) {
    this.main = main;
  }

  /** Set input texture handle */
  public void setSrc(int src) {
    this.src = src;
  }

  private void initializeUniforms() {
    this.uniforms.iSrc = getUniformInt1("iSrc");
    this.uniforms.level = getUniformFloat1("level");
  }

  private void setUniforms(GLShader s) {
    // Use Uniform objects to track locations and values
    if (!initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms();
    }

    // Bind input textures to texture units
    bindTextureUnit(this.textureUnitSrc, this.src);
    activateDefaultTextureUnit();

    // Stage uniform values for updating
    this.uniforms.iSrc.setValue(this.textureUnitSrc);
    this.uniforms.level.setValue(this.level);
  }

  boolean firstFrame = true;

  @Override
  protected void render() {
    // Bind vertex array object
    bindVAO();

    // Bind framebuffer object (FBO).
    this.fbo.bind();

    // Render frame
    drawElements();

    // Bind the current PBO
    this.ppPBOs.render.bind();

    // Read pixels into current PBO
    this.gl4.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, 0);

    if (firstFrame) {
      // Skip the first frame, PBO is empty
      firstFrame = false;
    } else {

      // Map the other PBO for reading (from previous frame)
      this.ppPBOs.copy.bind();
      ByteBuffer pboData = this.gl4.glMapBuffer(GL4.GL_PIXEL_PACK_BUFFER, GL4.GL_READ_ONLY);

      if (pboData != null) {
        // Copy data from PBO to main array
        pboData.rewind();
        IntBuffer src = pboData.asIntBuffer();
        // Clamp
        int count = Math.min(src.remaining(), this.main.length);
        // Safe copy
        src.get(this.main, 0, count);

        // Unmap the PBO
        this.gl4.glUnmapBuffer(GL4.GL_PIXEL_PACK_BUFFER);
      }
    }

    // Unbind the PBO
    this.gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, 0);

    // Switch to the next PBO for the next frame
    this.ppPBOs.swap();

    unbindTextureUnit(this.textureUnitSrc);
    activateDefaultTextureUnit();

    // No need to unbind VAO.
    // Also not unbinding the FBO here, as other shader render passes will change it.
    // And GLMixer will unbind the last FBO at the end of postMix().
  }

  public int getRenderTexture() {
    return this.fbo.getTextureHandle();
  }

  @Override
  public void dispose() {
    if (isInitialized()) {
      this.fbo.dispose();
      this.ppPBOs.dispose();
    }
    super.dispose();
  }
}
