#! /usr/bin/env python3

import asyncio
import os
import select
import sys

from datetime import datetime
from time import time, sleep
from random import random
from tecfg import *
from mikeartnet import *

LAST_LOADED = 0

DEFAULT_PIXELS_PER_CHANNEL = 250
PIXELS_PER_UNIVERSE = 170
DMX_PER_UNIVERSE = PIXELS_PER_UNIVERSE * 3

FULL=255
RED    = [FULL,0,0]
GREEN  = [0,FULL,0]
BLUE   = [0,0,FULL]
YELLOW = [FULL,FULL,0]
PINK   = [FULL,0,FULL]
WHITE  = [FULL,FULL,FULL]

FLOW_SPACING = 10 # gap between white columns
DELAY = 0.1 # seconds between animation frames

def pop_n(l, n):
  rv = []
  for i in range(n):
    if not l:
      break
    rv.append(l.pop(0))
  return rv

def draw(ip, dots, yellow_start, yellow_end, anim_frame):
  dots = dots.copy()
  row_len = int(dots.pop(0))
  if dots[0].startswith('C'):
    channel_lengths = [int(s) for s in dots.pop(0)[1:].split(',')]
  else:
    channel_lengths = []
  next_starting_side = dots.pop(0)
  if next_starting_side not in ['L', 'R']:
    raise ValueError("Need an L or R to indicate first starting side") 

  full_dmx = []
  horiz_offset = 0.0 # The number of columns between the start pixel of this
                     # row and the first row. It can be negative and/or end in .5
  for row in dots:
    if all(c == 'g' for c in row):
      full_dmx.extend(RED * len(row))
      continue

    left, right = row.split('.')
    left_shift = 0 # The net change on the left side due to +'s and -'s
    for i in range(2):
      if i == 0:
        s = left
      else:
        s = right
      for c in s:
        if c == '-':
          if i == 0: left_shift -= 1
          row_len -= 1
        elif c == '+':
          if i == 0: left_shift += 1
          row_len += 1
        else:
          raise ValueError("Bad character %r" % c)

    if next_starting_side == 'L':
      row_color = GREEN
      next_starting_side = 'R'
      horiz_offset -= left_shift
      flip_row = False
    else:
      row_color = BLUE
      next_starting_side = 'L'
      horiz_offset -= left_shift
      flip_row = True
    if row_len < 1:
      raise ValueError("Too many dots; row length has shrunk to %d" % row_len)

    # Which index into this row, starting from the left, should
    # be the first pixel highlighted in white
    white_index = math.floor(anim_frame/2 - horiz_offset) % FLOW_SPACING

    for i in range(row_len):
      if flip_row:
        pixels_from_left = row_len - 1 - i
      else:
        pixels_from_left = i
      if pixels_from_left % FLOW_SPACING == white_index:
        full_dmx.extend(WHITE)
      else:
        full_dmx.extend(row_color)
    row_len -= 1
    horiz_offset += 0.5

  # Overwrite DMX values with yellow for the specified range, if any
  if yellow_start is not None:
    while yellow_start <= yellow_end:
      for component in range(3):
        full_dmx[yellow_start*3+component] = YELLOW[component]
      yellow_start += 1

  next_index = 0
  for channel in [1,2,3,4,5,6,7,8]:
    if channel_lengths:
      pixels_in_channel = channel_lengths.pop(0)
    else:
      pixels_in_channel = DEFAULT_PIXELS_PER_CHANNEL
    channel_dmx = pop_n(full_dmx, pixels_in_channel * 3)
    for offset in range(3):
      universe = channel * 10 + offset
      universe_dmx = pop_n(channel_dmx, DMX_PER_UNIVERSE)
      padding = (DMX_PER_UNIVERSE - len(universe_dmx)) // 3
      universe_dmx.extend(PINK * padding)
      assert len(universe_dmx) == DMX_PER_UNIVERSE
      send_packet(ip, universe, universe_dmx)


def file_timestamp(filename):
  age = None
  for tries in range(3):
    try:
      timestamp = os.path.getmtime(filename)
    except FileNotFoundError:
      sleep(0.1)
  if timestamp is None:
    raise ValueError(filename + " is missing")
  return timestamp


def main():
  yellow_start = None
  yellow_end = None
  framerate = 1 # Number of DELAY periods to go between incrementing the white stripes

  while len(sys.argv) > 3:
    arg = sys.argv.pop(-1)
    if arg.lower().startswith("y"):
      yellow_str = arg[1:]
      if '-' not in yellow_str:
        yellow_str += "-" + yellow_str
      yellow_start_str, yellow_end_str = yellow_str.split("-")
      yellow_start = int(yellow_start_str)
      yellow_end   = int(yellow_end_str)
    elif arg.lower().startswith("f"):
      framerate_str = arg[1:]
      framerate = int(framerate_str)
    else:
      raise ValueError("Unknown arg: " + arg)

  if len(sys.argv) != 3:
    sys.stderr.write("Need an IP and panel_id, and optionally a range of offsets to mark in yellow\n")
    sys.exit(1)

  _, ip, panel_id = sys.argv

  striping_instructions = None
  datafile = find_config_dir() + '/striping-instructions.txt'
  last_load = 0

  anim_frame = 0
  subframe = 0
  while True:
    datafile_timestamp = file_timestamp(datafile)
    if datafile_timestamp > last_load:
      last_load = datafile_timestamp
      striping_instructions = load_striping_instructions()
      current_time = datetime.now().strftime("%H:%M")
      print("Loaded datafile at " + current_time)
    draw(ip, striping_instructions[panel_id.upper()], yellow_start, yellow_end, anim_frame) 
    subframe += 1
    if framerate > 0 and subframe >= framerate:
      subframe = 0
      anim_frame += 1
    sleep(DELAY)


main()
