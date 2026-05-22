# build_glfw.ps1 - Build glfw_uwp.cpp -> glfw.dll (the CoreWindow shim)
param(
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path $PSScriptRoot -Parent) "scripts\common.ps1")

$tools = Resolve-VSTools
$sdk = Resolve-WindowsSdk
$sdkRoot = $sdk.Root
$sdkVer = $sdk.Version

if (-not $OutputDir) {
    $OutputDir = Join-Path (Get-ConfigPath "BuildDir") "glfw_shim"
}
$OutputDir = (New-Item -ItemType Directory -Force -Path $OutputDir).FullName
$dllPath = Join-Path $OutputDir "glfw.dll"
$objPath = Join-Path $OutputDir "glfw_uwp.obj"
$libPath = Join-Path $OutputDir "glfw_uwp.lib"

$env:INCLUDE = "$($tools.MsvcRoot)\include;" +
               "${sdkRoot}Include\$sdkVer\ucrt;" +
               "${sdkRoot}Include\$sdkVer\shared;" +
               "${sdkRoot}Include\$sdkVer\um;" +
               "${sdkRoot}Include\$sdkVer\winrt;" +
               "${sdkRoot}Include\$sdkVer\cppwinrt"
$env:LIB = "$($tools.MsvcRoot)\lib\x64;" +
           "${sdkRoot}Lib\$sdkVer\ucrt\x64;" +
           "${sdkRoot}Lib\$sdkVer\um\x64"

Push-Location $PSScriptRoot
Write-Host "Building glfw.dll (CoreWindow shim)..."
& $tools.ClExe glfw_uwp.cpp /LD /EHsc /O2 /D_UNICODE /DUNICODE /D_WIN32_WINNT=0x0A00 /Fo"$objPath" `
    /DWINAPI_FAMILY=WINAPI_FAMILY_APP `
    /link /DEF:glfw_uwp.def /OUT:"$dllPath" /IMPLIB:"$libPath" /MACHINE:X64 `
    kernel32.lib runtimeobject.lib windowsapp.lib ole32.lib oleaut32.lib
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "glfw_uwp build FAILED" }
Pop-Location
Write-Host "glfw.dll built OK -> $dllPath"
