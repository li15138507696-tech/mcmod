# -*- coding: utf-8 -*-
"""只读验证（修正版）：炼蛊边界判定点 40 / 70 / 100 是否会被跨过。

关键点：本体 `== 40` / `== 70` / `== 100` 的判定，只有在**本体的 +1 恰好落在该值**时才执行。
附属的补点与本体 +1 订阅的是同一个 PlayerTickEvent.Post，**监听器顺序无保证**，
所以必须两种顺序都验证：
  - order="tianfu_first"：附属先补点，本体后 +1
  - order="base_first"  ：本体先 +1，附属后补点
只要任一顺序下本体漏掉边界点，就是缺陷（因为实际顺序可能正好是那一种）。
"""
import math

WIN = [(20.0, 40.0), (50.0, 70.0)]
BOUNDARY = [40.0, 70.0]


def in_window(cur, windows):
    return any(lo <= cur <= hi for lo, hi in windows)


def run(L, windows, near_fix, order, start=1.0, max_beats=5000):
    """返回：本体 +1 的落点集合（base_landings）。"""
    cur = start
    acc = 0.0
    base_landings = set()

    def base_step(c):
        n = min(100.0, c + 1.0)
        base_landings.add(n)
        return n

    def tianfu_step(c, acc):
        if in_window(c, windows):
            return c, min(acc, 0.999)
        near = near_fix and ((17.0 <= c < 20.0) or (47.0 <= c < 50.0))
        acc += L
        b = int(math.floor(acc))
        if near:
            b = min(b, 1)
        if b > 0:
            acc -= b
            c = min(99.0, c + b)
        return c, min(acc, 0.999)

    for _ in range(max_beats):
        if cur >= 100.0:
            break
        if order == "tianfu_first":
            cur, acc = tianfu_step(cur, acc)
            cur = base_step(cur)
        else:
            cur = base_step(cur)
            cur, acc = tianfu_step(cur, acc)
    return base_landings


print("=" * 92)
print("本体边界判定点是否被漏掉（漏 = 该点不在本体 +1 的落点集合里）")
print("=" * 92)

for order in ["tianfu_first", "base_first"]:
    print(f"\n### 监听器顺序：{order}")
    print(f"{'L':<6}{'旧逻辑(只冻结50-70)':<30}{'新逻辑(并集+临窗减速)':<30}")
    print("-" * 92)
    for L in [0.0, 0.4, 0.9, 1.0, 1.5, 2.2, 2.35, 2.5]:
        o = run(L, [(50.0, 70.0)], False, order)
        n = run(L, WIN, True, order)
        om = [b for b in BOUNDARY if b not in o]
        nm = [b for b in BOUNDARY if b not in n]
        o_s = f"漏 {om}" if om else "全部命中"
        n_s = f"漏 {nm}" if nm else "全部命中"
        print(f"{L:<6}{o_s:<30}{n_s:<30}")

print()
print("=" * 92)
print("投料窗口内可交互时长（本体 +1 的落点在该段内有几个 => ×1.5s）")
print("=" * 92)
print(f"{'L':<6}{'旧·20-40':<12}{'旧·50-70':<12}{'新·20-40':<12}{'新·50-70':<12}")
print("-" * 92)
for L in [0.0, 0.9, 2.2, 2.5]:
    row = [f"{L:<6}"]
    for windows, fix in [([(50.0, 70.0)], False), (WIN, True)]:
        land = run(L, windows, fix, "tianfu_first")
        s1 = sum(1 for k in range(20, 41) if float(k) in land)
        s2 = sum(1 for k in range(50, 71) if float(k) in land)
        row.append(f"{s1:>2}点/{s1*1.5:>4.1f}s".ljust(12))
        row.append(f"{s2:>2}点/{s2*1.5:>4.1f}s".ljust(12))
    print("".join(row))
