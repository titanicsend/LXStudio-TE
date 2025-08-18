#! /bin/sh

# We are assuming that the parent process has already set the correct
# working directory, ie:
#     cd /Users/te/src/code/LXStudio-TE;
# as should be in the automator app.

SCRIPT_DIR="$( cd "$( dirname "$(readlink -f "$0")" )" &> /dev/null && pwd )"

APP_MODULE_DIR="${SCRIPT_DIR}/.."

MODULE_NAME="te-app"
# ensure we run this command from the right directory
pushd $APP_MODULE_DIR
# Get Maven path first
MVN_PATH=$(which mvn 2>/dev/null)

if [ -z "$MVN_PATH" ]; then
  # Check under Apple M1 homebrew path if not found
  if [ -f "/opt/homebrew/bin/mvn" ]; then
    MVN_PATH="/opt/homebrew/bin/mvn"
  # Check under older intel homebrew path if not found
  elif [ -f "/usr/local/bin/mvn" ]; then
    MVN_PATH="/usr/local/bin/mvn"
  else
    echo "mvn not found"
    exit 1
  fi
fi

# Get version using the found mvn path
MODULE_VERSION=$($MVN_PATH -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)

LXP="${APP_MODULE_DIR}/Projects/BM2024_Pacman.lxp"
JAR_FILE="${APP_MODULE_DIR}/target/${MODULE_NAME}-${MODULE_VERSION}-jar-with-dependencies.jar"

# Burning man: Activate driving safety pattern if FOH is not up
#LIGHTING_LAPTOP=10.1.1.10
#
#PING_TARGET=${LIGHTING_LAPTOP?}
#
#if ping -q -t 5 -c 3 ${PING_TARGET?}; then
#  echo "$PING_TARGET is reachable; launching AutoVJ"
#  LXP=Projects/Oct_21.lxp
#else
#  echo "$PING_TARGET is not reachable; launching Driving Mode"
#  LXP=Projects/driving.lxp
#fi



# Build the compiled target jar if it doesn't already exist.

# Check if the jar file already exists
if [ -f "$JAR_FILE" ]; then
    echo "Jar file already exists, skipping 'mvn package'."
else
    # MVN_PATH is already set above, no need to find it again

    osascript -e 'display dialog "Using mvn to build a clean '"$JAR_FILE"'\n\nThis can take a couple minutes. This dialog will close automatically when complete." buttons {"Dismiss now"} giving up after 3600' &
    DIALOG_PID=$!
    $MVN_PATH clean package &
    COMMAND_PID=$!
    wait $COMMAND_PID
    kill $DIALOG_PID
fi


# Start Chromatik


java \
  -XstartOnFirstThread \
  -Djava.awt.headless=true \
  -jar $JAR_FILE \
  ${LXP?};
