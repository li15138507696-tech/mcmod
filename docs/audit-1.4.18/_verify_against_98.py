# -*- coding: utf-8 -*-
"""只读：把 1.4.18 各条改动所依赖的本体事实，在**用户实际使用的 9.8 jar** 上重新核对。

9.8 与 10.4/10.20 有三个类不同（UikongqiaoquedingProcedure / KE1Procedure /
UIxiuliananjianProcedure），其中 KE1Procedure 与 UIxiuliananjianProcedure 正是
「daohen_zong 每 tick 重算」与「人气系数 7.5E-4」两条结论的来源，必须在 9.8 上重验。
"""
import struct
import zipfile

JAR = r"D:\game\mcmod\1\.minecraft\versions\2\mods\guzhenren蛊真人模组本体9.8.jar"
z = zipfile.ZipFile(JAR)
names = set(z.namelist())


def has(cls, needle):
    res = cls + ".class"
    if res not in names:
        return None
    data = z.read(res)
    if isinstance(needle, str):
        needle = needle.encode("utf-8")
    return needle in data


def dbl(v):
    return struct.pack(">d", v)


print("=" * 78)
print("A. PlayerVariables：44 项道痕字段（该类 9.8 与 10.x 字节相同）")
print("=" * 78)
import re
pv = z.read("net/guzhenren/network/GuzhenrenModVariables$PlayerVariables.class")
# 从常量池抓字段名（以 UTF8 常量形式出现）
fields = set()
for m in re.finditer(rb"\x01\x00([\x00-\xff]{2})((?:daohen|dahen)_[a-z0-9_]{2,24})", pv):
    fields.add(m.group(2).decode())
try:
    import subprocess, tempfile, os
    env = dict(os.environ)
    out = subprocess.run(
        [r"C:\Program Files\Java\jdk-17\bin\javap.exe", "-p", "-classpath", JAR,
         "net.guzhenren.network.GuzhenrenModVariables$PlayerVariables"],
        capture_output=True, text=True, encoding="utf-8", errors="replace", env=env)
    f45 = sorted(set(re.findall(r"\b((?:daohen|dahen)_[a-z0-9_]+);", out.stdout)))
    print(f"  daohen_*/dahen_* 字段总数 = {len(f45)}（含汇总 daohen_zong）")
    print(f"  非汇总 = {len([f for f in f45 if f != 'daohen_zong'])}")
    lp = sorted(set(re.findall(r"\b((?:liupai|Liupai)_[a-z0-9_]+);", out.stdout)))
    print(f"  liupai_* 字段总数 = {len(lp)}")
except Exception as e:
    print("  javap 失败:", e)

print()
print("=" * 78)
print("B. KE1Procedure（9.8 版本）：daohen_zong 是否仍按 44 项重算")
print("=" * 78)
print("  引用 daohen_zong             :", has("net/guzhenren/procedures/KE1Procedure", "daohen_zong"))
print("  含 tizhi != 11 门（11.0 常量）:", has("net/guzhenren/procedures/KE1Procedure", dbl(11.0)))
try:
    out = subprocess.run(
        [r"C:\Program Files\Java\jdk-17\bin\javap.exe", "-p", "-c", "-classpath", JAR,
         "net.guzhenren.procedures.KE1Procedure"],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    body = out.stdout
    puts = body.count("putfield") 
    cnt = len(re.findall(r"Field net/guzhenren/network/GuzhenrenModVariables\$PlayerVariables\.((?:daohen|dahen)_[a-z0-9_]+):D", body))
    print(f"  字节码里出现的 daohen/dahen 字段引用次数 = {cnt}")
    print("  含 PUTFIELD daohen_zong    :", "daohen_zong" in body)
except Exception as e:
    print("  javap 失败:", e)

print()
print("=" * 78)
print("C. UIxiuliananjianProcedure（9.8）：人气系数常量")
print("=" * 78)
cls = "net/guzhenren/procedures/UIxiuliananjianProcedure"
print("  有 7.5E-4 (0.00075):", has(cls, dbl(0.00075)))
print("  有 1.0E-3 (0.001)  :", has(cls, dbl(0.001)))
print("  有 5.0E-4 (0.0005) :", has(cls, dbl(0.0005)))
print("  有 2.0E-4 (0.0002) :", has(cls, dbl(0.0002)))

print()
print("=" * 78)
print("D. BuffLiuCheng1Procedure：道痕互斥公式")
print("=" * 78)
cls = "net/guzhenren/procedures/BuffLiuCheng1Procedure"
print("  引用 daohen_zong :", has(cls, "daohen_zong"))
print("  有 1000.0 除数   :", has(cls, dbl(1000.0)))
print("  引用 ShangHai    :", has(cls, "ShangHai"))

print()
print("=" * 78)
print("E. UishuxingshijuetiProcedure：tizhi 11 = 至尊仙胎体")
print("=" * 78)
cls = "net/guzhenren/procedures/UishuxingshijuetiProcedure"
print("  有「至尊仙胎体」:", has(cls, "至尊仙胎体"))
print("  有 11.0 常量    :", has(cls, dbl(11.0)))
print("  有「太日阳莽体」:", has(cls, "太日阳莽体"))

print()
print("=" * 78)
print("F. 炼蛊：GuilianguwanchengProcedure 的窗口与完成判定")
print("=" * 78)
cls = "net/guzhenren/procedures/GuilianguwanchengProcedure"
for v, label in [(100.0, "== 100.0 完成判定"), (50.0, "窗口下界 50"), (70.0, "窗口上界 70"),
                 (20.0, "B 族下界 20"), (40.0, "B 族上界 40")]:
    print(f"  有 {label:<20}:", has(cls, dbl(v)))
print("  含「中途加材料」        :", has(cls, "中途加材料"))

print()
print("=" * 78)
print("G. 1.4.18 新代码引用的本体类在 9.8 是否都在")
print("=" * 78)
need = [
    "net/guzhenren/network/GuzhenrenModVariables$PlayerVariables",
    "net/guzhenren/procedures/UishuxingshijuetiProcedure",
    "net/guzhenren/procedures/BuffLiuCheng1Procedure",
    "net/guzhenren/procedures/ShangHaiLiuCheng1Procedure",
    "net/guzhenren/procedures/KE1Procedure",
    "net/guzhenren/procedures/UIxiuliananjianProcedure",
    "net/guzhenren/procedures/JingJieBaoCunProcedure",
    "net/guzhenren/item/YizhuanrendaoxiwangguItem",
]
missing = [n for n in need if n + ".class" not in names]
print("  缺失:", missing or "无")
