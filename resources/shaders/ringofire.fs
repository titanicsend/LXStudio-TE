// Experimental fire for Mothership
#pragma name "RingOfFire"
#pragma TEControl.SIZE.Range(0.91,1.15,0.8)
#pragma TECONTROL.SPEED.Value(0.64)

#pragma TEControl.QUANTITY.Disable
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.WOW2.Disable
#pragma TEControl.WOW1.Disable

#include <include/constants.fs>
#include <include/colorspace.fs>

// generate 2D rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

// cheap 3D simplex noise from Shadertoy
// TODO - use texture-based noise fn instead.
float snoise(vec3 uv, float res) {
    const vec3 s = vec3(1.0, 10.0, 100.0);
    uv *= res;

    vec3 uv0 = floor(mod(uv, res))*s;
    vec3 uv1 = floor(mod(uv+vec3(1.), res))*s;

    vec3 f = fract(uv); f = f*f*(3.0-2.0*f);

    vec4 v = vec4(uv0.x+uv0.y+uv0.z, uv1.x+uv0.y+uv0.z,
    uv0.x+uv1.y+uv0.z, uv1.x+uv1.y+uv0.z);

    vec4 r = fract(sin(v*1e-1)*1e3);
    float r0 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);

    r = fract(sin((v + uv1.z - uv0.z)*1e-1)*1e3);
    float r1 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);

    return mix(r0, r1, f.z)*2.-1.;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 p = -.5 + fragCoord.xy / iResolution.xy;
    //p.x *= iResolution.x/iResolution.y;

    p *= r2d(iRotationAngle);
    p *= iScale;

    // convert to polar coordinates, scaled to fit the
    // Mothership's ring.
    vec3 coord = vec3(atan(p.x,p.y)/TAU+.5, length(p)*.4, .5);

    // Generate a few octaves of animated noise for the fire
    // Lower waves have more weight, higher have more detail
    float color = 3.0 - (3.*length(2.*p));
    for(int i = 1; i <= 7; i++) {
        float p2 = pow(2.0, float(i));
        color += (.5 / p2) * snoise(coord + vec3(0.,-iTime*.05, -iTime*.01), p2*16.);
    }
    color = clamp(color, 0., 1.);

    // adjust color for more variation
    vec3 hsb = iColorHSB;

    // just a tiny shift in hue gives the fire more life
    hsb.x += 0.08 * color;
    // saturation -- hotter (brighter) is whiter
    hsb.y = 1.5 - color * color;
    // n.b.- we leave value where it was in the palette color
    // let alpha handle brightness

    fragColor = vec4(hsv2rgb(hsb), color);
}