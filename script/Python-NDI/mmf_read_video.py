import cv2
import numpy as np
import mmap

def display_memory_mapped_file_video(mmf_filename, frame_width, frame_height, frame_channels, frame_dtype):
    """
    Display video from a memory-mapped file using OpenCV.

    :param mmf_filename: The filename of the memory-mapped file.
    :param frame_width: The width of the video frame.
    :param frame_height: The height of the video frame.
    :param frame_channels: The number of channels in the video frame.
    :param frame_dtype: The data type of the video frames.
    """

    # Calculate the size of each frame in bytes
    frame_size = frame_width * frame_height * frame_channels * np.dtype(frame_dtype).itemsize

    # Open the memory-mapped file
    with open(mmf_filename, "r+b") as f:
        mmapped_file = mmap.mmap(f.fileno(), length=frame_size, access=mmap.ACCESS_READ)

        # Main loop to display the video
        try:
            while True:
                # Read a frame from the memory-mapped file
                mmapped_file.seek(0)
                frame_data = mmapped_file.read(frame_size)

                # Create a NumPy array with the frame data
                frame_buffer = np.frombuffer(frame_data, dtype=frame_dtype).reshape((frame_height, frame_width, frame_channels))

                # Display the frame using OpenCV
                cv2.imshow('Memory Mapped File Video', frame_buffer)

                # Wait for 1 ms and check if the 'q' key is pressed to exit the loop
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    break
        finally:
            # Cleanup OpenCV window
            cv2.destroyAllWindows()
            # Close the memory-mapped file
            mmapped_file.close()

def main():
    memory_mapped_filename = 'ndi_video_frame.bin'
    frame_width = 640
    frame_height = 360
    frame_channels = 4  # Assuming a color video with 4 channels (RGBA)
    frame_dtype = np.uint8  # Assuming the data type is unsigned byte

    # Call the function to display the video
    display_memory_mapped_file_video(memory_mapped_filename, frame_width, frame_height, frame_channels, frame_dtype)

if __name__ == '__main__':
    main()