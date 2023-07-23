import psutil
import time


POLLING_WAIT_TIME_SECS = 5
SK_PROCESS_NAME = "xxx"
MEMORY_GB_LIMIT = 15

def is_process_sk(process):
    """
    Returns True if this is the SK process, False otherwise

    Should be two processes:
        ['ShowKontrol', 'ShowKontrolRemoteServer']
    """
    process_name = p.name().lower()
#     print(f"Is {process_name} like 'showkontrol' ? {'showkontrol' in p.name().lower()}")
    return "showkontrol" in process_name

def is_process_taking_too_much_memory(process):
    """
    Uses psutil's `process.memory_info()` to check for memory usage
    https://psutil.readthedocs.io/en/latest/#psutil.Process.memory_info

    I have found this number to be a little low. For example, I was seeing
    a process report as 1.81 GB in ActivityMonitor, but report 780 GB using psutil,
    so we may want to have a fudge factor or add another form of memory to it. If
    we can't get the field we may need to run as "sudo" to get it.
    """
    try:
        gb_using = p.memory_info().rss / (1024.0 ** 3)
        print(f"Process '{p.name()} (pid={p.pid})' is using ~{gb_using} GB of memory")
        return True
#         return gb_using > MEMORY_GB_LIMIT
    except psutil.AccessDenied:
        return False

def kill_process(process):
    # TODO: need to kill a process given a PID
    print("Killing process")

def restart_sk():
    # TODO: need to run the application for SK to boot it up
    # need this to be a separate process that isn't dependent on
    # this script, ideally
    print("Restarting SK...")

while True:
    pids = psutil.pids()
    print(f"Found {len(pids)} pids")
    for i, pid in enumerate(pids):
        try:
            p = psutil.Process(pid)
            if is_process_sk(p):
                should_kill = is_process_taking_too_much_memory(p)
                if should_kill:
                    kill_process(p)
                    restart_sk()
        except psutil.NoSuchProcess:
            continue

    time.sleep(POLLING_WAIT_TIME_SECS)

print([psutil.Process(pid).name() for pid in psutil.pids() if "showkontrol" in psutil.Process(pid).name().lower()])
#top -l 1 | grep -i "showkontrol" | awk '{print "NAME="$2 " MEM="$9 "\tRPRVT="$10}'
