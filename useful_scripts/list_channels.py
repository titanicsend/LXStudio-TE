#!/usr/bin/env python3
"""
List all channels in an LX Studio project file
"""

import json
import sys
import os

def main():
    if len(sys.argv) != 2:
        print("Usage: python list_channels.py <project_file>")
        sys.exit(1)
    
    project_file = sys.argv[1]
    
    if not os.path.exists(project_file):
        print(f"Error: Project file '{project_file}' not found!")
        sys.exit(1)
    
    try:
        with open(project_file, 'r') as f:
            data = json.load(f)
    except Exception as e:
        print(f"Error reading project file: {e}")
        sys.exit(1)
    
    engine = data.get('engine', {})
    mixer = engine.get('children', {}).get('mixer', {})
    channels = mixer.get('channels', [])
    
    print(f"Found {len(channels)} channels:")
    print("-" * 50)
    
    for i, channel_data in enumerate(channels):
        if isinstance(channel_data, dict):
            label = channel_data.get('parameters', {}).get('label', 'Unknown')
            print(f"{i+1}. {label}")
            
            # Also show patterns in this channel
            patterns = channel_data.get('patterns', [])
            if patterns:
                print(f"   Patterns ({len(patterns)}):")
                for j, pattern_data in enumerate(patterns):
                    if isinstance(pattern_data, dict):
                        pattern_label = pattern_data.get('parameters', {}).get('label', 'Unknown')
                        print(f"     - {pattern_label}")
                print()

if __name__ == "__main__":
    main()
