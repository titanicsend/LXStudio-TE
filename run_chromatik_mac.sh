#!/bin/bash

# Ensure we're in the correct directory
cd "$(dirname "$0")"

# Build if needed
mvn clean package

# Run Chromatik with appropriate options for macOS
# Note: You can modify the last argument to change the project file
java -XstartOnFirstThread \
     -jar target/LXStudio-TE-*-SNAPSHOT-jar-with-dependencies.jar \
     vehicle Vehicle.lxp 