# OSC Remapping Plugin Project (Done)

## Project Evolution

**Original Goal**: Create a Resolume OSC output plugin for forwarding messages from Chromatik/TE to Resolume.

**Current Implementation**: Evolved into a comprehensive **OscRemapper Plugin** with YAML-based configuration supporting multiple remotes, custom OSC address mappings, and intelligent output routing using minimal LX engine changes.

## Current Status ✅ COMPLETE

The project has been **successfully completed** with a comprehensive OSC remapping plugin featuring YAML-based configuration:

**Phase 1 ✅ Complete**:
- **Plugin Operational**: OscRemapper plugin loads and integrates successfully
- **Message Capture**: Captures ALL outgoing OSC messages including tempo/beat messages
- **Clean Architecture**: Uses minimal LX engine changes with proper TransmissionListener interface

**Phase 2 ✅ Complete**:
- **YAML Configuration**: Full support for `te-app/resources/osc_remapper/remapper_config.yaml`
- **Multiple Remotes**: Creates OSC outputs for each configured remote (Resolume, MSHIP, etc.)
- **Custom Mappings**: Configurable address remapping with multiple destination support
- **Smart Filtering**: Automatic OSC output filter calculation based on mapping prefixes
- **Passthrough Detection**: Intelligent handling of identity mappings to prevent loops
- **UI Controls**: "Set Up Now", "Refresh Config", and "Enable Logs" buttons
- **Multiple Destinations**: Single OSC message can map to multiple output addresses

## Final Implementation ✨

**Multi-Destination Support**: The plugin now supports mapping one OSC message to multiple destinations:
```yaml
"/lx/tempo/beat":
  - "/composition/tempo/resync"
  - "/composition/layer/1/tempo/resync"
```

**Passthrough Handling**: Automatically detects identity mappings and prevents infinite loops by marking remotes as "passthrough" and skipping re-transmission.

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

## Success Criteria ✅ ALL COMPLETE

- [x] OscRemapper Plugin UI appears in the Content Pane
- [x] "Set Up Now" button successfully creates OSC output
- [x] Plugin captures ALL outgoing OSC messages (including tempo/beat)
- [x] Successful remapping with YAML-configured custom addresses
- [x] Clean integration following LX plugin patterns
- [x] **COMPLETED**: Route remapped messages through specific outputs per remote
- [x] **COMPLETED**: YAML configuration with multiple remotes support
- [x] **COMPLETED**: Multiple destination mapping (1-to-many OSC routing)
- [x] **COMPLETED**: Passthrough detection and infinite loop prevention
- [x] **COMPLETED**: UI controls for config refresh and logging toggle

## Files Modified/Created

### LX Engine Changes
- **Modified**: `LX/src/main/java/heronarts/lx/osc/LXOscEngine.java`
  - Added `TransmissionListener` interface
  - Added listener management methods
  - Added listener notification in `Transmitter.send()`

### OscRemapper Plugin (New External Project)
- **New Directory**: `/Users/sinas/workspace/LX-OscRemapper/`
- **Integration**: `LXStudio-TE/te-app/pom.xml` dependency on `magic:oscremapper`

### TEApp.java Integration Changes
- **Modified**: `LXStudio-TE/te-app/src/main/java/heronarts/lx/studio/TEApp.java`
  - **Import Added**: `import java.nio.file.Path;`
  - **Import Added**: `import magic.oscremapper.OscRemapperPlugin;`
  - **Field Added**: `private final OscRemapperPlugin oscRemapperPlugin;`
  - **Constructor Changes**:
    ```java
    // OscRemapper plugin
    Path configPath = Path.of("resources", "osc_remapper", "remapper_config.yaml");
    this.oscRemapperPlugin = new OscRemapperPlugin(lx, configPath);
    ```
  - **Plugin Lifecycle**: Added calls to `initialize()`, `initializeUI()`, and `onUIReady()` methods

### Migration Actions Completed
- **Removed**: All `titanicsend.resolume` references from TE codebase
- **Renamed**: Complete package structure migration
- **Relocated**: Plugin moved outside TE directory structure
