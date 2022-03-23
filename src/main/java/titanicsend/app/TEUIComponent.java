package titanicsend.app;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.UI2dComponent;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.UI3dComponent;
import heronarts.p4lx.ui.component.UIButton;
import heronarts.p4lx.ui.component.UIDoubleBox;
import heronarts.p4lx.ui.component.UIDropMenu;
import heronarts.p4lx.ui.component.UILabel;

import java.util.LinkedHashMap;
import java.util.Map;

import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.LEFT;

public class TEUIComponent extends UI3dComponent {
        private final Map<String, LXParameter> parameters = new LinkedHashMap<String, LXParameter>();

        protected void addParameter(String path, LXParameter parameter) {
            this.parameters.put(path, parameter);
        }

        protected void buildControlUI(LXStudio.UI ui, UI2dContainer controls) {
            float yp = 0;
            float controlWidth = 80;
            float xp = controls.getContentWidth() - controlWidth;
            for (LXParameter p : this.parameters.values()) {
                UI2dComponent control = null;
                if (p instanceof BooleanParameter) {
                    control = new UIButton(xp, yp, controlWidth, 16).setParameter((BooleanParameter) p).setActiveLabel("On").setInactiveLabel("Off");
                } else if (p instanceof BoundedParameter) {
                    control = new UIDoubleBox(xp, yp, controlWidth, 16).setParameter((BoundedParameter) p);
                } else if (p instanceof DiscreteParameter) {
                    control = new UIDropMenu(xp, yp, controlWidth, 16, (DiscreteParameter) p);
                }
                if (control != null) {
                    new UILabel(0, yp, controls.getContentWidth() - control.getWidth() - 4, 16)
                            .setLabel(p.getLabel())
                            .setPadding(0, 4)
                            .setFont(ui.theme.getControlFont())
                            .setTextAlignment(LEFT, CENTER)
                            .addToContainer(controls);
                    control.addToContainer(controls);
                    yp += 20;
                }
            }
            controls.setContentHeight(Math.max(0, yp - 4));
        }
}
