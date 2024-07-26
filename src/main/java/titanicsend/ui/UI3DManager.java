package titanicsend.ui;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import titanicsend.ui.UIModelLabels;
import titanicsend.ui.UIBackings;
import titanicsend.ui.UILasers;

// Thin wrapper that allows global access to 3D UI components, so we can
// update them easily when the model changes
public class UI3DManager extends LXComponent {
  public static UI3DManager current;

  public UIModelLabels modelLabels = null;
  public UIBackings backings = null;
  public UILasers lasers = null;

  public UI3DManager(LX lx) {
    super(lx, "UI3DManager");
    current = this;
  }
}
