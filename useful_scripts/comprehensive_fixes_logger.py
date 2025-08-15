#!/usr/bin/env python3
"""
Comprehensive Strip Fixes Logger

Analyzes LED strips for reverse pattern corrections and universe overflow issues.
Works with both A-side and B-side strips.

Usage:
    python3 comprehensive_fixes_logger.py <project_file> <side> [--from-strip N]

Examples:
    # Analyze all A-side strips
    python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp A
    
    # Analyze all B-side strips
    python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp B
    
    # Analyze A-side strips starting from strip 40
    python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_Pacman.lxp A --from-strip 40
"""

import re
import json
import sys
import argparse
from datetime import datetime

def comprehensive_strip_analysis(project_file, side, min_strip=None):
    """Analyze and log ALL needed fixes: reverse corrections AND universe overflows"""
    
    with open(project_file, 'r') as f:
        content = f.read()
    
    # Determine side name for display
    side_name = "A-side" if side.upper() == 'A' else "B-side"
    
    print("🔍 COMPREHENSIVE STRIP ANALYSIS")
    print("=" * 80)
    print(f"Analyzing: {side_name} strips")
    print("Checking for: 1) Reverse pattern fixes, 2) Universe overflow fixes")
    print("Correct pattern: Odd strips = reverse:true, Even strips = reverse:false")
    if min_strip:
        print(f"Filter: Analyzing strips {min_strip} and above only")
    print("=" * 80)
    
    # Find all strips with their complete data
    # Look for jsonParameters section first, then find the corresponding parameters section
    strip_pattern = r'"jsonParameters": \{[^}]*"points": (\d+)[^}]*"universe": (\d+)[^}]*"dmxChannel": (\d+)[^}]*"reverse": (true|false)[^}]*\}.*?"parameters": \{[^}]*"label": "(Strip [0-9.]+(?:b)?)"[^}]*"x": ([0-9.-]+)[^}]*"y": ([0-9.-]+)[^}]*"z": ([0-9.-]+)[^}]*'
    all_strip_sections = re.findall(strip_pattern, content, re.DOTALL)
    
    # Filter strips by side
    strip_sections = []
    for section in all_strip_sections:
        points, universe, dmx_channel, reverse, label, x, y, z = section
        
        # Filter by side
        if side.upper() == 'A' and not label.endswith('b'):
            strip_sections.append(section)
        elif side.upper() == 'B' and label.endswith('b'):
            strip_sections.append(section)
    
    reverse_fixes = []
    overflow_fixes = []
    comprehensive_log = []
    
    print("📋 Strip Analysis:")
    print("-" * 80)
    
    for match in strip_sections:
        points, universe, dmx_channel, reverse_str, label, x, y, z = match
        points = int(points)
        universe = int(universe)
        dmx_channel = int(dmx_channel)
        current_reverse = reverse_str == "true"
        x = float(x)
        y = float(y)
        z = float(z)
        
        # Extract strip number (handle decimal numbers like "Strip 4.5" and b suffix like "Strip 1b")
        strip_num_str = label.split()[1]
        # Remove 'b' suffix if present
        clean_strip_num_str = strip_num_str.rstrip('b')
        strip_num = float(clean_strip_num_str)
        base_strip_num = int(clean_strip_num_str.split('.')[0])  # Get the base number (4 from "4.5")
        
        # Apply filter if specified
        if min_strip and base_strip_num < min_strip:
            continue
        
        # Determine correct reverse value based on the BASE strip number (odd=true, even=false)
        correct_reverse = (base_strip_num % 2 == 1)  # Odd strips should be true
        
        # Check if reverse needs fixing
        reverse_needs_fix = (current_reverse != correct_reverse)
        
        # Determine flow direction (using CORRECT reverse value)
        if correct_reverse:
            correct_flow = "right→left"
        else:
            correct_flow = "left→right"
        
        # Calculate overflow
        required_channels = points * 3
        end_channel = dmx_channel + required_channels - 1
        overflow_needs_fix = (end_channel > 512)
        
        # Create strip entry
        strip_entry = {
            'label': label,
            'strip_num': strip_num,
            'base_strip_num': base_strip_num,
            'points': points,
            'universe': universe,
            'dmx_channel': dmx_channel,
            'x': x, 'y': y, 'z': z,
            'current_reverse': current_reverse,
            'correct_reverse': correct_reverse,
            'correct_flow': correct_flow,
            'reverse_needs_fix': reverse_needs_fix,
            'overflow_needs_fix': overflow_needs_fix
        }
        
        # If reverse needs fixing, log it
        if reverse_needs_fix:
            reverse_fix = {
                'strip': label,
                'strip_num': strip_num,
                'base_strip_num': base_strip_num,
                'current_reverse': current_reverse,
                'correct_reverse': correct_reverse,
                'current_flow': "right→left" if current_reverse else "left→right",
                'correct_flow': correct_flow
            }
            reverse_fixes.append(reverse_fix)
        
        # If overflow needs fixing, calculate split with CORRECT flow
        if overflow_needs_fix:
            overflow_channels = end_channel - 512
            overflow_leds = overflow_channels // 3
            
            # Calculate split
            available_channels = 512 - dmx_channel + 1
            points_that_fit = available_channels // 3
            remaining_points = points - points_that_fit
            
            # Calculate coordinates using CORRECT reverse value
            if correct_reverse:  # Should be right→left
                # Original strip stays at x (right side start)
                original_x = x
                # Overflow goes to left side
                overflow_x = x - (points_that_fit * 0.8)
                overflow_direction = "left"
            else:  # Should be left→right
                # Original strip stays at x (left side start)
                original_x = x
                # Overflow goes to right side
                overflow_x = x + (points_that_fit * 0.8)
                overflow_direction = "right"
            
            overflow_fix = {
                'strip': label,
                'strip_num': strip_num,
                'base_strip_num': base_strip_num,
                'original_points': points,
                'universe': universe,
                'dmx_channel': dmx_channel,
                'overflow_leds': overflow_leds,
                'correct_flow': correct_flow,
                'split': {
                    'original': {
                        'points': points_that_fit,
                        'x': original_x,
                        'universe': universe,
                        'dmx_channel': dmx_channel,
                        'reverse': correct_reverse
                    },
                    'overflow': {
                        'label': label + '.5',
                        'points': remaining_points,
                        'x': overflow_x,
                        'universe': universe + 1,
                        'dmx_channel': 1,
                        'reverse': not correct_reverse,
                        'direction': overflow_direction,
                        'offset': abs(overflow_x - original_x)
                    }
                }
            }
            overflow_fixes.append(overflow_fix)
        
        # Show status for each strip
        status_icons = []
        if reverse_needs_fix:
            status_icons.append("🔄")
        if overflow_needs_fix:
            status_icons.append("💥")
        if not reverse_needs_fix and not overflow_needs_fix:
            status_icons.append("✅")
        
        print(f"{' '.join(status_icons)} {label}: {points} LEDs, Universe {universe}, DMX {dmx_channel}")
        print(f"   Current: reverse={current_reverse} ({'✅' if not reverse_needs_fix else '❌'})")
        print(f"   Correct: reverse={correct_reverse} ({correct_flow})")
        
        if overflow_needs_fix:
            print(f"   Overflow: {overflow_leds} LEDs → Split needed")
        
        # Add to comprehensive log
        comprehensive_entry = {
            'timestamp': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            'strip_data': strip_entry,
            'fixes_needed': {
                'reverse': reverse_needs_fix,
                'overflow': overflow_needs_fix
            }
        }
        
        if reverse_needs_fix:
            comprehensive_entry['reverse_fix'] = reverse_fix
        if overflow_needs_fix:
            comprehensive_entry['overflow_fix'] = overflow_fix
            
        comprehensive_log.append(comprehensive_entry)
        print()
    
    print("=" * 80)
    print(f"📊 SUMMARY:")
    print(f"   🔄 {len(reverse_fixes)} strips need reverse correction")
    print(f"   💥 {len(overflow_fixes)} strips need overflow splitting")
    print(f"   ✅ {len(strip_sections) - len(set([r['strip'] for r in reverse_fixes] + [o['strip'] for o in overflow_fixes]))} strips are perfect")
    
    # Generate detailed logs
    filter_suffix = f"_from_{min_strip}" if min_strip else ""
    log_filename = f"comprehensive_strip_fixes{filter_suffix}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
    
    print(f"\n📝 REVERSE FIXES NEEDED:")
    print("-" * 50)
    for fix in reverse_fixes:
        print(f"{fix['strip']}: {fix['current_reverse']} → {fix['correct_reverse']} ({fix['correct_flow']})")
    
    print(f"\n💥 OVERFLOW FIXES NEEDED:")
    print("-" * 50)
    for fix in overflow_fixes:
        orig = fix['split']['original']
        over = fix['split']['overflow']
        print(f"{fix['strip']}: {fix['original_points']} LEDs → {orig['points']} + {over['points']} LEDs")
        print(f"   Flow: {fix['correct_flow']}")
        print(f"   Coordinates: x={orig['x']:.1f} + x={over['x']:.1f} ({over['direction']}, {over['offset']:.1f} units)")
        print(f"   Universes: {orig['universe']} + {over['universe']}")
        print()
    
    # Write comprehensive log file
    with open(log_filename, 'w') as f:
        f.write("# COMPREHENSIVE STRIP FIXES LOG\n")
        f.write(f"# Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("# Fixes needed: Reverse corrections + Universe overflow splits\n")
        f.write("# Correct zigzag: Odd strips = reverse:true, Even strips = reverse:false\n")
        if min_strip:
            f.write(f"# Filter: Strips {min_strip} and above only\n")
        f.write("\n")
        
        f.write("=" * 60 + "\n")
        f.write("REVERSE FIXES SUMMARY\n")
        f.write("=" * 60 + "\n")
        for fix in reverse_fixes:
            f.write(f"Strip: {fix['strip']}\n")
            f.write(f"Current: reverse={fix['current_reverse']} ({fix['current_flow']})\n")
            f.write(f"Correct: reverse={fix['correct_reverse']} ({fix['correct_flow']})\n")
            f.write("-" * 40 + "\n")
        
        f.write("\n" + "=" * 60 + "\n")
        f.write("OVERFLOW FIXES SUMMARY\n")
        f.write("=" * 60 + "\n")
        for fix in overflow_fixes:
            f.write(f"Strip: {fix['strip']}\n")
            f.write(f"Original: {fix['original_points']} LEDs, Universe {fix['universe']}\n")
            f.write(f"Flow: {fix['correct_flow']}\n")
            f.write(f"Split Configuration:\n")
            f.write(f"  Original: {fix['split']['original']['points']} LEDs at x={fix['split']['original']['x']:.1f}\n")
            f.write(f"  Overflow: {fix['split']['overflow']['points']} LEDs at x={fix['split']['overflow']['x']:.1f}\n")
            f.write(f"  Offset: {fix['split']['overflow']['offset']:.1f} units {fix['split']['overflow']['direction']}\n")
            f.write(f"  Universes: {fix['split']['original']['universe']} + {fix['split']['overflow']['universe']}\n")
            f.write("-" * 40 + "\n")
        
        f.write("\n" + "=" * 60 + "\n")
        f.write("DETAILED LOG (JSON)\n")
        f.write("=" * 60 + "\n")
        f.write(json.dumps(comprehensive_log, indent=2))
    
    print(f"📄 Comprehensive log saved to: {log_filename}")
    
    return reverse_fixes, overflow_fixes, comprehensive_log

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Analyze strip fixes for specified side with optional filtering')
    parser.add_argument('project_file', help='Path to the .lxp project file')
    parser.add_argument('side', choices=['A', 'B', 'a', 'b'], 
                        help='Side to analyze: A (no b suffix) or B (with b suffix)')
    parser.add_argument('--from-strip', type=int, metavar='N', 
                        help='Only analyze strips N and above (e.g., --from-strip 40)')
    
    args = parser.parse_args()
    
    if not args.project_file or not args.side:
        parser.print_help()
        sys.exit(1)
    
    try:
        reverse_fixes, overflow_fixes, comprehensive_log = comprehensive_strip_analysis(
            args.project_file, args.side, min_strip=args.from_strip)
        
        print(f"\n🔧 READY TO APPLY FIXES:")
        print(f"   🔄 {len(reverse_fixes)} reverse corrections")
        print(f"   💥 {len(overflow_fixes)} overflow splits")
        print(f"   📍 All coordinates calculated with correct zigzag flow")
        
    except FileNotFoundError:
        print(f"Error: Project file '{args.project_file}' not found")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
