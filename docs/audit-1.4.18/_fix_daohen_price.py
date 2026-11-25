# -*- coding: utf-8 -*-
"""按「1 点 = 200 道痕（进位）」重排道痕天赋价格。

只改 `t("id", "name", "quality", <cost>, "desc", dao("<field>", <amount>))` 这类条目：
    cost = ceil(amount / 200)
+200 -> 1、+500 -> 3、+800 -> 4、+1200 -> 6

不碰：
  - `f(Op.DAOHEN_ALL, ...)`（已下架）
  - `f(Op.DAOHEN_MAIN, ...)`（主修强化走任务书指定价：1500->8 已是 ceil(7.5)=8；
    3000 任务书指定 16，高于线性 15，保留指定值）
其余条目（非道痕）不动。

用法：python _fix_daohen_price.py [--apply]
不加 --apply 只打印将发生的改动（默认干跑）。
"""
import math
import pathlib
import re
import sys

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
TAL = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java"

src = TAL.read_text(encoding="utf-8")

# 匹配单条 t(...) 定义：从 t( 到匹配的 ) 为止（含跨行）。
# 用非贪婪 + 逐个括号配平更稳，这里用「到 dao("...", N)) 或 )),」结尾」的近似。
ENTRY = re.compile(
    r'(?P<head>t\("(?P<id>[a-z0-9_]+)",\s*"[^"]*",\s*"(?P<q>[^"]*)",\s*)(?P<cost>-?\d+)(?P<mid>[^)]*?)'
    r'dao\("(?P<field>[a-z0-9_]+)",\s*(?P<amount>[0-9.]+)\)\)',
    re.S,
)

changes = []
out = []
pos = 0
for m in ENTRY.finditer(src):
    amount = float(m.group("amount"))
    old = int(m.group("cost"))
    new = int(math.ceil(amount / 200.0))
    out.append(src[pos:m.start()])
    out.append(m.group("head"))
    if new != old:
        out.append(str(new))
        changes.append((m.group("id"), amount, old, new))
    else:
        out.append(str(old))
    out.append(m.group("mid"))
    out.append(f'dao("{m.group("field")}", {m.group("amount")}))')
    pos = m.end()
out.append(src[pos:])
result = "".join(out)

print(f"扫描到道痕单条天赋：{len(ENTRY.findall(src))} 条")
print()
print(f"{'id':<24}{'道痕':>8}{'原价':>8}{'新价':>8}{'ceil(道痕/200)':>16}")
print("-" * 66)
for cid, amount, old, new in changes:
    print(f"{cid:<24}{amount:>8.0f}{old:>8}{new:>8}{amount/200.0:>16.1f}")
print("-" * 66)
print(f"需要改价：{len(changes)} 条")

# 反向校验：所有道痕条目改后是否都满足不变量
bad = []
for m in ENTRY.finditer(result):
    amount = float(m.group("amount"))
    cost = int(m.group("cost"))
    want = int(math.ceil(amount / 200.0))
    if cost != want:
        bad.append((m.group("id"), amount, cost, want))
print("不变量违反：", bad if bad else "无")

if "--apply" in sys.argv:
    TAL.write_text(result, encoding="utf-8")
    print("\n已写回 TianfuTalents.java")
else:
    print("\n干跑结束（加 --apply 才会写回）")
