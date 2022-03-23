package titanicsend.util;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEVertex;
import Jama.Matrix;

// Holds the information necessary to get a point off the floor
// and up into a flying triangle, as well as three points on the floor
// corresponding to where the triangle was lying before it floated away.
public class FloorTransform {
  Matrix flightInstructions;
  FloorPoint f0; // Always at the origin:  (0, 0)
  FloorPoint f1; // Always on the Z-axis:  (x, 0)
  FloorPoint f2; // Anywhere on the plane: (x, z)

  // Take a point on the floor and move it back into the sky using the stored transform
  LXPoint fly(FloorPoint f) {
    double x = f.x;
    double y = 0.0;
    double z = f.z;

    Matrix floorMatrix = new Matrix(new double[][]{{x}, {y}, {z}, {1}});
    Matrix flyingMatrix = this.flightInstructions.times(floorMatrix);
    return new LXPoint(
              flyingMatrix.get(0, 0),
              flyingMatrix.get(1, 0),
              flyingMatrix.get(2, 0)
              );
  }

  FloorTransform(TEVertex v0, TEVertex v1, TEVertex v2) {
    double distance01 = v0.distanceTo(v1);
    double distance02 = v0.distanceTo(v2);
    double distance12 = v1.distanceTo(v2);

    // Put the first vertex at the origin
    this.f0 = new FloorPoint(0.0,0.0);

    // Put the second to the right of the first, along the X-axis
    this.f1 = new FloorPoint(distance01, 0.0);

    // As for the third, well... it's complicated
    double d01Squared = Math.pow(distance01, 2);
    double d02Squared = Math.pow(distance02, 2);
    double d12Squared = Math.pow(distance12, 2);
    double denominator = 2.0 * distance01 * distance02;
    double fraction = (d01Squared + d02Squared - d12Squared) / denominator;
    double angle = Math.acos(fraction);
    LXVector vector = new LXVector((float)distance02, 0, 0);
    vector.rotate((float)angle);
    assert vector.z == 0.0;
    this.f2 = new FloorPoint(vector.x, vector.y);

    // Now we need to save instructions for going back to the original coordinate system
    double[][] flyingVals = {
            {v0.x, v1.x, v2.x},
            {v0.y, v1.y, v2.y},
            {v0.z, v1.z, v2.z},
            {1, 1, 1},
    };
    Matrix flyingMatrix = new Matrix(flyingVals);

    double[][] floorVals = {
            {f0.x, f1.x, f2.x},
            {0.0, 0.0, 0.0},
            {f0.z, f1.z, f2.z},
            {1, 1, 1},
    };
    Matrix floorMatrix = new Matrix(floorVals);
    this.flightInstructions = flyingMatrix.times(floorMatrix.inverse());
  }
}