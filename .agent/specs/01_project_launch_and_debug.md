# Project Launch and Debug
This document provides specifications for launching and debugging the LXStudio-TE project. It outlines the necessary steps, tools, and procedures to ensure successful project initialization and troubleshooting.

## Prerequisites

Before launching the TE application, ensure you have:
- **Java 21** (Temurin) installed
- **Maven 3.3.1+** installed
- **Git** for version control

## Build Steps

### 1. Clone the Repository (if not already done)
```powershell
git clone https://github.com/titanicsend/LXStudio-TE.git
cd LXStudio-TE
```

### 2. Build the Project
Navigate to the te-app directory and build the project with Maven:

```powershell
cd te-app
mvn clean compile
```

This will:
- Clean any previous build artifacts
- Compile all Java source files (373 source files)
- Validate dependencies

### 3. Package the Application
Create the executable JAR with all dependencies:

```powershell
mvn package -DskipTests
```

This creates: `target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar`

**Note:** The build takes approximately 1-2 minutes and may show warnings (these are non-critical).

## Launch LX/Chromatik For TE

### GUI Mode (With User Interface)
To launch the TE application with the graphical user interface:

```powershell
cd te-app
java -ea -Dgpu -cp target\te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp --resolution 1080x1400
```

### Headless Mode (No GUI)
For testing or server deployments without a display:

```powershell
cd te-app
java -ea '-Djava.awt.headless=true' -Dgpu -cp target\te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp --resolution 1080x1400
```

## Launch Parameters Explained

| Parameter | Description |
|-----------|-------------|
| `-ea` | Enable assertions for debugging |
| `-Djava.awt.headless=true` | Run without GUI (headless mode) |
| `-Dgpu` | Enable GPU rendering mode |
| `-cp` | Classpath - points to the JAR file |
| `heronarts.lx.studio.TEApp` | Main class to execute |
| `--resolution 1080x1400` | Canvas resolution (WxH in pixels) |

## Alternative Resolutions

You can adjust the resolution based on your needs:
- Low res (testing): `--resolution 512x512`
- Medium res: `--resolution 1080x1400`
- High res: `--resolution 2048x2048`

## Common Issues and Solutions

### Issue: Shader Cache Errors
**Error:** `FileNotFoundException: resources\shaders\cache\*.bin`

**Solution:** The shader cache directory may not exist. It will be created automatically on first run, or you can create it manually:
```powershell
mkdir te-app\resources\shaders\cache
```

### Issue: "Cannot find main class"
**Error:** `Could not find or load main class`

**Solution:** Ensure you're running from the `te-app` directory and the JAR file exists in `target/`.

### Issue: NDI Library Crash
**Error:** `EXCEPTION_ACCESS_VIOLATION` in `Processing.NDI.Lib.x64.dll`

**Solution:** This is a known issue with NDI initialization in headless mode. Run in GUI mode instead, or the crash can be safely ignored if it occurs during shutdown.

### Issue: Resolution Too Small
**Warning:** `GLEngine resolution too small for number of points in the model`

**Solution:** Increase the resolution parameter, e.g., `--resolution 2048x2048`

## Project Structure

```
LXStudio-TE/
├── te-app/                          # Main application module
│   ├── src/main/java/              # Java source files
│   │   └── titanicsend/
│   │       ├── effect/             # Effects (including ShaderStrobe)
│   │       ├── pattern/            # Patterns
│   │       └── ...
│   ├── resources/                  # Application resources
│   │   ├── shaders/               # GLSL shader files
│   │   │   ├── cache/            # Compiled shader binaries
│   │   │   └── *.fs              # Fragment shaders
│   │   ├── model/                # 3D model data
│   │   └── pattern/              # Pattern configurations
│   ├── target/                    # Build output
│   │   └── te-app-*-jar-with-dependencies.jar
│   └── pom.xml                    # Maven configuration
├── Projects/                       # LX project files (.lxp)
├── Presets/                        # User presets
└── pom.xml                        # Parent Maven configuration
```

## Debugging

### Enable Debug Logging
Add the following JVM argument for verbose logging:
```powershell
java -ea -Dgpu -Dlx.log=DEBUG -cp target\te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp --resolution 1080x1400
```

### Check for Compile Errors
Before running, validate the build:
```powershell
mvn clean compile
```
Review any `[ERROR]` or `[WARNING]` messages in the output.

### Verify Shader Compilation
When adding new shaders, check the logs for:
```
Creating Shader class: titanicsend.effect.ShaderStrobe for shader_strobe.fs
```

## Successful Launch Indicators

When the application launches successfully, you should see:
```
[LX 2025/10/24] Starting Chromatik version 1.1.1-TE.5.GPU-SNAPSHOT
[GLX] CLI args: --resolution 1080x1400
[LX] GLEngine: Rendering canvas size: 1080x1400 = 1512000 total points
[LX] Project loaded successfully from .\Projects\CoG_2025.lxp
[LX] NDI: Successfully initialized sender 'TitanicsEnd'
```

## Quick Reference Commands

### Full Build and Run (GUI)
```powershell
cd te-app
mvn clean package -DskipTests
java -ea -Dgpu -cp target\te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp --resolution 1080x1400
```

### Rebuild Only (After Code Changes)
```powershell
cd te-app
mvn compile
mvn package -DskipTests
```

### Run Without Rebuild
```powershell
cd te-app
java -ea -Dgpu -cp target\te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp --resolution 1080x1400
```
