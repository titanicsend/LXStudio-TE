/**
 * Portions adapted from Chromatik UI source
 *
 * <p>Copyright 2023- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.ui;

import heronarts.glx.*;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.studio.TEApp;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEVertex;
import titanicsend.ui.text3d.Label;
import titanicsend.ui.text3d.TextManager3d;

public class UIModelLabels extends UI3dComponent {
  private final TEVirtualOverlays virtualOverlays;
  private final GLX glx;
  private TextManager3d textManager = null;
  private final List<Label> panelLabels = new ArrayList<Label>();
  private final List<Label> vertexLabels = new ArrayList<Label>();

  // initialization that should be done once, when the UI is ready
  public UIModelLabels(GLX glx, final TEVirtualOverlays virtualOverlays) {
    this.virtualOverlays = virtualOverlays;
    this.glx = glx;

    // Create text manager object and load font texture atlas
    // (Inconsolata is a nicely readable, open source font)
    this.textManager = new TextManager3d(glx, "resources/fonts/Inconsolata.font3d");
  }

  /** Build labels for the current model. Called when the model is loaded or changed. */
  public void rebuild() {
    float fontScale = 0.5f;
    float labelOffset = 5f;

    // clean up any existing labels
    clear();

    Vector3f position = new Vector3f(0, 0, 0);
    Vector3f rotation = new Vector3f(0, 0, 0);

    // create labels for panels
    textManager.setFontScale(fontScale);
    textManager.setFontColor(LXColor.WHITE);
    textManager.setFontBackground(LXColor.rgba(0, 0, 0, 255)); // opaque black

    for (TEPanelModel p : TEApp.wholeModel.getPanels()) {
      getPanelCoordinates(p, position, rotation, labelOffset);
      panelLabels.add(textManager.labelMaker(p.getId(), position, rotation));
    }

    // create labels for vertices
    textManager.setFontScale(fontScale * 1.333f);
    textManager.setFontColor(LXColor.GREEN);
    textManager.setFontBackground(LXColor.rgba(0, 64, 64, 200));
    for (TEVertex v : TEApp.wholeModel.getVertexes()) {
      getVertexCoordinates(v, position, rotation, labelOffset);
      vertexLabels.add(textManager.labelMaker(String.valueOf(v.id), position, rotation));
    }
  }

  /**
   * Free objects, native memory and GPU resources associated with the current model. Should be
   * called when the model is changed or the UI is closed before loading labels for the new model
   */
  public void clear() {
    for (Label l : panelLabels) {
      l.dispose();
    }
    for (Label l : vertexLabels) {
      l.dispose();
    }
    panelLabels.clear();
    vertexLabels.clear();
  }

  /**
   * Given the coordinates of a label center on the model, return the offset position and the
   * rotation needed to orient the label correctly.
   *
   * @param inCenter - label center
   * @param outPosition - offset position of label center
   * @param outRotation - rotations (radians) needed to orient the label correctly
   */
  public void getLabelCoordinates(
      Vector3f inCenter,
      Vector3f outPosition,
      Vector3f outRotation,
      float offset,
      boolean onCarEnd) {

    outPosition.set(inCenter);
    outRotation.set(0, 0, 0);

    // if the label is on the end of the car, displace x outward and build the
    // texture box in z and y, with offset x-ward.  On the lowest fore/aft panels,
    // which are highly angled, increase the offset to bump the labels outward a
    // little extra. Also flip fore/aft labels by 180 degrees as needed to keep
    // text oriented correctly.
    if (onCarEnd) {
      outPosition.x += ((outPosition.x > 0) ? 1 : -1) * offset * ((outPosition.y < 1f) ? 1.5f : 1);
      outRotation.y += (outPosition.x > 0) ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
    }
    // otherwise, displace z and build the box on the x/y plane
    // (by rotating 90 degrees about y)
    else {
      outPosition.z += ((outPosition.z > 0) ? 1 : -1) * offset;
      outRotation.y = (outPosition.z > 0) ? (float) Math.PI : 0;
    }
  }

  /**
   * STATIC MODEL VARIANT - Remove when switch to dynamic model is complete
   *
   * <p>Given the coordinates of a label center on the model, return the offset position and the
   * rotation needed to orient the label correctly.
   *
   * @param inCenter - label center
   * @param outPosition - offset position of label center
   * @param outRotation - rotations (radians) needed to orient the label correctly
   */
  public void getLabelCoordinatesStatic(
      Vector3f inCenter, Vector3f outPosition, Vector3f outRotation, float offset) {
    outPosition.set(inCenter);
    outRotation.set(0, 0, 0);

    // if the label is on the end of the car, displace z outward and build the
    // texture box in x and y, with offset z-ward.  On the lowest fore/aft panels,
    // which are highly angled, increase the offset to bump the labels outward a
    // little extra. Also flip fore/aft labels by 180 degrees as needed to keep
    // text oriented correctly.
    if (Math.abs(outPosition.x) < 1200000) {
      outPosition.z +=
          ((outPosition.z > 0) ? 1 : -1) * offset * ((outPosition.y < 1000000f) ? 1.5f : 1);
      outRotation.y += (outPosition.z > 0) ? (float) Math.PI : 0;

    }
    // otherwise, displace x and build the box on the z/y plane
    // (by rotating 90 degrees about y)
    else {
      outPosition.x += ((outPosition.x > 0) ? 1 : -1) * offset;
      outRotation.y = (float) Math.PI / 2f + ((outPosition.x > 0) ? (float) Math.PI : 0);
    }
  }

  public void getPanelCoordinates(
      TEPanelModel panel, Vector3f position, Vector3f rotation, float offset) {
    Vector3f panelCenter = new Vector3f(panel.centroid.x, panel.centroid.y, panel.centroid.z);
    boolean onCarEnd = panel.getId().startsWith("F") || panel.getId().startsWith("A");

    getLabelCoordinates(panelCenter, position, rotation, offset, onCarEnd);
  }

  public void getVertexCoordinates(
      TEVertex vertex, Vector3f position, Vector3f rotation, float offset) {
    Vector3f panelCenter = new Vector3f(vertex.x, vertex.y, vertex.z);

    getLabelCoordinates(panelCenter, position, rotation, offset, false);
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (UI3DManager.labelsLocked()) return;

    // try to acquire the backings lock, if we can't, skip this draw
    boolean inDraw = false;
    try {
      inDraw = UI3DManager.labelsLock.tryLock(0, java.util.concurrent.TimeUnit.MILLISECONDS);
      if (inDraw) {
        if (this.virtualOverlays.panelLabelsVisible.isOn()) {
          textManager.draw(view, panelLabels);
        }

        // TODO(look): I wasn't sure how to deal with this code while removing static model support.
        /*
        // TODO - fill in vertex locations for dynamic model.  It currently has
        // TODO - labels, but no data, so we only show these labels for the static model.
        if (this.virtualOverlays.vertexLabelsVisible.isOn() && this.isStatic) {
          textManager.draw(view, vertexLabels);
        }
        */
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (inDraw) {
        UI3DManager.labelsLock.unlock();
      }
    }
  }

  @Override
  public void dispose() {

    super.dispose();

    boolean inDispose = false;

    // try to acquire the labels lock.  Here, we can afford to wait a while for it because
    // we're disposing of the object, and everything else that uses the lock should be
    // either stopped or stopping
    try {
      inDispose = UI3DManager.labelsLock.tryLock(100, java.util.concurrent.TimeUnit.MILLISECONDS);
      if (inDispose) {

        // free native and GPU resources
        for (Label l : panelLabels) {
          l.dispose();
        }
        for (Label l : vertexLabels) {
          l.dispose();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (inDispose) {
        UI3DManager.labelsLock.unlock();
      }
    }
  }
}
