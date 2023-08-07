const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

float shape(in vec2 st, in int sides) {
    // Angle and radius from the current pixel
    float a = atan(st.x,st.y)+PI;
    float r = TWO_PI/float(sides);

    // Shaping function that modulate the distance
    float d = cos(floor(.5+a/r)*r-a)*length(st);

    return d;
}

float box(in vec2 _st, in vec2 _size){
    _size = vec2(0.5) - _size*0.5;
    vec2 uv = smoothstep(_size,
                        _size+vec2(0.001),
                        _st);
    uv *= smoothstep(_size,
                    _size+vec2(0.001),
                    vec2(1.0)-_st);
    return uv.x*uv.y;
}

float xcross(in vec2 st, float xsize, float ysize, float outer, float inner){
    float pct = 0.;
    pct += box(st, vec2(outer*xsize, outer*xsize/4.));
    pct += box(st, vec2(outer*ysize/4., outer*ysize));

    pct -= box(st, vec2(inner*xsize, inner*xsize/4.));
    pct -= box(st, vec2(inner*ysize/4., inner*ysize));

    pct += box(st, vec2(ysize/4., ysize));
    pct += box(st, vec2(xsize, xsize/4.));

    return pct;
}

float nest_xcross(float prev_pct, in vec2 st, float xsize, float ysize, float outer, float inner, float alpha, float base, float inc){
    float f1 = base + inc;
    float f2 = base + 2. * inc;
    float f3 = base + 3. * inc;
    float f4 = base + 4. * inc;

    float pct = prev_pct;
    pct = alpha*xcross(st, xsize*base, ysize*base, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f1, ysize*f1, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f2, ysize*f2, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f3, ysize*f3, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f3, ysize*f3, outer, inner) - pct;
    return pct;
}

float noise1d(in float x, in float ts) {
  float amplitude = 0.2 * pow(x, 3.);
  float frequency = 2.;
  float y = sin(x * frequency);
  // float t = 0.01*(-iTime*130.0);
  float t = 0.01*(-ts*130.0);
  y += sin(x*frequency*2.1 + t)*4.5;
  y += sin(x*frequency*1.72 + t*1.121)*4.0;
  y += sin(x*frequency*2.221 + t*0.437)*5.0;
  y += sin(x*frequency*3.1122+ t*4.269)*2.5;
  y *= amplitude*0.06;
  return y;
}

vec2 random2(vec2 st){
    st = vec2( dot(st,vec2(127.1,311.7)),
              dot(st,vec2(269.5,183.3)) );
    return -1.0 + 2.0*fract(sin(st)*43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    vec2 u = f*f*(3.0-2.0*f);
    return mix( mix( dot( random2(i + vec2(0.0,0.0) ), f - vec2(0.0,0.0) ),
                     dot( random2(i + vec2(1.0,0.0) ), f - vec2(1.0,0.0) ), u.x),
                mix( dot( random2(i + vec2(0.0,1.0) ), f - vec2(0.0,1.0) ),
                     dot( random2(i + vec2(1.0,1.0) ), f - vec2(1.0,1.0) ), u.x), u.y);
}

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

void mainImage(out vec4 fragColor, in vec2 fragCoord){
    vec2 st = fragCoord.xy/iResolution.xy;
    st.x *= iResolution.x/iResolution.y;

    st = st - vec2(0.5);
    st = rotate(st, iRotationAngle) / iScale;
    st -= iTranslate;

    vec3 color = vec3(0.0);
    float pct = 0.;
    float pct2 = 0.;

    float xsize = 0.1 + iWow1 * 0.5 * trebleLevel;
    float ysize = 0.25 - iWow1 * 0.2 * trebleLevel;
    float outer = 1.2 + iWow1 * 1.5 * bassLevel;
    float inner = 1.1 + iWow1 * 2.0 * bassLevel;
    //float xsize = 0.1;
    //float ysize = 0.15;
    //float outer = 1.2 + 0.2 * sin(iTime);
    //float inner = 1.1 + 0.3 * cos(iTime);
    float yoffset = 0.7;
    st += vec2(0.5, yoffset);
    for (int i = 0; i < int(iQuantity); i++) {
        if (i % 2 == 0) {
            pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 1.0 + 1.5*float(i), 0.5);
        } else {
            pct2 = nest_xcross(pct2, st, xsize, ysize, outer, inner, 0.5, 1.0 + 1.5*float(i), 0.5);
        }
    }
    st -= vec2(0.5, yoffset);

    // original triangle shape
    float f = shape(st, 3);
    // setup radial noise
    float a = atan(st.y,st.x);
    float normAngle = a / TWO_PI;

    // The audio texture size is 512x2
    // mapping to screen depends on iScale and iQuantity - here
    // we use iQuantity to figure out which texture pixels are relevant
    float index = mod(normAngle * TEXTURE_SIZE * 2.0, TEXTURE_SIZE);
    // The second row of is normalized waveform data
    // we'll just draw this over the spectrum analyzer.  Sound data
    // coming from LX is in the range -1 to 1, so we scale it and move it down
    // a bit so we can see it better.
    float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;
//    // subdivide fft data into bins determined by iQuantity
//    float p = floor(index / pixPerBin);
//    float tx = halfBin+pixPerBin * p;
//    float dist = abs(halfBin - mod(index, pixPerBin)) / halfBin;
//    // since we're using dist to calculate desatuation for a specular
//    // reflection effect, we'll modulate it with beat, to change
//    // apparent shininess and give it some extra bounce.
//    dist = dist - (beat * beat);
//    // get frequency data pixel from texture
//    float freq  = texelFetch(iChannel0, ivec2(tx, 0), 0).x;


    // scaling factor on triangle distance field
    float triangle_scale = 1.1;
    //triangle_scale = iWow2;
    float triangle_dist = f / triangle_scale;

    // innermost step function we apply to triangle distance field
    float r = 0.15;
    r += iWow2 * wave;
    r += iWow1 * 0.2 * volumeRatio;
    //r = iWow1;
    float tri_inner_mask = r + noise1d(0.35, 3.*iTime);

    float tri_inner_margin = 0.04;// - 0.04 * volumeRatio;
    float tri_inner_start = tri_inner_mask + tri_inner_margin;
    float tri_inner_thickness = 0.1;

    float tri_outer_margin = 0.01;
    float tri_outer_start = tri_inner_start + tri_inner_thickness + tri_outer_margin;
    float tri_outer_thickness = 0.01;

    float inner_mask = step(tri_inner_mask, triangle_dist);
    float outer_triangle_edge = step(tri_outer_start+tri_outer_thickness+0.07, triangle_dist);

    pct *= 1. - inner_mask;
    pct += step(tri_inner_start, triangle_dist) - step(tri_inner_start+tri_inner_thickness, triangle_dist);
    pct += 0.4*step(tri_outer_start, triangle_dist) - step(tri_outer_start+tri_outer_thickness, triangle_dist);
    pct += step(tri_outer_start+2.*tri_outer_thickness, triangle_dist) - step(tri_outer_start+3.*tri_outer_thickness, triangle_dist);

    if (pct2 == 0. || distance(iColor2RGB, vec3(0.)) < 0.1) {
        color += pct * iColorRGB;
    } else {
        pct2 += pct;
        color += pct2 * iColor2RGB;
    }
    //color += pct * iColorRGB;
    fragColor = vec4(color,1.0);
}