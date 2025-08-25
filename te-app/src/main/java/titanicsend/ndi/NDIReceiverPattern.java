package titanicsend.ndi;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.preset.UIUserPresetCollection;
import titanicsend.ui.UIMFTControls;

/** Receives video over NDI */
@LXCategory("NDI")
@LXComponentName("NDI Receiver")
public class NDIReceiverPattern extends GLShaderPattern
    implements GpuDevice, UIDeviceControls<NDIReceiverPattern> {

  private final NDIEngine ndi;

  protected final DevolayReceiver receiver;
  protected final DevolayVideoFrame videoFrame;
  protected int frameWidth;
  protected int frameHeight;

  protected boolean lastConnectState = false;
  protected long connectTimer = 0;

  protected final ByteBuffer buffer;
  protected TextureData textureData = null;
  protected Texture texture = null;
  protected GL4 gl4;

  public final StringParameter source =
      new StringParameter("Source", "")
          .setDescription("Name of the NDI stream we are currently receiving");

  public final NDIEngine.Selector sources;

  public final TriggerParameter select =
      new TriggerParameter("Select", this::onSelect)
          .setDescription("Receive from the currently selected item in the Sources list");

  private void onSelect() {
    String sourceName = this.sources.getObject();
    if (sourceName != null) {
      if (!sourceName.equals(this.source.getString())) {
        this.source.setValue(sourceName);
      } else {
        // Button was clicked but name was already selected.
        // Force reconnect by name.
        this.source.bang();
      }
    }
  }

  protected final LXListenableNormalizedParameter gain =
      new CompoundParameter("Gain", 1, 0.5, 2).setDescription("Video gain");

  public LXNormalizedParameter getSourceControl() {
    return sources;
  }

  public LXNormalizedParameter getGainControl() {
    return gain;
  }

  public NDIReceiverPattern(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    this.ndi = NDIEngine.get();

    this.receiver =
        new DevolayReceiver(
            DevolayReceiver.ColorFormat.BGRX_BGRA, RECEIVE_BANDWIDTH_HIGHEST, true, "TE");

    // Create frame objects to handle incoming video stream
    // (note that we are omitting audio and metadata frames for now)
    videoFrame = new DevolayVideoFrame();

    // set scale control to something that works for video.
    controls.setRange(TEControlTag.SIZE, 1, 5, 0.1);

    // allocate a backbuffer for all the shaders to share
    buffer = TEShader.allocateBackBuffer();

    // add the primary shader, which handles mapping incoming video frames
    // from the NDI source to the shared buffer.
    addShader(
        GLShader.config(lx)
            .withFilename("ndidefault.fs")
            .withUniformSource(this::setUniforms)
            .withLegacyBackBuffer(buffer));

    addCommonControls();

    addParameter("source", this.source);
    addParameter("sources", this.sources = this.ndi.newSourceSelector());
    addParameter("select", this.select);
    addParameter("gain", this.gain);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    controls.markUnused(this.sources);
    controls.markUnused(this.select);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);

    // If the [string] source parameter changed, attempt to connect by name
    if (p == this.source) {
      reconnect();
    }
  }

  private void reconnect() {
    String s = this.source.getString();
    if (!LXUtils.isEmpty(s)) {
      this.lastConnectState = this.ndi.connectByName(s, receiver);
    }
  }

  private void disconnect() {
    this.receiver.connect(null);
  }

  @Override
  public void onActive() {
    super.onActive();
    reconnect();
  }

  @Override
  public void onInactive() {
    disconnect();
    super.onInactive();
  }

  /**
   * Create a texture (and associated Java objects) appropriately dimensioned the current incoming
   * video frame size. This is called on initial connection, and whenever the frame size changes.
   */
  public void createTextureForFrame() {

    // free the previous GPU texture if it exists
    if (texture != null) {
      texture.destroy(gl4);
    }

    // save dimensions of the current frame
    frameWidth = videoFrame.getXResolution();
    frameHeight = videoFrame.getYResolution();

    int format = GL.GL_RGBA;
    int type = GL.GL_UNSIGNED_BYTE;
    textureData =
        new TextureData(
            gl4.getGLProfile(),
            format,
            frameWidth,
            frameHeight,
            0,
            GL.GL_BGRA,
            type,
            false,
            false,
            false,
            videoFrame.getData(),
            null);
    texture = TextureIO.newTexture(textureData);
  }

  private void setUniforms(GLShader s) {
    // If not connected, regularly attempt reconnect by name
    if (!lastConnectState) {
      if (System.currentTimeMillis() - connectTimer > 1000) {
        connectTimer = System.currentTimeMillis();
        reconnect();
      }
    }

    if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null, null, 0)) {
      // get pixel data from video frame
      ByteBuffer frameData = videoFrame.getData();

      // if it's the first frame, or if the frame dimensions changed,
      // build our texture data object. Otherwise, just replace the
      // buffer with the current frame's pixel data.
      if (textureData == null
          || frameWidth != videoFrame.getXResolution()
          || frameHeight != videoFrame.getYResolution()) {
        gl4 = s.getGL4();
        createTextureForFrame();
      } else {
        textureData.setBuffer(frameData);
        texture.updateImage(gl4, textureData);
      }
      // pass the video frame texture to the shader
      s.setUniform("gain", gain.getValuef());
      s.setUniform("ndivideo", texture);
    }
  }

  @Override
  public void dispose() {
    // shut down receiver and free everything we allocated
    disconnect();
    this.videoFrame.close();

    if (texture != null) {
      texture.destroy(gl4);
    }

    super.dispose();
  }

  /** Device UI */
  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, NDIReceiverPattern device) {
    uiDevice.setLayout(UI2dContainer.Layout.NONE);

    // Remote controls, MFT-layout
    UIMFTControls mftControls = new UIMFTControls(ui, device, uiDevice.getContentHeight());

    // NDI source controls
    newNDIcontrols(ui, device).addToContainer(mftControls);

    // User Presets list
    UIUserPresetCollection presets =
        (UIUserPresetCollection)
            new UIUserPresetCollection(ui, device, uiDevice.getContentHeight())
                .setX(mftControls.getContentWidth() + 4);

    uiDevice.addChildren(mftControls, presets);

    uiDevice.setContentWidth(presets.getX() + presets.getWidth());
  }

  private UI2dContainer newNDIcontrols(LXStudio.UI ui, NDIReceiverPattern device) {
    UI2dContainer uiNDI = new UI2dContainer(184, 50, 160, 0);
    uiNDI.setLayout(UI2dContainer.Layout.VERTICAL, 4);

    uiNDI.addChildren(
        new UILabel(100, "Selected NDI Source:").setFont(ui.theme.getControlFont()),
        new UITextBox(150, 16, device.source).setEditable(false),
        new UILabel(100, "Available Sources:").setFont(ui.theme.getControlFont()),
        UI2dContainer.newHorizontalContainer(
            16,
            4,
            new UIDropMenu(116, device.sources).setMenuWidth(166),
            new UIButton(46, 16, device.select)));

    return uiNDI;
  }
}
