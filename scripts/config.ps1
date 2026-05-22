# Shared project settings. Keep version-sensitive values here so build and
# setup scripts do not drift apart.
$ProjectConfig = [ordered]@{
    MinecraftVersion         = "1.21.11"
    MinecraftAssetIndex      = "29"
    FabricLoaderVersion      = "0.19.2"
    MixinVersion             = "0.17.2+mixin.0.8.7"
    JnaVersion               = "5.17.0"
    LwjglGlfwVersion         = "3.3.3"
    JavaRelease              = 21
    CompatModId              = "banditvault-xbox-compat"
    CompatModVersion         = "1.0.0"
    StagingDir               = "staging"
    CacheDir                 = "staging/cache"
    BuildDir                 = "staging/build"
    OutputDir                = "output"
    GameDir                  = "staging/cache/gameDir"
    AssetsDir                = "staging/cache/assets"
    NativesDir               = "staging/cache/natives-1.21"
    MesaRuntimeDir           = "mesa-runtime"
    ToolsDir                 = "staging/cache/tools"
    NotesDir                 = "staging/notes"
    PackageContentDir        = "staging/package"
    CertificateDir           = "staging/certs"
    AppxFileName             = "MC_Java_1.0.0.0.appx"
    CertificateFileName      = "MC_DevMode.pfx"
    CertificatePassword      = "devmode"
    DefaultCertificateSubject = "CN=MinecraftJavaUWP Dev"
}
