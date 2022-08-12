package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.blend.MultiplyBlend;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.*;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  Use symmetric edges in the model, and colors them together or
 *  produces black and white mask to multiply-blend other content.
 *  `edgesBySymmetryGroup` is Vector -> List<Edges>, where the vector
 *  has x zeroed, and catalogs them by the abs(z). Therefore, 1, 2, or 4
 *  edges will be in each List, and they all have either port-starboard
 *  or fore-aft symmetry (or both).
 *
 *  Color is ignored if placed in a channel set to the multiply
 *  blendMode, and the pattern outputs a white & black mask.
 */

@LXCategory("Geometry Masks")
public class EdgeSymmetry extends TEPattern {
    public final LinkedColorParameter colorParam =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Primary color for edges");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .31, 0, 1)
                    .setDescription("Number of adjacent edges to select");

    public final CompoundParameter fracFromZCenter =
            new CompoundParameter("CtrDist", .25, 0, 1)
                    .setDescription("Select edges by distance index from center");

    public final CompoundParameter height =
            new CompoundParameter("Height", .59, 0, 1)
                    .setDescription("Edges must be lower than this");

    public final BooleanParameter maskMode =
            new BooleanParameter("Mask", false)
                    .setDescription("B&W - set channel to Multiply other edge content");

    // This stores edgesBySymmetryGroup keys sorted by their distance from the center.
    protected List<LXVector> edgeGroupsByZ;

    // Collection of edges that should be on based on parameters
    List<TEEdgeModel> litEdges;

    public EdgeSymmetry(LX lx) {
        super(lx);
        addParameter("energy", energy);
        addParameter("width", fracFromZCenter);
        addParameter("height", height);
        addParameter("mask", maskMode);
        edgeGroupsByZ = new ArrayList<>(model.edgesBySymmetryGroup.keySet());

        /* Arrange from L-R (aft-fore, axis Z) for index-based sweep.
         * Accessing this by index is mostly just for learning purposes.
         * An alternate way that might be usually better is to select
         * the edgesBySymmetryGroup to operate on via the point coordinate
         * of their vector keys.
         */
        edgeGroupsByZ.sort((LXVector v1, LXVector v2) -> Float.compare(v1.z, v2.z));

        selectEdges();
    }

    public void run(double deltaMs) {
        int color = colorParam.getColor();
        if (getChannel() != null) {
            if (getChannel().blendMode.getObject().getClass().equals(MultiplyBlend.class)) {
                // Operate in Mask mode
                setEdges(LXColor.BLACK);
                color = LXColor.WHITE;
            } else {
                clearEdges();
            }
        }

        for (TEEdgeModel edge : litEdges) {
            for (TEEdgeModel.Point point : edge.points) {
                colors[point.index] = color;
            }
        }
    }

    protected void selectEdges() {
        // How many nearby symmetry groups (sorted by Z, fore-aft) to select
        int groupCount = (int) (edgeGroupsByZ.size() * energy.getNormalized());

        // Select the range of symmetry groups by initial z index and groupCount
        // Allowing the range to start and end outside the real set of edges
        int idxRange = model.edgesBySymmetryGroup.size() + groupCount;
        int from = (int) (fracFromZCenter.getNormalized() * idxRange) - groupCount;
        int to =  from + groupCount;
        List<LXVector> selectedEdgeGroups =
                edgeGroupsByZ.subList(Math.max(from, 0),
                                      Math.min(to, edgeGroupsByZ.size()));

        // Find all applicable edges as a list. Filter by Y coordinate (height) and
        // flatten the Hashmap's values to get a combined list of all selected edges
        litEdges = model.edgesBySymmetryGroup.entrySet().stream()
                .filter(e -> selectedEdgeGroups.contains(e.getKey()))
                .filter(e -> e.getKey().y / model.yMax < height.getValue())
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);

        if (Arrays.<LXParameter>asList(energy, fracFromZCenter, height).contains(parameter)) {
            selectEdges();
        }
    }
}
