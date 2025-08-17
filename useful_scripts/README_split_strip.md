# Split Strip Script

A utility script to split LED strips that exceed the 512 DMX channel limit per universe in the Pacman LED display project.

## Overview

When an LED strip has more than 170 LEDs (170 × 3 channels = 510 channels), it exceeds the 512-channel limit of a single DMX universe. This script automatically splits such strips into two logical strips:

- **Main strip**: Uses remaining channels in the current universe
- **Overflow strip**: Starts at channel 1 in the next universe (named with `.5` suffix)

## Usage

```bash
python split_strip.py <side> <strip_number>
```

### Parameters

- **side**: `a` or `b` (case insensitive)
  - `a` for A-side strips (e.g., "Strip 36")
  - `b` for B-side strips (e.g., "Strip 36b")
- **strip_number**: The numeric identifier of the strip to split

### Examples

```bash
# Split Strip 36 on A-side
python split_strip.py a 36

# Split Strip 42 on B-side  
python split_strip.py b 42
```

## What the Script Does

1. **Validates Input**: Checks that the side and strip number are valid
2. **Locates Strip**: Finds the specified strip in `BM2024_Pacman.lxp`
3. **Calculates Split**: Determines optimal split point based on universe capacity
4. **Creates Backup**: Automatically backs up the project file
5. **Performs Split**: 
   - Updates original strip with reduced LED count
   - Creates new overflow strip (e.g., "Strip 36.5" or "Strip 36.5b")
   - Sets overflow strip to start at channel 1 in next universe

## Example Output

```
🔍 Looking for strip 36 on side A...
📊 Split calculation for Strip 36:
   Total LEDs: 163
   Current universe 27 has 86 channels remaining
   Main strip: 28 LEDs (channels 427-512)
   Overflow strip: 135 LEDs (universe 28, channels 1-405)
💾 Backup created: BM2024_Pacman.lxp.backup
✅ Strip split completed!
   Original: Strip 36 (163 LEDs)
   Main: Strip 36 (28 LEDs)
   Overflow: Strip 36.5 (135 LEDs)

⚠️  Note: Both strips have the same X,Y coordinates. You'll need to adjust them manually.
```

## Important Notes

### Coordinate Handling
- **The script does NOT adjust X,Y coordinates**
- Both the main and overflow strips will have identical coordinates
- You must manually adjust the overflow strip's position after running the script

### Backup Safety
- The script automatically creates a backup file before making changes
- Backup is saved as `BM2024_Pacman.lxp.backup`
- If something goes wrong, you can restore from this backup

### Strip Naming Convention
- A-side strips: `Strip X` → `Strip X` + `Strip X.5`
- B-side strips: `Strip Xb` → `Strip Xb` + `Strip X.5b`

## DMX Universe Logic

- Each universe supports **512 channels maximum**
- Each LED uses **3 channels** (RGB)
- Maximum LEDs per universe: **170** (170 × 3 = 510 channels)
- Split calculation: `remaining_channels ÷ 3 = main_strip_leds`

## Files Modified

- **Target**: `te-app/Projects/BM2024_Pacman.lxp`
- **Backup**: `te-app/Projects/BM2024_Pacman.lxp.backup`

## Error Handling

The script will exit with an error message if:
- Strip is not found in the project file
- Strip doesn't actually need splitting (≤ 170 LEDs)
- Invalid side parameter (not 'a' or 'b')
- Invalid strip number (not an integer)
- Project file is missing

## Use Cases

- **Initial Setup**: Split strips during LED installation when universe overflows are discovered
- **Troubleshooting**: Fix DMX communication issues caused by universe boundary violations
- **Maintenance**: Adjust strip configurations when LED counts change

## Related Scripts

- `comprehensive_fixes_logger.py` - Analyze which strips need splitting
- `move_strip_side.py` - Move strip coordinates after splitting
- `make_backup.py` - Create additional backups of project files
