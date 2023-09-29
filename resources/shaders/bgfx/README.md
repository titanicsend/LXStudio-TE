Tools and files necessary to compile custom BGFX shaders for Titanic's End.

Notes:
- Compiler, source code and includes are in the bgfx subdirectory
- The shader compiler - shaderc for Mac, shaderc.exe for Windows - is built for v3.33 of LWJGL
- Shaders for Windows can only be compiled on a Windows machine. 
- To compile a shader for all platforms on Windows, use the include .bat command "compile frag_shader_name vertex_shader_name"
- To compile a shader for Metal and OpenGL on a MAC... that's a WIP.
- You may need to give shaderc execute permission the first time you use it (chmod -R +x shaderc)
- We currently support DirectX11, OpenGL and Metal renderers.
- OpenGL is untested because it isn't yet supported in Chromatik.

- Compiled shader binaries must be placed in the platform-appropriate subdirectory
