// FACE MELT - a smiley face pattern with two-tone color glitch animation best viewed in deep playa.
//

#pragma name "FaceMelt"

#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable
#pragma TEControl.WOWTRIGGER.Disable

#include <include/colorspace.fs>

const float PI = asin(1.) * 2.;
const float TAU = PI * 2.0;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

// smiley pattern value at grid position
float getSmileyValue(vec2 gridPos) {
    int x = int(gridPos.x);
    int y = int(gridPos.y);
    if (x < 0 || x >= 8 || y < 0 || y >= 8) return 0.0;
    
    // eyes - 2 squares at row 2
    if (y == 2) {
        if (x == 2) return 1.0; // left eye
        if (x == 5) return 2.0; // right eye
    }
    // smile - 6 squares
    else if (y == 5) {
        // base of smile - 4 squares across
        if (x >= 2 && x <= 5) return 3.0;
    }
    else if (y == 4) {
        // corner squares - UP one grid position for smile
        if (x == 1) return 4.0; // left corner up
        if (x == 6) return 5.0; // right corner up
    }
    return 0.0;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 coord = fragCoord / iResolution.xy;
    
    float t = iTime * iSpeed;
    float beatPulse = beat * iWow1;
    
    // fast glitch - change every 2 frames
    float frameRate = 35.0;
    float frameCount = floor(t * frameRate);
    bool isEvenFrame = mod(frameCount, 4.0) < 2.0;
    
    vec3 colorA = iColorRGB;
    vec3 colorB = iColor2RGB;
    
    // inverting/shift hues to make more different
    vec3 colorAGlitch = mix(colorA, vec3(1.0) - colorA, 0.7);
    vec3 colorBGlitch = mix(colorB, vec3(1.0) - colorB, 0.7);
    
    vec3 currentColorA = isEvenFrame ? colorA : colorBGlitch;
    vec3 currentColorB = isEvenFrame ? colorB : colorAGlitch;
    
    // randomly appear and fade out
    float numSmileys = floor(iQuantity * 2.5); // 2.5x multiplier for more smileys
    float baseSmileySize = 0.08;

    vec3 totalColor = vec3(0.0);
    float totalAlpha = 0.0;
    
    // update each smiley instance
    for (float i = 0.0; i < 90.0; i++) {
        if (i >= numSmileys) break;
        
        vec2 smileySeed = vec2(i * 1.618, i * 2.236);
        float spawnOffset = hash(smileySeed) * 3.0;
        float lifeTime = mod(t * 0.5 + spawnOffset, 3.0); // 3 second life cycle
        
        if (lifeTime < 2.5) {
            vec2 gridCell = vec2(
                floor(hash(smileySeed + vec2(1.0, 0.0)) * 6.0) / 6.0,
                floor(hash(smileySeed + vec2(0.0, 1.0)) * 6.0) / 6.0
            );
            vec2 cellOffset = vec2(
                hash(smileySeed + vec2(2.0, 0.0)) * 0.15 - 0.075,
                hash(smileySeed + vec2(0.0, 2.0)) * 0.15 - 0.075
            );
            vec2 smileyPos = gridCell + vec2(0.083) + cellOffset;
            
            // start small, scale up
            float scaleProgress = lifeTime / 2.5;
            float scale = 1.0 + scaleProgress * 3.0; // Scale from 1x to 4x
            float currentSize = baseSmileySize * scale;
            
            // fade out as it scales up
            float alpha = 1.0 - smoothstep(0.5, 2.5, lifeTime);
            
            vec2 toSmiley = coord - smileyPos;
            if (abs(toSmiley.x) < currentSize * 0.5 && abs(toSmiley.y) < currentSize * 0.5) {
                // Y flipped
                vec2 smileyUV = (toSmiley / currentSize) + 0.5;
                smileyUV.y = 1.0 - smileyUV.y; // Flip Y coordinate
                vec2 smileyGrid = floor(smileyUV * 8.0);
                vec2 smileyFract = fract(smileyUV * 8.0);
                
                float smileyValue = getSmileyValue(smileyGrid);
                
                if (smileyValue > 0.0) {
                    float strokeWidth = 0.05;
                    float shrinkFactor = 0.69;
                    
                    vec2 squareMin = vec2(strokeWidth * 0.5 + shrinkFactor * 0.1);
                    vec2 squareMax = vec2(1.0 - strokeWidth * 0.5 - shrinkFactor * 0.1);
                    
                    // check if we're inside the square
                    float shape = 0.0;
                    if (smileyFract.x > squareMin.x && smileyFract.x < squareMax.x &&
                        smileyFract.y > squareMin.y && smileyFract.y < squareMax.y) {
                        shape = 1.0;
                    }
                    
                    // pick color using gradient for vibrancy
                    vec3 pixelColor;
                    float gradientPos;
                    
//                    if (smileyValue <= 2.0) {
                        // eyes
                        gradientPos = fract(i * 0.123 + t * 0.1);
                        pixelColor = getGradientColor(gradientPos);
///                    } else {
                        // Mouth - use different gradient position
//                        gradientPos = fract(i * 0.234 + t * 0.15 + 0.5);
//                        pixelColor = getGradientColor(gradientPos);
//                    }
                    
                    // apply glitch effect to colors
                    if (!isEvenFrame) {
                        pixelColor = mix(pixelColor, vec3(1.0) - pixelColor, 0.7);
                    }
                    
                    float brightness = shape * alpha;
                    brightness *= (1.0 + beatPulse * 0.5);
                    brightness = max(brightness, shape * 0.3);
                    
                    totalColor += pixelColor * brightness;
                    totalAlpha = max(totalAlpha, brightness);
                }
            }
        }
    }
    
    float finalBrightness = iBrightness * (1.0 + beat * iWow1 * 0.3);
    
    // apply brightness
    totalColor *= finalBrightness;
    totalAlpha = min(totalAlpha * finalBrightness, 1.0);
    
    // brightness boost
    float temp = iWow2;
    vec3 warmTint = vec3(1.2, 1.0, 0.8);
    totalColor = mix(totalColor, totalColor * warmTint, temp);
    totalColor = max(totalColor, vec3(totalAlpha * 0.1));

    fragColor = vec4(totalColor, totalAlpha);
}
