# How to Create a PacMan Dock App

## Overview
This guide shows you how to create a clickable dock application that launches the TE/PacMan lighting system.

## Step 1: Create the Automator App

1. **Open Automator** (Applications → Utilities → Automator)
2. Choose **Application** when prompted
3. In the Actions library, search for "Run Shell Script"
4. Drag the **Run Shell Script** action to the workflow area
5. Set the script content to:
   ```bash
   cd /Users/artarazavi/LXStudio-Pacman
   ./start-te.sh
   ```
6. Make sure "Shell" is set to `/bin/bash`

## Step 2: Save the App

1. Go to **File → Save** (or Cmd+S)
2. Name it `PacMan.app`
3. Save it in the `/utils` folder alongside the other apps
4. Click **Save**

## Step 3: Add to Dock

1. Navigate to the `/utils` folder
2. Drag `PacMan.app` to your dock
3. The app will now appear in your dock for easy access

## Step 4: Set Up Auto-Startup (Optional)

If you want PacMan to start automatically when you log in:

1. Open **System Preferences** → **Users & Groups**
2. Click your user account
3. Click the **Login Items** tab
4. Click the **+** button
5. Navigate to your project folder and select `start-te.sh`
6. Make sure it's checked in the list

## Usage

- **Manual Launch**: Double-click the PacMan app in your dock
- **Auto Launch**: The app will start automatically on login if you set up the startup script
- **Logs**: Check `~/te-startup.log` for startup information

## Troubleshooting

- If the app doesn't start, check the log file: `cat ~/te-startup.log`
- Make sure `start-te.sh` is executable: `chmod +x start-te.sh`
- Verify the project path is correct in the script

## Notes

- The app will launch TE/Chromatik and bring it to the foreground
- The startup script includes logging for debugging
- You can have both auto-startup and manual dock access