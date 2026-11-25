# -*- coding: utf-8 -*-
"""只读验证：突破成功率「旧口径（失败率乘算）」vs「新口径（加性 + P<=0 不生效）」。

本体：P = 1 - 0.1z - 0.1z^2 + 0.0002*yuanCap + 0.01*luck + k*renqi
      k = 7.5e-4 (mingge==5) / 5.0e-4 (其余)
      判定 Math.random() < P，本体不 clamp。
"""


def native_p(rank, renqi=0.0, yuan_cap=800.0, luck=10.0, destiny_five=False):
    k = 0.00075 if destiny_five else 0.0005
    return 1.0 - rank * 0.1 + renqi * k - rank * rank * 0.1 + yuan_cap * 0.0002 + luck * 0.01


def old_chance(p, r):
    """旧：先把 P 钳到 [0,1]，再按失败率乘算。"""
    c = min(1.0, max(0.0, p))
    return min(1.0, max(0.0, 1.0 - (1.0 - c) * max(0.0, 1.0 - r)))


def new_chance(p, r):
    """新：P<=0 天赋不生效；P>=1 必成；否则加性。"""
    if p <= 0.0:
        return 0.0
    if p >= 1.0:
        return 1.0
    return min(0.95, max(0.0, p + r))   # MAX_TALENT_CHANCE


print("=" * 96)
print("本体 P 的量级（yuanCap=800, luck=10, renqi=0, mingge!=5）")
print("=" * 96)
for z in [1, 2, 3, 4, 5]:
    print(f"  {z} 转：P = {native_p(z):+.3f}")

print()
print("=" * 96)
print("天赋加成后的实际成功率：旧 vs 新（加性百分点 r）")
print("=" * 96)
print(f"{'转数':<6}{'本体P':<10}{'r=0.30 旧':<12}{'r=0.30 新':<12}{'r=0.50 旧':<12}{'r=0.50 新':<12}")
print("-" * 96)
for z in [1, 2, 3, 4, 5]:
    p = native_p(z)
    print(f"{z:<6}{p:<+10.3f}"
          f"{old_chance(p,0.30):<12.3f}{new_chance(p,0.30):<12.3f}"
          f"{old_chance(p,0.50):<12.3f}{new_chance(p,0.50):<12.3f}")

print()
print("=" * 96)
print("关键结论：四转 P=-0.74（本体判定必失败）时，天赋还能不能撬开？")
print("=" * 96)
p4 = native_p(4)
for r in [0.20, 0.30, 0.50, 0.90, 1.50]:
    print(f"  Σr={r:<5}  旧 = {old_chance(p4, r):.3f}（= r，即'必失败'变成'r 的成功率'）"
          f"   新 = {new_chance(p4, r):.3f}（不生效）")

print()
print("=" * 96)
print("旧口径下的叠加风险（任务书担心的 100%）—— 新口径已被 CAP=0.50 杜绝")
print("=" * 96)
combos = [
    ("厚积薄发(0.20) + 突破通天(0.30)", 0.50),
    ("+ 修炼如神(0.40)", 0.90),
    ("+ 神·修炼无极(0.60)", 1.50),
    ("突破通天 + 极·众道至尊(0.80)", 1.10),
]
for name, r in combos:
    p = native_p(4)
    capped = min(r, 0.50)          # 新口径的硬封顶
    print(f"  {name:<42} Σr={r:<5.2f} 旧={old_chance(p,r):.3f}  新(封顶0.50)={new_chance(p,capped):.3f}")

