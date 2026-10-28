# 天赋重构 · 证据包（已实测，非推测）

> 本文件由 DSH 实测生成，供架构师（Codex）直接采信。所有字段名均来自 `javap` 实读字节码，
> 所有流派/数值均来自知识库原文。未经标注的内容不要当成事实使用。

## 0. 项目坐标

| 项 | 值 |
|---|---|
| 工程根 | `D:\daima\mcmod\蛊真人附属` |
| modid | `tianfu` |
| 包名 | `net.guzhenren.tianfu` |
| 环境 | Minecraft 1.21.1 / NeoForge |
| Java | 21（toolchain 固定） |
| 版本号唯一来源 | `version.properties`（当前 1.2.37） |
| 构建 | `gradlew build`，离线 classpath（见 build.gradle 顶部注释） |

## 1. 权威来源

| 来源 | 位置 | 用途 |
|---|---|---|
| 知识库 | `_ref/knowledge-base.txt`（原 `D:\cxdownload\蛊真人整合包知识库_AI版.txt`，8991 行） | 26 流派、十绝体映射、数值规则、道痕规则 |
| 本体 10.20 | `D:\cxdownload\guzhenrn蛊真人模组本体10.20.jar` | 真实字段与玩法实现 |
| 本体真实内层 jar | 上述 jar 内 `META-INF/jarjar/guzhenren-0.1-neoforge-1.21.1.jar`（106MB） | 已抽出到 `_tmp_verify/guzhenren-core.jar` |
| 字段全表 | `_ref/player-variables-fields.txt`（`javap -p` 输出，421 行，410 个 public 字段） | 字段名唯一权威 |
| 现有词条清单 | `_ref/talent-inventory.txt`（103 条） | 重构基线 |

注意：`D:\cxdownload\guzhenrn蛊真人模组本体10.20.jar` 是**外壳**，其自身只有
`com/guzhenrenlibrary/**`（蛊真人库：五域两天世界生成、城镇、关系网、NPC），
**没有** `net/guzhenren/**`。真正的玩法本体在内层 jar 里。

## 2. 真实道痕字段（45 个，javap 实读）

```
dahen_liandao   dahen_nudao    dahen_qidao    dahen_rendao   dahen_tiandao
dahen_xingdao   dahen_zhendao  dahen_zhidao   dahen_zhoudao
daohen_andao    daohen_bianhuadao  daohen_bingdao  daohen_bingxuedao  daohen_dandao
daohen_daodao   daohen_dudao   daohen_fengdao daohen_guangdao daohen_gudao
daohen_huadao   daohen_huandao daohen_hundao daohen_jiandao daohen_jindao
daohen_jindao2  daohen_leidao  daohen_lidao  daohen_lvdao   daohen_mengdao
daohen_mudao    daohen_shidao  daohen_shuidao daohen_toudao daohen_tudao
daohen_xindao   daohen_xudao   daohen_xuedao daohen_yandao  daohen_yindao
daohen_yingdao  daohen_yudao   daohen_yuedao daohen_yundao  daohen_yundao2
daohen_zong
```

**前缀陷阱（已实测）**：`dahen_`（少一个 o）与 `daohen_` **两套前缀并存**，
不是笔误。`dahen_*` 用于 炼/奴/气/人/天/星/真/智/宙 九道；`daohen_*` 用于其余。
任何"统一前缀"的整理都会导致反射取不到字段、天赋静默失效。**不要改前缀。**

## 3. 现有词条的覆盖情况（实测比对）

- 现有 `TianfuTalents.BASE` 共 **103 条**：黄 27 / 玄 29 / 地 24 / 天 23。
- 其中「流派亲和」30 条：剑 雷 炎 冰雪 风 影 月 水 土 木 金 骨 力 魂 血 虚 智 宙 梦 毒 兵 刀 光 星 丹 炼 音 幻 变化 阵。
- 现有词条引用的 37 个字段名 **全部真实存在**（无拼写错误）。
- **未被任何词条使用的真实道痕字段（15 个）**：
  `dahen_nudao`(奴道)、`dahen_qidao`(气道)、`dahen_rendao`(人道)、`dahen_tiandao`(天道)、
  `daohen_yudao`(宇道)、`daohen_lvdao`(律道)、`daohen_toudao`(偷道)、`daohen_huadao`(化道)、
  `daohen_shidao`(食道)、`daohen_xindao`(心道)、`daohen_andao`(暗道)、`daohen_yundao`(运道)、
  `daohen_yundao2`、`daohen_jindao2`、`daohen_zong`。
- 知识库第 4 章的 **26 个主流派**中，**宇道 / 气道 / 律道 / 偷道 / 奴道** 这 5 个在亲和表里缺失。

## 4. 知识库 26 流派（第 4 章小节标题原文）

力道、剑道、魂道、血道、月道、宇道、宙道、气道、炼道、水道、律道、偷道、智道、金道、木道、
炎道、土道、雷道、变化道、光道、风道、骨道、冰雪道、奴道、其余流派（星道/毒道/云道/刀道/食道等）、
阵道（未完善）。

## 5. 十绝体 → 流派映射（知识库 16.2 原文表）

| 十绝体 | 对应流派 | 关键效果 |
|---|---|---|
| 大力真武体 | 力道 | 精力上限 200，基础攻击力 63，力道道痕 +2000 |
| 森海轮回体 | 木道 | 饱和 2；初始生命 +40；木道 +2000；升转加血 20/30/480/2430/7680 |
| 北冥冰魄体 | 魂道 / 冰雪道 | 免疫低级寒冷与低级魂魄攻击；随机冰雪 2000 或魂道 2000；+400 魂魄上限，可额外承载 5 万魂魄 |
| 炎煌雷泽体 | 炎道 / 雷道 | 免疫低级麻痹；防火 4；随机雷 2000 或炎 2000；引爆「烬」每秒 3000 |
| 万金妙华体 | 金道 | 急迫 6；金道 +2000；元石收入 50% 翻倍 |
| 逍遥智心体 | 智道 | 念头上限 2 万，恢复上限 1 万，恢复更快；灵光一闪 6；智道 +2000 |
| 厚土元央体 | 土道 | 土道 +2000；周围威力减倍，范围 5/10/15/20/25 格、等级 1~5；每秒修装备耐久 10 |
| 宇宙大衍体 | 律道 | 威力翻倍（等级随转数）；律道 +2000 |
| 古月阴荒体 | 宙道 | 修炼加快；蛊虫冷却 -0.2 倍；宙道 +2000；buff 时长 ×3；「烬」+30 秒 |
| 太日阳莽体 | 宇道 | 前闪改超远空间移动且更省精力；宇道 +2000；宇道增幅 +2 级 |

十绝体共同规则（16.1）：资质无法永久改变、气运上限为 0、压力到 100% 未突破大境界即自爆
（伤害极高、范围极大、扣「转数×10」年寿元）、压力 ≥60 立即恢复十成资质。

## 6. 知识库数值规则（第 2 章，会影响词条数值口径）

- 2.1 **道痕不再参与伤害计算**；每 1 点流派道痕使该流派**熟练度获取效率 +0.1%**，且是许多蛊虫/杀招的增幅系数。
- 2.2 **熟练度降低真元与冷却消耗**。
- 2.3 气运上限受人气影响。
- 2.4 气运实际影响战斗与战利品。
- 2.5 魂道数值分档削弱 + 魂压机制（超出部分百分之一增伤）。
- 2.6 律道增伤限制。
- 2.7 力道普攻改动。

## 7. 引擎边界（本次重构不得改动）

`TianfuData` / `TianfuEffects` / `TianfuCombat` / `TianfuVfx` / `TianfuVfxEntity` /
`TianfuZhu` / `TianfuSlay` / `TianfuNative` / `TianfuPool` / `TianfuRoll` /
`TianfuApply` / `TianfuAttributes` / `TianfuDaohen` 以及全部 `net/**` payload 类。
本次只动两张数据表：`TianfuTalents` 的词条表、`TianfuLiupai` 的体质映射表。

## 8. 硬约束

1. **存档兼容**：现有 103 条的 `id` 字符串不得改动、不得删除（天赋已写入玩家 NBT）。
   新增条目一律用新 id。
2. **字段前缀**：`dahen_` / `daohen_` 保持原样，走反射访问。
3. **编码**：Java 源文件为 UTF-8（含中文注释与词条名），写入时**不要加 BOM**。
4. **不碰构建**：不改 `build.gradle` / `version.properties` / 依赖。
5. 词条效果必须能落到现有 `Op` 枚举原语上；若确实需要新原语，必须同时给出
   在 `TianfuEffects` / `TianfuCombat` 中的落地位置（作为 UNVERIFIED 项标注）。
