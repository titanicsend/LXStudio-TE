# How to Run TE App and Configure for Your Project

This guide explains how to run the Titanic's End (TE) lighting control application and configure it to load your specific project by default.

## Quick Start

### Option 1: Use the Existing App (Recommended)
1. **Double-click** `utils/RunTEApplication.app`
2. The app will automatically load your configured project

### Option 2: Use IntelliJ
1. Open the project in IntelliJ
2. Run the `TEApp` main class
3. Load your project through the UI

## How to Configure Which Project Loads by Default

The app loads projects based on the configuration in `te-app/script/start-lx.sh`.

### Current Configuration
- **Line 18** in `te-app/script/start-lx.sh` sets the default project:
  ```bash
  LXP="${APP_MODULE_DIR}/Projects/BM2024_Pacman.lxp"
  ```

### To Change the Default Project
1. Open `te-app/script/start-lx.sh`
2. Find line 18: `LXP="${APP_MODULE_DIR}/Projects/BM2024_Pacman.lxp"`
3. Change `BM2024_Pacman.lxp` to your desired project file
4. Save the file

### Available Projects
You can find all available projects in `te-app/Projects/`:
- `BM2024_Pacman.lxp` - Pacman art car project
- `BM2024_TE.lxp` - Titanic's End project
- `Vehicle.lxp` - Basic vehicle project
- `driving.lxp` - Driving mode project
- And many others...

## How the App Works

### The App Structure
- `utils/RunTEApplication.app` - macOS Automator app that launches TE
- `te-app/script/start-lx.sh` - Script that actually runs the Java application
- `te-app/target/te-app-*.jar` - The compiled Java application

### What Happens When You Launch
1. `RunTEApplication.app` runs the shell script
2. Script changes to `te-app/` directory (where all resources are located)
3. Script checks if JAR file exists, builds if needed
4. Script launches Java with the configured project file
5. Application loads with your project

## Troubleshooting

### App Won't Start
- Make sure Java 21 (Temurin) is installed
- Check that the project file exists in `te-app/Projects/`
- Kill any existing Java processes: `pkill -f "java.*te-app"`

### Maven Issues (Fixed!)
- ✅ **"mvn: command not found"** - The script now automatically detects Maven in common locations
- ✅ **JAR file path issues** - Fixed version detection and JAR file naming

### Wrong Project Loads
- Check line 18 in `te-app/script/start-lx.sh`
- Make sure the project file name is correct
- Restart the app after making changes

### Missing Resources
- The app must run from the `te-app/` directory
- All resources (shaders, fonts, etc.) are relative to this directory
- Don't move or rename the `te-app/` folder

## For Startup (Auto-Launch on Boot)

To make the app start automatically when you turn on your computer:

1. **System Preferences** > **Users & Groups**
2. Click your user account
3. **Login Items** tab
4. Click **"+"** button
5. Navigate to your project folder
6. Select `utils/RunTEApplication.app`
7. Check the box to enable it

Now the app will automatically launch your configured project when you start your computer.

## Building the Project

If you need to rebuild the application:

```bash
cd te-app
mvn clean package
```

This creates the JAR file that the app uses.

## Summary

- **To run**: Double-click `utils/RunTEApplication.app`
- **To change default project**: Edit line 18 in `te-app/script/start-lx.sh`
- **For startup**: Add `utils/RunTEApplication.app` to Login Items
- **Current default**: `BM2024_Pacman.lxp` (your Pacman project)

The app is now configured to load your Pacman project by default! 🎮
