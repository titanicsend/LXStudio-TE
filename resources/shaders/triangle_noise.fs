
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// build 2D rotation matrix
mat2 rotate2D(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

// triangle wave functions
float tri(in float x) {return abs(fract(x) - .5);}
vec2 tri2(in vec2 p) {return vec2(tri(p.x + tri(p.y * 2.)), tri(p.y + tri(p.x * 2.)));}

// precalculated rotation matrix for noise octaves
mat2 m2 = mat2(0.970, 0.242, -0.242, 0.970);

// Triangle noise algorithm from nimitz
// https://www.shadertoy.com/view/Mts3zM
float triangleNoise(in vec2 p)
{
    float z = 1.5;
    float z2 = 1.5;
    float rz = 0.;
    vec2 bp = p + 0.02 * vec2(sin(iTime * 0.3), -cos(10. + iTime * 0.35));
    for (float i = 0.; i <= 3.; i++) {
        vec2 dg = tri2(bp * 2.) * .8;
        dg *= rotate2D(iTime * .3);
        p += dg / z2;

        bp *= 1.6;
        z2 *= .6;
        z *= 1.8;
        p *= 1.2;
        p *= m2;

        rz += tri(p.x + tri(p.y)) / z;
    }
    return rz;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    float aspect = iResolution.x / iResolution.y;

    vec2 p = fragCoord.xy / iResolution.xy * 2. - 1.;
    p.x *= aspect;
    p *= iScale;

    float noise = 0.5 + triangleNoise(0.2 * p);
    noise = log(10.0 * pow(noise, 4.));

    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = noise * mix(iColorRGB, mix(iColor2RGB, iColorRGB,smoothstep(0.5,0.9, noise)), iWow2);

    fragColor = vec4(col, max(col.r,max(col.g,col.b)));
}