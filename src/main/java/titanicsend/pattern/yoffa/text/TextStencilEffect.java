package titanicsend.pattern.yoffa.text;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.transform.LXProjection;
import heronarts.lx.transform.LXVector;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UILabel;
import heronarts.p4lx.ui.component.UITextBox;
import titanicsend.effect.TEEffect;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.jeff.SignalDebugger;

import java.awt.*;
import java.util.*;
import java.util.List;

@LXCategory("Titanics End")
public class TextStencilEffect extends TEEffect implements UIDeviceControls<TextStencilEffect> {

    public final StringParameter textParameter = new StringParameter("Text")
            .setDescription("Text to display");

    private static final String[] FONT_LIST = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

    public final DiscreteParameter fontParameter = new DiscreteParameter("Font", 1, 0,
            FONT_LIST.length).setDescription("Font");

    public final CompoundParameter sizeParameter = new CompoundParameter("Size", 1, 0.01, 5.0)
            .setDescription("Size");

    public enum View {
        PANELS_SINGLE,
        PANELS_DOUBLE,
        MODEL_SINGLE,
        MODEL_DOUBLE;

        @Override
        public String toString() {
            return switch (this) {
                case PANELS_SINGLE -> "SnglPnls";
                case PANELS_DOUBLE -> "DblPnls";
                case MODEL_SINGLE -> "SnglMdl";
                case MODEL_DOUBLE -> "DblMdl";
            };
        }
    }

    public final CompoundParameter xPosParameter = new CompoundParameter("xPos", 0, -1, 1)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setDescription("xPos");

    public final CompoundParameter yPosParameter = new CompoundParameter("yPos", 0, -1, 1)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setDescription("yPos");

    public final CompoundParameter angleParameter = new CompoundParameter("Angle", 0, -180, 180)
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setDescription("Angle");

    public final EnumParameter<View> viewParameter = new EnumParameter<>("View", View.PANELS_SINGLE);

    TextPainter textPainter;

    public TextStencilEffect(LX lx) {
        super(lx);
        addParameter("text", this.textParameter);
        addParameter("font", this.fontParameter);
        addParameter("size", this.sizeParameter);
        addParameter("view", this.viewParameter);
        addParameter("xPos", this.xPosParameter);
        addParameter("yPos", this.yPosParameter);
    }

    @Override
    protected void run(double deltaMs, double enabledAmount) {
        if (textPainter == null) {
            textPainter = new TextPainter(getColors());
        }
        if (!isEnabled()) {
            return;
        }

        Set<TEPanelModel> writtenPanels = new HashSet<>();
        String text = textParameter.getString();
        switch (viewParameter.getEnum()) {
            case MODEL_SINGLE -> {
                List<TEPanelModel> panels = new LinkedList<>();
                panels.addAll(this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_FORE));
                panels.addAll(this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_AFT));
                writeToModels(text, panels);
                writtenPanels.addAll(panels);
            }
            case MODEL_DOUBLE -> {
                writeToModels(text, this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_FORE));
                writeToModels(text, this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_AFT), true);
                writtenPanels.addAll(this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_FORE));
                writtenPanels.addAll(this.modelTE.getPanelsBySection(TEPanelSection.STARBOARD_AFT));
            }
            case PANELS_SINGLE -> {
                if (text.contains(" ")) {
                    String[] words = text.split("\s");
                    int maxPanelsPerSide = modelTE.getMaxPanelsForWriting() / 2;
                    if (words.length == 2 && words[0].length() < maxPanelsPerSide && words[1].length() < maxPanelsPerSide) {
                        writeCharacterPanels(words[0], TEPanelSection.STARBOARD_FORE, writtenPanels);
                        writeCharacterPanels(words[1], TEPanelSection.STARBOARD_AFT, writtenPanels);
                        break;
                    }
                }
                writeCharacterPanels(text, writtenPanels);
            }
            case PANELS_DOUBLE -> {
                writeCharacterPanels(text, TEPanelSection.STARBOARD_FORE, writtenPanels);
                writeCharacterPanels(text, TEPanelSection.STARBOARD_AFT, writtenPanels);
            }
        }

        for (TEPanelModel panel : this.modelTE.getAllPanels()) {
            if (!writtenPanels.contains(panel)) {
                for (LXPoint point : panel.getPoints()) {
                    setColor(point, 0);
                }
            }
        }
        for (TEEdgeModel edge : this.modelTE.getAllEdges()) {
            for (LXPoint point : edge.getPoints()) {
                setColor(point, 0);
            }
        }
    }

    private void writeToModels(String text, Collection<? extends TEModel> models) {
        writeToModels(text, models, false);
    }

    private void writeToModels(String text, Collection<? extends TEModel> models, boolean invert) {
        double xPos = xPosParameter.getValue();
        double angle = angleParameter.getValue();
        if (invert) {
            xPos = -xPos;
            angle = -angle;
        }
        textPainter.stencil(models, text, getCurrentFontName(),
                sizeParameter.getValue(), xPos, yPosParameter.getValue(), angle);
    }

    private void writeCharacterPanels(String text, Set<TEPanelModel> writtenPanels) {
        writeCharacterPanels(text, null, writtenPanels);
    }

    private void writeCharacterPanels(String text, TEPanelSection section, Set<TEPanelModel> writtenPanels) {
        List<TEPanelModel> writablePanels = this.modelTE.getPanelsForWriting(
                text.length(), section);

        for (int i = 0; i < text.length() && i < writablePanels.size(); i++) {
            writeToModels(String.valueOf(text.charAt(i)),
                    List.of(writablePanels.get(i)));
            writtenPanels.add(writablePanels.get(i));
        }
    }

    UILabel fontLabel;
    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, TextStencilEffect textStencilEffect) {
        uiDevice.setLayout(UI2dContainer.Layout.VERTICAL_GRID);
        uiDevice.setChildSpacing(6);
        uiDevice.setContentWidth(COL_WIDTH);
        fontLabel = controlLabel(ui, "");
        refreshFontLabel();

        UITextBox tb;
        uiDevice.addChildren(
                tb = new UITextBox(0, 0, COL_WIDTH, 16),
                controlLabel(ui, "Text"),
                newKnob(fontParameter),
                fontLabel,
                newKnob(sizeParameter),
                newKnob(viewParameter),
                newKnob(xPosParameter),
                newKnob(yPosParameter),
                newKnob(angleParameter)
        );
        tb.setParameter(textParameter);
        tb.setEmptyValueAllowed(true);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter.equals(fontParameter)) {
            refreshFontLabel();
        }
    }

    private void refreshFontLabel() {
        fontLabel.setLabel(getCurrentFontName());
    }

    private String getCurrentFontName() {
        return FONT_LIST[fontParameter.getValuei()];
    }

}