# OSC Remapping Plugin Project

## Project Evolution

**Original Goal**: Create a Resolume OSC output plugin for forwarding messages from Chromatik/TE to Resolume.

**Current Implementation**: Evolved into a comprehensive **OscRemapper Plugin** with YAML-based configuration supporting multiple remotes, custom OSC address mappings, and intelligent output routing using minimal LX engine changes.

## Current Status 🚧 EXPANDING

The project has successfully implemented a clean, minimal approach to OSC message remapping and is being extended with YAML configuration support:

**Phase 1 Complete**:
- **Plugin Operational**: OscRemapper plugin loads and integrates successfully
- **Message Capture**: Captures ALL outgoing OSC messages including tempo/beat messages
- **Clean Architecture**: Uses minimal LX engine changes with proper TransmissionListener interface

**Phase 2 In Progress**:
- **YAML Configuration**: Support for `te-app/resources/osc_remapper/remapper_config.yaml`
- **Multiple Remotes**: Create OSC outputs for each configured remote (Resolume, MSHIP, etc.)
- **Custom Mappings**: Configurable address remapping (e.g., `/lx/tempo/beat` → `/composition/tempo/resync`)
- **Smart Filtering**: Automatic OSC output filter calculation based on mapping prefixes

## Known Issue 🔧

**Output Routing**: Messages currently broadcast via `lx.engine.osc.sendMessage()` to all outputs. Need to route through specific output created by "Set Up Now" button.

**Challenge**: Cannot access private `output.transmitter` field directly, and setup creates `/composition` filtered output while remapped messages use `/test` prefix.

## Major Changes Made

### 1. Project Restructure & Rename
- **Moved**: From `LXStudio-TE/resolume/` to `/Users/sinas/workspace/OscRemapper/` (external directory)
- **Renamed**: All references from "Resolume" → "OscRemapper" throughout codebase
- **Package**: Changed from `titanicsend.resolume` → `magic.oscremapper`
- **Maven**: Updated to `magic:oscremapper:1.0.0-SNAPSHOT` in `te-app/pom.xml`

### 2. LX Engine Enhancement (Minimal Changes)
Added clean **TransmissionListener** interface to `LX/src/main/java/heronarts/lx/osc/LXOscEngine.java`:

```java
public interface TransmissionListener {
  public void oscMessageTransmitted(OscPacket packet);
}
```

Plus listener management methods and notification in `Transmitter.send()` - **no reflection or complex proxies needed**.

### 3. Plugin Functionality Evolution
- **From**: Simple OSC forwarding to Resolume 
- **To**: Comprehensive OSC message interception and remapping
- **Captures**: ALL outgoing OSC messages including tempo/beat that previous approaches missed
- **Remaps**: `/lx/tempo/beat` → `/test/tempo/beat`, `/lx/tempo/trigger` → `/test/tempo/trigger`, etc.

## Test Results ✅

**Working Log Output** (from te-app):
```
[LX 2025/07/30 16:59:15] [OscRemapper] OSC remapping started - listening to all transmitted OSC messages
[LX 2025/07/30 16:59:16] [OscRemapper] [OSC-REMAP] /lx/tempo/beat -> /test/tempo/beat (fallback: 2)
[LX 2025/07/30 16:59:16] [OscRemapper] [OSC-REMAP] /lx/tempo/trigger -> /test/tempo/trigger (fallback: 1)
```

## Launch Command

```bash
LOG_FILE="/Users/sinas/workspace/LXStudio-TE/.agent_logs/te_app_$(date +%Y%m%d_%H%M%S).log" && cd /Users/sinas/workspace/LXStudio-TE/te-app && mvn package -DskipTests && java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1920x1200 &> "$LOG_FILE"
```

## Technical Architecture

### OSC Remapping Strategy

The plugin uses a clean TransmissionListener approach to intercept all outgoing OSC messages:

```java
// OscRemapper/src/main/java/magic/oscremapper/OscRemapperPlugin.java
private class OscRemapperTransmissionListener implements LXOscEngine.TransmissionListener {
  @Override
  public void oscMessageTransmitted(OscPacket packet) {
    if (packet instanceof OscMessage) {
      OscMessage message = (OscMessage) packet;
      String originalAddress = message.getAddressPattern().getValue();
      
      if (originalAddress.startsWith("/lx")) {
        // Create new address with /test prefix
        String newAddress = originalAddress.replaceFirst("^/lx", "/test");
        // Send remapped message...
      }
    }
  }
}
```

### Implementation Strategy

1. **Minimal LX Changes** - Added only TransmissionListener interface, no complex modifications
2. **Clean Plugin Architecture** - Uses standard LX plugin patterns with proper lifecycle management
3. **Comprehensive Capture** - Intercepts ALL outgoing messages, not just parameter changes
4. **Simple Remapping** - Direct string replacement of OSC address prefixes

## Current File Structure

```
/Users/sinas/workspace/OscRemapper/
├── pom.xml (magic:oscremapper:1.0.0-SNAPSHOT)
├── src/main/java/magic/oscremapper/
│   ├── OscRemapperPlugin.java (main plugin + TransmissionListener)
│   ├── ui/UIOscRemapperPlugin.java
│   ├── modulator/
│   │   ├── OscRemapperBrightnessModulator.java
│   │   └── OscRemapperTempoModulator.java
│   └── parameter/OscRemapperCompoundParameter.java
```

## Success Criteria

- [x] OscRemapper Plugin UI appears in the Content Pane
- [x] "Set Up Now" button successfully creates OSC output
- [x] Plugin captures ALL outgoing OSC messages (including tempo/beat)
- [x] Successful remapping of `/lx/*` → `/test/*` addresses
- [x] Clean integration following LX plugin patterns
- [ ] **TODO**: Route remapped messages through specific output (not broadcast)

## Files Modified/Created

### LX Engine Changes
- **Modified**: `LX/src/main/java/heronarts/lx/osc/LXOscEngine.java`
  - Added `TransmissionListener` interface
  - Added listener management methods
  - Added listener notification in `Transmitter.send()`

### OscRemapper Plugin (New External Project)
- **New Directory**: `/Users/sinas/workspace/OscRemapper/`
- **Integration**: `LXStudio-TE/te-app/pom.xml` dependency on `magic:oscremapper`
- **Integration**: `LXStudio-TE/te-app/src/main/java/heronarts/lx/studio/TEApp.java` instantiation

### Migration Actions Completed
- **Removed**: All `titanicsend.resolume` references from TE codebase
- **Renamed**: Complete package structure migration
- **Relocated**: Plugin moved outside TE directory structure
