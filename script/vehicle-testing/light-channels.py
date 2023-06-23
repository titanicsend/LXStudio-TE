#! /usr/bin/env python3

import sys

from time import time, sleep
from mikeartnet import *
from tecfg import *

RGB_PER_UNIVERSE = 170
DMX_PER_UNIVERSE = RGB_PER_UNIVERSE * 3
SPACING = 10
FRAMERATE = 10
MAX_CHANNEL_LEN = 250

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


if __name__ == "__main__":
  ips = set()
  if len(sys.argv) == 2 and sys.argv[1] == 'all':
    ips = get_all_backpack_ips()
    print("Lighting: " + ' '.join(sorted(ips)))
  else:
    for ip in sys.argv[1:]:
      assert ip != 'all'
      if ip in ips:
        raise ValueError(f"You said {ip} twice")
      ips.add(ip)

  if not ips:
    sys.stderr.write("You need to specify some IPs, or 'all'\n")
    sys.exit(1)

  for ip in ips:
    set_channel(ip, 1, [(ARTNET_RED,    MAX_CHANNEL_LEN)])
    set_channel(ip, 2, [(ARTNET_GREEN,  MAX_CHANNEL_LEN)])
    set_channel(ip, 3, [(ARTNET_BLUE,   MAX_CHANNEL_LEN)])
    set_channel(ip, 4, [(ARTNET_YELLOW, MAX_CHANNEL_LEN)])

  anim_frame = 0
  while True:
    animate(anim_frame)
    anim_frame += 1
