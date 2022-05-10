package titanicsend.pattern.yoffa.media;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.utils.LXUtils;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEAudioPattern;

import java.io.IOException;
import java.util.List;

// Example to show how we could map a video onto our panels
// Scales up with sound reactivity
// Not intended from production use
// Video clip has NOT been confirmed as licensed for re-use
@LXCategory("Video Examples")
public class ReactiveHeartPattern extends TEAudioPattern {

    private static final String VID_PATH = "resources/pattern/heart.mov";

    private final VideoPainter videoPainter;

    public ReactiveHeartPattern(LX lx) throws IOException {
        super(lx);
        videoPainter = new VideoPainter(VID_PATH, colors);
    }

    @Override
    public void onActive() {
        videoPainter.startVideo();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        double scaledTrebleRatio = LXUtils.clamp(
                (getTrebleRatio() - .5) / (1.01 - .7) / 6 -
                        .2 + .7 / 2,
                0, 1);
        double scaledRatio = 1 + scaledTrebleRatio*2;

        videoPainter.grabFrame();
        videoPainter.paint(model.getPanelsBySection(TEPanelSection.STARBOARD_AFT), scaledRatio);
        for (TEPanelModel panel : model.getPanelsBySection(TEPanelSection.STARBOARD_FORE)) {
            videoPainter.paint(List.of(panel), scaledRatio);
        }
    }

    @Override
    public void onInactive() {
        videoPainter.stopVideo();
    }

}
