# Universe Boundary Overflow Fix - Documentation

## Overview

This document explains the workflow for fixing **ArtNet universe boundary overflow** issues that cause "split colors" on LED strips. When an LED strip requires more than 512 DMX channels within a single universe, it overflows into undefined territory, causing color display issues.

## The Problem

### What is Universe Boundary Overflow?

ArtNet universes have a **maximum of 512 DMX channels**. Each RGB LED requires **3 channels** (Red, Green, Blue). When a strip has too many LEDs starting at a high DMX channel, it exceeds the 512-channel limit.

### Symptoms:
- **Split colors** on physical LED strips (e.g., half red, half green)
- **Incorrect colors** in the middle of strips
- **ArtNet collision errors** in console logs
- Colors look fine in software but wrong on hardware

### Example Problem:
**Strip 4**: 67 LEDs × 3 channels = 201 channels
- **Universe 0, DMX Channel 382**
- **Needs channels**: 382-582
- **Universe 0 limit**: 512
- **Overflow**: Channels 513-582 (70 channels = 23+ LEDs overflowing!)

## The Solution: Strip Splitting

### Workflow

1. **Identify Overflow**:
   ```
   Required channels = LED_count × 3
   End channel = start_channel + required_channels - 1
   If end_channel > 512: OVERFLOW!
   ```

2. **Calculate Split Point**:
   ```
   Available channels in current universe = 512 - start_channel + 1
   LEDs that fit = available_channels ÷ 3
   Remaining LEDs = total_LEDs - LEDs_that_fit
   ```

3. **Create Split Strips**:
   - **Original Strip**: Reduce `points` to fit in current universe
   - **New Strip (.5)**: Create with remaining LEDs in next universe

4. **Update Configuration**:
   - Same physical coordinates (x, y, z, yaw, pitch, roll)
   - Correct `reverse` parameter for zigzag continuation
   - Next available universe, starting at DMX channel 1

## Step-by-Step Example: Strip 4

### Original Configuration:
```json
{
  "label": "Strip 4",
  "points": 67,
  "universe": 0,
  "dmxChannel": 382,
  "reverse": true,
  "x": 55.0, "y": 11.0, "z": 0.0
}
```

### Problem Analysis:
- **67 LEDs × 3 = 201 channels**
- **Channels needed**: 382-582
- **Universe 0 limit**: 512
- **Overflow**: 70 channels (23+ LEDs)

### Solution:

#### Strip 4 (Updated):
```json
{
  "label": "Strip 4",
  "points": 43,           // Reduced from 67
  "universe": 0,
  "dmxChannel": 382,
  "reverse": true,
  "x": 55.0, "y": 11.0, "z": 0.0
}
```
- **Uses channels**: 382-510 (fits perfectly in Universe 0)

#### Strip 4.5 (New):
```json
{
  "label": "Strip 4.5",
  "points": 24,           // Remaining LEDs (67 - 43)
  "universe": 1,          // Next available universe
  "dmxChannel": 1,        // Start of new universe
  "reverse": false,       // Continues seamlessly
  "x": 55.0, "y": 11.0, "z": 0.0  // Same position
}
```
- **Uses channels**: 1-72 in Universe 1

### Math Verification:
- **Universe 0**: Channels 382-510 = 129 channels = 43 LEDs ✅
- **Universe 1**: Channels 1-72 = 72 channels = 24 LEDs ✅
- **Total**: 43 + 24 = 67 LEDs (same as original) ✅

## Implementation Code Example

```python
def fix_strip_overflow(strip_data):
    """Fix universe boundary overflow for a strip"""
    
    points = strip_data["points"]
    universe = strip_data["universe"]
    dmx_channel = strip_data["dmxChannel"]
    
    # Calculate overflow
    required_channels = points * 3
    end_channel = dmx_channel + required_channels - 1
    
    if end_channel <= 512:
        return  # No overflow
    
    # Calculate split
    available_channels = 512 - dmx_channel + 1
    points_that_fit = available_channels // 3
    remaining_points = points - points_that_fit
    
    # Update original strip
    strip_data["points"] = points_that_fit
    
    # Create overflow strip
    overflow_strip = strip_data.copy()
    overflow_strip["label"] += ".5"
    overflow_strip["points"] = remaining_points
    overflow_strip["universe"] = universe + 1  # Next universe
    overflow_strip["dmxChannel"] = 1           # Start of universe
    overflow_strip["reverse"] = not strip_data["reverse"]  # Zigzag continuation
    
    return overflow_strip
```

## Common Patterns

### Reverse Parameter Logic
For zigzag wiring patterns:
- If original strip has `reverse: true`, overflow strip gets `reverse: false`
- If original strip has `reverse: false`, overflow strip gets `reverse: true`
- This maintains the continuous zigzag pattern across the split

### Universe Assignment
- Always assign overflow strips to the **next available universe**
- Start overflow strips at **DMX channel 1**
- Check for universe conflicts with other strips

## Strips Fixed in This Project

The following strips were successfully split using this method:

| Original Strip | LEDs | Split Into | Universe Assignment |
|---------------|------|------------|-------------------|
| Strip 4 | 67 | Strip 4 (43) + Strip 4.5 (24) | Universe 0 + 1 |
| Strip 6 | 85 | Strip 6 (71) + Strip 6.5 (14) | Universe 1 + 2 |
| Strip 8 | 99 | Strip 8 (63) + Strip 8.5 (36) | Universe 2 + 3 |
| Strip 10 | 113 | Strip 10 (27) + Strip 10.5 (86) | Universe 3 + 4 |
| Strip 11 | 119 | Strip 11 (84) + Strip 11.5 (35) | Universe 4 + 5 |
| Strip 14 | 131 | Strip 14 (43) + Strip 14.5 (88) | Universe 6 + 7 |
| Strip 15 | 135 | Strip 15 (82) + Strip 15.5 (53) | Universe 7 + 8 |
| Strip 16 | 137 | Strip 16 (117) + Strip 16.5 (20) | Universe 8 + 9 |
| Strip 18 | 143 | Strip 18 (9) + Strip 18.5 (134) | Universe 9 + 10 |
| Strip 19 | 145 | Strip 19 (36) + Strip 19.5 (109) | Universe 10 + 11 |
| Strip 21 | 151 | Strip 21 (21) + Strip 21.5 (130) | Universe 12 + 13 |
| Strip 22 | 153 | Strip 22 (40) + Strip 22.5 (113) | Universe 13 + 14 |
| Strip 23 | 155 | Strip 23 (57) + Strip 23.5 (98) | Universe 14 + 15 |
| Strip 24 | 157 | Strip 24 (72) + Strip 24.5 (85) | Universe 15 + 16 |
| Strip 25 | 157 | Strip 25 (85) + Strip 25.5 (72) | Universe 16 + 17 |
| Strip 27 | 161 | Strip 27 (11) + Strip 27.5 (150) | Universe 18 + 19 |
| Strip 28 | 161 | Strip 28 (20) + Strip 28.5 (141) | Universe 19 + 20 |
| Strip 29 | 163 | Strip 29 (29) + Strip 29.5 (134) | Universe 20 + 21 |
| Strip 30 | 163 | Strip 30 (36) + Strip 30.5 (127) | Universe 21 + 22 |
| Strip 31 | 163 | Strip 31 (43) + Strip 31.5 (120) | Universe 22 + 23 |
| Strip 33 | 163 | Strip 33 (7) + Strip 33.5 (156) | Universe 24 + 25 |
| Strip 34 | 163 | Strip 34 (14) + Strip 34.5 (149) | Universe 25 + 26 |
| Strip 35 | 163 | Strip 35 (21) + Strip 35.5 (142) | Universe 26 + 27 |
| Strip 36 | 163 | Strip 36 (28) + Strip 36.5 (135) | Universe 27 + 28 |
| Strip 37 | 163 | Strip 37 (35) + Strip 37.5 (128) | Universe 28 + 29 |
| Strip 39 | 161 | Strip 39 (7) + Strip 39.5 (154) | Universe 30 + 31 |
| Strip 40 | 161 | Strip 40 (16) + Strip 40.5 (145) | Universe 31 + 32 |

## Tools and Scripts

### Manual Fix Script Template
```python
def fix_specific_strip(strip_number, original_points, universe, dmx_channel):
    """
    Template for manual strip fixing
    Adapt the values based on your specific strip configuration
    """
    available_channels = 512 - dmx_channel + 1
    points_that_fit = available_channels // 3
    remaining_points = original_points - points_that_fit
    
    print(f"Strip {strip_number}:")
    print(f"  Original: {original_points} LEDs")
    print(f"  Split into: {points_that_fit} + {remaining_points} LEDs")
    print(f"  Universe: {universe} + {universe + 1}")
    
    return points_that_fit, remaining_points
```

## Best Practices

1. **Always verify math**: Original LEDs = Split LEDs + Overflow LEDs
2. **Test on hardware**: Software preview doesn't show overflow issues
3. **Document changes**: Keep track of which strips were split
4. **Use consistent naming**: Original strip + .5 for overflow
5. **Maintain zigzag patterns**: Alternate reverse parameters correctly
6. **Check universe availability**: Ensure new universes don't conflict

## Troubleshooting

### Still seeing split colors after fix?
- Verify both strips are using correct universes
- Check DMX channel assignments don't overlap
- Confirm reverse parameters for zigzag wiring
- Restart LED controllers to clear cache

### Colors wrong across entire strip?
- Check byte order configuration (RGB, GRB, RBG, etc.)
- Verify host IP addresses are correct
- Confirm ArtNet sequence settings

### Controller not responding?
- Check network connectivity to controller IP
- Verify universe numbers match controller config
- Confirm ArtNet port (usually 6454)

## Related Issues Fixed

- **Color Mapping**: Updated all strips to use RBG byte order via master PacmanStrip fixture
- **Host Configuration**: Changed all strips from 127.0.0.1 to 10.1.1.47
- **ArtNet Collisions**: Resolved universe conflicts through systematic splitting

---

*This documentation was created during the Pacman LED display setup project. All strip splits were successfully tested and verified on physical hardware.*
