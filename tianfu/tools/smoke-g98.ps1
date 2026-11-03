param(
    [string]$TianfuJar = '',
    [string]$NativeJar = 'D:\cxdownload\1\.minecraft\versions\转生到蛊界就要拿出真本事\mods\guzhenren蛊真人模组本体9.8.jar',
    [string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin',
    [string]$ProbeJar = '',
    [string[]]$RequiredDependencyJars = @(),
    [switch]$Run,
    [int]$StartupTimeoutSeconds = 180
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$serverSource = (Resolve-Path (Join-Path $project '..\_crash6\testserver')).Path
$runtime = [IO.Path]::GetFullPath((Join-Path $project 'build\runtime-g98'))
if (!$runtime.StartsWith((Join-Path $project 'build\'),[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe smoke output path.' }
if (!$TianfuJar) {
    $match = [regex]::Match([IO.File]::ReadAllText((Join-Path $project 'version.properties')),'(?m)^\s*version\s*=\s*(\S+)')
    if (!$match.Success) { throw 'No source version.' }
    $TianfuJar = Join-Path $project ('build\libs\tianfu-' + $match.Groups[1].Value + '-mc98.jar')
}
foreach ($path in @($TianfuJar,$NativeJar,(Join-Path $Jdk 'java.exe'))) { if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing smoke input: $path" } }
New-Item -ItemType Directory -Force (Join-Path $runtime 'mods') | Out-Null
if (Test-Path -LiteralPath (Join-Path $runtime 'smoke-running.json')) { throw 'Prior smoke marker exists. Verify the recorded process has exited before clearing it.' }
$allowedNames = @('guzhenren-exact98.jar','tianfu-candidate.jar')
if ($RequiredDependencyJars.Count -eq 0) {
    # Exact 9.8 metadata calls GeckoLib optional, but construction directly links
    # GeoBlockEntity and fails without it (verified isolated server evidence).
    $RequiredDependencyJars = @((Join-Path (Split-Path $NativeJar) 'geckolib-neoforge-1.21.1-4.7.7.jar'))
}
$dependencyInputs = @()
foreach ($dependency in $RequiredDependencyJars) {
    if (!(Test-Path -LiteralPath $dependency -PathType Leaf)) { throw "Required runtime dependency missing: $dependency" }
    $name = 'g98-dependency-' + [IO.Path]::GetFileName($dependency)
    $allowedNames += $name
    $dependencyInputs += [ordered]@{ jar=$dependency; name=$name; sha256=(Get-FileHash -LiteralPath $dependency).Hash }
}
if ($ProbeJar) {
    if (!(Test-Path -LiteralPath $ProbeJar -PathType Leaf)) { throw 'Probe JAR missing.' }
    $allowedNames += 'g98-probe.jar'
}
foreach ($file in (Get-ChildItem (Join-Path $runtime 'mods') -File)) { if ($file.Name -notin $allowedNames) { throw "Unexpected mod in isolated smoke: $($file.Name)" } }
Copy-Item -LiteralPath $NativeJar -Destination (Join-Path $runtime 'mods\guzhenren-exact98.jar') -Force
Copy-Item -LiteralPath $TianfuJar -Destination (Join-Path $runtime 'mods\tianfu-candidate.jar') -Force
if ($ProbeJar) { Copy-Item -LiteralPath $ProbeJar -Destination (Join-Path $runtime 'mods\g98-probe.jar') -Force }
foreach ($dependency in $dependencyInputs) { Copy-Item -LiteralPath $dependency.jar -Destination (Join-Path $runtime ('mods\'+$dependency.name)) -Force }
$absoluteLibraryRoot = [IO.Path]::GetFullPath((Join-Path $serverSource 'libraries'))
if (!(Test-Path -LiteralPath (Join-Path $absoluteLibraryRoot 'cpw\mods\bootstraplauncher\2.0.2\bootstraplauncher-2.0.2.jar'))) { throw 'Shared BootstrapLauncher JAR missing.' }
# Windows java launcher argument files may use the active native encoding. Keep paths
# ASCII and relative to this isolated working directory; never copy or mutate libraries.
$libraryRoot = [IO.Path]::GetRelativePath($runtime,$absoluteLibraryRoot).Replace('\','/')
if ($libraryRoot -match '[^\x00-\x7f]' -or [IO.Path]::GetFullPath((Join-Path $runtime $libraryRoot)) -ne $absoluteLibraryRoot) { throw 'Shared library path did not resolve safely to its read-only source.' }
$launchArgs = [IO.File]::ReadAllText((Join-Path $serverSource 'libraries\net\neoforged\neoforge\21.1.263\win_args.txt'))
$launchArgs = $launchArgs.Replace('libraries/',$libraryRoot+'/').Replace('-DlibraryDirectory=libraries',('-DlibraryDirectory='+$libraryRoot))
[IO.File]::WriteAllText((Join-Path $runtime 'launch-args.txt'),$launchArgs,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $runtime 'eula.txt'),"eula=true`n",[Text.UTF8Encoding]::new($false))
$properties = @('server-ip=127.0.0.1','server-port=0','enable-rcon=false','enable-query=false','enable-status=false','online-mode=false','level-name=smoke-world','level-type=minecraft:flat','generate-structures=false','gamemode=survival','difficulty=peaceful','spawn-protection=0','view-distance=3','simulation-distance=3','max-players=1','max-tick-time=120000','sync-chunk-writes=true')
[IO.File]::WriteAllLines((Join-Path $runtime 'server.properties'),$properties,[Text.UTF8Encoding]::new($false))
$manifest = [ordered]@{ nativeJar=$NativeJar; nativeSha256=(Get-FileHash -LiteralPath $NativeJar).Hash; tianfuJar=$TianfuJar; tianfuSha256=(Get-FileHash -LiteralPath $TianfuJar).Hash; sharedLibraries=$libraryRoot; bind='127.0.0.1:0'; otherMods=@(); proof='Fresh isolated server startup/shutdown only. Lazy mixin application and player behavior require separate checks.' }
$manifest.requiredRuntimeDependencies=$dependencyInputs
if ($ProbeJar) { $manifest.otherMods=@('tianfu98_probe'); $manifest.probeJar=$ProbeJar; $manifest.probeSha256=(Get-FileHash -LiteralPath $ProbeJar).Hash }
$manifest | ConvertTo-Json -Depth 3 | Set-Content -Encoding utf8 (Join-Path $runtime 'inputs.json')
Write-Output "Prepared isolated exact-9.8 server: $runtime"
if (!$Run) { Write-Output 'Prepared only. Add -Run after candidate approval to execute bounded startup and clean shutdown.'; return }
$log = Join-Path $runtime 'logs\latest.log'
if (Test-Path -LiteralPath $log) { Remove-Item -LiteralPath $log -Force }
$probeResultPath=Join-Path $runtime 'probe-result.json'
if (Test-Path -LiteralPath $probeResultPath) { Remove-Item -LiteralPath $probeResultPath -Force }
$startInfo = [Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = Join-Path $Jdk 'java.exe'
$startInfo.WorkingDirectory = $runtime
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
$startInfo.StandardErrorEncoding = [Text.Encoding]::UTF8
foreach ($argument in @('-Xms512M','-Xmx2048M','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','@launch-args.txt','nogui')) { $startInfo.ArgumentList.Add($argument) }
$process = [Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$ready = $false
$forced = $false
$exitCode = $null
$started = $false
$probeResult = $null
function ReadSharedText([string]$path) {
    $stream=[IO.FileStream]::new($path,[IO.FileMode]::Open,[IO.FileAccess]::Read,([IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete))
    $reader=[IO.StreamReader]::new($stream,[Text.Encoding]::UTF8)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
try {
    [void]$process.Start()
    $started = $true
    @{pid=$process.Id; startedUtc=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $runtime 'smoke-running.json')
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    while (!$process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        if ((Test-Path -LiteralPath $log) -and ((ReadSharedText $log) -match 'Done \([\d.]+s\)!')) { $ready=$true }
        if ($ProbeJar -and (Test-Path -LiteralPath $probeResultPath)) {
            try { $probeResult=[IO.File]::ReadAllText($probeResultPath)|ConvertFrom-Json } catch { $probeResult=$null }
        }
        if ($ready -and (!$ProbeJar -or ($null -ne $probeResult -and $probeResult.completed))) { break }
        Start-Sleep -Milliseconds 500
    }
    if (!$process.HasExited) {
        $process.StandardInput.WriteLine('stop')
        $process.StandardInput.Flush()
        if (!$process.WaitForExit(60000)) { $forced=$true; $process.Kill($true); $process.WaitForExit() }
    }
    $exitCode=$process.ExitCode
    [IO.File]::WriteAllText((Join-Path $runtime 'stdout.txt'),$stdout.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $runtime 'stderr.txt'),$stderr.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
} finally {
    if ($started -and !$process.HasExited) {
        try { $process.StandardInput.WriteLine('stop'); $process.StandardInput.Flush() } catch { }
        if (!$process.WaitForExit(15000)) { $process.Kill($true); $process.WaitForExit(); $forced=$true }
    }
    if ($started) {
        [IO.File]::WriteAllText((Join-Path $runtime 'stdout.txt'),$stdout.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
        [IO.File]::WriteAllText((Join-Path $runtime 'stderr.txt'),$stderr.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
    }
    if (Test-Path -LiteralPath (Join-Path $runtime 'smoke-running.json')) { Remove-Item -LiteralPath (Join-Path $runtime 'smoke-running.json') -Force }
    $process.Dispose()
}
$result = [ordered]@{ ready=$ready; exitCode=$exitCode; forcedTermination=$forced; nativeSha256=$manifest.nativeSha256; tianfuSha256=$manifest.tianfuSha256; proof='Server startup and process exit only; no player or lazy target acceptance.' }
if ($ProbeJar) { $result.probe=$probeResult; $result.proof='Server startup, native integration with named FakePlayers, and process exit. No real player, client, GUI, or visual acceptance.' }
$result | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $runtime 'result.json')
if (!$ready -or $forced -or $exitCode -ne 0) { throw "Smoke failed: ready=$ready exit=$exitCode forced=$forced. Inspect build/runtime-g98/stdout.txt and stderr.txt." }
if ($ProbeJar -and ($null -eq $probeResult -or !$probeResult.completed -or $probeResult.failed -ne 0)) { throw 'Native integration probe failed or did not complete. Inspect build/runtime-g98/probe-result.json and logs.' }
Write-Output 'Exact-9.8 isolated startup reached Done; stop completed with process exit 0.'
