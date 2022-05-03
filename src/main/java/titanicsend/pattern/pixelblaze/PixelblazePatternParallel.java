package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PixelblazePatternParallel extends TEAudioPattern {
  public static final int N_THREADS = 4;
  private ArrayList<Wrapper> wrappers = new ArrayList<>();

  ExecutorService es = Executors.newFixedThreadPool(N_THREADS);

  public PixelblazePatternParallel(LX lx) {
    super(lx);

    try {

      //split edge points into chunks
      int chunksize = (int) Math.ceil((float) model.edgePoints.size() / N_THREADS);

      for (int i = 0; i < model.edgePoints.size(); i += chunksize) {
        List<LXPoint> chunk = model.edgePoints.subList(i, Math.min(i + chunksize, model.edgePoints.size() - 1));
        LXPoint[] chunkPoints = new LXPoint[chunk.size()];
        wrappers.add(Wrapper.fromResource("neon_ice", this, model.edgePoints.toArray(chunkPoints), colors));
      }

      //one for every panel
      for (TEPanelModel panelModel : model.panelsById.values()) {
        wrappers.add(Wrapper.fromResource("xorcery", this, panelModel.points, colors));
      }

      LX.log("parallel chunks=" + wrappers.size());

    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
    }
  }

  public void runTEAudioPattern(double deltaMs) {
    if (wrappers.size() == 0)
      return;
    try {
      updateGradients();
      ArrayList<Future<Void>> futures = new ArrayList<>(wrappers.size());
      for (Wrapper wrapper : wrappers) {
        futures.add(es.submit((Callable<Void>) () -> {
            wrapper.reloadIfNecessary();
            wrapper.render(deltaMs);
            return null;
        }));
      }

      for (Future<Void> future : futures) {
        future.get(); //block until done
      }
    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
      return;
    }

  }
}