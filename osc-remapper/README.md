# OscRemapper Plugin

A comprehensive OSC (Open Sound Control) remapping and forwarding plugin for LX Studio/Chromatik that intelligently routes OSC messages to multiple remote applications with custom address mappings.

## Attribution & Inspiration

This plugin was heavily inspired by and built upon the excellent work from the **Beyond Plugin** by [jkbelcher](https://github.com/jkbelcher):

🎯 **[Beyond Plugin Repository](https://github.com/jkbelcher/Beyond)**

We extensively used their code architecture, plugin structure, and LX Studio integration patterns as the foundation for creating the OscRemapper plugin. The Beyond plugin provided invaluable guidance on proper LX Studio plugin development, UI integration, and Maven configuration. Our sincere thanks to jkbelcher and the Beyond project contributors for their open-source contribution to the LX Studio ecosystem!

## What is OSC Remapping?

OSC Remapping allows you to:
- **Intercept** outgoing OSC messages from LX Studio/Chromatik
- **Transform** OSC addresses (e.g., `/lx/tempo/beat` → `/composition/tempo/resync`)
- **Route** messages to specific remote applications
- **Duplicate** single messages to multiple destinations
- **Filter** which messages reach which remotes

## Quick Build & Install (for Titanic's End LX Project)

Assuming the `LX-OscRemapper` and `LXStudio-TE` are cloned in `~/workspace/{LX-OscRemapper | LXStudio-TE}`
```bash
# this step only needed if LX engine was changed
cd ~/workspace/LX && mvn compile && mvn install -DskipTests 

cd ~/workspace/LX-OscRemapper && mvn compile && mvn install -DskipTests

cd ~/workspace/LXStudio-TE/te-app && mvn package -DskipTests

cd ~/workspace/LXStudio-TE/te-app && LOG_FILE="../.agent_logs/te_app_logs_$(date +%Y%m%d_%H%M%S).log" && echo "🎯 Testing Fix - Logs: $LOG_FILE" && java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1920x1200 &> "$LOG_FILE"
```

## Features

### 🎯 Core Capabilities
- **YAML-Based Configuration**: Easy-to-edit configuration files
- **Multiple Remotes**: Support for unlimited remote applications simultaneously
- **1-to-Many Mapping**: Single OSC message can trigger multiple destinations
- **Smart Filtering**: Automatic OSC output filtering based on address prefixes
- **Passthrough Detection**: Prevents infinite loops with identity mappings
- **Real-Time Control**: UI buttons for setup, config refresh, and logging

### 🔧 Advanced Features
- **Wildcard Support**: Use `*` for pattern matching (with limitations)
- **Collision-Free**: Intelligent handling of overlapping address spaces
- **Non-Destructive**: Original OSC messages remain unchanged in LX Studio

## Configuration

The plugin uses `LXStudio-TE/te-app/resources/osc_remapper/remapper_config.yaml`:

```yaml
# OSC Remapper Configuration
# This file defines OSC destinations and remapping rules for TE/Chromatik

# Configuration is split into two sections:
# 1. OSC destinations: Network endpoints for sending OSC messages
# 2. OSC remappings: Global remapping rules applied to all destinations

# ================================
# 1. OSC DESTINATIONS
# ================================
# Each destination defines where to send OSC messages.
# - name: Unique identifier for this destination
# - ip: Target IP address
# - port: Target port number  
# - filter: MANDATORY OSC filter prefix for routing messages to this destination

destinations:
  - name: "Resolume Mapped"
    ip: "127.0.0.1"
    port: 7000
    filter: "/composition"

  - name: "TouchDesigner Raw LX"
    ip: "127.0.0.1"
    port: 7891
    filter: "/lx"

  - name: "MSHIP"
    ip: "127.0.0.1"
    port: 7002
    filter: "/mship"

  - name: "MSHIP-Identity"
    ip: "127.0.0.1"
    port: 7003
    filter: "/lx"

# ================================
# 2. OSC REMAPPINGS
# ================================
# Global remapping rules applied to OSC messages.
# These define how OSC addresses are transformed before sending to destinations.
# 
# Two types of mappings supported:
# 1. Exact mapping: "/lx/tempo/beat" -> ["/composition/tempo/resync"]
# 2. Wildcard mapping: "/lx/tempo/*" -> ["/remote/tempo/*"]
#
# Each source can map to multiple destinations (1-to-many mapping)
# Identity mappings (same source and destination) create passthrough routing

remappings:
  # Tempo and beat sync - single source can map to multiple destinations
  "/lx/tempo/beat":
    - "/composition/tempo/resync"
    - "/composition/layer/1/tempo/resync"
    - "/lx/mship/layer/1/tempo/beat"

  "/lx/tempo/trigger":
    - "/composition/layer/1/tempo/trigger"
    - "/lx/tempo/trigger"  # Identity mapping: preserves original for MSHIP-Identity

  # Wildcard mappings for MSHIP
  "/lx/tempo/*":
    - "/mship/wildcard/*"
```

### Configuration Explained

- **remotes**: Array of target applications
- **name**: Unique identifier for each remote
- **host/port**: Network destination for OSC messages
- **mappings**: Dictionary of source → destination address transformations

## Single Source to Multiple Destinations

The plugin supports **1-to-many** mapping where one OSC message triggers multiple outputs:

```yaml
"/lx/tempo/beat":
  - "/composition/tempo/resync"      # Resolume global tempo
  - "/composition/layer/1/resync"    # Resolume layer 1
  - "/external/clock/beat"           # External sequencer
```

When `/lx/tempo/beat` is transmitted, all three destinations receive the message simultaneously.

## Rerouting Without Renaming

### Passthrough Mappings

<!-- 
TODO(look): pretty sure this section is unnecessary
-->

For scenarios where you want to route messages without changing addresses:

```yaml
"/lx/tempo/*":
  - "/lx/tempo/*"
```

The plugin automatically detects these **identity mappings** and:
- ✅ Routes messages through the remote's OSC output
- ✅ Applies network filtering (host/port)
- ✅ Prevents infinite loops by skipping re-transmission

### Limitations

1. **Wildcard Restrictions**: 
   - Source wildcards (`/lx/tempo/*`) must map to single wildcard destination
   - Cannot combine wildcards with multiple specific destinations
   
2. **Address Collision**:
   - Multiple remotes with overlapping filters may receive duplicate messages
   - Use specific address prefixes to avoid conflicts

3. **Value Type Assumption**:
   - All OSC values are treated as floats
   - Complex OSC argument types are not preserved

## Multiple Remotes Setup

### Step 1: Configure YAML
Edit `te-app/resources/osc_remapper/remapper_config.yaml` with your remotes.

### Step 2: Launch LX Studio
The plugin loads configuration automatically on startup.

### Step 3: Plugin UI Controls
In the LX Studio interface, find the "OscRemapper" plugin panel:

- **"Set Up Now"**: Creates OSC outputs for all configured remotes
- **"Refresh Config"**: Reloads YAML and re-creates outputs  
- **"Enable Logs"**: Toggles verbose logging for debugging

### Step 4: Verify Operation
Check logs for messages like:
```
[resolume] /lx/tempo/beat → /composition/tempo/resync (120.0)
[mothership] /lx/brightness → /mship/brightness (0.75)
```

## Integration into LX Studio Projects

### Adding to Your LX Project

To integrate OscRemapper into any LX Studio application:

#### 1. Add Maven Dependency

In your project's `pom.xml`:

```xml
<dependency>
    <groupId>magic</groupId>
    <artifactId>oscremapper</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. Modify Your Main Application Class

In your main LX application class (equivalent to `TEApp.java`):

```java
// Add imports
import java.nio.file.Path;
import magic.oscremapper.OscRemapperPlugin;

public class YourApp extends LXStudio {
    // Add field
    private final OscRemapperPlugin oscRemapperPlugin;
    
    // In constructor after LX initialization
    public YourApp(LX lx) {
        super(lx);
        
        // Create config path (adjust path as needed)
        Path configPath = Path.of("resources", "osc_remapper", "remapper_config.yaml");
        this.oscRemapperPlugin = new OscRemapperPlugin(lx, configPath);
    }
    
    // In initialize() method
    @Override
    public void initialize(LX lx) {
        super.initialize(lx);
        this.oscRemapperPlugin.initialize(lx);
    }
    
    // In initializeUI() method  
    @Override
    public void initializeUI(LX lx, LXStudio.UI ui) {
        super.initializeUI(lx, ui);
        this.oscRemapperPlugin.initializeUI(lx, ui);
    }
    
    // In onUIReady() method
    @Override
    public void onUIReady(LX lx, LXStudio.UI ui) {
        super.onUIReady(lx, ui);
        this.oscRemapperPlugin.onUIReady(lx, ui);
    }
}
```

#### 3. Create Configuration File

Create `resources/osc_remapper/remapper_config.yaml` in your project:

```yaml
remotes:
  - name: "your-remote"
    host: "127.0.0.1"
    port: 7000
    mappings:
      "/lx/tempo/beat":
        - "/your/app/tempo/beat"
```

#### 4. Build Plugin First

```bash
# Clone and build the plugin
git clone <plugin-repo> LX-OscRemapper
cd LX-OscRemapper
mvn compile && mvn install -DskipTests
```

#### 5. Build Your Project

```bash
cd your-lx-project
mvn package -DskipTests
```

## Build & Install (LXStudio-TE Example)

For the specific LXStudio-TE integration:

```bash
# Build plugin
cd ~/workspace/LX-OscRemapper
mvn compile && mvn install -DskipTests

# Build and run TE application
cd ~/workspace/LXStudio-TE/te-app
mvn package -DskipTests

# Launch with logging
LOG_FILE="../.agent_logs/te_app_logs_$(date +%Y%m%d_%H%M%S).log"
java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu \
     -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar \
     --resolution 1920x1200 &> "$LOG_FILE"
```

## Architecture

The plugin uses a clean **TransmissionListener** approach that:
- Requires minimal changes to LX engine core
- Captures ALL outgoing OSC messages (not just parameter changes)
- Routes through dedicated OSC outputs per remote
- Maintains thread safety and plugin lifecycle compliance

## Troubleshooting

**No messages received?**
- Check remote host/port configuration
- Verify "Set Up Now" was clicked
- Enable logs to see transmission activity

**Infinite loops?**
- Review passthrough mappings in configuration
- Check for circular address dependencies
- Ensure wildcard patterns don't overlap
