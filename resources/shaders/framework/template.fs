#version 410

out vec4 finalColor;

// standard shadertoy
uniform float iTime;
uniform vec2 iResolution;
uniform vec4 iMouse;

// TE Audio
uniform float beat;
uniform float sinPhaseBeat;
uniform float bassLevel;
uniform float trebleLevel;

// TE color
uniform vec3 iPalette[5];
uniform vec3 iColorRGB;
uniform vec3 iColorHSB;

// TE common controls
uniform float iSpeed;
uniform float iScale;
uniform float iQuantity;
uniform vec2 iTranslate;
uniform float iSpin;
uniform float iRotationAngle;
uniform float iBrightness;
uniform float iWow1;
uniform float iWow2;
uniform bool iWowTrigger;

// Shadertoy audio channel + optional textures
uniform sampler2D iChannel0;
uniform sampler2D iChannel1;
uniform sampler2D iChannel2;
uniform sampler2D iChannel3;

#define TE_BACKGROUND 0
#define TE_TRANSITION 1
#define TE_PRIMARY 2
#define TE_SECONDARY 3
#define TE_SECONDARY_BACKGROUND 4

{{%shader_body%}}

void main() {
    // translate according to XPos and YPos controls
    // can't really scale here, because 'good' scaling behavior depends a lot
    // on what's being rendered.
    mainImage(finalColor, gl_FragCoord.xy-(iTranslate * iResolution));

    // The brightness control works as "contrast".  It sets the brightness of colors, without
    // affecting transparency.
    finalColor.rgb *= iBrightness;
}
