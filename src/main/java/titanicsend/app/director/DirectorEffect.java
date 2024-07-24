package titanicsend.app.director;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.app.director.Director.Filter;
import titanicsend.effect.TEEffect;

/**
 * Applies filters from the Director component.
 * Should be used on the Master channel.
 */
@LXCategory("Titanics End")
public class DirectorEffect extends TEEffect {
  
  public DirectorEffect(LX lx) {
    super(lx);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    final float main = Director.get().main.getValuef();

    for (Filter filter : Director.get().filters) {
      filter.run(this.colors, main);
    }
  }
}