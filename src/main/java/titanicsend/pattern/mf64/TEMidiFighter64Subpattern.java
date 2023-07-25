package titanicsend.pattern.mf64;

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

    /*
      Fast MF64 pixel blending methods have three goals:
        1 - make sure something interesting happens when the user presses buttons
        2 - make sure that there's nothing the user can do to make the car look really
            terrible (or blow it up, or set it on fire)
        3 - be as fast as possible
    */

    /**
     * Set pixel to specified color at full opacity.  Assumes that the color
     * value already has the desired brightness.
     */
    protected void setColor(int index, int color) {
        driver.getColors()[index] = color | 0xFF000000;
    }

    /**
      Set pixel color at index with controlled "blending". Note that is fast
      rather than accurate blending. It simply takes the color with the highest
      alpha value.
    */
    protected void blendColor(int index, int color) {
        // the simple way:  highest alpha wins!
        int a1 = (0xff000000 & color) >>> 24;
        int a2 = (0xff000000 & driver.getColors()[index]) >>> 24;
        if (a1 > a2) driver.getColors()[index] = color;
    }

    /**
     Set pixel color only if underlying color hasn't previously been set (or has been
     deliberately cleared). This is used to let patterns like "flash" work as a colored
     backdrop for more complex patterns
     */
    protected void setColorAsBackground(int index, int color) {
        if (driver.getColors()[index] == 0) {
            driver.getColors()[index] = color;
        }
    }

    public abstract void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping);

    public abstract void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping);

    public abstract void run(double deltaMsec);
}
