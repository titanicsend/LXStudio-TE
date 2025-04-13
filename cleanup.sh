#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "$(readlink -f "$0")" )" &> /dev/null && pwd )"


# For 'Logs' and 'Autosave' dirs, move the content to the new subfolders
if [ -d "$SCRIPT_DIR/Logs" ]; then
    echo "Checking Logs"
    if [ -z "$(ls -A "$SCRIPT_DIR/Logs")" ]; then
        echo "  Empty; clearning"
        rmdir "$SCRIPT_DIR/Logs"
    else
        echo "  Not empty; moving contents to ./te-app/Logs"
        mkdir -p $SCRIPT_DIR/te-app/Logs/
        mv $SCRIPT_DIR/Logs/* $SCRIPT_DIR/te-app/Logs/
        rmdir "$SCRIPT_DIR/Logs"
    fi
fi


if [ -d "$SCRIPT_DIR/Autosave" ]; then
    echo "Checking Autosave"
    if [ -z "$(ls -A "$SCRIPT_DIR/Autosave")" ]; then
        echo "  Empty; clearning"
        rmdir "$SCRIPT_DIR/Autosave"
    else
        echo "  Not empty; moving contents to ./te-app/Autosave"
        mkdir -p $SCRIPT_DIR/te-app/Autosave/
        mv $SCRIPT_DIR/Autosave/* $SCRIPT_DIR/te-app/Autosave/
        rmdir "$SCRIPT_DIR/Autosave"
    fi
fi


# Define the directories to check, where we don't want to auto-move the files
PHANTOM_DIRS=("Fixtures" "Projects" "Models" "Colors" "Presets")


for name in "${PHANTOM_DIRS[@]}"; do
    echo "Checking $name"
    cur_dir="$SCRIPT_DIR/$name"

    if [ -d "$cur_dir" ]; then
        # 'find -d' returns most deeply nested folders first
        for dir in $(find $cur_dir -d); do
            echo "Checking $dir"
            # Check if directory exists and is empty
            if [ -d "$dir" ]; then
                echo "  Found phantom directory: $dir"

                # Find and delete .DS_Store in all subfolders
                find $cur_dir -iname ".DS_Store" -exec rm {} \;

                # After clearing any DS_Store entries, check if empty
                if [ -z "$(ls -A "$dir")" ]; then
                    echo "  Phantom directory is empty: $dir"
                    
                    # Ask for confirmation (skip if running in non-interactive mode)
                    if [ -t 0 ]; then
                        read -p "Delete this empty directory? (y/n): " confirm
                        if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
                            rmdir "$dir"
                            echo "    Deleted: $dir"
                        else
                            echo "    Skipped: $dir"
                        fi
                    else
                        # In non-interactive mode (like when running from Maven), auto-delete
                        rmdir "$dir"
                        echo "    Auto-deleted phantom directory: $dir"
                    fi
                else
                    echo "  Phantom directory is not empty: $dir"
                    echo "  -------------"
                    ls -al $dir
                    echo "  -------------"
                    echo "  Please move the contents to their new home in './te-app/$name' and retry"
                    exit 1
                fi
            fi
        done
    fi
done
