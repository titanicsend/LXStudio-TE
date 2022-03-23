package titanicsend.lasercontrol;

import titanicsend.model.TELaserModel;

// Controls one laser
public abstract class LaserControl {
  public TELaserModel laser;
  public LaserControl(TELaserModel laser) { this.laser = laser; }
  public abstract void update(double deltaMsec);
}
