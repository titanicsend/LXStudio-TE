@echo off
REM Run Titanic's End Dynamic Application
echo Starting Titanic's End Dynamic...

cd te-app
java -ea -Djava.awt.headless=true -cp "target/classes;../lib/*" heronarts.lx.studio.TEApp dynamic Projects/BM2024_TE.lxp

echo Application closed.
pause 