# patch-fabric.ps1 - Patch fabric-loader to fix Xbox toRealPath() issue
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

$root   = Resolve-RepoRoot
$java   = Resolve-JavaHome
$gameDir = Get-ConfigPath "GameDir"
$buildDir = Get-ConfigPath "BuildDir"
$loaderVersion = $ProjectConfig.FabricLoaderVersion
$loader = Join-Path $gameDir "libraries\net\fabricmc\fabric-loader\$loaderVersion\fabric-loader-$loaderVersion.jar"
$patch  = Join-Path $root "patch"
$tmp    = Join-Path $buildDir "patch"

New-Item -ItemType Directory -Force $tmp | Out-Null

# Compile the patched class against the original JAR
Write-Host "Compiling patched LoaderUtil..."
& (Join-Path $java "bin\javac.exe") -cp $loader -d $tmp (Join-Path $patch "LoaderUtil.java")
if ($LASTEXITCODE -ne 0) { throw "Compile failed" }

# Inject the patched class into the JAR
Write-Host "Patching JAR..."
$classFile = "net\fabricmc\loader\impl\util\LoaderUtil.class"
Push-Location $tmp
& (Join-Path $java "bin\jar.exe") uf $loader $classFile
if ($LASTEXITCODE -ne 0) { throw "JAR patch failed" }
Pop-Location

Write-Host "Done - fabric-loader-$loaderVersion.jar patched"
Write-Host "Class injected: $classFile"
