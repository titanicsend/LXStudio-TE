// Happy Chibi - bouncing kawaii smiley face with animated highlights
// for the deep playa!!!

#include <include/colorspace.fs>

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

float drawChibiFace(vec2 uv, vec2 center, float scale, float scaleX, float yOffset, float time, out vec3 faceColor) {
    // apply Y offset (pop up animation)
    center.y += yOffset;
    
    uv = (uv - center) / scale;
    uv.x /= scaleX;

    float faceRadius = 0.35;
    float faceBounds = smoothstep(faceRadius + 0.01, faceRadius, length(uv));
    
    if (faceBounds <= 0.0) {
        faceColor = vec3(0.0);
        return 0.0;
    }
    
    // start transparent
    float alpha = 0.0;
    vec3 color = vec3(0.0);

    // eye positions
    vec2 leftEye = vec2(-0.15, 0.1);
    vec2 rightEye = vec2(0.15, 0.1);
    float eyeRadius = 0.11;

    // get gradient color
    vec3 gradientColor = getGradientColor(time * 0.1 + center.x + center.y);
    faceColor = gradientColor;
    
    // draw eyes
    vec2 leftEyeUV = uv - leftEye;
    vec2 rightEyeUV = uv - rightEye;
    
    // make eyes elliptical with scaling
    leftEyeUV.x *= 1.5;
    rightEyeUV.x *= 1.5;
    
    float eyeL = smoothstep(eyeRadius, eyeRadius - 0.01, length(leftEyeUV));
    float eyeR = smoothstep(eyeRadius, eyeRadius - 0.01, length(rightEyeUV));
    float eyeMask = max(eyeL, eyeR);
    alpha = max(alpha, eyeMask);
    color += gradientColor * eyeMask;

    // add eye highlights (these could be a little better, but work!)
    vec2 hOffset = vec2(0.04 * sin(time * 2.0), 0.03 * cos(time * 1.5));
    float highlightRadius = 0.06;
    
    // apply same elliptical transform to highlights
    vec2 leftHighlightUV = uv - (leftEye + hOffset);
    vec2 rightHighlightUV = uv - (rightEye + hOffset);
    leftHighlightUV.x *= 1.5;
    rightHighlightUV.x *= 1.5;
    
    float highlightL = smoothstep(highlightRadius, 0.0, length(leftHighlightUV)) * eyeL;
    float highlightR = smoothstep(highlightRadius, 0.0, length(rightHighlightUV)) * eyeR;
    float highlights = highlightL + highlightR;
    color = mix(color, vec3(1.0), highlights);

    // adjust eye highlight (also much larger)
    vec2 lowerHighlightOffset = vec2(-0.04, -0.05);
    float lowerHighlightRadius = 0.04;
    
    vec2 leftLowerUV = uv - (leftEye + lowerHighlightOffset);
    vec2 rightLowerUV = uv - (rightEye + lowerHighlightOffset);
    leftLowerUV.x *= 1.5;
    rightLowerUV.x *= 1.5;
    
    float lowerL = smoothstep(lowerHighlightRadius, 0.0, length(leftLowerUV)) * eyeL;
    float lowerR = smoothstep(lowerHighlightRadius, 0.0, length(rightLowerUV)) * eyeR;
    float lowerHighlights = (lowerL + lowerR) * 0.5;
    color = mix(color, vec3(0.9), lowerHighlights);

    // add mouth (solid half circle)
    vec2 mouthPos = vec2(0.0, -0.15);
    vec2 mUV = uv - mouthPos;
    float mouthOuter = smoothstep(0.07, 0.06, length(mUV)) * step(mUV.y, 0.0);
    alpha = max(alpha, mouthOuter);
    color += gradientColor * mouthOuter;
    
    // tongue (pink area inside mouth)
    float tongue = smoothstep(0.035, 0.03, length(mUV)) * step(mUV.y, 0.0);
    vec3 tongueColor = mix(gradientColor, vec3(1.0, 0.6, 0.7), 0.7);
    color = mix(color, tongueColor, tongue * mouthOuter);
    
    faceColor = color;
    return alpha;
}
    
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv = (uv - 0.5) * vec2(iResolution.x / iResolution.y, 1.0) + 0.5;

    float t = iTime * iSpeed;
    
    vec3 totalColor = vec3(0.0);
    float totalAlpha = 0.0;
    
    float numFaces = floor(iQuantity);
    
    for (float i = 0.0; i < 40.0; i++) {
        if (i >= numFaces) break;
        
        vec2 seed = vec2(i * 1.618, i * 2.236);
        vec2 pos = hash2(seed);
        
        float animSpeed = 2.0 + hash(seed + vec2(0.0, 1.0)) * 2.0;
        float animPhaseX = hash(seed + vec2(1.0, 0.0)) * 6.28318;
        float animPhaseY = hash(seed + vec2(2.0, 0.0)) * 6.28318;
        
        float scaleX = mix(0.5, 1.5, 0.5 + 0.5 * sin(t * animSpeed + animPhaseX));
        
        float yOffset = abs(sin(t * animSpeed * 0.8 + animPhaseY)) * 0.15;
        
        float faceScale = 0.25 * iScale;
        
        vec3 faceColor;
        float faceAlpha = drawChibiFace(uv, pos, faceScale, scaleX, yOffset, t + i * 0.1, faceColor);
        
        totalColor += faceColor;
        totalAlpha = max(totalAlpha, faceAlpha);
    }
    
    float beatPulse = beat * iWow1;
    float finalBrightness = iBrightness * (1.0 + beatPulse * 0.2);
    
    if (iWow2 > 0.0) {
        vec3 tintColor = getGradientColor(t * 0.2);
        totalColor = mix(totalColor, totalColor * tintColor, iWow2);
    }
    
    totalColor *= finalBrightness;
    totalAlpha = clamp(totalAlpha * finalBrightness, 0.0, 1.0);

    fragColor = vec4(totalColor, totalAlpha);
}
