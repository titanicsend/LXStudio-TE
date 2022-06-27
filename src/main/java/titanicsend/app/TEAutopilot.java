package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.osc.OscMessage;
import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TEAutopilot implements LXLoopTask {
    private boolean enabled = false;
    private LX lx;

    // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html
    private ConcurrentLinkedQueue<OscMessage> unprocessedOscMessages;

    private TEMath.EMA bassBandFastAvg;
    private TEMath.EMA bassBandSlowAvg;

    private CircularArray bassBandFastHistory;
    private CircularArray bassBandSlowHistory;

    public TEAutopilot(LX lx) {
        this.lx = lx;
        this.unprocessedOscMessages = new ConcurrentLinkedQueue<OscMessage>();

        // FOR DEBUGGING
        this.enabled = true;

        // for moving averages
        bassBandFastAvg = new TEMath.EMA(0.0, 0.2);
        bassBandSlowAvg = new TEMath.EMA(0.0, 0.005);
    }

    protected void onOscMessage(OscMessage msg) {
        String address = msg.getAddressPattern().toString();
        if (address.equals("/lx/autopilot/something")) {
            // TODO(will) fill in more of these
        } else {
            System.out.printf("Unrecognized OSC message detected: %s\n", address);
            return;
        }

        // if this is a message autopilot knows how to work with, add to the queue!
        unprocessedOscMessages.add(msg);
    }

    @Override
    public void loop(double deltaMs) {
        long now = System.currentTimeMillis();
        try {
            // if autopilot isn't enabled, just ignore for now
            if (!isEnabled()) return;

            // debug for now, just make sure firing...
//            System.out.printf("[Autopilot] Heartbeat. Mills since last loop: " + Double.toString(deltaMs) +"\n");

            // collect statistics
            // TODO
//            int numBands = lx.engine.audio.meter.getNumBands();
//            int sr = lx.engine.audio.meter.fft.getSampleRate();
//            double octaveRatio = lx.engine.audio.meter.fft.getBandOctaveRatio();
//            int bufferSize = lx.engine.audio.meter.fft.getSize();
//            double measurementDuration = ((double)bufferSize) / sr * 1000.0;
//            double freqResolution = sr / ((double)bufferSize);

            float avgInstantBassAmplitude = lx.engine.audio.meter.fft.getAverage(0.0f, 0.1f);
            double fastAvg = bassBandFastAvg.update(avgInstantBassAmplitude, deltaMs);
            double slowAvg = bassBandSlowAvg.update(avgInstantBassAmplitude, deltaMs);

//            System.out.printf("NFFT=%d, sr=%d samples /sec, octaveRatio=%f, buffer=%d, measurementDuration=%f ms, freqResolution=%f, fastAvg=%f, slowAvg=%f \n",
//                    numBands, sr, octaveRatio, bufferSize, measurementDuration, freqResolution, fastAvg, slowAvg);

            if (fastAvg < 0.5 * slowAvg) {
                lx.engine.mixer.channels.get(2).fader.setValue(1.0);
                lx.engine.mixer.channels.get(4).fader.setValue(1.0);
            } else {
                lx.engine.mixer.channels.get(2).fader.setValue(0.0);
                lx.engine.mixer.channels.get(4).fader.setValue(0.0);
            }

            // check for new OSC messages
            // TODO

            // act on OSC messages recieved!
            // TODO
            // DEBUG try adjusting some levels
//            try {
//                if (this.a) {
//                    lx.engine.mixer.channels.get(2).fader.setValue(1.0);
//                    lx.engine.mixer.channels.get(4).fader.setValue(0.0);
//                    LXChannel channel = (LXChannel) lx.engine.mixer.channels.get(2);
//                    channel.triggerPatternCycle.setValue(true);
//                    this.a = false;
//                } else {
//                    lx.engine.mixer.channels.get(2).fader.setValue(0.0);
//                    lx.engine.mixer.channels.get(4).fader.setValue(1.0);
//                    LXChannel channel = (LXChannel) lx.engine.mixer.channels.get(4);
//                    channel.triggerPatternCycle.setValue(true);
//                    this.a = true;
//                }
//            } catch (IndexOutOfBoundsException ie) {
//                System.out.printf("LX mixer is not initialized yet, waiting for next Autopilot.run()...");
//            }

        } catch (Exception e) {
            System.out.println("ERROR - unexpected exception in Autopilot.run()");
            e.printStackTrace(System.out);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
