package titanicsend.pattern.yoffa.media;

import heronarts.lx.model.LXPoint;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import titanicsend.model.TEModel;
import titanicsend.util.Dimensions;

import java.util.Collection;

public class VideoPainter {

    private final FFmpegFrameGrabber frameGrabber;
    private final Java2DFrameConverter frameConverter;
    private final int[] colors;

    private ImagePainter currentFramePainter;

    public VideoPainter(String vidPath, int[] colors) {
        this.frameGrabber = new FFmpegFrameGrabber(vidPath);
        this.frameConverter = new Java2DFrameConverter();
        this.colors = colors;
        this.currentFramePainter = null;
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
                //loop by default
                restartVideo();
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void paint(Collection<? extends TEModel> panels) {
        paint(panels, 1);
    }

    public void paint(Collection<? extends TEModel> panels, double scaleRatio) {
        if (currentFramePainter != null) {
            currentFramePainter.paint(panels, scaleRatio);
        }
    }

    public void paint(LXPoint point, Dimensions canvasDimensions) {
        if (currentFramePainter != null) {
            currentFramePainter.paint(point, canvasDimensions, 1);
        }
    }

    public void startVideo() {
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
