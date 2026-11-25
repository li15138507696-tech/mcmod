param(
    [string]$TianfuJar = '',
    [string]$DaotuJar = '',
    [string[]]$Targets = @('10.4', '11.22', '9.8'),
    [string]$MinecraftDir = 'D:\game\mcmod\1\.minecraft',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin',
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repoRoot = (Resolve-Path (Join-Path $project '..')).Path
$daotuDir = Join-Path $repoRoot 'daotu'
$serverSource = (Resolve-Path (Join-Path $repoRoot '_crash6\testserver')).Path
$javaExe = Join-Path $Jdk 'java.exe'
$javacExe = Join-Path $Jdk 'javac.exe'
$utf8NoBom = [Text.UTF8Encoding]::new($false)

if (!$TianfuJar) {
    $m = [regex]::Match([IO.File]::ReadAllText((Join-Path $project 'version.properties'), [Text.Encoding]::UTF8), '(?m)^\s*version\s*=\s*([^\s#]+)')
    if (!$m.Success) { throw 'Cannot parse tianfu/version.properties' }
    $TianfuJar = Join-Path $project ('build\libs\天命-' + $m.Groups[1].Value + '.jar')
}
if (!$DaotuJar) {
    $m = [regex]::Match([IO.File]::ReadAllText((Join-Path $daotuDir 'version.properties'), [Text.Encoding]::UTF8), '(?m)^\s*version\s*=\s*([^\s#]+)')
    if (!$m.Success) { throw 'Cannot parse daotu/version.properties' }
    $DaotuJar = Join-Path $daotuDir ('build\libs\道途-' + $m.Groups[1].Value + '.jar')
}

foreach ($req in @($TianfuJar, $DaotuJar, $javaExe, $javacExe)) {
    if (!(Test-Path -LiteralPath $req -PathType Leaf)) {
        throw "Required file missing: $req"
    }
}

Write-Output '=== 1. 编译打包 UnifiedServerProbe 集成探针 ==='
$bestProfile = $null
$bestCount = -1
foreach ($file in (Get-ChildItem (Join-Path $MinecraftDir 'versions') -Directory | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -File -Filter *.json })) {
    try {
        $profile = [IO.File]::ReadAllText($file.FullName, [Text.Encoding]::UTF8) | ConvertFrom-Json
        $gameArgs = @($profile.arguments.game | Where-Object { $_ -is [string] })
        $mcIndex = [array]::IndexOf($gameArgs, '--fml.mcVersion')
        if ($gameArgs -contains '--fml.neoForgeVersion' -and $mcIndex -ge 0 -and ($mcIndex + 1) -lt $gameArgs.Count -and $gameArgs[$mcIndex + 1] -eq '1.21.1' -and $profile.libraries.Count -gt $bestCount) {
            $bestProfile = $profile
            $bestCount = $profile.libraries.Count
        }
    } catch {
        continue
    }
}
if ($null -eq $bestProfile) { throw "No usable NeoForge 1.21.1 profile found under $MinecraftDir" }

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
$base98Compile = Join-Path $repoRoot '_crash3\basever\guzhenren-9.8.jar'
if (!(Test-Path -LiteralPath $base98Compile -PathType Leaf)) { throw "Missing compile base jar: $base98Compile" }
$cp.Add($base98Compile)
$cp.Add($TianfuJar)
$cp.Add($DaotuJar)

$probeClasses = [IO.Path]::GetFullPath((Join-Path $project 'build\classes-unified-probe'))
if (!$probeClasses.StartsWith((Join-Path $project 'build\'), [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Unsafe probe output path.'
}
if (Test-Path -LiteralPath $probeClasses) { Remove-Item -LiteralPath $probeClasses -Recurse -Force }
New-Item -ItemType Directory -Force $probeClasses | Out-Null

$probeSources = @(Get-ChildItem (Join-Path $project 'src\unifiedProbe\java') -Recurse -Filter *.java | Sort-Object FullName)
if ($probeSources.Count -eq 0) { throw 'No unifiedProbe Java sources found.' }

$probeBuildDir = Join-Path $project 'build\verification-unified'
New-Item -ItemType Directory -Force $probeBuildDir | Out-Null
$argFile = Join-Path $probeBuildDir 'probe-javac-args.txt'
function Q([string]$s) { return '"' + $s.Replace('\', '/').Replace('"', '\"') + '"' }
$argLines = @('-encoding', 'UTF-8', '-proc:none', '-g', '-d', (Q $probeClasses), '-cp', (Q ($cp -join ';'))) + @($probeSources | ForEach-Object { Q $_.FullName })
[IO.File]::WriteAllLines($argFile, $argLines, $utf8NoBom)

$compilerOut = & $javacExe ('@' + $argFile) 2>&1
$compilerExit = $LASTEXITCODE
[IO.File]::WriteAllLines((Join-Path $probeBuildDir 'probe-javac.log'), @($compilerOut | ForEach-Object { $_.ToString() }), $utf8NoBom)
if ($compilerExit -ne 0) {
    $compilerOut
    throw "Unified probe compilation failed ($compilerExit)."
}

New-Item -ItemType Directory -Force (Join-Path $project 'build\libs') | Out-Null
$probeJar = Join-Path $project 'build\libs\unified-integration-probe.jar'
if (Test-Path -LiteralPath $probeJar) { Remove-Item -LiteralPath $probeJar -Force }
$zip = [IO.Compression.ZipFile]::Open($probeJar, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($root in @($probeClasses, (Join-Path $project 'src\unifiedProbe\resources'))) {
        foreach ($file in (Get-ChildItem -LiteralPath $root -Recurse -File | Sort-Object FullName)) {
            $entryName = $file.FullName.Substring($root.Length + 1).Replace('\', '/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $entryName, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally {
    $zip.Dispose()
}
Write-Output ("Compiled probe mod: $probeJar | SHA256=" + (Get-FileHash -LiteralPath $probeJar -Algorithm SHA256).Hash)

$targetSpecs = @{
    '10.4' = @{
        nativeJar = 'D:\daima\mcmod\蛊真人本体\guzhenrn蛊真人模组本体10.4.jar'
        expectedProfile = '10.4/10.20'
        expectedVariant = 'V1_3PARAM'
        extraDeps = @()
    }
    '11.22' = @{
        nativeJar = 'D:\daima\mcmod\蛊真人本体\guzhenren蛊真人11.22-宗门家族优化.jar'
        expectedProfile = '11.22'
        expectedVariant = 'V2_6PARAM'
        extraDeps = @()
    }
    '9.8' = @{
        nativeJar = 'D:\daima\mcmod\蛊真人本体\guzhenren蛊真人模组本体9.8.jar'
        expectedProfile = '9.8'
        expectedVariant = 'V1_3PARAM'
        extraDeps = @(
            'D:\game\mcmod\1\.minecraft\versions\2\mods\geckolib-neoforge-1.21.1-4.7.7.jar',
            'D:\game\mcmod\1\.minecraft\versions\2\mods\Pehkui-3.8.3+1.21-neoforge.jar'
        )
    }
}

$absoluteLibraryRoot = [IO.Path]::GetFullPath((Join-Path $serverSource 'libraries'))
if (!(Test-Path -LiteralPath (Join-Path $absoluteLibraryRoot 'cpw\mods\bootstraplauncher\2.0.2\bootstraplauncher-2.0.2.jar'))) {
    throw "Shared BootstrapLauncher JAR missing under $absoluteLibraryRoot"
}
$winArgsTemplate = [IO.File]::ReadAllText((Join-Path $serverSource 'libraries\net\neoforged\neoforge\21.1.263\win_args.txt'), $utf8NoBom)

function ReadSharedText([string]$path) {
    $stream = [IO.FileStream]::new($path, [IO.FileMode]::Open, [IO.FileAccess]::Read, ([IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete))
    $reader = [IO.StreamReader]::new($stream, [Text.Encoding]::UTF8)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

$summaryResults = [Collections.Generic.List[object]]::new()
$overallFailures = 0

foreach ($target in $Targets) {
    if (!$targetSpecs.ContainsKey($target)) { throw "Unknown target: $target" }
    $spec = $targetSpecs[$target]
    $nativeJar = $spec.nativeJar
    $expectedProfile = $spec.expectedProfile
    $expectedVariant = $spec.expectedVariant
    $extraDeps = @($spec.extraDeps)

    if (!(Test-Path -LiteralPath $nativeJar -PathType Leaf)) { throw "Missing native jar for $target : $nativeJar" }
    foreach ($dep in $extraDeps) {
        if (!(Test-Path -LiteralPath $dep -PathType Leaf)) { throw "Missing extra dependency for $target : $dep" }
    }

    $runtimeName = 'runtime-unified-' + $target
    $runtime = [IO.Path]::GetFullPath((Join-Path $project ("build\" + $runtimeName)))
    if (!$runtime.StartsWith((Join-Path $project 'build\'), [StringComparison]::OrdinalIgnoreCase)) {
        throw "Unsafe smoke runtime path: $runtime"
    }
    if (Test-Path -LiteralPath (Join-Path $runtime 'smoke-running.json')) {
        throw "Prior smoke-running.json exists for $target. Verify no stale java process is running before clearing it."
    }

    Write-Output "=== 2. 启动孤立服务端验证 [$target] (期望档位=$expectedProfile, 路由=$expectedVariant) ==="
    New-Item -ItemType Directory -Force $runtime | Out-Null
    foreach ($dirName in @('mods', 'logs', 'crash-reports', 'smoke-world')) {
        $p = Join-Path $runtime $dirName
        if (Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p -Recurse -Force }
    }
    foreach ($fileName in @('probe-result.json', 'result.json', 'stdout.txt', 'stderr.txt', 'inputs.json', 'launch-args.txt')) {
        $p = Join-Path $runtime $fileName
        if (Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p -Force }
    }

    $modsDir = Join-Path $runtime 'mods'
    New-Item -ItemType Directory -Force $modsDir | Out-Null
    Copy-Item -LiteralPath $nativeJar -Destination (Join-Path $modsDir 'guzhenren-target.jar') -Force
    Copy-Item -LiteralPath $TianfuJar -Destination (Join-Path $modsDir 'tianfu-unified.jar') -Force
    Copy-Item -LiteralPath $DaotuJar -Destination (Join-Path $modsDir 'daotu-unified.jar') -Force
    Copy-Item -LiteralPath $probeJar -Destination (Join-Path $modsDir 'unified-probe.jar') -Force
    foreach ($dep in $extraDeps) {
        Copy-Item -LiteralPath $dep -Destination (Join-Path $modsDir ('dep-' + [IO.Path]::GetFileName($dep))) -Force
    }

    $fromUri = [Uri]($runtime.TrimEnd('\', '/') + '\')
    $toUri = [Uri]$absoluteLibraryRoot
    $libraryRoot = [Uri]::UnescapeDataString($fromUri.MakeRelativeUri($toUri).ToString()).TrimEnd('/')
    if ($libraryRoot -match '[^\x00-\x7f]' -or [IO.Path]::GetFullPath((Join-Path $runtime $libraryRoot)) -ne $absoluteLibraryRoot) {
        throw "Unsafe or non-ASCII relative library path: $libraryRoot"
    }

    $launchArgs = $winArgsTemplate.Replace('libraries/', $libraryRoot + '/').Replace('-DlibraryDirectory=libraries', ('-DlibraryDirectory=' + $libraryRoot))
    [IO.File]::WriteAllText((Join-Path $runtime 'launch-args.txt'), $launchArgs, $utf8NoBom)
    [IO.File]::WriteAllText((Join-Path $runtime 'eula.txt'), "eula=true`n", $utf8NoBom)
    $properties = @(
        'server-ip=127.0.0.1',
        'server-port=0',
        'enable-rcon=false',
        'enable-query=false',
        'enable-status=false',
        'online-mode=false',
        'level-name=smoke-world',
        'level-type=minecraft:flat',
        'generate-structures=false',
        'gamemode=survival',
        'difficulty=peaceful',
        'spawn-protection=0',
        'view-distance=3',
        'simulation-distance=3',
        'max-players=1',
        'max-tick-time=120000',
        'sync-chunk-writes=true'
    )
    [IO.File]::WriteAllLines((Join-Path $runtime 'server.properties'), $properties, $utf8NoBom)

    $inputs = [ordered]@{
        target = $target
        expectedProfile = $expectedProfile
        expectedVariant = $expectedVariant
        nativeJar = $nativeJar
        nativeSha256 = (Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash
        tianfuJar = $TianfuJar
        tianfuSha256 = (Get-FileHash -LiteralPath $TianfuJar -Algorithm SHA256).Hash
        daotuJar = $DaotuJar
        daotuSha256 = (Get-FileHash -LiteralPath $DaotuJar -Algorithm SHA256).Hash
        probeJar = $probeJar
        probeSha256 = (Get-FileHash -LiteralPath $probeJar -Algorithm SHA256).Hash
        extraDeps = $extraDeps
        libraryRoot = $libraryRoot
    }
    [IO.File]::WriteAllText((Join-Path $runtime 'inputs.json'), ($inputs | ConvertTo-Json -Depth 4), $utf8NoBom)

    $logPath = Join-Path $runtime 'logs\latest.log'
    $probeResultPath = Join-Path $runtime 'probe-result.json'

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $javaExe
    $startInfo.WorkingDirectory = $runtime
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
    $startInfo.StandardErrorEncoding = [Text.Encoding]::UTF8
    $startInfo.Arguments = '-Xms512M -Xmx2048M -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 ' + ('"-Dunified.probe.expectedProfile=' + $expectedProfile + '" ') + '@launch-args.txt nogui'

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $ready = $false
    $forced = $false
    $exitCode = $null
    $started = $false
    $probeResult = $null
    $stdoutTask = $null
    $stderrTask = $null

    try {
        [void]$process.Start()
        $started = $true
        [IO.File]::WriteAllText((Join-Path $runtime 'smoke-running.json'), (@{ pid = $process.Id; startedUtc = [DateTime]::UtcNow.ToString('o') } | ConvertTo-Json), $utf8NoBom)
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)

        while (!$process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
            if ((Test-Path -LiteralPath $logPath) -and ((ReadSharedText $logPath) -match 'Done \([\d.]+s\)!')) {
                $ready = $true
            }
            if (Test-Path -LiteralPath $probeResultPath) {
                try {
                    $probeResult = [IO.File]::ReadAllText($probeResultPath, [Text.Encoding]::UTF8) | ConvertFrom-Json
                } catch {
                    $probeResult = $null
                }
            }
            if ($ready -and ($null -ne $probeResult) -and ($probeResult.completed -eq $true)) {
                break
            }
            Start-Sleep -Milliseconds 500
        }

        if (!$process.HasExited) {
            $process.StandardInput.WriteLine('stop')
            $process.StandardInput.Flush()
            if (!$process.WaitForExit(60000)) {
                $forced = $true
                try { $process.Kill() } catch { }
                try { & taskkill /PID $($process.Id) /T /F 2>$null | Out-Null } catch { }
                $process.WaitForExit()
            }
        }
        $exitCode = $process.ExitCode
    } finally {
        if ($started -and !$process.HasExited) {
            try { $process.StandardInput.WriteLine('stop'); $process.StandardInput.Flush() } catch { }
            if (!$process.WaitForExit(15000)) {
                $forced = $true
                try { $process.Kill() } catch { }
                try { & taskkill /PID $($process.Id) /T /F 2>$null | Out-Null } catch { }
                $process.WaitForExit()
            }
        }
        if ($started) {
            if ($null -ne $stdoutTask) {
                [IO.File]::WriteAllText((Join-Path $runtime 'stdout.txt'), $stdoutTask.GetAwaiter().GetResult(), $utf8NoBom)
            }
            if ($null -ne $stderrTask) {
                [IO.File]::WriteAllText((Join-Path $runtime 'stderr.txt'), $stderrTask.GetAwaiter().GetResult(), $utf8NoBom)
            }
        }
        if (Test-Path -LiteralPath (Join-Path $runtime 'smoke-running.json')) {
            Remove-Item -LiteralPath (Join-Path $runtime 'smoke-running.json') -Force
        }
        $process.Dispose()
    }

    if (Test-Path -LiteralPath $probeResultPath) {
        $probeResult = [IO.File]::ReadAllText($probeResultPath, [Text.Encoding]::UTF8) | ConvertFrom-Json
    }

    $logText = if (Test-Path -LiteralPath $logPath) { [IO.File]::ReadAllText($logPath, [Text.Encoding]::UTF8) } else { '' }
    if (!$ready -and ($logText -match 'Done \([\d.]+s\)!')) { $ready = $true }

    $expectedRoutingLines = if ($expectedVariant -eq 'V1_3PARAM') {
        @(
            '[天命] 转世钩子路由：检测到 V1_3PARAM → JingJieBaoCunProcedureMixin(3参)=true',
            '[天命] 转世钩子路由：检测到 V1_3PARAM → JingJieBaoCunV2ProcedureMixin(6参)=false',
            '[道途] 转世钩子路由：检测到 V1_3PARAM → JingJieBaoCunProcedureMixin(3参)=true',
            '[道途] 转世钩子路由：检测到 V1_3PARAM → JingJieBaoCunV2ProcedureMixin(6参)=false'
        )
    } else {
        @(
            '[天命] 转世钩子路由：检测到 V2_6PARAM → JingJieBaoCunProcedureMixin(3参)=false',
            '[天命] 转世钩子路由：检测到 V2_6PARAM → JingJieBaoCunV2ProcedureMixin(6参)=true',
            '[道途] 转世钩子路由：检测到 V2_6PARAM → JingJieBaoCunProcedureMixin(3参)=false',
            '[道途] 转世钩子路由：检测到 V2_6PARAM → JingJieBaoCunV2ProcedureMixin(6参)=true'
        )
    }

    $missingRouting = @()
    foreach ($line in $expectedRoutingLines) {
        if (!$logText.Contains($line)) { $missingRouting += $line }
    }
    $mixinRoutingOk = ($missingRouting.Count -eq 0)

    $expectedCompatLine = "[天命] 蛊真人本体校验通过：$expectedProfile（8 个机制类，契约全匹配）"
    $compatLogOk = $logText.Contains($expectedCompatLine)

    $mixinErrorMatches = @([regex]::Matches($logText, '(MixinApplyError|InvalidInjectionException|MixinTransformerError|Critical injection failure)'))
    $probeLogErrors = @([regex]::Matches($logText, '(UNIFIED_PROBE_FAIL|UNIFIED_PROBE_EXCEPTION)'))
    $probePassed = if ($null -ne $probeResult -and $probeResult.completed -eq $true) { [int]$probeResult.passed } else { 0 }
    $probeFailed = if ($null -ne $probeResult -and $probeResult.completed -eq $true) { [int]$probeResult.failed } else { -1 }

    $targetPassed = (
        $ready -and
        !$forced -and
        ($exitCode -eq 0) -and
        ($null -ne $probeResult) -and
        ($probeResult.completed -eq $true) -and
        ($probeFailed -eq 0) -and
        $mixinRoutingOk -and
        $compatLogOk -and
        ($mixinErrorMatches.Count -eq 0) -and
        ($probeLogErrors.Count -eq 0)
    )

    $targetReport = [ordered]@{
        target = $target
        passed = $targetPassed
        ready = $ready
        forcedTermination = $forced
        exitCode = $exitCode
        expectedProfile = $expectedProfile
        expectedVariant = $expectedVariant
        matchedProfile = if ($null -ne $probeResult) { $probeResult.matchedProfile } else { $null }
        mixinRoutingOk = $mixinRoutingOk
        missingRoutingLines = $missingRouting
        compatLogOk = $compatLogOk
        expectedCompatLine = $expectedCompatLine
        mixinErrors = $mixinErrorMatches.Count
        probeLogErrors = $probeLogErrors.Count
        probePassed = $probePassed
        probeFailed = $probeFailed
        runtimeDir = $runtime
    }
    [IO.File]::WriteAllText((Join-Path $runtime 'result.json'), ($targetReport | ConvertTo-Json -Depth 4), $utf8NoBom)
    $summaryResults.Add($targetReport)

    if (!$targetPassed) {
        $overallFailures++
        Write-Output ("FAIL [$target]: ready=$ready exit=$exitCode forced=$forced probePassed=$probePassed probeFailed=$probeFailed routingOk=$mixinRoutingOk compatOk=$compatLogOk mixinErrors=$($mixinErrorMatches.Count)")
    } else {
        Write-Output ("PASS [$target]: ready=$ready exit=$exitCode probe=$probePassed/0 routing=$expectedVariant compat=$expectedProfile")
    }
}

$summaryPath = Join-Path $project 'build\runtime-unified-summary.json'
$summaryObj = [ordered]@{
    generatedUtc = [DateTime]::UtcNow.ToString('o')
    tianfuJar = $TianfuJar
    daotuJar = $DaotuJar
    overallPassed = ($overallFailures -eq 0)
    targets = $summaryResults
}
[IO.File]::WriteAllText($summaryPath, ($summaryObj | ConvertTo-Json -Depth 6), $utf8NoBom)

if ($overallFailures -ne 0) {
    throw "Unified smoke failed for $overallFailures target(s). Inspect $summaryPath and build/runtime-unified-*/result.json"
}
Write-Output "All $($summaryResults.Count) isolated cross-version server verification targets PASSED."
