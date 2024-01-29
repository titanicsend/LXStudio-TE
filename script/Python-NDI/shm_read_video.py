import cv2
from multiprocessing import shared_memory
import numpy as np

def display_shared_memory_video(shm_name, frame_width, frame_height, frame_channels, frame_dtype):
    """
    Display video from shared memory using OpenCV.

    :param shm_name: The name of the shared memory block.
    :param frame_width: The width of the video frame.
    :param frame_height: The height of the video frame.
    :param frame_channels: The number of channels in the video frame.
    :param frame_dtype: The data type of the video frames.
    """
    # Attach to the existing shared memory block
    try:
        shm = shared_memory.SharedMemory(name=shm_name)
    except FileNotFoundError:
        print(f"No shared memory block with name {shm_name} found.")
        return

    # Create a NumPy array backed by shared memory
    frame_buffer = np.ndarray((frame_height, frame_width, frame_channels), dtype=frame_dtype, buffer=shm.buf)

    # Main loop to display the video
    try:
        while True:
            # Display the frame using OpenCV
            cv2.imshow('Shared Memory Video', frame_buffer)

            # Wait for 1 ms and check if the 'q' key is pressed to exit the loop
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
    finally:
        # Cleanup OpenCV window
        cv2.destroyAllWindows()

        # Detach from the shared memory
        shm.close()

def main():
    shared_memory_name = 'shm_test'
    frame_width = 640
    frame_height = 360
    frame_channels = 4  # Assuming a color video with 3 channels (RGB)
    frame_dtype = np.uint8  # Assuming the data type is unsigned byte

    # Call the function to display the video
    display_shared_memory_video(shared_memory_name, frame_width, frame_height, frame_channels, frame_dtype)

if __name__ == '__main__':
    main()
