#! /usr/bin/env python

import csv
import importlib
import math
import sys

PIXELS_PER_PANEL_CHANNEL = 250

ap = importlib.import_module("angio-pinger")

if len(sys.argv) == 1:
  module = None
else:
  assert len(sys.argv) == 2
  module = int(sys.argv[1])

config_dir = ap.find_config_dir()

backpacks_by_ip = dict()
class Backpack:
  def __init__(self, ip):
    self.ip = ip
    self.channels = [None, None, None, None]

  def add_edge(self, channel, edge_id):
    if self.channels[channel-1] is None:
      self.channels[channel-1] = edge_id
    else:
      self.channels[channel-1] += ", " + edge_id 

  def add_subpanel(self, channel, subpanel_id):
    assert self.channels[channel-1] is None
    self.channels[channel-1] = subpanel_id

def find_or_make_backpack(ip):
  if ip not in backpacks_by_ip:
    backpacks_by_ip[ip] = Backpack(ip)
  return backpacks_by_ip[ip]

with open(config_dir + '/edges.txt') as tsv_file:
  for row in csv.reader(tsv_file, delimiter="\t"):
    edge_id, _, _, output = row
    ip, chaninfo = output.split("#")
    if '?' in ip:
      continue
    if ip.startswith('x'):
      ip = ip[1:]
      edge_id += "(disabled)"
    channel_str = chaninfo.split(":")[0]
    channel = int(channel_str)
    backpack = find_or_make_backpack(ip)
    backpack.add_edge(channel, edge_id)
  
with open(config_dir + '/panels.txt') as tsv_file:
  for row in csv.reader(tsv_file, delimiter="\t"):
    panel_id, num_pixels_str, _, _, _, _, _, outputs_str = row
    num_pixels = int(num_pixels_str)
    num_channels = math.ceil(num_pixels/PIXELS_PER_PANEL_CHANNEL)    
    if '?' in outputs_str:
      continue
    outputs = list(outputs_str.split("/"))
    ip = None
    channel = 5
    for i in range(num_channels):
      if channel > 4:
        ip, channel_str = outputs.pop(0).split("#")
        channel = int(channel_str)
        if ip.startswith("x"):
          ip = ip[1:]
          panel_id += "(disabled)"
      backpack = find_or_make_backpack(ip)
      subpanel_id = f"{panel_id}_{i * PIXELS_PER_PANEL_CHANNEL}"
      backpack.add_subpanel(channel, subpanel_id)
      channel += 1

for ip in sorted(backpacks_by_ip):
  if module is not None and not ip.startswith(f"10.7.{module}."):
    continue
  backpack = backpacks_by_ip[ip]
  print(ip + ":")
  for i in range(4):
    print(f"  {i+1}: {backpack.channels[i]}")
