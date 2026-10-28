<!-- V2-VERIFICATION.md -->
# Tianfu V2 架构验证记录

日期：2026-10-25。交付为可编译架构骨架，业务未实现；不发布、不安装到玩家存档。

## 已通过

- 全源编译：62 个 Java 源文件，真实 JDK 21，`-source 21 -target 21 -proc:none -implicit:none`，javac 退出码 **0**。
- 目标核心：Minecraft 1.21.1（NeoForm 1.21.1-20240808.144430）、NeoForge **21.1.249** 的 client/universal jar。classpath 排除其它 NeoForge 核心版本。
- 字节码复核：输出 94 个 class，全部 major version **65**（Java 21）。
- 原 140 条 TianfuTalents 的 ID、名称、品质、成本、描述与完整效果表达式保持原样；162 条新旧词条无重复 ID。
- 所有既有源码方法签名保持；build.gradle、settings.gradle、gradle.properties、version.properties、wrapper 文件哈希与改动前基线一致。
- 219 项旧/新池条目有唯一显式页面归属；七页顺序固定，神/极不另建页。
- 负面 20 条，−2×12 / −5×5 / −8×3，总面值 73；新心智正项 12 条，成本分布 2×5 / 3×5 / 5×2。
- 出身 5 地域与 4 类型；中州恰好覆盖证据表的 44 个非汇总道痕字段，原前缀保留。
- Java UTF-8 无 BOM；**75** 个业务待填标记在 FILL-SPEC.md 中逐一索引。
- 独立只读审查完成；发现的独立出身成交价遗漏、资质/随机策略不明确、UI Javadoc 泛化均已修正，复查未发现新的重要问题。

## 验证命令与产物

```powershell
$env:PYTHONIOENCODING='utf-8'
& 'D:/DSH Desktop/resources/runtime/x64/primary-runtime/dependencies/python/python.exe' -u _tmp_verify/verify_v2_architecture.py --compile
```

脚本固定读取目标核心 jar，输出目录 `_tmp_verify/v2_javac_21.1.249/`；编译诊断见 `compile.log`，准确源清单与 classpath 见 `args.txt`。源代码基线和禁改哈希见 `_tmp_verify/v2-architecture-baseline.json`。

本次编译有 6 条弃用警告：旧 TianfuKeyMappings/TianfuParticles 的 bus/Bus 各 2 条，新增纯注册类 TianfuV2Registration 的同类警告 2 条；无编译错误。

## 验证限制

- 本机只有 21.1.249 核心产物，没有其完整启动器 profile；传递库来自本机 1.21.1 / NeoForge 21.1.263 profile。本结果是目标核心上的本地全源编译，**不等于完整锁定依赖矩阵构建**。
- 现有 wrapper 是 Gradle 8.12，build.gradle 使用 java 插件与离线 classpath；用户矩阵要求 Gradle 9.2.1 / ModDev 2.0.144。遵守禁改约束，未改动也未声称通过该矩阵的 Gradle build。
- 初次 `--release 21` 编译在资源关闭阶段访问 JDK `lib/ct.sym` 被沙箱拒绝，`-XDdev` 确认是 AccessDeniedException；改用真实 JDK 21 的 source/target 参数后退出码 0。没有修改 JDK 或提升权限。
- 未启动游戏、专用服务端或双端网络；出身生效、收入与硬约束、存档迁移等仍须按 FILL-SPEC.md 的业务契约实现并实测。
- 不生成发行 jar，不升版本，不复制到整合包；本轮仅交付架构文件、数据与编码交接规范。
