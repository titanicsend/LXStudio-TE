package titanicsend.pattern.yoffa.media;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEPattern;

import java.io.IOException;
import java.util.List;

// Example to show how we could map a video onto our panels
// Not intended from production use
// Video clip has NOT been confirmed as licensed for re-use
@LXCategory("TE Examples")
public class BasicVideoPattern extends TEPattern {

    private static final String VID_PATH = "resources/pattern/test12.mov";

    private final BooleanParameter edges =
            new BooleanParameter("Edges", false);

    private final VideoPainter videoPainter;

    public BasicVideoPattern(LX lx) throws IOException {
        super(lx);
        addParameter(edges.getLabel(), edges);
        videoPainter = new VideoPainter(VID_PATH, colors);
    }

    @Override
    public void onActive() {
        videoPainter.startVideo();
    }

    @Override
    public void run(double deltaMs) {
        videoPainter.grabFrame();
        clearColors();
        if (edges.getValueb()) {
            videoPainter.paint(model.getAllEdges());
        } else {
            videoPainter.paint(model.getPanelsBySection(TEPanelSection.STARBOARD_AFT));
            for (TEPanelModel panel : model.getPanelsBySection(TEPanelSection.STARBOARD_FORE)) {
                videoPainter.paint(List.of(panel));
            }
        }
    }

    @Override
    public void onInactive() {
        videoPainter.stopVideo();
    }

}
