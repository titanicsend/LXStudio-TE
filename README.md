# LXStudio-TE

## Table of Contents

- [LXStudio-TE](#lxstudio-te)
  - [Table of Contents](#table-of-contents)
  - [Titanic's End LXStudio](#titanics-end-lxstudio)
  - [JDK Installation](#jdk-installation)
    - [macOS (with Homebrew)](#macos-with-homebrew)
    - [Debian/Ubuntu Linux (experimental)](#debianubuntu-linux-experimental)
  - [Quick Start](#quick-start)
    - [Troubleshooting IntelliJ](#troubleshooting-intellij)
      - [Common Issues (All Platforms)](#common-issues-all-platforms)
      - [🍎 macOS-Specific Setup](#-macos-specific-setup)
      - [🐧 Linux-Specific Setup and Troubleshooting](#-linux-specific-setup-and-troubleshooting)
  - [Common Issues](#common-issues)
    - [Graphics Issues](#graphics-issues)
    - [Build Issues](#build-issues)
    - [IDE Issues](#ide-issues)
  - [Development](#development)
    - [Running the Application](#running-the-application)
    - [Debugging](#debugging)
  - [Contributing](#contributing)
    - [Code Style](#code-style)
    - [Pull Requests](#pull-requests)

## Titanic's End LXStudio

[Titanic's End](http://titanicsend.com) is a mutant vehicle that debuted at [Burning Man](https://burningman.org) 2022 and has since participated in EDC and Framework events.

We are the largest team developing on Chromatik in the open so other artists can benefit from our work (see the [license](LICENSE.md)). This repo contains the shader & pattern code for the 128,000 LEDS, sound reactivity, MIDI control, OSC middleware, and ArtNet bidirectional control.

We use [Chromatik](https://chromatik.co/) (formerly known as [LX Studio](https://lx.studio/)) to control the show. Although not required, we also support Chromatik financially because it's excellent. This README will get you up and running with it so you, too, can create stunning LED art. 

Our work is notable for:

* AutoVJ - an autopilot that uses Pioneer Rekordbox's phrase and beat analysis to change patterns when a human VJ would
* GLSL shader support
* Developed to team-friendly maintainability standards for long-term enjoyment of the codebase
* GigglePixel, Pixelblaze, and ArtNet integration

Want a personal intro to the project and codebase? Contact current team lead [Andrew Look](https://github.com/andrewlook) by messaging andrew-m-look (s/-/./g) electronically at the big Gm.com thing.

<details>
    <summary>What if I want to know more?</summary>
    <p>This doc sets out the project vision and has much more information: <a href="https://docs.google.com/document/d/1YK9umrhOodwnRWGRzYOR1iOocrO6Cf9ZpHF7FWgBKKI/edit#">2022 Lighting Design Doc</a></p>
    <p>Team members can reference several docs on our Notion for more background including <a href="https://www.notion.so/titanicsend/Networking-and-Front-of-House-Setup-fe5360a00b594955b735e02115548ff4">Networking and Front of House</a> and <a href="https://www.notion.so/titanicsend/2023-Lighting-Software-Integration-61c9cd5c6e884c6db66d4f843a1b8812">Software / Integration Hub</a>.</p>
</details>

## JDK Installation

Visit https://adoptium.net/installation/ or follow the instructions below for your operating system:

### macOS (with Homebrew)

```sh
brew uninstall --cask temurin # ensure you are running 17
brew install --cask temurin@17
```

### Debian/Ubuntu Linux (experimental)

```sh
# Add Adoptium repository
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# Update package list and install Temurin 17
sudo apt update
sudo apt install temurin-17-jdk
```

Verify your installation:

For macOS:
``` 
/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java --version
```

For Linux:
```
java --version
```

After installing the temurin JDK, we recommend installing and building maven, a package manager for our project:

For macOS:
```sh
brew install maven 
```

For Debian/Ubuntu Linux:
```sh
sudo apt install maven
```

Clean and install the maven dependencies (this may take a while):

```
mvn clean -U package && mvn install
```

One more thing… we have a coding style setup, as described below, so you'll also need to install [google-java-format](https://github.com/google/google-java-format):

For macOS:
```sh
brew install google-java-format  
```

For Debian/Ubuntu Linux:

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

2. Using SDKMAN! (if you have it installed, untested):
```sh
sdk install google-java-format
```

Verify the installation:
```sh
google-java-format --version
```

## Quick Start

> These are geared toward running Chromatik on macOS and Linux with `git` already installed. If you need help with anything, ask in the Slack #lighting-software channel!

First, you'll need an IDE (editor). IntelliJ's Community Edition is the best free one available. You can download it here:

https://www.jetbrains.com/idea/

For Debian/Ubuntu Linux, you have several installation options:

1. Using the JetBrains Toolbox (Recommended):
```sh
# Download JetBrains Toolbox
curl -fsSL https://raw.githubusercontent.com/nagygergo/jetbrains-toolbox-install/master/jetbrains-toolbox.sh | bash

# Launch JetBrains Toolbox and install IntelliJ IDEA Community Edition from the UI
```

2. Using the official .tar.gz (Alternative, untested):
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

### Troubleshooting IntelliJ

#### Common Issues (All Platforms)

- `Maven resources compiler: Failed to copy [...]/target/generated-test-sources/test-annotations/Icon ' to '[...]/target/test-classes/Icon`
    - Solution: Go to the top of your TE repo and run `find . -name Icon\? -delete`

#### 🍎 macOS-Specific Setup

1. Run Configuration:
   - Ensure `-XstartOnFirstThread` is present in VM options
   - This is automatically configured when using the IDE's run configuration

2. Graphics:
   - macOS users don't need to modify any graphics settings
   - The application will use the appropriate renderer automatically

#### 🐧 Linux-Specific Setup and Troubleshooting

> Note: macOS users can skip this section

1. Graphics Setup:
   - Install required graphics libraries:
     ```sh
     sudo apt install libgl1-mesa-dev mesa-common-dev libvulkan-dev
     ```
   - The application automatically uses OpenGL instead of Vulkan on Linux systems
   - If you still experience graphics issues, try these solutions in sequence:
     
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

   Note: For the most reliable experience on Linux, we recommend using the provided shell script:
   ```sh
   ./run_chromatik_linux.sh
   ```

   The IntelliJ configuration is primarily useful when you need debugging capabilities. For normal development and testing, the shell script provides a more reliable solution.

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

### Getting Started

1. Clone the Repository:
   ```sh
   git clone https://github.com/titanicsend/LXStudio-TE.git
   ```

2. Open Project:
   - Launch IntelliJ
   - Select "Open" when presented with the initial screen
   - Navigate to and select the cloned project directory

3. Configure Project Structure:
   - macOS: Press ⌘-; 
   - Linux: Press Ctrl+Alt+Shift+S
   - Or navigate to: File → Project Structure
   ![Project Structure](assets/IDE%20Setup/Project%20Structure.png)

4. Set Up SDK:
   a. Under Platform Settings → SDKs:
      - Add Temurin 17 JDK if not listed
      - Or click '+' → "Add JDK"
      - macOS: Navigate to `/Library/Java/JavaVirtualMachines/temurin-17.jdk`
      - Linux: Navigate to `/usr/lib/jvm/temurin-17-jdk-amd64`

   b. Under Project Settings → Project:
      - Select Temurin 17 as the Project SDK

5. Select Model:
   - In the top bar dropdown (right of the hammer icon):
     - Choose "Titanic's End" for the vehicle model
     - Or "Testahedron" for the testahedron model

6. Run Configuration:
   🐧 Linux users only:
   - Click "Edit Configurations..." in the run configuration dropdown
   - Remove `-XstartOnFirstThread` from VM options
   - Add `-Djava.awt.headless=true` if not present
   - Click "Apply" and "OK"

7. Launch:
   - Click the green "play" button to run
   - Or click the hammer icon to build without running

### Running Without IDE

If you prefer to run Chromatik directly without the IDE, we provide platform-specific scripts:

#### 🍎 macOS (untested):
```sh
./run_chromatik_mac.sh
```

#### 🐧 Linux:
```sh
./run_chromatik_linux.sh
```

These scripts will:
1. Build the project if needed
2. Set appropriate platform-specific options
3. Launch Chromatik with the vehicle model

You can modify the scripts to:
- Change the project file (default: `Vehicle.lxp`)
- Switch to testahedron model (change `vehicle` to `testahedron`)
- Adjust Java options or environment variables

### Coding Style

These are the steps to use [google-java-format](https://github.com/google/google-java-format) automatically
and ensure that each commit gets formatted before being submitted.

1. Setup the git pre-commit hook to run the `google-java-format` CLI tool on changed files

```sh
cp pre-commit .git/hooks/pre-commit
```

Commits may now fail if there's a style violation, since this runs `mvn spotless:check`.

You can manually apply formatting fixes using `mvn spotless:apply`.

2. (Optional) Install the IDE plugin
   for [IntelliJ](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
    1. After plugin install, go to `Settings > Tools > Actions on Save` and enable `Reformat Code`
       and `Optimize Imports`.
       
3. (Optional) Or install the IDE plugin for [Eclipse](https://github.com/google/google-java-format#eclipse).

## Digging in

So you've got the app up and running. You see some patterns in the code. How do you make sense of them?

### Coordinate System

![JSON File Types](assets/vehicle-axes-orientation.png)

To understand the point, edge, and panel naming scheme, see
the [Visual Map tab](https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=877106241)
of the Modules, Edges and Panels sheet.

### Learning Chromatik and Developing Patterns

TE is using the full IDE-ready distribution instead of the P4 Processing Applet version. Don't struggle - ask questions
in [#lighting-software on Slack](https://titanicsend.slack.com/archives/C02L0MDQB2M).

The tutorials in the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki) are an effective introduction.

These have been compiled by our team:

* [Operation Modes and Art Direction Standards](https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit)
* [Using Tempo and Sound](https://docs.google.com/document/d/17iICAfbhCzPL77KbmFDL4-lN0zgBb1k6wdWnoBSPDjk/edit)
* [The APC40 and LX Studio](https://docs.google.com/document/d/110qgYR_4wtE0gN8K3QZdqU75Xq0W115qe7J6mcgm1So/edit)

As you really get rolling, you'll appreciate the [API docs](https://lx.studio/api/) and public portion
of [the source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx).

## Celebrating the installation

Once it's running, go tell Slack so we can celebrate with you and maybe give you a tour.
Or, if you prefer self-guided tours:

* Read the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki)
* Play with the UI until you have a modulator controlling the parameter for a pattern, and an effect applied on top.
    * See [this guide](https://github.com/tracyscott/RainbowStudio/blob/master/LXStudioUserGuide.md) from another
      memorable Burning Man art piece
* Define a new fixture in the UI
* [Optional] Save your playground as a new project with your name: `Playground <YourName>.lxp`. You can mess this
  project up and experiment broadly.

## Tips and tricks

Things that can help improve your experience with LX Studio.

### Recognize Chromatik JSON file extensions

It can be handy to edit Chromatik's JSON config files in the IDE. Add the .lxf and .lxp extensions to be recognized as JSON.

1. Open IntelliJ preferences (⌘-, on Mac) and go to Editor → File Types → JSON.
2. Next, add "*.lxp" to the list, and so on.

![JSON File Types](assets/IDE%20Setup/JSON%20File%20Types.png)

### Optional Plugins

Jeff's enjoying the following (he comes from Sublime and vim):

* CodeGlance
* Rainbow Brackets
* IdeaVim
* CSV
* KeyPromoter X
* Python Community Edition

### Coming from VS Code?

Many of you may use VS Code in your day-to-day life. If you do, and you'd like
IntelliJ to behave more like VS Code, I'd recommend:

1. In IntelliJ, open the "IntelliJ IDEA" menu and select "Preferences"
2. Click "Plugins"
3. Search for "VSCode Keymap"; install
4. Go back to "Preferences"
5. Go to "Keymap", select one of the VS Code keymap options, (either macOS or
   not) hit apply, and enjoy increased happiness in your IDE

### Running TE without the IDE

If you just need to execute Chromatik to run a show without editing anything, you can do that:

0. Install Temurin JDK (see JDK installation above).

1. Build into a runnable JAR:
   ```shell
   mvn clean package  # Packaging creates the JAR and cleaning is optional
   ```
2. Execute the JAR (Note that the version number may be different — The version
   as of this document revision is 0.2.1-SNAPSHOT — substitute the correct
   version as necessary):

   For macOS:
   ```shell
   java -XstartOnFirstThread -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

   For Linux:
   ```shell
   java -Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0 -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

3. If the Temurin JDK isn't your default Java:

   For macOS:
   ```shell
   /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java -XstartOnFirstThread -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

   For Linux:
   ```shell
   /usr/lib/jvm/temurin-17-jdk-amd64/bin/java -Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0 -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```

4. Use Maven to execute the program instead of the `java` command:

   For macOS:
   ```shell
   mvn clean compile  # Cleaning and compiling is optional, depending on your needs
   MAVEN_OPTS="-XstartOnFirstThread" mvn exec:exec@Main -Dexec.args="vehicle Vehicle.lxp"
   ```

   For Linux:
   ```shell
   mvn clean compile  # Cleaning and compiling is optional, depending on your needs
   MAVEN_OPTS="-Djava.awt.headless=true -DBGFX_RUNTIME_OPENGL=1 -DBGFX_RUNTIME_VULKAN=0" mvn exec:exec@Main -Dexec.args="vehicle Vehicle.lxp"
   ```

   Fun fact: The "Main" target isn't defined in the POM to have arguments, but
   it could, in which case you wouldn't need the `vehicle Vehicle.lxp` args.

### Potential issues

If your `~/.m2` Maven cache has any conflicting things, you may need to delete
the conflicts, otherwise the execution may complain about things like missing
libraries or invalid versions, and the like. Finding the conflicts is a more
advanced approach. A simple one is to just delete that whole directory:

```shell
rm -r ~/.m2
```

## Running LXStudio on startup

To run on machine startup (ie: press power button and Chromatik just starts up), you'll need to do three things:

1. Add `TE.app` to your startup items
   1. System Preferences > Users & Groups
   2. Click the user to run Chromatik with
   3. Login Items > "+" button > add TE.app 
2. Change to automatic login
    1. System Preferences > Users & Groups
    2. Click "Login Options" underneath list of accounts (may need to enter password)
    3. Using the combo box, select desired user, ie "te" or whatever
    4. Uncheck all the boxes underneath
3. Remove the password from your user account
    1. System Preferences > Users & Groups
    2. Click the user > "Change Password"
    3. Leave new password blank
4. Keep in Dock
    1. When TE.app is running, right click on it, and say "Keep in Dock"
    2. This way, during a show, it's very easy for anyone non-technical to simply quit the program and re-run it if
       there is an issue

Restart your machine and you should see Chromatik open automatically on startup.

## Eclipse

If Eclipse is like a warm snuggie to you, we'd appreciate you adding any SDK and environment configuration tips here.

## Connecting remote USB-MIDI devices

The car's main Chromatik instance runs on a Mac Studio that lives on the car, connected
to the car's LAN. The UI is controlled via a remote desktop (VNC) connection over
a high-speed PTP wireless bridge. Since the VJ at this remote Front-of-House desk
will want to use MIDI surfaces and controllers to perform, we needed to come up
with a MIDI-over-WiFi solution to connect the USB MIDI devices to the box running
Chromatik on the car. 

In 2022, we utilized OSX's arcane built-in support for RTP-MIDI. This was brittle
and fickle to maintain.

In 2023 we've changed to using a device called a BomeBox that uses a proprietary
encapsulation protocol. To make this work:

1. The Bome Network tool should be installed on the computer that runs Chromatik. The 
    "Multiple Named Virtual interfaces" upgrade is required.
2. The remote BomeBox should be on the same subnet, with updated firmware
3. Optionally, renamed the BomeBox. We changed "BomeBox" to "FoH" for "Front of House"
4. Connect the MIDI controllers to the BomeBox USB port via a USB Hub. In the
   Bome Network tool, enable Remote Direct Midi for those devices.
5. You can disable MIDI routes that aren't used, such as the DIN ports or
    MIDI messaging between the USB devices. This likely helps performance.
    Leave 2 routes per device: The bidirection pair Chromatik->Device, and Device->Chromatik. 
6. Register the correct new names in Chromatik. The Bome Remote Direct Midi device 
    names follow a pattern of "{BomeBoxName}: {DeviceName}", like
    "FoH: APC40 mkII". For example, in your main app you may need to
    `lx.engine.midi.registerSurface(name, class)` or match the name with 
    an entry in `lx.engine.midi.inputs[].getName()`. If using more than one midi
    device of the same type BoxBox will present each device with a unique name by
    appending a number such as "FoH: Midi Fighter Twister (2)".
    `registerSurface(name, class)` needs to be called for each of these unique names.

[Here's a video](https://youtu.be/ulBLF_IR46I) illustrating our configuration.

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
