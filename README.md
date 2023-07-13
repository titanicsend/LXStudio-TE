Titanic's End LXStudio
==

Titanic's End is a mutant vehicle designed for Burning Man 2022. (and beyond!)

This repo covers the code for the design and instrumentation of the many LEDS wrapped around this mutant vehicle.

We use [LX Studio](https://lx.studio/) to control the light show, and this README will get you up and running with LX Studio so you, too, can help us create some great patterns.

<details>
    <summary>What if I want to know more?</summary>
    This doc sets out the project vision and has so much more information: <a href="https://docs.google.com/document/d/1YK9umrhOodwnRWGRzYOR1iOocrO6Cf9ZpHF7FWgBKKI/edit#">Lighting Design Doc</a>
</details>

## Getting started

> These are geared toward running LX Studio on a Macbook with `git` already installed. If you need help with anything, ask in the Slack #lighting-software channel!

First, you'll need an IDE (editor). IntelliJ's Community Edition is the best
free one available. You can download it here:
https://www.jetbrains.com/idea/

Steps for setup:
1. Clone the git repo you're looking at:
   ```
   git clone https://github.com/titanicsend/LXStudio-TE.git
   ```

2. Open the project director you cloned to when you're presented with "New Project" and
   "Open" options. That's the initial screen.

3. File → Project Structure (or ⌘-;)
   ![Project Structure](assets/IDE%20Setup/Project%20Structure.png)

   1. Platform Settings → SDKs
      1. Either add the installed Temurin 17 JDK
      2. Or, if that JDK is not installed, you can click the '+' and then select
         "Download JDK..."
         1. Select 17 as the Version
         2. Select "Eclipse Temarin" as the Vendor
           - Do NOT select the "aarch64" version otherwise you'll experience pain. Pick the other one
           
         ![The highlighted one](assets/IDE%20Setup/Install%20temurin17.png)

   2. Project Settings → Project
      1. Select the Temurin 17 JDK
      ![Project SDK](assets/IDE%20Setup/Select%20Project.png)

4. Select "Titanic's End" in the top bar (in the dropdown to the right of the hammer) if
   you want to use the vehicle model, or "Testahedron" if you want the testahedron model.
   ![Play button](assets/IDE%20Setup/Play%20Button.png)

5. Hit the green arrow "play" button. (If you just want to build, you can hit the hammer.)

6. Assuming things work okay, a UI for LX Studio will pop up: Great! Now, you can play with the buttons.


### Potential issues

- `Maven resources compiler: Failed to copy [...]/target/generated-test-sources/test-annotations/Icon
  ' to '[...]/target/test-classes/Icon`
  - Go to the top of your TE repo and run `find . -name Icon\? -delete`

## Digging in

So you've got the app up and running. You see some patterns in the code. How do you make sense of them?

### Coordinate System

![JSON File Types](assets/vehicle-axes-orientation.png)

To understand the point, edge, and panel naming scheme, see the [Visual Map tab](https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=877106241) of the Modules, Edges and Panels sheet.

### Learning LX and Developing Patterns

TE is using the full IDE-ready distribution instead of the P4 Processing Applet version. Don't struggle - ask questions in [#lighting-software on Slack](https://titanicsend.slack.com/archives/C02L0MDQB2M).

The tutorials in the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki) are an effective introduction.

These have been compiled by our team:
* [Operation Modes and Art Direction Standards](https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit)
* [Using Tempo and Sound](https://docs.google.com/document/d/17iICAfbhCzPL77KbmFDL4-lN0zgBb1k6wdWnoBSPDjk/edit) 
* [The APC40 and LX Studio](https://docs.google.com/document/d/110qgYR_4wtE0gN8K3QZdqU75Xq0W115qe7J6mcgm1So/edit)

As you really get rolling, you’ll appreciate the [API docs](https://lx.studio/api/) and public portion of [the source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx).

## Celebrating the installation

Once it's running, go tell Slack so we can celebrate with you and maybe give you a tour.
Or, if you prefer self-guided tours:

 * Read the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki)
 * Play with the UI until you have a modulator controlling the parameter for a pattern, and an effect applied on top.
    * See [this guide](https://github.com/tracyscott/RainbowStudio/blob/master/LXStudioUserGuide.md) from another memorable Burning Man art piece
 * Define a new fixture in the UI
 * [Optional] Save your playground as a new project with your name: `Playground <YourName>.lxp`. You can mess this project up and experiment broadly.

## Tips and tricks

Things that can help improve your experience with LX Studio.

### Recognize LX Studio JSON file extensions

It can be handy to edit LX Studio's JSON config files in the IDE. Add the .lxf
and .lxp extensions to be recognized as JSON.

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

If you just need to execute LX to run a show without editing anything, you can do that:

0. Install Temurin JDK

Either go to https://adoptium.net/installation/ or, on a Mac with Homebrew, `brew install temurin17`

(If you'll be using an IDE, it'll do this for you; skip to the Editing section.)

1. Build into a runnable JAR:
   ```shell
   mvn clean package  # Packaging creates the JAR and cleaning is optional
   ```
2. Execute the JAR (Note that the version number may be different — The version
   as of this document revision is 0.2.1-SNAPSHOT — substitute the correct
   version as necessary):
   ```shell
   java -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```
3. If the Temurin JDK isn't your default Java, then you can use the full path,
   for example:
   ```shell
   /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java -jar target/LXStudio-TE-0.2.1-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```
4. Use Maven to execute the program instead of the `java` command:
   ```shell
   mvn clean compile  # Cleaning and compiling is optional, depending on your needs
   mvn exec:java@Main -Dexec.args="vehicle Vehicle.lxp"
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

To run on machine startup (ie: press power button and LX just starts up), you'll need to do three things:

1. Add `TE.app` to your startup items
   1. System Preferences > Users & Groups
   2. Click the user to run LXStudio with
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
   2. This way, during a show, it's very easy for anyone non-technical to simply quit the program and re-run it if there is an issue

Restart your machine and you should see on startup, LXStudio automatically opens.

## Eclipse

If Eclipse is like a warm snuggie to you, we'd appreciate you adding any SDK and
environment configuration tips here.

## Connecting remote USB-MIDI devices

The car's main LX instance runs on a Mac Studio that lives on the car, connected
to the car's LAN. The UI is controlled via a remote desktop (VNC) connection over
a high-speed PTP wireless bridge. Since the VJ at this remote Front-of-House desk
will want to use MIDI surfaces and controllers to perform, we needed to come up
with a MIDI-over-WiFi solution to connect the USB MIDI devices to the box running
LX on the car. 

In 2022, we utilized OSX's arcane built-in support for RTP-MIDI. This was brittle
and fickle to maintain.

In 2023 we've changed to using a device called a BomeBox that uses a proprietary
encapsulation protocol. To make this work:

1. The Bome Network tool should be installed on the computer that runs LX. The 
    "Multiple Named Virtual interfaces" upgrade is required.
2. The remote BomeBox should be on the same subnet, with updated firmware
3. Optionally, renamed the BomeBox. We changed "BomeBox" to "FoH" for "Front of House"
4. Connect the MIDI controllers to the BomeBox USB port via a USB Hub. In the 
    Bome Network tool, enable Remote Direct Midi for those devices.
5. You can disable MIDI routes that aren't used, such as the DIN ports or
    MIDI messaging between the USB devices. This likely helps performance.
    Leave 2 routes per device: The bidirection pair LX->Device, and Device->LX. 
6. Register the correct new names in LX. The Bome Remote Direct Midi device 
    names follow a pattern of "{BomeBoxName}: {DeviceName}", like
    "FoH: APC40 mkII". For example, in your main app you may need to
    `lx.engine.midi.registerSurface(name, class)` or match the name with 
    an entry in `lx.engine.midi.inputs[].getName()`. If using more than one midi
    device of the same type BoxBox will present each device with a unique name by
    appending a number such as "FoH: Midi Fighter Twister (2)".
    `registerSurface(name, class)` needs to be called for each of these unique names.

[Here's a video](https://youtu.be/ulBLF_IR46I) illustrating our configuration.

---

## License Note

Please see LICENSE.md - significant parts of this repository are not open source and we support those authors' wishes.
