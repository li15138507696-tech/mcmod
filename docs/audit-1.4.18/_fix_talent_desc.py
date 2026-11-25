# -*- coding: utf-8 -*-
"""P1-8 机械部分：把道痕天赋描述里的「熟练度效率 +N%」改成「熟练度 +N」。

依据：`addPaired` 实际写的是 `liupai_* += amount/10`（**平铺熟练度点**，不是百分比效率），
而 N（20/50/80/120）恰好等于 道痕/10，所以只是把单位说法改对，不动任何数值。

用法：python _fix_talent_desc.py [--apply]
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
TAL = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java"
src = TAL.read_text(encoding="utf-8")

PATTERN = re.compile(r"熟练度效率 \+(\d+)%")
found = PATTERN.findall(src)
print(f"命中「熟练度效率 +N%」：{len(found)} 处 -> {sorted(set(int(x) for x in found))}")

# 「该流派熟练度效率 +20%，不直接增伤」-> 「该流派熟练度 +20（不直接增伤）」
def repl(m):
    return "熟练度 +" + m.group(1)

result = PATTERN.sub(repl, src)

# 顺带把紧随其后的「，不直接增伤」改成括注，读起来不再是并列效果
result = result.replace("熟练度 +(\\d)，不直接增伤", "熟练度 +\\1（不直接增伤）")
result = re.sub(r"熟练度 \+(\d+)，不直接增伤", r"熟练度 +\1（不直接增伤）", result)

left = PATTERN.findall(result)
print(f"替换后残留：{len(left)} 处")
print("改动行数：", sum(1 for a, b in zip(src.splitlines(), result.splitlines()) if a != b))

if "--apply" in sys.argv:
    TAL.write_text(result, encoding="utf-8")
    print("已写回")
else:
    print("干跑结束（加 --apply 写回）")
