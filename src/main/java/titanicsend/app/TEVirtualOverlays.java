package titanicsend.app;

import java.util.*;

import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.transform.LXVector;
import heronarts.p4lx.ui.UI;
import processing.core.PGraphics;
import titanicsend.model.*;
import titanicsend.util.TE;

public class TEVirtualOverlays extends TEUIComponent {
  TEWholeModel model;

  public final BooleanParameter speakersVisible =
          new BooleanParameter("Speakers")
                  .setDescription("Toggle whether speakers are visible")
                  .setValue(false);

  public final BooleanParameter vertexLabelsVisible =
          new BooleanParameter("Vertex Labels")
                  .setDescription("Toggle whether vertex labels are visible")
                  .setValue(false);

  public final BooleanParameter panelLabelsVisible =
          new BooleanParameter("Panel Labels")
                  .setDescription("Toggle whether panel labels are visible")
                  .setValue(false);

  public final BooleanParameter unknownPanelsVisible =
          new BooleanParameter("Unk Panels")
                  .setDescription("Toggle whether unknown panels are visible")
                  .setValue(true);

  public final BooleanParameter opaqueBackPanelsVisible =
          new BooleanParameter("Backings")
                  .setDescription("Toggle whether to render the back of lit panels as opaque")
                  .setValue(true);

  public final BooleanParameter powerBoxesVisible =
          new BooleanParameter("Power boxes")
                  .setDescription("Toggle whether to show power boxes")
                  .setValue(false);

  private static class POV {
    LXVector v;
    int rgb;

    POV(LXVector v, int rgb) {
      this.v = v;
      this.rgb = rgb;
    }
  }
  private static final int numPOVs = 10;

  private final LXVector groundNormal = new LXVector(0,1,0);
  private final LXVector groundMountainPoint = new LXVector(-20e6F, 0, 0);
  private final LXVector mountainNormal = new LXVector(-1, 0, 0);
  private final List<List<POV>> laserPOV;

  private HashMap<TEVertex, Integer> vertex2Powerboxes;

  public TEVirtualOverlays(TEWholeModel model) {
    super();
    this.model = model;
    addParameter("vertexSpheresVisible", this.speakersVisible);
    addParameter("vertexLabelsVisible", this.vertexLabelsVisible);
    addParameter("panelLabelsVisible", this.panelLabelsVisible);
    addParameter("unknownPanelsVisible", this.unknownPanelsVisible);
    addParameter("opaqueBackPanelsVisible", this.opaqueBackPanelsVisible);
    addParameter("powerBoxesVisible", this.powerBoxesVisible);
    this.laserPOV = new ArrayList<>();
    for (int i = 0; i < numPOVs; i++) {
      this.laserPOV.add(new ArrayList<>());
    }
    vertex2Powerboxes = loadPowerboxes();
  }

  public HashMap<TEVertex, Integer> loadPowerboxes() {
    Scanner s = this.model.loadFile("power_assignments.tsv");
    int i = 0;
    HashMap<TEVertex, Integer> v2p = new HashMap<>();

    while (s.hasNextLine()) {
      String line = s.nextLine();

      // skip our header
      i++;
      if (i == 1) {
        TE.log("skipping header...");
        continue;
      }

      // parse the line!
      try {
        String[] tokens = line.split("\\t");
        assert tokens.length == 7;
        //TE.log("Line: %s (len tokens=%d)", line, tokens.length);

        // parse fields
//        String stripID = tokens[0].strip();
//        String panelOrEdgeId = tokens[1].strip();
//        String panelOrEdgeVertices = tokens[2].strip();
//        double numLEDS = Double.parseDouble(tokens[3].strip());
        String jbox = tokens[4].strip();
//        String circuit = tokens[5].strip();
//        double current = Double.parseDouble(tokens[6].strip());
//        boolean isEdge = panelOrEdgeId.contains("-");
        int jboxVertex = Integer.parseInt(jbox.split("-")[0]);
        int jboxIdx = Integer.parseInt(jbox.split("-")[1]);

        // collect vertices that are powerboxes
        if (!v2p.containsKey(jboxVertex)) {
          v2p.put(this.model.vertexesById.get(jboxVertex), 1);
        }

        if (jboxIdx > 0) {
          v2p.put(this.model.vertexesById.get(jboxVertex), jboxIdx + 1);
        }

      } catch (Exception e) {
        TE.err(e.toString());
        e.printStackTrace();
        TE.err(line);
      }
    }

    return v2p;
  }

  // https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
  private LXVector laserIntersection(LXVector planeNormal, LXVector planePoint,
                                     LXVector linePoint, LXVector lineDirection) {
    float numerator = planeNormal.dot(planePoint) - planeNormal.dot(linePoint);
    float denominator = planeNormal.dot(lineDirection.normalize());
    if (denominator == 0.0) return null;
    float t = numerator / denominator;
    return linePoint.copy().add(lineDirection.normalize().mult(t));
  }

  @Override
    public void onDraw(UI ui, PGraphics pg) {

    // if powerboxes are on, set opaque to false
    if (this.powerBoxesVisible.isOn())
      this.opaqueBackPanelsVisible.setValue(false);

    beginDraw(ui, pg);
    pg.noStroke();
    pg.textSize(40);
    for (Map.Entry<Integer, TEVertex> entry : model.vertexesById.entrySet()) {
      TEVertex v = entry.getValue();
      pg.pushMatrix();
      pg.translate(v.x, v.y, v.z);
      pg.ambientLight(255, 255, 255);
      pg.noLights();
      if (this.vertexLabelsVisible.getValueb()) {
        // Vertex labels are further outset past vertexSpheres by different percentages of x and z,
        // with hand-picked values to provide ovular clearance for the rotated labels below.
        pg.translate(v.x * .15f, 0, v.z * .02f);
        // Squashing z (the long fore-aft dimension) before rotating text to be normal to a radial
        pg.rotateY((float) (Math.atan2(v.x, v.z/5) + Math.PI));  // Face out
        pg.scale(10000, -10000);
        pg.fill(128, 128, 128);
        pg.text(entry.getKey().toString(), 0, 0, 0);
      }
      pg.popMatrix();

      // should we draw a powerbox (or 2) here?
      pg.pushMatrix();
      pg.translate(v.x, v.y, v.z);
      pg.ambientLight(255, 255, 255);
      pg.noLights();
      if (this.powerBoxesVisible.getValueb()) {
        if (vertex2Powerboxes.containsKey(v)) {  //HashMap<TEVertex, Integer>
          int boxCount = vertex2Powerboxes.get(v);
          if (boxCount == 1) {
            pg.translate(-1 * v.x * .2f, 0, -1 * v.z * .02f);
            pg.rotateY((float) (Math.atan2(v.x, v.z/5) + Math.PI));  // Face out
            pg.fill(255, 255, 0);
            pg.sphere(100000);
          } else {
            pg.translate(-1 * v.x * .2f, 0, -1 * v.z * .02f);
            pg.rotateY((float) (Math.atan2(v.x, v.z/5) + Math.PI));  // Face out
            pg.fill(255, 255, 0);
            pg.sphere(100000);
            pg.translate(2 * 100000, 0, 0);
            pg.fill(100, 100, 0);
            pg.sphere(100000);
          }
        }
      }
      pg.popMatrix();
    }

    for (Map.Entry<String, TEPanelModel> entry : model.panelsById.entrySet()) {
      TEPanelModel p = entry.getValue();
      if (p.virtualColor != null) {
        // respect unknown panel rendering ui toggle.
        if (p.panelType.equals(TEPanelModel.UNKNOWN) && !this.unknownPanelsVisible.isOn()) {
          continue;
        }
        pg.fill(p.virtualColor.rgb, p.virtualColor.alpha);
        pg.beginShape();
        pg.vertex(p.v0.x, p.v0.y, p.v0.z);
        pg.vertex(p.v1.x, p.v1.y, p.v1.z);
        pg.vertex(p.v2.x, p.v2.y, p.v2.z);
        pg.endShape();
      }

      if (this.opaqueBackPanelsVisible.isOn() && p.panelType.equals(TEPanelModel.LIT)) {
        LXVector[] inner = p.offsetTriangles.inner;
        pg.fill(LXColor.rgb(0,0,0), 230);
        pg.beginShape();
        pg.vertex(inner[0].x, inner[0].y, inner[0].z);
        pg.vertex(inner[1].x, inner[1].y, inner[1].z);
        pg.vertex(inner[2].x, inner[2].y, inner[2].z);
        pg.endShape();
      }

      // Label each panel
      if (this.panelLabelsVisible.getValueb()) {
        pg.pushMatrix();
        LXVector centroid = p.centroid;
        // Panel labels are outset from their centroid by different percentages of x and z,
        // with hand-picked values to provide ovular clearance for the rotated labels below.
        pg.translate(centroid.x * 1.15f, centroid.y, centroid.z * 1.02f);

        // Squashing z (the long fore-aft dimension) before rotating text to be normal to a radial
        pg.rotateY((float) (Math.atan2(centroid.x, centroid.z/5) + Math.PI));  // Face out
        //pg.rotateY((float) (-Math.PI / 2.0));  // Face port (non-show) side
        //pg.rotateY((float) (Math.PI / 2.0));  // Face starboard (show) side

        pg.scale(10000, -10000);
        pg.fill(255, 0, 0);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.text(entry.getKey(), 0, 0, -100000);
        pg.popMatrix();
      }
    }

    // should we draw the speakers?
    if (this.speakersVisible.getValueb()) {
      // draw speakers as rectangular prisms
      for (TEBox box : model.boxes) {
        pg.fill(LXColor.rgb(50, 60, 40), 255);
        for (List<LXVector> face : box.faces) {
          pg.beginShape();
          for (LXVector corner : face) {
            pg.vertex(corner.x, corner.y, corner.z);
          }
          pg.endShape();
        }
      }
    }

    for (List<POV> povs : this.laserPOV) {
      for (POV p : povs) {
        pg.pushMatrix();
        pg.stroke(p.rgb, 0xA0);
        pg.translate(p.v.x, p.v.y, p.v.z);
        pg.sphere(10000);
        pg.popMatrix();
      }
    }

    List<POV> newPOV = new ArrayList<>();
    for (TELaserModel laser : model.lasersById.values()) {
      if ((laser.color | LXColor.ALPHA_MASK) == LXColor.BLACK) continue;
      LXVector direction = laser.getDirection();
      if (direction == null) continue;
      LXVector groundSpot = laserIntersection(groundNormal, groundMountainPoint,
              laser.origin, direction);

      LXVector mountainSpot = laserIntersection(mountainNormal, groundMountainPoint,
              laser.origin, direction);

      // If the laser is pointed at a very steep upward angle, the math will
      // be so determined to find a spot where it hits the ground anyway that
      // it will conclude the laser must be capable of firing backward. Since
      // this is not a Darth Maul double-sided laser, ignore those "solutions".
      if (groundSpot != null && groundSpot.x > 0) groundSpot = null;
      if (mountainSpot != null && mountainSpot.x > 0) mountainSpot = null;

      LXVector laserSpot;
      if (groundSpot == null && mountainSpot == null) {
        continue;  // Laser never intersects ground or "mountain" plane
      } else if (groundSpot == null) {
        laserSpot = mountainSpot;
      } else if (mountainSpot == null) {
        laserSpot = groundSpot;
      } else if (laser.origin.dist(groundSpot) < laser.origin.dist(mountainSpot)) {
        laserSpot = groundSpot;
      } else {
        laserSpot = mountainSpot;
      }

      pg.stroke(laser.color, 0xA0);
      pg.line(laser.origin.x, laser.origin.y, laser.origin.z, laserSpot.x, laserSpot.y, laserSpot.z);
      pg.pushMatrix();
      pg.stroke(laser.color);
      pg.translate(laserSpot.x, laserSpot.y, laserSpot.z);
      newPOV.add(new POV(laserSpot, laser.color));
      pg.sphere(10000);
      pg.popMatrix();
    }

    this.laserPOV.remove(0);
    this.laserPOV.add(newPOV);

    endDraw(ui, pg);
  }
}
