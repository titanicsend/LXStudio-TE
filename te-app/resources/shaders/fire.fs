#pragma TEControl.SIZE.Disable
#pragma TEControl.QUANTITY.Disable
#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable

//fork of https://www.shadertoy.com/view/4ttGWMf

float rand(vec2 n) {
    return fract(sin(cos(dot(n, vec2(12.9898,12.1414)))) * 83758.5453);
}

float noise(vec2 n) {
    const vec2 d = vec2(0.0, 1.0);
    vec2 b = floor(n), f = smoothstep(vec2(0.0), vec2(1.0), fract(n));
    return mix(mix(rand(b), rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y);
}

float fbm(vec2 n) {
    float total = 0.0, amplitude = 1.0;
    for (int i = 0; i <5; i++) {
        total += noise(n) * amplitude;
        n += n*1.7;
        amplitude *= 0.47;
    }
    return total;
}

// Color Space Conversions
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 speed = vec2(0.1, 0.9);
    float shift = 1.327+sin(iTime*2.0)/2.4;

	float dist = 3.5-sin(iTime*0.4)/1.89;

    vec2 uv = fragCoord.xy / iResolution.xy;
    vec2 p = fragCoord.xy * dist / iResolution.xx;
    p += sin(p.yx*4.0+vec2(.2,-.3)*iTime)*0.04;
    p += sin(p.yx*8.0+vec2(.6,+.1)*iTime)*0.01;

    p.x -= iTime/1.1;
    float q = fbm(p - iTime * 0.3+1.0*sin(iTime+0.5)/2.0);
    float qb = fbm(p - iTime * 0.4+0.1*cos(iTime)/2.0);
    float q2 = fbm(p - iTime * 0.44 - 5.0*cos(iTime)/2.0) - 6.0;
    float q3 = fbm(p - iTime * 0.9 - 10.0*cos(iTime)/15.0)-4.0;
    float q4 = fbm(p - iTime * 1.4 - 20.0*sin(iTime)/14.0)+2.0;
    q = (q + qb - .4 * q2 -2.0*q3  + .6*q4)/3.8;
    vec2 r = vec2(fbm(p + q /2.0 + iTime * speed.x - p.x - p.y), fbm(p + q - iTime * speed.y));

    vec3 color=vec3(1.0,.2,.05)/(pow((r.y+r.y)* max(.0,p.y)+0.1, 4.0));;
    color = color/(1.0+max(vec3(0),color));

    // colored fire retrofit.  Convert calculated fire color to HSV then
    // take the hue of the current palette foreground color, and slightly
    // resaturate because colored fire looks better with a little less white
    vec3 c2 = rgb2hsv(color);
    c2.x = iColorHSB.x;
    c2.y += r.y * r.y;
    c2 = hsv2rgb(c2);

    // use Wow2 control to blend w/original fire-colored fire for
    // very nice tinted flame effects.
    fragColor = vec4(mix(c2,color,iWow2), 1.0);
}
