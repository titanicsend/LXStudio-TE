//fork of https://www.shadertoy.com/view/3sXSD2

const float PI = 3.1415926;

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 st = fragCoord.xy / iResolution.xy;
    st = st * 2. - 1.;

    fragColor = vec4(vec3(0.), 1.);
}
