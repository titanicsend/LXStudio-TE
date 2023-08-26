import psutil
import re
import os
import time
import subprocess
import platform

POLLING_WAIT_TIME_SECS = 5  # 120
MEMORY_GB_LIMIT = 15
SAFETY_FACTOR = 0.85


def get_memory_safe_limit():
    return MEMORY_GB_LIMIT * SAFETY_FACTOR

def get_mac_osx_pagesize():
#     CMD_PAGESIZE = "getconf PAGESIZE"
#     process = subprocess.Popen(CMD_PAGESIZE, shell=True,
#                                stdout=subprocess.PIPE,
#                                stderr=subprocess.PIPE)
#     out, err = process.communicate()
#
#     if process.returncode == 0:
#         return int(out.decode().strip())
#     else:
#         # something went wrong, let's guess
#         processor_string = platform.processor()
#         if 'arm' in processor_string:
#             return 16384
#         return 4096
    return os.sysconf('SC_PAGE_SIZE')

def is_process_sk(process):
    """
    Returns True if this is the SK process, False otherwise

    Should be two processes:
        ['ShowKontrol', 'ShowKontrolRemoteServer']
    """
    process_name = p.name().lower()
#     print(f"Is {process_name} like 'showkontrol' ? {'showkontrol' in p.name().lower()}")
    return "showkontrol" in process_name

def kill_process(process):
    print(f"Killing process: {process}")
    process.kill()

def restart_sk():
    # TODO: need to run the application for SK to boot it up
    # need this to be a separate process that isn't dependent on
    # this script, ideally
    print("Restarting SK...")

def get_memory_stats(verbose=False):
    # adapted from: https://apple.stackexchange.com/a/4296
    # Get process info
    ps = subprocess.Popen(['ps', '-caxm', '-orss,comm'], stdout=subprocess.PIPE).communicate()[0].decode()
    vm = subprocess.Popen(['vm_stat'], stdout=subprocess.PIPE).communicate()[0].decode()

    # Iterate processes
    processLines = ps.split('\n')
    sep = re.compile('[\s]+')
    rssTotal = 0 # kB
    for row in range(1,len(processLines)):
        rowText = processLines[row].strip()
        rowElements = sep.split(rowText)
        try:
            rss = float(rowElements[0]) * 1024
        except:
            rss = 0 # ignore...
        rssTotal += rss

    # Process vm_stat
    vmLines = vm.split('\n')
    sep = re.compile(':[\s]+')
    vmStats = {}
    for row in range(1, len(vmLines) - 2):
        rowText = vmLines[row].strip()
        rowElements = sep.split(rowText)
        vmStats[(rowElements[0])] = int(rowElements[1].strip('\.')) * get_mac_osx_pagesize()

    bytes_in_gb = 1024 * 1024
    wired = vmStats["Pages wired down"] / bytes_in_gb
    active = vmStats["Pages active"] / bytes_in_gb
    inactive = vmStats["Pages inactive"] / bytes_in_gb
    free = vmStats["Pages free"] / bytes_in_gb
    real_total = rssTotal / bytes_in_gb

    if verbose:
        print('Wired Memory:\t\t%d MB' % wired)
        print('Active Memory:\t\t%d MB' % active)
        print('Inactive Memory:\t%d MB' % inactive)
        print('Free Memory:\t\t%d MB' % free)
        print('Real Mem Total (ps):\t%.3f MB' % real_total)
    return {
        "wired": wired,
        "active": active,
        "inactive": inactive,
        "free": free,
        "real": real_total,
    }

while True:
    mem = get_memory_stats()
    if mem['real'] > get_memory_safe_limit():
        # memory usage is dangerously high, we need to restart SK
        print(f"Memory usage is high at: {mem['real']}! Need to kill SK")

        # get SK processes
        sk_processes = []
        pids = psutil.pids()
        for i, pid in enumerate(pids):
            try:
                p = psutil.Process(pid)
                if is_process_sk(p):
                    sk_processes.append(p)
            except psutil.NoSuchProcess:
                continue

        # kill all SK processes
        for sk_process in sk_processes:
            try:
                kill_process(sk_process)
            except psutil.NoSuchProcess:
                print(f"Could not kill process: {sk_process}, was already dead")
                continue

        # TODO: restart showkontrol process
        #restart_sk()
        #new_sk_proc = subprocess.POpen(["open", ""])


    # wait for a while
    time.sleep(POLLING_WAIT_TIME_SECS)

# debugging:
# print([psutil.Process(pid).name() for pid in psutil.pids() if "showkontrol" in psutil.Process(pid).name().lower()])