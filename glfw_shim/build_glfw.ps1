# build_glfw.ps1 - Build glfw_uwp.cpp -> glfw.dll (the CoreWindow shim)
$ErrorActionPreference = "Stop"

function Resolve-VSTools {
    $vswhere = Join-Path ${env:ProgramFiles(x86)} "Microsoft Visual Studio\Installer\vswhere.exe"
    if (-not (Test-Path $vswhere)) {
        throw "vswhere.exe not found. Install Visual Studio Build Tools or set up the MSVC environment manually."
    }

    $installPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
    if (-not $installPath) {
        throw "Visual Studio with C++ tools not found."
    }

    $msvcRoot = Get-ChildItem (Join-Path $installPath "VC\Tools\MSVC") -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $msvcRoot) {
        throw "MSVC tools directory not found."
    }

    $clExe = Join-Path $msvcRoot "bin\Hostx64\x64\cl.exe"
    if (-not (Test-Path $clExe)) {
        throw "cl.exe not found at $clExe"
    }

    return @{
        MsvcRoot = $msvcRoot
        ClExe = $clExe
    }
}

$tools = Resolve-VSTools
$sdkRoot = (Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Windows Kits\Installed Roots").KitsRoot10
$sdkVer = Get-ChildItem (Join-Path $sdkRoot "Include") | Sort-Object Name | Select-Object -Last 1 -ExpandProperty Name
$sdkX86Root = Join-Path ${env:ProgramFiles(x86)} "Windows Kits\10"
if (-not (Test-Path (Join-Path $sdkRoot "Include\$sdkVer\um\winsock2.h")) -or
    -not (Test-Path (Join-Path $sdkRoot "Lib\$sdkVer\um\x64\kernel32.lib"))) {
    $sdkRoot = $sdkX86Root
    $sdkVer = Get-ChildItem (Join-Path $sdkRoot "Include") | Sort-Object Name | Select-Object -Last 1 -ExpandProperty Name
}

$sdkInclude = Join-Path $sdkRoot "Include\$sdkVer"
$sdkLib = Join-Path $sdkRoot "Lib\$sdkVer"

$env:INCLUDE = "$($tools.MsvcRoot)\include;" +
               "$sdkInclude\ucrt;" +
               "$sdkInclude\shared;" +
               "$sdkInclude\um;" +
               "$sdkInclude\winrt;" +
               "$sdkInclude\cppwinrt"
$env:LIB = "$($tools.MsvcRoot)\lib\x64;" +
           "$sdkLib\ucrt\x64;" +
           "$sdkLib\um\x64"

Push-Location $PSScriptRoot
Write-Host "Building glfw.dll (CoreWindow shim)..."
& $tools.ClExe glfw_uwp.cpp /LD /EHsc /Od /Zi /FS /D_UNICODE /DUNICODE /D_WIN32_WINNT=0x0A00 /Fd:glfw_build30.pdb `
    /DWINAPI_FAMILY=WINAPI_FAMILY_APP `
    /link /DEF:glfw_uwp.def /OUT:glfw.dll /MACHINE:X64 /DEBUG /PDB:glfw_link30.pdb /MAP:glfw.map `
    kernel32.lib runtimeobject.lib windowsapp.lib ole32.lib oleaut32.lib d3d11.lib dxgi.lib gameinput.lib
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "glfw_uwp build FAILED" }
Pop-Location
Write-Host "glfw.dll built OK -> $PSScriptRoot\glfw.dll"
