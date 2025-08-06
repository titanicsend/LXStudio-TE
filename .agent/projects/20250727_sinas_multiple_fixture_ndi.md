# Multiple Fixture NDI Integration Project (Abandoned)

Review these specifications and all referenced documentation:

    - .agent/specs/00_codex.md
    - .agent/specs/01_git.md
    - .agent/specs/02_project_files.md
    - .agent/specs/03_mvn.md
    - .agent/specs/04_chromatik_arch.md

Create detailed implementation plan for each phase, develop comprehensive task lists, and execute tasks systematically. Document progress updates directly alongside task descriptions.

## CRITICAL: Documentation Quality Standards

**ESSENTIAL**: Handle this project document as a professional design specification. When maintaining this document:

- **Ensure clarity and organization** - This document serves as the authoritative design record
- **Consider future maintainers** - Others will reference this to understand project implementation
- **Explain technical decisions** - Document rationale behind architectural choices
- **Track progress systematically** - Update task status as work proceeds
- **Preserve implementation history** - Keep record of completed work for reference

## Implementation Context and Scope

**Project Objective:** Enhance the NDI Out Shader Effect to support flexible fixture selection and output management for high-resolution NDI streaming with multiple fixture configurations.

**Primary Deliverables:**

- **Updated NDI Out Shader Effect**: Enhanced UI with fixture selection capabilities
- **New Chromatik Project**: `playalchemist_bm_2025.lxp` with both TE and highres grid fixtures
- **Fixture Selection System**: Either unified output or selective fixture streaming
- **Resolume Integration Preparation**: Framework for parameter streaming (future work)

**Success Criteria:**

- NDI Out Shader Effect provides configurable fixture selection via dropdown UI
- New project file successfully loads with multiple fixture types (TE + highres grid)
- User can either stream all fixtures combined or select specific fixtures for NDI output
- System maintains performance standards established in previous NDI work
- Clean integration with existing NDI architecture (NDIOutFixture, NDIOutShader)

## Technical Architecture Overview

### Current System Analysis

**NDI Out Shader Effect Architecture:**

- **NDIOutFixture Detection**: Automatic discovery of NDIOutFixture instances from LX structure
- **Output Management**: Each fixture gets corresponding NDIOutShader instance
- **Dynamic Lifecycle**: Automatic creation/disposal when fixtures added/removed
- **GPU Integration**: Full GPU pipeline support with texture handling

**Key Components:**

- **NDIOutShaderEffect**: Main effect class implementing GpuDevice and UIDeviceControls
- **Output Class**: Internal wrapper for NDIOutFixture + NDIOutShader pairs
- **NDIOutShader**: GPU-accelerated NDI streaming per fixture
- **NDIOutFixture**: Fixture definition with dimensions and stream parameters

### Target Architecture Enhancement

**Fixture Selection System:**

1. **Selection Mode Parameter**: Dropdown to choose between "All Fixtures" vs "Selective Fixtures"
2. **Fixture Selector**: Multi-select capability for choosing specific fixtures when in selective mode
3. **Output Consolidation**: Single NDI stream combining selected fixtures or individual streams per fixture
4. **UI Enhancement**: Clear visual indication of selected fixtures and streaming status

**UI Design Pattern (following NDIReceiverPattern):**

- **DiscreteParameter** for selection mode (All/Selective)
- **LXListenableNormalizedParameter** for fixture selection
- **UIDropMenu** for mode selection
- **UIButton** array or list for fixture selection when in selective mode

## Phase-Based Implementation Plan

### Phase 1 - Repository Synchronization and Project Setup

Phase 1 Status: **[PENDING]**

Merge latest main branch and create the new Chromatik project file for testing multiple fixture configurations.

#### Task 1.1 - **PENDING** - Merge latest main and resolve conflicts

**Objective:** Synchronize with main branch to incorporate Justin's latest NDI implementations
**Implementation Notes:**

- Use `git fetch origin main` and `git merge main`
- Resolve any conflicts prioritizing main branch changes
- Ensure NDIOutShaderEffect.java is preserved but updated if needed

#### Task 1.2 - **PENDING** - Create playalchemist_bm_2025.lxp project file

**Objective:** Create new Chromatik project based on BM2024_TE.lxp for testing multiple fixtures
**Implementation Notes:**

- Copy `te-app/Projects/BM2024_TE.lxp` to `te-app/Projects/playalchemist_bm_2025.lxp`
- Verify project loads correctly with existing TE fixtures

#### Task 1.3 - **PENDING** - Launch and configure highres grid fixture

**Objective:** Add highres grid fixture via UI and save project with both fixture types
**Implementation Notes:**

- Launch TE app with new project file
- Add highres grid fixture through UI
- Save project to preserve both TE and grid fixture configurations
- Document fixture IDs and configurations

### Phase 2 - NDI Out Shader Effect Enhancement

Phase 2 Status: **[PLANNED]**

Implement fixture selection system and UI enhancements for the NDI Out Shader Effect.

#### Task 2.1 - **PLANNED** - Analyze current NDI Out Shader Effect implementation

**Objective:** Understand existing fixture detection and output management system
**Prerequisites:** Phase 1 completion with working multi-fixture project

#### Task 2.2 - **PLANNED** - Design fixture selection parameter system

**Objective:** Create parameter architecture for fixture selection modes
**Implementation Approach:**

- Add DiscreteParameter for selection mode ("All Fixtures", "Selective Fixtures")
- Add fixture selector parameter for multi-select capability
- Study NDIReceiverPattern implementation for UI patterns

#### Task 2.3 - **PLANNED** - Implement fixture selection logic

**Objective:** Add filtering logic to control which fixtures stream to NDI
**Technical Requirements:**

- Maintain existing automatic fixture detection
- Add selective filtering based on user parameters
- Preserve individual NDI stream naming/identification

#### Task 2.4 - **PLANNED** - Enhance UI controls for fixture selection

**Objective:** Create intuitive UI for fixture selection management
**Implementation Goals:**

- Dropdown for selection mode
- Multi-select interface for fixtures when in selective mode
- Clear visual indication of active/streaming fixtures
- Follow existing UI patterns from NDIReceiverPattern

### Phase 3 - Testing and Integration Validation

Phase 3 Status: **[PLANNED]**

Comprehensive testing of multi-fixture NDI streaming with different selection modes.

#### Task 3.1 - **PLANNED** - Test unified output mode (All Fixtures)

**Objective:** Validate that all fixtures can stream combined to single NDI output
**Prerequisites:** Phase 2 implementation complete

#### Task 3.2 - **PLANNED** - Test selective fixture streaming

**Objective:** Verify selective fixture streaming works correctly
**Testing Scope:**

- Individual fixture selection
- Multiple fixture selection
- Dynamic fixture addition/removal

#### Task 3.3 - **PLANNED** - Performance validation and optimization

**Objective:** Ensure performance meets established NDI streaming standards
**Validation Criteria:**

- No degradation compared to single-fixture streaming
- Memory usage within acceptable bounds
- Frame rate maintenance under load

### Phase 4 - Future Integration Preparation

Phase 4 Status: **[PLANNED]**

Prepare framework for Resolume parameter integration and document extension points.

#### Task 4.1 - **PLANNED** - Add Resolume parameter streaming TODO framework

**Objective:** Create placeholder structure for future Resolume integration
**Implementation Notes:**

- Add TODO comments with detailed specifications
- Document parameter passing architecture requirements
- Identify integration points for OSC/network parameter streaming

## Technical Architecture and Design

### System Overview

The enhanced NDI Out Shader Effect will maintain the existing automatic fixture detection while adding user-configurable output modes. The system will support both unified streaming (all fixtures combined) and selective streaming (user-chosen fixtures only).

### Key Components

- **Enhanced NDIOutShaderEffect**: Main effect with fixture selection parameters and UI
- **Selection Mode System**: Parameter-driven control over which fixtures stream
- **Dynamic Output Management**: Runtime filtering of fixture outputs based on selection
- **UI Enhancement Layer**: User interface for intuitive fixture selection management

### Design Decisions

**Decision:** Maintain automatic fixture detection while adding selective filtering
**Rationale:** Preserves existing functionality while adding new capabilities without breaking changes
**Alternatives:** Complete rewrite with manual fixture registration
**Impact:** Zero breaking changes to existing NDI workflows

**Decision:** Use parameter-driven selection system following NDIReceiverPattern
**Rationale:** Consistent with existing codebase patterns and proven UI approach
**Alternatives:** Custom UI components or external configuration
**Impact:** Familiar user experience and maintainable code

## Implementation Notes and Progress

### Development Environment

- **Platform:** macOS 15.5 with Metal GPU support, Java 24.0.1 targeting Java 21
- **Dependencies:** BGFX Metal renderer, Devolay NDI library, Maven build system
- **Build Process:** `mvn package -DskipTests` for compilation, incremental builds supported

### Current System State

- **Repository:** LXStudio-TE on branch `sinas/ndi_gpu`
- **NDI Implementation:** Justin's new NDI patterns and effects are active
- **Previous Work:** GPU NDI integration completed and working in previous project

### Modifications and Enhancements

_Track changes made during implementation_

## Risk Assessment and Mitigation

### Technical Risks

- **Risk:** Parameter system complexity affecting performance
  **Mitigation:** Use efficient filtering logic, benchmark against single-fixture performance

- **Risk:** UI complexity overwhelming users
  **Mitigation:** Follow existing UI patterns, provide clear defaults and documentation

### Integration Risks

- **Risk:** Breaking existing NDI Out Shader Effect functionality
  **Mitigation:** Maintain backward compatibility, add features as opt-in enhancements

- **Risk:** Conflict with main branch NDI updates
  **Mitigation:** Merge main early and regularly, coordinate with Justin's NDI work

## Testing and Validation

### Test Strategy

1. **Functional Testing**: Verify all selection modes work correctly
2. **Performance Testing**: Compare with baseline single-fixture performance
3. **Integration Testing**: Test with multiple fixture types and configurations
4. **UI Testing**: Validate user experience and parameter interaction

### Acceptance Criteria

- All fixture selection modes functional without performance degradation
- UI provides clear indication of streaming status for each fixture
- System handles dynamic fixture addition/removal gracefully
- NDI streams maintain quality and performance standards

## Future Considerations

### Extension Points

- **Resolume Parameter Integration**: OSC/network parameter streaming architecture
- **Advanced Fixture Grouping**: User-defined fixture groups for complex shows
- **Performance Monitoring**: Real-time NDI streaming statistics and health monitoring

### Maintenance Requirements

- **Regular Testing**: Validate against new fixture types and configurations
- **Performance Monitoring**: Track NDI streaming performance as system scales
- **Documentation Updates**: Maintain user guides for new fixture selection features

## Resource References

### Documentation

- **NDI SDK**: Network Device Interface technical specifications
- **LX Framework**: Pattern, effect, and parameter system documentation
- **BGFX**: GPU rendering pipeline and texture management

### Related Projects

- **Previous NDI Work**: `.agent/projects/20250724_sinas_make_ndi_work_with_gpu_mixer.md`
- **Chromatik Architecture**: `.agent/specs/04_chromatik_arch.md`
- **Justin's NDI Implementation**: Current NDI patterns and effects in main branch
