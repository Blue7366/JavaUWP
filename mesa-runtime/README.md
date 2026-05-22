# Mesa Runtime

Bundled Mesa UWP runtime DLLs for local builds live in this folder.

Expected files:
- libEGL.dll
- libGLESv2.dll
- opengl32.dll
- libgallium_wgl.dll
- libglapi.dll
- glu32.dll
- dxil.dll
- z-1.dll

If you need to test a different Mesa build, point `build.ps1` at another folder
with `-MesaRuntimeDir` or `MESA_UWP_DIR`.
