@echo off
rem
rem Windows batch file to compile TE custom BGFX shaders for DirectX 11 running on Windows
rem Note that dx11 shaders can only be compiled on Windows.  Metal and OpenGL can be 
rem compiled on any supported platform.
rem syntax is: compile fragment_shader_name vertex_shader_name
rem file extensions should not be specified on the command line, but should be BGFX shaders -- ".sc"
echo Fragment shader for dx11: %1.sc
shaderc -f %1.sc -o %1.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type fragment 
echo  
echo Vertex shader for dx11: %2.sc
shaderc -f %2.sc -o %2.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type vertex 
echo
echo Moving binary files to shaders/dx11 folder
move /Y %1.bin ../dx11
move /Y %2.bin ../dx11