#!/usr/bin/env python3
"""
Debug script to examine LX project file structure
"""

import json
import sys

def explore_structure(data, path="", max_depth=3, current_depth=0):
    """Recursively explore the data structure"""
    if current_depth >= max_depth:
        return
    
    if isinstance(data, dict):
        for key, value in data.items():
            current_path = f"{path}.{key}" if path else key
            
            # Look for interesting keys
            if any(interesting in key.lower() for interesting in ['channel', 'pattern', 'mixer', 'fireflies', 'high']):
                print(f"🔍 Found interesting key: {current_path}")
                if isinstance(value, dict):
                    print(f"   Keys: {list(value.keys())}")
                    if 'label' in value:
                        print(f"   Label: {value.get('label', 'N/A')}")
                elif isinstance(value, list):
                    print(f"   List length: {len(value)}")
                else:
                    print(f"   Value type: {type(value)}")
            
            # Recursively explore
            if isinstance(value, (dict, list)) and current_depth < max_depth - 1:
                explore_structure(value, current_path, max_depth, current_depth + 1)
    
    elif isinstance(data, list):
        for i, item in enumerate(data[:5]):  # Only look at first 5 items
            current_path = f"{path}[{i}]"
            if isinstance(item, (dict, list)) and current_depth < max_depth - 1:
                explore_structure(item, current_path, max_depth, current_depth + 1)

def main():
    if len(sys.argv) != 2:
        print("Usage: python debug_project_structure.py <project_file>")
        sys.exit(1)
    
    project_file = sys.argv[1]
    
    try:
        with open(project_file, 'r') as f:
            data = json.load(f)
    except Exception as e:
        print(f"Error reading file: {e}")
        sys.exit(1)
    
    print("Top-level keys:", list(data.keys()))
    print("\nExploring structure for interesting keys...")
    print("=" * 50)
    
    explore_structure(data)
    
    # Also look specifically for mixer structure
    print("\n" + "=" * 50)
    print("Looking for mixer structure specifically...")
    
    engine = data.get('engine', {})
    if 'mixer' in engine.get('children', {}):
        mixer = engine['children']['mixer']
        print("✅ Found mixer!")
        print("Mixer keys:", list(mixer.keys()))
        
        if 'children' in mixer:
            children = mixer['children']
            print("Mixer children keys:", list(children.keys()))
            
            # Check if channels are directly under mixer
            if 'channels' in mixer:
                channels = mixer['channels']
                print("✅ Found channels directly under mixer!")
                print(f"Channels is a list with {len(channels)} items")
                
                # Look for HIGH channel in the list
                for i, channel_data in enumerate(channels):
                    if isinstance(channel_data, dict):
                        label = channel_data.get('parameters', {}).get('label', 'Unknown')
                        print(f"  Channel {i}: {label}")
                        
                        if label == 'HIGH':
                            print(f"  ✅ Found HIGH channel!")
                            patterns = channel_data.get('children', {}).get('patterns', {}).get('children', {})
                            print(f"  Number of patterns: {len(patterns)}")
                            
                            for pattern_id, pattern_data in patterns.items():
                                pattern_label = pattern_data.get('parameters', {}).get('label', 'Unknown')
                                print(f"    Pattern {pattern_id}: {pattern_label}")
                                
                                if pattern_label == 'Fireflies':
                                    print(f"    ✅ Found Fireflies pattern!")
                                    return
            else:
                print("❌ No 'channels' key found directly under mixer")
                print("Available keys under mixer:", list(mixer.keys()))
    else:
        print("❌ No 'mixer' key found in engine.children")

if __name__ == "__main__":
    main()
