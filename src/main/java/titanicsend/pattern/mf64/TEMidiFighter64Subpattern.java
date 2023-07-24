package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

public abstract class TEMidiFighter64Subpattern {
    protected final TEMidiFighter64DriverPattern driver;
    protected TEWholeModel modelTE;

    // array to hold the current color for each button
    protected int[] overlayColors;

    protected ButtonColorMgr buttons;

    protected TEMidiFighter64Subpattern(TEMidiFighter64DriverPattern driver) {
        this.driver = driver;
        this.modelTE = this.driver.getModelTE();
        this.buttons = new ButtonColorMgr();
        this.overlayColors = this.driver.overlayColors;
    }

    // set pixel color at index with controlled "blending". Note that this isn't
    // "accurate" blending.  It has three goals:
    // 1 - make sure that *something* happens when the user presses controls in
    // multiple rows or columns
    // 2 - make sure that there's nothing the user can do to make the car look really
    // terrible (or blow it up).
    // 3 - third, to be as computationally efficient as possible.
    // (Eventually, this should really be done on the GPU!)
    protected void setColor(int index, int color) {
        // the simple way:  highest alpha wins!
        int a1 = (0xff000000 & color) >>> 24;
        int a2 = (0xff000000 & driver.getColors()[index]) >>> 24;
        if (a1 > a2) driver.getColors()[index] = color;
    }

    public abstract void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping);

    public abstract void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping);

    public void run(double deltaMsec) {

    };
}
