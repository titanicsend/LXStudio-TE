#!/bin/bash

# VJLab Startup Script
# This script starts VJLab and ensures it comes to the foreground

LOG_FILE="$HOME/vjlab-startup.log"

echo "$(date): Starting VJLab startup script" >> "$LOG_FILE"

# Start VJLab
echo "$(date): Launching VJLab..." >> "$LOG_FILE"
open -a "VJLab" &

# Wait for it to start
sleep 5

# Bring to foreground
echo "$(date): Bringing VJLab to foreground..." >> "$LOG_FILE"
osascript <<EOF
tell application "VJLab"
    activate
end tell
EOF

# Alternative method if the above doesn't work
osascript <<EOF
tell application "System Events"
    tell process "VJLab"
        set frontmost to true
    end tell
end tell
EOF

echo "$(date): VJLab startup complete" >> "$LOG_FILE"