package titanicsend.model.justin;

import heronarts.lx.parameter.LXParameter;

public interface DisposableParameter {

    public static interface DisposeListener {
        public void disposing(LXParameter parameter);
    }

    public void listenDispose(DisposeListener listener);

    public void unlistenDispose(DisposeListener listener);
}
