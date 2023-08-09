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

import heronarts.lx.utils.LXUtils;
import titanicsend.dmx.model.DmxWholeModel;
import titanicsend.dmx.parameter.DmxParameter;

/**
 * A generic DMX blender used for any DmxParameter.
 */
public class DmxBlend {

  public void blend(DmxBuffer[] dst, DmxBuffer[] src, double alpha, DmxFullBuffer buffer, DmxWholeModel model) {
    blend(dst, src, alpha, buffer.getArray(), model);
  }

  /**
   * Blends the src buffer onto the destination buffer at the specified alpha amount.
   *
   * @param dst Destination buffer (lower layer)
   * @param src Source buffer (top layer)
   * @param alpha Alpha blend, from 0-1
   * @param output Output buffer, which may be the same as src or dst
   * @param model A model which indicates the set of points to blend
   */
  public void blend(DmxBuffer[] dst, DmxBuffer[] src, double alpha, DmxBuffer[] output, DmxWholeModel model) {
    for (int i = 0; i < dst.length; i++) {
      DmxBuffer d = dst[i];
      DmxBuffer s = src[i];
      DmxBuffer o = output[i];

      if (d.isActive) {
        if (s.isActive) {
          // Both active
          for (int j = 0; j < d.array.length; j++) {
            DmxParameter dp = d.array[j];
            DmxParameter sp = s.array[j];
            DmxParameter op = o.array[j];

            switch (dp.getBlendMode()) {
            case LERP:
              op.setValue(LXUtils.lerp(dp.getValue(), sp.getValue(), alpha));
              break;
            case JUMP_START:
              if (alpha == 0) {
                op.setDmxValue(dp.getDmxValue(alpha));
              } else {
                op.setDmxValue(sp.getDmxValue(alpha));
              }
              break;
            default:
            case JUMP_END:
              if (alpha == 1) {
                op.setDmxValue(sp.getDmxValue(alpha));
              } else {
                op.setDmxValue(dp.getDmxValue(alpha));
              }
              break;          
            }
          }
        } else {
          // Only d active
          DmxBlend.copyTo(d, o, 1 - alpha);
        }
        o.isActive = true;
      } else {
        if (s.isActive) {
          // Only s active
          DmxBlend.copyTo(s, o, alpha);
          o.isActive = true;
        } else {
          // Neither is active for this fixture
          o.isActive = false;
        }
      }
    }
  }

  static public void copyTo(DmxBuffer from, DmxBuffer to, double alpha) {
    for (int i = 0; i < from.array.length; i++) {      
      DmxParameter paramFrom = from.array[i];
      DmxParameter paramTo = to.array[i];
      paramTo.setDmxValue(paramFrom.getDmxValue(alpha));
    }
  }

  /**
   * Transitions from one buffer to another. By default, this is used by first
   * blending from-to with alpha 0-1, then blending to-from with
   * alpha 1-0. Blends which are asymmetrical may override this method for
   * custom functionality. This method is used by pattern transitions on
   * channels as well as the crossfader.
   *
   * @param from First buffer
   * @param to Second buffer
   * @param amt Interpolation from-to (0-1)
   * @param output Output buffer, which may be the same as from or to
   * @param model The model with points that should be blended
   */
  public void lerp(DmxBuffer[] from, DmxBuffer[] to, double amt, DmxBuffer[] output, DmxWholeModel model) {
    DmxBuffer[] dst, src;
    double alpha;
    if (amt <= 0.5) {
      dst = from;
      src = to;
      alpha = amt * 2.;
    } else {
      dst = to;
      src = from;
      alpha = (1-amt) * 2.;
    }
    blend(dst, src, alpha, output, model);
  }
}
