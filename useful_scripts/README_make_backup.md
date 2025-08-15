# Make Backup Script

A utility script for creating timestamped backup copies of files to prevent data loss during modifications.

## Overview

This script creates backup copies of files with automatic timestamping or custom suffixes. It's essential for safely modifying important files like project configurations, ensuring you can always revert changes if something goes wrong.

## Usage

```bash
python3 make_backup.py <file_path> [--suffix SUFFIX]
```

### Parameters

- `file_path`: Path to the file you want to backup (required)
- `--suffix` / `-s`: Optional custom suffix instead of timestamp

## Examples

### Basic timestamped backup
```bash
python3 make_backup.py te-app/Projects/BM2024_Pacman.lxp
# Creates: BM2024_Pacman.lxp.backup_20250815_143022
```

### Backup with custom suffix
```bash
python3 make_backup.py config.json --suffix "before_changes"
# Creates: config.json.backup_before_changes
```

### Before running risky operations
```bash
python3 make_backup.py important_data.txt --suffix "pre_script"
# Creates: important_data.txt.backup_pre_script
```

### Backup project files before modifications
```bash
python3 make_backup.py te-app/Projects/BM2024_Pacman.lxp --suffix "working_version"
# Creates: BM2024_Pacman.lxp.backup_working_version
```

## Features

- **Automatic timestamping**: Uses `YYYYMMDD_HHMMSS` format by default
- **Custom suffixes**: Override timestamp with meaningful names
- **Collision handling**: Automatically adds numbers if backup already exists
- **Size verification**: Confirms backup matches original file size
- **Error handling**: Clear error messages for common issues
- **Metadata preservation**: Preserves file timestamps and permissions

## Output

The script provides:
- ✅ Confirmation of backup creation
- 📁 Source and backup file paths with sizes
- ✅ Size verification (ensures complete copy)
- ⚠️ Warnings if sizes don't match

## Example Output

```
$ python3 make_backup.py te-app/Projects/BM2024_Pacman.lxp --suffix "before_fixes"

✅ Backup created: te-app/Projects/BM2024_Pacman.lxp.backup_before_fixes
📁 Source: te-app/Projects/BM2024_Pacman.lxp (1,234,567 bytes)
📁 Backup: te-app/Projects/BM2024_Pacman.lxp.backup_before_fixes (1,234,567 bytes)
✅ Backup verified - sizes match
```

## Common Use Cases

### Before script execution
```bash
# Always backup before running modification scripts
python3 make_backup.py project.lxp --suffix "pre_move_strips"
python3 move_strip_side.py project.lxp B 0 0 -10
```

### Version checkpoints
```bash
# Create named checkpoints during development
python3 make_backup.py config.json --suffix "v1_working"
python3 make_backup.py config.json --suffix "v2_with_new_features"
```

### Before manual edits
```bash
# Backup before hand-editing important files
python3 make_backup.py te-app/Projects/BM2024_Pacman.lxp --suffix "before_manual_edit"
```

## Error Handling

- **File not found**: Clear error if source file doesn't exist
- **Not a file**: Error if path points to directory
- **Permission errors**: Reports if backup can't be created
- **Disk space**: Will fail gracefully if insufficient space

## File Naming Convention

### With timestamp (default):
- Format: `{original_name}.backup_{YYYYMMDD_HHMMSS}`
- Example: `project.lxp.backup_20250815_143022`

### With custom suffix:
- Format: `{original_name}.backup_{suffix}`
- Example: `project.lxp.backup_working_version`

### Collision handling:
- If backup exists, adds counter: `project.lxp.backup_working_version_1`

## Integration with Other Scripts

This backup script pairs well with other utilities:

```bash
# Safe workflow pattern
python3 make_backup.py project.lxp --suffix "before_analysis"
python3 comprehensive_fixes_logger.py project.lxp A

python3 make_backup.py project.lxp --suffix "before_move"
python3 move_strip_side.py project.lxp B 0 0 -10
```

## Dependencies

- Python 3
- Standard libraries: `sys`, `shutil`, `argparse`, `datetime`, `pathlib`

This script is essential for safe file modification workflows and preventing accidental data loss during development and testing.
