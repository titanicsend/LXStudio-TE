package titanicsend.dmx.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.DmxModel;

@LXCategory("DMX")
public class BeaconStrobeEffect extends BeaconEffect {

  BooleanParameter isStrobe = new BooleanParameter("Strobe!", false)
      .setMode(Mode.MOMENTARY);

  CompoundParameter strobeSpeed = new CompoundParameter("StrbSpd", 95, 64, 95)
      .setDescription("Strobe speed, slow->fast");

  public BeaconStrobeEffect(LX lx) {
    super(lx);

    addParameter("isStrobe", this.isStrobe);
    addParameter("strobeSpeed", this.strobeSpeed);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    for (DmxModel d : this.modelTE.beacons) {      
      if (this.isStrobe.isOn()) {
        setDmxValue(d, BeaconModel.INDEX_SHUTTER, (int)this.strobeSpeed.getValue());
        setDmxNormalized(d, BeaconModel.INDEX_DIMMER, BeaconModel.DIMMER_NORMALIZED_100);
      }
    }
  }
}
