import sys
import numpy as np
import cv2
import NDIlib as ndi
from multiprocessing import shared_memory

def create_recv_instance(source, color_format=ndi.RECV_COLOR_FORMAT_BGRX_BGRA):
    recv_create_struct = ndi.RecvCreateV3()
    recv_create_struct.color_format = color_format
    recv_instance = ndi.recv_create_v3(recv_create_struct)
    ndi.recv_connect(recv_instance, source)
    return recv_instance

def main():
    if not ndi.initialize():
        print("Cannot run NDI.")
        return 0

    ndi_find = ndi.find_create_v2()
    if ndi_find is None:
        print("Cannot create NDI find instance.")
        return 0

    # Wait for at least two sources
    sources = []
    while len(sources) < 2:
        print('Looking for sources ...')
        ndi.find_wait_for_sources(ndi_find, 1000)
        sources = ndi.find_get_current_sources(ndi_find)

        if len(sources) < 2:
            print("Waiting for at least two NDI sources...")
    
    ndi_recv1 = create_recv_instance(sources[0])
    ndi_recv2 = create_recv_instance(sources[1])
    
    if ndi_recv1 is None or ndi_recv2 is None:
        print("Cannot create NDI recv instances.")
        return 0

    ndi.find_destroy(ndi_find)

    # Assuming both sources have the same resolution
    frame_width = 640
    frame_height = 360
    frame_channels = 4  # RGBA has 4 channels
    shm_size = frame_width * frame_height * frame_channels

    # Create shared memory for both sources
    shm1 = shared_memory.SharedMemory(name='shm_test_1', create=True, size=shm_size)
    shm2 = shared_memory.SharedMemory(name='shm_test_2', create=True, size=shm_size)

    try:
        while True:
            for i, ndi_recv in enumerate([ndi_recv1, ndi_recv2], start=1):
                t, v, a, _ = ndi.recv_capture_v2(ndi_recv, 5000)

                if t == ndi.FRAME_TYPE_VIDEO:
                    print(f'Video data received from source {i} ({v.xres}x{v.yres}).')
                    # Convert video frame to a NumPy array
                    frame = np.frombuffer(v.data, dtype=np.uint8).reshape((v.yres, v.xres, frame_channels))

                    # Show video frame using OpenCV
                    cv2.imshow(f'NDI Video Source {i}', frame)
                    if cv2.waitKey(1) & 0xFF == ord('q'):
                        break

                    # Write the frame to the shared memory for the source
                    shm = shm1 if i == 1 else shm2
                    np.copyto(np.ndarray((frame_height, frame_width, frame_channels), dtype=np.uint8, buffer=shm.buf), frame)

                    # Free the video frame after we are done with it
                    ndi.recv_free_video_v2(ndi_recv, v)

                elif t == ndi.FRAME_TYPE_AUDIO:
                    print(f'Audio data received from source {i} ({a.no_samples} samples).')
                    ndi.recv_free_audio_v2(ndi_recv, a)

    finally:
        # Cleanup
        ndi.recv_destroy(ndi_recv1)
        ndi.recv_destroy(ndi_recv2)
        shm1.close()
        shm1.unlink()
        shm2.close()
        shm2.unlink()
        ndi.destroy()

    return 0

if __name__ == "__main__":
    sys.exit(main())