package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

public class MF64FlashPattern extends TEMidiFighter64Subpattern {
    private boolean active;

    public MF64FlashPattern(TEMidiFighter64DriverPattern driver) {
        super(driver);
        active = false;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.active = false;
    }

    private void paintAll(int color) {
        // flash uses setColorAsBackground which only paints previously transparent pixels
        // so that it will automatically work as a background for other patterns
        for (LXPoint point : this.modelTE.getPoints()) {
            setColorAsBackground(point.index, color);
        }
    }

    @Override
    public void run(double deltaMsec) {
        if (active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
