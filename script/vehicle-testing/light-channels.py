#! /usr/bin/env python3

import sys

from time import time, sleep
from mikeartnet import *
from tecfg import *

RGB_PER_UNIVERSE = 170
DMX_PER_UNIVERSE = RGB_PER_UNIVERSE * 3
SPACING = 10
FRAMERATE = 10
FLOOD_LEN = 500

UNIVERSES = [10, 11, 12,
             20, 21, 22,
             30, 31, 32,
             40, 41, 42]

ARTNET_RED   = (255,   0,   0)
ARTNET_GREEN = (  0, 255,   0)
ARTNET_BLUE  = (  0,   0, 255)
ARTNET_YELLOW= (255, 255,   0)
ARTNET_PINK  = (255,   0, 255)
ARTNET_WHITE = (255, 255, 255)

CHANNEL_COLORS={
  1: ARTNET_RED,
  2: ARTNET_GREEN,
  3: ARTNET_BLUE,
  4: ARTNET_YELLOW,
}

assigned_colors = dict()

# Turn [(ARTNET_RED, 3), ARTNET_BLUE(10)] into 3 reds and 10 blues, with all RGB values
# catenated together into a simple list of ints
def flatten_colors(colors):
  rv = []
  for color, count in colors:
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
        edges_and_panels = load_edges_and_panels()
        ips = edges_and_panels.keys()
      else:
        usage()

  if not ips: usage()

  print("Lighting: " + ' '.join(sorted(ips)))

  for ip in ips:
    if mode in (None, 'flood'):
      for channel in [1, 2, 3, 4]:
        set_channel(ip, channel, [(CHANNEL_COLORS[channel], FLOOD_LEN)])
    else:
      channel_info = edges_and_panels[ip]
      for channel_minus_1, fixtures in enumerate(channel_info):
        channel_pixels = []
        for fixture in fixtures:
          num_fixture_pixels = fixture[1]
          if mode == "random":
            color = random.choice(CHANNEL_COLORS)
          else:
            assert mode == "active"
            color = CHANNEL_COLORS[channel_minus_1+1]
          channel_pixels.extend(color * num_fixture_pixels)
        set_channel(ip, channel, channel_pixels.extend)

  anim_frame = 0
  while True:
    animate(anim_frame)
    anim_frame += 1
