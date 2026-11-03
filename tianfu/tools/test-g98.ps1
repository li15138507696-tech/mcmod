param([string]$TianfuJar = '', [switch]$Run)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$build=Join-Path $project 'build'
$reportPath=Join-Path $build 'verification-g98\build-report.json'
if (!(Test-Path -LiteralPath $reportPath)) { throw 'Run tools/build-g98.ps1 first.' }
$report=[IO.File]::ReadAllText($reportPath)|ConvertFrom-Json
if (!$TianfuJar) { $TianfuJar=$report.jar }
if (!(Test-Path -LiteralPath $TianfuJar -PathType Leaf)) { throw 'Candidate JAR missing.' }
$out=[IO.Path]::GetFullPath((Join-Path $build 'classes-test-g98'))
if ($out -ne (Join-Path $project 'build\classes-test-g98')) { throw 'Unsafe integration compiler output path.' }
if (Test-Path -LiteralPath $out) { Remove-Item -LiteralPath $out -Recurse -Force }
New-Item -ItemType Directory -Force $out|Out-Null
$classpath=$TianfuJar+';'+($report.dependencies -join ';')
$sources=@(Get-ChildItem (Join-Path $project 'src\g98Test\java') -Recurse -Filter *.java|Sort-Object FullName)
$argsPath=Join-Path $build 'verification-g98\probe-javac-args.txt'
function QuoteArg([string]$value) { '"'+$value.Replace('\','/').Replace('"','\"')+'"' }
$argsLines=@('-encoding','UTF-8','-proc:none','-g','-d',(QuoteArg $out),'-cp',(QuoteArg $classpath))+@($sources|ForEach-Object { QuoteArg $_.FullName })
[IO.File]::WriteAllLines($argsPath,$argsLines,[Text.UTF8Encoding]::new($false))
$compiler=& 'C:\Users\ADMIN\.jdks\temurin-21\bin\javac.exe' ('@'+$argsPath) 2>&1
$code=$LASTEXITCODE
$compiler|Set-Content -Encoding utf8 (Join-Path $build 'verification-g98\probe-javac.log')
if($code -ne 0){$compiler;throw "Integration probe compilation failed ($code)."}
$jar=Join-Path $build 'libs\tianfu-g98-integration-probe.jar'
if(Test-Path -LiteralPath $jar){Remove-Item -LiteralPath $jar -Force}
$zip=[IO.Compression.ZipFile]::Open($jar,[IO.Compression.ZipArchiveMode]::Create)
try {
    foreach($root in @($out,(Join-Path $project 'src\g98Test\resources'))){
        foreach($file in (Get-ChildItem -LiteralPath $root -Recurse -File)){
            $name=$file.FullName.Substring($root.Length+1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$name,[IO.Compression.CompressionLevel]::Optimal)|Out-Null
        }
    }
}finally{$zip.Dispose()}
Write-Output "Compiled isolated test mod: $jar"
if($Run){& (Join-Path $PSScriptRoot 'smoke-g98.ps1') -TianfuJar $TianfuJar -NativeJar $report.nativeJar -ProbeJar $jar -Run}
else{Write-Output 'Compile only. Add -Run after the final candidate is ready to run the isolated native integration tests.'}
