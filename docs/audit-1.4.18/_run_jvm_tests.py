# -*- coding: utf-8 -*-
"""只读：编译并运行仓库里已有的纯 JVM 测试（main 入口），复用主编译的 classpath。

用途：1.4.18 改了 breakthroughChance / cultivation 等数值函数，
必须确认既有的 Guzhenren98ProgressionTest / Guzhenren98SettlementTest /
HotPathRegression 没有被改坏。
"""
import os
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
ARGS = ROOT / (os.environ.get('TF_ARGS','_crash3/javac_out_1418/args.txt'))
CLASSES = ROOT / (os.environ.get('TF_CLASSES','_crash3/javac_out_1418'))
OUT = ROOT / (os.environ.get('TF_JTEST','_crash3/jtest_out'))
JAVAC = r"C:\Users\ADMIN\.jdks\temurin-21\bin\javac.exe"
JAVA = r"C:\Users\ADMIN\.jdks\temurin-21\bin\java.exe"

text = ARGS.read_text(encoding="utf-8").splitlines()
cp = None
for i, line in enumerate(text):
    if line.strip() == "-cp":
        cp = text[i + 1]
        break
if cp is None:
    sys.exit("没找到 -cp 行")

tests = sorted((ROOT / "tianfu/src/test/java").rglob("*.java"))
print(f"测试源文件：{len(tests)}")
OUT.mkdir(parents=True, exist_ok=True)

r = subprocess.run(
    [JAVAC, "-encoding", "UTF-8", "-proc:none", "-d", str(OUT),
     "-cp", str(CLASSES) + ";" + cp] + [str(t) for t in tests],
    capture_output=True, text=True, encoding="utf-8", errors="replace")
print("javac exit:", r.returncode)
if r.returncode != 0:
    print(((r.stdout or "") + (r.stderr or ""))[-4000:])
    sys.exit(r.returncode)

fail = 0
SRC_ROOT = ROOT / "tianfu/src/test/java"
for t in tests:
    # 用文件路径推出全限定名（测试类都在包里）
    name = t.relative_to(SRC_ROOT).with_suffix("").as_posix().replace("/", ".")
    run = subprocess.run(
        [JAVA, "-Dstdout.encoding=UTF-8", "-cp", str(OUT) + ";" + str(CLASSES) + ";" + cp, name],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = ((run.stdout or "") + (run.stderr or "")).strip()
    print(f"\n===== {name} (exit {run.returncode}) =====")
    print(out[-2500:] if out else "(无输出)")
    if run.returncode != 0:
        fail += 1

print()
print("=" * 60)
print(f"测试类 {len(tests)} 个，失败 {fail} 个")
print("=" * 60)
sys.exit(1 if fail else 0)

