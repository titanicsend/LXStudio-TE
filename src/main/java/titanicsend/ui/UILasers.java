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
import static org.lwjgl.bgfx.BGFX.bgfx_set_transform;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.VertexBuffer;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.studio.TEApp;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.model.TELaserModel;
import titanicsend.model.TEWholeModel;

public class UILasers extends UI3dComponent {

  private final TEWholeModel wholeModel;

  private final TEVirtualOverlays virtualOverlays;
  private final VertexBuffer vertexBuffer;

  private final Matrix4f modelMatrix = new Matrix4f();
  private final FloatBuffer modelMatrixBuf;

  // This array is populated and de-populated by the LX thread, but
  // accessed for drawing by the UI thread
  private final List<TELaserModel> models = new CopyOnWriteArrayList<TELaserModel>();

  public UILasers(GLX glx, final TEVirtualOverlays virtualOverlays) {
    this.wholeModel = TEApp.wholeModel;
    this.virtualOverlays = virtualOverlays;
    this.vertexBuffer = new VertexBuffer.UnitCube(glx);
    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);

    models.addAll(wholeModel.lasersById.values());
  }

  @Override
  public void onDraw(UI ui, View view) {
    if (!this.virtualOverlays.lasersVisible.isOn()) {
      return;
    }

    final long state =
        BGFX_STATE_PT_TRISTRIP |
        BGFX_STATE_WRITE_RGB |
        BGFX_STATE_WRITE_Z |
        BGFX_STATE_BLEND_ALPHA |
        BGFX_STATE_ALPHA_REF(32) |
        BGFX_STATE_DEPTH_TEST_LESS;

    for (TELaserModel laser : models) {

      this.modelMatrix
      .identity()
      .translate(laser.origin.x, laser.origin.y, laser.origin.z)
      .scale(this.virtualOverlays.laserBoxSize.getValuef());

      bgfx_set_transform(this.modelMatrix.get(this.modelMatrixBuf));

      // TODO: make color access thread safe?
      ui.lx.program.uniformFill.submit(view, state, laser.color, this.vertexBuffer);
    }
  }

  @Override
  public void dispose() {
    this.vertexBuffer.dispose();
    MemoryUtil.memFree(this.modelMatrixBuf);
    super.dispose();
  }
}
