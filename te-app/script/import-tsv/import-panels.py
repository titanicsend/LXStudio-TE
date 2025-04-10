#! /usr/bin/env python

import sys
import textwrap

# Read panels.txt
with open("panels.txt", "r") as f:
    panels_data = [line.strip().split() for line in f.readlines()]

# Read TSV file
# Assumes a direct export of the "Backpacks" tab of the "TE2 Modules, Panels, and Edges" spreadsheet
tsv_filename = sys.argv[1]
with open(tsv_filename, "r") as f:
    tsv_data = [line.strip().split('\t') for line in f.readlines()]

# The first three lines of that tab are header notes, and we're not
# going to bother asserting that they contain any particular text
del tsv_data[0:3]
# Check if TSV begins with the appropriate header
assert tsv_data.pop(0) == ["Backpack ID", "Module", "Sequence in Module", "IP", "Backpack Type", "Quadrant",
                           "Going to EDC?", "Primary Fixture", "Secondary Fixture", "Additional Secondary", "",
                           "Pixel Count Primary", "Pixel Count Secondary", "Pixel Count Additional Secondary",
                           "Fixture ID", "Pixel Start", "Fixture ID", "Pixel Start", "Fixture ID", "Pixel Start",
                           "Fixture ID", "Pixel Start", "Edge Mount"]

# Build the mappings from tsv

# Filter to only backpacks with assigned panel fixtures and extract relevant columns
# Format is [IP, ch1_panel_id, ch1_pixel_start, ch2_panel_id...ch4_pixel_start]
tsv_data = [[row[3]] + row[14:22] for row in tsv_data if len(row) > 15 and row[4] == "PNL" and row[14] != ""]

tsv_panel_to_outputs = {}
for row in tsv_data:
    assert row[2] in ("0", "1000")
    for channel_number in range(1, 5):
        channel_index = channel_number - 1
        panel_id_column = 1 + channel_index * 2
        if len(row) > panel_id_column:
            panel_id = row[panel_id_column]
            pixel_start = row[panel_id_column + 1]
            # current assumptions:
            #   we always fill all the channels in an IP
            #   there is max one overflow that starts at 1000
            # leveraging these assumptions makes for a cleaner/more readable panels.txt, but not hard to adapt by being
            #   explicit about every channel if necessary
            if pixel_start == "0" or pixel_start == "1000":
                if panel_id not in tsv_panel_to_outputs:
                    tsv_panel_to_outputs[panel_id] = []
                output = row[0] + "#" + str(channel_number)
                if pixel_start == "0":
                    tsv_panel_to_outputs[panel_id].insert(0, output)
                else:
                    tsv_panel_to_outputs[panel_id].append(output)

# Create dictionaries for cross-referencing
panels_dict = {panel[0]: panel for panel in panels_data}

# Keep track of updates and non-updates
updated_panels = []
found_only_in_panels = []
found_only_in_tsv = []

# Cross-reference and update
for panel_id in set(panels_dict.keys()) | set(tsv_panel_to_outputs.keys()):
    if panel_id in panels_dict and panel_id in tsv_panel_to_outputs:
        panels_dict[panel_id][-1] = '/'.join(tsv_panel_to_outputs[panel_id])
        updated_panels.append(panel_id)
    elif panel_id in panels_dict:
        found_only_in_panels.append(panel_id)
    else:
        found_only_in_tsv.append(panel_id)

updated_panels.sort()
found_only_in_panels.sort()
found_only_in_tsv.sort()

# Overwrite panels.txt with updated information
with open("panels.txt", "w") as f:
    for panel in panels_dict.values():
        f.write("\t".join(panel) + "\n")

# Print report
print("Updated Panels:")
print("\n".join(textwrap.wrap(" ".join(updated_panels), width=80)))

if found_only_in_panels:
    print("\nFound in Only Panels File:")
    print("\n".join(textwrap.wrap(" ".join(found_only_in_panels), width=80)))

if found_only_in_tsv:
    print("\nFound in Only TSV File:")
    print("\n".join(textwrap.wrap(" ".join(found_only_in_tsv), width=80)))
