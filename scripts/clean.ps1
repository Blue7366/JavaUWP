# Shows or removes local generated files.
param(
    [switch]$Apply,

    [ValidateSet("BuildOutputs", "AllIgnored")]
    [string]$Scope = "BuildOutputs"
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gitDir = Join-Path $root ".git"
if (-not (Test-Path $gitDir)) {
    throw "This cleanup script must be run from a git working tree."
}

function Remove-ProjectPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath,

        [switch]$Apply
    )

    $path = Join-Path $root $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        return
    }

    $resolved = (Resolve-Path -LiteralPath $path).Path
    if (-not $resolved.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove path outside repository: $resolved"
    }

    if ($Apply) {
        Write-Host "Removing $RelativePath"
        Remove-Item -LiteralPath $resolved -Recurse -Force
    } else {
        Write-Host "Would remove $RelativePath"
    }
}

Push-Location $root
try {
    if ($Scope -eq "AllIgnored") {
        if ($Apply) {
            Write-Host "Removing all ignored files..."
            git clean -Xdf
        } else {
            Write-Host "Dry run only. These ignored files would be removed:"
            git clean -Xdn
            Write-Host ""
            Write-Host "Run .\scripts\clean.ps1 -Scope AllIgnored -Apply to remove the listed ignored files."
        }
    } else {
        $buildOutputs = @(
            $ProjectConfig.BuildDir,
            $ProjectConfig.PackageContentDir,
            $ProjectConfig.CertificateDir,
            $ProjectConfig.OutputDir,
            "MC_DevMode.pfx",
            "MC_Java_1.0.0.0.appx",
            "PackageContent",
            "compat_mod\build",
            "MC.Xbox\App.obj",
            "MC.Xbox\MC.Xbox.exe",
            "glfw_shim\glfw.dll",
            "glfw_shim\glfw_uwp.exp",
            "glfw_shim\glfw_uwp.lib",
            "glfw_shim\glfw_uwp.obj",
            "patch\glfw_jar_tmp"
        )

        if ($Apply) {
            Write-Host "Removing build outputs..."
        } else {
            Write-Host "Dry run only. These build outputs would be removed:"
        }

        foreach ($item in $buildOutputs) {
            Remove-ProjectPath -RelativePath $item -Apply:$Apply
        }

        if (-not $Apply) {
            Write-Host ""
            Write-Host "Run .\scripts\clean.ps1 -Apply to remove the listed build outputs."
            Write-Host "Run .\scripts\clean.ps1 -Scope AllIgnored for a full ignored-file dry run."
        }
    }
} finally {
    Pop-Location
}
