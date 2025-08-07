#include <include/constants.fs>
#include <include/colorspace.fs>

#define DEG2RAD PI/180.

// NOTE: Change bin count if we ever decide to use more than 16 bins in
// Chromatik
#define BIN_COUNT 16.0
#define BIN_SIZE (512.0 / BIN_COUNT)

float horizonY = 0.15;
float fov = 90.0;
float farClip = -16.0;
float boltSize = 0.25;

// Parameters derived from controls.  Grouped here for
// easier tuning.
float invert = (iWowTrigger) ? 1.0 : -1.0; // WowTrigger inverts colors
float glowScale = iScale / 10.0;  // line width/glow factor
float gridScale = 0.5 + (iQuantity);  // grid size (number of lines visible)

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
  mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  return rotationMatrix * point;
}

float getSmoothedFFT(float x) {
    float xNorm = 0.5 - (abs(x));
    // Map to bin index [0, BIN_COUNT - 1]
    float binf = xNorm * BIN_SIZE;
    float bin = floor(binf);
    float t = binf - bin; // fractional part for interpolation

    float index = bin * BIN_SIZE + 0.5 * BIN_SIZE; // center of the bin

    // Sample current and adjacent bins for smoothing. Otherwise, with
    // only 16 FFT bins, the terrain data is pretty chunky.
    float v1 = texelFetch(iChannel0, ivec2(index, 0), 0).x;
    float v2 = texelFetch(iChannel0, ivec2(index + BIN_SIZE, 0), 0).x;

    return mix(v1,v2,t);
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
    vec2 p = (fragCoord.xy - iResolution.xy*.5) / iResolution.xy;

    // Calculate terrain height displacement based on the current frame
    // of FFT data.
    float heightOffset = levelReact * getSmoothedFFT(p.x);

    // rotate according to current control settings
    p = rotate(p,iRotationAngle);

    // Define the current grid displacement
    vec3 displace = vec3(0.0, -4.0 * (1.0 / gridScale) * -iTime, 1.5);

    // bend x axis to the beat - iWow1 controls the amplitude of the curves
    p.x += min(0.5,iWow1)/6.0 * sin(iTime + p.y * 20.0);

    // Get worldspace position of grid
    horizonY += heightOffset;
    vec3 gridPos = revProject(p - vec2(0.0, horizonY), displace.z, fov);

    // default to transparent blackness if outside of grid
    fragColor = vec4(0.0);

    // draw grid if inside z-clipping region
    if (p.y <= horizonY && gridPos.z >= farClip)  {

        // Create grid - Quantity sets the number of gridlines (subdivision size)
        vec2 grid = fract(gridScale * (gridPos.xz - displace.xy)) - 0.5;
        float clr = gridPos.z/farClip;

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

        // vary the underlying ground color a little to enhance movement
        float glow = (glowScale - dist) + max(0.3*sin(8.0 * zn - iTime*6.0),0.0);

        // Calculate and scale overall brightness
        float intensity = max(glow,14.0 * fade * (dist + pattern));

        // set final color and build reasonable alpha value from brightness
        fragColor = vec4(getGradientColor(clr) * intensity, 1.0);
    }
}