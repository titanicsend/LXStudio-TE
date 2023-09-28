@echo off
rem
rem Compile TE custom BGFX shaders for supported platforms
echo Fragment shader for dx11: %1.sc
shaderc -f %1.sc -o %1.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type fragment 
echo  
echo Vertex shader for dx11: %2.sc
shaderc -f %2.sc -o %2.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type vertex 