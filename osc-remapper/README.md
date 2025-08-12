# OscRemapper Plugin

A comprehensive OSC (Open Sound Control) remapping and forwarding plugin for LX Studio/Chromatik that intelligently
routes OSC messages to multiple remote applications with custom address mappings.

## Attribution & Inspiration

This plugin was heavily inspired by and built upon the excellent work from the **Beyond Plugin**
by [jkbelcher](https://github.com/jkbelcher):

🎯 **[Beyond Plugin Repository](https://github.com/jkbelcher/Beyond)**

We extensively used their code architecture, plugin structure, and LX Studio integration patterns as the foundation for
creating the OscRemapper plugin. The Beyond plugin provided invaluable guidance on proper LX Studio plugin development,
UI integration, and Maven configuration. Our sincere thanks to jkbelcher and the Beyond project contributors for their
open-source contribution to the LX Studio ecosystem!

## Usage

The plugin uses `LXStudio-TE/te-app/resources/osc_remapper.yaml`:

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
# - filter: MANDATORY OSC filter prefix (or comma-separated list of prefixes) for routing messages to this destination

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
# 2. Prefix mapping: "/lx/tempo" -> ["/remote/tempo"]
#
# Each source can map to multiple destinations (1-to-many mapping)

remappings:
  # Tempo and beat sync - single source can map to multiple destinations
  "/lx/tempo/beat":
    - "/composition/tempo/resync"
    - "/composition/layer/1/tempo/resync"
    - "/lx/mship/layer/1/tempo/beat"

  "/lx/tempo/trigger":
    - "/composition/layer/1/tempo/trigger"

  # Wildcard mappings for MSHIP
  "/lx/tempo":
    - "/mship/wildcard"
```

## What is OSC Remapping?

OSC Remapping allows you to:

- **Intercept** outgoing OSC messages from LX Studio/Chromatik
- **Transform** OSC addresses (e.g., `/lx/tempo/beat` → `/composition/tempo/resync`)
- **Route** messages to specific remote applications
- **Duplicate** single messages to multiple destinations
- **Filter** which messages reach which remotes

### Configuration Explained

- **detinations**: Array of target applications
- **remappings**: Dictionary of source → destination address transformations

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

### Limitations

1. **Value Type Assumption**:
    - All OSC values are treated as floats
    - Complex OSC argument types are not preserved

## Multiple Remotes Setup

### Step 1: Configure YAML

Edit `te-app/resources/osc_remapper.yaml` with your remotes.

### Step 2: Launch LX Studio

The plugin loads configuration automatically on startup.

### Step 3: Plugin UI Controls

In the LX Studio interface, find the "OscRemapper" plugin panel:

- **"Set Up OSC Outputs"**: Creates OSC outputs for all configured remotes
- **"Reload Config"**: Reloads YAML and re-creates outputs
- **"Enable Logs"**: Toggles verbose logging for debugging

### Step 4: Verify Operation

Check logs for messages like:

```
/lx/tempo/beat → /composition/tempo/resync (120.0)
/lx/brightness → /mship/brightness (0.75)
```
