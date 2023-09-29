@echo off
rem
rem Windows batch file to compile TE custom BGFX shaders for supported platforms.
rem Note that dx11 shaders can only be compiled on Windows.  Metal and OpenGL can be 
rem compiled on any supported platform.
rem syntax is: compile fragment_shader_name vertex_shader_name
rem file extensions should not be specified on the command line, but should be BGFX shaders -- ".sc"
echo Fragment shader for dx11: %1.sc
shaderc -f %1.sc -o %1.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type fragment 
if ERRORLEVEL 1 exit /B
echo  
echo Vertex shader for dx11: %2.sc
shaderc -f %2.sc -o %2.bin --varyingdef varying.def.sc -p s_5_0 -O 3 --platform windows --type vertex 
if ERRORLEVEL 1 exit /B
echo
echo Moving binary files to shaders/dx11 folder
move /Y %1.bin ../dx11
move /Y %2.bin ../dx11

rem
rem Compile TE custom BGFX shaders for Metal running on OSX
echo Fragment shader for Metal: %1.sc
shaderc -f %1.sc -o %1.bin --varyingdef varying.def.sc --platform osx -p metal --type fragment 
if ERRORLEVEL 1 exit /B
echo  
echo Vertex shader for Metal: %2.sc
shaderc -f %2.sc -o %2.bin --varyingdef varying.def.sc --platform osx -p metal --type vertex 
if ERRORLEVEL 1 exit /B
echo
echo Moving binary files to shaders/metal folder
move /Y %1.bin ../metal
move /Y %2.bin ../metal

rem
rem Compile TE custom BGFX shaders for OpenGL
echo Fragment shader for OpenGL: %1.sc
shaderc -f %1.sc -o %1.bin --varyingdef varying.def.sc --platform linux -p 410 --type fragment
if ERRORLEVEL 1 exit /B
echo  
echo Vertex shader for OpenGL: %2.sc
shaderc -f %2.sc -o %2.bin --varyingdef varying.def.sc --platform linux -p 410 --type vertex 
if ERRORLEVEL 1 exit /B
echo
echo Moving binary files to shaders/glsl folder
move /Y %1.bin ../glsl
move /Y %2.bin ../glsl

