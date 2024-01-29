# NDI Test Package
This set of scripts are aiming to bring NDI source to LX Studio/Chromatik.

# Set up

 - Make sure you have Python 3.10 install on your machine
 - Install Python virtual env
    - https://virtualenv.pypa.io/en/latest/installation.html
 - Install the virtual environment wrapper to manage the Python environment.
    - https://virtualenvwrapper.readthedocs.io/en/latest/install.html
 - Create a virtual environment and call it ndi
    - mkvirtualenv --python=/path/to/python3.10 ndi
 - Activate the NDI virtual env
    - workon ndi
 - Install the requirements
    - pip install -r requirements.txt
 - Run the scripts and enjoy!

# ndi_to_mmd.py : NDI to Memeory Mapped File
This scripts find the first NDI source on your local network and starts writing the data to a memory mapped file.

The memory mapped file is called 'ndi_video_frame.bin' and can then be read from Chromatik.

Example usage: `python ndi_to_mmf.py`