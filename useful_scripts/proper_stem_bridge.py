#!/usr/bin/env python3
"""
PROPER Audio Separator Bridge using audio-separator 0.35.2
Uses htdemucs.yaml for real 4-stem separation: vocals, drums, bass, other
FORCED CPU-ONLY for MacBook Air compatibility
"""

import os
import torch

# FORCE CPU ONLY - No GPU/MPS acceleration (MacBook Air compatibility)
os.environ['CUDA_VISIBLE_DEVICES'] = ''
os.environ['PYTORCH_ENABLE_MPS_FALLBACK'] = '1'
torch.set_default_device('cpu')

import numpy as np
import sounddevice as sd
from pythonosc import udp_client
import threading
import time
from collections import deque
from audio_separator.separator import Separator
import tempfile
import os
import soundfile as sf

class ProperStemBridge:
    def __init__(self, 
                 input_device="BlackHole 2ch",
                 osc_host="127.0.0.1", 
                 osc_port=3030,
                 sample_rate=44100,
                 buffer_size=1024,
                 processing_interval=4.0,  # Process every 4 seconds (configurable)
                 energy_boost=50):  # Energy multiplier (configurable)
        
        self.sample_rate = sample_rate
        self.buffer_size = buffer_size
        self.processing_interval = processing_interval
        self.energy_boost = energy_boost
        self.osc_client = udp_client.SimpleUDPClient(osc_host, osc_port)
        
        # Initialize the AI separator with proper 0.35.2 API - FORCE CPU ONLY
        print("🤖 Loading PROPER AI stem separation...")
        import torch
        import os
        
        # Force CPU usage only (no GPU/MPS acceleration)
        os.environ['CUDA_VISIBLE_DEVICES'] = ''
        torch.backends.mps.is_available = lambda: False
        
        self.separator = Separator(
            output_format='WAV',
            output_dir=tempfile.gettempdir(),
            sample_rate=sample_rate,
            # Optimized for SPEED over accuracy
            mdx_params={"batch_size": 1},
            vr_params={"batch_size": 1},
            demucs_params={"segment_size": "Default", "shifts": 1},  # Back to working settings
            mdxc_params={"batch_size": 1}
        )
        
        # Load the Demucs 4-stem model from the docs
        print("📥 Loading htdemucs.yaml (4-stem: vocals, drums, bass, other)...")
        try:
            self.separator.load_model(model_filename='htdemucs.yaml')
            print("✅ Demucs 4-stem model loaded successfully!")
        except Exception as e:
            print(f"❌ Failed to load model: {e}")
            raise e
        
        # Audio buffer for processing
        self.audio_buffer = deque(maxlen=sample_rate * int(processing_interval))
        
        # Stem levels (smoothed)
        self.stem_levels = {
            'vocals': 0.0,
            'drums': 0.0, 
            'bass': 0.0,
            'other': 0.0
        }
        
        # Setup audio input
        self.setup_audio_input(input_device)
        
        # Processing thread
        self.processing = True
        self.process_thread = threading.Thread(target=self.process_stems)
        self.process_thread.daemon = True
        
    def setup_audio_input(self, device_name):
        """Setup audio input device"""
        devices = sd.query_devices()
        device_id = None
        
        for i, device in enumerate(devices):
            if device_name.lower() in device['name'].lower():
                device_id = i
                break
                
        if device_id is None:
            print(f"❌ Device '{device_name}' not found. Available devices:")
            for i, device in enumerate(devices):
                print(f"  {i}: {device['name']}")
            return False
            
        print(f"🎧 Using audio device: {devices[device_id]['name']}")
        
        # Start audio stream
        self.stream = sd.InputStream(
            device=device_id,
            channels=2,
            samplerate=self.sample_rate,
            blocksize=self.buffer_size,
            callback=self.audio_callback
        )
        
        return True
        
    def audio_callback(self, indata, frames, time, status):
        """Audio input callback"""
        if status:
            print(f"Audio status: {status}")
            
        # Convert to mono and add to buffer
        mono_audio = np.mean(indata, axis=1)
        self.audio_buffer.extend(mono_audio)
        
    def process_stems(self):
        """Process audio buffer into stems using proper AI"""
        print("🎵 Starting PROPER AI stem processing thread...")
        
        while self.processing:
            if len(self.audio_buffer) >= self.sample_rate * (self.processing_interval * 0.8):
                try:
                    # Get audio data
                    audio_data = np.array(list(self.audio_buffer))
                    
                    # Create temporary file
                    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_file:
                        temp_path = temp_file.name
                    
                    try:
                        # Write audio to temp file
                        sf.write(temp_path, audio_data, self.sample_rate)
                        
                        print(f"🤖 AI processing {len(audio_data)/self.sample_rate:.1f}s of audio...")
                        print("  🔬 Running Demucs AI separation (faster with smaller samples)...")
                        
                        # Run AI separation
                        output_files = self.separator.separate(temp_path)
                        
                        print(f"  ✅ Got {len(output_files)} separated files!")
                        

                        
                        # Process each stem
                        stem_energies = {
                            'vocals': 0.0,
                            'drums': 0.0,
                            'bass': 0.0,
                            'other': 0.0
                        }
                        
                        for file_path in output_files:
                            # Fix the file path - audio-separator returns relative paths, need to add temp dir
                            if not os.path.isabs(file_path):
                                file_path = os.path.join(tempfile.gettempdir(), file_path)
                            
                            filename = os.path.basename(file_path)
                            
                            # Load separated audio with error handling
                            try:
                                audio, sr = sf.read(file_path)
                                if len(audio.shape) > 1:
                                    audio = np.mean(audio, axis=1)  # Convert to mono
                            except Exception as read_error:
                                print(f"  ⚠️  Could not read {filename}: {read_error}")
                                continue
                            
                            # Calculate RMS energy with configurable boost
                            rms = np.sqrt(np.mean(audio**2))
                            energy = np.clip(rms * self.energy_boost, 0.0, 1.0)
                            
                            # Map filename to stem type (case insensitive)
                            filename_lower = filename.lower()
                            if 'vocals' in filename_lower:
                                stem_energies['vocals'] = energy
                            elif 'drums' in filename_lower:
                                stem_energies['drums'] = energy
                            elif 'bass' in filename_lower:
                                stem_energies['bass'] = energy
                            elif 'other' in filename_lower:
                                stem_energies['other'] = energy
                            
                            # Clean up
                            try:
                                os.unlink(file_path)
                            except:
                                pass
                            
                    finally:
                        # Clean up temp file
                        try:
                            os.unlink(temp_path)
                        except:
                            pass
                    
                    # Update stem levels with smoothing
                    alpha = 0.7  # Smoothing factor
                    for stem_name in ['vocals', 'drums', 'bass', 'other']:
                        if stem_name in stem_energies:
                            self.stem_levels[stem_name] = (
                                alpha * stem_energies[stem_name] + 
                                (1 - alpha) * self.stem_levels[stem_name]
                            )
                    
                    # Send OSC messages (same format as TE's VJLab)
                    self.send_osc_updates()
                    
                    print(f"🎛️  Vocals: {self.stem_levels['vocals']:.2f}, "
                          f"Drums: {self.stem_levels['drums']:.2f}, "
                          f"Bass: {self.stem_levels['bass']:.2f}, "
                          f"Other: {self.stem_levels['other']:.2f}")
                    
                except Exception as e:
                    print(f"❌ Error processing stems: {e}")
                    
            time.sleep(1)  # Check every second
    
    def send_osc_updates(self):
        """Send OSC messages to Chromatik (same format as VJLab)"""
        try:
            self.osc_client.send_message("/te/stem/vocals", self.stem_levels['vocals'])
            self.osc_client.send_message("/te/stem/drums", self.stem_levels['drums'])
            self.osc_client.send_message("/te/stem/bass", self.stem_levels['bass'])
            self.osc_client.send_message("/te/stem/other", self.stem_levels['other'])
        except Exception as e:
            print(f"❌ OSC error: {e}")
    
    def start(self):
        """Start the stem bridge"""
        print("🚀 Starting FASTER AI Stem Bridge...")
        print("   Lower sample rate for faster processing!")
        print(f"   Processing every {self.processing_interval}s at {self.sample_rate}Hz with {self.energy_boost}x boost")
        print("   OSC messages: /te/stem/vocals, /te/stem/drums, /te/stem/bass, /te/stem/other")
        print("   Press Ctrl+C to stop")
        
        self.stream.start()
        self.process_thread.start()
        
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n🛑 Stopping...")
            self.stop()
    
    def stop(self):
        """Stop the stem bridge"""
        self.processing = False
        if hasattr(self, 'stream'):
            self.stream.stop()
            self.stream.close()

if __name__ == "__main__":
    # You can customize these parameters:
    # processing_interval: how often to analyze (in seconds) - smaller = more frequent
    # energy_boost: multiplier for sensitivity - higher = more sensitive
    # sample_rate: audio quality - lower = faster processing
    
    bridge = ProperStemBridge(
        processing_interval=6.0,  # Keep 8 seconds - this was stable
        energy_boost=75,          # Slightly higher boost to compensate for lower quality
        sample_rate=16000         # Half sample rate = much faster processing
    )
    bridge.start()
