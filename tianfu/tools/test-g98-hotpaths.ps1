param([string]$TianfuJar = '', [string]$Label = 'candidate')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$report = Get-Content (Join-Path $root 'build\verification-g98\build-report.json') -Raw | ConvertFrom-Json
if (!$TianfuJar) { $TianfuJar = $report.jar }
$TianfuJar = (Resolve-Path -LiteralPath $TianfuJar).Path
if ($Label -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Unsafe report label.' }
$out = Join-Path $root 'build\hotpaths-g98'
New-Item -ItemType Directory -Force $out | Out-Null
$classpath = $TianfuJar + ';' + ($report.dependencies -join ';')
$source = Join-Path $root 'src\test\java\net\guzhenren\tianfu\HotPathRegression.java'
$jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin'
& (Join-Path $jdk 'javac.exe') -encoding UTF-8 -proc:none -cp $classpath -d $out $source
if ($LASTEXITCODE -ne 0) { throw 'Hot-path regression compilation failed.' }
$result = & (Join-Path $jdk 'java.exe') '-Xmx256M' '-Dfile.encoding=UTF-8' -cp ($out+';'+$classpath) net.guzhenren.tianfu.HotPathRegression 2>&1
$code = $LASTEXITCODE
$result | Set-Content -Encoding utf8 (Join-Path $out ($Label+'.txt'))
$result
if ($code -ne 0) { throw "Hot-path regression failed ($code)." }
