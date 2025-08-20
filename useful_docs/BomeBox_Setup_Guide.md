# BomeBox Setup Guide for Remote MIDI Control

## Overview
This guide explains how to set up BomeBox for remote MIDI control when your Mac Mini is connected via Ethernet and you need to control Chromatik from a remote location.

## Your Setup
- **Mac Mini**: Connected to internet via Ethernet cable
- **WiFi**: Available for creating a local network
- **Goal**: Control Chromatik remotely using MIDI controllers

## How It Works
- **Ethernet**: Provides internet connection to Mac Mini
- **WiFi**: Creates local network for BomeBox and remote control
- **Both work simultaneously**

## Step-by-Step Setup

### Step 1: Create WiFi Hotspot from Mac Mini

**On your Mac Mini:**
1. **System Preferences** → **Sharing**
2. Turn on **Internet Sharing**
3. **Share from**: **Ethernet** (your internet connection)
4. **To computers using**: **WiFi**
5. Click **WiFi Options**
6. Set network name (like "BurningManVJ") and password
7. Click **OK**

### Step 2: Connect BomeBox

1. Power on BomeBox
2. BomeBox will create its own temporary WiFi network
3. Connect to BomeBox's temporary network with your phone/laptop
4. Open BomeBox web interface (usually 192.168.1.1)
5. Set BomeBox to **Client Mode**
6. Connect BomeBox to your "BurningManVJ" WiFi network
7. Enter the password you set in Step 1

### Step 3: Connect Remote VJ Setup

1. Your laptop/tablet connects to same "BurningManVJ" WiFi network
2. Now you can control Chromatik remotely

### Step 4: Configure MIDI Controllers

1. Plug your MIDI controllers into BomeBox's USB port
2. On Mac Mini, open **Bome Network** software
3. Enable **Remote Direct MIDI** for your controllers
4. Controllers should appear with names like "FoH: APC mini mkII"

## Final Result

- **Mac Mini**: Has internet (via Ethernet) + creates local WiFi network
- **BomeBox**: Connected to local WiFi, sends MIDI data
- **Remote VJ**: Connected to local WiFi, can control Chromatik
- **Controllers**: Plugged into BomeBox, work wirelessly

## Troubleshooting

- **BomeBox not connecting**: Check WiFi password and network name
- **Controllers not appearing**: Verify Remote Direct MIDI is enabled in Bome Network
- **Connection drops**: Check WiFi signal strength between locations

## Notes

- This is the most common setup for remote MIDI control
- Ethernet provides internet, WiFi provides local control network
- BomeBox acts as a wireless MIDI bridge between controllers and Chromatik
