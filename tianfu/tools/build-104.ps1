param(
    [string]$BaseJar = 'D:\daima\mcmod\蛊真人本体\guzhenrn蛊真人模组本体10.4.jar',
    [string]$Suffix = '10.4',
    [string]$MinecraftDir = 'D:\game\mcmod\1\.minecraft',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$buildRoot = [IO.Path]::GetFullPath((Join-Path $project 'build'))
$tag = $Suffix -replace '[^0-9A-Za-z]', ''
if ([string]::IsNullOrWhiteSpace($tag) -or $Suffix -notmatch '^[0-9A-Za-z][0-9A-Za-z._-]*$') { throw "Unsafe suffix: $Suffix" }
$classDir = [IO.Path]::GetFullPath((Join-Path $buildRoot "classes-$tag"))
if ($classDir -ne (Join-Path $project "build\classes-$tag") -or !( $classDir.StartsWith($buildRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))) { throw 'Unsafe compiler output path.' }
if (!(Test-Path -LiteralPath (Join-Path $Jdk 'javac.exe') -PathType Leaf)) { throw 'Missing javac' }
if (!(Test-Path -LiteralPath $BaseJar -PathType Leaf)) { throw "Missing base jar: $BaseJar" }

# 10.4 / 10.20 内层 jar 共用的 9 个机制类指纹。10.4 外层包是 jar-in-jar，
# 必须先取出 META-INF/jarjar/guzhenren-*.jar 才能作为 javac classpath。
$expectedHashes = [ordered]@{
    'net/guzhenren/procedures/UikongqiaoquedingProcedure' = '307F66451F87ED56E0E344964C88871716DA942372BF01B79C0B30F59AD74E37'
    'net/guzhenren/procedures/JingJieBaoCunProcedure'     = 'F991FBC556CD1B2D6162C133FE98C3E6A99799196603E3A29B36E715988866D2'
    'net/guzhenren/procedures/KE1Procedure'               = 'F0908D9903FC60A1E356B95D9D1CD29CBF6C8A9B4A85FB140544A9168D30A0DC'
    'net/guzhenren/procedures/UIxiuliananjianProcedure'   = '1060052B3C68C0B00401FBDBF013079A47D2F0EF6DF88767582DF55B89B7230B'
    'net/guzhenren/procedures/XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure' = 'BACAB72763BC9D1BA6DE1ABE30332A206640FB6BCF1221E3F24EC6CA5D9B1D04'
    'net/guzhenren/procedures/TianziliuchengProcedure'    = 'B585C12B5E217B5F70E2D217C0D8FC3AC17D1D6015E6C74E4FEABEB48953E590'
    'net/guzhenren/procedures/SxProcedure'                = '07447FC82B0C8AFEA25E9BC7007E3AB5012A1A1724C0C41E6E4420CC00AC8E81'
    'net/guzhenren/item/YizhuanrendaoxiwangguItem'        = '8A6D7035C1D510459FC63B8FB98E5C8DA69E1A15BFDDC633DB1B6A2B7D578274'
    'net/guzhenren/network/GuzhenrenModVariables$PlayerVariables' = '9EFE0F2C17F1EE5B6120EECE3D1C953705D7A965DA2E559FAB83E2344638ECFF'
}

$outerJar = (Resolve-Path -LiteralPath $BaseJar).Path
$baseZip = [IO.Compression.ZipFile]::OpenRead($outerJar)
try {
    $innerEntry = $baseZip.Entries | Where-Object { $_.FullName -match '^META-INF/jarjar/guzhenren-[^/]+\.jar$' } | Select-Object -First 1
    if ($null -ne $innerEntry) {
        $baseRoot = Join-Path $buildRoot "base-$tag"
        New-Item -ItemType Directory -Force $baseRoot | Out-Null
        $innerJar = Join-Path $baseRoot 'guzhenren-inner.jar'
        if (Test-Path -LiteralPath $innerJar) { Remove-Item -LiteralPath $innerJar -Force }
        $input = $innerEntry.Open()
        $output = [IO.File]::Create($innerJar)
        try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() }
        Write-Output "Extracted base mod: $innerJar"
    } else {
        $innerJar = $outerJar
    }
} finally { $baseZip.Dispose() }

# 指纹门禁：包名对得上不代表字节码对得上；不对就拒绝出包，避免把 9.8 或其它 10.x 误标成 10.4。
function Get-ZipEntrySha256([string]$path, [string]$entryName) {
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entry = $zip.GetEntry($entryName)
        if ($null -eq $entry) { throw "Missing entry: $entryName" }
        $stream = $entry.Open()
        $sha = [Security.Cryptography.SHA256]::Create()
        try {
            $bytes = $sha.ComputeHash($stream)
            return (-join ($bytes | ForEach-Object { $_.ToString('X2') }))
        } finally { $sha.Dispose(); $stream.Dispose() }
    } finally { $zip.Dispose() }
}

$mismatch = @()
foreach ($name in $expectedHashes.Keys) {
    $actual = Get-ZipEntrySha256 $innerJar ($name + '.class')
    if ($actual -ne $expectedHashes[$name]) {
        $mismatch += "$name expected=$($expectedHashes[$name]) actual=$actual"
    }
}
if ($mismatch.Count -gt 0) {
    throw ("Base jar is not the registered 10.4/10.20 baseline:`n" + ($mismatch -join "`n"))
}
Write-Output ('Base fingerprints: 9/9 match 10.4/10.20 baseline')
Write-Output ('Base inner SHA256: ' + (Get-FileHash -LiteralPath $innerJar -Algorithm SHA256).Hash)

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
        $mcIndex = [array]::IndexOf($gameArgs, '--fml.mcVersion')
        if ($gameArgs -contains '--fml.neoForgeVersion' -and $mcIndex -ge 0 -and $mcIndex + 1 -lt $gameArgs.Count -and $gameArgs[$mcIndex + 1] -eq '1.21.1' -and $profile.libraries.Count -gt $bestCount) { $bestProfile = $profile; $bestCount = $profile.libraries.Count }
    } catch { continue }
}
if ($null -eq $bestProfile) { throw 'No usable local NeoForge 1.21.1 profile.' }
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
$classpath.Add($innerJar)
$classpath.Add((Resolve-Path (Join-Path $project '..\libs\veil-neoforge-1.21.1-4.4.1.jar')).Path)
$toolOut = Join-Path $buildRoot "verification-$tag"
New-Item -ItemType Directory -Force $toolOut | Out-Null
if (Test-Path -LiteralPath $classDir) { Remove-Item -LiteralPath $classDir -Recurse -Force }
New-Item -ItemType Directory -Force $classDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $project 'src\main\java') -Recurse -Filter *.java | Sort-Object FullName)
if ($sources.Count -eq 0) { throw 'No Java sources found.' }
function ArgQuote([string]$value) { return '"' + $value.Replace('\','/').Replace('"','\"') + '"' }
$argLines = @('-encoding', 'UTF-8', '-proc:none', '-g', '-d', (ArgQuote $classDir), '-cp', (ArgQuote ($classpath -join ';'))) + @($sources | ForEach-Object { ArgQuote $_.FullName })
$argPath = Join-Path $toolOut 'javac-args.txt'
[IO.File]::WriteAllLines($argPath, $argLines, [Text.UTF8Encoding]::new($false))
$previousErrorAction = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$compilerOutput = & (Join-Path $Jdk 'javac.exe') ('@' + $argPath) 2>&1
$compilerCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorAction
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
$displayName = "蛊真人天命（本体$Suffix）"
if ([regex]::Matches($toml, '(?m)^\s*displayName\s*=\s*"[^"]*"').Count -ne 1) { throw 'Ambiguous mod displayName metadata.' }
$toml = [regex]::Replace($toml, '(?m)^(\s*displayName\s*=\s*)"[^"]*"', ('$1"' + $displayName + '"'))
if ($toml -notmatch '10\.4') { throw 'Description does not mention the 10.4 target.' }
$jarDir = Join-Path $buildRoot 'libs'
New-Item -ItemType Directory -Force $jarDir | Out-Null
$jarPath = Join-Path $jarDir ("天命-$version-$Suffix.jar")
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
    foreach ($required in @('META-INF/neoforge.mods.toml','META-INF/MANIFEST.MF','tianfu.mixins.json','pack.mcmeta','net/guzhenren/tianfu/TianfuMod.class')) { if ($names -notcontains $required) { throw "Missing JAR entry: $required" } }
    foreach ($name in @('META-INF/neoforge.mods.toml','tianfu.mixins.json')) {
        $stream = $zip.GetEntry($name).Open()
        try { $first = $stream.ReadByte(); $second = $stream.ReadByte(); $third = $stream.ReadByte(); if ($first -eq 239 -and $second -eq 187 -and $third -eq 191) { throw "BOM retained: $name" } } finally { $stream.Dispose() }
    }
    $packedToml = [IO.StreamReader]::new($zip.GetEntry('META-INF/neoforge.mods.toml').Open(), [Text.Encoding]::UTF8).ReadToEnd()
    if ($packedToml -notmatch ('version\s*=\s*"' + [regex]::Escape($version) + '"')) { throw 'Packed mods.toml version mismatch.' }
    if ($packedToml -notmatch '10\.4') { throw 'Packed mods.toml lost the 10.4 description.' }
} finally { $zip.Dispose() }
$report = [ordered]@{ jar=$jarPath; version=$version; suffix=$Suffix; sha256=(Get-FileHash -LiteralPath $jarPath).Hash; baseOuter=$outerJar; baseInner=$innerJar; baseInnerSha256=(Get-FileHash -LiteralPath $innerJar).Hash; compatProfile='10.4/10.20'; fingerprints='9/9 matched'; javaSources=$sources.Count; classes=$classCount; neoForge=$neo; minecraft=$mc; neoForm=$neoform; dependencies=$classpath.ToArray(); proof='Compilation, fingerprint gate and package checks only; no runtime/player acceptance implied.' }
$report | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $toolOut 'build-report.json')
Write-Output "Built $jarPath ($classCount classes)"
Write-Output ('SHA256 ' + $report.sha256)
