#! /usr/bin/env python3

import csv
import math
import os

PIXELS_PER_PANEL_CHANNEL = 250

def find_config_dir():
  file_name = "edges.txt"
  path = os.getcwd()
  while True:
    if os.path.isfile(os.path.join(path, file_name)):
      return path
    elif os.path.isfile(os.path.join(path, "resources", "vehicle", file_name)):
      return os.path.join(path, "resources", "vehicle")
    elif path == os.path.dirname(path):
      raise FileNotFoundError(f"{file_name} not found.")
    else:
      path = os.path.dirname(path)

def load_edges():
  config_dir = find_config_dir()
  rv = dict()
  with open(config_dir + '/edges.txt') as tsv_file:
    for row in csv.reader(tsv_file, delimiter="\t"):
      edge_id, _, num_pixels_str, output = row
      num_pixels = int(num_pixels_str)
      ip, chaninfo = output.split("#")
      if '?' in ip:
        continue
      if ip.startswith('x'):
        ip = ip[1:]
        edge_id += "(disabled)"
      if ip not in rv:
        rv[ip] = [[], [], [], []]
      chaninfo_tokens = chaninfo.split(":")
      channel_str = chaninfo_tokens[0]
      channel = int(channel_str)
      if len(chaninfo_tokens) == 2:
        offset_str = chaninfo_tokens[1]
        offset = int(offset_str)
      else:
        offset = 0
      rv[ip][channel-1].append((offset, edge_id, num_pixels))
      rv[ip][channel-1].sort()
  return rv


def load_panels():
  config_dir = find_config_dir()
  rv = dict()
  with open(config_dir + '/panels.txt') as tsv_file:
    for row in csv.reader(tsv_file, delimiter="\t"):
      panel_id, num_panel_pixels_str, _, _, _, _, _, outputs_str = row
      num_panel_pixels = int(num_panel_pixels_str)
      num_channels = math.ceil(num_panel_pixels/PIXELS_PER_PANEL_CHANNEL)
      if '?' in outputs_str:
        continue
      outputs = list(outputs_str.split("/"))
      ip = None
      channel = 5
      last_channel = None
      for i in range(num_channels):
        if channel > 4:
          ip, channel_str = outputs.pop(0).split("#")
          tokens = channel_str.split("-")
          channel = int(tokens[0])
          if len(tokens) == 1:
            last_channel = 4
          elif len(tokens) == 2:
            last_channel = int(tokens[1])
          else:
            raise ValueError("Couldn't parse " + channel_str)
          if ip.startswith("x"):
            ip = ip[1:]
            panel_id += "(disabled)"
        subpanel_id = f"{panel_id}_{i * PIXELS_PER_PANEL_CHANNEL}"
        if ip not in rv:
          rv[ip] = [None, None, None, None]
        assert rv[ip][channel-1] is None
        num_subpanel_pixels = min(num_panel_pixels, PIXELS_PER_PANEL_CHANNEL)
        num_panel_pixels -= num_subpanel_pixels
        rv[ip][channel-1] = [(0, subpanel_id, num_subpanel_pixels)]
        channel += 1
        if channel > last_channel:
          channel = 5 # Hack to force channel > 4 on next loop
      if outputs != []:
        raise ValueError(panel_id + " is configured with more outputs "
                         "than necessary")
  return rv


def load_striping_instructions():
  config_dir = find_config_dir()
  rv = dict()
  with open(config_dir + '/striping-instructions.txt') as fd:
    for line in fd.readlines():
      line = line.strip()
      tokens_with_comments = line.split(" ")
      tokens = []
      commenting = False
      for token in tokens_with_comments:
        if commenting:
          if token[-1] == ")":
            commenting = False
          continue
        elif token[0] == "(":
          commenting = token[-1] != ")"
        else:
          tokens.append(token)
      panel_id = tokens.pop(0)
      assert panel_id not in rv
      if not tokens: continue
      rv[panel_id] = tokens
  return rv
