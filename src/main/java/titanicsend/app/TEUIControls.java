package titanicsend.app;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics.Align;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;

public class TEUIControls extends UICollapsibleSection {

    private final float controlX = 100;
    private final float rowHeight = 20;

    private float y = 0;

    public TEUIControls(LXStudio.UI ui, TEVirtualOverlays visual, float w) {
        super(ui, 0, 0, w, 122);
        setTitle("TE UI Controls");

        final float controlWidth = getContentWidth() - this.controlX;

        controlRow(ui, controlWidth, visual.opaqueBackPanelsVisible);
        controlRow(ui, controlWidth, visual.lasersVisible);
        controlRow(ui, controlWidth, visual.laserBoxSize);
        controlRow(ui, controlWidth, visual.panelLabelsVisible);
        controlRow(ui, controlWidth, visual.vertexLabelsVisible);

        setContentHeight(Math.max(0, y - 4));
    }

    private void controlRow(LXStudio.UI ui, float controlWidth, LXParameter p) {
        UI2dComponent control = null;
        if (p instanceof BooleanParameter) {
            control = new UIButton(controlX, y, controlWidth, 16)
                    .setParameter((BooleanParameter) p)
                    .setActiveLabel("On")
                    .setInactiveLabel("Off");
        } else if (p instanceof BoundedParameter) {
            control = new UIDoubleBox(controlX, y, controlWidth, 16).setParameter((BoundedParameter) p);
        } else if (p instanceof DiscreteParameter) {
            control = new UIDropMenu(controlX, y, controlWidth, 16, (DiscreteParameter) p);
        }
        if (control != null) {
            new UILabel(0, y, getContentWidth() - control.getWidth() - 4, 16)
                    .setLabel(p.getLabel())
                    .setPadding(0, 4)
                    .setFont(ui.theme.getControlFont())
                    .setTextAlignment(Align.LEFT, Align.MIDDLE)
                    .addToContainer(this);
            control.addToContainer(this);
            y += rowHeight;
        }
    }
}
