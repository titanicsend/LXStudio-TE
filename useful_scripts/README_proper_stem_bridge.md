# Proper Stem Bridge

> **Note**: This script has been replaced by VJLab for the main Titanic's End project, but it's still a good alternative if you just want to get started with real-time audio stem separation for LED control.

A real-time audio separator bridge that uses AI to split audio into 4 stems (vocals, drums, bass, other) and sends the levels via OSC to control LED patterns.

## Overview

This script captures audio from your system, uses the `audio-separator` library with the htdemucs AI model to separate audio into individual stems, and sends the energy levels to Chromatik/LX Studio via OSC messages. It's optimized for MacBook Air compatibility with CPU-only processing.

## Features

- **Real AI separation**: Uses htdemucs.yaml model for true 4-stem separation
- **CPU-only processing**: Optimized for MacBook Air (no GPU required)
- **Real-time OSC output**: Sends stem levels to Chromatik
- **Configurable processing**: Adjustable interval and sensitivity
- **BlackHole integration**: Works with BlackHole for system audio capture

## Requirements

Install the required Python packages:

```bash
pip install -r requirements_stem_bridge.txt
```

The requirements file includes:
- `audio-separator[cpu]==0.35.2` - AI stem separation
- `sounddevice` - Audio input capture
- `python-osc` - OSC communication
- `soundfile` - Audio file handling
- `numpy` - Audio processing

## Usage

### Basic usage
```bash
python3 proper_stem_bridge.py
```

### With custom parameters
Edit the script to adjust:
- `processing_interval`: How often to analyze audio (default: 6.0 seconds)
- `energy_boost`: Sensitivity multiplier (default: 75)
- `sample_rate`: Audio quality vs speed tradeoff (default: 16000 Hz)

## Setup Requirements

### 1. Install BlackHole
Download and install [BlackHole 2ch](https://github.com/ExistentialAudio/BlackHole) for system audio capture.

### 2. Configure Audio Routing
Set up your system to route audio through BlackHole so the script can capture it.

### 3. Configure Chromatik/LX Studio
Set up OSC input on port 3030 to receive the stem data.

## OSC Output

The script sends these OSC messages:
- `/te/stem/vocals` - Vocal energy level (0.0-1.0)
- `/te/stem/drums` - Drum energy level (0.0-1.0)
- `/te/stem/bass` - Bass energy level (0.0-1.0)
- `/te/stem/other` - Other instruments energy level (0.0-1.0)

## Performance Optimization

The script is optimized for real-time performance:

- **CPU-only processing**: Forces CPU mode for MacBook Air compatibility
- **Lower sample rate**: 16kHz instead of 44.1kHz for faster processing
- **Batch processing**: Processes audio in chunks rather than continuously
- **Memory management**: Automatic cleanup of temporary files
- **Configurable intervals**: Balance between responsiveness and CPU usage

## Example Output

```
🤖 Loading PROPER AI stem separation...
📥 Loading htdemucs.yaml (4-stem: vocals, drums, bass, other)...
✅ Demucs 4-stem model loaded successfully!
🎧 Using audio device: BlackHole 2ch
🚀 Starting FASTER AI Stem Bridge...
   Processing every 6.0s at 16000Hz with 75x boost
   OSC messages: /te/stem/vocals, /te/stem/drums, /te/stem/bass, /te/stem/other
   Press Ctrl+C to stop

🤖 AI processing 6.0s of audio...
  🔬 Running Demucs AI separation...
  ✅ Got 4 separated files!
🎛️  Vocals: 0.23, Drums: 0.67, Bass: 0.45, Other: 0.12
```

## Configuration Options

### Processing Interval
```python
processing_interval=6.0  # Process every 6 seconds
```
- **Lower values**: More responsive, higher CPU usage
- **Higher values**: Less responsive, lower CPU usage

### Energy Boost
```python
energy_boost=75  # Sensitivity multiplier
```
- **Higher values**: More sensitive to quiet sounds
- **Lower values**: Only responds to loud sounds

### Sample Rate
```python
sample_rate=16000  # 16kHz for faster processing
```
- **Higher values**: Better quality, slower processing
- **Lower values**: Faster processing, lower quality

## Comparison with VJLab

| Feature | Proper Stem Bridge | VJLab |
|---------|-------------------|-------|
| **Setup** | Simple Python script | Full application |
| **Performance** | CPU-only, slower | GPU-optimized, faster |
| **Features** | Basic stem separation | Advanced audio analysis |
| **Integration** | OSC output only | Full Chromatik integration |
| **Use Case** | Quick start, testing | Production use |

## Troubleshooting

### Audio Device Not Found
```bash
❌ Device 'BlackHole 2ch' not found. Available devices:
  0: Built-in Microphone
  1: BlackHole 2ch
```
Make sure BlackHole is installed and your audio routing is configured.

### Slow Processing
- Reduce `sample_rate` (e.g., to 8000)
- Increase `processing_interval` (e.g., to 8.0)
- Close other CPU-intensive applications

### OSC Connection Issues
- Verify Chromatik is listening on port 3030
- Check firewall settings
- Ensure OSC client configuration matches

## Use Cases

### Quick Testing
Perfect for testing stem-reactive patterns without full VJLab setup:
```bash
python3 proper_stem_bridge.py
# Start your LED patterns in Chromatik
# Music will now control the LEDs via stems!
```

### Development
Good for pattern development when you need real stem data:
```bash
# Run stem bridge
python3 proper_stem_bridge.py

# In another terminal, monitor OSC
# Use OSC monitoring tools to see stem levels
```

### Educational
Great for understanding how stem separation works:
- Real AI processing you can observe
- Clear separation between vocals, drums, bass, other
- Immediate visual feedback via LED patterns

## Technical Details

- **AI Model**: Uses Facebook's Demucs htdemucs.yaml model
- **Processing**: Sliding window with configurable overlap
- **Output Format**: Normalized energy levels (0.0-1.0)
- **Smoothing**: Exponential smoothing to reduce jitter
- **Memory**: Automatic cleanup of temporary files

## Dependencies

- Python 3.7+
- BlackHole 2ch audio driver
- Sufficient CPU for real-time AI processing
- 1GB+ free disk space for AI model

This script provides a solid foundation for stem-based LED control and serves as a good stepping stone before moving to more advanced solutions like VJLab.
