package net.guzhenren.tianfu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.guzhenren.init.GuzhenrenModParticleTypes;
import net.guzhenren.init.GuzhenrenModSounds;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.procedures.UikongqiaoquedingProcedure;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * 开窍入口与收尾。
 *
 * <ul>
 *   <li><b>唯一开窍开关</b>：希望蛊右键 → {@link #onUseHopeGu}（由
 *       {@code YizhuanrendaoxiwangguItemMixin} 在 {@code YizhuanrendaoxiwangguItem.use()} HEAD 调用）。
 *       本体随机空窍资质（kongqiao 1–10）与本体资质面板已被整体取消；</li>
 *   <li><b>收尾</b>：本体初始化成功后提交开窍标记与消耗，再提示并打开附属界面。
 *       显式事务期间屏蔽本体 TAIL 的界面回调。</li>
 * </ul>
 *
 * <p>整个开窍流程中 kongqiao 保持本体初始值 0：本体随机资质结算不会命中，
 * 真元上限保持 0。最终确认空窍档位时，附属按本体原生范围写入该档位的真元上限。
 */
public final class TianfuOpening {
    private static final ThreadLocal<Map<UUID, Integer>> NATIVE_SETTLEMENT =
            ThreadLocal.withInitial(HashMap::new);

    private TianfuOpening() {
    }

    // ------------------------------------------------------------------ 希望蛊开窍（唯一开关）

    /**
     * 右键希望蛊：附属天赋系统的唯一开窍开关。
     *
     * <p>复刻本体的开窍仪式（音效 / kaiqiao 标记 / 消耗 / 粒子 / 成就），但<b>不</b>复刻随机
     * kongqiao 与资质面板；随后保持 kongqiao=0 调本体收尾。
     *
     * @param stack 玩家手持的希望蛊（未开窍时消耗一只）
     */
    public static void onUseHopeGu(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Level level = player.level();
        if (hasOpenedQiao(player)) {
            return; // 已开窍：无效果、不消耗（与本体原行为一致）
        }

        if (stack.isEmpty()) {
            throw new IllegalStateException("希望蛊已不在手中");
        }
        var vars = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
        CompoundTag beforeVars = vars.serializeNBT(player.registryAccess());
        CompoundTag beforeRoot = player.getPersistentData().contains(TianfuData.ROOT, Tag.TAG_COMPOUND)
                ? player.getPersistentData().getCompound(TianfuData.ROOT).copy() : null;
        ItemStack beforeStack = stack.copy();
        Scoreboard scoreboard = player.getScoreboard();
        var holder = ScoreHolder.forNameOnly(player.getScoreboardName());
        Map<Objective, Integer> beforeScores = new HashMap<>();
        for (Objective objective : scoreboard.getObjectives()) {
            var info = scoreboard.getPlayerScoreInfo(holder, objective);
            if (info != null) beforeScores.put(objective, info.value());
        }
        try {
            setKaiqiao(level, player, 1);
            // The replacement wizard chooses the grade later; never enter native's delayed
            // random physique branch while opening. Net lifespan remains the native ritual's
            // original value: its temporary +1/-1 pulse belongs to the replaced 30-tick ritual.
            vars.kongqiao = 0.0;
            runNativeSettlement(player, () -> UikongqiaoquedingProcedure.execute(level, player));
            TianfuData.markApplied(player);
            stack.shrink(1);
            vars.markSyncDirty();
        } catch (RuntimeException | LinkageError failure) {
            vars.deserializeNBT(player.registryAccess(), beforeVars);
            vars.markSyncDirty();
            if (beforeRoot == null) player.getPersistentData().remove(TianfuData.ROOT);
            else player.getPersistentData().put(TianfuData.ROOT, beforeRoot);
            scoreboard.resetAllPlayerScores(holder);
            beforeScores.forEach((objective, value) -> scoreboard.getOrCreatePlayerScore(holder, objective).set(value));
            player.setItemInHand(hand, beforeStack);
            throw new IllegalStateException("希望蛊开窍失败，已恢复物品与角色数据", failure);
        }

        // Presentation happens only after the reversible state has committed. A missing
        // cosmetic resource must not turn a completed opening into a reported failure.
        try {
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            level.playSound(null, BlockPos.containing(x, y, z),
                    GuzhenrenModSounds.GUCHONG_CUIDONG_4.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.addParticle(ParticleTypes.FLASH, x, y, z, 0.0D, 1.0D, 0.0D);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(GuzhenrenModParticleTypes.XIWANGGULIZI.get(),
                        x, y, z, 100, 1.0D, 1.0D, 1.0D, 0.05D);
            }
            awardAdvancement(player, "guzhenren:cchengjiukaiqiao");
            notifyOpened(player);
        } catch (RuntimeException | LinkageError failure) {
            TianfuMod.LOGGER.error("[天命] 开窍已成功，但仪式显示或界面通知失败；可按 K 打开", failure);
        }
    }

    /** Run native initialization without its TAIL hook opening or committing the wizard. */
    public static void runNativeSettlement(ServerPlayer player, Runnable settlement) {
        Map<UUID, Integer> scopes = NATIVE_SETTLEMENT.get();
        UUID id = player.getUUID();
        scopes.merge(id, 1, Integer::sum);
        try {
            settlement.run();
        } finally {
            int depth = scopes.get(id) - 1;
            if (depth == 0) scopes.remove(id);
            else scopes.put(id, depth);
            if (scopes.isEmpty()) NATIVE_SETTLEMENT.remove();
        }
    }

    /** True only while this thread is invoking the native grade/opening delegate for this player. */
    public static boolean isNativeSettlement(Player player) {
        Map<UUID, Integer> scopes = NATIVE_SETTLEMENT.get();
        boolean active = scopes.containsKey(player.getUUID());
        if (scopes.isEmpty()) NATIVE_SETTLEMENT.remove();
        return active;
    }

    /** 是否已以希望蛊开窍（本体 scoreboard {@code kaiqiao} 非 0）。 */
    public static boolean hasOpenedQiao(Player player) {
        return kaiqiaoScore(player.level(), player) != 0;
    }

    /**
     * 供诊断命令读取的原始 {@code kaiqiao} 分数。
     *
     * <p>{@link #hasOpenedQiao} 只回布尔，排查「按 K 没反应」时需要看到<b>具体值</b>
     * （0 = 没开窍 / 1 = 已开窍 / 其它 = 数据异常）。
     */
    public static int kaiqiaoScoreOf(Player player) {
        return kaiqiaoScore(player.level(), player);
    }

    /**
     * 转世：把本体 scoreboard {@code kaiqiao} 归零。
     *
     * <p>9.8 的转世入口会调用 SxProcedure，原生已经把此标记归零。
     * 保留此方法供旧集成调用，生命周期清理不再重复写本体标记。
     */
    public static void resetKaiqiao(Player player) {
        setKaiqiao(player.level(), player, 0);
    }

    // ------------------------------------------------------------------ 开窍收尾

    /**
     * 关闭开窍界面之后（本体收尾逻辑已跑完）：标记本世已结算 + 提示 K 键 + 未选难度时弹附属界面。
     *
     * <p>处理本体独立调用的 TAIL 回调；显式事务由调用方在成功后完成通知。
     */
    public static void onCloseOpenQiao(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (NATIVE_SETTLEMENT.get().containsKey(player.getUUID()) || !hasOpenedQiao(player)) {
            return;
        }
        if (TianfuData.isApplied(player)) {
            return; // 本世开窍已结算，防重入
        }
        TianfuData.markApplied(player);

        notifyOpened(player);
    }

    private static void notifyOpened(ServerPlayer player) {
        player.displayClientMessage(Component.literal("§b[天赋] 希望蛊已开窍，按 K 打开天赋界面"), false);

        // 未选开局难度 → 自动弹出天赋界面让玩家先选难度
        if (TianfuData.difficulty(player) == TianfuDifficulty.NOT_CHOSEN) {
            TianfuNet.openSelection(player);
        }
    }

    // ------------------------------------------------------------------ 生命周期

    /** 登录：重建属性修正（transient 修正不落盘，需每次登录重建）+ 一行按键提示。 */
    public static void onLogin(ServerPlayer player) {
        TianfuAttributes.sync(player);
        // 进世界提示一次：金底加粗标签 + 白色加粗键名，聊天栏里一眼能看到
        player.displayClientMessage(Component.literal(
                "§6§l[天命] §r§e按 §f§lK §e打开天赋界面"), false);
    }

    /** 死亡重生：把 tianfu 数据显式带过去，避免 PersistentData 在克隆时丢失。 */
    public static void onClone(ServerPlayer newPlayer, Entity oldPlayer) {
        onClone(newPlayer, oldPlayer, false);
    }

    /** 克隆玩家（死亡重生 / 末地传送门返程）：带回 NBT、同步永久属性修正并保留或补满真实血量。 */
    public static void onClone(ServerPlayer newPlayer, Entity oldPlayer, boolean wasDeath) {
        CompoundTag oldData = oldPlayer.getPersistentData();
        if (oldData.contains(TianfuData.GENERATION_KEY, Tag.TAG_INT)) {
            newPlayer.getPersistentData().putInt(TianfuData.GENERATION_KEY,
                    oldData.getInt(TianfuData.GENERATION_KEY));
        }
        if (oldData.contains(TianfuData.ROOT, Tag.TAG_COMPOUND)) {
            newPlayer.getPersistentData().put(TianfuData.ROOT, oldData.getCompound(TianfuData.ROOT).copy());
        }
        // Native 9.8 replaces PLAYER_VARIABLES at the end of its NORMAL clone subscriber,
        // including boolean shiqiao. Our LOWEST handler only restores our own NBT.
        TianfuAttributes.sync(newPlayer);
        if (wasDeath) {
            newPlayer.setHealth(newPlayer.getMaxHealth());
        } else if (oldPlayer instanceof net.minecraft.world.entity.LivingEntity oldLiving) {
            newPlayer.setHealth(Math.min(newPlayer.getMaxHealth(), Math.max(1.0F, oldLiving.getHealth())));
        }
    }

    // ------------------------------------------------------------------ 本体 scoreboard 读写

    private static int kaiqiaoScore(Level level, Entity entity) {
        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getObjective("kaiqiao");
        return objective != null
                ? scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(entity.getScoreboardName()), objective).get()
                : 0;
    }

    private static void setKaiqiao(Level level, Entity entity, int value) {
        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getObjective("kaiqiao");
        if (objective == null) {
            objective = scoreboard.addObjective("kaiqiao", ObjectiveCriteria.DUMMY,
                    Component.literal("kaiqiao"), ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(entity.getScoreboardName()), objective).set(value);
    }

    private static void awardAdvancement(ServerPlayer player, String id) {
        AdvancementHolder holder = player.server.getAdvancements().get(ResourceLocation.parse(id));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return;
        }
        for (String criteria : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criteria);
        }
    }
}
