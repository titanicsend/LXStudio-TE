#! /usr/bin/env python3

LISTEN_IP = '127.0.0.1'
LISTEN_PORT = 6454

BUFSIZE=1500

import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

sock.bind((LISTEN_IP, LISTEN_PORT))

def get_color(r,g,b):
  if r == 0 and g == 0 and b == 0:
    return 'K'
  if g == 0 and b == 0:
    return 'R'
  if r == 0 and b == 0:
    return 'G'
  if r == 0 and g == 0:
    return 'B'
  if r == 0:
    return 'C'
  if g == 0:
    return 'M'
  if b == 0:
    return 'Y'
  return 'W'

def ansi_from_color(c):
  if c == 'K':
    code = 90
  elif c == 'R':
    code = 91
  elif c == 'G':
    code = 92
  elif c == 'B':
    code = 94
  elif c == 'C':
    code = 96
  elif c == 'M':
    code = 95
  elif c == 'Y':
    code = 93
  else:
    return ''
  return "\033[%dm" % code

def render(data):
  if not data.startswith(b"Art-Net"):
    return "junk"
  universe_lo = data[14]
  universe_hi = data[15]
  universe = universe_hi * 256 + universe_lo
  len_hi = data[16]
  len_lo = data[17]
  dmx_len = len_hi * 256 + len_lo
  num_pixels = dmx_len // 3
  colors = []
  last_color = None
  for i in range(num_pixels):
    r = data[18 + i * 3 + 0]
    g = data[18 + i * 3 + 1]
    b = data[18 + i * 3 + 2]
    color = get_color(r,g,b)
    if color == last_color:
      colors[-1][1] += 1
    else:
      last_color = color
      colors.append([color, 1])
  rv = str(universe) + " "
  for color, count in colors:
    rv += ansi_from_color(color)
    if count <= 5:
      rv += color * count
    else:
      rv += f"({color}x{count})"
    rv += "\033[0m" # Reset to white
  return rv

while True:
  data, sender = sock.recvfrom(BUFSIZE)
  sender_ip, sender_port = sender
  result = render(data)
  print(f"{sender_ip}: {result}")

