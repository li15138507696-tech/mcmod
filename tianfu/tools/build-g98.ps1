param(
    [string]$MinecraftDir = 'D:\game\mcmod\1\.minecraft',
    [string]$NativeJar = 'D:\cxdownload\1\.minecraft\versions\转生到蛊界就要拿出真本事\mods\guzhenren蛊真人模组本体9.8.jar',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$buildRoot = [IO.Path]::GetFullPath((Join-Path $project 'build'))
$classDir = [IO.Path]::GetFullPath((Join-Path $buildRoot 'classes-g98'))
if ($classDir -ne (Join-Path $project 'build\classes-g98') -or !( $classDir.StartsWith($buildRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))) { throw 'Unsafe compiler output path.' }
foreach ($required in @($NativeJar, (Join-Path $Jdk 'javac.exe'), (Join-Path $project '..\libs\veil-neoforge-1.21.1-4.4.1.jar'))) { if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Missing dependency: $required" } }
$versionMatch = [regex]::Match([IO.File]::ReadAllText((Join-Path $project 'version.properties')), '(?m)^\s*version\s*=\s*([^\s#]+)')
if (!$versionMatch.Success) { throw 'version.properties has no version.' }
$version = $versionMatch.Groups[1].Value
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Unexpected version value.' }
$bestProfile = $null
$bestCount = -1
foreach ($file in (Get-ChildItem (Join-Path $MinecraftDir 'versions') -Directory | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -File -Filter *.json })) {
    try {
        $profile = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
        $gameArgs = @($profile.arguments.game | Where-Object { $_ -is [string] })
        if ($gameArgs -contains '--fml.neoForgeVersion' -and $profile.libraries.Count -gt $bestCount) { $bestProfile = $profile; $bestCount = $profile.libraries.Count }
    } catch { continue }
}
if ($null -eq $bestProfile) { throw 'No usable local NeoForge profile.' }
$gameArgs = @($bestProfile.arguments.game | Where-Object { $_ -is [string] })
function ProfileValue([string]$key) { $index = [array]::IndexOf($gameArgs, $key); if ($index -lt 0) { throw "Missing profile argument $key" }; return $gameArgs[$index + 1] }
$neo = ProfileValue '--fml.neoForgeVersion'
$mc = ProfileValue '--fml.mcVersion'
$neoform = ProfileValue '--fml.neoFormVersion'
$libraries = Join-Path $MinecraftDir 'libraries'
$classpath = [Collections.Generic.List[string]]::new()
foreach ($relative in @("net/neoforged/neoforge/$neo/neoforge-$neo-client.jar", "net/minecraft/client/$mc-$neoform/client-$mc-$neoform-srg.jar", "net/neoforged/neoforge/$neo/neoforge-$neo-universal.jar")) {
    $path = Join-Path $libraries $relative
    if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing patched Minecraft/NeoForge artifact: $path" }
    $classpath.Add($path)
}
foreach ($library in $bestProfile.libraries) {
    if ($null -ne $library.downloads -and $null -ne $library.downloads.artifact) {
        $path = Join-Path $libraries $library.downloads.artifact.path
        if (Test-Path -LiteralPath $path -PathType Leaf) { $classpath.Add($path) }
    }
}
$classpath.Add((Resolve-Path -LiteralPath $NativeJar).Path)
$classpath.Add((Resolve-Path (Join-Path $project '..\libs\veil-neoforge-1.21.1-4.4.1.jar')).Path)
$toolOut = Join-Path $buildRoot 'verification-g98'
New-Item -ItemType Directory -Force $toolOut | Out-Null
if (Test-Path -LiteralPath $classDir) { Remove-Item -LiteralPath $classDir -Recurse -Force }
New-Item -ItemType Directory -Force $classDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $project 'src\main\java') -Recurse -Filter *.java | Sort-Object FullName)
if ($sources.Count -eq 0) { throw 'No Java sources found.' }
function ArgQuote([string]$value) { return '"' + $value.Replace('\','/').Replace('"','\"') + '"' }
$argLines = @('-encoding', 'UTF-8', '-proc:none', '-g', '-d', (ArgQuote $classDir), '-cp', (ArgQuote ($classpath -join ';'))) + @($sources | ForEach-Object { ArgQuote $_.FullName })
$argPath = Join-Path $toolOut 'javac-args.txt'
[IO.File]::WriteAllLines($argPath, $argLines, [Text.UTF8Encoding]::new($false))
$compilerOutput = & (Join-Path $Jdk 'javac.exe') ('@' + $argPath) 2>&1
$compilerCode = $LASTEXITCODE
$compilerOutput | Set-Content -Encoding utf8 (Join-Path $toolOut 'javac.log')
if ($compilerCode -ne 0) { $compilerOutput | Select-Object -Last 50; throw "Compilation failed ($compilerCode)." }
$resourceRoot = Join-Path $project 'src\main\resources'
$mixinJson = [IO.File]::ReadAllText((Join-Path $resourceRoot 'tianfu.mixins.json')).TrimStart([char]0xfeff)
$mixinConfig = $mixinJson | ConvertFrom-Json
foreach ($mixin in $mixinConfig.mixins) {
    $entry = $mixinConfig.package.Replace('.', '/') + '/' + $mixin + '.class'
    if (!(Test-Path -LiteralPath (Join-Path $classDir $entry) -PathType Leaf)) { throw "Registered mixin missing: $entry" }
}
$toml = [IO.File]::ReadAllText((Join-Path $resourceRoot 'META-INF\neoforge.mods.toml')).TrimStart([char]0xfeff)
if ($toml -notmatch '(?m)^\s*modId\s*=\s*"tianfu"\s*$') { throw 'Missing Tianfu modId.' }
if ([regex]::Matches($toml, '(?m)^\s*version\s*=\s*"[^"]+"').Count -ne 1) { throw 'Ambiguous mod version metadata.' }
$toml = [regex]::Replace($toml, '(?m)^(\s*version\s*=\s*)"[^"]+"', ('$1"' + $version + '"'))
$jarDir = Join-Path $buildRoot 'libs'
New-Item -ItemType Directory -Force $jarDir | Out-Null
$jarPath = Join-Path $jarDir ("tianfu-$version-mc98.jar")
if (Test-Path -LiteralPath $jarPath) { Remove-Item -LiteralPath $jarPath -Force }
$zip = [IO.Compression.ZipFile]::Open($jarPath, [IO.Compression.ZipArchiveMode]::Create)
function AddText([string]$name, [string]$content) {
    $entry = $zip.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
    $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
    try { $writer.Write($content) } finally { $writer.Dispose() }
}
try {
    foreach ($file in (Get-ChildItem $classDir -Recurse -Filter *.class | Sort-Object FullName)) {
        $name = $file.FullName.Substring($classDir.Length + 1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
    foreach ($file in (Get-ChildItem $resourceRoot -Recurse -File | Sort-Object FullName)) {
        $name = $file.FullName.Substring($resourceRoot.Length + 1).Replace('\','/')
        if ($name -in @('META-INF/MANIFEST.MF','META-INF/neoforge.mods.toml','tianfu.mixins.json')) { continue }
        if ($file.Extension -eq '.java') { throw "Java source unexpectedly present under resources: $name" }
        if ($file.Extension -eq '.json') { [void]([IO.File]::ReadAllText($file.FullName).TrimStart([char]0xfeff) | ConvertFrom-Json) }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
    AddText 'META-INF/neoforge.mods.toml' $toml
    AddText 'tianfu.mixins.json' $mixinJson
    AddText 'META-INF/MANIFEST.MF' ("Manifest-Version: 1.0`r`nSpecification-Title: tianfu`r`nSpecification-Vendor: tianfu`r`nSpecification-Version: 1`r`nImplementation-Title: tianfu`r`nImplementation-Version: $version`r`nImplementation-Vendor: tianfu`r`n`r`n")
} finally { $zip.Dispose() }
$zip = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $names = @($zip.Entries | ForEach-Object FullName)
    if (($names | Select-Object -Unique).Count -ne $names.Count) { throw 'Duplicate JAR entries.' }
    $classCount = @($names | Where-Object { $_.EndsWith('.class') }).Count
    foreach ($required in @('META-INF/neoforge.mods.toml','META-INF/MANIFEST.MF','tianfu.mixins.json','net/guzhenren/tianfu/TianfuMod.class')) { if ($names -notcontains $required) { throw "Missing JAR entry: $required" } }
    foreach ($name in @('META-INF/neoforge.mods.toml','tianfu.mixins.json')) {
        $stream = $zip.GetEntry($name).Open()
        try { $first = $stream.ReadByte(); $second = $stream.ReadByte(); $third = $stream.ReadByte(); if ($first -eq 239 -and $second -eq 187 -and $third -eq 191) { throw "BOM retained: $name" } } finally { $stream.Dispose() }
    }
} finally { $zip.Dispose() }
$report = [ordered]@{ jar=$jarPath; version=$version; sha256=(Get-FileHash -LiteralPath $jarPath).Hash; nativeJar=(Resolve-Path -LiteralPath $NativeJar).Path; nativeSha256=(Get-FileHash -LiteralPath $NativeJar).Hash; javaSources=$sources.Count; classes=$classCount; neoForge=$neo; minecraft=$mc; neoForm=$neoform; dependencies=$classpath.ToArray(); proof='Compilation and package checks only; no runtime/player acceptance implied.' }
$report | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $toolOut 'build-report.json')
Write-Output "Built $jarPath ($classCount classes)"
Write-Output ('SHA256 ' + $report.sha256)
