# -*- coding: utf-8 -*-
"""只读：扫描本体所有「中途加材料」投料窗口过程的阈值常量。

方法：class 文件里 ldc2_w 指向常量池 Double，CP 中 Double 按 IEEE754 8 字节原样存储，
所以直接在 class 字节里搜 8 字节模式即可判定「该常量是否被引用」。
用来回答：是否所有蛊方的投料窗口都是 50~70，还是存在 20~40 之类的其它区间。
"""
import zipfile, struct, collections

JAR = r"D:\daima\mcmod\蛊真人附属\_crash3\basever\guzhenren-10.20.jar"
MARK = "中途加材料".encode("utf-8")

CANDS = [20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
PATS = {c: struct.pack(">d", c) for c in CANDS}

z = zipfile.ZipFile(JAR)
hits = {}
for n in z.namelist():
    if not n.endswith(".class"):
        continue
    d = z.read(n)
    if MARK not in d:
        continue
    present = [c for c in CANDS if PATS[c] in d]
    hits[n] = present

print(f"含「中途加材料」判定点的类：{len(hits)}\n")

sig = collections.Counter()
for n, present in hits.items():
    sig[tuple(present)] += 1

print("=== 阈值组合分布 ===")
for combo, cnt in sorted(sig.items(), key=lambda kv: -kv[1]):
    print(f"  {list(combo)!s:<45} {cnt} 个类")

print("\n=== 不含 70.0 的类（若存在，说明该蛊方窗口不是 50~70）===")
odd = {n: p for n, p in hits.items() if 70.0 not in p}
if not odd:
    print("  无 —— 全部含 70.0")
else:
    for n, p in sorted(odd.items()):
        print(f"  {n.split('/')[-1]:<55} {p}")

print("\n=== 含 40.0 但不含 70.0（疑似 20~40 区间）===")
cand = {n: p for n, p in hits.items() if 40.0 in p and 70.0 not in p}
print("  无" if not cand else "")
for n, p in sorted(cand.items()):
    print(f"  {n.split('/')[-1]:<55} {p}")

print("\n=== 抽样：几个具体类 ===")
for n in sorted(hits)[:6]:
    print(f"  {n.split('/')[-1]:<55} {hits[n]}")
