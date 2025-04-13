package titanicsend.quantize;

import heronarts.lx.LX;
import heronarts.lx.Tempo;

/**
 * A helper utility to delay method calls until a system quantize event such as Tempo Beat or Tempo
 * Bar.
 *
 * <p>Quantize behavior can be disabled in which case input events will result in immediate output.
 */
public class Quantizer implements Tempo.Listener {

  private final LX lx;
  private boolean enabled = false;
  private QuantizeEvent event = QuantizeEvent.BEAT;
  private Runnable onEvent;
  private int queue;

  /** What to do if queue() was called multiple times between quantize events */
  public static enum CallbackMode {
    /** onEvent is called a maximum of once per quantize event, even if multiple queues were set */
    SINGLE,
    /** onEvent is called once per queue call */
    MULTIPLE
  }

  private CallbackMode mode = CallbackMode.SINGLE;

  public Quantizer(LX lx, boolean enabled) {
    this(lx, null, enabled);
  }

  public Quantizer(LX lx, Runnable onEvent) {
    this(lx, onEvent, true);
  }

  public Quantizer(LX lx, Runnable onEvent, boolean enabled) {
    this.lx = lx;
    setEnabled(enabled);
    onEvent(onEvent);
  }

  /** Specify behavior if queue() were to be called multiple times between quantize events */
  public Quantizer setMode(CallbackMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * Set enabled state of Quantizer. If on, calls to queue() will result in a quantized callback to
   * onEvent. If off, calls to queue() will result in immediate callback to onEvent;
   *
   * @param on Whether quantizer is enabled
   * @return this
   */
  public Quantizer setEnabled(boolean on) {
    if (this.enabled == on) {
      return this;
    }
    this.enabled = on;
    if (on) {
      register();
    } else {
      unregister();
      if (ready()) {
        fire();
      }
    }
    return this;
  }

  private boolean registered = false;

  private void register() {
    this.registered = true;
    this.lx.engine.tempo.addListener(this);
  }

  private void unregister() {
    this.registered = false;
    this.lx.engine.tempo.removeListener(this);
  }

  public Quantizer setQuantizeEvent(QuantizeEvent event) {
    this.event = event;
    return this;
  }

  /**
   * Set the method to be run when a quantize event occurs.
   *
   * @param onEvent A runnable method to call
   * @return this
   */
  public Quantizer onEvent(Runnable onEvent) {
    if (this.onEvent != null) {
      LX.warning("WARNING / SHOULDFIX: Overwriting previous onEvent on Quantizer");
    }
    this.onEvent = onEvent;
    return this;
  }

  /**
   * Prime the quantizer to call onEvent when the next quantize event occurs
   *
   * @return this
   */
  public Quantizer queue() {
    if (this.enabled) {
      this.queue++;
    } else {
      // If quantize is disabled, call back immediately.
      fire();
    }
    return this;
  }

  private boolean ready() {
    return this.queue > 0;
  }

  /** Go time */
  private void fire() {
    int remaining = this.queue;
    // Reset before callbacks in case of immediate re-queue
    this.queue = 0;

    if (this.onEvent != null) {
      if (this.mode == CallbackMode.MULTIPLE) {
        while (remaining > 0) {
          this.onEvent.run();
          remaining--;
        }
      } else {
        this.onEvent.run();
      }
    }
  }

  /**
   * Clear the queue count. The next quantize event will result in no callbacks.
   *
   * @return this
   */
  public Quantizer reset() {
    this.queue = 0;
    return this;
  }

  @Override
  public void onBeat(Tempo tempo, int beat) {
    if (ready() && this.event == QuantizeEvent.BEAT) {
      fire();
    }
  }

  @Override
  public void onBar(Tempo tempo, int bar) {
    if (ready() && this.event == QuantizeEvent.BAR) {
      fire();
    }
  }

  public void dispose() {
    if (this.registered) {
      unregister();
    }
  }
}
