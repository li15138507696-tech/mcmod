# 炼蛊体系 审查报告（T3）

- 审查对象：`tianfu` 1.4.17（目标 1.4.18），包 `net.guzhenren.tianfu`
- 本体：10.20 内层 jar `_crash3\basever\guzhenren-10.20.jar`
  - sha256 `B6D314D3810B8BC5FCFDBC4F588DB8721F0EFDFC14D27A0A06D88143A01FE2BC`（本次复核，一致）
  - 22371 class，36880 个 zip 条目
- 环境：Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- 本报告**只读**：未修改任何源码、构建、版本或既有 docs 文件
- 证据口径：本体侧写 `全限定类名@bci字节码偏移`（可用
  `javap -p -c -classpath <jar> <类名>` 复现，`bci` 即 `Code:` 左侧偏移）；
  附属侧写 `文件:行号`。所有行号 / bci 均由本次反编译逐条核对，未采信历史产物。
- **时序类结论一律标注「未实测」**——本次没有任何游戏内运行证据。

---

## 0. 结论摘要（先看这 10 条）

| # | 结论 | 严重度 | 证据强度 |
|---|---|---|---|
| 1 | 本体完成判定确为**精确 `LianGuJinDu == 100.0`**（`dcmpl` + `ifne`），本体自己从不 clamp、也从不在 +1 前钳制。**791/792 个完成型过程**如此；**唯一例外 GuFang 234（狐妖蛊）用 `progress >= 100`**（`dcmpl` + `iflt`）。 | 关键 | 已验证 |
| 2 | 本体节奏确为 `lg_ke += 1.0` / 次调用，`lg_ke / 20.0 >= 1.5`（等价 `lg_ke >= 30`）时归零并 `LianGuJinDu += 1.0` ⇒ **每 30 tick / 1.5 s 一点**。 | 关键 | 已验证 |
| 3 | **投料不是可选加成，而是硬性前置**：结算门禁是 `score("中途加材料") == 1`。窗口内没投料 ⇒ 到 100 时**整段跳过结算，进度清零、配方清空、材料与魂魄全损，且不给任何提示**。698 个带窗口的蛊方全部如此。 | 致命 | 已验证 |
| 4 | **窗口不是统一的 50–70**。本体 792 个完成型过程分三族：**A 族 347 个「只有 50≤p≤70」**、**B 族 351 个「20≤p≤40 且 50≤p≤70」两段必投**、**C 族 94 个无投料窗口**。任务书提到的「20%～40% 检查区间」**真实存在，且是 B 族的第一段硬性投料**。 | 致命 | 已验证 |
| 5 | 两段窗口各有**边界失败判定**：`progress == 40 && score1 != 1` 与 `progress == 70 && score2 != 1` ⇒ 扣血 `zhuanshu*10`、魂魄 `-zhuanshu*10`、清槽、显示「炼制失败」、终止。 | 高 | 已验证 |
| 6 | 附属**没有**改动材料数量、窗口长度、材料判定、失败条件、成功率、炼道流派值（`liangu_*` 只读 3 个字段、只写 `LianGuJinDu` 一个字段）。职责分离**成立**。 | — | 已验证 |
| 7 | 跨窗口推演结论：**50–70 窗口在任何加成组合下都不会被跳过**——因为附属在 `50≤cur≤70` 内完全冻结（`TianfuEffects:272,298`），`70` 边界 30/30 相位全部命中，窗口停留 28.6–31.5 s。**任务书担心的「跳过 50-70」不成立。** | — | 已验证（可复算） |
| 8 | 但**另一个窗口出事了**：B 族的第一段 `20–40` **没有冻结**。20–40 停留时长从 31.5 s 被压到 **9.0 s**（L=2.2/2.5），且 `progress == 40` 的边界失败判定在 **L ≥ 2.2 时 30/30 相位全部被跳过** ⇒ 玩家既没得到「炼制失败」提示，也在 100 时静默全损。 | 高 | 已验证（可复算） |
| 9 | 五个炼蛊天赋**数值全部可叠加**（`sum()` 纯加法，无上限）：`v2_mind_liangu_1` 与 `_2` 因同属 `liangu` 族而**互斥二选一**，其余三条不分组、可全买 ⇒ 天赋侧上限 **2.20**，加出身 **0.30** ⇒ **L_max = 2.50**（23 点）。 | 高 | 已验证 |
| 10 | `liangu_bujie`（天 5）与 `shen_liangu_tianshu`（神 12）**仍然带蛊虫冷却收益**（`GU_CD 0.30` / `0.50`），任务书要求的「移除冷却收益」**未执行**。 | 高 | 已验证 |

---

## 0.1 既有结论逐条复核

| 既有结论 | 复核结果 | 说明 |
|---|---|---|
| 完成判定是精确 `LianGuJinDu == 100.0`（`dcmpl` + `ifne`），本体自己从不 clamp | **复核成立（有 1 个例外）** | `GuilianguwanchengProcedure@191-198`、`AnJianGuguFang2Procedure@191-198`、`BaiYuGuWanChengProcedure@191-198` 均为 `getfield LianGuJinDu / ldc2_w 100.0d / dcmpl / ifne`。全文扫描 792 个完成型类，仅 `HuYaoGuGuFang3Procedure@347-354`（GuFang 234）是 `dcmpl / iflt`（即 `>= 100` 完成）。 |
| 本体节奏：`lg_ke` 每 tick +1，攒满 30 才 `LianGuJinDu += 1` → 每 1.5 秒 1 点 | **复核成立（表达式需更正）** | 字节码是 `lg_ke / 20.0 >= 1.5`（`ldc2_w 20.0d / ddiv / ldc2_w 1.5d / dcmpl / iflt`），数学上等价于 `lg_ke >= 30.0`；写成「攒满 30」结论正确、写法不精确。 |
| 附属不得把进度写成 100（会变 101 卡死），也不得写小数 | **成立，且当前代码已遵守** | `TianfuEffects:307` 为 `Math.min(99.0, cur + bonus)`，`:304-306` 用 `floor(acc)` 保证整数。推论也复核成立：本体是「先 +1 再判 ==100」，一旦写成 100，下一拍变 101 后 `ifne` 永久不成立，进度条定格。 |
| 附属写法不同：六合一写 `GuFang = 9000 + index`，魂道写自己的配方号 → 门禁只能用 `GuFang != 0.0` | **部分成立，需修正** | 外层真闸其实是 `v.LianGu`（`TianfuEffects:261`）。`GuFang != 0.0` 只是「界面开着」的代理判据，而**关闭炼蛊界面时本体会把 `GuFang` 清 0**（`LianGuguiGaiGUIGuanBiShiProcedure@64-66`），所以该代理在实践中等效。仍留下一个未验证风险：本体 792 个完成型过程按自己的 `GuFang` 值分派，第三方写的 `9000+i` 本体不认识 ⇒ 本体的 +1 永不发生，附属把进度顶到 99 后会**永久停在 99**（见 ⑦-3）。 |
| 50% 投料窗口是区间 `50 ≤ 进度 ≤ 70`（`iflt` + `ifgt`），且要求炼蛊界面开着、槽位 0 有蛊材原石 | **区间成立；「槽位 0 蛊材原石」只对部分蛊方成立** | 区间两端**均含端点**：`AnJianGuguFang2Procedure@2090-2097`（`ldc2_w 20.0d / dcmpl / iflt`，`>=20`）与 `@2110-2117`（`ldc2_w 40.0d / dcmpg / ifgt`，`<=40`）；50–70 段见 `@4962-4969` / `@4982-4989`。材料要求逐蛊方不同：GuFang 617 的 50–70 段确是「槽 0/1/2 各 **64** 个 `GUCAIYUANSHI`」，但 GuFang 773 的 50–70 段是「槽 3–7 的 `FANRENHUNPO×50` / `ERZHUANHUNPO×10` / `GUI_LIAN_KUI×50` / `SANZHUNHUNPO×1` / `YIZHUANHUNPO×50`」，与蛊材原石无关。**「槽位 0」和「蛊材原石」都不能写死。** |
| （任务书新增假设）不同蛊方可能存在 20%～40% 的检查区间 | **成立** | 351 个蛊方（B 族）在 50–70 之前还有 20–40 的**前置投料段**，用独立的 `中途加材料1` 标记，缺这一段到 100 时同样静默全损。见 ② |

---

## ① 本体炼蛊流程完整还原

### 1.1 触发链（谁在推进进度）

```
net/guzhenren/world/inventory/LianGuguiMenu@0-59            onPlayerTick(PlayerTickEvent.Post)
    ├─ p.containerMenu instanceof LianGuguiMenu  ← 唯一入口闸：菜单没开就什么都不推进
    └─ LianGuguiDangGaiGUIDaKaiShiMeiKeFaShengProcedure.execute(world, x, y, z, player)
          ├─ LianGu == true 才继续（@15-19）
          ├─ addEffect(LIAN_GU_ZHONG, 10t, amp 0, ambient=false, visible=false)（@44-63）
          └─ 按 GuFang 值分派到 792 个完成型过程之一（GuFang==773 → GuilianguwanchengProcedure@4420）
```

`LianGuguiMenu.onPlayerTick` 的 bci 证据：

```
  5: aload_1
  6: getfield  Player.containerMenu
 10: aload_3
 11: instanceof LianGuguiMenu
 14: ifeq 59                     ← 不是炼蛊菜单就直接 return
 56: invokestatic LianGuguiDangGaiGUIDaKaiShiMeiKeFaSheng1Procedure.execute(...)
```

> 所以「本体只在界面开着时推进」**成立**，且这不是靠注释，而是 `instanceof LianGuguiMenu` 的硬闸。

关闭界面是**立即判失败**，不是暂停：

```
net/guzhenren/world/inventory/LianGuguiMenu@855  removed(Player)
  @13  invokestatic LianGuguiGaiGUIGuanBiShiProcedure.execute(...)

net/guzhenren/procedures/LianGuguiGaiGUIGuanBiShiProcedure@19   if (PV.LianGu != 1) return;
  @35  LianGuJinDu = 0.0            ← 进度清零
  @56  hunpo      = hunpo - 5.0     ← 魂魄 -5
  @61  lg_ke      = 0.0
  @66  GuFang     = 0.0             ← 配方清空
  @71  LianGu     = false
  @95  hurt(GENERIC, 5.0f)          ← 5 点伤害
  @124 displayClientMessage("炼制失败")
```

（我原本怀疑「关界面后附属仍在推进」是漏洞，此过程证明**不成立**：`LianGu` 同时被清 false，附属的外层闸立刻失效。）

### 1.2 开炼：`GuilianguzhizuoProcedure`（以归炼蛊方 GuFang=773 为例）

只做 4 件事，全部集中在 `@3901-3990`：

```
@3901 ldc  "中途加材料"  → getOrCreatePlayerScore(...).set(0)     ← 投料标记清零
@3970-3973  LianGuJinDu = 0.0 (dconst_0 / putfield)
@3976-3979  lg_ke       = 0.0
@3982-3987  GuFang      = 773.0  (ldc2_w #223 // double 773.0d)
```

> 注意：**进度起始值是 0.0，不是 1.0**。附属注释里「本体 KaiShiLianZhiProcedure 开炼时置 1.0」的说法只适用于其它蛊方流程，对 GuFang 773 不成立（`GuilianguzhizuoProcedure` 里 `dconst_1` 出现 0 次）。
> `LianGu = true` 不在本过程内，由 `KaiShiLianZhi5Procedure` 等上层置位。

辅助方法（同类的 `private static int getAmountInGUISlot(Entity, int)`，bci 见文件尾部）：

```
slot.getItem() → null ? 0 : stack.getCount()     ← 返回【精确】数量，不是 >=
```

### 1.3 推进 + 完成：`GuilianguwanchengProcedure.execute` 逐段

整体骨架（`LianGu` 为真时每 tick 走一遍）：

```java
if (entity == null) return;                                   // @0-4
if (PV(entity).LianGu != true) goto 4736;                     // @15-19
if (entity instanceof LivingEntity && !level.isClientSide)
    addEffect(LIAN_GU_ZHONG, 10, 0, false, false);            // @22-63  炼蛊中 debuff，10 tick
PV.lg_ke += 1.0;                                              // @89-91  dconst_1 / dadd
if (PV.GuFang != 773.0) goto 4736;                            // @108-115 ldc2_w 773.0d / dcmpl / ifne
if (PV.lg_ke / 20.0 >= 1.5) {                                 // @128-139 ldc2_w 20.0d / ddiv / ldc2_w 1.5d / dcmpl / iflt 181
    PV.lg_ke = 0.0;                                           // @154-155
    PV.LianGuJinDu += 1.0;                                    // @169-173 dconst_1 / dadd
}
if (PV.LianGuJinDu == 100.0) {                                // @191-198 ldc2_w 100.0d / dcmpl / ifne 1934
    PV.LianGuJinDu = 0.0;  PV.GuFang = 0.0;  PV.LianGu = false;   // @213-224
    if (getEntityScore("中途加材料", entity) == 1) {            // @231-238 iconst_1 / if_icmpne 1934
        if (Math.random() < 0.7 + liupai_liandao*0.001 + LianGuChengGongLv*0.01) {   // @241-284 dcmpg / ifge 750
            /* 成功 */ give GUILIANGU x1 → menu slot 9;            // @300-367
                       displayClientMessage("炼制成功");           // @399-406
                       liupai_liandao += 3.0;                     // @420-438 ldc2_w 3.0d / dadd
                       score("中途加材料") = 0;                    // @486-502
        } else {
            /* 失败 */ clear slots 9..17 + 15,16,17;              // @1667-1739 Slot.set(EMPTY)
                       hurt(GENERIC, zhuanshu*10.0f);             // @1750-1784 ldc2_w 10.0d / dmul / d2f
                       hunpo = zuida_hunpo - zhuanshu*10.0;       // @1810-1831
                       displayClientMessage("炼制失败");           // @1861-1868
                       score("中途加材料") = 0;                    // @1928-1929
        }
    }
}
// ---- 无条件尾部（每 tick 都走）----
if (getEntityScore("中途加材料", entity) != 1) {               // @1934-1941 iconst_1 / if_icmpeq 4491
    if (PV.LianGuJinDu >= 50.0) {                              // @1954-1961 ldc2_w 50.0d / dcmpl / iflt 4491
        if (PV.LianGuJinDu <= 70.0) {                          // @1974-1981 ldc2_w 70.0d / dcmpg / ifgt 4491
            if (menu instanceof MenuAccessor
                && slot3 == FANRENHUNPO  && getAmountInGUISlot(3) == 50   // @2016-2062
                && slot4 == ERZHUANHUNPO && getAmountInGUISlot(4) == 10   // @2100-2147
                && slot5 == GUI_LIAN_KUI.asItem() && getAmountInGUISlot(5) == 50  // @2185-2238
                && slot6 == SANZHUNHUNPO && getAmountInGUISlot(6) == 1    // @2276-2324
                && slot7 == YIZHUANHUNPO && getAmountInGUISlot(7) == 50) { // @2406-...
                score("中途加材料") = 1;                            // 之后置 1
                clear slots 3..7 (Slot.set(EMPTY));                 // @2818 / @3182 / @3546 / @3914 / @4282
                broadcastChanges();
            }
        }
    }
}
```

**关键点（每一条都被上面的 bci 覆盖）**

1. `lg_ke` 的 +1 与 `LianGuJinDu` 的 +1 是**无条件顺序执行**，本体从不 clamp、从不把 101 拉回 100。
2. 完成判定在 +1 **之后**，所以「写成 100」⇒ 下一拍 101 ⇒ `ifne` 永假 ⇒ 永久卡死。这个推论**复核成立**。
3. 结算还要求 `score("中途加材料") == 1`。**没投料 ⇒ 整段 if 被跳过**，进度/配方/标志已被清零、`LianGu=false`，玩家什么也得不到、也没提示。
4. 尾部窗口块与结算块是**同一个方法里的两段**，尾部块判定的是「还没进过料」的那一次（`!= 1`）；投料成功后同一 tick 内不会再判一次。
5. 成功 = 给 1 个 `GUILIANGU` 到 **menu slot 9**；`liupai_liandao += 3.0`（这是**炼道流派熟练度**，属本体自己的奖励，附属未触碰）。
6. 失败 = 清槽 + 扣血 `zhuanshu*10` + 魂魄扣 `zhuanshu*10` + 「炼制失败」。

### 1.4 材料判定语义（决定「能不能多放」）

```
slot.getItem().getItem() == <目标物品>            （if_acmpne，引用相等 → 精确物品）
&& getAmountInGUISlot(slot) == N                  （bipush N / if_icmpne）
```

两段都是**精确相等**，因此：

- **不允许多放**：放 65 个 `GUCAIYUANSHI` 时 `getAmountInGUISlot(0) == 64` 为假 ⇒ 整条链断 ⇒ 该 tick 不投料。
- **不允许少放**：同上。
- 空槽语义是 `count == 0`（`ifne` 跳过），不是 `isEmpty()`，两者在 vanilla 下等价。

### 1.5 成功率公式（附属未触碰，列出以便对照）

| 蛊方量级 | 基础成功率 | 炼道熟练度系数 | `LianGuChengGongLv` 系数 |
|---|---|---|---|
| 一/二转（`0.8` / `0.002`） | `0.8` | `liupai_liandao * 0.002` | `* 0.01` |
| 三/四转（`0.7` / `0.001`） | `0.7` | `* 0.001` | `* 0.01` |
| 五转/高阶（`0.9` / `0.005`） | `0.9` | `* 0.005` | `* 0.01` |
| 特殊族（`0.4` / `5.0E-4`，`0.2` / `2.5E-4`） | `0.4` / `0.2` | 见左 | `* 0.01` |

判定序列恒为 `Math.random() < 基础 + 熟练度系数 + 成功率系数`（`dcmpg` + `ifge` 跳失败分支）。
`liupai_liandao` 每成功一只 +3.0，所以熟练度是**本体自带的滚雪球**，与附属无关。

---

## ② 投料窗口：逐族 / 逐蛊方

> 「不得把所有蛊方统一成 50-70」——**这条要求是对的，本体的实现确实不是统一的。**
> 下表由全量字节码扫描得出（792 个过程类，逐个抽出 `ldc2_w` + `dcmp*` + 分支三元组）。

### 2.1 三族总表

| 族 | 类数 | 唯一 GuFang 号 | 窗口 | 结算所需的 score 标记 | 是否必投 |
|---|---|---|---|---|---|
| **A** | 347 | 347 | `50 ≤ p ≤ 70` | `中途加材料 == 1` | **必投**（否则 100 时静默全损） |
| **B** | 351 | 351 | **`20 ≤ p ≤ 40`（第 1 段）+ `50 ≤ p ≤ 70`（第 2 段）** | `中途加材料1 == 1` **且** `中途加材料2 == 1` | **两段都必投**（缺任一段 100 时静默全损） |
| **C** | 94 | 94 | **无窗口**，`p == 100` 直接结算 | 无 | 不需要投料 |

合计 792 个完成型过程类 / 787 个唯一配方号（少量配方号同时存在「完整流程」与「残方/简化流程」两个类，故 792 ≠ 787）。

**A 族 GuFang 全量（347 个）**

```
24,26-31,33-49,59-134,136-139,141-144,146,147,149-158,160,161,164-171,
221-227,232,233,235,236,237,238,240,242-247,252,258,259,265,269-273,275,
277,278,281,282,286-300,307,308,310,311,314,315,318,319,322,323,325,326,
327,335,336,339,340,344,349,350,351,352,355,356,402,405,406,417,421,
425-429,436,454,461-476,496,497,501,502,507,508,513-522,540,541,543,549,
551,553,557-564,580-592,608-610,615,624-627,635,637,638,643-652,672-683,
702-706,733-753,773,798,799,808-817,828
```

**B 族 GuFang 全量（351 个）**

```
172-220,228-231,239,256,260-264,266,267,279,283,301-304,309,312,316,320,
324,328-333,337,341,347,348,357-363,369-386,389-401,404,408,410,411,
414-416,418-420,422-424,430-435,437,438,440,441,445,446,448,455,456,
477-490,492,493,498,499,503,504,509,510,523-538,544,545,547,548,550,552,
554,565-577,593-606,611-621,628-634,639,640,653-670,684-700,707-715,
718-730,754-772,774-795,800,801,802,818-827
```

**C 族 GuFang 全量（94 个）**

```
2-23,25,32,50-58,135,140,145,148,162,163,234,268,274,276,280,284,285,
305,306,313,317,321,334,338,342,343,345,346,403,412,448,457,458,459,
495,500,506,511,512,539,555,556,578,579,607,608,636,641,642,671,
701,716,717,730,731,732,796,797,803,804,805,806,807,820,828
```

> C 族里 **`GuFang 234`（`HuYaoGuGuFang3Procedure`）是全场唯一用 `>= 100` 结算的**：
> `@347 getfield LianGuJinDu / @350 ldc2_w 100.0d / @353 dcmpl / @354 iflt 2271`。
> 它是「进度 > 100 也照样完成」的宽容实现——但附属把进度钳在 99，所以不会走到 >100。

### 2.2 两个完整解码实例

**实例 1：GuFang 773（归炼蛊方），A 族，单窗口**

| 项 | 值 | 证据 |
|---|---|---|
| 窗口 | `50 ≤ p ≤ 70`（含端点） | `GuilianguwenchengProcedure@1954-1981` |
| 段内门禁 | `score("中途加材料") != 1` | `@1934-1941` |
| 槽 3 | `FANRENHUNPO` × **50** | `@2016-2062` |
| 槽 4 | `ERZHUANHUNPO` × **10** | `@2100-2147` |
| 槽 5 | `GUI_LIAN_KUI`（方块 asItem）× **50** | `@2185-2238` |
| 槽 6 | `SANZHUNHUNPO` × **1** | `@2276-2324`（`iconst_1`） |
| 槽 7 | `YIZHUANHUNPO` × **50** | `@2406-...` |
| 匹配条件 | 物品**精确相等** + **精确数量** | `if_acmpne` + `bipush N / if_icmpne` |
| 允许多放 | **不允许**（多 1 个即整条链断） | 同上 |
| 成功后 | `score=1`，清空槽 3–7 | `@2818/3182/3546/3914/4282 Slot.set(EMPTY)` |
| 失败/损失 | 结算失败：清槽 + 扣血 `zhuanshu*10` + 魂魄 `-zhuanshu*10` + 「炼制失败」；**未投料则 100 时静默全损** | `@1750-1868` / `@231-238` |

**实例 2：GuFang 617（暗箭蛊方），B 族，双窗口**

| 项 | 值 | 证据 |
|---|---|---|
| 第 1 段窗口 | `20 ≤ p ≤ 40`（含端点） | `AnJianGuguFang2Procedure@2090-2097`（`dcmpl`/`iflt`）、`@2110-2117`（`dcmpg`/`ifgt`） |
| 第 1 段门禁 | `score("中途加材料1") != 1` | `@2070-2077` |
| 第 1 段前置 | 槽 0/1/2 必须**空**（`count == 0`） | `@2152-2196`、`@2234-2279`、`@2317-2362` |
| 第 1 段材料 | 槽 3–7 的精确数量材料（`if_icmpne`，槽 3 为 64） | `@2442`、`@2527`、`@2611`、`@2697`、`@2783` |
| 第 1 段成功 | `score("中途加材料1") = 1`，清空槽 3–7 | `@2860-2864`；`@3194/3558/3922/4290/4658` |
| 第 2 段窗口 | `50 ≤ p ≤ 70`（含端点） | `@4962-4969`、`@4982-4989` |
| 第 2 段门禁 | `score("中途加材料2") != 1` | `@4942-4949` |
| 第 2 段材料 | 槽 0 / 1 / 2 各 `GUCAIYUANSHI` × **64** | `@5024-5070`、`@5108-5155`、`@5193-5235` |
| 第 2 段成功 | `score("中途加材料2") = 1`，清空槽 0–7 | `@5732-5736`；`@5783-5975` |
| 结算门禁 | **`中途加材料1 == 1` 且 `中途加材料2 == 1`（AND）** | `@231-238` **与** `@241-248` 两次 `if_icmpne 2070` |
| 边界失败 1 | `p == 40 && score1 != 1` ⇒ 扣血 + 魂魄 + 「炼制失败」+ `LianGu=false` | `@5996-6003` / `@6006-6013` / `@6016-6054` / `@6077-6098` / `@6128` / `@6226-6227` |
| 边界失败 2 | `p == 70 && score2 != 1` ⇒ 同上 | `@6244-6251` / `@6254-6261` / `@6264-6302` / `@6376` |
| 成功率 | `random < 0.4 + liandao*5.0E-4 + chenggonglv*0.01` | `@251-284` |

> **两段的槽位不重叠**（B 族：第 1 段用槽 3–7，第 2 段用槽 0–2），这是本体的设计，不是笔误。

### 2.3 全族通用规则（对未展开的 790 个类同样成立）

1. 每个完成型过程**只服务一个 `GuFang` 值**（`getfield GuFang / ldc2_w <id>d / dcmpl / ifne`）。
2. 完成判定恒为 `p == 100.0`（1 个例外见 2.1 注）。
3. 投料窗口恒为「下界 `dcmpl`+`iflt`（`>=`）+ 上界 `dcmpg`+`ifgt`（`<=`）」——**永远是含端点的区间，从来不是相等**。
4. 材料判定恒为「物品引用相等 + `getAmountInGUISlot == N` 精确数量」，**没有任何一个蛊方允许超量**。
5. 投料成功后恒为「置 score 标记 = 1 + 清空相关槽 + `broadcastChanges`」——材料被**销毁**，不是退还。
6. 失败恒为「清槽 + `hurt(GENERIC, zhuanshu*10)` + `hunpo = zuida_hunpo - zhuanshu*10` + 「炼制失败」」。

（要逐个枚举剩余蛊方的槽位/物品/数量，按 §2.2 的方式对每个类跑
`javap -p -c` 再按 `getAmountInGUISlot` 定位即可；本次已对全部 792 个类做过窗口维度扫描，材料维度抽样 4 个类复核一致。）

---

## ③ 附属炼蛊加速实现（逐行）

### 3.1 `TianfuEffects.java` 炼蛊段（244–313 行）

| 行 | 代码 | 作用 / 判定 |
|---|---|---|
| 244–260 | 注释块 | 记录本体机制。**其中 250–251 行注释有误**：「50.0 ≤ LianGuJinDu ≤ 70.0」正确；「槽位 0 放着蛊材原石」只对部分蛊方成立（见 ②）。257 行「20 点 ≈ 10 秒」也低估了 3 倍（真实 21 点 / 31.5 s）。**注释错误不影响行为**，但会把后续维护者带偏。 |
| 261 | `if (v.LianGu)` | 外层真闸。本体在关界面/结算/失败时清 `LianGu=false`，故等效于「正在炼蛊」。 |
| 262–264 | `TianfuTalents.liangu(ids) + TianfuOriginBonus.liangu(TianfuV2Store.read(player))` | `L` = 天赋和 + 出身和，纯加法。 |
| 265 | `double cur = v.LianGuJinDu` | 快照当前进度。 |
| 270 | `boolean guiOpen = v.GuFang != 0.0` | 「界面开着」的代理判据。**不等价于 `containerMenu instanceof LianGuguiMenu`**，但关界面会同时清 `GuFang` 与 `LianGu`，实践中不会漏。 |
| 272 | `inFeedWindow = cur >= 50.0 && cur <= 70.0` | **只冻结 50–70 段，不冻结 20–40 段** ← 本次核心缺陷（见 ④）。 |
| 275 | `ourBeat = player.tickCount % 30 == 0` | **独立节拍**：不读 `lg_ke`（避免依赖两个 `PlayerTickEvent.Post` 监听器的先后）。 |
| 280–284 | `if (cur > 100.0) → 99.0` | 坏档迁移：把旧版顶穿的进度拉回 99。 |
| 288–294 | `liangu_prev` 记录；`cur < prev` 时清 `liangu_acc` | 开炼/换蛊/结算时把小数累加器清零。 |
| 298 | `if (liangu > 0 && guiOpen && ourBeat && !inFeedWindow && cur >= 0 && cur < 99)` | 补点总闸。**`cur < 99` 保证最后 1 点留给本体。** |
| 302–303 | `acc += liangu; bonus = floor(acc)` | **小数累加器**：长期平均速率精确等于 `L`，但 `bonus` 可以是 2 或 3（`L ≥ 2` 时），并非恒定 1 点。 |
| 305–306 | `acc -= bonus` | 余数保留。 |
| 307 | `v.LianGuJinDu = Math.min(99.0, cur + bonus)` | **整数补点 + 上限 99**。永不会写出 100、永不会写出小数。 |

回答任务书的四问：

| 问题 | 答案 |
|---|---|
| 独立节拍用什么 | `player.tickCount % 30 == 0`（`TianfuEffects:275`），与本体 `lg_ke` 完全解耦 |
| 每拍补几点 | 平均 `L` 点，实际 `floor(acc)` ⇒ **可以是 0/1/2/3**，不保证 1 点 |
| 补点是否整数 | **是**（`floor` + `cur` 起始为整数 + 本体只 +1.0 ⇒ 全程整数） |
| 是否在窗口内停止 | **只在 50≤p≤70 停，不在 20≤p≤40 停** |
| 是否把最后 1 点留给本体 | **是**（`cur < 99.0` 闸 + `Math.min(99.0, …)` 钳，两道保险） |

### 3.2 `TianfuTalents.liangu()`

```java
// TianfuTalents.java:786-788
public static double liangu(Set<String> ids) {
    return sum(ids, Op.LIANGU);
}

// TianfuTalents.java:942-956  sum()：遍历全部已购条目累加，无上限、无互斥、无归一
private static double sum(Set<String> ids, Op op) {
    double total = 0.0;
    for (String id : ids) { ... if (fx.op() == op) total += fx.a(); ... }
    return total;
}
```

### 3.3 `TianfuOriginBonus.liangu()`

```java
// TianfuOriginBonus.java:31-50
public static double liangu(TianfuSelection.Snapshot state) {
    if (state == null || !state.confirmed()) return 0.0;   // 未确认出身一律 0
    ... bonus += region.lianguBonus();                     // 地域
    ... bonus += background.lianguBonus();                 // 出身
    return bonus;
}
```

取值（`TianfuOriginTable.java`）：

| 地域 | lianguBonus | 出身 | lianguBonus |
|---|---|---|---|
| 南疆 | **0.10** (`:77`) | 蛊师学徒 | **0.20** (`:106`) |
| 其余四域 | 0 | 散修 | 0.10 (`:96`) |
| | | 世家旁支 | 0.08 (`:93`) |
| | | 书痴 | 0.05 (`:117`) |
| | | 其余 | 0 |

⇒ **出身侧上限 0.30**（南疆 + 蛊师学徒）。

---

## ④ 跨窗口风险推演（本节是报告重点）

### 4.1 速率模型

设 `L = TianfuTalents.liangu() + TianfuOriginBonus.liangu()`。在界面开着、`LianGu` 为真、`GuFang != 0` 时：

```
每 30 tick:
    本体： +1.0                    （lg_ke 攒满 30）
    附属： +floor(acc += L)        （仅当 cur ∉ [50,70] 且 cur < 99）
⇒ 合计速率 = (1 + L) 点 / 30 tick = (1 + L) / 1.5 点/秒
```

`L` 的可达区间与上限：

| 来源 | 值 | 成本 | 备注 |
|---|---|---|---|
| `v2_mind_liangu_1` | 0.15 | 玄 2 | 与 `_2` **互斥**（`TianfuMindTable:20-21` 同 `liangu` 族，`TianfuEconomy:155-172` 原子替换） |
| `v2_mind_liangu_2` | 0.30 | 地 3 | 与 `_1` 互斥 |
| `liangu_rushen` | 0.40 | 地 3 | 不分组 |
| `liangu_bujie` | 0.60 | 天 5 | 不分组 |
| `shen_liangu_tianshu` | 0.90 | 神 12 | 不分组 |
| 出身（南疆 + 蛊师学徒） | 0.30 | — | `TianfuOriginBonus` |

- 天赋侧上限 = `0.30 + 0.40 + 0.60 + 0.90` = **2.20**（23 点）
- **`L_max` = 2.50**

### 4.2 数值推演（逐相位扫描，30 个相位取统计）

推演脚本逻辑与本体/附属字节码一一对应：本体每 30 tick `+1`；附属错位 `phase` tick 后每 30 tick `acc += L; bonus = floor(acc); cur = min(99, cur + bonus)`（且 `cur ∉ [50,70]`）。统计量：窗口停留秒数（`tick / 20`）、关键边界是否被精确命中。

| L | 合计速率 | 20–40 最短停留 | 50–70 最短停留 | 跳过 `==40` 的相位数 | 跳过 `==70` | 跳过 `==100` |
|---|---|---|---|---|---|---|
| 0.00（无天赋） | 1.00 | **31.5 s** | **31.5 s** | 0/30 | 0/30 | **0/30** |
| 0.15 | 1.15 | 27.0 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 0.30 | 1.30 | 24.0 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 0.40 | 1.40 | 22.5 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 0.60 | 1.60 | 19.5 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 0.90 | 1.90 | 16.5 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 1.15（0.40+0.60+0.15，10 点） | 2.15 | 15.0 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| 1.30（0.40+0.60+出身 0.30） | 2.30 | 13.5 s | 30.1 s | 0/30 | 0/30 | 0/30 |
| 1.45 | 2.45 | 12.0 s | 28.6 s | 0/30 | 0/30 | 0/30 |
| 1.65 | 2.65 | 12.0 s | 30.1 s | 0/30 | 0/30 | 0/30 |
| 1.90 | 2.90 | 10.6 s | 31.5 s | 0/30 | 0/30 | 0/30 |
| **2.20（四条全买）** | 3.20 | **9.0 s** | 28.6 s | **30/30** | 0/30 | 0/30 |
| **2.50（四条 + 出身满）** | 3.50 | **9.0 s** | 31.5 s | **30/30** | 0/30 | 0/30 |

### 4.3 结论一：**50–70 窗口不会被任何加成组合跳过**

理由是结构性的，不是巧合：

```
TianfuEffects:272  inFeedWindow = cur >= 50.0 && cur <= 70.0
TianfuEffects:298  ... && !inFeedWindow && ...
```

一旦 `cur` 落进 `[50,70]`，附属**整段停止补点**，此后进度只由本体 `+1` 推进 ⇒

- 逐点推进 ⇒ `progress == 70` **必然被精确命中**（上表 0/30 跳过）；
- 窗口内停留恒为 21 点 × 1.5 s ≈ **31.5 s**（最差 28.6 s，仅因进入窗口那一步是跳进来的、少占几个点）；
- 单拍最大步长 = `floor(L + 0.999) + 1 ≤ 4`，**远小于窗口宽度 21**，不可能跨过。

⇒ **任务书假设的「一步跨过 50-70 导致失去投料机会」不成立。** 这段设计是对的。

### 4.4 结论二：**20–40 段有真实缺陷（B 族 351 个蛊方）**

B 族的第 1 段窗口 `20 ≤ p ≤ 40` **没有出现在 `inFeedWindow` 判定里**（`TianfuEffects:272` 只写了 50/70）。后果三条：

**(a) 窗口时长被压缩 3.5 倍。** `L=0` 时 31.5 s，`L=2.2/2.5` 时 **9.0 s**。而玩家要做的是：往 5 个槽精确放入指定物品与**精确数量**（例如 617 的第 1 段槽 3 要 64 个）。9 秒内完成「读配方 → 摆 5 个槽 → 数对数量」在实测上很勉强（未实测，需进游戏确认）。

**(b) `progress == 40` 的边界失败判定被 100% 跳过。** `L ≥ 2.2` 时 30 个相位全部跳过 40（上表）。后果是**结局更糟而不是更好**：

| 玩家行为 | 本体设计路径 | 加速后的实际路径 |
|---|---|---|
| 第 1 段没投料，正常推进 | 到 40 时触发「炼制失败」：扣血 + 魂魄 -`zhuanshu*10` + **明确提示** | 跳点跨过 40 ⇒ 无扣血、无提示、继续推进 |
| 继续到 50–70 投第 2 段 | （已被 40 判定终止，不会走到这里） | 可以投第 2 段并成功置 `score2=1` |
| 到 100 | — | `score1 != 1` ⇒ **结算整段跳过** ⇒ 进度清零、配方清空、材料与魂魄**静默全损**、零提示 |

即：加速把「有一次明确失败反馈的失败」变成了「一次毫无反馈的、看起来像成功的全损」。这是**负体验修正**，必须处理。

**(c) `bonus` 可以是 2–3 点，破坏本体「逐点推进」的隐含前提。** 本体全部 4 个边界判定（`==100`、`==40`、`==70`、以及窗口区间的每个整数点）都是按「每次 +1」写的。附属 `floor(acc)` 在 `L ≥ 2` 时单拍给 2–3 点（`TianfuEffects:303-307`），这正是 (b) 的机理。

### 4.5 安全处理方案（不改本体，只改附属）

按性价比排序，**建议 (1)+(2) 一起做**：

**(1) 把冻结区间改成「所有投料窗口的并集」——最小改动、收益最大**

```java
// TianfuEffects.java:272 现状
boolean inFeedWindow = cur >= 50.0 && cur <= 70.0;

// 建议：20–40 段同样是硬性投料窗口，必须一起冻结
boolean inFeedWindow = (cur >= 20.0 && cur <= 40.0)
                    || (cur >= 50.0 && cur <= 70.0);
```

效果：B 族第 1 段恢复到 31.5 s，`==40` 恢复 30/30 命中，`==70` 与 `==100` 不受影响（本来就是 0/30）。C 族（94 个无窗口蛊方）会额外多冻结 41 点 ≈ 61.5 s 的无意义等待——若在意，可再按 `GuFang` 是否属于 C 族做一次白名单跳过（需要一张 94 项的 id 表，见 ②）。

**(2) 每拍最多补 1 点（把 `bonus` 钳到 1）**

```java
// TianfuEffects.java:303-307
int bonus = (int) Math.floor(acc);
if (bonus > 0) {
    bonus = 1;                    // ← 新增：单拍恒定 1 点，恢复本体的逐点语义
    acc -= 1.0;
    v.LianGuJinDu = Math.min(99.0, cur + 1.0);
    ...
}
```

代价：`L > 1` 时小数累加器会**积压**（`acc` 每拍净增 `L-1`）。必须同时改溢出策略，否则后期速率失真。二选一：

- `acc = Math.min(acc, 0.999)`：速率被硬钳到 1 点/拍，等于「+100% 封顶」——**语义最简单、最安全**，但高投入天赋的收益被削顶；
- `acc` 不钳，但在 `cur` 落进任何窗口前**把积压清零**：保住长期平均速率，又保证窗口逐点推进。

**(3)（可选，治本）窗口前减速到「必然逐点」**

在 `cur` 距任一窗口下界 ≤ 3 点时强制 `bonus = 1`，保证「进入窗口的那一步」是逐点进入，从而窗口内每个整数点都被采样。这比 (2) 更温和（只在临界区降速），但代码更绕。

**不做的事**（明确写下来防误改）：

- **不要**去改本体 `==100` / `==40` / `==70` / 窗口区间 —— 那是本体规则，且 791/792 个类都这么写。
- **不要**把进度推进改成「每 tick 无条件 +」。
- **不要**在窗口内继续加速。
- **不要**为了规避静默全损而替玩家投料 / 绕过 `score` 标记 —— 那等于跳过材料检查。

---

## ⑤ 五个炼蛊天赋逐条核对

| id | 名称 | 品质 | cost | 实际 `LIANGU` | 其它收益 | 可叠加性 | 候选表对照 |
|---|---|---|---|---|---|---|---|
| `v2_mind_liangu_1` | 炼蛊进度 I | 玄 | **2** | **+0.15** | 无 | 与 `_2` 互斥（同 `liangu` 族） | ✅ 候选 +0.15 / 2 **一致** |
| `v2_mind_liangu_2` | 炼蛊进度 II | 地 | **3** | **+0.30** | 无 | 与 `_1` 互斥 | ✅ 候选 +0.30 / 3 **一致** |
| `liangu_rushen` | 炼蛊如神 | 地 | **3** | **+0.40** | 无 | 可与其余三条全叠 | ❌ 候选 +0.40 / **4**；实际 cost **3**（便宜 1 点） |
| `liangu_bujie` | 炼蛊不竭 | 天 | **5** | **+0.60** | **`GU_CD +0.30`（蛊冷却每 tick −0.3）仍在** | 可全叠 | ❌ 候选 +0.60 / **6**；实际 cost **5**；且**冷却收益未移除** |
| `shen_liangu_tianshu` | 神·炼蛊天术 | 神 | **12** | **+0.90** | **`GU_CD +0.50`（每 tick −0.5）仍在** | 可全叠 | ❌ 候选 +0.90 / **9**；实际 cost **12**（贵 3 点）；且**冷却收益未移除** |

证据：

```
TianfuTalents.java:168   t("v2_mind_liangu_1", "炼蛊进度 I", "玄", 2, "+15%", f(Op.LIANGU, 0.15))
TianfuTalents.java:169   t("v2_mind_liangu_2", "炼蛊进度 II", "地", 3, "+30%", f(Op.LIANGU, 0.30))
TianfuTalents.java:250   t("liangu_rushen", "炼蛊如神", "地", 3, "炼蛊进度 +40%", f(Op.LIANGU, 0.40))
TianfuTalents.java:290-291 t("liangu_bujie", "炼蛊不竭", "天", 5,
                             "炼蛊进度 +60%、蛊冷却缩短（每 tick −0.3）",
                             f(Op.LIANGU, 0.60), f(Op.GU_CD, 0.30))
TianfuTalents.java:317-318 t("shen_liangu_tianshu", "神·炼蛊天术", "神", 12,
                             "炼蛊进度 +90%、蛊冷却缩短（每 tick −0.5）",
                             f(Op.LIANGU, 0.90), f(Op.GU_CD, 0.50))
TianfuTalents.java:132   private static Talent t(String id, String name, String q, int cost, String desc, Fx... fx)
```

**叠加性结论**：`liangu_rushen(0.40) + liangu_bujie(0.60) + shen_liangu_tianshu(0.90) = 1.90` 三条无任何分组约束，**必然可以全买**；再加 `v2_mind_liangu_2(0.30)`（与 `_1` 二选一）⇒ **2.20**，加出身 **0.30** ⇒ **2.50**。这正是 4.4 中「`==40` 必然被跳过」的达成门槛。

**关于「是否仍带蛊虫冷却收益」**：仍在。`Op.GU_CD` 由 `TianfuTalents.guCd()`（`:791-793`）汇总，在 `TianfuNativeProgression.java:68` 被用来缩短本体蛊冷却：

```java
// TianfuNativeProgression.java:68
return Guzhenren98Progression.itemCooldownTicks(nativeTicks, TianfuTalents.guCd(TianfuApply.effective(player)));
```

⇒ 任务书要求「移除 `liangu_bujie` / `shen_liangu_tianshu` 的蛊虫冷却收益」**未执行**。这两条现在是**双收益条目**（进度 + 冷却），与「一事一条」的设计口径冲突，且让「炼蛊」类条目挤占了「蛊冷却」类条目的生态位（`v2_mind_cooldown_1/2` 只有 0.15/0.25）。

**候选表评价**：

- `+0.15/2`、`+0.30/3`：**采纳现状**，与候选表完全一致。
- `+0.40/4`：建议**采纳候选的 4 点**（现状 3 点偏便宜：0.40/3 = 0.133 点/%，而 `+0.30/3` 是 0.100 点/%，`+0.15/2` 是 0.133 点/%，三条同价档内 0.40 明显更划算）。
- `+0.60/6`：**采纳候选**（现状 5 点；且移除 `GU_CD` 后必须涨价，否则纯收益缩水却没补偿）。
- `+0.90/9`：候选 9 点 vs 现状 12 点。**建议维持 12 点**——移除 `GU_CD 0.50` 后若还降价，`神` 档的性价比会高于 `天` 档，破坏品质梯度；若确实要降到 9，应同时把 `+0.90` 降到 `+0.75` 左右以维持「每点 0.083~0.10 进度」的横向一致性。

---

## ⑥ 职责分离核对

| 检查项 | 结果 | 证据 |
|---|---|---|
| 是否把「炼蛊进度加成」误接成「炼蛊成功率」 | **否** | 全附源码搜索 `LianGuChengGongLv` **0 命中**；`Op.LIANGU` 只在 `TianfuEffects:262` 与 `TianfuTalents:786` 出现 |
| 是否改动炼道流派值 `liupai_liandao` | **否** | 无任何写入；`liupai_liandao` 仅出现在本体成功率公式里 |
| 是否改动材料判定 / 槽位 / 加料窗口 | **否** | 全附源码搜索 `GUCAIYUANSHI` / `getSlots` / `Slot` / `中途加材料` **0 命中**（`TianfuEffects:246,251` 是注释） |
| 对本体炼蛊字段的写入清单 | 只有 2 处，都是 `LianGuJinDu` | `TianfuEffects:281`（坏档迁移）、`TianfuEffects:307`（正常补点） |
| 对本体炼蛊字段的读取清单 | 3 处 | `TianfuEffects:261`（`LianGu`）、`:265`（`LianGuJinDu`）、`:270`（`GuFang`） |
| 是否写 `LianGu = false` 强行终止 | **否** | 无写入 |
| 是否写 `GuFang` | **否** | 无写入 |
| 是否改失败条件（`score` 标记） | **否** | 无写入 |
| 是否自动添加材料 | **否** | 无 `Slot.set` 调用 |

⇒ **职责分离成立**：附属只做一个动作——按 30 tick 节拍往 `LianGuJinDu` 里加整数点，其余一概不碰。

---

## ⑦ 禁止项核对

| 禁止项 | 是否违反 | 说明 |
|---|---|---|
| 改变材料数量 | **未违反** | 附属不写任何槽位 |
| 改变加料窗口长度 | **未违反**（但 4.4 的冻结缺失会**间接**缩短 B 族第 1 段窗口） | 附属没有改窗口常量；只是没有在 20–40 段减速 |
| 跳过材料检查 | **未违反** | 无 `Slot` / `score` 写入 |
| 自动添加材料 | **未违反** | 同上 |
| 改变失败条件 | **未违反** | 同上 |
| 把进度写成 100 | **未违反** | `Math.min(99.0, …)` + `cur < 99.0` 双保险 |
| 写小数进度 | **未违反** | `floor(acc)` + 本体只 `+1.0`，全程整数 |
| 移除冷却收益（任务书要求） | **未执行** | 见 ⑤ |

---

## ⑧ 需修改的类与方法（1.4.18 建议清单）

按优先级：

| # | 文件:行 | 改动 | 理由 | 优先级 |
|---|---|---|---|---|
| 1 | `TianfuEffects.java:272` | `inFeedWindow` 扩成 `(20 ≤ cur ≤ 40) \|\| (50 ≤ cur ≤ 70)` | B 族 351 个蛊方的第 1 段投料窗口被加速压到 9.0 s 且 `==40` 判定被绕过（4.4 a/b） | **P0** |
| 2 | `TianfuEffects.java:303-307` | `bonus` 钳到 1，并明确 `acc` 溢出策略 | 恢复本体「逐点推进」前提；否则任何 `L ≥ 2` 的组合都会破坏边界判定（4.4 c） | **P0** |
| 3 | `TianfuTalents.java:290-291` | 删除 `f(Op.GU_CD, 0.30)`，描述改为纯「炼蛊进度 +60%」 | 任务书要求移除冷却收益；消除双收益条目 | **P1** |
| 4 | `TianfuTalents.java:317-318` | 删除 `f(Op.GU_CD, 0.50)`，描述改为纯「炼蛊进度 +90%」 | 同上 | **P1** |
| 5 | `TianfuTalents.java:250` | `cost` 3 → 4 | 与候选表对齐，修正性价比断层 | P2 |
| 6 | `TianfuTalents.java:290` | `cost` 5 → 6（与 #3 一起改） | 移除冷却收益后需补偿定价 | P2 |
| 7 | `TianfuEffects.java:250-251` | 修正注释：窗口是区间且**分两段**；材料要求逐蛊方不同，不是「槽 0 蛊材原石」 | 防止后续维护者按错误注释改代码 | P2 |
| 8 | `TianfuEffects.java:257` | 修正注释「20 点 ≈ 10 秒」→「21 点 ≈ 31.5 s」 | 同上 | P3 |
| 9 | `TianfuEffects.java:280-284` | 坏档迁移只在「本世已确认」时执行（当前未受 `active` 闸约束） | 未确认玩家也会被改写 `LianGuJinDu`，虽然无害但越界 | P3 |

> 若无 `GuFang` 白名单，改动 #1 会让 C 族（94 个无投料窗口的蛊方）在 20–40 与 50–70 各白等 31.5 s。这是**可接受的副作用**（只慢不坏），比「静默全损」轻得多；若要消除，附一张 94 项 id 表即可（清单见 ②）。

---

## ⑨ 未验证项（需进游戏实测）

以下全部**没有**运行证据，只是静态推导，交付时必须原样保留这句：

1. **所有时序结论一律未实测。** 包括：`lg_ke` 是否真的一 tick 调一次 `execute`、`player.tickCount % 30` 与本体 `lg_ke` 节拍的实际相位关系、窗口停留秒数、`L` 与实测加速倍率是否吻合。相位影响 4.2 中「跳过 40」的具体取值（0/30 或 30/30），实测可能落在中间。
2. **9.0 s 是否真的做不完投料。** 取决于 GUI 交互（拖拽/Shift 点击/是否要数 64 个）。需实测：买满 `liangu_bujie + shen_liangu_tianshu + liangu_rushen + v2_mind_liangu_2`（L=2.20）后炼一只 **B 族**蛊（推荐 GuFang 617 暗箭蛊方），观察是否来得及在 20–40 段摆好槽 3–7。
3. **第三方附属写的 `GuFang`（六合一 `9000+index`、魂道自有编号）是否会导致「卡在 99」。** 推理：本体 792 个完成型过程按自己的 `GuFang` 值分派，`9000+i` 不匹配任何一个 ⇒ 本体永不 `+1` ⇒ 附属把进度顶到 99 后 `cur < 99.0` 闸生效 ⇒ **永久停在 99，永远不完成**。这一条**无法静态确认**，因为需要看六合一/魂道自己的完成逻辑。本次环境里
   `D:\game\mcmod\1\.minecraft\versions\转生到蛊界就要拿出真本事\mods` **不存在**，无法取出那两个 jar 核对。
   **建议实测项**：装六合一，用它的炼蛊流程开炼，看进度条是否停在 99。
   若确实卡住，最小修法是给补点加一道「本体是否认识这个 GuFang」的白名单判据（用 ② 的 787 个 id 建表），而不是继续放宽 `!= 0.0`。
4. **`liangu_prev` / `liangu_acc` 在换蛊、死亡重生、维度切换、断线重连后的表现。** 代码只在 `cur < prev` 时清累加器，`PlayerVariables` 是否在这些场景重置进度未核实。
5. **`GU_CD` 移除后的实际冷却手感**（#3/#4 改动的副作用），以及 `v2_mind_cooldown_1/2` 是否需要同步调价。
6. **C 族是否需要 20–40/50–70 也冻结**（改动 #1 的副作用是给 94 个蛊方白加 61.5 s 等待），需实测确认是否可接受。
7. **`inFeedWindow` 端点语义**：本体窗口含端点（`<=70`），附属冻结也用 `<= 70.0`，两者一致；但若日后本体改成 `<= 69.9`，附属的 70 冻结会多停一拍。属潜在耦合，非当前缺陷。

---

## 附录 A：本次使用的扫描方法（可复现）

1. **窗口族分类**：解析全部 `net/guzhenren/procedures/*.class` 的常量池与 `Code` 属性，逐条找
   `ldc2_w <const>` + `dcmpl/dcmpg` + 条件分支 的三元组，并回溯前一条 `getfield` 取字段名。
   得到 `(字段, 常量, 分支)` 全集后按 `LianGuJinDu iflt 50 / ifgt 70`、`LianGuJinDu iflt 20 / ifgt 40`、
   `GuFang ifne <id>`、`LianGuJinDu ifne 100` 组合归类 ⇒ 792 个完成型类分三族。
2. **中文字符串还原**：`javap` 经 PowerShell 管道会把中文压成 GBK 乱码，改为直接用
   Python `zipfile` 解析 class 常量池的 `CONSTANT_Utf8`，按 UTF-8 解码 ⇒ 还原出
   `中途加材料` / `中途加材料1` / `中途加材料2` / `炼制成功` / `炼制失败` / `guzhenren:ben_ming_gu` /
   `guzhenren:lian_gu_shi_bao_liu`。
3. **材料表抽取**：在 `javap -c` 文本里定位 `invokestatic getAmountInGUISlot`，向前取槽位号
   （`iconst_N` / `bipush N`）与物品字段（`GuzhenrenModItems.*` / `GuzhenrenModBlocks.*`），
   向后取 `bipush M` + `if_icmpne` ⇒ `(槽, 物品, 数量, 分支)`。
4. **相位推演**：按 4.1 的速率模型逐 tick 模拟 30 个相位，统计窗口停留与边界命中。
   （脚本 `sim.py` / `sim2.py` / `sim4.py` 的等价逻辑已写在 4.2，可据此复核。）

## 附录 B：本次用到的本体类清单

| 类 | 作用 |
|---|---|
| `net/guzhenren/world/inventory/LianGuguiMenu` | `onPlayerTick` 唯一送入口 + `removed` 关界面判失败 |
| `net/guzhenren/procedures/GuilianguzhizuoProcedure` | 开炼（归炼蛊方）：进度=0、`lg_ke`=0、`GuFang`=773、清投料标记 |
| `net/guzhenren/procedures/GuilianguwanchengProcedure` | 推进 + 完成 + 结算 + 单窗口（GuFang 773） |
| `net/guzhenren/procedures/AnJianGuguFang2Procedure` | B 族双窗口样本（GuFang 617） |
| `net/guzhenren/procedures/BaiYuGuWanChengProcedure` | A 族单窗口样本（GuFang 42） |
| `net/guzhenren/procedures/HuYaoGuGuFang3Procedure` | 唯一 `>= 100` 结算例外（GuFang 234） |
| `net/guzhenren/procedures/LianGuguiGaiGUIGuanBiShiProcedure` | 关界面 → 判失败：清进度、魂魄 −5、5 点伤害、「炼制失败」 |
| `net/guzhenren/procedures/LianGuguiDangGaiGUIDaKaiShiMeiKeFaShengProcedure` | GUI 开启期间每 tick 的总分派 |
| `net/guzhenren/procedures/LianGu2Procedure` / `LianGu5Procedure` / `LianGu6Procedure` | 按 `GuFang` 值再分派到具体蛊方过程 |
| `net/guzhenren/network/GuzhenrenModVariables$PlayerVariables` | `LianGu` / `LianGuJinDu` / `lg_ke` / `GuFang` / `huopo` / `liupai_liandao` / `LianGuChengGongLv` 宿主 |
| `net/guzhenren/init/GuzhenrenModMenus$MenuAccessor` | `getSlots()` 返回 `Map<Integer, Slot>`，槽号即菜单槽号 |
