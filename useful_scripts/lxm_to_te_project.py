#!/usr/bin/env python3
"""
LXM to TE Project Converter

Converts any .lxm file with LED strips into a complete TE project with:
- Individual .lxf fixture files for each strip
- Main fixture file that includes all strips
- Complete TE project file with all channels and patterns
- IntelliJ run configuration

Usage:
    python lxm_to_te_project.py path/to/your/model.lxm [output_name]

Example:
    python lxm_to_te_project.py MyLEDs.lxm MyProject
    python lxm_to_te_project.py /path/to/strips.lxm CustomDisplay
"""

import json
import os
import sys
import re
from pathlib import Path

def extract_strip_data(lxm_file_path):
    """Extract strip data from any LXM file"""
    try:
        with open(lxm_file_path, 'r') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"❌ Error: File '{lxm_file_path}' not found!")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ Error: Invalid JSON in '{lxm_file_path}': {e}")
        sys.exit(1)
    
    strips = []
    for fixture in data.get('fixtures', []):
        if fixture.get('class') == 'heronarts.lx.structure.StripFixture':
            params = fixture['parameters']
            
            # Extract strip number from label (e.g., "Strip 1" -> 1)
            label = params.get('label', '')
            strip_match = re.search(r'Strip\s+(\d+)', label, re.IGNORECASE)
            if strip_match:
                strip_id = int(strip_match.group(1))
            else:
                # Fallback: use a counter if no number found
                strip_id = len(strips) + 1
                print(f"⚠️  Warning: Could not extract strip number from '{label}', using {strip_id}")
            
            strip_data = {
                'id': strip_id,
                'label': label,
                'numPoints': params.get('numPoints', 50),
                'spacing': params.get('spacing', 1.0),
                'x': params.get('x', 0.0),
                'y': params.get('y', 0.0), 
                'z': params.get('z', 0.0),
                'yaw': params.get('yaw', 0.0),
                'pitch': params.get('pitch', 0.0),
                'roll': params.get('roll', 0.0),
                'host': params.get('host', '127.0.0.1'),
                'universe': params.get('artNetUniverse', 0),
                'dmxChannel': params.get('dmxChannel', 1),
                'reverse': params.get('reverse', False)
            }
            strips.append(strip_data)
    
    if not strips:
        print(f"❌ Error: No strip fixtures found in '{lxm_file_path}'!")
        print("   Make sure the file contains StripFixture objects.")
        sys.exit(1)
    
    # Sort by strip ID
    strips.sort(key=lambda x: x['id'])
    return strips

def create_fixture_file(strip, project_name):
    """Create a .lxf fixture file for a single strip"""
    fixture_content = f'''{{
  /* {project_name} Display Fixture File */
  label: "Strip {strip['id']}",
  tags: [ "strip{strip['id']}", "strip", "{project_name.lower()}" ],

  parameters: {{
    "points": {{ default: {strip['numPoints']}, type: "int", min: 1, label: "Points", description: "Number of points in the strip" }},
    "host": {{ default: "{strip['host']}", type: "string", label: "Host", description: "Controller IP address or hostname" }},    
    "universe": {{ default: {strip['universe']}, type: "int", min: 0, label: "Universe", description: "ArtNet universe number" }},
    "dmxChannel": {{ default: {strip['dmxChannel']}, type: "int", min: 1, label: "DMX Channel", description: "Starting DMX channel" }},
    "artnetSequence": {{ default: false, type: "boolean", label: "ArtNet Sequence", description: "Whether ArtNet sequence packets are enabled" }},
    "reverse": {{ default: {str(strip['reverse']).lower()}, type: "boolean", label: "Reverse", description: "Reverse the output direction" }},
    
    /* Debug tools */
    "onCar": {{ default: false, type: "boolean", label: "On Display", description: "True = Locate strip to its position on the display, False = Locate to origin" }},

    /* Option to disable output, such as when outputs are configured in a parent fixture */
    "hasOutput": {{ default: true, type: "boolean", label: "Has Output", description: "Whether built-in output is enabled" }}
  }},
  
  transforms: [
    /* Position the strip in 3D space */
    {{ x: "{strip['x']}", enabled: "$onCar" }},
    {{ y: "{strip['y']}", enabled: "$onCar" }},
    {{ z: "{strip['z']}", enabled: "$onCar" }},

    /* Rotate the strip */
    {{ yaw: "{strip['yaw']}", enabled: "$onCar" }},
    {{ pitch: "{strip['pitch']}", enabled: "$onCar" }},
    {{ roll: "{strip['roll']}", enabled: "$onCar" }}
  ],

  meta: {{
    "stripId": "{strip['id']}",
    "{project_name.lower()}Display": "true"
  }},

  components: [
    {{ type: "strip",
      numPoints: "$points",
      spacing: "{strip['spacing']}"
    }}
  ],
  
  outputs: [
    {{ enabled: "$hasOutput",
      host: "$host",
      universe: "$universe",
      channel: "$dmxChannel",
      protocol: "artnet", 
      sequenceEnabled: "$artnetSequence",
      reverse: "$reverse"
    }}
  ]
}}
'''
    
    # Create directory and write file
    fixture_dir = f"te-app/Fixtures/{project_name}/strip"
    os.makedirs(fixture_dir, exist_ok=True)
    
    filename = f"{fixture_dir}/Strip{strip['id']}.lxf"
    with open(filename, 'w') as f:
        f.write(fixture_content)
    
    return filename

def create_main_fixture(strips, project_name):
    """Create the main fixture that includes all strips"""
    components = []
    for strip in strips:
        components.append(f'    {{ type: "strip/Strip{strip["id"]}", artnetSequence: "$artnetSequence" }}')
    
    components_str = ',\n'.join(components)
    
    fixture_content = f'''{{
  /* {project_name} Display Main Fixture File */
  label: "{project_name}",
  tags: [ "{project_name.lower()}", "display" ],
  
  parameters: {{
    "artnetSequence": {{ default: false, type: "boolean", label: "ArtNet Sequence", description: "Whether ArtNet sequence packets are enabled" }},
    "showBacking": {{ type: "boolean", default: "false", label: "Backings", description: "Whether to display an opaque backing behind the pixels" }}
  }},
  
  components: [
{components_str}
  ]
}}
'''
    
    fixture_dir = f"te-app/Fixtures/{project_name}"
    os.makedirs(fixture_dir, exist_ok=True)
    
    filename = f"{fixture_dir}/{project_name}.lxf"
    with open(filename, 'w') as f:
        f.write(fixture_content)
    
    return filename

def create_all_fixture_entries(strips, project_name):
    """Create fixture entries for all strips for the project file"""
    fixtures = []
    
    for strip in strips:
        fixture = {
            "jsonFixtureType": f"{project_name}/strip/Strip{strip['id']}",
            "jsonParameters": {},
            "id": 471992 + strip['id'],
            "class": "heronarts.lx.structure.JsonFixture",
            "internal": {
                "modulationColor": 0,
                "modulationControlsExpanded": True,
                "modulationsExpanded": True
            },
            "parameters": {
                "label": f"Strip {strip['id']}",
                "x": strip['x'],
                "y": strip['y'],
                "z": strip['z'],
                "yaw": strip['yaw'],
                "pitch": strip['pitch'],
                "roll": strip['roll'],
                "scale": 1.0,
                "hasCustomPointSize": False,
                "pointSize": 5.0,
                "selected": False,
                "deactivate": False,
                "enabled": True,
                "brightness": 1.0,
                "identify": False,
                "mute": False,
                "solo": False,
                "tags": "",
                "fixtureType": f"{project_name}/strip/Strip{strip['id']}"
            },
            "children": {}
        }
        fixtures.append(fixture)
    
    return fixtures

def create_project_file(strips, project_name):
    """Create the complete TE project file by copying BM2024_TE and replacing fixtures"""
    
    # Read the master TE project file
    te_project_file = "te-app/Projects/BM2024_TE.lxp"
    if not os.path.exists(te_project_file):
        print(f"❌ Error: Could not find {te_project_file}!")
        print("   Make sure you're running this from the LXStudio-TE root directory.")
        sys.exit(1)
    
    with open(te_project_file, 'r') as f:
        lines = f.readlines()
    
    # Find fixtures section
    fixtures_start = None
    fixtures_end = None
    
    for i, line in enumerate(lines):
        if '"fixtures": [' in line:
            fixtures_start = i
        elif fixtures_start is not None and line.strip() == ']' and i > fixtures_start + 10:
            fixtures_end = i + 1
            break
    
    if fixtures_start is None or fixtures_end is None:
        print("❌ Error: Could not find fixtures section in TE project file!")
        sys.exit(1)
    
    # Get parts before and after fixtures
    before_fixtures = lines[:fixtures_start]
    after_fixtures = lines[fixtures_end:]
    
    # Update view selectors to use our project name
    for i, line in enumerate(before_fixtures):
        if '"selector":' in line and ('912;983;9114' in line or 'Edge' in line or 'Panel' in line or 'outer' in line):
            before_fixtures[i] = f'              "selector": "{project_name.lower()}",\n'
    
    # Create all fixtures
    all_fixtures = create_all_fixture_entries(strips, project_name)
    
    # Build fixtures section
    fixtures_lines = ['    "fixtures": [\n']
    
    for i, fixture in enumerate(all_fixtures):
        fixture_json = json.dumps(fixture, indent=6)
        fixture_lines = fixture_json.split('\n')
        fixture_lines[0] = '      ' + fixture_lines[0]
        for j in range(1, len(fixture_lines)):
            if fixture_lines[j].strip():
                fixture_lines[j] = '      ' + fixture_lines[j]
        
        fixtures_lines.extend([line + '\n' for line in fixture_lines])
        
        if i < len(all_fixtures) - 1:
            fixtures_lines[-1] = fixtures_lines[-1].rstrip() + ',\n'
    
    fixtures_lines.append('    ]\n')
    
    # Combine everything
    new_lines = before_fixtures + fixtures_lines + after_fixtures
    
    # Write project file
    project_file = f"te-app/Projects/BM2024_{project_name}.lxp"
    with open(project_file, 'w') as f:
        f.writelines(new_lines)
    
    return project_file

def create_run_configuration(project_name):
    """Create IntelliJ run configuration"""
    run_config = f'''<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="{project_name} with TE Channels" type="Application" factoryName="Application">
    <option name="ALTERNATIVE_JRE_PATH" value="temurin-21" />
    <option name="MAIN_CLASS_NAME" value="heronarts.lx.studio.TEApp" />
    <module name="te-app" />
    <option name="PROGRAM_PARAMETERS" value="Projects/BM2024_{project_name}.lxp" />
    <option name="VM_PARAMETERS" value="-ea -XstartOnFirstThread -Djava.awt.headless=true " />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$/te-app" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>'''
    
    os.makedirs(".run", exist_ok=True)
    config_file = f".run/{project_name} with TE Channels.run.xml"
    with open(config_file, 'w') as f:
        f.write(run_config)
    
    return config_file

def main():
    if len(sys.argv) < 2:
        print("❌ Usage: python lxm_to_te_project.py <lxm_file> [project_name]")
        print("\nExamples:")
        print("  python lxm_to_te_project.py MyLEDs.lxm")
        print("  python lxm_to_te_project.py /path/to/strips.lxm CustomDisplay")
        sys.exit(1)
    
    lxm_file = sys.argv[1]
    
    # Determine project name
    if len(sys.argv) >= 3:
        project_name = sys.argv[2]
    else:
        # Use filename without extension
        project_name = Path(lxm_file).stem
    
    # Clean project name (remove invalid characters)
    project_name = re.sub(r'[^a-zA-Z0-9_]', '', project_name)
    if not project_name:
        project_name = "CustomLEDs"
    
    print(f"🎯 Converting {lxm_file} to TE project '{project_name}'...")
    
    # Extract strip data
    strips = extract_strip_data(lxm_file)
    print(f"   Found {len(strips)} LED strips")
    
    # Create individual strip fixture files
    print(f"   Creating {len(strips)} individual strip fixtures...")
    for strip in strips:
        create_fixture_file(strip, project_name)
    
    # Create main fixture file
    print(f"   Creating main {project_name} fixture...")
    main_fixture = create_main_fixture(strips, project_name)
    
    # Create complete project file
    print(f"   Creating complete TE project file...")
    project_file = create_project_file(strips, project_name)
    
    # Create run configuration
    print(f"   Creating IntelliJ run configuration...")
    run_config = create_run_configuration(project_name)
    
    print(f"\n🎉 Conversion Complete!")
    print(f"   ✅ {len(strips)} strip fixtures created")
    print(f"   ✅ Main fixture: {main_fixture}")
    print(f"   ✅ Project file: {project_file}")
    print(f"   ✅ Run config: {run_config}")
    print(f"\n🚀 To use:")
    print(f"   1. Open IntelliJ")
    print(f"   2. Look for '{project_name} with TE Channels' in run configurations")
    print(f"   3. Click ▶️ to launch with all TE channels and your {len(strips)} LED strips!")

if __name__ == "__main__":
    main()
