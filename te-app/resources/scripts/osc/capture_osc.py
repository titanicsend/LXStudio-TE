#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Capture OSC signals and save as a .json file in current directory.
Formated as a TimeSerieBundle.

usage:

    python capture_osc.py -p 42069 -f recordings/<trackname>.json -d

"""

import sys
import time
import argparse

import liblo

from osc_recorder import CaptureOSCServer

parser = argparse.ArgumentParser(
    description='Capture OSC and save to .json file in current directory.')
parser.add_argument('-p', '--port',
                    dest='port', type=int, default=42069,
                    help='OSC port to listen on.')
parser.add_argument('-f', '--filename',
                    dest='filename', type=str,
                    default='OSC_capture',
                    help='Name of json file to save to.')
parser.add_argument('-d', '--debug',
                    action='store_true', dest='debug',
                    help='Print values captured.')

args = parser.parse_args()

try:
    server = CaptureOSCServer(args.port, debug=args.debug)
    server.start()
    print("Server started on %d. Press Ctrl-C to terminate and save." % (args.port))
    print("Data saved to %s.json" % args.filename)
    if not args.debug:
        print("Debug is off. Can be turned on with '-d' flag.")
except liblo.ServerError as err:
    print(str(err))

while True:
    try:
        time.sleep(0.1)
    except KeyboardInterrupt:
        time_series_bundle = server.get_time_series()
        time_series_bundle.to_json(args.filename)
        server.free()
        print('Done')
        sys.exit()

sys.exit()
