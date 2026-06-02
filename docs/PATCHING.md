# Patching Notes

Minecraft, Fabric, LWJGL, and Java expect desktop Windows behavior. Xbox Developer Mode UWP runs inside a packaged sandbox, so a few targeted patches are needed.

## Fabric Loader patch

`scripts\patch-fabric.ps1` compiles Java sources from `patch\` and overlays the resulting classes into a local Fabric Loader JAR:

```text
staging\cache\gameDir\libraries\net\fabricmc\fabric-loader\<loader-version>\fabric-loader-<loader-version>.jar
```

Patched classes:

- `LoaderUtil`
- `FileSystemUtil`
- `FileSystemReference`
- `OutputConsumerPath`
- `FabricLauncherBase`

Fabric Loader `0.14.25` does not bundle TinyRemapper's `OutputConsumerPath`, so that part of the Fabric patch is skipped for legacy loader jars. The package build also patches TinyRemapper `0.8.2` separately when a legacy Fabric target needs it.

The patch script copies every compiled `.class` file from the patch output into
the Fabric Loader JAR. This is intentional: Java sources can generate synthetic
inner classes such as `OutputConsumerPath$1.class`, and leaving an old inner
class in the JAR can break Fabric at remap time.

The main goals are:

- Avoid `Path.toRealPath()` in places where the Xbox sandbox blocks or breaks the underlying Windows path query.
- Keep Fabric remapping from using file system calls that fail in packaged app paths.
- Make loader launch behavior tolerate the UWP runtime layout.

Run it directly with:

```powershell
.\scripts\patch-fabric.ps1
```

The top level build also runs it automatically.

## Compatibility mod

`compat_mod` is a Fabric client mod with mixins for Minecraft, mod, controller, filesystem, and graphics code paths that need sandbox aware behavior.

Current mixins:

- `BanditControllerClientMixin`
- `BanditControllerGameRendererMixin`
- `BanditControllerHandledScreenMixin`
- `BanditControllerRecipeBookScreenMixin`
- `BanditControllerScreenMixin`
- `CobblemonShowdownFileSystemMixin`
- `MinecraftClientProbeMixin`
- `PathUtilBypassMixin`
- `SystemDetailsOshiBypassMixin`
- `WorldLoadProgressTrackerMixin`
- `ZipFsBypass121Mixin`
- `ZipFsBypassMixin`

Build it directly with:

```powershell
.\compat_mod\build_compat_mod.ps1
```

You can build a specific target with:

```powershell
.\compat_mod\build_compat_mod.ps1 -MinecraftVersion 1.19.2 -LoaderVersion 0.14.25
```

The build disables mixins and sources that do not apply to the requested Minecraft version. Controller sources are included for legacy controller targets such as `1.16.5` and `1.19.2`, and excluded from modern targets where Controlify is the expected controller mod.

The top level package step places the default compatibility mod under `runtime\bundled-mods`, and places per target compatibility mod jars under `runtime\version-mods\<target-id>`. The UWP host copies the right launcher owned mods into writable `LocalState\game\mods` on launch.

## GLFW shim

`glfw_shim\glfw_uwp.cpp` builds a replacement `glfw.dll` for LWJGL GLFW.

It handles:

- `CoreWindow` based window setup.
- EGL surface creation for Mesa.
- Keyboard and text input callbacks.
- Basic monitor, cursor, timing, and window API responses expected by LWJGL.
- Xbox controller state through GameInput and the GLFW joystick and gamepad APIs.

Build it directly with:

```powershell
.\glfw_shim\build_glfw.ps1
```

The top level build copies the DLL into package natives. The JVM launch forces LWJGL to use that DLL with `-Dorg.lwjgl.glfw.libname`, so the package no longer rewrites downloaded LWJGL native jars.

## Runtime layout

The packaged app keeps launcher owned runtime files under the package folder. At launch, `MC.Xbox.exe` prepares writable state in `LocalState`:

```text
LocalState\game
LocalState\assets
LocalState\natives
```

The game uses `LocalState\game` for saves, config, logs, mods, downloaded libraries, downloaded client jars, and other writable files. Bundled compatibility mods are copied there during launch. Mojang libraries, Minecraft client jars, version JSON files, asset indexes, and asset objects are verified from `download_manifest.tsv` and downloaded into `LocalState` after Minecraft ownership verification.

The package includes:

```text
runtime\version_catalog.tsv
runtime\manifests\<target-id>.tsv
runtime\version-mods\<target-id>\
```

The root `download_manifest.tsv` remains the default target manifest. The `runtime\manifests` folder contains per target manifests for cataloged Fabric targets.

`MC.Xbox.exe` writes a `LocalState\.download_manifest` marker containing the selected launch target and packaged manifest hash. If that marker changes, the launcher removes downloaded official runtime folders before validating the new manifest. The signed in menu's `Repair downloads` action forces this cleanup for the current target.

## Version targets

Playable setups are modeled as launch targets:

```text
minecraft version + loader + loader version
```

When changing the default Minecraft or Fabric Loader version:

1. Update `scripts/config.ps1`, or pass build overrides while testing.
2. Update `config\versions.tsv` if the launcher catalog should change.
3. Update `compat_mod/src/main/resources/fabric.mod.json` only when the compatibility mod metadata needs a new default range.
4. Recreate `staging\cache\gameDir`.
5. Recreate `staging\cache\natives-1.21` if native versions changed.
6. Run the local Fabric client once so remapped jars are generated for the default target.
7. Run `.\build.ps1`; the build regenerates the default manifest, per target manifests, patched loader jars, and per target compatibility mod jars.

Forge, NeoForge, and older vanilla targets can be cataloged before their launch providers are implemented, but they should stay experimental until their providers are complete.

Do not commit generated game files, downloaded assets, natives, certificates, app packages, logs, or saves.
