package titanicsend.model.justin;

import heronarts.lx.parameter.LXParameter;

public interface DisposableParameter {

  static public interface DisposeListener {
    public void disposing(LXParameter parameter);
  }

  public void listenDispose(DisposeListener listener);
  public void unlistenDispose(DisposeListener listener);

}
