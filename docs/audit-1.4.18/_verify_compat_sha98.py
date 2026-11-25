# -*- coding: utf-8 -*-
"""只读：把 Guzhenren98Compatibility 里的 9 条 SHA-256 常量与 10.20 本体 jar 实测比对。"""
import zipfile, hashlib, re, pathlib

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
JAR = ROOT / "_crash3/basever/guzhenren-9.8.jar"
SRC = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/compat/Guzhenren98Compatibility.java"

src = SRC.read_text(encoding="utf-8")
# 从源码里抓 Map.entry("类名", "HASH") —— 避免手工转录出错
entries = re.findall(r'Map\.entry\("([^"]+)",\s*"([0-9A-Fa-f]{64})"\)', src)
print(f"从源码解析到 {len(entries)} 条常量\n")

z = zipfile.ZipFile(JAR)
names = set(z.namelist())
bad = []
for cls, expect in entries:
    res = cls + ".class"
    if res not in names:
        print(f"MISSING   {cls}")
        bad.append(cls)
        continue
    actual = hashlib.sha256(z.read(res)).hexdigest().upper()
    if actual == expect.upper():
        print(f"MATCH     {cls}")
    else:
        bad.append(cls)
        print(f"MISMATCH  {cls}")
        print(f"          10.20 = {actual}")
        print(f"          常量  = {expect.upper()}")

print(f"\n=== 不一致 {len(bad)}/{len(entries)} ===")
for b in bad:
    print("  -", b)

