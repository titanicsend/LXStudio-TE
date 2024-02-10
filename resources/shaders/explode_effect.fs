// tell the preprocessor and any control management scripts that this is a post effect shader
// and doesn't use the common controls.
#define TE_EFFECTSHADER

uniform float basis;
uniform float size;

float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.543, 514.123)))*4732.12);
}

vec2 random2(vec2 p) {
    return vec2(rand(p), rand(p*vec2(12.9898, 78.233)));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec2 quv = uv; // copy we use to quantize later

    // To "explode", we randomly displace uv coordinates in
    // variable-sized cells. The x size is doubled because
    // we're using the full 3D projection texture to map
    // onto the car, and it is set up so each half of the
    // car (sliced at x == 0) occupies half the width of the
    // texture.
    if (size > 0.0) {
        vec2 scale=vec2(2. * size, size);
        quv = floor(quv * scale) / scale;
    }
    vec2 displacement = (-0.5 + random2(quv));

    fragColor = texture(iBackbuffer, uv + displacement * basis);
}
