package titanicsend.pattern.yoffa.framework;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.util.Dimensions;

import java.util.*;
import java.util.stream.Collectors;

public class PatternTarget {

    final Map<LXPoint, Dimensions> pointsToCanvas = new HashMap<>();

    TEPerformancePattern pattern;
    public TEPattern.ColorType colorType = TEPattern.ColorType.PRIMARY;

    public PatternTarget(TEPerformancePattern pattern) {
        this.pattern = pattern;
    }

    public PatternTarget(TEPerformancePattern pattern,TEPattern.ColorType ct) {
        this.pattern = pattern;
        this.colorType = ct;
    }

    public PatternTarget addPointsAsCanvas(Collection<LXPoint> points) {
        Dimensions dimensions = Dimensions.fromPoints(points);
        for (LXPoint point : points) {
            // skip gap points
            if ((point.x == 0f) && (point.y == 0f) && (point.z == 0f)) continue;
            pointsToCanvas.put(point,dimensions);
        }
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
        return addModelsAsOneCanvas(pattern.getModelTE().getPanelsBySection(section));
    }

    public PatternTarget addPanelSectionAsIndividualCanvases(TEPanelSection section) {
        return addModelsWithIndividualCanvases(pattern.getModelTE().getPanelsBySection(section));
    }

    public static PatternTarget allPointsAsCanvas(TEPerformancePattern pattern) {
        ArrayList<LXPoint> points = new ArrayList<>();
        points.addAll(pattern.getModelTE().panelPoints);
        points.addAll(pattern.getModelTE().edgePoints);

        PatternTarget pt = new PatternTarget(pattern);
        pt.addPointsAsCanvas(points);
        return pt;
    }

    public static PatternTarget allEdgesAsCanvas(TEPerformancePattern pattern) {
        return new PatternTarget(pattern,TEPattern.ColorType.PRIMARY).addPointsAsCanvas(pattern.getModelTE().edgePoints);
    }

    public static PatternTarget allPanelsAsCanvas(TEPerformancePattern pattern) {
        return new PatternTarget(pattern).addPointsAsCanvas(pattern.getModelTE().panelPoints);
    }

    public static PatternTarget allPanelsAsIndividual(TEPerformancePattern pattern) {
        return new PatternTarget(pattern).addModelsWithIndividualCanvases(pattern.getModelTE().getAllPanels());
    }

    public static PatternTarget doubleLargeCanvas(TEPerformancePattern pattern) {
        return new PatternTarget(pattern)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE)
                .addPanelSectionAsCanvas(TEPanelSection.AFT)
                .addPanelSectionAsCanvas(TEPanelSection.FORE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT_SINGLE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE_SINGLE);
    }

    public static PatternTarget splitPanelSections(TEPerformancePattern pattern) {
        return new PatternTarget(pattern)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE)
                .addPanelSectionAsCanvas(TEPanelSection.FORE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_AFT_SINGLE)
                .addPanelSectionAsCanvas(TEPanelSection.STARBOARD_FORE_SINGLE)
                .addPanelSectionAsIndividualCanvases(TEPanelSection.STARBOARD_AFT)
                .addPanelSectionAsIndividualCanvases(TEPanelSection.AFT);
    }

}
