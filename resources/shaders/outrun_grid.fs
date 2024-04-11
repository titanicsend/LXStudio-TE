#define PI 3.14159265359
#define TAU PI * 2.0
#define DEG2RAD PI/180.

float horizonY = 0.15;
float fov = 90.0;
float farClip = -16.0;
float boltSize = 0.25;


float trebleEffect = frequencyReact*trebleRatio;
float bassEffect = levelReact*bassRatio;

// Parameters derived from controls.  Grouped here for
// easier tuning.
float invert = (iWowTrigger) ? 1.0 : -1.0; // WowTrigger inverts colors
float glowScale = (iScale+(1.+0.2*trebleEffect)) / 10.0;  // line width/glow factor
float gridScale = 0.5 + (iQuantity * trebleEffect);  // grid size (number of lines visible)

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
  mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  return rotationMatrix * point;
}

// polynomial smooth min/max from
// https://iquilezles.org/articles/smin/
// (works as max if you invert the signs of a and b.)
// k is the level of smoothing.  k = 0.1 is a good starting place
float smin( float a, float b, float k ) {
    float h = max( k-abs(a-b), 0.0 )/k;
    return min( a, b ) - h*h*k*(1.0/4.0);
}

// Project camera to world plane with constant worldY (height)
vec3 revProject(vec2 camPos, float worldY, float fov) {
    float worldZ = worldY / (camPos.y * tan(fov*DEG2RAD*.5));
    float worldX = worldY * camPos.x / camPos.y;
    return vec3(worldX, worldY, worldZ);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = fragCoord / iResolution.xy;
    vec2 p = (fragCoord.xy - iResolution.xy*.5) / iResolution.y;

    fragColor = vec4(0.0);

    // rotate according to current control settings
    p = rotate(p,iRotationAngle);

    // iWow1 controls path curvature
    float pathCurvature = iWow1 + bassEffect;

    // high values of iWow1 add 3D waves to the horizon
    horizonY += (pathCurvature > 0.5) ? (0.2*(pathCurvature - 0.5)) * sin(p.x * 5.0 + iTime * 3.0) : 0.0;

    // Define the current grid displacement
    vec3 displace = vec3(0.0, -4.0 * (1.0 / gridScale) * -iTime, 1.5);

    // bend x axis to the beat - iWow1 controls the amplitude of the curves
    p.x += min(0.5,pathCurvature)/6.0 * sin(iTime + p.y * 12.0);

    // Get worldspace position of grid
    vec3 gridPos = revProject(p - vec2(0.0, horizonY), displace.z, fov);

    // display grid if inside z-clipping region
    if (p.y <= horizonY && gridPos.z >= farClip)  {

        // Create grid - Quantity sets the number of gridlines (subdivision size)
        vec2 grid = fract(gridScale * (gridPos.xz - displace.xy)) - 0.5;

        // Compute distance from grid edges
        float dist = smin(invert * grid.x * grid.x,invert * grid.y * grid.y,-invert * glowScale);
        dist = dist * dist;

        // Send brightness pulses down the Y gridlines to the beat - iWow2 controls amplitude

        // compute normalized distance to far clipping plane
        float zn = gridPos.z/farClip;

        // change the direction of the beat pulses depending on the direction of time
        float beatDist = (iSpeed >= 0.0) ? abs((1.-zn) - beat) : abs(zn - beat);

        beatDist = (beatDist <= boltSize) ? (boltSize - beatDist) / boltSize : 0.0;
        float pattern = (grid.x * grid.x) * beatDist;
        pattern = iWow2 * pow(pattern,1.3);

        // Fade grid as we approach far clipping plane.  This gets rid of a lot
        // of potential aliasing trouble.
        float fade = 2.0-zn;

        // vary the underlying "terrain" color a little to enhance movement
        float glow = (glowScale - dist) + max(0.3*sin(8.0 * zn - iTime*6.0),0.0);

        // Calculate and scale overall brightness
        float intensity = max(glow,14.0 * fade * (dist + pattern));

        // set final color and build reasonable alpha value from brightness
        fragColor = vec4(iColorRGB * intensity, 1.0);
    }
}