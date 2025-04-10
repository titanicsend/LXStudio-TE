const float PI = 3.1415926;
const float TAU = PI * 2;
const float brightness_floor = 0.4;

// build 2D rotation matrix
mat2 rotate2D(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float wave(float n) {
    return 0.5 + 0.5 * sin(fract(n) * TAU);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // normalize and rotat
    vec2 uv = fragCoord.xy / iResolution.xy * 2.0 - 1.0;
    uv *= rotate2D(-iRotationAngle);

    float bassTrebleMax = clamp(levelReact * max(bassRatio, trebleRatio / 8.), 0.4, 8.0);
    float beatEffect = 1.0 - beat * frequencyReact;

    float frequency = iQuantity;
    float speed = iTime / frequency;
    float d = length(uv);
    float a = atan(uv.y, uv.x);
    uv.x = cos(a) * d * bassTrebleMax;
    uv.y = sin(a) * d * bassTrebleMax;

    d -= speed;
    // Wow1 controls pixelated decomposition
    uv += sin(uv * 1234.567 + speed) * iWow1 * 0.25;
    float bpmReactivityAmount = max(beatEffect, brightness_floor);
    float bri = 0.05 + abs(mod(uv.y + uv.x * frequency * d, uv.x * 2.0)) * bpmReactivityAmount;

    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = bri * mix(iColorRGB, mix(iColor2RGB, iColorRGB, wave(bri)), iWow2);
    fragColor = vec4(col, 1.);
}