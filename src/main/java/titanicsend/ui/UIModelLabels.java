/**
 * Portions adapted from Chromatik UI source
 *
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.ui;

import heronarts.glx.*;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.studio.TEApp;
import org.joml.Vector3f;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEVertex;
import titanicsend.ui.text3d.Label;
import titanicsend.ui.text3d.TextManager3d;
import titanicsend.util.TE;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class UIModelLabels extends UI3dComponent {
  private final TEVirtualOverlays virtualOverlays;
  private TextManager3d textManager;
  private final List<Label> panelLabels = new ArrayList<Label>();
  private final List<Label> vertexLabels = new ArrayList<Label>();

  public UIModelLabels(GLX glx, final TEVirtualOverlays virtualOverlays) {

    this.virtualOverlays = virtualOverlays;

    // Create text manager object and load ttf font. Inconsolata is a decently readable
    // public domain font.  Hey, I could've used Comic Sans!
    try {
      this.textManager = new TextManager3d(glx, new FileInputStream("resources/fonts/Inconsolata.ttf"), 24);
    } catch (Exception e) {
      TE.log("WARNING:  Unable to load specified 3d font, using default.");
      this.textManager = new TextManager3d(glx, 24);
    }

    Vector3f position = new Vector3f(0, 0, 0);
    Vector3f rotation = new Vector3f(0, 0, 0);

    // create labels for panels
    textManager.setFontScale(15000);
    textManager.setFontColor(LXColor.WHITE);
    textManager.setFontBackground(LXColor.rgba(0,0,0,255));  // opaque black
    for (TEPanelModel p : TEApp.wholeModel.panelsById.values()) {
      getPanelCoordinates(p, position, rotation);
      panelLabels.add(textManager.labelMaker(p.getId(), position, rotation));
    }

    // create labels for vertices
    textManager.setFontScale(20000);
    textManager.setFontColor(LXColor.GREEN);
    textManager.setFontBackground(LXColor.rgba(0,64,64,200));
    for (TEVertex v : TEApp.wholeModel.vertexesById.values()) {
      getVertexCoordinates(v, position, rotation);
      vertexLabels.add(textManager.labelMaker(String.valueOf(v.id), position, rotation));
    }
  }

  /**
   * Given the coordinates of a label center on the model, return the offset position and the
   * rotation needed to orient the label correctly.
   * @param inCenter - label center
   * @param outPosition - offset position of label center
   * @param outRotation - rotations (radians) needed to orient the label correctly
   */
  public void getLabelCoordinates(Vector3f inCenter, Vector3f outPosition, Vector3f outRotation, float offset) {
    outPosition.set(inCenter);
    outRotation.set(0,0,0);

    // if the label is on the end of the car, displace z outward and build the
    // texture box in x and y, with offset z-ward.  On the lowest fore/aft panels,
    // which are highly angled, increase the offset to bump the labels outward a
    // little extra. Also flip fore/aft labels by 180 degrees as needed to keep
    // text oriented correctly.
    if (Math.abs(outPosition.x) < 1200000) {
      outPosition.z += ((outPosition.z > 0) ? 1 : -1) * offset * ((outPosition.y < 1000000f) ? 1.5f : 1);
      outRotation.y += (outPosition.z > 0) ? (float) Math.PI : 0;

    }
    // otherwise, displace x and build the box on the z/y plane
    // (by rotating 90 degrees about y)
    else {
      outPosition.x += ((outPosition.x > 0) ? 1 : -1) * offset;
      outRotation.y = (float) Math.PI / 2f + ((outPosition.x > 0) ? (float) Math.PI : 0);
    }
  }

  public void getPanelCoordinates(TEPanelModel panel, Vector3f position, Vector3f rotation) {
    Vector3f panelCenter = new Vector3f(panel.centroid.x, panel.centroid.y, panel.centroid.z);
    getLabelCoordinates(panelCenter, position, rotation, 200000);
  }

  public void getVertexCoordinates(TEVertex vertex, Vector3f position, Vector3f rotation) {
    Vector3f panelCenter = new Vector3f(vertex.x, vertex.y, vertex.z);
    getLabelCoordinates(panelCenter, position, rotation, 210000);
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (this.virtualOverlays.panelLabelsVisible.isOn()) {
      textManager.draw(view, panelLabels);
    }
    if (this.virtualOverlays.vertexLabelsVisible.isOn()) {
      textManager.draw(view, vertexLabels);
    }
  }

  @Override
  public void dispose() {
    // free native and GPU resources
    super.dispose();
    for (Label l : panelLabels) {
      l.dispose();
    }
    for (Label l : vertexLabels) {
      l.dispose();
    }
  }
}
