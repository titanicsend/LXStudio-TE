## Titanic's End LXStudio (Linux Guide)

[Titanic's End](http://titanicsend.com) is a mutant vehicle that debuted at [Burning Man](https://burningman.org) 2022 and has since participated in EDC and Framework events.

We are the largest team developing on Chromatik in the open so other artists can benefit from our work (see the [license](LICENSE.md)). This repo contains the shader & pattern code for the 128,000 LEDS, sound reactivity, MIDI control, OSC middleware, and ArtNet bidirectional control.

We use [Chromatik](https://chromatik.co/) (formerly known as [LX Studio](https://lx.studio/)) to control the show. Although not required, we also support Chromatik financially because it's excellent. This README will get you up and running with it on Linux so you, too, can create stunning LED art.

Our work is notable for:

* AutoVJ - an autopilot that uses Pioneer Rekordbox's phrase and beat analysis to change patterns when a human VJ would
* GLSL shader support
* Developed to team-friendly maintainability standards for long-term enjoyment of the codebase
* GigglePixel, Pixelblaze, and ArtNet integration

## Linux Prerequisites

### JDK Installation

```sh
# Add Adoptium repository
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# Update package list and install Temurin 17
sudo apt update
sudo apt install temurin-17-jdk
```

Verify your installation:
```sh
java --version
```

### Maven Installation

```sh
# Install Maven
sudo apt install maven

# Clean and install the maven dependencies
mvn clean -U package && mvn install
```

### Graphics Libraries
```sh
# Install required graphics libraries
sudo apt install libgl1-mesa-dev mesa-common-dev libvulkan-dev
```

### Google Java Format Installation

There are several ways to install google-java-format:

1. Using the JAR file (recommended):
```sh
# Create a directory for the tool
mkdir -p ~/.local/bin
cd ~/.local/bin

# Download the latest version
wget https://github.com/google/google-java-format/releases/download/v1.17.0/google-java-format-1.17.0-all-deps.jar

# Create a wrapper script
echo '#!/bin/bash
java -jar ~/.local/bin/google-java-format-1.17.0-all-deps.jar "$@"' > ~/.local/bin/google-java-format

# Make it executable
chmod +x ~/.local/bin/google-java-format

# Add to PATH if not already there
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

2. Using SDKMAN! (if you have it installed):
```sh
sdk install google-java-format
```

Verify the installation:
```sh
google-java-format --version
```

## IDE Setup

You'll need IntelliJ IDEA Community Edition. Here are your installation options:

1. Using the JetBrains Toolbox (Recommended):
```sh
# Download JetBrains Toolbox
curl -fsSL https://raw.githubusercontent.com/nagygergo/jetbrains-toolbox-install/master/jetbrains-toolbox.sh | bash

# Launch JetBrains Toolbox and install IntelliJ IDEA Community Edition from the UI
```

2. Using the official .tar.gz (Alternative):
```sh
# Download and extract to /opt
sudo wget -O /opt/ideaIC.tar.gz https://download.jetbrains.com/idea/ideaIC-2023.3.4.tar.gz
cd /opt && sudo tar -xzf ideaIC.tar.gz
sudo rm ideaIC.tar.gz

# Create a desktop entry
echo "[Desktop Entry]
Version=1.0
Type=Application
Name=IntelliJ IDEA Community
Icon=/opt/idea-IC-*/bin/idea.svg
Exec="/opt/idea-IC-*/bin/idea.sh" %f
Comment=Capable and Ergonomic IDE for JVM
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-idea-ce" | sudo tee /usr/share/applications/jetbrains-idea-ce.desktop

# Make it executable
sudo chmod +x /usr/share/applications/jetbrains-idea-ce.desktop
```

3. Using snap (Not recommended due to potential plugin issues):
```sh
sudo snap install intellij-idea-community --classic
```

## Project Setup

1. Clone the git repo:
   ```sh
   git clone https://github.com/titanicsend/LXStudio-TE.git
   ```

2. Open the project directory in IntelliJ IDEA

3. Configure Project Structure (Ctrl+Alt+Shift+S):
   - Under Platform Settings → SDKs:
     - Add Temurin 17 JDK if not listed
     - Or click '+' → "Add JDK"
     - Navigate to `/usr/lib/jvm/temurin-17-jdk-amd64`
   - Under Project Settings → Project:
     - Select Temurin 17 as the Project SDK

4. Select Model:
   - In the top bar dropdown (right of the hammer icon):
     - Choose "Titanic's End" for the vehicle model
     - Or "Testahedron" for the testahedron model

## Linux-Specific Setup and Troubleshooting

1. Graphics Setup:
   - The application automatically uses OpenGL instead of Vulkan on Linux systems
   - If you experience graphics issues, try these solutions in sequence:
     
     a. Set environment variables:
        ```sh
        BGFX_RUNTIME_OPENGL=1
        BGFX_RUNTIME_VULKAN=0
        JAVA_TOOL_OPTIONS=-DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0 -Dprism.order=sw
        LD_LIBRARY_PATH=$ProjectFileDir$/target/natives-linux/linux/x64/org/lwjgl
        ```
     
     b. Set VM options (if running in IntelliJ):
        ```
        -DBGFX_RUNTIME_OPENGL=1
        -DBGFX_RUNTIME_VULKAN=0
        -Djava.awt.headless=true
        -Dprism.order=sw
        -Dsun.java2d.opengl=false
        -Dorg.lwjgl.util.Debug=true
        -Dos.name=Linux
        -Dos.arch=x86_64
        -Dorg.lwjgl.librarypath=$ProjectFileDir$/target/natives-linux/linux/x64/org/lwjgl
        -Djava.library.path=$ProjectFileDir$/target/natives-linux/linux/x64/org/lwjgl
        -XX:+UseCompressedOops
        -XX:+UseG1GC
        -Djavax.accessibility.assistive_technologies=" "
        ```

2. IntelliJ Configuration:
   
   a. Plugin Registry Fix:
      - Navigate to Help → Edit Custom Properties
      - Add: `idea.is.internal=true`
      - Restart IntelliJ

   b. Cache/Index Issues:
      ```sh
      # Clear IntelliJ caches and restart
      rm -rf ~/.cache/JetBrains/IdeaIC*/
      rm -rf ~/.local/share/JetBrains/IdeaIC*/
      ```

   c. Kotlin Compiler Issues:
      - Go to File → Invalidate Caches...
      - Select all boxes
      - Click "Invalidate and Restart"
      - Let indexing complete after restart
      - If issues persist:
        ```sh
        # Clear Kotlin cache
        rm -rf ~/.cache/JetBrains/IdeaIC*/kotlin-data-container
        ```

   d. Permission Issues:
      ```sh
      # Fix IntelliJ directory permissions
      sudo chown -R $USER:$USER ~/.cache/JetBrains
      sudo chown -R $USER:$USER ~/.local/share/JetBrains
      sudo chown -R $USER:$USER ~/.config/JetBrains
      ```

## Running the Application

### Using the Script (Recommended)
The most reliable way to run the application on Linux is using the provided script:
```sh
./run_chromatik_linux.sh
```

### Manual Execution
If you need to run the application manually:

1. Build the JAR:
   ```sh
   mvn clean package
   ```

2. Run with Java:
   ```sh
   java -Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0 -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

3. Or with specific JDK path:
   ```sh
   /usr/lib/jvm/temurin-17-jdk-amd64/bin/java -Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0 -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

4. Or using Maven:
   ```sh
   MAVEN_OPTS="-Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0" mvn exec:exec@Main -Dexec.args="vehicle Vehicle.lxp"
   ```

### Troubleshooting Maven Issues

If you encounter Maven-related issues, you may need to clear the Maven cache:
```sh
rm -r ~/.m2
```

## Network Configuration

When running Chromatik on Linux, you may need to optimize your network settings for better UDP performance, which is crucial for real-time lighting control. Common symptoms of unoptimized network settings include:

- UDP blocking warnings
- Flickering or inconsistent lighting behavior
- Packet loss
- Performance degradation during high-throughput operations

We provide two resources to help you optimize your network configuration:

1. [NETWORK_LINUX.md](NETWORK_LINUX.md) - Detailed documentation about Linux network configuration, common issues, and troubleshooting.

2. [setup_network.sh](setup_network.sh) - An automated script to apply optimal network settings:
   ```sh
   # Apply optimizations
   sudo ./setup_network.sh
   
   # Revert changes if needed
   sudo ./setup_network.sh --revert
   ```

The script will:
- Increase UDP buffer sizes to ~25MB (default is 208KB)
- Optimize socket settings for real-time performance
- Make changes persistent across reboots
- Back up original settings for easy restoration

## Learning Chromatik and Developing Patterns

The tutorials in the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki) are an effective introduction.

These have been compiled by our team:

* [Operation Modes and Art Direction Standards](https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit)
* [Using Tempo and Sound](https://docs.google.com/document/d/17iICAfbhCzPL77KbmFDL4-lN0zgBb1k6wdWnoBSPDjk/edit)
* [The APC40 and LX Studio](https://docs.google.com/document/d/110qgYR_4wtE0gN8K3QZdqU75Xq0W115qe7J6mcgm1So/edit)

As you really get rolling, you'll appreciate the [API docs](https://lx.studio/api/) and public portion
of [the source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx).

## Resources

* [#lighting-software on Slack](https://titanicsend.slack.com/archives/C02L0MDQB2M)
* [Chromatik Wiki](https://github.com/heronarts/LXStudio/wiki) 
* [Chromatik API](https://chromatik.co/api/)
* [Chromatik Source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx)
* [TE Visual Map](https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=877106241)
* [TE Operation Modes and Art Direction Standards](https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit)
* [Using Tempo and Sound](https://docs.google.com/document/d/17iICAfbhCzPL77KbmFDL4-lN0zgBb1k6wdWnoBSPDjk/edit)
* [The APC40 and LX Studio](https://docs.google.com/document/d/110qgYR_4wtE0gN8K3QZdqU75Xq0W115qe7J6mcgm1So/edit)
* [Titanic's End](http://titanicsend.com)

## License Note

Please see LICENSE.md - significant parts of this repository are not open source and we support those authors' wishes. 