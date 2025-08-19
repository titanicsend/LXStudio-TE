# Pattern Copy Script for LX Studio Projects

A script that safely copies pattern settings and audio modulation components between channels in LX Studio project files (.lxp).

## Overview

This script copies pattern parameters and all associated components (audio stems, modulators, modulations) from a source pattern to a target pattern. Unlike copying entire pattern objects (which causes ID conflicts), this approach preserves the target pattern's structure while updating its settings.

## ⚠️ Important: Pattern Must Exist First

**You MUST create the target pattern in LX Studio BEFORE running this script.**

### Step-by-Step Workflow:

1. **Create the target pattern in LX Studio:**
   - Open your LX Studio project
   - Navigate to the target channel (e.g., "Pacman High")
   - Add a new pattern of the same type (e.g., add a Phasers pattern)
   - This creates a pattern with proper IDs and structure

2. **Run the copy script:**
   ```bash
   python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 2
   ```

3. **The script will:**
   - Copy all parameter values (speed, brightness, colors, etc.)
   - Copy all audio stem modulators (Audio Stem, Audio Stem 2, Audio Stem 3, Audio Stem 4)
   - Copy all modulation mappings and settings
   - Preserve the target pattern's ID and structure
   - Create a backup of your project file

## Usage

```bash
python3 copy_pattern_simple.py <project_file> <source_channel> <target_channel> <source_pattern_name> <target_pattern_name> [source_channel_index]
```

### Parameters

- `project_file`: Path to your .lxp project file
- `source_channel`: Name of the channel containing the source pattern
- `target_channel`: Name of the channel containing the target pattern
- `source_pattern_name`: Name of the source pattern to copy from
- `target_pattern_name`: Name of the target pattern to copy to
- `source_channel_index`: **Optional** - Use when there are multiple channels with the same name (1-based index)

### Examples

```bash
# Copy "+ Phasers" from the 2nd HIGH channel to "Phasers" in Pacman High channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 2

# Copy "Fireflies" from HIGH channel to "Fireflies" in Pacman High channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "Fireflies" "Fireflies"

# Copy "Electric" from LOW channel to "Electric" in Pacman Low channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "LOW" "Pacman Low" "Electric" "Electric"

# Copy "ArcEdges" from EDGES channel to "ArcEdges" in Pacman Edge channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "EDGES" "Pacman Edge" "ArcEdges" "ArcEdges"
```

## Multiple Channels with Same Name

When your project has multiple channels with the same name (like multiple "HIGH" channels), use the optional `source_channel_index` parameter:

```bash
# Copy from the 1st HIGH channel (default)
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "Phasers" "Phasers"

# Copy from the 2nd HIGH channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 2

# Copy from the 3rd HIGH channel
python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 3
```

## What Gets Copied

### Parameters
- All pattern-specific parameters (speed, brightness, size, etc.)
- Color settings (hue, saturation, brightness)
- Position and animation parameters
- MIDI filter settings
- All `te_*` parameters

### Audio Components
- **Audio Stem Modulators**: Audio Stem, Audio Stem 2, Audio Stem 3, Audio Stem 4
- **Modulation Mappings**: Connections between audio stems and pattern parameters
- **Modulation Settings**: Ranges, polarities, and other modulation configurations
- **Audio Stem Settings**: Stem numbers, EMA settings, output modes, etc.

### Structure
- All children components
- Internal references and relationships
- Modulation engine configuration

## What Gets Preserved

- **Target Pattern ID**: The target pattern keeps its original ID
- **Target Pattern Class**: The pattern type remains unchanged
- **Target Pattern Label**: The pattern name stays the same
- **Target Channel Structure**: No changes to channel organization

## Safety Features

- **Automatic Backup**: Creates a `.backup` file before making changes
- **ID Preservation**: No new IDs are generated, preventing conflicts
- **Structure Validation**: Maintains proper LX Studio structure
- **Error Handling**: Comprehensive error checking and reporting

## Troubleshooting

### "Pattern not found" Error
- Make sure the pattern exists in both source and target channels
- Check that pattern names match exactly (case-sensitive)
- Verify channel names are correct

### "Channel not found" Error
- Check that both source and target channels exist
- Verify channel names match exactly (case-sensitive)
- Run the script from the correct directory

### Multiple Channels with Same Name
- Use the `list_channels.py` script to see all available channels and their indices
- Use the optional `source_channel_index` parameter to specify which channel
- Channel indices are 1-based (1 = first, 2 = second, etc.)

### Audio Stems Not Working
- Ensure the target pattern was created in LX Studio first
- Check that the source pattern has audio stems configured
- Verify the project file path is correct

### LX Studio Crashes
- Restore from the backup file: `cp project.lxp.backup project.lxp`
- Make sure the target pattern exists before running the script
- Check that you're not copying to a pattern that's currently active

## File Structure

```
useful_scripts/
├── copy_pattern_simple.py     # Main copy script
├── list_channels.py           # Script to list all channels and patterns
└── README_pattern_copy.md     # This documentation
```

## Example Workflow

1. **Open LX Studio** and load your project
2. **Navigate to target channel** (e.g., "Pacman High")
3. **Add a new pattern** of the desired type (e.g., Phasers)
4. **Close LX Studio** (to ensure file is saved)
5. **List available channels** (if needed):
   ```bash
   python3 useful_scripts/list_channels.py te-app/Projects/BM2024_Pacman.lxp
   ```
6. **Run the copy script:**
   ```bash
   python3 copy_pattern_simple.py te-app/Projects/BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 2
   ```
7. **Open LX Studio** and verify the pattern has all settings copied
8. **Test the audio reactivity** to ensure audio stems are working

## Why This Approach Works

Unlike copying entire pattern objects (which causes ID conflicts and null pointer exceptions), this script:

1. **Preserves Structure**: Keeps the target pattern's ID and internal structure
2. **Copies Settings**: Transfers all parameter values and configurations
3. **Maintains References**: Preserves internal component relationships
4. **Avoids Conflicts**: No new IDs are generated that could conflict with existing ones
5. **Handles Duplicates**: Supports multiple channels with the same name using indices

This makes it safe to copy complex patterns with audio modulation while maintaining LX Studio's stability.
