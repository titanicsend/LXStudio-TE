# Maven Build System for LXStudio-TE

## Project Structure

LXStudio-TE uses a multi-module Maven project structure:

- **Root Project**: `/pom.xml` - Parent POM managing shared configuration
- **Main Application**: `te-app/pom.xml` - Core LX Studio application with Titanics End customizations
- **Audio Stems**: `audio-stems/pom.xml` - Audio processing utilities module

## Key Build Information

### Java Version

- **Target**: Java 21 (release 21)
- **Runtime**: Oracle Corporation Java 24.0.1 (observed during execution)
- **Platform**: Mac OS X 15.5 aarch64

### Maven Configuration

- **Maven Version**: 3.9.11 (Homebrew installation)
- **Assembly Plugin**: Creates fat JAR with dependencies
- **Surefire Plugin**: 3.2.5 for test execution
- **Compiler Plugin**: 3.12.1 with debug and release=21

## Essential Build Commands

### Development Workflow (Recommended)

```bash
# Quick compilation after code changes
cd te-app && mvn compile

# Package without tests (faster)
mvn package -DskipTests

# Full build with tests
mvn package
```

### ⚠️ Performance Notes

- **Avoid `mvn clean`** during development - significantly slows rebuilds
- Use incremental compilation: `mvn compile` for quick iterations
- Assembly phase takes ~1m20s for fat JAR creation

### Launch Configuration

```bash
# GPU-enabled launch (matches VSCode configuration)
java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu \
  -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar \
  --resolution 1920x1200 Projects/BM2024_TE.lxp
```

## Key Dependencies and Frameworks

### Graphics and Rendering

- **LWJGL**: OpenGL/Metal bindings for GPU operations
- **BGFX**: Cross-platform rendering (Metal on macOS)
- **GLX**: OpenGL extensions and utilities

### Audio Processing

- **AudioStems Plugin**: Version 0.1.0-SNAPSHOT
- **LX Audio Engine**: Real-time audio analysis

### NDI Integration

- **Devolay**: Java bindings for NDI (Network Device Interface)
- Classes: `DevolaySender`, `DevolayReceiver`, `DevolayVideoFrame`

### UI Framework

- **Chromatik**: Version 1.1.1-TE.3.GPU-SNAPSHOT (LX Studio variant)
- **SuperMod Plugin**: Version 0.1.5-SNAPSHOT

## Build Warnings and Issues

### Known Warnings (Non-blocking)

```
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers
```

### Deprecation Warnings

- Some input files use deprecated APIs in `DmxPattern.java`
- Recompile with `-Xlint:deprecation` for details

## Local Repository

Custom Maven repository at `repo/` contains:

- **heronarts**: LX framework variants (lx, glx, glxstudio)
- **devolay**: NDI bindings version 2.1.0-te
- **gigglepixel**: Custom utilities version 0.0.3
- **SuperMod**: Modulation framework 0.1.5-SNAPSHOT

## GPU and Native Libraries

### GPU Support Requirements

- **macOS**: `-XstartOnFirstThread` JVM argument mandatory
- **GPU Flag**: `-Dgpu` enables GPU-accelerated rendering
- **Headless Mode**: `-Djava.awt.headless=true` for server deployment

### Native Access

- LWJGL loads native libraries via `System::load`
- Future Java versions may require `--enable-native-access=ALL-UNNAMED`

## External Libraries and Plugins

### Building External Dependencies

LXStudio-TE supports external plugins and libraries that can be developed separately and included as Maven dependencies.

#### Development Workflow for External Libraries

1. **Create External Project** (e.g., OscRemapper plugin)
```bash
# Structure outside main project
/Users/sinas/workspace/
├── LXStudio-TE/           # Main application
├── OscRemapper/           # External plugin
├── LX/                    # LX framework (for local dev)
└── Beyond/                # Beyond plugin (for local dev)
```

2. **Build and Install External Library**
```bash
cd /path/to/external-library
mvn clean install -DskipTests
```

3. **Add Dependency to te-app**
```xml
<!-- te-app/pom.xml -->
<dependency>
    <groupId>magic</groupId>
    <artifactId>oscremapper</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

4. **Build te-app with External Dependencies**
```bash
cd te-app
mvn package -DskipTests
```

### Example: OscRemapper Plugin Integration

**Plugin Structure:**
```
OscRemapper/
├── pom.xml                           # groupId: magic, artifactId: oscremapper
├── src/main/java/magic/oscremapper/
│   ├── OscRemapperPlugin.java        # Main plugin class
│   ├── modulator/                    # OSC modulators
│   ├── parameter/                    # OSC parameters
│   └── ui/                          # Plugin UI components
└── src/main/resources/
    ├── oscremapper.properties        # Plugin metadata
    └── sync-config.yaml             # OSC configuration
```

**Integration Steps:**
1. Plugin is built and installed to local Maven repository (`~/.m2/repository/magic/oscremapper/`)
2. te-app references it as a dependency
3. TEApp.java instantiates and manages the plugin lifecycle:
   ```java
   import magic.oscremapper.OscRemapperPlugin;
   
   private final OscRemapperPlugin oscRemapperPlugin;
   this.oscRemapperPlugin = new OscRemapperPlugin(lx);
   ```

### Local Development with External Libraries

For rapid development cycles with external libraries:

1. **Install External Library Locally**
```bash
cd /path/to/external-lib
mvn install -DskipTests
```

2. **Incremental Development**
```bash
# After changes to external library
cd external-library && mvn install -DskipTests
cd ../LXStudio-TE/te-app && mvn compile
```

3. **IDE Integration**
- Import external projects as Maven modules in IntelliJ/Eclipse
- Changes are automatically picked up during development
- No need to reinstall for every change when using IDE

### Repository Dependencies

External libraries can reference:
- **Local Repository**: `file://${basedir}/../LXStudio-TE/repo` for TE-specific dependencies
- **Maven Central**: Standard dependencies
- **Local Maven Cache**: `~/.m2/repository/` for installed external libraries

## Project Versioning

- **Main Version**: 0.3.0-SNAPSHOT
- **LX Framework**: 1.1.1-TE.3.GPU-SNAPSHOT (Titanics End GPU variant)
- **Build Artifact**: `te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar`
