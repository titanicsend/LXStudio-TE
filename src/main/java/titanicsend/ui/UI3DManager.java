package titanicsend.ui;

import heronarts.lx.studio.LXStudio;
import titanicsend.app.TEVirtualOverlays;
import java.util.concurrent.atomic.AtomicBoolean;

// Thin wrapper that allows global access to 3D UI components, so we can
// update them easily when the model changes
public class UI3DManager  {
  public static UI3DManager current;
  public static final AtomicBoolean inRebuild = new AtomicBoolean(false);
  public static final AtomicBoolean inDraw = new AtomicBoolean(false);

  public final UIModelLabels modelLabels;

  // Chromatik requires a separate UI object per context (main and aux) if
  // we want the element to show up in performance mode.
  public final UIBackings backings;
  public final UIBackings backingsAux;

  public final UILasers lasers;
  public final UILasers lasersAux;

  public static void lock() {
      UI3DManager.inRebuild.set(true);
  }

  public static void unlock() {
      UI3DManager.inRebuild.set(false);
  }

  public static boolean isLocked() {
      return UI3DManager.inRebuild.get();
  }

  public static void beginDraw() {
    UI3DManager.inDraw.set(true);
  }

  public static void endDraw() {
    UI3DManager.inDraw.set(false);
  }
  public int oneshot = 0;
  public void rebuild() {

    if (oneshot > 0) return;
    oneshot++;

    // lock everyone out of draw while rebuilding
    UI3DManager.lock();

    // wait for any current draws to finish
     while (UI3DManager.inDraw.get()) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
     }

    // rebuild the model
    modelLabels.rebuild();
    backings.rebuild();
    backingsAux.rebuild();

    UI3DManager.unlock();
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
