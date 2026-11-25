# 修炼体系 + 突破体系 审查报告（T2）

- 审查对象：`tianfu` 1.4.17（目标 1.4.18），包 `net.guzhenren.tianfu`
- 本体：10.20 内层 jar `_crash3\basever\guzhenren-10.20.jar`
  - sha256 `B6D314D3810B8BC5FCFDBC4F588DB8721F0EFDFC14D27A0A06D88143A01FE2BC`，22371 class
- 环境：Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- 本报告**只读**，未修改任何源码、构建或版本文件
- 证据口径：本体侧一律写 `全限定类名@bci字节码偏移`（可用
  `javap -p -c -classpath <jar> <类名>` 复现，`bci` 即 `Code:` 左侧偏移，跨 javap 版本稳定）；
  附属侧写 `文件:行号`。行号已逐一核对。

---

## 0. 结论摘要（先看这 10 条）

| # | 结论 | 严重度 |
|---|---|---|
| 1 | 本体修炼**只在打坐态**发生：`XiulianjingjieMobEffect` 每 tick 调一次脉冲过程，而该效果就是打坐（动画名 `dazuo3`），且每次脉冲烧 **10 真元**。**"修炼速度"= 进度/真元的效率**，不是绝对速度。 | 关键 |
| 2 | `dazuo_wangwo`（打坐忘我）**没有任何打坐判定**，只是一个普通 `XIULIAN +0.40`。它的描述与实现**效果范围一致但不一致于机制**——因为所有修炼天赋本来都只在打坐脉冲生效，它是"重复的普通条目"，不是"限定打坐的特化条目"。 | 高 |
| 3 | 本体突破概率常数为 **`k = 7.5e-4`（mingge==5）/ `5.0e-4`（其余）**；附属 `Guzhenren98Progression:39` 写成 `destinyFive ? 0.001 : 0.0005` —— **mingge==5 分支系数错了 33%**，实测会导致天命五玩家"算出的基础概率 > 1 → 被 clamp 成 1 → 天赋加成被整体丢弃"。 | 高 |
| 4 | 现有突破实现是**「失败率乘算」**：`P' = 1-(1-P)(1-r)`，不是基础概率乘算、也不是百分点。当本体 `P ≤ 0`（四转起就是这个值）时，它会**把 0% 变成恰好 r**。 | 致命 |
| 5 | 因 `sum()` 纯加法，`突破通天 0.30 + 厚积薄发 0.20 + 修炼如神 0.40 = 0.90`，**10 点就能把"本体判定为不可能"的四转突破变成 90%**；再买 `神·修炼无极 0.60` → 合计 1.50 → 公式里 `max(0,1-1.5)=0` → **100% 必成**。 | 致命 |
| 6 | `suhui`（宿慧）实际只有 `+0.05 × max(1, zhuanshu)`，**无上限、也无"道痕获取"**。本体入口把 `zhuanshu` 卡在 5，故上限 +25%；若其它模组/命令抬高 `zhuanshu` 则线性无界。描述中的"道痕获取"**完全未实现**。 | 高 |
| 7 | `suiyue_chendian`（岁月沉淀）是**恒定 +10%**，与"已耗寿元越多"无关，描述与实现不符。 | 中 |
| 8 | 负面天赋**不能重复购买刷点**：`TianfuEconomy:106` 按 id 去重，且 `NEGATIVE_CAP=24` 总量封顶；买→撤销对称退还，净额为零，无刷点回路。 | 无 |
| 9 | **互斥表只覆盖 5 族 12 条 `v2_mind_*`**；`dazuo_wangwo / kuxiu_buchuo / suiyue_chendian / suhui / xiulian_rushen / shen_xiulian_wuji / ji_zhongdao_zhizun / tupo_tongtian / houji_bofa` **全部在表外**，同页同分组却可无限叠加。 | 致命 |
| 10 | 5 条条目**同时给"修炼"和"突破"两种独立收益**（`xiulian_rushen`、`shen_xiulian_wuji`、`ji_zhongdao_zhizun`、`v2_xinzhi_yousun`、`xiuxing_chizhi`），违反任务书禁止项；出身 `scholar`（书痴）也是双收益。 | 高 |

---

## ① 本体原始公式

### 1.1 修炼进度（`gushi_xiulian_dangqian`）

**触发链（唯一常驻入口）**

```
net/guzhenren/potion/XiulianjingjieMobEffect@11   shouldApplyEffectTickThisTick -> return true（每 tick 都算）
net/guzhenren/potion/XiulianjingjieMobEffect@16   applyEffectTick -> XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure.execute(level,x,y,z,entity)
```

全 jar 扫描：引用 `XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure` 的类**只有** `XiulianjingjieMobEffect`；
引用字段 `XIULIANJINGJIE` 的类**只有 4 个**（`GuzhenrenModMobEffects` 注册、
`XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure`、`UIxiuliananjianProcedure`、`GuroutuanyuangulvbuffXiaoGuoChiXuShiMeiKeFaShengProcedure`）。

**进门槛（`XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure.execute`）**

| bci | 条件 | 不满足时 |
|---:|---|---|
| 132 | `hasEffect(XIULIANJINGJIE) && duration > 0` | return |
| 176–177 | `duration > 2` | return（**最后 2 tick 不结算**） |
| 211 | `!hasEffect(LENGQUE)` | return（冷却态暂停） |
| 242–243 | `gushi_xiulian_dangqian != gushi_xiulian_jindu` | 跳到 1394（"已满"分支） |
| 272–279 | **`zhenyuan -= 10.0`** | —— 每次脉冲固定烧 10 真元 |

**增量公式（3 个互斥分支，全部 `dangqian += Δ`，本体自己不 clamp）**

设
`zsj  = zishenshijianjiasu`（自身时间加速，double）
`aA = amp(SI_WEI_JIU_CHONG_1)`、`aB = amp(QI_XIANG_JIU_CHONG_1)`、`aC = amp(JIU_CHONG_JIN_LIAN_ZHEN_YUAN)`
`aD = amp(YICUERJIU)`、`aF = amp(FEN_SHOU_JI_XIU_JUE_6)`
（无该效果时 amplifier 取 0）

| 分支 | 条件 | bci(PUTFIELD) | Δ |
|---|---|---:|---|
| A 凡体 | `tizhi == 2 && zhongzu != 1` | **618** | `(15.0*zsj + aA+aB+aC) * (aD+1) * (aF*10+1)` |
| B 天命四残寿 | `mingge == 4 && shouyuan < 100` | **964** | `((2.5 + (100-shouyuan)*0.1)*zsj + aA+aB+aC) * (aD+1) * (aF*10+1)` |
| C 默认 | 其余 | **1245** | `(2.5*zsj + aA+aB+aC) * (aD+1) * (aF*10+1)` |

- 分支 B 的"寿元越少越快"是**本体自带**的：`shouyuan` 每少 1 年，基数 +0.1（100 年→2.5，0 年→12.5，约 5 倍）。
  **这就是 `suiyue_chendian` 本应挂的位置，而它没有挂在这里。**
- 脉冲自续：bci 1255–1321，若 `zhenyuan >= 10` 则 `addEffect(new MobEffectInstance(XIULIANJINGJIE, 15, 0, false, false))`
  —— 打坐态靠真元自动续期；`zhenyuan < 10` 时走 bci 1324 播 `dazuo3` 动画（**效果≡打坐，此处得到确认**）。
- 全 jar 精确统计（24 个类引用 `gushi_xiulian_*`，其中 **19 个类对 `gushi_xiulian_dangqian` 有 `PUTFIELD`，合计 35 处**）：

  | 类 | Dangqian PUTFIELD | 性质 |
  |---|---:|---|
  | `XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure` | **3** | 每 tick 打坐脉冲（附属已挂乘区） |
  | `UIxiuliananjianProcedure` | 10 | 突破成功后的 `dangqian = 0` 重置（**不应**加速） |
  | `JingJieBaoCunProcedure` | 4 | 境界存档恢复 |
  | `KE1Procedure` | 2 | 空窍初始化 |
  | `KE3Procedure` | 1 | 开窍流程 |
  | `XiuLianJinDu1Procedure` | 1 | `/xljd` 命令直写 |
  | `GuroutuanyuangulvbuffXiaoGuoChiXuShiMeiKeFaShengProcedure` | 1 | **古肉团圆蛊共振，真实进度增益，未挂乘区** |
  | 10 个 `*YouJiKongQiShiShiTiDeWeiZhiProcedure`（蛊实体位置回写） | 10×1 | 蛊主人字段回写 |
  | `GuzhenrenModVariables` / `$PlayerVariables` | 3 | 序列化 / 结构方法 |

  具体：`Guroutuanyuangulvbuff…@360` 写
  `dangqian += 5.0 * pow(zhuanshu_对方 - zhuanshu_自己, zhuanshu_自己 - zhuanshu_对方)`。**见 §3 漏洞 G。**

### 1.2 突破成功率（`UIxiuliananjianProcedure.execute`）

**前置门（按 bci 顺序）**

| bci | 条件 |
|---:|---|
| 17–20 | `!shiqiao` |
| 34–39 | `kongqiao != 0` |
| 53–71 | `gushi_xiulian_dangqian < gushi_xiulian_jindu` 时走"未满"支路 361 |
| 389–390 | **`gushi_xiulian_dangqian == gushi_xiulian_jindu`（`dcmpl`+`ifne`，精确双精度相等，否则 return）** |
| 407–409 | `zhongzu == 0` |
| 423–451 | `!(zhuanshu >= 5 && jieduan >= 4)` |
| 471–477 等 | 效果条件（`JIU_CHONG_JIN_LIAN_ZHEN_YUAN` / `SI_WEI_JIU_CHONG_1` / `QI_XIANG_JIU_CHONG_1`） |

> `UitupoxianshiProcedure@32-33` 同款精确相等（`dcmpl`+`ifne`）——突破按钮的**显示**也依赖 `dangqian == jindu`。
> 这是附属 `align()`/`clamp` 存在的理由，已实现正确（见 §3 漏洞 H 的边界说明）。

**概率公式（4 个真实尝试点，形状完全一致）**

```
P = 1.0
  - 0.1 * zhuanshu
  - 0.1 * zhuanshu * zhuanshu
  + 0.0002 * zuida_zhenyuan
  + 0.01   * qiyun
  + k      * renqi
k = 7.5e-4  (mingge == 5.0)
k = 5.0e-4  (其余)
```

**判定方式：`Math.random() < P`——`dcmpg` + `ifge <失败分支>`（相等也算失败），且本体对 P 不做任何 clamp。**

4 个站点（`require=4/expect=4/allow=4` 的 Mixin Redirect 命中全部 4 个）：

| 站点 | `Math.random` bci | `mingge` | `jieduan` 条件 | renqi 系数 bci | 失败跳转 |
|---|---:|---|---|---:|---|
| 1 | 1058 | `== 5.0` | `> 5.0`（bci 1054 `ifle`） | 7.5e-4 @1095 | `ifge 1864` |
| 2 | 2139 | `!= 5.0` | `> 5.0`（bci 2136 `ifle`） | 5.0e-4 @2176 | `ifge 2945` |
| 3 | 3827 | `== 5.0` | `> 4.0`（bci 3824 `ifle`） | 7.5e-4 @3864 | `ifge 4633` |
| 4 | 4908 | `!= 5.0` | `> 4.0`（bci 4905 `ifle`） | 5.0e-4 @4945 | `ifge 5714` |

**P 的实际量级**（`mingge != 5`，`zuida_zhenyuan=800`、`qiyun=10`、`renqi=0`）：

| zhuanshu | P |
|---:|---:|
| 1 | **+1.060** |
| 2 | +0.660 |
| 3 | +0.060 |
| 4 | **−0.740** |
| 5 | **−1.740** |

（满值 `zuida_zhenyuan=990 / qiyun=45 / renqi=1000` 时，四转才回到 `+0.148`，五转仍是 `−0.852`。）

**结论：本体的 P 是一个"未归一化的线性分"，常态落在 [−2, +2]，三转起就跌破 0。**
这意味着"突破成功率"在本体里**不是概率**，而是一个"能不能突破"的阈值量；任何"按概率修"的改法都必须先处理 P ≤ 0 与 P ≥ 1 两个饱和区。

**显示通道（与判定脱钩）**
`net/guzhenren/procedures/TupochenggonglvProcedure@execute` → `Math.round(P*100)` 拼字符串；
`net/guzhenren/client/gui/UIKONGQIAOScreen` 调用它展示百分比。
该过程**不认识任何天赋**，因此天赋加成后玩家看到的是**未加成的基础百分比**。

---

## ② 逐天赋表格

价格取 `TianfuTalents` 的 `cost` 字段（`TianfuLegacyPrices` 冻结价与之一致，已逐条比对）：
`v2_mind_xiulian_1/2/3 → 2/3/5`、`dazuo_wangwo → 3`、`kuxiu_buchuo → 2`、`suiyue_chendian → 2`、
`xiulian_rushen → 5`、`shen_xiulian_wuji → 12`、`suhui → 5`、`xiuxing_chizhi → −2`、
`v2_xinzhi_yousun → −5`、`houji_bofa → 2`、`tupo_tongtian → 3`、`ji_zhongdao_zhizun → 16`。

「层」列：`原语` = `TianfuTalents.Op` 经 `sum()` 聚合 → `TianfuNativeProgression` 在**本体真实写入点**做乘区/偏移；
`硬编码` = 不在词条表里，直接写死在 `TianfuNativeProgression` 的 if 分支。

| # | ID | 名称 | 品质/价 | 实际代码效果 | 证据（文件:行） | 挂载层 | 同类叠加 | 双收益 |
|---:|---|---|---|---|---|---|:--:|:--:|
| 1 | `v2_mind_xiulian_1` | 修炼速度 I | 玄 2 | `XIULIAN +0.20` | `TianfuTalents.java:160` | 原语 | ✗ 同族互斥（`TianfuMindTable:12`） | 否 |
| 2 | `v2_mind_xiulian_2` | 修炼速度 II | 地 3 | `XIULIAN +0.40` | `TianfuTalents.java:161` | 原语 | ✗ 同族互斥（`:13`） | 否 |
| 3 | `v2_mind_xiulian_3` | 修炼速度 III | 天 5 | `XIULIAN +0.60` | `TianfuTalents.java:162` | 原语 | ✗ 同族互斥（`:14`）；**但仍可与 #4~#9 叠加** | 否 |
| 4 | `dazuo_wangwo` | 打坐忘我 | 地 3 | `XIULIAN +0.40`，**无打坐判定** | `TianfuTalents.java:249`；`TianfuNativeProgression.java:17`（无 effect 判定）；`TianfuApply.java:63-74`（`effective()` 只查确认+出身） | 原语 | ✓ 与 #1-3、#5、#6、#7、#8 全部相加（`TianfuTalents.java:942-956` 纯加法） | 否 |
| 5 | `kuxiu_buchuo` | 苦修不辍 | 玄 2 | `XIULIAN += 0.20`（写死） | `TianfuNativeProgression.java:19` | 硬编码 | ✓ | 否 |
| 6 | `suiyue_chendian` | 岁月沉淀 | 玄 2 | `XIULIAN += 0.10`（写死，**与寿元无关**） | `TianfuNativeProgression.java:20`；本体真正的寿元项在 `Xiulianjingjie…@697-740` | 硬编码 | ✓ | 否 |
| 7 | `xiulian_rushen` | 修炼如神 | 天 5 | `XIULIAN +0.60` **且** `BREAKTHROUGH +0.40` | `TianfuTalents.java:288-289` | 原语 ×2 | ✓ 双向叠加 | **是** |
| 8 | `shen_xiulian_wuji` | 神·修炼无极 | 神 12 | `XIULIAN +1.50` **且** `BREAKTHROUGH +0.60` | `TianfuTalents.java:315-316` | 原语 ×2 | ✓ | **是** |
| 9 | `suhui` | 宿慧 | 天 5 | `XIULIAN += 0.05 * max(1.0, zhuanshu)`；**无道痕部分** | `TianfuNativeProgression.java:18`；全库 grep 仅此一处实现 | 硬编码 | ✓ | 否（但描述虚标） |
| 10 | `xiuxing_chizhi` | 修行迟滞 | 负 −2 | `XIULIAN −0.30` **且** `BREAKTHROUGH −0.40` | `TianfuTalents.java:343-344` | 原语 ×2 | ✓ | **是（负向）** |
| 11 | `v2_xinzhi_yousun` | 心智有损 | 负 −5 | `XIULIAN −0.40` **且** `BREAKTHROUGH −0.50` | `TianfuTalents.java:156` | 原语 ×2 | ✓ | **是（负向）** |
| 12 | `houji_bofa` | 厚积薄发 | 玄 2 | `BREAKTHROUGH += 0.20`（写死） | `TianfuNativeProgression.java:38` | 硬编码 | ✓ | 否 |
| 13 | `tupo_tongtian` | 突破通天 | 地 3 | `BREAKTHROUGH +0.30` | `TianfuTalents.java:248` | 原语 | ✓ | 否 |
| 14 | `ji_zhongdao_zhizun` | 极·众道至尊（**仅突破部分**） | 极 16 | `BREAKTHROUGH +0.80`（同条还含 `DAOHEN_ALL +3000`、`XIULIAN +2.00`） | `TianfuTalents.java:329-330` | 原语 ×3 | ✓ | **是（三重）** |

**效果落地链（三条）**

1. `XIULIAN` → `TianfuTalents.xiulian()` (`:776-778`) → `TianfuNativeProgression.cultivation()` (`:14-23`)
   → `Guzhenren98Progression.cultivation()` (`:16-26`)：`result = before + Δ * max(0, 1+bonus)`，再 `clamp(0, jindu)`。
   注入点 = `CultivationProgressMixin.java:16-22`（对 `Xiulianjingjie…` 的 3 处 `PUTFIELD gushi_xiulian_dangqian` 做 `@Redirect`，
   `require=3 expect=3 allow=3`，与本体实测 3 处一致）。
2. `BREAKTHROUGH` → `TianfuTalents.breakthrough()` (`:781-783`) → `TianfuNativeProgression.breakthroughRoll()` (`:35-44`)
   → `Guzhenren98Progression.breakthroughRoll()` (`:51-54`)：**平移随机数** `roll' = roll + P − P'`。
   注入点 = `CultivationButtonMixin.java:21-25`（对 `UIxiuliananjianProcedure` 的 4 处 `Math.random()` 做 `@Redirect`，
   `require=4 expect=4 allow=4`，与本体实测 4 处一致）。
   数学上 `roll' < P ⟺ roll < P'`，**机制本身是对的**（前提：`P` 算得准，见 §3 漏洞 D）。
3. `suhui / kuxiu_buchuo / suiyue_chendian / houji_bofa` → 不在词条表，走 `TianfuNativeProgression` 的 4 条 `if`。

**关于"是否同时给修炼和突破两种独立收益"（任务书禁止项）**
命中的有 5 条：`xiulian_rushen`、`shen_xiulian_wuji`、`ji_zhongdao_zhizun`、`v2_xinzhi_yousun`、`xiuxing_chizhi`。
额外的同性质问题（越界但同因）：出身 `scholar`「书痴」`TianfuOriginTable.java:117-120` 同时给
`XIULIAN +0.15` 与 `BREAKTHROUGH +0.10`，且文案写「突破成功率 +10%」（实现是失败率降低，
与突破通天等等价的语义不同，属 §3 漏洞 F）。

---

## ③ 数值漏洞清单

### 漏洞 A（致命）｜突破"失败率乘算"把 0% 变成 r

`Guzhenren98Progression.java:44-48`
```java
public static double breakthroughChance(double nativeChance, double reduction) {
    double chance = Math.clamp(nativeChance, 0.0, 1.0);
    return Math.clamp(1.0 - (1.0 - chance) * Math.max(0.0, 1.0 - reduction), 0.0, 1.0);
}
```
即 `P' = 1 − (1−P)(1−r) = P + r(1−P)`。

实测三口径差异（`r = 0.30`，即突破通天）：

| 本体 P | **A 现实现（失败率乘算）** | B 百分点（P+r） | C 基础概率乘算（P×(1+r)） |
|---:|---:|---:|---:|
| −0.74（四转常态） | **0.300** | 0.000 | 0.000 |
| −0.20（三转残值） | **0.300** | 0.100 | 0.000 |
| 0.00 | **0.300** | 0.300 | 0.000 |
| 0.06（三转常态） | **0.342** | 0.360 | 0.078 |
| 0.30 | 0.510 | 0.600 | 0.390 |
| 0.50 | 0.650 | 0.800 | 0.650 |
| 0.80 | 0.860 | 1.000 | 1.000 |
| 0.96 | 0.972 | 1.000 | 1.000 |

**问题**：本体 P 在**四转常态就已是 −0.74**。此时 A 口径给出**恰好 30%**——把"本体判定为不可能"的突破变成三成把握，
而 B/C 口径都保持 0%。这是"0 基处的绝对增益 ≠ 相对增益"的经典陷阱。

### 漏洞 B（致命）｜BREAKTHROUGH 加性叠加 → 确定性 100%

`TianfuTalents.java:942-956` 的 `sum()` 是纯加法；`Guzhenren98Progression:47` 用 `Math.max(0.0, 1.0 - reduction)`，
所以 `Σreduction ≥ 1` 时 `P' ≡ 1`（任何 P，包括负 P）。

| 组合 | 点数 | Σr | 对手感的影响 |
|---|---:|---:|---|
| `houji_bofa` + `tupo_tongtian` | 5 | 0.50 | 四转 0% → 50% |
| + `xiulian_rushen` | 10 | **0.90** | 四转 0% → **90%** |
| + `shen_xiulian_wuji` | 22 | **1.50** | **恒 100%** |
| `tupo_tongtian` + `ji_zhongdao_zhizun` | 19 | **1.10** | **恒 100%**（且白送 +3000 道痕、+200% 修炼） |
| 含出身 `scholar` 的六条全买 | 22 | **2.40** | 恒 100% |

点数预算：难度基础 2/4/6/8 + 转世加成上限 +10 + 负面补点上限 24（`TianfuDifficulty:47-51`、`TianfuSelection:11`）
→ **最高约 42 点**。上述任一组合都在预算内。

### 漏洞 C（致命）｜同族互斥未覆盖修炼/突破类

`TianfuMindTable.java:11-23` 全员清点：`xiulian / capacity / recovery / liangu / cooldown` 共 5 族 12 条，
**全部是 `v2_mind_*`**。逐个核对：

| 条目 | 在 `TianfuMindTable` 内？ |
|---|:--:|
| `v2_mind_xiulian_1/2/3` | ✅ |
| `dazuo_wangwo` | ❌ |
| `kuxiu_buchuo` | ❌ |
| `suiyue_chendian` | ❌ |
| `suhui` | ❌ |
| `xiulian_rushen` | ❌ |
| `shen_xiulian_wuji` | ❌ |
| `ji_zhongdao_zhizun` | ❌ |
| `tupo_tongtian` | ❌ |
| `houji_bofa` | ❌ |
| `xiuxing_chizhi` / `v2_xinzhi_yousun` | ❌（负向，可与正向共存） |

而 UI 侧 `TianfuViewScreen.java:196-197` 把它们**全部归到同一个「修炼」分组**：
```
if(id.contains("xiulian")||id.contains("dazuo")||id.contains("tupo")||id.contains("suhui")
        ||id.contains("kuxiu")||id.contains("houji")||id.contains("suiyue"))return "修炼";
```
→ 界面告诉玩家"这是同一类"，经济层却允许全买。这是"同类只允许选最高等级"没有落实的直接证据。

**修炼叠加实际上限（已验证加性）**

| 组合 | 点数 | ΣXIULIAN | 进度速率 |
|---|---:|---:|---:|
| `v2_mind_xiulian_3` + `dazuo_wangwo` + `kuxiu_buchuo` + `suiyue_chendian` | 12 | 1.30 | ×2.30 |
| 上排 + `suhui`(五转) + `xiulian_rushen` | 22 | 2.15 | ×3.15 |
| 上排 + `shen_xiulian_wuji` + `ji_zhongdao_zhizun` + 出身`药童` | 50（超预算，需转世） | 5.80 | **×6.80** |

### 漏洞 D（高）｜mingge==5 的 renqi 系数错 33%

| 项 | 本体 | 附属 |
|---|---|---|
| `mingge == 5` 的 renqi 系数 | **`7.5E-4`**（`TupochenggonglvProcedure@37`；`UIxiuliananjianProcedure@1095`、`@3864` 两处尝试点一致） | `0.001`（`Guzhenren98Progression.java:39`） |
| 其余 | `5.0E-4`（`@61`；`@2176`、`@4945`） | `0.0005` ✅ |

**后果（不是"多送"，而是"白送"）**：平移量 `roll' = roll + P算 − P'算`。
当 `P算 ≥ 1` 时 `clamp` 把 `P算`、`P'算` 都压成 1 → **平移量为 0 → 天赋加成被静默吞掉**。
实测误差（`r=0.30`，天命五，`zhuanshu=4`，`zuida_zhenyuan=800`，`qiyun=10`）：

| `renqi` | 本体 P | 附属算出的 P | 实际成功率 | **应为** |
|---:|---:|---:|---:|---:|
| 0 | −0.740 | −0.740 | 0.300 | 0.300 ✅ |
| 100 | −0.665 | −0.640 | 0.293 | 0.300 |
| 500 | −0.365 | −0.240 | 0.263 | 0.300 |
| 1000 | +0.010 | +0.260 | **0.232** | 0.307 |
| 2000 | +0.760 | +1.260（→clamp 1） | **0.500** | **0.832** |

即：**天命五 + 高人气，`突破通天` 从"应给 83%"变成"实际只有 50%"，甚至更快地趋近 0 收益。**

### 漏洞 E（高）｜`suhui` 描述虚标 + 无上限

`TianfuNativeProgression.java:18`
```java
if (ids.contains("suhui")) bonus += 0.05 * Math.max(1.0, variables.zhuanshu);
```
- **道痕获取**：`TianfuPool.java:67` 与描述都写"每转数提供额外修炼基数**与道痕获取**"；
  全库（`TianfuTalents` / `TianfuEffects` / `TianfuDaohen` / `TianfuNative*`）grep `suhui` 只有上面这一处实现
  → **道痕部分完全未实现**（0 行代码）。
- **上限**：代码里**没有** `Math.min` / clamp。上限只来自本体侧 `zhuanshu` 的天然天花板（`UIxiuliananjianProcedure@423-451`
  在 `zhuanshu≥5 && jieduan≥4` 时直接 return）→ 五转 = **+25%**。
- **会不会无限增长**：只要 `zhuanshu` 被抬高（`/xljd` 类命令、`KE1Procedure`/`JingJieBaoCunProcedure` 直写、
  其它附属、存档编辑），`bonus` **线性无界**。`20200` 转 → `+1010% → rate 11.1×`。
  **建议加 `Math.min(0.05*zhuanshu, 0.30)` 之类显式封顶，不要依赖别处的天花板。**

### 漏洞 F（中）｜描述与实现不符（3 条）

| 条目 | 描述 | 实现 | 判定 |
|---|---|---|---|
| `suiyue_chendian` | 「已耗寿元越多、修炼越快」（`TianfuPool:76`） | 恒 `+0.10`（`TianfuNativeProgression:20`） | **不符**：寿元项在本体 `Xiulianjingjie…@697-740` 已存在（分支 B），附属完全没接 |
| `suhui` | 「额外修炼基数**与道痕获取**」 | 只有 `+0.05*zhuanshu` 修炼 | **虚标** |
| `dazuo_wangwo` | 「**打坐时**修炼进度 +40%」 | 普通 `XIULIAN +0.40`，无任何打坐判定 | **机制不符 / 效果范围一致**（见 §3.1 专节） |

附带的显示问题：`TupochenggonglvProcedure` 不认识天赋，`UIKONGQIAOScreen` 展示的仍是**未加成**的百分比
→ 玩家看到 6%、实际 34%。同时 `TianfuOriginTable:120`「突破成功率 +10%」与 `突破通天`「失败概率降低 30%」
两种措辞在同一个加成池里混用，语义不同（+10 百分点 vs 降到 70% 失败），必须统一。

### 漏洞 G（中）｜修炼乘区覆盖不全（团圆蛊路径漏挂）

`CultivationProgressMixin` 只挂了 `Xiulianjingjie…` 的 3 处 `PUTFIELD`。实测统计：**19 个类、35 处** `PUTFIELD gushi_xiulian_dangqian`
（明细见 §1.1 表）。逐类判定性质后，**大部分漏挂是正确的**：

- ✅ 正确漏挂：`UIxiuliananjianProcedure` 的 10 处是突破成功后 `dangqian = 0` 的重置、
  `JingJieBaoCunProcedure` / `KE1Procedure` / `KE3Procedure` 是存档恢复与开窍初始化、
  `XiuLianJinDu1Procedure` 是 `/xljd` 管理命令。**这些若被乘区放大反而是 bug。**
- ❌ 真正的漏挂：`GuroutuanyuangulvbuffXiaoGuoChiXuShiMeiKeFaShengProcedure@360`——
  古肉团圆蛊共振，是**真实进度增益**（`dangqian += 5.0 * pow(Δzhuanshu, -Δzhuanshu)`），
  且它同样要求 `hasEffect(XIULIANJINGJIE)`（`@21-29`）。它不经过天赋乘区，也不经过 `clamp`/`align`。

后果：① 团圆蛊收益与天赋"修炼速度"不同乘区，`×6.8` 的速率对此路径无效；
② 更关键的——**它是唯一一条"打坐态之外"（但仍在 XIULIANJINGJIE 态内）能推进进度的路径**，
若将来把它接进 `TianfuNativeProgression.cultivation`，`dazuo_wangwo`「打坐时」的描述就会真正失真（见 §3.1）。

### 漏洞 H（低，需明确记录）｜clamp 是必要的，不是 bug

`Guzhenren98Progression.java:24` `result = Math.clamp(result, 0.0, target)` 与 `align()`（`:8-13`）
配合本体**精确相等**门（`UIxiuliananjianProcedure@389-390`、`UitupoxianshiProcedure@32-33`）是**正确且必须的**：
本体不 clamp 增量，一旦冲过 `jindu` 就永远无法突破。此处不要"改成不 clamp"。
唯一注意：`target <= 0`（未设定）时 `cultivation()` 不做上界，行为与本体一致，可接受。

### 漏洞 I（无）｜负面天赋不能刷点（已验证）

- `TianfuEconomy.java:106` `if (state.purchased().contains(id) || state.receipts().containsKey(id)) → CONFLICT`
  → **同一 id 不能买第二次**。
- `TianfuEconomy.java:175-178` 补点总量封顶 `NEGATIVE_CAP = 24`（`TianfuSelection.java:11`）。
  `v2_xinzhi_yousun`(−5) + `xiuxing_chizhi`(−2) = 7 ≤ 24，可同买，但总量受限。
- `TianfuEconomy.java:212` 撤销按原价 `points += receipt.paidCost()`；负条目 `paidCost = −5` → 扣回 5 点，
  `:216-219` 还要求 `grant` 不为负。**买↔撤销完全对称，净额为零，无刷点回路。**
- `TianfuApply.java:147-149` 已确认/已结算后禁止撤销，进一步封死。

---

## ③.1 `dazuo_wangwo` 专节（任务书要求）

**问题：是否真的只在打坐时生效？**

**已验证的代码事实：**
1. `TianfuTalents.java:249` 只声明 `f(Op.XIULIAN, 0.40)`，与 `kuxiu_buchuo`「苦修不辍」的 `+0.20`
   **机理完全同类**，没有任何专属判定字段/条件。
2. `TianfuNativeProgression.java:14-23` 的 `cultivation()` **没有** `hasEffect` / `isSneaking` /
   动画 / `PlayerVariables` 打坐标志的任何检查。
3. `TianfuApply.java:63-74` 的 `effective()` 只过滤"是否已确认"和追加出身前缀，**不看打坐状态**。
4. 全库 grep `dazuo`/`打坐`：除本条目声明与两处注释外，无任何行为代码。

**但结论不是"它能全天生效"，而是"它不需要判定就已经是打坐专用"：**
5. 本体修炼进度**只在 `XIULIANJINGJIE` 效果存在时**才推进（`XiulianjingjieMobEffect@11,16`，
   且脉冲过程 `@132` 自己再判一次 `hasEffect(XIULIANJINGJIE)`）；
   该效果**就是打坐**——脉冲过程 `@1324-1391` 播放的动画名是 `dazuo3`，
   `@1255-1321` 在 `zhenyuan >= 10` 时以 15 tick 自续。
6. 因此 `v2_mind_xiulian_1/2/3`、`kuxiu_buchuo`、`suiyue_chendian`、`suhui`，
   以及 `xiulian_rushen`/`shen_xiulian_wuji`/`ji_zhongdao_zhizun` 的**修炼那一半**，
   **全部同样"只在打坐时"生效**。

**判定：描述与实现"效果范围一致"，但与"效果机制"不一致。**
`dazuo_wangwo` 的「打坐时」措辞是**冗余的**——它不是"限定条件的特化条目"，而是"同一原语的第二个正价副本"
（地 3 点买 +40%，而 `v2_mind_xiulian_3` 天 5 点买 +60%，性价比反而更差，却能与它叠加）。
真正的风险面是 `Guroutuanyuangulvbuff…@235-360` 这条**非打坐、但同样要求 XIULIANJINGJIE** 的写入路径：
当前它**不经过**天赋乘区（漏洞 G），所以今天还不构成问题；一旦将来把它接进 `TianfuNativeProgression.cultivation`，
`dazuo_wangwo` 就会在"团圆蛊共振"这条路径上也吃到 +40%，而它描述写的是"打坐时"。

**两个选项的影响**

| 选项 | 做法 | 收益 | 代价/风险 |
|---|---|---|---|
| **修实现** | 在 `TianfuNativeProgression.cultivation()` 加打坐判定（如 `player.hasEffect(GuzhenrenModMobEffects.XIULIANJINGJIE)`），仅在打坐时把 `dazuo_wangwo` 的 0.40 计入 | 语义与描述严格一致；未来接入非打坐路径时不会误放大 | 需要反射/holder 访问本体效果（`GuzhenrenModMobEffects.XIULIANJINGJIE` 是 `DeferredHolder`，编译期依赖本体）；**今天判定恒为真 → 纯装饰，不改变任何数值**；仍不能解决与 #1-3 叠加的问题 |
| **隐藏入口**（推荐） | 不写判定，改**并入 `xiulian` 族**（`TianfuMindTable` family=`xiulian`, tier=2 或独立 tier），并把描述改成「修炼速度 +40%」，与其它修炼条目统一口径 | 一次性同时解决"同名不同源"（漏洞 C）与"机制说不清"（漏洞 F）；零新依赖 | 会与 `v2_mind_xiulian_3` 变成互斥 → 需要给这一族重新定价（地 3 点 +40% 与天 5 点 +60% 的档位要重排） |

> 推荐"隐藏入口"：因为**当前不存在"非打坐的修炼"这条数值通道**，加判定得不到任何可观测收益，
> 只是把一条本就冗余的描述写得更绕。真正的数字矛盾在叠加与互斥，不在打坐判定。

---

## ④ 与任务书候选的对照与建议

### 4.1 突破数值口径（任务书问题 6）

**先明确现有实现的口径**：**既不是"基础概率乘算修正"，也不是"最终概率百分点"，而是「失败概率乘算」**
——`P' = 1 − (1−P)(1−r)`（`Guzhenren98Progression.java:44-48`）。
它与两种候选口径的差距（`r=0.30`，见 §3 漏洞 A 表）：
- 对 **百分点**：`P ≥ 0.5` 时现实现更保守（0.65 vs 0.80），**`P ≤ 0` 时完全相反**（0.30 vs 0.00）；
- 对 **基础概率乘算**：`P ≥ 0.5` 时基本相同，**`P ≤ 0` 时完全相反**（0.30 vs 0.00）。

**候选值评价（厚积薄发 +20% / 突破通天 +30%）**

| 维度 | 评价 |
|---|---|
| 数值本身 | **就是现状**（`TianfuNativeProgression:38` = 0.20、`TianfuTalents:248` = 0.30）。照抄候选 = 不改代码。 |
| 口径 | **不可照搬**。因为本体 P 在四转常态为 −0.74，现口径把每条都变成"0% → r"的绝对保底。任务书若只想"提高突破几率"，必须在文档里写清是**相对失败率**还是**绝对百分点**，否则 +20/+30 会被理解成百分点。 |
| 叠加 | **必须加封顶**。`Σr ≥ 1` 即 100% 必成，10~19 点即可达成。 |
| 建议 | ① 把 `breakthroughChance` 改为**只在 `P > 0` 时生效**：`P' = P + r·P`（即基础概率乘算，`P ≤ 0` 保持 0），或 `P' = P + r·max(0, P)`；② 在 `TianfuTalents.breakthrough()` 之后加 `Σr = min(Σr, 0.80)` 硬封顶（`P ≤ 0` 时仍有 20% 空间给其它系统）；③ 修 `Guzhenren98Progression.java:39` 的 `0.001 → 0.00075`。 |
| 若坚持保留"相对失败率"口径 | 那么候选值必须下调并**明确写"相对"**：`厚积薄发 +10%`、`突破通天 +15%`，且 `Σr` 封顶 0.60；同时在天赋详情页显示"当前实际成功率"（需要 hook `TupochenggonglvProcedure`，否则玩家看到的是未加成的数）。 |

### 4.2 修炼候选表（任务书问题 7）

候选：岁月沉淀 +10% / 修炼 I +20% / II +40% / III +60% / 修炼如神 +80% / 神·修炼无极 +150%。

**本体真实计算方式下的可行性**：
- 加成本质是"同一份 10 真元买到的进度更多"：`Δ' = Δ·(1+bonus)`，**线性**，不会指数爆炸；
  再叠加 `clamp(0, jindu)` 与精确相等门（漏洞 H），不会把玩家卡死。**这一点是安全的。**
- 但 `sum()` 纯加法 → 6 条候选 + 3 条现有（`dazuo_wangwo`/`kuxiu_buchuo`/`suhui`）+ 出身，
  实测合计 **+5.80 → ×6.80 速率**（`v2_mind_xiulian_3` 取 +60%）。候选表本身只把 `修炼如神` 从 60% 提到 80%，
  合计变 `+6.00 → ×7.00`。**问题不在单条数值，在没有任何互斥/封顶。**

| 条目 | 候选 | 建议 | 理由 |
|---|---:|---|---|
| 岁月沉淀 | +10% | **需调整** | 保持 +10% 但**必须**改成真挂本体寿元项（`mingge==4 && shouyuan<100` 的分支 B），或改为"每已耗 100 寿元 +3%（封顶 +15%）"。当前恒 +10% 与描述不符，且与 `kuxiu_buchuo` 重复。 |
| 修炼 I / II / III | +20/+40/+60 | **可照搬** | 与现有值完全相同，且已在 `xiulian` 族内互斥，口径最干净。 |
| 修炼如神 | +80% | **需调整（拆条）** | 现 +60% 且**同时给 `BREAKTHROUGH +0.40`**。任务书禁止双收益 → 必须拆成两条，或删掉突破那一半。若只保留修炼一半并提到 +80%，则天 5 点换 +80% 会**直接压过** 天 5 的 `v2_mind_xiulian_3`(+60%) 且同价 → 二选一里 III 变废条；建议 `修炼如神` 只加修炼、定价提到 6~7 点，或把 III 提到 +70%。 |
| 神·修炼无极 | +150% | **可照搬数值，但需删突破半边** | 现 +150% + `BREAKTHROUGH +0.60`。删掉突破半边后 12 点买 +150% 合理（相对 III 的 5 点 +60%，边际收益递减：+90% / 7 点 ≈ 12.9%/点 vs III 的 12%/点，勉强平）。 |

**统一建议（推荐组合）**
1. 把 `xiulian` 族扩到 **6 档**：`v2_mind_xiulian_1..3` + `dazuo_wangwo`(tier4) + `xiulian_rushen`(tier5) + `shen_xiulian_wuji`(tier6)，
   全部互斥，价格 2/3/5/6/8/12，数值 20/40/60/80/110/150（**边际递减**）。
2. `kuxiu_buchuo` / `suiyue_chendian` / `suhui` 三条低档旧条目**并入同一族**（tier 1 附近）或直接下架，
   不要再作为可叠加的独立 `+%`。
3. 在 `TianfuTalents.xiulian()` 出口加 **`Math.min(Σ, 2.00)` 硬封顶**（×3.0 速率），
   作为任何未来新增条目的最后一道闸。
4. 突破侧同样在 `TianfuTalents.breakthrough()` 出口加封顶（建议 0.60~0.80），并把 `ji_zhongdao_zhizun` 的
   `BREAKTHROUGH` 与 `XIULIAN` 两半**移出该条**（它已经在给 +3000×44 道痕，不该再拿修炼/突破）。

---

## ⑤ 需修改的具体类与方法清单

按优先级排列。**本阶段未改动任何文件，以下为 1.4.18 的实施清单。**

| 优先级 | 文件 | 方法 / 位置 | 改动 |
|---|---|---|---|
| P0 | `tianfu/.../compat/Guzhenren98Progression.java` | `nativeBreakthroughChance` `:37-41` | `destinyFive ? 0.001 : 0.0005` → **`0.00075 : 0.0005`**（本体实测 `7.5E-4`） |
| P0 | 同上 | `breakthroughChance` `:44-48` | 改为"只在 `P > 0` 时生效"：`P' = clamp(P + reduction*max(0,P))`；并明确注释口径为"基础概率乘算"，避免"把 0% 变 r" |
| P0 | `tianfu/.../TianfuTalents.java` | `breakthrough` `:781-783` | 出口加封顶：`Math.min(sum(ids, Op.BREAKTHROUGH), 0.80)` |
| P0 | `tianfu/.../TianfuTalents.java` | `xiulian` `:776-778` | 出口加封顶：`Math.min(sum(ids, Op.XIULIAN), 2.00)` |
| P0 | `tianfu/.../v2/TianfuMindTable.java` | `LEVELS` `:11-23` | 扩族：把 `dazuo_wangwo`、`kuxiu_buchuo`、`suiyue_chendian`、`suhui`、`xiulian_rushen`、`shen_xiulian_wuji` 并入 `xiulian` 族（按 tier 排序）；另建 `tupo` 族收 `houji_bofa`、`tupo_tongtian`（+`ji_zhongdao_zhizun` 若保留突破半边） |
| P1 | `tianfu/.../TianfuTalents.java` | `BASE` 表 `:156`、`:248-249`、`:288-289`、`:315-316`、`:329-330`、`:343-344` | 拆双收益：从 `xiulian_rushen`/`shen_xiulian_wuji`/`ji_zhongdao_zhizun` 移除 `Op.BREAKTHROUGH`；`v2_xinzhi_yousun`/`xiuxing_chizhi` 二选一保留（建议留修炼，突破的负面交给独立条目） |
| P1 | `tianfu/.../TianfuNativeProgression.java` | `cultivation` `:14-23` | ① `suiyue_chendian` 的 `+0.10` 改为真·寿元函数（读 `variables.shouyuan` / 或直接复用本体分支 B），② `suhui` 的 `0.05*zhuanshu` 加 `Math.min(..., 0.30)`，③（若选"修实现"）`dazuo_wangwo` 加 `hasEffect(XIULIANJINGJIE)` 判定 |
| P1 | `tianfu/.../TianfuNativeProgression.java` | `breakthroughRoll` `:35-44` | `houji_bofa` 的硬编码 `+0.20` 应移入 `TianfuTalents` 词条表（与其它突破项同通道、同封顶），不再绕开 `sum()` |
| P1 | `tianfu/.../TianfuTalents.java` | 描述文本 `:249`、`:288`、`:315`、`:329`、`:343`、`:156` | 统一"突破"措辞；`dazuo_wangwo` 描述按选项改（并入族则改「修炼速度 +40%」） |
| P1 | `tianfu/.../TianfuPool.java` | `:67`、`:76` | `suhui` 删"与道痕获取"（未实现）；`suiyue_chendian` 描述与新实现对齐 |
| P2 | `tianfu/.../mixin/CultivationButtonMixin.java` | `:21-25` | 保留（4 处命中已核对）；新增：若要让 GUI 显示真实成功率，另加对 `TupochenggonglvProcedure.execute` 的 `@Redirect`/`@ModifyReturnValue`，把 P 换成加成后的 P |
| P2 | `tianfu/.../mixin/CultivationProgressMixin.java` | `:16-22` | 保留（3 处 `PUTFIELD` 已核对）。**不要**扩到 `KE1/KE3/JingJieBaoCun`（那些是境界持久化/初始化写入，乘区会污染存档值）；如需覆盖团圆蛊路径，另开一个仅对 `Guroutuanyuangulvbuff…@360` 的 mixin |
| P2 | `tianfu/.../TianfuEffects.java` | `wenyangStep` `:543-563` | 该方法是 1.4.10 的旧口径，现已由 `Guzhenren98Progression.cultivation` 的 `align`+`clamp` 取代；如确认无调用方则删除，避免两套口径并存 |
| P3 | `tianfu/.../v2/TianfuOriginTable.java` | `scholar` `:117-120` | 出身 `书痴` 同时给修炼与突破（双收益），建议只留修炼，或把突破改成"突破失败率 −10%"并统一措辞 |

---

## ⑥ 未验证项

以下**没有**代码/字节码证据，必须进游戏或补工具链才能确认，**本报告不作结论**：

1. **谁首次授予 `XIULIANJINGJIE`。** 已确认全 jar 只有 4 个类引用该效果字段，**无一处 `addEffect`**；
   最可能的授予方是本体外部的命令/数据包/其它附属（`g98Test` 探针 `G98ServerProbe.java:182` 就是这么给的）。
   影响：若真实环境中该效果的授予节奏与"打坐"不完全等价（例如带冷却断续、或被其它模组延长），
   则所有"修炼速度"天赋（含 `dazuo_wangwo`）的**实际覆盖率**与本报告的判断不同。
2. **`gushi_xiulian_jindu`（进度目标）由谁、按什么规则设定。** 已知 `UIxiuliananjianProcedure@2587` 在突破成功后把它置 0，
   但设定点在 `KE1Procedure`/`JingJieBaoCunProcedure` 的 18 处写入里，未逐一核对哪一处是"当前境界目标"。
   影响：`clamp(0, jindu)` 与精确相等门的实际手感。
3. **`renqi`（人气）的实际取值范围。** 漏洞 D 的误差幅度正比于 `renqi`；
   本报告用 0/100/500/1000/2000 做了量级演示，但**真实服务器上人气能到多少未验证**
   → 无法判断"天命五加成被吞"是常见还是理论。
4. **`zhuanshu` 是否可能被抬高到 >5。** 本报告只验证了 `UIxiuliananjianProcedure@423-451` 这一个门；
   其它模组/命令/`JingJieBaoCunProcedure` 是否能写 6 转以上未验证。这决定 `suhui` 的实际上限。
5. **`Math.random()` 是否被其它 mixin 一并 Redirect。** 本报告只核对了本模组的一个 Redirect；
   若服主另装同类模组，两个 Redirect 叠加后的平移语义需实测。
6. **`15.0`（凡体分支 A）与 `2.5`（默认分支 C）的差异是否是有意设计。**
   字节码确认分支条件为 `tizhi == 2 && zhongzu != 1`，但"凡体修炼快 6 倍"是否符合本体作者意图，
   属玩法判断，本报告不下结论。**但这会显著放大/缩小所有 `+%` 的感觉**（同一套 +150% 在凡体上是每 tick +37.5，在十绝体上是 +6.25）。
7. **`YICUERJIU` / `SI_WEI_JIU_CHONG_1` / `QI_XIANG_JIU_CHONG_1` / `JIU_CHONG_JIN_LIAN_ZHEN_YUAN` /
   `FEN_SHOU_JI_XIU_JUE_6` 的 amplifier 上限。** 公式里这 5 项是加法/加法级乘子，
   若 amplifier 能到 10+ 会与天赋乘区同量级；未查这些效果的等级上限。
8. **`GameTest` / 实机**：本阶段按要求未跑 Gradle，所有结论均为静态证据（javap 字节码 + 源码）。

---

## 附录：证据复现命令

```powershell
$jar='D:\daima\mcmod\蛊真人附属\_crash3\basever\guzhenren-10.20.jar'
$javap='C:\Program Files\Java\jdk-17\bin\javap.exe'
& $javap -p -c -classpath $jar 'net.guzhenren.procedures.UIxiuliananjianProcedure'              | Out-File a.txt -Encoding utf8
& $javap -p -c -classpath $jar 'net.guzhenren.procedures.XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure' | Out-File b.txt -Encoding utf8
& $javap -p -c -classpath $jar 'net.guzhenren.procedures.TupochenggonglvProcedure'              | Out-File c.txt -Encoding utf8
& $javap -p -c -classpath $jar 'net.guzhenren.procedures.UitupoxianshiProcedure'                | Out-File d.txt -Encoding utf8
& $javap -p -c -classpath $jar 'net.guzhenren.potion.XiulianjingjieMobEffect'                   | Out-File e.txt -Encoding utf8
& $javap -p -c -classpath $jar 'net.guzhenren.procedures.GuroutuanyuangulvbuffXiaoGuoChiXuShiMeiKeFaShengProcedure' | Out-File f.txt -Encoding utf8
```

**关键 bci 速查**

| 内容 | 位置 |
|---|---|
| 打坐门槛（效果/时长/冷却/已满/扣真元） | `Xiulianjingjie…@132,176,211,242,272` |
| 修炼增量 3 处写入 | `Xiulianjingjie…@618 / @964 / @1245` |
| 打坐态自续 + `dazuo3` 动画 | `Xiulianjingjie…@1255-1321 / @1324-1391` |
| 突破精确相等门 | `UIxiuliananjianProcedure@389-390`；`UitupoxianshiProcedure@32-33` |
| 突破概率 4 处 | `UIxiuliananjianProcedure@1058 / @2139 / @3827 / @4908` |
| renqi 系数 7.5e-4 | `@1095`、`@3864`、`TupochenggonglvProcedure@37` |
| renqi 系数 5.0e-4 | `@2176`、`@4945`、`TupochenggonglvProcedure@61` |
| 突破成功后的 zhuanshu/jieduan 重置 | `UIxiuliananjianProcedure@2512 / @2581 / @2587` |
| 天赋乘区注入点 | `CultivationProgressMixin.java:16-22`、`CultivationButtonMixin.java:21-25` |
| 突破/修炼概率复刻 | `Guzhenren98Progression.java:37-54` |
| 聚合计（纯加法，无封顶） | `TianfuTalents.java:942-956` |
| 互斥表（5 族 12 条） | `TianfuMindTable.java:11-23` |
| 购买去重 / 补点封顶 | `TianfuEconomy.java:106 / :175-178` |

---

*报告完毕。全部结论标注了「已验证（字节码/源码）」或「未验证（需实机）」；未验证项集中在 §⑥，共 8 条。*
