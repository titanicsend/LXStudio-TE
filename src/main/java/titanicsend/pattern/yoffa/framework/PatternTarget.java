package titanicsend.pattern.yoffa.framework;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;
import titanicsend.util.Dimensions;

import java.util.*;
import java.util.stream.Collectors;

public class PatternTarget {

    final Map<LXPoint, Dimensions> pointsToCanvas = new HashMap<>();

    TEAudioPattern pattern;
    public TEPattern.ColorType colorType = TEPattern.ColorType.PRIMARY;

    public PatternTarget(TEAudioPattern pattern) {
        this.pattern = pattern;
    }

    public PatternTarget(TEAudioPattern pattern,TEPattern.ColorType ct) {
        this.pattern = pattern;
        this.colorType = ct;
    }

    public PatternTarget addPointsAsCanvas(Collection<LXPoint> points) {
        Dimensions dimensions = Dimensions.fromPoints(points);
        points.forEach(point -> pointsToCanvas.put(point, dimensions));
        return this;
    }

    public PatternTarget addModelsAsOneCanvas(Collection<? extends TEModel> models) {
        addPointsAsCanvas(models.stream().map(TEModel::getPoints).flatMap(List::stream).collect(Collectors.toSet()));
        return this;
    }

    public PatternTarget addModelsWithIndividualCanvases(Collection<? extends TEModel> models) {
        models.forEach(model -> addPointsAsCanvas(model.getPoints()));
        return this;
    }

    public PatternTarget addPanelSectionAsCanvas(TEPanelSection section) {
        return addModelsAsOneCanvas(pattern.getModel().getPanelsBySection(section));
    }

    public PatternTarget addPanelSectionAsIndividualCanvases(TEPanelSection section) {
        return addModelsWithIndividualCanvases(pattern.getModel().getPanelsBySection(section));
    }

    public static PatternTarget allPointsAsCanvas(TEAudioPattern pattern) {
        PatternTarget pt = new PatternTarget(pattern);
        pt.addPointsAsCanvas(pattern.getModel().panelPoints);
        pt.addPointsAsCanvas(pattern.getModel().edgePoints);
        return pt;
    }

    public static PatternTarget allEdgesAsCanvas(TEAudioPattern pattern) {
        return new PatternTarget(pattern,TEPattern.ColorType.PRIMARY).addPointsAsCanvas(pattern.getModel().edgePoints);
    }

    public static PatternTarget allPanelsAsCanvas(TEAudioPattern pattern) {
        return new PatternTarget(pattern).addPointsAsCanvas(pattern.getModel().panelPoints);
    }

    public static PatternTarget allPanelsAsIndividual(TEAudioPattern pattern) {
        return new PatternTarget(pattern).addModelsWithIndividualCanvases(pattern.getModel().getAllPanels());
    }

    public static PatternTarget doubleLargeCanvas(TEAudioPattern pattern) {
        return new PatternTarget(pattern)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE)
                .addPanelSectionAsCanvas(TEPanelSection.AFT)
                .addPanelSectionAsCanvas(TEPanelSection.FORE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT_SINGLE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE_SINGLE);
    }

    public static PatternTarget splitPanelSections(TEAudioPattern pattern) {
        return new PatternTarget(pattern)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE)
                .addPanelSectionAsCanvas(TEPanelSection.FORE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT_SINGLE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE_SINGLE)
                .addPanelSectionAsIndividualCanvases(TEPanelSection.STARBOARD_AFT)
                .addPanelSectionAsIndividualCanvases(TEPanelSection.AFT);
    }

}
