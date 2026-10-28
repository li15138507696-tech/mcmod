# mcmod

蛊真人整合包（Minecraft 1.21.1 / NeoForge 21.1.x / Java 21）的附属模组集合。

| 目录 | 模组 | 说明 |
|---|---|---|
| [`tianfu/`](tianfu/) | 蛊真人天命（`tianfu`） | 开局天赋系统：七页选择、点数经济、十绝体、出身、20 条负面天赋、Veil 后处理演出 |
| [`daotu/`](daotu/) | 蛊真人道途（`daotu`） | 后期成长：随机抽选、阅历纪事三模块、势力/身份观察 |

两者共用同一个开窍时刻（希望蛊），数据各自放在 `PersistentData` 的 `tianfu` / `daotu` 子标签里，可同装。

## 环境

- Minecraft 1.21.1 / NeoForge 21.1.263（编译期固定 21.1.249 平台 jar）
- Java 21
- 前置：`guzhenren` 本体 0.1（整合包提供）、可选 `veil`（仅影响全屏后处理特效）

## 构建

`tianfu/` 与 `daotu/` 的构建方式不同：

```powershell
# 天命：Gradle（离线 classpath，从启动器 libraries 取依赖）
cd tianfu
.\gradlew.bat build

# 道途：无 Gradle wrapper，走自带的 javac + 打包脚本
cd daotu
python _crash3\javac_check.py
python _crash3\pack_jar.py
```

> `daotu/_crash3` 与 `tianfu/_crash3`、`_reverse`、`build` 等目录属于本地工作区，
> 体积数百 MB 且含逆向产物，**不入库**（见根目录 `.gitignore`）。

## 文档

- 天命：`tianfu/docs/`（设计、宣传、版本兼容矩阵）、`tianfu/_ref/`（界面与数值规格）、`tianfu/FILL-SPEC.md`
- 道途：`daotu/docs/`、`daotu/PLAN.md`、`daotu/FILL-SPEC.md`

## 状态

代码处于「编译通过」状态，游戏内行为尚未完整实测；各目录文档里的 `UNVERIFIED` 与「未验证」条目为已知缺口，请勿当作已验收。
