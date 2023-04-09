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

def read_controllers(file_name):
    ips = []
    with open(file_name) as tsv_file:
        tsv_reader = csv.reader(tsv_file, delimiter="\t")
        for row in tsv_reader:
            x = int(row[0])
            if len(row) > 9:
                ips.append((f"10.7.{x}.1", f"{x}-1"))
                ips.append((f"10.7.{x}.2", f"{x}-2"))
            else:
                ips.append((f"10.7.{x}.1", f"{x}"))
    return ips

def find_controllers_file():
    file_name = "controllers.tsv"
    path = os.getcwd()
    while True:
        if os.path.isfile(os.path.join(path, file_name)):
            return os.path.join(path, file_name)
        elif os.path.isfile(os.path.join(path, "resources", "vehicle", file_name)):
            return os.path.join(path, "resources", "vehicle", file_name)
        elif path == os.path.dirname(path):
            raise FileNotFoundError(f"{file_name} not found.")
        else:
            path = os.path.dirname(path)


async def main():
    controllers_file = find_controllers_file()
    ips = read_controllers(controllers_file)
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

