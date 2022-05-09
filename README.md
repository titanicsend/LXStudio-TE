Titanic's End LXStudio
==

You may wish to get context first from the [Lighting Design Doc](https://docs.google.com/document/d/1YK9umrhOodwnRWGRzYOR1iOocrO6Cf9ZpHF7FWgBKKI/edit#).

## Coordinate System

![JSON File Types](assets/vehicle-axes-orientation.png)

To understand the point, edge, and panel naming scheme, see the [Visual Map tab](https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=877106241) of the Modules, Edges and Panels sheet.

## Learning LX and Developing Patterns

TE is using the full IDE-ready distribution instead of the P4 Processing Applet version. Don't struggle - ask questions in [#Lighting on Slack](https://titanicsend.slack.com/archives/C02L0MDQB2M).

The tutorials in the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki) are an effective introduction.

These have been compiled by our team:
* [Operation Modes and Art Direction Standards](https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit)
* [Using Tempo and Sound](https://docs.google.com/document/d/17iICAfbhCzPL77KbmFDL4-lN0zgBb1k6wdWnoBSPDjk/edit) 
* [The APC40 and LX Studio](https://docs.google.com/document/d/110qgYR_4wtE0gN8K3QZdqU75Xq0W115qe7J6mcgm1So/edit)

As you really get rolling, you’ll appreciate the [API docs](https://lx.studio/api/) and public portion of [the source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx).

## Getting the code and running it

Clone the Git repository here:
```shell
git clone https://github.com/titanicsend/LXStudio-TE.git
cd LXStudio-TE
```

You'll also need to download and install [Processing 4](https://processing.org/download). Just drag the uncompressed app to Applications. If you already had a Java IDE running, you should now restart it to resolve dependencies on GlueGen, JOGL, and Processing Core.

## Running TE directly from the command line

If you're going to be editing the code, you can just skip straight to the Editing section below. But
if you just want to run it, here are some quick command line instructions (without full explanations)
for how you can do that without having to install the whole IDE:

0. Install Temurin JDK

Either go to https://adoptium.net/installation/ or, on a Mac with Homebrew, `brew install temurin17`

(If you'll be using an IDE, it'll do this for you; skip to the Editing section.)

1. Build into a runnable JAR:
   ```shell
   mvn clean package  # Packaging creates the JAR and cleaning is optional
   ```
2. Execute the JAR (Note that the version number may be different — The version
   as of this document revision is 0.2.0-SNAPSHOT — substitute the correct
   version as necessary):
   ```shell
   java -jar target/LXStudio-TE-0.2.0-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```
3. If the Temurin JDK isn't your default Java, then you can use the full path,
   for example:
   ```shell
   /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java -jar target/LXStudio-TE-0.2.0-SNAPSHOT-jar-with-dependencies.jar vehicle Vehicle.lxp
   ```
4. Use Maven to execute the program instead of the `java` command:
   ```shell
   mvn clean compile  # Cleaning and compiling is optional, depending on your needs
   mvn exec:java@Main -Dexec.args="vehicle Vehicle.lxp"
   ```

   Fun fact: The "Main" target isn't defined in the POM to have arguments, but
   it could, in which case you wouldn't need the `vehicle Vehicle.lxp` args.

## Potential issues

If your `~/.m2` Maven cache has any conflicting things, you may need to delete
the conflicts, otherwise the execution may complain about things like missing
libraries or invalid versions, and the like. Finding the conflicts is a more
advanced approach. A simple one is to just delete that whole directory:

```shell
rm -r ~/.m2
```

## Celebrating the installation

Once it's running, go tell Slack so we can celebrate with you and maybe give you a tour.
Or, if you prefer self-guided tours:

 * Read the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki)
 * Play with the UI until you have a modulator controlling the parameter for a pattern, and an effect applied on top.
    * See [this guide](https://github.com/tracyscott/RainbowStudio/blob/master/LXStudioUserGuide.md) from another memorable Burning Man art piece
 * Define a new fixture in the UI
 * [Optional] Save your playgorund as a new project with your name: `Playground <YourName>.lxp`. You can mess this project up and experiment broadly.

## Editing and improving the code, writing patterns

If you'd like to change the TE code, you'll need write access to the repo. Ask
for that on Slack before you do anything else, just in case it takes a while.

The other thing you'll need is an IDE (editor). IntelliJ's Community Edition is the best
free one available. You can download it here:
https://www.jetbrains.com/idea/

Steps for setup:
1. Open the project directory when you're presented with "New Project" and
   "Open" options. That's the initial screen.
2. File → Project Structure (or ⌘-;)
   1. Platform Settings → SDKs
      1. Either add the installed Temurin 17 JDK
      2. Or, if that JDK is not installed, you can click the '+' and then select
         "Download JDK..."
         1. Select 17 as the Version
         2. Select "Eclipse Temarin" as the Vendor
   2. Project Settings → Project
      1. Select the Temurin 17 JDK
3. Select "Titanic's End" in the top bar (in the dropdown to the right of the hammer) if
   you want to use the vehicle model, or "Testahedron" if you want the testahedron model.
4. Hit the green arrow "play" button. (If you just want to build, you can hit the hammer.)

### Potential issues

`Maven resources compiler: Failed to copy [...]/target/generated-test-sources/test-annotations/Icon
' to '[...]/target/test-classes/Icon`

Go to the top of your TE repo and run `find . -name Icon\? -delete`

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

## Eclipse

If Eclipse is like a warm snuggie to you, we'd appreciate you adding any SDK and
environment configuration tips here.

