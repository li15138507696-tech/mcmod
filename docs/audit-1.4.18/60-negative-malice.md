# 60 · 负面天赋 + 恶意天赋 只读审查（1.4.17 → 准备 1.4.18）

- 审查对象：`tianfu/src/main/java/net/guzhenren/tianfu/`（附属）；恶意联动外mod：`D:\daima\mcmod\蛊真人恶意`（`guzhenren_malice` 1.7.0）
- 环境：MC 1.21.1 / NeoForge 21.1.x / Java 21；本体 10.20（`_crash3/basever/guzhenren-10.20.jar`）
- 标记约定：**【已验证】** = 有代码/资源证据；**【未验证】** = 需实机或需反编译本体其它过程
- 本文件为唯一写入物，未改动任何源码/构建/版本/doc 既有文件。

---

## ① 负面天赋全表（脚本生成）

枚举方式：脚本对 `TianfuTalents.java` 做括号配平解析（不是手抄），提取全部 `t("id","名","品质",cost,"描述",fx...)` 语句并筛 `cost < 0`。
脚本结果：**总词条 166 条，其中负面 20 条，返还点数合计 73 点。**

```
TOTAL ENTRIES: 166
NEGATIVE ENTRIES: 20
SUM_GRANT: 73
COST DISTRIBUTION: -8:3  -5:5  -2:12 | 1:35 2:39 3:31 5:27 6:1 10:1 12:10 16:2
```

另外 **3 条出身（Background）也是负成本**，走 `TianfuEconomy.selectOrigin()` 计入同一个 `negativeGrant` 账（`v2/TianfuOriginTable.java:84/87/90`），但**只能选 1 条**：

| 出身 id | 中文名 | 返还 | 效果 |
|---|---|---|---|
| `foundling` | 弃婴 | -3 | 气运上限 −30%，暴击伤害 +30%（`TianfuOriginTable.java:84-86`）|
| `orphan` | 亡族遗孤 | -2 | 气运上限 −20%，44 项非汇总道痕各 +150（`:87-89`）|
| `clan` | 家族子弟 | -1 | 气运上限 −10%，确认时元石 +20%（`:90-92`）|

### 20 条负面词条逐条（id | 中文名 | 返还 | 实际负面效果（代码位置）| 属性维度）

| # | id | 中文名 | 返还 | 实际负面效果（代码证据） | 属性维度 |
|---|---|---|---|---|---|
| 1 | `v2_jingli_buji` | 精力不济 | 2 | `zuida_jingli` ×0.80 → `TianfuTalents.java:150`（FIELD_MULT）→ `applyOnce` `:631` → `multiply() :1135-1145`，仅在**确认结算时乘一次** | 其它（精力上限）|
| 2 | `v2_gu_cd_ningse` | 催蛊凝涩 | 2 | `GU_CD −0.10`（`:151`）→ `TianfuNativeProgression.java:66-69` → `Guzhenren98Progression.itemCooldownTicks():57-63`（`ticks = ceil(native/0.9)`，即冷却 **+11.1%**）| 其它（蛊冷却）|
| 3 | `v2_zhenyuan_kubai` | 真元枯败 | 5 | `REGEN_ZY −0.75`（`:152`）→ `TianfuNativeProgression.java:46-56` → `recovery():29-34`（`rate = max(0, 1+bonus)`）→ 只缩放**正向**回复增量 | 真元恢复 |
| 4 | `v2_routi_canque` | 肉体残缺 | 5 | `HEALTH_MULT −0.50`（`:153`）→ `TianfuAttributes.java:62-67` `ADD_MULTIPLIED_TOTAL` | 生命上限 |
| 5 | `v2_shouyuan_kuwei` | 寿元枯萎 | 5 | `FIELD_ADD shouyuan −250`（`:154`）→ `applyOnce :632` → `add() :1110-1113` | 寿元 |
| 6 | `v2_qiyun_duanjue` | 气运断绝 | 5 | `FIELD_MULT qiyun_shangxian −0.375`（`:155`）→ ×0.625（乘**当时**上限，`multiply() :1135`，不做 0 兜底但 0×0.625=0 安全）| 气运 |
| 7 | `v2_xinzhi_yousun` | 心智有损 | 5 | `XIULIAN −0.40` + `BREAKTHROUGH −0.50`（`:156`）→ `NativeProgression.java:14-23 / 35-44`（修炼 rate 0.60；突破失败率 ×1.5）| 修炼速度 + 突破成功率 |
| 8 | `v2_aptitude_shackled` | 空窍桎梏 | 8 | `V2_APTITUDE_LOCK` + `niantou_rongliang ×0`（`:157`）；**实际落地在适配层**：`NeoForge1211Adapter.java:161-164`（`niantou_rongliang -= abs(...)` → 0，并记 `v2_lock_kongqiao`）+ `:196-201` 每 tick 把 `kongqiao` 压回锁定值 | 念头容量 + 资质 |
| 9 | `v2_luck_forfeit` | 天弃之命 | 8 | `V2_LUCK_ZERO`（`:158`）→ `NeoForge1211Adapter.java:190-193` **每 tick** `qiyun = 0; qiyun_shangxian = 0` | 气运 |
| 10 | `v2_lifespan_scar` | 命烛残尽 | 8 | `FIELD_ADD shouyuan −400`（`:159`，该 FIELD_ADD 被 `TianfuApply.java:282-284` 的 `ADAPTER_HARD_COSTS` 跳过）+ 适配层 `NeoForge1211Adapter.java:165-168` 扣 400 并记 `v2_lifespan_cap`，`:204-213` 每 tick `min(shouyuan, cap)` 封回 → **寿元不得再增长** | 寿元 |
| 11 | `zhenyuan_kujie` | 真元枯竭 | 2 | `REGEN_ZY −0.30`（`:333`）| 真元恢复 |
| 12 | `niantou_ganhe` | 念头干涸 | 2 | `REGEN_NT −0.30`（`:334`）→ `NativeProgression.java:58-64` | 念头（恢复）|
| 13 | `routi_chanruo` | 肉体孱弱 | 2 | `HEALTH_MULT −0.20`（`:335`）→ 与 #4 **乘算**：0.5×0.8 = **0.40** | 生命上限 |
| 14 | `quanshi_piruan` | 拳势疲软 | 2 | `ATTACK_MULT −0.10`（`:336`）→ `TianfuAttributes.java:68` | 伤害输出 |
| 15 | `shenfa_chidun` | 身法迟钝 | 2 | `SPEED_MULT −0.08`（`:337`）→ `TianfuAttributes.java:69` | 移动 |
| 16 | `shouyuan_duancu` | 寿元短促 | 2 | `FIELD_ADD shouyuan −100`（`:338`）| 寿元 |
| 17 | `qiyun_xibo` | 气运稀薄 | 2 | `FIELD_MULT qiyun_shangxian −0.15`（`:339`）→ ×0.85 | 气运 |
| 18 | `hunli_shuairuo` | 魂力衰弱 | 2 | `zuida_hunpo ×0.75` + `hunpochengzaijixian ×0.75`（`:340-341`）| 其它（魂魄上限/承载）|
| 19 | `nianfu_xiayai` | 念府狭隘 | 2 | `niantou_rongliang ×0.75`（`:342`）| 念头容量 |
| 20 | `xiuxing_chizhi` | 修行迟滞 | 2 | `XIULIAN −0.30` + `BREAKTHROUGH −0.40`（`:343-344`）→ 与 #7 加性叠加：修炼 rate **0.30**，突破失败率 **×1.9** | 修炼速度 + 突破成功率 |

**维度覆盖空缺（重要）**：`真元上限`、`减伤`、`防御` 三个维度**没有任何负面条目**。也就是说玩家可以「零代价换点」，只要他不介意被削的维度是自己不用的那些。

**结算时序要点【已验证】**：`FIELD_*` 只在**最终确认结算**那一刻写入一次（`TianfuApply.java:225-255`，先 `applyBaseEffects` 再 `adapter.settle`），之后 `TianfuData.isConfirmed()==false` 时效果一律不生效（`TianfuApply.effective():63-74`）。所以负面代价**确认后不可撤销**（`TianfuApply.revoke():147-149` 拦 `isConfirmed||isSettled`）。

---

## ② 同属性可重复刷点组合与累计点数

**机制前提【已验证】**：同一 id 不能重复购买（`TianfuEconomy.buy():106-108` `CONFLICT`），所以「刷点」= **在同一属性维度上买多条不同的负面**。各负面的数值与价格**严格线性**（V2 的 −5 档恰好是旧 −2 档数值×2.5、价格×2.5），因此**决定性价比的不是「这条负面有多重」，而是「该维度是否已被别的负面打饱和」**。

### 同维度分组（可同时购买）

| 组 | 条目（返还）| 合并后真实效果 | 累计返还 | 是否饱和→白拿 |
|---|---|---|---|---|
| **A. 气运** | `v2_luck_forfeit`(8) + `v2_qiyun_duanjue`(5) + `qiyun_xibo`(2) | 气运与上限**恒为 0**（`NeoForge1211Adapter:190-193` 每 tick 清零）。另两条的 ×0.625 / ×0.85 落在**已被清零**的字段上 → 边际代价 **0** | **15** | **白拿 7 点**（若另加出身 `foundling` −3 → 再白拿 3 点）|
| **B. 寿元** | `v2_shouyuan_kuwei`(5) + `shouyuan_duancu`(2) + `v2_lifespan_scar`(8) | 合计 **−750 寿元**，且 `maintain` 每 tick 把寿元压回 cap（`:204-213`）→ **永久不可回复**。每条都有真实边际代价 | **15** | 否（真实损失）|
| **C. 修炼+突破** | `v2_xinzhi_yousun`(5) + `xiuxing_chizhi`(2) | 修炼 rate 0.30（−70%）、突破失败率 ×1.9。两条同时作用于**同一对**维度 | **7** | 部分（见下）|
| **D. 生命上限** | `v2_routi_canque`(5) + `routi_chanruo`(2) | 乘算 0.5×0.8 = **×0.40**（−60% 生命上限）| **7** | 否 |
| **E. 真元恢复** | `v2_zhenyuan_kubai`(5) + `zhenyuan_kujie`(2) | rate `max(0, 1−1.05)` = **0** → 真元**完全不再自然恢复**。单买 #3 时是 0.25（25%） | **7** | 否，但第 2 条边际价值极低（25%→0）|
| **F. 念头容量** | `v2_aptitude_shackled`(8) + `nianfu_xiayai`(2) | 桎梏已把 `niantou_rongliang` 减到 **0**（`:161-164`）→ ×0.75 落在 0 上 → 边际代价 **0** | **10** | **白拿 2 点** |
| G. 单条维度 | 精力上限 / 蛊冷却 / 伤害输出 / 移动 / 魂魄 / 念头恢复 | 各自唯一 | 2 each | — |

### 「同一维度最多叠几条」

- 气运维度全买 = **3 条 / 15 点**（A 组）。
- 寿元维度全买 = **3 条 / 15 点**（B 组）。
- 修炼+突破维度全买 = **2 条 / 7 点**（C 组，且同时削两个维度）。
- 总账上限恒为 `NEGATIVE_CAP = 24`，而负面总供给 73 点 → **上限才是唯一限流器**。

### 组合到 24 点的整数解（穷举成立）

| 组合 | 点数 | 玩家净得失 |
|---|---|---|
| `8+8+8`（天弃之命 + 空窍桎梏 + 命烛残尽）| **24** | 气运永久 0、念头容量 0、寿元 −400 且不可回复 |
| A(15) + C(7) + 任一条 2 点 | **24** | 气运 0 + 修炼 0.30 + 突破 ×1.9 + 另一条 |
| A(15) + D(7) + 2 点 | **24** | 气运 0 + 生命 ×0.40 |
| B(15) + E(7) + 2 点 | **24** | 寿元 −750 + 真元恢复 0 |
| F(10) + B 的 5+2+8=15 → 25 ❌ | 超限 | 被整笔拒绝（见 ③）|

### 结论：白拿总额

- **纯白拿（维度已被打饱和、边际代价为 0）= 9 点**：气运组 7 点 + 念头容量组 2 点。若再算出身 `foundling`/`orphan`/`clan`（−3/−2/−1，全部只削气运，而气运已被天弃之命归零）→ **再加最多 3 点，合计 12 点**。
- 玩家开局基础点：`顺天应命 8 / 凡尘问道 6 / 逆天争命 4 / 万劫磨道 2`（`TianfuDifficulty.java:17-21`），另转世加成 ≤10（`:47-51`）。补点 24 点意味着：
  - 万劫磨道：2 → 26 点（**13 倍**预算），其中 12 点零边际代价；
  - 顺天应命：8 → 32 点，最高（含转世 +10）**42 点**。
- 即：**「负面可以很重，但补点价值必须匹配」这一条目前不成立**——因为存在 9~12 点完全零成本的补点。

---

## ③ NEGATIVE_CAP / planRoll 上限推演

### 3.1 常量与用法【已验证】

- `NEGATIVE_CAP = 24` —— `v2/TianfuSelection.java:11`。（任务书说"TianfuSelection"，此处准确位置为 `v2/TianfuSelection.java`）
- 使用点一：`TianfuEconomy.buy():174-178`

```java
int newGrant = grant + Math.max(0, -cost);      // 整条全额计入，不做部分发放
if (newGrant > NEGATIVE_CAP) return fail(state, NEGATIVE_CAP);
if (cost > points)           return fail(state, INSUFFICIENT_POINTS);
```

- 使用点二：`TianfuEconomy.selectOrigin():292-296`（出身同样整条计入）
- 使用点三：`TianfuEconomy.revoke():216-219`（`grant -= receipt.grant()`，`<0` 拒绝）
- 落库一致性：`TianfuV2Store.commit():254-258` 用 `deriveGrant(receipts, originReceipt)` 反算，与快照不符**直接抛异常**，无法通过伪造 `negativeGrant` 绕过。
- UI 文案与常量一致：`client/TianfuUiRules.java:51`、`client/TianfuViewScreen.java:487 / 570`、`net/TianfuBuyPayload.java:65`。

### 3.2 「超上限时是整笔拒绝还是砍到上限」

**整笔拒绝，零部分发放，断点原子。** 依据：

1. `buy():176-178` 在扣点之前 `return fail(...)`，`fail()` 返回**原快照实例**（`:48-50`），`TianfuApply.buyReason():116-118` 拿到 `!ok` 直接返回原因、**不 commit**；`TianfuV2Store.commit()` 还要求 `current.revision() == change.expectedRevision()`（`:247-249`）。
2. `acceptRoll():327-336` 逐条调 `buy()`，任一失败即 `return fail(state, step.reason())` —— **回滚到入参快照**，不会留下「已买一半」。
3. 拒绝原因 `NEGATIVE_CAP` 会回显给玩家（`net/TianfuBuyPayload.java:65`："负面补点已达上限 24 点，不能再选负面天赋"）。

**推演：上限之内最多叠几个同属性负面？**
- 气运组 3 条 / 15 点 ✅ 全部可买（15 ≤ 24）。此时余额 grant=15，剩 9 点预算。
- 若再买 B 组的 −8（命烛残尽）→ 15+8 = **23 ≤ 24 ✅ 允许**；再买 −5 → 28 ❌ **整笔拒绝**。
- 也就是说：**气运 3 条 + 寿元 1 条 = 4 条跨两维度**可到 23 点；**同维度最多 3 条**（气运/寿元），因为同维度只剩 3 条。
- 边界：`newGrant == 24` **允许**（判据是 `>`），所以 `8+8+8` 正好 24 成立。

### 3.3 planRoll 的双重上限是否挡得住「随机批次刷满负面」

`TianfuEconomy.planRoll():388-401`：

```java
for (String id : shuffled(negatives, rng)) {
    if (negativesPicked >= 6 || working.negativeGrant() >= 12) break;   // ← 买之前才判
    TianfuSelection.Result step = buy(working, id);
    if (!step.ok()) continue;
    picked.add(id); working = step.snapshot(); negativesPicked++;
}
```

**判定：挡不住 12 点，只能挡住 24 点（靠 buy 内部）。**

原因：12 点是**买前判据**，最后一条会把总额顶过 12，泄漏量 = 「买前 ≤11」+「单条最大 8」。用 DP 穷举 `{8×3, 5×5, 2×12}` 全部购买次序（状态只依赖数量与已买数，精确解）：

```
planRoll max final negativeGrant = 19
example order (prefix bought): 5, 2, 2, 2, 8   → 5+2+2+2=11（买前 11<12 通过）→ +8 = 19
```

- **一次随机批次最多可拿到 19 点负面补点（19/24）**，而非 12。
- 之后再手工补一条 −5 → **24 点正好打满**。第二次 planRoll 从 grant=19 起算，任何 −8/−5 都会被 `buy()` 拒（19+8=27、19+5=24 ✅ 其实 −5 可以），−8 不可、−5 可。
- `negativesPicked >= 6` 也不是紧约束（组合 5,2,2,2,8 只用 5 条）。
- 本世 3 次摇号（`TianfuRoll.MAX_ROLLS`，`net/TianfuRollPayload.java:65-80`）叠加也逃不出 24：每次接受都重新过 `acceptRoll → buy`。

**一致性结论**：`planRoll` 的 12 与 `NEGATIVE_CAP` 的 24 **不一致**（差 12 点；实际泄漏 7 点）。`planRoll` 的注释「补到 12 点即停，避免整批全是负面」（`:388`）**与实现不符**。

### 3.4 附带发现：恶意组门槛可被随机批次绕过【已验证·中危】

- `TianfuPool` 只有 `maliceLoaded()` 为真时才把恶意词条注册进池（`TianfuPool.java:140-142` → `TianfuTalents.maliceSpecs():492-501` 未装 l2hostility 返回空表）。
- 但 `TianfuEconomy.catalogCost():33-40` 在 `TianfuPool.byId` 落空后**回退查 `TianfuTalents.byId`**（不查 l2hostility），`availableForPurchase():43-46` 也只看 `TianfuTalents.isAvailable`（`isAvailable:522-526` → `available():562-570` 只检查 `MOB_EFFECT` 的附属），恶意条目全是非 `MOB_EFFECT` 原语 → **恒为可用**。
- `planRoll():364-373` 遍历的是 `TianfuTalents.all()`（含 `MALICE`），只按 `cost>0` 归入 `positives` → **未装 l2hostility 时恶意条目仍会进随机候选，且 `buy()` 会成功**。
- 后果：`恶意_*` 在未装恶意时可通过「随机批次」买到并生效（效果是 tianfu 自己落的，不崩）。设计约束「必须装恶意才可见/可选」被绕过。**建议在 `catalogCost`/`availableForPurchase` 加 `id.startsWith("malice_") && !maliceLoaded() → 拒绝`。**

### 3.5 死代码：`TianfuRoll.generate()` 无补点上限

`TianfuRoll.java:94-115` 的贪心循环对负成本条目「永远买得起」（`:105`）且只有 `MAX_STEPS=64` 硬闸，**完全没有 `negativeGrant` 上限**。目前 `generate()` 已无调用方（`grep` 仅剩 `MAX_ROLLS` 引用，随机流程已改走 `planRoll`，`net/TianfuRollPayload.java:71-80`）。若被重新启用，会产生 >24 的整批 → `acceptRoll` **整批拒绝**、`picked` 为空 → 玩家白摇一次并消耗次数（`:75-79` 只在 plan 为空时提示，`generate` 路径不会）。建议删除或补限。

---

## ④ 补点价值不匹配清单

### 4.1 「负面极重但只给 2 点」（补点偏低，玩家被亏）

| 条目 | 返还 | 实际严重度 | 说明 |
|---|---|---|---|
| `xiuxing_chizhi` 修行迟滞 | 2 | 修炼 −30% **且** 突破失败率 ×1.4（与 #7 叠加后 ×1.9）| 同时削两个维度、且贯穿整个修炼期，2 点是全表最亏的一条 |
| `hunli_shuairuo` 魂力衰弱 | 2 | 魂魄上限 −25% + 承载 −25% | 对魂道/十绝体 3 号流是重伤；与同为 2 点的「精力上限 −20%」代价量级完全不同 |
| `niantou_ganhe` 念头干涸 | 2 | 念头回复 −30% | 智道流整条线被削 30%，仅 2 点 |
| `v2_gu_cd_ningse` 催蛊凝涩 | 2 | 全部蛊冷却 **+11.1%** | 影响面是全流派、全游戏期，2 点偏低 |
| `quanshi_piruan` 拳势疲软 | 2 | 出伤 −10%（永久乘区）| 与 −5 档的「真元枯败」相比，DPS −10% 换 2 点明显偏便宜 |
| `v2_jingli_buji` 精力不济 | 2 | 精力上限 −20% | **【未验证】** 精力在本体玩法中的作用路径未确认，若精力几乎不用则是「白拿」，若用于炼蛊/战斗则是重伤 |

### 4.2 「负面很轻却给 5 点以上」（补点偏高，可刷）

| 条目 | 返还 | 为什么偏轻 |
|---|---|---|
| `v2_luck_forfeit` 天弃之命 **+** 气运组另两条 | 8（+7）| 气运归零后，`v2_qiyun_duanjue`(−5)、`qiyun_xibo`(−2)、以及出身 `foundling/orphan/clan` 全部变成**零边际代价**。**合计 7（+3 出身）= 纯白拿** |
| `v2_aptitude_shackled` 空窍桎梏 **+** `nianfu_xiayai` | 8（+2）| 若玩家本就不买资质档位（不买 `v2_apt_*`、不买十绝体），"锁定原资质"**零成本**；念头容量归零只打击智道流。**净白拿 2 点**，且桎梏本身 8 点对一个不打算突破资质的 build 近乎免费 |
| `v2_shouyuan_kuwei` 寿元枯萎 | 5 | −250 寿元。**【未验证】** 本体 `shouyuan` 的基数/上限未确认；若基数远超 250，这条 5 点几乎无感（唯一硬兜底是确认时 `validLifespan(shouyuan) > 0`，`Guzhenren98Settlement.java:11-13` + `NeoForge1211Adapter.java:116-121`）|
| `v2_qiyun_duanjue` 气运断绝 | 5 | 气运上限 ×0.625。气运在本体的主要作用见 `nativeBreakthroughChance():37-41`（`luck*0.01` 一项），单独削 37.5% 上限对高转数玩家影响有限 → 5 点偏高 |

### 4.3 结构性结论

**V2 的 −5/−8 档与旧 −2 档数值严格成正比（×2.5、×4），没有梯度溢价也没有梯度折扣**，所以「负面越重给点越多」在数值上是公平的；不公平**只来自维度饱和**（4.1 的 9~12 点纯白拿）和维度空缺（无 `真元上限 / 减伤 / 防御` 负面）。

---

## ⑤ 恶意天赋逐项核实（触发对象 + 执行效果）

### 5.0 实现落点（关键前提）

- 恶意词条表：`TianfuTalents.java:365-403`，14 条，成本梯度沿用 1/2/3/5/12。
- **tianfu 完全不调用 l2hostility / guzhenren_malice 的任何类**（类注释 `:357-359`：「不调用恶意的任何类——完全由本 mod 自己的原语落地」）。唯一联动是门禁：`ModList.isLoaded("l2hostility")`（`:417-426`）。
- 外 mod `guzhenren_malice` 1.7.0：`modId = guzhenren_malice`，**`l2hostility` 为 required `[3.0.18]`**（`蛊真人恶意/src/main/resources/META-INF/neoforge.mods.toml`）。它注册的标签（`MaliceTags.java`）：
  - `guzhenren:malice_normal`（30 条）`malice_elite`（62）`malice_boss`（13）`malice_exclude`（22）
  - `l2hostility:whitelist`（**105** 条，`data/l2hostility/tags/entity_type/whitelist.json`）
  - 分级配置 `data/guzhenren/l2hostility_config/entity/guzhenren_malice.json`：normal/elite/boss 三档的 `healthScale/attackScale/maxTraitCount` 与 trait 白名单，且 normal/elite 的 `blacklist` 明确排除 **`l2hostility:undying` / `adaptive` / `dementor` / `dispell` / `ragnarok` / `levitation`** —— 正好对应 tianfu 侧的 4 个同名词条，说明「这些强度只给玩家、不给怪」是刻意设计。

### 5.1 状态效果叠加（缓慢 / 毒素 / 虚弱 / 凋零 / 致盲）

**tianfu 侧（玩家 → 目标）**（`TianfuCombat.java:385-396`）：

```java
private static void applyOnHit(ServerPlayer attacker, Set<String> ids, LivingEntity target) {
    for (var fx : TianfuTalents.onHitEffects(ids)) {
        var holder = TianfuTalents.effectHolder(fx.arg());
        if (holder == null) continue;
        target.addEffect(new MobEffectInstance(holder, (int) fx.a(), (int) fx.b()));
    }
}
```

| 词条 | 效果 | 时长 | 等级 | 代码 |
|---|---|---|---|---|
| `malice_slowness` 流沙缚足 (1点) | `minecraft:slowness` | 100 tick = 5 s | amp 1 = **II** | `:369-370` |
| `malice_poison` 毒蚀之能 (2) | `minecraft:poison` | 5 s | II | `:373-374` |
| `malice_freezing` 寒流侵体 (2) | `minecraft:slowness` | 5 s | II | `:375-376` |
| `malice_weakness` 卸力之能 (2) | `minecraft:weakness` | 5 s | II | `:377-378` |
| `malice_blindness` 夺明之术 (2) | `minecraft:blindness` | 60 tick = 3 s | I | `:379-380` |
| `malice_wither` 枯荣之力 (3) | `minecraft:wither` | 5 s | amp 1 = **II** | `:387-388` |

逐项判定：

1. **施加给谁**：**被打的目标**（`LivingEntity target`），不是玩家自己。触发条件是「伤害来源可解析为 `ServerPlayer`」（`attackerPlayer():150-163`，含投射物 owner 回溯）且被击者 `instanceof LivingEntity`（`:124-127`）。→ **不会自伤，也不会误伤队友（只打命中的那一个）。**
2. **时长/等级**：全部**有限时长**，5 s / 3 s，等级 I~II。**没有无限时长**。
3. **是否被本体每秒 clamp 掉**：`docs/audit-1.4.18/_raw_buffliucheng.txt`（本体 `BuffLiuCheng1Procedure` 完整 javap 导出）内 `removeEffect=0 / addEffect=0 / clearEffects=0 / getDuration=0 / setDuration=0`，只有 `getEffect(...)` + `getAmplifier()` 的**只读**访问 → **该过程不改写任何效果时长**。故原版 debuff 不会被本体每秒清掉。**【部分未验证】**：本体是否在**其它**过程里清 HARMFUL 效果未穷举。
4. **是否每 tick 重复 `addEffect`**：**否**。tianfu 的「每 tick 重挂」风险只在常驻 `MOB_EFFECT` 路径，而那条路径已正确判 `getDuration() == -1`：
   ```java
   // TianfuTalents.java:971-984
   MobEffectInstance existing = player.getEffect(effect);
   boolean inPlace = existing != null && existing.getDuration() == -1
                     && existing.getAmplifier() == grantedAmp;
   if (!inPlace) { ... player.addEffect(new MobEffectInstance(effect, -1, amp, true, false)); }
   ```
   且只在 `player.tickCount % 100 == 0` 时被调（`TianfuEffects.java:359-362`）。**任务书提到的「`< 600` 恒真导致重复施加」这个坑，在本版本已修好（有注释显式说明）。**
5. **效果 id 解析**：走注册表真实 holder（`TianfuTalents.effectOf():1086-1089` `BuiltInRegistries.MOB_EFFECT.getHolder(id)`），**没有用 `Holder.direct`**（注释 `:1084-1085` 显式警告）。→ 任务书第 2 个教训也已避开。

**外 mod 侧（怪 → 玩家）** `蛊真人恶意/.../hundred/RealmProfile.java`：

| 触发 | 对象 | 效果 | 时长/等级 | 代码 | 默认配置 |
|---|---|---|---|---|---|
| POISON 型妖兽命中 | 被击者（玩家）| `POISON` | `POISON_SECONDS*20` | `:90-98` | **4 s / amp 1**（`HundredConfig.java:61-62`）|
| realm-3 妖兽每 `QUAKE_INTERVAL` | 半径内所有 `Player`（含队友）| 击退 + `MOVEMENT_SLOWDOWN` | 100 tick = 5 s / **II** | `:155-167` | 半径 3.0、间隔 **120** tick（`:63-64`）|
| realm-4 妖兽每 `AURA4_INTERVAL` | 半径内所有 `Player`（含队友）| `WEAKNESS` + `MOVEMENT_SLOWDOWN` | 60 tick = 3 s / **I** | `:169-178` | 半径 8.0、间隔 **100** tick（`:65-66`）|
| realm-1 妖兽每 20 tick | **友方妖兽**（realm 0），不是玩家 | `MOVEMENT_SPEED` transient 修正 | 45 tick | `:130-153` | 半径 12、+8%（`:59-60`）|

- 全部**有限时长**；因为 `100 > 60`、`120 > 100`，**存在空窗，不是常驻**。但站桩不动会被持续刷新 → 等效「在光环内永久减速/虚弱」。**【已验证（代码）/未验证（实机强度）】**
- 妖兽侧光环由**妖兽自身 AI tick 驱动**，不是全局扫描（`:101` 注释「never a global entity scan」）。

### 5.2 伤害反射

`TianfuCombat.java:291-312`：

```java
if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;   // ← 只对「玩家来源」反射
if (victim.getRandom().nextDouble() >= chance) return;
float reflected = (float) (original * ratio);
IN_REFLECT.set(TRUE);
try { attacker.hurt(victim.damageSources().thorns(victim), reflected); } finally { IN_REFLECT.set(FALSE); }
```

| 项 | 结论 | 证据 |
|---|---|---|
| 反射比例 | `malice_reflect` = **30%**，概率 **15%**（`TianfuTalents.java:383-384`）| — |
| 是否与其它反射叠加 | **不叠加**，取「概率×比例」最强者替换：`bestReflect():928-940`（0.15×0.30=0.045 > `youshou_nengkai` 0.04×0.10=0.004 > `wuying_bi` 0.0072）| `TianfuCombat.java:292-300` |
| 触发上限 | **无伤害上限**，但受**来源限制**：仅当伤害来源实体是 `ServerPlayer` | `:302` |
| 内置冷却 | **无**，每次受击独立判概率 | `:303` |
| **对无来源伤害** | **不反射**（`getEntity()==null` → return）→ 摔落/虚空/中毒等环境伤害安全，✅ | `:302` |
| **⚠ 对怪物伤害** | **完全不反射** —— `getEntity()` 是 `Zombie` 等 `Mob` 时 `instanceof ServerPlayer` 为 false 直接 return。**词条描述「受击时 15% 概率把 30% 伤害以魔法形式反弹」在 PVE 中是 0 收益** | `:302` |
| 递归保护 | `ThreadLocal IN_REFLECT`，事件内再 `hurt()` 不会重入 | `:65`、`:40` |

> 判定：**功能与描述不符（PVE 无效）**，不是超模，是失效。同样问题也影响 `youshou_nengkai`（黄 1 点）与 `wuying_bi`（玄 2 点）。

### 5.3 伤害抗性 / 适应性

**分类减伤（摄魂 / 破魔）** —— `TianfuCombat.java:217-220`：

```java
boolean armorApplies = !event.getSource().typeHolder().is(DamageTypeTags.BYPASSES_ARMOR);
r += armorApplies ? TianfuTalents.physicalReduction(ids) : TianfuTalents.magicReduction(ids);
return Math.min(r, 0.5);
```

- `malice_dementor` **15%**（吃护甲类，`TianfuTalents.java:391-392`）、`malice_dispell` **15%**（穿护甲类，`:393-394`）。
- **抗性上限 = 50%**（`Math.min(r, 0.5)`，`:220`），与 `铁皮肉骨 4%`、`入微定神 8%`、`神·不灭金身 20%`、**恶意·适应 20%** 全部挤在同一个 50% 上限池里。
- **【未验证】**：本体重写了伤害流程（`ShangHaiLiuCheng1Procedure` 自建 `DamageSource`），`BYPASSES_ARMOR` 标签在这些自定义伤害类型上是否被正确设置，需实机确认；否则「摄魂/破魔」可能永远只命中同一分支。

**适应性** —— `TianfuCombat.java:316-334`：

```java
String key = event.getSource().getMsgId();                 // 按伤害类型分别记层
int cur = stacks.merge(key, 1, Integer::sum);              // ← 先叠层
if (cur > cap) { cur = cap; stacks.put(key, cap); }
event.setAmount((float) (event.getAmount() * (1.0 - per * cur)));
```

| 项 | 结论 |
|---|---|
| 每层/上限 | `per = 0.02`、`cap = 10` → **每类伤害最多 20% 减伤**（`TianfuTalents.java:395-396`、`:838-845` 用 `first()` 取绝对值最大者，**多条不叠**）|
| 是否无限成长 | **不会**。单类型封顶 10 层，总减伤封顶 20% |
| 是否衰减 | **不衰减**（本世内永久，仅下线 `forget()` 清内存：`:380-383`）|
| 记忆表膨胀 | `stacks.size() > 16` 时整体 `clear()`（`:325-327`）|
| **⚠ 首击即生效** | `merge` 在减伤**之前**，所以第 1 次被某类伤害打就已经吃 2%（描述「每次受同类伤害再减伤 2%」暗示首击应为 0%）→ **等价多送 1 层**。10 次后 20%，永久 |

### 5.4 猎杀光环

**tianfu 侧 `malice_killer_aura` 杀意凛然**（5 点）—— `TianfuEffects.java:181-194`：

```java
double[] aura = TianfuTalents.aura(ids);                       // {4.0, 0.05}
if (aura != null && player.tickCount % 60 == 0) {              // 每 3 s
    double dmg = player.getAttributeValue(ATTACK_DAMAGE) * aura[1];   // 攻击力 5%
    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(aura[0]),                            // 4 格
            x -> x != player && x.isAlive() && x instanceof Enemy)) {            // ← 关键
        target.hurt(player.damageSources().magic(), (float) dmg);
    }
}
```

| 项 | 结论 |
|---|---|
| 半径 | **4 格**（`aura(4.0, 0.05)`，`TianfuTalents.java:397-398`）|
| 对象 | `x instanceof net.minecraft.world.entity.monster.Enemy` —— **只打「原版敌对」标记的实体** |
| 误伤自己/队友 | **不会**：`x != player`，且玩家不是 `Enemy` |
| 扫描方式 | **每 3 秒对施放者包围盒做一次 `getEntitiesOfClass` 全量查询**，`inflate(4)` 无上限目标数；**无视线（LOS）检查 → 可透墙** |
| 伤害类型/量 | `damageSources().magic()`，`攻击力 × 5%`，随 `ATTACK_DAMAGE`（含杀神 5 倍乘区）线性放大；**无消耗、无每目标冷却** |
| **⚠ 对象错配** | 外 mod `MaliceTags.java` 类注释实测结论：「本体 450 个实体里只有 **7 个**继承 `Monster`（即 `Enemy`），其余妖兽全是 `TamableAnimal`/`PathfinderMob`」→ 在蛊真人环境下，**这条光环对绝大多数本体妖兽不生效**，只在打原版怪物时有效。功能与描述「对 4 格内敌对生物」不符（应为「原版 Enemy 标签」）|

**外 mod 侧**（`RealmProfile`）已在 5.1 表内：realm1 光环加速**友方妖兽**（半径 12、+8%、45 tick，`：130-153`）；realm3/realm4 光环作用于**半径内所有玩家**（含队友，`：155-178`），半径 3.0 / 8.0，间隔 120 / 100 tick。均为「怪自身 AI tick 驱动 + 有限时长 + 有间隔」。

### 5.5 致命伤害免死 —— **有冷却，不是无限免死**

`TianfuCombat.java:350-366`：

```java
private static void tryUndying(ServerPlayer victim, Set<String> ids, LivingIncomingDamageEvent event) {
    double[] und = TianfuTalents.undying(ids);                       // {0.50, 6000}
    if (und == null || event.getAmount() < victim.getHealth()) return;    // 非致命 → 不动
    long now = victim.level().getGameTime();
    var data = victim.getPersistentData();
    if (now < data.getLong(TAG_UNDYING_READY)) return;              // ← 冷却闸
    data.putLong(TAG_UNDYING_READY, now + (long) und[1]);           // 写 300 s 冷却
    event.setAmount(Math.max(0.0F, victim.getHealth() - 1.0F));     // 留 1 血
    victim.heal((float) (victim.getMaxHealth() * und[0]));          // 回 50% 上限
    victim.displayClientMessage(... "不死之身：免于一死", true);
}
```

| 项 | 结论 | 证据 |
|---|---|---|
| 触发条件 | `event.getAmount() >= victim.getHealth()`（**致命**判定），且在**减伤 → 闪避 → 反射**之后（`:73-102` 顺序），所以判定用的是**最终伤害** | `:353`、`:102` |
| **冷却** | **有：6000 tick = 300 秒**，写在 `player.getPersistentData()` 键 `tianfu_malice_undying_ready`（**重登/换维度/死亡不清**）| `:46`、`:361` |
| 使用次数上限 | 无每日/每世次数上限，**只受 300 s 冷却限制** | — |
| 每次消耗 | **无资源消耗**（不扣真元/念头/寿元） | — |
| 效果 | 伤害改为 `当前血量 − 1`（留 1 血）+ 立即回 **50% 最大生命** | `:362-363` |
| 重生继承 | `copyCooldownOnClone():368-377`：普通重生**保留**冷却；全新一世（未 confirmed）从 0 开始 | — |
| 边界 bug | 若玩家血量 ≤ 1，`setAmount(health-1)` 可能是 0 或负（被 `max(0,·)` 兜成 0）→ **血量 1 时免死会把伤害清零**，等价于该次攻击完全无效，直到 300 s 后失效 | `:362` |

> **明确结论：不是无限免死**。`12 点 → 每 5 分钟一次免死 + 回 50%`，属可控但极强的 PVE 保命能力（在本体存在「寿元耗尽/必死判定」的体系里尤其如此）。

---

## ⑥ 最严重超模 Top 3

### TOP 1 — 气运维度整体饱和：`天弃之命` 8 点 + 两条气运负面 7 点 = **15 点，其中 7 点零边际代价**（+出身再 +3）
- 证据：`TianfuTalents.java:158/155/339`；`NeoForge1211Adapter.java:190-193` 每 tick 强制 `qiyun = qiyun_shangxian = 0`。
- 因为归零是**每 tick 硬写**，`v2_qiyun_duanjue`(×0.625)、`qiyun_xibo`(×0.85)、以及 `foundling`(−30%)/`orphan`(−20%)/`clan`(−10%) 的减益**全部落在已被清零的字段上**，边际代价 = 0。
- 严重度：气运在本体主要影响突破概率（`Guzhenren98Progression.nativeBreakthroughChance():37-41` 的 `luck*0.01`）与掉落；对「不靠突破/不靠运气」的战斗 build 近乎免费 → **8 点换「气运 0」，再白拿最多 10 点**。
- 修复方向：`buy()` 层做**维度互斥/递减**（买了 `v2_luck_forfeit` 后 `v2_qiyun_duanjue`/`qiyun_xibo` 的 grant 置 0 或禁买）。

### TOP 2 — `空窍桎梏` + `念府狭隘`：**10 点，对不走智道的 build 近乎零成本**
- 证据：`TianfuTalents.java:157/342`；`NeoForge1211Adapter.java:161-164`（念头容量归 0）、`:196-201`（锁 `kongqiao`）。
- 「锁定确认时原有资质」的判断依据是 `v2_lock_kongqiao` —— 如果玩家本就不打算买 `v2_apt_*` 资质档位、也不买十绝体（`buy():128-138` 只禁止「买了资质档位再买桎梏」的反向顺序，桎梏先买则**后续资质档位会被 CONFLICT 拦**，`APTITUDE_LOCK_ID` 判定在 `:136-138`），那么锁定的代价只是「失去一个本来就不买的东西」。
- 真正代价仅剩「念头容量 0」→ 只惩罚智道流。**净白拿 2 点 + 8 点对一个不突破资质的 build 近乎免费。**
- 附注：该条目同时把 `niantou_rongliang` 减到 0（`:163`）+ 词条自带 `fld(-1.0)`（`:157`），靠 `TianfuApply.ADAPTER_HARD_COSTS`（`:32-34`、`:282-284`）跳过重复计算 —— **这条链路是正确的**，不要误改。

### TOP 3 — `不死之身`：12 点买「每 300 s 一次免死 + 回 50%」，且**无资源消耗、无次数上限**
- 证据见 5.5。冷却 6000 tick、写 PersistentData、死亡重生继承、按最终伤害判定致命。
- 之所以排第三而不是第一：**它有 300 s 冷却**（任务书担心的「无限免死」不成立）。
- 之所以仍算超模：本体体系里寿元/必死判定是硬约束，而这条是**唯一能对冲必死的手段**，且 12 点即可获得、不吃任何资源；与「极·万世不灭」16 点（生命 +150%/回 40%/减伤 40%）相比，战术价值更高但更便宜。
- 附带缺陷：血量 = 1 时 `setAmount(max(0, health-1)) = 0` → 该次攻击完全无效（`:362`）。

**未入选但需注意**：`随境应变`（5 点）首击即 2%、10 层永久不衰减、与减伤池共用 50% 上限 → 实际是「永久 +20% 减伤，无需操作」，比描述强一档（详见 5.3）。

---

## ⑦ 需修改的类与方法（仅建议，本次未改动）

| # | 位置 | 问题 | 建议 |
|---|---|---|---|
| 1 | `v2/TianfuEconomy.java#planRoll` (`:388-401`) | 12 点/6 条是**买前判据**，实测泄漏到 **19 点**，与 `NEGATIVE_CAP=24` 不一致 | 改为「买之前预测 `grant + cost > 预算` 就 break」；或把预算显式定为 `min(remainingCap, 12)` 并在买后重判 |
| 2 | `v2/TianfuEconomy.java#catalogCost` (`:33-40`) / `#availableForPurchase` (`:43-46`) | 恶意组门槛只在 `TianfuPool` 注册层把关；`planRoll` 走 `TianfuTalents.all()` 可绕过 | 增加 `id.startsWith("malice_") && !TianfuTalents.maliceLoaded() → 不可购`（catalogCost 返回 null / availableForPurchase false）|
| 3 | `TianfuTalents.java` MALICE 数据 / `v2/TianfuEconomy.java#buy` | 同维度饱和（气运 7 点、念头 2 点白拿） | 在 `buy()` 增加「维度饱和互斥」：`v2_luck_forfeit` ⊃ `v2_qiyun_duanjue`/`qiyun_xibo`；`v2_aptitude_shackled` ⊃ `nianfu_xiayai`。或对第二、三条同维度负面打折 grant |
| 4 | `TianfuCombat.java#reflectDamage` (`:302`) | `instanceof ServerPlayer` 使反射**对怪物完全无效**（描述不符） | 改成 `instanceof LivingEntity`（排除 `null` 来源），并保留 `IN_REFLECT` 递归保护 |
| 5 | `TianfuCombat.java#applyAdaptive` (`:328`) | `merge` 先于减伤 → **首击即叠层**；层数永不衰减 | 先读 `cur`（从 0 起算）再减伤；加脱战/超时衰减 |
| 6 | `TianfuEffects.java#onTick` 光环 (`:187-189`) | `x instanceof Enemy` 对本体妖兽（`TamableAnimal`/`PathfinderMob`）几乎全不命中；且无 LOS 检查、无每目标冷却 | 改为「`Enemy` ∪ `guzhenren:malice_normal/elite/boss` 标签 ∪ 本体妖兽判定」；加 `hasLineOfSight` 与每目标 20 tick 冷却 |
| 7 | `TianfuRoll.java#generate` (`:94-115`) | 死代码且**无补点上限**（若复活会整批被 `acceptRoll` 拒绝并白耗一次摇号） | 删除，或补 `negativeGrant` 上限与 `planRoll` 保持一致 |
| 8 | `TianfuCombat.java#tryUndying` (`:362`) | 血量 ≤1 时免死把伤害归零 | `setAmount(Math.max(1.0F, health - 1.0F))` 或额外判 `event.getAmount() > 0` 后的兜底 |
| 9 | `TianfuUiRules.java:51` / `TianfuViewScreen.java:487,570` / `TianfuBuyPayload.java:65` | 文案硬编码 24 | 若调整 `NEGATIVE_CAP`，需同步这 4 处（建议改为读常量）|
| 10 | `v2/NeoForge1211Adapter.java#maintainConstraints` (`:190-213`) | 气运/寿元每 tick 硬写，属「周期性拉回」；与天赋撤销/转世无显式解钩 | 【未验证】转世（`TianfuReincarnation`）清 `tianfu` 根时是否会残留 `KEY_LIFESPAN_CAP`/`KEY_LOCK_KONGQIAO`，建议确认这两个键随根一起清 |

---

## ⑧ 未验证项（需实机 / 需反编译本体其它过程）

1. **寿元基数**：本体 `shouyuan` 的初值与上限未确认（`docs/audit-1.4.18/_raw_playervars.txt:106` 仅有字段声明）→ 无法判定「−250 / −750」的真实严重度，也无法判定 `v2_shouyuan_kuwei` 5 点是否偏贵。唯一硬兜底是确认时 `validLifespan() > 0`。
2. **精力（`zuida_jingli`）的玩法作用路径** 未验证 → `v2_jingli_buji`（−20%，2 点）可能是白拿也可能是重伤。
3. **魂魄上限 / 承载极限（`zuida_hunpo` / `hunpochengzaijixian`）** −25% 的实际后果未验证（哪些本体流程读这两个字段）。
4. **本体是否在 `BuffLiuCheng1Procedure` 之外每秒清除玩家 HARMFUL 效果**：本报告只证明了该过程**不写**效果（`removeEffect/addEffect/getDuration` 计数均为 0），其它过程未穷举。
5. **摄魂/破魔的 `BYPASSES_ARMOR` 分流** 在本体自建 `DamageSource`（`ShangHaiLiuCheng1Procedure`）上是否成立 —— 未实机，若标签缺失则两条 5 点词条可能只命中同一分支。
6. **蛊冷却 +11.1%**（`ceil(native/0.9)`）与本体冷却表/UI 显示是否一致 —— 未跑服务端。
7. **l2hostility 3.0.18 的 trait 实际效果**（`undying`/`adaptive`/`dementor`/`dispell`/`cursed` 等）未在本仓库内，仅核对了 `蛊真人恶意` 的分级配置与黑名单；外部 mod 的实机强度未验证。
8. **`guzhenren_malice` 的 `hundred`（百天芥潮）模式** 与 tianfu 恶意词条是否会产生叠加（例如妖兽 300 级 trait 与玩家 50% 减伤上限的交互）—— 未实机。
9. 所有平衡数值（24 点补点的实际收益、300 s 免死的实战价值）均需实机验证；本报告只给代码层判定。
10. `v2_gu_cd_ningse` 的 `−0.10` 与「每 tick 额外增加 0.10」的描述是否与本体 `itemCooldown` 单位一致 —— 代码层只确认了 `rate = 0.9` 的换算（`Guzhenren98Progression.java:57-63`），单位语义未与本体核对。
