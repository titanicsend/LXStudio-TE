package titanicsend.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.*;
import titanicsend.util.Dimensions;

public abstract class TEModel extends LXModel {
    private final String teModelType;
    private final Dimensions dimensions;

    public TEModel(String teModelType, List<LXPoint> points, String... tags) {
        super(points, combineTags(teModelType, tags));
        this.teModelType = teModelType;
        this.dimensions = Dimensions.fromPoints(points);
    }

    public abstract String getId();

    public String repr() {
        return teModelType + "_" + this.getId();
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    // This method can probably be replaced by a simple inline later
    public static String[] combineTags(String tag0, String... tags) {
        if (tag0 == null && tags == null) {
            return null;
        }

        int i = 0;
        String[] finalTags = new String[(tag0 != null ? 1 : 0) + (tags != null ? tags.length : 0)];

        if (tag0 != null) {
            finalTags[i++] = tag0;
        }
        if (tags != null) {
            for (String tag : tags) {
                finalTags[i++] = tag;
            }
        }
        return finalTags;
    }
}
