package titanicsend.util;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;

import java.util.ArrayList;
import java.util.Comparator;

// A SimpleScreen is a two-dimensional approximation of a monitor or TV screen against
// the side of Titanic's End.
// TODO: allow drawing against any dimensions. Currently, we flatten the X axis (the axis
// the lasers will blast the crowd through) and preserve the Y and Z coordinates.
// TODO: allow arbitrary positioning of this screen
public class SimpleScreen {
    public ArrayList<LXPoint> screenGrid;

    private void buildScreenGrid(
        ArrayList<LXPoint> pointsList,
        int yLowerBound,
        int yUpperBound,
        int zLowerBound,
        int zUpperBound,
        boolean doubleSided) {
        LX.log("Inside SimpleScreen.buildScreenGrid");
        LX.log(String.format("  Lower Y: %d", yLowerBound));
        LX.log(String.format("  Upper Y: %d", yUpperBound));
        LX.log(String.format("  Lower Z: %d", zLowerBound));
        LX.log(String.format("  Upper Z: %d", zUpperBound));

        ArrayList<LXPoint> screenGrid = new ArrayList<LXPoint>();

        for (LXPoint point : pointsList) {
            if (
                (point.z <= zUpperBound && point.z >= zLowerBound) &&
                (point.y <= yUpperBound && point.y >= yLowerBound)) {
                if (doubleSided) {
                    screenGrid.add(point);
                } else if (point.x >= 0) {
                    screenGrid.add(point);
                }
            }
        }
        this.screenGrid = screenGrid;
    }

    public SimpleScreen(
        ArrayList<LXPoint> pointsList,
        int yLowerBound,
        int yUpperBound,
        int zLowerBound,
        int zUpperBound,
        boolean doubleSided) {
        buildScreenGrid(pointsList, yLowerBound, yUpperBound, zLowerBound, zUpperBound, doubleSided);
    }
}
