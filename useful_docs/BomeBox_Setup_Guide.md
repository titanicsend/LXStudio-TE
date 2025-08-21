# BomeBox Setup Guide for Remote MIDI Control

## Overview
This guide explains how to set up BomeBox for remote MIDI control when your Mac Mini is connected via Ethernet and BomeBox connects to your main WiFi network.

## Your Setup
- **Mac Mini**: Connected to internet via Ethernet cable
- **Main WiFi Router**: Broadcasting WiFi network (not Mac Mini)
- **BomeBox**: Connected to main WiFi network
- **Goal**: Control Chromatik remotely using MIDI controllers

## How It Works
- **Ethernet**: Provides internet connection to Mac Mini
- **Main WiFi**: Network that both Mac Mini and BomeBox connect to
- **BomeBox**: Receives MIDI from controllers, sends over WiFi to Mac Mini

## Step-by-Step Setup

### Step 1: Network Setup

**Your network should be:**
1. **Main WiFi Router**: Broadcasting your WiFi network
2. **Mac Mini**: Connected to router via Ethernet (for internet)
3. **Mac Mini**: Also connected to same WiFi network (for local communication)
4. **BomeBox**: Connected to same WiFi network

### Step 2: Connect BomeBox to Main WiFi

1. **Power on BomeBox**
2. **Connect to BomeBox's temporary WiFi** (it creates its own network initially)
3. **Open BomeBox web interface** (usually 192.168.1.1)
4. **Set BomeBox to Client Mode**
5. **Connect BomeBox to your main WiFi network** (the one your router broadcasts)
6. **Enter your main WiFi password**

### Step 3: Configure Bome Network Software

1. **On Mac Mini**: Open **Bome Network** software
2. **Look for your BomeBox** in the network list
3. **Enable "Remote Direct MIDI"** for your controllers
4. **Controllers should appear** with names like "FoH: APC mini mkII"

### Step 4: Connect MIDI Controllers

1. **Plug your MIDI controllers** into BomeBox's USB port
2. **Verify they appear** in Bome Network software
3. **Test the connection** by pressing buttons on your controller

## Final Result

- **Main WiFi Router**: Provides network connectivity
- **Mac Mini**: Connected via Ethernet (internet) + WiFi (local network)
- **BomeBox**: Connected to main WiFi, sends MIDI data
- **Controllers**: Plugged into BomeBox, work wirelessly
- **Remote VJ**: Can control Chromatik from anywhere on the same WiFi network

## Troubleshooting

- **BomeBox not connecting**: Check WiFi password and network name
- **Controllers not appearing**: Verify Remote Direct MIDI is enabled in Bome Network
- **Connection drops**: Check WiFi signal strength between locations
- **Mac Mini not seeing BomeBox**: Ensure both are on the same WiFi network

## Notes

- **No WiFi sharing needed** - both devices connect to main WiFi router
- **More reliable** than Mac Mini WiFi sharing
- **Better range** - uses your main WiFi router's antenna
- **Easier setup** - no need to deal with macOS WiFi sharing bugs
