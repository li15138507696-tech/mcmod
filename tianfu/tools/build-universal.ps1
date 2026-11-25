param(
    [string]$MinecraftDir = 'D:\game\mcmod\1\.minecraft',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$buildRoot = [IO.Path]::GetFullPath((Join-Path $project 'build'))
$classDir = [IO.Path]::GetFullPath((Join-Path $buildRoot 'classes-universal'))
if ($classDir -ne (Join-Path $project 'build\classes-universal') -or !( $classDir.StartsWith($buildRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))) { throw 'Unsafe compiler output path.' }
if (!(Test-Path -LiteralPath (Join-Path $Jdk 'javac.exe') -PathType Leaf)) { throw 'Missing javac' }
$bestProfile = $null
$bestCount = -1
foreach ($file in (Get-ChildItem (Join-Path $MinecraftDir 'versions') -Directory | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -File -Filter *.json })) {
    try {
        $profile = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
        $gameArgs = @($profile.arguments.game | Where-Object { $_ -is [string] })
        $mcIndex = [array]::IndexOf($gameArgs, '--fml.mcVersion')
        if ($gameArgs -contains '--fml.neoForgeVersion' -and $mcIndex -ge 0 -and $mcIndex + 1 -lt $gameArgs.Count -and $gameArgs[$mcIndex + 1] -eq '1.21.1' -and $profile.libraries.Count -gt $bestCount) { $bestProfile = $profile; $bestCount = $profile.libraries.Count }
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
$toolOut = Join-Path $buildRoot 'verification-universal'
New-Item -ItemType Directory -Force $toolOut | Out-Null
if (Test-Path -LiteralPath $classDir) { Remove-Item -LiteralPath $classDir -Recurse -Force }
New-Item -ItemType Directory -Force $classDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $project 'src\universal\java') -Recurse -Filter *.java | Sort-Object FullName)
if ($sources.Count -eq 0) { throw 'No Java sources found.' }
function ArgQuote([string]$value) { return '"' + $value.Replace('\','/').Replace('"','\"') + '"' }
$argLines = @('-encoding', 'UTF-8', '-proc:none', '-g', '-d', (ArgQuote $classDir), '-cp', (ArgQuote ($classpath -join ';'))) + @($sources | ForEach-Object { ArgQuote $_.FullName })
$argPath = Join-Path $toolOut 'javac-args.txt'
[IO.File]::WriteAllLines($argPath, $argLines, [Text.UTF8Encoding]::new($false))
$compilerOutput = & (Join-Path $Jdk 'javac.exe') ('@' + $argPath) 2>&1
$compilerCode = $LASTEXITCODE
$compilerOutput | Set-Content -Encoding utf8 (Join-Path $toolOut 'javac.log')
if ($compilerCode -ne 0) { $compilerOutput | Select-Object -Last 50; throw "Compilation failed ($compilerCode)." }
$resourceRoot = Join-Path $project 'src\universal\resources'
$jarDir = Join-Path $buildRoot 'libs'
New-Item -ItemType Directory -Force $jarDir | Out-Null
$jarPath = Join-Path $jarDir 'tianfu-1.4.20-universal.jar'
if (Test-Path -LiteralPath $jarPath) { Remove-Item -LiteralPath $jarPath -Force }
$zip = [IO.Compression.ZipFile]::Open($jarPath, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($root in @($classDir, $resourceRoot)) {
        foreach ($file in (Get-ChildItem -LiteralPath $root -Recurse -File | Sort-Object FullName)) {
            $name = $file.FullName.Substring($root.Length + 1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally { $zip.Dispose() }
$zip = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $names = @($zip.Entries | ForEach-Object FullName)
    if ($names -contains 'tianfu.mixins.json' -or ($names | Where-Object { $_ -match '/mixin/' })) { throw 'Unexpected mixin in universal JAR.' }
    if ($names -notcontains 'META-INF/neoforge.mods.toml') { throw 'Missing metadata.' }
    foreach ($entry in $zip.Entries | Where-Object { $_.FullName.EndsWith('.class') }) {
        $stream = $entry.Open(); $mem = [IO.MemoryStream]::new()
        try { $stream.CopyTo($mem); $str = [Text.Encoding]::Latin1.GetString($mem.ToArray()) } finally { $mem.Dispose(); $stream.Dispose() }
        if ($str -match 'net/guzhenren/(network|procedures|init|item|client)/') { throw "Base class reference in $($entry.FullName)" }
    }
} finally { $zip.Dispose() }
Write-Output "Built $jarPath"
Write-Output ('SHA256 ' + (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash)
