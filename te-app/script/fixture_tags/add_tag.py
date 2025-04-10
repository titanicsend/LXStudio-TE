#! /usr/bin/env python

import os
import re

def edit_lxf_files(directory, existing_tag, new_tag):
    # Define the regex pattern to find the line with "tags:" and the existing string
    tag_line_pattern = re.compile(r'^\s*tags:.*"' + re.escape(existing_tag) + r'",')

    added = 0
    found = 0

    # Iterate over all files in the given directory
    for filename in os.listdir(directory):
        # Check if the file ends with .lxf
        if filename.endswith(".lxf"):
            filepath = os.path.join(directory, filename)
            try:
                # Read the file content
                with open(filepath, 'r') as file:
                    content = file.readlines()

                # Flag to track if any modification is made
                modified = False

                # Process each line
                for i, line in enumerate(content):
                    if tag_line_pattern.match(line):
                        if f'"{new_tag}",' not in line:
                            # Insert '"new_string",' after '"existing_string",'
                            content[i] = line.replace(f'"{existing_tag}",', f'"{existing_tag}", "{new_tag}",')
                            modified = True
                            added += 1
                            # print(f"Edited line {i} in {filepath}")
                        else:
                            found += 1
                        break

                # If modifications were made, write the new content back to the file
                if modified:
                    with open(filepath, 'w') as file:
                        file.writelines(content)

            except Exception as e:
                print(f"Failed to edit {filepath}: {e}")

    print(f"Added tag to {added} files. Tag already existed in {found} files.")

# Add a tag to LXF files if it does not exist
directory_path = '../../Fixtures/TE/edge'
existing_tag = 'edge'
new_tag = 'Edge'

edit_lxf_files(directory_path, existing_tag, new_tag)