//#define HIGH_QUALITY_NOISE

float noise( in vec3 x )
{
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    #ifndef HIGH_QUALITY_NOISE
    vec2 uv = (p.xy+vec2(37.0,17.0)*p.z) + f.xy;
    vec2 rg = textureLod( iChannel0, (uv+ 0.5)/256.0, 0.0 ).yx;
    #else
    vec2 uv  = (p.xy+vec2(37.0,17.0)*p.z);
    vec2 rg1 = textureLod( iChannel0, (uv+ vec2(0.5,0.5))/256.0, 0.0 ).yx;
    vec2 rg2 = textureLod( iChannel0, (uv+ vec2(1.5,0.5))/256.0, 0.0 ).yx;
    vec2 rg3 = textureLod( iChannel0, (uv+ vec2(0.5,1.5))/256.0, 0.0 ).yx;
    vec2 rg4 = textureLod( iChannel0, (uv+ vec2(1.5,1.5))/256.0, 0.0 ).yx;
    vec2 rg  = mix( mix(rg1,rg2,f.x), mix(rg3,rg4,f.x), f.y );
    #endif
    return mix( rg.x, rg.y, f.z );
}

float noise( in vec2 x ) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    vec2 uv = p.xy + f.xy*f.xy*(3.0-2.0*f.xy);
    return textureLod( iChannel0, (uv+118.4)/256.0, 0.0 ).x;
}

float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.543,514.123)))*4732.12);
}

vec2 random2(vec2 p) {
    return vec2(rand(p), rand(p*vec2(12.9898,78.233)));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec4 col1 = texture(iBackbuffer, uv);

    // quantize uv into a grid of 0.1 x 0.1 squares
    vec2 scale=vec2(200.0,100.0);
    uv = floor(uv * scale) / scale;
    float t = fract(iTime / 2.);
    t = t * t * t;

    float mag = t * 0.3;

    vec2 displacement = (-0.5 + random2(uv)) * mag;
    uv += displacement;

    vec4 col = mix(col1,texture(iBackbuffer, uv),t);
    fragColor = col;
}
