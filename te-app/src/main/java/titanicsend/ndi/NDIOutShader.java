package titanicsend.ndi;

import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.yoffa.shader_engine.Uniform;
import titanicsend.pattern.yoffa.shader_engine.UniformNames;

public class NDIOutShader extends GLShader implements GLShader.UniformSource {

  private static final int UNINITIALIZED = -1;

  // Framebuffer objects (FBOs) for rendering
  private GLShader.TripleFBO FBOs;

  // Pixel Pack Buffers (PBOs) for ping-pong output
  private GLShader.TriplePBO PBOs;

  private DevolaySender ndiSender;
  private DevolayVideoFrame ndiFrame;
  protected ByteBuffer imageBuffer;

  private int modelCoordsTextureHandle = UNINITIALIZED;

  // Variables that will be passed to uniforms
  // Incoming texture handle
  private int iDst = -1;

  private static class NdiOutUniforms {
    private Uniform.Sampler2D iDst;
    private Uniform.Sampler2D lxModelCoords;
  }

  private final NdiOutUniforms uniforms = new NdiOutUniforms();
  private boolean initializedUniforms = false;

  public NDIOutShader(LX lx) {
    super(config(lx).withFilename("ndi_out_effect.fs"));

    addUniformSource(this);
  }

  @Override
  protected boolean useTEPreProcess() {
    return false;
  }

  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    // FBOs (framebuffer and texture) for rendering
    this.FBOs = new GLShader.TripleFBO();

    // Pixel Pack Buffers (PBOs) for ping-pong output
    this.PBOs = new GLShader.TriplePBO();

    this.imageBuffer = TEShader.allocateBackBuffer();
  }

  /** Set input texture handle */
  public void setDst(int iDst) {
    this.iDst = iDst;
  }

  private void initializeUniforms() {
    this.uniforms.lxModelCoords = getUniformSampler2D(UniformNames.LX_MODEL_COORDS);
    this.uniforms.iDst = getUniformSampler2D("iDst");
    initializeNdiSender();
  }

  private void initializeNdiSender() {
    ndiSender = new DevolaySender("TitanicsEnd");
    ndiFrame = new DevolayVideoFrame();
    ndiFrame.setResolution(width, height);
    ndiFrame.setFourCCType(DevolayFrameFourCCType.RGBA);
    ndiFrame.setData(this.imageBuffer);
    ndiFrame.setFrameRate(60, 1);
    ndiFrame.setAspectRatio(1);
  }

  @Override
  public void setUniforms(GLShader s) {
    // Use Uniform objects to track locations and values
    if (!initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms();
    }

    // Stage uniform values for updating
    this.uniforms.lxModelCoords.setValue(this.modelCoordsTextureHandle);
    this.uniforms.iDst.setValue(this.iDst);
  }

  int frameNum = 0;
  boolean everyOtherFrame = true;

  @Override
  protected void render() {
    // Bind vertex array object
    bindVAO();

    // Bind framebuffer object (FBO).
    this.FBOs.a().bind();

    // Render frame
    drawElements();

    // Start async read of framebuffer into PBO
    this.PBOs.a().startRead();

    if (frameNum < 2) {
      // Skip the first frame, PBO is empty
      frameNum++;
      // Unbind the PBO
      this.gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, 0);
    } else if (this.everyOtherFrame) {

      // Map the other PBO for reading (from previous frame)
      this.imageBuffer = this.PBOs.b().getData();

      if (this.imageBuffer != null) {
        // Send NDI frame
        this.ndiFrame.setData(this.imageBuffer);
        this.ndiSender.sendVideoFrame(this.ndiFrame);
      }

      // PBO is unbound after getData()
    }

    this.everyOtherFrame = !this.everyOtherFrame;

    // Switch to the next PBO for the next frame
    this.FBOs.swap();
    this.PBOs.swap();

    // No need to unbind VAO.
    // Also not unbinding the FBO here, as other shader render passes will change it.
    // And GLMixer will unbind the last FBO at the end of postMix().
  }

  @Override
  public void unbindTextures() {
    this.uniforms.lxModelCoords.unbind();
    this.uniforms.iDst.unbind();
  }

  public int getRenderTexture() {
    return this.iDst;
  }

  // Staging Uniforms: LX Model

  /**
   * Copy LXPoints' normalized coordinates into textures for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered. And should be called
   * by the pattern's frametime run() function if the model has changed since the last frame.
   *
   * @param model Current LXModel of the calling context, which is a LXView or the global model
   */
  public void setModelCoordinates(LXModel model) {
    this.modelCoordsTextureHandle = this.glEngine.textureCache.getCoordinatesTexture(model);
  }

  @Override
  public void dispose() {
    if (isInitialized()) {
      this.FBOs.dispose();
      this.PBOs.dispose();
      this.ndiSender.close();
    }
    super.dispose();
  }
}
