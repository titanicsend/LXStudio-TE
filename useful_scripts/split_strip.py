#!/usr/bin/env python3
"""
Split Strip Script - SIMPLE VERSION THAT WORKS
==============================================

Splits LED strips by doing EXACTLY what we do manually:
1. Find the strip's jsonParameters 
2. Change its points value
3. Copy the entire strip section and modify it for overflow

Usage: python split_strip.py <side> <strip_number>
"""

import sys
import re
import os

def main():
    if len(sys.argv) != 3:
        print("Usage: python split_strip.py <side> <strip_number>")
        sys.exit(1)
    
    side = sys.argv[1].lower()
    strip_num = int(sys.argv[2])
    
    if side == 'a':
        strip_label = f"Strip {strip_num}"
    else:
        strip_label = f"Strip {strip_num}b"
    
    project_file = "/Users/artarazavi/projects/LXStudio-TE/te-app/Projects/BM2024_Pacman.lxp"
    
    # Read file
    with open(project_file, 'r') as f:
        content = f.read()
    
    print(f"🔍 Looking for {strip_label}...")
    
    # Find strip data using the working pattern
    pattern = r'"jsonParameters": \{[^}]*"points": (\d+)[^}]*"universe": (\d+)[^}]*"dmxChannel": (\d+)[^}]*"reverse": (true|false)[^}]*\}.*?"parameters": \{[^}]*"label": "(Strip [0-9.]+(?:b)?)"[^}]*'
    matches = re.findall(pattern, content, re.DOTALL)
    
    strip_data = None
    for points, universe, dmx_channel, reverse, label in matches:
        if label == strip_label:
            strip_data = {
                'points': int(points),
                'universe': int(universe), 
                'dmx_channel': int(dmx_channel),
                'reverse': reverse == 'true'
            }
            break
    
    if not strip_data:
        print(f"❌ {strip_label} not found!")
        sys.exit(1)
    
    print(f"✅ Found {strip_label}: {strip_data['points']} LEDs, U{strip_data['universe']}, Ch{strip_data['dmx_channel']}")
    
    # Check if needs splitting
    end_channel = strip_data['dmx_channel'] + (strip_data['points'] * 3) - 1
    if end_channel <= 512:
        print(f"✅ {strip_label} doesn't need splitting (ends at channel {end_channel})")
        sys.exit(0)
    
    # Calculate split
    remaining_channels = 512 - strip_data['dmx_channel'] + 1
    main_leds = remaining_channels // 3
    overflow_leds = strip_data['points'] - main_leds
    
    print(f"📊 Splitting: {main_leds} + {overflow_leds} LEDs")
    
    # Backup
    backup_file = f"{project_file}.backup"
    with open(backup_file, 'w') as f:
        f.write(content)
    print(f"💾 Backup: {backup_file}")
    
    # STEP 1: Update main strip points (SAFE - just change one number)
    old_points_pattern = f'"points": {strip_data["points"]}'
    new_points_value = f'"points": {main_leds}'
    
    # Find the specific jsonParameters section for our strip and replace ONLY its points
    json_section_pattern = rf'("jsonParameters": \{{[^}}]*)"points": {strip_data["points"]}([^}}]*"universe": {strip_data["universe"]}[^}}]*"dmxChannel": {strip_data["dmx_channel"]}[^}}]*\}})'
    
    def update_main_strip(match):
        return match.group(1) + f'"points": {main_leds}' + match.group(2)
    
    content = re.sub(json_section_pattern, update_main_strip, content, count=1)
    
    # STEP 2: Find complete strip sections - use proper brace matching
    # First find the jsonParameters part we just updated
    json_start_pattern = rf'"jsonFixtureType": "Pacman/PacmanStrip"[^{{}}]*"jsonParameters": \{{[^}}]*"points": {main_leds}[^}}]*"universe": {strip_data["universe"]}[^}}]*"dmxChannel": {strip_data["dmx_channel"]}[^}}]*\}}'
    json_match = re.search(json_start_pattern, content, re.DOTALL)
    
    if not json_match:
        print("❌ Could not find updated jsonParameters section")
        sys.exit(1)
    
    # Find the start of this complete fixture by going backwards to find the opening brace
    search_start = json_match.start()
    brace_start = content.rfind('{', 0, search_start)
    
    # Now use proper brace counting to find the matching closing brace
    brace_count = 0
    brace_end = brace_start
    for i in range(brace_start, len(content)):
        if content[i] == '{':
            brace_count += 1
        elif content[i] == '}':
            brace_count -= 1
            if brace_count == 0:
                brace_end = i + 1
                break
    
    if brace_count != 0:
        print("❌ Could not find matching closing brace")
        sys.exit(1)
    
    # Extract the complete strip (this should be a complete JSON object)
    original_strip = content[brace_start:brace_end]
    
    # STEP 3: Create overflow strip by copying and modifying
    overflow_label = f"{strip_label}.5" if not strip_label.endswith('b') else f"{strip_label[:-1]}.5b"
    overflow_universe = strip_data['universe'] + 1
    
    overflow_strip = original_strip
    # Simple replacements
    overflow_strip = re.sub(f'"points": {main_leds}', f'"points": {overflow_leds}', overflow_strip)
    overflow_strip = re.sub(f'"universe": {strip_data["universe"]}', f'"universe": {overflow_universe}', overflow_strip)
    overflow_strip = re.sub(f'"dmxChannel": {strip_data["dmx_channel"]}', f'"dmxChannel": 1', overflow_strip)
    overflow_strip = re.sub(f'"label": "{strip_label}"', f'"label": "{overflow_label}"', overflow_strip)
    overflow_strip = re.sub(r'"id": (\d+)', lambda m: f'"id": {int(m.group(1)) + 1000}', overflow_strip)
    
    # STEP 4: Insert overflow strip right after original
    insertion_point = brace_end
    content = content[:insertion_point] + ',\n      ' + overflow_strip + content[insertion_point:]
    
    # Write file
    with open(project_file, 'w') as f:
        f.write(content)
    
    print(f"✅ Split complete!")
    print(f"   {strip_label}: {main_leds} LEDs")
    print(f"   {overflow_label}: {overflow_leds} LEDs")
    print("⚠️  Coordinates are identical - adjust manually if needed")

if __name__ == "__main__":
    main()