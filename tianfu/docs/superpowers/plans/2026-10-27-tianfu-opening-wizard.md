# 天赋开局选择向导 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` for inline, checkpointed execution. Complete only the user-approved task, report its concrete result, and wait for approval before starting the next task.

**Goal:** 将开窍后的天赋配置改成可前进、返回、跳过可选步骤的向导，同时保留现有玩法交易与最终结算规则。

**Architecture:** 新增仅供客户端使用的向导阶段枚举与会话游标。`TianfuViewScreen` 继续复用现有分类列表、搜索、购买/撤销确认、随机候选与 V2 快照；改为按阶段呈现。服务端 payload、经济、NBT 和结算入口不变。

**Tech Stack:** Java 21；Minecraft 1.21.1；NeoForge 21.1.x；项目自带 `_crash3` 直接 `javac` 与 ZIP 打包脚本。

## Global Constraints

- 保留 1.3.7 当前难度、价格、点数、出身、随机候选、硬代价、互斥和最终结算规则。
- 难度仍为必选且本世不可重选；总复核必经；出身与流派、体质、心智、气运可以跳过。
- 资质根基仅在本体资质不满足且未选合规十绝体/机缘时阻塞跳过与最终确认；服务端仍负责最终校验。
- 阶段游标只保存在客户端会话内，不新增 payload、玩家 NBT 或服务端玩法数据。
- 不自动接受随机候选或自动最终确认；未接受候选仍不计入结算。
- 不运行 Gradle；交付版本必须同步递增。由 1.3.7 升至 1.3.8，并使用 `_crash3` 脚本构建。
- 本计划不新增或运行测试套件；最终只执行项目要求的编译、打包和静态产物校验。游戏内表现仍需实际启动游戏才能确认。

---

## 文件结构与职责

- Create: `src/main/java/net/guzhenren/tianfu/client/TianfuWizardStep.java` — 向导步骤顺序、映射到现有 `TianfuPage`、可跳过条件。
- Create: `src/main/java/net/guzhenren/tianfu/client/TianfuWizardSession.java` — 当前客户端连接会话中的步骤游标；断线/换玩家时可清空。
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuViewScreen.java` — 阶段显示、阶段门禁、返回/继续/跳过、复核页和随机候选入口。
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuClientHandler.java` — 首次开屏时使用会话游标或按服务端快照定位入口，状态刷新时保留当前步骤。
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuClientEvents.java` — 登录、登出、玩家替换时清空步骤游标。
- Modify via script: `version.properties`、`META-INF/neoforge.mods.toml`、`docs/天赋系统介绍文档.md`、`docs/宣传文档.md` — 同步到 1.3.8。
- Modify: `docs/宣传文档.md` — 在版本记录顶部增加向导改版说明。

现有 `TianfuPage`、`TianfuPageTable` 与服务端网络/交易类保持原职责，不把 `REVIEW` 混入天赋分类枚举。

---

### Task 1: 定义客户端向导阶段和会话游标

**Files:**
- Create: `src/main/java/net/guzhenren/tianfu/client/TianfuWizardStep.java`
- Create: `src/main/java/net/guzhenren/tianfu/client/TianfuWizardSession.java`

**Interfaces:**
- `TianfuWizardStep.page()` 返回分类阶段对应的 `TianfuPage`；`REVIEW` 返回 `null`。
- `TianfuWizardStep.next()` / `previous()` 返回固定序列中的相邻步骤并在两端停留。
- `TianfuWizardStep.canSkip(int kongqiao, boolean anchorSatisfied)`：`ORIGIN`、`LIUPAI`、`PHYSIQUE`、`MIND`、`QIYUN` 为真；`APTITUDE` 仅当本体资质为 1–10 或已有根基时为真；`DIFFICULTY`、`REVIEW` 为假。
- `TianfuWizardStep.initial(boolean difficultyChosen, boolean anchorSatisfied)`：依次返回 `DIFFICULTY`、`APTITUDE` 或 `REVIEW`。
- `TianfuWizardSession.current()`、`remember(TianfuWizardStep)`、`clear()` 管理一个 nullable 客户端会话游标，不接触网络或玩家存档。

- [x] **Step 1: Create the ordered stage enum.** 按 `DIFFICULTY → ORIGIN → APTITUDE → LIUPAI → PHYSIQUE → MIND → QIYUN → REVIEW` 声明阶段，并实现上面的映射、相邻步骤、跳过与首次定位接口。
- [x] **Step 2: Create the in-memory session cursor.** 提供线程无关的客户端主线程静态游标；`remember(null)` 等价于清空，避免写入无效阶段。
- [x] **Step 3: Review stage rules against the approved design.** 确认没有将阶段状态写入 NBT 或服务端协议，也没有修改 `TianfuPage` 与游戏规则。

**Checkpoint:** 只提交阶段模型与游标文件，向用户说明接口和跳过条件，等待批准后再接入界面。

---

### Task 2: 接入打开、续步与重登定位

**Files:**
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuViewScreen.java`
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuClientHandler.java`
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuClientEvents.java`

**Interfaces:**
- Screen 持有 `TianfuWizardStep wizardStep`；阶段变化时调用 `TianfuWizardSession.remember(wizardStep)`。
- Screen 用现有 `hasAnchorSelection()` 和同步快照判定根基，不复制或改写结算规则。
- Screen 首次定位使用 `TianfuWizardStep.initial(...)`；当会话游标存在且当前状态仍允许开局选择时沿用该游标。

- [x] **Step 1: Add stage initialization without changing navigation.** Screen 在构造/首次载入 V2 快照后，根据是否已选难度及当前根基初始化阶段；已确认状态强制进入 `REVIEW`。
- [x] **Step 2: Preserve the step during state refresh.** `TianfuClientHandler.open` 刷新已打开 Screen 时只更新服务端状态；不要重置当前阶段、搜索、分类滚动位置。
- [x] **Step 3: Reset only at player lifecycle boundaries.** `TianfuClientEvents.clearSelectionView()` 在登录、登出和玩家替换时清空游标；普通关闭界面保留游标供本会话下次 K 键打开。
- [x] **Step 4: Confirm deterministic re-login entry.** 没有难度进入 `DIFFICULTY`；有难度但根基未满足进入 `APTITUDE`；二者满足进入 `REVIEW`。不增加网络包或 NBT 字段。

**Checkpoint:** 核对“关屏后 K 键续步、重登后按服务端状态定位、刷新不跳步”，等待批准后再改布局与导航。

---

### Task 3: 将七页自由导航改成向导导航

**Files:**
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuViewScreen.java`

**Interfaces:**
- 用 `showStep(TianfuWizardStep)` 统一阶段切换、分类映射、搜索恢复、列表重建和游标更新。
- 用 `canAdvance()`、`canSkipCurrentStep()` 计算按钮状态；难度未选时不能离开 `DIFFICULTY`，资质阶段无根基时不能跳过。
- 既有 `decision(Row)`、`submit()` 与 `Tianfu*Payload` 交易路径继续处理服务端变更。

- [x] **Step 1: Replace clickable category rail with a progress rail.** 展示八阶段名称和当前进度；正常选择阶段不允许任意点选跳页。只有在 `REVIEW` 上允许点击可编辑阶段作为“返回修改”快捷入口；难度不可重选。
- [x] **Step 2: Add back, continue, skip, and review controls.** “上一步”回退；“下一步”推进；可选阶段显示“跳过”；`APTITUDE` 根基不满足时禁用跳过和继续，并提示需要满足现有结算条件。
- [x] **Step 3: Keep existing category tools inside mapped stages.** `ORIGIN` 与五个命格分类继续使用原条目列表、详情、搜索、滚动、购买和撤销交互。阶段按钮只移动界面，不发送玩法请求。
- [x] **Step 4: Preserve close and keyboard behavior.** Esc 仍只关闭/取消弹窗；空格仍执行当前条目动作；为向导前进和返回提供可见按钮，不覆盖 K 键开屏行为。

**Checkpoint:** 完成导航后先展示改动结果并暂停；未经批准不继续复核页和发版。

---

### Task 4: 整合总复核、随机候选与只读状态

**Files:**
- Modify: `src/main/java/net/guzhenren/tianfu/client/TianfuViewScreen.java`

**Interfaces:**
- `renderReview()` 显示现有 `finalReview()` 汇总内容及 `pending` 未接受候选提示。
- 随机和接受动作继续发 `TianfuRollPayload` / `TianfuRollAcceptPayload`；最终确认继续发 `TianfuConfirmPayload`。
- `canConfirm()` 除现有服务端前置条件外，界面必须位于 `REVIEW`；服务端仍是最终裁决者。

- [x] **Step 1: Render an explicit summary stage.** 总复核列出难度、出身、余额、已购条目、负面代价与未接受候选；提供返回各可编辑阶段的入口。
- [x] **Step 2: Keep random generation inside talent-selection stages.** 在 `APTITUDE`、`LIUPAI`、`PHYSIQUE`、`MIND`、`QIYUN` 保留“随机 / 接受候选”入口；不在难度、出身阶段或只读确认状态中显示可操作按钮。
- [x] **Step 3: Gate final confirmation at review.** 只有总复核阶段、已选难度、根基满足且服务端快照可操作时显示可确认状态；触发后保留现有二次确认弹窗、预检、结算和锁定。
- [x] **Step 4: Render confirmed players as read-only review.** 仍能查看汇总，不显示购买、撤销、摇取、接受或最终确认操作；刷新状态不离开复核页。

**Checkpoint:** 对照批准设计逐条复核流程，等待用户批准后才递增版本与打包。

---

### Task 5: 递增版本并生成交付 Jar

**Files:**
- Modify via script: `version.properties`, `META-INF/neoforge.mods.toml`, `docs/天赋系统介绍文档.md`, `docs/宣传文档.md`
- Create/Update: `build/libs/tianfu-1.3.8.jar`

- [x] **Step 1: Add the release note.** 在 `docs/宣传文档.md` 顶部版本记录加入“开局选择改为可返回、可跳过可选阶段的向导；核心点数和结算规则保持不变”。
- [x] **Step 2: Bump the synchronized version.** 从项目根目录执行 `python _crash3/bump_version.py 1.3.8`，检查四处版本均为 `1.3.8`。
- [x] **Step 3: Compile and package with the project scripts.** 执行 `python _crash3/javac_check.py`，成功后执行 `python _crash3/pack_jar.py`；不得运行 Gradle。
- [x] **Step 4: Check the artifact.** 依次执行 `python _crash3/verify_jar.py`、`python _crash3/verify_version.py`、`python _crash3/check_bom.py`；预期 Jar 校验通过、四个版本一致且没有 BOM。
- [x] **Step 5: Report the boundary.** 报告源代码和 Jar 路径、脚本检查结果；明确未安装到游戏目录，且除非实际启动游戏，不宣称游戏内交互已验证。

**Checkpoint:** 交付 Jar 与静态校验结果，停止等待用户决定是否要安装或继续调整。

---

## Plan Self-Review

- **设计覆盖：** 八阶段顺序、必选/可跳过条件、立即服务端保存、随机候选明确接受、最终复核、会话内续步、重登定位、已确认只读状态分别由 Tasks 1–4 覆盖；Task 5 覆盖项目要求的版本与 Jar 交付。
- **范围检查：** 选择经济、网络载荷、NBT、效果结算与开窍入口均不更改；没有把 UI 阶段写入玩家数据。
- **占位符检查：** 未使用 TBD/TODO；所有目标文件、接口、脚本和成功条件均已列出。
- **版本一致性：** 当前项目 `version.properties` 为 1.3.7，Task 5 递增至 1.3.8；使用项目的四点同步脚本，未列入 `build.gradle` 手动修改。
- **验证范围：** 不新建、不运行测试套件；按项目构建说明编译并静态检查 Jar。游戏内体验另行注明。

