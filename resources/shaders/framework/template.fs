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

vec4 _blendFix(vec4 col) {
// if alpha is exactly 1.0, it has probably been forced there, so
// we will fix it to assure proper blending.
    if (col.a == 1.0) {
        col.rgb = clamp(col.rgb,0.0, 1.0);

        col.a = max(col.r,max(col.g, col.b)); // alpha derived from brightness
        col.rgb = col.rgb / col.a; // rgb set to brightest possible value of that color
    }
    return col;
}

void main() {
    // translate according to XPos and YPos controls unless explicitly overriden
    #ifndef TE_NOTRANSLATE
    mainImage(finalColor, gl_FragCoord.xy-(iTranslate * iResolution));
    #else
    mainImage(finalColor, gl_FragCoord.xy);
    #endif

    // Post-processing: Make sure we've got optimal color and alpha values for brightness
    // and blending. define TE_NOALPHAFIX in your shader code if you need
    // to disable this feature.
    #ifndef TE_NOALPHAFIX
    finalColor = _blendFix(finalColor);
    #endif
}
