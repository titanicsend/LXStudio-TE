#! /bin/sh

LIGHTING_LAPTOP=10.1.1.10

PING_TARGET=${LIGHTING_LAPTOP?}

if ping -q -t 5 -c 3 ${PING_TARGET?}; then
  echo "$PING_TARGET is reachable; launching AutoVJ"
  LXP=Projects/BM_2023.lxp
else
  echo "$PING_TARGET is not reachable; launching Driving Mode"
  LXP=Projects/driving.lxp
fi

java -XstartOnFirstThread -Djava.awt.headless=true -jar target/LXStudio-TE-0.2.2-SNAPSHOT-jar-with-dependencies.jar vehicle ${LXP?} 
