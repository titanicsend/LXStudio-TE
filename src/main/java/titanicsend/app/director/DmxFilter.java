package titanicsend.app.director;

import heronarts.lx.model.LXModel;

public class DmxFilter extends Filter {

    private final String tag;

    public DmxFilter(String path, String label, String tag) {
        super(path, label);
        this.tag = tag;
    }

    @Override
    public void run(int[] colors, float master) {
        // TODO: scale the dimmer for DMX fixtures
    }

    @Override
    public void modelChanged(LXModel model) {
        // TODO: find matching DMX fixtures
    }
}
