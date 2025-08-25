package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXModel;
import heronarts.lx.studio.TEApp;
import titanicsend.model.TEWholeModel;

public abstract class TEEffect extends LXEffect {

  protected final TEWholeModel modelTE;

  public TEEffect(LX lx) {
    super(lx);

    this.modelTE = TEApp.wholeModel;

    // Listen to model generation changes (geometry changes) at top level and treat it
    // as a modelChanged event, because the points have moved.  JKB: LX fix needed here?
    this.lx.addListener(this.lxListener);
  }

  private final LX.Listener lxListener =
      new LX.Listener() {
        @Override
        public void modelGenerationChanged(LX lx, LXModel model) {
          // The model (object) didn't change but the points moved. Redirect to onModelChanged() to
          // force recalculation (such as for shader lxModelCoordinates to reload)
          onModelChanged(getModelView());
        }
      };

  @Override
  public void dispose() {
    this.lx.removeListener(this.lxListener);
    super.dispose();
  }
}
