package titanicsend.ndi;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.nio.ByteBuffer;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

public class NDIPattern extends GLShaderPattern {

  protected NDIEngine ndiEngine = null;

  protected DevolayVideoFrame videoFrame = null;
  protected DevolayReceiver receiver = null;
  protected int frameWidth;
  protected int frameHeight;

  protected ByteBuffer buffer = null;
  protected TextureData textureData = null;
  protected Texture texture = null;
  protected GL4 gl4;

  public NDIPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public NDIPattern(LX lx, TEShaderView view) {
    super(lx, view);

    if (ndiEngine == null) {
      ndiEngine = (NDIEngine) lx.engine.getChild(NDIEngine.PATH);
      // TE.log("Shader: Retrieved NDIEngine from LX");

      // Create frame objects to handle incoming video stream
      // (note that we are omitting audio and metadata for now)
      videoFrame = new DevolayVideoFrame();
      ndiEngine.printSourceNames();
    }

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

    // add the primary shader, which handles mapping incoming video frames
    // from the NDI source to the shared buffer.
    addPrimaryShader();
  }

  /**
   * The primary shader handles incoming video frames from the NDI source. Your pattern can override
   * this method if you need something other than the default behavior. (Xpos/Ypos, Spin, Angle,
   * Size, Brightness, Explode work as expected, Wow1 controls the full color vs. palette color mix)
   */
  public void addPrimaryShader() {
    GLShader shader = new GLShader(lx, "nditest.fs", this, buffer);
    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {

            if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null, null, 0)) {
              // get pixel data from video frame
              ByteBuffer frameData = videoFrame.getData();

              // TODO - create new textureData if the frame size changes

              // if it's the first frame, build our texture data object
              // otherwise, just replace the buffer with the current
              // frame's pixel data.
              if (textureData == null) {
                gl4 = s.getGL4();

                int width = videoFrame.getXResolution();
                int height = videoFrame.getYResolution();
                int format = GL.GL_RGBA;
                int type = GL.GL_UNSIGNED_BYTE;
                textureData =
                    new TextureData(
                        s.getGLProfile(),
                        format,
                        width,
                        height,
                        0,
                        GL.GL_BGRA,
                        type,
                        false,
                        false,
                        false,
                        frameData,
                        null);
                texture = TextureIO.newTexture(textureData);
              } else {
                textureData.setBuffer(frameData);
                texture.updateImage(gl4, textureData);
              }

              // pass the texture to the shader
              s.setUniform("ndivideo", texture);
            }
          }
        });
  }

  @Override
  public void onActive() {
    super.onActive();
    // if no receiver yet, create one. Otherwise connect to the
    // previously connected source.
    if (receiver == null) {
      receiver =
          new DevolayReceiver(
              ndiEngine.sources[0],
              DevolayReceiver.ColorFormat.BGRX_BGRA,
              RECEIVE_BANDWIDTH_HIGHEST,
              true,
              "TE");
    } else {
      receiver.connect(ndiEngine.sources[0]);
    }
  }

  @Override
  public void onInactive() {
    // disconnect receiver from all sources
    receiver.connect(null);
    super.onInactive();
  }

  @Override
  public void dispose() {
    // shut down receiver and free everything
    // we allocated.
    if (receiver != null) {
      receiver.connect(null);
    }
    if (videoFrame != null) {
      videoFrame.close();
    }
    if (texture != null)
      texture.destroy(gl4);

    super.dispose();
  }
}
