# LXM to TE Project Converter

A utility script that converts any `.lxm` file with LED strips into a complete TE project with individual fixture files, main fixture, project file, and IntelliJ run configuration.

## Overview

This script takes an existing `.lxm` model file containing LED strip fixtures and automatically generates all the necessary files to create a complete, runnable TE project. It's perfect for converting existing LED configurations into the TE framework format.

## Usage

```bash
python3 lxm_to_te_project.py <lxm_file> [project_name]
```

### Parameters

- `lxm_file`: Path to the source `.lxm` file containing LED strips (required)
- `project_name`: Optional custom name for the project (defaults to filename)

## Examples

### Basic conversion using filename
```bash
python3 lxm_to_te_project.py MyLEDs.lxm
# Creates project named "MyLEDs"
```

### Custom project name
```bash
python3 lxm_to_te_project.py /path/to/strips.lxm CustomDisplay
# Creates project named "CustomDisplay"
```

### Convert existing model
```bash
python3 lxm_to_te_project.py te-app/Models/Arta.lxm PacmanDisplay
# Creates project named "PacmanDisplay"
```

## What Gets Created

The script generates a complete project structure:

### 1. Individual Strip Fixtures
- **Location**: `te-app/Fixtures/{ProjectName}/strip/`
- **Files**: `Strip1.lxf`, `Strip2.lxf`, etc.
- **Purpose**: Individual fixture files for each LED strip with all parameters

### 2. Main Fixture File
- **Location**: `te-app/Fixtures/{ProjectName}/{ProjectName}.lxf`
- **Purpose**: Master fixture that includes all individual strips

### 3. Complete Project File
- **Location**: `te-app/Projects/BM2024_{ProjectName}.lxp`
- **Purpose**: Ready-to-run project file with all strips and TE channels

### 4. IntelliJ Run Configuration
- **Location**: `.run/{ProjectName} with TE Channels.run.xml`
- **Purpose**: One-click launch configuration for IntelliJ

## Features

- **Automatic strip detection**: Finds all StripFixture objects in the source file
- **Parameter preservation**: Maintains positions, rotations, LED counts, and DMX settings
- **Smart naming**: Extracts strip numbers from labels or assigns sequential IDs
- **Complete integration**: Generated project includes all TE channels and functionality
- **Ready to run**: Creates everything needed for immediate use

## Extracted Strip Data

From each strip in the source `.lxm`, the script extracts:

- **Label**: Strip name/identifier
- **LED count**: Number of points in the strip
- **Position**: X, Y, Z coordinates
- **Rotation**: Yaw, pitch, roll values
- **DMX settings**: Host IP, universe, channel
- **Configuration**: Reverse setting, spacing

## Output Structure

```
te-app/
├── Fixtures/
│   └── YourProject/
│       ├── YourProject.lxf          # Main fixture
│       └── strip/
│           ├── Strip1.lxf           # Individual strips
│           ├── Strip2.lxf
│           └── ...
├── Projects/
│   └── BM2024_YourProject.lxp       # Complete project
└── ...

.run/
└── YourProject with TE Channels.run.xml  # IntelliJ config
```

## Example Output

```
$ python3 lxm_to_te_project.py te-app/Models/Arta.lxm PacmanDisplay

🎯 Converting te-app/Models/Arta.lxm to TE project 'PacmanDisplay'...
   Found 67 LED strips
   Creating 67 individual strip fixtures...
   Creating main PacmanDisplay fixture...
   Creating complete TE project file...
   Creating IntelliJ run configuration...

🎉 Conversion Complete!
   ✅ 67 strip fixtures created
   ✅ Main fixture: te-app/Fixtures/PacmanDisplay/PacmanDisplay.lxf
   ✅ Project file: te-app/Projects/BM2024_PacmanDisplay.lxp
   ✅ Run config: .run/PacmanDisplay with TE Channels.run.xml

🚀 To use:
   1. Open IntelliJ
   2. Look for 'PacmanDisplay with TE Channels' in run configurations
   3. Click ▶️ to launch with all TE channels and your 67 LED strips!
```

## Generated Fixture Features

Each created strip fixture includes:

- **Configurable parameters**: Points, host, universe, DMX channel, reverse
- **Position controls**: X, Y, Z coordinates with "On Display" toggle
- **Rotation controls**: Yaw, pitch, roll
- **Debug tools**: Strip identification and positioning helpers
- **Output controls**: ArtNet configuration with sequence options

## Use Cases

### Project Migration
```bash
# Convert existing model to new project format
python3 lxm_to_te_project.py old_model.lxm NewProjectName
```

### Template Creation
```bash
# Create project template from working configuration
python3 lxm_to_te_project.py working_config.lxm BaseTemplate
```

### Multi-Configuration Setup
```bash
# Create different project variants
python3 lxm_to_te_project.py base.lxm ProjectA
python3 lxm_to_te_project.py base.lxm ProjectB
```

## Requirements

- **Source file**: Valid `.lxm` file with StripFixture objects
- **Base project**: `te-app/Projects/BM2024_TE.lxp` must exist
- **Working directory**: Must run from LXStudio-TE root directory

## Error Handling

- **File validation**: Checks for source file existence and valid JSON
- **Strip detection**: Warns if no strips found or labels can't be parsed
- **Dependencies**: Verifies required base files exist
- **Naming**: Sanitizes project names for filesystem compatibility

## Integration with Other Tools

Works well with other utilities:

```bash
# Convert and then analyze
python3 lxm_to_te_project.py model.lxm MyProject
python3 comprehensive_fixes_logger.py te-app/Projects/BM2024_MyProject.lxp A

# Convert and backup
python3 lxm_to_te_project.py model.lxm MyProject
python3 make_backup.py te-app/Projects/BM2024_MyProject.lxp --suffix "initial_conversion"
```

## Technical Details

- **Base template**: Uses `BM2024_TE.lxp` as the foundation
- **ID generation**: Assigns sequential IDs starting from 471992
- **JSON structure**: Maintains proper LX Studio project format
- **View integration**: Updates selectors to use project tags

## Dependencies

- Python 3
- Standard libraries: `json`, `os`, `sys`, `re`, `pathlib`

This tool streamlines the process of creating new TE projects from existing LED configurations, making it easy to adapt the framework for different installations.
