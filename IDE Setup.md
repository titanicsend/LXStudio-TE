# IDE Setup for the Project

It is suggested to use the Temurin JDK (v17).
You can either:
1. Install yourself; either:
   1. https://adoptium.net/installation/
   2. Or, on a Mac, you can easily install using Homebrew:
      `brew install temurin17`
2. Or, you can have your IDE do it for you. IntelliJ has such an option.

## Getting the project

Clone the Git repository here:
```shell
git clone https://github.com/titanicsend/LXStudio-TE.git
```

You will now see a `LXStudio-TE/` directory from wherever you cloned
the repository.

## Structure

The project uses Maven. This means that many IDEs will correctly recognize how
to use it. You can also operate the project from the command line using the
`mvn` tool.

## Command line operation

First, make sure to `cd` into the `LXStudio-TE` directory that was cloned from
the repository.

Some quick command line instructions (without full explanations):
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
   mvn exec:java@Main vehicle Vehicle.lxp
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

## Using IntelliJ

IntelliJ is one of the best IDEs out there.

Download link is available here: https://www.jetbrains.com/idea/
The Community edition is the free one.

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
3. Top window button bar → Add Configuration... (to the right of the hammer)
   1. Add new Application configuration
   2. Leave the "java 17 SDK of 'LXStudio-TE' module" as it is
   3. Name: TEApp
   4. Main class: titanicsend.app.TEApp
   5. Program arguments: vehicle Vehicle.lxp
   6. Leave the "Working directory" as-is
   7. Hit the "OK" button
4. Hit the green arrow "play" button. If you just want to build, hit the hammer
   button just to the left of the configurations drop-down that used to say
   "Add Configuration..." and now says "TEApp".

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
