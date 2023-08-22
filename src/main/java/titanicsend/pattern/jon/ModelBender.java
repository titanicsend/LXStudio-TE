package titanicsend.pattern.jon;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;

import java.util.ArrayList;

public class ModelBender {
    protected ArrayList<Float> modelZ;
    protected float endXMax;

    /**
     * Get a list of points on the ends of the car
     *
     * @param model
     * @return list of points
     */
    protected ArrayList<LXPoint> getEndPoints(TEWholeModel model) {
        ArrayList<LXPoint> endPoints = new ArrayList<LXPoint>();

        for (TEPanelModel panel : model.getAllPanels()) {
            String id = panel.getId();
            if (id.startsWith("F") || id.startsWith("A")) {
                for (LXPoint point : panel.getPoints()) {
                    endPoints.add(point);
                }
            }
        }
        return endPoints;
    }

    public void adjustEndGeometry(TEWholeModel model) {

        // save the model's original z coordinates so we can restore them
        // after all views are created.  endXMax is the largest x coordinate at
        // the boundary of side and end (more-or-less).  We use it as the
        // starting x for our taper.
        modelZ = new ArrayList<Float>();
        endXMax = 0;
        for (LXPoint p : model.getPoints()) {
            float x = Math.abs(p.x);
            if (x > endXMax) endXMax = x;
            modelZ.add(p.z);
        }

        // set z bounds for our modified model
        model.zMax += endXMax;
        model.zMin -= endXMax;
        model.zRange = model.zMax - model.zMin;

        // adjust the z coordinates of the end points to taper them
        // with decreasing x. This gives the model a slightly pointy
        // nose and makes texture mapping easier and better looking.
        ArrayList<LXPoint> endPoints = getEndPoints(model);
        for (LXPoint p : endPoints) {
            // kick out gap points or they'll break normalization.
            if (model.isGapPoint(p)) continue;

            double zOffset = endXMax - Math.abs(p.x);
            p.z += (p.z >= 0) ? zOffset : -zOffset;
            p.zn = (p.z - model.zMin) / model.zRange;
        }
    }

    public void restoreModel(TEWholeModel model) {
        // restore the model's original
        int i = 0;
        for (LXPoint p : model.getPoints()) {
            p.z = modelZ.get(i++);
        }
        // restore the model's original z bounds
        model.zMax -= endXMax;
        model.zMin += endXMax;
        model.zRange = model.zMax - model.zMin;
    }
}
