# Obsolete development launcher.
#
# Bandit Launcher now starts Minecraft through MC.Xbox.exe after Microsoft/Xbox
# authentication and Minecraft entitlement verification. This script is kept as
# a hard stop so old local workflows do not bypass the supported launch path.

$ErrorActionPreference = "Stop"
throw "MC.Xbox\launch.ps1 is obsolete. Run the packaged UWP app so the normal authenticated launch path is used."
