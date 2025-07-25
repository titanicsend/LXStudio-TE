# TE Shader System Architecture

## Overview

The Titanics End shader system provides GPU-accelerated pattern rendering using GLSL fragment shaders. The system handles shader compilation, caching, preprocessing, and execution with a sophisticated pipeline that supports both manual and automatic pattern generation.

## Shader Execution Pipeline

### High-Level Execution Flow

```
Shader File (.fs) → Preprocessor → Compilation → Caching → Runtime Execution
      ↓                ↓             ↓           ↓            ↓
  Parse Pragmas → Inject Template → Link Program → Save Binary → GPU Render
```

### Detailed Execution Steps

1. **File Loading**: GLSL files loaded from `resources/shaders/*.fs`
2. **Preprocessing**: GLPreprocessor handles includes, pragmas, and template injection
3. **Compilation**: OpenGL compilation with error checking and validation
4. **Caching**: Binary shader programs cached to disk for faster loading
5. **Runtime**: Shader execution with uniform updates and texture binding

## Shader File Structure

### Basic Shader Anatomy

```glsl
// Optional: Shader metadata and configuration
#pragma name "MyPattern"
#pragma TEControl.SPEED.Range(1.0, 0.1, 5.0)

// Optional: Include files for shared code
#include <include/colorspace.fs>

// Main shader function (required)
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Shader logic here
    vec2 uv = fragCoord / iResolution;
    fragColor = vec4(iColorRGB * sin(iTime), 1.0);
}
```

### Framework Template Integration

Every shader is automatically wrapped with the framework template (`resources/shaders/framework/template.fs`):

```glsl
#version 410
// Standard uniforms and functions
// ... (full template content)

#line 1
{{%shader_body%}}  // User shader code injected here

// Main function that calls user's mainImage()
void main() {
    vec2 coords = _getModelCoordinates().xy;
    if (isnan(coords.r)) {
        finalColor = vec4(0.0);
        return;
    }
    coords *= iResolution;
    mainImage(finalColor, coords - (iTranslate * iResolution));
    finalColor = _blendFix(finalColor);
}
```

## Preprocessing System

### GLPreprocessor Components

The `GLPreprocessor` class handles multiple preprocessing phases:

#### 1. Include Expansion

```glsl
#include <include/colorspace.fs>
#include "resources/shaders/framework/noise.fs"
```

- **Angle Brackets**: Relative to shader resource path
- **Quotes**: Absolute file paths
- **Nesting**: Up to 10 levels of nested includes
- **Line Tracking**: Maintains line numbers for error reporting

#### 2. Pragma Processing

```glsl
// Pattern metadata
#pragma name "FireEffect"
#pragma LXCategory("Fire")

// Control configuration
#pragma TEControl.SPEED.Range(1.0, 0.1, 5.0)
#pragma TEControl.SCALE.Value(2.0)
#pragma TEControl.WOW1.Disable

// Texture channels
#pragma iChannel1 "resources/shaders/textures/noise.png"

// Translation mode
#pragma TEControl.TranslateMode.DRIFT
```

#### 3. VSCode Shadertoy Support

```glsl
// Alternative syntax for VSCode Shadertoy extension
#iUniform float iSpeed=1.0 in{0.1,5.0}
#iUniform vec3 iColorRGB=vec3(1.0,0.0,0.5)
#iUniform bool iWowTrigger=false
```

#### 4. Legacy Parameter Support

```glsl
// Old-style parameter placeholders
float speed = {%speed[1.0,0.1,5.0]};
bool enabled = {%enabled[bool]};
```

### Preprocessor Output

The preprocessor produces:

- **Processed GLSL**: Complete shader code with template
- **Parameter List**: LX parameters for UI generation
- **Texture Mappings**: Channel assignments for image files
- **Configuration**: Pattern metadata and settings

## Compilation and Caching

### Shader Compilation Process

#### 1. OpenGL Program Creation

```java
int programId = gl4.glCreateProgram();
int vertexShader = createShader(gl4, programId, vertexSource, GL_VERTEX_SHADER);
int fragmentShader = createShader(gl4, programId, fragmentSource, GL_FRAGMENT_SHADER);
gl4.glLinkProgram(programId);
gl4.glValidateProgram(programId);
```

#### 2. Error Handling and Validation

```java
// Compile status checking
gl4.glGetShaderiv(shaderId, GL_COMPILE_STATUS, status);
if (status[0] != GL_TRUE) {
    String errorLog = getShaderInfoLog(gl4, shaderId);
    throw new RuntimeException("Shader compilation failed: " + errorLog);
}
```

#### 3. Binary Caching

```java
// Save compiled program to cache
int[] binaryLength = new int[1];
gl4.glGetProgramiv(programId, GL_PROGRAM_BINARY_LENGTH, binaryLength, 0);
ByteBuffer binary = GLBuffers.newDirectByteBuffer(binaryLength[0]);
gl4.glGetProgramBinary(programId, binaryLength[0], null, format, binary);
Files.write(cacheFile, binary.array());
```

### Cache Management

- **Location**: `resources/shaders/cache/*.bin`
- **Format**: Platform-specific binary shader programs
- **Invalidation**: Timestamp-based recompilation when source changes
- **Platform Support**:
  - **macOS**: Metal shaders (note: binary caching disabled due to macOS limitations)
  - **Windows**: DirectX 11 shaders
  - **Linux**: OpenGL shaders

## Runtime Execution

### Shader Class Hierarchy

```
GLShader (Abstract Base)
├── TEShader (TE-specific extensions)
│   ├── Model coordinate mapping
│   ├── Audio texture binding
│   ├── Texture file loading
│   └── Parameter injection
└── BusShader (GPU mixer utility)
    ├── Texture blending
    ├── Level control
    └── CPU buffer readback
```

### Pattern Integration

#### GLShaderPattern

```java
public class MyShaderPattern extends GLShaderPattern {
    public MyShaderPattern(LX lx) {
        super(lx);
        addShader("my_pattern.fs");
    }
}
```

#### Auto-Generated Patterns

```java
// Runtime class generation using ByteBuddy
Class<?> clazz = new ShaderPatternClassFactory()
    .make("FireEffect", "Fire", "fire.fs", false);
lx.registry.addPattern((Class<? extends LXPattern>) clazz);
```

### Uniform Management

#### Standard TE Uniforms

```glsl
// Time and animation
uniform float iTime;

// Colors from palette system
uniform vec3 iColorRGB;
uniform vec3 iColorHSB;
uniform vec3 iColor2RGB;
uniform vec3 iColor2HSB;

// Common controls
uniform float iSpeed;
uniform float iScale;
uniform float iQuantity;
uniform vec2 iTranslate;
uniform float iRotationAngle;
uniform float iBrightness;
uniform float iWow1;
uniform float iWow2;
uniform bool iWowTrigger;

// Audio reactivity
uniform float bassLevel;
uniform float trebleLevel;
uniform float volumeRatio;
uniform float levelReact;
uniform float frequencyReact;
```

#### Audio Textures

```glsl
uniform sampler2D iChannel0;  // Audio texture (512x2)
// Row 0: FFT frequency data
// Row 1: Waveform time data
vec4 fftData = texture(iChannel0, vec2(frequency, 0.0));
vec4 waveData = texture(iChannel0, vec2(time, 1.0));
```

#### Model Coordinates

```glsl
uniform sampler2D lxModelCoords;  // 3D LED positions
vec4 modelPos = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0);
vec3 ledPosition = modelPos.xyz;
```

### Texture System

#### Texture Types

1. **Audio Texture**: Real-time FFT/waveform data
2. **Model Coordinates**: 3D LED positions
3. **User Textures**: Image files from `resources/shaders/textures/`
4. **Backbuffer**: Previous frame output for feedback effects
5. **Mapped Buffer**: Rectangular texture mapping for special effects

#### Texture Loading and Caching

```java
// TextureManager handles loading and caching
int textureHandle = textureManager.useTexture("noise.png");
gl4.glActiveTexture(GL_TEXTURE0 + textureUnit);
gl4.glBindTexture(GL_TEXTURE_2D, textureHandle);
```

## GPU Mixer Integration

### GLMixer Pipeline

```
GPU Effects Processing:
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Pattern       │    │    Effect        │    │   Bus Shader    │
│   Rendering     │───▶│   Processing     │───▶│   Blending      │
│                 │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
        │                        │                        │
    Texture Out            Texture Chain             CPU Buffer
```

### Ping-Pong Rendering

#### Frame Buffer Objects (FBOs)

```java
// Double-buffered rendering for texture chaining
class PingPongFBO {
    FBO render;  // Current render target
    FBO copy;    // Previous frame / input texture

    void swap() {
        FBO temp = render;
        render = copy;
        copy = temp;
    }
}
```

#### CPU Readback

```java
// Pixel Buffer Objects for efficient GPU→CPU transfer
class PingPongPBO {
    int renderPBO;  // Current frame readback
    int copyPBO;    // Previous frame for CPU access

    // Asynchronous readback pattern
    gl4.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, 0);
    ByteBuffer cpuData = gl4.glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);
}
```

## BGFX Shader System

### Dual Shader Support

The system supports both OpenGL and BGFX shaders:

#### OpenGL Shaders (Main System)

- **Language**: GLSL 4.1
- **Format**: `.fs` fragment shaders
- **Platform**: Cross-platform OpenGL/Metal

#### BGFX Shaders (UI Components)

- **Language**: BGFX Shader Language (.sc files)
- **Compilation**: Platform-specific binaries
- **Usage**: 3D UI text rendering and special effects

### BGFX Compilation Pipeline

```bash
# Compilation for all platforms
./compile.sh fs_font3d vs_font3d

# Platform-specific outputs:
# - dx11/fs_font3d.bin (DirectX 11)
# - metal/fs_font3d.bin (Metal/macOS)
# - glsl/fs_font3d.bin (OpenGL)
```

## Performance Optimizations

### Compilation Optimizations

1. **Binary Caching**: Avoid recompilation of unchanged shaders
2. **Validation Caching**: Skip expensive validation for cached programs
3. **Hot-Loading**: Runtime shader replacement without restart

### Runtime Optimizations

1. **Uniform Batching**: Minimize OpenGL state changes
2. **Texture Binding**: Efficient texture unit management
3. **Double Rendering Prevention**: GPU effects run once per frame
4. **Memory Management**: Proper cleanup of GPU resources

### GPU Memory Management

```java
// Proper resource cleanup
public void dispose() {
    gl4.glDeleteTextures(1, textureHandles, 0);
    gl4.glDeleteBuffers(2, bufferHandles, 0);
    gl4.glDeleteFramebuffers(1, fboHandles, 0);
    gl4.glDeleteProgram(programId);
}
```

## Development Workflow

### Shader Development Process

1. **Create Shader**: Write `.fs` file in `resources/shaders/`
2. **Add Pragmas**: Configure controls and metadata
3. **Test Compilation**: Automatic validation on startup
4. **Runtime Testing**: Live pattern switching and testing
5. **Deployment**: Shader automatically registered as pattern

### Debugging Tools

1. **Compilation Errors**: Full GLSL error reporting with line numbers
2. **Uniform Inspection**: Runtime uniform value debugging
3. **Texture Debugging**: Shader texture content inspection
4. **Performance Monitoring**: Frame timing and GPU usage

### Best Practices

1. **Naming**: Use descriptive shader file names
2. **Documentation**: Comment complex shader algorithms
3. **Performance**: Avoid expensive operations in inner loops
4. **Compatibility**: Test on target hardware platforms
5. **Version Control**: Include shader files in source control

This shader system enables TE to achieve real-time GPU-accelerated visual effects with complex patterns while maintaining 60fps performance for live events.
