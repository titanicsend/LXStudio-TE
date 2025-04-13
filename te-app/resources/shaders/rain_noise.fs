#define TE_NOTRANSLATE
const float PI = asin(1.) * 2.;
const float TAU = PI * 2.0;
const float SQRT2 = sqrt(2.0);

// hash from Dave Hoskins hash-without-sine.
// https://www.shadertoy.com/view/4djSRW
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// quick, cheap cell-based value noise generator
float noise(float x, float s) {
    float fx = fract(x);
    float ix = floor(x);

    float mx = (3. - 2. * fx) * fx * fx;

    float l = hash(vec2(ix, s));
    float h = hash(vec2(ix + 1.0, s));

    return mix(l, h, mx);
}

// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    float c = cos(angle); float s = sin(angle);
    mat2 rotationMatrix = mat2(c, -s, s, c);
    return origin + iScale * (rotationMatrix * (point - origin));
}

void mainImage(out vec4 fragColor, in vec2 coord) {
    // the usual normalize, rotate, scale
    coord = coord / iResolution.xy;
    vec2 origin =  vec2(0.5);
    coord = rotate(coord,origin, iRotationAngle);
    coord -= iTranslate / 8.0;

    // get cell id based on scaled x coordinate
    float id = round(coord.x * iResolution.x);
    // Wow1 controls band relative y scale
    float ypos = coord.y * iWow1;

    // speed variance between cells
    float time = iTime / 16.;
    ypos += (time + noise(time * .2 + hash(vec2(id, 0.0)), id) * SQRT2) * 2.;

    // length variance
    ypos += 0.6 * -0.5 + noise(ypos, id);

    // tweak final feature scale to look nice!
    ypos *= 10.;

    // randomly decide which cells to display
    float bri = 0.0;
    if (hash(vec2(floor(ypos), id)) < iQuantity) {
        // set things up to slightly antialias bands along x axis and fade
        //  the band tail along y axis
        float xdist = 1.0 - fract(abs(coord.x * iResolution.x - id));
        float ydist = max(0.0,fract(ypos));

        // switch tail direction w/speed
        ydist = (iSpeed >= 0.) ? 1.0 - ydist : ydist;
        ydist *= ydist;

        bri = length(vec2(xdist,ydist));
    }
    // iWow2 controls the foreground/gradient mix
    vec3 col = mix(iColorRGB,mix(iColorRGB,iColor2RGB,mod(id,4.) / 4.),iWow2);
    fragColor = vec4(col,bri);
}
