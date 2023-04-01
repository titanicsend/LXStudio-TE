#define PI 3.14159265359
#define TAU PI * 2.0
#define DEG2RAD PI/180.

float horizonY = 0.15;
float fov = 90.0;
float farClip = -16.0;
float boltSize = 0.25;

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

    // scale and rotate according to current control settings
    p *= iScale;
    p = rotate(p,iRotationAngle);

    // iWowTrigger puts it in boat mode!
    horizonY += (iWowTrigger) ? 0.1 * sin(p.x * 5.0 + iTime * 3.0) : 0.0;

    fragColor = vec4(0.0);

    // Define the current grid displacement
    vec3 displace = vec3(0.0, -4.0*iTime, 1.5);

    // bend x axis to the beat - iWow1 controls the amplitude
    p.x += (iWow1/6.0) * sin(iTime + p.y * 12.0);

    // Get worldspace position of grid
    vec3 gridPos = revProject(p - vec2(0., horizonY), displace.z, fov);

    // display grid if inside z-clipping region
    if (p.y <= horizonY && gridPos.z >= farClip)  {

        // Create grid
        vec2 grid = fract(gridPos.xz - displace.xy) - 0.5;

        // Compute distance from grid edges
        float dist = smin(-grid.x * grid.x,-grid.y * grid.y,0.2);
        dist = dist * dist;

        // Send brightness pulses down the Y gridlines to the beat - iWow2 controls amplitude

        // compute normalized distance to far clipping plane
        float zn = gridPos.z/farClip;

        // change the direction of the beat pulses depending on the direction of time
        float beatDist = (iSpeed >= 0) ? abs((1.-zn) - beat) : abs(zn - beat);

        beatDist = (beatDist <= boltSize) ? (boltSize - beatDist) / boltSize : 0.0;
        float pattern = (grid.x * grid.x) * beatDist;
        pattern = iWow2 * pow(pattern,1.35);

        // Fade grid as we approach far clipping plane.  This gets rid of a lot
        // of potential aliasing trouble.
        float fade = 2.0-zn;

        // vary the underlying "terrain" color a little to enhanve movement
        float glow = (iQuantity * 1.25 - dist) + clamp(0.3*sin(8.0 * zn - iTime*6.0),-1.0,0.0);

        // Calculate and scale overall brightness - iQuantity controls glow level
        float intensity = max(glow,14.0 * fade * (dist + pattern));

        // set final color and build reasonable alpha value from brightness
        fragColor = vec4(iColorRGB * intensity,0.0);
        fragColor.a = max(fragColor.r,max(fragColor.g,fragColor.b));
    }
}