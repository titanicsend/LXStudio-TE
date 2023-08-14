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

uniform float volumeRatio;
uniform float bassRatio;
uniform float trebleRatio;

// TE Colors
uniform vec3 iColorRGB;
uniform vec3 iColorHSB;
uniform vec3 iColor2RGB;
uniform vec3 iColor2HSB;

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

{{%shader_body%}}

void main() {
    // translate according to XPos and YPos controls unless explicitly overriden
    #ifndef TE_NOTRANSLATE
    mainImage(finalColor, gl_FragCoord.xy-(iTranslate * iResolution));
    #else
    mainImage(finalColor, gl_FragCoord.xy);
    #endif

    // force black pixels to full transparency, otherwise use shader provided alpha
    finalColor.a = ((finalColor.r + finalColor.g + finalColor.b) == 0.0) ? 0.0 : finalColor.a;
}
