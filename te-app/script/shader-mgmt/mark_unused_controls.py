#! /usr/bin/env python

import glob
import json
import os
import re

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

    te_color_uniforms = are_uniforms_present(s['source'], TE_COLOR_UNIFORMS)
    te_controls_explicit = are_uniforms_present(s['source'], TE_CONTROLS_EXPLICIT)

    uses_palette = True
    if len(te_color_uniforms) == 4:
        uses_palette = False


    missing_control_tags = []

    for missing_explicit_control in te_controls_explicit:
        missing_control_tags.append(TE_CONTROL_TAGS[missing_explicit_control][0])

    shader_payload = {
        'shader_name': s['name'],
        'pattern_classes': [p['classname'] for p in patterns],
        'uses_palette': uses_palette,
        'missing_control_tags': missing_control_tags
    }
    payloads.append(shader_payload)

with open(MISSING_CONTROLS_FILE, 'w') as outfile:
    outfile.write(json.dumps(payloads, indent="  "))

print(f'wrote {MISSING_CONTROLS_FILE}')