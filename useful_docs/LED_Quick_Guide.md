# LED Quick Guide

A quick reference for LED strip configuration and technical concepts in LX Studio/Chromatik.

## DMX Channel Basics

### Universe Limits
- Each **DMX universe** supports a maximum of **512 channels**
- **ArtNet protocol** sends data in these 512-channel universes
- When you exceed 512 channels, you **must** move to the next universe

### LED Channel Usage
- Each LED uses **3 DMX channels** (Red, Green, Blue)
- **Maximum LEDs per universe**: 170 LEDs (170 × 3 = 510 channels)
- **Channel 511-512**: Usually left unused to avoid overflow issues

### Real Example: Pacman Strip Configuration
```
Strip 1: 25 LEDs, Universe 0, Channels 1-75
Strip 2: 45 LEDs, Universe 0, Channels 76-210  
Strip 3: 57 LEDs, Universe 0, Channels 211-381
Strip 4: 43 LEDs, Universe 0, Channels 382-510 ✅ (fits!)

But if Strip 4 had 67 LEDs:
Strip 4: 67 LEDs would need Channels 382-582 ❌ (582 > 512!)
Solution - Split it:
Strip 4:   43 LEDs, Universe 0, Channels 382-510 ✅
Strip 4.5: 24 LEDs, Universe 1, Channels 1-72   ✅
```

## Strip Configuration

### Starting Positions
- **Universe**: Which DMX universe (0, 1, 2, etc.)
- **DMX Channel**: Starting channel within that universe (1-512)
- **LED Count**: How many LEDs in the strip

### Overflow Splitting
When a strip needs more than 512 channels:
```
Problem:  Strip 4 with 67 LEDs starting at channel 382
          Needs channels 382-582 (67 × 3 = 201 channels)
          But 582 > 512! ❌

Solution: Split Strip 4:
          Strip 4:   43 LEDs, Universe 0, Channels 382-510 ✅
          Strip 4.5: 24 LEDs, Universe 1, Channels 1-72   ✅
          Total: 43 + 24 = 67 LEDs (same as original)
```

## Coordinate System

### X, Y, Z Positioning
- **X**: Left/Right position (positive = right)
- **Y**: Forward/Back position (positive = forward)  
- **Z**: Up/Down position (positive = up)
- **Units**: Usually in inches or centimeters

### Strip Start Points
- Coordinates represent the **starting point** of the strip
- Always positioned at the **left side** of the strip
- Data flow direction is controlled by `reverse` setting

### Example Layout (Pacman Strips)
```
Y (forward)
^
|     Strip 2: 45 LEDs (reverse: false) ──────────►
|     x=64, y=8      data flows left→right
|
|     ◄────────── Strip 1: 25 LEDs (reverse: true)  
|                   x=71, y=6      data flows right→left
└─────────────────────────────────────────► X (right)
```

## LED Spacing

### Physical Distance
- **Standard spacing**: 0.8 units between LED centers
- **Strip length calculation**: (LED count - 1) × 0.8
- **Example**: 50 LEDs = 49 × 0.8 = 39.2 units long

### Spacing in Practice
```
LED positions in a 5-LED strip starting at x=10:
LED 1: x=10.0
LED 2: x=10.8
LED 3: x=11.6
LED 4: x=12.4
LED 5: x=13.2
Total length: 3.2 units
```

### Why 0.8?
- Matches common LED strip densities
- Provides realistic visual spacing
- Compatible with standard 60 LEDs/meter strips

## Data Flow Direction

### Reverse Setting
- **reverse: false**: Data flows left→right (normal)
- **reverse: true**: Data flows right→left (reversed)

### Zigzag Wiring Pattern
Common in large installations:
```
Strip 1 (odd):  reverse: true  ──► ◄──┐
Strip 2 (even): reverse: false ──────►┘  
Strip 3 (odd):  reverse: true  ──► ◄──┐
Strip 4 (even): reverse: false ──────►┘
```

Benefits:
- **Simpler wiring**: Each strip connects to the next
- **Shorter cables**: No long runs back to controllers
- **Better signal integrity**: Shorter distances

## Common Calculations

### Channel Math
```python
# Calculate ending channel
start_channel = 100
led_count = 75
end_channel = start_channel + (led_count * 3) - 1
# Result: 100 + 225 - 1 = 324

# Check if overflow
if end_channel > 512:
    # Need to split or move to next universe
```

### Strip Length
```python
# Calculate physical length
led_count = 50
spacing = 0.8
length = (led_count - 1) * spacing
# Result: 49 * 0.8 = 39.2 units
```

### Split Calculation
```python
# How many LEDs fit in remaining channels?
channels_left = 512 - start_channel + 1
max_leds = channels_left // 3
# If strip has more LEDs, split is needed
```

## Troubleshooting

### "Split Colors" Issue
**Problem**: LEDs show different colors in middle of strip
**Cause**: Universe overflow (strip spans 2 universes)
**Solution**: Split the strip at the universe boundary

### Wrong Data Flow
**Problem**: Patterns appear backwards on strip
**Cause**: Incorrect `reverse` setting
**Solution**: Check zigzag pattern, fix reverse value

### Missing LEDs
**Problem**: Part of strip doesn't light up
**Cause**: DMX channel collision or overflow
**Solution**: Check channel assignments, fix overlaps

## Best Practices

### Planning Strips
1. **Map your layout**: Know physical positions first
2. **Calculate channels**: Ensure no universe overflows
3. **Plan data flow**: Use zigzag for efficient wiring
4. **Label clearly**: Use consistent naming (Strip 1, Strip 2, etc.)

### Configuration Workflow
1. **Set positions**: X, Y, Z coordinates
2. **Assign universes**: Start with universe 0
3. **Calculate channels**: 3 channels per LED
4. **Set reverse**: Follow zigzag pattern
5. **Test patterns**: Verify correct flow and colors

### File Management
1. **Backup before changes**: Always save working versions
2. **Test incrementally**: Don't change everything at once
3. **Document splits**: Note which strips were split and why
4. **Version control**: Track major configuration changes

This guide covers the essential concepts for working with LED strips in LX Studio. For detailed procedures, see the specific utility script documentation.
