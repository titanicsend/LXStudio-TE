package titanicsend.ui;

import heronarts.lx.studio.LXStudio;
import java.util.concurrent.atomic.AtomicBoolean;
import titanicsend.app.TEVirtualOverlays;

// Thin wrapper that allows global access to 3D UI components, so we can
// update them easily when the model changes
public class UI3DManager {
  public static UI3DManager current;

  public static final AtomicBoolean labelsRebuild = new AtomicBoolean(false);
  public static final AtomicBoolean backingsRebuild = new AtomicBoolean(false);

  public final UIModelLabels modelLabels;

  // NOTE: Chromatik requires a separate UI object per context (main and aux) if
  // we want the element to show up in performance mode.

  // TODO: The "old" backing panel system is disabled and can be removed
  // at some point in the future. Chromatik now supports opaque polygons
  // in fixtures.
  //public final UIBackings backings;
  //public final UIBackings backingsAux;

  // UILasers is currently broken due to UnitCube not having a public constructor.
  // public final UILasers lasers;
  // public final UILasers lasersAux;

  public static boolean needsLabelsRebuild() {
    return labelsRebuild.get();
  }

  public static boolean needsBackingsRebuild() {
    return backingsRebuild.get();
  }

  // Call this from the graphics thread after processing the rebuild
  public static void clearLabelsRebuild() {
    labelsRebuild.set(false);
  }

  // Call this from the graphics thread after processing the rebuild
  public static void clearBackingsRebuild() {
    backingsRebuild.set(false);
  }

  public void rebuild() {
    // Simply set flags to indicate rebuild needed
    backingsRebuild.set(true);
    labelsRebuild.set(true);
  }

  public UI3DManager(LXStudio lx, LXStudio.UI ui, TEVirtualOverlays virtualOverlays) {
    current = this;

    this.modelLabels = new UIModelLabels(lx, virtualOverlays);
    ui.preview.addComponent(modelLabels);

    //this.backings = new UIBackings(lx, virtualOverlays);
    //ui.preview.addComponent(backings);
    //this.backingsAux = new UIBackings(lx, virtualOverlays);
    //ui.previewAux.addComponent(backingsAux);

    // As of 6-9-25 UILasers is broken due to UnitCube not having a public constructor.
    // this.lasers = new UILasers(lx, virtualOverlays);
    // ui.preview.addComponent(lasers);
    // this.lasersAux = new UILasers(lx, virtualOverlays);
    // ui.previewAux.addComponent(lasersAux);
  }
}