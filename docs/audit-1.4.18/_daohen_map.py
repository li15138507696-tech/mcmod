# -*- coding: utf-8 -*-
"""只读：把 44 个道痕字段与天赋 id / 数值 / 价格 / 主修资格 交叉成表。
数据源：
  1) 本体 PlayerVariables 字段（javap 产物 _raw_playervars.txt，权威）
  2) 本体 KE1Procedure 里 daohen_zong 的求和清单（判断字段是否被纳入汇总）
  3) 附属 TianfuTalents.java 的 dao(...) 调用（天赋 -> 字段 + 数值）
  4) 附属 TianfuMainDao.KEYS（15 项主修候选）
只读，不写任何项目文件，结果打印到 stdout。
"""
import re
import pathlib

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
PV = ROOT / "docs/audit-1.4.18/_raw_playervars.txt"
TALENTS = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java"
MAIN = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuMainDao.java"
KE1 = ROOT / "_tmp_vf_out3/net/guzhenren/procedures/KE1Procedure.java"

# ---- 1) 本体字段 ----
pv = PV.read_text(encoding="utf-8", errors="replace")
fields = sorted(set(re.findall(r"\b((?:daohen|dahen)_[a-z0-9_]+);", pv)))
daohen44 = [f for f in fields if f != "daohen_zong"]
liupai = sorted(set(re.findall(r"\b((?:liupai|Liupai)_[a-z0-9_]+);", pv)))

# ---- 2) 汇总求和清单 ----
ke1 = KE1.read_text(encoding="utf-8", errors="replace")
m = re.search(r"daohen_zong\s*=\s*(.*?);", ke1, re.S)
summed = re.findall(r"\)\.((?:daohen|dahen)_[a-z0-9_]+)", m.group(1)) if m else []

# ---- 3) 天赋 ----
src = TALENTS.read_text(encoding="utf-8")
tal = re.findall(
    r't\("([a-z0-9_]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(-?\d+),\s*"[^"]*",\s*dao\("((?:daohen|dahen)_[a-z0-9_]+)",\s*([0-9.]+)\)',
    src,
)

# ---- 4) 主修候选 ----
main = MAIN.read_text(encoding="utf-8")
mk = re.search(r"KEYS\s*=\s*java\.util\.List\.of\((.*?)\);", main, re.S)
main_keys = re.findall(r'"([a-z0-9_]+)"', mk.group(1)) if mk else []

by_field = {}
for tid, name, q, cost, field, amt in tal:
    by_field.setdefault(field, []).append((tid, name, q, int(cost), float(amt)))

def suffix(f):
    return f.split("_", 1)[1]

print("=" * 100)
print("A. 字段盘点")
print("=" * 100)
print(f"daohen_*/dahen_* 字段总数      : {len(fields)}  (含汇总 daohen_zong)")
print(f"44 项真实道痕字段              : {len(daohen44)}")
print(f"liupai_* 字段数                : {len(liupai)}")
print(f"KE1Procedure 汇总求和项数      : {len(summed)}  唯一: {len(set(summed))}")
print(f"无 daohen 字段的 liupai 流派   : {sorted(set(l.split('_',1)[1] for l in liupai if l.lower().startswith('liupai_')) - set(suffix(f) for f in daohen44))}")
print(f"主修候选 (TianfuMainDao.KEYS)  : {len(main_keys)} -> {main_keys}")
print(f"特殊流派 = 44 - 15            : {len(daohen44) - len(main_keys)}")

print()
print("=" * 100)
print("B. 44 项道痕 x 天赋 映射表")
print("=" * 100)
hdr = f"{'#':<3}{'本体字段':<24}{'流派':<14}{'前缀':<9}{'汇总?':<7}{'主修?':<7}{'天赋 id':<24}{'量':<7}{'价':<4}{'品质'}"
print(hdr)
print("-" * 100)
missing, dup = [], []
for i, f in enumerate(sorted(daohen44, key=lambda x: suffix(x)), 1):
    ent = by_field.get(f, [])
    sfx = suffix(f)
    is_main = sfx in main_keys
    if not ent:
        missing.append(f)
        print(f"{i:<3}{f:<24}{sfx:<14}{f.split('_')[0]:<9}{'-':<7}{'':<7}{'*** 无天赋 ***'}")
        continue
    if len(ent) > 1:
        dup.append(f)
    for j, (tid, name, q, cost, amt) in enumerate(sorted(ent, key=lambda e: e[4])):
        tag = "" if j == 0 else "  ^同级第二档"
        print(f"{i:<3}{f:<24}{sfx:<14}{f.split('_')[0]:<9}"
              f"{'Y' if f in summed else 'N':<7}{'Y' if is_main else '-':<7}"
              f"{tid:<24}{amt:<7.0f}{cost:<4}{q}{tag}")

print()
print("=" * 100)
print("C. 覆盖性与异常")
print("=" * 100)
print(f"无任何天赋的字段            : {missing if missing else '无'}")
print(f"有天赋的字段数              : {len(by_field)}/{len(daohen44)}")
print(f"未纳入 daohen_zong 汇总的字段: {[f for f in daohen44 if f not in summed] or '无'}")
print(f"一字段多档位的流派          : {dup or '无'}")
print(f"天赋条数(dao 调用)          : {len(tal)}")

print()
print("=" * 100)
print("D. 价格分布（现价 vs 任务书规定梯度）")
print("=" * 100)
want = {200: 1, 500: 3, 800: 4, 1200: 6}
agg = {}
for tid, name, q, cost, field, amt in tal:
    agg.setdefault((float(amt), int(cost)), []).append(tid)
for (amt, cost), ids in sorted(agg.items()):
    exp = want.get(int(amt), "?")
    flag = "OK" if exp == cost else f"不符(应为{exp})"
    print(f"+{amt:<7.0f} 现价 {cost:<3} 任务书 {exp!s:<4} {flag:<12} 条数 {len(ids):<3} 例: {ids[0]}")

print()
print("=" * 100)
print("E. 15 项主修流派的天赋覆盖")
print("=" * 100)
for k in main_keys:
    fs = [f for f in daohen44 if suffix(f) == k]
    if not fs:
        print(f"{k:<12} *** 无对应道痕字段 ***")
        continue
    for f in fs:
        ents = by_field.get(f, [])
        print(f"{k:<12} {f:<22} 天赋: "
              + (", ".join(f"{t[0]}({t[4]:.0f}/{t[3]}点)" for t in ents) or "无"))

