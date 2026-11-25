# -*- coding: utf-8 -*-
"""只读：生成「互斥族表」供人工确认，并校验表内每个 id 是否真实存在。

输出：族 → 类型 → 成员(id:名/价格/品质) / 未归属条目清单 / 拼写错误清单。
"""
import re
import pathlib

ROOT = pathlib.Path(r"D:\daima\mcmod\蛊真人附属")
TALENTS = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java"
POOL = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/TianfuPool.java"
MINDT = ROOT / "tianfu/src/main/java/net/guzhenren/tianfu/v2/TianfuMindTable.java"

tsrc = TALENTS.read_text(encoding="utf-8")
psrc = POOL.read_text(encoding="utf-8")

# id -> (name, quality, cost)
cat = {}
for tid, name, q, cost in re.findall(
        r'\bt\(\s*"([a-z0-9_]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(-?\d+)', tsrc):
    cat[tid] = (name, q, int(cost))
for tid, name, cost in re.findall(
        r'entry\("([a-z0-9_]+)",\s*"([^"]+)",\s*(?:[A-Z_]+|"[^"]+"),\s*(-?\d+)', psrc):
    cat.setdefault(tid, (name, "?", int(cost)))

mind = MINDT.read_text(encoding="utf-8")
existing = re.findall(r'Map\.entry\("([a-z0-9_]+)",\s*new Level\("([a-z]+)",\s*(\d+)\)\)', mind)

# ---------------------------------------------------------------- 建议族表
# ("族名", "类型", [成员 id...], "理由")
DAO_SINGLE = [
    ("jiandao", ["jiandao_qinhe", "jiandao_dacheng"]),
    ("leidao", ["leidao_qinhe", "leidao_dacheng"]),
    ("lidao", ["lidao_qinhe", "lidao_dacheng"]),
]
FAMILIES = [
    # ---- TIER：同类只留最高级 ----
    ("xiulian", "TIER",
     ["v2_mind_xiulian_1", "v2_mind_xiulian_2", "v2_mind_xiulian_3",
      "dazuo_wangwo", "kuxiu_buchuo", "suiyue_chendian", "suhui",
      "xiulian_rushen", "shen_xiulian_wuji"],
     "修炼速度。现有族只有 3 条，9 条旧条目在族外可无限叠加（实测可到 +530%）"),
    ("tupo", "TIER",
     ["houji_bofa", "tupo_tongtian"],
     "突破成功率。P1-1 从 xiulian_rushen/shen_xiulian_wuji/ji_zhongdao_zhizun 剥离后只剩这两条"),
    ("liangu", "TIER",
     ["v2_mind_liangu_1", "v2_mind_liangu_2",
      "liangu_rushen", "liangu_bujie", "shen_liangu_tianshu"],
     "炼蛊进度。现有族只有 2 条，另 3 条在族外"),
    ("cooldown", "TIER",
     ["v2_mind_cooldown_1", "v2_mind_cooldown_2", "gudao_dup", "v2_gu_cd_ningse"],
     "蛊虫冷却。P1 从 liangu_bujie/shen_liangu_tianshu 剥离 GU_CD 后并入此族"),
    ("capacity", "TIER",
     ["v2_mind_capacity_1", "v2_mind_capacity_2", "v2_mind_capacity_3",
      "niantou_kuozhan", "qiqiao_linglong", "shen_hunxi_zhuzai"],
     "念头容量"),
    ("recovery", "TIER",
     ["v2_mind_recovery_1", "v2_mind_recovery_2",
      "niantou_zisheng", "niantou_ruyuan", "niantou_bujie"],
     "念头恢复"),
    ("jingli", "TIER",
     ["jingli_shengheng", "jingli_ruhai"],
     "精力上限"),
    # ---- 道痕：每流派一族（只可选最高级）----
    ("daohen:jiandao", "TIER", ["jiandao_qinhe", "jiandao_dacheng"], "剑道：+200 → +800 二选一"),
    ("daohen:leidao", "TIER", ["leidao_qinhe", "leidao_dacheng"], "雷道：+200 → +800 二选一"),
    ("daohen:lidao", "TIER", ["lidao_qinhe", "lidao_dacheng"], "力道：+200 → +800 二选一"),
    # ---- EXCLUSIVE：只能持一个 ----
    ("daohen_main", "TIER",
     ["shen_wandao_guizong", "ji_zhongdao_zhizun"],
     "主修强化两极：+1500(8点) → +3000(16点)"),
    ("main_vs_single", "EXCLUSIVE",
     ["shen_wandao_guizong", "ji_zhongdao_zhizun",
      "jiandao_qinhe", "jiandao_dacheng", "jindao_qinhe", "shuidao_qinhe",
      "xuedao_qinhe", "lidao_qinhe", "lidao_dacheng", "mudao_qinhe",
      "yudao_qinhe", "fengdao_qinhe", "bingdao_qinhe", "jindao2_jinjie",
      "hundao_qinhe", "leidao_qinhe", "leidao_dacheng", "yandao_qinhe",
      "lvdao_qinhe", "tudao_qinhe"],
     "★ 任务书要求『两个主修强化天赋与所有普通主修道痕天赋互斥』—— 15 主修流的 19 条道痕天赋"),
    ("liupai_bundle", "EXCLUSIVE",
     ["shen_liupai_jianji", "bingxue_daoti", "hundao_ronghe", "qiyun_yizhi"],
     "神·流派兼济 12 点打包了单卖 15 点的三条，且可叠加 → 套利"),
    # ---- 负面：同属性只能选一条 ----
    ("neg_health", "EXCLUSIVE", ["v2_routi_canque", "routi_chanruo"], "生命上限负面"),
    ("neg_zhenyuan", "EXCLUSIVE", ["v2_zhenyuan_kubai", "zhenyuan_kujie"], "真元恢复负面"),
    ("neg_xiulian", "EXCLUSIVE", ["v2_xinzhi_yousun", "xiuxing_chizhi"], "修炼+突破负面"),
    ("neg_shouyuan", "EXCLUSIVE", ["v2_shouyuan_kuwei", "shouyuan_duancu"], "寿元负面"),
    ("neg_qiyun", "EXCLUSIVE", ["v2_qiyun_duanjue", "qiyun_xibo", "v2_luck_forfeit"], "气运负面（含归零）"),
    ("neg_niantou", "EXCLUSIVE", ["nianfu_xiayai", "niantou_ganhe"], "念头负面"),
    # ---- CAP：可叠但封顶 ----
    ("cap_reduction", "CAP", ["tiepi_rougu", "ruwei_dingshen", "jianjia_ruyan",
                              "jianjia_rushen", "shen_busi_bumie", "shen_jingang_buhuai",
                              "ji_wanshi_bumie"], "非护甲减伤，硬上限 0.50"),
    ("cap_dodge", "CAP", ["shanbu_suifeng", "shenfa_ruyan"], "闪避，硬上限 0.60"),
    ("cap_crit_rate", "CAP", ["baoji_rulie", "baoji_rushen", "shen_tianji_suan"], "暴击率，硬上限 0.50"),
    ("cap_crit_dmg", "CAP", ["baoji_rushen", "jianbi_qingfeng", "shen_tianji_suan"], "暴击伤害"),
    ("cap_health", "CAP", ["xueqi_rulin", "shen_bumie_jinshen", "ji_wanshi_bumie",
                           "jingu_qiangjian"], "生命上限（连乘已达 6.6×）"),
    ("cap_qiyun", "CAP", ["qiyun_weiying", "qiyun_yingmen", "qiyun_ruhong",
                          "qiyun_yizhi", "shen_qiyun_zhizi", "fuze_mianchang",
                          "qiyun_weiguang"], "气运上限"),
    ("cap_shouyuan", "CAP", ["chuntian_yishou", "shouyuan_ruhe", "shouyuan_mianchang"], "寿元正向上限"),
    ("cap_zhenyuan_regen", "CAP", ["zhenyuan_huiliu", "zhenyuan_ruquan", "zhenyuan_bujie"], "真元恢复"),
    ("cap_hunsoul", "CAP", ["hunpo_lianxi", "hunzai_kuanhou", "hunpo_rugui",
                            "shen_hunxi_zhuzai"], "魂魄上限/承载"),
]

print("=" * 100)
print("A. 现有互斥族（TianfuMindTable.LEVELS）")
print("=" * 100)
fam = {}
for tid, f, tier in existing:
    fam.setdefault(f, []).append((tid, int(tier)))
for f, members in fam.items():
    print(f"  {f:<10} {len(members)} 条: " + ", ".join(f"{t}(T{i})" for t, i in sorted(members, key=lambda x: x[1])))
print(f"  合计 {sum(len(v) for v in fam.values())} 条 / {len(fam)} 族")

print()
print("=" * 100)
print("B. 建议族表（校验后）")
print("=" * 100)
unknown = []
assigned = set()
rows = []
for name, typ, members, why in FAMILIES:
    good, bad = [], []
    for m in members:
        (good if m in cat else bad).append(m)
    unknown += [(name, b) for b in bad]
    assigned.update(good)
    rows.append((name, typ, good, bad, why))

for name, typ, good, bad, why in rows:
    names = ", ".join(f"{m}({cat[m][2]:+d}点)" for m in good)
    flag = f"  ❌ 未知 id: {bad}" if bad else ""
    print(f"\n[{typ}] {name}  —  {len(good)} 条{flag}")
    print(f"    理由: {why}")
    print(f"    成员: {names}")

print()
print("=" * 100)
print("C. 校验结果")
print("=" * 100)
print(f"  目录条目总数（可购）：{len(cat)}")
print(f"  被至少一个族覆盖：{len(assigned)}")
print(f"  拼写错误 / 不存在的 id：{len(unknown)}")
for name, b in unknown:
    print(f"    ❌ {name} -> {b}")

print()
print("=" * 100)
print("D. 未纳入任何族的条目（设计上应保持自由组合）")
print("=" * 100)
free = sorted(set(cat) - assigned)
print(f"  共 {len(free)} 条：")
line = "  "
for f in free:
    tag = f"{f}({cat[f][2]:+d})"
    if len(line) + len(tag) > 96:
        print(line)
        line = "  "
    line += tag + " "
print(line)
