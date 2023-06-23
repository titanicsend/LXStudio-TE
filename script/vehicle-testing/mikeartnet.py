#! /usr/bin/env python3

from time import sleep

import socket

def pack_le(w):
  hi = w // 256
  lo = w % 256
  return bytes([lo, hi])

def make_packet(universe, dmx):
  global SEQ
  packet = b'Art-Net'
  packet += bytes([0])
  packet += pack_le(0x5000)
  packet += bytes([0, 14, 0, 0])
  packet += pack_le(universe)
  dlen = len(dmx)
  dhi = dlen // 256
  dlo = dlen % 256
  packet += bytes([dhi, dlo])
  packet += bytes(dmx)
  packet += bytes([0])
  return packet

def send_packet(ip, universe, dmx):
  packet = make_packet(universe, dmx)
  sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
  try:
    sock.sendto(packet, (ip, 6454))
    return True
  except OSError:
    print("Couldn't send to " + ip)
    return False
  finally:
    sock.close()
