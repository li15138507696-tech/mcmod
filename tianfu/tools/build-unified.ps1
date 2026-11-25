param(
    [string]$MinecraftDir = 'D:\game\mcmod\1\.minecraft',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

$tianfuDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repoRoot = (Resolve-Path (Join-Path $tianfuDir '..')).Path

$tfVersionMatch = [regex]::Match([IO.File]::ReadAllText((Join-Path $tianfuDir 'version.properties')), '(?m)^\s*version\s*=\s*([^\s#]+)')
if (!$tfVersionMatch.Success) { throw 'tianfu/version.properties has no version.' }
$tfVersion = $tfVersionMatch.Groups[1].Value

$daotuDir = Join-Path $repoRoot 'daotu'
$dtVersionMatch = [regex]::Match([IO.File]::ReadAllText((Join-Path $daotuDir 'version.properties')), '(?m)^\s*version\s*=\s*([^\s#]+)')
if (!$dtVersionMatch.Success) { throw 'daotu/version.properties has no version.' }
$dtVersion = $dtVersionMatch.Groups[1].Value

Write-Output "=== 1. 构建统一单包天命 JAR ($tfVersion) ==="
Push-Location $tianfuDir
try {
    & (Join-Path $tianfuDir 'gradlew.bat') clean jar compileTestJava
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed ($LASTEXITCODE)." }
} finally {
    Pop-Location
}

$jarPath = Join-Path $tianfuDir "build\libs\天命-$tfVersion.jar"
if (!(Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Expected unified JAR missing: $jarPath"
}

function Assert-MixinPackageHygiene([string[]]$EntryNames, [psobject]$MixinJson, [string]$Label) {
    $pkgPrefix = $MixinJson.package.Replace('.', '/') + '/'
    $allowedTopLevels = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($group in @('mixins', 'client', 'server')) {
        $prop = $MixinJson.PSObject.Properties[$group]
        if ($null -ne $prop -and $null -ne $prop.Value) {
            foreach ($m in @($prop.Value)) {
                [void]$allowedTopLevels.Add(($pkgPrefix + $m.Replace('.', '/')))
            }
        }
    }
    $pluginProp = $MixinJson.PSObject.Properties['plugin']
    if ($null -ne $pluginProp -and $pluginProp.Value) {
        [void]$allowedTopLevels.Add($pluginProp.Value.Replace('.', '/'))
    }
    $stray = @()
    foreach ($entry in $EntryNames) {
        if ($entry.StartsWith($pkgPrefix, [StringComparison]::Ordinal) -and $entry.EndsWith('.class', [StringComparison]::Ordinal)) {
            $rel = $entry.Substring(0, $entry.Length - '.class'.Length)
            $dollar = $rel.IndexOf('$')
            $topLevel = if ($dollar -ge 0) { $rel.Substring(0, $dollar) } else { $rel }
            if (!$allowedTopLevels.Contains($topLevel)) {
                $stray += $entry
            }
        }
    }
    if ($stray.Count -gt 0) {
        throw "Stray non-mixin classes inside $Label mixin package ($pkgPrefix): $($stray -join ', ')"
    }
}

Write-Output "=== 2. 校验统一天命 JAR 结构与 Mixin 路由配置 ==="
$zip = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $names = @($zip.Entries | ForEach-Object FullName)
    $dups = @($names | Group-Object | Where-Object { $_.Count -gt 1 })
    if ($dups.Count -gt 0) { throw "Duplicate JAR entries in tianfu: $($dups.Name -join ', ')" }
    foreach ($req in @(
        'tianfu.mixins.json',
        'META-INF/neoforge.mods.toml',
        'net/guzhenren/tianfu/mixin/TianfuMixinPlugin.class',
        'net/guzhenren/tianfu/mixin/JingJieBaoCunProcedureMixin.class',
        'net/guzhenren/tianfu/mixin/JingJieBaoCunV2ProcedureMixin.class'
    )) {
        if ($names -notcontains $req) { throw "Missing required JAR entry in tianfu: $req" }
    }
    $mixinEntry = $zip.GetEntry('tianfu.mixins.json')
    $sr = [IO.StreamReader]::new($mixinEntry.Open(), [Text.UTF8Encoding]::new($false))
    try { $mixinJson = $sr.ReadToEnd() | ConvertFrom-Json } finally { $sr.Dispose() }
    if ($mixinJson.plugin -ne 'net.guzhenren.tianfu.mixin.TianfuMixinPlugin') {
        throw "Unexpected tianfu mixin plugin: $($mixinJson.plugin)"
    }
    if (($mixinJson.mixins -notcontains 'JingJieBaoCunProcedureMixin') -or ($mixinJson.mixins -notcontains 'JingJieBaoCunV2ProcedureMixin')) {
        throw 'Missing dual JingJieBaoCun mixins in tianfu.mixins.json'
    }
    Assert-MixinPackageHygiene $names $mixinJson '天命'
} finally {
    $zip.Dispose()
}
Write-Output ("Unified 天命 JAR OK: $jarPath | SHA256=" + (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash)

Write-Output "=== 3. 编译、打包并校验统一单包道途 JAR ($dtVersion) ==="
$javac = Join-Path $Jdk 'javac.exe'
$java = Join-Path $Jdk 'java.exe'
$bestProfile = $null
$bestCount = -1
foreach ($file in (Get-ChildItem (Join-Path $MinecraftDir 'versions') -Directory | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -File -Filter *.json })) {
    try {
        $profile = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
        $gameArgs = @($profile.arguments.game | Where-Object { $_ -is [string] })
        $mcIndex = [array]::IndexOf($gameArgs, '--fml.mcVersion')
        if ($gameArgs -contains '--fml.neoForgeVersion' -and $mcIndex -ge 0 -and $mcIndex + 1 -lt $gameArgs.Count -and $gameArgs[$mcIndex + 1] -eq '1.21.1' -and $profile.libraries.Count -gt $bestCount) {
            $bestProfile = $profile; $bestCount = $profile.libraries.Count
        }
    } catch { continue }
}
if ($null -eq $bestProfile) { throw 'No usable NeoForge 1.21.1 profile found.' }
$gameArgs = @($bestProfile.arguments.game | Where-Object { $_ -is [string] })
function ProfVal([string]$k) { return $gameArgs[[array]::IndexOf($gameArgs, $k) + 1] }
$neo = ProfVal '--fml.neoForgeVersion'
$mc = ProfVal '--fml.mcVersion'
$neoform = ProfVal '--fml.neoFormVersion'
$libs = Join-Path $MinecraftDir 'libraries'
$cp = [Collections.Generic.List[string]]::new()
foreach ($rel in @("net/neoforged/neoforge/$neo/neoforge-$neo-client.jar", "net/minecraft/client/$mc-$neoform/client-$mc-$neoform-srg.jar", "net/neoforged/neoforge/$neo/neoforge-$neo-universal.jar")) {
    $p = Join-Path $libs $rel
    if (Test-Path -LiteralPath $p -PathType Leaf) { $cp.Add($p) }
}
foreach ($lib in $bestProfile.libraries) {
    if ($null -ne $lib.downloads -and $null -ne $lib.downloads.artifact) {
        $p = Join-Path $libs $lib.downloads.artifact.path
        if (Test-Path -LiteralPath $p -PathType Leaf) { $cp.Add($p) }
    }
}
$base98 = Join-Path $repoRoot '_crash3\basever\guzhenren-9.8.jar'
if (!(Test-Path -LiteralPath $base98 -PathType Leaf)) { throw "Missing base jar: $base98" }
$cp.Add($base98)

$daotuBuild = Join-Path $daotuDir 'build'
$daotuOut = Join-Path $daotuBuild 'classes'
if (Test-Path -LiteralPath $daotuOut) { Remove-Item -LiteralPath $daotuOut -Recurse -Force }
New-Item -ItemType Directory -Force $daotuOut | Out-Null
$daotuSources = @(Get-ChildItem (Join-Path $daotuDir 'src\main\java') -Recurse -Filter *.java | Sort-Object FullName)
$argFile = Join-Path $daotuBuild 'daotu-javac-args.txt'
function Q([string]$s) { return '"' + $s.Replace('\','/').Replace('"','\"') + '"' }
$cpStr = $cp -join ';'
$argLines = @('-encoding', 'UTF-8', '-proc:none', '-d', (Q $daotuOut), '-cp', (Q $cpStr)) + @($daotuSources | ForEach-Object { Q $_.FullName })
[IO.File]::WriteAllLines($argFile, $argLines, [Text.UTF8Encoding]::new($false))
$daotuCompilerOut = & $javac ('@' + $argFile) 2>&1
if ($LASTEXITCODE -ne 0) {
    $daotuCompilerOut | Select-Object -Last 40
    throw "daotu compilation failed ($LASTEXITCODE)."
}

$daotuRes = Join-Path $daotuDir 'src\main\resources'
$daotuLibs = Join-Path $daotuBuild 'libs'
New-Item -ItemType Directory -Force $daotuLibs | Out-Null
$daotuJar = Join-Path $daotuLibs "道途-$dtVersion.jar"
if (Test-Path -LiteralPath $daotuJar) { Remove-Item -LiteralPath $daotuJar -Force }

$utf8NoBom = [Text.UTF8Encoding]::new($false)
$manifestText = "Manifest-Version: 1.0`r`nSpecification-Title: daotu`r`nSpecification-Version: 1`r`nImplementation-Title: daotu`r`nImplementation-Version: $dtVersion`r`n`r`n"
$zipDt = [IO.Compression.ZipFile]::Open($daotuJar, [IO.Compression.ZipArchiveMode]::Create)
try {
    $mfEntry = $zipDt.CreateEntry('META-INF/MANIFEST.MF', [IO.Compression.CompressionLevel]::Optimal)
    $mfStream = $mfEntry.Open()
    $mfWriter = [IO.StreamWriter]::new($mfStream, $utf8NoBom)
    try { $mfWriter.Write($manifestText) } finally { $mfWriter.Dispose() }
    foreach ($root in @($daotuOut, $daotuRes)) {
        foreach ($file in (Get-ChildItem -LiteralPath $root -Recurse -File | Sort-Object FullName)) {
            $name = $file.FullName.Substring($root.Length + 1).Replace('\', '/')
            if ($name -eq 'META-INF/MANIFEST.MF') { continue }
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipDt, $file.FullName, $name, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally {
    $zipDt.Dispose()
}

$zipDtRead = [IO.Compression.ZipFile]::OpenRead($daotuJar)
try {
    $dtNames = @($zipDtRead.Entries | ForEach-Object FullName)
    $dtDups = @($dtNames | Group-Object | Where-Object { $_.Count -gt 1 })
    if ($dtDups.Count -gt 0) { throw "Duplicate JAR entries in daotu: $($dtDups.Name -join ', ')" }
    foreach ($req in @(
        'daotu.mixins.json',
        'META-INF/neoforge.mods.toml',
        'net/guzhenren/daotu/mixin/DaotuMixinPlugin.class',
        'net/guzhenren/daotu/DaotuReincarnationHook.class',
        'net/guzhenren/daotu/mixin/JingJieBaoCunProcedureMixin.class',
        'net/guzhenren/daotu/mixin/JingJieBaoCunV2ProcedureMixin.class'
    )) {
        if ($dtNames -notcontains $req) { throw "Missing required JAR entry in daotu: $req" }
    }
    $dtMixinEntry = $zipDtRead.GetEntry('daotu.mixins.json')
    $srDt = [IO.StreamReader]::new($dtMixinEntry.Open(), $utf8NoBom)
    try { $dtMixinJson = $srDt.ReadToEnd() | ConvertFrom-Json } finally { $srDt.Dispose() }
    if ($dtMixinJson.plugin -ne 'net.guzhenren.daotu.mixin.DaotuMixinPlugin') {
        throw "Unexpected daotu mixin plugin: $($dtMixinJson.plugin)"
    }
    if (($dtMixinJson.mixins -notcontains 'JingJieBaoCunProcedureMixin') -or ($dtMixinJson.mixins -notcontains 'JingJieBaoCunV2ProcedureMixin')) {
        throw 'Missing dual JingJieBaoCun mixins in daotu.mixins.json'
    }
    Assert-MixinPackageHygiene $dtNames $dtMixinJson '道途'
} finally {
    $zipDtRead.Dispose()
}
Write-Output ("Unified 道途 JAR OK: $daotuJar ($($daotuSources.Count) files) | SHA256=" + (Get-FileHash -LiteralPath $daotuJar -Algorithm SHA256).Hash)

$routingProbe = @'
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.guzhenren.daotu.mixin.DaotuMixinPlugin;

public class DaotuRoutingCheck {
    static byte[] readJingJie(Path jar) throws Exception {
        String cls = "net/guzhenren/procedures/JingJieBaoCunProcedure.class";
        byte[] inner = null;
        try (ZipFile z = new ZipFile(jar.toFile())) {
            ZipEntry e = z.getEntry(cls);
            if (e != null) { try (InputStream in = z.getInputStream(e)) { return in.readAllBytes(); } }
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                if (ze.getName().startsWith("META-INF/jarjar/guzhenren-") && ze.getName().endsWith(".jar")) {
                    try (InputStream in = z.getInputStream(ze)) { inner = in.readAllBytes(); }
                    break;
                }
            }
        }
        if (inner == null) return null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(inner))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (cls.equals(ze.getName())) return zis.readAllBytes();
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        Map<String, Path> jars = new LinkedHashMap<>();
        jars.put("9.8", Path.of("_crash3/basever/guzhenren-9.8.jar"));
        jars.put("10.4", Path.of("_crash3/basever/guzhenren-10.4.jar"));
        jars.put("10.20", Path.of("_crash3/basever/guzhenren-10.20.jar"));
        jars.put("11.22", Path.of("D:\\daima\\mcmod\\蛊真人本体\\guzhenren蛊真人11.22-宗门家族优化.jar"));
        int checked = 0;
        for (var e : jars.entrySet()) {
            if (!Files.isRegularFile(e.getValue())) continue;
            checked++;
            var v = DaotuMixinPlugin.detectReincarnationVariant(readJingJie(e.getValue()));
            System.out.println("  daotu " + e.getKey() + " -> " + v);
            boolean expect6 = "11.22".equals(e.getKey());
            if ((expect6 && v != DaotuMixinPlugin.ReincarnationVariant.V2_6PARAM)
                    || (!expect6 && v != DaotuMixinPlugin.ReincarnationVariant.V1_3PARAM)) {
                System.exit(1);
            }
        }
        if (checked == 0) System.exit(1);
    }
}
'@
$probeFile = Join-Path $daotuBuild 'DaotuRoutingCheck.java'
[IO.File]::WriteAllText($probeFile, $routingProbe, $utf8NoBom)
Push-Location $repoRoot
try {
    & $java -cp "$daotuOut;$cpStr" $probeFile
    if ($LASTEXITCODE -ne 0) { throw "daotu 4-version routing check failed ($LASTEXITCODE)." }
} finally {
    Pop-Location
}

Write-Output "=== 4. 运行天命全部 JVM 单元测试 ==="
Push-Location $repoRoot
try {
    $env:TF_CLASSES = 'tianfu/build/classes/java/main'
    python docs\audit-1.4.18\_run_jvm_tests.py
    if ($LASTEXITCODE -ne 0) { throw "JVM tests failed ($LASTEXITCODE)." }
} finally {
    Pop-Location
}
