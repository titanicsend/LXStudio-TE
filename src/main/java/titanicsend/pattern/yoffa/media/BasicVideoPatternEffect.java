package titanicsend.pattern.yoffa.media;

import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.Dimensions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Example to show how we could map a video onto our panels
// Not intended from production use
// Video clip has NOT been confirmed as licensed for re-use
@LXCategory("Video Examples")
public class BasicVideoPatternEffect extends PatternEffect {

    private static final String DEFAULT_VID_PATH = "resources/pattern/sizzle.mov";

    private final String vidPath;
    private VideoPainter videoPainter;

    public BasicVideoPatternEffect(PatternTarget target) {
        super(target);
        this.vidPath = DEFAULT_VID_PATH;
    }


    public BasicVideoPatternEffect(PatternTarget target, String vidPath) {
        super(target);
        this.vidPath = vidPath;
    }

    @Override
    public void onPatternActive() {
        try {
            videoPainter = new VideoPainter(vidPath, pattern.getColors());
            videoPainter.startVideo();
        } catch (Exception e) {
            //fail silently so we can swap out videos live
        }
    }

    @Override
    public void run(double deltaMs) {
        try {
            videoPainter.grabFrame();

            for (Map.Entry<LXPoint, Dimensions> pointToCanvas : pointsToCanvas.entrySet()) {
                videoPainter.paint(pointToCanvas.getKey(), pointToCanvas.getValue());
            }
        } catch (Exception e) {
            //fail silently so we can swap out videos live
            //when we live swap, we need to start the video again and i wasn't able to find a good way to detect this in 5min so hacky hack
            try {
                videoPainter.startVideo();
            } catch (Exception e2) {
                //fail silent
            }
        }
    }

    @Override
    public void onPatternInactive() {
        try {
            if (videoPainter != null) {
                videoPainter.stopVideo();
            }
        } catch (Exception e) {
            //fail silently so we can swap out videos live
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of();
    }

}
