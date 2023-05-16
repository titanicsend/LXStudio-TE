#! /usr/bin/env python

import asyncio
import csv
import importlib
import os

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
            output_list = row[6]
            outputs.extend(output_list.split('/'))
    ips = set()
    for output in outputs:
        if '?' in output:
            continue
        if output.startswith('x'):
            output = output[1:]
        ip = output.split('#')[0]
        ips.add((ip, ip))  # TODO: Label with backpack names
    return sorted(list(ips))

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


async def main():
    config_dir = find_config_dir()
    ips = read_controllers(config_dir)
    results = await check_pings(ips)

    timed_out_ips = []
    reachable_ips = []

    for (ip, ip_label), response_time in results:
        if response_time is None:
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

