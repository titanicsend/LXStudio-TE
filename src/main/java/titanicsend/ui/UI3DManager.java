package titanicsend.ui;

import heronarts.lx.studio.LXStudio;
import titanicsend.app.TEVirtualOverlays;
import java.util.concurrent.atomic.AtomicBoolean;

// Thin wrapper that allows global access to 3D UI components, so we can
// update them easily when the model changes
public class UI3DManager  {
  public static UI3DManager current;
  public static final AtomicBoolean inRebuild = new AtomicBoolean(true);
  public static final AtomicBoolean inDraw = new AtomicBoolean(false);

  public final UIModelLabels modelLabels;

  // Chromatik requires a separate UI object per context (main and aux) if
  // we want the element to show up in performance mode.
  public final UIBackings backings;
  public final UIBackings backingsAux;

  public final UILasers lasers;
  public final UILasers lasersAux;

  public static void lockRebuild() {
      UI3DManager.inRebuild.set(true);
  }

  public static void unlockRebuild() {
      UI3DManager.inRebuild.set(false);
  }

  public static boolean isRebuildLocked() {
      return UI3DManager.inRebuild.get();
  }

  public static void lockDraw() {
    UI3DManager.inDraw.set(true);
  }

  public static void unlockDraw() {
    UI3DManager.inDraw.set(false);
  }

  public void rebuild() {
    // lock everyone out of draw while rebuilding
    UI3DManager.lockRebuild();

    // wait for any current draws to finish
     while (UI3DManager.inDraw.get()) {
      try {
        Thread.sleep(10);
        //System.out.print(".");
      } catch (InterruptedException e) {
        ;
      }
     }

    // rebuild the model
    modelLabels.rebuild();
    backings.rebuild();
    backingsAux.rebuild();

    UI3DManager.unlockRebuild();
  }

  public UI3DManager(LXStudio lx, LXStudio.UI ui,  TEVirtualOverlays virtualOverlays) {
    current = this;

    this.modelLabels = new UIModelLabels(lx, virtualOverlays);
    ui.preview.addComponent(modelLabels);

    this.backings = new UIBackings(lx, virtualOverlays);
    ui.preview.addComponent(backings);
    this.backingsAux = new UIBackings(lx, virtualOverlays);
    ui.previewAux.addComponent(backingsAux);

    this.lasers = new UILasers(lx, virtualOverlays);
    ui.preview.addComponent(lasers);
    this.lasersAux = new UILasers(lx, virtualOverlays);
    ui.previewAux.addComponent(lasersAux);
  }
}
