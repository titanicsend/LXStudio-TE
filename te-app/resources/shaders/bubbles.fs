#include <include/constants.fs>
#include <include/colorspace.fs>
vec2 uv;

// build 2D rotation matrix
mat2 rotate2D(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

// get background color from the palette based on uv coord and time
vec3 getBackgroundColor(vec2 uv) {
    return getGradientColor(0.5 + 0.5 * sin((0.5 * uv.y + uv.x) + iTime * 0.4));
}

// pile of utility hash functions, most from Dave Hoskins
// https://www.shadertoy.com/view/4djGRh
vec2 hash2z(float r, float xb) {
    return vec2(fract(15.32354 * (r+xb)), fract(17.25865 * (r+xb)));
}

vec2 hash2a(vec2 x, float anim) {
    float r = 523.0*sin(dot(x, vec2(53.3158, 43.6143)));
    float xa1=fract(anim);// position
    float xb1=anim-xa1;// cell
    anim+=0.5;
    float xa2=fract(anim);
    float xb2=anim-xa2;

    vec2 z1=hash2z(r++, xb1);
    vec2 z2=hash2z(r++, xb1);
    vec2 z3=hash2z(r++, xb2);
    vec2 z4=hash2z(r, xb2);
    return (mix(z1, z2, xa1)+mix(z3, z4, xa2))*0.5;
}

float hashNull(vec2 x) {
    float r = fract(523.0*sin(dot(x, vec2(53.3158, 43.6143))));
    return r;
}

vec4 NC0=vec4(0.0, 157.0, 113.0, 270.0);
vec4 NC1=vec4(1.0, 158.0, 114.0, 271.0);

vec4 hash4(vec4 n) { return fract(sin(n)*753.5453123); }
vec2 hash2(vec2 n) { return fract(sin(n)*753.5453123); }

float hashNoise2(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f*f*(3.0-2.0*f);

    float n = p.x + p.y*157.0;
    vec2 s1=mix(hash2(vec2(n)+NC0.xy), hash2(vec2(n)+NC1.xy), vec2(f.x));
    return mix(s1.x, s1.y, f.y);
}

float hashNoise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);

    float n = p.x + dot(p.yz, vec2(157.0, 113.0));
    vec4 s1=mix(hash4(vec4(n)+NC0), hash4(vec4(n)+NC1), vec4(f.x));
    return mix(mix(s1.x, s1.y, f.y), mix(s1.z, s1.w, f.y), f.z);
}

// Create a bubble at the given position
vec4 bubble(vec2 te, vec2 pos, float numCells) {
    // squared distance to the bubble center
    float d = dot(te, te);

    // beat reactivity changes distance for "wobble" effect
    d -= (beat * iWow1);

    // adjust offset for moving bubble
    vec2 te1 = te + (pos - vec2(0.5, 0.5)) * 0.4/numCells;
    vec2 te2 =- te1;

    // Use a bunch of noise values to make highlights and reflections
    float zb1=max(pow(hashNoise2(te2 * 1000.11 * d), 10.0), 0.01);
    float zb2=hashNoise2(te1 * 1000.11 * d);
    float zb3=hashNoise2(te1 * 200.11 * d);
    float zb4=hashNoise2(te1 * 200.11 * d + vec2(20.0));

    // random greyscale highlight color based on the distance from the bubble center
    vec4 colorb = vec4(1.0);
    colorb.rgb *= (0.3 + hashNoise2(te1 * 1000.11 * d) * 0.3);

    // control transparency in the interior of the bubble. A lot of
    // the bubble should be transparent.
    zb2=max(pow(zb2, 20.1), 0.02);
    colorb.rgb *= zb2;

    // pick up "iridescence" colors from the background.
    // (scaled by the iWow2 setting, but always allowing a little palette color)
    vec4 color = (max(iWow2,0.35)) * vec4(getBackgroundColor(uv - 0.1), 1.0);
    color = mix(color, vec4(1.0), hashNoise2(te2*20.5+vec2(200.0, 200.0)));

    // randomize the color based on the distance to the bubble center
    color.rgb *= 0.7 + hashNoise2(te2*1000.11*d)*0.3;
    color.rgb *= 0.2 + zb1*1.9;

    // calculate the bubble's size
    float r1 = max(min((0.033 - min(0.04, d)) * 100.0/sqrt(numCells), 1.0), -1.6);
    float d2 = (0.06 - min(0.06, d)) * 10.0;


    d = (0.04 - min(0.04, d)) * 10.0;
    color.rgb = color.rgb + colorb.rgb * d * 1.5;

    // calculate brightness for layered highlights and fake reflection/refraction
    float f1 = min(d * 10.0, 0.5 - d)*2.2;
    f1 = f1 * f1 * f1 * f1;//pow(f1, 4.0);

    float f2=min(min(d * 4.1, 0.9 - d) * 2.0 * r1, 1.0);

    float f3 = min(d2 * 2.0, 0.7 - d2) * 2.2;
    f3 = f3 * f3 * f3 * f3;// pow(f3, 4.0);

    // and combine everything for the final color
    return vec4(color * max(min(f1 + f2, 1.0), -0.5) +
    vec4(zb3) * f3 - vec4(zb4) * (f2 * 0.5 + f1) * 0.5);
}

// Tiled cell algorithm from Dave Hoskins.  See link above.
vec4 drawCellLayer(vec2 p, vec2 move, in float numCells, in float count) {
    vec2 inp = p + move;
    inp *= numCells;
    float d = 1.0;
    vec2 te;
    vec2 pos;

    // find the closest cell center
    for (int xo = -1; xo <= 1; xo++) {
        for (int yo = -1; yo <= 1; yo++) {
            // calculate the current cell position
            vec2 tp = floor(inp) + vec2(xo, yo);

            vec2 rr=mod(tp, numCells);
            tp = tp + (hash2a(rr, iTime*0.1)+hash2a(rr, iTime*0.1+0.25))*0.5;

            // distance of current pixel to the cell center
            vec2 l = inp - tp;
            float dr = dot(l, l);

            // if cell is active and close enough, update the distance
            if (hashNull(rr) > (count * iQuantity)) {
                if (d > dr) {
                    d = dr;
                    pos = tp;
                }
            }
        }
    }
    // if outside the cell area, return black
    if (d>=0.06) return vec4(0.0);

    // we're drawing a bubble!
    // Find the bubble's offset within the current cell
    te = inp - pos;

    // add a little distortion based on the distance to the bubble center
    // to fake reflection and refraction
    if (d < 0.04) uv = uv + te * d * 2.0;

    // simple antialiasing blur.  Really helps smooth things out on LEDs
    // just redraws the bubble a few times at small offsets
    vec4 c = vec4(0.0);
    for (float x = -1.0; x < 1.0; x += 0.5) {
        for (float y = -1.0; y < 1.0; y += 0.5) {
            c += bubble(te + vec2(x, y) * 0.01, p, numCells);
        }
    }
    return c*0.05;

}

// add some amount of the current bubble's color to the final result
vec4 addBubbleTint(vec4 color, vec4 cr, float scale) {
    return max(color - vec4(dot(cr, cr)) * 0.1, 0.0) + cr * scale;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalize the coordinates to [0, 1] range
    uv = -1.0 + (2.0 * (fragCoord.xy) / iResolution.xy);
    uv *= iScale;

    // center, rotate and scale
    uv *= rotate2D(-iRotationAngle);

    // movement speed and direction vectors for bubble layers
    vec2 l1=vec2(iTime*0.06, -iTime*0.25);
    vec2 l2=vec2(-iTime*0.1, -iTime*0.45);
    vec2 l3=vec2(-iTime*0.005, -iTime*0.15);

    // base color is a gradient based on the current TE palette
    vec4 color = vec4((iWow2 * 0.5) * getBackgroundColor(uv), 0.995);

    // generate several layers of bubbles and calculate their contribution
    // to the pixel's color. Ideally this would be a loop, but
    // the parameters are hand tuned for each layer, and GLSL would
    // just unroll the loop anyway.
    vec4 cr = drawCellLayer(uv, vec2(20.2449, 93.78)+l1, 2.0, 0.5);
    color = addBubbleTint(color, cr, 1.8);

    cr = drawCellLayer(uv, vec2(10.0, 100.0)+ l3, 3.0, 0.5);
    color = addBubbleTint(color, cr, 1.4);

    cr = drawCellLayer(uv, vec2(230.79, 193.2)+l2, 4.0, 0.5);
    color = addBubbleTint(color, cr, 1.1);

    cr = drawCellLayer(uv, vec2(200.19, 393.2)+l3, 7.0, 0.8);
    color = addBubbleTint(color, cr, 1.3);

    cr = drawCellLayer(uv, vec2(10.3245, 233.645)+l3, 9.2, 0.9);
    color = addBubbleTint(color, cr, 1.6);

    cr = drawCellLayer(uv, vec2(10.3245, 233.645)+l3, 14.2, 0.95);
    color = addBubbleTint(color, cr, 1.6);

    fragColor = color;
    fragColor.a = 0.995;
}
