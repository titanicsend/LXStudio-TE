# Repository Structure

This document provides an overview of the LXStudio-TE repository structure and explains the purpose of each directory and key files.

## Core Directories

### src/
The source code directory containing the main application code:

- `main/` - Primary application source code
- `test/` - Test files and test-related resources

### resources/
Contains various resource files used by the application:

- `shaders/` - Shader programs for graphics rendering
- `vehicle/` - Vehicle-related resources and configurations
- `docs/` - Documentation files
- `stems/` - Audio stem files
- `pixelblaze/` - Pixelblaze pattern files
- `scripts/` - Utility scripts
- `pattern/` - Pattern definitions
- `fonts/` - Font files used in the application
- `gamecontrollers/` - Game controller configurations

### Projects/
Contains project files and configurations:

- `.lxp` files - LX Studio project files for different configurations and shows
- `Archive/` - Historical project files
- Various project files including:
  - BM2024 related projects
  - Test configurations
  - Grid layouts
  - Show configurations

### Presets/
Stores preset configurations for different aspects of the system.

### Models/
Contains 3D models and model-related files.

### Fixtures/
Lighting fixture definitions and configurations.

## Configuration and Build

### .run/
Contains run configurations for different environments.

### .settings/
IDE and project-specific settings.

### .vscode/
Visual Studio Code specific settings and configurations.

## Build and Dependency Files

- `pom.xml` - Maven project configuration file
- `backup-pom.xml` - Backup of the Maven configuration
- `LXStudio-TE.iml` - IntelliJ IDEA module file

## Scripts and Execution

- `run_chromatik_linux.sh` - Linux startup script
- `run_chromatik_mac.sh` - macOS startup script
- `setup_network.sh` - Network configuration script
- `start_sk.sh` - ShowKontrol startup script
- `run_stem_splitter.sh` - Audio stem splitting utility

## Documentation

- `README.md` - Main project documentation
- `README-linux.md` - Linux-specific instructions
- `NETWORK_LINUX.md` - Network setup documentation for Linux
- `LICENSE.md` - Project license information

## Runtime and Generated

### Logs/
Contains application log files.

### Autosave/
Stores automatic saves of project states.

### target/
Contains compiled files and build artifacts.

## Application Bundles

- `RunTEApplication.app/` - Main application bundle
- `RunMothershipApplication.app/` - Mothership application bundle
- `RunMaxMSP.app/` - MaxMSP runtime bundle
- `Launch ShowKontrol.app/` - ShowKontrol application bundle

## Additional Resources

### Colors/
Color palette definitions and configurations.

### TD/
TouchDesigner related files and configurations.

## Version Control

- `.git/` - Git repository data
- `.gitignore` - Git ignore rules
- `pre-commit` - Git pre-commit hook script 