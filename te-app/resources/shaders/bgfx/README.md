### Tools and files necessary to compile custom BGFX shaders for Titanic's End.

#### Notes:
- Compiler, build scripts, source code and includes are in the resources/shaders/bgfx/bgfx subdirectory. The shader
compiler - **shaderc** for Mac, **shaderc.exe** for Windows - is built for v3.33 of LWJGL, which is
compatible with the current version of Chromatik as of September 2023.
- We currently support custom shaders for DirectX11, OpenGL and Metal renderers.  OpenGL is untested
because it's currently unsupported by Chromatik.
- Shaders for Windows can only be compiled on a Windows machine. On OSX, **compile.sh** only builds for
Metal and OpenGL.
- On OSX, you may need to give **compile.sh** and **shaderc** and **compile_all.sh** execute permission the first time you
  use run them:
  - ```chmod +x shaderc```
  - ```chmod +x compile.sh```
  - ```chmod +x compile_all.sh```
- To compile a shader for all supported platforms use the "compile" script - **compile.bat** on
Windows, **compile.sh** on OSX.  In either case, the syntax is:
```compile fragment_shader_name vertex_shader_name```, where file names are given without extension.
  - On OSX you can just run ```compile_all.sh```
- If you compile shaders manually (outside the compile script), you are responsible for placing them in
the proper platform subdirectory.
- If we get more than a couple of custom shaders, we'll need a less labor-intensive build method. For now,
there's only the one, and it should not require frequent recompilation.
