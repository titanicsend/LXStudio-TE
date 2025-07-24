# NDI GPU Integration Project

Review these files and all referenced files:

    - .agent/specs/00_codex.md
    - .agent/specs/01_git.md
    - .agent/specs/02_project_files.md

Then plan for each phase, write a detailed plan for tasks to do, and start doing the tasks one by one. Write updates for each task in front of the task description.

## IMPORTANT: Documentation Standards

**CRITICAL**: Treat this project file as a very well-written design document. When updating this document:

- **Maintain coherence and readability** - This document will serve as the final design doc for the project
- **Write for future readers** - Someone else will read this to understand what happened in the project
- **Document decisions and rationale** - Explain why certain approaches were chosen
- **Update progress in real-time** - Mark task status as work proceeds
- **Preserve implementation history** - Keep completed work visible for reference

## Project Objective

Implement GPU-level NDI (Network Device Interface) integration with the Titanics End LXStudio application, enabling high-performance video streaming directly from GPU-rendered content without CPU bottlenecks.

## Primary Deliverables

- **Enhanced NDI Output Effect**: GPU-accelerated NDI streaming from shader/GL-rendered content
- **Improved NDI Input Pattern**: GPU-optimized reception and texture integration
- **Performance Documentation**: Benchmarks and optimization guidelines
- **Implementation Guide**: Documentation for future GPU NDI development

## Success Criteria

- NDI output streams GPU-rendered content at full resolution (1920x1200+) and target framerate (60fps)
- NDI input receives external streams and integrates seamlessly with GPU rendering pipeline
- Performance improvement over CPU-based implementation (measured latency and throughput)
- Stable operation during extended use without memory leaks or degradation

## Technical Architecture Overview

### Current System Analysis

**GLEngine Framework:**

- **GLMixer**: Complete GPU mixing pipeline with FBO ping-pong rendering
- **TEShader**: Individual shader execution with texture management
- **GLShaderPattern**: GPU pattern base class with sophisticated texture caching
- **Texture Management**: Advanced binding system for model coordinates, audio, and user textures

**NDI Integration Points:**

- **DevolayVideoFrame**: Java NDI bindings for video frame management
- **GPU Framebuffers**: OpenGL texture targets for rendered content
- **Color Arrays**: CPU-based pixel data currently used for NDI transmission

### Target Architecture

**GPU-Direct NDI Pipeline:**

1. **GPU Rendering** → OpenGL texture (no CPU readback)
2. **Texture Sharing** → Direct NDI texture binding or efficient GPU→NDI transfer
3. **Zero-Copy Streaming** → NDI transmission without intermediate CPU buffers

## Phase-Based Implementation Plan

### Phase 1 - NDI System Investigation and Repair

Phase 1 Status: **[COMPLETED]**

Investigate current NDI implementation, identify performance bottlenecks, and ensure basic functionality works reliably before implementing GPU optimizations.

### Task 1.1 - ✅ **COMPLETED** - Merge cursor_support branch

- **Result:** Successfully merged `sinas/cursor_support` into `sinas/ndi_gpu`
- **Conflict Resolution:** Resolved merge conflict in `.run/Grid_1280x240.run.xml`
- **Files Added:** VSCode configuration, shader cache files, Windows batch script

### Task 1.2 - ✅ **COMPLETED** - Analyze existing NDI implementation

- **NDIOutRawEffect Analysis:**
  - CPU-based effect reading from `colors[]` array
  - Manual color format conversion (ARGB reorganization)
  - Direct DevolaySender integration
  - Frame size fixed to GLEngine dimensions
- **TdNdiPattern Analysis:**
  - CPU-based pattern using DevolayReceiver
  - Manual pixel mapping from NDI frames to LXPoint indices
  - Periodic reconnection logic for stability
  - Channel name: "TE_TD_Mapped"

### Task 1.3 - ✅ **COMPLETED** - Add comprehensive logging to NDI output

- **Implementation:** Enhanced `NDIOutRawEffect.java` with detailed logging:
  - Initialization tracking and resolution reporting
  - Frame count and performance metrics (FPS calculation)
  - Color data validation and buffer status monitoring
  - NDI sender state and error reporting
  - Memory usage tracking for the 9.2MB video buffer
- **Build Optimization:**
  - Documented Maven workflow: avoid `mvn clean`, use `mvn compile` for iterations
  - Created comprehensive Maven documentation at `.agent/specs/03_mvn.md`
  - Successful incremental build: 3.4s compile + 1m20s package time

### Task 1.4 - ✅ **COMPLETED** - Launch TE app with GPU support and verify NDI initialization

- **Launch Success:** GPU-enabled TE app launched successfully with parameters:
  ```bash
  java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu \
    -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar \
    --resolution 1920x1200 Projects/BM2024_TE.lxp
  ```
- **Key Observations:**
  - **GPU Rendering:** BGFX renderer: Metal (native macOS GPU acceleration)
  - **NDI Initialization:** `NDIOutRawEffect: Created with resolution 1920x1200, buffer size: 9216000 bytes`
  - **Canvas Size:** GLEngine rendering canvas: 1920x1200 = 2,304,000 total points
  - **Project Load:** BM2024_TE.lxp loaded successfully
- **System Status:**
  - Java 24.0.1 Oracle Corporation running on Mac OS X 15.5 aarch64
  - Chromatik version 1.1.1-TE.3.GPU-SNAPSHOT active
  - Audio input thread started, Art-Net listener active on port 6454

### Task 1.5 - 🔄 **NEXT** - Test NDI output functionality and data flow

**Objective:** Verify the enhanced NDI output actually transmits video data

**Test Plan:**

1. **Enable NDI Effect:** Add NDI Raw Output effect to an active channel
2. **Monitor Logs:** Check for frame transmission logs, FPS metrics, and buffer activity
3. **NDI Discovery:** Use NDI tools to verify the "TE-Output" stream is discoverable
4. **Data Validation:** Confirm color data is being processed and sent (not empty frames)
5. **Performance Check:** Monitor FPS and memory usage under various loads

**Expected Logs to Verify:**

- `NDIOutRawEffect: Sending frame [X], FPS: [Y]`
- `NDIOutRawEffect: Processing [N] colors for transmission`
- `NDIOutRawEffect: Buffer [X]MB, Sender active`

**Tools for Verification:**

- NDI Studio Monitor (for receiving stream)
- NDI Access Manager (for network discovery)
- TE app logs (for transmission confirmation)

## Phase 2 - GPU NDI Integration

Phase 2 Status: **[PLANNED]**

Implement direct GPU-to-NDI streaming bypassing CPU color array conversion.

### Task 2.1 - **PLANNED** - Investigate GPU texture to NDI pathway

**Objective:** Research methods for direct GPU texture sharing with NDI

**Investigation Areas:**

- OpenGL texture binding with Devolay NDI framework
- GPU memory mapping options for zero-copy transfer
- Performance characteristics of different approaches
- Compatibility with BGFX Metal renderer on macOS

### Task 2.2 - **PLANNED** - Prototype GPU texture extraction

**Objective:** Create proof-of-concept for reading GPU textures efficiently

**Implementation Approach:**

- Identify GLMixer output texture
- Implement efficient GPU readback (if necessary)
- Benchmark texture access methods
- Test integration with existing NDI sender

### Task 2.3 - **PLANNED** - Implement GPU-accelerated NDI output

**Objective:** Replace CPU color array processing with direct GPU pipeline

**Technical Requirements:**

- Maintain compatibility with existing effect framework
- Preserve NDI stream quality and format
- Achieve target performance (60fps at 1920x1200)
- Handle error cases gracefully

### Task 2.4 - **PLANNED** - Optimize NDI input for GPU integration

**Objective:** Enhance NDI input pattern to work efficiently with GPU textures

**Implementation Goals:**

- Direct NDI frame to OpenGL texture conversion
- Integration with GLMixer texture pipeline
- Reduced latency and improved performance
- Seamless blend with other GPU patterns

## Risk Assessment and Mitigation

### Technical Risks

- **Risk:** NDI library limitations with direct GPU access
  **Mitigation:** Research alternative approaches, fallback to optimized CPU path
- **Risk:** Platform-specific OpenGL/Metal compatibility issues
  **Mitigation:** Test on target platforms early, maintain cross-platform compatibility

- **Risk:** Performance degradation with texture copying
  **Mitigation:** Benchmark multiple approaches, optimize critical paths

### Integration Risks

- **Risk:** Breaking existing NDI functionality during refactoring
  **Mitigation:** Maintain backward compatibility, comprehensive testing
- **Risk:** Memory management issues with GPU textures
  **Mitigation:** Careful resource cleanup, memory monitoring

## Development Environment

- **Platform:** macOS 15.5 with Metal GPU support
- **Java Runtime:** Oracle Corporation Java 24.0.1, target Java 21
- **Graphics Backend:** BGFX with Metal renderer
- **NDI Library:** Devolay 2.1.0-te (Java bindings)
- **Build System:** Maven 3.9.11 with incremental compilation

## Implementation Notes

### Key Files Modified

- `te-app/src/main/java/titanicsend/effect/NDIOutRawEffect.java` - Enhanced with logging
- `.agent/specs/03_mvn.md` - Maven build documentation
- `.gitignore` - Added `.agent_logs/` directory

### Current System State

- Repository restored from `git@github.com:titanicsend/LXStudio-TE.git`
- Working branch: `sinas/ndi_gpu` with cursor_support merged
- GPU-enabled application launches successfully
- Enhanced NDI logging implemented and ready for testing

### Next Steps

1. **Test NDI Output:** Verify actual video transmission with logging
2. **Investigate GPU Pipeline:** Research direct texture sharing options
3. **Prototype Integration:** Create minimal viable GPU-to-NDI pathway
4. **Performance Testing:** Benchmark and optimize implementation
