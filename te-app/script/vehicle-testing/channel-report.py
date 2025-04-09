#! /usr/bin/env python

import importlib
import sys

from tecfg import *

BAD_CHANNELS = [
  ('10.7.?.?', 4),
]

MAX_PIXELS_PER_EDGE_CHAIN = 500

av = importlib.import_module("angio-validator")

backpacks_by_ip = dict()
class Backpack:
  def __init__(self, ip):
    self.ip = ip
    self.channels = [None, None, None, None]
    self.type = None
    for bad_ip, bad_channel in BAD_CHANNELS:
      if bad_ip == ip:
        self.channels[bad_channel-1] = "BAD"

  def add_edge(self, channel, edge_id, num_pixels):
    if self.type is None:
      self.type = "edge"
      self.num_pixels = 0
    if self.type != "edge":
      raise ValueError(ip + " is configured with both edges and panels")
    for other_channel in [1,2,3,4]:
      if other_channel == channel: continue
      if self.channels[other_channel-1] is not None:
        raise ValueError(ip + " has edges configured on multiple channels")
    self.num_pixels += num_pixels
    if self.channels[channel-1] is None:
      self.channels[channel-1] = edge_id
    else:
      assert self.channels[channel-1] != "BAD"
      self.channels[channel-1] += ", " + edge_id 

  def add_subpanel(self, channel, subpanel_id):
    if self.type is None:
      self.type = "panel"
      self.num_pixels = None
    if self.type != "panel":
      raise ValueError(ip + " is configured with both edges and panels")
    if self.channels[channel-1] is not None:
      raise ValueError(f"{self.ip} #{channel} assigned to both "
          f"{self.channels[channel-1]} and {subpanel_id}")
    self.channels[channel-1] = subpanel_id


def find_or_make_backpack(ip):
  if ip not in backpacks_by_ip:
    backpacks_by_ip[ip] = Backpack(ip)
  return backpacks_by_ip[ip]

def init_backpacks():
  for ip, edge_lists in load_edges().items():
    backpack = find_or_make_backpack(ip)
    for channel_minus_1, edge_list in enumerate(edge_lists):
      for (offset, edge_id, num_pixels) in edge_list:
        backpack.add_edge(channel_minus_1 + 1, edge_id, num_pixels)

  for ip, subpanel_ids in load_panels().items():
    backpack = find_or_make_backpack(ip)
    for channel_minus_1, subpanel_tuples in enumerate(subpanel_ids):
      if subpanel_tuples is None:
        subpanel_id = None
      else:
        assert len(subpanel_tuples) == 1
        subpanel_tuple = subpanel_tuples[0]
        offset, subpanel_id, num_pixels = subpanel_tuple
      if subpanel_id is not None:
        backpack.add_subpanel(channel_minus_1 + 1, subpanel_id)

def colorize(s, color_code):
  return "\033[%dm%s\033[0m" % (color_code, s)

def RED(s):
  return colorize(s, 91)

def GREEN(s):
  return colorize(s, 92)

def YELLOW(s):
  return colorize(s, 93)

def CYAN(s):
  return colorize(s, 96)

def WHITE(s): # Bright/bold white
  return colorize(s, 97)

def GRAY(s):
  return colorize(s, 90)


if __name__ == "__main__":
  if len(sys.argv) == 1:
    module = None
  else:
    assert len(sys.argv) == 2
    module = int(sys.argv[1])

  init_backpacks()

  for ip in sorted(backpacks_by_ip):
    if module is not None and not ip.startswith(f"10.7.{module}."):
      continue
    backpack = backpacks_by_ip[ip]
    octets = ip.split(".")
    ip = (GRAY(octets[0] + "." + octets[1] + ".") + YELLOW(octets[2]) +
          GRAY(".") + CYAN(octets[3]) + GRAY(":"))
    print(ip)
    for i in range(4):
      label = backpack.channels[i]
      if label == "BAD":
        label = RED(label)
      elif label is not None:
        label = WHITE(label)
        if backpack.num_pixels is not None:
          num_remaining_pixels = MAX_PIXELS_PER_EDGE_CHAIN - backpack.num_pixels
          if num_remaining_pixels < 0:
            label += RED(f" ({-num_remaining_pixels}px too long)")
          elif num_remaining_pixels > 0:
            label += " (" + GREEN(str(num_remaining_pixels)) + "px left)"
          else:
            label += " (Exactly full)"
      elif backpack.type == "edge":
        label = GRAY("Unavailable")
      else:
        label = GREEN("Free")
      print(f"  {i+1}: {label}")
