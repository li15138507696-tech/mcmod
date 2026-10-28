# 天赋系统重做：死亡得点 · 寿元清零 · 本体天赋入池

## Context

用户要求重做天赋系统：
1. **点数来源改为死亡**：每死一次，按当前**转数**（本体 `zhuanshu` 字段，开窍=1，修炼突破+1，最高 5，转世清 0）发给天赋点数，点数可随时购买天赋。
2. **寿元结束点数清零**：本体转世（[JingJieBaoCunProcedure](file:///d:/daima/mcmod/_reverse/dec_out2/net/guzhenren/procedures/JingJieBaoCunProcedure.java) 监听死亡事件，`shouyuan<=0` 或修炼 gamerule 关闭时触发全面重置）时，清空点数与已购天赋。
3. **本体天赋入池**：本体的 10 种十绝体、甲乙丙丁档位机缘全部拉进天赋池，与现有凡体体质/词条一样由玩家用点数购买。

## 已核实的本体机制（设计依据）

| 机制 | 事实 | 来源 |
|---|---|---|
| 转数 | `PlayerVariables.zhuanshu`：开窍置 1（Uikongqiaoguanbi L338-344），修炼按钮突破 +1（UIxiuliananjian 4 处），转世清 0，KE1/KE2 按 1~5 转给奖励 | javap + grep |
| 转世入口 | `JingJieBaoCunProcedure`（dec_out2）`@SubscribeEvent LivingDeathEvent`：玩家无 FUHUO 效果时，`修炼gamerule开 && shouyuan<=0` 或 `修炼gamerule关`（任何死亡）→ 全面重置（zhuanshu/tizhi/kongqiao/全部道痕/真元清零） | L68-L247 |
| 甲乙丙丁天赋 | 甲(2)：真元上限 800-990 + 气运 1-10 + 1/3 命格-2否则天资流程；乙(1/4)：600-790 + 气运上限10-20 + 1/2命格-1；丙(5-7)：400-590 + 1-3 + 70%命格-1；丁(8-10)：200-390 + 1-10 + 90%命格-1 | Uikongqiaoguanbi L230-345 |
| 档位反推 | KE1 每 tick 按 `zuida_zhenyuan` 反推 kongqiao（800-999→甲2、600-799→乙1、400-599→丙5，<400 不动），**kongqiao==3 豁免** → 买机缘写真元后档位自动同步，天然自洽 | KE1 L593-614 |
| 十绝体 | 开窍时 zhenyuan=1000 + tizhi=1-10 随机，各写 2000/200 道痕 + 附带字段（2号周道时间/CD、3号魂魄承载+随机魂道/冰雪道、7号精力上限200、8号念头两字段）；运行时效果走 KE2 的 `kongqiao==3` 门控 → **购买时把 kongqiao 写成 3 即可让本体效果原生生效** | Uikongqiaoguanbi L20-215 |
| kongqiao 随机 | 在 `UikongqiaochongzhizizhiProcedure`（另一处）roll 1-10，与天赋授予分离 | grep |

## 借鉴「修仙家族模拟器」的采纳项

修仙家族模拟器（含修2）天赋/轮回机制 → 本系统映射：

| 修仙家族模拟器机制 | 采纳方式 |
|---|---|
| 死亡/转世按境界折算天赋点，境界越高越多 | ✅ 已定：每死一次按转数给点（转数=本体境界 1~5 转） |
| 转世时点数必须用完，剩余清零 | ✅ 已定：寿元结束点数清零（转世重置） |
| 灵根（资质）本身用点数购买 | ✅ 已设计：甲乙丙丁机缘入池（写真元上限→KE1 自动反推档位） |
| 定价曲线极端：词条 1-2 点 / 功能天赋 10 点 / 传说体质 50 点 | ✅ 采纳：拉开定价差距（见下方定价表），十绝体=传说级高价 |
| 无资质门禁，高级天赋纯靠攒点购买 | ✅ 采纳：神级天赋取消甲等门禁，12 点攒够即买（服务端校验同步取消） |
| 局内每次大境界突破额外得点 | ⬜ 待确认（决策 5）：是否在转数+1 时额外奖励一次点数 |
| 家族血脉继承（子女继承父母天赋） | ❌ 不采纳：单人流无家族概念，且用户明确"转世清零" |
| 天赋槽位上限 7 条 / 天赋丹道具 | ❌ 不采纳：用户已定"无条目数量上限"；项目约束"无新增物品" |

## 待确认决策（已按推荐值写好，回复「按推荐」即可，或指出要改的项）

| # | 决策 | 推荐 | 备选 |
|---|---|---|---|
| 1 | 本体开窍随机赠送是否取消 | **A：保留**——本体照常随机给（甲乙丙丁机缘/十绝体），池子额外可买，玩家可"逆天改命"（丁等攒点买甲等/十绝体）。安全：不动本体流程、无开局软锁风险、旧角色兼容 | B：取消本体赠送，全部靠购买（白手起家，但开局真元=0，修炼链路有软锁风险） |
| 2 | 死亡点数公式 | **(转数+1)×3**：一转死 6 点 → 五转死 18 点 | (转数+1)×2（4~12，偏紧）；自定义 |
| 3 | 购买入口 | **K 键合一界面**：现有查看页升级为「查看+购买」同屏（顶部点数条，点够即可点击购买），死亡时聊天栏提示 +N 点 | 保留死亡后自动弹购买页 + K 只查看 |
| 4 | 神级天赋（12 点 3 条）门禁 | **取消门禁**（借鉴修2：纯价格门槛，12 点攒够即买） | 保留「kongqiao==2 或已购甲等机缘」门禁 |
| 5 | 大境界突破额外得点（借鉴修2局内机制） | **不启用**——保持"每死一次给点"单一来源，突破收益已体现在转数公式里 | 启用：突破瞬间一次性奖励 新转数×2 点 |

**新条目定价**（借鉴修2曲线，拉开档次，可调）：凡体体质 3 / 词条 1~5（现状）/ 神级 12 / 丁等机缘 5 / 丙等机缘 8 / 乙等机缘 12 / 甲等机缘 16 / **十绝体 24**（传说级）。

## 实现方案

### 1. 死亡得点 + 转世清零（新 Mixin：`mixin/JingJieBaoCunProcedureMixin.java`）
`@Inject(method="execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V", at=HEAD)`，在本体重置**之前**判定，保证顺序确定：
```java
if (entity instanceof ServerPlayer sp) {
    boolean fuhuo = sp.hasEffect(GuzhenrenModMobEffects.FUHUO);   // 复活不算死
    boolean xiuwei = 修炼 gamerule 开;
    boolean zhuanshi = !fuhuo && (!xiuwei || v.shouyuan <= 0.0);
    if (zhuanshi)       TianfuLifecycle.onReincarnation(sp);      // 清空点数/已购/杀神值/吞噬进度
    else if (!fuhuo)    TianfuLifecycle.onDeathGrant(sp);         // points += (zhuanshu+1)*3，聊天提示
}
```
- `TianfuLifecycle`（新类）：`onDeathGrant` / `onReincarnation`（调 `TianfuData.wipeAll`，含 slay/devour/飞行体质收回——复用现有"失去体质收回+防摔"逻辑）。
- 现有 `TianfuSlay.onDeath`（杀神死亡 -20%）不动。

### 2. 数据死亡存活保险（`client`/common 事件）
新增 `PlayerEvent.Clone` 监听：把旧玩家 `getPersistentData()` 里 `tianfu` 子标签复制到新玩家（NeoForge 死亡 clone 对根级 persistentData 的复制行为不确定，显式复制保底）。

### 3. 池子扩充（`TianfuPool.java`）
- 新增 kind：`机缘`（4 条）与 `十绝体`（10 条，tizhi 1-10，名字用本体：太日阳莽体…宇宙大衍体）。
- 十绝体条目标注随机要素（3号随机魂道/冰雪道、5号随机炎/雷道）。

### 4. 购买结算重构（`TianfuApply.java` → 增量购买）
- 一次性 settle 改为 `buy(player, id)`：校验点数 ≥ cost、去重（同词条限 1）、**体质槽限 1**（凡体/十绝体共用）、**机缘槽限 1**、互斥（已购十绝体禁买机缘；已购机缘可升买十绝体）、神级门禁（决策 4）。
- 新增 `TianfuNative` 类复刻本体开窍写入：
  - 机缘条目：按档位写 `zuida_zhenyuan`（区间随机）+ `qiyun_shangxian`（+甲等另加 `qiyun`）+ 命格 roll（直接调 `TianziliuchengProcedure.execute` / 写 mingge，与本体同概率）→ KE1 自动同步档位。
  - 十绝体条目：写 `kongqiao=3`、`tizhi=N`、`zuida_zhenyuan=1000`、对应 2000/200 道痕 + 附带字段（3/5 号内部随机）→ KE2 原生生效、本体面板显示十绝体。
- `sanitize` 的神级门禁同步改（决策 4）。

### 5. UI / 网络（`TianfuViewScreen` → 合一界面，`TianfuNet`、payload）
- `TianfuRequestPayload`（K 键）→ 服务端回全量状态 → 客户端打开合一界面（查看区 + 可购区，条目 hover 显示价格/效果，点击购买）。
- 新增 C2S `TianfuBuyPayload(id)` → 服务端 `buy()` → 扣点、立即生效、回包刷新。
- 死亡得点聊天提示「§b[天赋] 死亡领悟 +N 点天赋点（转数X）」。

### 6. 清理
- `TianfuTier`（pointsFor）删除；`TianfuOpening` 去掉发点/弹窗逻辑，仅保留杀神 1/11 覆写 + 开窍提示语（决策 1A 下不动本体授予）。
- `project_memory.md` 同步新机制。

## 验证
1. `gradlew build` 编译通过。
2. 用户手动拷 jar 到 mods 后游戏内验证：
   - 死亡一次 → 聊天提示得点（转数 1 → 6 点）；K 打开界面点数正确。
   - 买丁等机缘 → 真元上限变化、KE1 自动把档位反推为对应档。
   - 买十绝体 → kongqiao 变 3、KE2 效果生效（本体面板显示体质名）。
   - 寿元耗尽转世 → 点数/已购清零、体质效果消失（飞行收回）。
   - 普通死亡不掉已购天赋（Clone 复制生效）。
