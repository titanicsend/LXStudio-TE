package titanicsend.pattern.yoffa.shaders.basicpatterns;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.shaders.FragmentShader;
import titanicsend.util.Dimensions;

import java.util.List;
import java.util.Set;

public abstract class BasicShaderPattern extends TEPattern {

    private final FragmentShader fragmentShader;
    private long startTime;

    public BasicShaderPattern(LX lx) {
        super(lx);
        fragmentShader = getFragmentShader();
        addParameters();
    }

    @Override
    public void onActive() {
        startTime = System.currentTimeMillis();
    }

    //TODO @yoffa perf isn't great, should prolly parallelize this
    @Override
    public void run(double deltaMs) {
        long timeMillis = System.currentTimeMillis() - startTime;

        for (TEPanelModel panel : model.getPanelsBySection(TEPanelSection.STARBOARD_FORE)) {
            Dimensions dimensions = Dimensions.fromPanels(List.of(panel));
            for (LXPoint point : panel.getPoints()) {
                colors[point.index] = fragmentShader.getColorForPoint(point, dimensions, timeMillis);
            }
        }

        Set<TEPanelModel> panels = model.getPanelsBySection(TEPanelSection.STARBOARD_AFT);
        Dimensions dimensions = Dimensions.fromPanels(panels);
        for (TEPanelModel panel : panels) {
            for (LXPoint point : panel.getPoints()) {
                colors[point.index] = fragmentShader.getColorForPoint(point, dimensions, timeMillis);
            }
        }

    }

    protected abstract FragmentShader getFragmentShader();

    //Override this to customize parameters
    protected void addParameters() {
        for (LXParameter parameter : fragmentShader.getParameters())
            addParameter(parameter.getLabel(), parameter);
    }

}
