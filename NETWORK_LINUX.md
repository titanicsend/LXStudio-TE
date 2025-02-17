# Linux Network Configuration Guide

## Overview

When running LXStudio/Chromatik on Linux, you may encounter network-related warnings or performance issues, particularly with UDP packet handling. This is common in real-time lighting control applications and can be resolved through proper system configuration.

## Quick Start

1. Run the network optimization script:
```bash
sudo ./setup_network.sh
```

2. Restart the application to apply changes.

## Common Issues

1. **UDP Blocking Warning**:
   ```
   Calls to DatagramSocket.send() appear to be unexpectedly blocking...
   ```
   This warning indicates that your system's network buffers are too small for real-time data transmission.

2. **Packet Loss**: If you experience flickering or inconsistent lighting behavior, this might be due to UDP packet drops.

## Manual Configuration

If you prefer to configure settings manually, here are the key parameters to adjust:

1. **UDP Buffer Sizes**:
   ```bash
   # Recommended values (~25MB)
   net.core.rmem_max=26214400
   net.core.rmem_default=26214400
   net.core.wmem_max=26214400
   net.core.wmem_default=26214400
   ```

2. **Socket Settings**:
   ```bash
   net.ipv4.tcp_max_tw_buckets=65536
   net.core.netdev_max_backlog=2048
   net.core.somaxconn=1024
   ```

To apply manually:
1. Edit `/etc/sysctl.conf` or create a new file in `/etc/sysctl.d/`
2. Add the above parameters
3. Run `sudo sysctl -p` to apply

## Using the Setup Script

Our `setup_network.sh` script automates this configuration:

- **Apply Optimizations**:
  ```bash
  sudo ./setup_network.sh
  ```

- **Revert Changes**:
  ```bash
  sudo ./setup_network.sh --revert
  ```

- **View Current Settings**:
  ```bash
  sysctl net.core.rmem_max
  sysctl net.core.wmem_max
  ```

## What These Settings Do

1. **Buffer Sizes** (`rmem_*/wmem_*`):
   - Increases UDP receive/send buffer sizes
   - Prevents packet drops during high-throughput operations
   - Default (208KB) → Optimized (25MB)

2. **Socket Settings**:
   - `tcp_max_tw_buckets`: Manages connection cleanup
   - `netdev_max_backlog`: Controls packet queue size
   - `somaxconn`: Sets connection backlog limit

## Troubleshooting

1. **Settings Not Persisting**:
   - Check if `/etc/sysctl.d/99-network-tuning.conf` exists
   - Verify file permissions
   - Try rebooting the system

2. **Still Seeing Warnings**:
   - Verify current settings with `sysctl -a | grep net.core`
   - Check application logs for specific packet sizes
   - Consider increasing buffer sizes further

3. **Performance Issues**:
   - Monitor system with `netstat -s | grep -i udp`
   - Check for packet drops
   - Consider network interface settings

## Additional Notes

- Settings are persistent across reboots
- Original values are backed up to `/tmp/network_defaults_chromatik.conf`
- Some changes may require a system restart
- Monitor system logs for network-related messages

## Support

If you encounter issues:
1. Check the application logs
2. Verify current settings
3. Try reverting and reapplying changes
4. Consult the project's issue tracker 