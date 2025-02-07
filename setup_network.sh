#!/bin/bash

# Network Optimization Script for LXStudio/Chromatik
# ------------------------------------------------
#
# This script optimizes Linux network settings for better UDP performance,
# which is crucial for real-time lighting control and visualization.
#
# The optimizations include:
# 1. UDP Buffer Sizes (rmem/wmem):
#    - Increases both send and receive buffer sizes to ~25MB
#    - Helps prevent packet loss during high-throughput operations
#    - Default is usually 208KB, which can be too small for real-time data
#
# 2. Socket Settings:
#    - tcp_max_tw_buckets: Controls number of TIME_WAIT sockets
#    - netdev_max_backlog: Size of packet processing queue
#    - somaxconn: Maximum length of pending connections queue
#
# These changes are particularly important for:
#   - Preventing UDP packet drops
#   - Reducing network latency
#   - Improving real-time performance
#   - Handling multiple simultaneous connections

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root (use sudo)"
    exit 1
fi

# File to store original network settings
DEFAULTS_FILE="/tmp/network_defaults_chromatik.conf"

# Function to read current system values
read_system_defaults() {
    echo "Reading current system network settings..."
    cat > "$DEFAULTS_FILE" << EOF
# Original network settings saved on $(date)
rmem_max=$(sysctl -n net.core.rmem_max)
rmem_default=$(sysctl -n net.core.rmem_default)
wmem_max=$(sysctl -n net.core.wmem_max)
wmem_default=$(sysctl -n net.core.wmem_default)
tcp_max_tw_buckets=$(sysctl -n net.ipv4.tcp_max_tw_buckets)
netdev_max_backlog=$(sysctl -n net.core.netdev_max_backlog)
somaxconn=$(sysctl -n net.core.somaxconn)
EOF
}

# Function to load values from defaults file
load_defaults() {
    if [ -f "$DEFAULTS_FILE" ]; then
        echo "Loading saved default values..."
        # Source the defaults file to set variables
        . "$DEFAULTS_FILE"
    else
        echo "No saved defaults found. Reading current system values..."
        read_system_defaults
        . "$DEFAULTS_FILE"
    fi
}

# Function to apply network settings
apply_settings() {
    local rmem_max=$1
    local rmem_default=$2
    local wmem_max=$3
    local wmem_default=$4
    local tcp_max_tw_buckets=$5
    local netdev_max_backlog=$6
    local somaxconn=$7
    
    echo "Applying network settings..."
    
    # Apply UDP buffer sizes
    sysctl -w net.core.rmem_max=$rmem_max
    sysctl -w net.core.rmem_default=$rmem_default
    sysctl -w net.core.wmem_max=$wmem_max
    sysctl -w net.core.wmem_default=$wmem_default
    
    # Apply socket settings
    sysctl -w net.ipv4.tcp_max_tw_buckets=$tcp_max_tw_buckets
    sysctl -w net.core.netdev_max_backlog=$netdev_max_backlog
    sysctl -w net.core.somaxconn=$somaxconn
    
    # Make changes persistent
    cat > /etc/sysctl.d/99-network-tuning.conf << EOF
# Network optimization settings for LXStudio/Chromatik
# UDP buffer sizes
net.core.rmem_max=$rmem_max
net.core.rmem_default=$rmem_default
net.core.wmem_max=$wmem_max
net.core.wmem_default=$wmem_default

# Socket settings
net.ipv4.tcp_max_tw_buckets=$tcp_max_tw_buckets
net.core.netdev_max_backlog=$netdev_max_backlog
net.core.somaxconn=$somaxconn
EOF
}

# Check for revert flag
if [ "$1" == "--revert" ]; then
    if [ ! -f "$DEFAULTS_FILE" ]; then
        echo "Error: No saved defaults found. Cannot revert settings."
        exit 1
    fi
    echo "Reverting network settings to saved default values..."
    load_defaults
    apply_settings "$rmem_max" "$rmem_default" "$wmem_max" "$wmem_default" \
                  "$tcp_max_tw_buckets" "$netdev_max_backlog" "$somaxconn"
    echo "Network settings have been reverted to default values."
    echo "You may need to restart your system for changes to take full effect."
    exit 0
fi

# Save defaults if they haven't been saved before
if [ ! -f "$DEFAULTS_FILE" ]; then
    read_system_defaults
fi

# Apply optimized settings
apply_settings 26214400 26214400 26214400 26214400 65536 2048 1024

echo "Network parameters have been optimized for LXStudio/Chromatik."
echo "Original settings have been saved to $DEFAULTS_FILE"
echo "To revert these changes, run: sudo $0 --revert"
echo "You may need to restart your application for changes to take effect."

# Display current settings
echo -e "\nCurrent network settings:"
sysctl net.core.rmem_max
sysctl net.core.rmem_default
sysctl net.core.wmem_max
sysctl net.core.wmem_default
sysctl net.ipv4.tcp_max_tw_buckets
sysctl net.core.netdev_max_backlog
sysctl net.core.somaxconn 