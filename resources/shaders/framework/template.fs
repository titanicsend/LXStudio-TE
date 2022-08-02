#version 410

out vec4 finalColor;

uniform float iTime;
uniform vec2 iResolution;
uniform vec4 iMouse;
uniform vec3 iColorRGB;
uniform vec3 iPalette[5];
uniform sampler2D iChannel0;
uniform sampler2D iChannel1;
uniform sampler2D iChannel2;
uniform sampler2D iChannel3;

uniform float beat;
uniform float sinPhaseBeat;
uniform float bassLevel;
uniform float trebleLevel;

#define TE_EDGE 0
#define TE_SECONDARY 1
#define TE_PANEL 2
#define TE_EDGE_BG 3
#define TE_PANEL_BG 4

{{%shader_body%}}

void main() {
    mainImage(finalColor, gl_FragCoord.xy);
}
