import psutil
import time


POLLING_WAIT_TIME_SECS = 5
SK_PROCESS_NAME = "xxx"
MEMORY_GB_LIMIT = 15

def is_process_sk(process):
    """
    Returns True if this is the SK process, False otherwise
    """
    return "rekordbox" in p.name().lower()

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
        return gb_using > MEMORY_GB_LIMIT
    except psutil.AccessDenied:
        return False

def kill_process(process):
    # TODO: need to kill a process given a PID
    pass

def restart_sk():
    # TODO: need to run the application for SK to boot it up
    # need this to be a separate process that isn't dependent on
    # this script, ideally
    pass

while True:
    pids = psutil.pids()
    for i, pid in enumerate(pids):
        p = psutil.Process(pid)
        if is_process_sk(p):
            should_kill = is_process_taking_too_much_memory(p)
            if should_kill:
                kill_process(process)
                restart_sk()

    time.sleep(POLLING_WAIT_TIME_SECS)
