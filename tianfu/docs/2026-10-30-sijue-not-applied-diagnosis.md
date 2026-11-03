# tianfu 1.4.5 — 「买了十绝体却还是凡人」排查结论 + 三处修复

> 2026-10-30

## 一、结论：那个存档里**没有十绝体**

读的是 `1\.minecraft\versions\转生到蛊界就要拿出真本事\saves\新的世界`
（最后保存 10-30 13:02，就是当前在玩的那个）：

```
neoforge:attachments/guzhenren:player_variables
    tizhi          = 0.0        ← 凡人（0 = 无体质）
    kongqiao       = 5.0
    zuida_zhenyuan = 516.0

NeoForgeData/tianfu
    purchased = ['v2_apt_bing', 'baoji_rushen']   ← 没有 sijue_*
    points    = 0
    confirmed = 1
    settled   = 1
```

买的是**丙等空窍**（3 点）+ **暴击如神**（5 点）= 8 点，正好等于「顺天应命」的总点数。

## 二、为什么买不了十绝体

1. **点数不够**：十绝体 16 点，顺天应命只发 8 点，且 8 点已花光。
2. **互斥**：空窍档位（丁/丙/乙/甲）与十绝体只能选一个，界面会显示
   「已选择空窍档位，不能再选择十绝体」。
3. **最关键**：`v2_apt_bing` 是「丙等**空窍**」，效果是 `Op.APTITUDE_SET 6`，
   只写 `kongqiao` 与真元上限，**从不写 `tizhi`**。所以买它之后体质仍然是凡人——
   这是设计，不是故障。

> 十绝体本身没坏：另一个存档（`蛊2\...\新的世界`，10-26）买了 `sijue_3`，
> 字段完整落地：`tizhi=3`、`kongqiao=3`、`zuida_zhenyuan=1000`、
> `zuida_hunpo=500`、`hunpochengzaijixian=50000`、`daohen_bingxuedao=2030`（2000+30 基线）。
> 也就是说「十绝体写不进去」这条不成立。

## 三、本轮修的三处（真问题）

### 1. 服务端漏了「体质唯一」校验（1.4.5）

`TianfuEconomy.buy` 原来按 `other.kind() == spec.kind()` 去重，等于把凡体和十绝体
当成两个槽位：**各买一个都能过**，结算时按 `purchased` 顺序互相覆盖 `tizhi`，
后写的赢 → 「买了十绝体却显示凡体」。

客户端 `TianfuUiRules.purchase` 早就按「凡体/十绝体合计一个」拦住了，是**服务端不一致**。
现在服务端对齐：新增 `isPhysique(kind)`，凡体与十绝体共用同一个体质槽位；
`TianfuApply.settleSelection` 的门禁也从 `bodies > 1 || sijues > 1`
改成 `bodies + sijues > 1`。

### 2. 购买失败**完全没有提示**（1.4.5）

`TianfuBuyPayload.handle` 原来只调 `TianfuApply.buy(...)` 并丢掉 boolean 返回值：
点数不足、互斥、已锁定三种情况在客户端表现**完全一样——点了没反应**。
这正是「我以为选上了十绝体」的来源。

现在：
- `TianfuApply` 新增 `buyReason(ServerPlayer, String)` 返回具体 `Rejection`；
  `buy(...)` 保留原签名（内部委托），零破坏。
- `TianfuBuyPayload` 失败时把原因回显到动作栏，并**带点数差额**：
  「点数不足：北冥冰魄体 需要 16 点，你现在只有 0 点」。

### 3. `/tianfu open` 补诊断项（1.4.5）

原来只打「开窍标记 / 空窍 / 已结算 / 难度 / 点数」，**不显示 `tizhi` 和已购列表**，
所以「买了十绝体却还是凡人」这类反馈无法从游戏内自证。

现在多两项：

```
§6[天赋] §7诊断 … §7体质tizhi §f0§8(凡人) …
§6[天赋] §7已购 §8(2)§f v2_apt_bing, baoji_rushen
```

`tizhi` 会标注含义：0 凡人 / 1–10 十绝体 / 17–53 凡体 / 54 杀神。

## 四、验证

```
javac exit: 0 / sources: 70（只有 6 条 EventBusSubscriber bus() 废弃警告）
gradlew.bat build  BUILD SUCCESSFUL in 20s
jar: tianfu-1.4.5.jar  313311 bytes  173 条目  107 class
sha256 73B0906D973DA32070C8842B2A4E28CDF8108191C52F3143B2B343F1A3B82DAA
已装入整合包（旧 1.4.4 已删）
```

## 五、顺带发现，待确认（本轮未改）

买了「丙等空窍」（描述写 `kongqiao=6`），实际存档里是 `kongqiao=5`。

原因：`setAptitude` 确实写了 6，但随后 `applyAptitudeTier` 只写
`zuida_zhenyuan = roll(400,590)`（本次 516），而**本体 KE1 会从真元上限反推档位**，
于是 6 被反推成 5。十绝体不受影响（`zuida_zhenyuan=1000` 反推正好是 3）。

要修的话有两条路：把四档的真元区间调到反推结果等于目标档位，或改描述写实际档位。
需要先确认本体反推规则，本轮没动。
