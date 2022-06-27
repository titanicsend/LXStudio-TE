//fork of https://www.shadertoy.com/view/WsSXzt

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;

    vec2 gv = uv * {%pixelation[50,1,100]};
    gv = fract(gv) - 0.5;

    float t = iTime * {%speed[5,1,10]};

    float s = (sin(t - length(uv * {%layers[2,2,10]}) * 5.0) * 0.4 + 0.5) * {%width[.6,.6,1.6]};
    float m = smoothstep(s, s - 0.05, length(gv)) + s*2.0;

    vec3 col = vec3(s, 0.0, {%color2[.5,.1,1.5]}) * m;

    fragColor = vec4(col, 1.0);
}
