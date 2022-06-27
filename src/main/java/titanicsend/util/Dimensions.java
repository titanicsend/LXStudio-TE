package titanicsend.util;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelModel;

import java.util.*;

/**
 * Since we're dealing with a very awkwardly shaped object, it's often helpful to think of/treat certain areas in terms
 * of a rectangular prism that would encapsulate the points you're working with. This class helps retrieve/represent
 * those dimensions.
 *
 * One relevant aspect to note is that the Z param which is generally referred to as depth is representing what one
 * would think of as the width if they were looking at the car from the front.
 */
public class Dimensions {

    private final Float minX;
    private final Float minY;
    private final Float minZ;

    private final Float maxX;
    private final Float maxY;
    private final Float maxZ;

    private final Float minXn;
    private final Float minYn;
    private final Float minZn;

    private final Float maxXn;
    private final Float maxYn;
    private final Float maxZn;

    public Dimensions(Float minX, Float minY, Float minZ, Float maxX, Float maxY, Float maxZ,
                      Float minXn, Float minYn, Float minZn, Float maxXn, Float maxYn, Float maxZn) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.minXn = minXn;
        this.minYn = minYn;
        this.minZn = minZn;
        this.maxXn = maxXn;
        this.maxYn = maxYn;
        this.maxZn = maxZn;
    }

    public Float getMinX() {
        return minX;
    }

    public Float getMinY() {
        return minY;
    }

    public Float getMinZ() {
        return minZ;
    }

    public Float getMaxX() {
        return maxX;
    }

    public Float getMaxY() {
        return maxY;
    }

    public Float getMaxZ() {
        return maxZ;
    }

    public Float getMinXn() {
        return minXn;
    }

    public Float getMinYn() {
        return minYn;
    }

    public Float getMinZn() {
        return minZn;
    }

    public Float getMaxXn() {
        return maxXn;
    }

    public Float getMaxYn() {
        return maxYn;
    }

    public Float getMaxZn() {
        return maxZn;
    }

    public Float getWidth() {
        return maxX - minX;
    }

    public Float getHeight() {
        return maxY - minY;
    }

    public Float getDepth() {
        return maxZ - minZ;
    }

    public Float getWidthNormalized() {
        return maxXn - minXn;
    }

    public Float getHeightNormalized() {
        return maxYn - minYn;
    }

    public Float getDepthNormalized() {
        return maxZn - minZn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dimensions that = (Dimensions) o;
        return Float.compare(that.minX, minX) == 0 && Float.compare(that.minY, minY) == 0 &&
                Float.compare(that.minZ, minZ) == 0 && Float.compare(that.maxX, maxX) == 0 &&
                Float.compare(that.maxY, maxY) == 0 && Float.compare(that.maxZ, maxZ) == 0 &&
                Float.compare(that.minXn, minXn) == 0 && Float.compare(that.minYn, minYn) == 0 &&
                Float.compare(that.minZn, minZn) == 0 && Float.compare(that.maxXn, maxXn) == 0 &&
                Float.compare(that.maxYn, maxYn) == 0 && Float.compare(that.maxZn, maxZn) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ, minXn, minYn, minZn, maxXn, maxYn, maxZn);
    }

    public static Dimensions fromModel(TEModel model) {
        return fromModels(List.of(model));
    }

    public static Dimensions fromModels(Collection<? extends TEModel> models) {
        Set<LXPoint> points = new HashSet<>();
        for (TEModel model : models) {
            points.addAll(model.getPoints());
        }
        return fromPoints(points);
    }
    
    public static Dimensions fromPoints(Collection<LXPoint> points) {
        Float minX = null;
        Float minY = null;
        Float minZ = null;

        Float maxX = null;
        Float maxY = null;
        Float maxZ = null;

        Float minXn = null;
        Float minYn = null;
        Float minZn = null;

        Float maxXn = null;
        Float maxYn = null;
        Float maxZn = null;
        
        for (LXPoint point : points) {
            minX = minX == null ? point.x : Math.min(point.x, minX);
            minY = minY == null ? point.y : Math.min(point.y, minY);
            minZ = minZ == null ? point.z : Math.min(point.z, minZ);
            
            maxX = maxX == null ? point.x : Math.max(point.x, maxX);
            maxY = maxY == null ? point.y : Math.max(point.y, maxY);
            maxZ = maxZ == null ? point.z : Math.max(point.z, maxZ);
            
            minXn = minXn == null ? point.xn : Math.min(point.xn, minXn);
            minYn = minYn == null ? point.yn : Math.min(point.yn, minYn);
            minZn = minZn == null ? point.zn : Math.min(point.zn, minZn);
            
            maxXn = maxXn == null ? point.xn : Math.max(point.xn, maxXn);
            maxYn = maxYn == null ? point.yn : Math.max(point.yn, maxYn);
            maxZn = maxZn == null ? point.zn : Math.max(point.zn, maxZn);
        }
        return new Dimensions(minX, minY, minZ, maxX, maxY, maxZ, minXn, minYn, minZn, maxXn, maxYn, maxZn);
    }

    //used to understand which side is the larger face to use as a canvas
    //would be cooler to make width depth in this class based on that but too lazy rn
    public boolean widerOnZThanX() {
        return getDepth() > getWidth();
    }
}
