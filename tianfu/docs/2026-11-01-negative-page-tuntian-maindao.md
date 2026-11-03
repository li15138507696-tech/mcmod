# tianfu 1.4.15 — 负面独立成页 / 删除吞天食地 / 杀神按主修流派得道痕

> 2026-11-01

三项改动，均已编译、单测、9.8 专用服务端验证。

---

## 一、负面条目独立成页

### 做了什么

| 项 | 内容 |
|---|---|
| 新增页 | `TianfuPage.NEGATIVE("negative", "负面")`，排在「体质」之后 |
| 新增向导步 | `TianfuWizardStep.NEGATIVE`，同样排在「体质」之后 |
| 页归属 | 20 条负面从 `PHYSIQUE` 改为 `NEGATIVE` |
| 页内分组 | 按扣点档位：**重负 −8 / 中度 −5 / 轻负 −2**，并据此排序 |
| 体质页 | 分类列表去掉「负面」（正面 / 十绝体） |
| 可跳过 | `canSkip` 里加入 `NEGATIVE`——一条负面都不买也能进下一步 |

### 一个必须一起改的地方（否则加了页也点不到）

`TianfuViewScreen` 的当前页是 **由向导步骤驱动的**：

```java
if(wizardStep.page()!=null){page=wizardStep.page();buildRows();}
```

左侧导航渲染的也是 `TianfuWizardStep.values()`，**不是** `TianfuPage.values()`。
所以只加 `TianfuPage` 的话，新页没有任何入口、永远进不去。
这次同时加了 `TianfuWizardStep.NEGATIVE` 并把它加进 `isTalentSelectionStep()`，
新页才会出现在导航里、并能正常抽选/接取。

（顺带：`TianfuWizardStep.canSkip` 的 switch 是全枚举的，加页后编译器直接报错强制处理——
这正是我们要的。）

### 只改展示，不动规则

价格、互斥、结算仍走 `TianfuPool` / `TianfuEconomy`，本次没碰。

---

## 二、删除「神·吞天噬地」

共 8 处引用，全部清掉：

| 文件 | 原内容 |
|---|---|
| `TianfuPool` | 条目定义（12 点，神级） |
| `TianfuPageTable` | 页归属 |
| `TianfuLegacyPrices` | 旧价目 12 |
| `TianfuCombat` ×2 | 吸血 +12%、反伤 25%/15% |
| `TianfuEffects` ×2 | 气运上限 +50 / 回滚 −50 |
| `TianfuSlay` | `tuntian = slay \|\| ids.contains(...)` → 改为 `tuntian = slay` |

**杀神不受影响**：它本来就靠 `slay ||` 走同一条噬道通道，删掉吞天食地只是去掉了非杀神的那条入口。

### 老存档（按你的选择：只移除，不退还）

`TianfuApply.sanitize` 只保留 `TianfuPool.byId != null || TianfuTalents.byId != null` 的 id，
吞天食地现在两边都查不到 → **被静默丢弃**，不崩、不生效、不退 12 点。

---

## 三、杀神按主修流派获得道痕

### 以前

`TianfuSlay.addDevourDaohen` 把每次击杀的道痕**固定均分给 7 路**
（气道 / 周道 / 金道 / 律道 / 土道 / 血道 / 浴道）——不管玩家练什么。

### 现在

新增 `TianfuMainDao`，按优先级判定主修：

1. **道途的选择**：读 `PersistentData/daotu/mainDao`（道途让玩家手动指定，最准）；
2. 否则退回 **`liupai_*` 熟练度最高的那一路**（不依赖任何其他 mod）；
3. 都没有 → 空串，按「未设定」处理。

只收**本体两侧字段都真实存在**的 15 个流派（`daohen_*` 与 `liupai_*` 都要有），
与道途的清单一致：

```
jiandao jindao shuidao xuedao lidao mudao yudao fengdao
bingdao jindao2 hundao leidao yandao lvdao tudao
```

`TianfuDaohen.addDaohen(v, key, amount)` 把道痕写进对应字段（另含
`qidao` / `zhidao` / `zhoudao` 三个本体字段名少一个 `o` 的历史坑）。

**主修未设定时退回原来的 7 路均分**——避免新号一点流派都没点就一点道痕都不给。

---

## 四、验证

| 项 | 结果 |
|---|---|
| `javac_tianfu.py` | `sources: 81` / `javac exit: 0` |
| `gradlew build` | `tianfu-1.4.15-mc98.jar` 340 709 bytes |
| **新增** `MainDaoHarness` | **54 / 54** |
| 旧 `WenyangHarness` | 21 / 21（无回归） |
| 旧 `HealHarness` | 50 / 50（无回归） |
| 旧 `ToleranceHarness` | 14 / 14（无回归） |
| 合计 | **139 条断言全过** |
| 9.8 专用服务端 | `Done (1.852s)!`，1.4.15 加载，mixin / tianfu 异常 **0** |

### 新测试覆盖了什么

- **A**：15 个主修键的 `liupai_*` 读写一一对应（15 条）
- **B**：`highest()` 对每个键都能选出最高者；全 0 返回空串（16 条）
- **C**：15 个键的 `addDaohen` 都写进对应 `daohen_*` 字段（15 条）
- **D**：未知键（如 `huodao`）返回 false 且不写（2 条）
- **E**：**所有 cost<0 的条目都在 NEGATIVE 页**，共 20 条（2 条）
- **F**：NEGATIVE 页上不许有正面条目（1 条）
- **G**：吞天食地在 Pool / 页表 / 旧价目三处都查不到（3 条）

E/F 是**不变量测试**：以后再加负面条目，忘了登记页归属会直接红。

---

## 五、顺带确认的两件事

1. **`tianfu-1.4.15-mc98.jar` 在 9.8 上可用**。我一度担心 `CultivationProgressMixin` /
   `CultivationButtonMixin` 用五参描述符在 9.8 上匹配不到，实测核对后确认
   `Xiulianjingjie…` 与 `UIxiuliananjian` 的 `execute` 在 **9.8 与 10.20 都是五参**，
   签名完全一致——担心是多余的。

2. **`build.gradle:125` 写死 `archiveClassifier = 'mc98'`**，所以现在只出 9.8 变体。
   如果以后还要出 10.20 版，得把这里改成可切换（恶意那边是 `pack_jar.py --variant`）。

---

## 六、交付

```
tianfu-1.4.15-mc98.jar  340709 bytes
已装入 D:\game\mcmod\1\.minecraft\versions\转生到蛊界就要拿出真本事\mods
（1.4.14-mc98 已删）
```

另外确认：你包里的 `daotu-1.8.2.jar` **保留了**我上一轮加的 `DaotuCoreVersion`（版本识别）。
