package titanicsend.pattern.glengine.mixer;

import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_RGBA;
import static com.jogamp.opengl.GL.GL_RGBA8;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static titanicsend.pattern.glengine.GLShaderPattern.NO_TEXTURE;

import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import heronarts.lx.LXEngine;
import heronarts.lx.ModelBuffer;
import heronarts.lx.blend.LXBlend;
import heronarts.lx.blend.MultiplyBlend;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.mixer.LXMasterBus;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import titanicsend.ndi.NDIOutShaderEffect;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.GLShaderEffect;
import titanicsend.pattern.glengine.GLShaderPattern;

/** Experimental OpenGL-based LX mixer */
public class GLMixer implements LXMixerEngine.Listener, LXMixerEngine.PostMixer {

  private static final int UNINITIALIZED = -1;

  private final LX lx;
  private final GLEngine glEngine;
  private GL4 gl4;

  private boolean initialized = false;

  // Default starting texture for buses
  private int blackBackground = UNINITIALIZED;

  // Dummy CPU buffer for Java effects in GPU mode
  private final ModelBuffer dummyBuffer;

  // Wrapper classes around LX channels
  private final GLMasterBus glMasterBus;
  private final Map<LXAbstractChannel, GLAbstractChannel> channelMap = new HashMap<>();
  private final List<GLAbstractChannel> glChannels = new ArrayList<>();

  // Cue/Aux preview buses
  private final GLPreviewBus glCueBus;
  private final GLPreviewBus glAuxBus;
  private boolean cueBusActive = false;
  private boolean auxBusActive = false;
  private int cueBusTexture = UNINITIALIZED;
  private int auxBusTexture = UNINITIALIZED;

  public GLMixer(LX lx, GLEngine glEngine) {
    this.lx = lx;
    this.glEngine = glEngine;

    this.dummyBuffer = new ModelBuffer(lx);

    this.glMasterBus = new GLMasterBus(lx.engine.mixer.masterBus);
    this.glCueBus = new GLPreviewBus(false);
    this.glAuxBus = new GLPreviewBus(true);

    // Register for channels added/removed
    this.lx.engine.mixer.addListener(this);
  }

  public void initialize(GL4 gl4) {
    if (this.initialized) {
      throw new IllegalStateException("GLMixer was already initialized");
    }
    this.initialized = true;
    this.gl4 = gl4;

    // Create the default starting texture for buses
    ByteBuffer blackBuffer =
        ByteBuffer.allocateDirect(this.glEngine.getWidth() * this.glEngine.getHeight() * 4);
    blackBuffer.rewind();
    int[] backgroundHandles = new int[1];
    gl4.glGenTextures(1, backgroundHandles, 0);
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glBindTexture(GL_TEXTURE_2D, backgroundHandles[0]);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    gl4.glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL_RGBA8,
        this.glEngine.getWidth(),
        this.glEngine.getHeight(),
        0,
        GL_RGBA,
        GL_UNSIGNED_BYTE,
        blackBuffer);
    this.blackBackground = backgroundHandles[0];

    // Initialize shader programs for any channels that were already created
    this.glMasterBus.init();
    for (GLAbstractChannel glChannel : this.glChannels) {
      glChannel.init();
    }

    // Initialize preview shaders
    this.glCueBus.init();
    this.glAuxBus.init();

    // Register GPU mixer
    this.lx.engine.mixer.addPostMixer(this);
  }

  public void loop(double deltaMs) {
    // This will be called [by GLEngine] every engine frame prior to the LXMixer running.
    // Do any pre-run setup here.

    // Patterns will be looped after this
  }

  /**
   * This is an experimental callback from the LX Mixer, allowing us to run our own mixer routine
   * and write the output to LX's (CPU) buffers for the main mix, primary cue, and aux cue.
   */
  @Override
  public void postMix(LXEngine.Frame frame, double deltaMs) {
    if (!this.initialized) {
      throw new IllegalStateException("GLMixer was not initialized");
    }

    // Patterns have been looped. Perform GPU mixing.

    // Reset cue/aux active flags every frame
    boolean cueWasActive = this.cueBusActive;
    boolean auxWasActive = this.auxBusActive;
    this.cueBusActive = false;
    this.auxBusActive = false;

    // Set the target CPU buffers
    this.glMasterBus.setBuffer(frame.getMain());
    this.glCueBus.setBuffer(frame.getCue());
    this.glAuxBus.setBuffer(frame.getAux());

    // Recursive run the buses
    this.glMasterBus.blend(deltaMs, this.blackBackground);
    this.glCueBus.blend(deltaMs, this.blackBackground);
    this.glAuxBus.blend(deltaMs, this.blackBackground);

    // Activate cues but not on the first frame they're enabled, to avoid stale buffer
    frame.setCueOn(cueWasActive && this.cueBusActive);
    frame.setAuxOn(auxWasActive && this.auxBusActive);

    // Unbind framebuffer, the next GL commands might be out of GLEngine scope...
    this.gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
  }

  /**
   * Checks a channel for inclusion in cue/aux previews
   */
  private void checkForPreview(LXAbstractChannel channel, GLBus bus) {
    if (channel.cueActive.isOn() && !this.cueBusActive) {
      // Preview the first bus cued, per frame.
      this.cueBusActive = true;
      this.cueBusTexture = bus.getSrcTexture();
    }
    if (channel.auxActive.isOn() && !this.auxBusActive) {
      // Preview the first bus aux cued, per frame.
      this.auxBusActive = true;
      this.auxBusTexture = bus.getSrcTexture();
    }
  }

  // LXMixerEngine.Listener

  @Override
  public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
    addChannel(channel);
  }

  @Override
  public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
    removeChannel(channel);
  }

  @Override
  public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) {
    moveChannel(channel);
  }

  private void addChannel(LXAbstractChannel channel) {
    GLAbstractChannel glAbstractChannel;
    if (channel.isGroup()) {
      glAbstractChannel = new GLGroup((LXGroup) channel);
    } else {
      glAbstractChannel = new GLChannel((LXChannel) channel);
    }
    this.channelMap.put(channel, glAbstractChannel);
    this.glChannels.add(channel.getIndex(), glAbstractChannel);
    // Only initialize shader if GLMixer was initialized (which sets GL context)
    if (this.initialized) {
      glAbstractChannel.init();
    }
  }

  private void removeChannel(LXAbstractChannel channel) {
    GLAbstractChannel glChannel = this.channelMap.remove(channel);
    if (glChannel != null) {
      this.glChannels.remove(glChannel);
      glChannel.dispose();
    }
  }

  private void moveChannel(LXAbstractChannel channel) {
    // Keep glChannels list ordered the same as LXMixer channels
    GLAbstractChannel glChannel = this.channelMap.get(channel);
    this.glChannels.remove(glChannel);
    this.glChannels.add(channel.getIndex(), glChannel);
  }

  public void dispose() {
    this.lx.engine.mixer.removeListener(this);

    // Dispose channel shaders
    this.glMasterBus.dispose();
    for (LXAbstractChannel channel : new ArrayList<>(this.channelMap.keySet())) {
      removeChannel(channel);
    }

    // Dispose preview shaders
    this.glCueBus.dispose();
    this.glAuxBus.dispose();

    if (this.initialized) {
      this.lx.engine.mixer.removePostMixer(this);
    }
  }

  // Wrapper classes

  private abstract class GLBus {

    protected final LXBus bus;

    private int lastSrc = UNINITIALIZED;

    public GLBus(LXBus bus) {
      this.bus = bus;
    }

    /** Blend down the bus onto the dst texture, returning the output texture handle */
    final int blend(double deltaMs, int dst) {
      // Fast-out if no blending or effect looping
      if (!isActive()) {
        return dst;
      }

      // Future expansion note: run geometry-manipulation effects here, *then* loop patterns.

      // Composite contents (If this is a group, subchannels. Or if this is a channel, patterns.)
      int src = blendContents(deltaMs);

      // Run Effects
      src = loopEffects(deltaMs, src, bus.effects);

      // Remember the pre-fader texture for preview buses
      this.lastSrc = src;

      // Blend the bus texture onto the dst texture
      return finalBlend(dst, src);
    }

    /**
     * Whether the bus should be looped and blended. Even with fader down, effects should be looped
     * unless automute is on.
     */
    protected abstract boolean isActive();

    /** Blend the bus contents (either channels or patterns), but do not yet run effects. */
    protected abstract int blendContents(double deltaMs);

    protected final int loopEffects(double deltaMs, int dst, List<LXEffect> effects) {
      for (LXEffect effect : effects) {
        // TODO: loop effect for damping even if disabled
        if (effect.isEnabled()) {
          effect.setBuffer(dummyBuffer);
          if (effect instanceof GLShaderEffect glShaderEffect) {
            // Shader effect in GPU mode
            glShaderEffect.setDst(dst);
            effect.setModel(effect.getModelView());
            effect.loop(deltaMs);
            dst = glShaderEffect.getRenderTexture();
          } else if (effect instanceof NDIOutShaderEffect ndiOutShaderEffect) {
            ndiOutShaderEffect.setDst(dst);
            effect.setModel(effect.getModelView());
            effect.loop(deltaMs);
            // Do not modify dst. Output texture is for NDI sending, not for us.
          } else {
            // Java effect in GPU mode
            // Currently gets looped for processing but the output is not used
            effect.setModel(effect.getModelView());
            effect.loop(deltaMs);
          }
        }
      }
      return dst;
    }

    /** Final step, blend the bus output texture onto the dst texture at the current fader level */
    protected abstract int finalBlend(int dst, int src);

    /** Retrieve the most recent pre-fader texture */
    private int getSrcTexture() {
      return this.lastSrc;
    }

    protected abstract void dispose();
  }

  private class GLMasterBus extends GLBus {

    private final BusShader mainBusShader;

    public GLMasterBus(LXMasterBus masterBus) {
      super(masterBus);
      this.mainBusShader = new BusShader(lx);
    }

    void init() {
      this.mainBusShader.init();
    }

    protected boolean isActive() {
      return true;
    }

    @Override
    protected int blendContents(double deltaMs) {
      int dst = blackBackground;
      // Blend all channels in the mixer
      for (LXAbstractChannel channel : lx.engine.mixer.channels) {
        if (!channel.isInGroup()) {
          GLAbstractChannel glChannel = channelMap.get(channel);
          dst = glChannel.blend(deltaMs, dst);
          checkForPreview(channel, glChannel);
        }
      }
      return dst;
    }

    /** Set the target CPU buffer for BusShader */
    void setBuffer(int[] cpuBuffer) {
      this.mainBusShader.setCpuBuffer(cpuBuffer);
    }

    @Override
    protected int finalBlend(int dst, int src) {
      this.mainBusShader.setSrc(src);
      this.mainBusShader.setLevel(bus.fader.getValuef());
      // Render GPU mixer output to current LX engine frame
      this.mainBusShader.run();

      return this.mainBusShader.getRenderTexture();
    }

    @Override
    protected void dispose() {
      this.mainBusShader.dispose();
    }
  }

  /**
   * Unique, simple bus for cue and aux previews
   */
  private class GLPreviewBus {

    public final boolean isAux;

    private final BusShader previewBusShader;

    public GLPreviewBus(boolean isAux) {
      this.isAux = isAux;
      this.previewBusShader = new BusShader(lx);
    }

    /** Blend down the bus onto the dst texture, returning the output texture handle */
    private int blend(double deltaMs, int dst) {
      // Fast-out if preview bus is not active
      if (!isActive()) {
        return dst;
      }

      // Composite contents
      // int dst = blackBackground;
      int src = this.isAux ? auxBusTexture : cueBusTexture;

      // Blend the bus texture onto the dst texture
      return finalBlend(dst, src);
    }

    private void init() {
      this.previewBusShader.init();
    }

    private boolean isActive() {
      return this.isAux ? auxBusActive : cueBusActive;
    }

    /** Set the target CPU buffer for BusShader */
    void setBuffer(int[] colors) {
      this.previewBusShader.setCpuBuffer(colors);
    }

    private int finalBlend(int dst, int src) {
      this.previewBusShader.setSrc(src);
      this.previewBusShader.setLevel(1);
      // Render GPU mixer output to current LX engine frame
      this.previewBusShader.run();

      return this.previewBusShader.getRenderTexture();
    }

    private void dispose() {
      this.previewBusShader.dispose();
    }
  }

  private abstract class GLAbstractChannel extends GLBus {

    private final LXAbstractChannel abstractChannel;

    private boolean initialized = false;

    protected BlendShader blendShader;

    private final LXParameterListener blendModeListener =
        (p) -> {
          updateBlendShader();
        };

    public GLAbstractChannel(LXAbstractChannel abstractChannel) {
      super(abstractChannel);
      this.abstractChannel = abstractChannel;

      // Listen for changes to blendMode, update transitionShader
      this.abstractChannel.blendMode.addListener(this.blendModeListener, true);
    }

    void updateBlendShader() {
      if (this.blendShader != null) {
        this.blendShader.dispose();
      }

      String shaderName;
      LXBlend lxB = this.abstractChannel.blendMode.getObject();
      if (lxB instanceof MultiplyBlend) {
        shaderName = "blendMultiply.fs";
      } else {
        shaderName = "blendAdd.fs";
      }

      this.blendShader = new BlendShader(BlendShader.config(lx).withFilename(shaderName));
      if (this.initialized) {
        this.blendShader.init();
      }
    }

    void init() {
      if (this.initialized) {
        throw new IllegalStateException("GLAbstractChannel was already initialized");
      }
      this.initialized = true;

      this.blendShader.init();
    }

    protected final boolean isActive() {
      return this.abstractChannel.cueActive.isOn() ||
        this.abstractChannel.auxActive.isOn() ||
        (this.abstractChannel.enabled.isOn() && !this.abstractChannel.isAutoMuted.isOn());
    }

    @Override
    protected int finalBlend(int dst, int src) {
      this.blendShader.setDst(dst);
      this.blendShader.setSrc(src);
      this.blendShader.setLevel(bus.fader.getValuef());
      this.blendShader.run();

      return this.blendShader.getRenderTexture();
    }

    @Override
    protected void dispose() {
      this.abstractChannel.blendMode.removeListener(this.blendModeListener);
      this.blendShader.dispose();
    }
  }

  private class GLGroup extends GLAbstractChannel {

    private final LXGroup group;

    public GLGroup(LXGroup group) {
      super(group);
      this.group = group;
    }

    @Override
    protected int blendContents(double deltaMs) {
      int dst = blackBackground;
      // Blend all channels in the group
      for (LXAbstractChannel channel : this.group.channels) {
        GLAbstractChannel glChannel = channelMap.get(channel);
        dst = glChannel.blend(deltaMs, dst);
        checkForPreview(channel, glChannel);
      }
      return dst;
    }
  }

  private class GLChannel extends GLAbstractChannel {

    private final LXChannel channel;

    private final BlendShader transitionShader;

    public GLChannel(LXChannel channel) {
      super(channel);
      this.channel = channel;

      this.transitionShader =
          new BlendShader(BlendShader.config(lx).withFilename("blendDissolve.fs"));
    }

    void init() {
      super.init();
      this.transitionShader.init();
    }

    // Get output of active pattern or patterns within a channel. Do not apply channel-level effects
    @Override
    protected int blendContents(double deltaMs) {
      // Fast-out if no patterns
      if (this.channel.patterns.isEmpty()) {
        return blackBackground;
      }

      // Blend patterns
      if (this.channel.isComposite()) {
        return compositePatterns();
      } else if (this.channel.isInTransition()) {
        return transition(deltaMs);
      } else {
        return getPatternTexture(deltaMs, this.channel.getActivePattern());
      }
    }

    private static boolean needsCompositeWarning = true;

    private int compositePatterns() {
      // TODO: composite all patterns in channel
      if (needsCompositeWarning) {
        needsCompositeWarning = false;
        LX.error("Composite channels are not yet implemented in GLMixer");
      }
      return blackBackground;
    }

    private int transition(double deltaMs) {
      // Get both patterns and blend them using the transition blend
      float transitionProgress = (float) this.channel.getTransitionProgress();
      int activeTexture = getPatternTexture(deltaMs, this.channel.getActivePattern());
      int nextTexture = getPatternTexture(deltaMs, this.channel.getNextPattern());
      this.transitionShader.setSrc(nextTexture);
      this.transitionShader.setDst(activeTexture);
      this.transitionShader.setLevel(transitionProgress);
      this.transitionShader.run();
      return this.transitionShader.getRenderTexture();
    }

    private int getPatternTexture(double deltaMs, LXPattern pattern) {
      int patternTexture = blackBackground;

      // Get output texture from pattern, if any
      if (pattern instanceof GLShaderPattern glShaderPattern) {
        int renderTexture = glShaderPattern.getRenderTexture();
        if (renderTexture != NO_TEXTURE) {
          patternTexture = renderTexture;
        }
      }

      // Loop pattern-level effects
      if (pattern != null) {
        patternTexture = loopEffects(deltaMs, patternTexture, pattern.effects);
      }

      return patternTexture;
    }

    @Override
    protected void dispose() {
      this.transitionShader.dispose();
      super.dispose();
    }
  }
}
