// src/main/java/net/guzhenren/tianfu/TianfuEffects.java
package net.guzhenren.tianfu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.guzhenren.init.GuzhenrenModMobEffects;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/**
 * 词条 / 体质的持续效果（服务端 tick）。
 *
 * <p>跟随本体 9.8 的真实结算入口工作：修炼加成只改本体已通过资源与状态门槛的进度增量；
 * 真元、念头回复只缩放本体实际产生的正向增量；蛊冷却只调整本体下发到服务端冷却表的时长。
 * 这些持续效果不自行猜测自然回复基线，也不改写真元上限。
 */
public final class TianfuEffects {
    private static final java.util.Map<java.util.UUID, Long> FAILED_SETTLE_REV =
            new java.util.concurrent.ConcurrentHashMap<>();

    private TianfuEffects() {
    }

    public static void forget(java.util.UUID id) {
        if (id != null) {
            FAILED_SETTLE_REV.remove(id);
        }
    }

    /**
     * 结算时的一次性效果（上限类）。
     *
     * @param bodyTizhi 本次购买条目对应的体质值（非体质条目传 0）。
     *                  体质上限类分支<b>只认这个参数、不读 {@code v.tizhi}</b>，
     *                  否则「已有体质时购买任意词条」会把该体质的上限重复乘算。
     */
    public static void applyOnce(ServerPlayer player, PlayerVariables v, Set<String> ids, int bodyTizhi) {
        applyOnce(player, v, ids, bodyTizhi, TianfuTalents.snapshotCapped(v));
    }

    static void applyOnce(ServerPlayer player, PlayerVariables v, Set<String> ids, int bodyTizhi,
                          java.util.Map<String, Double> capBase) {
        // 体质 · 上限类
        if (bodyTizhi == 24) {                // 幽魂凝识体
            v.zuida_hunpo *= 1.25;
            v.hunpochengzaijixian *= 1.25;
        }
        if (bodyTizhi == 28) {                // 搬山撼岳体
            v.zuida_jingli *= 1.15;
        }
        if (bodyTizhi == 42) {                // 慧心通明体
            v.niantou_rongliang *= 1.25;
            v.niantou_zhida *= 1.25;
        }
        if (bodyTizhi == 43) {                // 光阴逆转体
            v.zhoudao_shijian_baifenbi += 1.5;
            v.zhoudao_cd_baifenbi = v.zhoudao_cd_baifenbi <= 0.0 ? 0.875 : v.zhoudao_cd_baifenbi * 0.875;
        }

        // 词条 · 一次性
        // 气运一律走「上限」口径：本体每秒只做向下削平（cap < qiyun 时 qiyun = cap），
        // 直接加 qiyun 会被下一次削平抹掉，抬上限才耐久（上限本体从不覆写）。
        if (ids.contains("fuze_mianchang")) { // 福泽绵长
            addCap(v, 15.0);
        }
        if (ids.contains("shouyuan_mianchang")) { // 寿元绵长
            v.shouyuan += 50.0;
        }
        if (ids.contains("qiyun_weiguang")) { // 气运微光
            addCap(v, 5.0);
        }

        // 扩展词条表：上限类（乘法）与气运 / 寿元（加法）一次性写入本体字段，并按同一基值统一钳回
        TianfuTalents.applyOnce(player, v, ids, capBase);

        v.markSyncDirty();
    }

    /**
     * applyOnce 的反向回滚（撤销天赋时调用）。
     *
     * <p>乘法回滚直接除以因子；加法回滚直接减去加值；
     * 光阴逆转体的条件乘法（zhoudao_cd_baifenbi * 0.875）**不可完全回滚**
     * （不知道当时走的哪个分支），保守处理：只减 += 1.5 的部分，不碰条件乘法的残留。
     *
     * @param bodyTizhi 本次撤销条目对应的体质值（非体质条目传 0），与 {@link #applyOnce} 对称
     */
    public static void revokeOnce(ServerPlayer player, PlayerVariables v, Set<String> ids, int bodyTizhi) {
        // 体质 · 流派 buff 回收（只撤本模组挂的无限时长那份，不碰玩家自己吃蛊的限时 buff）
        TianfuLiupai.revoke(player, bodyTizhi);
        // 体质 · 上限类反向
        if (bodyTizhi == 24) {
            if (v.zuida_hunpo != 0.0) v.zuida_hunpo /= 1.25;
            if (v.hunpochengzaijixian != 0.0) v.hunpochengzaijixian /= 1.25;
        }
        if (bodyTizhi == 28 && v.zuida_jingli != 0.0) {
            v.zuida_jingli /= 1.15;
        }
        if (bodyTizhi == 42) {
            if (v.niantou_rongliang != 0.0) v.niantou_rongliang /= 1.25;
            if (v.niantou_zhida != 0.0) v.niantou_zhida /= 1.25;
        }
        if (bodyTizhi == 43) {
            v.zhoudao_shijian_baifenbi -= 1.5;
            // applyOnce 里是 *0.875（CD 倍率越小越快），故回滚必须除以 0.875，
            // 不能写死成 1.0——本体基线本身就不是 1.0，写死会把玩家 CD 倍率抬高（变相削弱）。
            if (v.zhoudao_cd_baifenbi > 0.0) {
                v.zhoudao_cd_baifenbi /= 0.875;
            }
        }

        // 词条 · 一次性反向
        if (ids.contains("fuze_mianchang")) {
            addCap(v, -15.0);
        }
        if (ids.contains("shouyuan_mianchang")) {
            v.shouyuan -= 50.0;
        }
        if (ids.contains("qiyun_weiguang")) {
            addCap(v, -5.0);
        }

        // 扩展词条表反向
        TianfuTalents.revokeOnce(player, v, ids);

        v.markSyncDirty();
    }

    /**
     * 气运上限增减并兜底非负。
     *
     * <p>本体每秒执行 {@code if (qiyun_shangxian < qiyun) qiyun = qiyun_shangxian}，
     * 上限一旦被减成负数会把气运硬压成负数（气运稀薄 −15 遇上低上限机缘时必然触发），
     * 故所有上限写入都经过这里，保证 {@code cap >= 0}。
     */
    private static void addCap(PlayerVariables v, double delta) {
        v.qiyun_shangxian = Math.max(0.0, v.qiyun_shangxian + delta);
    }

    /** 每 tick 调用。 */
    public static void onTick(ServerPlayer player) {
        // ---- 第三方界面自愈：确认了却没结算 ----
        // 「最终确认」必须【先结算、后落锁定标记】（见 TianfuConfirmPayload）。
        // 但第三方 UI（整合包里的 六合一大合集 newguui / TianfuBridge.confirmFinal）
        // 只调 TianfuData.markConfirmed(player)，不调 TianfuApply.applyAllConfirmed，
        // 结果 confirmed=true 却什么都没落地：体质没写、上限没乘、道痕没加，
        // 而且确认后已锁定不可撤销 → 玩家永久卡在「买了但没效果」。
        // 这里做一次幂等自愈：已确认但本世从未结算过的玩家，补跑一次结算。
        if (TianfuData.isConfirmed(player) && !TianfuData.isSettled(player) && player.tickCount % 20 == 0) {
            long rev = net.guzhenren.tianfu.v2.TianfuV2Store.read(player).revision();
            if (!Long.valueOf(rev).equals(FAILED_SETTLE_REV.get(player.getUUID()))) {
                // 旧存档兼容：本标记是 1.2.13 新增的，此前正常确认过的玩家都没有它。
                // 判据：已确认 + 体质已写进 v.tizhi（或已购里有十绝体/机缘）→ 说明当年结算过了，
                // 只补标记、绝不重跑 applyAllConfirmed（重跑会把上限 / 道痕乘两遍）。
                PlayerVariables pv = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
                boolean alreadyApplied = pv.tizhi != 0.0;
                if (!alreadyApplied) {
                    for (String id : TianfuApply.sanitize(TianfuData.purchased(player))) {
                        TianfuPool.Spec spec = TianfuPool.byId(id);
                        if (spec != null && (spec.kind() == TianfuPool.Kind.SIJUE
                                || spec.kind() == TianfuPool.Kind.JIYUAN)) {
                            alreadyApplied = true;
                            break;
                        }
                    }
                }
                // Legacy instant purchases must not receive their old upper bounds a second time.
                // V2 confirmation still has to settle origin/constraints before setting settled.
                var rejection = TianfuApply.tryApplyAllConfirmed(player, alreadyApplied && !TianfuData.isDeferred(player));
                if (rejection == net.guzhenren.tianfu.v2.TianfuSelection.Rejection.NONE) {
                    FAILED_SETTLE_REV.remove(player.getUUID());
                } else {
                    FAILED_SETTLE_REV.put(player.getUUID(), rev);
                    TianfuMod.LOGGER.warn("[天命] 自动补结算被拒绝（player={}, rev={}, reason={}），本修订不再每秒重试",
                            player.getScoreboardName(), rev, rejection);
                }
            }
        }

        // 闸门：本世天赋「最终确认」前，词条与体质加成一律不生效。
        // 只把 ids / tizhi 视为空，基础回复仍然保留（不 return，避免确认前真元完全不回）。
        boolean active = TianfuData.isConfirmed(player);
        Set<String> ids = TianfuApply.effective(player);
        PlayerVariables v = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
        int tizhi = active ? (int) Math.round(v.tizhi) : 0;
        boolean night = isNight(player);
        // 只有真的改动了 PlayerVariables 字段才置脏，避免每 tick 全量同步
        boolean touched = false;

        if (active && v.tizhi == 54.0) {
            TianfuSlay.onTick(player);
        }

        // ---- 恶意·杀戮光环：每 3 秒对 4 格内的敌对生物结算一次魔法伤害 ----
        if (active) {
            double[] aura = TianfuTalents.aura(ids);
            if (aura != null && player.tickCount % 60 == 0) {
                double dmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * aura[1];
                if (dmg > 0.0) {
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(aura[0]),
                            x -> x != player && x.isAlive() && x instanceof Enemy)) {
                        target.hurt(player.damageSources().magic(), (float) dmg);
                    }
                }
            }
        }

        // ---- 体质自带的流派 buff：无限时长常驻（附近玩家可见状态图标）----
        // 只挂本体确实存在对应流派 buff 的体质（见 TianfuLiupai），缺流派的不硬造。
        if (active && TianfuLiupai.hasBuff(tizhi)) {
            TianfuLiupai.maintain(player, tizhi, v);
        }

        // ---- 体特效总闸：/tianfu vfx off 后一行粒子都不发 ----
        boolean vfx = TianfuData.vfxEnabled(player);

        // ---- 体质持续特效（附近所有玩家可见；错峰广播，见 TianfuVfx）----
        if (vfx) {
            TianfuVfx.play(player.level() instanceof ServerLevel serverLevel ? serverLevel : null,
                    player, tizhi, player.tickCount);
        }

        // ---- 体质「形态层」：8 种专属形态的纯粒子表达 ----
        // v1.2.27 起彻底改为粒子，不再生成任何本体实体。
        // 旧实现生成 Dalongjuanfeng 等 GeckoLib 实体做装饰，但反编译确认：
        // setNoAi 不拦 baseTick，而 baseTick 里的 *ShiTi*GengXin*Procedure
        // 会对周围 7.5 格内所有实体造成 600 伤害（连玩家自己都打）。
        // 详见 TianfuVfxEntity 类文档。这里的调用只发粒子，零实体、零伤害。
        if (active && vfx) {
            TianfuVfxEntity.tick(
                    player.level() instanceof ServerLevel sl ? sl : null,
                    player, tizhi, player.tickCount);
        }

        // ---- 词条光环（按最高品质档只播一个，见 TianfuVfx#playAura）----
        if (active && vfx) {
            TianfuVfx.playAura(player.level() instanceof ServerLevel serverLevel ? serverLevel : null,
                    player, ids, player.tickCount);
        }

        // 9.8 的回复、温养与蛊冷却由真实本体过程钩子处理，保留本体资源门禁与节拍。
        // 杀招天赋的单位为每个游戏 tick 的额外冷却量，直接作用于已核实的四个本体计时字段。
        double shazhaoCd = TianfuTalents.shazhaoCd(ids);
        if (Double.isFinite(shazhaoCd) && shazhaoCd != 0.0) {
            double cd0 = extraCooldownStep(v.sanzhao_CD, shazhaoCd);
            double cd1 = extraCooldownStep(v.sanzhao_CD1, shazhaoCd);
            double cd2 = extraCooldownStep(v.sanzhao_CD2, shazhaoCd);
            double cd3 = extraCooldownStep(v.sanzhao_CD3, shazhaoCd);
            touched |= cd0 != v.sanzhao_CD || cd1 != v.sanzhao_CD1
                    || cd2 != v.sanzhao_CD2 || cd3 != v.sanzhao_CD3;
            v.sanzhao_CD = cd0;
            v.sanzhao_CD1 = cd1;
            v.sanzhao_CD2 = cd2;
            v.sanzhao_CD3 = cd3;
        }
        // ---- 炼蛊进度加成 ----
        // 本体真实机制（javap 实测）：
        //   ① 推进入口 LianGuguiMenu.onPlayerTick(PlayerTickEvent.Post)，要求 containerMenu 是炼蛊菜单；
        //   ② 节拍器 lg_ke 每 tick +1，`lg_ke / 20 >= 1.5`（lg_ke >= 30）时归零并 LianGuJinDu += 1，
        //      即【每 30 tick 才 +1 进度】；
        //   ③ 完成判定 LianGuJinDu == 100.0（dcmpl + ifne 精确相等，本体从不钳制）；
        //   ④ 50% 投料窗口是【区间】判定 50.0 <= LianGuJinDu <= 70.0（iflt + ifgt），
        //      且要求界面开着、槽位 0 放着蛊材原石（GUCAIYUANSHI）。
        //
        // 因此加成绝不能「每 tick 无条件加进度」：那会让进度脱离玩家操作，
        // 几十秒冲过 50→70 投料窗口，玩家来不及投料，炼出的蛊缺料。
        // 也不能用「差分观测本体增量」：我方与本体订阅的是同一个 PlayerTickEvent.Post，
        // 监听器先后顺序不受保证，差分会漏计或双计。
        //
        // 采用【独立节拍 + 整数补点】：不读 lg_ke（它会被本体归零，读它必然依赖监听器先后），
        // 改用我方 tickCount 每 30 tick 补一次整数点，每次只补到 min(99, …)，
        // 最后 1 点永远留给本体的 +1，确保精确命中 == 100.0。
        if (v.LianGu) {
            double liangu = TianfuTalents.liangu(ids)
                + net.guzhenren.tianfu.v2.TianfuOriginBonus.liangu(
                        net.guzhenren.tianfu.v2.TianfuV2Store.read(player));
            double cur = v.LianGuJinDu;
            // 界面开着 = 有配方编号。本体炼蛊写 617.0，但整合包里的「六合一大合集」
            // 用 KaiShiLianZhiMixin 写的是 9000 + 配方序号，魂道附属则写自己的配方号。
            // 早期版本硬判 == 617.0，在这些附属的炼蛊流程下加成会被整个绕过，
            // 故改为「非 0 即视为有配方在炼」。
            boolean guiOpen = v.GuFang != 0.0;
            // 投料窗口内不加速：给玩家留足手动投料时间。
            //
            // 1.4.18 修正（重要）：本体投料窗口**不是统一的 50–70**。对 792 个完成型过程
            // 逐一解码后分三族：
            //   A 族 347 个：只有 50 ≤ p ≤ 70；
            //   B 族 351 个：**20 ≤ p ≤ 40 前置段 + 50 ≤ p ≤ 70 第二段，两段都必须投料**
            //               （结算要求 中途加材料1 == 1 && 中途加材料 == 1）；
            //   C 族  94 个：无投料窗口。
            // 旧实现只冻结了 50–70，导致 B 族第一段窗口被加速压缩（31.5s → 9s）；
            // 更糟的是会跳过 p == 40 的边界失败判定（本体在此扣血 + 魂魄 −转数×10 +
            // 明确提示「炼制失败」），使玩家失去失败反馈，却在 p == 100 时因
            // 中途加材料1 != 1 而**整段跳过结算**——进度清零、材料与魂魄静默全损。
            // 因此冻结区间必须取所有投料窗口的**并集**。
            boolean inFeedWindow = (cur >= 20.0 && cur <= 40.0) || (cur >= 50.0 && cur <= 70.0);
            // 用我方独立节拍（每 30 tick = 本体 1 点的节奏），不读 lg_ke：
            // lg_ke 会被本体自己归零，读它必然依赖两个监听器的执行先后。
            boolean ourBeat = player.tickCount % 30 == 0;
            // ---- 坏档迁移：旧版本（1.2.10 及更早）的每 tick 推进可能把进度顶到 100 以上 ----
            // 本体完成判定是 == 100.0（精确相等），进度一旦 > 100 就再也命不中，
            // 该蛊永久炼不成、进度条也消失（区间判定 [n,n+10) 全部不匹配）。
            // 这里把它回正到 99，让本体的 +1 能重新精确落到 100 完成。
            if (cur > 100.0) {
                v.LianGuJinDu = 99.0;
                TianfuData.putDouble(player, "liangu_acc", 0.0);
                TianfuData.putDouble(player, "liangu_prev", 99.0);
                touched = true;
            } else {
                // 开炼/换蛊时本体把进度重置为 1.0：累加器必须一起清零，
                // 否则上一只蛊的小数余数会带进新的一只。
                double prev = TianfuData.getDouble(player, "liangu_prev", -1.0);
                if (prev >= 0.0 && cur < prev) {
                    TianfuData.putDouble(player, "liangu_acc", 0.0);
                }
                if (cur != prev) {
                    TianfuData.putDouble(player, "liangu_prev", cur);
                }
                // 进度为 0 也算「在炼」：本体 KaiShiLianZhiProcedure 开炼时置 1.0，
                // 而整合包「六合一大合集」的 KaiShiLianZhiMixin 置的是 0.0，
                // 早期用 cur > 0.0 当「已开炼」判据，在它的流程下加成同样不会启动。
                if (liangu > 0.0 && guiOpen && ourBeat && !inFeedWindow && cur >= 0.0 && cur < 99.0) {
                    // 小数累加器：每拍累加 liangu，攒够 1 点才补 1 点整数。
                    // 这样长期平均速率精确等于 liangu（+90% 就是每 10 拍补 9 点），
                    // 又保证 LianGuJinDu 始终是整数（本体完成判定是 == 100.0，小数会永远命不中）。
                    double acc = TianfuData.getDouble(player, "liangu_acc", 0.0) + liangu;
                    int bonus = (int) Math.floor(acc);
                    // 临窗减速：进入任一投料窗口前的最后 3 点强制逐点推进。
                    // 本体全部边界判定（== 40 / == 70 / == 100）都按「每次 +1」写，
                    // 单拍给 2–3 点会让这些精确相等判定被跳过。
                    boolean nearWindow = (cur >= 17.0 && cur < 20.0) || (cur >= 47.0 && cur < 50.0);
                    if (nearWindow) {
                        bonus = Math.min(bonus, 1);
                    }
                    if (bonus > 0) {
                        acc -= bonus;
                        // 上限 99：最后 1 点必须由本体的 +1 完成，越过 100 会让 == 100.0 永久失效
                        v.LianGuJinDu = Math.min(99.0, cur + bonus);
                        touched = true;
                    }
                    // 积压封顶 1 点：被临窗限速时不清账，但也不许攒过头，
                    // 否则窗口一结束会一次性释放多点，重新制造跳点。
                    TianfuData.putDouble(player, "liangu_acc", Math.min(acc, 0.999));
                } else if (inFeedWindow) {
                    // 窗口内完全不动进度，同时把积压压回 1 点以内（防窗口结束瞬间爆发）
                    if (TianfuData.getDouble(player, "liangu_acc", 0.0) > 0.999) {
                        TianfuData.putDouble(player, "liangu_acc", 0.999);
                    }
                }
            }
        }

        // ---- 万法皆通：全流派熟练度获取 ×3（每秒差分补偿，只奖励增量）----
        if (ids.contains("wanfa_jietong") && player.tickCount % 20 == 0 && wanfaJietong(player, v)) {
            touched = true;
        }

        // ---- 神·不灭金身：每秒回复 5% 最大生命 ----
        if (ids.contains("shen_bumie_jinshen") && player.tickCount % 20 == 0) {
            float max = player.getMaxHealth();
            if (player.getHealth() < max) {
                player.heal(max * 0.05F);
            }
        }
        // ---- 扩展词条：每 5 秒生命回复 ----
        double hp5s = TianfuTalents.regenHp5s(ids);
        if (hp5s > 0.0 && player.tickCount % 100 == 0) {
            float max = player.getMaxHealth();
            if (player.getHealth() < max) {
                player.heal((float) (max * hp5s));
            }
        }

        // ---- 心神如渊：沉默剩余时长减半（同一次施加只折半一次，用 floor 标记）----
        if (ids.contains("xinshen_ruyuan")) {
            var chenmo = player.getEffect(GuzhenrenModMobEffects.CHENMO);
            long floor = TianfuData.getLong(player, "chenmo_floor", 0L);
            if (chenmo == null) {
                if (floor != 0L) {
                    TianfuData.putLong(player, "chenmo_floor", 0L); // 效果结束，重置标记
                }
            } else if (chenmo.getDuration() > 40 && chenmo.getDuration() > floor) {
                int target = chenmo.getDuration() / 2;
                // Same-amplifier addEffect only extends duration; remove before shortening.
                player.removeEffect(GuzhenrenModMobEffects.CHENMO);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        GuzhenrenModMobEffects.CHENMO, target,
                        chenmo.getAmplifier(), chenmo.isAmbient(), chenmo.isVisible(), chenmo.showIcon()));
                TianfuData.putLong(player, "chenmo_floor", target);
            }
        }

        // ---- 御空踏云体：常驻飞行 ----
        updateFlight(player, tizhi);

        // ---- 周期性重建属性修正（幂等；同时兜住星辉照夜变的昼夜切换）----
        if (player.tickCount % 100 == 0) {
            TianfuAttributes.sync(player);
            TianfuTalents.maintainEffects(player, ids);   // 常驻药水效果（夜视 / 抗性 / 防火 等）
        }

        net.guzhenren.tianfu.v2.NeoForge1211Adapter.maintainConstraints(player, ids);

        if (touched) {
            v.markSyncDirty();
        }
    }

    // ---------------------------------------------------------------- 万法皆通

    /** 流派熟练度字段缓存（PlayerVariables 公共字段，反射解析一次后复用）。 */
    private static Field[] liupaiCache;

    /**
     * 万法皆通：所有流派熟练度（本体 {@code liupai_*} 权重字段，含大写 {@code Liupai_gudao}）获取 ×3。
     *
     * <p>每秒对每个字段做差分：正增量额外补 2 倍（净获取 ×3）；负变化（消耗/重置）只跟快照不补偿。
     * 快照存 PersistentData {@code liupai_snap}，首次购买时以当时值为基准，不追补历史。
     *
     * @return 是否发生了补偿写入（供置脏判断）
     */
    private static boolean wanfaJietong(ServerPlayer player, PlayerVariables v) {
        Field[] fields = liupaiFields();
        if (fields.length == 0) {
            return false;
        }
        CompoundTag snap = TianfuData.liupaiSnap(player);
        boolean gained = false;
        for (Field f : fields) {
            try {
                double cur = f.getDouble(v);
                double prev = snap.contains(f.getName(), Tag.TAG_DOUBLE) ? snap.getDouble(f.getName()) : cur;
                double delta = cur - prev;
                if (delta > 1.0E-9) {
                    double boosted = cur + delta * 2.0; // 净获取 ×3
                    f.setDouble(v, boosted);
                    snap.putDouble(f.getName(), boosted);
                    gained = true;
                } else {
                    snap.putDouble(f.getName(), cur);
                }
            } catch (IllegalAccessException ignored) {
                // 单字段反射失败：静默跳过（require=0 风格），不影响其余流派
            }
        }
        TianfuData.setLiupaiSnap(player, snap);
        return gained;
    }

    /** 收集 PlayerVariables 上全部流派熟练度公共字段（benign race，重复计算结果一致）。 */
    private static Field[] liupaiFields() {
        Field[] cached = liupaiCache;
        if (cached == null) {
            List<Field> out = new ArrayList<>();
            for (Field f : PlayerVariables.class.getFields()) {
                String n = f.getName();
                if ((n.startsWith("liupai_") || n.equals("Liupai_gudao")) && f.getType() == double.class) {
                    out.add(f);
                }
            }
            liupaiCache = cached = out.toArray(new Field[0]);
        }
        return cached;
    }

    // Abilities.mayfly 在本版 NeoForge 被打上 @Deprecated，但 Abilities 没有提供替代的 getter/setter，
    // 且 vanilla 的飞行判定仍在读这个字段，因此只能走字段。
    @SuppressWarnings("deprecation")
    public static void updateFlight(ServerPlayer player, int tizhi) {
        boolean want = tizhi == 45;
        boolean granted = TianfuData.getBoolean(player, "fly_granted", false);
        boolean creative = player.isCreative() || player.isSpectator();
        if (want) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            if (!granted) {
                TianfuData.putBoolean(player, "fly_granted", true); // 标记「本模组授予」
            }
            player.fallDistance = 0.0F;                              // 落地免摔伤
        } else if (granted) {
            // 只回收本模组授予的飞行，不干扰其它模组/创造模式
            if (!creative && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            TianfuData.putBoolean(player, "fly_granted", false);
        }
    }

    public static boolean isNight(ServerPlayer player) {
        long t = player.level().getDayTime() % 24000L;
        return t >= 13000L && t <= 23000L;
    }

    /**
     * 从已购列表里挑出「应当生效的体质档位」。
     *
     * <p>只认 {@link TianfuPool.Kind#BODY}（凡体，tizhi 17-53）与
     * {@link TianfuPool.Kind#SIJUE}（十绝体 1-10 / 杀神 54）。两者互斥
     * （{@code TianfuEconomy} 已保证），这里取第一个命中的。没有体质返回 0（凡人）。
     *
     * <p>抽成纯函数，便于脱离游戏跑回归测试。
     */
    static int physiqueTizhi(java.util.Collection<String> purchased) {
        if (purchased == null) {
            return 0;
        }
        for (String id : purchased) {
            TianfuPool.Spec spec = TianfuPool.byId(id);
            if (spec == null) {
                continue;
            }
            if (spec.kind() == TianfuPool.Kind.SIJUE || spec.kind() == TianfuPool.Kind.BODY) {
                return spec.tizhi();
            }
        }
        return 0;
    }

    /**
     * 空窍资质的<b>最终</b> kongqiao 值。
     *
     * <p>本体 {@code KE1Procedure} 会按真元上限把 kongqiao 反推成 甲2 / 乙1 / 丙5 / 丁8
     * （详见 {@code TianfuNative#applyAptitudeTier} 的注释）。这里按同一口径还原。
     */
    static double aptitudeKongqiao(java.util.Collection<String> purchased) {
        if (purchased == null) {
            return 0.0;
        }
        for (String id : purchased) {
            switch (id) {
                case "v2_apt_jia":
                    return 2.0;
                case "v2_apt_yi":
                    return 1.0;
                case "v2_apt_bing":
                    return 5.0;
                case "v2_apt_ding":
                    return 8.0;
                default:
                    break;
            }
        }
        return 0.0;
    }

    /** 旧调用入口仅重建附属派生属性；不覆盖本体合法变更的体质或资质。 */
    @Deprecated
    static void healPhysique(ServerPlayer player) {
        if (player != null) TianfuAttributes.sync(player);
    }

    /**
     * 推进「温养空窍」进度。
     *
     * <p>本体对这两个字段的用法（反编译 {@code UIxiuliananjianProcedure} 与
     * {@code XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure} 确认）：
     * <ul>
     *   <li>打坐每 tick 是 {@code dangqian = dangqian + 增量}，<b>本体自己不 clamp</b>；</li>
     *   <li>判定突破用 {@code dangqian == jindu} <b>精确相等</b>（offset 372-390，
     *       {@code dcmpl} + {@code ifne}，不等就 return），「已满」判定同理（offset 225-243）。</li>
     * </ul>
     *
     * <p>所以只要进度冲过目标，就永远「差一点」、永远无法突破。1.4.10 之前这里是
     * 无条件 {@code clamp(dangqian + xiulian, 0, jindu)}：{@code xiulian} 为负时
     * （心智有损 −0.40、另一条 −0.30）会把刚满的 400 打成 399.98，下一 tick 本体又
     * 加过头，如此往复——玩家点突破时读到的永远是不等于，表现就是
     * 「温养差一点点、永远突破不了」。
     *
     * <p>这里保证三件事：<b>溢出对齐</b>到 {@code jindu}、<b>正向</b>加速后夹到
     * {@code jindu}、<b>负向</b>只压未满的进度且绝不在已满时往回拉。
     *
     * @param dangqian 当前进度
     * @param jindu    目标进度（本体写入，0 表示还没设定）
     * @param xiulian  本 tick 的加速量（可为负）
     * @return 新的当前进度
     */
    static double wenyangStep(double dangqian, double jindu, double xiulian) {
        if (!Double.isFinite(jindu) || jindu <= 0.0) {
            return dangqian; // 没有目标，不碰
        }
        if (!Double.isFinite(dangqian)) {
            return 0.0;
        }
        if (!Double.isFinite(xiulian)) {
            xiulian = 0.0;
        }
        if (dangqian >= jindu - Math.max(1.0e-8, Math.ulp(jindu) * 8.0)) {
            return jindu; // 溢出对齐：让突破重新可用
        }
        if (xiulian > 0.0) {
            return clamp(dangqian + xiulian, 0.0, jindu);
        }
        if (xiulian < 0.0 && dangqian < jindu) {
            return Math.max(0.0, dangqian + xiulian); // 只压未满
        }
        return dangqian; // 已满 / 无加速：保持不动
    }

    /** 兼容 1.4.12 的入口：只对齐完成边界，不在玩家 tick 中伪造本体修炼增量。 */
    public static void alignCultivation(Entity entity) {
        TianfuNativeProgression.align(entity);
    }

    private static double extraCooldownStep(double current, double extra) {
        return Double.isFinite(current) && current > 0.0
                ? Math.max(0.0, current - extra) : current;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    /**
     * 回复乘区地板：把汇总后的百分比夹到 {@code [0, ∞)}，再交回给调用方乘到自然回复基准上。
     *
     * <p>存在的原因：负分项（真元枯竭 / 念头干涸 −30%）若按加性叠乘直穿
     * {@code 1 + Σ(−0.30) < 1} 甚至为负，会让「什么都没买」的玩家也每 tick 净扣真元／念头。
     * 夹到 0 表示「最多把自然回复抹平」，而不是把资源往外抽。
     */
    private static double multiplier(double raw) {
        return Math.max(0.0, raw);
    }

    /**
     * 只由现有onTick唯一通道在confirmed后调用适配器维护新心智和出身周期效果。
     * @param player 非null逻辑服务端玩家；未确认时不得应用新效果
     * @throws IllegalStateException 适配层字段失配时中止该次处理，不宣称已生效
     */
    public static void maintainV2(ServerPlayer player) {
        if (player == null) return;
        net.guzhenren.tianfu.v2.NeoForge1211Adapter.maintainConstraints(player, TianfuApply.effective(player));
    }
}
