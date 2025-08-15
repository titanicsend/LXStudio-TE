# Move Strip Side Script

A utility script for moving LED strips by side (A or B) in LX Studio project files.

## Overview

This script allows you to move all strips from a specified side (A-side or B-side) by given X, Y, Z coordinate offsets. It's useful for positioning multiple LED strip arrays relative to each other.

## Usage

```bash
python3 move_strip_side.py <project_file> <side> <x_offset> <y_offset> <z_offset>
```

### Parameters

- `project_file`: Path to the .lxp project file
- `side`: Either `A` or `B` 
  - `A` = strips without 'b' suffix (Strip 1, Strip 2.5, etc.)
  - `B` = strips with 'b' suffix (Strip 1b, Strip 2.5b, etc.)
- `x_offset`: Amount to move in X direction (positive = right, negative = left)
- `y_offset`: Amount to move in Y direction (positive = forward, negative = back)
- `z_offset`: Amount to move in Z direction (positive = up, negative = down)

## Examples

### Move B-side strips 10 units down in Z
```bash
python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp B 0 0 -10
```

### Move B-side strips to align with A-side horizontally 
```bash
# Assuming B-side is offset by +200 in X from A-side
python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp B -200 0 0
```

### Move A-side strips
```bash
python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp A 5 2 0
```

## Features

- **Automatic backup**: Creates a `.backup` file before making changes
- **Comprehensive strip detection**: Finds both regular strips (Strip 1) and split strips (Strip 4.5)
- **Reverse processing**: Processes strips from end to beginning to avoid position conflicts
- **Detailed output**: Shows which strips were moved and their old/new coordinates

## Common Use Cases

### Stacking Two LED Arrays
If you have A-side and B-side LED arrays and want to stack them:

1. **Separate vertically**: Move B-side down by 10 units
   ```bash
   python3 move_strip_side.py project.lxp B 0 0 -10
   ```

2. **Align horizontally**: If B-side is offset by 200 units in X, align with A-side
   ```bash
   python3 move_strip_side.py project.lxp B -200 0 0
   ```

### Repositioning an Entire Array
Move all A-side strips to a new position:
```bash
python3 move_strip_side.py project.lxp A 10 5 2
```

## Technical Details

- **Strip Detection**: Uses regex pattern matching to find strip labels
- **Side Classification**: 
  - A-side: Labels like "Strip 1", "Strip 2.5" (no 'b' suffix)
  - B-side: Labels like "Strip 1b", "Strip 2.5b" (with 'b' suffix)
- **Coordinate Updates**: Modifies the `x`, `y`, `z` parameters in the strip's parameters section
- **Safety**: Always creates a backup before making changes

## Error Handling

- Creates backup before any modifications
- Validates input parameters
- Reports missing or malformed strips
- Shows progress and results for each strip processed

## File Output

The script will:
1. Create a backup file: `<original_file>.backup`
2. Modify the original file with new coordinates
3. Display a summary of changes made

## Dependencies

- Python 3
- Standard libraries: `sys`, `re`, `json`, `pathlib`
