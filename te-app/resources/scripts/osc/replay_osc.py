#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Replay OSC signals from a file in current directory.
usage:

    python replay_osc.py -f recordings/<trackname>.json -a 127.0.0.1 -p 42069 -d

WARNING

Right now this package is terrible in terms of timing -- it's like 1.5 - 2x real time.
So we can't play the track along very well, that would be the goal eventually is to play
the track at the same rate as the OSC stream, and have a full ShowKontrol emulator.

This is apparently known issue: https://github.com/oishefarhan/OSC_recorder/issues/2
"""

import os
import argparse

from osc_recorder import TimeSeriesBundle, replay_osc, replay_osc_time_bundle

parser = argparse.ArgumentParser(
    description='Replay OSC data stream from file')
parser.add_argument('-a', '--address',
                    dest='address', type=str, default='127.0.0.1',
                    help='OSC address to replay to.')
parser.add_argument('-p', '--port',
                    dest='port', type=int, default=42069,
                    help='OSC port to listen on.')
parser.add_argument('-l', '--loop',
                    action='store_true', dest='loop',
                    help='Replay over and over.')
parser.add_argument('-d', '--debug',
                    action='store_true', dest='debug',
                    help='Print values being replayed.')
parser.add_argument('-t', '--time-bundle',
                    action='store_true', dest='time_bundle',
                    help='Sends using timestamped bundle format.')
requiredNamed = parser.add_argument_group('required arguments')
requiredNamed.add_argument('-f', '--filename',
                           dest='filename', type=str, required=True,
                           help='Name of file to replay.')
args = parser.parse_args()

file_path = os.path.abspath(args.filename)
data = TimeSeriesBundle()
data.from_json(file_path)


if args.time_bundle:
    replay_osc_time_bundle(data, address=args.address, port=args.port,
                           loop=args.loop, debug=args.debug)
else:
    replay_osc(data, address=args.address, port=args.port,
               loop=args.loop, debug=args.debug)

print('Done')
