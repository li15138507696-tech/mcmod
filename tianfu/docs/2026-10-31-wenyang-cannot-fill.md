# 温养空窍无法温满、无法突破 — 根因与修复（1.4.10）

> 2026-10-31

## 现象

玩家反馈：**天赋温养空窍的时候无法温满，导致无法突破。**

## 根因（反编译本体确认）

本体「温养空窍」用两个字段：`gushi_xiulian_dangqian`（当前）与 `gushi_xiulian_jindu`（目标，一转 = 400）。

### 1. 本体累加**不 clamp**

`XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure`（打坐效果每刻发生）：

```
374: getfield gushi_xiulian_dangqian
377: ldc2_w 15.0d            // 该分支每 tick +15
391: getfield zishenshijianjiasu ; dmul
617: dadd
618: putfield gushi_xiulian_dangqian      ← 直接写回，没有 min/clamp
```

### 2. 本体判定突破用**精确相等**

`UIxiuliananjianProcedure`：

```
372: getfield gushi_xiulian_dangqian
375: getfield gushi_xiulian_jindu
389: dcmpl
390: ifne 5965                 ← 只要不相等就跳过突破分支
```

打坐过程里「已满」的判定也是同一套（offset 225-243：`dcmpl` + `ifeq 1394`）。

**所以：只要进度冲过目标（400 → 415），就永远「差一点」，永远无法突破。**

### 3. tianfu 把它变成了「永远差一点」

`TianfuEffects` 1.4.9 及更早：

```java
if (xiulian != 0.0) {
    v.gushi_xiulian_dangqian = clamp(v.gushi_xiulian_dangqian + xiulian, 0.0, v.gushi_xiulian_jindu);
}
```

`xiulian` 可能为**负**——数据表里有两条负向修炼天赋：

| 天赋 | 效果 |
|---|---|
| `v2_xinzhi_yousun` 心智有损 | `Op.XIULIAN -0.40`、`Op.BREAKTHROUGH -0.50` |
| （另一条负向） | `Op.XIULIAN -0.30`、`Op.BREAKTHROUGH -0.40` |

`XIULIAN_TICK = 0.05`，所以 −0.40 → 每 tick **−0.02**。

于是循环变成：

```
tick N   : 本体加过头 → 414.98；tianfu clamp → 400（刚好满）
tick N+1 : tianfu 先把 400 打成 399.98  → 不再是「满」
           本体看到 399.98 != 400 → 继续加 → 414.98
           tianfu clamp → 400
           tianfu 再减 → 399.98   ← 玩家点突破时读到的就是这个
```

**玩家按突破的那一刻，`dangqian` 永远停在 `jindu - 0.02`，`dcmpl` 判不等，突破失败。**
进度条也就永远停在 99.995%，看起来「温养不满」。

## 修复（1.4.10）

把推进逻辑抽成 `TianfuEffects.wenyangStep(dangqian, jindu, xiulian)`，规则三条：

1. **溢出对齐**：`dangqian > jindu` → 直接设为 `jindu`（让已经被卡住的存档恢复可突破）
2. **正向**：`clamp(dangqian + xiulian, 0, jindu)`（不变）
3. **负向只压未满的进度**：`dangqian < jindu` 时才减，且下限 0；**已满时绝不往回拉**

`jindu <= 0`（目标未设定）时一律不动。

```java
static double wenyangStep(double dangqian, double jindu, double xiulian) {
    if (jindu <= 0.0) return dangqian;                 // 没有目标，不碰
    if (dangqian > jindu) return jindu;                // 溢出对齐
    if (xiulian > 0.0) return clamp(dangqian + xiulian, 0.0, jindu);
    if (xiulian < 0.0 && dangqian < jindu) return Math.max(0.0, dangqian + xiulian);
    return dangqian;                                    // 已满 / 无加速：保持
}
```

## 验证

### 单元测试 21 条全过（直接调发布的实现）

`_crash6/harness/src/net/guzhenren/tianfu/WenyangHarness.java`：

| 组 | 内容 | 条数 |
|---|---|---|
| A | 溢出必须对齐到 jindu（400.5 / 414.98 / 100000 → 400） | 3 |
| B | 负向天赋绝不能把「已满」拉回来 | 3 |
| C | 负向只压未满的进度（含夹到 0） | 4 |
| D | 正向加速夹到 jindu | 4 |
| E | `jindu = 0` 时一律不动 | 3 |
| F | 无加速时只做溢出对齐 | 3 |
| G | **模拟连续 30 tick：负向天赋下最终停在满** | 1 |

结果：**通过 21 / 失败 0**。

### 专用服务端加载

```
[14:28:16] Done (1.937s)!
tianfu 1.4.10 加载: True
总 ERROR 13 条，其中 tianfu 相关: 0
```

（13 条全是本体自身数据问题：`guzhenren:deleted_mod_element`、`guzhenren:fangju` 标签、`guzhenren:nilvhuafenjie` 配方。）

## 对已被卡住的存档

**不用删档**。1.4.10 装上后，玩家下次打坐/登录时 `dangqian > jindu` 会被自动对齐回 `jindu`，突破立刻恢复可用。

## 仍未验证的部分

**我没有在游戏里真正打坐一次。** 上面的结论来自：
- 本体字节码（累加不 clamp、突破判等）——实证
- tianfu 源码的算术路径——实证
- 21 条单元测试直接调发布的 `wenyangStep`——实证

但「装 1.4.10 后进游戏打坐到 100% 并成功突破」这一步需要真人操作确认。
如果你那边还卡着，请告诉我：
- 温养进度条显示的具体数值（例如 99.9% / 399.98 / 400）
- 是否买了「心智有损」这类负向修炼天赋
- `/tianfu open` 里 `已购` 列表

## 交付

```
tianfu-1.4.10.jar  313579 bytes
sha256 1020149E57505CFD7901CEED9A54B516D5D5F175FD8C21C2D1CA7F54A6B932DB
已装入整合包 + 测试服（1.4.9 已删）
```
