package titanicsend.lasercontrol;

import heronarts.lx.transform.LXVector;
import titanicsend.model.TELaserModel;

public class MovingTarget extends Target {
  public static final double MSEC_PER_REVOLUTION = 1500;
  public static final double RADIANS_PER_MSEC = (2.0 * Math.PI) / MSEC_PER_REVOLUTION;
  public static final float RADIUS = 1e6F;  // 1 meter
  private float theta = 0.0F;
  private LXVector center;

  public MovingTarget(TELaserModel laser) {
    super(laser);
    this.center = this.bullseye;
  }

  public void update(double deltaMsec) {
    if (this.center == null) return;  // Happens when super constructor calls .update()
    this.theta += (deltaMsec * RADIANS_PER_MSEC) % (2.0 * Math.PI);
    float x = RADIUS * (float)Math.sin(theta);
    float z = RADIUS * (float)Math.cos(theta);
    this.bullseye = new LXVector(x, 0, z).add(this.center);
    super.update(deltaMsec);
  }
}