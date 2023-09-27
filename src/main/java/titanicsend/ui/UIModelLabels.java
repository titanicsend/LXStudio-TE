/**
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
import heronarts.lx.studio.TEApp;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TEPanelModel;
import titanicsend.ui.text3d.Label;
import titanicsend.ui.text3d.TextManager3d;
import titanicsend.util.TE;
import java.io.FileInputStream;
import java.nio.FloatBuffer;

public class UIModelLabels extends UI3dComponent {


  private final TEVirtualOverlays virtualOverlays;
  private final int numModels;

  private TextManager3d textManager;

  private int VERTICES_PER_PANEL = 6;

  private final Matrix4f modelMatrix = new Matrix4f();
  private final FloatBuffer modelMatrixBuf;

  public UIModelLabels(GLX glx, final TEVirtualOverlays virtualOverlays) {

    this.virtualOverlays = virtualOverlays;
    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);

    // Create text manager object and load ttf font. Inconsolata is a decent looking
    // public domain font.  Hey, I could've used Comic Sans!
    try {
      this.textManager = new TextManager3d(glx, new FileInputStream("resources/fonts/Inconsolata.ttf"), 24);
    } catch (Exception e) {
      TE.log("WARNING:  Unable to load specified 3d font, using default.");
      this.textManager = new TextManager3d(glx, 24);
    }

    this.numModels = TEApp.wholeModel.panelsById.size();

    Vector3f position = new Vector3f(0,0,0);
    Vector3f rotation = new Vector3f(0,0,0);
    for (TEPanelModel p : TEApp.wholeModel.panelsById.values()) {
        getPanelCoordinates(p, position, rotation);
        Label b = new Label(p.getId(), position, rotation);
        textManager.addLabel(b);
    }
  }

  public void getPanelCoordinates(TEPanelModel panel, Vector3f position, Vector3f rotation) {
    position.x = panel.centroid.x;
    position.y = panel.centroid.y;
    position.z = panel.centroid.z;
    rotation.set(0,0,0);

    // if it's an end panel, displace z outward and build the
    // texture box in x and y, with offset z-ward.  Also flip
    // fore/aft labels 180 degrees as needed.
    if (Math.abs(position.x) < 1200000) {
      position.z += ((position.z > 0) ? 1 : -1) * 150000;
      rotation.y += (position.z > 0) ? (float) Math.PI : 0;

    }
    // otherwise, displace x and build the box on the z/y plane
    // (by rotating 90 degrees about y)
    else {
      position.x += ((position.x > 0) ? 1 : -1) * 120000;
      rotation.y = (float) Math.PI / 2f + ((position.x > 0) ? (float) Math.PI : 0);
    }
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (!this.virtualOverlays.panelLabelsVisible.isOn()) {
      return;
    }
    textManager.draw(ui, view);
  }

  @Override
  public void dispose() {
    MemoryUtil.memFree(this.modelMatrixBuf);
    super.dispose();
  }
}
