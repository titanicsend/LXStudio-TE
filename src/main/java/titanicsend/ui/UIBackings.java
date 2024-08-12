/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

import static org.lwjgl.bgfx.BGFX.*;

import heronarts.glx.DynamicVertexBuffer;
import heronarts.glx.GLX;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.studio.TEApp;
import heronarts.lx.transform.LXVector;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TEPanelModel;

public class UIBackings extends UI3dComponent {

  private class PanelBuffer extends VertexBuffer {

    private PanelBuffer(GLX glx) {
      super(glx, VERTICES_PER_PANEL, VertexDeclaration.ATTRIB_POSITION);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      LXVector[] inner = initPanel.offsetTriangles.inner;
      putVertex(inner[0].x, inner[0].y, inner[0].z);
      putVertex(inner[1].x, inner[1].y, inner[1].z);
      putVertex(inner[2].x, inner[2].y, inner[2].z);
    }
  }

  private final TEVirtualOverlays virtualOverlays;
  private final GLX glx;
  private final List<PanelBuffer> panels = new ArrayList<PanelBuffer>();
  private TEPanelModel initPanel;
  private static final int VERTICES_PER_PANEL = 3;
  private int numModels;

  private DynamicVertexBuffer colorBuffer;

  private final Matrix4f modelMatrix = new Matrix4f();
  private final FloatBuffer modelMatrixBuf;

  public UIBackings(GLX glx, final TEVirtualOverlays virtualOverlays) {
    this.virtualOverlays = virtualOverlays;
    this.glx = glx;
    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);

    // if using the static model, we can build our backing panels now
    // TODO - remove when switch to dynamic model is complete
    if (TEApp.wholeModel.isStatic()) {
      rebuild();
    }
  }

   /** Build backing panels for the current model. Called when the model is loaded or changed. */
  public void rebuild() {
    clear();

    List<TEPanelModel> models = TEApp.wholeModel.getPanels();
    numModels = models.size();
    int colorBufferMaxSize = Math.max(1, this.numModels) * VERTICES_PER_PANEL;

    for (TEPanelModel m : models) {
      initPanel = m;
      panels.add(new PanelBuffer(glx));
    }

    this.colorBuffer =
      new DynamicVertexBuffer(glx, colorBufferMaxSize, VertexDeclaration.ATTRIB_COLOR0);
  }

  // Free resources allocated by previous model
  public void clear() {
    for (PanelBuffer b : this.panels) {
      b.dispose();
    }
    this.panels.clear();
    if (this.colorBuffer != null) this.colorBuffer.dispose();
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (!this.virtualOverlays.opaqueBackPanelsVisible.isOn() || UI3DManager.isRebuildLocked()) {
      return;
    }

    UI3DManager.lockDraw();

    // Update the color data
    final ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();

    // TODO: fix variable opacity - may require a (BGFX) shader
    final int panelColor =
        LXColor.toABGR(
            LXColor.rgba(0, 0, 0, (int) this.virtualOverlays.backingOpacity.getNormalized() * 255));
    for (int i = 0; i < numModels; i++) {
      colorData.putInt(panelColor);
      colorData.putInt(panelColor);
      colorData.putInt(panelColor);
    }
    colorData.rewind();
    this.colorBuffer.update();

    final long state =
        BGFX_STATE_PT_TRISTRIP
            | BGFX_STATE_WRITE_RGB
            | BGFX_STATE_WRITE_A
            | BGFX_STATE_WRITE_Z
            | BGFX_STATE_DEPTH_TEST_LESS;

    int vertexIndex = 0;
    for (PanelBuffer b : panels) {
      bgfx_set_transform(this.modelMatrix.get(this.modelMatrixBuf));
      bgfx_set_dynamic_vertex_buffer(
          1, this.colorBuffer.getHandle(), vertexIndex++ * VERTICES_PER_PANEL, VERTICES_PER_PANEL);
      ui.lx.program.vertexFill.submit(view, state, b);
    }
    UI3DManager.unlockDraw();
  }

  @Override
  public void dispose() {
    // make sure we're not in the middle of a model rebuild when we
    // start destroying objects.
    while (UI3DManager.isRebuildLocked()) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        ;
      }
    }

    // Take draw lock to keep the model from being rebuilt while we're
    // disposing of objects.
    UI3DManager.lockDraw();
    for (PanelBuffer b : this.panels) {
      b.dispose();
    }
    this.panels.clear();

    // free borrowed BGFX resources if they exist
    if (this.colorBuffer != null) this.colorBuffer.dispose();
    MemoryUtil.memFree(this.modelMatrixBuf);
    UI3DManager.unlockDraw();
    super.dispose();
  }
}
