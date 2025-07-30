# Resolume Plugin for LX Studio

This plugin provides integration between LX Studio (Chromatik) and Resolume Arena/Avenue via OSC (Open Sound Control).

## Features

- **OSC Output Configuration**: Automatically sets up OSC output to Resolume with proper filtering
- **Global Modulators**: Pre-configured modulators for common Resolume controls
  - Brightness control
  - Tempo synchronization
  - Master opacity
  - Color controls (Hue, Saturation, Contrast)
- **Easy Setup**: One-click setup via "Set Up Now" button

## Usage

1. **Install the Plugin**: Place the compiled JAR in your LX Studio plugins directory
2. **Enable OSC in Resolume**:
   - Go to Avenue/Arena > Preferences > OSC
   - Enable "OSC Input"
   - Set port to 7000 (default)
3. **Set Up in LX Studio**:
   - Find the "RESOLUME" section in the left panel
   - Click "Set Up Now" to configure OSC output and add modulators
4. **Configure IP Address**:
   - Go to OSC tab in LX Studio
   - Set the output IP to your Resolume machine's IP address

## OSC Paths

The plugin uses standard Resolume OSC API paths:

- `/composition/tempo/resync` - Tempo synchronization
- `/composition/master/brightness` - Master brightness
- `/composition/master/opacity` - Master opacity
- `/composition/master/hue` - Hue adjustment
- `/composition/master/saturation` - Saturation adjustment
- `/composition/master/contrast` - Contrast adjustment
- `/composition/crossfader` - Crossfader position
- `/composition/columns/X/opacity` - Column opacity controls
- `/composition/layers/X/opacity` - Layer opacity controls

## Building

```bash
cd resolume
mvn package
```

The compiled plugin will be in `target/resolume-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

## Requirements

- LX Studio 1.1.1-TE.3.GPU-SNAPSHOT or compatible
- Resolume Arena 7+ or Avenue 7+
- Java 17+

## Based On

This plugin is based on the [Beyond Plugin](https://github.com/jkbelcher/Beyond) architecture by Justin Belcher.
