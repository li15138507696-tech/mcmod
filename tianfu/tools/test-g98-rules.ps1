$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$out = [IO.Path]::GetFullPath((Join-Path $root 'build\rules-g98'))
if ($out -ne (Join-Path $root 'build\rules-g98')) { throw 'Unsafe rule test output.' }
New-Item -ItemType Directory -Force $out | Out-Null
$javac = 'C:\Users\ADMIN\.jdks\temurin-21\bin\javac.exe'
$java = 'C:\Users\ADMIN\.jdks\temurin-21\bin\java.exe'
$sources = @(
    (Join-Path $root 'src\main\java\net\guzhenren\tianfu\compat\Guzhenren98Progression.java'),
    (Join-Path $root 'src\main\java\net\guzhenren\tianfu\compat\Guzhenren98Settlement.java'),
    (Join-Path $root 'src\test\java\net\guzhenren\tianfu\compat\Guzhenren98ProgressionTest.java'),
    (Join-Path $root 'src\test\java\net\guzhenren\tianfu\compat\Guzhenren98SettlementTest.java'))
& $javac -encoding UTF-8 -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Rule test compile failed.' }
foreach ($test in @('Guzhenren98SettlementTest', 'Guzhenren98ProgressionTest')) {
    & $java '-Dfile.encoding=UTF-8' -cp $out ('net.guzhenren.tianfu.compat.' + $test)
    if ($LASTEXITCODE -ne 0) { throw "Rule test failed: $test" }
}
