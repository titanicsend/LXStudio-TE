package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.pixelblaze.PixelblazePattern;

@LXCategory("Test")
public class Fireflies extends PixelblazePattern {

    public Fireflies(LX lx) {
        super(lx);
        enablePanels.setValue(false);
    }

    @Override
    protected String getScriptName() {
        return "fireflies";
    }
}