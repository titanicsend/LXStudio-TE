#!/usr/bin/env python3
"""
Move all strips from a specified side (A or B) by given X, Y, Z offsets.
This script modifies the position coordinates of all strips including .5 strips.

Usage:
    python3 move_strip_side.py <project_file> <side> <x_offset> <y_offset> <z_offset>

Example:
    # Move all B-side strips 10 units up in Z direction
    python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp B 0 0 -10
    
    # Move all A-side strips 5 units right and 2 units forward
    python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp A 5 2 0
"""

import sys
import re
import json
from pathlib import Path

def move_strips_by_side(project_file, side, x_offset, y_offset, z_offset):
    """
    Move all strips from specified side by given offsets.
    
    Args:
        project_file: Path to the .lxp project file
        side: 'A' or 'B' - which side to move
        x_offset, y_offset, z_offset: Amount to move in each direction
    """
    
    print(f"Reading {project_file}...")
    with open(project_file, 'r') as f:
        content = f.read()
    
    # Create backup
    backup_file = f"{project_file}.backup"
    with open(backup_file, 'w') as f:
        f.write(content)
    print(f"Backup created: {backup_file}")
    
    # Determine strip pattern based on side
    if side.upper() == 'A':
        # A-side strips: "Strip 1", "Strip 2.5", etc. (no 'b' suffix)
        # Use negative lookahead to exclude strips ending with 'b'
        strip_pattern = r'"label"\s*:\s*"(Strip\s+\d+(?:\.\d+)?)"\s*(?!.*b")'
        side_name = "A-side"
    elif side.upper() == 'B':
        # B-side strips: "Strip 1b", "Strip 2.5b", etc. (with 'b' suffix)
        strip_pattern = r'"label"\s*:\s*"(Strip\s+\d+(?:\.\d+)?b)"'
        side_name = "B-side"
    else:
        print(f"Error: Side must be 'A' or 'B', got '{side}'")
        return False
    
    print(f"Moving {side_name} strips by offset: X={x_offset}, Y={y_offset}, Z={z_offset}")
    
    # Find all label entries first, then filter by side
    all_label_pattern = r'"label"\s*:\s*"(Strip\s+\d+(?:\.\d+)?(?:b)?)"'
    all_matches = re.finditer(all_label_pattern, content)
    
    # Filter matches based on side
    strip_matches = []
    for match in all_matches:
        label = match.group(1)
        if side.upper() == 'A' and not label.endswith('b'):
            strip_matches.append(match)
        elif side.upper() == 'B' and label.endswith('b'):
            strip_matches.append(match)
    
    print(f"Found {len(strip_matches)} {side_name} strips")
    
    if len(strip_matches) == 0:
        print(f"No {side_name} strips found!")
        return False
    
    updates_made = 0
    
    # Process strips in reverse order to avoid position shifts from content replacement
    for match in reversed(strip_matches):
        strip_label = match.group(1)
        
        # Find the position parameters for this strip
        # Look for the "parameters" section that contains this label
        label_pos = match.start()
        
        # Search backwards from the label to find the start of this fixture's parameters section
        params_start_pattern = r'"parameters"\s*:\s*{'
        params_matches = list(re.finditer(params_start_pattern, content[:label_pos]))
        
        if not params_matches:
            print(f"Warning: Could not find parameters section for {strip_label}")
            continue
            
        # Get the last parameters section before our label
        params_start = params_matches[-1].end() - 1  # Include the opening brace
        
        # Find the closing brace for this parameters section
        brace_count = 0
        params_end = params_start
        for i, char in enumerate(content[params_start:], params_start):
            if char == '{':
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0:
                    params_end = i + 1
                    break
        
        if params_end <= params_start:
            print(f"Warning: Could not find end of parameters section for {strip_label}")
            continue
            
        # Extract the parameters section
        params_section = content[params_start:params_end]
        
        # Find and update x, y, z coordinates
        x_pattern = r'"x"\s*:\s*([-\d.]+)'
        y_pattern = r'"y"\s*:\s*([-\d.]+)'
        z_pattern = r'"z"\s*:\s*([-\d.]+)'
        
        x_match = re.search(x_pattern, params_section)
        y_match = re.search(y_pattern, params_section)
        z_match = re.search(z_pattern, params_section)
        
        if not all([x_match, y_match, z_match]):
            print(f"Warning: Could not find x, y, z coordinates for {strip_label}")
            continue
        
        # Get current coordinates
        current_x = float(x_match.group(1))
        current_y = float(y_match.group(1))
        current_z = float(z_match.group(1))
        
        # Calculate new coordinates
        new_x = current_x + x_offset
        new_y = current_y + y_offset
        new_z = current_z + z_offset
        
        print(f"  {strip_label}: ({current_x}, {current_y}, {current_z}) -> ({new_x}, {new_y}, {new_z})")
        
        # Update the coordinates in the parameters section
        new_params_section = params_section
        new_params_section = re.sub(x_pattern, f'"x": {new_x}', new_params_section)
        new_params_section = re.sub(y_pattern, f'"y": {new_y}', new_params_section)
        new_params_section = re.sub(z_pattern, f'"z": {new_z}', new_params_section)
        
        # Replace in the main content
        content = content[:params_start] + new_params_section + content[params_end:]
        
        # Adjust positions of subsequent matches due to content length change
        length_diff = len(new_params_section) - len(params_section)
        for later_match in strip_matches[strip_matches.index(match) + 1:]:
            if hasattr(later_match, '_start'):
                later_match._start += length_diff
        
        updates_made += 1
    
    if updates_made > 0:
        print(f"\nWriting updated file with {updates_made} strips moved...")
        with open(project_file, 'w') as f:
            f.write(content)
        print("File updated successfully!")
        return True
    else:
        print("No strips were moved.")
        return False

def main():
    if len(sys.argv) != 6:
        print("Usage: python3 move_strip_side.py <project_file> <side> <x_offset> <y_offset> <z_offset>")
        print("Example: python3 move_strip_side.py te-app/Projects/BM2024_Pacman.lxp B 0 0 -10")
        sys.exit(1)
    
    project_file = sys.argv[1]
    side = sys.argv[2]
    x_offset = float(sys.argv[3])
    y_offset = float(sys.argv[4])
    z_offset = float(sys.argv[5])
    
    if not Path(project_file).exists():
        print(f"Error: Project file '{project_file}' not found")
        sys.exit(1)
    
    if side.upper() not in ['A', 'B']:
        print(f"Error: Side must be 'A' or 'B', got '{side}'")
        sys.exit(1)
    
    success = move_strips_by_side(project_file, side, x_offset, y_offset, z_offset)
    
    if success:
        print(f"\nSuccess! All {side.upper()}-side strips moved by ({x_offset}, {y_offset}, {z_offset})")
    else:
        print("Failed to move strips")
        sys.exit(1)

if __name__ == "__main__":
    main()
