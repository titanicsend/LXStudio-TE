#pragma name "Vortex3"

#pragma TEControl.YPOS.Value(-0.15)
#pragma TEControl.SIZE.Range(1.0,0.1,5.0)

#pragma TEControl.QUANTITY.Range(30.0,5.0,100.0)
#pragma TEControl.WOW1.Range(0.5,0.0,2.0)
#pragma TEControl.WOW2.Range(0.5,0.0,1.0)

#pragma TEControl.FREQREACTIVITY.Range(1.0,0.0,1.0)
//#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable
// Wow2 controls audio reactivity

// #ifdef GL_ES
// precision mediump float;
// #endif
// uniform vec2 iResolution;
// uniform vec2 iMouse;
// uniform float u_time;

#define PI 3.141592653589793
#define HALF_PI 1.5707963267948966
#define TWO_PI 6.28318530718

float fill(float x, float size) {
    return 1. - step(size, x);
}

float triSDF(vec2 st) {
    st = (st*2.-1.)*2.;
    return max(abs(st.x) * 0.866025 + st.y * 0.5, -st.y * 0.5);
}

float rhombSDF(vec2 st) {
    return max(triSDF(st), triSDF(vec2(st.x, 1.-st.y)));
}

vec2 rotate(vec2 st, float a) {
    st = mat2(cos(a), -sin(a), sin(a), cos(a)) * (st-.5);
    return st + .5;
}

void mainImage(out vec4 fragColor,in vec2 fragCoord){
//     // hack: UI debug params
//     float norm_x = iMouse.x/iResolution.x;
//     float norm_y = iMouse.y/iResolution.y;
//
//     // norm_x = 1. - norm_x * 0.5;
//     norm_x = 0.5 + norm_x * 0.5;
//     //norm_y = 1. - norm_y;

    float norm_x = iWow1;
    float norm_y = iWow2;

    vec2 st = fragCoord.xy / iResolution.xy;
    st.x *= iResolution.x / iResolution.y;
    st *= iScale;
    st = rotate(st,iRotationAngle);


//     vec2 st = fragCoord.xy/iResolution.xy;
//     st.x *= iResolution.x/iResolution.y;

    vec3 color = vec3(0.);
    float pct = 0.;


    st -= .5;

    float r = dot(st,st);
    float a = (atan(st.y, st.x)/PI);
    vec2 uv = vec2(a,r);

    vec2 grid = vec2(5., log(r)*iQuantity*norm_y);
    // vec2 grid = vec2(5. + norm_x, log(r)*20.*norm_y);
    // vec2 grid = vec2(5.* 0.5*norm_x, log(r)*20.);
    // vec2 grid = vec2(5.* norm_x, log(r)*20.*norm_y);

    // vec2 grid = vec2(5.* norm_x, log(r)*20.*norm_y);
    // vec2 grid = vec2(9., log(r)*20.);

    vec2 uv_i = floor(uv * grid);
    // uv.x += .5 * mod(uv_i.y, 2.);
    uv.x += /*.5*/ norm_x * mod(uv_i.y, 4.);
    vec2 uv_f = fract(uv * grid);

    float rhomb = rhombSDF(uv_f);
    float tri = triSDF(uv_f);

    // float shape = abs(sin(u_time))*rhomb + (1.-sin(2.*u_time))*tri;
    float shape = norm_x * rhomb + norm_y * tri;
    // float shape = 0.9 * rhomb + 0.5 * tri;

    vec3 tmpColor = iColorRGB;
    if(mod(uv_i.y,2.) == 0.0){
        tmpColor=iColor2RGB;
    }
    if (uv_f.x * uv_f.y >= 0.15) {
        tmpColor = iColor2RGB;
    }

    float sz = .5;
    float iterMod = mod(uv_i.y, 4.);
    float d = .5;
    if (iterMod == 0.) {
        d *= mix(0., stemBass, frequencyReact);
    } else if (iterMod == 1.) {
        d *= mix(0., stemVocals, frequencyReact);
    } else if (iterMod == 2.) {
        d *= mix(0., stemDrums, frequencyReact);
    } else if (iterMod == 3.) {
        d *= mix(0., stemOther, frequencyReact);
    }
    sz += d;

    color += fill(shape,sz) * tmpColor;// * step(.75, 1. - r);

    // color = vec3(pct);
    fragColor = vec4(color,1.0);
}