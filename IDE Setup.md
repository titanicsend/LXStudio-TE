IDE Setup for LX Studio IDE
==

LX Studio IDE currently uses the temurin-11 SDK (Java 11).


## IntelliJ 
If you'd like to try IntelliJ, downloa IntelliJ IDEA **Community Edition** [here](https://www.jetbrains.com/idea/download/).

### Java SDK

Open the project in IntelliJ using `LXStudio-IDE/` as the root.

![Open Project](assets/IDE%20Setup/Open%20Project.png)

You will need to download the right Java SDK. Most people should see IntelliJ automatically prompt them to do it for them - accept these prompts.

![Auto install Java SDK](assets/IDE%20Setup/Auto%20install%20Java%20SDK.png)


### Manual config of Java SDK (shouldn't be needed)

You may need to select the Java SDK in the Project Structure:

![Project Structure](assets/IDE%20Setup/SDK%20in%20Project%20Structure.png)

As well as in the App Run Config:

![App config menu](assets/IDE%20Setup/App%20configuration menu.png)

![Run Configurations](assets/IDE%20Setup/Run%20Configurations.png)

### Recognize LX Studio JSON file extensions

If can be handy to edit LX Studio's JSON config files in the IDE. Add the .lxf and .lxp extension to be recognized as JSON.

Open IntelliJ preferences (⌘-, on Mac) and go to Editor → File Types → JSON.

![JSON File Types](assets/IDE%20Setup/JSON%20File%20Types.png)

### Optional Plugins

Jeff's enjoying the folllowing (he comes from Sublime and vim)

* CodeGlance
* Rainbow Brackets
* IdeaVim
* CSV
* KeyPromoter X
* Python Community Edition

### Coming from VS Code?

Many of you may use VS Code in your day to day life. If you do, and you'd like IntelliJ to behave more like VS Code, I'd recommend:

1. In IntelliJ, open "IntelliJ IDEA" menu and select "Preferences"
2. Click "Plugins"
3. Search for "VSCode Keymap"; install
4. Go back to "Preferences"
5. Go to "Keymap", select one of the VS Code keymap options, (either macOS or not) hit apply, and enjoy increased happiness in your IDE

## Eclipse

If Eclipse is like a warm snuggie to you, we'd appreciate you adding any SDK and evirnoment config tips here.
