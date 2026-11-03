# tianfu 1.4.6 — 分页自由切换 · 一转排查 · 全天赋效果核对 · 多人模式审计

> 2026-10-31

## 一、分页自由切换（已改）

**原来的限制**：左侧分页只有在停在「确认」页时才能点
（`canJumpToReviewStep` 要求 `wizardStep == REVIEW`），而且
「未选难度」「未选资质锚点」是**硬拦截**——点过去直接 `return`。
结果只能一页一页按「下一步」，想回头核对已选项都很别扭。

**现在的口径**：

| 行为 | 之前 | 现在 |
|---|---|---|
| 点左侧任意分页 | 仅「确认」页可点 | 任意页随时可点 |
| 未选难度时切页 | 硬拦，切不过去 | 允许，只给提示 |
| 未选资质锚点时切到后面页 | 硬拦 | 允许，只给提示 |
| 已最终确认后翻阅 | 强制回「确认」页 | 允许翻阅，按只读处理 |
| 真正的门禁 | 多处 | **只剩 `canConfirm()`**（已选难度 + 已有资质锚点） |

改动：`TianfuViewScreen.showStep()` 去掉邻接/REVIEW 限制与两处硬 `return`，
改为只设 `notice`；分页点击不再过闸；左侧按钮一律显示为可点；删掉已成死代码的
`canJumpToReviewStep`。

## 二、「选空窍资质 / 十绝体后没突破到一转」——**未复现**

### 存档证据（读 `playerdata` 原始 NBT）

| 存档 | 已购 | tizhi | kongqiao | jieduan | zhuanshu | 一转 |
|---|---|---|---|---|---|---|
| `1\...\新的世界 (1)` 10-30 19:33 | `sijue_10` … | 10 | 3 | **1** | **1** | 是 |
| `1\...\新的世界` 10-30 13:02 | `v2_apt_bing` … | 0 | 5 | **4** | **5** | 是 |
| `蛊2\...\新的世界 (1)` 10-27 | `fuze_mianchang` | 0 | 0 | **1** | **1** | 是 |
| `蛊2\...\新的世界` 10-26 | `sijue_3` … | 3 | 3 | **1** | **1** | 是 |

四个存档没有一个卡在零转。最新的那个（装了 1.4.5 之后新开的）买的是 `sijue_10`，
`jieduan=1 / zhuanshu=1`，正常进一转。

### 代码路径

`TianfuOpening.onUseHopeGu()` 第 89 行调 `UikongqiaoquedingProcedure.execute(level, player)`，
本体收尾里有：

```java
if (jieduan == 0) { jieduan = 1; zhuanshu = 1; markSyncDirty(); }
```

tianfu **自己不写** `jieduan`/`zhuanshu`（全工程只有两处注释提到），完全靠本体这一步。
所以只要开窍流程走过 `queding`，一转就成立——这条在 9.8 / 10.4 / 10.20 三个版本都验证过
（9.8 收尾在 `queding` 本体，10.x `queding` 转调 `guanbi`）。

> 结论：这条不是 bug。若你实际遇到卡零转，那是另一个现象，需要现场 `/tianfu open` 的输出。

## 三、全部天赋效果是否生效——**没找到静默失效**

### 判据（第一版脚本判据是错的，已修正）

第一版我按「Op 有没有 `case` 分支」判，报了 27 个 Op 无分支——**是假警报**：
`applyOnce` 只处理一次性字段类，其余 Op 是**按需查询**（`sum(ids, Op.X)` 暴露成访问器）。
换成正确判据后：

### 结果

| 检查 | 结果 |
|---|---|
| `Op` 枚举总数 | 40 |
| 数据表用到的 Op | 38，**全部有消费者** |
| 20 个效果访问器（regen/xiulian/crit/dodge/leech/reduction…） | **全部有外部调用者**（`TianfuCombat` / `TianfuEffects`） |
| 聚合器 `undying` / `aura` / `onHitEffects` / `bestReflect` / `counter*` | **全部有外部调用者** |
| 一次性类 `FIELD_MULT/FIELD_ADD/DAOHEN/DAOHEN_ALL/APTITUDE_SET` | `applyOnce` + `revokeOnce` 成对 |
| 效果引用的本体字段 | 44 个，**全部存在** |
| 跨附属 buff id | 3 个，**全部核实存在** |

跨附属 buff 逐个核对（拿实际装的 jar 扫注册字符串）：

| tianfu 里的 id | 提供者 | 结论 |
|---|---|---|
| `hundao_addon:hundao_zengfu` | 魂道附属10.29（modId `hundao_addon`） | ✓ |
| `yundao:yizhi_qiankun` | 运道附属（modId `yundao`） | ✓ |
| `guzhenren_addon:ice_path_boost` | 兼修附属0.7（modId `guzhenren_addon`，`IceContent.class`） | ✓ |

事件链也确认完整：`TianfuMod:39` 注册 `TianfuEvents` →
`onTick` → `TianfuEffects.onTick`；伤害事件 → `TianfuCombat`。

> 说明：这只证明「每条效果都有执行路径、引用的字段/注册名都真实存在」，
> **不等于**数值体验正确。数值平衡仍需实机。

## 四、多人模式审计

| 检查项 | 结果 |
|---|---|
| 包方向 | S2C 全部 `playToClient`、C2S 全部 `playToServer`，无错向 |
| 客户端类引用 | `TianfuStatePayload` 引用 `TianfuClientHandler`，但只在 S2C lambda 内，服务端不会执行 |
| 写本体字段是否标脏 | 5 个文件写 `PlayerVariables`，**全部**调用 `markSyncDirty()` |
| 按 UUID 键控的静态表 | `COUNTER_TICK` / `LAST_APPLY` 都是 `ConcurrentHashMap`，死亡/克隆/下线有 `forget()` |

### 发现并修掉一个（多人/长跑才会显现）

`TianfuZhuCombat.LAST_APPLY` 按**目标实体 UUID** 记录节流，只在
`LivingDeathEvent` 清理。实体随**区块卸载**离场时不会触发死亡事件，于是
「被打过又随区块卸载的怪」会永久留一条记录——长跑服务器上是持续增长的内存泄漏。

修法：新增 `EntityLeaveLevelEvent` 监听，实体离场时一并 `forget`。

> 本轮**没有**真机跑专用服务端 + 双客户端。以上是静态审计 + 数据流检查，
> 不是联机实测。

## 五、空窍资质的 kongqiao：逻辑是对的，**描述文字是错的**（1.4.7 已修）

上一轮遗留问题：买「丙等空窍」（描述写 `kongqiao=6`），存档里却是 **5**。

### 挖出本体规则（`KE1Procedure` 字节码实测）

`putfield PlayerVariables.kongqiao` 在 KE1 里只有 3 处，前面有一段守卫
`if (kongqiao != 3.0)`，然后按真元上限反推：

| 真元上限 | 反推出的 kongqiao | 对应档位 |
|---|---|---|
| 800–999 | **2** | 甲 |
| 600–799 | **1** | 乙 |
| 400–599 | **5** | 丙 |
| 200–399 | 不匹配 → 保持原值 | 丁（写 8 不被覆盖） |

这与本体「机缘」的编码完全一致（`applyJiyuan` 里 甲→2、乙→1、丙→5、丁→8）。

**关键**：`if (kongqiao != 3.0)` 这个守卫意味着**十绝体（kongqiao=3）不会被反推**，
所以十绝体写入的 `kongqiao=3` 是稳定的——这也是「十绝体不会变成凡人」的机制保证。

### 所以真实链路是

```
v2_apt_bing → APTITUDE_SET 写 kongqiao=6
            → applyAptitudeTier 写 zuida_zhenyuan=roll(400,590)=516
            → KE1 看到 kongqiao(6)≠3，按 516 落在 400–599 → 反推 kongqiao=5
```

最终 **5 = 丙等的正确编码**。逻辑没错，错的是天赋描述里写的「kongqiao=6 / 4」。

### 1.4.7 的修改

- 四条空窍资质的描述改成**最终**档位值，并标出真元区间：
  丁「最终 kongqiao=8，真元 200–390」／丙「最终 5，真元 400–590」／
  乙「最终 1，真元 600–790」／甲「最终 2，真元 800–990」。
- 在 `APTITUDE_SET` 数据表处和 `TianfuNative.applyAptitudeTier` 的 javadoc 里
  写清「写的初值只是过渡，最终由 KE1 按区间反推」，避免以后再被这个 6→5 绕进去。

**没有改任何逻辑**——只改文字与注释，因为反推结果本来就是对的。

## 六、验证

```
javac exit: 0 / sources: 70
gradlew.bat build  BUILD SUCCESSFUL in 8s（1.4.7）
jar: tianfu-1.4.7.jar  313419 bytes
sha256 3560F204B690671D86B2F863CE85A3FA3125447040D47C881940EABC1CD9C697
已装入整合包（旧版本已删）
```

### 版本递进

| 版本 | 内容 |
|---|---|
| 1.4.6 | 分页自由切换 + 多人 `LAST_APPLY` 泄漏修复 |
| 1.4.7 | 空窍资质描述改为真实最终值（纯文案/注释） |

