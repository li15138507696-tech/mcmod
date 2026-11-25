# -*- coding: utf-8 -*-
"""只读：校验 TianfuInterlock.java 里出现的每个天赋 id 是否真实存在于目录。

抓 TianfuTalents.t(...) 与 TianfuPool.entry(...) 的 id 全集，再与族表成员比对。
"""
import re
import pathlib

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
TAL = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java"
POOL = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuPool.java"
INT = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/v2/TianfuInterlock.java"

cat = set()
for m in re.finditer(r'\bt\(\s*"([a-z0-9_]+)"', TAL.read_text(encoding="utf-8")):
    cat.add(m.group(1))
for m in re.finditer(r'\bentry\("([a-z0-9_]+)"', POOL.read_text(encoding="utf-8")):
    cat.add(m.group(1))
# 出身与体质不在同一命名空间，族表不涉及

src = INT.read_text(encoding="utf-8")
fams = re.findall(r'fam\("([^"]+)",\s*Kind\.(\w+),\s*"([^"]*)",\s*(.*?)\);', src, re.S)

print(f"目录 id 总数：{len(cat)}")
print(f"族数：{len(fams)}\n")

bad = []
total_members = 0
seen = {}
for fid, kind, label, body in fams:
    members = re.findall(r'"([a-z0-9_]+)"', body)
    total_members += len(members)
    for m in members:
        seen[m] = seen.get(m, 0) + 1
        if m not in cat:
            bad.append((fid, m))
    if len(members) != len(set(members)):
        dup = [x for x in set(members) if members.count(x) > 1]
        print(f"  ⚠ 族 {fid} 内有重复成员: {dup}")

print("=" * 90)
print("A. id 存在性校验")
print("=" * 90)
if bad:
    print(f"❌ {len(bad)} 个成员不在目录里：")
    for fid, m in bad:
        print(f"    {fid} -> {m}")
else:
    print(f"✅ 全部 {total_members} 个成员 id 均存在于目录")

print()
print("=" * 90)
print("B. 按类型统计")
print("=" * 90)
by_kind = {}
for fid, kind, label, body in fams:
    by_kind.setdefault(kind, []).append((fid, len(re.findall(r'"([a-z0-9_]+)"', body))))
for kind in ("TIER", "EXCLUSIVE", "CAP"):
    items = by_kind.get(kind, [])
    total = sum(n for _, n in items)
    print(f"  {kind:<10} {len(items):>2} 族 / {total:>3} 条")
    for fid, n in items:
        print(f"             - {fid:<22} {n} 条")

print()
print("=" * 90)
print("C. 跨族成员（一个 id 属多个族）")
print("=" * 90)
multi = {k: v for k, v in seen.items() if v > 1}
if multi:
    for k, v in sorted(multi.items()):
        print(f"  {k:<26} 属于 {v} 个族")
else:
    print("  无")

print()
print("=" * 90)
print("D. 族表覆盖 vs 目录")
print("=" * 90)
covered = set(seen)
print(f"  被族覆盖的 id：{len(covered)}")
print(f"  未覆盖（保持自由组合）：{len(cat - covered)}")
