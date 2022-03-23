package titanicsend.app;

import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UICollapsibleSection;

import java.util.HashMap;
import java.util.Map;

public class TEUIControls extends UICollapsibleSection {
    private final Map<TEUIComponent, UI2dContainer> controls = new HashMap<TEUIComponent, UI2dContainer>();

    TEUIControls(final LXStudio.UI ui, TEVirtualOverlays visual, float width) {
        super(ui, 0, 0, width, 124);
        setTitle("TE UI controls");
        setLayout(UI2dContainer.Layout.VERTICAL);

        UI2dContainer control = new UI2dContainer(0, 0, getContentWidth(), 0);
        //control.setVisible(false);
        visual.buildControlUI(ui, control);
        control.addToContainer(this);
    }
}
