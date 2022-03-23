package titanicsend.pattern;

import java.util.*;
import java.util.concurrent.Callable;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;

@LXCategory("Testahedron")
public abstract class PeriodicPattern extends TEPattern {
  private static class PeriodicEntry {
    TimeAccumulator timeAccumulator;
    Runnable callback;

    PeriodicEntry(Runnable callback, double periodMsec) {
      this.timeAccumulator = new TimeAccumulator(periodMsec);
      this.callback = callback;
    }
  }

  private final List<PeriodicEntry> entries;

  public PeriodicPattern(LX lx) {
    super(lx);
    entries = new ArrayList<>();
  }

  public void register(Runnable callback, double period) {
    PeriodicEntry entry = new PeriodicEntry(callback, period);
    this.entries.add(entry);
  }

  // This is called for every run(), in case the subclass wants to hook into it
  public void runHook(double deltaMs) {}

  public void run(double deltaMs) {
    this.runHook(deltaMs);
    for (PeriodicEntry entry : this.entries) {
      entry.timeAccumulator.add(deltaMs);
      while (entry.timeAccumulator.timeToRun()) {
        entry.callback.run();
      }
    }
  }
}
