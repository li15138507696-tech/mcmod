# tianfu 1.4.9 — 无头测试 + 真专用服务端联调报告

> 2026-10-31

本轮按「必须实测 20 次以上」执行，分三层：
**A 逻辑层（72 条用例，直接跑编译产物）** · **B 产物层（对着实际安装的 jar 核对）** · **C 真服务端（NeoForge 21.1.263 专用服务端 + RCON 25 条命令）**

---

## A. 逻辑层：72 条用例，全过

不启动游戏，直接 `java` 调 `tianfu/build/classes` 里的真实类。

| 组 | 内容 | 条数 |
|---|---|---|
| A | 体质槽位互斥（凡体 / 十绝体 / 空窍档位 / 空窍桎梏） | 10 |
| B | 点数 / 锁定 / 未知 id / 未选难度 | 9 |
| C | 负面补点上限 24 | 2 |
| D | 空窍资质反推（4 档 × 5 次随机 roll） | 20 |
| E | 世代点数（4 难度 × 5 世代） | 20 |
| F | 目录完整性 | 7 |
| G | **回归：复现「买了十绝体却还是凡人」** | 4 |

结果：**通过 72 / 失败 0**（`_crash6/harness/report.txt`）。

关键用例（G 组，就是你那次操作）：

```
PASS  8 点花光后再点十绝体必须被拒（不能静默吞掉）
PASS  且必须报 CONFLICT（空窍档位与十绝体互斥优先）
PASS  点数充足但已有空窍档位，买十绝体仍拒
PASS  点数够且无冲突时买十绝体应成功
```

D 组实测 roll 与反推：

```
档位2(甲) roll=959/918/907/831/859 → kongqiao=2
档位4(乙) roll=700/662/653/609/621 → kongqiao=1
档位6(丙) roll=454/423/437/424/414 → kongqiao=5
档位8(丁) roll=292/380/344/207/308 → kongqiao=8
```

与本体 `KE1Procedure` 的阈值表完全一致。

> 实现细节：`PlayerVariables` 的构造器会触碰 `ItemStack.<clinit>`，需要 MC 注册表，而注册表又需要 FML 加载器。
> 用 `Bootstrap.bootStrap()` 也过不去（NeoForge `FeatureFlagLoader` 要 `LoadingModList`）。
> 最终用 `Unsafe.allocateInstance` 跳过构造器——`applyAptitudeTier` 只写两个 double，不依赖已初始化字段。

---

## B. 产物层：对着**实际安装的** jar 核对

| 检查 | 结果 |
|---|---|
| 4 个 Mixin 都在 `tianfu.mixins.json` 里、都在 jar 里 | PASS |
| 4 个 Mixin 的目标类/方法在**已安装的本体 jar** 里存在 | PASS |
| 11 个 Payload 全部注册，方向正确（S2C=playToClient，C2S=playToServer） | PASS |

本体 jar 的确认：10.20 外层包 `guzhenrn蛊真人模组本体10.20.jar` 的 jar-in-jar 里是
`guzhenren-0.1-neoforge-1.21.1.jar`（sha256 `b6d314d3…`，22371 class，modId `guzhenren`），
与 `_reverse/nested/` 副本**哈希完全一致**，所以后者就是运行时真正的本体。

> 过程中我自己的脚本错了两次，都不是代码问题：
> ① 只查 `TianfuMod.java` 就断言 `TianfuOriginPayload` / `TianfuV2StatePayload` 未注册——
>    它们其实在 `TianfuV2Registration.java` 里注册，方向也对；
> ② 第一版效果审计按「Op 有没有 case 分支」判，误报 27 个——实际其余 Op 是
>    `sum(ids, Op.X)` 暴露成访问器按需查询的。

---

## C. 真专用服务端（这层是你要的「服务器」）

### 环境

本地 `libraries` 里没有 `net/minecraft/server` 的 jar、也没有 NeoForge 安装器，
所以离线装不出服务端。改为联网下载官方安装器：

```
neoforge-21.1.263-installer.jar (6 992 409 bytes)
--installServer → The server installed successfully
```

装出的服务端 + mods：`guzhenrn蛊真人模组本体10.20.jar` + `tianfu-1.4.9.jar`。
RCON 开启（127.0.0.1:25575），用自写的极简 RCON 客户端下发命令。

### 启动

```
[10:52:05] [Server thread/INFO] [minecraft/DedicatedServer]: Done (2.549s)! For help, type "help"
Found mod file "tianfu-1.4.9.jar"
蛊真人天命 1.4.9 (tianfu)
tianfu (version 1.4.8 -> 1.4.9)
```

**tianfu 在专用服务端上正常加载，Mixin 无报错，无 `NoClassDefFoundError`，无 payload 注册错误。**

日志里的 12 条 ERROR **全部来自本体自身的数据**，与 tianfu 无关：

```
Spawn data: Unknown registry key ... guzhenren:deleted_mod_element
Couldn't load tag guzhenren:fangju ...
Parsing error loading recipe guzhenren:nilvhuafenjie
Couldn't load advancements: [guzhenren:zijingsheligu]
```

### 25 条命令（22 成功 / 3 「失败」= 设计如此）

| # | 命令 | 结果 |
|---|---|---|
| 1 | `list` | 0/20 在线 |
| 2 | `neoforge tps` | 20.000 TPS（含本体 5 个维度） |
| 3–6 | `gamerule` / `time` / `weather` / `difficulty` | OK |
| 7–8 | `setblock` / `fill` | 区块未加载（预期） |
| 9–11 | `scoreboard objectives add kaiqiao` + 读写 | OK |
| 12–14 | `summon` 电狼 / 虎 / 熊 | OK |
| 15 | **`summon guzhenren:gu_xiu`** | **OK → 「Summoned new 张照微」** |
| 16 | **`summon guzhenren:jiang_shi_gu_xiu`** | **「Can't find element」← 实证 10.20 已删** |
| 17 | **`summon guzhenren:jiangshiguxiusizhuan`** | **「Can't find element」← 同上** |
| 18–20 | `execute if entity` / `effect give` / `kill` | OK |
| 21–23 | `/tianfu`、`/tianfu vfx on`、`/tianfu open` | **「Unknown」——设计如此，见下** |
| 24–25 | `reload` / `save-all` | OK |

### 关于 `/tianfu` 在服务端「未知」

`TianfuCommands:38` 有 `.requires(CommandSourceStack::isPlayer)`——**命令只对玩家开放**，
控制台/RCON 看不到。注释也写明「控制台没有玩家，`getPlayerOrException` 会抛异常刷屏」。
所以这不是 bug。

**我自己在这上面判错过一次**：先误以为「`TianfuCommands.register` 没人调用」，
加了个 `@SubscribeEvent` 到 `TianfuEvents`；后来发现 `TianfuMod:41` 本来就有
`NeoForge.EVENT_BUS.addListener(TianfuCommands::register)`（我用 grep 找
`RegisterCommandsEvent` 时漏了方法引用写法）。该多余监听已撤销，1.4.9 是干净版。

---

## D. 顺带确认的两件事

### 1. 恶意标签的实体增删，被服务端实证

- `gu_xiu` 在 10.20 **存在**（能召唤，且是带名字的 NPC：苏守拙 / 韩听澜 / 张照微）
- `jiang_shi_gu_xiu`、`jiangshiguxiusizhuan` 在 10.20 **已删**（`Can't find element`）

### 2. ⚠️ `gu_xiu` 归「普通档」可能不对

上次按你选的「归普通档」把 `gu_xiu` 加进了 `malice_normal` + l2hostility 白名单。
但服务端召唤出来的名字是**苏守拙 / 韩听澜 / 张照微**——这是**有名字的 NPC**，
不像妖兽。如果它是友好/剧情 NPC，应该放 `malice_exclude` 而不是 `malice_normal`。

**这条需要你确认**：要不要我把它从 `malice_normal` 挪到 `malice_exclude`？

---

## E. 没能做到的

- **没有真人/机器人客户端进服**：`/tianfu` 只对玩家开放，而我没有可操控的客户端，
  所以「按 K 开界面、买十绝体、确认后看 tizhi」这条链路**无法在服务端跑通**。
  这部分由 A 组的 72 条逻辑用例覆盖（规则层面），界面与联机同步仍需真人点一遍。
- **联机双客户端**：同样受限于没有第二个客户端。

---

## F. 交付

```
javac exit: 0 / sources: 70
gradlew.bat build  BUILD SUCCESSFUL
jar: tianfu-1.4.9.jar  313420 bytes
sha256 E52F54B688C38462650AB02E91D69D4AE410A86B0D1DA6F4FE0EAF2E19547C8E
已装入整合包 + 测试服
```

| 版本 | 内容 |
|---|---|
| 1.4.8 | 误加的重复命令监听（已撤销） |
| **1.4.9** | 撤销 1.4.8 的多余监听，等价于 1.4.7 + 本轮测试结论 |

测试脚本：`_crash6/harness/`（逻辑用例）、`_crash6/rcon_test.py`（25 条命令）、
`_crash6/audit_shipped_artifacts.py`（产物核对）、`_crash6/testserver/`（服务端）。
