# Minecraft Java on Xbox UWP

`MinecraftJavaUWP` is an experiment to run modern Minecraft Java Edition inside an Xbox Developer Mode UWP app.

This project does not emulate the game. It embeds a real JDK in-process, boots Fabric, and presents the game through a custom GLFW compatibility layer backed by UWP `CoreWindow` and Mesa EGL/D3D12.

## Current state

The project is past the proof-of-concept stage:

- the game boots on Xbox Developer Mode
- menus render on screen
- keyboard input works
- singleplayer world loading reaches gameplay
- the renderer uses Mesa on the Xbox GPU rather than a fake software path

It is still an experimental port, not a polished product.

## What this repo contains

- `MC.Xbox/`
  UWP host app that embeds the JVM and starts Minecraft in-process.
- `glfw_shim/`
  A GLFW-compatible shim that bridges LWJGL to UWP `CoreWindow` and EGL.
- `compat_mod/`
  A small Fabric mod with targeted compatibility patches for sandboxed Xbox paths and related platform issues.
- `build.ps1`
  End-to-end packaging script for the UWP app.
- `scripts/`
  Setup, patching, cleanup, asset generation, shared configuration, and environment helper scripts.
- `scripts/patch-fabric.ps1`
  Patch step for Fabric loader path behavior that is incompatible with the Xbox sandbox.

## Documentation

- [Building](docs/BUILDING.md) - setup requirements, local game/runtime files, patching, packaging, and troubleshooting.
- [Patching notes](docs/PATCHING.md) - why the Fabric Loader patch, compatibility mod, and GLFW shim exist.
- [Legal notes](docs/LEGAL.md) - repository license boundaries and third-party content rules.

For a clean local workspace, run `.\scripts\clean.ps1` first to preview ignored generated files. It does not delete anything unless you pass `-Apply`.

Downloaded game/runtime inputs live under `staging\cache`, temporary package assembly lives under `staging\package`, and signed app packages are written to `output`.

## How it works

At a high level:

1. `MC.Xbox.exe` starts as a UWP app.
2. The host publishes the live `CoreWindow` to app properties.
3. The host loads `jvm.dll` in-process and starts a real Java VM.
4. Fabric launches Minecraft from inside that same process.
5. The custom `glfw.dll` shim creates an EGL surface for the UWP window.
6. Mesa translates OpenGL calls to D3D12 on Xbox hardware.

That architecture exists because the normal desktop assumptions do not hold on Xbox Dev Mode:

- there is no usable desktop `HWND` path
- packaged sandbox paths do not behave like normal Windows filesystem paths
- parts of Minecraft, Fabric, LWJGL, and Java NIO need targeted compatibility work

## What this repo does not include

This repository is intentionally source-focused, but it also vendors the Mesa
UWP runtime needed to run the project locally. It does not aim to redistribute:

- Minecraft game assets
- Mojang libraries
- a bundled JRE image in git
- packaged appx builds
- local signing certificates
- local logs, saves, or debug output

If you clone this repo, expect to supply your own legal game files and local build environment.

## Status and limitations

Known rough edges include:

- sandbox-specific path handling is still an active area of work
- some platform diagnostics code in the Java stack still emits JNA/OSHI warnings
- controller support is not the focus yet
- this has only been exercised in Xbox Developer Mode, not retail mode

## Why this exists

The point of the project is not "Minecraft launcher glue." The interesting part is proving that a modern Java game stack can be coerced into a UWP/Xbox sandbox that was never designed for it.

That required solving problems across:

- UWP app hosting
- in-process JVM startup
- native library loading inside packaged apps
- EGL/GL presentation without Win32 window handles
- path canonicalization failures in the Xbox sandbox
- Fabric and Minecraft assumptions about a normal desktop filesystem

## License and ownership

The original project code in this repository is available under the custom terms in [LICENSE](LICENSE): use is allowed with credit, but redistribution is not permitted without prior written permission from veroxsity / BanditVault.

Minecraft, Fabric, Mojang assets, and third-party runtime components remain subject to their own licenses and terms.
