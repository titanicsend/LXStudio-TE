package titanicsend.pattern.jon;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;

import java.nio.ByteBuffer;

import me.walkerknapp.devolay.*;
import titanicsend.ndi.NDIEngine;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

@LXCategory("AAAardvark")
public class NDIReceiverTest extends GLShaderPattern {
  private NDIEngine ndiEngine = null;

  DevolayVideoFrame videoFrame;
  DevolayReceiver receiver = null;

  ByteBuffer buffer;
  TextureData textureData = null;
  Texture texture;
  GLShader shader;

  // simple demo of multipass rendering
  public NDIReceiverTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    if (ndiEngine == null) {
      ndiEngine = (NDIEngine) lx.engine.getChild(NDIEngine.PATH);
      // TE.log("Shader: Retrieved NDIEngine from LX");

      // Create frame objects to handle incoming video stream
      // TODO - can we leave the audio frame out?
      videoFrame = new DevolayVideoFrame();
      ndiEngine.printSourceNames();
    }

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

    // add the first shader, passing in the shared backbuffer
    shader = new GLShader(lx, "nditest.fs", this, buffer);
    addShader(shader,        new GLShaderFrameSetup() {
      @Override
      public void OnFrame(GLShader s) {

        if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null,
          null, 0)) {
            // get pixel data from video frame
            ByteBuffer frameData = videoFrame.getData();

            // if it's the first frame, build our texture data object
            // otherwise, just replace the buffer with the current
            // frame's pixel data.
            if (textureData == null) {
              int width = videoFrame.getXResolution();
              int height = videoFrame.getYResolution();
              int format = GL.GL_RGBA;
              int type = GL.GL_UNSIGNED_BYTE;
              textureData = new TextureData(s.getGLProfile(), format, width, height,
                0, GL.GL_BGRA, type, false, false, false, frameData, null);
              texture = TextureIO.newTexture(textureData);
            } else {
              textureData.setBuffer(frameData);
              texture.updateImage(s.getGL4(), textureData);
            }

            // pass the texture to the shader
            s.setUniform("ndivideo", texture);
        }
      }
    });

    shader = new GLShader(lx, "sobel.fs", this, buffer);
    addShader(shader);
  }

  @Override
  public void onActive() {
    super.onActive();
    // if no receiver yet, create one.  Otherwise connect to the
    // previously connected source.
    if (receiver == null) {
      receiver = new DevolayReceiver(ndiEngine.sources[0], DevolayReceiver.ColorFormat.BGRX_BGRA, RECEIVE_BANDWIDTH_HIGHEST, true, "TE");
    }
    else {
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
    receiver.close();
    videoFrame.close();
    super.dispose();
  }

}
