# Comprehensive Fixes Logger

A diagnostic utility that analyzes LED strips for configuration issues.

## Overview

This script analyzes LED strips to detect:
1. **Reverse pattern errors**: Incorrect zigzag wiring configuration
2. **Universe overflow issues**: Strips that exceed 512 DMX channels per universe

## Usage

```bash
python3 comprehensive_fixes_logger.py <project_file> <side> [--from-strip N]
```

### Parameters

- `project_file`: Path to the .lxp project file
- `side`: Either `A` or `B`
  - `A` = strips without 'b' suffix (Strip 1, Strip 2.5, etc.)
  - `B` = strips with 'b' suffix (Strip 1b, Strip 2.5b, etc.)
- `--from-strip N`: Optional, only analyze strips N and above

## Examples

### Analyze all A-side strips
```bash
python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp A
```

### Analyze all B-side strips
```bash
python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp B
```

### Analyze A-side strips starting from strip 40
```bash
python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp A --from-strip 40
```

## Features

- **Side-specific analysis**: Analyze A-side or B-side independently
- **Reverse pattern detection**: Identifies incorrect zigzag wiring
- **Overflow detection**: Finds strips that need splitting due to universe limits
- **Coordinate calculations**: Computes split positions and offsets
- **Detailed logging**: Creates comprehensive log files with all findings
- **Visual indicators**: Uses emojis and colors for easy reading

## Output

The script provides:
1. **Console output**: Real-time analysis with visual indicators
   - ✅ Correct strips
   - 🔄 Strips needing reverse correction
   - 💥 Strips needing overflow splitting
2. **Log file**: Detailed analysis saved to timestamped file

## Understanding the Analysis

### Reverse Pattern Rules
- **Odd strips** (1, 3, 5, 7...): Should have `reverse: true` (right→left data flow)
- **Even strips** (2, 4, 6, 8...): Should have `reverse: false` (left→right data flow)

### Universe Overflow
- Each universe supports max 512 DMX channels
- Each LED uses 3 channels (RGB)
- Max LEDs per universe: 170 (170 × 3 = 510 channels)
- Strips exceeding this need to be split

### Split Calculations
The script calculates:
- How many LEDs fit in current universe
- How many LEDs overflow to next universe
- X-coordinate offsets based on data flow direction
- New universe assignments

## Log File Contents

- Strip-by-strip analysis
- Reverse correction recommendations
- Split configurations with coordinates
- Universe assignments
- Summary statistics

## Use Cases

- **Pre-deployment analysis**: Check strip configurations before hardware setup
- **Debugging display issues**: Identify configuration problems
- **Planning splits**: Get exact split configurations
- **Side comparisons**: Analyze A-side vs B-side independently

## Technical Details

- **Strip Detection**: Uses regex to find jsonParameters and parameters sections
- **Side Classification**: Filters by 'b' suffix presence
- **Flow Direction**: Calculates based on odd/even strip numbers
- **Coordinate Math**: Accounts for 0.8 units per LED spacing

## Error Handling

- Validates file existence
- Handles malformed JSON gracefully
- Reports missing or incomplete strip data
- Provides clear error messages

## Dependencies

- Python 3
- Standard libraries: `sys`, `re`, `json`, `argparse`, `datetime`

This tool is essential for maintaining proper LED strip configurations and ensuring optimal display performance.
