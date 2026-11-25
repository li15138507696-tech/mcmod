# 《蛊真人天命》1.4.17 → 1.4.18 · 完整天赋清单 + 战斗 / 生存 / 资源三类审查

> 审查员：audit-inventory（任务 `task-4`）
> 项目根：`D:\daima\mcmod\蛊真人附属`
> 附属源码：`tianfu/src/main/java/net/guzhenren/tianfu/`
> 本体证据：`_crash3/basever/guzhenren-10.20.jar`，sha256 `B6D314D3810B8BC5FCFDBC4F588DB8721F0EFDFC14D27A0A06D88143A01FE2BC`（**已核验一致**）
> 环境：MC 1.21.1 / NeoForge 21.1.x / Java 21
> 本阶段**只读**：未修改任何源码 / 构建 / 版本 / 已有 docs 文件。本文件与 `_dump_talents.py` 是唯一产出。

---

## 〇、结论速览

| 项 | 结果 |
|---|---|
| 脚本实测词条数 | **173** = 内置 21 + 扩展 152（历史口径 133 = 22 + 111，**差 +40**） |
| 脚本实测可购数 | **208** = 173 + 凡体 20 + 十绝体 10 + 杀神 1 + 机缘 4（历史口径 167，**差 +41**） |
| 源码条目总数（含恶意分页） | **222** |
| Kind 分布 | BODY 20 / ENTRY 187 / SIJUE 11 / JIYUAN 4 |
| 重复 id | **0 处** |
| 重复中文名 | **0 处** |
| 描述与代码不一致 | **19 类 / 去重后涉及 82 条**（第 5 章逐条；其中 75 条在 TianfuTalents 数据表内、7 条是 TianfuPool 硬编码内置词条） |
| 超模 top3 | 神·天机神算（12 点换恒定 ×4.4 输出）· 极·万世不灭（生命 ×2.5 + 8%/s 回血）· 万法皆通（全流派熟练度 ×3，永久复利） |
| 亏本 top3 | 冰雪道体 / 魂道融彻（amp=0，0 收益花 5 点）· 剑指偏锋（0.75%/点）· 总痕储备（可能负收益） |
| 属性写入真实性 | **未发现写入本体不读的字段**；45 个 `daohen_*` / 46 个 `liupai_*` 全部真实存在且成对匹配 |

---

# 第一部分 · 完整天赋清单（脚本生成）

## 0. 脚本说明

| 项 | 内容 |
|---|---|
| 脚本路径 | `docs/audit-1.4.18/_dump_talents.py` |
| 运行方式 | `$env:PYTHONIOENCODING='utf-8'; python docs\audit-1.4.18\_dump_talents.py` |
| 输入 | `TianfuTalents.java`（67,533 B）、`TianfuPool.java`（11,058 B），只读打开 |
| 解析方法 | ① 状态机剥除 `//` 与 `/* */` 注释（保留换行 → 行号不变，字符串字面量内的 `//` 不受影响）；② 括号配平切出 `BASE` / `MALICE` / `BODIES` / `entry` 块 / `SIJUES` / `JIYUANS` 六个区段；③ 对 `t(...)` / `body(...)` / `entry(...)` / `sijue(...)` / `new Spec(...)` 做顶层逗号切分（尊重字符串与嵌套括号，可跨行）；④ 品质归一化为 `极/神/天/地/玄/黄/负/其它`；⑤ 效果原语按 helper 名（`f/dao/fld/fldAdd/mob/reflect/onHit/adaptive/counter/undying/aura`）还原成可读串 |
| 自校验 | 解析结果 222 个 id 全唯一、222 个名字全唯一；`BASE` 首个=`v2_apt_ding`:146、末个=`xiuxing_chizhi`:343，行号连续无空洞 |
| 断言 | 本文件第一部分的每一行表格均来自该脚本 stdout，**无手工抄写、无估算** |

> 复现命令（只读，不写任何文件）：
> ```powershell
> $env:PYTHONIOENCODING='utf-8'
> cd D:\daima\mcmod\蛊真人附属
> python docs\audit-1.4.18\_dump_talents.py
> ```

<!-- DUMP_BEGIN -->

## A. 全量天赋清单（脚本生成，勿手改）

> 本表由 `docs/audit-1.4.18/_dump_talents.py` 直接解析源码生成，**非手工抄写**。
> 解析对象：`tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java`、`TianfuPool.java`。
> 品质列已归一化为 `极/神/天/地/玄/黄/负/其它`；凡体 / 十绝体 / 机缘 / 出身归入「其它」，原始品质见「备注」或分组标题。

### A.1 体质 · 凡体（TianfuPool.BODIES，Kind=BODY）

条目数：**20**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `tudao` | 九命不灭体 | 其它 | 3 | 否 | BODY | 濒死（血量 <10%）闪避率 +25%；土道道痕 +1000 | TianfuPool.java:39 | tizhi=17 |
| `bingdao` | 百战金戈体 | 其它 | 3 | 否 | BODY | 兵道道痕 +1000；兵刃伤害由本体流水线自动体现 | TianfuPool.java:40 | tizhi=18 |
| `bingxuedao` | 寒渊素雪体 | 其它 | 3 | 否 | BODY | 冰雪道道痕 +1000；冰系攻防由本体流水线自动体现 | TianfuPool.java:41 | tizhi=19 |
| `fengdao` | 长风破浪体 | 其它 | 3 | 否 | BODY | 风道道痕 +1000；移速+8%，风系伤害由本体流水线自动体现 | TianfuPool.java:42 | tizhi=21 |
| `gudao` | 白玉铁骨体 | 其它 | 3 | 否 | BODY | 骨道道痕 +1000；附加非护甲减伤（每千 10%，上限 50%） | TianfuPool.java:43 | tizhi=22 |
| `hundao` | 幽魂凝识体 | 其它 | 3 | 否 | BODY | 魂魄上限 +25%、承载极限 +25% | TianfuPool.java:44 | tizhi=24 |
| `jiandao` | 断岳剑心体 | 其它 | 3 | 否 | BODY | 剑道道痕 +1000；自带「剑道伤害增幅」常驻 | TianfuPool.java:45 | tizhi=25 |
| `jindao` | 百炼庚金体 | 其它 | 3 | 否 | BODY | 金道道痕 +1000；自带「镀金」常驻 | TianfuPool.java:46 | tizhi=26 |
| `lidao` | 搬山撼岳体 | 其它 | 3 | 否 | BODY | 力道道痕 +1000；力道属性（原版怪物）+12%；精力+15% | TianfuPool.java:47 | tizhi=28 |
| `mudao` | 苍木回春体 | 其它 | 3 | 否 | BODY | 生命上限 +10% | TianfuPool.java:48 | tizhi=30 |
| `qidao` | 太息养元体 | 其它 | 3 | 否 | BODY | 真元回复 +15%（不破 999 上限）；自带「气道蛊虫威力增幅」常驻 | TianfuPool.java:49 | tizhi=31 |
| `xiuluo` | 修罗战体 | 其它 | 3 | 否 | BODY | 每损 10% 血出伤 +5%（最多 +25%）；血道道痕 +1000 | TianfuPool.java:50 | tizhi=33 |
| `xudao` | 太虚化形体 | 其它 | 3 | 否 | BODY | 受击 15% 概率虚化免伤（内置 5 秒 CD） | TianfuPool.java:51 | tizhi=37 |
| `yingdao` | 如影随形体 | 其它 | 3 | 否 | BODY | 移速 +8%、闪避 +5% | TianfuPool.java:52 | tizhi=39 |
| `yuedao` | 明月入怀体 | 其它 | 3 | 否 | BODY | 夜间真元回复 +80% | TianfuPool.java:53 | tizhi=40 |
| `zhidao` | 慧心通明体 | 其它 | 3 | 否 | BODY | 念头之道 +25%、念头容量 +25% | TianfuPool.java:54 | tizhi=42 |
| `zhoudao` | 光阴逆转体 | 其它 | 3 | 否 | BODY | 宙道时间 +50%、CD −12.5% | TianfuPool.java:55 | tizhi=43 |
| `feixingdao` | 御空踏云体 | 其它 | 3 | 否 | BODY | 直接获得飞行能力（常驻） | TianfuPool.java:56 | tizhi=45 |
| `mengdao` | 南柯一梦体 | 其它 | 3 | 否 | BODY | 念头回复 +30% | TianfuPool.java:57 | tizhi=48 |
| `xueshi` | 血煞蜕变体 | 其它 | 3 | 否 | BODY | 攻击吸血 5%；血量低于 30% 时攻击 +12% | TianfuPool.java:58 | tizhi=53 |

### A.2 内置词条（TianfuPool 内联 entry(...)，Kind=ENTRY）

条目数：**21**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `wanfa_jietong` | 万法皆通 | 极 | 16 | 否 | ENTRY | 所有流派熟练度获取速度 ×3（各流派权重增长 ×3） | TianfuPool.java:64 |  |
| `shen_tianji_suan` | 神·天机神算 | 其它 | 12 | 否 | ENTRY | 暴击率 +50%、暴击伤害 +240%、闪避 +24% | TianfuPool.java:65 |  |
| `shen_bumie_jinshen` | 神·不灭金身 | 其它 | 12 | 否 | ENTRY | 生命上限 +100%、受击减伤 +20%、每秒回复 5% 生命 | TianfuPool.java:66 |  |
| `suhui` | 宿慧 | 天 | 5 | 否 | ENTRY | 每转数提供额外修炼基数与道痕获取 | TianfuPool.java:67 |  |
| `shazhao_tiancheng` | 杀招天成 | 地 | 3 | 否 | ENTRY | 杀招成功率 +5%、10 槽磨损 −1 | TianfuPool.java:68 | **运行期停用**（NATIVE_UNAVAILABLE） |
| `shiyuan_zhixing` | 噬元之性 | 地 | 3 | 否 | ENTRY | 击杀灵体回复真元/念头 | TianfuPool.java:69 |  |
| `fuze_mianchang` | 福泽绵长 | 地 | 3 | 否 | ENTRY | 气运上限 +15（上限越高，本体气运回复空间越大） | TianfuPool.java:70 |  |
| `zhenyuan_ninglian` | 真元凝练 | 玄 | 2 | 否 | ENTRY | 真元消耗 −10% | TianfuPool.java:71 | **运行期停用**（NATIVE_UNAVAILABLE） |
| `niantou_ninglian` | 念头凝练 | 玄 | 2 | 否 | ENTRY | 念头消耗 −10% | TianfuPool.java:72 | **运行期停用**（NATIVE_UNAVAILABLE） |
| `xinshen_ruyuan` | 心神如渊 | 玄 | 2 | 否 | ENTRY | 沉默时长减半 | TianfuPool.java:73 |  |
| `kuxiu_buchuo` | 苦修不辍 | 玄 | 2 | 否 | ENTRY | 修炼速度 +20% | TianfuPool.java:74 |  |
| `houji_bofa` | 厚积薄发 | 玄 | 2 | 否 | ENTRY | 本体突破失败概率降低 20% | TianfuPool.java:75 |  |
| `suiyue_chendian` | 岁月沉淀 | 玄 | 2 | 否 | ENTRY | 已耗寿元越多、修炼越快 | TianfuPool.java:76 |  |
| `youdou_gaoshou` | 游斗高手 | 玄 | 2 | 否 | ENTRY | 移动中受击 −6% | TianfuPool.java:77 |  |
| `jianbi_qingfeng` | 剑指偏锋 | 玄 | 2 | 否 | ENTRY | 暴击伤害 +30%（需配合暴击精通） | TianfuPool.java:78 |  |
| `shouyuan_mianchang` | 寿元绵长 | 黄 | 1 | 否 | ENTRY | 寿元 +50 | TianfuPool.java:79 |  |
| `qiyun_weiguang` | 气运微光 | 黄 | 1 | 否 | ENTRY | 气运上限 +5 | TianfuPool.java:80 |  |
| `jingu_qiangjian` | 筋骨强健 | 黄 | 1 | 否 | ENTRY | 生命上限 +10% | TianfuPool.java:81 |  |
| `baoji_jingtong` | 暴击精通 | 黄 | 1 | 否 | ENTRY | 暴击率 +5%、暴击伤害 +20%（叠加） | TianfuPool.java:82 |  |
| `zhanyi_angran` | 战意盎然 | 黄 | 1 | 否 | ENTRY | 满血时出伤 +8% | TianfuPool.java:83 |  |
| `youshou_nengkai` | 右守能铠 | 黄 | 1 | 否 | ENTRY | 受击 4% 概率反弹 10% 伤害 | TianfuPool.java:84 |  |

### A.3 十绝体 + 杀神（TianfuPool.SIJUES，Kind=SIJUE）

条目数：**11**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `sijue_1` | 太日阳莽体 | 其它 | 16 | 否 | SIJUE | 余道道痕 2000；本体十绝体效果原生生效 | TianfuPool.java:98 | tizhi=1 |
| `sijue_2` | 古月阴荒体 | 其它 | 16 | 否 | SIJUE | 周道道痕 2000；宙道时间 +300%、CD −25% | TianfuPool.java:99 | tizhi=2 |
| `sijue_3` | 北冥冰魄体 | 其它 | 16 | 否 | SIJUE | 魂魄上限 500、承载 50000；随机魂道/冰雪道道痕 2000 | TianfuPool.java:100 | tizhi=3 |
| `sijue_4` | 森海轮回体 | 其它 | 16 | 否 | SIJUE | 木道道痕 2000；寿蟽数值 +20 | TianfuPool.java:101 | tizhi=4 |
| `sijue_5` | 炎煌雷泽体 | 其它 | 16 | 否 | SIJUE | 随机炎道/雷道道痕 2000；自带对应流派增幅常驻 | TianfuPool.java:102 | tizhi=5 |
| `sijue_6` | 万金妙华体 | 其它 | 16 | 否 | SIJUE | 金道道痕 2000；自带「镀金」常驻 | TianfuPool.java:103 | tizhi=6 |
| `sijue_7` | 大力真武体 | 其它 | 16 | 否 | SIJUE | 力道道痕 2000；精力上限 200、骨骼力 +20 | TianfuPool.java:104 | tizhi=7 |
| `sijue_8` | 逍遥智心体 | 其它 | 16 | 否 | SIJUE | 智道道痕 2000；念头之道 2000、念头容量 20000 | TianfuPool.java:105 | tizhi=8 |
| `sijue_9` | 厚土元央体 | 其它 | 16 | 否 | SIJUE | 土道道痕 2000 | TianfuPool.java:106 | tizhi=9 |
| `sijue_10` | 宇宙大衍体 | 其它 | 16 | 否 | SIJUE | 律道道痕 2000；自带「律道增幅」常驻 | TianfuPool.java:107 | tizhi=10 |
| `shashen` | 杀神 | 其它 | 16 | 否 | SIJUE | 杀戮值累积伤害成长（封顶 5 倍伤害）+ 杀怪噬道（击杀积道痕）；不给开局道痕，愈杀愈强 | TianfuPool.java:108 | tizhi=54 |

### A.4 机缘（TianfuPool.JIYUANS，Kind=JIYUAN，仅存档兼容）

条目数：**4**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `jiyuan_ding` | 丁等机缘 | 其它 | 3 | 否 | JIYUAN | 真元上限 200-390、气运上限 1-10、90% 命格 −1（否则天资流畅） | TianfuPool.java:114 |  |
| `jiyuan_bing` | 丙等机缘 | 其它 | 5 | 否 | JIYUAN | 真元上限 400-590、气运上限 1-3、70% 命格 −1（否则天资流畅） | TianfuPool.java:116 |  |
| `jiyuan_yi` | 乙等机缘 | 其它 | 8 | 否 | JIYUAN | 真元上限 600-790、气运上限 10-20、50% 命格 −1（否则天资流畅） | TianfuPool.java:118 |  |
| `jiyuan_jia` | 甲等机缘 | 其它 | 12 | 否 | JIYUAN | 真元上限 800-990、气运 1-10、1/3 命格 −2 且气运上限 35-45（否则天资流畅） | TianfuPool.java:120 |  |

### A.5 扩展词条（TianfuTalents.BASE，Kind=ENTRY）

条目数：**152**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `v2_apt_ding` | 丁等空窍 | 黄 | 1 | 否 | ENTRY | 丁等空窍，真元上限 200–390，沿用本体 9.8 的气运与命格结算 | TianfuTalents.java:146 |  |
| `v2_apt_bing` | 丙等空窍 | 玄 | 3 | 否 | ENTRY | 丙等空窍，真元上限 400–590，沿用本体 9.8 的气运与命格结算 | TianfuTalents.java:147 |  |
| `v2_apt_yi` | 乙等空窍 | 地 | 6 | 否 | ENTRY | 乙等空窍，真元上限 600–790，沿用本体 9.8 的气运与命格结算 | TianfuTalents.java:148 |  |
| `v2_apt_jia` | 甲等空窍 | 天 | 10 | 否 | ENTRY | 甲等空窍，真元上限 800–990，沿用本体 9.8 的气运与命格结算 | TianfuTalents.java:149 |  |
| `v2_jingli_buji` | 精力不济 | 负 | -2 | 是 | ENTRY | 精力上限 −20% | TianfuTalents.java:150 |  |
| `v2_gu_cd_ningse` | 催蛊凝涩 | 负 | -2 | 是 | ENTRY | 蛊冷却每 tick 额外增加 0.10，仅对正在冷却的蛊 | TianfuTalents.java:151 |  |
| `v2_zhenyuan_kubai` | 真元枯败 | 负 | -5 | 是 | ENTRY | 真元回复 −75%（旧 −30% 的 2.5 倍） | TianfuTalents.java:152 |  |
| `v2_routi_canque` | 肉体残缺 | 负 | -5 | 是 | ENTRY | 生命上限 −50%（旧 −20% 的 2.5 倍） | TianfuTalents.java:153 |  |
| `v2_shouyuan_kuwei` | 寿元枯萎 | 负 | -5 | 是 | ENTRY | 寿元 −250（旧 −100 的 2.5 倍，确认前显示致死风险） | TianfuTalents.java:154 |  |
| `v2_qiyun_duanjue` | 气运断绝 | 负 | -5 | 是 | ENTRY | 基础气运上限 −37.5%（旧 −15% 的 2.5 倍） | TianfuTalents.java:155 |  |
| `v2_xinzhi_yousun` | 心智有损 | 负 | -5 | 是 | ENTRY | 修炼速度 −40%、本体突破失败概率提高 50% | TianfuTalents.java:156 |  |
| `v2_aptitude_shackled` | 空窍桎梏 | 负 | -8 | 是 | ENTRY | 本世锁定确认时原有资质；念头容量 −100%，不允许后续机缘或十绝体突破锁定 | TianfuTalents.java:157 |  |
| `v2_luck_forfeit` | 天弃之命 | 负 | -8 | 是 | ENTRY | 本世气运与气运上限持续为 0；气运增益不能抵消，约四倍量级并附不可补回代价 | TianfuTalents.java:158 |  |
| `v2_lifespan_scar` | 命烛残尽 | 负 | -8 | 是 | ENTRY | 确认时寿元 −400，且本世寿元恢复与增加无效；寿元不足不得确认 | TianfuTalents.java:159 |  |
| `v2_mind_xiulian_1` | 修炼速度 I | 玄 | 2 | 否 | ENTRY | +20% | TianfuTalents.java:160 |  |
| `v2_mind_xiulian_2` | 修炼速度 II | 地 | 3 | 否 | ENTRY | +40% | TianfuTalents.java:161 |  |
| `v2_mind_xiulian_3` | 修炼速度 III | 天 | 5 | 否 | ENTRY | +60% | TianfuTalents.java:162 |  |
| `v2_mind_capacity_1` | 念头容量 I | 玄 | 2 | 否 | ENTRY | +20% | TianfuTalents.java:163 |  |
| `v2_mind_capacity_2` | 念头容量 II | 地 | 3 | 否 | ENTRY | +30% | TianfuTalents.java:164 |  |
| `v2_mind_capacity_3` | 念头容量 III | 天 | 5 | 否 | ENTRY | +50% | TianfuTalents.java:165 |  |
| `v2_mind_recovery_1` | 念头恢复 I | 玄 | 2 | 否 | ENTRY | +20% | TianfuTalents.java:166 |  |
| `v2_mind_recovery_2` | 念头恢复 II | 地 | 3 | 否 | ENTRY | +40% | TianfuTalents.java:167 |  |
| `v2_mind_liangu_1` | 炼蛊进度 I | 玄 | 2 | 否 | ENTRY | +15% | TianfuTalents.java:168 |  |
| `v2_mind_liangu_2` | 炼蛊进度 II | 地 | 3 | 否 | ENTRY | +30% | TianfuTalents.java:169 |  |
| `v2_mind_cooldown_1` | 蛊冷却 I | 玄 | 2 | 否 | ENTRY | 每 tick −0.15 | TianfuTalents.java:170 |  |
| `v2_mind_cooldown_2` | 蛊冷却 II | 地 | 3 | 否 | ENTRY | 每 tick −0.25 | TianfuTalents.java:171 |  |
| `yudao_qinhe` | 宇道亲和 | 黄 | 1 | 否 | ENTRY | 宇道道痕 +200；熟练度效率 +20%，不直接增伤 | TianfuTalents.java:175 |  |
| `qidao_qinhe` | 气道亲和 | 黄 | 1 | 否 | ENTRY | 气道道痕 +200；熟练度效率 +20%，不直接增伤 | TianfuTalents.java:176 |  |
| `lvdao_qinhe` | 律道亲和 | 黄 | 1 | 否 | ENTRY | 律道道痕 +200；熟练度效率 +20%，不提供全流派增伤 | TianfuTalents.java:177 |  |
| `toudao_qinhe` | 偷道亲和 | 黄 | 1 | 否 | ENTRY | 偷道道痕 +200；熟练度效率 +20%，不直接增伤 | TianfuTalents.java:178 |  |
| `nudao_qinhe` | 奴道亲和 | 黄 | 1 | 否 | ENTRY | 奴道道痕 +200；熟练度效率 +20%，不直接增伤 | TianfuTalents.java:179 |  |
| `huadao_qinhe` | 化道亲和 | 黄 | 1 | 否 | ENTRY | 化道字段道痕 +200；与变化道字段独立，不直接增伤 | TianfuTalents.java:180 |  |
| `andao_qinhe` | 暗道亲和 | 黄 | 1 | 否 | ENTRY | 暗道道痕 +200；熟练度效率 +20%，与影道独立 | TianfuTalents.java:181 |  |
| `shidao_qinhe` | 食道亲和 | 玄 | 2 | 否 | ENTRY | 食道道痕 +500；熟练度效率 +50%，不直接增伤 | TianfuTalents.java:182 |  |
| `xindao_qinhe` | 心道亲和 | 玄 | 2 | 否 | ENTRY | 心道字段道痕 +500；不直接增伤 | TianfuTalents.java:183 |  |
| `yundao_qinhe` | 运道亲和 | 玄 | 2 | 否 | ENTRY | 运道字段道痕 +500；不直接修改气运或其上限 | TianfuTalents.java:184 |  |
| `yundao2_jinjie` | 云运支系进阶 | 玄 | 2 | 否 | ENTRY | yundao2 道痕 +500；支系名称待核，不等同气运 | TianfuTalents.java:185 |  |
| `jindao2_jinjie` | 金道支系进阶 | 玄 | 2 | 否 | ENTRY | jindao2 道痕 +500；与 jindao 字段独立，支系名称待核 | TianfuTalents.java:186 |  |
| `rendao_dacheng` | 人道大成 | 地 | 3 | 否 | ENTRY | 人道道痕 +800；熟练度效率 +80%，不直接等同人气 | TianfuTalents.java:187 |  |
| `tiandao_daocang` | 天道道藏 | 天 | 5 | 否 | ENTRY | 天道字段道痕 +1200；不提供全局增伤 | TianfuTalents.java:188 |  |
| `zonghen_chubei` | 总痕储备 | 天 | 5 | 否 | ENTRY | 总道痕汇总字段 +300；不增加各流派道痕或伤害 | TianfuTalents.java:190 |  |
| `jiandao_qinhe` | 剑道亲和 | 黄 | 1 | 否 | ENTRY | 剑道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:193 |  |
| `leidao_qinhe` | 雷道亲和 | 黄 | 1 | 否 | ENTRY | 雷道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:194 |  |
| `yandao_qinhe` | 炎道亲和 | 黄 | 1 | 否 | ENTRY | 炎道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:195 |  |
| `bingxuedao_qinhe` | 冰雪道亲和 | 黄 | 1 | 否 | ENTRY | 冰雪道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:196 |  |
| `fengdao_qinhe` | 风道亲和 | 黄 | 1 | 否 | ENTRY | 风道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:197 |  |
| `yingdao_qinhe` | 影道亲和 | 黄 | 1 | 否 | ENTRY | 影道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:198 |  |
| `yuedao_qinhe` | 月道亲和 | 黄 | 1 | 否 | ENTRY | 月道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:199 |  |
| `shuidao_qinhe` | 水道亲和 | 黄 | 1 | 否 | ENTRY | 水道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:200 |  |
| `tudao_qinhe` | 土道亲和 | 黄 | 1 | 否 | ENTRY | 土道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:201 |  |
| `mudao_qinhe` | 木道亲和 | 黄 | 1 | 否 | ENTRY | 木道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:202 |  |
| `jindao_qinhe` | 金道亲和 | 黄 | 1 | 否 | ENTRY | 金道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:203 |  |
| `gudao_qinhe` | 骨道亲和 | 黄 | 1 | 否 | ENTRY | 骨道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:204 |  |
| `lidao_qinhe` | 力道亲和 | 黄 | 1 | 否 | ENTRY | 力道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:205 |  |
| `hundao_qinhe` | 魂道亲和 | 黄 | 1 | 否 | ENTRY | 魂道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:206 |  |
| `xuedao_qinhe` | 血道亲和 | 黄 | 1 | 否 | ENTRY | 血道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:207 |  |
| `xudao_qinhe` | 虚道亲和 | 黄 | 1 | 否 | ENTRY | 虚道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:208 |  |
| `zhidao_qinhe` | 智道亲和 | 黄 | 1 | 否 | ENTRY | 智道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:209 |  |
| `zhoudao_qinhe` | 宙道亲和 | 黄 | 1 | 否 | ENTRY | 宙道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:210 |  |
| `mengdao_qinhe` | 梦道亲和 | 黄 | 1 | 否 | ENTRY | 梦道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:211 |  |
| `dudao_qinhe` | 毒道亲和 | 黄 | 1 | 否 | ENTRY | 毒道道痕 +200；该流派熟练度效率 +20%，不直接增伤 | TianfuTalents.java:212 |  |
| `tiepi_rougu` | 铁皮肉骨 | 黄 | 1 | 否 | ENTRY | 非护甲减伤 +4% | TianfuTalents.java:213 |  |
| `bufen_zhiwu` | 不焚之物 | 黄 | 1 | 否 | ENTRY | 常驻防火 | TianfuTalents.java:214 |  |
| `shenfa_qingjie` | 身法轻捷 | 黄 | 1 | 否 | ENTRY | 移动速度 +5% | TianfuTalents.java:215 |  |
| `zhaoqi_yinlu` | 朝气饮露 | 黄 | 1 | 否 | ENTRY | 每 5 秒回复 1% 生命 | TianfuTalents.java:216 |  |
| `niantou_zisheng` | 念头自生 | 黄 | 1 | 否 | ENTRY | 念头回复 +20% | TianfuTalents.java:217 |  |
| `bingdao_qinhe` | 兵道亲和 | 玄 | 2 | 否 | ENTRY | 兵道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:220 |  |
| `daodao_qinhe` | 刀道亲和 | 玄 | 2 | 否 | ENTRY | 刀道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:221 |  |
| `guangdao_qinhe` | 光道亲和 | 玄 | 2 | 否 | ENTRY | 光道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:222 |  |
| `xingdao_qinhe` | 星道亲和 | 玄 | 2 | 否 | ENTRY | 星道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:223 |  |
| `dandao_qinhe` | 丹道亲和 | 玄 | 2 | 否 | ENTRY | 丹道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:224 |  |
| `liandao_qinhe` | 炼道亲和 | 玄 | 2 | 否 | ENTRY | 炼道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:225 |  |
| `yindao_qinhe` | 音道亲和 | 玄 | 2 | 否 | ENTRY | 音道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:226 |  |
| `huandao_qinhe` | 幻道亲和 | 玄 | 2 | 否 | ENTRY | 幻道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:227 |  |
| `bianhuadao_qinhe` | 变化道亲和 | 玄 | 2 | 否 | ENTRY | 变化道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:228 |  |
| `zhendao_qinhe` | 阵道亲和 | 玄 | 2 | 否 | ENTRY | 阵道道痕 +500；该流派熟练度效率 +50%，不直接增伤 | TianfuTalents.java:229 |  |
| `wuying_bi` | 无影壁 | 玄 | 2 | 否 | ENTRY | 受击 6% 概率反弹 12% 伤害 | TianfuTalents.java:230 |  |
| `ruwei_dingshen` | 入微定神 | 玄 | 2 | 否 | ENTRY | 非护甲减伤 +8% | TianfuTalents.java:231 |  |
| `shanbu_suifeng` | 闪步随风 | 玄 | 2 | 否 | ENTRY | 闪避率 +6% | TianfuTalents.java:232 |  |
| `xueqi_rulin` | 血气如林 | 玄 | 2 | 否 | ENTRY | 生命上限 +20% | TianfuTalents.java:233 |  |
| `lijin_ruquan` | 力进如泉 | 玄 | 2 | 否 | ENTRY | 攻击伤害 +8% | TianfuTalents.java:234 |  |
| `quanru_jifeng` | 拳如疾风 | 玄 | 2 | 否 | ENTRY | 攻击速度 +10% | TianfuTalents.java:235 |  |
| `songyue_jianli` | 耸岳坚立 | 玄 | 2 | 否 | ENTRY | 击退抗性 +0.4 | TianfuTalents.java:236 |  |
| `jingli_shengheng` | 精力生生 | 玄 | 2 | 否 | ENTRY | 精力上限 +30% | TianfuTalents.java:237 |  |
| `chuntian_yishou` | 春田益寿 | 玄 | 2 | 否 | ENTRY | 寿元 +80 | TianfuTalents.java:238 |  |
| `qiyun_weiying` | 气运微荫 | 玄 | 2 | 否 | ENTRY | 基础气运上限 +12%（人气加成另计；零上限保持零） | TianfuTalents.java:239 |  |
| `qiyun_yingmen` | 气运盈门 | 玄 | 2 | 否 | ENTRY | 基础气运上限 +18%（人气加成另计；零上限保持零） | TianfuTalents.java:241 |  |
| `hunpo_lianxi` | 魂魄炼息 | 玄 | 2 | 否 | ENTRY | 魂魄上限 +15%（容量加成，不等于伤害 +15%） | TianfuTalents.java:242 |  |
| `hunzai_kuanhou` | 魂载宽厚 | 玄 | 2 | 否 | ENTRY | 魂魄承载极限 +15% | TianfuTalents.java:243 |  |
| `niantou_kuozhan` | 念头扩展 | 玄 | 2 | 否 | ENTRY | 念头容量 +15% | TianfuTalents.java:244 |  |
| `zhenyuan_huiliu` | 真元回流 | 玄 | 2 | 否 | ENTRY | 真元回复 +35% | TianfuTalents.java:245 |  |
| `tupo_tongtian` | 突破通天 | 地 | 3 | 否 | ENTRY | 本体突破失败概率降低 30% | TianfuTalents.java:248 |  |
| `dazuo_wangwo` | 打坐忘我 | 地 | 3 | 否 | ENTRY | 打坐时修炼进度 +40% | TianfuTalents.java:249 |  |
| `liangu_rushen` | 炼蛊如神 | 地 | 3 | 否 | ENTRY | 炼蛊进度 +40% | TianfuTalents.java:250 |  |
| `gudao_dup` | 蛊道独步 | 地 | 3 | 否 | ENTRY | 额外蛊冷却缩短（每 tick −0.25；熟练度降耗由本体另计） | TianfuTalents.java:251 |  |
| `shazhao_rufeng` | 杀招如风 | 地 | 3 | 否 | ENTRY | 额外杀招冷却缩短（每 tick −0.25；不等同真元降耗） | TianfuTalents.java:252 |  |
| `zhenyuan_ruquan` | 真元如泉 | 地 | 3 | 否 | ENTRY | 真元回复 +80% | TianfuTalents.java:253 |  |
| `niantou_ruyuan` | 念头如渊 | 地 | 3 | 否 | ENTRY | 念头回复 +60% | TianfuTalents.java:254 |  |
| `xieshi_ziyu` | 血食自愈 | 地 | 3 | 否 | ENTRY | 每 5 秒回复 3% 生命 | TianfuTalents.java:255 |  |
| `shenfa_ruyan` | 身法如燕 | 地 | 3 | 否 | ENTRY | 闪避率 +12% | TianfuTalents.java:256 |  |
| `baoji_rulie` | 暴击如裂 | 地 | 3 | 否 | ENTRY | 暴击率 +15% | TianfuTalents.java:257 |  |
| `baoshang_rufeng` | 暴伤如锋 | 地 | 3 | 否 | ENTRY | 暴击伤害 +80% | TianfuTalents.java:258 |  |
| `xixue_rumo` | 吸血入魔 | 地 | 3 | 否 | ENTRY | 攻击吸血 +6% | TianfuTalents.java:259 |  |
| `fanshang_ruyu` | 反伤如玉 | 地 | 3 | 否 | ENTRY | 受击 10% 概率反弹 20% 伤害 | TianfuTalents.java:260 |  |
| `jianjia_ruyan` | 坚甲如岩 | 地 | 3 | 否 | ENTRY | 非护甲减伤 +15% | TianfuTalents.java:261 |  |
| `quansheng_ruhong` | 全盛如洪 | 地 | 3 | 否 | ENTRY | 满血时出伤 +18% | TianfuTalents.java:262 |  |
| `canshen_rulang` | 残身如狼 | 地 | 3 | 否 | ENTRY | 残血（<30%）时出伤 +25% | TianfuTalents.java:263 |  |
| `wanjun_lili` | 万钧之力 | 地 | 3 | 否 | ENTRY | 常驻力量 II | TianfuTalents.java:264 |  |
| `shengji_rumu` | 生息如沐 | 地 | 3 | 否 | ENTRY | 常驻生命恢复 II | TianfuTalents.java:265 |  |
| `jiandao_dacheng` | 剑道大成 | 地 | 3 | 否 | ENTRY | 剑道道痕 +800；该流派熟练度效率 +80%，不直接增伤 | TianfuTalents.java:266 |  |
| `leidao_dacheng` | 雷道大成 | 地 | 3 | 否 | ENTRY | 雷道道痕 +800；该流派熟练度效率 +80%，不直接增伤 | TianfuTalents.java:267 |  |
| `lidao_dacheng` | 力道大成 | 地 | 3 | 否 | ENTRY | 力道道痕 +800；该流派熟练度效率 +80%，不直接增伤 | TianfuTalents.java:268 |  |
| `wuxing_daocang` | 五行道藏 | 天 | 5 | 否 | ENTRY | 44 项非汇总道痕各 +300；对应熟练度效率各 +30%，不直接增伤 | TianfuTalents.java:271 |  |
| `qiqiao_linglong` | 七窍玲珑 | 天 | 5 | 否 | ENTRY | 念头容量 +50%、念头之道 +50% | TianfuTalents.java:272 |  |
| `hunpo_rugui` | 魂魄如鬼 | 天 | 5 | 否 | ENTRY | 魂魄上限 +50%、承载极限 +50%（魂道伤害仍按本体分档） | TianfuTalents.java:274 |  |
| `jingli_ruhai` | 精力如海 | 天 | 5 | 否 | ENTRY | 精力上限 +80% | TianfuTalents.java:276 |  |
| `shouyuan_ruhe` | 寿元如鹤 | 天 | 5 | 否 | ENTRY | 寿元 +200 | TianfuTalents.java:277 |  |
| `qiyun_ruhong` | 气运如虹 | 天 | 5 | 否 | ENTRY | 基础气运上限 +40%（人气加成另计；零上限保持零） | TianfuTalents.java:278 |  |
| `zhenyuan_bujie` | 真元不竭 | 天 | 5 | 否 | ENTRY | 真元回复 +150% | TianfuTalents.java:280 |  |
| `niantou_bujie` | 念头不竭 | 天 | 5 | 否 | ENTRY | 念头回复 +120% | TianfuTalents.java:281 |  |
| `huixue_rusheng` | 回血如生 | 天 | 5 | 否 | ENTRY | 每 5 秒回复 5% 生命 | TianfuTalents.java:282 |  |
| `baoji_rushen` | 暴击如神 | 天 | 5 | 否 | ENTRY | 暴击率 +25%、暴击伤害 +120% | TianfuTalents.java:283 |  |
| `xixue_rushi` | 吸血如饕 | 天 | 5 | 否 | ENTRY | 攻击吸血 +10% | TianfuTalents.java:285 |  |
| `fanshang_rushan` | 反伤如山 | 天 | 5 | 否 | ENTRY | 受击 20% 概率反弹 35% 伤害 | TianfuTalents.java:286 |  |
| `jianjia_rushen` | 坚甲如神 | 天 | 5 | 否 | ENTRY | 非护甲减伤 +25% | TianfuTalents.java:287 |  |
| `xiulian_rushen` | 修炼如神 | 天 | 5 | 否 | ENTRY | 修炼速度 +60%、本体突破失败概率降低 40% | TianfuTalents.java:288 |  |
| `liangu_bujie` | 炼蛊不竭 | 天 | 5 | 否 | ENTRY | 炼蛊进度 +60%、蛊冷却缩短（每 tick −0.3） | TianfuTalents.java:290 |  |
| `jixing_ruying` | 疾行如影 | 天 | 5 | 否 | ENTRY | 常驻迅捷 III | TianfuTalents.java:292 |  |
| `bingxue_daoti` | 冰雪道体 | 天 | 5 | 否 | ENTRY | 常驻「冰雪道增幅」（兼修附属） | TianfuTalents.java:297 |  |
| `hundao_ronghe` | 魂道融彻 | 天 | 5 | 否 | ENTRY | 常驻「魂道增幅」（魂道附属） | TianfuTalents.java:299 |  |
| `qiyun_yizhi` | 一掷乾坤 | 天 | 5 | 否 | ENTRY | 常驻「一掷乾坤」、基础气运上限 +30%（零上限保持零） | TianfuTalents.java:301 |  |
| `shen_qiyun_zhizi` | 神·气运之子 | 神 | 12 | 否 | ENTRY | 基础气运上限 +80%（人气加成另计；零上限保持零） | TianfuTalents.java:305 |  |
| `shen_hunxi_zhuzai` | 神·魂兮主宰 | 神 | 12 | 否 | ENTRY | 魂魄上限 +100%、承载极限 +100%、念头容量 +100% | TianfuTalents.java:307 |  |
| `shen_busi_bumie` | 神·不死不灭 | 神 | 12 | 否 | ENTRY | 每 5 秒回复 12% 生命、非护甲减伤 +30% | TianfuTalents.java:309 |  |
| `shen_zhanshen_linshi` | 神·战神临世 | 神 | 12 | 否 | ENTRY | 攻击伤害 +60%、攻击速度 +25% | TianfuTalents.java:311 |  |
| `shen_jingang_buhuai` | 神·金刚不坏 | 神 | 12 | 否 | ENTRY | 击退抗性 +1、非护甲减伤 +35% | TianfuTalents.java:313 |  |
| `shen_xiulian_wuji` | 神·修炼无极 | 神 | 12 | 否 | ENTRY | 修炼速度 +150%、本体突破失败概率降低 60% | TianfuTalents.java:315 |  |
| `shen_liangu_tianshu` | 神·炼蛊天术 | 神 | 12 | 否 | ENTRY | 炼蛊进度 +90%、蛊冷却缩短（每 tick −0.5） | TianfuTalents.java:317 |  |
| `shen_wandao_guizong` | 神·万道归宗 | 神 | 12 | 否 | ENTRY | 44 项非汇总道痕各 +1500；对应熟练度效率各 +150%，不直接增伤 | TianfuTalents.java:319 |  |
| `shen_liupai_jianji` | 神·流派兼济 | 神 | 12 | 否 | ENTRY | 常驻「冰雪道增幅」+「魂道增幅」+「一掷乾坤」，三者可叠加 | TianfuTalents.java:320 |  |
| `ji_wanshi_bumie` | 极·万世不灭 | 极 | 16 | 否 | ENTRY | 生命上限 +150%、每 5 秒回复 40% 生命、非护甲减伤 +40% | TianfuTalents.java:327 |  |
| `ji_zhongdao_zhizun` | 极·众道至尊 | 极 | 16 | 否 | ENTRY | 44 项非汇总道痕各 +3000（效率各 +300%，不直接增伤）、修炼速度 +200%、本体突破失败概率降低 80% | TianfuTalents.java:329 |  |
| `zhenyuan_kujie` | 真元枯竭 | 负 | -2 | 是 | ENTRY | 真元回复 −30% | TianfuTalents.java:333 |  |
| `niantou_ganhe` | 念头干涸 | 负 | -2 | 是 | ENTRY | 念头回复 −30% | TianfuTalents.java:334 |  |
| `routi_chanruo` | 肉体孱弱 | 负 | -2 | 是 | ENTRY | 生命上限 −20% | TianfuTalents.java:335 |  |
| `quanshi_piruan` | 拳势疲软 | 负 | -2 | 是 | ENTRY | 攻击伤害 −10% | TianfuTalents.java:336 |  |
| `shenfa_chidun` | 身法迟钝 | 负 | -2 | 是 | ENTRY | 移动速度 −8% | TianfuTalents.java:337 |  |
| `shouyuan_duancu` | 寿元短促 | 负 | -2 | 是 | ENTRY | 寿元 −100 | TianfuTalents.java:338 |  |
| `qiyun_xibo` | 气运稀薄 | 负 | -2 | 是 | ENTRY | 基础气运上限 −15%（零上限保持零） | TianfuTalents.java:339 |  |
| `hunli_shuairuo` | 魂力衰弱 | 负 | -2 | 是 | ENTRY | 魂魄上限 −25%、承载极限 −25% | TianfuTalents.java:340 |  |
| `nianfu_xiayai` | 念府狭隘 | 负 | -2 | 是 | ENTRY | 念头容量 −25% | TianfuTalents.java:342 |  |
| `xiuxing_chizhi` | 修行迟滞 | 负 | -2 | 是 | ENTRY | 修炼速度 −30%、本体突破失败概率提高 40% | TianfuTalents.java:343 |  |

### A.6 恶意联动词条（TianfuTalents.MALICE，需 l2hostility）

条目数：**14**

| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |
|---|---|---|---|---|---|---|---|---|
| `malice_moonwalk` | 踏虚而行 | 黄 | 1 | 否 | ENTRY | 摔落伤害 −60%（恶意·月步） | TianfuTalents.java:367 |  |
| `malice_slowness` | 流沙缚足 | 黄 | 1 | 否 | ENTRY | 攻击命中使目标缓慢 II，5 秒（恶意·流沙） | TianfuTalents.java:369 |  |
| `malice_poison` | 毒蚀之能 | 玄 | 2 | 否 | ENTRY | 攻击命中使目标中毒 II，5 秒（恶意·剧毒） | TianfuTalents.java:373 |  |
| `malice_freezing` | 寒流侵体 | 玄 | 2 | 否 | ENTRY | 攻击命中使目标缓慢 II，5 秒（恶意·寒流） | TianfuTalents.java:375 |  |
| `malice_weakness` | 卸力之能 | 玄 | 2 | 否 | ENTRY | 攻击命中使目标虚弱 II，5 秒（恶意·虚弱） | TianfuTalents.java:377 |  |
| `malice_blindness` | 夺明之术 | 玄 | 2 | 否 | ENTRY | 攻击命中使目标致盲 3 秒（恶意·致盲） | TianfuTalents.java:379 |  |
| `malice_reflect` | 镜影反照 | 地 | 3 | 否 | ENTRY | 受击时 15% 概率把 30% 伤害以魔法形式反弹（恶意·反射） | TianfuTalents.java:383 |  |
| `malice_counter` | 反戈一击 | 地 | 3 | 否 | ENTRY | 被击中后 3 秒内攻击伤害 +15%（恶意·反击） | TianfuTalents.java:385 |  |
| `malice_wither` | 枯荣之力 | 地 | 3 | 否 | ENTRY | 攻击命中使目标凋零 II，5 秒（恶意·凋零） | TianfuTalents.java:387 |  |
| `malice_dementor` | 摄魂夺魄 | 天 | 5 | 否 | ENTRY | 受到的「吃护甲」类伤害（近战/弹射物/爆炸）−15%（恶意·摄魂） | TianfuTalents.java:391 |  |
| `malice_dispell` | 破法之瞳 | 天 | 5 | 否 | ENTRY | 受到的「穿护甲」类伤害（魔法/药水/虚空）−15%（恶意·破魔） | TianfuTalents.java:393 |  |
| `malice_adaptive` | 随境应变 | 天 | 5 | 否 | ENTRY | 每次受同类伤害再减伤 2%，同类最多叠 10 层（恶意·适应） | TianfuTalents.java:395 |  |
| `malice_killer_aura` | 杀意凛然 | 天 | 5 | 否 | ENTRY | 每 3 秒对 4 格内敌对生物造成 5% 攻击力的魔法伤害（恶意·杀戮光环） | TianfuTalents.java:397 |  |
| `malice_undying` | 不死之身 | 神 | 12 | 否 | ENTRY | 受到致命伤害时免死并回复 50% 生命，冷却 300 秒（恶意·不死） | TianfuTalents.java:401 |  |

## B. 统计（脚本实际数）

### B.1 总量

| 口径 | 实际数 | 说明 |
|---|---|---|
| TianfuPool 内置词条 | 21 | `entry(...)` 字面量 |
| TianfuTalents 扩展词条（BASE） | 152 | `t(...)` 字面量 |
| 词条合计（内置 + 扩展） | **173** | 与「133 = 22 + 111」口径对比 |
| 凡体（BODY） | 20 | |
| 十绝体 + 杀神（SIJUE） | 11 | 含 1 条杀神 |
| 机缘（JIYUAN） | 4 | |
| **可购合计（词条 + 凡体 + 十绝体 + 机缘）** | **208** | 与「167」口径对比 |
| 可购合计（不把杀神算进十绝体） | 207 | 历史口径 167 + 40 |
| 恶意联动词条（MALICE，独立分页） | 14 | 不进主池，单独页面 |
| **源码中条目总数** | **222** | 含恶意组 |
| 运行期停用（NATIVE_UNAVAILABLE） | 3 | 实测无法兑现，被 `available()` 摘出池子 |

> 上表「可购合计」是**源码字面量**口径，未扣运行期过滤。
> 运行期真正能买到的是 `可购合计 − NATIVE_UNAVAILABLE − 未装附属的联动条目`：
> 缺 `l2hostility` 时恶意组 14 条整页不可见；缺 `guzhenren_addon`/`hundao_addon`/`yundao`
> 时 `bingxue_daoti`/`hundao_ronghe`/`qiyun_yizhi`/`shen_liupai_jianji` 共 4 条被摘除。

### B.2 Kind 分布

| Kind | 条数 |
|---|---|
| BODY | 20 |
| ENTRY | 187 |
| SIJUE | 11 |
| JIYUAN | 4 |
| 合计 | 222 |

### B.3 品质分布

| 品质 | 条数 | 其中负成本 |
|---|---|---|
| 极 | 3 | 0 |
| 神 | 10 | 0 |
| 天 | 29 | 0 |
| 地 | 34 | 0 |
| 玄 | 48 | 0 |
| 黄 | 41 | 0 |
| 负 | 20 | 20 |
| 其它 | 37 | 0 |
| 合计 | 222 | 20 |

### B.4 cost 分布

| cost | 条数 | 条目 id |
|---|---|---|
| -8 | 3 | `v2_aptitude_shackled`, `v2_luck_forfeit`, `v2_lifespan_scar` |
| -5 | 5 | `v2_zhenyuan_kubai`, `v2_routi_canque`, `v2_shouyuan_kuwei`, `v2_qiyun_duanjue`, `v2_xinzhi_yousun` |
| -2 | 12 | `v2_jingli_buji`, `v2_gu_cd_ningse`, `zhenyuan_kujie`, `niantou_ganhe`, `routi_chanruo`, `quanshi_piruan`, `shenfa_chidun`, `shouyuan_duancu`, `qiyun_xibo`, `hunli_shuairuo`, `nianfu_xiayai`, `xiuxing_chizhi` |
| 1 | 41 | `shouyuan_mianchang`, `qiyun_weiguang`, `jingu_qiangjian`, `baoji_jingtong`, `zhanyi_angran`, `youshou_nengkai`, `v2_apt_ding`, `yudao_qinhe`, `qidao_qinhe`, `lvdao_qinhe`, `toudao_qinhe`, `nudao_qinhe` …（共 41 条） |
| 2 | 47 | `zhenyuan_ninglian`, `niantou_ninglian`, `xinshen_ruyuan`, `kuxiu_buchuo`, `houji_bofa`, `suiyue_chendian`, `youdou_gaoshou`, `jianbi_qingfeng`, `v2_mind_xiulian_1`, `v2_mind_capacity_1`, `v2_mind_recovery_1`, `v2_mind_liangu_1` …（共 47 条） |
| 3 | 55 | `tudao`, `bingdao`, `bingxuedao`, `fengdao`, `gudao`, `hundao`, `jiandao`, `jindao`, `lidao`, `mudao`, `qidao`, `xiuluo` …（共 55 条） |
| 5 | 29 | `suhui`, `jiyuan_bing`, `v2_mind_xiulian_3`, `v2_mind_capacity_3`, `tiandao_daocang`, `zonghen_chubei`, `wuxing_daocang`, `qiqiao_linglong`, `hunpo_rugui`, `jingli_ruhai`, `shouyuan_ruhe`, `qiyun_ruhong` …（共 29 条） |
| 6 | 1 | `v2_apt_yi` |
| 8 | 1 | `jiyuan_yi` |
| 10 | 1 | `v2_apt_jia` |
| 12 | 13 | `shen_tianji_suan`, `shen_bumie_jinshen`, `jiyuan_jia`, `shen_qiyun_zhizi`, `shen_hunxi_zhuzai`, `shen_busi_bumie`, `shen_zhanshen_linshi`, `shen_jingang_buhuai`, `shen_xiulian_wuji`, `shen_liangu_tianshu`, `shen_wandao_guizong`, `shen_liupai_jianji` …（共 13 条） |
| 16 | 14 | `wanfa_jietong`, `sijue_1`, `sijue_2`, `sijue_3`, `sijue_4`, `sijue_5`, `sijue_6`, `sijue_7`, `sijue_8`, `sijue_9`, `sijue_10`, `shashen` …（共 14 条） |

### B.5 负成本条目清单（买它反而返还点数）

| id | 中文名 | cost | 品质 | 描述原文 | 定义位置 |
|---|---|---|---|---|---|
| `v2_jingli_buji` | 精力不济 | -2 | 负 | 精力上限 −20% | TianfuTalents.java:150 |
| `v2_gu_cd_ningse` | 催蛊凝涩 | -2 | 负 | 蛊冷却每 tick 额外增加 0.10，仅对正在冷却的蛊 | TianfuTalents.java:151 |
| `v2_zhenyuan_kubai` | 真元枯败 | -5 | 负 | 真元回复 −75%（旧 −30% 的 2.5 倍） | TianfuTalents.java:152 |
| `v2_routi_canque` | 肉体残缺 | -5 | 负 | 生命上限 −50%（旧 −20% 的 2.5 倍） | TianfuTalents.java:153 |
| `v2_shouyuan_kuwei` | 寿元枯萎 | -5 | 负 | 寿元 −250（旧 −100 的 2.5 倍，确认前显示致死风险） | TianfuTalents.java:154 |
| `v2_qiyun_duanjue` | 气运断绝 | -5 | 负 | 基础气运上限 −37.5%（旧 −15% 的 2.5 倍） | TianfuTalents.java:155 |
| `v2_xinzhi_yousun` | 心智有损 | -5 | 负 | 修炼速度 −40%、本体突破失败概率提高 50% | TianfuTalents.java:156 |
| `v2_aptitude_shackled` | 空窍桎梏 | -8 | 负 | 本世锁定确认时原有资质；念头容量 −100%，不允许后续机缘或十绝体突破锁定 | TianfuTalents.java:157 |
| `v2_luck_forfeit` | 天弃之命 | -8 | 负 | 本世气运与气运上限持续为 0；气运增益不能抵消，约四倍量级并附不可补回代价 | TianfuTalents.java:158 |
| `v2_lifespan_scar` | 命烛残尽 | -8 | 负 | 确认时寿元 −400，且本世寿元恢复与增加无效；寿元不足不得确认 | TianfuTalents.java:159 |
| `zhenyuan_kujie` | 真元枯竭 | -2 | 负 | 真元回复 −30% | TianfuTalents.java:333 |
| `niantou_ganhe` | 念头干涸 | -2 | 负 | 念头回复 −30% | TianfuTalents.java:334 |
| `routi_chanruo` | 肉体孱弱 | -2 | 负 | 生命上限 −20% | TianfuTalents.java:335 |
| `quanshi_piruan` | 拳势疲软 | -2 | 负 | 攻击伤害 −10% | TianfuTalents.java:336 |
| `shenfa_chidun` | 身法迟钝 | -2 | 负 | 移动速度 −8% | TianfuTalents.java:337 |
| `shouyuan_duancu` | 寿元短促 | -2 | 负 | 寿元 −100 | TianfuTalents.java:338 |
| `qiyun_xibo` | 气运稀薄 | -2 | 负 | 基础气运上限 −15%（零上限保持零） | TianfuTalents.java:339 |
| `hunli_shuairuo` | 魂力衰弱 | -2 | 负 | 魂魄上限 −25%、承载极限 −25% | TianfuTalents.java:340 |
| `nianfu_xiayai` | 念府狭隘 | -2 | 负 | 念头容量 −25% | TianfuTalents.java:342 |
| `xiuxing_chizhi` | 修行迟滞 | -2 | 负 | 修炼速度 −30%、本体突破失败概率提高 40% | TianfuTalents.java:343 |

负成本合计返还点数：**73**

### B.6 重复检查

- 重复 id：**0 处**（222 个 id 全部唯一）

- 重复中文名：**0 处**（222 个名字全部唯一）

### B.7 运行期停用清单（NATIVE_UNAVAILABLE）

这些 id 在 `TianfuTalents.NATIVE_UNAVAILABLE` 里，`available()` 会返回 false，
`specs()` 因此把它们从词条池中摘掉 —— **玩家在 1.4.17 界面上看不到也买不到它们**。

| id | 停用原因 | 是否真在词条表内 |
|---|---|---|
| `zhenyuan_ninglian` | 本体 9.8 未提供统一真元扣费入口，尚不能完整兑现降耗；仅保留旧记录 | 是（TianfuPool.java:71） |
| `niantou_ninglian` | 本体 9.8 未提供统一念头扣费入口，尚不能完整兑现降耗；仅保留旧记录 | 是（TianfuPool.java:72） |
| `shazhao_tiancheng` | 杀招成功率与槽位磨损尚无完整的 9.8 钩子；仅保留旧记录 | 是（TianfuPool.java:68） |

### B.8 分组 × 品质 交叉表（核对源码注释里「黄 20+5 条」这类声明）

| 分组 | 极 | 神 | 天 | 地 | 玄 | 黄 | 负 | 其它 | 合计 |
|---|---|---|---|---|---|---|---|---|
| 凡体 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 20 |
| 内置词条 | 1 | 0 | 1 | 3 | 8 | 6 | 0 | 2 | 21 |
| 十绝体 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 11 | 11 |
| 机缘 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 4 | 4 |
| 扩展词条 | 2 | 9 | 24 | 28 | 36 | 33 | 20 | 0 | 152 |
| 恶意联动 | 0 | 1 | 4 | 3 | 4 | 2 | 0 | 0 | 14 |

## C. 与历史口径的差额（必须写明的部分）

| 口径项 | 历史值 | 脚本实际 | 差额 |
|---|---|---|---|
| 内置词条 | 22 | **21** | -1 |
| 扩展词条 | 111 | **152** | +41 |
| 词条合计 | 133 | **173** | +40 |
| 凡体 | 20 | **20** | +0 |
| 十绝体 | 10 | **10** | +0 |
| 机缘 | 4 | **4** | +0 |
| 可购合计 | 167 | **208** | +41 |

> 注：「十绝体」历史口径按 10 计，脚本实测 `SIJUES` 含 1 条**杀神**（`shashen`，tizhi=54），
> 故 Kind=SIJUE 共 11 条。上表「十绝体」行的实际值已按 11 − 1 计。
> 「可购合计」实际值含杀神；不含杀神时为 207。

### C.1 源码注释里写死的条目数（已过期，脚本核对）

源码多处 javadoc 仍写着旧计数。脚本把它们抓出来与实际数并列：

| 位置 | 注释里写的数 | 声称的对象 | 脚本实测 | 判定 |
|---|---|---|---|---|
| TianfuTalents.java:22 | 107 | 扩展词条 | **152** | **过期**（差 +45） |
| TianfuTalents.java:23 | 107 | 扩展词条 | **152** | **过期**（差 +45） |
| TianfuTalents.java:32 | 7 | 内置词条 | **21** | **过期**（差 +14） |
| TianfuPool.java:10 | 129 | 词条合计 | **173** | **过期**（差 +44） |
| TianfuPool.java:10 | 22 | 内置词条 | **21** | **过期**（差 -1） |
| TianfuPool.java:10 | 107 | 扩展词条 | **152** | **过期**（差 +45） |

> 逐档品质分组的注释声明请对照 **B.8 交叉表** 核对（脚本给出的是实测值）。

## D. 附录 · 效果原语明细（脚本从 fx 参数解析）

用于核对「描述写的 X」与「代码实际落的 Y」是否一致。

| id | 效果原语 |
|---|---|
| `v2_apt_ding` | Op.APTITUDE_SET=8 |
| `v2_apt_bing` | Op.APTITUDE_SET=6 |
| `v2_apt_yi` | Op.APTITUDE_SET=4 |
| `v2_apt_jia` | Op.APTITUDE_SET=2 |
| `v2_jingli_buji` | FIELD_MULT:zuida_jingli*-0.20 |
| `v2_gu_cd_ningse` | Op.GU_CD=-0.10 |
| `v2_zhenyuan_kubai` | Op.REGEN_ZY=-0.75 |
| `v2_routi_canque` | Op.HEALTH_MULT=-0.50 |
| `v2_shouyuan_kuwei` | FIELD_ADD:shouyuan+-250 |
| `v2_qiyun_duanjue` | FIELD_MULT:qiyun_shangxian*-0.375 |
| `v2_xinzhi_yousun` | Op.XIULIAN=-0.40; Op.BREAKTHROUGH=-0.50 |
| `v2_aptitude_shackled` | Op.V2_APTITUDE_LOCK=1; FIELD_MULT:niantou_rongliang*-1.0 |
| `v2_luck_forfeit` | Op.V2_LUCK_ZERO=1 |
| `v2_lifespan_scar` | FIELD_ADD:shouyuan+-400; Op.V2_LIFESPAN_SCAR=1 |
| `v2_mind_xiulian_1` | Op.XIULIAN=0.20 |
| `v2_mind_xiulian_2` | Op.XIULIAN=0.40 |
| `v2_mind_xiulian_3` | Op.XIULIAN=0.60 |
| `v2_mind_capacity_1` | FIELD_MULT:niantou_rongliang*0.20 |
| `v2_mind_capacity_2` | FIELD_MULT:niantou_rongliang*0.30 |
| `v2_mind_capacity_3` | FIELD_MULT:niantou_rongliang*0.50 |
| `v2_mind_recovery_1` | Op.REGEN_NT=0.20 |
| `v2_mind_recovery_2` | Op.REGEN_NT=0.40 |
| `v2_mind_liangu_1` | Op.LIANGU=0.15 |
| `v2_mind_liangu_2` | Op.LIANGU=0.30 |
| `v2_mind_cooldown_1` | Op.GU_CD=0.15 |
| `v2_mind_cooldown_2` | Op.GU_CD=0.25 |
| `yudao_qinhe` | DAOHEN:daohen_yudao+200 |
| `qidao_qinhe` | DAOHEN:dahen_qidao+200 |
| `lvdao_qinhe` | DAOHEN:daohen_lvdao+200 |
| `toudao_qinhe` | DAOHEN:daohen_toudao+200 |
| `nudao_qinhe` | DAOHEN:dahen_nudao+200 |
| `huadao_qinhe` | DAOHEN:daohen_huadao+200 |
| `andao_qinhe` | DAOHEN:daohen_andao+200 |
| `shidao_qinhe` | DAOHEN:daohen_shidao+500 |
| `xindao_qinhe` | DAOHEN:daohen_xindao+500 |
| `yundao_qinhe` | DAOHEN:daohen_yundao+500 |
| `yundao2_jinjie` | DAOHEN:daohen_yundao2+500 |
| `jindao2_jinjie` | DAOHEN:daohen_jindao2+500 |
| `rendao_dacheng` | DAOHEN:dahen_rendao+800 |
| `tiandao_daocang` | DAOHEN:dahen_tiandao+1200 |
| `zonghen_chubei` | FIELD_ADD:daohen_zong+300 |
| `jiandao_qinhe` | DAOHEN:daohen_jiandao+200 |
| `leidao_qinhe` | DAOHEN:daohen_leidao+200 |
| `yandao_qinhe` | DAOHEN:daohen_yandao+200 |
| `bingxuedao_qinhe` | DAOHEN:daohen_bingxuedao+200 |
| `fengdao_qinhe` | DAOHEN:daohen_fengdao+200 |
| `yingdao_qinhe` | DAOHEN:daohen_yingdao+200 |
| `yuedao_qinhe` | DAOHEN:daohen_yuedao+200 |
| `shuidao_qinhe` | DAOHEN:daohen_shuidao+200 |
| `tudao_qinhe` | DAOHEN:daohen_tudao+200 |
| `mudao_qinhe` | DAOHEN:daohen_mudao+200 |
| `jindao_qinhe` | DAOHEN:daohen_jindao+200 |
| `gudao_qinhe` | DAOHEN:daohen_gudao+200 |
| `lidao_qinhe` | DAOHEN:daohen_lidao+200 |
| `hundao_qinhe` | DAOHEN:daohen_hundao+200 |
| `xuedao_qinhe` | DAOHEN:daohen_xuedao+200 |
| `xudao_qinhe` | DAOHEN:daohen_xudao+200 |
| `zhidao_qinhe` | DAOHEN:dahen_zhidao+200 |
| `zhoudao_qinhe` | DAOHEN:dahen_zhoudao+200 |
| `mengdao_qinhe` | DAOHEN:daohen_mengdao+200 |
| `dudao_qinhe` | DAOHEN:daohen_dudao+200 |
| `tiepi_rougu` | Op.REDUCTION=0.04 |
| `bufen_zhiwu` | MOB:fire_resistance amp=0 |
| `shenfa_qingjie` | Op.SPEED_MULT=0.05 |
| `zhaoqi_yinlu` | Op.REGEN_HP_5S=0.01 |
| `niantou_zisheng` | Op.REGEN_NT=0.20 |
| `bingdao_qinhe` | DAOHEN:daohen_bingdao+500 |
| `daodao_qinhe` | DAOHEN:daohen_daodao+500 |
| `guangdao_qinhe` | DAOHEN:daohen_guangdao+500 |
| `xingdao_qinhe` | DAOHEN:dahen_xingdao+500 |
| `dandao_qinhe` | DAOHEN:daohen_dandao+500 |
| `liandao_qinhe` | DAOHEN:dahen_liandao+500 |
| `yindao_qinhe` | DAOHEN:daohen_yindao+500 |
| `huandao_qinhe` | DAOHEN:daohen_huandao+500 |
| `bianhuadao_qinhe` | DAOHEN:daohen_bianhuadao+500 |
| `zhendao_qinhe` | DAOHEN:dahen_zhendao+500 |
| `wuying_bi` | reflect(0.06,0.12) |
| `ruwei_dingshen` | Op.REDUCTION=0.08 |
| `shanbu_suifeng` | Op.DODGE=0.06 |
| `xueqi_rulin` | Op.HEALTH_MULT=0.20 |
| `lijin_ruquan` | Op.ATTACK_MULT=0.08 |
| `quanru_jifeng` | Op.ATTACK_SPEED_MULT=0.10 |
| `songyue_jianli` | Op.KNOCKBACK_FLAT=0.4 |
| `jingli_shengheng` | FIELD_MULT:zuida_jingli*0.30 |
| `chuntian_yishou` | FIELD_ADD:shouyuan+80 |
| `qiyun_weiying` | FIELD_MULT:qiyun_shangxian*0.12 |
| `qiyun_yingmen` | FIELD_MULT:qiyun_shangxian*0.18 |
| `hunpo_lianxi` | FIELD_MULT:zuida_hunpo*0.15 |
| `hunzai_kuanhou` | FIELD_MULT:hunpochengzaijixian*0.15 |
| `niantou_kuozhan` | FIELD_MULT:niantou_rongliang*0.15 |
| `zhenyuan_huiliu` | Op.REGEN_ZY=0.35 |
| `tupo_tongtian` | Op.BREAKTHROUGH=0.30 |
| `dazuo_wangwo` | Op.XIULIAN=0.40 |
| `liangu_rushen` | Op.LIANGU=0.40 |
| `gudao_dup` | Op.GU_CD=0.25 |
| `shazhao_rufeng` | Op.SHAZHAO_CD=0.25 |
| `zhenyuan_ruquan` | Op.REGEN_ZY=0.80 |
| `niantou_ruyuan` | Op.REGEN_NT=0.60 |
| `xieshi_ziyu` | Op.REGEN_HP_5S=0.03 |
| `shenfa_ruyan` | Op.DODGE=0.12 |
| `baoji_rulie` | Op.CRIT_CHANCE=0.15 |
| `baoshang_rufeng` | Op.CRIT_DAMAGE=0.80 |
| `xixue_rumo` | Op.LEECH=0.06 |
| `fanshang_ruyu` | reflect(0.10,0.20) |
| `jianjia_ruyan` | Op.REDUCTION=0.15 |
| `quansheng_ruhong` | Op.OUT_FULL=0.18 |
| `canshen_rulang` | Op.OUT_LOW=0.25 |
| `wanjun_lili` | MOB:strength amp=1 |
| `shengji_rumu` | MOB:regeneration amp=1 |
| `jiandao_dacheng` | DAOHEN:daohen_jiandao+800 |
| `leidao_dacheng` | DAOHEN:daohen_leidao+800 |
| `lidao_dacheng` | DAOHEN:daohen_lidao+800 |
| `wuxing_daocang` | Op.DAOHEN_ALL=300 |
| `qiqiao_linglong` | FIELD_MULT:niantou_rongliang*0.50; FIELD_MULT:niantou_zhida*0.50 |
| `hunpo_rugui` | FIELD_MULT:zuida_hunpo*0.50; FIELD_MULT:hunpochengzaijixian*0.50 |
| `jingli_ruhai` | FIELD_MULT:zuida_jingli*0.80 |
| `shouyuan_ruhe` | FIELD_ADD:shouyuan+200 |
| `qiyun_ruhong` | FIELD_MULT:qiyun_shangxian*0.40 |
| `zhenyuan_bujie` | Op.REGEN_ZY=1.50 |
| `niantou_bujie` | Op.REGEN_NT=1.20 |
| `huixue_rusheng` | Op.REGEN_HP_5S=0.05 |
| `baoji_rushen` | Op.CRIT_CHANCE=0.25; Op.CRIT_DAMAGE=1.20 |
| `xixue_rushi` | Op.LEECH=0.10 |
| `fanshang_rushan` | reflect(0.20,0.35) |
| `jianjia_rushen` | Op.REDUCTION=0.25 |
| `xiulian_rushen` | Op.XIULIAN=0.60; Op.BREAKTHROUGH=0.40 |
| `liangu_bujie` | Op.LIANGU=0.60; Op.GU_CD=0.30 |
| `jixing_ruying` | MOB:speed amp=2 |
| `bingxue_daoti` | MOB:guzhenren_addon:ice_path_boost amp=0 |
| `hundao_ronghe` | MOB:hundao_addon:hundao_zengfu amp=0 |
| `qiyun_yizhi` | MOB:yundao:yizhi_qiankun amp=0; FIELD_MULT:qiyun_shangxian*0.30 |
| `shen_qiyun_zhizi` | FIELD_MULT:qiyun_shangxian*0.80 |
| `shen_hunxi_zhuzai` | FIELD_MULT:zuida_hunpo*1.0; FIELD_MULT:hunpochengzaijixian*1.0; FIELD_MULT:niantou_rongliang*1.0 |
| `shen_busi_bumie` | Op.REGEN_HP_5S=0.12; Op.REDUCTION=0.30 |
| `shen_zhanshen_linshi` | Op.ATTACK_MULT=0.60; Op.ATTACK_SPEED_MULT=0.25 |
| `shen_jingang_buhuai` | Op.KNOCKBACK_FLAT=1.0; Op.REDUCTION=0.35 |
| `shen_xiulian_wuji` | Op.XIULIAN=1.50; Op.BREAKTHROUGH=0.60 |
| `shen_liangu_tianshu` | Op.LIANGU=0.90; Op.GU_CD=0.50 |
| `shen_wandao_guizong` | Op.DAOHEN_ALL=1500 |
| `shen_liupai_jianji` | MOB:guzhenren_addon:ice_path_boost amp=0; MOB:hundao_addon:hundao_zengfu amp=0; MOB:yundao:yizhi_qiankun amp=0 |
| `ji_wanshi_bumie` | Op.HEALTH_MULT=1.50; Op.REGEN_HP_5S=0.40; Op.REDUCTION=0.40 |
| `ji_zhongdao_zhizun` | Op.DAOHEN_ALL=3000; Op.XIULIAN=2.00; Op.BREAKTHROUGH=0.80 |
| `zhenyuan_kujie` | Op.REGEN_ZY=-0.30 |
| `niantou_ganhe` | Op.REGEN_NT=-0.30 |
| `routi_chanruo` | Op.HEALTH_MULT=-0.20 |
| `quanshi_piruan` | Op.ATTACK_MULT=-0.10 |
| `shenfa_chidun` | Op.SPEED_MULT=-0.08 |
| `shouyuan_duancu` | FIELD_ADD:shouyuan+-100 |
| `qiyun_xibo` | FIELD_MULT:qiyun_shangxian*-0.15 |
| `hunli_shuairuo` | FIELD_MULT:zuida_hunpo*-0.25; FIELD_MULT:hunpochengzaijixian*-0.25 |
| `nianfu_xiayai` | FIELD_MULT:niantou_rongliang*-0.25 |
| `xiuxing_chizhi` | Op.XIULIAN=-0.30; Op.BREAKTHROUGH=-0.40 |
| `malice_moonwalk` | Op.FALL_REDUCE=0.60 |
| `malice_slowness` | onHit(minecraft:slowness,100,1) |
| `malice_poison` | onHit(minecraft:poison,100,1) |
| `malice_freezing` | onHit(minecraft:slowness,100,1) |
| `malice_weakness` | onHit(minecraft:weakness,100,1) |
| `malice_blindness` | onHit(minecraft:blindness,60,0) |
| `malice_reflect` | reflect(0.15,0.30) |
| `malice_counter` | counter(0.15,60) |
| `malice_wither` | onHit(minecraft:wither,100,1) |
| `malice_dementor` | Op.REDUCTION_PHYS=0.15 |
| `malice_dispell` | Op.REDUCTION_MAGIC=0.15 |
| `malice_adaptive` | adaptive(0.02,10) |
| `malice_killer_aura` | aura(4.0,0.05) |
| `malice_undying` | undying(0.50,6000) |

内置词条的效果不在 `TianfuTalents` 的效果原语表内 —— 它们是**硬编码**分发在
`TianfuCombat` / `TianfuEffects` / `TianfuAttributes` 里的 `ids.contains("...")` 分支，
清单脚本无法从数据表推断，必须在代码里逐条核对（见第二部分审查）。

<!-- DUMP_END -->

---

# 第二部分 · 战斗 / 生存 / 资源三类天赋审查

## 1. 审查范围、方法与证据等级

### 1.1 判定链（每一类都走同一条链）

```
天赋 id
  → 数据结构（TianfuTalents.Op 原语 / TianfuPool 硬编码分支）
  → 落地通道
      ├─ 属性通道   TianfuAttributes.sync      → AttributeModifier(ADD_MULTIPLIED_TOTAL / ADD_VALUE)
      ├─ 战斗通道   TianfuCombat.onIncomingDamage → LivingIncomingDamageEvent 乘区
      ├─ 周期通道   TianfuEffects.onTick        → 每 tick / 每 5 秒 / 每 20 tick
      ├─ 本体字段   TianfuTalents.applyOnce     → PlayerVariables 反射写入
      └─ 本体钩子   mixin（NativeRecoveryMixin / NativeItemCooldownMixin）
  → 是否有上限 / 是否可叠 / 描述是否兑现
```

### 1.2 证据等级（本报告全文统一）

| 标记 | 含义 |
|---|---|
| **已验证** | 有源码行号或 jar 字节码指令作为直接证据 |
| **未验证** | 需要实机（启动日志 / 伤害数字对比）才能确认，已在第 9 章集中列出 |

### 1.3 三条全局闸门（先看这个，否则会误判「买了没效果」）

| 闸门 | 位置 | 后果 |
|---|---|---|
| 生效闸门 | `TianfuApply.effective` `TianfuApply.java:63-74` | 未「最终确认」时返回空集 → **所有词条加成一律不生效**，属性也不提前生效（`TianfuAttributes.java:37`） |
| 运行期停用表 | `TianfuTalents.java:40-43` `NATIVE_UNAVAILABLE` | 3 条词条被 `available()`（`:562-570`）摘出池子，**界面看不到、买不到** |
| 附属缺席过滤 | `TianfuTalents.java:580-593` `modLoadedFor` | 未装 `guzhenren_addon` / `hundao_addon` / `yundao` 时，4 条联动词条整条摘除 |

---

## 2. 战斗输出类

### 2.1 攻击力（`Op.ATTACK_MULT` / `ADD_MULTIPLIED_TOTAL`）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `lijin_ruquan` | 力进如泉 | 2（玄） | `TianfuTalents.java:234` `f(Op.ATTACK_MULT, 0.08)` → `attrs()` `:610` `attack *= 1.0 + fx.a()` → `TianfuAttributes.java:64,68` `ATTACK_DAMAGE ADD_MULTIPLIED_TOTAL` | **可叠（乘法）** | 一致 | 4%/点 |
| `shen_zhanshen_linshi` | 神·战神临世 | 12（神） | `TianfuTalents.java:311-312` `ATTACK_MULT 0.60` + `ATTACK_SPEED_MULT 0.25` | **可叠** | 一致 | 5%/点 + 攻速 |
| `quanshi_piruan` | 拳势疲软 | −2（负） | `TianfuTalents.java:336` `ATTACK_MULT −0.10` → `attack *= 0.90` | 可叠（会稀释正向） | 一致 | 返还 2 点 |

补充：`搬山撼岳体`（`tizhi=28`）另有 `TianfuAttributes.java:48-50` `attack *= 1.0 + scale(daohen_lidao, 0.12)`，即**每 1000 力道道痕 +12% 攻击**，与上表乘法叠加（`TianfuDaohen.java:26-28`）。

**判定**：攻击力这一维度**代码与描述完全一致、无上限、纯乘法叠加**。乘法叠加意味着 `力进如泉 ×1.08` 与 `神·战神临世 ×1.60` 不是 +68% 而是 ×1.728 —— 但相对误差不大，不构成问题。

### 2.2 攻击速度（`Op.ATTACK_SPEED_MULT`）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `quanru_jifeng` | 拳如疾风 | 2（玄） | `TianfuTalents.java:235` `ATTACK_SPEED_MULT 0.10` → `TianfuAttributes.java:70-71` `ATTACK_SPEED ADD_MULTIPLIED_TOTAL` | **可叠（乘法）** | 一致 | 5%/点 |
| `shen_zhanshen_linshi` | 神·战神临世 | 12 | 同上 `0.25` | 可叠 | 一致 | 同上 |

**判定**：`拳如疾风 ×1.10` × `神·战神临世 ×1.25` = ×1.375。原版 `ATTACK_SPEED` 影响攻击冷却，在冷却未归零前 DPS 提升近似线性，无超模。全表无「攻速」负分条目。

### 2.3 暴击率（`Op.CRIT_CHANCE` + 硬编码）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `baoji_jingtong` | 暴击精通 | 1（黄） | `TianfuCombat.java:266` `if (ids.contains("baoji_jingtong")) chance += 0.05;`（**硬编码，不在数据表**） | **可叠** | 一致 | ≈6%/点（含暴伤） |
| `baoji_rulie` | 暴击如裂 | 3（地） | `TianfuTalents.java:257` `CRIT_CHANCE 0.15` → `TianfuCombat.java:268` `chance += critChance(ids)` → `TianfuTalents.java:802-804` `sum()` | **可叠（加性）** | **未注明 50% 上限** | 5%/点 |
| `baoji_rushen` | 暴击如神 | 5（天） | `TianfuTalents.java:283-284` `CRIT_CHANCE 0.25` + `CRIT_DAMAGE 1.20` | **可叠** | **未注明上限** | ≈11%/点 |
| `shen_tianji_suan` | 神·天机神算 | 12（神） | `TianfuCombat.java:267` `chance += 0.50;`（**硬编码**） | 可叠但**必达上限** | **未注明上限** | 见 2.4 |

**上限证据**：`TianfuCombat.java:38` `CRIT_CAP = 0.50`；`:269-270` `return player.getRandom().nextDouble() < Math.min(chance, CRIT_CAP);`

**关键推论（已验证）**：`神·天机神算` 单条 +0.50 **恰好等于 `CRIT_CAP`** → 该条目的暴击率部分是**100% 必暴**。此时再买 `暴击如裂` 的 +15% 暴击率 **完全浪费**（溢出不生效），只剩 `暴击如神` 的暴伤部分仍有效。

**同类可叠清单**：最大名义值 `0.05 + 0.15 + 0.25 + 0.50 = 0.95` → 实际封顶 **0.50**。这 4 条**不在任何互斥族内**（`TianfuMindTable.java:11-23` 只有 xiulian/capacity/recovery/liangu/cooldown 五族）。

### 2.4 暴击伤害（`Op.CRIT_DAMAGE` + 硬编码）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `baoji_jingtong` | 暴击精通 | 1 | `TianfuCombat.java:276` `mult += 0.20`（硬编码） | **可叠** | 一致 | — |
| `jianbi_qingfeng` | 剑指偏锋 | 2（玄） | `TianfuCombat.java:277` `if (ids.contains("jianbi_qingfeng")) mult += 0.30;` | **可叠** | **不一致**：描述「需配合暴击精通」，代码**无任何门禁**，有暴击就加 | **最差：0.75%/点** |
| `baoshang_rufeng` | 暴伤如锋 | 3（地） | `TianfuTalents.java:258` `CRIT_DAMAGE 0.80` → `TianfuCombat.java:279` `mult += critDamage(ids)` | **可叠（加性）** | 一致 | 视暴击率 4%~24%/点 |
| `baoji_rushen` | 暴击如神 | 5 | 同上 `1.20` | 可叠 | 未注明上限 | — |
| `shen_tianji_suan` | 神·天机神算 | 12 | `TianfuCombat.java:278` `mult += 2.40`（硬编码） | 可叠 | 一致 | — |

**总倍率证据**：`TianfuCombat.java:275` `double mult = 2.0;`（基础暴击 ×2）→ 逐行累加，`:280` `return mult;`

**最大名义倍率**：`2.0 + 0.20 + 0.30 + 0.80 + 1.20 + 2.40 = 6.90`

**收益/价格判定**：
- `剑指偏锋` 的期望收益 = 暴击率 × 0.30。仅有 `暴击精通`（5%）时 = **+1.5% 总伤 / 2 点 = 0.75%/点**，全表最低。若同时有 `暴击如神`（暴击率 30% 时）= +9% / 2 点 = 4.5%/点，才算合格 → **强烈依赖搭配，单独购买是亏本条目**。
- `暴伤如锋`（3 点）在暴击率 5% 时仅 +4%，在暴击率 30% 时 +24% → 波动极大。

### 2.5 吸血（`Op.LEECH`）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `xixue_rumo` | 吸血入魔 | 3（地） | `TianfuTalents.java:259` `LEECH 0.06` → `TianfuCombat.java:287` `r += leech(ids)` → `:288` `Math.min(r, 0.15)` → `:130-133` `sp.heal(event.getAmount() * leech)` | **可叠但 15% 封顶** | **未注明上限** | 2%/点 |
| `xixue_rushi` | 吸血如饕 | 5（天） | `TianfuTalents.java:285` `LEECH 0.10` | 可叠但封顶 | **未注明上限** | 2%/点 |

**上限证据（已验证）**：`TianfuCombat.java:288` `return Math.min(r, 0.15);`

- 另有 `血煞蜕变体`（`tizhi=53`）`TianfuCombat.java:286` `r += 0.05`。
- **两条同买 = 0.16 → 封顶 0.15，浪费 1%**；若已有血煞体（0.05），`吸血如饕` 一条即 0.15 封顶，`吸血入魔` 完全无收益。
- 吸血基数是**经过全部乘区的最终伤害**（`TianfuCombat.java:130-132` 在 `event.setAmount` 之后读 `event.getAmount()`）→ 与暴击倍率联乘：`神·天机神算` 下 ×4.4 伤害 → 实际每击回 `0.15 × 4.4 = 66%` 攻击力，这就是它超模的第二层原因。

### 2.6 反伤（`Op.REFLECT`）—— **本类问题最集中**

| id | 中文名 | cost | 概率 | 比例 | 乘积 | 描述 vs 代码 |
|---|---|---|---|---|---|---|
| `youshou_nengkai` | 右守能铠 | 1（黄） | 0.04 | 0.10 | 0.0040 | 一致（但见下） |
| `wuying_bi` | 无影壁 | 2（玄） | 0.06 | 0.12 | 0.0072 | 一致 |
| `fanshang_ruyu` | 反伤如玉 | 3（地） | 0.10 | 0.20 | 0.0200 | 一致 |
| `fanshang_rushan` | 反伤如山 | 5（天） | 0.20 | 0.35 | 0.0700 | 一致 |
| `malice_reflect` | 镜影反照 | 3（地） | 0.15 | 0.30 | 0.0450 | 一致 |

**实现证据**：`TianfuTalents.java:928-940` `bestReflect()` —— **取「概率 × 比例」最大的那一条，不做求和**；`TianfuCombat.java:295-300` 同理（与 `右守能铠` 的 0.004 比大小）。

**三个必须指出的问题**：

1. **不叠（已验证）**：5 条反伤买齐，实际只生效乘积最大的 `反伤如山`（0.07）。其余 4 条**收益恰好为 0**。描述里没有任何「同类只取最强」的提示。
2. **只对玩家攻击者生效（已验证）**：`TianfuCombat.java:302`
   ```java
   if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
   ```
   → **对怪物 / BOSS 完全不反弹**。而描述统一写「受击 X% 概率反弹 Y% 伤害」，未注明 PvP 限定。以 PvE 为主的玩家买了 5 条反伤（合计 14 点）**收益为 0**。
3. **反弹走 `thorns` 伤害源**（`:308` `attacker.hurt(victim.damageSources().thorns(victim), reflected)`）并被 `IN_REFLECT` 递归锁保护（`:65`, `:306-311`），此部分实现正确。

### 2.7 满血增伤（`Op.OUT_FULL` + 硬编码）

| id | 中文名 | cost | 实际代码效果 | 同类可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|---|
| `zhanyi_angran` | 战意盎然 | 1（黄） | `TianfuCombat.java:172-175` `if (hpRatio >= 1.0f) { ...; mult *= 1.08; }`（**硬编码**） | **可叠（乘法）** | 一致 | **8%/点（全表最高之一）** |
| `quansheng_ruhong` | 全盛如洪 | 3（地） | `TianfuTalents.java:262` `OUT_FULL 0.18` → `TianfuCombat.java:176` `mult *= 1.0 + outFull(ids)` | **可叠** | 一致 | 6%/点 |

**判定**：
- 判定条件是 `hpRatio >= 1.0f`（**严格满血**），任何一点掉血即失效 → 实战覆盖率低，是设计上的自我平衡。
- 两者叠加 = `1.08 × 1.18 = 1.274`，**可以同时买**，不在任何互斥族内。
- **`战意盎然` 是 1 点换 8%，与 `力进如泉`（2 点换 8%）同效，性价比高一倍** —— 定价不一致的典型。

### 2.8 残血增伤（`Op.OUT_LOW` + 体质阶梯）

| id / 体质 | 触发 | cost | 实际代码效果 | 可叠 | 描述 vs 代码 |
|---|---|---|---|---|---|
| `canshen_rulang` 残身如狼 | `hp < 30%` | 3（地） | `TianfuTalents.java:263` `OUT_LOW 0.25` → `TianfuCombat.java:179-181` `mult *= 1.0 + outLow(ids)` | **可叠** | 一致 |
| `修罗战体` | `tizhi=33` | 3（凡体） | `TianfuCombat.java:184-189` 每损 10% 血 `+5%`，`steps` 上限 5 → **最多 +25%** | 可叠 | 一致 |
| `血煞蜕变体` | `tizhi=53`, `hp<30%` | 3（凡体） | `TianfuCombat.java:190-192` `mult *= 1.12` | 可叠 | 一致 |
| `杀神` | `tizhi=54` | 16 | `TianfuCombat.java:193-195` `mult *= TianfuSlay.multiplier(...)`；`TianfuSlay.java:43-63` 阶梯 **×1.4 / ×1.96 / ×2.6 / ×3.4 / ×4.2 / ×5.0** | 可叠 | 一致 |

**判定**：`残身如狼 (×1.25)` × `血煞蜕变体 (×1.12)` = ×1.40，两个 3 点条目叠出的残血爆发尚可接受。
但 **`杀神` 的 ×5.0 与 `神·天机神算` 的暴击 ×4.4 相乘 = ×22.0**（见第 6 章超模）。

---

## 3. 生存防御类

### 3.1 最大生命（`Op.HEALTH_MULT` / `ADD_MULTIPLIED_TOTAL`）

| id / 体质 | cost | 实际代码效果 | 可叠 | 描述 vs 代码 | 收益/价格 |
|---|---|---|---|---|---|
| `jingu_qiangjian` 筋骨强健 | 1（黄） | `TianfuAttributes.java:57-59` `health *= 1.10`（硬编码） | **可叠（乘法）** | 一致 | **10%/点（全表最高）** |
| `xueqi_rulin` 血气如林 | 2（玄） | `TianfuTalents.java:233` `HEALTH_MULT 0.20` → `TianfuAttributes.java:63` | 可叠 | 一致 | 10%/点 |
| `shen_bumie_jinshen` 神·不灭金身 | 12（神） | `TianfuAttributes.java:54-56` `health *= 2.0`（硬编码） | 可叠 | 一致 | 8.3%/点 |
| `ji_wanshi_bumie` 极·万世不灭 | 16（极） | `TianfuTalents.java:327-328` `HEALTH_MULT 1.50` → `health *= 2.50` | 可叠 | 一致 | 9.4%/点 |
| `routi_chanruo` 肉体孱弱 | −2（负） | `TianfuTalents.java:335` `−0.20` → ×0.80 | 可叠 | 一致 | 返还 2 点 |
| `v2_routi_canque` 肉体残缺 | −5（负） | `TianfuTalents.java:153` `−0.50` → ×0.50 | 可叠 | 一致 | 返还 5 点 |
| `苍木回春体` | 3（凡体） | `TianfuAttributes.java:45-47` `health *= 1.10` | 可叠 | 一致 | — |

**同类可叠清单（重要）**：`筋骨强健 ×1.10` × `血气如林 ×1.20` × `神·不灭金身 ×2.00` × `极·万世不灭 ×2.50` × `苍木回春体 ×1.10` = **×7.26 生命上限**。5 条全部无互斥、无上限。
**注意**：`ADD_MULTIPLIED_TOTAL` 是加法进总乘区，但 `TianfuAttributes.java:63` 把各条的乘积作为**单个 modifier 的 amount**，因此等效于乘法叠加（不是加性）。

### 3.2 减伤（`Op.REDUCTION`，统一 50% 硬上限）

| id / 来源 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `tiepi_rougu` 铁皮肉骨 | 1（黄） | `TianfuTalents.java:213` `REDUCTION 0.04` → `TianfuCombat.java:216` `r += reduction(ids)` | 4%/点 |
| `youdou_gaoshou` 游斗高手 | 2（玄） | `TianfuCombat.java:209-211` **移动中**（`getDeltaMovement().lengthSqr() > 0.01`）`r += 0.06`（硬编码） | 3%/点（有条件） |
| `ruwei_dingshen` 入微定神 | 2（玄） | `TianfuTalents.java:231` `0.08` | 4%/点 |
| `jianjia_ruyan` 坚甲如岩 | 3（地） | `TianfuTalents.java:261` `0.15` | 5%/点 |
| `jianjia_rushen` 坚甲如神 | 5（天） | `TianfuTalents.java:287` `0.25` | 5%/点 |
| `shen_bumie_jinshen` 神·不灭金身 | 12 | `TianfuCombat.java:213-215` `r += 0.20`（硬编码） | 2.5%/点 |
| `shen_busi_bumie` 神·不死不灭 | 12 | `TianfuTalents.java:309-310` `0.30` | 2.5%/点 |
| `shen_jingang_buhuai` 神·金刚不坏 | 12 | `TianfuTalents.java:313-314` `0.35` | 2.9%/点 |
| `ji_wanshi_bumie` 极·万世不灭 | 16 | `TianfuTalents.java:327-328` `0.40` | 2.5%/点 |
| `malice_dementor` 摄魂夺魄 | 5（天） | `TianfuCombat.java:218-219` 只对**吃护甲**伤害 `0.15` | 3%/点 |
| `malice_dispell` 破法之瞳 | 5（天） | 同上，只对**穿护甲**伤害 `0.15` | 3%/点 |
| `malice_adaptive` 随境应变 | 5（天） | `TianfuCombat.java:317-334` 同类每层 2%、上限 10 层 → **最多 20%** | — |
| `白玉铁骨体` | 3（凡体） | `TianfuCombat.java:202-204` 每千骨道道痕 10% | — |
| `寒渊素雪体` | 3（凡体） | `TianfuCombat.java:205-207` 冰系每千冰雪道 20% | — |

**上限证据（已验证）**：`TianfuCombat.java:220` `return Math.min(r, 0.5);`

**判定**：
- **50% 硬上限在描述里从未出现**。`jianjia_rushen 0.25 + shen_jingang_buhuai 0.35 = 0.60` 已经溢出，其中 0.10 白花；再加 `ji_wanshi_bumie 0.40` 溢出 0.50，**16 点中的减伤部分全部浪费**。
- 达成 0.50 上限的最低组合：`坚甲如神 (5) + 神·金刚不坏 (12) = 17 点`，或 `坚甲如神 (5) + 坚甲如岩 (3) + 神·不灭金身 (12) = 20 点`。
- `malice_dementor` / `malice_dispell` 的分流用了 `DamageTypeTags.BYPASSES_ARMOR`（`TianfuCombat.java:218`）—— 与描述「吃护甲 / 穿护甲」一致，实现正确。

### 3.3 闪避（`Op.DODGE`，60% 硬上限）

| id / 来源 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `shanbu_suifeng` 闪步随风 | 2（玄） | `TianfuTalents.java:232` `DODGE 0.06` → `TianfuCombat.java:254` | 3%/点 |
| `shenfa_ruyan` 身法如燕 | 3（地） | `TianfuTalents.java:256` `DODGE 0.12` | 4%/点 |
| `shen_tianji_suan` 神·天机神算 | 12 | `TianfuCombat.java:251-253` `chance += 0.24`（硬编码） | 2%/点 |
| `如影随形体` | 3（凡体） | `TianfuCombat.java:237-239` `chance += 0.05` | — |
| `九命不灭体` | 3（凡体） | `TianfuCombat.java:240-242` **濒死（hp<10%）** `chance += 0.25` | — |
| `太虚化形体` | 3（凡体） | `TianfuCombat.java:243-250` 15%，`xuhua_cd` 内置 100 tick（5 秒）CD | — |

**上限证据（已验证）**：`TianfuCombat.java:255-258` `Math.min(chance, 0.6)`

**判定**：
- 闪避是**完全免伤**（`TianfuCombat.java:89-92` `event.setAmount(0.0F); return;`），价值远高于同数值减伤。
- 最大名义值 `0.06 + 0.12 + 0.24 + 0.05 = 0.47`，加濒死 0.25 / 虚化 0.15 可轻松触顶 0.60。
- 描述统一未注明 **60% 上限**。
- 注意顺序：闪避在**减伤与适应之后、反伤之前**（`:73-94`），闪避成功时 `reflectDamage` 不再执行 —— 与类文档 `:31` 「非护甲减伤 → 闪避过滤 → 反伤反弹」一致。

### 3.4 生命恢复（两条独立通道）

**通道 A：每 5 秒（`Op.REGEN_HP_5S`，`TianfuEffects.java:328-334`，`player.tickCount % 100 == 0`）**

| id | cost | 数值 | 折算 | 收益/价格 |
|---|---|---|---|---|
| `zhaoqi_yinlu` 朝气饮露 | 1（黄） | `TianfuTalents.java:216` `0.01` | 0.2%/秒 | 0.2%/点 |
| `xieshi_ziyu` 血食自愈 | 3（地） | `:255` `0.03` | 0.6%/秒 | 0.2%/点 |
| `huixue_rusheng` 回血如生 | 5（天） | `:282` `0.05` | 1.0%/秒 | 0.2%/点 |
| `shen_busi_bumie` 神·不死不灭 | 12（神） | `:309-310` `0.12` | 2.4%/秒 | 0.2%/点 |
| `ji_wanshi_bumie` 极·万世不灭 | 16（极） | `:327-328` `0.40` | **8.0%/秒** | 0.5%/点 |

**通道 B：每秒（硬编码，`TianfuEffects.java:321-326`，`player.tickCount % 20 == 0`）**

| id | cost | 数值 |
|---|---|---|
| `shen_bumie_jinshen` 神·不灭金身 | 12 | `player.heal(max * 0.05F)` → **5%/秒** |

**判定**：
- **两条通道都用 `sum()` 加性叠加，全部可叠、无上限**。`sum()` 证据：`TianfuTalents.java:942-956`。
- 全买 = 每 5 秒 `0.01+0.03+0.05+0.12+0.40 = 0.61` → **12.2%/秒**，再加神·不灭金身的 5%/秒 = **17.2%/秒**。配合生命上限 ×7.26，等效「几乎不死」。
- `sum()` 是**加性**而非乘法 → 各条目数值线性可加，价格梯度（1/3/5/12/16 点对应 0.01/0.03/0.05/0.12/0.40）基本是 0.2%/点，**唯 `极·万世不灭` 给到 0.5%/点，明显优于同档**。
- 描述与代码一致（「每 5 秒回复 X% 生命」），但 `神·不灭金身` 写「每秒」而其余写「每 5 秒」——**这是对的**，两者确实是不同通道。

### 3.5 击退抗性（`Op.KNOCKBACK_FLAT` / `ADD_VALUE`）

| id | cost | 实际代码效果 | 描述 vs 代码 |
|---|---|---|---|
| `songyue_jianli` 耸岳坚立 | 2（玄） | `TianfuTalents.java:236` `KNOCKBACK_FLAT 0.4` → `TianfuAttributes.java:75-76` `KNOCKBACK_RESISTANCE ADD_VALUE` | 一致 |
| `shen_jingang_buhuai` 神·金刚不坏 | 12（神） | `TianfuTalents.java:313-314` `KNOCKBACK_FLAT 1.0` | 一致 |

**判定（已验证 + 未验证部分）**：
- 原版 `Attributes.KNOCKBACK_RESISTANCE` 的取值区间是 `[0, 1]`，1.0 = 完全免疫。`TianfuAttributes.java:74-77` 走 `ADD_VALUE` 且**不加钳制**。
- 两条同买 = 1.4，**超出 1.0 的部分被原版钳制** → 若先买 `耸岳坚立` 再买 `神·金刚不坏`，前者的 2 点**完全白花**。
- 「原版是否把 1.4 钳到 1.0」属**未验证**（需实机看 `getAttributeValue` 返回值），但 vanilla `AttributeInstance#getValue` 对 `KNOCKBACK_RESISTANCE` 的 clamp 是已知行为，风险高。

### 3.6 寿元（`Op.FIELD_ADD` `shouyuan`）

| id | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `shouyuan_mianchang` 寿元绵长 | 1（黄） | `TianfuEffects.java:63-65` `v.shouyuan += 50.0` | **50/点（最优）** |
| `chuntian_yishou` 春田益寿 | 2（玄） | `TianfuTalents.java:238` `fldAdd("shouyuan", 80)` → `TianfuTalents.java:632` `add()` | 40/点 |
| `shouyuan_ruhe` 寿元如鹤 | 5（天） | `TianfuTalents.java:277` `+200` | 40/点 |
| `shouyuan_duancu` 寿元短促 | −2（负） | `:338` `−100` | 返还 2 点 |
| `v2_shouyuan_kuwei` 寿元枯萎 | −5（负） | `:154` `−250` | 返还 5 点 |
| `v2_lifespan_scar` 命烛残尽 | −8（负） | `:159` `−400` + `V2_LIFESPAN_SCAR` | 返还 8 点 |

**判定**：
- 字段真实性 **已验证**：`PlayerVariables.shouyuan` 存在（javap 第 308/1252/1673 行 getfield，第 508/683 行 putfield）。
- 回滚对称：`TianfuTalents.java:1184-1186` `subtract()` 由 `revokeOnce`（`:1150-1167`）反向调用。
- **价格倒挂**：`寿元绵长`（黄/1，+50）严格优于 `春田益寿`（玄/2，+80）与 `寿元如鹤`（天/5，+200）的**每点**收益。三条可叠 → 全买 +330。

### 3.7 精力（`Op.FIELD_MULT` `zuida_jingli`）

| id / 体质 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `jingli_shengheng` 精力生生 | 2（玄） | `TianfuTalents.java:237` `fld("zuida_jingli", 0.30)` → `:631` `multiply()` → `:1141` `f.setDouble(v, cur * 1.30)` | 15%/点 |
| `jingli_ruhai` 精力如海 | 5（天） | `TianfuTalents.java:276` `0.80` → ×1.80 | 16%/点 |
| `搬山撼岳体` | 3（凡体） | `TianfuEffects.java:45-47` `v.zuida_jingli *= 1.15` | — |
| `v2_jingli_buji` 精力不济 | −2（负） | `TianfuTalents.java:150` `0.20` 负 → ×0.80 | 返还 2 点 |

**判定**：
- 字段真实性 **已验证**（`PlayerVariables.zuida_jingli` javap 第 833/846/982 行 getfield）。
- `FIELD_MULT` 走 `multiply()`，**乘法叠加**：`1.30 × 1.80 × 1.15 = ×2.69`，可叠、无互斥、无上限。
- 描述与代码一致；价格梯度合理（15%/点 vs 16%/点）。

### 3.8 魂魄（`zuida_hunpo` / `hunpochengzaijixian`）

| id / 体质 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `hunpo_lianxi` 魂魄炼息 | 2（玄） | `TianfuTalents.java:242` **只加** `zuida_hunpo ×1.15` | 7.5%/点 |
| `hunzai_kuanhou` 魂载宽厚 | 2（玄） | `TianfuTalents.java:243` **只加** `hunpochengzaijixian ×1.15` | 7.5%/点 |
| `hunpo_rugui` 魂魄如鬼 | 5（天） | `TianfuTalents.java:274-275` 两个字段各 ×1.50 | 10%/点 ×2 字段 |
| `shen_hunxi_zhuzai` 神·魂兮主宰 | 12（神） | `TianfuTalents.java:307-308` 魂魄 ×2.0 + 承载 ×2.0 + **念头容量 ×2.0** | 8.3%/点 ×3 字段 |
| `幽魂凝识体` | 3（凡体） | `TianfuEffects.java:41-44` ×1.25 / ×1.25 | — |
| `hunli_shuairuo` 魂力衰弱 | −2（负） | `TianfuTalents.java:340-341` ×0.75 / ×0.75 | 返还 2 点 |
| `北冥冰魄体` | 16（十绝体） | `TianfuNative.java:133-143` `zuida_hunpo = 500`、`hunpochengzaijixian = 50000`（**赋值，非乘算**） | — |

**判定**：
- 字段真实性 **已验证**（`zuida_hunpo` javap、`hunpochengzaijixian` javap）。
- **乘法叠加**：`1.15 × 1.50 × 2.00 × 1.25 = ×4.31`。可叠、无互斥。
- **描述与代码一致**，且明确写了「容量加成，不等于伤害 +15%」（`TianfuTalents.java:242`）—— 这是全表少见的诚实描述。
- ⚠️ **`hunpochengzaijixian`（承载极限）的实际作用点未在本次审查中验证**：本次只确认了字段存在与写入成功，没有反编译本体的「承载极限」消费逻辑。`hunzai_kuanhou`（2 点，只加承载不加魂魄上限）存在**收益无法证实**的风险 → 列入第 9 章未验证项。

---

## 4. 资源体系

### 4.1 真元恢复（`Op.REGEN_ZY`）

| id / 体质 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `zhenyuan_huiliu` 真元回流 | 2（玄） | `TianfuTalents.java:245` `REGEN_ZY 0.35` → `TianfuNativeProgression.java:50` `bonus = regenZy(ids)` → `Guzhenren98Progression.java:29-34` `recovery()` | 17.5%/点 |
| `zhenyuan_ruquan` 真元如泉 | 3（地） | `TianfuTalents.java:253` `0.80` | 26.7%/点 |
| `zhenyuan_bujie` 真元不竭 | 5（天） | `TianfuTalents.java:280` `1.50` | **30%/点（最优）** |
| 太息养元体 | 3（凡体） | `TianfuNativeProgression.java:52` `if (variables.tizhi == 31.0) bonus += 0.15;` | — |
| 明月入怀体 | 3（凡体） | `TianfuNativeProgression.java:53` 夜间 `bonus += 0.80` | — |
| `zhenyuan_kujie` 真元枯竭 | −2（负） | `TianfuTalents.java:333` `−0.30` | 返还 2 点 |
| `v2_zhenyuan_kubai` 真元枯败 | −5（负） | `TianfuTalents.java:152` `−0.75` | 返还 5 点 |

**生效链（已验证）**：
1. `NativeRecoveryMixin.java:14-20` 用 `@Redirect` 拦 `KE1Procedure.execute` 里的 `PUTFIELD zhenyuan`（`require = 7, expect = 7, allow = 7`）；
2. javap 实测 `KE1Procedure` 里 `putfield zhenyuan` **恰好 7 处**、`putfield niantou` **恰好 4 处**，且 `execute()` 描述符 `(LevelAccessor,DDD,Entity)V` 与 mixin 声明**完全一致** → **hook 结构成立**；
3. `TianfuNativeProgression.java:47-48` 只在 `nativeAfter > zhenyuan`（本体确实给了正向增量）时才加成；
4. `Guzhenren98Progression.java:32` `result = before + (nativeAfter - before) * max(0, 1 + bonus)`；`:33` 用 `zuida_zhenyuan` 封顶。

**判定**：
- **描述与代码一致**，且实现是「只放大本体真实产生的正向增量」，不伪造基线 —— 这是本类最干净的一段代码。
- 加性叠加：`.35 + .80 + 1.50 + .15 + .80(夜) = +3.60`（+360%）。无上限、无互斥。
- 负分项被 `Math.max(0.0, ...)` 兜底（`:32`），不会把真元往外抽 —— 与 `TianfuEffects.java:586-588` 的注释一致，正确。
- **价格倒挂**：天级 `真元不竭` 30%/点 > 地级 26.7%/点 > 玄级 17.5%/点。正常梯度，无问题。

### 4.2 念头恢复（`Op.REGEN_NT`）

| id / 体质 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `niantou_zisheng` 念头自生 | 1（黄） | `TianfuTalents.java:217` `REGEN_NT 0.20` → `TianfuNativeProgression.java:61` | **20%/点（最优）** |
| `v2_mind_recovery_1` 念头恢复 I | 2（玄） | `TianfuTalents.java:166` `0.20` | **10%/点（同效双价）** |
| `v2_mind_recovery_2` 念头恢复 II | 3（地） | `TianfuTalents.java:167` `0.40` | 13.3%/点 |
| `niantou_ruyuan` 念头如渊 | 3（地） | `TianfuTalents.java:254` `0.60` | 20%/点 |
| `niantou_bujie` 念头不竭 | 5（天） | `TianfuTalents.java:281` `1.20` | 24%/点 |
| 南柯一梦体 | 3（凡体） | `TianfuNativeProgression.java:62` `bonus += 0.30` | — |
| `niantou_ganhe` 念头干涸 | −2（负） | `TianfuTalents.java:334` `−0.30` | 返还 2 点 |

**判定**：
- **`念头自生`（黄/1，+20%）与 `v2_mind_recovery_1`（玄/2，+20%）数值完全相同、价格翻倍**，是明确的定价错误。见第 6 章亏本清单。
- **两者不在同一互斥族**：`TianfuMindTable.java:18-19` 只把 `v2_mind_recovery_1/2` 归入 `recovery` 族；`niantou_zisheng` 不在表内 → **可以同时买**，合计 +40%。
- 加性叠加最大 `.20 + .40 + .60 + 1.20 + .30 = +2.70`（+270%）。
- 封顶用 `niantou_rongliang`（`TianfuNativeProgression.java:63`），**因此「念头容量」词条会同步放大「念头恢复」的实际上限**，两者存在协同。

### 4.3 念头容量（`Op.FIELD_MULT` `niantou_rongliang`）

| id / 体质 | cost | 实际代码效果 | 收益/价格 |
|---|---|---|---|
| `v2_mind_capacity_1` | 2（玄） | `TianfuTalents.java:163` `0.20` → ×1.20 | 10%/点 |
| `v2_mind_capacity_2` | 3（地） | `TianfuTalents.java:164` `0.30` → ×1.30 | 10%/点 |
| `niantou_kuozhan` 念头扩展 | 2（玄） | `TianfuTalents.java:244` `0.15` → ×1.15 | 7.5%/点 |
| `v2_mind_capacity_3` | 5（天） | `TianfuTalents.java:165` `0.50` → ×1.50 | 10%/点 |
| `qiqiao_linglong` 七窍玲珑 | 5（天） | `TianfuTalents.java:272-273` 容量 ×1.50 **+** `niantou_zhida` ×1.50 | 10%×2 字段 |
| `shen_hunxi_zhuzai` 神·魂兮主宰 | 12（神） | `TianfuTalents.java:307-308` 容量 ×2.0（另两字段） | 8.3%×3 字段 |
| `慧心通明体` | 3（凡体） | `TianfuEffects.java:48-51` ×1.25 / ×1.25 | — |
| `nianfu_xiayai` 念府狭隘 | −2（负） | `TianfuTalents.java:342` ×0.75 | 返还 2 点 |
| `v2_aptitude_shackled` 空窍桎梏 | −8（负） | `TianfuTalents.java:157` `niantou_rongliang ×0.0` | 返还 8 点 |

**互斥族（已验证）**：`TianfuMindTable.java:15-17` 把 `v2_mind_capacity_1/2/3` 归入 `capacity` 族，同族只能选一个。

**判定**：
- 乘法叠加：`1.50 × 1.15 × 1.50 × 2.00 × 1.25 = ×6.47`（极端配置）。
- **`v2_mind_capacity_3`（天/5，仅 +50% 容量）被 `qiqiao_linglong`（天/5，+50% 容量 **且** +50% 念头之道）完全支配** —— 后者严格更优，同价同档。见第 6 章亏本清单。
- `niantou_zhida`（念头之道）的消费点 **已验证**：`KE1Procedure` javap 第 1175/1217/1259 行 `getfield niantou_zhida` → 参与念头恢复计算。
- ⚠️ `v2_aptitude_shackled` 的 `×0.0`：`TianfuTalents.java:1135-1145` `multiply()` 只在 `factor == 1.0` 时提前返回，`factor = 0.0` 会把容量**直接置 0**。回滚走 `divide()`（`:1169-1182`），而 `divide` 对 `|cur| < 1e-6` 直接 return → **容量被置 0 后撤销无法恢复**（除以 0 被保护成「不动」）。这是一个**真实的可撤销性缺陷**，但该条目是负分且属 V2 硬约束（`TianfuApply.java:33-34` `ADAPTER_HARD_COSTS`），影响面有限。

### 4.4 蛊虫冷却（`Op.GU_CD`）—— **单位语义与描述不符**

**实现（已验证）**：
`NativeItemCooldownMixin.java:14-20` 拦 `ItemCooldowns.addCooldown(Item,int)`，仅对 `guzhenren` 命名空间物品生效（`:17-18`），交给
`TianfuNativeProgression.java:66-69` → `Guzhenren98Progression.java:57-63`：
```java
double rate = 1.0 + extraTicksPerTick;
double ticks = Math.ceil(nativeTicks / rate);
return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0, ticks));
```

**这意味着 `GU_CD` 是「除数率」而不是「每 tick 减多少冷却」**：

| id | cost | 数据表值 | 描述原文 | 实际冷却倍率 | 实际缩短 |
|---|---|---|---|---|---|
| `v2_mind_cooldown_1` 蛊冷却 I | 2（玄） | `TianfuTalents.java:170` `0.15` | 「每 tick −0.15」 | `÷1.15` | **−13.0%** |
| `v2_mind_cooldown_2` 蛊冷却 II | 3（地） | `TianfuTalents.java:171` `0.25` | 「每 tick −0.25」 | `÷1.25` | **−20.0%** |
| `gudao_dup` 蛊道独步 | 3（地） | `TianfuTalents.java:251` `0.25` | 「每 tick −0.25」 | `÷1.25` | **−20.0%** |
| `liangu_bujie` 炼蛊不竭 | 5（天） | `TianfuTalents.java:290-291` `0.30` | 「蛊冷却缩短（每 tick −0.3）」 | `÷1.30` | **−23.1%** |
| `shen_liangu_tianshu` 神·炼蛊天术 | 12（神） | `TianfuTalents.java:317-318` `0.50` | 「（每 tick −0.5）」 | `÷1.50` | **−33.3%** |
| `v2_gu_cd_ningse` 催蛊凝涩 | −2（负） | `TianfuTalents.java:151` `−0.10` | 「每 tick 额外增加 0.10」 | `÷0.90` | **+11.1%** |

**判定**：
- **「每 tick」这个单位是错的**（`Guzhenren98Progression.java:56` 的注释也写「Extra cooldown ticks consumed per game tick」，与实际算法不符）。真实效果是「冷却时长 ÷ (1+值)」。
- 叠加（加性求和后取一次倒数）：`0.25 + 0.25 + 0.30 + 0.50 = 1.30` → `÷2.30` = **−56.5%**；再加 `v2_gu_cd_ningse` 则 `0.25+0.25+0.30+0.50−0.10 = 1.20` → −54.5%。
- **可叠、无互斥族**（只有 `v2_mind_cooldown_1/2` 之间互斥）。
- 数值上：`÷1.15 = −13%` 对应描述写的「−0.15」，作者显然把 `0.15` 当成了「15%」，写成「每 tick −0.15」。**方向对、单位错、可读性严重误导**。
- `v2_gu_cd_ningse` 的 `−0.10` → `÷0.90` = +11%，与描述「额外增加 0.10」量级接近但不精确（若真按「每 tick +0.10」对 100 tick 冷却应是 +10 tick = +10%）。

### 4.5 杀招冷却（`Op.SHAZHAO_CD`）—— **这条是对的**

| id | cost | 实际代码效果 |
|---|---|---|
| `shazhao_rufeng` 杀招如风 | 3（地） | `TianfuTalents.java:252` `SHAZHAO_CD 0.25` → `TianfuNativeProgression` 无 → `TianfuEffects.java:231-243` 每 tick 对 `sanzhao_CD / CD1 / CD2 / CD3` 各减 `extraCooldownStep(current, 0.25)` |

`TianfuEffects.java:570-573`：
```java
private static double extraCooldownStep(double current, double extra) {
    return Double.isFinite(current) && current > 0.0
            ? Math.max(0.0, current - extra) : current;
}
```

**判定**：这是**真正的「每 tick −0.25」**（逐 tick 从计时器里多扣 0.25）→ 与描述「每 tick −0.25」**一致**。四个 `sanzhao_CD*` 字段真实性 **已验证**（本报告第 8 章）。

> ⚠️ 同名前缀的原语 `GU_CD` 与 `SHAZHAO_CD` 语义**完全不同**（一个除数率、一个逐 tick 减），却都写成「每 tick −X」，这是描述文案的根因错误。

### 4.6 资源消耗（降耗类）—— **整组已停用**

| id | cost | 状态 | 证据 |
|---|---|---|---|
| `zhenyuan_ninglian` 真元凝练 | 2（玄） | **运行期停用** | `TianfuTalents.java:41` `NATIVE_UNAVAILABLE.put("zhenyuan_ninglian", "本体 9.8 未提供统一真元扣费入口…")` |
| `niantou_ninglian` 念头凝练 | 2（玄） | **运行期停用** | `TianfuTalents.java:42` |
| `shazhao_tiancheng` 杀招天成 | 3（地） | **运行期停用** | `TianfuTalents.java:43` |

**判定**：
- 三条被 `available()`（`TianfuTalents.java:562-570`）判为不可用 → `specs()`（`:475-484`）不再把它们放进词条池 → **界面看不到、买不到**。这一点是**正确处理**（比让玩家买到零效果条目好）。
- 但 3 条 id 仍在数据表里（`TianfuPool.java:68/71/72`），且 `sanitize()`（`TianfuApply.java:47-55`）会**认可**它们 → **旧存档已购玩家：点数已扣、效果为 0、且无法撤销**（`TianfuApply.java:142-169` 的 `revoke` 在已确认后被 `:147-149` 拦住）。这是遗留风险，建议 1.4.18 给出补偿或一次性退款路径。
- 全表**没有任何**「真元/念头消耗 −X%」的有效实现 → 资源体系缺一个维度。

---

## 5. 描述与代码不一致清单（问题 7）

> 逐条列「描述写 X，代码实际 Y」。全部附 `文件:行号`。

### 5.1 属于「未实现」的（描述写的效果代码里完全没有）

| # | id / 范围 | 描述写 X | 代码实际 Y | 证据 |
|---|---|---|---|---|
| 1 | **43 条「XX亲和 / 大成 / 道藏」** | 「该流派**熟练度效率** +20% / +50% / +80%」（`TianfuTalents.java:175-229, 266-268, 271`） | 只写 `daohen_*` 与配对的 `liupai_*/10`（`TianfuTalents.java:727-738` `addPaired`）；**没有任何「熟练度效率」实现**。全仓库 `grep` 只在 `wanfa_jietong`（`TianfuEffects.java:384-410`，独立差分机制）里出现过熟练度补偿 | 脚本实测 43 条 desc 含「熟练度效率」，无一有对应 Op |
| 2 | `suhui` 宿慧 | 「每转数提供额外修炼基数**与道痕获取**」 | 只有 `TianfuNativeProgression.java:18` `bonus += 0.05 * Math.max(1.0, variables.zhuanshu)`，**没有任何道痕获取逻辑** | `TianfuNativeProgression.java:14-23` |
| 3 | `suiyue_chendian` 岁月沉淀 | 「**已耗寿元越多**、修炼越快」 | 固定 `bonus += 0.10`（`TianfuNativeProgression.java:20`），**从不读 `shouyuan` 或任何寿元变量** | 同左 |
| 4 | `shiyuan_zhixing` 噬元之性 | 「击杀**灵体**回复真元/念头」 | 判定 `victim.getType().is(TEXIAO)`，`TEXIAO = guzhenren:texiao`（`TianfuSlay.java:23, 163`）。jar 内 `texiao.json` = **340 条特效实体**；真正的灵体 tag `ling.json` 只有 **5 条**且代码**从未引用**（`TianfuSlay` 只定义了 `SHENGWU/RENZU/TEXIAO` 三个 tag，`:21-23`） | `TianfuSlay.java:23,163` + jar `data/guzhenren/tags/entity_type/{texiao,ling}.json` |

### 5.2 属于「单位 / 语义写错」的

| # | id | 描述写 X | 代码实际 Y | 证据 |
|---|---|---|---|---|
| 5 | `v2_mind_cooldown_1` / `v2_mind_cooldown_2` / `gudao_dup` / `liangu_bujie` / `shen_liangu_tianshu` | 「**每 tick** −0.15 / −0.25 / −0.25 / −0.3 / −0.5」 | 冷却时长 **÷ (1+值)**：−13.0% / −20.0% / −20.0% / −23.1% / −33.3% | `Guzhenren98Progression.java:57-63` |
| 6 | `v2_gu_cd_ningse` | 「每 tick 额外增加 0.10」 | ÷0.90 = **+11.1%** 冷却 | 同上 |
| 7 | `jianbi_qingfeng` 剑指偏锋 | 「暴击伤害 +30%（**需配合暴击精通**）」 | `TianfuCombat.java:277` 独立 `if (ids.contains("jianbi_qingfeng")) mult += 0.30;`，**与是否买了暴击精通无关**，甚至只有 0% 暴击率时也照样生效（只是永远不触发） | `TianfuCombat.java:266-280` |

### 5.3 属于「漏写上限」的（买多了白花）

| # | id / 范围 | 描述漏写 | 实际上限 | 证据 |
|---|---|---|---|---|
| 8 | `baoji_jingtong` / `baoji_rulie` / `baoji_rushen` / `shen_tianji_suan` | 未写暴击率上限 | **50%** | `TianfuCombat.java:38, 270` |
| 9 | 全部暴伤条目 | 未写「只在暴击时生效」 | — | `TianfuCombat.java:112-115`（`if (crit)` 才乘） |
| 10 | `xixue_rumo` / `xixue_rushi` | 未写吸血上限 | **15%**，两条同买 = 16% 溢出 | `TianfuCombat.java:288` |
| 11 | `shanbu_suifeng` / `shenfa_ruyan` / `shen_tianji_suan` 闪避部分 | 未写闪避上限 | **60%** | `TianfuCombat.java:255-258` |
| 12 | 全部减伤条目（8 条 + 2 条恶意） | 未写减伤上限 | **50%**，`坚甲如神+神·金刚不坏` 即溢出 | `TianfuCombat.java:220` |
| 13 | `songyue_jianli` / `shen_jingang_buhuai` | 未写击退抗性上限 | 原版 **1.0**，两条同买 1.4 溢出 | `TianfuAttributes.java:75-76`（`ADD_VALUE`，无钳制） |
| 14 | 全部反伤条目（5 条） | 未写「**同类只取最强**」 | `bestReflect` 取 `概率×比例` 最大者，**不叠加** | `TianfuTalents.java:928-940` |
| 15 | 全部反伤条目（5 条） | 未写「**只对玩家攻击者生效**」 | `if (!(... instanceof ServerPlayer attacker)) return;` → **PvE 完全无效** | `TianfuCombat.java:302` |

### 5.4 属于「收益方向与描述不符」的

| # | id | 描述写 X | 代码实际 Y | 证据 |
|---|---|---|---|---|
| 16 | `zonghen_chubei` 总痕储备 | 「总道痕汇总字段 +300；**不增加各流派道痕或伤害**」 | `daohen_zong` 是本体伤害公式的**杂修项实参**：`ShangHaiLiuCheng1Procedure` offset 62/84/117/140 读 `daohen_zong`，按 `(daohen_zong - 本流派道痕)/1000` 参与 `L22/L24/L26/L28` 的钳制运算 → **+300 确实改变伤害乘区**（方向未验证，按「杂修惩罚」语义应为**降低**） | `ShangHaiLiuCheng1Procedure` 字节码；`TianfuTalents.java:190` |

### 5.5 `mob(..., 0)` 三条联动条目

| # | id | 描述写 X | 代码实际 Y | 证据 |
|---|---|---|---|---|
| 17 | `bingxue_daoti` 冰雪道体 | 「常驻**冰雪道增幅**」（5 点） | `TianfuTalents.java:297-298` `mob("guzhenren_addon:ice_path_boost", 0)` → **amplifier = 0** | 见下 |
| 18 | `hundao_ronghe` 魂道融彻 | 「常驻**魂道增幅**」（5 点） | `TianfuTalents.java:299-300` `mob("hundao_addon:hundao_zengfu", 0)` → amplifier = 0 | 见下 |
| 19 | `shen_liupai_jianji` 神·流派兼济 | 「常驻三 buff，**三者可叠加**」（12 点） | `TianfuTalents.java:320-324` 三个 `mob(..., 0)`，全部 amplifier = 0 | 见下 |

**为什么 amplifier = 0 就是 0 收益（已验证字节码）**：
`BuffLiuCheng1Procedure` 的伤害结算是
```
amount = base × (1+28) × (1+32) / (1+30) / (1+34) × (1 + acc36)
```
其中 `acc36` 由 `getAmplifier()` 逐项 `i2d` 后 `dadd` 累加（offset 1314–1322），在 `Entity.hurt` 前以 `dconst_1 / dload 36 / dadd / dmul` 进入乘区（offset 1682–1688，及 1755–1759）。**amplifier = 0 → `acc36 = 0` → 乘区 ×1.0，零收益**。

> **矛盾点（必须上报）**：`TianfuLiupai.java:39-46` 的注释断言「`amplifier` 直接作为**加算百分比**进伤害乘区，所以 amplifier = 1 就是 +1%」。上述字节码读法是 `(1 + Σamp)`，即 **amplifier = 1 → ×2.0（+100%）**。两种读法互相矛盾：
> - 若注释对（amp=1 → +1%）：`凡体` 的 5 条「自带流派增幅常驻」（`TianfuLiupai` 挂 amp=1）价值极小 → 3 点买 +1% 是亏本。
> - 若字节码对（amp=1 → +100%）：`凡体` 的这 5 条严重超模，而 `mob(...,0)` 三条联动仍然是 **0 收益**。
> **无论哪种读法，`amp = 0` 的 3 条联动词条的 buff 部分都是零收益** —— 这个结论不受影响。
> 「amp=1 到底是 +1% 还是 +100%」列入第 9 章未验证项。

---

## 6. 超模 / 亏本条目

### 6.1 明显超模（收益远超同价条目）

| 排名 | id | cost | 实际收益 | 为什么超模 |
|---|---|---|---|---|
| **1** | `shen_tianji_suan` 神·天机神算 | 12（神） | 暴击率 **+50%（恰好等于 `CRIT_CAP`，即 100% 必暴）** + 暴伤 `2.0+2.40 = ×4.4` + 闪避 **+24%** | `TianfuCombat.java:267` → `:270` `Math.min(chance, 0.50)` → **每次攻击都暴击**，输出恒定 ×4.4；吸血上限 15% × 4.4 = 每击回 **66% 攻击力**；闪避 24% 让 8 点条目（`身法如燕` 12%/3点 + `闪步随风` 6%/2点）相形见绌。12 点买到「全局 ×4.4 输出 + 24% 免伤 + 66%/击 吸血」，且**不在任何互斥族内** |
| **2** | `ji_wanshi_bumie` 极·万世不灭 | 16（极） | 生命上限 **×2.5** + 每 5 秒回 **40%**（8%/s）+ 减伤 **+40%** | `TianfuTalents.java:327-328`。三条原语**无一条被上限吃掉**（生命无上限、回血通道无上限、减伤 0.40 < 0.50）。与 `神·不灭金身`（生命 ×2.0 + 减伤 0.20 + 每秒 5%）叠乘后：生命 **×5.0**、回血 **13%/s**、减伤 **0.60→钳 0.50** |
| **3** | `wanfa_jietong` 万法皆通 | 16（极） | **全流派熟练度获取 ×3**，永久、无上限、可复利 | `TianfuEffects.java:316-318, 384-410`：每秒对所有 `liupai_*` 字段做差分，正增量额外补 `delta * 2.0`（净 ×3）。覆盖 **46 个 `liupai_*` 字段**（含 `liupai_feixingdao` / `liupai_qingmeidao` 这两个没有 `daohen_` 对应的字段）。熟练度是本体长期成长的唯一主轴 → 这一条的**长期收益无法用点数衡量**，且**不与任何条目冲突** |
| 4 | `shen_zhanshen_linshi` 神·战神临世 | 12 | 攻击 **×1.60** + 攻速 **×1.25** | 与天机神算的 ×4.4 相乘 → 单体 DPS **×8.8**（暴击口径）。5%/点攻击是除 `力进如泉`（4%/点）外最高 |
| 5 | `baoji_rushen` 暴击如神 | 5（天） | 暴击率 +25% + 暴伤 +120% | 期望增益 = `0.25 × (1.0 + 1.20) = +55%` 总伤，**11%/点**，高于任何单属性条目 |
| 6 | `zhanyi_angran` 战意盎然 | 1（黄） | 满血出伤 **+8%** | **8%/点**，与 2 点的 `力进如泉` 同效。同价位（1 点）的 `铁皮肉骨` 只有 4% 减伤、`气运微光` 只有 +5 上限 |
| 7 | `jingu_qiangjian` 筋骨强健 | 1（黄） | 生命上限 **+10%** | **10%/点**，与 2 点的 `血气如林` 同效、与 12 点的 `神·不灭金身`（8.3%/点）持平 |
| 8 | `niantou_zisheng` 念头自生 | 1（黄） | 念头回复 **+20%** | **20%/点**，与天级的 `念头不竭`（24%/点）几乎同价，是同档第一 |

**跨类联乘（最需要平衡的地方）**：
```
杀神 ×5.0（TianfuSlay.java:43-63）
 × 神·天机神算 4.4（TianfuCombat.java:278）
 × 神·战神临世 1.60（TianfuTalents.java:311）
 × 全盛如洪 1.18 + 战意盎然 1.08
 × 残身如狼 1.25 + 血煞 1.12
= 单次伤害最高可达 ×22.0（不含道痕/本体流水线）
```
而 `杀神` 只要 16 点、天机神算 12 点，**28 点即可拿到 ×22 的基础乘区**。

### 6.2 明显亏本（花了点数但收益≈0 或与本体机制冲突失效）

| # | id | cost | 问题 | 证据 |
|---|---|---|---|---|
| 1 | `bingxue_daoti` 冰雪道体 | 5（天） | `mob(..., 0)` → amplifier 0 → 乘区 ×1.0，**除状态图标外零收益** | `TianfuTalents.java:297-298`；`BuffLiuCheng1Procedure` offset 1682-1688 |
| 2 | `hundao_ronghe` 魂道融彻 | 5（天） | 同上，**零收益** | `TianfuTalents.java:299-300` |
| 3 | `shen_liupai_jianji` 神·流派兼济 | 12（神） | 三个 `mob(...,0)` 全为 0 收益，**12 点买三个图标** | `TianfuTalents.java:320-324` |
| 4 | `jianbi_qingfeng` 剑指偏锋 | 2（玄） | 暴伤只在暴击时生效；仅配 `暴击精通`（5%）时 EV = **+1.5% 总伤** → **0.75%/点，全表最低** | `TianfuCombat.java:277, 112-115, 266` |
| 5 | `youshou_nengkai` 右守能铠 | 1（黄） | 概率×比例 = 0.004，**任何一条其它反伤词条的存在都让它收益归 0**（`bestReflect` 取最大）；且 PvE 无效 | `TianfuCombat.java:295-300, 302` |
| 6 | `wuying_bi` 无影壁 | 2（玄） | 0.0072，被 `反伤如玉`(0.02) / `反伤如山`(0.07) 完全压制 → 同买即 0 收益 | `TianfuTalents.java:928-940` |
| 7 | `fanshang_ruyu` 反伤如玉 | 3（地） | 0.02 < `反伤如山` 0.07 → 同买即 0 收益 | 同上 |
| 8 | `malice_reflect` 镜影反照 | 3（地） | 0.045 < 0.07 → 同买即 0 收益 | 同上 |
| 9 | `zonghen_chubei` 总痕储备 | 5（天） | 只加 `daohen_zong`（杂修项实参），描述自称「不增加伤害」，实际可能**降低**伤害 | 第 5.4 节 |
| 10 | `shiyuan_zhixing` 噬元之性 | 3（地） | 触发 tag 是 **340 条「特效实体」**而非 **5 条「灵体」**；特效实体通常不可被玩家击杀 → 实际近 0 收益 | `TianfuSlay.java:23,163` |
| 11 | `songyue_jianli` 耸岳坚立 | 2（玄） | `+0.4` 与 `神·金刚不坏 +1.0` 同买 = 1.4，被原版钳到 1.0 → 后买的那条**完全失效** | `TianfuAttributes.java:75-76` |
| 12 | `v2_mind_recovery_1` 念头恢复 I | 2（玄） | 与 `念头自生`（**1 点**，+20%）**数值完全相同**，价格翻倍；且**不在同一互斥族**，可同买 → 纯粹是「多花 1 点买同效」 | `TianfuTalents.java:166` vs `:217`；`TianfuMindTable.java:18-19` |
| 13 | `v2_mind_capacity_3` | 5（天） | 被 `七窍玲珑`（**同价同档**、+50% 容量 **且** +50% 念头之道）**完全支配** | `TianfuTalents.java:165` vs `:272-273` |
| 14 | `zhenyuan_ninglian` / `niantou_ninglian` / `shazhao_tiancheng` | 2/2/3 | 已 `NATIVE_UNAVAILABLE` → **不可购买**；但旧存档已购玩家**点数已扣、零效果、不可撤销** | `TianfuTalents.java:41-43`；`TianfuApply.java:147-149` |
| 15 | 任何超出上限的减伤/暴击/闪避/吸血/击退条目 | — | 一旦突破上限（减伤 0.50 / 暴击 0.50 / 闪避 0.60 / 吸血 0.15 / 击退 1.0），**超出部分 100% 白花**且描述未提示 | 第 5.3 节 |

### 6.3 定价不一致（同效果不同价）汇总

| 效果 | 便宜的一条 | 贵的一条 | 结论 |
|---|---|---|---|
| 出伤 +8% | `zhanyi_angran` 1 点（满血） | `lijin_ruquan` 2 点（常驻） | 各有条件，尚可 |
| 生命 +10% | `jingu_qiangjian` 1 点 | `xueqi_rulin` 2 点（+20% → 同 10%/点） | **线性一致** |
| 念头回复 +20% | `niantou_zisheng` 1 点 | `v2_mind_recovery_1` 2 点 | **同效双价，明确错误** |
| 念头容量 +50% | `七窍玲珑` 5 点（+副属性） | `v2_mind_capacity_3` 5 点（无副属性） | **后者被支配** |
| 蛊冷却 −20% | `gudao_dup` 3 点 / `v2_mind_cooldown_2` 3 点 | — | 一致 |
| 寿元 | `寿元绵长` 1 点 +50（**50/点**） | `寿元如鹤` 5 点 +200（40/点） | **黄级最优，天级反而更差** |

---

## 7. 同类可叠清单（不在任何互斥族内）

**互斥族只有 5 个**（`TianfuMindTable.java:11-23`，共 12 条）：`xiulian` / `capacity` / `recovery` / `liangu` / `cooldown`。**其余全部可自由叠加**。

| 维度 | 可叠条目 | 叠加方式 | 叠加后极值 | 与上限的关系 |
|---|---|---|---|---|
| **暴击率** | `baoji_jingtong` 0.05 + `baoji_rulie` 0.15 + `baoji_rushen` 0.25 + `shen_tianji_suan` 0.50 | `sum()` 加性（`TianfuTalents.java:802-804`） | **0.95** | 封顶 **0.50** → **45% 溢出白花** |
| **暴击伤害** | `baoji_jingtong` +0.20 + `jianbi_qingfeng` +0.30 + `baoshang_rufeng` +0.80 + `baoji_rushen` +1.20 + `shen_tianji_suan` +2.40 | 加性 + 基础 2.0（`TianfuCombat.java:275-280`） | **×6.90** | 无上限 |
| **最大生命** | `jingu_qiangjian ×1.10` + `xueqi_rulin ×1.20` + `shen_bumie_jinshen ×2.00` + `ji_wanshi_bumie ×2.50` + `苍木回春体 ×1.10` | **乘法**（`TianfuTalents.java:609`） | **×7.26** | 无上限 |
| **减伤** | `tiepi_rougu` .04 + `youdou_gaoshou` .06 + `ruwei_dingshen` .08 + `jianjia_ruyan` .15 + `jianjia_rushen` .25 + `shen_bumie_jinshen` .20 + `shen_busi_bumie` .30 + `shen_jingang_buhuai` .35 + `ji_wanshi_bumie` .40 | `sum()` 加性（`:818-820`） | **1.83** | 封顶 **0.50** → **1.33 溢出白花** |
| **闪避** | `shanbu_suifeng` .06 + `shenfa_ruyan` .12 + `shen_tianji_suan` .24 + `如影随形` .05（+`九命不灭` .25 濒死 / `太虚化形` .15） | `sum()` 加性（`:810-812`） | **0.47~0.87** | 封顶 **0.60** |
| **吸血** | `xixue_rumo` .06 + `xixue_rushi` .10 + `血煞蜕变` .05 | `sum()` 加性（`:814-816`） | **0.21** | 封顶 **0.15** → **0.06 溢出** |
| **生命恢复（/5s）** | `zhaoqi_yinlu` .01 + `xieshi_ziyu` .03 + `huixue_rusheng` .05 + `shen_busi_bumie` .12 + `ji_wanshi_bumie` .40 | `sum()` 加性（`:771-773`） | **0.61/5s = 12.2%/s** | 无上限 |
| **生命恢复（/s·独立通道）** | `shen_bumie_jinshen` 5%/s | `TianfuEffects.java:321-326` 硬编码 | **5%/s** | 无上限 |
| **攻击力** | `lijin_ruquan ×1.08` + `神·战神临世 ×1.60` + `搬山撼岳体`（每千力道道痕 +12%） | **乘法**（`:610`） | ×1.73+ | 无上限 |
| **攻击速度** | `quanru_jifeng ×1.10` + `神·战神临世 ×1.25` | **乘法**（`:612`） | **×1.375** | 无上限 |
| **击退抗性** | `songyue_jianli` +0.4 + `shen_jingang_buhuai` +1.0 | `ADD_VALUE` 加性（`TianfuAttributes.java:75`） | **1.4** | 原版钳 **1.0** |
| **真元恢复** | `zhenyuan_huiliu` .35 + `zhenyuan_ruquan` .80 + `zhenyuan_bujie` 1.50 + `太息养元` .15 + `明月入怀` .80(夜) | `sum()` 加性（`:761-763`） | **+3.60（+360%）** | 无上限 |
| **念头恢复** | `niantou_zisheng` .20 + `v2_mind_recovery_2` .40 + `niantou_ruyuan` .60 + `niantou_bujie` 1.20 + `南柯一梦` .30 | `sum()` 加性（`:766-768`） | **+2.70（+270%）** | 无上限 |
| **念头容量** | `v2_mind_capacity_3 ×1.50` + `niantou_kuozhan ×1.15` + `qiqiao_linglong ×1.50` + `shen_hunxi_zhuzai ×2.00` + `慧心通明 ×1.25` | **乘法**（`:631`） | **×6.47** | 无上限 |
| **魂魄上限** | `hunpo_lianxi ×1.15` + `hunpo_rugui ×1.50` + `shen_hunxi_zhuzai ×2.00` + `幽魂凝识 ×1.25` | 乘法 | **×4.31** | 无上限 |
| **承载极限** | `hunzai_kuanhou ×1.15` + `hunpo_rugui ×1.50` + `shen_hunxi_zhuzai ×2.00` | 乘法 | **×3.45** | 无上限（消费点未验证） |
| **精力上限** | `jingli_shengheng ×1.30` + `jingli_ruhai ×1.80` + `搬山撼岳 ×1.15` | 乘法 | **×2.69** | 无上限 |
| **寿元** | `寿元绵长 +50` + `春田益寿 +80` + `寿元如鹤 +200` | `FIELD_ADD` 加性 | **+330** | 无上限 |
| **蛊冷却** | `gudao_dup` .25 + `v2_mind_cooldown_2` .25 + `liangu_bujie` .30 + `shen_liangu_tianshu` .50 | 加性求和后取倒数（`Guzhenren98Progression.java:59-61`） | **÷2.30 = −56.5%** | 无上限 |
| **炼蛊进度** | `v2_mind_liangu_2` .30 + `liangu_rushen` .40 + `liangu_bujie` .60 + `shen_liangu_tianshu` .90 | `sum()` 加性（`:786-788`）→ `TianfuEffects.java:262` | **+2.20** | 受 `[50,70]` 投料窗口与 99 上限约束（`TianfuEffects.java:272, 307`） |
| **修炼速度** | `kuxiu_buchuo` .20 + `suiyue_chendian` .10 + `suhui`(.05×转数) + `dazuo_wangwo` .40 + `xiulian_rushen` .60 + `ji_zhongdao_zhizun` 2.00 + `v2_mind_xiulian_3` .60 | `sum()` 加性（`:776-778`）→ `TianfuNativeProgression.java:17` | **+3.90（+390%）** | 无上限 |
| **反伤** | 5 条 | `bestReflect()` 取 `概率×比例` **最大者** | **0.07** | **不叠**（唯一不叠的战斗维度） |

**结论**：22 个维度里，**只有反伤是真正不叠的**；其余 21 个维度全部可叠，且 6 个维度存在硬上限（暴击 0.50 / 减伤 0.50 / 闪避 0.60 / 吸血 0.15 / 击退 1.0 / 反伤不叠），但这些上限**没有任何一条写进了天赋描述**。

---

## 8. 属性写入真实性核对（javap 逐字段）

**本体 jar**：`_crash3/basever/guzhenren-10.20.jar`，sha256 `B6D314D3…A01FE2BC`（已核验）。
**工具**：`jdk-17/bin/javap.exe -p -c -classpath <jar> <FQCN>`，长输出先 `Out-File`。

### 8.1 `daohen_*` / `dahen_*` 字段（`Op.DAOHEN` / `Op.DAOHEN_ALL` 的落点）

| 检查项 | 结果 |
|---|---|
| `PlayerVariables` 内 `daohen_*` / `dahen_*` 的 `public double` 字段总数 | **45**（含 `daohen_zong`） |
| 非汇总字段（`DAOHEN_ALL` 的迭代范围，`TianfuTalents.java:748-749` 排除 `daohen_zong`） | **44** —— 与数据表注释「44 项非汇总道痕」**完全吻合** |
| 数据表引用的每个 `dao(...)` 目标字段是否都存在 | **全部存在**（44 个） |
| 本体字段名混用 `daohen_` / `dahen_` 是否被正确区分 | **是**：`dahen_{liandao,nudao,qidao,rendao,tiandao,xingdao,zhendao,zhidao,zhoudao}` 共 9 个用 `dahen_`，其余用 `daohen_`；`TianfuDaohen.java:94-95` 的注释与 `TianfuTalents.java:173, 176, 179, 187, 188, 209, 210, 223, 225, 229` 的写法**一一对应** |
| 成对写（`liupai_*` = `daohen/10`）是否有孤儿 | **无**：45 个 `daohen_*` + 46 个 `liupai_*`，唯一「缺配对」的是 `daohen_zong`，而它**已被 `allDaohen` 排除**（`TianfuTalents.java:748`）；反向孤儿 `liupai_feixingdao` / `liupai_qingmeidao` 无 `daohen_` 对应，`TianfuDaohen.java:74` 已单独处理（只写 `liupai`） |
| `Liupai_gudao` 首字母大写坑 | **已处理**：`TianfuTalents.java:735-737` 先试 `liupai_`，失败再试 `Liupai_`；`TianfuDaohen.java:62, 130` 直接写 `Liupai_gudao` |

### 8.2 `Op.FIELD_MULT` / `Op.FIELD_ADD` 的目标字段

| 字段 | 存在 | 类型 | 本体消费点（已核对） |
|---|---|---|---|
| `zuida_jingli` | ✅ | `double` | `KE1Procedure` offset 2089 / 2122 / 2483（getfield） |
| `zuida_hunpo` | ✅ | `double` | 字段存在；**消费点未逐指令核对**（未验证） |
| `hunpochengzaijixian` | ✅ | `double` | 字段存在；**消费点未逐指令核对**（未验证） |
| `niantou_rongliang` | ✅ | `double` | `KE1Procedure` offset 2414 / 2447-2450（作为 `niantou` 的封顶） |
| `niantou_zhida` | ✅ | `double` | `KE1Procedure` offset 2962 / 3067 / 3174（参与念头恢复） |
| `shouyuan` | ✅ | `double` | `KE1Procedure` offset 756 / 1252-1257 / 1673-1695（getfield + putfield） |
| `qiyun_shangxian` | ✅ | `double` | `KE1Procedure` offset 2006-2056：`if (cap < qiyun) { qiyun = cap; markSyncDirty(); }` —— **只做向下削平**，抬上限有效但需等 `qiyun += 0.05`（offset 1006-1013）自然爬升 |
| `daohen_zong` | ✅ | `double` | `ShangHaiLiuCheng1Procedure` offset 62 / 84 / 117 / 140（**杂修项实参**，见 5.4 节） |
| `kongqiao` | ✅ | `double`（可写、非 static、非 final） | `TianfuTalents.java:700-712` `writableAptitudeField()` 显式校验类型与修饰符 |

### 8.3 本体钩子（mixin）结构核验

| 钩子 | 声明要求 | javap 实测 | 结论 |
|---|---|---|---|
| `NativeRecoveryMixin` → `PUTFIELD zhenyuan` | `require = 7, expect = 7, allow = 7`（`NativeRecoveryMixin.java:16`） | `KE1Procedure` 内 `putfield zhenyuan` **7 处**（offset 2263 / 3620 / 3697 / 3774 / 3870 / 3968 / 4066） | **匹配** |
| `NativeRecoveryMixin` → `PUTFIELD niantou` | `require = 4, expect = 4, allow = 4`（`:24`） | `putfield niantou` **4 处**（offset 2450 / 3015 / 3122 / 3229） | **匹配** |
| `NativeRecoveryMixin` → `execute` 描述符 | `execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V` | `public static void execute(LevelAccessor, double, double, double, Entity)` | **匹配** |
| `NativeItemCooldownMixin` → `addCooldown(Item,int)` | `require = 1`（`NativeItemCooldownMixin.java:15`） | 仅对 `guzhenren` 命名空间生效（`:17-18`） | **结构成立**（运行期 patch 成功与否未验证） |
| `ShangHaiLiuCheng1Procedure.execute` 签名（`TianfuZhuCombat.java:176-177` 调用 7 参） | — | `(LevelAccessor, Entity, Entity, double, double, double, String)` = **7 参** | **匹配**（`TianfuZhuCombat.java:38` 的注释写「8 参签名」是过期注释，代码本身正确） |

### 8.4 是否有「写了个本体不读的字段」

**结论：本次审查未发现此类字段。**

- 45 个 `daohen_*` / `46` 个 `liupai_*` **全部是 `PlayerVariables` 的真实 `public double` 字段**（javap 列字段已核对）；
- `applyOnce` / `revokeOnce` 走 `PlayerVariables.class.getField(name)` 反射（`TianfuTalents.java:1116-1133`），字段不存在会**抛异常并让结算回滚**（`TianfuTalents.java:1110-1113` `add()` 抛 `IllegalStateException`；`TianfuApply.java:225-261` 的 `try/catch` 会 `restoreSettlement`）—— 这个设计保证了「不存在则拒绝结算，而不是静默失效」，是好设计；
- `validateNativeDefinitions()`（`TianfuTalents.java:533-550`）在注册表填充后还会二次校验 `FIELD_ADD` / `FIELD_MULT` 的字段类型必须是 `double`。

**需要保留的不确定性**：本次只对 `daohen_zong`、`qiyun_shangxian`、`zhenyuan`、`niantou`、`niantou_rongliang`、`niantou_zhida`、`shouyuan`、`zuida_jingli` **逐指令**核对了消费点；其余 **43 个 `daohen_*` 字段、`zuida_hunpo`、`hunpochengzaijixian`** 只确认了「字段存在 + 写入成功」，**未逐一反编译其消费点** → 列入第 9 章。

---

## 9. 未验证项（需实机）

> 按「影响面 × 不确定性」排序。每条给出**验证方法**，便于 1.4.18 验收。

| # | 未验证项 | 为什么不确定 | 验证方法 | 影响 |
|---|---|---|---|---|
| 1 | **mixin 是否在运行期真正 patch 成功** | javap 静态匹配只证明结构；NeoForge + mixin 的重映射/加载顺序可能导致运行期 `MixinApplyError` | 启动服务端/客户端，`logs/latest.log` 搜 `NativeRecoveryMixin`、`NativeItemCooldownMixin`、`Mixin apply`、`MixinApplyError` | **若失败：真元恢复 / 念头恢复 / 蛊冷却 / 杀招冷却 全部静默失效**（4 个维度、18 条词条） |
| 2 | **buff amplifier 是 +1% 还是 +100%** | `TianfuLiupai.java:39-46` 注释与 `BuffLiuCheng1Procedure` offset 1682-1688 字节码读法**互相矛盾** | 实机：① 无 buff 打木桩记伤害 D；② `/effect give @s guzhenren:jiandaoshanghaizengfu 600 1` 再打，比较是否为 2D 还是 1.01D | 决定 `凡体` 5 条「自带流派增幅常驻」是**超模**还是**亏本**。`amp=0` 的 3 条联动词条**在任何读法下都是 0 收益**（该结论不受影响） |
| 3 | **`guzhenren:texiao`（340 条）能否被玩家正常击杀** | 若这些实体不可攻击 / 不可被玩家击杀，`噬元之性`（3 点）实际收益为 0 | 实机：`/summon guzhenren:bao_yan_1` 后用剑砍，看是否掉血 / 是否触发真元回复 | 决定第 6.2 节第 10 项是否成立 |
| 4 | **`总痕储备 +300` 对实际伤害的方向** | `daohen_zong` 在 `ShangHaiLiuCheng1Procedure` 里进的是「杂修项」，符号未逐指令推导 | 实机对比：买前/买后同一木桩伤害（保持其它条件不变） | 决定第 5.4 节是「描述不符」还是「描述不符 + 负收益」 |
| 5 | **`hunpochengzaijixian`（承载极限）的消费点** | 本报告只确认字段存在与写入，未反编译本体消费逻辑 | `grep`/javap 全 jar 查 `hunpochengzaijixian` 的 getfield 点；或用 `/tianfu` 面板观察承载数值变化 | `hunzai_kuanhou`（2 点，只加承载）、`hunpo_rugui` / `shen_hunxi_zhuzai` 的承载部分可能**零收益** |
| 6 | **43 个 `daohen_*` 字段是否都参与伤害结算** | 只逐指令核对了 `daohen_zong` | 对每个字段 javap 其 getfield 点，或实机对比买前/买后伤害 | 若某流派字段本体不读，则该流派全部「亲和 / 大成」条目（含 200~3000 道痕）**零收益**。这是**最大面积的不确定性**：涉及 43 条词条 + `五行道藏` / `万道归宗` / `众道至尊` / `总痕储备` |
| 7 | **各类上限与其它模组乘区的交互** | 减伤 0.50 / 闪避 0.60 / 暴击 0.50 是**附属自己钳的**，其它模组（恶意 l2hostility 等）也在同一事件上加乘区 | 装全套附属后实测减伤/闪避/暴击读数 | 决定第 5.3 节的「溢出白花」在实战中是否更严重 |
| 8 | **原版 `KNOCKBACK_RESISTANCE` 是否把 1.4 钳到 1.0** | vanilla `AttributeInstance#getValue` 的 clamp 行为未在本版本逐行核对 | 实机：两条都买后读 `getAttributeValue(KNOCKBACK_RESISTANCE)` | 决定 `耸岳坚立`（2 点）是否被 `神·金刚不坏` 完全作废 |
| 9 | **`杀神 ×5.0 × 天机神算 ×4.4 = ×22` 的实际 DPS** | 静态推导未含本体 `ShangHaiLiuCheng1Procedure` 的道痕/流派乘区 | 实机满配打木桩，与同点数的非暴击配置对比 | 决定第 6.1 节的最高超模量级是否需要削 |
| 10 | **`v2_aptitude_shackled` 置 0 后撤销无法恢复** | 静态推导：`multiply(×0.0)` 置 0，`divide()` 遇 `|cur|<1e-6` 直接 return（`TianfuTalents.java:1176-1178`） | 实机：买 → 撤销 → 看 `niantou_rongliang` 是否恢复 | 决定 `空窍桎梏`（负 8 点）是否会造成**永久性容量归零** |

---

## 10. 给 1.4.18 的最小修复建议（按优先级，不改设计只改文案/数值）

| 优先级 | 建议 | 涉及条目 | 理由 |
|---|---|---|---|
| P0 | 3 条 `mob(..., 0)` 的 amplifier 改为 `1`（或按第 9 章第 2 项确认后统一） | `bingxue_daoti` / `hundao_ronghe` / `shen_liupai_jianji` | 现状是**确定性的零收益**，花 5 / 5 / 12 点买图标 |
| P0 | `噬元之性` 的判定 tag 从 `TEXIAO` 改为 `LING`（或两者并集） | `shiyuan_zhixing` | 描述写「灵体」，代码查「特效」；`ling.json` 只有 5 条，`texiao.json` 有 340 条 |
| P1 | 把 6 条蛊冷却的「每 tick −X」改成「冷却 −Y%」 | `v2_mind_cooldown_1/2`、`gudao_dup`、`liangu_bujie`、`shen_liangu_tianshu`、`v2_gu_cd_ningse` | 单位写错，实际语义是除法 |
| P1 | 在描述里补上 5 个硬上限与「反伤仅 PvP / 同类不叠」 | 暴击 4 条、减伤 10 条、闪避 3 条、吸血 2 条、击退 2 条、反伤 5 条 | 玩家会为溢出的部分白花点数 |
| P1 | `v2_mind_recovery_1` 降价到 1 点，或提升到 +40% | `v2_mind_recovery_1` | 与 `念头自生`（1 点 +20%）同效双价 |
| P2 | `v2_mind_capacity_3` 补一条副属性，或降价 | `v2_mind_capacity_3` | 被同价的 `七窍玲珑` 完全支配 |
| P2 | `suhui` 补上「道痕获取」、`suiyue_chendian` 改成真正按寿元缩放 | `suhui` / `suiyue_chendian` | 描述承诺的效果不存在 |
| P2 | 43 条「熟练度效率 +X%」要么实现、要么删掉这半句 | 43 条亲和/大成/道藏 | 全表最大面积的描述空头承诺 |
| P3 | 给 `zhenyuan_ninglian` / `niantou_ninglian` / `shazhao_tiancheng` 的旧存档已购玩家一次性退款 | 3 条 | 点数已扣、零效果、不可撤销 |
| P3 | 修正 `TianfuPool.java:10` / `TianfuTalents.java:22-23,32` 的过期计数注释 | 3 处注释 | 现写 107 / 129 / 22 / 「7 条」，实际 152 / 173 / 21 / 21 |

---

## 11. 附：本报告的证据文件清单

| 用途 | 路径 |
|---|---|
| 清单生成脚本 | `docs/audit-1.4.18/_dump_talents.py` |
| 本报告 | `docs/audit-1.4.18/50-inventory-combat.md` |
| 本体 `PlayerVariables` 字段清单（javap） | `_tmp_verify/playervars.txt`（421 行） |
| 本体 `KE1Procedure`（javap，真元/念头/气运/寿元/精力） | `_tmp_verify/ke1.txt`（3,716 行） |
| 本体 `ShangHaiLiuCheng1Procedure`（javap，`daohen_zong` 杂修项） | `_tmp_verify/shanghai.txt`（1,429 行） |
| 本体 `BuffLiuCheng1Procedure`（javap，amplifier → 乘区） | `docs/audit-1.4.18/_raw_buffliucheng.txt`（既有） |
| 本体 entity_type tag | `_tmp_verify/tags/data/guzhenren/tags/entity_type/{texiao,ling,renzu}.json` |

> `_tmp_verify/` 下的文件为本次审查的 javap 中间产物，不属于交付物；`docs/audit-1.4.18/` 下只新增了本报告与脚本两个文件。
