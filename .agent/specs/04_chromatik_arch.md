# Chromatik Architecture for Titanics End

## Overview

Titanics End uses **Chromatik** (formerly LX Studio), a professional LED control software framework, as the foundation for controlling 128,000 LEDs across multiple art cars. The system has been extensively customized and enhanced with GPU acceleration, custom shader systems, and specialized hardware integration.

## Core Architecture

### Application Entry Point

- **Main Class**: `heronarts.lx.studio.TEApp`
- **Plugin System**: `TEApp.Plugin` implements `LXStudio.Plugin`
- **Initialization Flow**: GLX Window → Application Thread → LX Engine → Plugin Registration

```
TEApp.main() → GLXWindow → applicationThread() → TEApp(LX) → Plugin.initialize()
```

### Component Hierarchy

```
LX Engine (Root)
├── GLEngine (GPU Rendering)
│   ├── TextureManager
│   └── GLMixer (GPU Mixing Pipeline)
├── Audio Engine
│   ├── GraphicMeter (FFT Analysis)
│   └── AudioStems (Stem Separation)
├── DMX Engine (DMX512 Control)
├── NDI Engine (Network Video)
├── Mixer Engine (LXMixerEngine)
│   ├── Channels (LXChannel)
│   ├── Groups (LXGroup)
│   └── Master Bus (LXMasterBus)
├── Model System
│   ├── TEWholeModelDynamic
│   ├── Edge/Panel/Vertex Models
│   └── DMX Models
└── Output System
    ├── Art-Net
    ├── ChromatechSocket
    └── GigglePixel
```

## Rendering Architecture

### Dual-Mode Rendering

The system supports both **CPU** and **GPU** rendering modes:

- **CPU Mode**: Traditional LX patterns with software rendering
- **GPU Mode**: OpenGL/Metal shader-based patterns with hardware acceleration

**Mode Selection**:

- Startup flag: `-Dgpu` enables GPU mode
- Runtime property: `lx.engine.renderMode.gpu`

### GPU Rendering Pipeline

#### GLEngine Core

- **Resolution**: Configurable (default 1920x1200 = 2.3M points)
- **Backend**: BGFX with Metal on macOS, DirectX on Windows, OpenGL on Linux
- **Context**: Offscreen OpenGL context for headless rendering

#### GLMixer System

```
GLMixer.postMix()
├── Channel Processing
│   ├── Pattern Rendering (GLShaderPattern)
│   ├── Effect Processing (GLShaderEffect)
│   └── Texture Chaining
├── Bus Blending
│   ├── Alpha Compositing
│   ├── Blend Modes
│   └── Level Control
└── Final Output
    ├── CPU Buffer Copy
    └── Main Mix Generation
```

#### Texture Management

- **TextureManager**: Centralized texture caching and loading
- **Audio Textures**: Real-time FFT data (512x2 float texture)
- **Model Coordinates**: 3D LED positions as GPU textures
- **User Textures**: Image files for shader input

## Pattern and Effect System

### Pattern Types

1. **Classic Patterns**: CPU-based (`TEPerformancePattern`)
2. **Shader Patterns**: GPU-based (`GLShaderPattern`)
3. **Auto-Generated**: Dynamic class creation from `.fs` files
4. **NDI Patterns**: Network video input patterns

### Effect System

1. **CPU Effects**: Traditional color processing
2. **GPU Effects**: Shader-based post-processing (`GLShaderEffect`)
3. **Director Effects**: Global control and filtering

### Pattern Registration

```java
// Manual Registration
lx.registry.addPattern(MyPattern.class);

// Auto-Registration (shader files)
ShaderPatternClassFactory.registerShaders(lx);
```

## Audio System

### Audio Processing Pipeline

```
Audio Input → GraphicMeter → FFT Analysis → Texture Upload
                           ↓
Audio Stems ← OSC Input ← External Stem Splitter
```

### Audio Data Sources

- **GraphicMeter**: Real-time FFT analysis (16 bands default)
- **AudioStems**: Separated audio stems (bass, drums, vocals, other)
- **Beat Detection**: Tempo and phase analysis
- **Level Analysis**: Volume, bass, treble ratios

### Audio Textures

- **Size**: 512x2 pixels (float format)
- **Row 0**: FFT frequency data
- **Row 1**: Waveform time-domain data
- **Update**: Every frame, uploaded to GPU

## Model System

### Model Hierarchy

```
TEWholeModelDynamic (Root Model)
├── Edge Models (TEEdgeModel)
│   ├── Individual LEDs
│   └── Geometric Properties
├── Panel Models (TEPanelModel)
│   ├── 2D LED Arrays
│   └── Panel Geometry
├── Vertex Models (TEVertex)
│   ├── Connection Points
│   └── 3D Coordinates
└── DMX Models (DmxModel)
    ├── Moving Lights
    └── Traditional Fixtures
```

### Model Loading

- **Fixture Files**: `.lxf` files define LED layouts
- **Dynamic Loading**: Runtime model changes supported
- **Coordinate Mapping**: 3D positions → 2D texture coordinates

## Control and UI System

### Control Framework

- **Common Controls**: Standardized pattern parameters (Speed, Scale, etc.)
- **LX Parameters**: Reactive parameter system with listeners
- **OSC Integration**: External control via Open Sound Control
- **MIDI Support**: Hardware controller integration

### 3D Visualization

- **UI3DManager**: Manages 3D scene components
- **Model Labels**: Dynamic text rendering in 3D space
- **Performance Mode**: Dual-context rendering for live shows

### Director System

- **Global Control**: Master dimming and filtering
- **Tag-Based Filtering**: Separate control of edges, panels, FOH
- **DMX Integration**: Beacon and moving light control

## Hardware Integration

### Output Systems

1. **Art-Net**: Standard lighting protocol (DMX over Ethernet)
2. **ChromatechSocket**: Custom high-speed LED protocol
3. **GigglePixel**: PixelBlaze integration for secondary controllers

### Input Systems

1. **NDI**: Network Device Interface for video input/output
2. **OSC**: Open Sound Control for external software
3. **MIDI**: Hardware controller input
4. **Gamepad**: Game controller support

## Performance Characteristics

### GPU Performance

- **Target**: 60fps at 1920x1200 resolution
- **Parallelization**: GPU shader execution across thousands of cores
- **Memory**: ~9MB frame buffers for high-resolution content

### CPU Performance

- **Pattern Loop**: ~16.67ms target (60fps)
- **Audio Analysis**: Real-time FFT processing
- **Network I/O**: Asynchronous output to LED controllers

## Build and Deployment

### Maven Structure

```
titanicsend-parent/
├── te-app/ (Main Application)
│   ├── src/main/java/titanicsend/
│   └── resources/shaders/
└── audio-stems/ (Audio Plugin)
    └── src/main/java/titanicsend/audio/
```

### Runtime Configuration

- **Java Version**: Target Java 21, Runtime Java 24
- **JVM Args**: `-ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu`
- **Resolution**: `--resolution 1920x1200`
- **Project Loading**: `.lxp` project files

## Key Innovations

### GPU-Accelerated Mixing

- **Zero-Copy Pipeline**: Direct GPU texture operations
- **Parallel Effects**: Multiple shader effects in GPU pipeline
- **Real-time Performance**: 60fps with complex visual effects

### Dynamic Class Generation

- **ByteBuddy Integration**: Runtime pattern class creation
- **Shader-Driven Patterns**: Automatic UI generation from shader code
- **Hot-Loading**: Runtime shader compilation and loading

### Hybrid Architecture

- **CPU/GPU Coexistence**: Seamless mixing of CPU and GPU patterns
- **Legacy Compatibility**: Existing patterns work unchanged
- **Performance Scaling**: Automatic workload distribution

This architecture enables Titanics End to achieve unprecedented visual complexity while maintaining real-time performance for live events with 128,000+ LEDs.
