# Patching Notes

The project relies on a few targeted compatibility changes because desktop Java,
LWJGL, Fabric, and Minecraft assume APIs that are not available in the Xbox UWP
sandbox.

## Fabric Loader Patch

`patch/LoaderUtil.java` is a patched replacement for Fabric Loader's
`net.fabricmc.loader.impl.util.LoaderUtil`.

The important change is in `normalizeExistingPath0`:

- Desktop Fabric uses `Path.toRealPath()`.
- On Xbox Developer Mode, that path calls into Windows behavior that relies on
  `GetFinalPathNameByHandle`.
- That call is blocked or unreliable in the packaged sandbox.
- The patch uses `toAbsolutePath().normalize()` instead.

Apply it with:

```powershell
.\scripts\patch-fabric.ps1
```

The script compiles the patched class and updates the local Fabric Loader JAR in
`staging\cache\gameDir`. It does not modify tracked third-party files because the
cache is ignored.

## Compatibility Mod

`compat_mod` is a small Fabric mod with targeted mixins for sandbox-specific
behavior. The build script compiles it against the remapped Minecraft client JAR
and puts the result in `gameDir\mods`.

Build it directly with:

```powershell
.\compat_mod\build_compat_mod.ps1
```

The top-level `build.ps1` runs this automatically.

## GLFW Shim

`glfw_shim/glfw_uwp.cpp` builds a replacement `glfw.dll` for LWJGL GLFW. It
bridges Minecraft's GLFW calls to UWP `CoreWindow` and EGL.

Build it directly with:

```powershell
.\glfw_shim\build_glfw.ps1
```

The top-level build copies the DLL into `staging\package\natives` and injects it
into the LWJGL GLFW native JAR inside the assembled package.

## Version Bumps

When changing Minecraft, Fabric Loader, or key library versions:

1. Update `scripts/config.ps1`.
2. Update runtime constants in `MC.Xbox/App.cpp`.
3. Update fallback launch values in `MC.Xbox/launch.ps1`.
4. Update dependency metadata in `compat_mod/src/main/resources/fabric.mod.json`.
5. Recreate local `gameDir`, `assets`, and `natives-*` content.
6. Rerun `scripts\patch-fabric.ps1` and `build.ps1`.

Avoid committing generated game files, runtime files, certificates, or app
packages.
