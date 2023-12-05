package titanicsend.pattern.yoffa.media;

import heronarts.lx.model.LXPoint;
import java.util.List;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import titanicsend.util.TE;

public class VideoPainter {

    private final FFmpegFrameGrabber frameGrabber;
    private final Java2DFrameConverter frameConverter;
    private int[] colors;

    private ImagePainter currentFramePainter;

    public VideoPainter(String vidPath) {
        this(vidPath, null);
    }

    public VideoPainter(String vidPath, int[] colors) {
        this.frameGrabber = new FFmpegFrameGrabber(vidPath);
        this.frameConverter = new Java2DFrameConverter();
        this.colors = colors;
        this.currentFramePainter = null;
    }

    /**
     * The colors[] array is no longer available to patterns at constructor time
     * so this will need to be called from onActive()
     */
    public void initColors(int[] colors) {
        this.colors = colors;
    }

    public void grabFrame() {
        try {
            Frame frame = null;
            frame = frameGrabber.grabImage();

            if (frame != null) {
                if (frame.image != null) {
                    currentFramePainter = new ImagePainter(frameConverter.convert(frame), colors);
                }
            } else {
                // loop by default
                restartVideo();
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void paint(List<LXPoint> points) {
        if (currentFramePainter != null) {
            for (LXPoint point : points) {
                currentFramePainter.paint(point);
            }
        }
    }

    public void paint(List<LXPoint> points, double scaleRatio) {
        if (currentFramePainter != null) {
            for (LXPoint point : points) {
                currentFramePainter.paint(point, scaleRatio);
            }
        }
    }

    public void paint(LXPoint point) {
        if (currentFramePainter != null) {
            currentFramePainter.paint(point);
        }
    }

    public void paint(LXPoint point, double scaleRatio) {
        if (currentFramePainter != null) {
            currentFramePainter.paint(point, scaleRatio);
        }
    }

    public void startVideo() {
        if (colors == null) {
            TE.err("VideoPainter needs colors[] array before startVideo() is called.");
            return;
        }
        try {
            frameGrabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopVideo() {
        try {
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void restartVideo() {
        try {
            frameGrabber.restart();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }
}
