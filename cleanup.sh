#!/usr/bin/env bash

set -euo pipefail

# Define the directories to check
PHANTOM_DIRS=("Fixtures" "Projects" "Models" "Colors" "Presets" "Logs" "Autosave")

SCRIPT_DIR="$( cd "$( dirname "$(readlink -f "$0")" )" &> /dev/null && pwd )"

for name in "${PHANTOM_DIRS[@]}"; do
    echo "Checking $name"
    dir="$SCRIPT_DIR/$name"
    # Check if directory exists and is empty
    if [ -e "$dir/.DS_Store" ]; then
        echo "Found .DS_Store; removing"
        rm -f "$dir/.DS_Store"
    fi
    if [ -d "$dir" ]; then
        echo "Found phantom directory: $dir"
        if [ -z "$(ls -A "$dir")" ]; then
            echo "Phantom directory is empty: $dir"
            
            # Ask for confirmation (skip if running in non-interactive mode)
            if [ -t 0 ]; then
                read -p "Delete this empty directory? (y/n): " confirm
                if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
                    rmdir "$dir"
                    echo "Deleted: $dir"
                else
                    echo "Skipped: $dir"
                fi
            else
                # In non-interactive mode (like when running from Maven), auto-delete
                rmdir "$dir"
                    echo "Auto-deleted phantom directory: $dir"
            fi
        else
            echo "Phantom directory is not empty: $dir"
            echo "-------------"
            ls -al $dir
            echo "-------------"
            echo "Please move the contents to their new home in './te-app/$name' and retry"
            exit 1
        fi
    fi
done

# Base directory is the parent project directory
BASE_DIR="$(pwd)"

# Define the directories to check - with relative paths to project modules if needed
PHANTOM_DIRS=("FolderA" "FolderB" "module1/FolderC")

for dir in "${PHANTOM_DIRS[@]}"; do
  # Full path to the directory
  FULL_PATH="$BASE_DIR/$dir"
  
  # Check if directory exists and is empty
  if [ -d "$FULL_PATH" ] && [ -z "$(ls -A "$FULL_PATH")" ]; then
    echo "Found empty phantom directory: $dir"
    
    # In Maven context, auto-delete without prompting
    rmdir "$FULL_PATH"
    echo "Auto-deleted phantom directory: $dir"
  fi
done
