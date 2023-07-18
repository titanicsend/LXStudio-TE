#! /usr/bin/env python

import glob
import json
import os
import re
import sys

# required: use at least one of these
TE_COLOR_UNIFORMS = [
    "iColorRGB",
    "iColorHSB",
    "iColor2RGB",
    "iColor2HSB",
]

# Mark only these params if they're unused
TE_CONTROLS_EXPLICIT = [
    "iScale",
    "iQuantity",
    "iWow1",
    "iWow2",
    "iWowTrigger",
]

TE_CONTROL_TAGS = {
    "iScale": ("SIZE", "Size"),
    "iQuantity": ("QUANTITY", "Quantity"),
    "iBrightness": ("BRIGHTNESS", "Brightness"),
    "iWow1": ("WOW1", "Wow1"),
    "iWow2": ("WOW2", "Wow2"),
    "iWowTrigger": ("WOWTRIGGER", "WowTrigger"),
    "iSpeed": ("SPEED", "Speed"),
    "iRotationAngle": ("ANGLE", "Angle"),
    "iSpin": ("SPIN", "Spin"),
    #"iTranslate": (["XPOS", "YPOS"], ["xPos", "yPos"]),
}

PROJECT_ROOT = os.path.join(os.path.dirname(__file__), "../..")
SHADER_DIR = os.path.join(PROJECT_ROOT, "resources/shaders")
PATTERN_DIR = os.path.join(PROJECT_ROOT, "src/main/java/titanicsend/pattern")
MISSING_CONTROLS_FILE = os.path.join(PROJECT_ROOT, "resources/pattern/missingControls.json")

if not os.path.isdir(PATTERN_DIR):
    raise Exception(f"dir doesnt exist: {PATTERN_DIR}")
if not os.path.isdir(SHADER_DIR):
    raise Exception(f"dir doesnt exist: {SHADER_DIR}")

all_patterns = glob.glob(f"{PATTERN_DIR}/**/*.java", recursive=True)
all_shaders = glob.glob(f"{SHADER_DIR}/*.fs")


def find_shader_classname(shader_name, java_source):
    print(f"searching for: {shader_name}")
    pos = java_source.find(shader_name)
    if (pos < 1):
        raise Exception(f"not found: {shader_name}")

    # important: search for the nearest "super()" call after the class in question
    # was found - this is important for files like ShaderPatternConfig with multiple
    # patterns defined.
    previous_source = java_source[:pos]

    constructors = [c for c in re.finditer('class (\w+) .*?{', previous_source)]
    if not constructors:
        raise Exception(f"didnt find constructor")

    c = constructors[-1]
    return c.group(1)


def find_shader_refs(shader_name):
    matching_java_files = []
    for pattern_path in all_patterns:
        # print(pattern_path)
        with open(pattern_path, 'r') as infile:
            java_source = infile.read()
        # found = java_source.find()
        if shader_name in java_source:
            matching_java_files.append(pattern_path)
    patterns = []
    for path in matching_java_files:
        pattern = {
            'path': path,
            'name': os.path.basename(path)
        }
        with open(path, 'r') as infile:
            pattern_source = infile.read()

        pattern['classname'] = find_shader_classname(s['name'], pattern_source)
        patterns.append(pattern)
    return patterns


def are_uniforms_present(shader_source, uniforms):
    missing_uniforms = []
    for u in uniforms:
        if u not in shader_source:
            missing_uniforms.append(u)
    return missing_uniforms


def read_shaders():
    for path in all_shaders:
        # print(path)
        source = ""
        with open(path, 'r') as infile:
            source = infile.read()

        shader_name = os.path.basename(path)
        yield dict(name=shader_name, source=source, path=path)


def java_set_label(control_uniform):
    control_tag = TE_CONTROL_TAGS[control_uniform]
    return f'\n        markUnusedControl(TEControlTag.{control_tag[0]});'

payloads = []
for s in read_shaders():
    patterns = find_shader_refs(s['name'])

    print(f"{s['name']} -> {[p['classname'] for p in patterns]}")

    te_color_uniforms = are_uniforms_present(s['source'], TE_COLOR_UNIFORMS)
    te_controls_explicit = are_uniforms_present(s['source'], TE_CONTROLS_EXPLICIT)

    uses_palette = True
    if len(te_color_uniforms) == 4:
        uses_palette = False


    missing_control_tags = []

    # relabeling_code = ""
    for missing_explicit_control in te_controls_explicit:
        missing_control_tags.append(TE_CONTROL_TAGS[missing_explicit_control][0])
        # print(f"\t\tISSUE FOUND: TE control missing: {missing_explicit_control}")
        # set_label_call = java_set_label(missing_explicit_control)
        # print(f"\t\t\t{set_label_call}")
        # relabeling_code += set_label_call

    shader_payload = {
        'shader_name': s['name'],
        'pattern_classes': [p['classname'] for p in patterns],
        'uses_palette': uses_palette,
        'missing_control_tags': missing_control_tags
    }
    print(shader_payload)
    payloads.append(shader_payload)

with open(MISSING_CONTROLS_FILE, 'w') as outfile:
    outfile.write(json.dumps(payloads, indent="  "))

print(f'wrote {MISSING_CONTROLS_FILE}')
    # if relabeling_code:
    #     print("\trelabeling code found")
    #     for p in patterns:
    #         # re-read the source, because we might edit the same java source file multiple times
    #         # if it contains multiple pattern classes (and we re-write it once per pattern class)
    #         with open(p['path'], 'r') as infile:
    #             pattern_source = infile.read()
    #
    #         search_string = "public "+p['classname']+ "\(.*{"
    #         print(f"\tclassname = {p['classname']} / search_string = {search_string}")
    #         constructor = re.search(search_string, pattern_source)
    #         if not constructor:
    #             raise Exception(f"constructor not found with search string: {search_string}")
    #
    #         opening_brace_pos = constructor.end()
    #
    #         closing_brace_pos = pattern_source.find("}", opening_brace_pos)
    #         if closing_brace_pos < 1:
    #             raise Exception("closing brace not found")
    #         print('================')
    #         constructor_lines = pattern_source[opening_brace_pos:closing_brace_pos].split('\n')
    #         print('\n----\n'.join(constructor_lines))
    #
    #         new_constructor_lines = []
    #         for idx, line in enumerate(constructor_lines):
    #             if 'super(' in line:
    #                 print(f"super found: idx={idx} -- {line}")
    #                 new_constructor_lines.append(line)
    #                 # append the relabeling code right after the call to super - it's okay if there are duplicates
    #                 # from before, they'll be dropped in this for loop
    #                 new_constructor_lines.append(relabeling_code)
    #                 continue
    #             elif 'markUnusedControl(TEControlTag' in line:
    #                 continue
    #             new_constructor_lines.append(line)
    #
    #         new_source = pattern_source[:opening_brace_pos] + '\n'.join(new_constructor_lines) + pattern_source[closing_brace_pos:]
    #         with open(p['path'], 'w') as outfile:
    #             outfile.write(new_source)
    #         print(f"wrote {p['path']}")

