package titanicsend.ui;

import heronarts.lx.studio.LXStudio;
import java.util.concurrent.locks.ReentrantLock;
import titanicsend.app.TEVirtualOverlays;

// Thin wrapper that allows global access to 3D UI components, so we can
// update them easily when the model changes
public class UI3DManager {
  public static UI3DManager current;

  public static final ReentrantLock labelsLock = new ReentrantLock();
  public static final ReentrantLock backingsLock = new ReentrantLock();

  public final UIModelLabels modelLabels;

  // Chromatik requires a separate UI object per context (main and aux) if
  // we want the element to show up in performance mode.
  public final UIBackings backings;
  public final UIBackings backingsAux;

  public final UILasers lasers;
  public final UILasers lasersAux;

  public static boolean labelsLocked() {
    return UI3DManager.labelsLock.isLocked();
  }

  public static boolean backingsLocked() {
    return UI3DManager.backingsLock.isLocked();
  }

  public void rebuild() {
    boolean inRebuild = false;
    // lock everyone out of draw while rebuilding UI elements.
    // We'll wait for up to roughly one frame's worth of time to acquire
    // the lock for each object, and if we don't get it, we skip that
    // portion of the rebuild.

    // Rebuild panel backings
    try {
      inRebuild = UI3DManager.backingsLock.tryLock(17, java.util.concurrent.TimeUnit.MILLISECONDS);
      if (inRebuild) {
        backings.rebuild();
        backingsAux.rebuild();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (inRebuild) {
        UI3DManager.backingsLock.unlock();
      }
    }

    // Rebuild model labels
    try {
      inRebuild = UI3DManager.labelsLock.tryLock(17, java.util.concurrent.TimeUnit.MILLISECONDS);
      if (inRebuild) {
        modelLabels.rebuild();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (inRebuild) {
        UI3DManager.labelsLock.unlock();
      }
    }
  }

  public UI3DManager(LXStudio lx, LXStudio.UI ui, TEVirtualOverlays virtualOverlays) {
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
