param([string]$Jdk = 'C:\Users\ADMIN\.jdks\temurin-21\bin')
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $project
try {
    $lines = Get-Content 'build/verification-universal/javac-args.txt'
    $index = [array]::IndexOf($lines, '-cp')
    if ($index -lt 0) { throw 'Run tools/build-universal.ps1 first.' }
    $cp = $lines[$index + 1].Trim('"') + ';build/classes-universal'
    & (Join-Path $Jdk 'javac.exe') -proc:none -encoding UTF-8 -cp $cp -d build/classes-universal-test src/universalTest/java/net/guzhenren/tianfu/UniversalRulesTest.java src/universalTest/java/net/guzhenren/tianfu/UniversalMigrationTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    foreach ($name in @('UniversalRulesTest', 'UniversalMigrationTest')) {
        & (Join-Path $Jdk 'java.exe') -cp ($cp + ';build/classes-universal-test') "net.guzhenren.tianfu.$name"
        if ($LASTEXITCODE -ne 0) { throw "$name failed." }
    }
} finally { Pop-Location }
