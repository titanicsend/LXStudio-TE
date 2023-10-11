package titanicsend.app.dev;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.*;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.ui.device.UIControls;

public class UIArtNetInput extends UICollapsibleSection implements UIControls {

    static private final float VERTICAL_SPACING = 4;
    static private final float CHILD_SPACING = 8;
    static private final float ROW_HEIGHT = 16;
    static private final float LABEL_WIDTH = 90;
    final float controlWidth;
    final VGraphics.Font labelFont;

    public UIArtNetInput(UI ui, ArtNetInput artNetInput, float w) {
        super(ui, 0, 0, w, 0);

        setTitle("ArtNet Input");
        this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);

        this.controlWidth = getContentWidth() -  (PADDING * 2) - LABEL_WIDTH ;
        this.labelFont = ui.theme.getControlFont();

//        addRow(this, ROW_HEIGHT, "ArtNet Receive Active",
//                newControl(artNetInput.artNetReceive)
//        );
        new UISwitch(0, 4)
                .setParameter(artNetInput.artNetReceive)
                .addToContainer(this);
    }

    public UI2dComponent newControl(LXParameter p) {
        UI2dComponent control = null;
        if (p instanceof BooleanParameter) {
            control = new UIButton(0, 0, (BooleanParameter) p).setActiveLabel("On").setInactiveLabel("Off");
        } else if (p instanceof BoundedParameter) {
            control = new UIDoubleBox(0, 0, (BoundedParameter) p);
        } else if (p instanceof DiscreteParameter) {
            control = new UIDropMenu(0, 0, (DiscreteParameter) p);
        }
        return control;
    }

    public UI2dContainer addRow(UI2dContainer uiDevice, float rowHeight, String label, UI2dComponent... components) {
        UI2dContainer row = UI2dContainer.newHorizontalContainer(rowHeight, CHILD_SPACING);

        if (label != null) {
            rowLabel(label, LABEL_WIDTH).addToContainer(row);
        }

        if (components != null) {
            float cWidth = (label == null ? getContentWidth() - (PADDING * 2) : this.controlWidth - ((components.length - 1) * CHILD_SPACING)) / components.length;
            for (UI2dComponent component : components) {
                component.setWidth(cWidth).setHeight(ROW_HEIGHT).addToContainer(row);
            }
        }

        row.addToContainer(uiDevice);
        return row;
    }

    public UI2dComponent rowLabel(String label) {
        return sectionLabel(label, LABEL_WIDTH);
    }

    public UI2dComponent rowLabel(String label, float columnWidth) {
        return
                new UILabel(columnWidth, 16, label)
                        .setFont(this.labelFont)
                        .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
    }
}
