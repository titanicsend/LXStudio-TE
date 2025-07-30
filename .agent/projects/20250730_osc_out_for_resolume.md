# Resolume OSC Output Integration

## Project Overview

Create a Resolume Sync feature that enables OSC message forwarding from Chromatik/TE to Resolume, similar to the existing Laser Sync functionality.

## Objective

Implement a **Resolume Sync** button that:

1. Enables OSC message mapping from TE to Resolume
2. Uses a YAML configuration file for flexible setup
3. Supports dynamic IP/port configuration
4. Provides real-time OSC message forwarding

## Current Status

The OSC filtering is now working correctly, but the OSC path renaming is not functioning as expected. The original OSC path is being sent instead of the remapped path from the `sync-config.yaml` file. The next step is to investigate why the remapping is failing and implement the correct logic.

## Launch Command

```bash
LOG_FILE="/Users/sinas/workspace/LXStudio-TE/.agent_logs/te_app_$(date +%Y%m%d_%H%M%S).log" && cd /Users/sinas/workspace/LXStudio-TE/te-app && mvn package -DskipTests && java -ea -XstartOnFirstThread -Djava.awt.headless=false -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1920x1200 Projects/playalchemist_bm_2025.lxp &> "$LOG_FILE"
```

## Deliverables

### Phase 1: Research & Analysis

- [x] Study existing Laser Sync implementation
- [x] Understand OSC subscription and forwarding architecture
- [x] Identify integration points for Resolume sync

### Phase 2: Configuration System

- [x] Create YAML config file structure at `te-app/resources/resolume-setup/sync-config.yaml`
- [x] Define OSC message mapping schema (TE → Resolume)
- [x] Include IP address and port configuration
- [x] Implement YAML config parser

### Phase 3: Core Implementation

- [x] Add "Resolume Sync" button to UI (similar to Laser Sync)
- [x] Implement OSC message subscription system
- [ ] Create OSC message transformation/mapping logic
- [x] Add network OSC client for Resolume communication

### Phase 4: Integration & Testing

- [x] Integrate with existing TE OSC infrastructure
- [ ] Test message forwarding and transformation
- [x] Validate configuration loading and hot-reload
- [ ] Document usage and configuration options

## Technical Architecture

### Configuration File Structure

```yaml
# sync-config.yaml
resolume:
  network:
    ip: "127.0.0.1"
    port: 7000

  mappings:
    # TE OSC Path → Resolume OSC Path
    "/lx/tempo/beat": "/composition/tempo/resync"
    "/lx/mixer/master": "/composition/master"
```

### Implementation Strategy

1. **Follow Laser Sync Pattern** - Mirror the existing sync architecture
2. **Modular Design** - Separate config, mapping, and networking concerns
3. **Hot-reload Support** - Allow config changes without restart
4. **Error Handling** - Graceful handling of network and config issues

## Success Criteria

- [x] Resolume Sync button toggles OSC forwarding
- [x] YAML config controls all mapping behavior
- [ ] OSC messages transform correctly TE → Resolume
- [x] Network configuration works reliably
- [x] Integration follows TE architectural patterns

## Files to Modify/Create

- New: `te-app/resources/resolume-setup/sync-config.yaml`
- Study: Laser Sync implementation files
- Modify: Main UI for Resolume Sync button
- New: `ResolumePlugin.java`
- Modify: `ResolumeSyncTask.java`

## Notes

- Study existing laser sync architecture thoroughly
- Ensure compatibility with current OSC infrastructure
- Design for extensibility (other external tools)
- Follow TE coding standards and patterns
