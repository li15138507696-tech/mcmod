# 道途 / 恶意 — 专用服务端适配与找 bug 报告

> 2026-10-31

## 一、服务端环境

| 项 | 值 |
|---|---|
| 服务端 | NeoForge 21.1.263 专用服务端（官方 installer `--installServer` 装出） |
| 本体 | `guzhenrn蛊真人模组本体10.20.jar`（内层 `guzhenren-0.1-neoforge-1.21.1.jar`，sha256 `b6d314d3…`） |
| 被测 mod | `daotu-1.8.0`、`guzhenren_malice-1.1.2-mc1020`、`tianfu-1.4.9` |
| 恶意依赖 | `l2hostility-3.0.18` + `l2library-3.0.9` + `l2complements-3.1.3` + `Patchouli` + `Curios` |
| 交互方式 | RCON（127.0.0.1:25575），自写极简客户端，每条命令独立连接避免多包错位 |

启动结果：

```
[11:09:57] [Server thread/INFO] [minecraft/DedicatedServer]: Done (2.858s)!
```

**两个 mod 都正常加载，无 Mixin 失败、无 NoClassDefFoundError、无 payload 注册错误。**
日志里的报错全部来自本体自身数据（`guzhenren:deleted_mod_element`、`guzhenren:fangju` 标签、
`guzhenren:nilvhuafenjie` 配方）与 Curios 引用缺失的 cataclysm 实体，均与本次被测 mod 无关。

---

## 二、恶意：服务端实测通过

### 版本识别正确

```
/guzhenren_malice info
→ 本体版本：10.20 —— 本体与 10.4 是同一个内嵌 jar；库层新增势力/任务/世界地图（+439 类），
  据点与任务生成的妖兽不经过 finalizeSpawn，必须靠补挂保险丝
```

`GuzhenrenVersion.detect()` 用内容指纹（`jiang_shi_gu_xiu` / `FactionNpcManager` / `gu_xiu`）
在真服务端上判成 10.20，正确。

### 补挂保险丝真的在工作

| 实体 | 分档 | 恶意等级 | 血量倍率 | 结论 |
|---|---|---|---|---|
| 电狼 | 普通 | Lv.18 | ×1.81 | 补挂成功 |
| 白相仙蛇 | 精英 | Lv.73 | ×5.38 | 原生挂载 |
| 魂兽.兽皇 | 妖王 | Lv.124 | ×10.30 | 原生挂载 |
| 点元（陈红药） | 排除 | — | 本体原值 30.0 | 正确不接管 |

日志印证：`首次见到 guzhenren:dian_lang：恶意能力未挂载 —— 已启用补挂`，
而 `bai_xiang_xian_she` 是「已挂载」。说明 10.20 里确实有妖兽绕过 `finalizeSpawn`，保险丝有效。

### 标签条数与变体一致

```
数据检查: 普通=28 精英=62 妖王=13 排除=22 · l2hostility:whitelist=103
```

---

## 三、找到的 bug

### Bug A（已修）· `difficulty` 的中文档位根本按不进去

**现象**（1.1.1 实测）：

```
/guzhenren_malice difficulty 标准
→ Expected whitespace to end one argument, but found trailing data
   ...ifficulty 标准<--[HERE]
```

而 `/guzhenren_malice difficulty easy` 正常，`info` 的帮助文字和错误提示都在宣传
「安逸 / 标准 / 凶险 / 噩梦」——**给了个按不动的选项**。

**根因**：`MaliceCommands` 用的是 `StringArgumentType.word()`。Brigadier 的 `word()`
走 `StringReader.readUnquotedString()`，其字符白名单 `isAllowedInUnquotedString`
只有 `0-9A-Za-z_-+.`，**非 ASCII 一律不接受**，于是读 0 个字符后报「trailing data」。

**修法**：改成 `StringArgumentType.greedyString()`（`getString` 取值方式不变）。

**同时补的**：`set()` 原来只 `broadcastSystemMessage`（发给玩家），控制台/RCON 看不到任何反馈；
现在额外 `sendSuccess` 一条给命令源，从服务端控制台改难度不再「看起来没反应」。

**验证**（1.1.2 实测，四个中文档位全过）：

```
difficulty 安逸 → 难度已切换为「安逸」（等级 ×0.6）   info 复核：安逸 ×0.6 ✓
difficulty 标准 → 难度已切换为「标准」（等级 ×1.0）   info 复核：标准 ×1.0 ✓
difficulty 凶险 → 难度已切换为「凶险」（等级 ×1.5）   info 复核：凶险 ×1.5 ✓
difficulty 噩梦 → 难度已切换为「噩梦」（等级 ×2.2）   info 复核：噩梦 ×2.2 ✓
difficulty easy → 仍可用                              ✓
difficulty 乱写 → 未知档位：乱写（可选 …）            ✓
```

### Bug B（已修）· 变体数据没有生成器，`gu_xiu` 丢失 + 三个实体分档漂移

**现象**：`pack_jar.py --variant` 提示「先跑 gen_variants.py」，但**这个脚本全盘都不存在**。
后果有两个：

1. 源码里 `malice_normal.json` / `whitelist.json` 有 `gu_xiu`，但 **104 / 1020 变体里没有**
   → 打出来的 jar 缺它，10.4/10.20 上蛊修拿不到难度（这正是之前修过一次、又丢了的那个问题）。
2. 源码与变体已经漂移：`cangyuelingjun` / `hhongtaofuhailing` / `yuanyuelin`
   在源码里是 **elite**，在变体里却出现在 **normal**。

**修法**：补写 `_build/gen_variants.py`
- 以 `src/main/resources/data/...` 为唯一真源
- 用 `_build/basever/entities-<ver>.txt`（本体 `GuzhenrenModEntities` 常量池导出，699/656/656）逐版本过滤
- 支持 `--check` 只对比不写盘

**验证**（1.1.2 三个变体）：

| 变体 | boss | elite | exclude | normal | whitelist | 该版本不存在的 id |
|---|---|---|---|---|---|---|
| 98 | 13 | 62 | 22 | 29（含僵尸蛊修） | 104 | **0** |
| 104 | 13 | 62 | 22 | 28（含 gu_xiu） | 103 | **0** |
| 1020 | 13 | 62 | 22 | 28（含 gu_xiu） | 103 | **0** |

三个漂移实体回到 elite，`gu_xiu` 回到 104/1020、僵尸蛊修留在 98。

### Bug C（已修）· `audit_ids.py` 写死另一个工作区的路径

```
FileNotFoundError: 'D:\daima\mcmod\1.12.2原初修真工作区\_malice_scan\ent_cat.json'
```

原脚本依赖 `1.12.2原初修真工作区` 的扫描产物（`ent_cat.json` / `g98\ent98.json` /
`entity_lang.json` / `g98\attrs98.json`），本机根本没有这些文件，所以这个自检**从来没跑起来过**。

**修法**：重写成自包含——① 调 `gen_variants.py --check` 查漂移；② 逐个校验已构建 jar
的标签 id 是否都在对应本体版本存在；③ 打印每档条数。退出码 0 = 全过。

**验证**：

```
1) 变体数据 vs 源码 → 一致
2) 98 / 104 / 1020 三个 jar 全部命中，0 个不存在的 id
总判定: PASS   （python 退出码 0）
```

---

## 四、待你决定的一处（未改）

### `gu_xiu` 归「普通档」很可能是错的

服务端实测把它当敌对怪处理：

```
probe @e[type=guzhenren:gu_xiu,limit=1]
→ 分档：普通 ｜ 恶意白名单：是
  恶意血量修饰符：+0.90（×1.90） ｜ 反推恶意等级 ≈ Lv.20
```

但召唤出来的名字是 **楚不器 / 张无咎 / 苏守拙 / 韩听澜 / 张照微**——**有名字的 NPC**，
和同样带名字、却被放进 `malice_exclude` 的 **点元（陈红药 / 萧望舒）** 属于同一类。
项目自己的约定也是「NPC → 排除」（exclude 里有 `ren_wu_npc_1/2`、`shou_gou_shang_ren`、
`xun_jian_si_zong_guan` 等）。

**这是你上次亲自选的「归普通档」**，所以我没有擅自改。要不要我把它挪到 `malice_exclude`？

---

## 五、道途：服务端无问题

| 检查 | 结果 |
|---|---|
| 加载 | `Done (2.858s)`，无异常 |
| 命令 | `/daotu-chronicle` 存在；控制台调用返回「该命令只能由玩家执行」（设计如此） |
| 客户端类泄漏 | 服务端侧只有 `DaotuStatePayload` 引用 `DaotuClientHandler`，而它是 `playToClient` 的 S2C 包，服务端不会执行 → 安全 |
| 写本体字段是否标脏 | 通过 |
| 静态可变状态 | 全是只读查表，无按 UUID 的可变表 → 无泄漏点 |

**没有找到服务端 bug。**

---

## 六、我自己犯的三个错（都不是 mod 的问题）

1. **日志编码判错**：`srv_out.txt` 是 **GBK**，我按 UTF-8 解，于是 `[蛊真人×恶意]` 显示成
   `[�����ˡ�����]`，一度误判「补挂日志 0 行」。按 GBK 解就全对。
2. **`Get-Content -Raw` + `Set-Content` 往返把 UTF-8 中文按 ANSI 转坏**，
   一个校验脚本被写坏成 `SyntaxError`。之后一律用写文件工具处理中文源码。
3. **`Select-Object -Last N` 截断管道**会让退出码显示成 1，误以为脚本失败。

---

## 七、交付

```
恶意 1.1.2：javac exit 0 / sources 9 / 三变体各 17 条目
  guzhenren_malice-1.1.2-mc98.jar    26848 bytes
  guzhenren_malice-1.1.2-mc104.jar   26799 bytes
  guzhenren_malice-1.1.2-mc1020.jar  26831 bytes   ← 已装入整合包与测试服
道途 1.8.0：未改动（服务端无问题）
```

新增/重写脚本：
- `蛊真人恶意/_build/gen_variants.py`（新）
- `蛊真人恶意/_build/audit_ids.py`（重写为自包含）
- `蛊真人恶意/_build/bump_version.py`（新）
- `蛊真人恶意/_build/basever/entities-{9.8,10.4,10.20}.txt`（版本实体清单）
- `蛊真人附属/_crash6/rcon_addons{,2,3,4}.py`（服务端命令测试）
- `蛊真人附属/_crash6/audit_server_side.py`（通用服务端静态审计）
- `蛊真人附属/_crash6/verify_malice_jars.py`（三变体 jar 校验）
