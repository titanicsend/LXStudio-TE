#!/usr/bin/env python3
"""
Simple Pattern Copy Script for LX Studio Projects
================================================

Copies pattern parameters AND modulation components from a source pattern to a target pattern.
This preserves the target pattern's ID and copies modulation components without generating new IDs.

Usage:
    python copy_pattern_simple.py <project_file> <source_channel> <target_channel> <source_pattern_name> <target_pattern_name> [source_channel_index]

Examples:
    python copy_pattern_simple.py BM2024_Pacman.lxp "HIGH" "Pacman High" "+ Phasers" "Phasers" 2
    python copy_pattern_simple.py BM2024_Pacman.lxp "LOW" "Pacman Low" "Electric" "Electric"
    python copy_pattern_simple.py BM2024_Pacman.lxp "EDGES" "Pacman Edge" "ArcEdges" "ArcEdges"

The script will:
1. Find the source pattern by name in the source channel (or specific channel index if provided)
2. Find the target pattern by name in the target channel
3. Copy parameter values from source to target
4. Copy modulation components (modulators, modulations, triggers) without changing IDs
5. Preserve target pattern's main ID and structure
6. Save the modified project file

Note: If there are multiple channels with the same name, use the optional source_channel_index parameter
to specify which one (1-based index, e.g., 2 for the second "HIGH" channel).
"""

import json
import sys
import os

def find_channel_by_label(data, channel_label, channel_index=None):
    """Find a channel by its label in the mixer structure, optionally with index for duplicates"""
    engine = data.get('engine', {})
    mixer = engine.get('children', {}).get('mixer', {})
    channels = mixer.get('channels', [])  # Channels is a list
    
    found_channels = []
    for i, channel_data in enumerate(channels):
        if isinstance(channel_data, dict) and channel_data.get('parameters', {}).get('label') == channel_label:
            found_channels.append((i, channel_data))
    
    if not found_channels:
        return None, None
    
    if channel_index is not None:
        # Use 1-based index
        if 1 <= channel_index <= len(found_channels):
            return found_channels[channel_index - 1]
        else:
            print(f"❌ Error: Channel index {channel_index} out of range. Found {len(found_channels)} channels named '{channel_label}'")
            return None, None
    
    if len(found_channels) > 1:
        print(f"⚠️  Warning: Found {len(found_channels)} channels named '{channel_label}'. Using the first one.")
        print(f"   Available indices: 1-{len(found_channels)}")
        print(f"   Use: python copy_pattern_simple.py <project_file> <source_channel> <target_channel> <source_pattern_name> <target_pattern_name> <channel_index>")
    
    return found_channels[0]

def find_pattern_by_name(channel_data, pattern_name):
    """Find a pattern by name within a channel"""
    patterns = channel_data.get('patterns', [])  # Patterns is a list
    
    for i, pattern_data in enumerate(patterns):
        if isinstance(pattern_data, dict) and pattern_data.get('parameters', {}).get('label') == pattern_name:
            return i, pattern_data
    
    return None, None

def copy_parameters(source_params, target_params):
    """Copy parameter values from source to target, preserving target structure"""
    copied_count = 0
    
    for param_name, param_value in source_params.items():
        # Skip certain parameters that should not be copied
        if param_name in ['label', 'id', 'class']:
            continue
            
        # Copy the parameter value
        if param_name in target_params:
            target_params[param_name] = param_value
            copied_count += 1
    
    return copied_count

def copy_entire_pattern_structure(source_pattern, target_pattern):
    """Copy the entire pattern structure except for ID and parameters"""
    # Store the target's ID and parameters
    target_id = target_pattern.get('id')
    target_params = target_pattern.get('parameters', {})
    
    # Copy everything from source to target
    for key, value in source_pattern.items():
        if key not in ['id', 'parameters']:  # Don't copy ID or parameters
            target_pattern[key] = json.loads(json.dumps(value))
    
    # Restore target's ID and parameters
    target_pattern['id'] = target_id
    target_pattern['parameters'] = target_params

def main():
    if len(sys.argv) < 6 or len(sys.argv) > 7:
        print("❌ Usage: python copy_pattern_simple.py <project_file> <source_channel> <target_channel> <source_pattern_name> <target_pattern_name> [source_channel_index]")
        print("\nExamples:")
        print("  python copy_pattern_simple.py BM2024_Pacman.lxp \"HIGH\" \"Pacman High\" \"+ Phasers\" \"Phasers\" 2")
        print("  python copy_pattern_simple.py BM2024_Pacman.lxp \"LOW\" \"Pacman Low\" \"Electric\" \"Electric\"")
        print("  python copy_pattern_simple.py BM2024_Pacman.lxp \"EDGES\" \"Pacman Edge\" \"ArcEdges\" \"ArcEdges\"")
        print("\nNote: Use channel_index when there are multiple channels with the same name")
        sys.exit(1)
    
    project_file = sys.argv[1]
    source_channel_label = sys.argv[2]
    target_channel_label = sys.argv[3]
    source_pattern_name = sys.argv[4]
    target_pattern_name = sys.argv[5]
    source_channel_index = int(sys.argv[6]) if len(sys.argv) == 7 else None
    
    # Validate project file exists
    if not os.path.exists(project_file):
        print(f"❌ Error: Project file '{project_file}' not found!")
        sys.exit(1)
    
    print(f"🎯 Copying pattern '{source_pattern_name}' from '{source_channel_label}' to '{target_pattern_name}' in '{target_channel_label}'...")
    if source_channel_index:
        print(f"   Using {source_channel_label} channel #{source_channel_index}")
    
    # Read project file
    try:
        with open(project_file, 'r') as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"❌ Error: Invalid JSON in project file: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error reading project file: {e}")
        sys.exit(1)
    
    # Find source channel and pattern
    source_channel_id, source_channel_data = find_channel_by_label(data, source_channel_label, source_channel_index)
    if not source_channel_data:
        print(f"❌ Error: Source channel '{source_channel_label}' not found!")
        print("\nAvailable channels:")
        engine = data.get('engine', {})
        mixer = engine.get('children', {}).get('mixer', {})
        channels = mixer.get('channels', [])
        for i, channel_data in enumerate(channels):
            if isinstance(channel_data, dict):
                label = channel_data.get('parameters', {}).get('label', 'Unknown')
                print(f"  - {label}")
        sys.exit(1)
    
    source_pattern_id, source_pattern_data = find_pattern_by_name(source_channel_data, source_pattern_name)
    if not source_pattern_data:
        print(f"❌ Error: Pattern '{source_pattern_name}' not found in source channel '{source_channel_label}'!")
        print(f"\nAvailable patterns in '{source_channel_label}':")
        patterns = source_channel_data.get('patterns', [])
        for i, pattern_data in enumerate(patterns):
            if isinstance(pattern_data, dict):
                label = pattern_data.get('parameters', {}).get('label', 'Unknown')
                print(f"  - {label}")
        sys.exit(1)
    
    # Find target channel and pattern
    target_channel_id, target_channel_data = find_channel_by_label(data, target_channel_label)
    if not target_channel_data:
        print(f"❌ Error: Target channel '{target_channel_label}' not found!")
        print("\nAvailable channels:")
        engine = data.get('engine', {})
        mixer = engine.get('children', {}).get('mixer', {})
        channels = mixer.get('channels', [])
        for i, channel_data in enumerate(channels):
            if isinstance(channel_data, dict):
                label = channel_data.get('parameters', {}).get('label', 'Unknown')
                print(f"  - {label}")
        sys.exit(1)
    
    target_pattern_id, target_pattern_data = find_pattern_by_name(target_channel_data, target_pattern_name)
    if not target_pattern_data:
        print(f"❌ Error: Pattern '{target_pattern_name}' not found in target channel '{target_channel_label}'!")
        print(f"\nAvailable patterns in '{target_channel_label}':")
        patterns = target_channel_data.get('patterns', [])
        for i, pattern_data in enumerate(patterns):
            if isinstance(pattern_data, dict):
                label = pattern_data.get('parameters', {}).get('label', 'Unknown')
                print(f"  - {label}")
        sys.exit(1)
    
    # Copy parameters
    source_params = source_pattern_data.get('parameters', {})
    target_params = target_pattern_data.get('parameters', {})
    
    copied_params = copy_parameters(source_params, target_params)
    
    # Copy the entire pattern structure (including audio stems)
    copy_entire_pattern_structure(source_pattern_data, target_pattern_data)
    
    # Update the data structure
    engine = data.get('engine', {})
    mixer = engine.get('children', {}).get('mixer', {})
    channels = mixer.get('channels', [])
    channels[target_channel_id] = target_channel_data
    
    # Create backup
    backup_file = f"{project_file}.backup"
    try:
        with open(backup_file, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"   Created backup: {backup_file}")
    except Exception as e:
        print(f"⚠️  Warning: Could not create backup: {e}")
    
    # Save modified project file
    try:
        with open(project_file, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"✅ Successfully copied pattern '{source_pattern_name}' to '{target_pattern_name}'")
        print(f"   From: {source_channel_label} (pattern ID: {source_pattern_data.get('id')})")
        print(f"   To: {target_channel_label} (pattern ID: {target_pattern_data.get('id')})")
        print(f"   Copied {copied_params} parameters + all components")
        print(f"   Updated project file: {project_file}")
    except Exception as e:
        print(f"❌ Error saving project file: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
