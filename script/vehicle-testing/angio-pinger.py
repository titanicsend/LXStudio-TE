#! /usr/bin/env python

import asyncio
import csv
import importlib
import os

from time import sleep
from tecfg import *

av = importlib.import_module("angio-validator")
from ping3 import ping

async def ping_ip(ip):
    try:
      response_time = await asyncio.to_thread(ping, ip[0], timeout=3)
      return ip, response_time
    except OSError:  # Happens if host replies that it's down
      return ip, None

async def check_pings(ips):
    results = await asyncio.gather(*(ping_ip(ip) for ip in ips))
    return results

def read_controllers(config_dir):
    outputs = []
    with open(config_dir + '/edges.txt') as tsv_file:
        tsv_reader = csv.reader(tsv_file, delimiter="\t")
        for row in tsv_reader:
            output = row[3]
            outputs.append(output)
    with open(config_dir + '/panels.txt') as tsv_file:
        tsv_reader = csv.reader(tsv_file, delimiter="\t")
        for row in tsv_reader:
            output_list = row[7]
            outputs.extend(output_list.split('/'))
    ips = set()
    for output in outputs:
        if '?' in output:
            continue
        disabled = False
        if output.startswith('x'):
            disabled = True
            output = output[1:]
        ip = output.split('#')[0]
        octets = ip.split('.')
        assert len(octets) == 4
        ten, seven, module, n = octets
        assert ten == '10'
        assert seven == '7'
        label = module + '-' + n
        if disabled:
            label += "-disabled"
        else:
            ips.add((ip, label))
    return sorted(list(ips))

async def main():
    config_dir = find_config_dir()
    ip_queue = read_controllers(config_dir)
    final_results = []
    for i in range(3):
      if len(ip_queue) > 0:
        sleep(1)
      results = await check_pings(ip_queue)
      ip_queue = []
      for tup, response_time in results:
        ip, ip_label = tup
        if i < 2 and response_time in (None, False):
          print(f"{ip_label} was unreachable; will try again")
          ip_queue.append(tup)
        else:
          final_results.append((tup, response_time))

    timed_out_ips = []
    reachable_ips = []

    for (ip, ip_label), response_time in final_results:
        if response_time in (None, False):
            timed_out_ips.append(ip_label)
        else:
            reachable_ips.append((ip, ip_label))

    grouped_results = av.check_configs(reachable_ips)

    RED=31
    GREEN=32
    YELLOW=33
    MAGENTA=35
    CYAN=36

    categories = [
        ("up-to-date controllers", grouped_results['up-to-date'], GREEN),
        ("stale-firmware controllers", grouped_results['stale'], YELLOW),
        ("misconfigured controllers", grouped_results['misconfig'], YELLOW),
        ("reachable controllers that nonetheless couldn't be checked", grouped_results['error'], RED),
        ("unreachable controllers", timed_out_ips, RED),
    ]

    for category_name, ips, color_number in categories:
        if len(ips) > 0:
            print(f"\033[{color_number}m{len(ips)} {category_name}: {', '.join(ips)}\033[0m")

if __name__ == "__main__":
    asyncio.run(main())

