#!/usr/bin/env bash

# halt the script if any commands fail, or if any shell variables are unbound.
set -euo pipefail

# safely establish the directory containing this script, in case it's invoked from
# a different directory.
SCRIPT_DIR="$( cd "$( dirname "$(readlink -f "$0")" )" &> /dev/null && pwd )"

MODULE_PATH="te-app"

# For dirs with auto-generated contents, move the content to the new subfolders
MOVE_DIRS=("Logs" "Autosave" "Deleted" "Scripts" "Views" "MIDI Mappings")

for name in "${MOVE_DIRS[@]}"; do
    echo "Checking $name"
    cur_dir="$SCRIPT_DIR/$name"

    if [ -d "$cur_dir" ]; then
        if [ -z "$(ls -A "$cur_dir")" ]; then
            echo "  Empty; clearing"
            rmdir "$cur_dir"
        else
            echo "  Not empty; moving contents to ./$MODULE_PATH/$name"
            mkdir -p "$SCRIPT_DIR/$MODULE_PATH/$name/"
            mv "$cur_dir"/* "$SCRIPT_DIR/$MODULE_PATH/$name/"
            rmdir "$cur_dir"
        fi
    fi
done


MOVE_FILES=(".lxpreferences" ".devSwitch")

for fname in "${MOVE_FILES[@]}"; do
    echo "Checking $fname"
    if [ -f "$fname" ]; then
        echo "  Found $fname; moving"
        mv "$fname" "$SCRIPT_DIR/$MODULE_PATH/"
    fi
done

# Define the directories to check, where we don't want to auto-move the files
PHANTOM_DIRS=("Fixtures" "Projects" "Models" "Colors" "Presets" "script" "resources" "src" "TD" "Packages")


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
                find "$cur_dir" -iname ".DS_Store" -exec rm {} \;

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
                    ls -al "$dir"
                    echo "  -------------"
                    echo "  Please move the contents to their new home in './$MODULE_PATH/$name' and retry"
                    exit 1
                fi
            fi
        done
    fi
done

echo "Checking target"
if [ -d "$SCRIPT_DIR/target" ]; then
    echo "  Found target; clearing"
    rm -rf "$SCRIPT_DIR/target"
fi
