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

vec4 _blendFix(vec4 col) {
  /*
     if alpha is exactly 1.0, it has probably been deliberately clamped there, so
     we will fix it to improve blending by making a 1:1 substitution
     of brightness for alpha.

     Note that while this works perfectly for colors on a black
     background, the blending process may reduce the visibility of dimmer colors
     on more complex backgrounds.

     If this presents a problem for a particular shader, define TE_ALPHATHRESHOLD in the
     shader to set a brightness threshold above which colors will be fully opaque (alpha == 1.0).
     Colors below this threshold will be blended with brightness-derived alpha as usual.
   */
    if (col.a == 1.0) {
        col.rgb = clamp(col.rgb,0.0, 1.0);

        // use the maximum color value as alpha.
        col.a = max(col.r,max(col.g, col.b));

        #ifdef TE_ALPHATHRESHOLD
        // make colors with brightness above the threshold fully opaque
        if (col.a >= TE_ALPHATHRESHOLD) col.a = 1.0;
        #endif

        // use alpha value to set the brightness of the rgb components
        // if alpha is 1.0, brightness will not be changed, otherwise, the
        // color will become the brightest possible version of itself.
        col.rgb = col.rgb / col.a;
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
    #else
    // Old EDC blending - force black pixels to full transparency, otherwise
    //use shader provided alpha
    finalColor.a = ((finalColor.r + finalColor.g + finalColor.b) == 0.0) ? 0.0 : finalColor.a;
    #endif
}
