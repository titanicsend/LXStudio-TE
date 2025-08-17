#!/usr/bin/env python3
"""
Mirror Strip Mapping Script
===========================

Generic script to mirror strip configurations between sides (A ↔ B).
Handles splits and coordinate mapping while preserving target side specifics.

Usage:
    python mirror_a_to_b_mapping.py <from_side> <to_side> [min_strip]

Arguments:
    from_side: Source side ('a' or 'b')
    to_side: Target side ('a' or 'b') 
    min_strip: Minimum strip number to process (optional, defaults to 1)

Examples:
    python mirror_a_to_b_mapping.py a b 44    # Mirror A→B for strips 44+
    python mirror_a_to_b_mapping.py b a 50    # Mirror B→A for strips 50+
    python mirror_a_to_b_mapping.py a b       # Mirror A→B for all strips
"""

import sys
import re
import os
import subprocess

def get_strip_data(content, side, strip_num, is_half=False):
    """Get strip data for a specific strip"""
    if side.upper() == 'A':
        if is_half:
            strip_label = f"Strip {strip_num}.5"
        else:
            strip_label = f"Strip {strip_num}"
    else:
        if is_half:
            strip_label = f"Strip {strip_num}.5b"
        else:
            strip_label = f"Strip {strip_num}b"
    
    # Use the working pattern from split_strip.py
    pattern = r'"jsonParameters": \{[^}]*"points": (\d+)[^}]*"universe": (\d+)[^}]*"dmxChannel": (\d+)[^}]*"reverse": (true|false)[^}]*\}.*?"parameters": \{[^}]*"label": "(Strip [0-9.]+(?:b)?)"[^}]*"x": ([0-9.-]+)[^}]*"y": ([0-9.-]+)[^}]*"z": ([0-9.-]+)[^}]*'
    matches = re.findall(pattern, content, re.DOTALL)
    
    for points, universe, dmx_channel, reverse, label, x, y, z in matches:
        if label == strip_label:
            return {
                'label': label,
                'points': int(points),
                'universe': int(universe),
                'dmx_channel': int(dmx_channel),
                'reverse': reverse == 'true',
                'x': float(x),
                'y': float(y),
                'z': float(z)
            }
    return None

def update_strip_coordinates(content, strip_label, new_x):
    """Update X coordinate for a specific strip - exact copy of working move_strip_side.py logic"""
    
    # Find all labels (same as move_strip_side.py)
    all_label_pattern = r'"label"\s*:\s*"([^"]+)"'
    all_matches = list(re.finditer(all_label_pattern, content))
    
    # Find the target strip
    strip_match = None
    for match in all_matches:
        if match.group(1) == strip_label:
            strip_match = match
            break
    
    if not strip_match:
        return content
    
    label_pos = strip_match.start()
    
    # Search backwards from the label to find the start of this fixture's parameters section
    params_start_pattern = r'"parameters"\s*:\s*{'
    params_matches = list(re.finditer(params_start_pattern, content[:label_pos]))
    
    if not params_matches:
        return content
        
    params_start = params_matches[-1].start()
    
    # Find the end of the parameters section by counting braces (exact copy from move_strip_side.py)
    brace_count = 0
    params_end = params_start
    in_string = False
    escape_next = False
    
    for i, char in enumerate(content[params_start:], params_start):
        if escape_next:
            escape_next = False
            continue
        
        if char == '\\':
            escape_next = True
            continue
        
        if char == '"' and not escape_next:
            in_string = not in_string
            continue
        
        if not in_string:
            if char == '{':
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0:
                    params_end = i + 1
                    break
    
    if params_end <= params_start:
        return content
    
    # Extract the parameters section
    params_section = content[params_start:params_end]
    
    # Find current coordinates (exact patterns from move_strip_side.py)
    x_pattern = r'"x"\s*:\s*([-\d.]+)'
    y_pattern = r'"y"\s*:\s*([-\d.]+)'
    z_pattern = r'"z"\s*:\s*([-\d.]+)'
    
    x_match = re.search(x_pattern, params_section)
    y_match = re.search(y_pattern, params_section)
    z_match = re.search(z_pattern, params_section)
    
    if not all([x_match, y_match, z_match]):
        return content
    
    # Get current coordinates
    current_x = float(x_match.group(1))
    current_y = float(y_match.group(1))
    current_z = float(z_match.group(1))
    
    # Update the coordinates in the parameters section (exact logic from move_strip_side.py)
    new_params_section = params_section
    new_params_section = re.sub(x_pattern, f'"x": {new_x}', new_params_section)
    new_params_section = re.sub(y_pattern, f'"y": {current_y}', new_params_section)  # preserve y
    new_params_section = re.sub(z_pattern, f'"z": {current_z}', new_params_section)  # preserve z
    
    # Replace in the main content (exact logic from move_strip_side.py)
    content = content[:params_start] + new_params_section + content[params_end:]
    
    return content

def main():
    if len(sys.argv) < 3:
        print("Usage: python mirror_a_to_b_mapping.py <from_side> <to_side> [min_strip] [--single]")
        print("Examples:")
        print("  python mirror_a_to_b_mapping.py a b 44           # Mirror strips 44+ from A to B")
        print("  python mirror_a_to_b_mapping.py a b 45 --single  # Mirror only strip 45 from A to B")
        sys.exit(1)
    
    from_side = sys.argv[1].lower()
    to_side = sys.argv[2].lower()
    min_strip = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    
    # Check for --single flag
    single_strip_mode = '--single' in sys.argv
    if single_strip_mode and len(sys.argv) < 4:
        print("❌ --single mode requires a strip number")
        print("Example: python mirror_a_to_b_mapping.py a b 45 --single")
        sys.exit(1)
    
    if from_side not in ['a', 'b'] or to_side not in ['a', 'b']:
        print("❌ Sides must be 'a' or 'b'")
        sys.exit(1)
    
    if from_side == to_side:
        print("❌ From and to sides must be different")
        sys.exit(1)
    
    project_file = "/Users/artarazavi/projects/LXStudio-TE/te-app/Projects/BM2024_Pacman.lxp"
    
    if not os.path.exists(project_file):
        print(f"❌ Project file not found: {project_file}")
        sys.exit(1)
    
    # Read the project file
    with open(project_file, 'r') as f:
        content = f.read()
    
    from_side_name = "A-side" if from_side == 'a' else "B-side"
    to_side_name = "B-side" if to_side == 'b' else "A-side"
    
    if single_strip_mode:
        print(f"🔍 Analyzing {from_side_name} Strip {min_strip} for single mirror...")
    else:
        print(f"🔍 Analyzing {from_side_name} splits (Strip {min_strip}+)...")
    
    # Find which source side strips are split
    split_strips = []
    
    if single_strip_mode:
        # Single strip mode - only check the specified strip
        strip_range = [min_strip]
    else:
        # Range mode - check from min_strip to 67
        strip_range = range(min_strip, 68)
    
    for strip_num in strip_range:
        from_main = get_strip_data(content, from_side, strip_num, is_half=False)
        from_half = get_strip_data(content, from_side, strip_num, is_half=True)
        
        if single_strip_mode:
            # In single mode, process the strip even if it's not split (we'll mirror coordinates)
            if from_main:
                split_strips.append({
                    'strip_num': strip_num,
                    'main': from_main,
                    'half': from_half  # Could be None if not split
                })
                suffix = "b" if from_side == 'b' else ""
                if from_half:
                    print(f"   ✅ Strip {strip_num}{suffix}: {from_main['points']} + {from_half['points']} LEDs (split)")
                else:
                    print(f"   ✅ Strip {strip_num}{suffix}: {from_main['points']} LEDs (not split)")
        else:
            # In range mode, only process strips that are actually split
            if from_main and from_half:
                split_strips.append({
                    'strip_num': strip_num,
                    'main': from_main,
                    'half': from_half
                })
                suffix = "b" if from_side == 'b' else ""
                print(f"   ✅ Strip {strip_num}{suffix}: {from_main['points']} + {from_half['points']} LEDs")
    
    if not split_strips:
        if single_strip_mode:
            print(f"❌ Strip {min_strip} not found on {from_side_name}")
        else:
            print(f"ℹ️  No split {from_side_name} strips found ({min_strip}+)")
        sys.exit(0)
    
    if single_strip_mode:
        print(f"\n📊 Found Strip {min_strip} on {from_side_name} to mirror to {to_side_name}")
    else:
        print(f"\n📊 Found {len(split_strips)} split {from_side_name} strips to mirror to {to_side_name}")
    
    # Create backup
    backup_file = f"{project_file}.backup"
    with open(backup_file, 'w') as f:
        f.write(content)
    print(f"💾 Backup created: {backup_file}")
    
    # Process each split strip
    for split_data in split_strips:
        strip_num = split_data['strip_num']
        from_main = split_data['main']
        from_half = split_data['half']
        
        from_suffix = "b" if from_side == 'b' else ""
        to_suffix = "b" if to_side == 'b' else ""
        
        print(f"\n🔄 Processing Strip {strip_num}{from_suffix} → {strip_num}{to_suffix}...")
        
        # Check if target side strip exists and needs splitting
        to_main = get_strip_data(content, to_side, strip_num, is_half=False)
        to_half = get_strip_data(content, to_side, strip_num, is_half=True)
        
        if not to_main:
            print(f"   ⚠️  Strip {strip_num}{to_suffix} not found, skipping")
            continue
        
        # STEP 1: Check if target side needs to be split (based on source side pattern)
        split_needed = not to_half  # If no .5 strip exists, we need to split
        
        if split_needed:
            print(f"   🔧 Target side needs splitting to match source pattern...")
            print(f"      Splitting Strip {strip_num}{to_suffix}...")
            
            try:
                result = subprocess.run([
                    'python3', 'useful_scripts/split_strip.py', to_side, str(strip_num)
                ], capture_output=True, text=True, cwd='/Users/artarazavi/projects/LXStudio-TE')
                
                if result.returncode == 0:
                    print(f"      ✅ Strip {strip_num}{to_suffix} split successfully")
                    # Re-read the file after splitting
                    with open(project_file, 'r') as f:
                        content = f.read()
                else:
                    print(f"      ⚠️  Strip {strip_num}{to_suffix} split failed - will only copy main strip coordinates")
                    
            except Exception as e:
                print(f"      ❌ Error splitting Strip {strip_num}{to_suffix}: {e}")
                print(f"      Will only copy main strip coordinates")
        else:
            print(f"   ✅ Strip {strip_num}{to_suffix} already split, proceeding to coordinate copy...")
        
        # STEP 2: Copy X coordinates from source side to target side - using EXACT move_strip_side.py logic
        print(f"   📍 Copying X coordinates from {from_side_name} to {to_side_name}...")
        
        # Find all labels in content
        all_label_pattern = r'"label"\s*:\s*"([^"]+)"'
        all_matches = list(re.finditer(all_label_pattern, content))
        
        # List of strips to update with their target X coordinates
        strips_to_update = [
            (f"Strip {strip_num}{to_suffix}", from_main['x']),
        ]
        
        # Check if .5 strip exists and add to update list
        to_half_final = get_strip_data(content, to_side, strip_num, is_half=True)
        if to_half_final and from_half:  # Only update .5 if both source and target have .5
            strips_to_update.append((f"Strip {strip_num}.5{to_suffix}", from_half['x']))
        
        # Process each strip to update (exact logic from move_strip_side.py)
        for strip_label, new_x in reversed(strips_to_update):  # reverse to avoid position shifts
            # Find this strip's label
            strip_match = None
            for match in all_matches:
                if match.group(1) == strip_label:
                    strip_match = match
                    break
            
            if not strip_match:
                print(f"      ⚠️  Could not find {strip_label}")
                continue
                
            label_pos = strip_match.start()
            
            # Search backwards to find parameters section start
            params_start_pattern = r'"parameters"\s*:\s*{'
            params_matches = list(re.finditer(params_start_pattern, content[:label_pos]))
            
            if not params_matches:
                print(f"      ⚠️  Could not find parameters section for {strip_label}")
                continue
                
            params_start = params_matches[-1].start()
            
            # Find parameters section end by counting braces
            brace_count = 0
            params_end = params_start
            in_string = False
            escape_next = False
            
            for i, char in enumerate(content[params_start:], params_start):
                if escape_next:
                    escape_next = False
                    continue
                
                if char == '\\':
                    escape_next = True
                    continue
                
                if char == '"' and not escape_next:
                    in_string = not in_string
                    continue
                
                if not in_string:
                    if char == '{':
                        brace_count += 1
                    elif char == '}':
                        brace_count -= 1
                        if brace_count == 0:
                            params_end = i + 1
                            break
            
            if params_end <= params_start:
                print(f"      ⚠️  Could not find end of parameters section for {strip_label}")
                continue
                
            # Extract parameters section
            params_section = content[params_start:params_end]
            
            # Find coordinates
            x_pattern = r'"x"\s*:\s*([-\d.]+)'
            y_pattern = r'"y"\s*:\s*([-\d.]+)'  
            z_pattern = r'"z"\s*:\s*([-\d.]+)'
            
            x_match = re.search(x_pattern, params_section)
            y_match = re.search(y_pattern, params_section)
            z_match = re.search(z_pattern, params_section)
            
            if not all([x_match, y_match, z_match]):
                print(f"      ⚠️  Could not find coordinates for {strip_label}")
                continue
                
            current_x = float(x_match.group(1))
            current_y = float(y_match.group(1))
            current_z = float(z_match.group(1))
            
            print(f"      {strip_label}: X {current_x} → {new_x}")
            
            # Update coordinates in parameters section
            new_params_section = params_section
            new_params_section = re.sub(x_pattern, f'"x": {new_x}', new_params_section)
            new_params_section = re.sub(y_pattern, f'"y": {current_y}', new_params_section)
            new_params_section = re.sub(z_pattern, f'"z": {current_z}', new_params_section)
            
            # Replace in main content
            content = content[:params_start] + new_params_section + content[params_end:]
    
    # Write the updated file
    with open(project_file, 'w') as f:
        f.write(content)
    
    print(f"\n✅ Mirror mapping complete!")
    print(f"   Processed {len(split_strips)} strip pairs ({from_side_name} → {to_side_name})")
    print(f"   {to_side_name} strips now match {from_side_name} X coordinates")
    print(f"   {to_side_name} Z coordinates and DMX channels preserved")

if __name__ == "__main__":
    main()
