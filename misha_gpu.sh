#!/usr/bin/env bash

cd /Users/mishafradin/Desktop/LXStudio-TE/te-app && java -XstartOnFirstThread -Djava.awt.headless=true -Dgpu -jar ./target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution=1920x1080 ./Projects/BM2024_TE_Misha.lxp 
