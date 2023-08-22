package titanicsend.dmx.effect;

import heronarts.lx.LX;
import heronarts.lx.studio.TEApp;
import titanicsend.model.TEWholeModel;

public abstract class BeaconEffect extends DmxEffect {

  protected final TEWholeModel modelTE;

  public BeaconEffect(LX lx) {
    super(lx);
    this.modelTE = TEApp.wholeModel;
  }

}
