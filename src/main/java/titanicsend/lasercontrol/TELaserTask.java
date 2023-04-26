package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TE;

public class TELaserTask implements LXLoopTask {
    public LX lx;
    //public int oldPrimaryHue = -1;
    public TELaserTask(LX lx) {
        this.lx = lx;
    }

    @Override
    public void loop(double deltaMs) {
//        LXDynamicColor primary = this.lx.engine.palette.swatch.getColor(TEPattern.ColorType.PRIMARY.index);
        int swatchIndex = this.lx.engine.palette.swatch.getIndex();
        TE.log("Swatch index: %s", swatchIndex);
//        int rgb = primary.getColor();
//        int hue = (int) (360 * LXColor.h(rgb));
//        TE.log("Sending palette: %d", hue);
//        TEOscMessage.sendOscToPangolin(lx, TEOscMessage.makePaletteHueAddress(), hue);
    }
}
