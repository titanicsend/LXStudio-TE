#!/bin/bash

# Ensure we're in the correct directory
cd "$(dirname "$0")"

# Detect OS and architecture
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

# Convert architecture names to standard format
case ${ARCH} in
    x86_64)
        ARCH="amd64"
        LWJGL_ARCH="x86_64"  # Changed to match LWJGL's expected format
        ;;
    aarch64)
        ARCH="arm64"
        LWJGL_ARCH="arm64"
        ;;
    *)
        echo "Unsupported architecture: ${ARCH}"
        exit 1
        ;;
esac

echo "Detected OS: ${OS}, Architecture: ${ARCH}, LWJGL Architecture: ${LWJGL_ARCH}"

# Set environment variables for Linux
export BGFX_RUNTIME_OPENGL=1
export BGFX_RUNTIME_VULKAN=0

# Set LWJGL debug mode for detailed diagnostics
export _JAVA_OPTIONS="${_JAVA_OPTIONS} -Dorg.lwjgl.util.Debug=true"

# Ensure native library paths are correctly set
NATIVES_PATH="${PWD}/target/natives-linux"
if [ ! -d "$NATIVES_PATH" ]; then
    echo "Error: Native libraries directory not found at $NATIVES_PATH"
    exit 1
fi

# Function to check if rebuild is needed
needs_rebuild() {
    local target_jar="target/LXStudio-TE-0.2.3-SNAPSHOT-jar-with-dependencies.jar"
    
    # If target doesn't exist, rebuild is needed
    if [ ! -f "$target_jar" ]; then
        echo "Target JAR not found. Build needed."
        return 0
    fi
    
    # Check if any source files are newer than the target
    if find src -type f -newer "$target_jar" 2>/dev/null | grep -q .; then
        echo "Source files have changed. Build needed."
        return 0
    fi
    
    # Check if pom.xml is newer than the target
    if [ "pom.xml" -nt "$target_jar" ]; then
        echo "POM file has changed. Build needed."
        return 0
    fi
    
    echo "No rebuild needed. Using existing build."
    return 1
}

# Build only if needed
if needs_rebuild; then
    echo "Building for ${OS}-${ARCH}..."
    mvn -X clean package \
        -Dos.detected.name=${OS} \
        -Dos.detected.arch=${ARCH} \
        -Dmaven.test.skip=true \
        -Djavafx.platform=${OS}-${ARCH}
else
    echo "Skipping build - using existing JAR"
fi

# Run Chromatik with platform-specific options
echo "Running Chromatik..."
java -verbose \
     -Djava.awt.headless=true \
     -Dos.name=Linux \
     -Dos.arch=${LWJGL_ARCH} \
     -Dorg.lwjgl.librarypath=${NATIVES_PATH} \
     -Djava.library.path=${NATIVES_PATH} \
     -XX:+UseCompressedOops \
     -XX:+UseG1GC \
     -Djavax.accessibility.assistive_technologies=" " \
     -jar target/LXStudio-TE-*-SNAPSHOT-jar-with-dependencies.jar \
     vehicle Vehicle.lxp 2>&1 | tee chromatik.log 