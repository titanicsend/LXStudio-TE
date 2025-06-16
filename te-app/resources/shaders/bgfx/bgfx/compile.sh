#! /bin/bash
echo Fragment shader for Metal: $1.sc
./shaderc -f $1.sc -o $1.bin --varyingdef varying.def.sc --platform osx -p metal --type fragment
if [ $? -ne 0 ]; then exit 1; fi
echo  
echo Vertex shader for Metal: $2.sc
./shaderc -f $2.sc -o $2.bin --varyingdef varying.def.sc --platform osx -p metal --type vertex
if [ $? -ne 0 ]; then exit 1; fi
echo
echo Moving binary files to shaders/metal folder
mv -f $1.bin ../metal
mv -f $2.bin ../metal

echo Fragment shader for OpenGL: $1.sc
./shaderc -f $1.sc -o $1.bin --varyingdef varying.def.sc --platform linux -p 410 --type fragment
if [ $? -ne 0 ]; then exit 1; fi
echo  
echo Vertex shader for OpenGL: $2.sc
./shaderc -f $2.sc -o $2.bin --varyingdef varying.def.sc --platform linux -p 410 --type vertex
if [ $? -ne 0 ]; then exit 1; fi
echo
echo Moving binary files to shaders/glsl folder
mv -f $1.bin ../glsl
mv -f $2.bin ../glsl
