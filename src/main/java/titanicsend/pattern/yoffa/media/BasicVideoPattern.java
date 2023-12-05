package titanicsend.pattern.yoffa.media;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.io.IOException;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

// Example to show how we could map a video onto our panels
// Not intended from production use
// Video clip has NOT been confirmed as licensed for re-use
@LXCategory("TE Examples")
public class BasicVideoPattern extends TEPerformancePattern {

    private static final String VID_PATH = "resources/pattern/test12.mov";

    private final VideoPainter videoPainter;

    public BasicVideoPattern(LX lx) throws IOException {
        super(lx, TEShaderView.ALL_POINTS);

        addCommonControls();

        videoPainter = new VideoPainter(VID_PATH);
    }

    @Override
    public void onActive() {
        super.onActive();
        videoPainter.initColors(getColors());
        videoPainter.startVideo();
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        clearPixels();

        videoPainter.grabFrame();
        videoPainter.paint(getModel().getPoints());
    }

    @Override
    public void onInactive() {
        videoPainter.stopVideo();
    }
}
