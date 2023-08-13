/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
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

package titanicsend.dmx;

import heronarts.lx.LX;
import heronarts.lx.output.ArtNetDatagram;
import heronarts.lx.output.IndexBuffer;
import heronarts.lx.output.LXBufferOutput.ByteEncoder;
import heronarts.lx.output.LXOutput;
import heronarts.lx.output.LXOutput.GammaTable.Curve;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.output.IndexBuffer.Segment;
import titanicsend.dmx.model.DmxModel.DmxOutputDefinition;
import titanicsend.dmx.parameter.DmxParameter;

public class DmxOutput extends LXOutput {

  private final DmxOutputDefinition definition;

  private ArtNetDatagram dg;

  private DmxBuffer dmx;

  private class DmxByteEncoder implements ByteEncoder {

    @Override
    public int getNumBytes() {
      return definition.numBytes;
    }

    /**
     * Write actual DMX output to the byte[] array.
     * Because we claimed a single-length index buffer, this will get called once.
     */
    @Override
    public void writeBytes(int argb, Curve gamma, byte[] output, int offset) {
      for (int i = 0; i < dmx.array.length; i++) {
        DmxParameter p = dmx.array[i];
        p.writeBytes(output, offset);
        offset += p.getNumBytes();
      }
    }
  }

  private final DmxByteEncoder byteEncoder;

  public DmxOutput(LX lx, DmxOutputDefinition definition) {
    super(lx);
    this.definition = definition;
    this.byteEncoder = new DmxByteEncoder();
    int[] indices = { 0 };
    Segment segment = new Segment(indices, this.byteEncoder, this.definition.channel);

    this.dg = new ArtNetDatagram(this.lx, new IndexBuffer(segment), definition.universe);
    this.dg.setAddress(definition.address);
    this.dg.setSequenceEnabled(definition.sequenceEnabled);
    this.dg.framesPerSecond.setValue(definition.fps);

    setGammaDelegate(lx.engine.output);
    this.dg.setGammaDelegate(lx.engine.output);
  }

  /**
   * Call before send() to pass the DMX values to the output
   */
  public void setDmxData(DmxBuffer dmx) {
    this.dmx = dmx;
  }

  private final int[] dummyColors = { 0 };

  /**
   * Entry point for sending DMX output.  
   * Internally a custom ByteEncoder will write the staged DMX data.
   */
  @Override
  public LXOutput send(int[] colors) {
    this.dg.send(dummyColors);
    return this;
  }

  @Override
  protected void onSend(int[] colors, GammaTable glut, double brightness) {
    // We're just a wrapper, the private datagram does the real work.
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.enabled) {
      dg.enabled.setValue(this.enabled.getValueb());
    }
  }

  @Override
  public void dispose() {
    dg.dispose();
    super.dispose();
  }
}
