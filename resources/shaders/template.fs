#version 410

out vec4 finalColor;

uniform float iTime;
uniform vec2 iResolution;
uniform vec4 iMouse;
uniform sampler2D iChannel0;
uniform sampler2D iChannel1;
uniform sampler2D iChannel2;
uniform sampler2D iChannel3;

{{%shader_body%}}

void main() {
    mainImage(finalColor, gl_FragCoord.xy);
}
