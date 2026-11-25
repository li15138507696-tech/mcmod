# 40 · 互斥机制 / 经济结算 / 兼容与旧存档迁移（只读审查）

- 审查对象：`蛊真人天命` 1.4.17（工作区现状，目标 1.4.18）
- 源码根：`tianfu\src\main\java\net\guzhenren\tianfu\`
- 本体基线：`_crash3\basever\guzhenren-10.20.jar`，SHA-256 `B6D314D3810B8BC5FCFDBC4F588DB8721F0EFDFC14D27A0A06D88143A01FE2BC`（已本机复核一致）
- 对照基线：`_crash3\basever\guzhenren-9.8.jar`
- 环境：MC 1.21.1 / NeoForge 21.1.x / Java 21
- 本报告为**只读**审查结论；除本文件外未修改任何源码、构建、版本或 docs 既有文件。`docs\audit-1.4.18\10-daohen.md` 由他人产出，本次未触碰。
- 证据分级：**【已验证】**＝代码/字节码直接证据（给 `文件:行号` 或 `javap` 偏移）；**【未验证】**＝需实机压测或需本机不具备的依赖（MC 客户端 jar 等）。

---

## 摘要（结论先行）

1. **互斥覆盖严重不足。** 全库 166 条词条 + 20 凡体 + 11 体质/杀神 + 4 机缘，但互斥族只有 **5 个 family、12 个 id**（`TianfuMindTable.LEVELS`）。除「体质唯一 / 资质档位唯一 / 5 个 v2_mind 族」外，**其余 16 个数值维度 + 1 条负面同属性规则完全没有任何互斥**，其中「突破成功率」与「道痕同流派」可被叠加到数学上限（详见 ①③）。
2. **服务端校验链本身是干净的**：所有 C2S 包只传 `id` 或空负载，价格/净成本一律服务端重算，`revision` CAS 阻断并发双花；客户端**不能**伪造价格、批次或点数。**但「服务端主动放行」的互斥缺口不是绕过，而是功能缺失**；另有一条**真实的迁移时序绕过**：迁移前首个购买包会永久锁死 legacy 账本（详见 ②R1）。
3. **`Guzhenren98Compatibility` 有 SHA-256 校验，且在 10.20 下必然失败。** 9 条常量全部对应 9.8；在 10.20 里 **3/9 不一致**（`UikongqiaoquedingProcedure`、`KE1Procedure`、`UIxiuliananjianProcedure`）。`verify()` 在 `FMLCommonSetupEvent` 抛出 `IllegalStateException` → **1.4.18 若直接换成 10.20 会加载失败**。这是 P0 阻断项。
4. **9 个 Mixin 的注入目标在 10.20 字节码里全部存在，且计数精确匹配**（含 `require/expect/allow` 的 4/7/4/3/2）。**任务书说的「10 个 Mixin」实际只有 9 个文件**。语义层面有一处实质漂移：10.20 把开窍仪式搬进 `UikongqiaoguanbiProcedure`，`UikongqiaoquedingProcedure` 退化为 26 行壳（详见 ③）。
5. **并列主修确实静默认定剑道。** `TianfuMainDao.KEYS` 第 0 位是 `jiandao`（:38），`highest()` 用 `>`（:70）→ 并列时保留先遍历到的 `jiandao`。全 0 时返回空串（不会误判）。该值只用于杀神噬道分流，不会被回写进 `daotu.mainDao`（`fromDaotu` 纯读，:51-62）。
6. **`daohen_zong` 不需要修复路径，但 `zonghen_chubei` 是无效天赋。** 本体 `KE1Procedure` 每 tick 用全部 `daohen_*` 之和 PUTFIELD 重算 `daohen_zong`（10.20 字节码偏移 8433，且全类只有这一处写入 → 不自指、不双计）。故旧档错值自愈；反之 `zonghen_chubei` 的 `+300` 会被下一 tick 覆盖，等于**白扣 5 点**。

---

## ① 互斥覆盖缺口全表

### 1.1 现有互斥机制清单（已验证）

`TianfuEconomy.buy()` 是唯一互斥判定点（`v2\TianfuEconomy.java:93-187`），共 4 条规则：

| # | 规则 | 代码位置 | 覆盖范围 |
|---|---|---|---|
| M1 | 资质档位唯一（丁/丙/乙/甲）+ 与十绝体/空窍桎梏互斥 | `TianfuEconomy.java:128-138` | `v2_apt_*` 4 条 + `v2_aptitude_shackled` |
| M2 | 体质槽位：凡体(BODY) 与十绝体(SIJUE) **合计只允许一个** | `TianfuEconomy.java:145-152` + `isPhysique :62-64` | 20 凡体 + 11 十绝体/杀神 |
| M3 | 同族层级：退款旧价再扣新价、原子替换 | `TianfuEconomy.java:155-172` | **仅 12 个 `v2_mind_*`** |
| M4 | 补点合计 ≤ 24 | `TianfuEconomy.java:175-181` + `TianfuSelection.java:11` | 所有负成本项（但见 ②R1 的漏计） |

- M3 的族表：`v2\TianfuMindTable.java:11-23`，5 族 —— `xiulian`(3)、`capacity`(3)、`recovery`(2)、`liangu`(2)、`cooldown`(2)。
- `TianfuMindTable.java:6` 的注释已自认：**「旧条目不纳入新互斥组」** —— 这就是全部缺口的根因。
- 确认期只复检 M1/M2（`TianfuApply.java:206-207`）；客户端只是镜像同一套（`client\TianfuUiRules.java:16-35`），没有额外规则。
- **M3 没有「只能选最高级」的单调性检查**：已持 `xiulian_3` 仍可买 `xiulian_1`（退款 5 扣 2，降级成功）。`TianfuEconomy.java:155-172` 内无 tier 比较。

### 1.2 缺口全表（16 个数值维度 + 1 条负面规则）

标注：**覆盖**＝该维度是否已有 family；**成员**给 `文件:行号`；数值取自代码。

| # | 维度（聚合点） | 覆盖 | 无 family 覆盖的成员（id:值） | 同维度可叠加后果 |
|---|---|---|---|---|
| 1 | **修炼速度** `Op.XIULIAN` + 硬编码 | 部分（仅 3/9） | `dazuo_wangwo:+.40`(Talents:249)、`xiulian_rushen:+.60`(:288)、`shen_xiulian_wuji:+1.50`(:315)、`ji_zhongdao_zhizun:+2.00`(:329)、`kuxiu_buchuo:+.20`(Progression:19)、`suiyue_chendian:+.10`(:20)、`suhui:+.05×转数`(:18)、负：`v2_xinzhi_yousun:-.40`(:156)、`xiuxing_chizhi:-.30`(:343) | 加算求和（`TianfuTalents.java:776-778`），可叠到 **+540%** |
| 2 | **炼蛊进度** `Op.LIANGU` | 部分（仅 2/5） | `liangu_rushen:+.40`(:250)、`liangu_bujie:+.60`(:290)、`shen_liangu_tianshu:+.90`(:317) | 加算（:786-788）→ **+220%**（另有本体 `==100.0` 精确完成判定，见 skill 记录） |
| 3 | **蛊冷却** `Op.GU_CD` | 部分（仅 2/6） | `gudao_dup:+.25`(:251)、`liangu_bujie:+.30`(:290)、`shen_liangu_tianshu:+.50`(:317)、负：`v2_gu_cd_ningse:-.10`(:151) | rate=1+Σ → **2.30**，`ceil(ticks/rate)` 后 CD≈43%（`Guzhenren98Progression.java:57-62`） |
| 4 | **突破成功率** `Op.BREAKTHROUGH` | **无** | `tupo_tongtian:+.30`(:248)、`houji_bofa:+.20`(Progression:38)、`xiulian_rushen:+.40`(:288)、`shen_xiulian_wuji:+.60`(:315)、`ji_zhongdao_zhizun:+.80`(:329)、负：`v2_xinzhi_yousun:-.50`(:156)、`xiuxing_chizhi:-.40`(:343) | 加算 → Σreduction=2.30；`breakthroughChance` 用 `max(0,1-reduction)`（`Guzhenren98Progression.java:44-48`）→ **失败率被压到 0（100% 成功）**，代价仅 3+2+5+12+16=38 点 |
| 5 | **念头容量** `niantou_rongliang` | 部分（仅 3/8） | `niantou_kuozhan:×1.15`(:244)、`qiqiao_linglong:×1.50`(:272)、`shen_hunxi_zhuzai:×2.0`(:307)、`v2_aptitude_shackled:×0`(:157)、负：`nianfu_xiayai:×0.75`(:342) | 写字段前逐个连乘（`TianfuTalents.applyOnce` 的 FIELD_MULT），见 1.3 |
| 6 | **念头恢复** `Op.REGEN_NT` | 部分（仅 2/6） | `niantou_zisheng:+.20`(:217)、`niantou_ruyuan:+.60`(:254)、`niantou_bujie:+1.20`(:281)、负：`niantou_ganhe:-.30`(:334) | 加算 → **+180%** |
| 7 | **道痕·同流派** `Op.DAHEN`/`DAOHEN_ALL` | **无** | 亲和 200（36 条，:175-212、:220-229 等）、大成 800（:266-268）、支系 500（:185-186）、全流派 `wuxing_daocang:+300`(:271)、`shen_wandao_guizong:+1500`(:319)、`ji_zhongdao_zhizun:+3000`(:329) | 同一流派可叠 **200+800+300+1500+3000=5800**；再叠凡体 `TianfuDaohen.java:64` +1000 → **6800** |
| 8 | **生命上限** `Op.HEALTH_MULT` | **无** | `xueqi_rulin:×1.2`(Talents:233)、`ji_wanshi_bumie:×2.5`(:327)、`shen_bumie_jinshen:×2.0`(池条目 `TianfuPool.java:66`，效果硬编码 `TianfuAttributes.java:54-56`)、`jingu_qiangjian:×1.1`(`TianfuAttributes.java:57-59`)、负：`routi_chanruo:×0.8`(:335)、`v2_routi_canque:×0.5`(:153) | **乘算连乘**（`TianfuTalents.attrs` `health *= 1+a`）→ 1.2×2.0×2.5×1.1 = **6.6×** |
| 9 | **真元回复** `Op.REGEN_ZY` | **无** | `zhenyuan_huiliu:+.35`(:245)、`zhenyuan_ruquan:+.80`(:253)、`zhenyuan_bujie:+1.50`(:280)、负：`zhenyuan_kujie:-.30`(:333)、`v2_zhenyuan_kubai:-.75`(:152) | 加算 → **+265%** |
| 10 | **寿元** `fldAdd shouyuan` | **无** | `chuntian_yishou:+80`(:238)、`shouyuan_ruhe:+200`(:277)、`shouyuan_mianchang:+50`(`TianfuEffects.java:63-65`)、负：`shouyuan_duancu:-100`(:338)、`v2_shouyuan_kuwei:-250`(:154)、`v2_lifespan_scar:-400`(:159) | 加算；负项叠加若不被 24 上限拦住可自我处死（见 ②R1） |
| 11 | **气运上限** `qiyun_shangxian` | **无** | `qiyun_weiying:×1.12`(:239)、`qiyun_yingmen:×1.18`(:241)、`qiyun_ruhong:×1.40`(:278)、`qiyun_yizhi:×1.30`(:301)、`shen_qiyun_zhizi:×1.80`(:305)、`fuze_mianchang:+15`(`TianfuEffects.java:60-62`)、`qiyun_weiguang:+5`(:66-68)、负：`qiyun_xibo:×0.85`(:339)、`v2_qiyun_duanjue:×0.625`(:155)、`v2_luck_forfeit:归零`(:158) | 乘法连乘 + 加算混用，同一字段双重口径 |
| 12 | **暴击率 / 暴伤** | **无** | `baoji_rulie:+率.15`(:257)、`baoji_rushen:+率.25/伤1.20`(:283)、`baoji_jingtong`(`TianfuCombat.java:266,276`)、`jianbi_qingfeng`(`TianfuCombat.java:277`) | 加算叠加，无 cap |
| 13 | **闪避** `Op.DODGE` | **无** | `shanbu_suifeng:+.06`(:232)、`shenfa_ruyan:+.12`(:256)（+ 体质侧 `TianfuAttributes.java:51-53`） | 加算到 18% 以上不受限 |
| 14 | **非护甲减伤** `Op.REDUCTION` | **无** | `tiepi_rougu:+.04`(:213)、`ruwei_dingshen:+.08`(:231)、`jianjia_ruyan:+.15`(:261)、`jianjia_rushen:+.25`(:287)、`shen_busi_bumie:+.30`(:309)、`shen_jingang_buhuai:+.35`(:313)、`ji_wanshi_bumie:+.40`(:327) | 加算 → **>100% 减伤**，需本体有无下限 clamp 才能兜住（未验证） |
| 15 | **魂魄上限/承载** | **无** | `hunpo_lianxi:×1.15`(:242)、`hunzai_kuanhou:×1.15`(:243)、`hunpo_rugui:×1.50`(:274)、`shen_hunxi_zhuzai:×2.0`(:307)、负：`hunli_shuairuo:×0.75`(:340) | 连乘 |
| 16 | **精力上限** `zuida_jingli` | **无** | `jingli_shengheng:×1.30`(:237)、`jingli_ruhai:×1.80`(:276)、负：`v2_jingli_buji:×0.8`(:150) | 连乘 → 2.34× |
| 17 | **负面·同属性重复** (跨维度) | **无** | 6 组同维度正负/双负：生命上限 `v2_routi_canque`(:153)↔`routi_chanruo`(:335)；真元 `v2_zhenyuan_kubai`(:152)↔`zhenyuan_kujie`(:333)；修炼+突破 `v2_xinzhi_yousun`(:156)↔`xiuxing_chizhi`(:343)；念头恢复 `niantou_ganhe`(:334) 与容量 `nianfu_xiayai`(:342) 同姓不同维；寿元 `v2_shouyuan_kuwei`(:154)↔`shouyuan_duancu`(:338)；气运 `v2_qiyun_duanjue`(:155)↔`qiyun_xibo`(:339) | 同一负面属性可被买两次 → 补点翻倍而惩罚近似重复，属于**刷补点**面 |

> 注：`shen_bumie_jinshen` 在 `TianfuPool.java:66`（池内条目，`+100%` 生命上限），与 `TianfuTalents` 中同名 id **不存在重名冲突**（池条目无 fx，效果在 `TianfuAttributes.java:54-56` 硬编码）。该条目的成本/价目走 `TianfuPool`。

### 1.3 复合叠加示例（已验证的算术链）

| 组合 | 点数 | 结果 |
|---|---|---|
| 修炼：`v2_mind_xiulian_3`+`xiulian_rushen`+`shen_xiulian_wuji`+`ji_zhongdao_zhizun`+`dazuo_wangwo`+`kuxiu_buchuo` | 5+5+12+16+3+2=43 | bonus = .60+.60+1.50+2.00+.40+.20 = **+530%**（`TianfuNativeProgression.java:17-20` 加算 → `Guzhenren98Progression.cultivation` 的 `1+bonus`） |
| 突破：`tupo_tongtian`+`houji_bofa`+`xiulian_rushen`+`shen_xiulian_wuji`+`ji_zhongdao_zhizun` | 3+2+5+12+16=38 | reduction=2.30 → `max(0,1-2.30)=0` → **成功率=1.0（必成）**（`Guzhenren98Progression.java:44-48`） |
| 道痕：`jiandao_qinhe`+`jiandao_dacheng`+`wuxing_daocang`+`shen_wandao_guizong`+`ji_zhongdao_zhizun`（+凡体 断岳剑心体） | 1+3+5+12+16 +3 | 剑道道痕 **5800 + 1000 = 6800**（其中凡体侧 `TianfuDaohen.java:64`） |
| 生命：`xueqi_rulin`+`shen_bumie_jinshen`+`ji_wanshi_bumie`+`jingu_qiangjian` | 2+12+16+1=31 | **1.2×2.0×2.5×1.1 = 6.6 倍**生命上限（`TianfuTalents.attrs` 连乘 + `TianfuAttributes.java:54-59`） |

---

## ② 服务端校验链与绕过路径

### 2.1 包入口 → Economy 调用链（已验证）

| 入口包 | 负载 | 处理函数 | 迁移 | 门禁 | 最终提交点 |
|---|---|---|---|---|---|
| `TianfuBuyPayload` | 只有 `String id`（`nete\TianfuBuyPayload.java:25-31`，`writeUtf(64)` 有界） | `:39-50` → `TianfuApply.buyReason` | **无 migrate**（只调 `sendAll`; `TianfuNet.java:31-42` 不含 migrate，migrate 仅在 `openSelection :47`） | `TianfuApply.java:109-111`（`isApplied && !confirmed`）、`:112-115` 读快照、`:115` `TianfuEconomy.buy` | `TianfuApply.java:119-122` `commit` + revision CAS |
| `TianfuRevokePayload` | `String id`（`:17-23`） | `:31-37` → `TianfuApply.revoke` | **无 migrate** | `TianfuApply.java:147`（confirmed/settled 拒绝）、`:151-155`（SIJUE/JIYUAN 拒撤）、`:158-159` 按 Receipt 原价 | `:163-166` `commit` |
| `TianfuConfirmPayload` | 空（`:26-32`） | `:40-74` → `TianfuApply.confirmFinal` | `:49` migrate | `:45` 开窍门禁、`:51-54` 幂等短路 | `TianfuApply.settleSelection:246-250` |
| `TianfuRollAcceptPayload` | 空（`:33-39`）**不接受客户端批次** | `:47-74` | `:61` migrate | `:52`、`:56-60`、`:62` 从**服务端** `rollpending` 读批 | `TianfuData.applyRoll:325-326` → `acceptRoll` |
| `TianfuRollPayload` | 空 | `:56`、`:71` | `:71` migrate | 已确认/已结算拒绝 | `TianfuData.setRollPending` |
| `TianfuOriginPayload` / `TianfuDifficultyPayload` | id / index | `:36,:40` / `:41,:56` | 有 | 已确认拒绝 | `TianfuEconomy.selectOrigin:259-306` |

**结论（已验证）**：

1. **价格不可伪造。** 所有包都不携带价格；`catalogCost`（`TianfuEconomy.java:33-40`）只查 `TianfuPool` / `TianfuTalents` 目录。`TianfuData.purchase` / `revoke` / `applyRoll` 的 `cost`/`netCost` 参数被显式标注为「仅兼容参数」（`TianfuData.java:220`、`:244`、`:317`）。
2. **退款不可伪造。** 只认 `Receipt.paidCost`（`TianfuEconomy.java:207-212`、`TianfuV2Store.java:51-57`）；无成交账 → `LEGACY_PRICE_UNKNOWN`（:209-211），不猜价。
3. **并发不可双花。** `commit` 先比 `current.revision() != expectedRevision` 直接返回 false（`TianfuV2Store.java:246-249`），调用方各自读旧 revision（`TianfuApply.java:112-121`、`:156-165`）→ 并发第二个包必失败。
4. **随机批次不可伪造。** `TianfuRollAcceptPayload` 无负载，批次来自服务端 `rollpending`（`:62`），且 `acceptRoll` 逐条重跑 `buy` 并对整批失败零写入（`TianfuEconomy.java:327-336`）。
5. **补点账一致性双检。** `commit` 用 `deriveGrant` 反算，与快照不符即抛（`TianfuV2Store.java:254-258`）。

### 2.2 绕过与缺陷路径

| 编号 | 路径 | 证据 | 等级 |
|---|---|---|---|
| **B1** | **互斥缺口不是「绕过」而是「服务端主动放行」**：伪造/正常发包都能买下同维度两条，因为 `buy()` 只查 M1-M4。客户端镜像同样放行（`TianfuUiRules.java:16-35`），UI 不会置灰。 | `TianfuEconomy.java:128-172` | **高（功能）** |
| **B2** | **迁移前首包可永久锁死 legacy 账本**：`buyReason` 不做 migrate（`TianfuApply.java:103-125`），而 `commit` **无条件硬写 `SCHEMA=2`**（`TianfuV2Store.java:267`）。后果链：① `migrate` 之后永远跳过（`:185` 的 `schema>=2` 门禁）→ ② 旧 `purchased` 的 Receipt **永不建立** → ③ `deriveGrant` 只算 Receipt（`:101-111`）→ 旧负面项的补点**永久计 0**，24 上限被实质抬高；④ 旧条目永久无法撤销（`LEGACY_PRICE_UNKNOWN`）；⑤ 旧 `v2_mind_*` 无法被同族替换（`TianfuEconomy.java:162-165`）。触发条件：客户端**不打开界面**直接发 buy 包（`openSelection` 是唯一迁移入口 `TianfuNet.java:45-48`）。 | `TianfuV2Store.java:185,267` + `TianfuApply.java:103-125` + `TianfuNet.java:47` | **高（经济/迁移）** |
| **B3** | `sendAll`（每个包末尾都会调）**不迁移**，只有 `openSelection` 迁移 → B2 的窗口不止「第一次」，而是「任何未开界面就发包」的场景。 | `TianfuNet.java:31-42` vs `:45-54` | 中（B2 的放大器） |
| **B4** | 无按玩家的包频控：buy/revoke 循环每次 `commit` 都复制并重写整个 `tianfu` 根（`TianfuV2Store.java:260-293`）。 | 同左 | 中-低（性能，**未验证**实机影响） |
| **B5** | `TianfuApply.settleSelection:193` 用 `TianfuTalents.isAvailable(id)` 而非 `byIdIncludingUnavailable`，而 `NATIVE_UNAVAILABLE` 含 3 个 id（`TianfuTalents.java:40-43`）→ **已购这 3 条的旧玩家永远无法最终确认**（永久 `UNVERIFIED_EFFECT`）。 | `TianfuApply.java:193` + `TianfuTalents.java:40-43`、`:522-525` | **高（卡号）** |
| **B6** | 服务端不校验「所购 id 是否属于当前可见页面」：`malice_*`、`v2_apt_*` 等可被直接发包购买（只在 `availableForPurchase` 处按附属安装态过滤）。 | `TianfuEconomy.java:43-46` | 低（不构成刷取） |

---

## ③ 兼容性核查

### 3.1 `Guzhenren98Compatibility` 是什么

- 用途：启动期**语义指纹门禁**。在 `FMLCommonSetupEvent` 的 `enqueueWork` 中调用（`TianfuMod.java:46-51`，调用点 `:48`），对该 mod 强依赖的 9 个本体类做 `.class` 字节 SHA-256 比对，再反射校验 `PlayerVariables` 字段类型（`Guzhenren98Compatibility.java:41-52`）；通过后置 `verified=true` 并打日志（`:53-54`）。
- **有 SHA-256 校验**，常量位置 `Guzhenren98Compatibility.java:16-25`，比较点 `:34-36`：

```
同文件 常量（16-25 行）
UikongqiaoquedingProcedure        E1707AA64910048CA7ED76B37F3DA7467DE0CD589C3632E901A4A8B3E1E7912F
JingJieBaoCunProcedure            F991FBC556CD1B2D6162C133FE98C3E6A99799196603E3A29B36E715988866D2
KE1Procedure                      772225C51A12CBC2C408C6438FE4EE06D0B41977B10D38D088E7661D0F4F64DA
UIxiuliananjianProcedure          B03E83AB4E937B85E918B9CC93F6CAF98110F14C861512D4D8C3ECFDD0D10162
XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure
                                  BACAB72763BC9D1BA6DE1ABE30332A206640FB6BCF1221E3F24EC6CA5D9B1D04
TianziliuchengProcedure           B585C12B5E217B5F70E2D217C0D8FC3AC17D1D6015E6C74E4FEABEB48953E590
SxProcedure                       07447FC82B0C8AFEA25E9BC7007E3AB5012A1A1724C0C41E6E4420CC00AC8E81
YizhuanrendaoxiwangguItem         8A6D7035C1D510459FC63B8FB98E5C8DA69E1A15BFDDC633DB1B6A2B7D578274
GuzhenrenModVariables$PlayerVariables
                                  9EFE0F2C17F1EE5B6120EECE3D1C953705D7A965DA2E559FAB83E2344638ECFF
```

### 3.2 10.20 实测比对（已验证：本机提取 `.class` 计算 SHA-256）

| 类 | 9.8 jar | 10.20 jar | 判定 |
|---|---|---|---|
| `procedures/UikongqiaoquedingProcedure` | MATCH | **307F6645…AD74E37** | **不一致** |
| `procedures/JingJieBaoCunProcedure` | MATCH | MATCH | 一致 |
| `procedures/KE1Procedure` | MATCH | **F0908D99…8D30A0DC** | **不一致** |
| `procedures/UIxiuliananjianProcedure` | MATCH | **1060052B…89B7230B** | **不一致** |
| `procedures/XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure` | MATCH | MATCH | 一致 |
| `procedures/TianziliuchengProcedure` | MATCH | MATCH | 一致 |
| `procedures/SxProcedure` | MATCH | MATCH | 一致 |
| `item/YizhuanrendaoxiwangguItem` | MATCH | MATCH | 一致 |
| `network/GuzhenrenModVariables$PlayerVariables` | MATCH | MATCH | 一致 |

结论：
- **常量确实是 9.8 基线**（9/9 对 9.8 全部 MATCH）。
- **10.20 下 3/9 不一致** → `verify()` 在 `:36` 抛 `incompatible(...)` → `:57-59` 的 `IllegalStateException`，冒泡出 `commonSetup` → **加载失败**。1.4.18 直换 10.20 必须重钉这 3 条（或改成版本区间 + 字段探测）。
- `PlayerVariables` 一致 → `:42-49` 的字段级断言（`shiqiao` boolean + 11 个 double）在 10.20 通过；`TianfuTalents.validateNativeDefinitions`（`TianfuTalents.java:533+`）同样通过。
- **【未验证】** NeoForge 对该异常的具体处置（是「mod 加载失败」还是「仅该回调抛异常并继续」）需实机确认；按 NeoForge 21.1 惯例，`enqueueWork` 内未捕获异常会终止 mod 加载。

### 3.3 Mixin 目标表（9 个文件；任务书所称「10 个」有误）

核验方法：`javap -p -c -classpath guzhenren-10.20.jar` 全量反汇编后按方法归属统计（脚本见附注）。

| # | Mixin 文件 | 注入目标 | 方法 / 注入点 | require / expect / allow | 10.20 实测 | 判定 |
|---|---|---|---|---|---|---|
| 1 | `CultivationButtonMixin` | `net.guzhenren.procedures.UIxiuliananjianProcedure` | `execute(LevelAccessor,DDD,Entity)V` / `@Inject HEAD` | 1 | 方法存在 | ✅ |
| 2 | 同上 | 同上 | `@Redirect INVOKE Math.random()D` | 4 / 4 / 4 | **实测 4 处**（偏移 465/927/1662/2124） | ✅ |
| 3 | `CultivationProgressMixin` | `...XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure` | `@Redirect PUTFIELD gushi_xiulian_dangqian:D` | 3 / 3 / 3 | **实测 3 处** | ✅ |
| 4 | 同上 | 同上 | `@Inject RETURN` | 1 | 方法存在 | ✅ |
| 5 | `JingJieBaoCunProcedureMixin` | `...JingJieBaoCunProcedure`（`private static execute(Event,LevelAccessor,Entity)V`） | `@Inject HEAD`（capture） | 1 | 私有重载存在 | ✅ |
| 6 | 同上 | 同上 | `@Inject FIELD shiqiao:Z opcode=181 shift=AFTER` | 2 / – / 2 | **实测 2 处**（偏移 721/1278） | ✅ |
| 7 | 同上 | 同上 | `@Inject RETURN`（release） | 1 | 方法存在 | ✅ |
| 8 | `NativeRecoveryMixin` | `...KE1Procedure` | `@Redirect PUTFIELD zhenyuan:D` | 7 / 7 / 7 | **实测 7 处** | ✅ |
| 9 | 同上 | 同上 | `@Redirect PUTFIELD niantou:D` | 4 / 4 / 4 | **实测 4 处** | ✅ |
| 10 | `UikongqiaoquedingProcedureMixin` | `...UikongqiaoquedingProcedure` | `@Redirect INVOKE Player.closeContainer()V` | 1 | **实测 1 处**（偏移 21-23） | ✅（脆，见 3.4） |
| 11 | 同上 | 同上 | `@Inject RETURN`（`onCloseOpenQiao`） | 1 | 方法存在 | ✅ |
| 12 | `UishuxingshijuetiProcedureMixin` | `...UishuxingshijuetiProcedure` | `execute(Entity)Ljava/lang/String;` / `@Inject HEAD cancellable` | 1 | 签名一致 | ✅ |
| 13 | `YizhuanrendaoxiwangguItemMixin` | `net.guzhenren.item.YizhuanrendaoxiwangguItem` | `use(Level,Player,InteractionHand)` / `@Inject HEAD cancellable` | 1 | 签名一致 | ✅ |
| 14 | `NativeItemCooldownMixin` | `net.minecraft.world.item.ItemCooldowns` | `addCooldown(Item;I)V` / `@ModifyVariable HEAD` | 1 | MC 侧，**未验证** | ⚠ 未验证 |
| 15 | `ServerItemCooldownsAccessor` | `net.minecraft.world.item.ServerItemCooldowns` | `@Accessor("player")` | – | MC 侧，**未验证**（本机无 1.21.1 client jar） | ⚠ 未验证 |

> 注：`CultivationButtonMixin.java` 内 `@Redirect` 的处理器签名（`LevelAccessor, double x3, Entity`）未携带被 Redirect 的 `Math.random()` 返回值作为首个参数——`@Redirect` 的处理器**不需要**原返回值，故形态正确；此处仅记录以备复核。

### 3.4 语义漂移：`UikongqiaoquedingProcedure`（已验证，字节码级）

| 项 | 9.8 | 10.20 |
|---|---|---|
| 反汇编规模 | 1215 行 | **26 行** |
| 行为 | `closeContainer` → 随机 `kongqiao`（`Mth.nextInt`）→ `markSyncDirty` → `queueServerWork(10,…)` 30 tick 链 → `guzhenren:cchengjiushijietianzi` 成就 → `TianziliuchengProcedure.execute` → 更多随机（`zuida_zhenyuan`/`qiyun`/`mingge`/`qiyun_shangxian`） | `if (entity==null) return;` → `UikongqiaoguanbiProcedure.execute(world,entity)` → `if (entity instanceof Player) ((Player)e).closeContainer()` |
| 仪式本体所在 | 本类 | **`procedures/UikongqiaoguanbiProcedure`（1207 行，10.x 新增）** |

- `TianfuOpening.onUseHopeGu` 仍调用 `UikongqiaoquedingProcedure.execute`（`TianfuOpening.java:90`）。在 10.20 下它等价于「`UikongqiaoguanbiProcedure` + 关容器」。
- **关键缓解事实**：`UikongqiaoguanbiProcedure` 的随机体质分支被 `kongqiao == 3.0` 严格门控（字节码偏移 15-22：`getfield kongqiao:D` → `dcmpl 3.0` → `ifne 170`），而附属在开窍时**强制 `vars.kongqiao = 0.0`**（`TianfuOpening.java:89`）→ 随机 `tizhi`/`zuida_zhenyuan`/`qiyun`/`mingge`/`qiyun_shangxian` 分支全部跳过。**故 10.20 下开窍行为与 9.8 近似等价**，`closeContainer` Redirect 仍命中 1 处。
- 风险点：该 Redirect 依赖 `queding` 里的**一行**转发调用；10.20 已把它压成壳，10.21 若再删这行则 Mixin 直接 `InjectionError`。建议同时给 `UikongqiaoguanbiProcedure` 加同样 Redirect（`require = 0`）。

---

## ④ 迁移与幂等性风险

### 4.1 迁移机制现状（已验证）

| 机制 | 位置 | 说明 |
|---|---|---|
| 版本标记 | `TianfuV2Store.java:27` 键 `v2_schema`；写 `:211`（migrate）/ `:267`（commit）；读 `:128`；门禁 `:185` | **唯一** marker，`SCHEMA=2`（`TianfuSelection.java:10`），**没有**逐条目 marker |
| 迁移建账 | `TianfuV2Store.java:182-223` | 旧价查冻结表 → 目录价（`:162-173`）；查不到则 `continue`（`:198-201`）**不建账**；**只建账不动余额**；`legacy=true` |
| 事务序号 | `:32` 键 `v2_revision`；`:246-249` CAS；`:268` 写 | 每次成功变更 +1 |
| 结算幂等键 | `Snapshot.originEffectKeys`（`TianfuSelection.java:30`）；结算时写入 `settlement_complete`（`TianfuApply.java:241-245`）；随 `tianfu` 根一起被转世清除（`TianfuData.java:159-161`） | 仅用于出身/效果，**不用于账本** |
| 已结算标记 | `TianfuData.isSettled/markSettled`（`TianfuData.java:87-94`） | 键 `settled` |

### 4.2 风险清单

| 编号 | 场景 | 代码路径 | 是否可能重复退款/重复结算 | 等级 |
|---|---|---|---|---|
| **R1** | 未迁移先购买 → 永久跳过 migrate | `TianfuApply.java:103-125`（无 migrate）→ `commit` 硬写 schema（`TianfuV2Store.java:267`）→ `migrate` 门禁（`:185`） | **不会重复退款**，但会造成「24 上限永久少算 + 旧条目永久不可撤销 + 旧族不可替换」（见 ②B2） | **高** |
| **R2** | 已购条目在 `NATIVE_UNAVAILABLE` | `TianfuApply.java:193` + `TianfuTalents.java:40-43` | 不重复结算，但**永久无法确认**（卡号） | **高** |
| **R3** | 转世期间崩溃 / 服务器重启 | capture 存 `ThreadLocal<WeakHashMap>`（`TianfuReincarnation.java:14-15`、`:20-26`）；`complete` 取不到状态即 `return`（`:30-31`） | **不会重复退款**（不涉及退款），但会**漏 wipe**：本体已转世而 `tianfu` 根（点数/purchased/confirmed/settled）原封不动 → 下世仍持有点数与已确认天赋 | 中 |
| **R4** | `complete` 单次调用被执行两次 | `@Inject` 落在**两个** `PUTFIELD shiqiao:Z`（`JingJieBaoCunProcedureMixin.java` 第 2 个 @Inject，`allow=2`） | **幂等**：第一个执行后 `BEFORE_RESET.remove(player)` 已删（`:30`），第二个直接 return | 低（已验证幂等） |
| **R5** | 同 tick 双开背包/连点撤销 | `TianfuApply.java:156-166` 各自读旧 revision → `commit` CAS 失败（`TianfuV2Store.java:246-249`） | **不能**重复退款 | 低（已验证） |
| **R6** | 第三方 UI 只写 `confirmed` 不结算 → 自愈重试 | `TianfuEffects.java:146-165`：`confirmed && !settled && tickCount%20==0` → `applyAllConfirmed` | 正常路径**不会**重复结算（`settleSelection:185-186` 早退）；失败时 `restoreSettlement` 回滚本体字段/根/计分板/成就（`TianfuApply.java:226-233`、`:256-261`、`:324-339`）。风险是**确定性失败时每 20 tick 重试一次**（日志与性能） | 中 |
| **R7** | 死亡（非转世） | `TianfuOpening.onClone`（`:215-227`）复制 `tianfu` 根 | 无退款、无重复结算 | 低 |
| **R8** | 服务端重启 | `settled`/`confirmed`/`schema` 均落 NBT | 无重复结算 | 低 |
| **R9** | `daohen_zong` 旧档错值 | 无附属侧重算；本体 `KE1Procedure` 每 tick 重算（字节码偏移 8433，全类唯一写入点，且**无 `getfield daohen_zong`** → 不自指/不双计） | 自愈，无需修复路径 | 无（已验证） |
| **R10** | `zonghen_chubei`（`TianfuTalents.java:190`，`fldAdd("daohen_zong",300)`） | 写入被 R9 的每 tick 重算覆盖 | 花 5 点买不到东西（效果被抹除）；`TianfuSlay.java:209-210` 的注释已自认此机制 | 中（经济） |

### 4.3 `daotu.mainDao` 读取与并列值（任务书点名项，已验证）

```
TianfuMainDao.java:37-39  KEYS = ["jiandao", "jindao", "shuidao", ..., "tudao"]
TianfuMainDao.java:45-48  of() = fromDaotu(...) 非空则用之，否则 highest(v)
TianfuMainDao.java:51-62  fromDaotu(): 纯读 PersistentData/daotu/mainDao，不在清单内返回 ""
TianfuMainDao.java:65-76  highest(): bestValue 从 0.0 起，判据 `value > bestValue`（:70）
```

- **并列时确实取 `KEYS` 中靠前者 = `jiandao`（剑道）**：`:70` 的严格大于使并列候选全部被跳过，`KEYS[0]` 即剑道。任务书所述 bug **成立**。
- **全 0 时返回空串**：`bestValue` 初值 0.0，判据 `>` 使 0 不进入分支 → `best=""`，调用方按「未设定」处理（`TianfuSlay.java:213-216`：空串 → 退回 7 路均分）。**不会**把全 0 误判成剑道。
- **主修不会被自动改写**：`fromDaotu` 只有 `getString`（`:57`），全类无任何 `putString/put`（`TianfuMainDao.java` 全文 99 行无写入），亦无其它类写 `daotu`（全项目 grep 仅命中本类的读取）。**故并列时是「静默按剑道分流」，而不是「篡改玩家选择」**。
- **实际影响面**：仅 `tianfu` 根 + 杀神（`tizhi=54`）的噬道分流（`TianfuSlay.java:212-225` 经 `TianfuDaohen.addDaohen(:97-122)`）。并列玩家（两路熟练度恰好相等，例如全新号全 0 之外的等值场景）会把击杀道痕全部灌进剑道。

---

## ⑤ 建议的 family 划分与迁移方案

### 5.1 规则原语（建议在 `TianfuMindTable` 之上扩展为三类表）

| 规则 | 语义 | 适用 |
|---|---|---|
| `TIER`（等级族） | 同族**只保留一条**，允许替换；可选「只升不降」 | 有层级的维度（修炼/炼蛊/冷却/容量/恢复/突破/道痕/生命/…） |
| `EXCLUSIVE`（互斥族） | 同族**只允许一条**，无层级语义 | 同类互斥但无强弱关系（如 6 组同属性负面） |
| `CAP`（数值上限） | 族内允许叠加但总和封顶 | 减伤/闪避/暴击/气运上限这类「叠加但需封顶」 |

### 5.2 建议族表（T = TIER，E = EXCLUSIVE，C = CAP）

| 族名 | 类型 | 成员（现全部无覆盖的已列出） |
|---|---|---|
| `xiulian` | T（扩员，只升不降） | 现有 3 + `dazuo_wangwo`、`xiulian_rushen`、`shen_xiulian_wuji`、`ji_zhongdao_zhizun`、`kuxiu_buchuo`、`suiyue_chendian`、`suhui` |
| `liangu` | T（扩员） | 现有 2 + `liangu_rushen`、`liangu_bujie`、`shen_liangu_tianshu` |
| `cooldown` | T（扩员） | 现有 2 + `gudao_dup`、**`liangu_bujie`（跨族，见 5.3）**、`shen_liangu_tianshu`、`v2_gu_cd_ningse` |
| `capacity`（念头容量） | T（扩员） | 现有 3 + `niantou_kuozhan`、`qiqiao_linglong`、`shen_hunxi_zhuzai` |
| `recovery`（念头恢复） | T（扩员） | 现有 2 + `niantou_zisheng`、`niantou_ruyuan`、`niantou_bujie` |
| `breakthrough` | T（新建） | `tupo_tongtian`、`houji_bofa`、`xiulian_rushen`、`shen_xiulian_wuji`、`ji_zhongdao_zhizun` |
| `daohen_<liupai>`（44 个族，按流派键） | T（新建，**只可选最高级**） | 每族：`<X>_qinhe`(200) → `<X>_dacheng`(800)（剑/雷/力三族有二级）；`jindao2_jinjie`/`yundao2_jinjie` 各自独立族 |
| `daohen_all` | T（新建） | `wuxing_daocang`(300) → `shen_wandao_guizong`(1500) → `ji_zhongdao_zhizun`(3000)（**只可选最高级**，且与各族**另行**叠加规则需明确） |
| `health` | C（新建，封顶如 3.0×） | `xueqi_rulin`、`shen_bumie_jinshen`、`ji_wanshi_bumie`、`jingu_qiangjian` |
| `zhenyuan_regen` | C/T（新建） | `zhenyuan_huiliu`、`zhenyuan_ruquan`、`zhenyuan_bujie` |
| `shouyuan` | C（新建，上限如 +400） | `chuntian_yishou`、`shouyuan_ruhe`、`shouyuan_mianchang` |
| `qiyun_cap` | C（新建） | `qiyun_weiying`、`qiyun_yingmen`、`qiyun_ruhong`、`qiyun_yizhi`、`shen_qiyun_zhizi`、`fuze_mianchang`、`qiyun_weiguang` |
| `crit_rate` / `crit_damage` | C（新建，两个独立族） | `baoji_rulie`、`baoji_rushen`、`baoji_jingtong` / `baoshang_rufeng`、`baoji_rushen`、`jianbi_qingfeng` |
| `dodge` | C（新建，封顶如 0.35） | `shanbu_suifeng`、`shenfa_ruyan` |
| `reduction` | C（新建，封顶如 0.70） | `tiepi_rougu`、`ruwei_dingshen`、`jianjia_ruyan`、`jianjia_rushen`、`shen_busi_bumie`、`shen_jingang_buhuai`、`ji_wanshi_bumie` |
| `hunsoul` | C（新建） | `hunpo_lianxi`、`hunzai_kuanhou`、`hunpo_rugui`、`shen_hunxi_zhuzai` |
| `jingli` | T（新建） | `jingli_shengheng`、`jingli_ruhai` |
| `neg_<attr>`（6 族） | E（新建） | 生命：`v2_routi_canque`↔`routi_chanruo`；真元：`v2_zhenyuan_kubai`↔`zhenyuan_kujie`；修炼/突破：`v2_xinzhi_yousun`↔`xiuxing_chizhi`；寿元：`v2_shouyuan_kuwei`↔`shouyuan_duancu`；气运：`v2_qiyun_duanjue`↔`qiyun_xibo`；念头：`nianfu_xiayai`↔`niantou_ganhe`（后者按属性判，可归 `recovery` 族负项） |

### 5.3 需要先定夺的三处设计冲突

1. **跨族条目**：`liangu_bujie`(`LIANGU`+`GU_CD`)、`shen_liangu_tianshu`(同)、`shen_xiulian_wuji`(`XIULIAN`+`BREAKTHROUGH`)、`ji_zhongdao_zhizun`(三属性)。**一个 id 只能落入一个族** → 建议给族表加「多族成员」支持（`Map<String, Set<String>> memberships`），或把这些复合项拆成两条 id。
2. **道痕的「只能选最高级」与「全流派道藏」的交叠**：`wuxing_daocang` 与 `<X>_dacheng` 是不同类的加值（前者全 44 项 +300、后者单项 +800），若 `<X>` 族只留最高级，则 200/800 二选一，但 +300 全流派仍会叠加。需明确「单流派族」与「全流派族」是否互斥。
3. **降级/替换语义**：现有 M3 允许降级（`TianfuEconomy.java:155-172` 无 tier 比较）。建议改为「只升不降」（新 tier ≤ 旧 tier 时直接 `CONFLICT`），避免玩家误点掉高 tier 又无法后悔（撤销窗口只在确认前）。

### 5.4 迁移方案（旧档已存在同族多条）

建议新增 `SCHEMA = 3` + 键 `v2_interlock`，一次性、幂等地处理历史冲突：

1. **不剥夺、不退款、不改 `purchased`**（历史玩家的既得利益不动；退款会破坏「Receipt 原价」账本一致性）。
2. 写入 `v2_interlock = { resolved_at: <revision>, duplicates: [<family>:<id>,…] }`，作为「已裁决」标记，避免重复扫描。
3. **效果侧取最高值**：在 `TianfuTalents.attrs/sum` 与硬编码分支（`TianfuNativeProgression.java:17-20,38`、`TianfuCombat.java:266-277`、`TianfuEffects.java:60-68,113-116`）前加一层 `TianfuInterlock.effective(ids)` 过滤，把同族非最高项从「生效集合」里剔除（仍保留在账上、不可再购、不可撤）。
4. **禁止再购**：`buy()` 在 M3 处对「已存在任意同族成员」一律 `CONFLICT`（含降级）。
5. **迁移顺序修正（同时修 ②B2）**：把 `TianfuV2Store.migrate()` 前移到 `TianfuApply.buyReason/revoke` 的入口（而非只放在 `openSelection`），或令 `commit()` 在 `next.schema() < SCHEMA` 时拒绝落库。

---

## ⑥ 需修改的类与方法（按优先级）

| 优先级 | 类::方法 | 位置 | 变更要点 |
|---|---|---|---|
| **P0** | `compat::Guzhenren98Compatibility.SEMANTICS` / `verify` | `Guzhenren98Compatibility.java:16-25`、`:27-40` | 重钉 10.20 的 3 条 SHA（`UikongqiaoquedingProcedure`/`KE1Procedure`/`UIxiuliananjianProcedure`），或改成「多版本允许集」+ 字段探测。**否则 1.4.18 在 10.20 无法启动**（`TianfuMod.java:48`） |
| **P0** | `v2::TianfuMindTable` | `TianfuMindTable.java:11-23` | 扩为 3 类表：`TIER`（17 族含扩员）、`EXCLUSIVE`（6 个 `neg_*`）、`CAP`（减伤/闪避/暴击/气运等封顶） |
| **P0** | `v2::TianfuEconomy.buy` | `TianfuEconomy.java:155-172` | 把 M3 从「仅 `LEVELS`」改为查新表；补 `EXCLUSIVE`/`CAP` 判定；tier 降级 → `CONFLICT`；`CAP` 需要「族内 Σ」计算，建议新增 `TianfuInterlock` 工具类 |
| **P0** | `v2::TianfuV2Store.commit` / `migrate` | `TianfuV2Store.java:267`、`:185` | `commit` 不得在 `next.schema() < SCHEMA` 时硬写 2（或直接拒绝）；`migrate` 提到买/撤入口（配合 `TianfuApply.java:103-125`、`:142-169`） |
| **P1** | `TianfuApply.settleSelection` | `TianfuApply.java:193` | `isAvailable` → `byIdIncludingUnavailable`（或对 `NATIVE_UNAVAILABLE` 只警告不阻断），解 ②B5 永久卡号 |
| **P1** | `TianfuMainDao.highest` | `TianfuMainDao.java:65-76` | 并列检测：收集所有最大值，`size>1` 时返回空串（交给 7 路均分）或返回「并列集合」交由调用方处理；**至少不要静默取 `jiandao`**。另：`KEYS` 顺序不应承载语义（`:37-39`） |
| **P1** | `client::TianfuUiRules.purchase` | `TianfuUiRules.java:16-35` | 镜像新表（置灰 + 原因文案），否则 UI 与服务器判定继续分叉 |
| **P1** | `TianfuInterlock`（新增） | 新文件 | 输出 `effective(ids)`，供 `TianfuTalents.sum/attrs`、`TianfuNativeProgression`、`TianfuCombat`、`TianfuEffects` 统一取用（5.4 第 3 步） |
| **P2** | `TianfuTalents` `zonghen_chubei` | `TianfuTalents.java:190` | 改写成写具体 `daohen_*` 字段（或直接下架并退款），否则 +300 被本体每 tick 重算覆盖（R10） |
| **P2** | `TianfuReincarnation.capture/complete` | `TianfuReincarnation.java:14-15`、`:38-39` | 把「待完成转世」标记落 NBT（如 `tianfu_pending_wipe`）而不是 `ThreadLocal`，避免崩溃/重启后漏 wipe（R3） |
| **P2** | `TianfuEffects.onTick` 自愈分支 | `TianfuEffects.java:146-165` | 加重试计数上限 + 一次性玩家提示，避免确定性失败时每 20 tick 刷日志（R6） |
| **P2** | Mixin 兜底 | `UikongqiaoquedingProcedureMixin.java` | 追加对 `UikongqiaoguanbiProcedure` 的同类 Redirect（`require = 0`），抵御 10.20→后续版本把壳再删 |

---

## ⑦ 未验证项（需实机 / 需缺失依赖）

1. **【未验证】NeoForge 21.1 对 `commonSetup` 内 `IllegalStateException` 的确切处置**（mod 加载失败 vs 仅回调失败）。结论「1.4.18 无法启动」按惯例推断，需实机跑一次 10.20 + 现版本 jar 确认。
2. **【未验证】MC 侧注入点**：`ItemCooldowns.addCooldown(Item;I)V`（1.21.1）与 `ServerItemCooldowns` 的 `player` 字段名。本机仅有 1.20.1 的 `client-extra.jar`，无法核 1.21.1；需 MC/NeoForge 21.1.xx jar 或实机启动日志。
3. **【未验证】并发压测**：`revision` CAS 在真实网络抖动/重发包下的表现（同 tick 多包、丢包重传）需实机注入验证。
4. **【未验证】崩溃注入**：转世流程在 capture 与 complete 之间 kill 进程，验证 R3 的漏 wipe 频率与后果。
5. **【未验证】本体对减伤的 clamp**：`reduction` 族叠到 >1.0 时，本体是否自带下限（本报告只证明附属侧加算无 cap）。
6. **【未验证】本体是否另有 `daohen_zong` 写入点**：本次只核了 `KE1Procedure`（10.20 唯一命中）；`TianfuSlay.java:209` 还提到「冒名实体」，未逐一反编译核实。
7. **【未验证】`migrate` 对「旧价未知」条目的长期影响**：`TianfuV2Store.java:198-201` 静默跳过，未验证这类条目在确认/撤销/UI 展示上的完整表现。
8. **【未验证】实机体感**：①1.3 表中 6.6× 生命、5800 道痕、+530% 修炼等组合的实际战斗表现与是否被本体其它管线二次约束。

---

## 附注：核验方法与可复现命令

```powershell
# 1) class SHA-256 比对（10.20 vs 9.8 的 9 条常量）——用 .NET ZipFile 取条目后 SHA256
# 2) 注入点计数
& 'C:\Program Files\Java\jdk-17\bin\javap.exe' -p -c -classpath '<jar>' '<FQCN>' |
    Out-File -Encoding utf8 "$env:TEMP\cls.txt"
#   然后按方法归属统计（本报告用 python 逐行扫描 'putfield …Field' / 'Math.random' 并与方法头比对）
```

实测计数一览（10.20）：`Math.random`×4（`UIxiuliananjianProcedure.execute`）、`PUTFIELD gushi_xiulian_dangqian`×3、`PUTFIELD shiqiao:Z`×2（私有 `execute(Event,LevelAccessor,Entity)`）、`PUTFIELD zhenyuan`×7、`PUTFIELD niantou`×4、`Player.closeContainer`×1、`PUTFIELD daohen_zong`×1（KE1，偏移 8433，无对应 `GETFIELD`）。
