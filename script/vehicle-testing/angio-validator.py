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

expected_config = [
                ("globals", dict(brightness=190, target_fps=100)),
                ("leds", dict(chipset="SK9822", color_order='BGR',
                              length=500, speed=2000000,
                              gamma=[[2.20, 0.80, 0.00],
                                   [2.50, 0.70, 0.00],
                                   [2.50, 0.60, 0.00]])),
                ("network", "net"),
]

def set_config(ip):
    print("Attempting to configure " + ip)
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    for key, new_data in expected_config:
        request = {"cmd": "get", "key": key}
        request_str = str(request)
        response = loop.run_until_complete(send_request_and_get_response(ip, request_str))
        data = json.loads(response)["data"]
        #print("Old: %r" % data)
        if new_data == "net":
          data['wifi'] = dict(ssid='')
          data['ethernet']['subnet'] = '255.0.0.0'
          data['ethernet']['gateway'] = '10.0.0.1'
        else:
          data.update(new_data)
        #print("New: %r" % data)
        request = {"cmd": "set", "key": key, "data": data}
        request_str = json.dumps(request)
        response = loop.run_until_complete(send_request_and_get_response(ip, request_str))
        response_dict = json.loads(response)
        if response_dict["result"] != True:
            print("While attempting to configure %s via \n\n%s\n\ngot:\n\n%r\n" % 
                   (key, request_str, response_dict))
            break
    loop.close()

def get_currents(ip):
  loop = asyncio.new_event_loop()
  response = loop.run_until_complete(
               send_request_and_get_response(ip, str(dict(cmd="state", key="power")))
             )
  if response is None:
    return None
  try:
    response_dict = json.loads(response)
  except JSONDecodeError as e:
    print(f"Failed to decode JSON: {response}\nError details: {e}")
    return None
  
  ext = response_dict["data"]["external"]
  rv = []
  for d in ext:
    rv.append(d['current']/1000)
  return rv

def check_config(possibly_labeled_ip, debug=False):
    if isinstance(possibly_labeled_ip, tuple):
      ip, label = possibly_labeled_ip
    else:
      ip = possibly_labeled_ip
      label = possibly_labeled_ip

    power_array = [{'enabled': True}, {'enabled': True}, {'enabled': True}, {'enabled': True}]

    requests = [("info", dict(version="v0.9.2"))] + expected_config + [
                ("netstate", dict(ethernet=dict(subnet="255.0.0.0", gateway="10.0.0.1"))),
                ("power", dict(external=power_array)),
                ("state:power", 'power'),
                #("artnet", 'print')
               ]
    responses = []
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    
    for key, expected_response in requests:
        if expected_response == 'print' and not debug:
            continue
        if ":" in key:
            cmd, key = key.split(":")
        else:
            cmd = "get"
        request = {"cmd": cmd, "key": key}
        response = loop.run_until_complete(send_request_and_get_response(ip, str(request)))
        if expected_response == 'print':
            print(f"{label} {key}: {repr(response)}")
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
            if expected_response == "power":
              temp = response_dict["data"]["external"][0]["temp"] * 9 / 5 + 32
              if debug:
                  print("%s %s %d°F" % (ip, label, temp))
              if temp > 130:
                  print(label + " is HOT: %d°F" % (label, temp))
              for index in range(4):
                d = response_dict["data"]["external"][index]
                channel = index + 1
                volts = d["voltage"]
                amps = d["current"]/1000
                sublabel = label + " #%d" % channel
                if debug:
                    print("%s %.2fV %.2fA" % (sublabel, volts, amps))
                if volts < 4.8:
                    print("%s has low voltage: %.2fV" % (sublabel, volts))
                if volts > 5.3:
                    print("%s has high voltage: %.2fV" % (sublabel, volts))
                if amps > 11.0:
                    print("%s drawing high current: %.2fA" % (sublabel, amps))
              continue
            if expected_response == "net":
              if data["wifi"]["ssid"] != "":
                if debug: print(f"{label} has WIFI turned on")
                return 'misconfig'
              if data["ethernet"]["subnet"] != "255.0.0.0":
                if debug: print(f"{label} has bad subnet")
                return 'misconfig'
              if data["ethernet"]["gateway"] != "10.0.0.1":
                if debug: print(f"{label} has bad gateway")
                return 'misconfig'
            else:
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
    setting = False

    if not ips:
        print("Need one or more IPs")
        sys.exit(1)

    if ips[0] == '-s':
        setting = True
        ips.pop(0)

    if len(ips) != len(set(ips)):
        print("There were some dupes in there")
        sys.exit(1)

    for ip in ips:
        try:
            ipaddress.ip_address(ip)
        except ValueError:
            print(f"Invalid IP address: {ip}")
            sys.exit(1)

    if setting:
        for ip in ips:
            set_config(ip)  

    results = check_configs(ips, debug=True)
    for k, v in results.items():
        if not v: continue
        print(f"{k}: {' '.join(v)}")

if __name__ == "__main__":
    main(sys.argv)
