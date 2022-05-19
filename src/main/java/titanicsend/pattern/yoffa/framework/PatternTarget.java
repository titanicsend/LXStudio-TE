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

    public PatternTarget(TEAudioPattern pattern) {
        this.pattern = pattern;
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

    public static PatternTarget allEdgesAsCanvas(TEAudioPattern pattern) {
        return new PatternTarget(pattern).addPointsAsCanvas(pattern.getModel().edgePoints);
    }

    public static PatternTarget allPanelsAsCanvas(TEAudioPattern pattern) {
        return new PatternTarget(pattern).addPointsAsCanvas(pattern.getModel().panelPoints);
    }

    public static PatternTarget splitPanelSections(TEAudioPattern pattern) {
        return new PatternTarget(pattern)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT)
                .addPanelSectionAsIndividualCanvases(TEPanelSection.STARBOARD_FORE);
    }

}
