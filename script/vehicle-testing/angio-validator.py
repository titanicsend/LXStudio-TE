#! /usr/bin/env python

import asyncio
import ipaddress
import json
import sys
import websockets

from concurrent.futures import ThreadPoolExecutor
from json.decoder import JSONDecodeError

async def send_request_and_get_response(ip, request):
    try:
        async with websockets.connect(f'ws://{ip}:81/websocket', timeout=5) as websocket:
            await websocket.send(request)
            response = await websocket.recv()
            return response
    except Exception:
        return None

def check_config(possibly_labeled_ip, debug=False):
    if isinstance(possibly_labeled_ip, tuple):
      ip, label = possibly_labeled_ip
    else:
      ip = possibly_labeled_ip
      label = possibly_labeled_ip

    power_array = [{'enabled': True}, {'enabled': True}, {'enabled': True}, {'enabled': True}]

    requests = [("info", dict(version="v0.8.0")),
                ("globals", dict(brightness=190, target_fps=100)),
                ("netstate", dict(ethernet=dict(subnet="255.0.0.0", gateway="10.0.0.1"))),
                ("leds", dict(chipset="SK9822", gamma=[[2.20, 0.80, 0.00],
                                                       [2.50, 0.70, 0.00],
                                                       [2.50, 0.60, 0.00]])),
                ("network", dict(ethernet=dict(subnet="255.0.0.0", gateway="10.0.0.1"),
                                 wifi=dict(ssid=""))),
                ("power", dict(external=power_array)),
                #("artnet", 'print')
               ]
    responses = []
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    
    for key, expected_response in requests:
        request = {"cmd": "get", "key": key}
        response = loop.run_until_complete(send_request_and_get_response(ip, str(request)))
        if expected_response == 'print':
            if debug: print(f"{label} {key}: {repr(response)}")
        elif response is None:
            if debug: print(f"{label} {key}: Failed to return response")
            return 'error'
        else:
            try:
                response_dict = json.loads(response)
            except JSONDecodeError as e:
                if debug: print(f"{label} {key}: Failed to decode JSON: {response}\nError details: {e}")
                return 'error'
            if "data" not in response_dict:
                if debug: print(f"{label} {key}: Got data-less response: {response_dict}\n")
                return 'error'
            data = response_dict["data"]
            for rk, rv in expected_response.items():
                if rk not in data or data[rk] != rv:
                    if isinstance(rv, dict) and all(item in data[rk].items() for item in rv.items()): 
                        continue
                    elif key == "info" and rk == "version":
                        if debug: print(f"{label} running version {data[rk]} instead of {rv}")
                        return 'stale'
                    else:
                        if debug: print(f"{label} {key}: Expected {rk}={rv} in {data}")
                        return 'misconfig'

    loop.close()
    return 'up-to-date'

def check_configs(possibly_labeled_ips, debug=False):
    with ThreadPoolExecutor() as executor:
        results = [executor.submit(check_config, pip, debug) for pip in possibly_labeled_ips]
        grouped_results = {'up-to-date': [], 'stale': [], 'misconfig': [], 'error': []}
        for result, pip in zip(results, possibly_labeled_ips):
            if isinstance(pip, tuple):
              ip, label = pip
            else:
              label = pip
            grouped_results[result.result()].append(label)
        return grouped_results

def main(argv):
    ips = argv[1:]

    if not ips:
        print("Need one or more IPs")
        sys.exit(1)

    if len(ips) != len(set(ips)):
        print("There were some dupes in there")
        sys.exit(1)

    for ip in ips:
        try:
            ipaddress.ip_address(ip)
        except ValueError:
            print(f"Invalid IP address: {ip}")
            sys.exit(1)

    results = check_configs(ips, debug=True)
    for k, v in results.items():
        if not v: continue
        print(f"{k}: {' '.join(v)}")

if __name__ == "__main__":
    main(sys.argv)
