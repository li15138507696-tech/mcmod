# 天命录 V3 实现交接与验收规范

本文件记录 _ref/v3-spec.md 的本轮实现状态，供后续维护与验收使用。

## 1. 目标与验收标准

目标平台为 NeoForge，Minecraft 1.21.1，Java 21。本轮完成三项改动：

1. **资质可选**：新增丁、丙、乙、甲四档空窍天赋，ID 为 v2_apt_ding、v2_apt_bing、v2_apt_yi、v2_apt_jia，成本分别为 1、3、6、10，最终结算时将 kongqiao 直接设为 8、6、4、2。保存原 kongqiao 至 tianfu/v2_apt_prev，撤销时恢复并清除此快照。四档互斥，且与 Kind.SIJUE、v2_aptitude_shackled 互斥。资质页不再显示本体资质只读行，购买档位可作为确认锚点。
2. **世代点数**：PersistentData 根下的 tianfu_generation 默认读取为 1，读取时不创建；转世清除 tianfu 子标签前递增。选择难度时发放选项基础点加 min(10, (generation - 1) * 2)，负面补点 24 点上限不变。
3. **分类 chip**：按页面现有 ID、品质、Kind 与成本派生分类。每页包含“全部”，难度页只有“全部”；分类按与搜索的关系过滤。分类选项、分类行水平偏移、列表垂直偏移和搜索词均按页保存。chip 使用 TianfuViewScreen 已有的 PANEL、SELECTED、LINE、BORDER、HOVER、LIGHT、MUTED 颜色常量。

验收约束：旧有词条 ID 不改不删；dahen_ 与 daohen_ 字段前缀保持原样；不修改 build.gradle、version.properties；7 参数构造器、refresh、refreshV2 签名不变；Java 源文件 UTF-8 无 BOM；不保留 Agent B 占位标记。

## 2. 文件所有权表

| 文件 | 状态 | 所有权与职责 |
|---|---|---|
| src/main/java/net/guzhenren/tianfu/TianfuTalents.java | 已完成 | 四条资质数据、APTITUDE_SET、kongqiao 原值快照与写回 |
| src/main/java/net/guzhenren/tianfu/TianfuData.java | 已完成 | 根标签世代读取与递增；tianfu wipe 仍只清除本世子标签 |
| src/main/java/net/guzhenren/tianfu/TianfuDifficulty.java | 已完成 | 难度基础点与世代奖励计算 |
| src/main/java/net/guzhenren/tianfu/net/TianfuDifficultyPayload.java | 已完成 | 按当前世代发放基础点与奖励点 |
| src/main/java/net/guzhenren/tianfu/mixin/JingJieBaoCunProcedureMixin.java | 已完成 | wipeAll 前递增世代 |
| src/main/java/net/guzhenren/tianfu/TianfuApply.java | 已完成 | 结算锚点识别已购资质档位 |
| src/main/java/net/guzhenren/tianfu/v2/TianfuEconomy.java | 已完成 | 资质档位、十绝体与空窍桎梏互斥的权威购买规则 |
| src/main/java/net/guzhenren/tianfu/v2/TianfuPageTable.java | 已完成 | 将四个新 ID 路由至资质页 |
| src/main/java/net/guzhenren/tianfu/client/TianfuUiRules.java | 已完成 | 对互斥档位显示具体不可购买原因 |
| src/main/java/net/guzhenren/tianfu/client/TianfuViewScreen.java | 已完成 | 资质只读行移除、锚点提示、分类 chip 与分页状态 |
| FILL-SPEC.md | 已完成 | 本轮规范与验证状态 |
| build.gradle、version.properties、gradle.properties、wrapper | 未修改 | 按要求保留现状 |
| 等待填充文件 | 无 | 所有本轮目标均已实现 |

## 3. 接口契约与不变量

没有待填文件。以下契约用于维护已实现接口：

- TianfuTalents.applyOnce(ServerPlayer, PlayerVariables, Set<String>)：遍历已购词条效果；APTITUDE_SET 从 Fx.a 取目标档位，首次写入时保存购买前字段值。kongqiao 字段不存在、不可访问、不可写或类型不受支持时抛 IllegalStateException。
- TianfuTalents.revokeOnce(ServerPlayer, PlayerVariables, Set<String>)：APTITUDE_SET 从 tianfu/v2_apt_prev 恢复 kongqiao，成功后移除快照；快照缺失或字段不可写时抛 IllegalStateException。
- TianfuData.generation(Player)：只读 PersistentData 根标签，缺失或无效值返回 1，不主动创建键。
- TianfuData.incrementGeneration(Player)：根标签写入递增值，最低按 1 处理，Integer.MAX_VALUE 饱和。
- TianfuDifficulty.pointsForGeneration(Option, int)：基础 points 加世代奖励，奖励最高 10；不改选项目录价或负面补点限制。
- TianfuEconomy.buy(Snapshot, String)：业务拒绝以失败 Result 表示且原 Snapshot 不变；任何资质档位与已购资质档位、Kind.SIJUE 或空窍桎梏冲突。购买不直接修改 PlayerVariables。
- TianfuViewScreen 的原有 7 参数构造器、refresh 与 refreshV2 均保持签名。pageCategory、pageCategoryScroll、pageScroll、pageQuery 分别保存各页分类、分类行偏移、列表偏移和搜索词。分类过滤在搜索过滤之前执行，最终结果同时满足两者。
- 24 点补点上限继续由 TianfuSelection.NEGATIVE_CAP 与现有经济流程执行；世代奖励不进入 negativeGrant。

## 4. 待填标记索引

待填标记总数：0。文件、方法和描述：无。

## 5. 构建命令与建议超时

目标构建命令：gradlew.bat build。首次构建建议预留 5–15 分钟；本轮工作区缓存构建耗时约 5 分 41 秒。

默认 Gradle 用户目录 C:\.gradle 不可写时，可将 GRADLE_USER_HOME 指向工作区已有的 _tmp_verify/gradle-home 后重跑。不要为此修改项目构建脚本。

本轮实际执行 gradlew.bat build，退出码 0，BUILD SUCCESSFUL。Gradle 报告 test 为 NO-SOURCE；没有额外新增或运行测试。

## 6. UNVERIFIED 清单与风险处理

- U1 — 构建矩阵差异：用户锁定矩阵是 Gradle 9.2.1、ModDev 2.0.144、NeoForge 21.1.249；工作区现有 wrapper 和构建配置实际启动 Gradle 8.12，并报告 NeoForge 21.1.263。按约束未修改 build.gradle、version.properties 或 wrapper。故本轮成功构建只证明现有工程配置可编译，锁定矩阵仍未验证。
- U2 — 资质跨系统影响：kongqiao 会影响本体蛊虫使用与突破判定。代码只按规格写入目标值，并保存/恢复购买前值；没有真实游戏内验证其跨系统安全性，不宣称安全。无法访问、写入或识别字段类型时明确抛 IllegalStateException，不静默跳过。
- U3 — 老档世代：缺少 tianfu_generation 的玩家读取为第 1 世；读取不创建键。第一次转世时递增到第 2 世。不会因迁移而补发历史点数。
- U4 — 已购十绝体老档：十绝体仍保留原购买记录；资质档位预览由经济层判为冲突、UI 显示不可购买原因，服务端购买也会拒绝。
- U5 — 实机界面与存档回滚：未启动客户端或专服；分类 chip 的最终像素呈现、实际输入体验、kongqiao 写回与撤销后的游戏状态仍需实机验收。
- 本轮新增的 Minecraft/NeoForge API 调用通过现有配置下的 Gradle 编译；没有新增 Maven 坐标或第三方依赖。锁定 ModDev 版本下的完整依赖矩阵未验证。

## 7. 完成判据

本轮代码交付完成需满足：现有配置下 gradlew.bat build 退出 0；四个资质 ID、成本与目标值准确；购买互斥和结算锚点覆盖新档位；世代键位于 PersistentData 根且在 wipeAll 前递增；所有页面分类按当前数据派生并按页保留选择与偏移；无旧词条 ID 改写、无禁改构建文件变更、Java 无 BOM、待填标记为零。

锁定矩阵构建与真实游戏验证属于 U1、U2、U5，未以本轮现有配置构建成功代替。