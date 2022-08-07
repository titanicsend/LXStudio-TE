#! /usr/bin/env python3

import os
from time import time

RESOURCES_DIR = "../../resources/vehicle"
STRIPING_FILE = RESOURCES_DIR + "/striping-instructions.txt"
PANEL_PATHS_FILE = RESOURCES_DIR + "/panel_signal_paths.tsv"
EDGE_PATHS_FILE = RESOURCES_DIR + "/edge_signal_paths.tsv"
CONTROLLERS_FILE =  RESOURCES_DIR + "/controllers.tsv"
PANELS_FILE = RESOURCES_DIR + "/panels.txt"
EDGES_FILE = RESOURCES_DIR + "/edges.txt"

PANELS = dict()
EDGES = dict()
EDGE_CONTROLLERS = dict()
EDGE_PIXELS = dict()
EDGE_SOURCES = dict()
EDGE_OFFSETS = dict()
TAILS = dict()
CONTROLLERS = dict()
CHANNEL_LENGTHS = dict()
NOW = time()

for line in open(STRIPING_FILE).readlines():
  line = line.strip()
  tokens = line.split()
  if len(tokens) >= 3:
    id = tokens[0]
    c = tokens[2]
    if c[0] == "C":
      CHANNEL_LENGTHS[id] = c[1:].split(",")

expected_header = "\t".join(["Panel ID", "Edge1", "Edge2", "Edge3",
                             "Type", "Channels", "Controller vertex",
                             "Signal in vertex", "Signal edge",
                             "Build Priority",
                             "Responsible Power Box Junction IDs"])
for line in open(PANEL_PATHS_FILE).readlines():
  line = line.strip()
  if expected_header:
    if line != expected_header:
      raise Exception("Unexpected header %r" % line)
    expected_header = None
    continue
  tokens = line.split("\t")
  assert len(tokens) == 11
  id = tokens[0]
  kind = tokens[4]
  channels = int(tokens[5])
  controller_vertex = int(tokens[6])
  if channels < 1:
    print (id + " has no channels")
    continue
  if controller_vertex not in CONTROLLERS:
    CONTROLLERS[controller_vertex] = []
  if id in CHANNEL_LENGTHS:
    pixels_per_channel = CHANNEL_LENGTHS[id]
    if len(pixels_per_channel) != channels:
      raise Exception("Panel %s is configured to have %d channels in the striping "
                      "instructions but has %d in the paths file" %
                      (id, len(pixels_per_channel), channels))
  else:
    pixels_per_channel = [500] * channels
  first_channel = len(CONTROLLERS[controller_vertex]) + 1
  if first_channel <= 8:
    octet = 1
  else:
    assert first_channel <= 16
    octet = 2
    first_channel -= 8
  last_channel = first_channel + channels - 1
  if last_channel > 8:
    raise Exception("%s would span two controllers" % id)
  ip = "10.7.%d.%d" % (controller_vertex, octet)
  PANELS[id] = "%s#%d" % (ip, first_channel)
  for i in range(channels):
    CONTROLLERS[controller_vertex].append(id)

expected_header = "\t".join(["Edge ID", "Signal from", "Controller vertex", "Priority",
                             "Pixels"])
for line in open(EDGE_PATHS_FILE).readlines():
  line = line.strip()
  if expected_header:
    if line != expected_header:
      raise Exception("Unexpected header %r" % line)
    expected_header = None
    continue
  tokens = line.split("\t")
  assert len(tokens) == 5
  id = tokens[0]
  sig_from = tokens[1]
  pixels = int(tokens[4])
  EDGE_PIXELS[id] = pixels
  if sig_from == "Controller":
    controller_vertex = int(tokens[2])
    if controller_vertex not in CONTROLLERS:
      CONTROLLERS[controller_vertex] = []
    channel = len(CONTROLLERS[controller_vertex]) + 1
    if channel <= 8:
      octet = 1
    else:
      assert channel <= 16
      octet = 2
      channel -= 8
    ip = "10.7.%d.%d" % (controller_vertex, octet)
    EDGES[id] = "%s#%d" % (ip, channel)
    EDGE_CONTROLLERS[id] = EDGES[id]
    EDGE_OFFSETS[id] = 0
    TAILS[id] = pixels
    CONTROLLERS[controller_vertex].append(id)
  else:
    assert tokens[2] == ""
    EDGE_SOURCES[id] = sig_from

while EDGE_SOURCES:
  # print (repr(EDGE_SOURCES))
  next = dict()
  for dst, src in EDGE_SOURCES.items():
    if src not in EDGE_OFFSETS:
      next[dst] = src
    else:
      TAILS.pop(src)
      EDGE_OFFSETS[dst] = EDGE_OFFSETS[src] + EDGE_PIXELS[src]
      TAILS[dst] = EDGE_PIXELS[dst] + EDGE_OFFSETS[dst]
      EDGE_CONTROLLERS[dst] = EDGE_CONTROLLERS[src]
      EDGES[dst] = EDGE_CONTROLLERS[dst] + ":%d" % EDGE_OFFSETS[dst]
  if len(next) == len(EDGE_SOURCES):
    raise Exception("Can't map edge map")
  EDGE_SOURCES = next

if False:
  for id in sorted(TAILS.keys()):
    print ("Plug into %s to get pixel #%d of its channel" % (id, TAILS[id]))

fd_out = open(CONTROLLERS_FILE, "w")
for vertex in sorted(CONTROLLERS):
  connections = CONTROLLERS[vertex]
  print("%d\t%s" % (vertex, "\t".join(connections)), file=fd_out)
fd_out.close()
print ("Wrote " + CONTROLLERS_FILE)
  
fd_in = open(PANELS_FILE)
outfile = PANELS_FILE + ".new"
fd_out = open(outfile, "w")
for line in fd_in.readlines():
  line = line.strip()
  tokens = line.split("\t")
  assert len(tokens) == 6
  if tokens[0] in PANELS and (tokens[5] == "lit" or "." in tokens[5]):
    tokens[5] = PANELS.pop(tokens[0])
    line = "\t".join(tokens)
  print(line, file=fd_out)
fd_out.close()
os.rename(PANELS_FILE, "%s.%d" % (PANELS_FILE, NOW))
os.rename(outfile, PANELS_FILE)
print("Made a backup and updated " + PANELS_FILE)

if PANELS:
  print("Couldn't write these panels to the config:")
  for id, cfg in PANELS.items():
    print("%s %s" % (id, cfg))

fd_in = open(EDGES_FILE)
outfile = EDGES_FILE + ".new"
fd_out = open(outfile, "w")
for line in fd_in.readlines():
  line = line.strip()
  tokens = line.split("\t")
  assert len(tokens) == 3
  if tokens[0] in EDGES and (tokens[2] == "uncontrolled" or "." in tokens[2]):
    tokens[2] = EDGES.pop(tokens[0])
    line = "\t".join(tokens)
  print(line, file=fd_out)
fd_out.close()
os.rename(EDGES_FILE, "%s.%d" % (EDGES_FILE, NOW))
os.rename(outfile, EDGES_FILE)
print("Made a backup and updated " + EDGES_FILE)

if EDGES:
  print("Couldn't write these edges to the config:")
  for id, cfg in EDGES.items():
    print("%s %s" % (id, cfg))
