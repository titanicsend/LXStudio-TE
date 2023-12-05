package titanicsend.pattern.yoffa.media;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.utils.LXUtils;
import java.io.IOException;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

// Example to show how we could map a video onto our panels
// Scales up with sound reactivity
// Not intended from production use
// Video clip has NOT been confirmed as licensed for re-use
@LXCategory("Video Examples")
public class ReactiveHeartPattern extends TEPerformancePattern {

    private static final String VID_PATH = "resources/pattern/heart.mov";

    private final VideoPainter videoPainter;

    public ReactiveHeartPattern(LX lx) throws IOException {
        super(lx, TEShaderView.ALL_POINTS);

        addCommonControls();

        videoPainter = new VideoPainter(VID_PATH);
    }

    @Override
    public void onActive() {
        videoPainter.initColors(getColors());
        videoPainter.startVideo();
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        double scaledTrebleRatio = LXUtils.clamp((getTrebleRatio() - .5) / (1.01 - .7) / 6 - .2 + .7 / 2, 0, 1);
        double scaledRatio = 1 + scaledTrebleRatio * 2;

        videoPainter.grabFrame();
        videoPainter.paint(getModel().getPoints(), scaledRatio);
    }

    @Override
    public void onInactive() {
        videoPainter.stopVideo();
    }
}
