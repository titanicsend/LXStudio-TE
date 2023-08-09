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

import static org.lwjgl.bgfx.BGFX.BGFX_STATE_ALPHA_REF;
import static org.lwjgl.bgfx.BGFX.BGFX_STATE_BLEND_ALPHA;
import static org.lwjgl.bgfx.BGFX.BGFX_STATE_DEPTH_TEST_LESS;
import static org.lwjgl.bgfx.BGFX.BGFX_STATE_PT_TRISTRIP;
import static org.lwjgl.bgfx.BGFX.BGFX_STATE_WRITE_RGB;
import static org.lwjgl.bgfx.BGFX.BGFX_STATE_WRITE_Z;
import static org.lwjgl.bgfx.BGFX.bgfx_set_dynamic_vertex_buffer;
import static org.lwjgl.bgfx.BGFX.bgfx_set_transform;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.DynamicVertexBuffer;
import heronarts.glx.GLX;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.studio.TEApp;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TEPanelModel;

public class UIBackings extends UI3dComponent {

  private class PanelBuffer extends VertexBuffer {

    // To use for setting color by panel type
    private final TEPanelModel panel;

    private PanelBuffer(GLX glx, TEPanelModel panel) {
      super(glx, VERTICES_PER_PANEL,  VertexDeclaration.ATTRIB_POSITION);
      this.panel = panel;
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      putVertex(initPanel.v0.x, initPanel.v0.y, initPanel.v0.z);
      putVertex(initPanel.v1.x, initPanel.v1.y, initPanel.v1.z);
      putVertex(initPanel.v2.x, initPanel.v2.y, initPanel.v2.z);
    }
  }

  private final TEVirtualOverlays virtualOverlays;
  private final List<PanelBuffer> panels = new ArrayList<PanelBuffer>();
  private TEPanelModel initPanel;
  private static final int VERTICES_PER_PANEL = 3;
  private final int numModels;
  private final int colorBufferMaxSize;

  private final DynamicVertexBuffer colorBuffer;

  private final Matrix4f modelMatrix = new Matrix4f();
  private final FloatBuffer modelMatrixBuf;

  public UIBackings(GLX glx, final TEVirtualOverlays virtualOverlays) {
    this.virtualOverlays = virtualOverlays;
    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);

    this.numModels = TEApp.wholeModel.panelsById.size();
    this.colorBufferMaxSize = this.numModels * VERTICES_PER_PANEL;    

    for (TEPanelModel m : TEApp.wholeModel.panelsById.values()) {
      initPanel = m;
      panels.add(new PanelBuffer(glx, m));
    }

    this.colorBuffer = new DynamicVertexBuffer(glx, this.colorBufferMaxSize, VertexDeclaration.ATTRIB_COLOR0);
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (!this.virtualOverlays.opaqueBackPanelsVisible.isOn()) {
      return;
    }

    // Update the color data
    final ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();

    // TODO: vary color by panel type
    // TODO: improve alpha transparency
    final int panelColor = LXColor.toABGR(LXColor.rgba(0, 0, 0, (int)(this.virtualOverlays.backingOpacity.getNormalized() * 255)));
    for (int i=0; i < numModels; i++) {
      colorData.putInt(panelColor);
      colorData.putInt(panelColor);
      colorData.putInt(panelColor);
    }
    colorData.rewind();
    this.colorBuffer.update();

    final long state =
        BGFX_STATE_PT_TRISTRIP |
        BGFX_STATE_WRITE_RGB |
        BGFX_STATE_WRITE_Z |
        BGFX_STATE_BLEND_ALPHA |
        BGFX_STATE_ALPHA_REF(32) |
        BGFX_STATE_DEPTH_TEST_LESS;

    int vertexIndex = 0;
    for (PanelBuffer b : panels) {
      bgfx_set_transform(this.modelMatrix.get(this.modelMatrixBuf));
      bgfx_set_dynamic_vertex_buffer(1, this.colorBuffer.getHandle(), vertexIndex++ * VERTICES_PER_PANEL, VERTICES_PER_PANEL);
      ui.lx.program.vertexFill.submit(view, state, b);
    }
  }

  @Override
  public void dispose() {
    for (PanelBuffer b : this.panels) {
      b.dispose();
    }
    this.panels.clear();
    this.colorBuffer.dispose();
    MemoryUtil.memFree(this.modelMatrixBuf);
    super.dispose();
  }
}
