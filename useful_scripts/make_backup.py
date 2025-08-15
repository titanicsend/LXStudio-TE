#!/usr/bin/env python3
"""
Make Backup Script

Creates timestamped backup copies of files to prevent data loss during modifications.

Usage:
    python3 make_backup.py <file_path> [--suffix SUFFIX]

Examples:
    # Basic backup with timestamp
    python3 make_backup.py te-app/Projects/BM2024_Pacman.lxp
    
    # Backup with custom suffix
    python3 make_backup.py config.json --suffix "before_changes"
    
    # Backup before running a risky script
    python3 make_backup.py important_file.txt --suffix "pre_script"
"""

import sys
import shutil
import argparse
from datetime import datetime
from pathlib import Path

def make_backup(file_path, suffix=None):
    """
    Create a timestamped backup of the specified file.
    
    Args:
        file_path: Path to the file to backup
        suffix: Optional custom suffix (defaults to timestamp)
    
    Returns:
        str: Path to the created backup file
    """
    
    source_path = Path(file_path)
    
    # Check if source file exists
    if not source_path.exists():
        raise FileNotFoundError(f"Source file '{file_path}' not found")
    
    if not source_path.is_file():
        raise ValueError(f"'{file_path}' is not a file")
    
    # Generate backup filename
    if suffix:
        backup_name = f"{source_path.name}.backup_{suffix}"
    else:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_name = f"{source_path.name}.backup_{timestamp}"
    
    backup_path = source_path.parent / backup_name
    
    # Handle case where backup already exists
    counter = 1
    original_backup_path = backup_path
    while backup_path.exists():
        if suffix:
            backup_name = f"{source_path.name}.backup_{suffix}_{counter}"
        else:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_name = f"{source_path.name}.backup_{timestamp}_{counter}"
        backup_path = source_path.parent / backup_name
        counter += 1
    
    try:
        # Create the backup
        shutil.copy2(source_path, backup_path)
        print(f"✅ Backup created: {backup_path}")
        return str(backup_path)
        
    except Exception as e:
        raise RuntimeError(f"Failed to create backup: {e}")

def main():
    parser = argparse.ArgumentParser(
        description='Create timestamped backups of files',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s project.lxp
  %(prog)s config.json --suffix "before_update"
  %(prog)s data.txt --suffix "working_version"
        """
    )
    
    parser.add_argument('file_path', 
                        help='Path to the file to backup')
    parser.add_argument('--suffix', '-s',
                        help='Custom suffix for backup filename (instead of timestamp)')
    
    args = parser.parse_args()
    
    try:
        backup_path = make_backup(args.file_path, args.suffix)
        
        # Show file sizes for verification
        source_size = Path(args.file_path).stat().st_size
        backup_size = Path(backup_path).stat().st_size
        
        print(f"📁 Source: {args.file_path} ({source_size:,} bytes)")
        print(f"📁 Backup: {backup_path} ({backup_size:,} bytes)")
        
        if source_size == backup_size:
            print("✅ Backup verified - sizes match")
        else:
            print("⚠️  Warning: Backup size doesn't match source")
            
    except FileNotFoundError as e:
        print(f"❌ Error: {e}")
        sys.exit(1)
    except ValueError as e:
        print(f"❌ Error: {e}")
        sys.exit(1)
    except RuntimeError as e:
        print(f"❌ Error: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n❌ Backup cancelled by user")
        sys.exit(1)

if __name__ == "__main__":
    main()
