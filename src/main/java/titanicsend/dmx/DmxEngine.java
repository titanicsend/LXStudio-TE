/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.dmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import heronarts.lx.LX;
import heronarts.lx.LXBuffer;
import heronarts.lx.LXLoopTask;
import heronarts.lx.blend.AddBlend;
import heronarts.lx.blend.LXBlend;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.mixer.LXMasterBus;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.output.LXOutput;
import heronarts.lx.studio.TEApp;
import titanicsend.app.dev.DevSwitch;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxWholeModel;
import titanicsend.dmx.parameter.DmxParameter;

/**
 * DmxEngine manages the dmx buffers and mixes the dmx frames.
 * Much of the heavy lifting here is a modified copy of LXMixerEngine.
 *
 * Roles:
 *  - Maintains a reference of one DmxModelBuffer per LXBuffer for consumption by patterns/effects.
 *  - Acts as a DmxMixer
 */
public class DmxEngine implements LXLoopTask {

  private static final boolean ENABLE_DEBUG = false;

  private static DmxEngine current;
  public static DmxEngine get() {
    return current;
  }

  protected final LX lx;

  protected final DmxWholeModel dmxWholeModel;

  /*
   * LXMixerEngine.Listener for releasing buffers when channels are removed
   */

  public class MixerListener implements heronarts.lx.mixer.LXMixerEngine.Listener {
    @Override
    public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) { }

    @Override
    public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      removeChannel(channel);
    }

    @Override
    public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) { }    
  }

  private final MixerListener mixerListener = new MixerListener();

  /*
   * Internal LXOutput class, is called after LXMixerEngine ie patterns have run.
   * Used to start the DMX mixer.
   */

  public class OutputTrigger extends LXOutput {

    private OutputTrigger(LX lx) {
      super(lx);
    }

    @Override
    protected void onSend(int[] colors, GammaTable glut, double brightness) {
      runDmxMixer();
    }    
  }

  private final OutputTrigger outputTrigger;

  // Copied from LXEngine
  // Buffer for a single frame, which was rendered with
  // a particular model state, has a main view along with
  // a cue and auxiliary view, as well as cue/aux view state
  public static class Frame extends DmxFullBuffer {

    private DmxWholeModel model;
    private DmxBuffer[] main = null;
    private DmxBuffer[] cue = null;
    private DmxBuffer[] aux = null;
    private boolean cueOn = false;
    private boolean auxOn = false;

    public Frame(DmxWholeModel model) {
      setModel(model);
    }

    public void setModel(DmxWholeModel model) {
      if (model instanceof DmxWholeModel) {
        DmxWholeModel dmxWholeModel = (DmxWholeModel)model;

        this.model = dmxWholeModel;
        if (this.main == null) {
          this.main = createFullBuffer(this.model);
          this.cue = createFullBuffer(this.model);
          this.aux = createFullBuffer(this.model);
        }
      } else {
        LX.error("Model is not DmxWholeModel, DmxEngine will fail.");
      }
    }

    public void setCueOn(boolean cueOn) {
      this.cueOn = cueOn;
    }

    public void setAuxOn(boolean auxOn) {
      this.auxOn = auxOn;
    }

    public void copyFrom(Frame that) {
      this.cueOn = that.cueOn;
      this.auxOn = that.auxOn;
      copyFullBuffer(that.main, this.main);
      copyFullBuffer(that.cue, this.cue);
      copyFullBuffer(that.aux, this.aux);
    }

    public DmxBuffer[] getColors() {
      return this.cueOn ? this.cue : this.main;
    }

    public DmxBuffer[] getAuxColors() {
      return this.auxOn ? this.aux : this.main;
    }

    public DmxWholeModel getModel() {
      return this.model;
    }

    @Override
    public DmxBuffer[] getArray() {
      return this.main;
    }

    public DmxBuffer[] getMain() {
      return this.main;
    }

    public DmxBuffer[] getCue() {
      return this.cue;
    }

    public DmxBuffer[] getAux() {
      return this.aux;
    }
  }

  // Copied from LXEngine.DoubleBuffer
  // A double buffer that holds two frames which are flipped back and forth such that
  // the engine thread may render into one of them while UI or networking threads may
  // copy off the contents of another
  class DoubleBuffer {

    // Frame buffer that is currently used by the engine to render
    Frame render;

    // Complete buffer that may be copied off for UI or networking while engine
    // works on the other buffer.
    Frame copy;

    DoubleBuffer(DmxWholeModel model) {
      this.render = new Frame(model);
      this.copy = new Frame(model);
    }

    synchronized void sync() {
      this.copy.copyFrom(this.render);
    }

    synchronized void flip() {
      Frame tmp = this.render;
      this.render = this.copy;
      this.copy = tmp;
    }

    synchronized void copyTo(Frame that) {
      that.copyFrom(this.copy);
    }
  }

  private final DoubleBuffer buffer;

  private final HashMap<LXAbstractChannel, List<LXBuffer>> lxBuffersByChannel = new HashMap<LXAbstractChannel, List<LXBuffer>>();
  private final HashMap<LXBuffer, DmxModelBuffer> dmxBufferByLXBuffer = new HashMap<LXBuffer, DmxModelBuffer>();
  private final HashMap<LXGroup, DmxModelBuffer> dmxBufferByGroup = new HashMap<LXGroup, DmxModelBuffer>();

  private final AddBlend addBlend;

  private final DmxBlend dmxBlend;

  final DmxModelBuffer backgroundBlack;
  final DmxModelBuffer backgroundTransparent;
  private final DmxModelBuffer blendBufferLeft;
  private final DmxModelBuffer blendBufferRight;

  public DmxEngine(LX lx) {
    current = this;

    this.lx = lx;

    this.dmxWholeModel = TEApp.wholeModel;

    // Set up global add blend
    this.addBlend = new AddBlend(lx);
    this.addBlend.onActive();

    // Global default blend
    this.dmxBlend = new DmxBlend();

    // Monitor for channels deleted to release buffers
    this.lx.engine.mixer.addListener(this.mixerListener);

    // LXEngine hook: pre-mixer
    lx.engine.addLoopTask(this);

    // LXEngine hook: post-mixer
    this.outputTrigger = new OutputTrigger(lx);
    lx.engine.addOutput(this.outputTrigger);

    // Initialize double-buffer of frame contents
    this.buffer = new DoubleBuffer(this.dmxWholeModel);

    // Background and blending buffers
    this.backgroundBlack = new DmxModelBuffer(lx, this.dmxWholeModel);
    this.backgroundTransparent = new DmxModelBuffer(lx, this.dmxWholeModel);
    this.blendBufferLeft = new DmxModelBuffer(lx, this.dmxWholeModel);
    this.blendBufferRight = new DmxModelBuffer(lx, this.dmxWholeModel);
  }

  /*
   * Buffer management
   */

  public DmxModelBuffer getDmxModelBuffer(LXBuffer buffer, LXAbstractChannel channel) {
    if (dmxBufferByLXBuffer.containsKey(buffer)) {
      return dmxBufferByLXBuffer.get(buffer);
    } else {
      return createBufferDmx(buffer, channel);
    }
  }

  private DmxModelBuffer createBufferDmx(LXBuffer buffer, LXAbstractChannel channel) {
    DmxModelBuffer bufferDmx = new DmxModelBuffer(this.lx, this.dmxWholeModel);
    this.dmxBufferByLXBuffer.put(buffer, bufferDmx);

    List<LXBuffer> chBuffers = this.lxBuffersByChannel.get(channel);
    if (chBuffers == null) {
      chBuffers = new ArrayList<LXBuffer>();
      this.lxBuffersByChannel.put(channel, chBuffers);
    }
    chBuffers.add(buffer);

    return bufferDmx;
  }

  private DmxModelBuffer getDmxModelBufferByGroup(LXGroup group) {
    if (this.dmxBufferByGroup.containsKey(group)) {
      return this.dmxBufferByGroup.get(group);
    } else {
      DmxModelBuffer bufferDmx = new DmxModelBuffer(this.lx, this.dmxWholeModel);
      this.dmxBufferByGroup.put(group, bufferDmx);
      return bufferDmx;
    }
  }

  /**
   * Monitor LX mixer for channels removed, release references to expiring buffers
   */
  protected void removeChannel(LXAbstractChannel channel) {
    List<LXBuffer> chBuffers = this.lxBuffersByChannel.remove(channel);
    if (chBuffers != null) {
      for (LXBuffer buffer : chBuffers) {
        DmxModelBuffer dmxBuffer = this.dmxBufferByLXBuffer.remove(buffer);
        if (dmxBuffer != null) {
          dmxBuffer.dispose();
        }
      }
    }
    if (channel instanceof LXGroup) {
      DmxModelBuffer groupBuffer = this.dmxBufferByGroup.remove((LXGroup)channel);
      if (groupBuffer != null) {
        groupBuffer.dispose();
      }
    }
  }

  /*
   * LXLoopTask. Initialize buffers before patterns are run.
   */

  @Override
  public void loop(double deltaMs) {
    // LXLoopTasks are run before the mixer.

    // Initialize buffers.
    for (DmxModelBuffer buffer : this.dmxBufferByLXBuffer.values()) {
      buffer.resetModified();
    }
  }

  /*
   * DMX Mixer
   */

  /**
   * Copied from LXEngine, modified for DMX
   */
  private class BlendStack {

    private DmxBuffer[] destination;
    private DmxBuffer[] output;

    void initialize(DmxBuffer[] destination, DmxBuffer[] output) {
      this.destination = destination;
      this.output = output;

      if (this.destination == this.output) {
        LX.error(new Exception("BlendStack initialized with the same destination/output"));
      } else {
        DmxFullBuffer.copyFullBuffer(this.destination, this.output);
        this.destination = this.output;
      }
    }

    void blend(LXBlend blend, BlendStack that, double alpha, DmxWholeModel model) {
      blend(blend, that.destination, alpha, model);
    }

    void blend(LXBlend blend, DmxBuffer[] src, double alpha, DmxWholeModel model) {
      dmxBlend.blend(destination, src, alpha, output, model);
      this.destination = this.output;
    }

    void transition(LXBlend blend, DmxBuffer[] src, double lerp, DmxWholeModel model) {
      dmxBlend.lerp(destination, src, lerp, output, model);
      this.destination = this.output;
    }

    void copyFrom(BlendStack that) {
      DmxFullBuffer.copyFullBuffer(that.destination, this.output);
      this.destination = this.output;
    }
  }

  private final BlendStack blendStackMain = new BlendStack();
  private final BlendStack blendStackCue = new BlendStack();
  private final BlendStack blendStackAux = new BlendStack();
  private final BlendStack blendStackLeft = new BlendStack();
  private final BlendStack blendStackRight = new BlendStack();

  private void runDmxMixer() {
    // By now the patterns and normal pixel mixer have run.
    // Run DMX Mixer.  This was derived from LXMixerEngine.loop()

    Frame render = this.buffer.render;

    DmxEngine.debug("runDmxMixer 1 previous frame", render.main);

    // Initialize blend stacks
    this.blendStackMain.initialize(this.backgroundBlack.getArray(), render.getMain());
    this.blendStackCue.initialize(this.backgroundBlack.getArray(), render.getCue());
    this.blendStackAux.initialize(this.backgroundBlack.getArray(), render.getAux());
    this.blendStackLeft.initialize(this.backgroundBlack.getArray(), this.blendBufferLeft.getArray());
    this.blendStackRight.initialize(this.backgroundBlack.getArray(), this.blendBufferRight.getArray());

    DmxEngine.debug("runDmxMixer 2 initialized to black", render.main);

    double crossfadeValue = this.lx.engine.mixer.crossfader.getValue();

    boolean leftBusActive = crossfadeValue < 1.;
    boolean rightBusActive = crossfadeValue > 0.;
    boolean cueBusActive = false;
    boolean auxBusActive = false;

    final boolean isPerformanceMode = this.lx.engine.performanceMode.isOn();

    // Step 3: blend the channel buffers down
    boolean blendLeft = leftBusActive || this.lx.engine.mixer.cueA.isOn();
    boolean blendRight = rightBusActive || this.lx.engine.mixer.cueB.isOn();
    boolean leftExists = false, rightExists = false;
    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      long blendStart = System.nanoTime();

      // Is this a group sub-channel? Those don't blend, they are already composited
      // into their group
      boolean isSubChannel = channel.getGroup() != null;

      // Blend into the output buffer
      if (!isSubChannel && hasDmxBuffer(channel)) {
        BlendStack blendStack = null;

        // Which output group is this channel mapped to
        switch (channel.crossfadeGroup.getEnum()) {
        case A:
          leftExists = true;
          blendStack = blendLeft ? this.blendStackLeft : null;
          break;
        case B:
          rightExists = true;
          blendStack = blendRight ? this.blendStackRight : null;
          break;
        default:
        case BYPASS:
          blendStack = blendStackMain;
          break;
        }

        if (blendStack != null && channel.enabled.isOn()) {
          double alpha = channel.fader.getValue();
          double transitionProgress = channel instanceof LXChannel ? ((LXChannel)channel).getTransitionProgress() : 0;
          if (transitionProgress > 0) {
            // TODO: Blend the two buffers together *before* blending into the blendstack.
            // Assuming primary (blendBuffer) was used first
            blendStack.blend(channel.blendMode.getObject(), getDmxBuffersByChannel(channel), alpha, dmxWholeModel);
            // Secondary (renderBuffer) is second in the list
            blendStack.blend(channel.blendMode.getObject(), getRenderBuffersByChannel(channel), alpha, dmxWholeModel);
          } else {
            blendStack.blend(channel.blendMode.getObject(), getDmxBuffersByChannel(channel), alpha, dmxWholeModel);
          }
        }
      }
      DmxEngine.debug("runDmxMixer 3", render.main);

      // Blend into the cue buffer, always a direct add blend for any type of channel
      if (channel.cueActive.isOn()) {
        cueBusActive = true;
        this.blendStackCue.blend(this.addBlend, getDmxBuffersByChannel(channel), 1, dmxWholeModel);
      }
      DmxEngine.debug("runDmxMixer 4", render.main);

      // Blend into the aux buffer when in performance mode
      if (isPerformanceMode && channel.auxActive.isOn()) {
        auxBusActive = true;
        this.blendStackAux.blend(this.addBlend, getDmxBuffersByChannel(channel), 1, dmxWholeModel);
      }

      ((LXAbstractChannel.Profiler) channel.profiler).blendNanos = System.nanoTime() - blendStart;
    }

    // Check if the crossfade group buses are cued
    if (this.lx.engine.mixer.cueA.isOn()) {
      this.blendStackCue.copyFrom(this.blendStackLeft);
      cueBusActive = true;
    } else if (this.lx.engine.mixer.cueB.isOn()) {
      this.blendStackCue.copyFrom(this.blendStackRight);
      cueBusActive = true;
    }

    // Crossfade groups can be aux-cued in performance mode
    if (isPerformanceMode) {
      if (this.lx.engine.mixer.auxA.isOn()) {
        this.blendStackAux.copyFrom(this.blendStackLeft);
        auxBusActive = true;
      } else if (this.lx.engine.mixer.auxB.isOn()) {
        this.blendStackAux.copyFrom(this.blendStackRight);
        auxBusActive = true;
      }
    }

    DmxEngine.debug("runDmxMixer 5", render.main);

    // Step 4: now we have three output buses that need mixing... the left/right crossfade
    // groups plus the main buffer. We figure out which of them are active and blend appropriately
    // Note that the A+B crossfade groups are additively mixed AFTER the main buffer
    final boolean leftContent = leftBusActive && leftExists;
    final boolean rightContent = rightBusActive && rightExists;

    if (leftContent && rightContent) {
      // There are left and right channels assigned!
      LXBlend blend = this.lx.engine.mixer.crossfaderBlendMode.getObject();
      blendStackLeft.transition(blend, blendStackRight.destination, crossfadeValue, dmxWholeModel);
      // Add the crossfaded groups to the main buffer
      this.blendStackMain.blend(this.addBlend, blendStackLeft, 1., dmxWholeModel);
    } else if (leftContent) {
      // Add the left group to the main buffer
      this.blendStackMain.blend(this.addBlend, this.blendStackLeft, Math.min(1, 2. * (1-crossfadeValue)), dmxWholeModel);
    } else if (rightContent) {
      // Add the right group to the main buffer
      this.blendStackMain.blend(this.addBlend, this.blendStackRight, Math.min(1, 2. * crossfadeValue), dmxWholeModel);
    }
    DmxEngine.debug("runDmxMixer 6", render.main);

    // Step 5: Master FX, not using in DMX

    // Step 6: If the master fader is POST-visualizer/output, apply global scaling now
    if (this.lx.engine.mixer.masterBus.previewMode.getEnum() == LXMasterBus.PreviewMode.POST) {
      double fader = this.lx.engine.mixer.masterBus.fader.getValue();
      if (fader < 1.) {
        // Apply a pass to scale brightness
        scaleBrightness(this.blendStackMain.output, fader);
      }
    }

    // Mark the cue active state of the buffer
    render.setCueOn(cueBusActive);
    render.setAuxOn(auxBusActive);
    DmxEngine.debug("runDmxMixer 7", render.main);

    // DMX Outputs
    sendDmx();
  }

  private boolean hasDmxBuffer(LXAbstractChannel channel) {
    return this.lxBuffersByChannel.containsKey(channel);
  }

  private DmxBuffer[] getDmxBuffersByChannel(LXAbstractChannel channel) {
    if (channel instanceof LXGroup) {
      return getDmxModelBufferByGroup((LXGroup)channel).getArray();
    } else {
      return this.dmxBufferByLXBuffer.get(this.lxBuffersByChannel.get(channel).get(0)).getArray();
    }
  }

  private DmxBuffer[] getRenderBuffersByChannel(LXAbstractChannel channel) {
    return this.dmxBufferByLXBuffer.get(this.lxBuffersByChannel.get(channel).get(1)).getArray();
  }

  private void scaleBrightness(DmxBuffer[] fullBuffer, double brightness) {
    for (DmxBuffer dmxBuffer : fullBuffer) {
      for (DmxParameter p : dmxBuffer.array) {
        p.setDmxValue(p.getDmxValue(brightness));
      }
    }
  }

  /**
   * Send DMX outputs
   */
  private void sendDmx() {
    // Step 5: our cue and render frames are ready! Let's get them output
    boolean isNetworkMultithreaded = this.lx.engine.isNetworkMultithreaded.isOn();
    boolean isDoubleBuffering = this.lx.engine.isThreaded() || isNetworkMultithreaded;
    if (isDoubleBuffering) {
      // We are multi-threading, lock the double buffer and flip it
      this.buffer.flip();
    }

    if (isNetworkMultithreaded) {
      LX.error("DMX outputs not configured for network multithreading.");
    } else {
      // Or do it ourself here on the engine thread
      Frame sendFrame = isDoubleBuffering ? this.buffer.copy : this.buffer.render;
      DmxEngine.debug("sendDmx 1", sendFrame.main);

      DmxBuffer[] sendColors = (this.lx.flags.sendCueToOutput && sendFrame.cueOn) ? sendFrame.cue : sendFrame.main;
      DmxEngine.debug("sendDmx 2", sendColors);

      // Scale for master brightness
      double masterBrightness = this.lx.engine.mixer.masterBus.getOutputBrightness();
      if (masterBrightness < 1.) {
        scaleBrightness(sendColors, masterBrightness);
      }

      DmxEngine.debug("sendDmx 3", sendColors);
      sendFinalDmx(sendColors);
    }
  }

  private final Map<DmxModel, DmxOutput> dmxOutputs = new HashMap<DmxModel, DmxOutput>();

  private void sendFinalDmx(DmxBuffer[] data) {
    // DMX Mixer finished, data is ready to send
    DmxEngine.debug("sendFinalDmx 1", data);

    DmxOutput output;
    for (DmxModel m : this.dmxWholeModel.getDmxModels()) {
      // Rebuild outputs if needed
      output = null;
      if (m.outputChanged) {
        DmxOutput oldOutput = dmxOutputs.get(m);
        if (oldOutput != null) {
          oldOutput.dispose();
        }
        output = createOutput(m);
        dmxOutputs.put(m, output);
        m.outputChanged = false;

        // Sigh. Shortcut. Almost out of time.
        DevSwitch.current.applyDmxOutputsEnabled();
      } else {
        output = dmxOutputs.get(m);
      }

      // Skip outputs that weren't built
      if (output == null) {
        continue;
      }

      // Stage the data
      output.setDmxData(data[m.index]);

      // Send it
      output.send(null);
    }
  }

  private DmxOutput createOutput(DmxModel m) {
    DmxOutput o = new DmxOutput(this.lx, m.getDmxOutputDefinition());
    return o;
  }

  public void setOutputsEnabledByType(Class<? extends DmxModel> modelType, boolean enabled) {
    for (Entry<DmxModel, DmxOutput> entry : this.dmxOutputs.entrySet()) {
      if (modelType.isInstance(entry.getKey())) {
        entry.getValue().enabled.setValue(enabled);
      }
    }
  }

  public void dispose() {
    for (DmxModelBuffer value : this.dmxBufferByLXBuffer.values()) {
      value.dispose();
    }
    for (DmxModelBuffer value : this.dmxBufferByGroup.values()) {
      value.dispose();
    }
    this.dmxBufferByLXBuffer.clear();
    this.dmxBufferByGroup.clear();
    this.lx.engine.mixer.removeListener(this.mixerListener);
    current = null;
  }

  public static void debug(String location, DmxBuffer[] dmxAll) {
    if (!ENABLE_DEBUG) {
      return;
    }
    // This was a quick edit
    byte[] output = new byte[26];
    int[] outputUnsigned = new int[26];
    int offset;

    for (int d = 0; d < dmxAll.length; d++) {
      DmxBuffer dmx = dmxAll[d];
      offset = 0;
      for (int i = 0; i < dmx.array.length; i++) {
        DmxParameter p = dmx.array[i];
        p.writeBytes(output, offset);
        offset += p.getNumBytes();
      }
      for (int i = 0; i < output.length; i++) {
        outputUnsigned[i] = output[i] & 0xFF;
      }
      System.out.println(location + " ".repeat(38-location.length()) + Arrays.toString(outputUnsigned) + (dmx.isActive ? "" : " NOT ACTIVE"));
    }
     
  }
}
