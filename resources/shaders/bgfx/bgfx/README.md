Tools and files necessary to compile custom BGFX shaders for Titanic's End.

Notes:
- The shader compiler - shaderc - is built for v3.33 of LWJGL
- We currently support DirectX11, OpenGL and Metal renderers.
- OpenGL is untested because it isn't yet supported in Chromatik.
- Shaders for Windows can only be compiled on a Windows machine. 
- compiled shader binaries must be placed in the platform-appropriate subdirectory
- makefile in progress doesn't quite work yet