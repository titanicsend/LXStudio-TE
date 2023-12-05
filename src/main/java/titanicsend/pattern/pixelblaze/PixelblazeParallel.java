package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

public class PixelblazeParallel extends TEPerformancePattern {
    public static final int N_THREADS = 4;
    private ArrayList<Wrapper> wrappers = new ArrayList<>();

    ExecutorService es = Executors.newFixedThreadPool(N_THREADS, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });

    public PixelblazeParallel(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        try {

            // split edge points into chunks
            int chunksize = (int) Math.ceil((float) this.modelTE.edgePoints.size() / N_THREADS);

            for (int i = 0; i < modelTE.edgePoints.size(); i += chunksize) {
                List<LXPoint> chunk =
                        modelTE.edgePoints.subList(i, Math.min(i + chunksize, modelTE.edgePoints.size() - 1));
                LXPoint[] chunkPoints = new LXPoint[chunk.size()];
                wrappers.add(Wrapper.fromResource("neon_ice", this, modelTE.edgePoints.toArray(chunkPoints)));
            }

            chunksize = (int) Math.ceil((float) modelTE.panelPoints.size() / N_THREADS);
            for (int i = 0; i < modelTE.panelPoints.size(); i += chunksize) {
                List<LXPoint> chunk =
                        modelTE.panelPoints.subList(i, Math.min(i + chunksize, modelTE.panelPoints.size() - 1));
                LXPoint[] chunkPoints = new LXPoint[chunk.size()];
                wrappers.add(Wrapper.fromResource("xorcery", this, modelTE.panelPoints.toArray(chunkPoints)));
            }
            LX.log("parallel chunks=" + wrappers.size());

        } catch (Exception e) {
            e.printStackTrace();
            LX.error(e);
        }
    }

    public void runTEAudioPattern(double deltaMs) {
        if (wrappers.size() == 0) return;
        try {
            ArrayList<Future<Void>> futures = new ArrayList<>(wrappers.size());
            for (Wrapper wrapper : wrappers) {
                futures.add(es.submit((Callable<Void>) () -> {
                    wrapper.reloadIfNecessary();
                    wrapper.render(deltaMs, colors);
                    return null;
                }));
            }

            for (Future<Void> future : futures) {
                future.get(); // block until done
            }
        } catch (Exception e) {
            e.printStackTrace();
            LX.error(e);
            return;
        }
    }
}
