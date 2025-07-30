package titanicsend.resolume.parameter;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.utils.LXUtils;
import titanicsend.resolume.ResolumeVariable;

/**
 * A parameter that automatically sends its value to Resolume via OSC
 * Based on BeyondCompoundParameter from the Beyond plugin
 */
public class ResolumeCompoundParameter extends CompoundParameter {

  private final LX lx;

  // OSC address in Resolume
  private String resolumePath;
  private boolean isValidPath = false;

  // Whether parameter values will be sent to Resolume
  private boolean outputEnabled = true;

  // OSC feedback, not yet implemented
  private boolean feedbackEnabled = false;
  private boolean registered = false;

  // Last value that was sent to Resolume
  private double lastValue = 0;
  private boolean needsUpdate = false;

  private final LXLoopTask checkForUpdates = new LXLoopTask() {
    @Override
    public void loop(double deltaMs) {
      // Don't even query the modulated value if output is disabled
      if (canSend()) {
        // Retrieve *potentially modulated* value, send if changed
        double value = getValue();
        if (lastValue != value || needsUpdate) {
          lastValue = value;
          needsUpdate = false;
          sendOscMessage(resolumePath, (float) value);
        }
      }
    }
  };

  public ResolumeCompoundParameter(LX lx, String label, String resolumePath) {
    this(lx, label, resolumePath, 0, 0, 1);
  }

  public ResolumeCompoundParameter(LX lx, ResolumeVariable v) {
    this(lx, v.label, v.oscPath, v.defaultValue, v.min, v.max);
  }

  public ResolumeCompoundParameter(LX lx, ResolumeVariable v, String label) {
    this(lx, label, v.oscPath, v.defaultValue, v.min, v.max);
  }

  public ResolumeCompoundParameter(LX lx, String label, String resolumePath, double value, double v0, double v1) {
    super(label, value, v0, v1);
    this.lx = lx;

    setResolumePath(resolumePath);
    updateRegistration();

    lx.engine.addLoopTask(this.checkForUpdates);
  }

  public final String getResolumePath() {
    return this.resolumePath;
  }

  public ResolumeCompoundParameter setResolumePath(String resolumePath) {
    this.resolumePath = resolumePath;
    this.isValidPath = !LXUtils.isEmpty(resolumePath);
    return this;
  }

  public ResolumeCompoundParameter setOutputEnabled(boolean outputEnabled) {
    this.outputEnabled = outputEnabled;
    if (outputEnabled) {
      this.needsUpdate = true;
    }
    return this;
  }

  // OSC Feedback

  /**
   * Set whether this parameter should listen for OSC input matching its Resolume path
   */
  private ResolumeCompoundParameter setFeedbackEnabled(boolean feedbackEnabled) {
    this.feedbackEnabled = feedbackEnabled;
    updateRegistration();
    return this;
  }

  private void updateRegistration() {
    if (this.feedbackEnabled) {
      if (!this.registered) {
        register();
      }
    } else {
      if (this.registered) {
        unregister();
      }
    }
  }

  private void register() {
    this.registered = true;
    // TODO: listen for OSC input
  }

  private void unregister() {
    this.registered = false;
    // TODO
  }

  // Output

  private boolean canSend() {
    return this.isValidPath && this.outputEnabled;
  }

  private void sendOscMessage(String address, float value) {
    lx.engine.osc.sendMessage(address, value);
  }

  public ResolumeCompoundParameter resend() {
    this.needsUpdate = true;
    return this;
  }

  @Override
  public void dispose() {
    this.lx.engine.removeLoopTask(this.checkForUpdates);
    super.dispose();
  }
}