package titanicsend.pattern.jon;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;

import java.util.ArrayList;

public class ModelBender {
    protected ArrayList<Float> modelZ;
    protected float endXMax;

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

        // save all z coordinates so we can restore the
        // model after views are created
        modelZ = new ArrayList<Float>();
        endXMax = 0;
        for (LXPoint p : model.getPoints()) {
            float x = Math.abs(p.x);
            if (x > endXMax) endXMax = x;
            modelZ.add(p.z);
        }


        // now get the points that are at the end of the model
        model.zMax += endXMax;
        model.zMin -= endXMax;
        model.zRange = model.zMax - model.zMin;
        System.out.println("zMax = " + model.zMax + ", zMin = " + model.zMin + ", endXMax = " + endXMax + ", zRange = " + model.zRange);

        ArrayList<LXPoint> endPoints = getEndPoints(model);
        for (LXPoint p : endPoints) {
            if (model.isGapPoint(p)) continue;

            double zOffset = endXMax - Math.abs(p.x);
            p.z += (p.z >= 0) ? zOffset : -zOffset;
            p.zn = (p.z - model.zMin) / model.zRange;
            if (Math.abs(p.zn) > 1) {
                System.out.println("ERROR: point " + p.index + " has zn = " + p.zn);
                System.out.println("   x,y,z = " + p.x + " " + p.y + " " + p.z);
            }
        }


    }

    public void restoreModel(TEWholeModel model) {
        int i = 0;
        for (LXPoint p : model.getPoints()) {
            p.z = modelZ.get(i++);
        }
        model.zMax -= endXMax;
        model.zMin += endXMax;

        model.zRange = model.zMax - model.zMin;

        //model.normalizePoints();
    }


}
