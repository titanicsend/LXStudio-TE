#!/bin/bash

# Simple TE Startup Script
# This script starts the TE application and ensures it comes to the foreground

LOG_FILE="$HOME/te-startup.log"

echo "$(date): Starting TE startup script" >> "$LOG_FILE"

# Change to the project directory
cd /Users/artarazavi/LXStudio-Pacman
echo "$(date): Changed to directory: $(pwd)" >> "$LOG_FILE"

# Wait a bit for BomeBox to connect (simple delay)
echo "$(date): Waiting 30 seconds for BomeBox to connect..." >> "$LOG_FILE"
sleep 30
echo "$(date): Starting TE..." >> "$LOG_FILE"

# Start TE using the exact command from README
echo "$(date): Launching TE..." >> "$LOG_FILE"
cd te-app && java -XstartOnFirstThread -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar Projects/BM2024_Pacman.lxp &

# Wait for it to start
sleep 5

# Bring to foreground
echo "$(date): Bringing TE to foreground..." >> "$LOG_FILE"
osascript <<EOF
tell application "System Events"
    tell process "java"
        set frontmost to true
    end tell
end tell
EOF

echo "$(date): TE startup complete" >> "$LOG_FILE"
