#! /usr/bin/env python3

import random
import sys

from time import time, sleep
from mikeartnet import *
from tecfg import *

RGB_PER_UNIVERSE = 170
DMX_PER_UNIVERSE = RGB_PER_UNIVERSE * 3
SPACING = 5
FRAMERATE =5 
FLOOD_LEN = 510
SOLID_IP = '10.7.19.121x'

UNIVERSES = [10, 11, 12,
             20, 21, 22,
             30, 31, 32,
             40, 41, 42]

ARTNET_RED   = (255,   0,   0)
ARTNET_GREEN = (  0, 255,   0)
ARTNET_BLUE  = (  0,   0, 255)
ARTNET_YELLOW= (255, 255,   0)
ARTNET_PINK  = (255,   0, 255)
ARTNET_CYAN  = (  0, 255, 255)
ARTNET_WHITE = (255, 255, 255)

CHANNEL_COLORS={
  1: ARTNET_RED,
  2: ARTNET_GREEN,
  3: ARTNET_BLUE,
  4: ARTNET_YELLOW,
}

RANDOM_EDGE_COLORS = [ARTNET_RED, ARTNET_GREEN, ARTNET_BLUE]
RANDOM_PANEL_COLORS = [ARTNET_YELLOW, ARTNET_CYAN, ARTNET_WHITE]

assigned_colors = dict()

# Turn [(ARTNET_RED, 3), ARTNET_BLUE(10)] into 3 reds and 10 blues, with all RGB values
# catenated together into a simple list of ints
def flatten_colors(color_pairs):
  rv = []
  for color, count in color_pairs:
    rv.extend(color * count)
  return rv


def set_channel(ip, channel, colors):
  if ip not in assigned_colors:
    assigned_colors[ip] = dict()
  assigned_colors[ip][channel] = flatten_colors(colors)


# Blacken all pixels except the ones corresponding to the current anim_frame
def blacken(dmx, anim_frame):
  for i in range(len(dmx)):
    pixel_num = i // 3
    if pixel_num % SPACING != anim_frame % SPACING:
      dmx[i] = 0

FAILURES = dict()
MAX_FAILURES = 3

def animate(anim_frame):
  start = time()
  for ip in assigned_colors:
    if ip not in FAILURES:
      FAILURES[ip] = 0
    for universe in UNIVERSES:
      if FAILURES[ip] >= MAX_FAILURES:
        continue
      channel = universe // 10
      offset = (universe % 10) * DMX_PER_UNIVERSE
      if channel in assigned_colors[ip]:
        dmx = assigned_colors[ip][channel][offset:offset+DMX_PER_UNIVERSE]
        if ip != SOLID_IP:
          blacken(dmx, anim_frame)
        padding = RGB_PER_UNIVERSE - len(dmx) // 3
        assert padding >= 0
        dmx.extend(ARTNET_PINK * padding)
        if send_packet(ip, universe, dmx):
          FAILURES[ip] = 0
        else:
          FAILURES[ip] += 1
          if FAILURES[ip] >= MAX_FAILURES:
            print(f"Won't send to {ip} anymore")
  end = time()
  elapsed_sec = end - start
  remaining_time = 1/FRAMERATE - elapsed_sec
  if remaining_time < 0:
    print("Overrran by %0.4f msec" % (remaining_time * -1000))
  else:
    sleep(remaining_time)

def usage():
  sys.stderr.write("Need to pass a list of IPs or one of 'flood', 'active', 'random'\n")
  sys.stderr.write("  flood:  Light up all channels on all configured IPs\n")
  sys.stderr.write("  active: Light up all channels configured with an active fixture\n")
  sys.stderr.write("  random: Like active, but use random per-fixture colors\n")
  sys.exit(1)

if __name__ == "__main__":
  ips = set()
  edges_and_panels = None
  mode = None

  for arg in sys.argv[1:]:
    if '-' in arg:
      module, num = arg.split('-')
      arg = "10.7." + module + "." + num
    if '.' in arg:
      if mode is not None: usage()
      octets = arg.split('.')
      if len(octets) != 4: usage()
      ips.add(arg)
    else:
      if mode is not None: usage()
      if ips: usage()
      if arg in ('flood', 'active', 'random'):
        edges_by_ip = load_edges()
        panels_by_ip = load_panels()
        ips = set(edges_by_ip).union(set(panels_by_ip))
        mode = arg
      else:
        print(f"Bad arg: {arg}")
        usage()

  if not ips: usage()

  print("Lighting: " + ' '.join(sorted(ips)))

  random_color_by_fixture = dict()
  for ip in sorted(ips):
    if mode in (None, 'flood'):
      for channel in [1, 2, 3, 4]:
        set_channel(ip, channel, [(CHANNEL_COLORS[channel], FLOOD_LEN)])
    else:
      if ip in edges_by_ip:
        channel_info = edges_by_ip[ip]
        is_edge = True
      else:
        channel_info = panels_by_ip[ip]
        is_edge = False
      for channel_minus_1, fixtures in enumerate(channel_info):
        if fixtures is None or len(fixtures) == 0:
          continue
        channel_pairs = []
        cursor = 0
        for fixture in fixtures:
          offset, fixture_id, num_fixture_pixels = fixture
          if mode == "random":
            fixture_key = fixture_id.split("_")[0]
            if fixture_key not in random_color_by_fixture:
              if is_edge:
                random_color_by_fixture[fixture_key] = random.choice(RANDOM_EDGE_COLORS)
              else:
                random_color_by_fixture[fixture_key] = random.choice(RANDOM_PANEL_COLORS)
            color = random_color_by_fixture[fixture_key]
          else:
            assert mode == "active"
            color = CHANNEL_COLORS[channel_minus_1+1]
          blank_space = offset - cursor
          assert blank_space >= 0
          if blank_space > 0:
            channel_pairs.append((ARTNET_PINK, blank_space))
            cursor = offset
          channel_pairs.append((color, num_fixture_pixels))
          cursor += num_fixture_pixels
        set_channel(ip, channel_minus_1 + 1, channel_pairs)

  anim_frame = 0
  while True:
    animate(anim_frame)
    anim_frame += 1
