import sys
import numpy as np
import cv2
import NDIlib as ndi
from multiprocessing import shared_memory

def main():

    if not ndi.initialize():
        return 0

    ndi_find = ndi.find_create_v2()

    if ndi_find is None:
        return 0

    sources = []
    while not len(sources) > 0:
        print('Looking for sources ...')
        ndi.find_wait_for_sources(ndi_find, 1000)
        sources = ndi.find_get_current_sources(ndi_find)

    ndi_recv_create = ndi.RecvCreateV3()
    ndi_recv_create.color_format = ndi.RECV_COLOR_FORMAT_BGRX_BGRA

    ndi_recv = ndi.recv_create_v3(ndi_recv_create)

    if ndi_recv is None:
        return 0

    ndi.recv_connect(ndi_recv, sources[0])
    ndi.find_destroy(ndi_find)
    
    # Define shared memory block size for 640x360 RGBA image
    shared_memory_name = 'shm_test'
    frame_width = 640
    frame_height = 480
    frame_channels = 4  # RGBA has 4 channels
    shm_size = frame_width * frame_height * frame_channels
    shm = shared_memory.SharedMemory(name = shared_memory_name, create=True, size=shm_size)


    while True:
        t, v, a, _ = ndi.recv_capture_v2(ndi_recv, 5000)

        if t == ndi.FRAME_TYPE_NONE:
            print('No data received.')
            continue

        if t == ndi.FRAME_TYPE_VIDEO:
            print('Video data received (%dx%d).' % (v.xres, v.yres))
            # Convert video data to a NumPy array
            frame = np.frombuffer(v.data, dtype=np.uint8).reshape((v.yres, v.xres, frame_channels))

            # Show video frame using OpenCV
            cv2.imshow('NDI Video', frame)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

            # Write the frame to the shared memory block
            np.copyto(np.ndarray((frame_height, frame_width, frame_channels), dtype=np.uint8, buffer=shm.buf), frame)

            # Free the video frame after we are done with it
            ndi.recv_free_video_v2(ndi_recv, v)

        if t == ndi.FRAME_TYPE_AUDIO:
            print('Audio data received (%d samples).' % a.no_samples)
            ndi.recv_free_audio_v2(ndi_recv, a)
            continue

    ndi.recv_destroy(ndi_recv)

    ndi.destroy()
    return 0


if __name__ == "__main__":
    sys.exit(main())