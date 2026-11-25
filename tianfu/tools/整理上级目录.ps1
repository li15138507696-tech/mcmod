$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = [IO.Path]::GetFullPath('D:\daima\mcmod\蛊真人附属')
$names = @('_tmp_vf','_tmp_vf_cls','_tmp_vf_cls2','_tmp_vf_out','_tmp_vf_out2','_tmp_vf_out3','_tmp_vf_out4','_tmp_vf_out5','_tmp_extract.py')
$targets = @()
foreach ($name in $names) {
    $path = [IO.Path]::GetFullPath((Join-Path $root $name))
    if (!$path.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw "路径超出指定目录：$path" }
    if (!(Test-Path -LiteralPath $path)) { continue }
    $resolved = (Resolve-Path -LiteralPath $path).Path
    if ($resolved -ne $path) { throw "路径被重定向：$path" }
    $item = Get-Item -LiteralPath $path -Force
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw "跳过链接：$path" }
    $targets += $path
}
$note = Join-Path $root '目录用途说明.md'
if (Test-Path -LiteralPath $note) { throw '已有同名目录说明，请先检查，避免覆盖。' }
$text = @'
# 蛊真人附属目录说明

- `tianfu/`：天命工程，源码在 `tianfu/src/`，打包成果集中在 `tianfu/build/libs/`；该工程内另有《目录说明》和《成果说明》。
- `daotu/`：此上级目录中的道途历史源码和资料。当前单独的道途工程位于 `D:\daima\mcmod\道途附属`，两者不要混淆。
- `libs/`：构建依赖，包含 Veil；天命构建会引用。
- `_crash3/`：历史排错和构建工具；天命默认本体依赖引用其中的文件，请保留。
- `_crash6/`：历史检查工具和隔离测试服务端资料；天命的服务端验证脚本引用此处，请保留。本次未启动服务端。
- `_crash/`、`_crash_inbox/`、`_crash2/`、`_crash4/`、`_crash5/`：历史崩溃日志及启动信息，可在不再需要排错记录时另行归档。
- `_reverse/`：历史反编译资料和工具；`_tmp_verify/`：旧验证数据及工具，可能被其他构建脚本引用。
- `_tools/`：构建辅助配置；`docs/`：项目审计资料；`logs/`：历史运行日志。
- `.gradle/`：Gradle 缓存；`.vscode/`、`.trae/`、`.dsh/`、`.dsh-plugins/`：编辑器或工具配置；`.git/`：版本历史。
- `_ftbcheck/`：历史检查目录，当前为空。

2026-11-23：已清理 `_tmp_vf*` 临时反编译目录与对应的 `_tmp_extract.py`。源码、成果、构建依赖及历史排错资料予以保留。
'@
[IO.File]::WriteAllText($note,$text,[Text.UTF8Encoding]::new($false))
foreach ($path in $targets) { Remove-Item -LiteralPath $path -Recurse -Force }
foreach ($path in $targets) { if (Test-Path -LiteralPath $path) { throw "清理未完成：$path" } }
Write-Output "已清理 $($targets.Count) 个临时目录/文件；已创建：$note"
