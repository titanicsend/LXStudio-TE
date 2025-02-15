# Titanic's End Shader Guide

## Introduction

This guide explains how to write, test, and use shaders in the Titanic's End project. Shaders are small programs that run on the GPU to create dynamic visual effects for the LED fixtures on our vehicles.

## What is a Shader?

A shader is a program that runs on the GPU and determines the color of each pixel (or in our case, LED) in real-time. In the Titanic's End project, shaders are written in GLSL (OpenGL Shading Language) and are used to create dynamic, interactive visual patterns that respond to music and user controls.

## Shader File Structure

Shaders in this project use the `.fs` extension (Fragment Shader) and are stored in the `resources/shaders/` directory. Each shader file should contain:

1. A main function with the signature:
```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord)
```

2. Any helper functions and constants needed by your shader
3. Proper use of the provided uniforms for interaction

## Available Uniforms

Shaders have access to various uniform variables that provide real-time data:

### Time and Resolution
```glsl
uniform float iTime;       // Variable speed time, linked to the speed control
uniform vec2 iResolution;  // Pixel resolution of the drawing surface
```

### Audio Data
```glsl
uniform float beat;            // Sawtooth wave (0-1) synced to beat
uniform float sinPhaseBeat;    // Sinusoidal wave between 0-1 with beat
uniform float bassLevel;       // Low frequency content level
uniform float trebleLevel;     // High frequency content level
uniform float volumeRatio;     // Ratio of current to recent average volume
uniform float bassRatio;       // Bass ratio
uniform float trebleRatio;     // Treble ratio
uniform float levelReact;      // Reactivity to audio level changes
uniform float frequencyReact;  // Reactivity to frequency content
```

### Color Controls
```glsl
uniform vec3 iColorRGB;    // Primary color (RGB, normalized 0-1)
uniform vec3 iColorHSB;    // Primary color (HSB, normalized 0-1)
uniform vec3 iColor2RGB;   // Secondary color (RGB, normalized 0-1)
uniform vec3 iColor2HSB;   // Secondary color (HSB, normalized 0-1)
```

### Pattern Controls
```glsl
uniform float iSpeed;          // Speed control (affects iTime)
uniform float iScale;          // Scale/zoom control
uniform float iQuantity;       // Generic quantity control
uniform vec2  iTranslate;      // X/Y translation
uniform float iSpin;           // Spin control value
uniform float iRotationAngle;  // Computed rotation angle
uniform float iBrightness;     // Brightness control
uniform float iWow1;           // Generic effect control 1
uniform float iWow2;           // Generic effect control 2
uniform bool  iWowTrigger;     // Trigger button state
```

## Writing a Shader

Here's a basic template to start with:

```glsl
// Optional: Add any constants or helper functions here

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    // Normalize pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;
    
    // Create your effect here
    vec3 col = vec3(0.0);
    
    // Example: Use primary color modified by position
    col = iColorRGB * uv.x;
    
    // Output final color (alpha should usually be 1.0)
    fragColor = vec4(col, 1.0);
}
```

## Best Practices

1. **Coordinate Space**
   - Use normalized coordinates (0 to 1) for consistent scaling
   - Consider aspect ratio when needed: `uv.x *= iResolution.x/iResolution.y;`

2. **Performance**
   - Avoid excessive branching (if statements)
   - Minimize expensive operations (pow, exp, log)
   - Use built-in GLSL functions when possible

3. **Audio Reactivity**
   - Use `levelReact` and `frequencyReact` for smooth responses
   - Combine multiple audio inputs for complex effects
   - Consider using `beat` for rhythmic animations

4. **Animation**
   - Use `iTime` for continuous animation
   - Multiply by `iSpeed` for variable speed
   - Use `iRotationAngle` for rotation effects

5. **Color**
   - Always use normalized colors (0-1 range)
   - Consider both RGB and HSB color spaces
   - Use `iBrightness` for final intensity adjustment

## Testing Your Shader

1. Create your shader file in `resources/shaders/`
2. Test with different audio inputs
3. Verify behavior with all control parameters
4. Check performance at different resolutions
5. Test both forward and reverse animation (negative speed)

## Examples

The `resources/shaders/` directory contains many example shaders. Here are some recommended ones to study:

- `electric.fs` - Complex audio-reactive electric effect
- `demo_simple_effect.fs` - Basic starter example
- `waterfall.fs` - Smooth flowing animation
- `neon_heart.fs` - Shape generation example

## Debugging Tips

1. Use solid colors to test specific components
2. Visualize intermediate values using RGB channels
3. Start simple and add complexity gradually
4. Test edge cases (zero values, extremes)
5. Check for undefined behavior

## Common Issues and Solutions

1. **Black Screen**
   - Check fragment color alpha value (should be 1.0)
   - Verify coordinate normalization
   - Check for division by zero

2. **Performance Issues**
   - Reduce complex mathematical operations
   - Minimize texture lookups
   - Avoid nested loops

3. **Visual Artifacts**
   - Check coordinate space calculations
   - Verify color normalization
   - Test at different resolutions

## Resources

1. [The Book of Shaders](https://thebookofshaders.com/) - Excellent shader programming introduction
2. [Shadertoy](https://www.shadertoy.com/) - Online shader editor and examples
3. [GLSL Reference](https://www.khronos.org/registry/OpenGL-Refpages/gl4/) - Official GLSL documentation
4. [Inigo Quilez's Articles](https://iquilezles.org/articles/) - Advanced shader techniques

## Contributing

1. Follow the existing naming convention
2. Document your shader with comments
3. Include example values for controls
4. Test thoroughly before submitting
5. Update this README if adding new techniques 