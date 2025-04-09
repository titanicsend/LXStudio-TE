#! /usr/bin/env python

import sys
import textwrap

# Read edges.txt
with open("edges.txt", "r") as f:
    edges_data = [line.strip().split() for line in f.readlines()]

# Sort edges_data numerically
edges_data.sort(key=lambda x: (int(x[0].split("-")[0]), int(x[0].split("-")[1])))

# Read TSV file
tsv_filename = sys.argv[1]
with open(tsv_filename, "r") as f:
    tsv_data = [line.strip().split('\t') for line in f.readlines()]

# Check if TSV begins with the appropriate header
assert tsv_data.pop(0) == ["Edge ID", "Module", "Backpack ID", "Controller IP", "Controller channel", "Pixel offset", "Output"]

# Create dictionaries for cross-referencing
edges_dict = {edge[0]: edge for edge in edges_data}
tsv_dict = {edge[0]: edge for edge in tsv_data}

# Keep track of updates and non-updates
updated_edges = []
not_updated_edges = []
found_only_in_edges = []
found_only_in_tsv = []

# Cross-reference and update
for edge_id in set(edges_dict.keys()) | set(tsv_dict.keys()):
    if edge_id in edges_dict and edge_id in tsv_dict:
        if '.' in tsv_dict[edge_id][-1]:
            edges_dict[edge_id][-1] = tsv_dict[edge_id][-1]
            updated_edges.append(edge_id)
        else:
            not_updated_edges.append(edge_id)
    elif edge_id in edges_dict:
        found_only_in_edges.append(edge_id)
    else:
        found_only_in_tsv.append(edge_id)

def edge_key(edge_id):
  tokens = edge_id.split("-")
  return (int(tokens[0]), int(tokens[1]))

updated_edges.sort(key=edge_key)
not_updated_edges.sort(key=edge_key)
found_only_in_edges.sort(key=edge_key)
found_only_in_tsv.sort(key=edge_key)

# Overwrite edges.txt with updated information
with open("edges.txt", "w") as f:
    for edge in edges_dict.values():
        f.write("\t".join(edge) + "\n")

# Print report
print("Updated Edges:")
print("\n".join(textwrap.wrap(" ".join(updated_edges), width=80)))

print("\nNot Updated Edges:")
print("\n".join(textwrap.wrap(" ".join(not_updated_edges), width=80)))

if found_only_in_edges:
    print("\nFound in Only Edges File:")
    print("\n".join(textwrap.wrap(" ".join(found_only_in_edges), width=80)))

if found_only_in_tsv:
    print("\nFound in Only TSV File:")
    print("\n".join(textwrap.wrap(" ".join(found_only_in_tsv), width=80)))
