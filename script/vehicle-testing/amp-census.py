#! /usr/bin/env python3

# How much time to light up a channel before reading its current
LIGHT_DELAY = 0.1

# How much time to sleep between Artnet packets
LIGHT_PERIOD = 0.1


# Dark channels should draw less than this; lit channels should draw more
AMP_THRESHOLD = 0.25

import importlib
import sys
import threading

from mikeartnet import *

cr = importlib.import_module("channel-report")
av = importlib.import_module("angio-validator")
lc = importlib.import_module("light-channels")

cr.init_backpacks()

if len(sys.argv) == 1:
  ip_wanted = None
else:
  assert len(sys.argv) == 2
  ip_wanted = sys.argv[1]

keep_lighting = False
def light_up(ip, lit_channel):
  while keep_lighting:
    for channel in [1,2,3,4]:
      for subchannel in range(3):
        universe = channel * 10 + subchannel
        if channel == lit_channel:
          color = lc.CHANNEL_COLORS[channel]
        else:
          color = (0,0,0)
        dmx = color * lc.RGB_PER_UNIVERSE
        send_packet(ip, universe, dmx, silent=True)
    sleep(LIGHT_PERIOD)


def check_currents(ip):
  global keep_lighting
  rv = []

  # Channel 0 leaves everything dark and thus establishes the baseline
  for channel in range(5):
    keep_lighting = True
    thread = threading.Thread(target=light_up, args=(ip, channel))
    thread.start()
    sleep(LIGHT_DELAY)
    currents = av.get_currents(ip)
    keep_lighting = False
    thread.join()
    if currents is None:
      return None
    rv.append(currents)

  return rv    
  
if ip_wanted is not None and ip_wanted not in cr.backpacks_by_ip:
  print(f"{ip_wanted} is not mapped to anything; pretending it's full")
  cr.backpacks_by_ip[ip_wanted] = cr.Backpack(ip_wanted) 

for ip in sorted(cr.backpacks_by_ip):
  if ip_wanted is not None and not ip == ip_wanted:
    continue
  backpack = cr.backpacks_by_ip[ip]
  
  currents = check_currents(ip)

  if currents is None:
    print(cr.YELLOW(ip + " is unreachable"))
  else:
    for channel in [1,2,3,4]:
      amps = currents[0][channel-1]
      amp_str = '%0.2fA' % amps
      if amps >= AMP_THRESHOLD:
        print(cr.RED(f"{ip}#{channel} is drawing {amp_str} "
                     f"when it should be dark"))
      cross_powered = False
      for other_channel in [1,2,3,4]:
        if other_channel == channel: continue
        amps = currents[other_channel][channel-1]
        if amps >= AMP_THRESHOLD:
          print(cr.RED(f"{ip}#{channel} is drawing power when #{other_channel} is on"))
      amps = currents[channel][channel-1]
      amp_str = '%0.2fA' % amps
      should_be_on = backpack.channels[channel-1] not in (None, "BAD")
      if should_be_on:
        if amps < AMP_THRESHOLD:
          print(cr.CYAN(f"{ip}#{channel} is only drawing {amp_str} "
                        f"when it should be lit up"))
        else:
          print(cr.GREEN(f"{ip}#{channel} is drawing {amp_str} as expected"))
      else:
        if amps < AMP_THRESHOLD:
          print(f"{ip}#{channel} is only drawing {amp_str}, as expected "
                "because it's unused")
        else:
          print(cr.RED(f"{ip}#{channel} is drawing {amp_str} even though "
                "nothing is supposed to be connected"))

