package titanicsend.modulator;

import heronarts.glx.GLX;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIIndicator;
import heronarts.glx.ui.component.UILabel;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import heronarts.lx.utils.LXUtils;
import java.time.LocalTime;

/**
 * Slowly moves the master fader to zero when the time of day is between StartTime and EndTime.
 * Detects if the master fader is moved up by something else, in which case it displays a dialog to
 * alert the user to the alarm status.
 */
@LXModulator.Global("Master Lights Off")
@LXCategory("Utility")
public class MasterLightsOff extends LXModulator implements UIModulatorControls<MasterLightsOff> {

  public final CompoundParameter startTime =
      new CompoundParameter("StartTime", 7, 0, 24)
          .setDescription("Start of alarm, in hours of the day (0-24)");

  public final CompoundParameter endTime =
      new CompoundParameter("EndTime", 10, 0, 24)
          .setDescription("End of alarm, in hours of the day (0-24)");

  public final CompoundParameter movementDuration =
      new CompoundParameter("MoveDuration", 5, 0, 20)
          .setDescription("Amount of time over which to transition the target parameter to zero");

  public final BooleanParameter enableDialog =
      new BooleanParameter("Dialog", true)
          .setDescription("Whether to show a dialog to the user if parameter movement is detected");

  public final CompoundParameter dialogFrequencySec =
      new CompoundParameter("DialogFreq", 15, 0, 60)
          .setDescription(
              "Minimum time (in seconds) between displaying the dialog if user is moving the parameter");

  public final BooleanParameter alarmIndicator =
      new BooleanParameter("Alarm", false)
          .setDescription("TRUE if the alarm is currently active. Read-Only.");

  private LXListenableNormalizedParameter target;
  private double lastParameterValue = 0;

  private LocalTime start;
  private LocalTime end;

  private boolean alarm = false;
  private long alarmStarted;

  /** alarmLevel ramps from 0-1 over a period of movementDuration from the start of alarm */
  private double alarmLevel = 0;

  private long lastDialogDisplay = 0;

  public MasterLightsOff() {
    this("Master Lights Off");
  }

  public MasterLightsOff(String label) {
    super(label);

    addParameter("startTime", this.startTime);
    addParameter("endTime", this.endTime);
    addParameter("movementDuration", this.movementDuration);
    addParameter("enableDialog", this.enableDialog);
    addParameter("dialogFrequencySec", this.dialogFrequencySec);
    // Do not add alarmIndicator parameter

    refreshLocalTimes();
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.startTime || p == this.endTime) {
      refreshLocalTimes();
    }
  }

  private void refreshLocalTimes() {
    double startTime = this.startTime.getValue();
    double endTime = this.endTime.getValue();

    int startHour = (int) startTime;
    int endHour = (int) endTime;
    int startMinute = (int) ((startTime - startHour) * 59.99);
    int endMinute = (int) ((endTime - endHour) * 59.99);

    this.start = LocalTime.of(startHour, startMinute);
    this.end = LocalTime.of(endHour, endMinute);
  }

  private boolean isNowAlarm() {
    LocalTime now = LocalTime.now();
    if (this.start.isBefore(this.end)) {
      return now.isAfter(this.start) && now.isBefore(this.end);
    } else {
      // Crosses midnight
      return now.isAfter(this.start) || now.isBefore(this.end);
    }
  }

  private boolean confirmParameterLink() {
    if (this.target != null) {
      return true;
    } else {
      if (this.lx != null) {
        this.target = this.lx.engine.mixer.masterBus.fader;
        return true;
      } else {
        // LX has not been set on the modulator
        return false;
      }
    }
  }

  private double getParameterValue() {
    if (this.target == null) {
      return 0;
    }
    // Note: This does not currently account for modulated value which could be higher
    return this.target.getNormalized();
  }

  private void setParameterValue(double value) {
    if (this.target != null) {
      this.lastParameterValue = value;
      this.target.setNormalized(value);
    }
  }

  /** Main loop */
  @Override
  protected double computeValue(double deltaMs) {
    // Fast bail if LX has not been set on this modulator
    if (!confirmParameterLink()) {
      return 0;
    }

    final boolean isNowAlarm = isNowAlarm();

    if (this.alarm) {
      // Alarm already started
      if (!isNowAlarm) {
        // The end time has elapsed
        finishAlarm();
      } else {
        // We are in the alarm period
        tickAlarm(deltaMs);
      }
    } else {
      // Alarm not yet started. Is it time?
      if (isNowAlarm) {
        beginAlarm();
      }
    }

    // Public indicator
    this.alarmIndicator.setValue(this.alarm);

    // Modulator output is current alarm level 0-1 (uses linear ramp-up)
    return this.alarmLevel;
  }

  private void beginAlarm() {
    this.alarm = true;
    this.alarmStarted = System.currentTimeMillis();
    updateAlarmLevel();
    this.lastParameterValue = getParameterValue();

    LX.log("Master Lights Off STARTED at " + LocalTime.now());
    showDialog();
  }

  private void finishAlarm() {
    this.alarm = false;
    this.alarmLevel = 0;

    LX.log("Master Lights Off FINISHED at " + LocalTime.now());
  }

  private void updateAlarmLevel() {
    double now = System.currentTimeMillis();
    double elapsed = now - this.alarmStarted;
    double duration = this.movementDuration.getValue() * 1000;

    // Contains extra safety to avoid divide by zero:
    if (elapsed >= duration || duration == 0) {
      this.alarmLevel = 1;
    } else {
      this.alarmLevel = elapsed / duration;
    }
  }

  private void tickAlarm(double deltaMs) {
    updateAlarmLevel();

    // If user raises the parameter manually, optional show dialog a maximum of every X seconds
    double parameterValue = getParameterValue();
    if (parameterValue > this.lastParameterValue
        && this.enableDialog.isOn()
        && dialogTimeElapsed()) {
      LX.warning(
          "User tried to raise the master fader while it was suppressed by Master Lights Off");
      showDialog();
    }
    this.lastParameterValue = parameterValue;

    // Move parameter towards zero
    if (parameterValue > 0) {
      double durationMs = this.movementDuration.getValue() * 1000;
      if (durationMs == 0) {
        setParameterValue(0);
      } else {
        double moveAmt = LXUtils.min(deltaMs / durationMs, 1);
        double newParameterValue = LXUtils.max(0, parameterValue - moveAmt);
        setParameterValue(newParameterValue);
      }
    }
  }

  private boolean dialogTimeElapsed() {
    long now = System.currentTimeMillis();
    double elapsed = now - this.lastDialogDisplay;
    double dialogFrequencySec = this.dialogFrequencySec.getValue();
    return elapsed > (dialogFrequencySec * 1000);
  }

  private void showDialog() {
    this.lastDialogDisplay = System.currentTimeMillis();
    if (this.lx instanceof GLX glx) {
      glx.ui.showContextDialogMessage(
          "WARNING! Master Lights Off is lowering the master fader to zero.");
    }
  }

  private static final float labelWidth = 130;

  @Override
  public void buildModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, MasterLightsOff modulator) {
    uiModulator.setLayout(UI2dContainer.Layout.VERTICAL, 2);
    uiModulator.addChildren(
        newRow("Start Time (hour of day)", newDoubleBox(modulator.startTime)),
        newRow("End Time (hour of day)", newDoubleBox(modulator.endTime)),
        newRow("Movement Duration (sec)", newDoubleBox(modulator.movementDuration)),
        newRow(
            "Dialog",
            newButton(modulator.enableDialog)
                .setActiveLabel("Enabled")
                .setInactiveLabel("Disabled")),
        newRow("Dialog Frequency", newDoubleBox(modulator.dialogFrequencySec)),
        newRow(
            "Alarm",
            new UIIndicator(ui, modulator.alarmIndicator)
                .setClickable(false)
                .setTriggerable(false)
                .setY(2)));
  }

  private UI2dComponent newRow(String description, UI2dComponent component) {
    return UI2dContainer.newHorizontalContainer(
        16, 2, new UILabel(labelWidth, description), component);
  }
}
