# 道痕体系审查报告（T1 / 第一阶段）

- 审查对象：`tianfu` 1.4.17（`tianfu\version.properties` → `version=1.4.17`）
- 本体：10.20 内层 jar `_crash3\basever\guzhenren-10.20.jar`
  - sha256 `B6D314D3810B8BC5FCFDBC4F588DB8721F0EFDFC14D27A0A06D88143A01FE2BC`
  - 36880 entries / **22371 class**，与 `_reverse\nested_1020\guzhenren-0.1-neoforge-1.21.1.jar` 字节完全相同
- 环境：Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- 本报告**只读**，未修改任何源码、构建或版本文件。

---

## 0. 结论摘要（先看这 10 条）

| # | 结论 | 严重度 |
|---|---|---|
| 1 | 本体道痕互斥**已完整实现**，公式在 `ShangHaiLiuCheng1Procedure:27-30` 与 `BuffLiuCheng1Procedure:46-54`，靠 `daohen_zong` 做「异道总和」。**必须复用，不得另造。** | — |
| 2 | `daohen_zong` 由 `KE1Procedure:846-889` 每 tick 用 **44 个字段**重新求和覆盖（`tizhi != 11` 时）。→ **任何对 `daohen_zong` 的直接写入都会被下一次 tick 抹掉。** | 致命 |
| 3 | 因此 `zonghen_chubei`（总痕储备，天/5 点）**完全无效**，是纯亏 5 点。 | 高 |
| 4 | `wuxing_daocang` / `shen_wandao_guizong` / `ji_zhongdao_zhizun` 把道痕**均摊到 44 路**，在本体互斥公式下是**巨额自我削弱**（分别约 ×1/13.6、×1/64、×1/127）。任务书要求改为「只加主修」是**正确的修复方向**。 | 致命 |
| 5 | 至尊仙体的「道痕不互斥」在本体的实现 = `tizhi == 11` 时**跳过 `daohen_zong` 重算**。本体名称为**至尊仙胎体**。 | 关键 |
| 6 | 44 项道痕字段 ↔ 天赋 **44/44 全覆盖**，无遗漏、无多余；一字段两档位只有剑/雷/力 3 路（+200 与 +800）。 | — |
| 7 | 价格梯度**不符**任务书：现价 `+200→1 ✓`、`+500→2（应3）`、`+800→3（应4）`、`+1200→5（应6）`。47 条里 **20 条需改价**。 | 高 |
| 8 | 47 条道痕天赋**全部**描述写「熟练度效率 +N%」，代码实际是**平铺 `liupai_* += N/10`**（+200→+20、+500→+50…），不是百分比。**44 条描述与实现不符**。 | 高 |
| 9 | 「主修流派」**实际上没有接入天赋系统**：`TianfuMainDao.of()` 全局**只有 1 处调用**（`TianfuSlay:213`）。购买主修强化天赋**没有任何主修门禁**。 | 高 |
| 10 | 主修并列时 `TianfuMainDao.highest()` 用 `>` 比较，**静默取 `KEYS` 首位 = `jiandao`（剑道）**，正是任务书点名的 bug。 | 高 |

---

## A. 字段盘点（javap 实测，非推测）

数据源：`docs/audit-1.4.18/_raw_playervars.txt`（`javap -p` 本体 `PlayerVariables`）+ `_tmp_vf_out3/.../KE1Procedure.java`（vineflower 反编译）

| 项 | 数量 | 说明 |
|---|---:|---|
| `daohen_*` / `dahen_*` 字段总数 | **45** | 含汇总字段 `daohen_zong` |
| **44 项真实道痕字段** | **44** | 35 个 `daohen_*` + 9 个 `dahen_*` |
| `liupai_*` / `Liupai_*` 字段 | **46** | 44 + `feixingdao` + `qingmeidao` |
| `KE1Procedure` 汇总求和项 | **44** | 与 44 项逐一对应，无遗漏 |
| 主修候选 `TianfuMainDao.KEYS` | **15** | |
| 特殊流派 = 44 − 15 | **29** | 与任务书一致 |

### A.1 命名特殊情况（全部实测确认）

| 情况 | 字段 | 证据 |
|---|---|---|
| 少一个 `o`（`dahen_` 前缀） | `dahen_qidao` `dahen_zhidao` `dahen_zhoudao` `dahen_rendao` `dahen_tiandao` `dahen_nudao` `dahen_xingdao` `dahen_zhendao` `dahen_liandao` | `_raw_playervars.txt` |
| 流派权重首字母大写 | `Liupai_gudao` | `_raw_playervars.txt`；`TianfuTalents:725` 注释已指出 |
| 汇总字段（**不是第 45 项道痕**） | `daohen_zong` | `KE1Procedure:846` 是它的**唯一求和写入点** |
| 有 `liupai` 无 `daohen` | `feixingdao`、`qingmeidao` | 差集计算结果；`TianfuDaohen:74` 已按此处理 |
| 支系字段（与主字段独立） | `jindao2` / `yundao2` | 本体各自独立求和与独立参与互斥 |

> 任务书要求「没有独立道痕字段的流派值不能加入 44 项映射」→ **已满足**：44 项全部有真实 `daohen_*`/`dahen_*` 字段，`feixingdao`/`qingmeidao` 未计入。

---

## B. 44 项 × 天赋 全映射表（脚本生成，`_daohen_map.py`）

`汇总?` = 是否被 `KE1Procedure` 纳入 `daohen_zong`；`主修?` = 是否在 15 项 `KEYS` 内。

| # | 本体字段 | 流派 | 前缀 | 汇总 | 主修 | 天赋 id | 增加量 | 现价 | 品质 |
|---:|---|---|---|:--:|:--:|---|---:|---:|:--:|
| 1 | `daohen_andao` | andao | daohen | Y | – | `andao_qinhe` | 200 | 1 | 黄 |
| 2 | `daohen_bianhuadao` | bianhuadao | daohen | Y | – | `bianhuadao_qinhe` | 500 | 2 | 玄 |
| 3 | `daohen_bingdao` | bingdao | daohen | Y | **Y** | `bingdao_qinhe` | 500 | 2 | 玄 |
| 4 | `daohen_bingxuedao` | bingxuedao | daohen | Y | – | `bingxuedao_qinhe` | 200 | 1 | 黄 |
| 5 | `daohen_dandao` | dandao | daohen | Y | – | `dandao_qinhe` | 500 | 2 | 玄 |
| 6 | `daohen_daodao` | daodao | daohen | Y | – | `daodao_qinhe` | 500 | 2 | 玄 |
| 7 | `daohen_dudao` | dudao | daohen | Y | – | `dudao_qinhe` | 200 | 1 | 黄 |
| 8 | `daohen_fengdao` | fengdao | daohen | Y | **Y** | `fengdao_qinhe` | 200 | 1 | 黄 |
| 9 | `daohen_guangdao` | guangdao | daohen | Y | – | `guangdao_qinhe` | 500 | 2 | 玄 |
| 10 | `daohen_gudao` | gudao | daohen | Y | – | `gudao_qinhe` | 200 | 1 | 黄 |
| 11 | `daohen_huadao` | huadao | daohen | Y | – | `huadao_qinhe` | 200 | 1 | 黄 |
| 12 | `daohen_huandao` | huandao | daohen | Y | – | `huandao_qinhe` | 500 | 2 | 玄 |
| 13 | `daohen_hundao` | hundao | daohen | Y | **Y** | `hundao_qinhe` | 200 | 1 | 黄 |
| 14 | `daohen_jiandao` | jiandao | daohen | Y | **Y** | `jiandao_qinhe` | 200 | 1 | 黄 |
| 14b | 同上 | | | | | `jiandao_dacheng` | 800 | 3 | 地 |
| 15 | `daohen_jindao` | jindao | daohen | Y | **Y** | `jindao_qinhe` | 200 | 1 | 黄 |
| 16 | `daohen_jindao2` | jindao2 | daohen | Y | **Y** | `jindao2_jinjie` | 500 | 2 | 玄 |
| 17 | `daohen_leidao` | leidao | daohen | Y | **Y** | `leidao_qinhe` | 200 | 1 | 黄 |
| 17b | 同上 | | | | | `leidao_dacheng` | 800 | 3 | 地 |
| 18 | `dahen_liandao` | liandao | dahen | Y | – | `liandao_qinhe` | 500 | 2 | 玄 |
| 19 | `daohen_lidao` | lidao | daohen | Y | **Y** | `lidao_qinhe` | 200 | 1 | 黄 |
| 19b | 同上 | | | | | `lidao_dacheng` | 800 | 3 | 地 |
| 20 | `daohen_lvdao` | lvdao | daohen | Y | **Y** | `lvdao_qinhe` | 200 | 1 | 黄 |
| 21 | `daohen_mengdao` | mengdao | daohen | Y | – | `mengdao_qinhe` | 200 | 1 | 黄 |
| 22 | `daohen_mudao` | mudao | daohen | Y | **Y** | `mudao_qinhe` | 200 | 1 | 黄 |
| 23 | `dahen_nudao` | nudao | dahen | Y | – | `nudao_qinhe` | 200 | 1 | 黄 |
| 24 | `dahen_qidao` | qidao | dahen | Y | – | `qidao_qinhe` | 200 | 1 | 黄 |
| 25 | `dahen_rendao` | rendao | dahen | Y | – | `rendao_dacheng` | 800 | 3 | 地 |
| 26 | `daohen_shidao` | shidao | daohen | Y | – | `shidao_qinhe` | 500 | 2 | 玄 |
| 27 | `daohen_shuidao` | shuidao | daohen | Y | **Y** | `shuidao_qinhe` | 200 | 1 | 黄 |
| 28 | `dahen_tiandao` | tiandao | dahen | Y | – | `tiandao_daocang` | 1200 | 5 | 天 |
| 29 | `daohen_toudao` | toudao | daohen | Y | – | `toudao_qinhe` | 200 | 1 | 黄 |
| 30 | `daohen_tudao` | tudao | daohen | Y | **Y** | `tudao_qinhe` | 200 | 1 | 黄 |
| 31 | `daohen_xindao` | xindao | daohen | Y | – | `xindao_qinhe` | 500 | 2 | 玄 |
| 32 | `dahen_xingdao` | xingdao | dahen | Y | – | `xingdao_qinhe` | 500 | 2 | 玄 |
| 33 | `daohen_xudao` | xudao | daohen | Y | – | `xudao_qinhe` | 200 | 1 | 黄 |
| 34 | `daohen_xuedao` | xuedao | daohen | Y | **Y** | `xuedao_qinhe` | 200 | 1 | 黄 |
| 35 | `daohen_yandao` | yandao | daohen | Y | **Y** | `yandao_qinhe` | 200 | 1 | 黄 |
| 36 | `daohen_yindao` | yindao | daohen | Y | – | `yindao_qinhe` | 500 | 2 | 玄 |
| 37 | `daohen_yingdao` | yingdao | daohen | Y | – | `yingdao_qinhe` | 200 | 1 | 黄 |
| 38 | `daohen_yudao` | yudao | daohen | Y | **Y** | `yudao_qinhe` | 200 | 1 | 黄 |
| 39 | `daohen_yuedao` | yuedao | daohen | Y | – | `yuedao_qinhe` | 200 | 1 | 黄 |
| 40 | `daohen_yundao` | yundao | daohen | Y | – | `yundao_qinhe` | 500 | 2 | 玄 |
| 41 | `daohen_yundao2` | yundao2 | daohen | Y | – | `yundao2_jinjie` | 500 | 2 | 玄 |
| 42 | `dahen_zhendao` | zhendao | dahen | Y | – | `zhendao_qinhe` | 500 | 2 | 玄 |
| 43 | `dahen_zhidao` | zhidao | dahen | Y | – | `zhidao_qinhe` | 200 | 1 | 黄 |
| 44 | `dahen_zhoudao` | zhoudao | dahen | Y | – | `zhoudao_qinhe` | 200 | 1 | 黄 |

**校验结果（脚本输出）**
- 无任何天赋的字段：**无**
- 有天赋的字段：**44 / 44**
- 未纳入 `daohen_zong` 汇总的字段：**无**
- 一字段多档位的流派：`jiandao` / `leidao` / `lidao`（各 +200 与 +800 两档）
- 天赋条数（`dao(...)` 调用）：**47** = 44 + 3 个第二档

> 注意 `gudao`（骨道）**不在** 15 项主修候选内，属 29 项特殊流派。源码 `TianfuMainDao` 注释称 15 项是「两侧字段都真实存在」的——**该理由不成立**：44 项全部两侧都存在，15 项纯属人为筛选。

---

## C. 本体道痕互斥机制（关键，必须复用）

### C.1 公式原文（vineflower 反编译，权威）

`ShangHaiLiuCheng1Procedure:27-30`（**主伤害流水线**）与 `BuffLiuCheng1Procedure:46-54`（蛊虫伤害/buff 流水线）**完全同一套**：

```java
A = LiuPai1 / 1000.0 - (daohen_zong - LiuPai1) / 1000.0;
B = (daohen_zong - LiuPai1) / 1000.0 - LiuPai1 / 1000.0;
C = LiuPai2 / 1000.0 - (Entity2.daohen_zong - LiuPai2) / 1000.0;   // 受击方，同上结构
D = (Entity2.daohen_zong - LiuPai2) / 1000.0 - LiuPai2 / 1000.0;
if (A < 0) A = 0;  if (B < 0) B = 0;  if (C < 0) C = 0;  if (D < 0) D = 0;
```

伤害应用（`BuffLiuCheng1Procedure:413` / `ShangHaiLiuCheng1Procedure` 同构）：

```java
ShangHai * (1 + A) * (1 + C) / (B + 1) / (D + 1) * (1 + JiaCheng)
```

`LiuPai1` 是**调用方传入的该流派 `daohen_*` 原值**，实测依据 `Chun4DanSheWuJiZhongShiTiShiProcedure:50`：
`((PlayerVariables)sourceentity.getData(...)).dahen_zhoudao`。
→ **`LiuPai1` = 道痕**，不是熟练度；单位是「千分之一」。

### C.2 名字翻译成中文

设 `own` = 本次攻击所用流派的道痕，`other` = 其余 43 路道痕之和 = `daohen_zong − own`：

- `A = max(0, own/1000 − other/1000)` → **同流派优势**
- `B = max(0, other/1000 − own/1000)` → **异道劣势**
- 最终倍率 = `(1+A) / (1+B)`（单方，双方各算一次）

### C.3 数值后果（这是本轮平衡的核心）

| 玩家道痕分布 | own | other | 伤害倍率 | 结论 |
|---|---:|---:|---:|---|
| 无道痕 | 0 | 0 | **×1.00** | 基线 |
| 单流派 1000（剑道亲和+大成） | 1000 | 0 | **×2.00** | 专精收益极高 |
| 单流派 2000（十绝体级） | 2000 | 0 | **×3.00** | |
| 双流派各 1000 | 1000 | 1000 | **×1.00** | 兼修把专精收益清零 |
| 三流派各 1000 | 1000 | 2000 | **×0.50** | 开始受罚 |
| 均摊 44 路共 13200（五行道藏） | 300 | 12900 | **×0.074** | ≈ 削弱 13.6 倍 |
| 均摊 44 路共 66000（神·万道归宗） | 1500 | 64500 | **×0.0156** | ≈ 削弱 64 倍 |
| 均摊 44 路共 132000（极·众道至尊） | 3000 | 129000 | **×0.0079** | ≈ 削弱 127 倍 |

> **这张表直接解释了任务书为什么要改这三个天赋。** 它们现在不是「神级天赋」，而是**把玩家砍成废人的陷阱**；同时它们又各自标价 5 / 12 / 16 点。这是本次审查最严重的失衡。

### C.4 不要做的事

- **不要**另写一套五行相生相克倍率：本体没有任何五行生克代码，`BuffLiuCheng1Procedure` 只有上表这一套 `own/other` 线性机制。
- **不要**把 `A/B` 改成 `own/(own+other)` 之类的比例式：那会改变本体所有伤害数值。
- 附属要做的只是**正确地往字段里写数**，让本体公式自己算。

---

## D. `daohen_zong` 与 `zonghen_chubei`（天/5 点，实测无效）

### D.1 汇总字段每 tick 被重算

`KE1Procedure:844-891`：

```java
if (tizhi != 11.0) {                                  // 844
    _varsxx.daohen_zong = daohen_yudao + dahen_zhoudao + ... + daohen_bianhuadao;  // 846-889
    _varsxx.markSyncDirty();                          // 890
}
```

求和清单实测 **44 项**，与 A 节 44 字段逐一对上。

### D.2 结论

`zonghen_chubei` 的定义是（`TianfuTalents:190`）：

```java
t("zonghen_chubei", "总痕储备", "天", 5, "总道痕汇总字段 +300；不增加各流派道痕或伤害",
    fldAdd("daohen_zong", 300)),
```

- 它写 `daohen_zong += 300`
- 下一次 player tick 被 `KE1Procedure:846` **整体覆盖**
- → **净效果恒为 0**，玩家白付 5 点

**这不仅是「只改汇总字段」，而是「改了一个每 tick 被重算的派生值」。** 任务书要求「提出处理方案，不得未经确认直接删除」——见 H 节方案。

### D.3 反查：谁读 `daohen_zong`

| 类 | 用途 |
|---|---|
| `KE1Procedure` | **唯一写入**（每 tick 覆盖） |
| `ShangHaiLiuCheng1Procedure` | 主伤害流水线互斥公式 |
| `BuffLiuCheng1Procedure` | 蛊虫伤害/buff 流水线互斥公式 |
| `HunPoLiuCheng1Procedure` | 魂魄伤害流水线 |
| `ShangHaiLiuCheng43Procedure` | 另一条伤害流水线 |
| `Citeng/Duanshu/Duteng/Maomin/Muxiuyu/Yaochumianyi/Yaoteng...` | 本体植物类实体 tick（与本 Mod 无关） |

→ 汇总字段是**纯派生值**，正确做法是「只写 44 项字段，让本体自己汇总」，而 `TianfuTalents.allDaohen`（`:714-719`）正好就是这么做的（显式跳过 `daohen_zong`）。这一处实现是对的。

---

## E. 至尊仙体（本体名：至尊仙胎体）

`UishuxingshijuetiProcedure`（vineflower, `_tmp_vf_out4`）实测本体体质名表：

| tizhi | 名称 |
|---:|---|
| 1 | 太日阳莽体 |
| 2 | 古月阴荒体 |
| 3 | 北冥冰魄体 |
| 4 | 森海轮回体 |
| 5 | 炎煌雷泽体 |
| 6 | 万金妙华体 |
| 7 | 大力真武体 |
| 8 | 逍遥智心体 |
| 9 | 厚土元央体 |
| 10 | 宇宙大衍体 |
| **11** | **至尊仙胎体**（`UishuxingshijuetiProcedure:31-32`） |
| 12 | 纯梦求真体 |
| 13 | 气运之子 |
| 14 | 天外之魔 |
| 15 | 正道善德身 |
| 16 | 无 |

**本体的「道痕不互斥」实现方式**：`KE1Procedure:844` 的 `if (tizhi != 11.0)` —— tizhi=11 时**跳过汇总重算**。后果推演：

- `daohen_zong` 停在旧值（新角色通常为 0）
- `other = daohen_zong − own = −own` → 负
- `A = own/1000 − (−own/1000) = 2·own/1000`（**双倍优势**）
- `B = (−own/1000) − own/1000 = −2·own/1000` → 钳制为 **0**（**零劣势**）

→ 至尊仙胎体不只「不互斥」，实际是 **A 翻倍 + B 归零**。比「普通玩家」强得多。

**对本 Mod 的意义**
- `tianfu` 目前**不发放** tizhi 11：`TianfuDaohen.hasDaochen` 覆盖 凡体 17–53 与十绝体 1–10，`TianfuPool` 的 `BODY` 体质号与 54 杀神；**11 未被占用**。
- `TianfuTalents.daohenFields()`（`:742-756`）会用反射遍历 `PlayerVariables` 全部 `daohen_*`/`dahen_*` 字段。**若将来有人把 tizhi 11 加进体质表，普通玩家就会直接获得至尊仙胎体的互斥豁免 + 双倍优势**（任务书原则 9 明令禁止）。
- `TianfuTalents.java:40-43` 已有一张 `NATIVE_UNAVAILABLE` 黑名单机制，可用来锁死。

**建议**：在 `TianfuDaohen.hasDaochen` / `TianfuNative.applySijue` 之外再补一道**显式断言**：`tizhi == 11` 不得由本 Mod 写入；`audit` 脚本锁死。

---

## F. 价格梯度不符（任务书已定，不得擅改）

| 增加量 | 现价 | 任务书 | 状态 | 条目数 |
|---:|---:|---:|---:|---:|
| +200 | 1 | 1 | ✅ 一致 | 27 |
| +500 | 2 | **3** | ❌ 低了 1 点 | 15 |
| +800 | 3 | **4** | ❌ 低了 1 点 | 4 |
| +1200 | 5 | **6** | ❌ 低了 1 点 | 1 |

→ **47 条中 20 条需改价**（27 条 +200 不用动）。

现行价格同时带来「品质锚点」冲突：`TianfuTalents` 用品质字段决定成本锚（黄 1 / 玄 2 / 地 3 / 天 5 / 神 12 / 极 16），改价必须**同时改品质字段**，否则 `audit_new_talents.py` 会报「品质与成本不一致」。建议映射：

| 增加量 | 目标价 | 应使用的品质 |
|---:|---:|---|
| +200 | 1 | 黄 |
| +500 | 3 | **地**（现为玄） |
| +800 | 4 | **无对应锚**（地=3，天=5）→ 需新增锚点或允许 4 |
| +1200 | 6 | **无对应锚**（天=5，神=12） |

> ⚠ **需你决策**：4 点与 6 点在现有 7 档品质锚里没有对位。两条路：(a) 扩展品质锚（新增「地+」「天+」或改用 cost 直接表）；(b) 保持品质字段仅作显示、由 cost 独立决定。我倾向 (b)（只把 cost 改到位，品质仅显示），因为它不动 `TianfuPool`/`audit` 的口径。

---

## G. 三个全流派天赋 / 两个特殊天赋 审查

### G.1 `wuxing_daocang`（五行道藏）— `TianfuTalents:271`

```java
t("wuxing_daocang", "五行道藏", "天", 5, "44 项非汇总道痕各 +300；…", f(Op.DAOHEN_ALL, 300)),
```

- 现价 **5**（与任务书「退款 5 点」吻合 ✅）
- 现效果：44 路各 +300 → 总 13200 → **伤害 ×0.074**
- 任务书要求：移出新购买池、保留旧 ID、旧记录退 5 点、不再加 44 项道痕
- **判定：任务书方向正确，且比「微调数值」更必要**——这个天赋在本体公式下无法通过调数值变好，只要它碰多路道痕就一定是削弱。

### G.2 `shen_wandao_guizong`（神·万道归宗）— `TianfuTalents:319`

```java
t("shen_wandao_guizong", "神·万道归宗", "神", 12, "44 项非汇总道痕各 +1500；…", f(Op.DAOHEN_ALL, 1500)),
```

- 现价 **12** → 任务书要求 **8**
- 现效果：总 66000 → **伤害 ×0.0156**
- 任务书新效果：主修道痕 +1500、对应流派值 +150、价格 8、不加其他路、不加修炼/突破
- **一致性核对**：任务书的「+1500 道痕 / +150 流派值」正好等于现有 `addPaired` 的 1/10 成对写口径（`TianfuTalents:734` `double share = amount / 10.0;`）→ **新效果可用现有原语直接实现，无需新代码路径**。

### G.3 `ji_zhongdao_zhizun`（极·众道至尊）— `TianfuTalents:329-330`

```java
t("ji_zhongdao_zhizun", "极·众道至尊", "极", 16,
  "44 项非汇总道痕各 +3000（…）、修炼速度 +200%、本体突破失败概率降低 80%",
  f(Op.DAOHEN_ALL, 3000), f(Op.XIULIAN, 2.00), f(Op.BREAKTHROUGH, 0.80)),
```

- 现价 **16** ✅（任务书不变）
- 现效果：总 132000 → **伤害 ×0.0079**，且**同时**给修炼 +200% 与突破 +80pp → **违反**「高阶天赋不能包揽多个体系」（任务书四.4 / 五.3）
- 任务书新效果：主修道痕 +3000、流派值 +300、16 点、移除修炼与突破
- **一致性核对**：3000/10 = 300 ✅ 同 G.2 口径

### G.4 互斥缺口

三者与普通道痕天赋**当前无任何互斥**（`TianfuMindTable.LEVELS` 只有 `xiulian`/`capacity`/`recovery`/`liangu`/`cooldown` 五族，`TianfuEconomy.buy()` 也不查道痕）。任务书要求「与所有普通主修道痕天赋互斥」→ 需新增 `daohen_main` 互斥族（详见 I 节）。

### G.5 `zonghen_chubei`（总痕储备）— `TianfuTalents:190`

见 D 节：**实测无效**。处理方案（三选一，需你确认）：

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| **Z1（推荐）** | 移出新购买池，保留 ID，旧记录退 5 点 | 与五行道藏同款处理，口径统一 | 老玩家少一个天赋 |
| Z2 | 改为「主修道痕 +300」 | 保留天赋价值 | 与 `+200` 档天赋功能重叠 |
| Z3 | 保留但改为**真正有意义的**「道痕上限/转换」类效果 | 有新玩法 | 违反「不随意增加新机制」原则 |

> 任务书说「不得未经确认直接删除」，所以我**没有**任何删除动作，只给方案。

### G.6 `shen_liupai_jianji`（神·流派兼济）— `TianfuTalents:320-324`

```java
mob("guzhenren_addon:ice_path_boost", 0),
mob("hundao_addon:hundao_zengfu", 0),
mob("yundao:yizhi_qiankun", 0)
```

- 三个**附属命名空间**的常驻 buff 打包，12 点
- 对照单卖版本（`TianfuTalents:297-302`）：`bingxue_daoti` 天/5、`hundao_ronghe` 天/5、`qiyun_yizhi` 天/5 = **合计 15 点**
- → `shen_liupai_jianji` 用 12 点买到 15 点内容，**且与之完全可叠加**（无互斥族）
- 额外差异：`qiyun_yizhi` 还带 `qiyun_shangxian +0.30`（`:302`），而 `shen_liupai_jianji` **没有**这一项 → 描述「三者可叠加」与实际略不符
- **判定**：属于任务书点名的「高阶天赋包揽多个体系」，且「打包比单买便宜」违反价格一致性
- 方案：**不与** `bingxue_daoti`/`hundao_ronghe`/`qiyun_yizhi` 同时可购（放进同一互斥族 `liupai_bundle`），价格上调到 15，或拆成「仅在三个附属都装时才出现」的整合档。**需你确认**。

---

## H. 描述与实现不符（问题 7）

### H.1 47 条道痕天赋全部存在同一处偏差

- 描述模板：`"XX道痕 +200；该流派熟练度效率 +20%，不直接增伤"`
- 实现（`TianfuTalents:727-738`）：

```java
private static void addPaired(PlayerVariables v, String daohenName, double amount) {
    add(v, daohenName, amount);                       // daohen += amount
    String suffix = daohenName.substring(cut + 1);
    double share = amount / 10.0;                     // ★ 平铺，不是百分比
    tryAdd(v, "liupai_" + suffix, share);             // liupai += amount/10
}
```

→ 所谓「效率 +20%」实际是 **`liupai_jiandao += 20`（固定 20 点熟练度）**。
- +200 → +20 点、+500 → +50、+800 → +80、+1200 → +120
- 参照系：本体十绝体给 `liupai 200`（`TianfuDaohen:8`），所以 +20 只有十绝体档的 1/10，**不是 20% 效率**

**两种修法**（需你选）：
- **H-a（改描述）**：描述改为「该流派熟练度 +20」——零风险，不动数值
- **H-b（改实现）**：真要给「效率百分比」，需找到本体熟练度增速的乘算入口（属修炼体系，需与 T2 的结论合并），**风险更高**

> 我倾向 **H-a**：本轮目标是平衡不是增能，且「熟练度效率 +N%」这种乘算在没有本体钩子的情况下无法可靠实现。

### H.2 `ji_zhongdao_zhizun` 描述里的「效率各 +300%」

同上问题，且合并 H.1 一起修。

---

## I. 主修流派系统（当前基本未实现）

### I.1 调用点唯一

全仓 `TianfuMainDao.` 调用点扫描结果：**只有 1 行**

```
TianfuSlay.java:213:  String main = TianfuMainDao.of(sp, v);
```

→ 除「杀神噬道」外，**主修流派对天赋购买、道痕写入、伤害计算没有任何影响**。
任务书的规则「没有有效主修时，不允许购买主修强化天赋」**完全没有实现**。

### I.2 并列 / 全 0 处理（bug 确认）

`TianfuMainDao:65-76`：

```java
static String highest(PlayerVariables v) {
    String best = "";
    double bestValue = 0.0;
    for (String key : KEYS) {
        double value = liupai(v, key);
        if (value > bestValue) {            // ★ 严格大于
            bestValue = value; best = key;
        }
    }
    return best;                            // 全 0 → 空串
}
```

- `KEYS` 首位是 `jiandao` → **并列最高时取剑道**（任务书点名 bug，**已复现**）
- 全 0 时返回 `""`（「未设定」）✅ 正确
- 但 `of()` 会**静默**用 `highest()` 兜底（`TianfuMainDao:45-48`），所以并列时玩家**不会被要求选择**，直接被当成剑道

### I.3 自动改写风险

`of()` 每次调用都重算 `highest()`。当前唯一消费者是 `TianfuSlay`（噬道分流），**不会**把结果写回 NBT，所以「主修被自动改动」目前**不会发生**——但也意味着主修选择**从未被持久化到 tianfu 自己的存储**，只读道途的 `daotu.mainDao`。

### I.4 需要的改造（详见下节方案）

1. 主修选择必须**显式**且**落库**（不能只依赖道途）
2. `highest()` 严格大于 → 改为「唯一最高才采用，并列返回空」
3. 空主修时拒买主修强化天赋（**服务端**校验）
4. 29 项特殊流派可获道痕但不参与主修识别

---

## J. 建议的 1.4.18 道痕改造方案（待确认，尚未实施）

### J.1 新增互斥族（改 `v2/TianfuMindTable.java`）

```
family "daohen_jiandao"  -> jiandao_qinhe(1), jiandao_dacheng(2)
family "daohen_leidao"   -> leidao_qinhe(1),  leidao_dacheng(2)
family "daohen_lidao"    -> lidao_qinhe(1),   lidao_dacheng(2)
family "daohen_main"     -> 上述全部 + shen_wandao_guizong + ji_zhongdao_zhizun
```
- 同族「只能选最高级」：`TianfuEconomy.buy()` 已有「退款旧价再扣新价」逻辑（`:154-172`），**直接复用即可**，无需新机制
- `daohen_main` 与具体流派族**同时命中**时，需要「复合天赋占用它涉及的所有互斥类别」→ 任务书八.5 要求；实现方式：允许一个 id 出现在多个 family（把 `LEVELS` 的 `Map<String, Level>` 改成 `Map<String, List<Level>>`）

### J.2 价格（只改 cost，不动品质字段）— F 节

| 条目组 | 条数 | 现价 | 改为 |
|---|---:|---:|---:|
| +200 档 | 27 | 1 | 1（不动） |
| +500 档 | 15 | 2 | **3** |
| +800 档 | 4 | 3 | **4** |
| +1200 档 | 1 | 5 | **6** |

### J.3 三个全流派天赋

| id | 现状 | 1.4.18 目标 |
|---|---|---|
| `wuxing_daocang` | `DAOHEN_ALL 300`, 5 点 | **移出新购池**；保留 ID；旧记录退 5 点；不再加道痕 |
| `shen_wandao_guizong` | `DAOHEN_ALL 1500`, 12 点 | 改名「神·主道归宗」；`主修道痕 +1500` + `对应 liupai +150`；**8 点**；去掉其他路收益 |
| `ji_zhongdao_zhizun` | `DAOHEN_ALL 3000` + `XIULIAN 2.0` + `BREAKTHROUGH 0.8`, 16 点 | 改名「极·主道至尊」；`主修道痕 +3000` + `对应 liupai +300`；**16 点**；移除修炼与突破 |

实现注意：
- 需要一个**新原语** `Op.DAOHEN_MAIN`（arg = null，a = amount），执行时先 `TianfuMainDao.of()` 取主修 → 再用 `addPaired` 写。若主修为空 → 购买时应被 J.4 门禁拦住。
- **不要**复用 `DAOHEN_ALL`：它按 `daohenFields()` 反射遍历 44 路，正是要废除的行为。

### J.4 主修门禁（服务端）

- `TianfuEconomy.buy()` 新增检查：`if (id 属 daohen_main 族 && 主修为空) return fail(state, NO_MAIN_DAO)`
- `TianfuSelection.Rejection` 需新增枚举值 `NO_MAIN_DAO`
- 客户端 `TianfuViewScreen` 同步置灰 + 文案
- 主修记录：优先读 `PersistentData/daotu/mainDao`；若为空则**要求玩家在界面里选择**并写入 tianfu 自己的存储（`TianfuV2Store`）。**不得**用 `highest()` 自动兜底（除非唯一最高）

### J.5 `zonghen_chubei`

推荐 **Z1**（移出新购池 + 保留 ID + 退 5 点），在 `TianfuTalents.NATIVE_UNAVAILABLE` 同款机制里加一条「效果不可兑现」标记。

### J.6 描述修正

47 条统一把「熟练度效率 +N%」改为「该流派熟练度 +N」（N = 道痕量/10）。**纯文案改动，不动数值。**

### J.7 至尊仙胎体防护

- 加断言：本 Mod 任何代码路径不得写入 `tizhi = 11`
- 加审计脚本：扫描全仓 `tizhi` 赋值，命中 11 即报错

---

## K. 未验证事项（必须实机确认，不得当作已通过）

1. **互斥公式在实战中的实际倍率**：C.3 的表是**纯公式推演**，未经游戏内伤害实测。可能存在我未看到的旁路（例如某些流派走 `ShangHaiLiuCheng43Procedure` 而非 `ShangHaiLiuCheng1Procedure`）。
2. **`DaohenCommand` 之外的第三条写入路径**：是否存在我漏掉的 `daohen_zong` 写者（已扫描 14 个引用类，未发现第二个写入点，但扫描基于字段名常量）。
3. **`liupai_*` 的真实量纲**：+20 熟练度在实战中相当于多少收益，需要看熟练度→境界的换算（属 T2 修炼体系范围）。
4. **`markSyncDirty()` 的频率**：`daohen_zong` 每 tick 重算 + `markSyncDirty`，对多人服务器的网络开销未评估。
5. **道途 `daotu.mainDao` 的实际存储格式**：本报告依据 `TianfuMainDao:27-28` 的常量 `"daotu"` / `"mainDao"`，**未**到道途源码里二次确认（道途源码在 `daotu\src`，可另行核对）。

---

*脚本产物：`docs/audit-1.4.18/_daohen_map.py`（只读）、`_daohen_map_out.txt`（输出）、`_raw_playervars.txt`（javap）*
