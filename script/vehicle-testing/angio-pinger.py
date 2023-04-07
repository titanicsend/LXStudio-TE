#! /usr/bin/env python

import asyncio
import csv
import random
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

def check_config(ip):
    # TODO: Actually return one of ['up-to-date', 'stale', 'misconfig', 'error']
    return 'error'

async def main():
    controllers_file = "controllers.tsv"
    ips = read_controllers(controllers_file)
    results = await check_pings(ips)
        
    timed_out_ips = []
    up_to_date_ips = []
    stale_ips = []
    misconfig_ips = []
    cfg_error_ips = []

    for (ip, ip_label), response_time in results:
        if response_time is None:
            timed_out_ips.append(ip_label)
        else:
            config_status = check_config(ip)
            if config_status == 'up-to-date':
                up_to_date_ips.append(ip_label)
            elif config_status == 'stale':
                stale_ips.append(ip_label)
            elif config_status == 'misconfig':
                misconfig_ips.append(ip_label)
            else:
                cfg_error_ips.append(ip_label)

    categories = [
        ("up-to-date controllers", up_to_date_ips),
        ("stale-firmware controllers", stale_ips),
        ("misconfigured controllers", misconfig_ips),
        ("reachable controllers that nonetheless couldn't be checked", cfg_error_ips),
        ("unreachable controllers", timed_out_ips),
    ]

    for category_name, ips in categories:
        if len(ips) > 0:
            print(f"{len(ips)} {category_name}: {', '.join(ips)}")

if __name__ == "__main__":
    asyncio.run(main())

