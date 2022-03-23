package titanicsend.lasercontrol;

import heronarts.lx.transform.LXVector;
import titanicsend.model.TELaserModel;

public class Cone extends LaserControl {
  public static final double MSEC_PER_REVOLUTION = 2500;
  public static final double RADIANS_PER_MSEC = (2.0 * Math.PI) / MSEC_PER_REVOLUTION;
  public static final float SPIN_RADIUS_RATIO = 0.1F; // Slope of the cone; smaller=tighter
  public static final LXVector TOWARD_AUDIENCE = new LXVector(-3, -1, 0).normalize();
  public static final LXVector TOWARD_MOUNTAINS = new LXVector(-1, Cone.SPIN_RADIUS_RATIO, 0).normalize();

  public LXVector homeDirection;
  private final LXVector perpendicular;
  private float theta;

  public Cone(TELaserModel laser) {
    super(laser);

    if (laser.id.startsWith("AS")) {
      // Shine towards the audience and down at a 3:1 slope
      this.homeDirection = Cone.TOWARD_AUDIENCE;
    } else {
      // Shine almost straight out, but slightly uppish
      this.homeDirection = Cone.TOWARD_MOUNTAINS;
    }

    float ecks = homeDirection.x;  // Avoid an annoying warning about passing "x" to a parameter called y
    this.perpendicular = new LXVector(-homeDirection.y, ecks, 0);
    this.perpendicular.normalize();
    this.perpendicular.mult(SPIN_RADIUS_RATIO);

    this.theta = 0.0F;
    this.update(0);
  }

  public void update(double deltaMsec) {
    this.theta += (deltaMsec * RADIANS_PER_MSEC) % (2.0 * Math.PI);
    LXVector direction = this.perpendicular.copy();
    direction.rotate(this.theta,
            this.homeDirection.x, this.homeDirection.y, this.homeDirection.z);
    direction.add(this.homeDirection);
    this.laser.setDirection(direction);
  }

}
