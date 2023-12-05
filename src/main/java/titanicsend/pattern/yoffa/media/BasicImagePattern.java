package titanicsend.pattern.yoffa.media;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.io.IOException;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

// This is just an example to show how we could map an image onto a set of our panels
// Not intended from production use
@LXCategory("TE Examples")
public class BasicImagePattern extends TEPerformancePattern {

    private static final String IMG_PATH = "resources/pattern/eddie.jpg";

    private final ImagePainter eddiePainter;

    public BasicImagePattern(LX lx) throws IOException {
        super(lx, TEShaderView.ALL_POINTS);

        addCommonControls();

        eddiePainter = new ImagePainter(IMG_PATH);
    }

    @Override
    public void onActive() {
        super.onActive();
        eddiePainter.initColors(getColors());
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        for (LXPoint p : getModel().getPoints()) {
            eddiePainter.paint(p);
        }
    }
}
