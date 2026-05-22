# MC.Xbox.ps1 - Bootstrap script executed by MC.Xbox.exe
# Builds classpath and launches Minecraft Java Edition via Fabric

$root = Split-Path $MyInvocation.MyCommand.Path
$java = "$root\jre\bin\java.exe"
$gameDir = "$root\game"
$nativesDir = "$root\natives"
$assetsDir = "$root\assets"

# Keep these in sync with scripts/config.ps1 when changing game versions.
$minecraftVersion = "1.21.11"
$fabricLoaderVersion = "0.19.2"
$assetIndex = "29"

# Build classpath from all JARs in game\libraries
$jars = Get-ChildItem "$gameDir\libraries" -Recurse -Filter "*.jar" |
    Where-Object { $_.Name -notmatch "sources|javadoc" } |
    Select-Object -ExpandProperty FullName

$jars += "$gameDir\versions\$minecraftVersion\$minecraftVersion.jar"
$cp = $jars -join ";"

# Write args file to avoid command line length limit
$argsFile = "$root\java_args.txt"
@"
-Xmx4G
-Xms512M
--enable-native-access=ALL-UNNAMED
-Djava.library.path=$nativesDir
-Dfabric.gameJarPath=$gameDir\versions\$minecraftVersion\$minecraftVersion.jar
-cp
$cp
net.fabricmc.loader.impl.launch.knot.KnotClient
--username
DevPlayer
--version
fabric-loader-$fabricLoaderVersion-$minecraftVersion
--gameDir
$gameDir
--assetsDir
$assetsDir
--assetIndex
$assetIndex
--uuid
00000000-0000-0000-0000-000000000000
--accessToken
0
--versionType
release
"@ | Set-Content $argsFile -Encoding ASCII

& $java "@$argsFile"
