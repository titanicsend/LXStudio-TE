package titanicsend.pattern.alex;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.component.UIButton;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dContainer;

import titanicsend.pattern.TEPattern;
import titanicsend.util.SimpleScreen;

import java.util.ArrayList;
import java.util.*;

@LXCategory("Combo FG")
// A ResizeableScreen is a dynamically-resized rectangular screen that maps pixels without an area
// defined by the caller.
// TODO: right now, we just color all pixels white. Soon, we should take a data array and color array
// to dynamically color each point and re-render the canvas whenever these points come in.
public class ResizeableScreen extends TEPattern implements UIDeviceControls<ResizeableScreen> {
    private SimpleScreen screen;

    // Technically, we do have doubles, but the values are in microns, so if you really need a fraction
    // of a micron, you can figure out how to do this with BoundedParameters instead.
    // Note: extra +1 is because DiscreteParameters have an _exclusive_ bound on the upper end.
    private int roundedLowerYLimit = (int)this.model.boundaryPoints.minYBoundaryPoint.y;
    private int roundedUpperYLimit = (int)this.model.boundaryPoints.maxYBoundaryPoint.y + 1;
    private int roundedLowerZLimit = (int)this.model.boundaryPoints.minZBoundaryPoint.z;
    private int roundedUpperZLimit = (int)this.model.boundaryPoints.maxZBoundaryPoint.z + 1;

    // The extra +1 on the ends is because DiscreteParameter bounds are exclusive at the top end.
    public final DiscreteParameter lowerYBoundParam =
            new DiscreteParameter("Lower Y Bound", this.roundedLowerYLimit / 2, this.roundedLowerYLimit, this.roundedUpperYLimit)
                    .setDescription("Lower boundary for the Y coordinate of the screen");
    public final DiscreteParameter upperYBoundParam =
            new DiscreteParameter("Upper Y Bound", this.roundedUpperYLimit / 2, this.roundedLowerYLimit, this.roundedUpperYLimit)
                    .setDescription("Upper boundary for the Y coordinate of the screen");
    public final DiscreteParameter lowerZBoundParam =
            new DiscreteParameter("Lower Z Bound", this.roundedLowerZLimit / 2, this.roundedLowerZLimit, this.roundedUpperZLimit)
                    .setDescription("Lower boundary for the Z coordinate of the screen");
    public final DiscreteParameter upperZBoundParam =
            new DiscreteParameter("Upper Z Bound", this.roundedUpperZLimit / 2, this.roundedLowerZLimit, this.roundedUpperZLimit)
                    .setDescription("Upper boundary for the Z coordinate of the screen");
    public BooleanParameter doubleSidedParam =
            new BooleanParameter("Double Sided?")
                    .setDescription("Toggle whether screen is drawn on both sides of the car or not (Default false)")
                    .setValue(false);

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Color of the screen");

    private void toggleDoubleSided() {
        this.doubleSidedParam.setValue(!this.doubleSidedParam.getValueb());
    }
    
    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, ResizeableScreen pattern) {
        uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
        uiDevice.setChildSpacing(5);
        uiDevice.setContentWidth(3 * COL_WIDTH);

        uiDevice.addChildren(
            controlLabel(ui, "Lower Y Bound"),
            newIntegerBox(this.lowerYBoundParam),
            controlLabel(ui, "Upper Y Bound"),
            newIntegerBox(this.upperYBoundParam),
            controlLabel(ui, "Lower Z Bound"),
            newIntegerBox(this.lowerZBoundParam), 
            controlLabel(ui, "Upper Z Bound"),
            newIntegerBox(this.upperZBoundParam),
            new UIButton(0, 0, 2 * COL_WIDTH, 20) {
                @Override
                public void onToggle(boolean unused) {
                    toggleDoubleSided();
                }
            }
            .setLabel("Double Sided?"));
        this.lowerYBoundParam.addListener(this::repaint);
        this.upperYBoundParam.addListener(this::repaint);
        this.lowerZBoundParam.addListener(this::repaint);
        this.upperZBoundParam.addListener(this::repaint);
        this.doubleSidedParam.addListener(this::repaint);
    }

    private void paint(double deltaMs) {
        int color = this.color.calcColor();

        for (LXPoint point : this.screen.screenGrid) {
            colors[point.index] = color;
        }
    }

    private void sizeAndPaintScreen() {
        ArrayList<LXPoint> pointsList = new ArrayList<>(Arrays.asList(this.model.points));
        this.screen = new SimpleScreen(
            pointsList,
            this.lowerYBoundParam.getValuei(),
            this.upperYBoundParam.getValuei(),
            this.lowerZBoundParam.getValuei(),
            this.upperZBoundParam.getValuei(),
            this.doubleSidedParam.getValueb());
        LX.log(String.format("%d points in screen:", this.screen.screenGrid.size()));

        this.paint(0);
    }

    public void repaint(LXParameter unused) {
        this.clearPixels();
        this.sizeAndPaintScreen();
    }

    public ResizeableScreen(LX lx) {
        super(lx);
        this.sizeAndPaintScreen();
    }

    public void run(double deltaMs) {
        this.paint(deltaMs);
    }
}