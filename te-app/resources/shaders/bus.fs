#version 410

out vec4 fragColor;

uniform sampler2D iSrc;

uniform float level;

void main() {
    vec4 color1 = texelFetch(iSrc, ivec2(gl_FragCoord.xy), 0);
    fragColor = vec4(color1.rgb, color1.a * level);
}
